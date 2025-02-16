package com.kliksigurnost.demo.controller;

import com.kliksigurnost.demo.exception.CloudflareApiException;
import com.kliksigurnost.demo.exception.PolicyNotFoundException;
import com.kliksigurnost.demo.exception.UnauthorizedAccessException;
import com.kliksigurnost.demo.model.CloudflareAccount;
import com.kliksigurnost.demo.model.CloudflareDevice;
import com.kliksigurnost.demo.model.CloudflareLog;
import com.kliksigurnost.demo.model.CloudflarePolicy;
import com.kliksigurnost.demo.service.CloudflareAccountService;
import com.kliksigurnost.demo.service.CloudflareDeviceService;
import com.kliksigurnost.demo.service.CloudflareLogService;
import com.kliksigurnost.demo.service.CloudflarePolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/policies")
public class CloudflareController {

    private final CloudflarePolicyService cloudflareService;
    private final CloudflareLogService cloudflareLogService;
    private final CloudflareDeviceService cloudflareDeviceService;
    private final CloudflareAccountService cloudflareAccountService;

    // Get all policies for the current user
    @GetMapping
    public ResponseEntity<List<CloudflarePolicy>> getPolicies() {
        return ResponseEntity.ok(cloudflareService.getPoliciesByUser());
    }

    // Create a new policy
    @PostMapping
    public ResponseEntity<String> createPolicy(@RequestBody CloudflarePolicy policy) {
        try {
            return ResponseEntity.ok(cloudflareService.createPolicy(policy));
        } catch (CloudflareApiException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    // Delete a policy by ID
    @DeleteMapping("/{policyId}")
    public ResponseEntity<String> deletePolicy(@PathVariable String policyId) {
        try {
            cloudflareService.deletePolicy(policyId);
            return ResponseEntity.ok("Policy deleted successfully");
        } catch (PolicyNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (UnauthorizedAccessException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (CloudflareApiException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    // Update an existing policy
    @PutMapping("/{policyId}")
    public ResponseEntity<String> updatePolicy(@PathVariable String policyId, @RequestBody CloudflarePolicy updatedPolicy) {
        try {
            cloudflareService.updatePolicy(policyId, updatedPolicy);
            return ResponseEntity.ok("Policy updated successfully");
        } catch (PolicyNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (UnauthorizedAccessException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (CloudflareApiException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    // Set up a Cloudflare account
    @PostMapping("/setupAccount")
    public ResponseEntity<String> setupAccount(@RequestBody CloudflareAccount account) {
        try {
            return ResponseEntity.ok(cloudflareAccountService.createAccount(account));
        } catch (CloudflareApiException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    // Get devices for the current user
    @GetMapping("/devices")
    public ResponseEntity<List<CloudflareDevice>> getDevices() {
        try {
            return ResponseEntity.ok(cloudflareDeviceService.getDevicesByUser());
        } catch (CloudflareApiException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    // Get logs for the current user
    @GetMapping("/userLogs")
    public ResponseEntity<List<CloudflareLog>> getUserLogs(
            @RequestParam String startDateTime,
            @RequestParam String endDateTime,
            @RequestParam(defaultValue = "datetime_DESC") List<String> orderBy,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "25") int pageSize,
            @RequestParam(required = false) String lastDateTime,
            @RequestParam(required = false) String lastPolicyId,
            @RequestParam(required = false) String direction) {
        try {
            return ResponseEntity.ok(cloudflareLogService.getLogsForUser(startDateTime, endDateTime, orderBy, lastDateTime, lastPolicyId, pageSize, direction));
        } catch (CloudflareApiException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
}