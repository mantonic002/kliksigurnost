package com.kliksigurnost.demo.service;

import com.kliksigurnost.demo.model.CloudflareDevice;
import com.kliksigurnost.demo.model.CloudflareLog;
import com.kliksigurnost.demo.model.CloudflarePolicy;

import java.util.List;

public interface CloudflareService {
    String createPolicy(CloudflarePolicy req);

    void deletePolicy(String policyId);

    void updatePolicy(String policyId, CloudflarePolicy updatedPolicy);

    List<CloudflarePolicy> getPoliciesByUser();

    List<CloudflareDevice> getDevicesByUser();

    List<CloudflareLog> getLogsForUser(String startDateTime, String endDateTime, List<String> orderBy);
}
