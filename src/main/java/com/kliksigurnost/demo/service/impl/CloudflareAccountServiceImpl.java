package com.kliksigurnost.demo.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kliksigurnost.demo.helper.MakeApiCall;
import com.kliksigurnost.demo.model.CloudflareAccount;
import com.kliksigurnost.demo.repository.CloudflareAccountRepository;
import com.kliksigurnost.demo.service.CloudflareAccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class CloudflareAccountServiceImpl implements CloudflareAccountService {

    private final MakeApiCall makeApiCall;
    private final CloudflareAccountRepository repository;
    private final Environment env;
    private static final String BASE_URL = "https://api.cloudflare.com/client/v4/accounts/";

    @Override
    public String createAccount(CloudflareAccount account) {
        return repository.findByAccountId(account.getAccountId())
                .orElseGet(() -> createNewAccount(account))
                .getAccountId();
    }

    @Override
    public List<CloudflareAccount> getAllAccounts() {
        return repository.findAll();
    }

    private CloudflareAccount createNewAccount(CloudflareAccount account) {
        String appId = getOrCreateEnrollmentApplication(account);
        account.setEnrollmentApplicationId(appId);

        String policyId = getOrCreateEnrollmentPolicy(account);
        account.setEnrollmentPolicyId(policyId);
        account.setUserNum(0);
        return repository.save(account);
    }

    public String createEnrollmentApplication(CloudflareAccount account) {
        String url = BASE_URL + account.getAccountId() + "/access/apps";
        HttpHeaders headers = createHeaders(account.getAuthorizationToken());

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("name", account.getEmail());
        requestBody.put("type", "warp");
        requestBody.put("session_duration", "24h");

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = makeApiCall.makeApiCall(url, HttpMethod.POST, entity);
            JsonNode responseBody = new ObjectMapper().readTree(response.getBody());
            return responseBody.path("result").path("id").asText();
        } catch (RestClientException e) {
            log.error("Error creating enrollment application", e);
            throw new RuntimeException(env.getProperty("cloudflare-api-exception"), e);
        } catch (JsonProcessingException e) {
            log.error("Error parsing Cloudflare API response", e);
            throw new RuntimeException(env.getProperty("cloudflare-api-processing-exception"), e);
        }
    }

    public ResponseEntity<String> getApplications(CloudflareAccount account) {
        String url = BASE_URL + account.getAccountId() + "/access/apps";
        HttpEntity<String> entity = new HttpEntity<>(createHeaders(account.getAuthorizationToken()));

        try {
            return makeApiCall.makeApiCall(url, HttpMethod.GET, entity);
        } catch (RestClientException e) {
            log.error("Error fetching applications from Cloudflare API", e);
            throw new RuntimeException(env.getProperty("cloudflare-api-exception"), e);
        }
    }

    public String createEnrollmentPolicy(CloudflareAccount account) {
        String url = BASE_URL + account.getAccountId() + "/access/apps/" + account.getEnrollmentApplicationId() + "/policies";
        HttpHeaders headers = createHeaders(account.getAuthorizationToken());

        Map<String, Object> requestBody = buildPolicyRequestBody(account.getEmail());

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = makeApiCall.makeApiCall(url, HttpMethod.POST, entity);
            JsonNode responseBody = new ObjectMapper().readTree(response.getBody());
            return responseBody.path("result").path("id").asText();
        } catch (RestClientException e) {
            log.error("Error creating enrollment policy", e);
            throw new RuntimeException(env.getProperty("cloudflare-api-exception"), e);
        } catch (JsonProcessingException e) {
            log.error("Error parsing Cloudflare API response", e);
            throw new RuntimeException(env.getProperty("cloudflare-api-processing-exception"), e);
        }
    }

    @Override
    public String updateEnrollmentPolicyAddEmail(CloudflareAccount acc, String email) {
                String ret = updatePolicyWithEmail(acc, email);
                acc.setUserNum(acc.getUserNum() + 1);
                repository.save(acc);
                return ret;
    }

    private String updatePolicyWithEmail(CloudflareAccount account, String email) {
        String url = BASE_URL + account.getAccountId() + "/access/apps/" +
                account.getEnrollmentApplicationId() + "/policies/" +
                account.getEnrollmentPolicyId();

        // Get current policy to preserve existing emails
        JsonNode currentPolicy = getCurrentPolicy(account);
        JsonNode currentIncludes = currentPolicy.path("result").path("include");

        HttpHeaders headers = createHeaders(account.getAuthorizationToken());

        // Build new request body with existing emails plus new one
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("name", "Allow");
        requestBody.put("decision", "allow");

        List<Map<String, Object>> includeList = new ArrayList<>();

        // Add existing email conditions
        if (currentIncludes.isArray()) {
            for (JsonNode include : currentIncludes) {
                if (include.has("email")) {
                    includeList.add(new HashMap<>(Map.of(
                            "email", Map.of("email", include.path("email").path("email").asText())
                    )));
                }
            }
        }

        // Add new email condition
        includeList.add(Map.of("email", Map.of("email", email)));

        requestBody.put("include", includeList);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = makeApiCall.makeApiCall(url, HttpMethod.PUT, entity);
            return response.getBody();
        } catch (RestClientException e) {
            log.error("Error updating Cloudflare policy", e);
            throw new RuntimeException(env.getProperty("cloudflare-update-policy-exception"), e);
        }
    }

    private String getOrCreateEnrollmentApplication(CloudflareAccount account) {
        return Optional.ofNullable(getWarpApplicationId(account))
                .orElseGet(() -> createEnrollmentApplication(account));
    }

    private String getOrCreateEnrollmentPolicy(CloudflareAccount account) {
        return Optional.ofNullable(getEnrollmentPolicyId(account))
                .orElseGet(() -> createEnrollmentPolicy(account));
    }

    private String getWarpApplicationId(CloudflareAccount account) {
        try {
            ResponseEntity<String> response = getApplications(account);
            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode responseBody = new ObjectMapper().readTree(response.getBody());
                for (JsonNode app : responseBody.path("result")) {
                    if ("warp".equals(app.path("type").asText())) {
                        return app.path("id").asText();
                    }
                }
            }
        } catch (RestClientException e) {
            log.error("Error fetching applications from Cloudflare API", e);
            throw new RuntimeException(env.getProperty("cloudflare-api-exception"), e);
        } catch (JsonProcessingException e) {
            log.error("Error parsing Cloudflare API response", e);
            throw new RuntimeException(env.getProperty("cloudflare-api-processing-exception"), e);
        }
        return null;
    }

    private String getEnrollmentPolicyId(CloudflareAccount account) {
        try {
            ResponseEntity<String> response = getApplications(account);
            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode responseBody = new ObjectMapper().readTree(response.getBody());
                for (JsonNode app : responseBody.path("result")) {
                    if ("warp".equals(app.path("type").asText())) {
                        for (JsonNode policy : app.path("policies")) {
                            if (policy.path("precedence").asInt() == 1) {
                                return policy.path("id").asText();
                            }
                        }
                    }
                }
            }
        } catch (RestClientException e) {
            log.error("Error fetching applications from Cloudflare API", e);
            throw new RuntimeException(env.getProperty("cloudflare-api-exception"), e);
        } catch (JsonProcessingException e) {
            log.error("Error parsing Cloudflare API response", e);
            throw new RuntimeException(env.getProperty("cloudflare-api-processing-exception"), e);
        }
        return null;
    }

    private Map<String, Object> buildPolicyRequestBody(String email) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("name", "Allow");
        requestBody.put("decision", "allow");

        List<Map<String, Object>> includeList = new ArrayList<>();
        Map<String, Object> emailFilter = new HashMap<>();
        emailFilter.put("email", Map.of("email", email));
        includeList.add(emailFilter);

        requestBody.put("include", includeList);
        return requestBody;
    }

    private HttpHeaders createHeaders(String authToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", authToken);
        headers.set("Content-Type", "application/json");
        return headers;
    }

    private JsonNode getCurrentPolicy(CloudflareAccount account) {
        String url = BASE_URL + account.getAccountId() + "/access/apps/" +
                account.getEnrollmentApplicationId() + "/policies/" +
                account.getEnrollmentPolicyId();

        HttpEntity<String> entity = new HttpEntity<>(createHeaders(account.getAuthorizationToken()));

        try {
            ResponseEntity<String> response = makeApiCall.makeApiCall(url, HttpMethod.GET, entity);
            return new ObjectMapper().readTree(response.getBody());
        } catch (RestClientException | JsonProcessingException e) {
            log.error("Error fetching current policy", e);
            throw new RuntimeException("Failed to fetch current policy", e);
        }
    }
}