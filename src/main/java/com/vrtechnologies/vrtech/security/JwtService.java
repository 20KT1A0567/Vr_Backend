package com.vrtechnologies.vrtech.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.expiry-ms}")
    private long expiryMs;

    public String generateToken(UserDetails userDetails) {
        return generateAccessToken(userDetails);
    }

    public String generateAccessToken(UserDetails userDetails) {
        return generateAccessToken(userDetails, null);
    }

    public String generateAccessToken(UserDetails userDetails, Long sessionId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", userDetails.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList());
        if (sessionId != null) {
            claims.put("sid", sessionId);
        }
        return buildToken(claims, userDetails.getUsername(), expiryMs);
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Long extractSessionId(String token) {
        try {
            Object value = extractClaim(token, claims -> claims.get("sid"));
            if (value instanceof Number n) {
                return n.longValue();
            }
            if (value instanceof String s && !s.isBlank()) {
                return Long.parseLong(s);
            }
            return null;
        } catch (Exception ex) {
            return null;
        }
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        String username = extractUsername(token);
        return username.equalsIgnoreCase(userDetails.getUsername()) && !isTokenExpired(token);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private String buildToken(Map<String, Object> claims, String subject) {
        return buildToken(claims, subject, expiryMs);
    }

    private String buildToken(Map<String, Object> claims, String subject, long ttlMs) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + ttlMs);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    public LocalDateTime tokenExpiry(String token) {
        return toLocalDateTime(extractClaim(token, Claims::getExpiration).toInstant());
    }

    public LocalDateTime accessTokenExpiryFromNow() {
        return toLocalDateTime(Instant.now().plusMillis(expiryMs));
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Key getSigningKey() {
        // Use SHA-256 to hash the secret string into a 256-bit (32-byte) key.
        // This ensures compatibility with HS256 even if the provided secret is short.
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = digest.digest(secret.getBytes(StandardCharsets.UTF_8));
            return Keys.hmacShaKeyFor(keyBytes);
        } catch (java.security.NoSuchAlgorithmException e) {
            // Fallback for extreme cases, though SHA-256 is standard in JVM
            throw new RuntimeException("Failed to initialize JWT signing key", e);
        }
    }

    private LocalDateTime toLocalDateTime(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}
