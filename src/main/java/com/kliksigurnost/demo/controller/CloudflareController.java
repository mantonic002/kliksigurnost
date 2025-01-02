package com.kliksigurnost.demo.controller;

import com.kliksigurnost.demo.model.CloudflareAccount;
import com.kliksigurnost.demo.model.CloudflarePolicy;
import com.kliksigurnost.demo.model.User;
import com.kliksigurnost.demo.repository.CloudflareAccountRepository;
import com.kliksigurnost.demo.service.AuthenticationService;
import com.kliksigurnost.demo.service.CloudflareService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class CloudflareController {
    private final CloudflareService cloudflareService;
    private final AuthenticationService authenticationService;
    private final CloudflareAccountRepository repository;

    @PostMapping("/setupAccount")
    public String setupAccount(@RequestBody CloudflareAccount account) {
        return cloudflareService.createAccount(account);
    }

    @GetMapping("/getPolicies")
    public ResponseEntity<List<CloudflarePolicy>> getPolicies() {
        return ResponseEntity.ok(cloudflareService.getPoliciesByUser());
    }

    @PostMapping("/createPolicy")
    public String createPolicy(@RequestBody CloudflarePolicy policy)
    {
        return cloudflareService.createPolicy(policy);
    }

    @GetMapping("/getApplications")
    public String getApplications(@RequestParam String id) {
        var acc = repository.findByAccountId(id);
        if (acc.isEmpty()) {
            return "Account not found";
        }
        return cloudflareService.getApplications(acc.get()).getBody();
    }
}
