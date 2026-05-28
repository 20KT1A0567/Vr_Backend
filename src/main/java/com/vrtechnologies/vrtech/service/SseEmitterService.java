package com.vrtechnologies.vrtech.service;

import com.vrtechnologies.vrtech.dto.event.SystemEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class SseEmitterService {
    private static final Logger log = LoggerFactory.getLogger(SseEmitterService.class);
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(180_000L); // 3-minute timeout
        emitters.add(emitter);

        emitter.onCompletion(() -> {
            log.debug("SSE emitter completed");
            emitters.remove(emitter);
        });
        emitter.onTimeout(() -> {
            log.debug("SSE emitter timed out");
            emitters.remove(emitter);
        });
        emitter.onError((e) -> {
            log.warn("SSE emitter encountered error", e);
            emitters.remove(emitter);
        });

        // Send a handshake event
        try {
            emitter.send(SseEmitter.event()
                    .name("CONNECT")
                    .id("handshake")
                    .data("Handshake successful"));
        } catch (IOException e) {
            log.error("Failed to send handshake event", e);
            emitters.remove(emitter);
        }
        return emitter;
    }

    public void broadcast(SystemEvent event) {
        if (emitters.isEmpty()) {
            return;
        }
        log.info("Broadcasting SystemEvent: type={}, title={}", event.getEventType(), event.getTitle());
        List<SseEmitter> deadEmitters = new CopyOnWriteArrayList<>();
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("SYSTEM_EVENT")
                        .id(String.valueOf(System.currentTimeMillis()))
                        .data(event));
            } catch (Exception e) {
                log.debug("Removing failed SSE emitter client connection", e);
                deadEmitters.add(emitter);
            }
        }
        emitters.removeAll(deadEmitters);
    }
}
