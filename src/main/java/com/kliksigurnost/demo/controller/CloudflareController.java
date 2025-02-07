package com.kliksigurnost.demo.controller;

import com.kliksigurnost.demo.model.CloudflareAccount;
import com.kliksigurnost.demo.model.CloudflareDevice;
import com.kliksigurnost.demo.model.CloudflarePolicy;
import com.kliksigurnost.demo.service.CloudflareAccountService;
import com.kliksigurnost.demo.service.CloudflareService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/policies")
public class CloudflareController {
    private final CloudflareService cloudflareService;
    private final CloudflareAccountService cloudflareAccountService;


    @GetMapping
    public ResponseEntity<List<CloudflarePolicy>> getPolicies() {
        return ResponseEntity.ok(cloudflareService.getPoliciesByUser());
    }

    @PostMapping
    public ResponseEntity<String> createPolicy(@RequestBody CloudflarePolicy policy)
    {
        return ResponseEntity.ok(cloudflareService.createPolicy(policy));
    }

    @PostMapping("/setupAccount")
    public ResponseEntity<String> setupAccount(@RequestBody CloudflareAccount account) {
        return ResponseEntity.ok(cloudflareAccountService.createAccount(account));
    }

    @GetMapping("/devices")
    public ResponseEntity<List<CloudflareDevice>> getDevices() {
        return ResponseEntity.ok(cloudflareService.getDevicesByUser());
    }

    @GetMapping("/userLogs")
    public ResponseEntity<String> getUserLogs(
            @RequestParam String startDateTime,
            @RequestParam String endDateTime,
            @RequestParam(required = false) List<String> orderBy) {

        return ResponseEntity.ok(cloudflareService.getLogsForUser(startDateTime, endDateTime, orderBy));
    }

//    @GetMapping("/getApplications")
//    public String getApplications(@RequestParam String id) {
//        var acc = repository.findByAccountId(id);
//        if (acc.isEmpty()) {
//            return "Account not found";
//        }
//        return cloudflareService.getApplications(acc.get()).getBody();
//    }


}
