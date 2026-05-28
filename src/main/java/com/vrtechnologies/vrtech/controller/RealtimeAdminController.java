package com.vrtechnologies.vrtech.controller;

import com.vrtechnologies.vrtech.service.SseEmitterService;
import com.vrtechnologies.vrtech.service.UserContextService;
import com.vrtechnologies.vrtech.entity.User;
import com.vrtechnologies.vrtech.entity.AdminPing;
import com.vrtechnologies.vrtech.repository.AdminPingRepository;
import com.vrtechnologies.vrtech.dto.event.SystemEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.prepost.PreAuthorize;
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
    private final AdminPingRepository adminPingRepository;
    
    private static final Map<String, Map<String, Object>> activeLocks = new ConcurrentHashMap<>();
    private static final Map<String, Map<String, Object>> activePresence = new ConcurrentHashMap<>();

    public RealtimeAdminController(SseEmitterService sseEmitterService, UserContextService userContextService, AdminPingRepository adminPingRepository) {
        this.sseEmitterService = sseEmitterService;
        this.userContextService = userContextService;
        this.adminPingRepository = adminPingRepository;
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        log.info("Received new SSE subscription request for admin stream");
        return sseEmitterService.subscribe();
    }

    @GetMapping("/pings")
    public List<Map<String, Object>> getPings() {
        log.info("Fetching real team chats from database");
        List<AdminPing> dbPings = adminPingRepository.findAllByOrderByPingTimestampAsc();
        List<Map<String, Object>> response = new ArrayList<>();
        for (AdminPing p : dbPings) {
            response.add(Map.of(
                "id", p.getId(),
                "senderEmail", p.getSenderEmail(),
                "senderName", p.getSenderName() != null ? p.getSenderName() : p.getSenderEmail().split("@")[0],
                "senderRole", p.getSenderRole() != null ? p.getSenderRole() : "ADMIN",
                "message", p.getMessage(),
                "timestamp", p.getPingTimestamp().toString()
            ));
        }
        return response;
    }

    @PostMapping("/ping")
    public Map<String, Object> broadcastPing(@RequestBody Map<String, Object> payload) {
        log.info("Saving and broadcasting team peer note from admin session");
        
        String senderEmail = (String) payload.get("senderEmail");
        String senderName = (String) payload.get("senderName");
        String senderRole = (String) payload.get("senderRole");
        String message = (String) payload.get("message");
        
        // Save to database
        AdminPing newPing = AdminPing.builder()
                .senderEmail(senderEmail != null ? senderEmail : "admin@vrtech.in")
                .senderName(senderName)
                .senderRole(senderRole)
                .message(message)
                .pingTimestamp(LocalDateTime.now())
                .build();
        
        AdminPing savedPing = adminPingRepository.save(newPing);
        
        // Put the newly saved ID into the broadcast payload
        Map<String, Object> broadcastPayload = new java.util.HashMap<>(payload);
        broadcastPayload.put("id", savedPing.getId());
        broadcastPayload.put("timestamp", savedPing.getPingTimestamp().toString());
        
        SystemEvent pingEvent = SystemEvent.builder()
                .eventType("ADMIN_PING")
                .title("Peer Message")
                .message(message)
                .severity("INFO")
                .payload(broadcastPayload)
                .timestamp(LocalDateTime.now())
                .build();
        sseEmitterService.broadcast(pingEvent);
        return Map.of("success", true, "message", "Command note broadcasted", "id", savedPing.getId());
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PostMapping("/pings/clear")
    public Map<String, Object> clearPings(@RequestBody Map<String, Object> payload) {
        String mode = (String) payload.get("mode"); // "selected", "day-wise", "all"
        log.info("Super admin requested clear pings: mode={}", mode);
        
        if ("selected".equalsIgnoreCase(mode)) {
            List<Number> ids = (List<Number>) payload.get("ids");
            if (ids != null && !ids.isEmpty()) {
                List<Long> longIds = ids.stream().map(Number::longValue).toList();
                adminPingRepository.deleteSelectedPings(longIds);
                log.info("Deleted selected pings: {}", longIds);
            }
        } else if ("day-wise".equalsIgnoreCase(mode)) {
            Number days = (Number) payload.get("days");
            if (days != null) {
                LocalDateTime threshold = LocalDateTime.now().minusDays(days.longValue());
                adminPingRepository.deletePingsOlderThan(threshold);
                log.info("Deleted pings older than {} days (threshold={})", days, threshold);
            }
        } else if ("all".equalsIgnoreCase(mode)) {
            adminPingRepository.clearAllPings();
            log.info("Cleared all team pings from database");
        }
        
        // Broadcast refresh event so all connected admins clear their client screens in real-time
        SystemEvent resetEvent = SystemEvent.builder()
                .eventType("ADMIN_PING_RESET")
                .title("Chat Cleared")
                .message("Team chats have been cleared/updated by super admin")
                .severity("INFO")
                .payload(Map.of("mode", mode != null ? mode : "all"))
                .timestamp(LocalDateTime.now())
                .build();
        sseEmitterService.broadcast(resetEvent);
        
        return Map.of("success", true, "message", "Clear operation executed");
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

    @GetMapping("/presence")
    public List<Map<String, Object>> getPresence() {
        return getUnexpiredPresence();
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
