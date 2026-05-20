package com.vrtechnologies.vrtech.service;

import com.vrtechnologies.vrtech.entity.AuthSession;
import com.vrtechnologies.vrtech.entity.User;
import com.vrtechnologies.vrtech.exception.BadRequestException;
import com.vrtechnologies.vrtech.repository.AuthSessionRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Objects;

@Service
public class AuthSessionService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final AuthSessionRepository authSessionRepository;

    @Value("${app.jwt.refresh-expiry-ms}")
    private long refreshExpiryMs;

    public AuthSessionService(AuthSessionRepository authSessionRepository) {
        this.authSessionRepository = authSessionRepository;
    }

    @Transactional
    public SessionToken createSession(User user) {
        String refreshToken = generateTokenValue();
        LocalDateTime now = LocalDateTime.now();

        AuthSession session = new AuthSession();
        session.setUserId(user.getId());
        session.setUserEmail(user.getEmail());
        session.setRefreshTokenHash(hash(refreshToken));
        session.setRefreshTokenExpiresAt(now.plusNanos(refreshExpiryMs * 1_000_000));
        session.setLastUsedAt(now);
        applyRequestMeta(session);
        session = authSessionRepository.save(session);

        return new SessionToken(session, refreshToken);
    }

    @Transactional(readOnly = true)
    public AuthSession requireActiveSession(String refreshToken) {
        AuthSession session = authSessionRepository.findByRefreshTokenHash(hash(refreshToken))
                .orElseThrow(() -> new BadRequestException("Invalid refresh token"));
        if (!session.isActive()) {
            throw new BadRequestException("Refresh token has expired or been revoked");
        }
        return session;
    }

    @Transactional
    public SessionToken rotateSession(String refreshToken) {
        AuthSession session = requireActiveSession(refreshToken);
        String nextToken = generateTokenValue();
        LocalDateTime now = LocalDateTime.now();
        session.setRefreshTokenHash(hash(nextToken));
        session.setRefreshTokenExpiresAt(now.plusNanos(refreshExpiryMs * 1_000_000));
        session.setLastUsedAt(now);
        applyRequestMeta(session);
        session = authSessionRepository.save(session);
        return new SessionToken(session, nextToken);
    }

    @Transactional(readOnly = true)
    public List<AuthSession> listActiveForUser(Long userId) {
        return authSessionRepository.findActiveByUserId(userId, LocalDateTime.now());
    }

    @Transactional
    public void revokeById(Long userId, Long sessionId) {
        AuthSession session = authSessionRepository.findById(sessionId)
                .orElseThrow(() -> new BadRequestException("Session not found"));
        if (!Objects.equals(session.getUserId(), userId)) {
            throw new BadRequestException("Session does not belong to this user");
        }
        if (session.getRevokedAt() != null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        session.setRevokedAt(now);
        session.setLogoutAt(now);
        authSessionRepository.save(session);
    }

    @Transactional
    public int revokeAllExcept(Long userId, Long keepSessionId) {
        return authSessionRepository.revokeAllExcept(userId, keepSessionId == null ? -1L : keepSessionId, LocalDateTime.now());
    }

    @Transactional
    public int revokeAllForUser(Long userId) {
        return authSessionRepository.revokeAllForUser(userId, LocalDateTime.now());
    }

    @Transactional
    public AuthSession revokeSession(String refreshToken) {
        AuthSession session = authSessionRepository.findByRefreshTokenHash(hash(refreshToken))
                .orElseThrow(() -> new BadRequestException("Invalid refresh token"));
        LocalDateTime now = LocalDateTime.now();
        if (session.getLogoutAt() == null) {
            session.setLogoutAt(now);
        }
        if (session.getRevokedAt() == null) {
            session.setRevokedAt(now);
        }
        session.setLastUsedAt(now);
        return authSessionRepository.save(session);
    }

    private void applyRequestMeta(AuthSession session) {
        HttpServletRequest request = currentRequest();
        if (request == null) {
            return;
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        session.setIpAddress(forwarded != null && !forwarded.isBlank() ? forwarded.split(",")[0].trim() : request.getRemoteAddr());
        session.setUserAgent(truncate(request.getHeader("User-Agent"), 512));
    }

    private HttpServletRequest currentRequest() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attributes != null ? attributes.getRequest() : null;
        } catch (Exception exception) {
            return null;
        }
    }

    private String generateTokenValue() {
        byte[] bytes = new byte[48];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String rawValue) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashedBytes = digest.digest(rawValue.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hashedBytes);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm is unavailable", exception);
        }
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    public record SessionToken(AuthSession session, String refreshToken) {
    }
}
