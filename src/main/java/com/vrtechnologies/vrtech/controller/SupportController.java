package com.vrtechnologies.vrtech.controller;

import com.vrtechnologies.vrtech.dto.event.SystemEvent;
import com.vrtechnologies.vrtech.dto.response.ProductResponse;
import com.vrtechnologies.vrtech.service.ProductService;
import com.vrtechnologies.vrtech.service.SseEmitterService;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/public/support")
public class SupportController {
    private final SseEmitterService sseEmitterService;
    private final ProductService productService;

    public SupportController(SseEmitterService sseEmitterService, ProductService productService) {
        this.sseEmitterService = sseEmitterService;
        this.productService = productService;
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
