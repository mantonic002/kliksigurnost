package com.kliksigurnost.demo.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.kliksigurnost.demo.exception.CloudflareApiException;
import com.kliksigurnost.demo.exception.LimitReached;
import com.kliksigurnost.demo.exception.NotFoundException;
import com.kliksigurnost.demo.exception.UnauthorizedAccessException;
import com.kliksigurnost.demo.helper.MakeApiCall;
import com.kliksigurnost.demo.model.CloudflarePolicy;
import com.kliksigurnost.demo.model.User;
import com.kliksigurnost.demo.repository.CloudflarePolicyRepository;
import com.kliksigurnost.demo.service.CloudflarePolicyService;
import com.kliksigurnost.demo.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
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
public class CloudflarePolicyServiceImpl implements CloudflarePolicyService {
    private static final String GATEWAY_RULES_ENDPOINT = "accounts/{account_id}/gateway/rules";

    private final MakeApiCall makeApiCall;

    private final CloudflarePolicyRepository policyRepository;
    private final UserService userService;

    private final Environment env;

    @Override
    public String createPolicy(CloudflarePolicy policy) {
        User currentUser = userService.getCurrentUser();
        if (policyRepository.countByUser(currentUser) >= 10) {
            throw new LimitReached(env.getProperty("policy-limit-exceeded"));
        }
        return createPolicy(policy, currentUser);
    }

