package com.kliksigurnost.demo.controller;

import com.kliksigurnost.demo.model.CloudflareAccount;
import com.kliksigurnost.demo.service.CloudflareService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
public class CloudflareController {
    private final CloudflareService cloudflareService;

    @Autowired
    public CloudflareController(CloudflareService cloudflareService) {
        this.cloudflareService = cloudflareService;
    }

    @PostMapping("/setupAccount")
    public String setupAccount(@RequestBody CloudflareAccount account) {
        return cloudflareService.createAccount(account);
    }

    @GetMapping("/getPolicies")
    public String getPolicies() {
        return cloudflareService.getPolicies();
    }

    @PostMapping("/createPolicy")
    public String createPolicy(@RequestParam String email)
    {
        return cloudflareService.createPolicy("block", email);
    }

    @GetMapping("/getApplications")
    public String getApplications() {
        return cloudflareService.getApplications().getBody();
    }

    @PostMapping("/createEnrollmentPolicy")
    public String createEnrollmentPolicy(@RequestParam String email)
    {
        return cloudflareService.createEnrollmentPolicy("f94963bb-a350-40eb-9c8c-d88525ed59cf", email);
    }
}
