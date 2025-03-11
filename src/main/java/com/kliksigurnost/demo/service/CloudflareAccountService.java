package com.kliksigurnost.demo.service;

import com.kliksigurnost.demo.model.CloudflareAccount;

public interface CloudflareAccountService {
    String createAccount(CloudflareAccount account);

    String updateEnrollmentPolicyAddEmail(CloudflareAccount account, String email);
}
