package com.kliksigurnost.demo.service.impl;

import com.kliksigurnost.demo.service.CloudflareService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

@Service
public class CloudflareServiceImpl implements CloudflareService {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${cloudflare.account.id}")
    private String accountId;

    @Value("${cloudflare.authorization.token}")
    private String authorizationToken;

    private String baseUrl = "https://api.cloudflare.com/client/v4/accounts/";

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
}