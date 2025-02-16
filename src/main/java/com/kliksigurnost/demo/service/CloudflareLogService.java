package com.kliksigurnost.demo.service;

import com.kliksigurnost.demo.model.CloudflareLog;

import java.util.List;

public interface CloudflareLogService {
    List<CloudflareLog> getLogsForUser(String startDateTime, String endDateTime, List<String> orderBy);
}
