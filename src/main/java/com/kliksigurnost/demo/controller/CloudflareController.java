package com.kliksigurnost.demo.controller;

import com.kliksigurnost.demo.exception.CloudflareApiException;
import com.kliksigurnost.demo.exception.NotFoundException;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/policies")
public class CloudflareController {

    private final CloudflarePolicyService cloudflarePolicyService;
    private final CloudflareLogService cloudflareLogService;
    private final CloudflareDeviceService cloudflareDeviceService;
    private final CloudflareAccountService cloudflareAccountService;

    @GetMapping
    public ResponseEntity<List<CloudflarePolicy>> getUserPolicies() {
        log.info("Fetching all policies for the current user");
        return ResponseEntity.ok(cloudflarePolicyService.getPoliciesByUser());
    }

    @PostMapping
    public ResponseEntity<String> createUserPolicy(@RequestBody CloudflarePolicy policy) {
        log.info("Creating a new policy for the current user");
        try {
            String response = cloudflarePolicyService.createPolicy(policy);
            return ResponseEntity.ok(response);
        } catch (CloudflareApiException e) {
            log.error("Failed to create policy: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @DeleteMapping("/{policyId}")
    public ResponseEntity<String> deleteUserPolicy(@PathVariable String policyId) {
        log.info("Deleting policy with ID: {}", policyId);
        try {
            cloudflarePolicyService.deletePolicy(policyId);
            return ResponseEntity.ok("Policy deleted successfully");
        } catch (NotFoundException e) {
            log.warn("Policy not found: {}", policyId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (UnauthorizedAccessException e) {
            log.warn("Unauthorized access to delete policy: {}", policyId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (CloudflareApiException e) {
            log.error("Failed to delete policy: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @PutMapping("/{policyId}")
    public ResponseEntity<String> updateUserPolicy(@PathVariable String policyId, @RequestBody CloudflarePolicy updatedPolicy) {
        log.info("Updating policy with ID: {}", policyId);
        try {
            cloudflarePolicyService.updatePolicy(policyId, updatedPolicy);
            return ResponseEntity.ok("Policy updated successfully");
        } catch (NotFoundException e) {
            log.warn("Policy not found: {}", policyId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (UnauthorizedAccessException e) {
            log.warn("Unauthorized access to update policy: {}", policyId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (CloudflareApiException e) {
            log.error("Failed to update policy: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @PostMapping("/setupAccount")
    public ResponseEntity<String> setupCloudflareAccount(@RequestBody CloudflareAccount account) {
        log.info("Setting up Cloudflare account");
        try {
            String response = cloudflareAccountService.createAccount(account);
            return ResponseEntity.ok(response);
        } catch (CloudflareApiException e) {
            log.error("Failed to set up Cloudflare account: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @GetMapping("/devices")
    public ResponseEntity<List<CloudflareDevice>> getUserDevices() {
        log.info("Fetching devices for the current user");
        try {
            return ResponseEntity.ok(cloudflareDeviceService.getDevicesByUser());
        } catch (CloudflareApiException e) {
            log.error("Failed to fetch devices: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @GetMapping("/userLogs")
    public ResponseEntity<List<CloudflareLog>> getUserLogs(
            @RequestParam String startDateTime,
            @RequestParam String endDateTime,
            @RequestParam(defaultValue = "datetime_DESC") List<String> orderBy,
            @RequestParam(defaultValue = "25") int pageSize,
            @RequestParam(defaultValue = "0") int resolverDecision, // 9 is blocked, 10 is allowed
            @RequestParam(required = false) String lastDateTime,
            @RequestParam(required = false) String lastPolicyId) {
        log.info("Fetching logs for the current user");
        try {
            return ResponseEntity.ok(cloudflareLogService.getLogsForUser(
                    startDateTime, endDateTime, orderBy, lastDateTime, lastPolicyId, pageSize, resolverDecision));
        } catch (CloudflareApiException e) {
            log.error("Failed to fetch logs: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
}