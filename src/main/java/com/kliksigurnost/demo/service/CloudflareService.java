package com.kliksigurnost.demo.service;

public interface CloudflareService {
    String getPolicies();
    String createPolicy(String action, String email);
    public String createEnrollmentApplication(String name);
    String getApplications();
    public String createEnrollmentPolicy(String appId, String email);
}
