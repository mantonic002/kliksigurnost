package com.kliksigurnost.demo.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kliksigurnost.demo.model.CloudflareAccount;
import com.kliksigurnost.demo.repository.CloudflareAccountRepository;
import com.kliksigurnost.demo.service.CloudflareService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CloudflareServiceImpl implements CloudflareService {

    @Autowired
    private RestTemplate restTemplate;

    private final String baseUrl = "https://api.cloudflare.com/client/v4/accounts/";

    private final CloudflareAccountRepository repository;

    @Override
    public String createAccount(CloudflareAccount account) {
        var cloudflareAccount = repository.findByAccountId(account.getAccountId());
        // if acc doesnt exist create new
        if(cloudflareAccount.isEmpty())
        {
            String appId = getWarpApplicationId(account);
            if (appId == null)
            {
                appId = createEnrollmentApplication(account);
            }
            account.setEnrollmentApplicationId(appId);

            String policyId = getEnrollmentPolicyId(account);
            if (policyId == null)
            {
                policyId = createEnrollmentPolicy(account);
            }

            account.setEnrollmentPolicyId(policyId);
            account.setUserNum(0);
            repository.save(account);
        }
        return account.getAccountId();
    }

    @Override
    public String getPolicies(CloudflareAccount account) {
        String url = baseUrl + account.getAccountId() + "/gateway/rules";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", account.getAuthorizationToken());
        headers.set("Content-Type", "application/json");

        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            return response.getBody();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public String createPolicy(CloudflareAccount account, String action, String email) {
        String url = baseUrl + account.getAccountId() + "/gateway/rules/fdd21e22-1a0f-4281-8189-3ec664e256f5";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", account.getAuthorizationToken());
        headers.set("Content-Type", "application/json");

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("action", action);
        requestBody.put("name", email);
        requestBody.put("enabled", true);
        requestBody.put("identity", "identity.email == \"" + email + "\"");

        List<String> filters = new ArrayList<>();
        filters.add("dns");
        requestBody.put("filters", filters);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            return response.getBody();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public String createEnrollmentApplication(CloudflareAccount account) {
        String url = baseUrl + account.getAccountId() + "/access/apps";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", account.getAuthorizationToken());
        headers.set("Content-Type", "application/json");

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("name", account.getEmail());
        requestBody.put("type", "warp");
        requestBody.put("session_duration", "24h");

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseBody = objectMapper.readTree(response.getBody());

            return responseBody.path("result").path("id").asText();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public ResponseEntity<String> getApplications(CloudflareAccount account) {
        String url = baseUrl + account.getAccountId() + "/access/apps";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", account.getAuthorizationToken());
        headers.set("Content-Type", "application/json");

        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            return restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public String createEnrollmentPolicy(CloudflareAccount account) {
        String url = baseUrl + account.getAccountId() + "/access/apps/" + account.getEnrollmentApplicationId() + "/policies";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", account.getAuthorizationToken());
        headers.set("Content-Type", "application/json");

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("name", "Allow");
        requestBody.put("decision", "allow");

        List<Map<String, Object>> includeList = new ArrayList<>();
        Map<String, Object> emailFilter = new HashMap<>();
        Map<String, Object> emailObj = new HashMap<>();
        emailObj.put("email", account.getEmail());
        emailFilter.put("email", emailObj);
        includeList.add(emailFilter);
        requestBody.put("include", includeList);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseBody = objectMapper.readTree(response.getBody());

            return responseBody.path("result").path("id").asText();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public String updateEnrollmentPolicyAddEmail(String accountId, String email) {
        var acc = repository.findByAccountId(accountId);
        if (acc.isEmpty()) {
            return null;
        }
        CloudflareAccount account = acc.get();
        String url = baseUrl + account.getAccountId() + "/access/apps/" + account.getEnrollmentApplicationId() + "/policies/" + account.getEnrollmentPolicyId();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", account.getAuthorizationToken());
        headers.set("Content-Type", "application/json");

        // Create the request body to update the policy by adding a new email
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("name", "Allow");
        requestBody.put("decision", "allow");

        // Include the existing email filter and add the new one
        List<Map<String, Object>> includeList = getEnrollmentPolicyEmails(account);

        // Retrieve existing filters from the policy (if needed) or initialize as empty
        Map<String, Object> emailFilter = new HashMap<>();
        Map<String, Object> emailObj = new HashMap<>();
        emailObj.put("email", email);
        emailFilter.put("email", emailObj);
        includeList.add(emailFilter);

        // Add the new include list to the request body
        requestBody.put("include", includeList);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            // Send the PUT request to update the policy
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.PUT, entity, String.class);

            // Return the response body if the update was successful
            return response.getBody();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    public String getWarpApplicationId(CloudflareAccount account) {
        try {
            ResponseEntity<String> response = getApplications(account);

            if (response.getStatusCode() == HttpStatus.OK) {
                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode responseBody = objectMapper.readTree(response.getBody());

                // Iterate through the applications and check if a 'warp' type application exists
                for (JsonNode app : responseBody.path("result")) {
                    if ("warp".equals(app.path("type").asText())) {
                        // Return the ID if a 'warp' application is found
                        return app.path("id").asText();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        // Return null if no 'warp' application is found
        return null;
    }

    public String getEnrollmentPolicyId(CloudflareAccount account) {
        try {
            ResponseEntity<String> response = getApplications(account);

            if (response.getStatusCode() == HttpStatus.OK) {
                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode responseBody = objectMapper.readTree(response.getBody());

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
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return null;
    }

    public List<Map<String, Object>> getEnrollmentPolicyEmails(CloudflareAccount account) {
        String url = baseUrl + account.getAccountId() + "/access/apps/" + account.getEnrollmentApplicationId() + "/policies/" + account.getEnrollmentPolicyId();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", account.getAuthorizationToken());
        headers.set("Content-Type", "application/json");

        // Retrieve the current list of filters (if any)
        List<Map<String, Object>> includeList = new ArrayList<>();

        // Fetch the existing policy to get its current include list
        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
            if (response.getStatusCode() == HttpStatus.OK) {
                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode responseBody = objectMapper.readTree(response.getBody());

                // Retrieve existing "include" filters from the policy
                JsonNode existingInclude = responseBody.path("result").path("include");
                for (JsonNode include : existingInclude) {
                    includeList.add(objectMapper.convertValue(include, Map.class));
                }
                return includeList;
            }
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


}