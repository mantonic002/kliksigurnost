package com.kliksigurnost.demo.service;

import com.kliksigurnost.demo.model.CloudflareAccount;
import org.springframework.http.ResponseEntity;


public interface CloudflareAccountService {
    String createAccount(CloudflareAccount account);
    String getPolicies(CloudflareAccount account);


    public String createEnrollmentApplication(CloudflareAccount account);
    ResponseEntity<String> getApplications(CloudflareAccount account);
    public String createEnrollmentPolicy(CloudflareAccount account);

    String updateEnrollmentPolicyAddEmail(String accountId, String email);
}
