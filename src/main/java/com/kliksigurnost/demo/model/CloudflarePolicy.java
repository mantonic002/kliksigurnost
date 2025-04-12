package com.kliksigurnost.demo.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;

@Data
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CloudflarePolicy {
    @Id
    String id;
    String name;
    String action;

    @Column(columnDefinition = "TEXT")
    String traffic;
    String cloudflareAccId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JsonBackReference
    User user;

    @Embedded
    private Schedule schedule;

    @Column(nullable = false)
    private boolean isAllowAll = false;
}

