package com.vrtechnologies.vrtech.service;

import com.vrtechnologies.vrtech.entity.AdminEmailOtp;
import com.vrtechnologies.vrtech.entity.User;
import com.vrtechnologies.vrtech.exception.BadRequestException;
import com.vrtechnologies.vrtech.repository.AdminEmailOtpRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

@Service
public class AdminEmailOtpService {

    private static final Logger log = LoggerFactory.getLogger(AdminEmailOtpService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final AdminEmailOtpRepository otpRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.otp.length:6}")
    private int codeLength;

    @Value("${app.admin.otp.ttl-minutes:10}")
    private int ttlMinutes;

    @Value("${app.admin.otp.resend-cooldown-seconds:60}")
    private int resendCooldownSeconds;

    @Value("${app.admin.otp.max-attempts:5}")
    private int maxAttempts;

    @Value("${app.admin.otp.dev-log:false}")
    private boolean devLog;

    public AdminEmailOtpService(
            AdminEmailOtpRepository otpRepository,
            EmailService emailService,
            PasswordEncoder passwordEncoder
    ) {
        this.otpRepository = otpRepository;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
    }

    public record IssuedChallenge(String challengeId, String maskedEmail, int expiresInSeconds, int resendCooldownSeconds) {}

    @Transactional
    public IssuedChallenge issueChallenge(User admin, String ipAddress, String userAgent) {
        otpRepository.deleteByUserId(admin.getId());

        String code = generateCode();
        String challengeId = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        AdminEmailOtp otp = new AdminEmailOtp();
        otp.setChallengeId(challengeId);
        otp.setUserId(admin.getId());
        otp.setEmail(admin.getEmail());
        otp.setCodeHash(passwordEncoder.encode(code));
        otp.setExpiresAt(now.plusMinutes(Math.max(1, ttlMinutes)));
        otp.setLastSentAt(now);
        otp.setIpAddress(ipAddress);
        otp.setUserAgent(userAgent);
        otpRepository.save(otp);

        deliverCode(admin, code);

        return new IssuedChallenge(
                challengeId,
                maskEmail(admin.getEmail()),
                Math.max(60, ttlMinutes * 60),
                Math.max(15, resendCooldownSeconds)
        );
    }

    @Transactional
    public IssuedChallenge issueTotpChallenge(User admin, String ipAddress, String userAgent) {
        otpRepository.deleteByUserId(admin.getId());

        String challengeId = java.util.UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        AdminEmailOtp otp = new AdminEmailOtp();
        otp.setChallengeId(challengeId);
        otp.setUserId(admin.getId());
        otp.setEmail(admin.getEmail());
        otp.setCodeHash("TOTP-CHALLENGE");
        otp.setExpiresAt(now.plusMinutes(Math.max(1, ttlMinutes)));
        otp.setLastSentAt(now);
        otp.setIpAddress(ipAddress);
        otp.setUserAgent(userAgent);
        otpRepository.save(otp);

        return new IssuedChallenge(
                challengeId,
                "TOTP",
                Math.max(60, ttlMinutes * 60),
                Math.max(15, resendCooldownSeconds)
        );
    }

    @Transactional(readOnly = true)
    public Long peekUserId(String challengeId) {
        return otpRepository.findByChallengeId(challengeId)
                .map(AdminEmailOtp::getUserId)
                .orElseThrow(() -> new BadRequestException("Verification session expired. Please sign in again."));
    }

    @Transactional
    public void invalidateChallenge(String challengeId) {
        otpRepository.findByChallengeId(challengeId).ifPresent(otp -> {
            if (otp.getConsumedAt() == null) {
                otp.setConsumedAt(LocalDateTime.now());
                otpRepository.save(otp);
            }
        });
    }

    @Transactional
    public IssuedChallenge resend(String challengeId, User admin) {
        AdminEmailOtp otp = otpRepository.findByChallengeId(challengeId)
                .orElseThrow(() -> new BadRequestException("Verification session expired. Please sign in again."));
        if (!otp.getUserId().equals(admin.getId())) {
            throw new BadRequestException("Invalid verification session.");
        }
        if (otp.getConsumedAt() != null) {
            throw new BadRequestException("This verification code has already been used.");
        }
        Duration sinceLastSend = Duration.between(otp.getLastSentAt(), LocalDateTime.now());
        long cooldown = Math.max(15, resendCooldownSeconds);
        if (sinceLastSend.getSeconds() < cooldown) {
            long wait = cooldown - sinceLastSend.getSeconds();
            throw new BadRequestException("Please wait " + wait + "s before requesting another code.");
        }

        String code = generateCode();
        otp.setCodeHash(passwordEncoder.encode(code));
        otp.setExpiresAt(LocalDateTime.now().plusMinutes(Math.max(1, ttlMinutes)));
        otp.setLastSentAt(LocalDateTime.now());
        otp.setAttempts(0);
        otpRepository.save(otp);

        deliverCode(admin, code);

        return new IssuedChallenge(
                otp.getChallengeId(),
                maskEmail(admin.getEmail()),
                Math.max(60, ttlMinutes * 60),
                Math.max(15, resendCooldownSeconds)
        );
    }

