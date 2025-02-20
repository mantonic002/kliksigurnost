package com.kliksigurnost.demo.service.impl;

import com.kliksigurnost.demo.model.*;
import com.kliksigurnost.demo.repository.CloudflareAccountRepository;
import com.kliksigurnost.demo.repository.CloudflarePolicyRepository;
import com.kliksigurnost.demo.repository.NotificationRepository;
import com.kliksigurnost.demo.service.CloudflareLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import com.kliksigurnost.demo.service.CloudflareNotificationService;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;


@Slf4j
@Service
@RequiredArgsConstructor
public class CloudflareNotificationServiceImpl implements CloudflareNotificationService {

    private final CloudflareLogService cloudflareLogService;
    private final CloudflarePolicyRepository policyRepository;
    private final NotificationRepository notificationRepository;
    private final CloudflareAccountRepository accRepository;

    @Override
    @Scheduled(fixedRate = 300000)
    public void checkBlockedContent() {
        log.info("Checking for blocked content...");

        // Calculate start and end time for the last 5 minutes
        Instant endTime = Instant.now();
        Instant startTime = endTime.minus(5, ChronoUnit.MINUTES);

        List<String> accountIds = getAllAccountIds();

        for (String accountId : accountIds) {
            log.info("Fetching logs for account: {}", accountId);

            // Fetch logs for the current account
            List<CloudflareLog> logs = cloudflareLogService.getLogsForAccount(
                    accountId,
                    startTime.toString(),
                    endTime.toString(),
                    List.of("datetime_DESC"),
                    accountId,
                    null,
                    1000,
                    "next"
            );

            // Process logs for the current account
            logs.forEach(clog -> {
                if (clog.getResolverDecision() == 9) {
                    CloudflarePolicy policy = policyRepository.findById(clog.getPolicyId())
                            .orElse(null);

                    if (policy != null) {
                        User user = policy.getUser();
                        String notificationMessage = String.format(
                                "Blocked content accessed under policy %s: %s",
                                policy.getName(),
                                clog.getQueryName()
                        );

                        Notification notification = Notification.builder()
                                .isSeen(false)
                                .message(notificationMessage)
                                .user(user)
                                .timestamp(Instant.parse(clog.getDatetime())).build();

                        log.info("Saving notification: {}", notificationMessage);
                        notificationRepository.save(notification);
                    }
                }
            });
        }
    }

    private List<String> getAllAccountIds() {
        List<CloudflareAccount> accounts =  accRepository.findAll();
        List<String> accIds =  new ArrayList<>();
        for (CloudflareAccount acc : accounts) {
            accIds.add(acc.getAccountId());
        }
        return accIds;
    }
}
