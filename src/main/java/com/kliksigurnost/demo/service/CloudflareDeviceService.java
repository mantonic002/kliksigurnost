package com.kliksigurnost.demo.service;

import com.kliksigurnost.demo.model.CloudflareDevice;

import java.util.List;

public interface CloudflareDeviceService {
    List<CloudflareDevice> getDevicesByUser();

}
