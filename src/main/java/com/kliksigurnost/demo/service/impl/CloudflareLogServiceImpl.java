package com.kliksigurnost.demo.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kliksigurnost.demo.exception.CloudflareApiException;
import com.kliksigurnost.demo.helper.MakeApiCall;
import com.kliksigurnost.demo.model.CloudflareAccount;
import com.kliksigurnost.demo.model.CloudflareLog;
import com.kliksigurnost.demo.model.CloudflarePolicy;
import com.kliksigurnost.demo.model.User;
import com.kliksigurnost.demo.repository.CloudflareAccountRepository;
import com.kliksigurnost.demo.repository.CloudflarePolicyRepository;
import com.kliksigurnost.demo.service.CloudflareLogService;
import com.kliksigurnost.demo.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CloudflareLogServiceImpl implements CloudflareLogService {
    private static final String GRAPHQL_ENDPOINT = "graphql";

    private final MakeApiCall makeApiCall;

    private final CloudflarePolicyRepository policyRepository;
    private final UserService userService;
    private final CloudflareAccountRepository accountRepository;

    @Override
    public List<CloudflareLog> getLogsForUser(
            String startDateTime,
            String endDateTime,
            List<String> orderBy,
            String lastDateTime,
            String lastPolicyId,
            int pageSize,
            int resolverDecision
    ) {
        User user = userService.getCurrentUser();
        List<String> policyIds = policyRepository.findByUser(user).stream()
                .map(CloudflarePolicy::getId)
                .collect(Collectors.toList());

        String url = makeApiCall.buildUrl(GRAPHQL_ENDPOINT, user.getCloudflareAccount().getAccountId());
        String query = buildGraphQLQuery();
        Map<String, Object> variables = buildGraphQLVariables(
                user.getCloudflareAccount().getAccountId(), startDateTime, endDateTime, policyIds,
                orderBy, lastDateTime, lastPolicyId, pageSize, resolverDecision
        );
        HttpHeaders headers = makeApiCall.createHeaders(user.getCloudflareAccount().getAuthorizationToken());
        return getCloudflareLogs(url, query, variables, headers);
    }

    @Override
    public List<CloudflareLog> getLogsForAccount(
            String accountId,
            String startDateTime,
            String endDateTime,
            List<String> orderBy,
            String lastDateTime,
            String lastPolicyId,
            int pageSize,
            int resolverDecision
    ) {
        String url = makeApiCall.buildUrl(GRAPHQL_ENDPOINT, accountId);
        String query = buildGraphQLQuery();
        Map<String, Object> variables = buildGraphQLVariables(
                accountId, startDateTime, endDateTime,
                Collections.emptyList(), orderBy, lastDateTime, lastPolicyId, pageSize, resolverDecision
        );

        CloudflareAccount acc = accountRepository.findById(accountId).orElseThrow();
        HttpHeaders headers = makeApiCall.createHeaders(acc.getAuthorizationToken());
        return getCloudflareLogs(url, query, variables, headers);
    }

    private List<CloudflareLog> getCloudflareLogs(String url, String query, Map<String, Object> variables, HttpHeaders headers) {
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(Map.of("query", query, "variables", variables), headers);

        try {
            ResponseEntity<String> response = makeApiCall.makeApiCall(url, HttpMethod.POST, entity);
            JsonNode responseBody = makeApiCall.parseResponse(response.getBody());

            JsonNode logs = responseBody.path("data").path("viewer").path("accounts").get(0)
                    .path("gatewayResolverQueriesAdaptiveGroups");

            return mapLogsToCloudflareLogs(logs);
        } catch (JsonProcessingException e) {
            log.error("Error parsing Cloudflare API response", e);
            throw new CloudflareApiException("Error processing Cloudflare API response", e);
        }
    }

    private List<CloudflareLog> mapLogsToCloudflareLogs(JsonNode logs) {
        ObjectMapper objectMapper = new ObjectMapper();
        List<CloudflareLog> cloudflareLogs = new ArrayList<>();

        logs.forEach(logNode -> {
            CloudflareLog cloudflareLog = CloudflareLog.builder()
                    .categoryNames(objectMapper.convertValue(logNode.path("dimensions").path("categoryNames"), String[].class))
                    .datetime(logNode.path("dimensions").path("datetime").asText())
                    .matchedApplicationName(logNode.path("dimensions").path("matchedApplicationName").asText())
                    .policyId(logNode.path("dimensions").path("policyId").asText())
                    .policyName(logNode.path("dimensions").path("policyName").asText())
                    .queryName(logNode.path("dimensions").path("queryName").asText())
                    .resolverDecision(logNode.path("dimensions").path("resolverDecision").asInt())
                    .build();

            cloudflareLogs.add(cloudflareLog);
        });

        return cloudflareLogs;
    }

    private String buildGraphQLQuery() {
        return """
            query GetRecentQueries(
              $accountId: string!,
              $datetime_gt: Time!,
              $datetime_lt: Time,
              $limit: uint64!,
              $policyIdsIn: [string],
              $orderBy: [string!],
              $datetime_geq: Time,
              $resolverDecision: uint64
            ) {
              viewer {
                accounts(filter: {accountTag: $accountId}) {
                  gatewayResolverQueriesAdaptiveGroups(
                    filter: {
                      datetime_gt: $datetime_gt,
                      datetime_lt: $datetime_lt,
                      policyId_in: $policyIdsIn,
                      datetime_geq: $datetime_geq,
                      resolverDecision: $resolverDecision
                    }
                    limit: $limit
                    orderBy: $orderBy
                  ) {
                    count
                    dimensions {
                      categoryNames
                      datetime
                      matchedApplicationName
                      policyId
                      policyName
                      queryName
                      resolverDecision
                    }
                  }
                }
              }
            }""";
    }

    private Map<String, Object> buildGraphQLVariables(
            String accountId, String start, String end,
            List<String> policyIds, List<String> orderBy,
            String lastDateTime, String lastPolicyId,
            int limit, int resolverDecision
    ) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("accountId", accountId);
        variables.put("datetime_gt", start);
        variables.put("datetime_lt", end);
        variables.put("limit", limit);
        if (!policyIds.isEmpty()) {
            variables.put("policyIdsIn", policyIds);
        }
        variables.put("orderBy", orderBy);
        if (resolverDecision != 0) {
            variables.put("resolverDecision", resolverDecision);
        }
        // Handle pagination based on direction
        if (lastDateTime != null && lastPolicyId != null) {
            // Fetch logs after the last log of the current page
            variables.put("datetime_lt", lastDateTime);
        }

        return variables;
    }
}
