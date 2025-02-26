package com.kliksigurnost.demo.service;

import com.kliksigurnost.demo.model.CloudflarePolicy;
import com.kliksigurnost.demo.model.User;
import jakarta.annotation.Nullable;

import java.util.List;

public interface CloudflarePolicyService {
    String createPolicy(CloudflarePolicy req, User user);
    String createPolicy(CloudflarePolicy req);

    void deletePolicy(String policyId);

    void updatePolicy(String policyId, CloudflarePolicy updatedPolicy);

    List<CloudflarePolicy> getPoliciesByUser();
}
