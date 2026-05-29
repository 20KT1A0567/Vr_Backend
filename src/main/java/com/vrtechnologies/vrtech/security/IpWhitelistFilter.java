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
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Optional;

/**
 * IP Whitelist Filter — Dynamic Access Perimeter Firewall
 *
 * Behaviour:
 *  1. If the request already carries a Bearer JWT token → SKIP IP check entirely.
 *     The token will be validated by JwtAuthFilter downstream. This means logged-in
 *     admins are never locked out due to dynamic IP changes.
 *  2. If no token is present AND the path is an admin/super-admin path AND
 *     adminAllowedIps is configured → enforce the IP whitelist.
 *  3. If adminAllowedIps is null/empty → allow all (Global Transit Mode).
 */
@Component
public class IpWhitelistFilter extends OncePerRequestFilter {

    private final SiteSettingsRepository siteSettingsRepository;
    private final ObjectMapper objectMapper;

    public IpWhitelistFilter(SiteSettingsRepository siteSettingsRepository, ObjectMapper objectMapper) {
        this.siteSettingsRepository = siteSettingsRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        // Only enforce on admin / super-admin paths
        if (!path.startsWith("/api/admin/") && !path.startsWith("/api/super-admin/")) {
            filterChain.doFilter(request, response);
            return;
        }

        // If the request already has a Bearer JWT token, the user is authenticated.
        // Skip IP check — they are already a verified admin session.
        // Dynamic IP changes will not lock out active sessions.
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // No token — check the IP whitelist (if configured)
        Optional<SiteSettings> settingsOpt = siteSettingsRepository.findTopByOrderByIdAsc();
        if (settingsOpt.isPresent()) {
            String allowedIpsRaw = settingsOpt.get().getAdminAllowedIps();
            if (allowedIpsRaw != null && !allowedIpsRaw.trim().isEmpty()) {
                String clientIp = resolveClientIp(request);
                boolean isAllowed = checkIp(clientIp, allowedIpsRaw);
                if (!isAllowed) {
                    response.setStatus(403);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    objectMapper.writeValue(
                            response.getWriter(),
                            ApiResponse.error("Access Denied: Your IP address is not whitelisted.", null)
                    );
                    return;
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private boolean checkIp(String clientIp, String allowedIpsRaw) {
        if (clientIp == null) {
            return false;
        }
        for (String rule : allowedIpsRaw.split(",")) {
            String trimmedRule = rule.trim();
            if (trimmedRule.isEmpty()) {
                continue;
            }
            if (trimmedRule.equals(clientIp)) {
                return true;
            }
            if (trimmedRule.contains("/")) {
                try {
                    if (ipInCidr(clientIp, trimmedRule)) {
                        return true;
                    }
                } catch (Exception ignored) {
                }
            }
        }
        return false;
    }

    private boolean ipInCidr(String ip, String cidr) throws UnknownHostException {
        String[] parts = cidr.split("/");
        if (parts.length != 2) {
            return false;
        }
        String subnetStr = parts[0];
        int prefixLength;
        try {
            prefixLength = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            return false;
        }

        InetAddress clientAddr = InetAddress.getByName(ip);
        InetAddress subnetAddr = InetAddress.getByName(subnetStr);

        byte[] clientBytes = clientAddr.getAddress();
        byte[] subnetBytes = subnetAddr.getAddress();

        if (clientBytes.length != subnetBytes.length) {
            return false;
        }

        int bytesToCheck = prefixLength / 8;
        int bitsToCheck = prefixLength % 8;

        for (int i = 0; i < bytesToCheck; i++) {
            if (clientBytes[i] != subnetBytes[i]) {
                return false;
            }
        }

        if (bitsToCheck > 0) {
            int mask = 0xff00 >>> bitsToCheck;
            mask = mask & 0xff;
            return (clientBytes[bytesToCheck] & mask) == (subnetBytes[bytesToCheck] & mask);
        }

        return true;
    }
}
