package com.vrtechnologies.vrtech.service;

import com.vrtechnologies.vrtech.dto.response.SystemHealthResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class SystemHealthService {

    private final JdbcTemplate jdbcTemplate;
    private final CloudinaryService cloudinaryService;
    private final RazorpayService razorpayService;

    public SystemHealthService(
            JdbcTemplate jdbcTemplate,
            CloudinaryService cloudinaryService,
            RazorpayService razorpayService
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.cloudinaryService = cloudinaryService;
        this.razorpayService = razorpayService;
    }

    public SystemHealthResponse currentHealth() {
        List<SystemHealthResponse.Component> components = new ArrayList<>();
        components.add(databaseStatus());
        components.add(cloudinaryStatus());
        components.add(razorpayStatus());

        boolean degraded = components.stream().anyMatch(item -> !"OK".equals(item.getStatus()));
        return SystemHealthResponse.builder()
                .status(degraded ? "DEGRADED" : "OK")
                .checkedAt(LocalDateTime.now())
                .components(components)
                .build();
    }

    private SystemHealthResponse.Component databaseStatus() {
        try {
            Integer result = jdbcTemplate.queryForObject("select 1", Integer.class);
            return component("database", "Database", result != null && result == 1 ? "OK" : "WARN", "Database query completed.");
        } catch (RuntimeException exception) {
            return component("database", "Database", "ERROR", "Database health check failed.");
        }
    }

    private SystemHealthResponse.Component cloudinaryStatus() {
        try {
            boolean configured = cloudinaryService.isConfigured();
            return component(
                    "cloudinary",
                    "Cloudinary media",
                    configured ? "OK" : "WARN",
                    configured ? "Image and video uploads are configured." : "Set CLOUDINARY_CLOUD_NAME, CLOUDINARY_API_KEY, and CLOUDINARY_API_SECRET."
            );
        } catch (RuntimeException exception) {
            return component("cloudinary", "Cloudinary media", "ERROR", "Cloudinary health check failed.");
        }
    }

    private SystemHealthResponse.Component razorpayStatus() {
        try {
            boolean enabled = razorpayService.isEnabled();
            return component(
                    "razorpay",
                    "Razorpay payments",
                    enabled ? "OK" : "WARN",
                    enabled ? "Online payment checkout is configured." : "Set Razorpay key id and secret, or keep online payments disabled."
            );
        } catch (RuntimeException exception) {
            return component("razorpay", "Razorpay payments", "ERROR", "Razorpay health check failed.");
        }
    }

    private SystemHealthResponse.Component component(String key, String label, String status, String message) {
        return SystemHealthResponse.Component.builder()
                .key(key)
                .label(label)
                .status(status)
                .message(message)
                .build();
    }
}
