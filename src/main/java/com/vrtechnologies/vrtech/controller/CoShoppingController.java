package com.vrtechnologies.vrtech.controller;

import com.vrtechnologies.vrtech.dto.event.SystemEvent;
import com.vrtechnologies.vrtech.service.SseEmitterService;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/public/coshopping")
public class CoShoppingController {
    private final SseEmitterService sseEmitterService;
    
    // In-memory registry of active co-shopping sessions
    private final Map<String, Object> activeSessions = new ConcurrentHashMap<>();

    public CoShoppingController(SseEmitterService sseEmitterService) {
        this.sseEmitterService = sseEmitterService;
    }

    @PostMapping("/sync")
    public Map<String, Object> syncSession(@RequestBody Map<String, Object> payload) {
        String sessionUuid = (String) payload.get("sessionUuid");
        if (sessionUuid == null || sessionUuid.isBlank()) {
            return Map.of("success", false, "message", "sessionUuid is required");
        }
        
        activeSessions.put(sessionUuid, payload);

        SystemEvent coShoppingEvent = SystemEvent.builder()
                .eventType("CO_SHOPPING_UPDATE")
                .title("Shared Cart Updated")
                .message("Collaborative shopping cart mutated.")
                .severity("INFO")
                .payload(payload)
                .timestamp(LocalDateTime.now())
                .build();
                
        sseEmitterService.broadcast(coShoppingEvent);
        
        return Map.of("success", true, "session", payload);
    }
}
