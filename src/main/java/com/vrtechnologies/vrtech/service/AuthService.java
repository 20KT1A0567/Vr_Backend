package com.vrtechnologies.vrtech.service;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.vrtechnologies.vrtech.dto.request.LoginRequest;
import com.vrtechnologies.vrtech.dto.request.LogoutRequest;
import com.vrtechnologies.vrtech.dto.request.PhoneVerifyRequest;
import com.vrtechnologies.vrtech.dto.request.RefreshTokenRequest;
import com.vrtechnologies.vrtech.dto.request.RegisterRequest;
import com.vrtechnologies.vrtech.dto.response.AuthResponse;
import com.vrtechnologies.vrtech.entity.User;
import com.vrtechnologies.vrtech.entity.enums.AdminStatus;
import com.vrtechnologies.vrtech.entity.enums.Module;
import com.vrtechnologies.vrtech.entity.enums.PermissionAction;
import com.vrtechnologies.vrtech.entity.enums.Role;
import com.vrtechnologies.vrtech.exception.BadRequestException;
import com.vrtechnologies.vrtech.repository.UserRepository;
import com.vrtechnologies.vrtech.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

@Service
public class AuthService {

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
            PermissionService permissionService
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

    public AuthResponse login(LoginRequest request) {
        String email = request.getEmail() == null ? "" : request.getEmail().trim();

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

        AuthSessionService.SessionToken sessionToken = authSessionService.createSession(user);
        if (user.getRole() != Role.USER) {
            user.setLastLoginAt(LocalDateTime.now());
            user = userRepository.save(user);
            loginHistoryService.recordSuccess(user, sessionToken.session().getId());
            activityLogService.log(user, Module.DASHBOARD, PermissionAction.VIEW, "Auth", user.getId(),
                    null, null, "Admin login: " + user.getEmail());
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String accessToken = jwtService.generateAccessToken(userDetails);
        return toAuthResponse(user, accessToken, sessionToken);
    }

    public AuthResponse verifyPhone(PhoneVerifyRequest request) {
        if (FirebaseApp.getApps().isEmpty()) {
            throw new BadRequestException("Phone login is not configured on this server");
        }

        FirebaseToken decoded;
        try {
            decoded = FirebaseAuth.getInstance().verifyIdToken(request.getIdToken());
        } catch (FirebaseAuthException ex) {
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

    private User createPhoneUser(String phone) {
        User user = new User();
        user.setPhone(phone);
        user.setName("Customer");
        user.setEmail(phone + "@phone.anushabazaar.local");
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
        String accessToken = jwtService.generateAccessToken(userDetails);
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
    }

    private void recordAdminFailure(User user, String reason) {
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
                loginHistoryService.recordFailure(email, reason);
            }
        });
    }

    private AuthResponse toAuthResponse(User user, String accessToken, AuthSessionService.SessionToken sessionToken) {
        AuthResponse.AuthResponseBuilder builder = AuthResponse.builder()
                .token(accessToken)
                .refreshToken(sessionToken != null ? sessionToken.refreshToken() : null)
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
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
}
