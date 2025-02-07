package com.kliksigurnost.demo.service;

import com.kliksigurnost.demo.model.CloudflareDevice;
import com.kliksigurnost.demo.model.CloudflarePolicy;

import java.util.List;

public interface CloudflareService {
    String createPolicy(CloudflarePolicy req);
    List<CloudflarePolicy> getPoliciesByUser();

    List<CloudflareDevice> getDevicesByUser();

    String getLogsForUser(String startDateTime, String endDateTime, List<String> orderBy);
}
