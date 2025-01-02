package com.kliksigurnost.demo.service;

import com.kliksigurnost.demo.model.CloudflareAccount;
import com.kliksigurnost.demo.model.CloudflarePolicy;
import com.kliksigurnost.demo.model.User;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface CloudflareService {
    String createAccount(CloudflareAccount account);
    String getPolicies(CloudflareAccount account);
    String createPolicy(CloudflarePolicy req);

    List<CloudflarePolicy> getPoliciesByUser();

    public String createEnrollmentApplication(CloudflareAccount account);
    ResponseEntity<String> getApplications(CloudflareAccount account);
    public String createEnrollmentPolicy(CloudflareAccount account);

    String updateEnrollmentPolicyAddEmail(String accountId, String email);
}
