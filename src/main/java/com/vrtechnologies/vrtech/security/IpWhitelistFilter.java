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

        if (path.startsWith("/api/admin/") || path.startsWith("/api/super-admin/")) {
            Optional<SiteSettings> settingsOpt = siteSettingsRepository.findTopByOrderByIdAsc();
            if (settingsOpt.isPresent()) {
                String allowedIpsRaw = settingsOpt.get().getAdminAllowedIps();
                if (allowedIpsRaw != null && !allowedIpsRaw.trim().isEmpty()) {
                    String clientIp = clientIp(request);
                    boolean isAllowed = checkIp(clientIp, allowedIpsRaw);

                    if (!isAllowed) {
                        response.setStatus(403);
                        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                        objectMapper.writeValue(response.getWriter(), ApiResponse.error("Access Denied: Your IP address is not whitelisted.", null));
                        return;
                    }
                }
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
