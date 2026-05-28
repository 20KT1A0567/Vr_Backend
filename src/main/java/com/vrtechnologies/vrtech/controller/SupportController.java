package com.vrtechnologies.vrtech.controller;

import com.vrtechnologies.vrtech.dto.event.SystemEvent;
import com.vrtechnologies.vrtech.service.SseEmitterService;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/public/support")
public class SupportController {
    private final SseEmitterService sseEmitterService;

    public SupportController(SseEmitterService sseEmitterService) {
        this.sseEmitterService = sseEmitterService;
    }

    @PostMapping("/assist")
    public Map<String, Object> requestAssist(@RequestBody Map<String, Object> payload) {
        SystemEvent assistEvent = SystemEvent.builder()
                .eventType("SUPPORT_ASSIST")
                .title("Live Assist Requested")
                .message("Customer \"" + payload.getOrDefault("customerName", "Guest") + "\" is encountering issues at checkout.")
                .severity("INFO")
                .payload(payload)
                .timestamp(LocalDateTime.now())
                .build();
        sseEmitterService.broadcast(assistEvent);
        return Map.of("success", true, "message", "Support request broadcasted successfully");
    }
}