    @Transactional
    public Long verify(String challengeId, String code) {
        if (challengeId == null || challengeId.isBlank() || code == null || code.isBlank()) {
            throw new BadRequestException("Verification code is required.");
        }
        AdminEmailOtp otp = otpRepository.findByChallengeId(challengeId)
                .orElseThrow(() -> new BadRequestException("Verification session expired. Please sign in again."));

        if (otp.getConsumedAt() != null) {
            throw new BadRequestException("This verification code has already been used.");
        }
        if (otp.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Verification code expired. Please request a new one.");
        }
        if (otp.getAttempts() >= maxAttempts) {
            throw new BadRequestException("Too many incorrect attempts. Please sign in again.");
        }

        boolean ok = passwordEncoder.matches(code.trim(), otp.getCodeHash());
        if (!ok) {
            otp.setAttempts(otp.getAttempts() + 1);
            otpRepository.save(otp);
            int remaining = Math.max(0, maxAttempts - otp.getAttempts());
            throw new BadRequestException("Invalid code. " + remaining + " attempt(s) remaining.");
        }

        otp.setConsumedAt(LocalDateTime.now());
        otpRepository.save(otp);
        return otp.getUserId();
    }

    private void deliverCode(User admin, String code) {
        String subject = "Your VR Technologies admin sign-in code";
        String html = renderEmail(admin.getName(), code, ttlMinutes);
        if (emailService.isConfigured()) {
            try {
                emailService.sendHtml(admin.getEmail(), subject, html);
            } catch (RuntimeException ex) {
                log.error("OTP delivery failed for {}: {}", admin.getEmail(), ex.getMessage());
                if (devLog) {
                    log.warn("[DEV] OTP for {} = {}", admin.getEmail(), code);
                }
                throw new BadRequestException("We could not send the verification email. Try again shortly.");
            }
        } else if (devLog) {
            log.warn("[DEV] Mail not configured. OTP for {} = {}", admin.getEmail(), code);
        } else {
            throw new BadRequestException("Email delivery is not configured on this server.");
        }
    }

    private String generateCode() {
        int len = Math.max(4, Math.min(codeLength, 8));
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append(RANDOM.nextInt(10));
        }
        return sb.toString();
    }

    private String maskEmail(String email) {
        if (email == null) return "";
        int at = email.indexOf('@');
        if (at <= 1) return email;
        String local = email.substring(0, at);
        String domain = email.substring(at);
        String visible = local.substring(0, Math.min(2, local.length()));
        return visible + "•".repeat(Math.max(2, local.length() - 2)) + domain;
    }

    private String renderEmail(String name, String code, int ttlMinutes) {
        String greeting = (name == null || name.isBlank()) ? "Hello," : "Hi " + escapeHtml(name) + ",";
        return """
            <!doctype html>
            <html lang="en"><body style="margin:0;padding:0;background:#0f172a;font-family:'Segoe UI',Roboto,Arial,sans-serif;color:#e2e8f0;">
              <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background:#0f172a;padding:40px 12px;">
                <tr><td align="center">
                  <table role="presentation" width="520" cellpadding="0" cellspacing="0" style="max-width:520px;background:linear-gradient(160deg,#111827 0%%,#1e293b 100%%);border:1px solid rgba(99,102,241,0.25);border-radius:18px;overflow:hidden;">
                    <tr><td style="padding:28px 32px;border-bottom:1px solid rgba(148,163,184,0.15);">
                      <div style="font-size:13px;letter-spacing:.18em;text-transform:uppercase;color:#a5b4fc;">VR Technologies · Admin Console</div>
                      <div style="margin-top:6px;font-size:22px;font-weight:600;color:#f8fafc;">Two-step verification</div>
                    </td></tr>
                    <tr><td style="padding:28px 32px;">
                      <p style="margin:0 0 12px;font-size:15px;line-height:1.55;">%s</p>
                      <p style="margin:0 0 18px;font-size:15px;line-height:1.55;color:#cbd5f5;">Use the code below to finish signing in to the admin console. The code expires in <strong style="color:#f8fafc;">%d minutes</strong>.</p>
                      <div style="margin:24px 0 28px;text-align:center;">
                        <div style="display:inline-block;padding:18px 28px;background:rgba(15,23,42,0.6);border:1px solid rgba(99,102,241,0.45);border-radius:14px;letter-spacing:.45em;font-size:30px;font-weight:700;color:#f8fafc;font-family:'SFMono-Regular',Menlo,Consolas,monospace;">%s</div>
                      </div>
                      <p style="margin:0;font-size:13px;line-height:1.6;color:#94a3b8;">If you did not try to sign in, you can ignore this email — your account is safe. For security, never share this code with anyone, including VR Technologies staff.</p>
                    </td></tr>
                    <tr><td style="padding:18px 32px;background:rgba(15,23,42,0.4);border-top:1px solid rgba(148,163,184,0.12);font-size:12px;color:#64748b;">
                      Sent automatically by the VR Technologies admin platform.
                    </td></tr>
                  </table>
                </td></tr>
              </table>
            </body></html>
            """.formatted(greeting, ttlMinutes, code);
    }

    private String escapeHtml(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    @SuppressWarnings("unused")
    private static String normalizeForCompare(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}
