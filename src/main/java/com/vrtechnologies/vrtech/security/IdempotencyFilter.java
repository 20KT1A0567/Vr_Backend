package com.vrtechnologies.vrtech.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vrtechnologies.vrtech.dto.response.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class IdempotencyFilter extends OncePerRequestFilter {

    private final Map<String, Instant> processedKeys = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;
    private final Duration expiryWindow = Duration.ofSeconds(10); // 10-second deduplication lock

    public IdempotencyFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();

        // Target checkout endpoints for transaction deduplication
        if ("POST".equalsIgnoreCase(method) && 
            (path.equals("/api/orders/place") || path.equals("/api/orders/guest"))) {

            String idempotencyKey = request.getHeader("X-Idempotency-Key");

            if (idempotencyKey != null && !idempotencyKey.trim().isEmpty()) {
                Instant now = Instant.now();
                Instant existingExpiry = processedKeys.get(idempotencyKey);

                if (existingExpiry != null && existingExpiry.isAfter(now)) {
                    response.setStatus(409); // 409 Conflict
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    objectMapper.writeValue(response.getWriter(), 
                        ApiResponse.error("Duplicate transaction detected. If your payment was deducted, please verify your orders page.", null));
                    return;
                }

                // Cache the key to expire after our lock duration
                processedKeys.put(idempotencyKey, now.plus(expiryWindow));

                // Cleanup expired keys periodically to prevent memory leaks
                cleanupExpiredKeys(now);
            }
        }

        filterChain.doFilter(request, response);
    }

    private void cleanupExpiredKeys(Instant now) {
        processedKeys.entrySet().removeIf(entry -> entry.getValue().isBefore(now));
    }
}
