package com.vrtechnologies.vrtech.controller;

import com.vrtechnologies.vrtech.service.SseEmitterService;
import com.vrtechnologies.vrtech.service.UserContextService;
import com.vrtechnologies.vrtech.entity.User;
import com.vrtechnologies.vrtech.dto.event.SystemEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/admin/realtime")
public class RealtimeAdminController {
    private static final Logger log = LoggerFactory.getLogger(RealtimeAdminController.class);
    
    private final SseEmitterService sseEmitterService;
    private final UserContextService userContextService;
    
    private static final Map<String, Map<String, Object>> activeLocks = new ConcurrentHashMap<>();
    private static final Map<String, Map<String, Object>> activePresence = new ConcurrentHashMap<>();

    public RealtimeAdminController(SseEmitterService sseEmitterService, UserContextService userContextService) {
        this.sseEmitterService = sseEmitterService;
        this.userContextService = userContextService;
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        log.info("Received new SSE subscription request for admin stream");
        return sseEmitterService.subscribe();
    }

    @PostMapping("/ping")
    public Map<String, Object> broadcastPing(@RequestBody Map<String, Object> payload) {
        log.info("Broadcasting team peer note from admin session");
        SystemEvent pingEvent = SystemEvent.builder()
                .eventType("ADMIN_PING")
                .title("Peer Message")
                .message((String) payload.get("message"))
                .severity("INFO")
                .payload(payload)
                .timestamp(LocalDateTime.now())
                .build();
        sseEmitterService.broadcast(pingEvent);
        return Map.of("success", true, "message", "Command note broadcasted");
    }

    @GetMapping("/locks")
    public Map<String, Map<String, Object>> getLocks() {
        return getUnexpiredLocks();
    }

    @PostMapping("/locks/acquire")
    public Map<String, Object> acquireLock(@RequestBody Map<String, String> payload) {
        String resourceId = payload.get("resourceId");
        if (resourceId == null || resourceId.isBlank()) {
            return Map.of("success", false, "message", "resourceId is required");
        }

        User currentUser = userContextService.getCurrentUser();
        long now = System.currentTimeMillis();
        long threshold = now - 45000;

        Map<String, Object> existingLock = activeLocks.get(resourceId);
        if (existingLock != null) {
            Long timestamp = (Long) existingLock.get("timestamp");
            String holderEmail = (String) existingLock.get("adminEmail");
            
            // Check if lock has not expired and belongs to someone else
            if (timestamp != null && timestamp >= threshold && !currentUser.getEmail().equalsIgnoreCase(holderEmail)) {
                return Map.of(
                    "success", false,
                    "holderEmail", holderEmail,
                    "holderName", existingLock.get("adminName")
                );
            }
        }

        // Acquire or renew lock
        Map<String, Object> lockData = Map.of(
            "adminEmail", currentUser.getEmail(),
            "adminName", currentUser.getName(),
            "timestamp", now
        );
        activeLocks.put(resourceId, lockData);
        log.info("Lock acquired/renewed by {} on resource {}", currentUser.getEmail(), resourceId);
        
        broadcastCollaborationUpdate();
        return Map.of("success", true);
    }

    @PostMapping("/locks/release")
    public Map<String, Object> releaseLock(@RequestBody Map<String, String> payload) {
        String resourceId = payload.get("resourceId");
        if (resourceId == null || resourceId.isBlank()) {
            return Map.of("success", false, "message", "resourceId is required");
        }

        User currentUser = userContextService.getCurrentUser();
        Map<String, Object> existingLock = activeLocks.get(resourceId);
        if (existingLock != null) {
            String holderEmail = (String) existingLock.get("adminEmail");
            if (currentUser.getEmail().equalsIgnoreCase(holderEmail)) {
                activeLocks.remove(resourceId);
                log.info("Lock released by {} on resource {}", currentUser.getEmail(), resourceId);
                broadcastCollaborationUpdate();
            }
        }

        return Map.of("success", true);
    }

    @Scheduled(fixedDelay = 15000)
    public void sweepExpiredLocks() {
        long threshold = System.currentTimeMillis() - 45000;
        boolean removedAny = false;
        for (Map.Entry<String, Map<String, Object>> entry : activeLocks.entrySet()) {
            Map<String, Object> lockData = entry.getValue();
            Long timestamp = (Long) lockData.get("timestamp");
            if (timestamp != null && timestamp < threshold) {
                activeLocks.remove(entry.getKey());
                removedAny = true;
                log.info("Evicted expired lock on resource: {}", entry.getKey());
            }
        }
        if (removedAny) {
            broadcastCollaborationUpdate();
        }
    }

    private void broadcastCollaborationUpdate() {
        SystemEvent event = SystemEvent.builder()
                .eventType("COLLABORATION_UPDATE")
                .title("Collaboration Update")
                .message("Active resource locks updated")
                .severity("INFO")
                .payload(getUnexpiredLocks())
                .timestamp(LocalDateTime.now())
                .build();
        sseEmitterService.broadcast(event);
    }

    private Map<String, Map<String, Object>> getUnexpiredLocks() {
        long threshold = System.currentTimeMillis() - 45000;
        Map<String, Map<String, Object>> unexpired = new ConcurrentHashMap<>();
        activeLocks.forEach((key, val) -> {
            Long timestamp = (Long) val.get("timestamp");
            if (timestamp != null && timestamp >= threshold) {
                unexpired.put(key, val);
            }
        });
        return unexpired;
    }

    @PostMapping("/presence/heartbeat")
    public Map<String, Object> updatePresence(@RequestBody Map<String, String> payload) {
        String page = payload.get("page");
        String url = payload.get("url");
        if (page == null) page = "Admin Workspace";
        if (url == null) url = "/";

        User currentUser = userContextService.getCurrentUser();
        String sessionKey = currentUser.getEmail();

        Map<String, Object> presenceData = Map.of(
            "adminEmail", currentUser.getEmail(),
            "adminName", currentUser.getName(),
            "adminRole", currentUser.getRole().name(),
            "page", page,
            "url", url,
            "timestamp", System.currentTimeMillis()
        );
        activePresence.put(sessionKey, presenceData);
        broadcastPresenceUpdate();

        return Map.of("success", true);
    }

    @Scheduled(fixedDelay = 15000)
    public void sweepExpiredPresence() {
        long threshold = System.currentTimeMillis() - 30000; // 30s threshold
        boolean removedAny = false;
        for (Map.Entry<String, Map<String, Object>> entry : activePresence.entrySet()) {
            Map<String, Object> data = entry.getValue();
            Long timestamp = (Long) data.get("timestamp");
            if (timestamp != null && timestamp < threshold) {
                activePresence.remove(entry.getKey());
                removedAny = true;
                log.info("Evicted inactive presence for administrator session: {}", entry.getKey());
            }
        }
        if (removedAny) {
            broadcastPresenceUpdate();
        }
    }

    private void broadcastPresenceUpdate() {
        SystemEvent event = SystemEvent.builder()
                .eventType("PRESENCE_UPDATE")
                .title("Presence Update")
                .message("Active co-presence updated")
                .severity("INFO")
                .payload(getUnexpiredPresence())
                .timestamp(LocalDateTime.now())
                .build();
        sseEmitterService.broadcast(event);
    }

    private List<Map<String, Object>> getUnexpiredPresence() {
        long threshold = System.currentTimeMillis() - 30000;
        List<Map<String, Object>> list = new ArrayList<>();
        activePresence.forEach((key, val) -> {
            Long timestamp = (Long) val.get("timestamp");
            if (timestamp != null && timestamp >= threshold) {
                list.add(val);
            }
        });
        return list;
    }
}
