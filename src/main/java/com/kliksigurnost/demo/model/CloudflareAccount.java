package com.kliksigurnost.demo.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "cloudflare_account")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CloudflareAccount {
    @Id
    private String accountId;
    private String email;
    private String authorizationToken;
    private String enrollmentApplicationId;
}
