package com.kliksigurnost.demo.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kliksigurnost.demo.exception.CloudflareApiException;
import com.kliksigurnost.demo.exception.PolicyNotFoundException;
import com.kliksigurnost.demo.exception.UnauthorizedAccessException;
import com.kliksigurnost.demo.model.CloudflareDevice;
import com.kliksigurnost.demo.model.CloudflareLog;
import com.kliksigurnost.demo.model.CloudflarePolicy;
import com.kliksigurnost.demo.model.User;
import com.kliksigurnost.demo.repository.CloudflarePolicyRepository;
import com.kliksigurnost.demo.service.CloudflareService;
import com.kliksigurnost.demo.service.UserService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CloudflareServiceImpl implements CloudflareService {

    private static final Logger logger = LoggerFactory.getLogger(CloudflareServiceImpl.class);

    private static final String CLOUDFLARE_BASE_URL = "https://api.cloudflare.com/client/v4/";
    private static final String GATEWAY_RULES_ENDPOINT = "accounts/{account_id}/gateway/rules";
    private static final String DEVICES_ENDPOINT = "accounts/{account_id}/devices";
    private static final String GRAPHQL_ENDPOINT = "graphql";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String CONTENT_TYPE_HEADER = "Content-Type";
    private static final String APPLICATION_JSON = "application/json";

    private final RestTemplate restTemplate;
    private final CloudflarePolicyRepository policyRepository;
    private final UserService userService;

    @Override
    public String createPolicy(CloudflarePolicy policy) {
        User user = userService.getCurrentUser();
        policy.setUser(user);

        var account = user.getCloudflareAccount();
        policy.setCloudflareAccId(account.getAccountId());

        String policyName = generatePolicyName(user);
        policy.setName(policyName);

        String url = buildUrl(GATEWAY_RULES_ENDPOINT, account.getAccountId());

        HttpHeaders headers = createHeaders(account.getAuthorizationToken());
        Map<String, Object> requestBody = buildPolicyRequestBody(policy);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = makeApiCall(url, HttpMethod.POST, entity);
            JsonNode responseBody = parseResponse(response.getBody());

            String policyId = responseBody.path("result").path("id").asText();
            policy.setId(policyId);
            policyRepository.save(policy);

            return response.getBody();
        } catch (JsonProcessingException e) {
            logger.error("Error parsing Cloudflare API response", e);
            throw new CloudflareApiException("Error processing Cloudflare API response", e);
        }
    }

    @Override
    public void deletePolicy(String policyId) {
        User user = userService.getCurrentUser();
        CloudflarePolicy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new PolicyNotFoundException("Policy not found"));

        if (!policy.getUser().equals(user)) {
            throw new UnauthorizedAccessException("Unauthorized to delete this policy");
        }

        String accountId = user.getCloudflareAccount().getAccountId();
        String url = buildUrl(GATEWAY_RULES_ENDPOINT, accountId) + "/" + policyId;

        HttpHeaders headers = createHeaders(user.getCloudflareAccount().getAuthorizationToken());
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = makeApiCall(url, HttpMethod.DELETE, entity);
            JsonNode responseBody = parseResponse(response.getBody());

            if (responseBody.path("success").asBoolean()) {
                policyRepository.delete(policy);
            } else {
                logger.error("Failed to delete policy from Cloudflare API: {}", responseBody);
                throw new CloudflareApiException("Failed to delete policy from Cloudflare API");
            }
        } catch (JsonProcessingException e) {
            logger.error("Error parsing Cloudflare API response", e);
            throw new CloudflareApiException("Error processing Cloudflare API response", e);
        }
    }

    @Override
    public void updatePolicy(String policyId, CloudflarePolicy updatedPolicy) {
        User user = userService.getCurrentUser();
        CloudflarePolicy existingPolicy = policyRepository.findById(policyId)
                .orElseThrow(() -> new PolicyNotFoundException("Policy not found"));

        if (!existingPolicy.getUser().equals(user)) {
            throw new UnauthorizedAccessException("Unauthorized to update this policy");
        }

        String accountId = user.getCloudflareAccount().getAccountId();
        String url = buildUrl(GATEWAY_RULES_ENDPOINT, accountId) + "/" + policyId;

        HttpHeaders headers = createHeaders(user.getCloudflareAccount().getAuthorizationToken());
        Map<String, Object> requestBody = buildPolicyRequestBody(updatedPolicy);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = makeApiCall(url, HttpMethod.PUT, entity);
            JsonNode responseBody = parseResponse(response.getBody());

            if (responseBody.path("success").asBoolean()) {
                existingPolicy.setAction(updatedPolicy.getAction());
                existingPolicy.setName(updatedPolicy.getName());
                existingPolicy.setTraffic(updatedPolicy.getTraffic());
                existingPolicy.setSchedule(updatedPolicy.getSchedule());

                policyRepository.save(existingPolicy);
            } else {
                logger.error("Failed to update policy in Cloudflare API: {}", responseBody);
                throw new CloudflareApiException("Failed to update policy in Cloudflare API");
            }
        } catch (JsonProcessingException e) {
            logger.error("Error parsing Cloudflare API response", e);
            throw new CloudflareApiException("Error processing Cloudflare API response", e);
        }
    }

    @Override
    public List<CloudflarePolicy> getPoliciesByUser() {
        return policyRepository.findByUser(userService.getCurrentUser());
    }

    @Override
    public List<CloudflareDevice> getDevicesByUser() {
        User user = userService.getCurrentUser();
        String url = buildUrl(DEVICES_ENDPOINT, user.getCloudflareAccount().getAccountId());

        HttpHeaders headers = createHeaders(user.getCloudflareAccount().getAuthorizationToken());
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = makeApiCall(url, HttpMethod.GET, entity);
            Map<String, Object> responseMap = parseResponseToMap(response.getBody());

            List<Map<String, Object>> devices = (List<Map<String, Object>>) responseMap.get("result");

            return devices.stream()
                    .filter(device -> {
                        Map<String, Object> userInfo = (Map<String, Object>) device.get("user");
                        return userInfo != null && userInfo.get("email").equals(user.getEmail());
                    })
                    .map(this::mapToCloudflareDevice)
                    .collect(Collectors.toList());

        } catch (JsonProcessingException e) {
            logger.error("Error parsing Cloudflare API response", e);
            throw new CloudflareApiException("Error processing Cloudflare API response", e);
        }
    }

    @Override
    public List<CloudflareLog> getLogsForUser(String startDateTime, String endDateTime, List<String> orderBy) {
        User user = userService.getCurrentUser();
        List<String> policyIds = policyRepository.findByUser(user).stream()
                .map(CloudflarePolicy::getId)
                .collect(Collectors.toList());

        String url = CLOUDFLARE_BASE_URL + GRAPHQL_ENDPOINT;
        String query = buildGraphQLQuery();
        Map<String, Object> variables = buildGraphQLVariables(user.getCloudflareAccount().getAccountId(), startDateTime, endDateTime, policyIds, orderBy);

        HttpHeaders headers = createHeaders(user.getCloudflareAccount().getAuthorizationToken());
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(Map.of("query", query, "variables", variables), headers);

        try {
            ResponseEntity<String> response = makeApiCall(url, HttpMethod.POST, entity);
            JsonNode responseBody = parseResponse(response.getBody());

            JsonNode logs = responseBody.path("data").path("viewer").path("accounts").get(0)
                    .path("gatewayResolverQueriesAdaptiveGroups");

            if (logs == null || logs.isMissingNode()) {
                logger.error("No logs found for the given parameters.");
                return Collections.emptyList();
            }

            return mapLogsToCloudflareLogs(logs);
        } catch (JsonProcessingException e) {
            logger.error("Error parsing Cloudflare API response", e);
            throw new CloudflareApiException("Error processing Cloudflare API response", e);
        }
    }

    // Helper Methods
    private String generatePolicyName(User user) {
        String email = user.getEmail();
        String uniqueId = UUID.randomUUID().toString().substring(0, 8); // Short UUID
        return email + "-" + uniqueId;
    }

    private HttpHeaders createHeaders(String authToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(AUTHORIZATION_HEADER, authToken);
        headers.set(CONTENT_TYPE_HEADER, APPLICATION_JSON);
        return headers;
    }

    private Map<String, Object> buildPolicyRequestBody(CloudflarePolicy policy) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("action", policy.getAction());
        requestBody.put("name", policy.getName());
        requestBody.put("enabled", true);
        requestBody.put("identity", "identity.email == \"" + userService.getCurrentUser().getEmail() + "\"");
        requestBody.put("traffic", policy.getTraffic());
        requestBody.put("schedule", policy.getSchedule());

        List<String> filters = new ArrayList<>();
        filters.add("dns");
        requestBody.put("filters", filters);

        return requestBody;
    }

    private String buildUrl(String endpoint, String accountId) {
        return CLOUDFLARE_BASE_URL + endpoint.replace("{account_id}", accountId);
    }

    private <T> ResponseEntity<String> makeApiCall(String url, HttpMethod method, HttpEntity<T> entity) {
        try {
            return restTemplate.exchange(url, method, entity, String.class);
        } catch (RestClientException e) {
            logger.error("Error making REST call to Cloudflare API", e);
            throw new CloudflareApiException("Error contacting Cloudflare API", e);
        }
    }

    private JsonNode parseResponse(String responseBody) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readTree(responseBody);
    }

    private Map<String, Object> parseResponseToMap(String responseBody) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(responseBody, Map.class);
    }

    private CloudflareDevice mapToCloudflareDevice(Map<String, Object> device) {
        Map<String, Object> userInfo = (Map<String, Object>) device.get("user");
        return CloudflareDevice.builder()
                .id((String) device.get("id"))
                .manufacturer((String) device.get("manufacturer"))
                .model((String) device.get("model"))
                .lastSeenTime((String) device.get("last_seen"))
                .email((String) userInfo.get("email"))
                .build();
    }

    private String buildGraphQLQuery() {
        return "query GetRecentQueries($accountId: string!, $datetime_gt: Time!, $datetime_lt: Time, $limit: uint64!, $policyIdsIn: [string!]) {\n" +
                "  viewer {\n" +
                "    accounts(filter: {accountTag: $accountId}) {\n" +
                "      gatewayResolverQueriesAdaptiveGroups(\n" +
                "        filter: {datetime_gt: $datetime_gt, datetime_lt: $datetime_lt, policyId_in: $policyIdsIn}\n" +
                "        limit: $limit\n" +
                "        orderBy: $orderBy\n" +
                "      ) {\n" +
                "        count\n" +
                "        dimensions {\n" +
                "          categoryNames\n" +
                "          datetime\n" +
                "          matchedApplicationName\n" +
                "          policyId\n" +
                "          policyName\n" +
                "          queryName\n" +
                "          resolverDecision\n" +
                "        }\n" +
                "      }\n" +
                "      accountTag\n" +
                "    }\n" +
                "  }\n" +
                "  cost\n" +
                "}";
    }

    private Map<String, Object> buildGraphQLVariables(String accountId, String startDateTime, String endDateTime, List<String> policyIds, List<String> orderBy) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("accountId", accountId);
        variables.put("datetime_gt", startDateTime);
        variables.put("datetime_lt", endDateTime);
        variables.put("limit", 25);
        variables.put("policyIdsIn", policyIds);
        variables.put("orderBy", orderBy);
        return variables;
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
}