package com.kliksigurnost.demo.service.impl;

import com.kliksigurnost.demo.exception.NotificationNotFoundException;
import com.kliksigurnost.demo.exception.UnauthorizedAccessException;
import com.kliksigurnost.demo.model.*;
import com.kliksigurnost.demo.repository.CloudflareAccountRepository;
import com.kliksigurnost.demo.repository.CloudflarePolicyRepository;
import com.kliksigurnost.demo.repository.NotificationRepository;
import com.kliksigurnost.demo.service.CloudflareLogService;
import com.kliksigurnost.demo.service.CloudflareNotificationService;
import com.kliksigurnost.demo.service.UserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CloudflareNotificationServiceImpl implements CloudflareNotificationService {

    private final CloudflareLogService cloudflareLogService;
    private final UserService userService;
    private final CloudflarePolicyRepository policyRepository;
    private final NotificationRepository notificationRepository;
    private final CloudflareAccountRepository accRepository;

    private final Environment env;

    @Override
    @Scheduled(fixedRate = 300000)
    public void checkBlockedContent() {
        log.info("Checking for blocked content...");

        Instant endTime = Instant.now();
        Instant startTime = endTime.minus(5, ChronoUnit.MINUTES);

        List<String> accountIds = getAllAccountIds();

        for (String accountId : accountIds) {
            log.info("Fetching logs for account: {}", accountId);

            List<CloudflareLog> logs = cloudflareLogService.getLogsForAccount(
                    accountId,
                    startTime.toString(),
                    endTime.toString(),
                    List.of("datetime_DESC"),
                    accountId,
                    null,
                    1000,
                    9
            );

            logs.forEach(clog -> {
                if (clog.getResolverDecision() == 9) {
                    CloudflarePolicy policy = policyRepository.findById(clog.getPolicyId())
                            .orElse(null);

                    if (policy != null) {
                        User user = policy.getUser();
                        String notificationMessage = String.format(
                                "Pokušaj pristupa zabranjenom sadržaju (pravilo: %s: %s)",
                                policy.getName(),
                                clog.getQueryName()
                        );

                        Notification notification = Notification.builder()
                                .isSeen(false)
                                .message(notificationMessage)
                                .user(user)
                                .type(NotificationType.LOG)
                                .timestamp(Instant.parse(clog.getDatetime())).build();

                        log.info("Saving notification: {}", notificationMessage);
                        notificationRepository.save(notification);
                    }
                }
            });
        }
    }

    private List<String> getAllAccountIds() {
        List<CloudflareAccount> accounts = accRepository.findAll();
        List<String> accIds = new ArrayList<>();
        for (CloudflareAccount acc : accounts) {
            accIds.add(acc.getAccountId());
        }
        return accIds;
    }

    @Override
    public List<Notification> getNotificationsByUser() {
        return notificationRepository.findByUser(userService.getCurrentUser());
    }

    @Override
    @Transactional
    public List<Notification> getUnseenNotificationsByUser() {
        List<Notification> notifications = notificationRepository.findByUserAndIsSeen(userService.getCurrentUser(), false);
        List<Integer> notificationIds = new ArrayList<>();
        notifications.forEach(notification -> {
            notificationIds.add(notification.getNotificationId());
        });
        markNotificationsAsSeen(notificationIds);
        return notifications;
    }

    @Override
    public Integer getUnseenNotificationCountByUser() {
        return notificationRepository.countByUserAndIsSeen(userService.getCurrentUser(), false);
    }

    @Override
    @Transactional
    public void markNotificationsAsSeen(List<Integer> notificationIds) {
        User currentUser = userService.getCurrentUser();
        List<Notification> notifications = notificationRepository.findAllById(notificationIds);

        notifications.forEach(notification -> {
            if (!notification.getUser().equals(currentUser)) {
                throw new UnauthorizedAccessException(env.getProperty("notification-unauthorized-seen"));
            }
        });

        notificationRepository.markNotificationsAsSeen(notificationIds);
    }

    @Override
    public void deleteNotification(Integer notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotificationNotFoundException(env.getProperty("notification-not-found")));

        if (!notification.getUser().equals(userService.getCurrentUser())) {
            throw new UnauthorizedAccessException(env.getProperty("notification-unauthorized-delete"));
        }

        notificationRepository.delete(notification);
    }

    @Override
    public void createNotificationForDevices(List<CloudflareDevice> devicesList, User user) {
        for (CloudflareDevice device : devicesList) {
            // if device last seen is before 5 days ago create notification
            if (Instant.parse(device.getLastSeenTime()).isBefore(Instant.now().minus(5, ChronoUnit.DAYS)) ) {

                if (!notificationRepository.existsByDeviceIdAndIsSeen(device.getId(), false)) {
                    Notification notification = Notification.builder()
                            .isSeen(false)
                            .message("Uredjaj " + device.getManufacturer() + " zadnji put vidjen pre više od 5 dana. Proverite 'Cloudflare One' aplikaciju na vašim uredjajima")
                            .user(user)
                            .type(NotificationType.DEVICE)
                            .deviceId(device.getId())
                            .timestamp(Instant.now()).build();

                    notificationRepository.save(notification);
                }
            }
        }
    }
}