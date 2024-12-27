package com.kliksigurnost.demo.service.impl;

import com.kliksigurnost.demo.service.CloudflareService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class CloudflareServiceImpl implements CloudflareService {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${cloudflare.account.id}")
    private String accountId;

    @Value("${cloudflare.authorization.token}")
    private String authorizationToken;

    @Override
    public String getPolicies() {
        String url = "https://api.cloudflare.com/client/v4/accounts/" +
                        accountId + "/gateway/rules";

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
}

