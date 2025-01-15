package com.kliksigurnost.demo.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonProperty;
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
    String traffic;
    String cloudflareAccId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonBackReference
    User user;

    @Embedded
    private Schedule schedule;
}

