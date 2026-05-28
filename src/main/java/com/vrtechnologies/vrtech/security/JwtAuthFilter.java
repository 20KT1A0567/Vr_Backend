package com.vrtechnologies.vrtech.security;

import com.vrtechnologies.vrtech.entity.AuthSession;
import com.vrtechnologies.vrtech.repository.AuthSessionRepository;
import com.vrtechnologies.vrtech.service.SseEmitterService;
import com.vrtechnologies.vrtech.dto.event.SystemEvent;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import io.jsonwebtoken.JwtException;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Map;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;
    private final AuthSessionRepository authSessionRepository;
    private final SseEmitterService sseEmitterService;

    public JwtAuthFilter(JwtService jwtService, CustomUserDetailsService userDetailsService, AuthSessionRepository authSessionRepository, SseEmitterService sseEmitterService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.authSessionRepository = authSessionRepository;
        this.sseEmitterService = sseEmitterService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.equals("/actuator/health");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        String token = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        } else {
            token = request.getParameter("token");
        }

        if (token == null || token.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }
        String username;
        try {
            username = jwtService.extractUsername(token);
        } catch (JwtException | IllegalArgumentException exception) {
            SecurityContextHolder.clearContext();
            filterChain.doFilter(request, response);
            return;
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                if (jwtService.isTokenValid(token, userDetails)) {
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    
                    Long sessionId = jwtService.extractSessionId(token);
                    if (sessionId != null) {
                        request.setAttribute("currentSessionId", sessionId);

                        // Active Session & Device Hijacking Validation
                        Optional<AuthSession> sessionOpt = authSessionRepository.findById(sessionId);
                        if (sessionOpt.isEmpty() || !sessionOpt.get().isActive()) {
                            SecurityContextHolder.clearContext();
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"message\":\"Session has been revoked or expired. Please sign in again.\"}");
                            return;
                        }

                        AuthSession session = sessionOpt.get();
                        String currentUa = request.getHeader("User-Agent");
                        String currentIp = clientIp(request);

                        String storedUa = session.getUserAgent();
                        String storedIp = session.getIpAddress();

                        boolean uaMatch = storedUa == null || storedUa.equals(currentUa);
                        boolean ipMatch = true;

                        if (storedIp != null && currentIp != null && !storedIp.equals(currentIp)) {
                            // Check if first 2 segments of IP match (dynamic IP subnet check)
                            String[] storedParts = storedIp.split("\\.");
                            String[] currentParts = currentIp.split("\\.");
                            if (storedParts.length >= 2 && currentParts.length >= 2) {
                                ipMatch = storedParts[0].equals(currentParts[0]) && storedParts[1].equals(currentParts[1]);
                            }
                        }

                        if (!uaMatch || !ipMatch) {
                            // Session compromised - Auto-revoke and block request
                            session.setRevokedAt(LocalDateTime.now());
                            authSessionRepository.save(session);
                            
                            // Broadcast real-time threat alert
                            try {
                                SystemEvent securityAlert = SystemEvent.builder()
                                        .eventType("SECURITY_ALERT")
                                        .title("Session Compromise Blocked")
                                        .message("Admin session " + session.getId() + " (" + session.getUserEmail() + ") was compromised and auto-revoked.")
                                        .severity("CRITICAL")
                                        .payload(Map.of(
                                                "sessionId", session.getId(),
                                                "adminEmail", session.getUserEmail(),
                                                "incomingIp", currentIp,
                                                "incomingUa", currentUa
                                        ))
                                        .timestamp(LocalDateTime.now())
                                        .build();
                                sseEmitterService.broadcast(securityAlert);
                            } catch (Exception e) {
                                // Suppress to ensure filtering flows smoothly
                            }

                            SecurityContextHolder.clearContext();
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"message\":\"Security compromise detected: Session accessed from a different device or subnet.\"}");
                            return;
                        }
                    }
                }
            } catch (UsernameNotFoundException ignored) {
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
