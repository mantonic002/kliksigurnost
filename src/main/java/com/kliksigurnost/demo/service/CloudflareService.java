package com.kliksigurnost.demo.service;

public interface CloudflareService {
    String getPolicies();
    String createPolicy(String action, String email);
}
