package com.kliksigurnost.demo.service.impl;

import com.kliksigurnost.demo.model.CloudflareAccount;
import com.kliksigurnost.demo.model.CloudflareLog;
import com.kliksigurnost.demo.model.CloudflarePolicy;
import com.kliksigurnost.demo.repository.CloudflareAccountRepository;
import com.kliksigurnost.demo.repository.CloudflarePolicyRepository;
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
    private final CloudflareAccountRepository accRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    @Scheduled(fixedRate = 300000)
    public void checkBlockedContent() {
        log.info("Checking for blocked content...");

        // Calculate start and end time for the last 5 minutes
        Instant endTime = Instant.now();
        Instant startTime = endTime.minus(5, ChronoUnit.DAYS);

        // Retrieve the list of accounts (this should be implemented)
        List<String> accountIds = getAllAccountIds();

        for (String accountId : accountIds) {
            log.info("Fetching logs for account: {}", accountId);

            // Fetch logs for the current account
            List<CloudflareLog> logs = cloudflareLogService.getLogsForAccount(
                    accountId,
                    startTime.toString(),
                    endTime.toString(),
                    List.of("datetime_DESC"),
                    accountId, // Provide the account ID
                    null,
                    1000,
                    "next"
            );

            // Process logs for the current account
            logs.forEach(clog -> {
                if (clog.getResolverDecision() == 9) { // Check if resolverDecision is 9
                    CloudflarePolicy policy = policyRepository.findById(clog.getPolicyId())
                            .orElse(null);

                    if (policy != null) {
                        // Notify the user associated with the policy
                        String notificationMessage = String.format(
                                "Blocked content accessed under policy %s: %s",
                                policy.getName(),
                                clog.getQueryName()
                        );

                        // Send notification via WebSocket
                        log.info("Sending notification to /queue/notifications: {}", notificationMessage);
                        messagingTemplate.convertAndSend("/queue/notifications", notificationMessage);

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
