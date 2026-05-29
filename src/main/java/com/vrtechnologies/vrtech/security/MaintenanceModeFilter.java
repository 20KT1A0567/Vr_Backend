package com.vrtechnologies.vrtech.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vrtechnologies.vrtech.dto.response.ApiResponse;
import com.vrtechnologies.vrtech.entity.SiteSettings;
import com.vrtechnologies.vrtech.repository.SiteSettingsRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Component
public class MaintenanceModeFilter extends OncePerRequestFilter {

    private final SiteSettingsRepository siteSettingsRepository;
    private final ObjectMapper objectMapper;

    public MaintenanceModeFilter(SiteSettingsRepository siteSettingsRepository, ObjectMapper objectMapper) {
        this.siteSettingsRepository = siteSettingsRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String path = request.getRequestURI();

        if (isBypassed(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        Optional<SiteSettings> settingsOpt = siteSettingsRepository.findTopByOrderByIdAsc();
        if (settingsOpt.isPresent() && settingsOpt.get().isMaintenanceModeActive()) {
            if (!isAdminUser()) {
                response.setStatus(503);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                objectMapper.writeValue(response.getWriter(), ApiResponse.error("Service Temporarily Unavailable: The system is undergoing scheduled maintenance.", null));
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isBypassed(String path) {
        if (path.equals("/actuator/health") || path.equals("/sitemap.xml") || path.equals("/robots.txt")) {
            return true;
        }
        if (path.startsWith("/api/admin/") || path.startsWith("/api/super-admin/")) {
            return true;
        }
        if (path.equals("/api/settings/public")) {
            return true;
        }
        if (path.startsWith("/api/payments/webhooks/") || path.startsWith("/api/courier/webhooks/")) {
            return true;
        }
        if (path.equals("/api/auth/login") || path.equals("/api/auth/logout") ||
            path.equals("/api/auth/me") || path.equals("/api/auth/refresh") ||
            path.startsWith("/api/auth/2fa/")) {
            return true;
        }
        return false;
    }

    private boolean isAdminUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            return auth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .anyMatch(authority -> authority.startsWith("ROLE_") && !authority.equals("ROLE_USER"));
        }
        return false;
    }
}
