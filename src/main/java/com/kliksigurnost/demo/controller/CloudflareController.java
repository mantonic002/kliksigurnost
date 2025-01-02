package com.kliksigurnost.demo.controller;

import com.kliksigurnost.demo.model.CloudflareAccount;
import com.kliksigurnost.demo.model.CloudflarePolicyRequest;
import com.kliksigurnost.demo.repository.CloudflareAccountRepository;
import com.kliksigurnost.demo.service.CloudflareService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class CloudflareController {
    private final CloudflareService cloudflareService;
    private final CloudflareAccountRepository repository;

    @PostMapping("/setupAccount")
    public String setupAccount(@RequestBody CloudflareAccount account) {
        return cloudflareService.createAccount(account);
    }

    @GetMapping("/getPolicies")
    public String getPolicies(@RequestParam String id) {
        var acc = repository.findByAccountId(id);
        if (acc.isEmpty()) {
            return "Account not found";
        }
        return cloudflareService.getPolicies(acc.get());
    }

    @PostMapping("/createPolicy")
    public String createPolicy(@RequestBody CloudflarePolicyRequest request)
    {
        var acc = repository.findFirstByUserNumIsLessThan(50);
        if (acc.isEmpty()) {
            return "Account not found";
        }
        return cloudflareService.createPolicy(acc.get(), request);
    }

    @GetMapping("/getApplications")
    public String getApplications(@RequestParam String id) {
        var acc = repository.findByAccountId(id);
        if (acc.isEmpty()) {
            return "Account not found";
        }
        return cloudflareService.getApplications(acc.get()).getBody();
    }
//
//    @PostMapping("/createEnrollmentPolicy")
//    public String createEnrollmentPolicy(@RequestParam String email)
//    {
//        return cloudflareService.createEnrollmentPolicy("f94963bb-a350-40eb-9c8c-d88525ed59cf", "test@gmail.com");
//    }
}
