package com.vrtechnologies.vrtech.controller;

import com.vrtechnologies.vrtech.service.SseEmitterService;
import com.vrtechnologies.vrtech.dto.event.SystemEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/realtime")
public class RealtimeAdminController {
    private static final Logger log = LoggerFactory.getLogger(RealtimeAdminController.class);
    private final SseEmitterService sseEmitterService;

    public RealtimeAdminController(SseEmitterService sseEmitterService) {
        this.sseEmitterService = sseEmitterService;
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
}
