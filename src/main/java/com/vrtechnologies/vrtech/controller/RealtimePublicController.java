package com.vrtechnologies.vrtech.controller;

import com.vrtechnologies.vrtech.service.SseEmitterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/public/realtime")
public class RealtimePublicController {
    private static final Logger log = LoggerFactory.getLogger(RealtimePublicController.class);
    private final SseEmitterService sseEmitterService;

    public RealtimePublicController(SseEmitterService sseEmitterService) {
        this.sseEmitterService = sseEmitterService;
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(HttpServletResponse response) {
        log.info("Received new SSE subscription request for public storefront stream");
        response.setHeader("X-Accel-Buffering", "no");
        response.setHeader("Cache-Control", "no-cache, no-transform");
        response.setHeader("Connection", "keep-alive");
        return sseEmitterService.subscribe();
    }
}
