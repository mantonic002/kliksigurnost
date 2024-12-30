package com.kliksigurnost.demo.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kliksigurnost.demo.model.CloudflareAccount;
import com.kliksigurnost.demo.repository.CloudflareAccountRepository;
import com.kliksigurnost.demo.service.CloudflareService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${cloudflare.account.id}")
    private String accountId;

    @Value("${cloudflare.authorization.token}")
    private String authorizationToken;

    private final String baseUrl = "https://api.cloudflare.com/client/v4/accounts/";

    private final CloudflareAccountRepository repository;

    @Override
    public String createAccount(CloudflareAccount account) {
        var cloudflareAccount = repository.findByAccountId(account.getAccountId());
        if(cloudflareAccount.isEmpty())
        {
            String appId = getWarpApplicationId();
            if (appId == null)
            {
                appId = createEnrollmentApplication(account.getAccountId());
            }
            account.setEnrollmentApplicationId(appId);
            account.setUserNum(0);
            repository.save(account);
        }
        return account.getAccountId();
    }

    @Override
    public String getPolicies() {
        String url = baseUrl + accountId + "/gateway/rules";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", authorizationToken);
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
    public String createPolicy(String action, String email) {
        String url = baseUrl + accountId + "/gateway/rules/fdd21e22-1a0f-4281-8189-3ec664e256f5";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", authorizationToken);
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
    public String createEnrollmentApplication(String name) {
        String url = baseUrl + accountId + "/access/apps";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", authorizationToken);
        headers.set("Content-Type", "application/json");

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("name", name);
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
    public ResponseEntity<String> getApplications() {
        String url = baseUrl + accountId + "/access/apps";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", authorizationToken);
        headers.set("Content-Type", "application/json");

        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            return restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public String getWarpApplicationId() {
        try {
            ResponseEntity<String> response = getApplications();

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

    @Override
    public String createEnrollmentPolicy(String appId, String email) {
        String url = baseUrl + accountId + "/access/apps/" + appId + "/policies";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", authorizationToken);
        headers.set("Content-Type", "application/json");

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("name", "Allow");
        requestBody.put("decision", "allow");

        List<Map<String, Object>> includeList = new ArrayList<>();
        Map<String, Object> emailFilter = new HashMap<>();
        Map<String, Object> emailObj = new HashMap<>();
        emailObj.put("email", email);
        emailFilter.put("email", emailObj);
        includeList.add(emailFilter);
        requestBody.put("include", includeList);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            return response.getBody();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

}