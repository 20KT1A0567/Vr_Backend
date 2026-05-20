package com.vrtechnologies.vrtech.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vrtechnologies.vrtech.dto.response.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final Map<String, Window> windows = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;
    private final int limit;
    private final Duration window;

    public RateLimitFilter(
            ObjectMapper objectMapper,
            @Value("${app.rate-limit.window-seconds:60}") long windowSeconds,
            @Value("${app.rate-limit.max-requests:10}") int limit
    ) {
        this.objectMapper = objectMapper;
        this.limit = limit;
        this.window = Duration.ofSeconds(windowSeconds);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        if (!isLimited(request)) {
            filterChain.doFilter(request, response);
            return;
        }
        String key = request.getRequestURI() + ":" + clientIp(request);
        Window current = windows.compute(key, (ignored, existing) -> {
            Instant now = Instant.now();
            if (existing == null || existing.expiresAt.isBefore(now)) {
                return new Window(now.plus(window), 1);
            }
            existing.count++;
            return existing;
        });
        if (current.count > limit) {
            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getWriter(), ApiResponse.error("Too many requests. Please try again shortly.", null));
            return;
        }
        filterChain.doFilter(request, response);
    }

    private boolean isLimited(HttpServletRequest request) {
        String method = request.getMethod();
        String path = request.getRequestURI();
        if (!"POST".equalsIgnoreCase(method) && !"PATCH".equalsIgnoreCase(method)) {
            return false;
        }
        return path.equals("/api/auth/login")
                || path.equals("/api/auth/phone/send")
                || path.equals("/api/auth/phone/verify")
                || path.equals("/api/enquiries")
                || path.equals("/api/orders/place")
                || path.equals("/api/orders/guest")
                || path.equals("/api/products/back-in-stock");
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private static class Window {
        private final Instant expiresAt;
        private int count;

        private Window(Instant expiresAt, int count) {
            this.expiresAt = expiresAt;
            this.count = count;
        }
    }
}
