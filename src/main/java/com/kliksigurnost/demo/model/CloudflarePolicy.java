package com.kliksigurnost.demo.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.*;

@Data
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CloudflarePolicy {
    @Id
    String id;
    String action;
    String traffic;
    String cloudflareAccId;
    @ManyToOne(fetch = FetchType.LAZY)
    @JsonBackReference
    User user;
}
