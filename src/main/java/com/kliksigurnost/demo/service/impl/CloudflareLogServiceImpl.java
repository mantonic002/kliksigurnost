package com.kliksigurnost.demo.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kliksigurnost.demo.exception.CloudflareApiException;
import com.kliksigurnost.demo.helper.MakeApiCall;
import com.kliksigurnost.demo.model.CloudflareLog;
import com.kliksigurnost.demo.model.CloudflarePolicy;
import com.kliksigurnost.demo.model.User;
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

    @Override
    public List<CloudflareLog> getLogsForUser(
            String startDateTime,
            String endDateTime,
            List<String> orderBy,
            String lastDateTime,
            String lastPolicyId,
            int pageSize,
            String direction
    ) {
        User user = userService.getCurrentUser();
        List<String> policyIds = policyRepository.findByUser(user).stream()
                .map(CloudflarePolicy::getId)
                .collect(Collectors.toList());

        String url = makeApiCall.buildUrl(GRAPHQL_ENDPOINT, user.getCloudflareAccount().getAccountId());
        String query = buildGraphQLQuery();
        Map<String, Object> variables = buildGraphQLVariables(
                user.getCloudflareAccount().getAccountId(), startDateTime, endDateTime, policyIds,
                orderBy, lastDateTime, lastPolicyId, pageSize, direction
        );
        HttpHeaders headers = makeApiCall.createHeaders(user.getCloudflareAccount().getAuthorizationToken());
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(Map.of("query", query, "variables", variables), headers);

        try {
            ResponseEntity<String> response = makeApiCall.makeApiCall(url, HttpMethod.POST, entity);
            JsonNode responseBody = makeApiCall.parseResponse(response.getBody());

            JsonNode logs = responseBody.path("data").path("viewer").path("accounts").get(0)
                    .path("gatewayResolverQueriesAdaptiveGroups");

            if (logs == null || logs.isMissingNode()) {
                log.error("No logs found for the given parameters.");
                return Collections.emptyList();
            }

            return mapLogsToCloudflareLogs(logs);
        } catch (JsonProcessingException e) {
            log.error("Error parsing Cloudflare API response", e);
            throw new CloudflareApiException("Error processing Cloudflare API response", e);
        }
    }


    private String buildGraphQLQuery() {
        return """
                query GetRecentQueries(
                  $accountId: string!,
                  $datetime_gt: Time!,
                  $datetime_lt: Time,
                  $limit: uint64!,
                  $policyIdsIn: [string!],
                  $orderBy: [string!],
                  $datetime_geq: Time,
                ) {
                  viewer {
                    accounts(filter: {accountTag: $accountId}) {
                      gatewayResolverQueriesAdaptiveGroups(
                        filter: {
                          datetime_gt: $datetime_gt,
                          datetime_lt: $datetime_lt,
                          policyId_in: $policyIdsIn,
                          datetime_geq: $datetime_geq,
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

    private Map<String, Object> buildGraphQLVariables(
            String accountId, String start, String end,
            List<String> policyIds, List<String> orderBy,
            String lastDateTime, String lastPolicyId,
            int limit, String direction
    ) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("accountId", accountId);
        variables.put("datetime_gt", start);
        variables.put("datetime_lt", end);
        variables.put("limit", limit);
        variables.put("policyIdsIn", policyIds);
        variables.put("orderBy", orderBy);

        // Add pagination filters for next page
        if ("next".equals(direction) && lastDateTime != null && lastPolicyId != null) {
            variables.put("datetime_lt", lastDateTime);
        }

        return variables;
    }
}
