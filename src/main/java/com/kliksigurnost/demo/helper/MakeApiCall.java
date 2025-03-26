package com.kliksigurnost.demo.helper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kliksigurnost.demo.exception.CloudflareApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.env.Environment;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Log4j2
@Component
@RequiredArgsConstructor
public class MakeApiCall {

    private static final String CLOUDFLARE_BASE_URL = "https://api.cloudflare.com/client/v4/";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String CONTENT_TYPE_HEADER = "Content-Type";
    private static final String APPLICATION_JSON = "application/json";

    private final Environment env;

    private final RestTemplate restTemplate;

    public <T> ResponseEntity<String> makeApiCall(String url, HttpMethod method, HttpEntity<T> entity) {
        try {
            ResponseEntity<String> response = restTemplate.exchange(url, method, entity, String.class);
            JsonNode responseBody = parseResponse(response.getBody());

            if (responseBody.path("errors").asBoolean()) {
                String errorMessage = extractCloudflareErrorMessage(responseBody);
                throw new CloudflareApiException(errorMessage, response.getStatusCode());
            }

            return response;
        } catch (JsonProcessingException e) {
            log.error("Error parsing Cloudflare response", e);
            throw new CloudflareApiException(env.getProperty("cloudflare-api-processing-exception"), e);
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            // Handle HTTP errors
            String errorDetails = e.getResponseBodyAsString();
            throw new CloudflareApiException(
                    env.getProperty("cloudflare-api-exception") + e.getStatusCode() + " - " + errorDetails,
                    e.getStatusCode()
            );
        } catch (Exception e) {
            log.error("Error making REST call to Cloudflare API", e);
            throw new CloudflareApiException(env.getProperty("cloudflare-api-exception") + e.getMessage());
        }
    }

    private String extractCloudflareErrorMessage(JsonNode responseBody) {
        JsonNode errors = responseBody.path("errors");
        if (errors.isArray() && !errors.isEmpty()) {
            return errors.get(0).path("message").asText("Unknown Cloudflare error");
        }
        return responseBody.toString();
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
