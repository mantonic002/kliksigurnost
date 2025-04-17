package com.kliksigurnost.demo.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CloudflareDevice {
    private String id;
    private String manufacturer;
    private String model;
    private String lastSeenTime;
    private String email;
    private String serialNumber;
}
