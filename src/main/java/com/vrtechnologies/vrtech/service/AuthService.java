package com.vrtechnologies.vrtech.service;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.vrtechnologies.vrtech.dto.request.LoginRequest;
import com.vrtechnologies.vrtech.dto.request.LogoutRequest;
import com.vrtechnologies.vrtech.dto.request.PhoneSendRequest;
import com.vrtechnologies.vrtech.dto.request.PhoneVerifyRequest;
import com.vrtechnologies.vrtech.dto.request.RefreshTokenRequest;
import com.vrtechnologies.vrtech.dto.request.RegisterRequest;
import com.vrtechnologies.vrtech.dto.request.TwoFactorBackupRequest;
import com.vrtechnologies.vrtech.dto.request.TwoFactorResendRequest;
import com.vrtechnologies.vrtech.dto.request.TwoFactorVerifyRequest;
import com.vrtechnologies.vrtech.dto.response.AuthResponse;
import com.vrtechnologies.vrtech.dto.response.LoginOutcome;
import com.vrtechnologies.vrtech.dto.response.PhoneSendResponse;
import com.vrtechnologies.vrtech.dto.response.TwoFactorChallengeResponse;
import com.vrtechnologies.vrtech.entity.User;
import com.vrtechnologies.vrtech.entity.enums.AdminStatus;
import com.vrtechnologies.vrtech.entity.enums.Module;
import com.vrtechnologies.vrtech.entity.enums.PermissionAction;
import com.vrtechnologies.vrtech.entity.enums.Role;
import com.vrtechnologies.vrtech.exception.BadRequestException;
import com.vrtechnologies.vrtech.repository.UserRepository;
import com.vrtechnologies.vrtech.security.JwtService;
import com.vrtechnologies.vrtech.security.TotpUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final int MAX_ADMIN_FAILED_ATTEMPTS = 5;
    private static final int ADMIN_LOCK_MINUTES = 30;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtService jwtService;
    private final UserContextService userContextService;
    private final AdminLoginHistoryService loginHistoryService;
    private final AdminActivityLogService activityLogService;
    private final AuthSessionService authSessionService;
    private final PermissionService permissionService;
    private final AdminEmailOtpService adminEmailOtpService;
    private final AdminBackupCodeService backupCodeService;

    @Value("${app.admin.otp.required-roles:SUPER_ADMIN}")
    private String otpRequiredRolesProperty;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            UserDetailsService userDetailsService,
            JwtService jwtService,
            UserContextService userContextService,
            AdminLoginHistoryService loginHistoryService,
            AdminActivityLogService activityLogService,
            AuthSessionService authSessionService,
            PermissionService permissionService,
            AdminEmailOtpService adminEmailOtpService,
            AdminBackupCodeService backupCodeService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.jwtService = jwtService;
        this.userContextService = userContextService;
        this.loginHistoryService = loginHistoryService;
        this.activityLogService = activityLogService;
        this.authSessionService = authSessionService;
        this.permissionService = permissionService;
        this.adminEmailOtpService = adminEmailOtpService;
        this.backupCodeService = backupCodeService;
    }

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.getEmail())) {
            throw new BadRequestException("Email already exists");
        }

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail().trim().toLowerCase());
        user.setPhone(request.getPhone());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.USER);
        user = userRepository.save(user);

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        AuthSessionService.SessionToken sessionToken = authSessionService.createSession(user);
        String accessToken = jwtService.generateAccessToken(userDetails);
        return toAuthResponse(user, accessToken, sessionToken);
    }

    public LoginOutcome login(LoginRequest request) {
        return login(request, null, null);
    }

    public LoginOutcome login(LoginRequest request, String ipAddress, String userAgent) {
        String email = request.getEmail() == null ? "" : request.getEmail().trim();
        userRepository.findByEmailIgnoreCase(email)
                .filter(user -> user.getRole() != Role.USER)
                .ifPresent(this::ensureAdminNotLocked);

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.getPassword())
            );
        } catch (BadCredentialsException ex) {
            recordAdminFailureIfApplicable(email, "Invalid credentials");
            throw new BadRequestException("Invalid credentials");
        } catch (AuthenticationException ex) {
            recordAdminFailureIfApplicable(email, ex.getMessage());
            throw new BadRequestException("Invalid credentials");
        }

        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new BadRequestException("Invalid credentials"));

        ensureUserCanAuthenticate(user);

        if (user.getRole() != Role.USER && requiresTwoFactor(user)) {
            if (user.isTotpEnabled()) {
                AdminEmailOtpService.IssuedChallenge challenge = adminEmailOtpService.issueTotpChallenge(user, ipAddress, userAgent);
                activityLogService.log(user, Module.DASHBOARD, PermissionAction.VIEW, "Auth", user.getId(),
                        null, null, "Admin TOTP challenge issued: " + user.getEmail());
                return LoginOutcome.twoFactor(TwoFactorChallengeResponse.builder()
                        .challengeId(challenge.challengeId())
                        .maskedEmail(challenge.maskedEmail())
                        .expiresInSeconds(challenge.expiresInSeconds())
                        .resendCooldownSeconds(challenge.resendCooldownSeconds())
                        .message("Please enter the code from your authenticator app.")
                        .build());
            } else {
                AdminEmailOtpService.IssuedChallenge challenge = adminEmailOtpService.issueChallenge(user, ipAddress, userAgent);
                activityLogService.log(user, Module.DASHBOARD, PermissionAction.VIEW, "Auth", user.getId(),
                        null, null, "Admin email 2FA challenge issued: " + user.getEmail());
                return LoginOutcome.twoFactor(TwoFactorChallengeResponse.builder()
                        .challengeId(challenge.challengeId())
                        .maskedEmail(challenge.maskedEmail())
                        .expiresInSeconds(challenge.expiresInSeconds())
                        .resendCooldownSeconds(challenge.resendCooldownSeconds())
                        .message("A verification code has been sent to your email.")
                        .build());
            }
        }

        return LoginOutcome.authenticated(finalizeAdminLogin(user));
    }

    public AuthResponse verifyTwoFactor(TwoFactorVerifyRequest request) {
        Long userId = adminEmailOtpService.peekUserId(request.getChallengeId());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("Account not found"));
        ensureUserCanAuthenticate(user);

        if (user.isTotpEnabled()) {
            boolean ok = TotpUtil.verifyCode(user.getTotpSecret(), request.getCode(), 1);
            if (!ok) {
                throw new BadRequestException("Invalid code. Please try again.");
            }
            adminEmailOtpService.invalidateChallenge(request.getChallengeId());
        } else {
            adminEmailOtpService.verify(request.getChallengeId(), request.getCode());
        }

        return finalizeAdminLogin(user);
    }

    public AuthResponse verifyBackupCode(TwoFactorBackupRequest request, String ipAddress) {
        Long userId = adminEmailOtpService.peekUserId(request.getChallengeId());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("Verification session expired. Please sign in again."));

        boolean ok = backupCodeService.redeem(user.getId(), request.getBackupCode(), ipAddress);
        if (!ok) {
            activityLogService.log(user, Module.DASHBOARD, PermissionAction.VIEW, "Auth", user.getId(),
                    null, null, "Invalid backup code attempt: " + user.getEmail());
            throw new BadRequestException("Invalid or already-used backup code.");
        }

        adminEmailOtpService.invalidateChallenge(request.getChallengeId());
        ensureUserCanAuthenticate(user);
        activityLogService.log(user, Module.DASHBOARD, PermissionAction.VIEW, "Auth", user.getId(),
                null, null, "Admin login via backup code: " + user.getEmail());
        return finalizeAdminLogin(user);
    }

    public TwoFactorChallengeResponse resendTwoFactor(TwoFactorResendRequest request) {
        Long userId = adminEmailOtpService.peekUserId(request.getChallengeId());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("Verification session expired. Please sign in again."));
        AdminEmailOtpService.IssuedChallenge challenge = adminEmailOtpService.resend(request.getChallengeId(), user);
        return TwoFactorChallengeResponse.builder()
                .challengeId(challenge.challengeId())
                .maskedEmail(challenge.maskedEmail())
                .expiresInSeconds(challenge.expiresInSeconds())
                .resendCooldownSeconds(challenge.resendCooldownSeconds())
                .message("A new verification code has been sent.")
                .build();
    }

    private AuthResponse finalizeAdminLogin(User user) {
        AuthSessionService.SessionToken sessionToken = authSessionService.createSession(user);
        if (user.getRole() != Role.USER) {
            user.setLastLoginAt(LocalDateTime.now());
            user.setFailedLoginAttempts(0);
            user.setLockedUntil(null);
            user = userRepository.save(user);
            loginHistoryService.recordSuccess(user, sessionToken.session().getId());
            activityLogService.log(user, Module.DASHBOARD, PermissionAction.VIEW, "Auth", user.getId(),
                    null, null, "Admin login: " + user.getEmail());
        }
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String accessToken = jwtService.generateAccessToken(userDetails, sessionToken.session().getId());
        return toAuthResponse(user, accessToken, sessionToken);
    }

    private boolean requiresTwoFactor(User user) {
        if (user.isTwoFactorEnabled()) {
            return true;
        }
        Set<String> required = parseRequiredOtpRoles();
        if (required.contains("ALL")) {
            return user.getRole() != Role.USER;
        }
        return required.contains(user.getRole().name());
    }

    private Set<String> parseRequiredOtpRoles() {
        Set<String> result = new HashSet<>();
        if (otpRequiredRolesProperty == null || otpRequiredRolesProperty.isBlank()) {
            return result;
        }
        Arrays.stream(otpRequiredRolesProperty.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> s.toUpperCase(Locale.ROOT))
                .forEach(result::add);
        return result;
    }

    public AuthResponse verifyPhone(PhoneVerifyRequest request) {
        return firebaseCustomerLogin(request.getIdToken(), request.getSessionInfo());
    }

    public AuthResponse firebaseCustomerLogin(String firebaseIdToken) {
        return firebaseCustomerLogin(firebaseIdToken, null);
    }

    private AuthResponse firebaseCustomerLogin(String firebaseIdToken, String sessionInfo) {
        if (FirebaseApp.getApps().isEmpty()) {
            throw new BadRequestException("Phone login is not configured on this server");
        }

        FirebaseToken decoded;
        try {
            decoded = FirebaseAuth.getInstance().verifyIdToken(firebaseIdToken);
        } catch (FirebaseAuthException ex) {
            log.warn("Firebase phone token verification failed: {}. SessionInfo: {}", ex.getMessage(), sessionInfo);
            throw new BadRequestException("Invalid OTP token");
        }

        String phone = (String) decoded.getClaims().get("phone_number");
        if (phone == null || phone.isBlank()) {
            throw new BadRequestException("Phone number not present in verification token");
        }

        User user = userRepository.findByPhone(phone).orElseGet(() -> createPhoneUser(phone));

        if (user.getRole() != Role.USER) {
            throw new BadRequestException("Admin accounts must sign in with email and password");
        }
        if (!user.isActive()) {
            throw new BadRequestException("Your account is disabled");
        }

        user.setLastLoginAt(LocalDateTime.now());
        user = userRepository.save(user);

        AuthSessionService.SessionToken sessionToken = authSessionService.createSession(user);
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String accessToken = jwtService.generateAccessToken(userDetails);
        return toAuthResponse(user, accessToken, sessionToken);
    }

    public PhoneSendResponse sendPhoneOtp(PhoneSendRequest request) {
        log.info("Registering Firebase OTP attempt for {}", request.getPhone());
        // The actual SMS is sent by Firebase Web SDK in the browser. This endpoint
        // keeps the website/backend auth flow aligned and gives us an audit point.
        return PhoneSendResponse.builder()
                .verificationId("internal-" + generateRandomSecret().substring(0, 8))
                .sessionInfo("firebase-session-placeholder")
                .token("placeholder-token")
                .build();
    }

    private User createPhoneUser(String phone) {
        User user = new User();
        user.setPhone(phone);
        user.setName("Customer");
        user.setEmail(phone);
        user.setPassword(passwordEncoder.encode(generateRandomSecret()));
        user.setRole(Role.USER);
        user.setActive(true);
        return userRepository.save(user);
    }

    private String generateRandomSecret() {

        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public AuthResponse refresh(RefreshTokenRequest request) {
        var existingSession = authSessionService.requireActiveSession(request.getRefreshToken());
        User user = userRepository.findById(existingSession.getUserId())
                .orElseThrow(() -> new BadRequestException("User session is no longer valid"));
        ensureUserCanAuthenticate(user);
        AuthSessionService.SessionToken rotatedSession = authSessionService.rotateSession(request.getRefreshToken());
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String accessToken = jwtService.generateAccessToken(userDetails, rotatedSession.session().getId());
        return toAuthResponse(user, accessToken, rotatedSession);
    }

    public void logout(LogoutRequest request) {
        var session = authSessionService.revokeSession(request.getRefreshToken());
        User user = userRepository.findById(session.getUserId()).orElse(null);
        if (user != null && user.getRole() != Role.USER) {
            loginHistoryService.recordLogout(session.getId());
            activityLogService.log(user, Module.DASHBOARD, PermissionAction.VIEW, "Auth", user.getId(),
                    null, null, "Admin logout: " + user.getEmail());
        }
    }

    public AuthResponse me() {
        User user = userContextService.getCurrentUser();
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        return toAuthResponse(user, jwtService.generateAccessToken(userDetails), null);
    }

    private void ensureUserCanAuthenticate(User user) {
        if (user.getRole() == Role.USER) {
            if (!user.isActive()) {
                throw new BadRequestException("Your account is disabled");
            }
            return;
        }
        enforceAdminLoginRules(user);
    }

    private void enforceAdminLoginRules(User user) {
        ensureAdminNotLocked(user);
        AdminStatus status = user.effectiveAdminStatus();
        if (status == AdminStatus.DISABLED || !user.isActive()) {
            recordAdminFailure(user, "Account disabled");
            throw new BadRequestException("Your account is disabled");
        }
        if (status == AdminStatus.SUSPENDED) {
            recordAdminFailure(user, "Account suspended");
            throw new BadRequestException("Your account is suspended");
        }

        LocalDate today = LocalDate.now();
        if (user.getAccessStartDate() != null && today.isBefore(user.getAccessStartDate())) {
            recordAdminFailure(user, "Access not started");
            throw new BadRequestException("Account access has not started yet");
        }
        if (user.getAccessEndDate() != null && today.isAfter(user.getAccessEndDate())) {
            recordAdminFailure(user, "Access expired");
            throw new BadRequestException("Account access has expired");
        }

        if (user.getAllowedLoginStartTime() != null && user.getAllowedLoginEndTime() != null) {
            LocalTime now = LocalTime.now();
            LocalTime start = user.getAllowedLoginStartTime();
            LocalTime end = user.getAllowedLoginEndTime();
            boolean withinWindow;
            if (start.equals(end)) {
                withinWindow = true;
            } else if (start.isBefore(end)) {
                withinWindow = !now.isBefore(start) && !now.isAfter(end);
            } else {
                withinWindow = !now.isBefore(start) || !now.isAfter(end);
            }
            if (!withinWindow) {
                recordAdminFailure(user, "Outside allowed login hours");
                throw new BadRequestException("Login attempts are only allowed between "
                        + start + " and " + end);
            }
        }

        EnumSet<DayOfWeek> allowedDays = user.allowedLoginDaysSet();
        if (!allowedDays.isEmpty()) {
            DayOfWeek currentDay = LocalDate.now().getDayOfWeek();
            if (!allowedDays.contains(currentDay)) {
                recordAdminFailure(user, "Login not allowed on " + currentDay);
                throw new BadRequestException("Login is not allowed on " + formatDay(currentDay)
                        + ". Allowed days: " + formatDays(allowedDays));
            }
        }
    }

    private String formatDay(DayOfWeek day) {
        String name = day.name();
        return name.charAt(0) + name.substring(1).toLowerCase(Locale.ROOT);
    }

    private String formatDays(EnumSet<DayOfWeek> days) {
        StringBuilder sb = new StringBuilder();
        for (DayOfWeek d : days) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(formatDay(d));
        }
        return sb.toString();
    }

    private void ensureAdminNotLocked(User user) {
        if (user.getLockedUntil() == null) {
            return;
        }
        if (LocalDateTime.now().isBefore(user.getLockedUntil())) {
            loginHistoryService.recordFailure(user.getEmail(), "Account locked after failed attempts");
            throw new BadRequestException("Too many failed login attempts. Try again after " + user.getLockedUntil());
        }
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        userRepository.save(user);
    }

    private void recordAdminFailure(User user, String reason) {
        registerFailedAdminAttempt(user);
        loginHistoryService.recordFailure(user.getEmail(), reason);
        activityLogService.log(user, Module.DASHBOARD, PermissionAction.VIEW, "Auth", user.getId(),
                null, null, "Admin login failure: " + reason);
    }

    private void recordAdminFailureIfApplicable(String email, String reason) {
        if (email.isBlank()) {
            return;
        }
        userRepository.findByEmailIgnoreCase(email).ifPresent(user -> {
            if (user.getRole() != Role.USER) {
                registerFailedAdminAttempt(user);
                loginHistoryService.recordFailure(email, reason);
            }
        });
    }

    private void registerFailedAdminAttempt(User user) {
        int attempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(attempts);
        if (attempts >= MAX_ADMIN_FAILED_ATTEMPTS) {
            user.setLockedUntil(LocalDateTime.now().plusMinutes(ADMIN_LOCK_MINUTES));
        }
        userRepository.save(user);
    }

    private AuthResponse toAuthResponse(User user, String accessToken, AuthSessionService.SessionToken sessionToken) {
        AuthResponse.AuthResponseBuilder builder = AuthResponse.builder()
                .token(accessToken)
                .refreshToken(sessionToken != null ? sessionToken.refreshToken() : null)
                .id(user.getId())
                .name(user.getName())
                .email(publicEmail(user))
                .phone(user.getPhone())
                .role(user.getRole())
                .sessionId(sessionToken != null ? sessionToken.session().getId() : null)
                .tokenExpiresAt(jwtService.tokenExpiry(accessToken))
                .refreshTokenExpiresAt(sessionToken != null ? sessionToken.session().getRefreshTokenExpiresAt() : null);

        if (user.getRole() != Role.USER) {
            String roleKey = permissionService.resolveRoleKey(user);
            builder.roleKey(roleKey);
            try {
                builder.roleName(permissionService.requireRole(roleKey).getDisplayName());
            } catch (Exception ignored) {
                builder.roleName(user.getRole().name());
            }
            builder.visibleModules(permissionService.visibleModules(user).stream().map(Enum::name).toList());

            List<AuthResponse.PermissionSummary> permissions = new ArrayList<>();
            for (Map.Entry<Module, EnumSet<PermissionAction>> entry : permissionService.effectivePermissions(user).entrySet()) {
                for (PermissionAction action : entry.getValue()) {
                    permissions.add(AuthResponse.PermissionSummary.builder()
                            .module(entry.getKey().name())
                            .action(action.name())
                            .build());
                }
            }
            builder.permissions(permissions);
        } else {
            builder.roleKey(null);
            builder.roleName(null);
            builder.visibleModules(List.of());
            builder.permissions(List.of());
        }

        return builder.build();
    }

    private String publicEmail(User user) {
        if (user == null || user.getEmail() == null) {
            return null;
        }
        if (user.getRole() == Role.USER && isInternalPhoneLoginEmail(user)) {
            return null;
        }
        return user.getEmail();
    }

    private boolean isInternalPhoneLoginEmail(User user) {
        String email = user.getEmail().toLowerCase(Locale.ROOT);
        String phone = user.getPhone() == null ? "" : user.getPhone().toLowerCase(Locale.ROOT);
        if (!phone.isBlank() && email.equals(phone)) {
            return true;
        }
        int atIndex = email.lastIndexOf('@');
        if (atIndex < 0 || atIndex == email.length() - 1) {
            return false;
        }
        String domain = email.substring(atIndex + 1);
        return domain.endsWith(".local");
    }

    public java.util.Map<String, String> setupTotp() {
        User user = userContextService.getCurrentUser();
        String secretKey = TotpUtil.generateSecretKey();
        user.setTotpSecret(secretKey);
        userRepository.save(user);

        String qrCodeUrl = TotpUtil.getQrCodeUrl(secretKey, user.getEmail(), "VR Technologies");

        java.util.Map<String, String> response = new java.util.HashMap<>();
        response.put("secret", secretKey);
        response.put("qrCodeUrl", qrCodeUrl);
        return response;
    }

    public void enableTotp(String code) {
        User user = userContextService.getCurrentUser();
        if (user.getTotpSecret() == null || user.getTotpSecret().isBlank()) {
            throw new BadRequestException("TOTP setup has not been initialized. Please call setup first.");
        }
        boolean verified = TotpUtil.verifyCode(user.getTotpSecret(), code, 1);
        if (!verified) {
            throw new BadRequestException("Invalid verification code. Please try again.");
        }
        user.setTwoFactorEnabled(true);
        userRepository.save(user);
        activityLogService.log(user, Module.DASHBOARD, PermissionAction.UPDATE, "Auth", user.getId(),
                "2FA=false", "2FA=true", "TOTP MFA enabled successfully");
    }

    public void disableTotp(String code) {
        User user = userContextService.getCurrentUser();
        if (user.getTotpSecret() == null || user.getTotpSecret().isBlank()) {
            throw new BadRequestException("TOTP is not enabled.");
        }
        boolean verified = TotpUtil.verifyCode(user.getTotpSecret(), code, 1);
        if (!verified) {
            throw new BadRequestException("Invalid verification code. Please try again.");
        }
        user.setTwoFactorEnabled(false);
        user.setTotpSecret(null);
        userRepository.save(user);
        activityLogService.log(user, Module.DASHBOARD, PermissionAction.UPDATE, "Auth", user.getId(),
                "2FA=true", "2FA=false", "TOTP MFA disabled successfully");
    }
}
