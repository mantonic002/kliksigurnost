package com.kliksigurnost.demo.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.kliksigurnost.demo.exception.CloudflareApiException;
import com.kliksigurnost.demo.helper.MakeApiCall;
import com.kliksigurnost.demo.model.CloudflareDevice;
import com.kliksigurnost.demo.model.User;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CloudflareDeviceServiceImpl implements CloudflareDeviceService {
    private static final Logger logger = LoggerFactory.getLogger(CloudflareDeviceServiceImpl.class);

    private static final String DEVICES_ENDPOINT = "accounts/{account_id}/devices";

    private final MakeApiCall makeApiCall;
    private final UserService userService;

    @Override
    public List<CloudflareDevice> getDevicesByUser() {
        User user = userService.getCurrentUser();
        String url = makeApiCall.buildUrl(DEVICES_ENDPOINT, user.getCloudflareAccount().getAccountId());

        HttpHeaders headers = makeApiCall.createHeaders(user.getCloudflareAccount().getAuthorizationToken());
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = makeApiCall.makeApiCall(url, HttpMethod.GET, entity);
            Map<String, Object> responseMap = makeApiCall.parseResponseToMap(response.getBody());

            List<Map<String, Object>> devices = (List<Map<String, Object>>) responseMap.get("result");

            return devices.stream()
                    .filter(device -> {
                        Map<String, Object> userInfo = (Map<String, Object>) device.get("user");
                        return userInfo != null && userInfo.get("email").equals(user.getEmail());
                    })
                    .map(this::mapToCloudflareDevice)
                    .collect(Collectors.toList());

        } catch (JsonProcessingException e) {
            logger.error("Error parsing Cloudflare API response", e);
            throw new CloudflareApiException("Error processing Cloudflare API response", e);
        }
    }

    private CloudflareDevice mapToCloudflareDevice(Map<String, Object> device) {
        Map<String, Object> userInfo = (Map<String, Object>) device.get("user");
        return CloudflareDevice.builder()
                .id((String) device.get("id"))
                .manufacturer((String) device.get("manufacturer"))
                .model((String) device.get("model"))
                .lastSeenTime((String) device.get("last_seen"))
                .email((String) userInfo.get("email"))
                .build();
    }
}
