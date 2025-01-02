package com.kliksigurnost.demo.service;

import com.kliksigurnost.demo.model.CloudflareAccount;
import com.kliksigurnost.demo.model.CloudflarePolicyRequest;
import org.springframework.http.ResponseEntity;

public interface CloudflareService {
    String createAccount(CloudflareAccount account);
    String getPolicies(CloudflareAccount account);
    String createPolicy(CloudflareAccount account, CloudflarePolicyRequest req);
    public String createEnrollmentApplication(CloudflareAccount account);
    ResponseEntity<String> getApplications(CloudflareAccount account);
    public String createEnrollmentPolicy(CloudflareAccount account);

    String updateEnrollmentPolicyAddEmail(String accountId, String email);
}
