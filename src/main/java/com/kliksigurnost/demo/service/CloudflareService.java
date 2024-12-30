package com.kliksigurnost.demo.service;

import com.kliksigurnost.demo.model.CloudflareAccount;
import org.springframework.http.ResponseEntity;

public interface CloudflareService {
    String createAccount(CloudflareAccount account);
    String getPolicies();
    String createPolicy(String action, String email);
    public String createEnrollmentApplication(String name);
    ResponseEntity<String> getApplications();
    public String createEnrollmentPolicy(String appId, String email);
}
