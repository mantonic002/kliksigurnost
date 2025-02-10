package com.kliksigurnost.demo.repository;

import com.kliksigurnost.demo.model.CloudflarePolicy;
import com.kliksigurnost.demo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CloudflarePolicyRepository extends JpaRepository<CloudflarePolicy, String> {
    List<CloudflarePolicy> findByUser(User user);
    long countByUser(User user);
}
