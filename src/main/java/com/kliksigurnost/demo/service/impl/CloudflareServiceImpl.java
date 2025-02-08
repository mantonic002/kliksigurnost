package com.kliksigurnost.demo.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CloudflareServiceImpl implements CloudflareService {

    private final RestTemplate restTemplate;

    private final String baseUrl = "https://api.cloudflare.com/client/v4/";

    private final CloudflarePolicyRepository policyRepository;
    private final UserService userService;

    private static final Logger logger = LoggerFactory.getLogger(CloudflareServiceImpl.class);


    @Override
    public String createPolicy(CloudflarePolicy policy) {
        User user = userService.getCurrentUser();
        policy.setUser(user);

        var account = user.getCloudflareAccount();
        policy.setCloudflareAccId(account.getAccountId());

        String url = baseUrl + account.getAccountId() + "accounts/gateway/rules";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", account.getAuthorizationToken());
        headers.set("Content-Type", "application/json");

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("action", policy.getAction());
        requestBody.put("name", policy.getName());
        requestBody.put("enabled", true);
        requestBody.put("identity", "identity.email == \"" + policy.getUser().getEmail() + "\"");
        requestBody.put("traffic", policy.getTraffic());
        requestBody.put("schedule", policy.getSchedule());

        List<String> filters = new ArrayList<>();
        filters.add("dns");
        requestBody.put("filters", filters);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseBody = objectMapper.readTree(response.getBody());

            String id =  responseBody.path("result").path("id").asText();

            policy.setId(id);
            policyRepository.save(policy);

            return response.getBody();
        } catch (RestClientException e) {
            logger.error("Error making REST call to Cloudflare API", e);
            throw new RuntimeException ("Error contacting Cloudflare API", e);
        } catch (JsonProcessingException e) {
            logger.error("Error parsing Cloudflare API response", e);
            throw new RuntimeException ("Error processing Cloudflare API response", e);
        } catch (Exception e) {
            logger.error("Unexpected error occurred", e);
            throw new RuntimeException ("Unexpected error occurred", e);
        }
    }

    @Override
    public List<CloudflarePolicy> getPoliciesByUser() {
        return policyRepository.findByUser(userService.getCurrentUser());
    }

    @Override
    public List<CloudflareDevice> getDevicesByUser() {
        User user = userService.getCurrentUser();
        String url = baseUrl + "accounts/" + user.getCloudflareAccount().getAccountId() + "/devices";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", user.getCloudflareAccount().getAuthorizationToken());
        headers.set("Content-Type", "application/json");

        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, Object> responseMap = objectMapper.readValue(response.getBody(), Map.class);

            List<Map<String, Object>> devices = (List<Map<String, Object>>) responseMap.get("result");

            // Filter the devices by matching user email and map them to CloudflareDevice objects
            List<CloudflareDevice> deviceList = devices.stream()
                    .filter(device -> {
                        Map<String, Object> userInfo = (Map<String, Object>) device.get("user");
                        return userInfo != null && userInfo.get("email").equals(user.getEmail());
                    })
                    .map(device -> {
                        return CloudflareDevice.builder()
                                .id((String) device.get("id"))
                                .manufacturer((String) device.get("manufacturer"))
                                .model((String) device.get("model"))
                                .lastSeenTime((String) device.get("last_seen"))
                                .email((String) ((Map<String, Object>) device.get("user")).get("email"))
                                .build();
                    })
                    .collect(Collectors.toList());

            // If user has devices connected and has at least one policy his acc is set up
            if (!deviceList.isEmpty() && !getPoliciesByUser().isEmpty()) {
                user.setIsSetUp(true);
                userService.updateUser(user);
            }

            return deviceList;

        } catch (RestClientException e) {
            logger.error("Error making REST call to Cloudflare API", e);
            throw new RuntimeException("Error contacting Cloudflare API", e);
        } catch (Exception e) {
            logger.error("Unexpected error occurred", e);
            throw new RuntimeException("Unexpected error occurred", e);
        }
    }

    @Override
    public List<CloudflareLog> getLogsForUser(String startDateTime, String endDateTime, List<String> orderBy) {
        User user = userService.getCurrentUser();

        // Collect policy IDs
        List<String> policyIds = new ArrayList<>();
        for (CloudflarePolicy policy : policyRepository.findByUser(user)) {
            policyIds.add(policy.getId());
        }

        String accountId = user.getCloudflareAccount().getAccountId();
        String authToken = user.getCloudflareAccount().getAuthorizationToken();

        String url = baseUrl + "graphql";

        // Construct GraphQL query
        String query = "query GetRecentQueries($accountId: string!, $datetime_gt: Time!, $datetime_lt: Time, $limit: uint64!, $policyIdsIn: [string!]) {\n" +
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

        // Construct the variables for the GraphQL query
        Map<String, Object> variables = new HashMap<>();
        variables.put("accountId", accountId);
        variables.put("datetime_gt", startDateTime);
        variables.put("datetime_lt", endDateTime);
        variables.put("limit", 25);
        variables.put("policyIdsIn", policyIds);  // Add the policyIds filter here
        List<String> order = new ArrayList<>();
        order.add("datetime_DESC");
        variables.put("orderBy", order);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", authToken);
        headers.set("Content-Type", "application/json");

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("query", query);
        requestBody.put("variables", variables);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseBody = objectMapper.readTree(response.getBody());

            // Handle and process the response as per your requirement
            JsonNode logs = responseBody.path("data").path("viewer").path("accounts").get(0)
                    .path("gatewayResolverQueriesAdaptiveGroups");

            // Map the JSON response to a list of CloudflareLog objects
            List<CloudflareLog> cloudflareLogs = new ArrayList<>();
            for (JsonNode logNode : logs) {
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
            }

            return cloudflareLogs;

        } catch (RestClientException e) {
            logger.error("Error making REST call to Cloudflare API", e);
            throw new RuntimeException("Error contacting Cloudflare API", e);
        } catch (JsonProcessingException e) {
            logger.error("Error parsing Cloudflare API response", e);
            throw new RuntimeException("Error processing Cloudflare API response", e);
        } catch (Exception e) {
            logger.error("Unexpected error occurred", e);
            throw new RuntimeException("Unexpected error occurred", e);
        }
    }


}
