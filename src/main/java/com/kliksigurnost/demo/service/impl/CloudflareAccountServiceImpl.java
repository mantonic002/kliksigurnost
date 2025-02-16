package com.kliksigurnost.demo.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kliksigurnost.demo.model.CloudflareAccount;
import com.kliksigurnost.demo.repository.CloudflareAccountRepository;
import com.kliksigurnost.demo.service.CloudflareAccountService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
@RequiredArgsConstructor
public class CloudflareAccountServiceImpl implements CloudflareAccountService {

    private final RestTemplate restTemplate;
    private final CloudflareAccountRepository repository;
    private static final Logger logger = LoggerFactory.getLogger(CloudflareAccountServiceImpl.class);
    private static final String BASE_URL = "https://api.cloudflare.com/client/v4/accounts/";

    @Override
    public String createAccount(CloudflareAccount account) {
        return repository.findByAccountId(account.getAccountId())
                .orElseGet(() -> createNewAccount(account))
                .getAccountId();
    }

    private CloudflareAccount createNewAccount(CloudflareAccount account) {
        String appId = getOrCreateEnrollmentApplication(account);
        account.setEnrollmentApplicationId(appId);

        String policyId = getOrCreateEnrollmentPolicy(account);
        account.setEnrollmentPolicyId(policyId);
        account.setUserNum(0);
        return repository.save(account);
    }

    public String getPolicies(CloudflareAccount account) {
        String url = BASE_URL + account.getAccountId() + "/gateway/rules";
        HttpEntity<String> entity = new HttpEntity<>(createHeaders(account.getAuthorizationToken()));

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            return response.getBody();
        } catch (RestClientException e) {
            logger.error("Error fetching policies from Cloudflare API", e);
            throw new RuntimeException("Error contacting Cloudflare API", e);
        }
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
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            JsonNode responseBody = new ObjectMapper().readTree(response.getBody());
            return responseBody.path("result").path("id").asText();
        } catch (RestClientException e) {
            logger.error("Error creating enrollment application", e);
            throw new RuntimeException("Error contacting Cloudflare API", e);
        } catch (JsonProcessingException e) {
            logger.error("Error parsing Cloudflare API response", e);
            throw new RuntimeException("Error processing Cloudflare API response", e);
        }
    }

    public ResponseEntity<String> getApplications(CloudflareAccount account) {
        String url = BASE_URL + account.getAccountId() + "/access/apps";
        HttpEntity<String> entity = new HttpEntity<>(createHeaders(account.getAuthorizationToken()));

        try {
            return restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
        } catch (RestClientException e) {
            logger.error("Error fetching applications from Cloudflare API", e);
            throw new RuntimeException("Error contacting Cloudflare API", e);
        }
    }

    public String createEnrollmentPolicy(CloudflareAccount account) {
        String url = BASE_URL + account.getAccountId() + "/access/apps/" + account.getEnrollmentApplicationId() + "/policies";
        HttpHeaders headers = createHeaders(account.getAuthorizationToken());

        Map<String, Object> requestBody = buildPolicyRequestBody(account.getEmail());

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            JsonNode responseBody = new ObjectMapper().readTree(response.getBody());
            return responseBody.path("result").path("id").asText();
        } catch (RestClientException e) {
            logger.error("Error creating enrollment policy", e);
            throw new RuntimeException("Error contacting Cloudflare API", e);
        } catch (JsonProcessingException e) {
            logger.error("Error parsing Cloudflare API response", e);
            throw new RuntimeException("Error processing Cloudflare API response", e);
        }
    }

    @Override
    public String updateEnrollmentPolicyAddEmail(String accountId, String email) {
        return repository.findByAccountId(accountId)
                .map(account -> updatePolicyWithEmail(account, email))
                .orElse(null);
    }

    private String updatePolicyWithEmail(CloudflareAccount account, String email) {
        String url = BASE_URL + account.getAccountId() + "/access/apps/" + account.getEnrollmentApplicationId() + "/policies/" + account.getEnrollmentPolicyId();

        HttpHeaders headers = createHeaders(account.getAuthorizationToken());
        Map<String, Object> requestBody = buildPolicyRequestBody(email);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.PUT, entity, String.class);
            return response.getBody();
        } catch (RestClientException e) {
            logger.error("Error updating Cloudflare policy", e);
            throw new RuntimeException("Error updating Cloudflare policy", e);
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
            logger.error("Error fetching applications from Cloudflare API", e);
            throw new RuntimeException("Error contacting Cloudflare API", e);
        } catch (JsonProcessingException e) {
            logger.error("Error parsing Cloudflare API response", e);
            throw new RuntimeException("Error processing Cloudflare API response", e);
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
            logger.error("Error fetching applications from Cloudflare API", e);
            throw new RuntimeException("Error contacting Cloudflare API", e);
        } catch (JsonProcessingException e) {
            logger.error("Error parsing Cloudflare API response", e);
            throw new RuntimeException("Error processing Cloudflare API response", e);
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
}