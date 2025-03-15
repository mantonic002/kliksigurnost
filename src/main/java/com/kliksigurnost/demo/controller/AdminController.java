package com.kliksigurnost.demo.controller;

import com.kliksigurnost.demo.exception.CloudflareApiException;
import com.kliksigurnost.demo.exception.NotFoundException;
import com.kliksigurnost.demo.model.*;
import com.kliksigurnost.demo.service.CloudflareAccountService;
import com.kliksigurnost.demo.service.CloudflarePolicyService;
import com.kliksigurnost.demo.service.SupportAppointmentService;
import com.kliksigurnost.demo.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminController {

    private final CloudflareAccountService cloudflareAccountService;
    private final CloudflarePolicyService cloudflarePolicyService;
    private final UserService userService;
    private final SupportAppointmentService supportAppointmentService;

    // Endpoint to get all Cloudflare accounts
    @GetMapping("/accounts")
    public ResponseEntity<List<CloudflareAccount>> getAllCloudflareAccounts() {
        log.info("Fetching all Cloudflare accounts");
        try {
            List<CloudflareAccount> accounts = cloudflareAccountService.getAllAccounts();
            return ResponseEntity.ok(accounts);
        } catch (CloudflareApiException e) {
            log.error("Failed to fetch Cloudflare accounts: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    // Endpoint to add a new Cloudflare account
    @PostMapping("/accounts/setup")
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

    // Endpoint to get all users
    @GetMapping("/users")
    public ResponseEntity<List<UserProfile>> getAllUsers() {
        log.info("Fetching all users");
        try {
            List<UserProfile> users = userService.getAllUsers();
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            log.error("Failed to fetch users: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    // Endpoint to get all policies
    @GetMapping("/policies")
    public ResponseEntity<List<CloudflarePolicy>> getAllPolicies() {
        log.info("Fetching all policies");
        try {
            List<CloudflarePolicy> policies = cloudflarePolicyService.getAllPolicies();
            return ResponseEntity.ok(policies);
        } catch (CloudflareApiException e) {
            log.error("Failed to fetch policies: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    // Endpoint to get all policies
    @GetMapping("/policies/{userId}")
    public ResponseEntity<List<CloudflarePolicy>> getPoliciesByUserId(@PathVariable Integer userId) {
        log.info("Fetching policies for user {}", userId);
        try {
            List<CloudflarePolicy> policies = cloudflarePolicyService.getPoliciesByUser(userId);
            return ResponseEntity.ok(policies);
        } catch (CloudflareApiException e) {
            log.error("Failed to fetch policies: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    // Endpoint to get all appointments, searchable by user
    @GetMapping("/appointments")
    public ResponseEntity<List<SupportAppointment>> getAppointmentsBetween(@RequestParam(required = true) String start, @RequestParam(required = true) String end) {
        log.info("Fetching appointments between: {}, {}", start, end);
        try {
            LocalDateTime startTime = LocalDateTime.parse(start);
            LocalDateTime endTime = LocalDateTime.parse(end);
            List<SupportAppointment> appointments = supportAppointmentService.getAllAppointmentsBetween(startTime, endTime);
            return ResponseEntity.ok(appointments);
        } catch (NotFoundException e) {
            log.warn("Appointments not found between: {}, {}", start, end);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        } catch (Exception e) {
            log.error("Failed to fetch appointments: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
}