    @Override
    public String createPolicy(CloudflarePolicy policy, User user) {
        policy.setUser(user);

        var account = user.getCloudflareAccount();
        policy.setCloudflareAccId(account.getAccountId());

        String policyName = generatePolicyName(user);
        policy.setName(policyName);

        String url = makeApiCall.buildUrl(GATEWAY_RULES_ENDPOINT, account.getAccountId());

        HttpHeaders headers = makeApiCall.createHeaders(account.getAuthorizationToken());
        Map<String, Object> requestBody = buildPolicyRequestBody(policy);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = makeApiCall.makeApiCall(url, HttpMethod.POST, entity);
            JsonNode responseBody = makeApiCall.parseResponse(response.getBody());

            String policyId = responseBody.path("result").path("id").asText();
            policy.setId(policyId);
            policyRepository.save(policy);

            updateAllowAllPolicy(user);

            return response.getBody();
        } catch (JsonProcessingException e) {
            log.error("Error parsing Cloudflare API response", e);
            throw new RuntimeException(env.getProperty("cloudflare-api-processing-exception"), e);
        }
    }

    @Override
    public void deletePolicy(String policyId) {
        User user = userService.getCurrentUser();
        CloudflarePolicy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new NotFoundException(env.getProperty("policy-not-found")));

        if (!policy.getUser().equals(user)) {
            throw new UnauthorizedAccessException(env.getProperty("policy-unauthorized-delete"));
        }

        String accountId = user.getCloudflareAccount().getAccountId();
        String url = makeApiCall.buildUrl(GATEWAY_RULES_ENDPOINT, accountId) + "/" + policyId;

        HttpHeaders headers = makeApiCall.createHeaders(user.getCloudflareAccount().getAuthorizationToken());
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = makeApiCall.makeApiCall(url, HttpMethod.DELETE, entity);
            JsonNode responseBody = makeApiCall.parseResponse(response.getBody());

            if (responseBody.path("success").asBoolean()) {
                policyRepository.delete(policy);

                // Update the "allow-all" policy
                updateAllowAllPolicy(user);
            } else {
                log.error("Failed to delete policy from Cloudflare API: {}", responseBody);
                throw new CloudflareApiException(env.getProperty("policy-delete-fail"));
            }
        } catch (JsonProcessingException e) {
            log.error("Error parsing Cloudflare API response", e);
            throw new RuntimeException(env.getProperty("cloudflare-api-processing-exception"), e);
        }
    }

    @Override
    public void updatePolicy(String policyId, CloudflarePolicy updatedPolicy) {
        User user = userService.getCurrentUser();
        CloudflarePolicy existingPolicy = policyRepository.findById(policyId)
                .orElseThrow(() -> new NotFoundException(env.getProperty("policy-not-found")));

        if (!existingPolicy.getUser().equals(user)) {
            throw new UnauthorizedAccessException(env.getProperty("policy-unauthorized-update"));
        }

        String accountId = user.getCloudflareAccount().getAccountId();
        String url = makeApiCall.buildUrl(GATEWAY_RULES_ENDPOINT, accountId) + "/" + policyId;

        HttpHeaders headers = makeApiCall.createHeaders(user.getCloudflareAccount().getAuthorizationToken());
        Map<String, Object> requestBody = buildPolicyRequestBody(updatedPolicy);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = makeApiCall.makeApiCall(url, HttpMethod.PUT, entity);
            JsonNode responseBody = makeApiCall.parseResponse(response.getBody());

            if (responseBody.path("success").asBoolean()) {
                existingPolicy.setAction(updatedPolicy.getAction());
                existingPolicy.setName(updatedPolicy.getName());
                existingPolicy.setTraffic(updatedPolicy.getTraffic());
                existingPolicy.setSchedule(updatedPolicy.getSchedule());

                policyRepository.save(existingPolicy);

                // Update the "allow-all" policy
                updateAllowAllPolicy(user);
            } else {
                log.error("Failed to update policy in Cloudflare API: {}", responseBody);
                throw new CloudflareApiException(env.getProperty("policy-update-fail"));
            }
        } catch (JsonProcessingException e) {
            log.error("Error parsing Cloudflare API response", e);
            throw new RuntimeException(env.getProperty("cloudflare-api-processing-exception"), e);
        }
    }

    @Override
    public List<CloudflarePolicy> getPoliciesByUser(Integer userId) {
        User user = userService.getById(userId);
        return policyRepository.findByUser(user);
    }

    @Override
    public List<CloudflarePolicy> getPoliciesByUser() {
        return policyRepository.findByUser(userService.getCurrentUser());
    }

    @Override
    public List<CloudflarePolicy> getAllPolicies() {
        return policyRepository.findAll();
    }

    @Override
    public String createDefaultPolicy(User user) {
        log.debug("Creating default policy for user: {}", user);
        String defaultTrafficString = "any(dns.content_category[*] in {2 67 125 133 8 99})"; //adult themes and gambling
        CloudflarePolicy policy = CloudflarePolicy.builder()
                .action("block")
                .schedule(null)
                .traffic(defaultTrafficString)
                .build();
        return createPolicy(policy, user);
    }

    // Helper Methods
    private String generatePolicyName(User user) {
        String email = user.getEmail();
        String uniqueId = UUID.randomUUID().toString().substring(0, 8); // Short UUID
        return email + "-" + uniqueId;
    }

    private Map<String, Object> buildPolicyRequestBody(CloudflarePolicy policy) {
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

        return requestBody;
    }


    private CloudflarePolicy ensureAllowAllPolicyExists(User user) {
        // First check if we already have it in our database
        Optional<CloudflarePolicy> existingDbPolicy = policyRepository.findByUser(user).stream()
                .filter(CloudflarePolicy::isAllowAll)
                .findFirst();

        if (existingDbPolicy.isPresent()) {
            return existingDbPolicy.get();
        }

        // If not in DB, check Cloudflare API for existing policy
        String url = makeApiCall.buildUrl(GATEWAY_RULES_ENDPOINT, user.getCloudflareAccount().getAccountId());
        HttpHeaders headers = makeApiCall.createHeaders(user.getCloudflareAccount().getAuthorizationToken());
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            // Get all policies from Cloudflare
            ResponseEntity<String> response = makeApiCall.makeApiCall(url, HttpMethod.GET, entity);
            JsonNode responseBody = makeApiCall.parseResponse(response.getBody());

            // Look for policy with user's email as name
            for (JsonNode policyNode : responseBody.path("result")) {
                String policyName = policyNode.path("name").asText();
                if (policyName.equals(user.getEmail())) {
                    // Found existing policy - create local record
                    CloudflarePolicy existingPolicy = CloudflarePolicy.builder()
                            .id(policyNode.path("id").asText())
                            .name(policyName)
                            .action(policyNode.path("action").asText())
                            .traffic(policyNode.path("traffic").asText())
                            .cloudflareAccId(user.getCloudflareAccount().getAccountId())
                            .user(user)
                            .isAllowAll(true)
                            .build();

                    return policyRepository.save(existingPolicy);
                }
            }

            // If we get here, no existing policy found - create new one
            CloudflarePolicy newPolicy = CloudflarePolicy.builder()
                    .name(user.getEmail())
                    .action("allow")
                    .traffic("")
                    .cloudflareAccId(user.getCloudflareAccount().getAccountId())
                    .user(user)
                    .isAllowAll(true)
                    .build();

            Map<String, Object> requestBody = buildPolicyRequestBody(newPolicy);
            HttpEntity<Map<String, Object>> postEntity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> createResponse = makeApiCall.makeApiCall(url, HttpMethod.POST, postEntity);
            JsonNode createResponseBody = makeApiCall.parseResponse(createResponse.getBody());

            String policyId = createResponseBody.path("result").path("id").asText();
            newPolicy.setId(policyId);

            return policyRepository.save(newPolicy);

        } catch (JsonProcessingException e) {
            log.error("Error parsing Cloudflare API response", e);
            throw new RuntimeException(env.getProperty("cloudflare-api-processing-exception"), e);
        }
    }

    private void updateAllowAllPolicy(User user) {
        List<CloudflarePolicy> userPolicies = policyRepository.findByUser(user);
        CloudflarePolicy allowAllPolicy = ensureAllowAllPolicyExists(user);

        // Build the "allow-all" traffic string
        String allowAllTraffic = buildAllowAllTrafficString(userPolicies);
        allowAllPolicy.setTraffic(allowAllTraffic);

        // Update the "allow-all" policy in the database
        policyRepository.save(allowAllPolicy);

        // Update the "allow-all" policy in Cloudflare API
        String url = makeApiCall.buildUrl(GATEWAY_RULES_ENDPOINT, user.getCloudflareAccount().getAccountId()) + "/" + allowAllPolicy.getId();

        HttpHeaders headers = makeApiCall.createHeaders(user.getCloudflareAccount().getAuthorizationToken());
        Map<String, Object> requestBody = buildPolicyRequestBody(allowAllPolicy);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = makeApiCall.makeApiCall(url, HttpMethod.PUT, entity);
            JsonNode responseBody = makeApiCall.parseResponse(response.getBody());

            if (!responseBody.path("success").asBoolean()) {
                log.error("Failed to update allow-all policy in Cloudflare API: {}", responseBody);
                throw new CloudflareApiException(env.getProperty("policy-allow-update-fail"));
            }
        } catch (JsonProcessingException e) {
            log.error("Error parsing Cloudflare API response", e);
            throw new RuntimeException(env.getProperty("cloudflare-api-processing-exception"), e);
        }
    }

    private String buildAllowAllTrafficString(List<CloudflarePolicy> userPolicies) {
        Set<Integer> blockedCategories = new HashSet<>();
        Set<Integer> blockedAppTypes = new HashSet<>();
        Set<Integer> blockedAppIds = new HashSet<>();

        // Extract blocked categories, app types, and app IDs from user policies
        for (CloudflarePolicy policy : userPolicies) {
            if (!policy.isAllowAll()) { // Skip the "allow-all" policy itself
                log.debug("Processing policy traffic: {}", policy.getTraffic());
                extractBlockedCategories(policy.getTraffic(), blockedCategories);
                extractBlockedAppTypes(policy.getTraffic(), blockedAppTypes);
                extractBlockedAppIds(policy.getTraffic(), blockedAppIds);
            }
        }

        log.debug("Blocked categories: {}", blockedCategories);
        log.debug("Blocked app types: {}", blockedAppTypes);
        log.debug("Blocked app IDs: {}", blockedAppIds);

        // Build the "allow-all" traffic string
        StringBuilder trafficBuilder = new StringBuilder();
        if (!blockedCategories.isEmpty()) {
            trafficBuilder.append("any(dns.content_category[*] in {")
                    .append(blockedCategories.stream().map(String::valueOf).collect(Collectors.joining(" ")))
                    .append("})");
        }
        if (!blockedAppTypes.isEmpty()) {
            if (!trafficBuilder.isEmpty()) trafficBuilder.append(" or ");
            trafficBuilder.append("any(app.type.ids[*] in {")
                    .append(blockedAppTypes.stream().map(String::valueOf).collect(Collectors.joining(" ")))
                    .append("})");
        }
        if (!blockedAppIds.isEmpty()) {
            if (!trafficBuilder.isEmpty()) trafficBuilder.append(" or ");
            trafficBuilder.append("any(app.ids[*] in {")
                    .append(blockedAppIds.stream().map(String::valueOf).collect(Collectors.joining(" ")))
                    .append("})");
        }

        String trafficString = trafficBuilder.toString();
        log.debug("Generated allow-all traffic string: {}", trafficString);

        if (trafficString.isEmpty()) return "";
        return "not(" + trafficString + ")";
    }

    private void extractBlockedCategories(String traffic, Set<Integer> blockedCategories) {
        String categoryPattern = "dns\\.content_category\\[\\*] in \\{([^}]+)}";
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(categoryPattern);
        java.util.regex.Matcher matcher = pattern.matcher(traffic);
        if (matcher.find()) {
            String[] categories = matcher.group(1).split(" ");
            for (String category : categories) {
                blockedCategories.add(Integer.parseInt(category));
            }
        } else {
            log.debug("No categories found in traffic: {}", traffic);
        }
    }

    private void extractBlockedAppTypes(String traffic, Set<Integer> blockedAppTypes) {
        String appTypePattern = "app\\.type\\.ids\\[\\*] in \\{([^}]+)}";
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(appTypePattern);
        java.util.regex.Matcher matcher = pattern.matcher(traffic);
        if (matcher.find()) {
            String[] appTypes = matcher.group(1).split(" ");
            for (String appType : appTypes) {
                blockedAppTypes.add(Integer.parseInt(appType));
            }
        } else {
            log.debug("No app types found in traffic: {}", traffic);
        }
    }

    private void extractBlockedAppIds(String traffic, Set<Integer> blockedAppIds) {
        String appIdPattern = "app\\.ids\\[\\*] in \\{([^}]+)}";
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(appIdPattern);
        java.util.regex.Matcher matcher = pattern.matcher(traffic);
        if (matcher.find()) {
            String[] appIds = matcher.group(1).split(" ");
            for (String appId : appIds) {
                blockedAppIds.add(Integer.parseInt(appId));
            }
        } else {
            log.debug("No app IDs found in traffic: {}", traffic);
        }
    }
}