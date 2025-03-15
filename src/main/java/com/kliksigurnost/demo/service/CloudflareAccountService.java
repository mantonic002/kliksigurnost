package com.kliksigurnost.demo.service;

import com.kliksigurnost.demo.model.CloudflareAccount;

import java.util.List;

public interface CloudflareAccountService {
    String createAccount(CloudflareAccount account);

    List<CloudflareAccount> getAllAccounts();

    String updateEnrollmentPolicyAddEmail(CloudflareAccount account, String email);
}
