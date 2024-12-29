package com.kliksigurnost.demo.service.impl;

import com.kliksigurnost.demo.model.CloudflareAccount;
import com.kliksigurnost.demo.repository.CloudflareAccountRepository;
import com.kliksigurnost.demo.service.CloudflareAccountService;
import com.kliksigurnost.demo.service.CloudflareService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CloudflareAccountServiceImpl implements CloudflareAccountService {

    private final CloudflareService cloudflareService;
    private final CloudflareAccountRepository repository;

    @Override
    public String create(CloudflareAccount account) {
        String appId = cloudflareService.createEnrollmentApplication(account.getAccountId());
        account.setEnrollmentApplicationId(appId);
        repository.save(account);

        return account.getAccountId();
    }
}
