package com.kliksigurnost.demo.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CloudflareServiceImpl implements CloudflareService {

    private final RestTemplate restTemplate;

    private final String baseUrl = "https://api.cloudflare.com/client/v4/accounts/";

    private final CloudflarePolicyRepository policyRepository;
    private final UserService userService;

    private static final Logger logger = LoggerFactory.getLogger(CloudflareServiceImpl.class);


    @Override
    public String createPolicy(CloudflarePolicy policy) {
        User user = userService.getCurrentUser();
        policy.setUser(user);

        var account = user.getCloudflareAccount();
        policy.setCloudflareAccId(account.getAccountId());

        String url = baseUrl + account.getAccountId() + "/gateway/rules";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", account.getAuthorizationToken());
        headers.set("Content-Type", "application/json");

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("action", policy.getAction());
        requestBody.put("name", policy.getName());
        requestBody.put("enabled", true);
        requestBody.put("identity", "identity.email == \"" + policy.getUser().getEmail() + "\"");
        requestBody.put("traffic", policy.getTraffic());

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

}
