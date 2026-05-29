package com.vrtechnologies.vrtech.controller;

import com.vrtechnologies.vrtech.dto.response.ApiResponse;
import com.vrtechnologies.vrtech.service.SystemTelemetryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class SystemTelemetryController {

    private static final Logger log = LoggerFactory.getLogger(SystemTelemetryController.class);

    private final SystemTelemetryService telemetryService;

    public SystemTelemetryController(SystemTelemetryService telemetryService) {
        this.telemetryService = telemetryService;
    }

    @GetMapping("/api/admin/system/telemetry/current")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Map<String, Object>> getTelemetryCurrent() {
        return ApiResponse.ok("Current system telemetry retrieved", telemetryService.collectMetrics());
    }

    @PostMapping("/api/super-admin/system/gc")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiResponse<Object> triggerGarbageCollection() {
        log.warn("Super Admin triggered manual Garbage Collection run.");
        System.gc();
        return ApiResponse.ok("Manual JVM Garbage Collection cycle requested", null);
    }
}
