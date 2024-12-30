package com.kliksigurnost.demo.repository;

import com.kliksigurnost.demo.model.CloudflareAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CloudflareAccountRepository  extends JpaRepository<CloudflareAccount, String> {

    Optional<CloudflareAccount> findByAccountId(String accountId);
    Optional<CloudflareAccount> findFirstByUserNumIsLessThan(Integer userNum);
}
