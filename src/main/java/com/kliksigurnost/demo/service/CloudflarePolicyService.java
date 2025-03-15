package com.kliksigurnost.demo.service;

import com.kliksigurnost.demo.model.CloudflarePolicy;
import com.kliksigurnost.demo.model.User;

import java.util.List;

public interface CloudflarePolicyService {
    String createPolicy(CloudflarePolicy req, User user);
    String createPolicy(CloudflarePolicy req);

    void deletePolicy(String policyId);

    void updatePolicy(String policyId, CloudflarePolicy updatedPolicy);

    List<CloudflarePolicy> getPoliciesByUser(Integer userId);
    List<CloudflarePolicy> getPoliciesByUser();

    List<CloudflarePolicy> getAllPolicies();

    void createDefaultPolicy(User user);
}
