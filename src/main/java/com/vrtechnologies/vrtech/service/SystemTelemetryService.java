package com.vrtechnologies.vrtech.service;

import com.vrtechnologies.vrtech.dto.event.SystemEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class SystemTelemetryService {

    private static final Logger log = LoggerFactory.getLogger(SystemTelemetryService.class);

    private final DataSource dataSource;
    private final SseEmitterService sseEmitterService;

    public SystemTelemetryService(DataSource dataSource, SseEmitterService sseEmitterService) {
        this.dataSource = dataSource;
        this.sseEmitterService = sseEmitterService;
    }

    public Map<String, Object> collectMetrics() {
        Map<String, Object> metrics = new HashMap<>();

        // JVM RAM Memory Metrics
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        long maxMemory = runtime.maxMemory();
        double ramUsagePercentage = ((double) usedMemory / maxMemory) * 100.0;

        metrics.put("ramUsedBytes", usedMemory);
        metrics.put("ramTotalBytes", totalMemory);
        metrics.put("ramMaxBytes", maxMemory);
        metrics.put("ramUsage", Math.round(ramUsagePercentage * 10.0) / 10.0);

        // System CPU Load Metric
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        double cpuLoad = 0.0;
        if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
            cpuLoad = ((com.sun.management.OperatingSystemMXBean) osBean).getCpuLoad() * 100.0;
        }
        if (cpuLoad < 0) {
            cpuLoad = 0.0;
        }
        metrics.put("cpuUsage", Math.round(cpuLoad * 10.0) / 10.0);

        // Database Hikari Connection Pool Metrics
        int activeConnections = 0;
        int maxConnections = 0;
        if (dataSource instanceof com.zaxxer.hikari.HikariDataSource) {
            com.zaxxer.hikari.HikariDataSource hikariDS = (com.zaxxer.hikari.HikariDataSource) dataSource;
            com.zaxxer.hikari.HikariPoolMXBean poolMXBean = hikariDS.getHikariPoolMXBean();
            if (poolMXBean != null) {
                activeConnections = poolMXBean.getActiveConnections();
            }
            maxConnections = hikariDS.getMaximumPoolSize();
        }
        metrics.put("dbActive", activeConnections);
        metrics.put("dbMax", maxConnections);

        return metrics;
    }

    @Scheduled(fixedDelay = 3000)
    public void broadcastTelemetry() {
        try {
            Map<String, Object> payload = collectMetrics();
            SystemEvent event = SystemEvent.builder()
                    .eventType("TELEMETRY_UPDATE")
                    .title("System Telemetry Update")
                    .message("Live server diagnostics telemetry push")
                    .severity("INFO")
                    .payload(payload)
                    .timestamp(LocalDateTime.now())
                    .build();
            sseEmitterService.broadcast(event);
        } catch (Exception e) {
            log.error("Failed to collect or broadcast system telemetry: {}", e.getMessage());
        }
    }
}
