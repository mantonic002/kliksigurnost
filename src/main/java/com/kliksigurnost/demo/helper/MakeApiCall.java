package com.kliksigurnost.demo.helper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kliksigurnost.demo.exception.CloudflareApiException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class MakeApiCall {
    private static final Logger logger = LoggerFactory.getLogger(MakeApiCall.class);

    private static final String CLOUDFLARE_BASE_URL = "https://api.cloudflare.com/client/v4/";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String CONTENT_TYPE_HEADER = "Content-Type";
    private static final String APPLICATION_JSON = "application/json";

    private final RestTemplate restTemplate;

    public <T> ResponseEntity<String> makeApiCall(String url, HttpMethod method, HttpEntity<T> entity) {
        try {
            return restTemplate.exchange(url, method, entity, String.class);
        } catch (Exception e) {
            logger.error("Error making REST call to Cloudflare API", e);
            throw new CloudflareApiException("Error contacting Cloudflare API", e);
        }
    }

    public JsonNode parseResponse(String responseBody) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readTree(responseBody);
    }

    public Map<String, Object> parseResponseToMap(String responseBody) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(responseBody, Map.class);
    }

    public HttpHeaders createHeaders(String authToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(AUTHORIZATION_HEADER, authToken);
        headers.set(CONTENT_TYPE_HEADER, APPLICATION_JSON);
        return headers;
    }

    public String buildUrl(String endpoint, String accountId) {
        return CLOUDFLARE_BASE_URL + endpoint.replace("{account_id}", accountId);
    }
}
