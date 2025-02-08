package com.kliksigurnost.demo.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CloudflareLog {
    String[] categoryNames;
    String datetime;
    String matchedApplicationName;
    String policyId;
    String policyName;
    String queryName;
    Integer resolverDecision;
}
