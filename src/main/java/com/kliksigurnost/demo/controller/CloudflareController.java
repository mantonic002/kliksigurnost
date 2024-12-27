package com.kliksigurnost.demo.controller;

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

    @GetMapping("/getPolicies")
    public String getPolicies() {
        return cloudflareService.getPolicies();
    }

    @PostMapping("/createPolicy")
    public String createPolicy(@RequestParam String email)
    {
        return cloudflareService.createPolicy("block", email);
    }
}
