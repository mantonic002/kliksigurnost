package com.kliksigurnost.demo.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfile {
    private Integer id;

    private String email;

    private Boolean isSetUp;

    private CloudflareAccount cloudflareAccount;

    private List<CloudflarePolicy> policies;

    private Role role;

    private Boolean locked;

    private Boolean enabled;

    private AuthProvider authProvider;
}
