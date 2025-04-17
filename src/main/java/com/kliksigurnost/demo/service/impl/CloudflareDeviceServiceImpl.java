package com.kliksigurnost.demo.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.kliksigurnost.demo.helper.MakeApiCall;
import com.kliksigurnost.demo.model.CloudflareDevice;
import com.kliksigurnost.demo.model.User;
import com.kliksigurnost.demo.service.CloudflareDeviceService;
import com.kliksigurnost.demo.service.CloudflareNotificationService;
import com.kliksigurnost.demo.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class CloudflareDeviceServiceImpl implements CloudflareDeviceService {

    private static final String PHYSICAL_DEVICES_ENDPOINT = "accounts/{account_id}/devices/physical-devices";
    private static final String DELETE_DEVICE_ENDPOINT = "accounts/{account_id}/devices/physical-devices/";

    private final MakeApiCall makeApiCall;
    private final UserService userService;
    private final CloudflareNotificationService notificationService;
    private final Environment env;

    @Override
    public List<CloudflareDevice> getDevicesByUser() {
        User user = userService.getCurrentUser();
        String url = makeApiCall.buildUrl(PHYSICAL_DEVICES_ENDPOINT, user.getCloudflareAccount().getAccountId());

        // Add query parameter to filter by user email
        url += "?last_seen_user.email=" + user.getEmail();

        HttpHeaders headers = makeApiCall.createHeaders(user.getCloudflareAccount().getAuthorizationToken());
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = makeApiCall.makeApiCall(url, HttpMethod.GET, entity);
            Map<String, Object> responseMap = makeApiCall.parseResponseToMap(response.getBody());

            List<Map<String, Object>> devices = (List<Map<String, Object>>) responseMap.get("result");
            log.info(devices.toString());

            // Process devices and handle duplicates
            List<CloudflareDevice> devicesList = processDevices(devices, user);

            if (!user.getIsSetUp() && !user.getPolicies().isEmpty() && !devicesList.isEmpty()) {
                user.setIsSetUp(true);
                userService.updateUser(user);
            }

            notificationService.createNotificationForDevices(devicesList, user);

            return devicesList;
        } catch (JsonProcessingException e) {
            log.error("Error parsing Cloudflare API response", e);
            throw new RuntimeException(env.getProperty("cloudflare-api-processing-exception"), e);
        }
    }

    private List<CloudflareDevice> processDevices(List<Map<String, Object>> devices, User user) {
        if (devices == null || devices.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, CloudflareDevice> uniqueDevices = new HashMap<>();

        for (Map<String, Object> device : devices) {
            try {
                CloudflareDevice cfDevice = mapToCloudflareDevice(device);

                if (cfDevice.getSerialNumber() == null || cfDevice.getSerialNumber().isEmpty()) {
                    continue;
                }

                if (uniqueDevices.containsKey(cfDevice.getSerialNumber())) {
                    CloudflareDevice existingDevice = uniqueDevices.get(cfDevice.getSerialNumber());

                    if (isNewerDevice(cfDevice, existingDevice)) {
                        deleteDevice(existingDevice.getId(), user);
                        uniqueDevices.put(cfDevice.getSerialNumber(), cfDevice);
                    } else {
                        deleteDevice(cfDevice.getId(), user);
                    }
                } else {
                    uniqueDevices.put(cfDevice.getSerialNumber(), cfDevice);
                }
            } catch (Exception e) {
                log.error("Error processing device: {}", device, e);
            }
        }

        return new ArrayList<>(uniqueDevices.values());
    }

    private boolean isNewerDevice(CloudflareDevice newDevice, CloudflareDevice existingDevice) {
        try {
            Instant newDeviceTime = parseLastSeen(newDevice.getLastSeenTime());
            Instant existingDeviceTime = parseLastSeen(existingDevice.getLastSeenTime());
            return newDeviceTime.isAfter(existingDeviceTime);
        } catch (Exception e) {
            log.error("Error comparing device timestamps", e);
            return false;
        }
    }

    private Instant parseLastSeen(String lastSeen) {
        if (lastSeen == null || lastSeen.isEmpty()) {
            return Instant.MIN;
        }
        return ZonedDateTime.parse(lastSeen, DateTimeFormatter.ISO_DATE_TIME).toInstant();
    }

    private void deleteDevice(String deviceId, User user) {
        try {
            String url = makeApiCall.buildUrl(DELETE_DEVICE_ENDPOINT,
                    user.getCloudflareAccount().getAccountId()) + deviceId;

            HttpHeaders headers = makeApiCall.createHeaders(user.getCloudflareAccount().getAuthorizationToken());
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = makeApiCall.makeApiCall(url, HttpMethod.DELETE, entity);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Successfully deleted duplicate/older device: {}", deviceId);
            } else {
                log.warn("Failed to delete device: {}. Status: {}", deviceId, response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Error deleting device: {}", deviceId, e);
        }
    }

    private CloudflareDevice mapToCloudflareDevice(Map<String, Object> device) {
        Map<String, Object> userInfo = (Map<String, Object>) device.get("last_seen_user");
        return CloudflareDevice.builder()
                .id((String) device.get("id"))
                .manufacturer((String) device.get("manufacturer"))
                .model((String) device.get("model"))
                .lastSeenTime((String) device.get("last_seen_at"))
                .serialNumber((String) device.get("serial_number"))
                .email(userInfo != null ? (String) userInfo.get("email") : null)
                .build();
    }
}