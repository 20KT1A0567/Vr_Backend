package com.vrtechnologies.vrtech.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vrtechnologies.vrtech.entity.User;
import com.vrtechnologies.vrtech.entity.WebAuthnCredential;
import com.vrtechnologies.vrtech.exception.BadRequestException;
import com.vrtechnologies.vrtech.repository.UserRepository;
import com.vrtechnologies.vrtech.repository.WebAuthnCredentialRepository;
import com.yubico.webauthn.*;
import com.yubico.webauthn.data.*;
import com.yubico.webauthn.exception.AssertionFailedException;
import com.yubico.webauthn.exception.RegistrationFailedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class WebAuthnService {

    private final WebAuthnCredentialRepository credentialRepo;
    private final UserRepository userRepository;
    private final RelyingParty relyingParty;
    private final ObjectMapper objectMapper;

    // In-memory challenge stores (production: use Redis with 5-min TTL)
    private final Map<String, PublicKeyCredentialCreationOptions> registrationChallenges   = new ConcurrentHashMap<>();
    private final Map<String, AssertionRequest>                   authenticationChallenges = new ConcurrentHashMap<>();

    public WebAuthnService(WebAuthnCredentialRepository credentialRepo,
                           UserRepository userRepository,
                           ObjectMapper objectMapper,
                           @org.springframework.beans.factory.annotation.Value("${app.webauthn.rp.id:localhost}") String rpId,
                           @org.springframework.beans.factory.annotation.Value("${app.webauthn.rp.name:VR Technologies Admin}") String rpName) {
        this.credentialRepo = credentialRepo;
        this.userRepository = userRepository;
        this.objectMapper   = objectMapper;

        RelyingPartyIdentity rpIdentity = RelyingPartyIdentity.builder()
                .id(rpId)
                .name(rpName)
                .build();

        this.relyingParty = RelyingParty.builder()
                .identity(rpIdentity)
                .credentialRepository(new VrCredentialRepository(credentialRepo, userRepository))
                .allowOriginPort(true)
                .allowOriginSubdomain(false)
                .build();
    }

    // ─────────────────────────── REGISTRATION ──────────────────────────────

    public String startRegistration(User user) throws Exception {
        byte[] userHandleBytes = longToBytes(user.getId());
        UserIdentity userIdentity = UserIdentity.builder()
                .name(user.getEmail() != null ? user.getEmail() : user.getName())
                .displayName(user.getName())
                .id(new ByteArray(userHandleBytes))
                .build();

        StartRegistrationOptions options = StartRegistrationOptions.builder()
                .user(userIdentity)
                .build();

        PublicKeyCredentialCreationOptions creationOptions = relyingParty.startRegistration(options);
        String sessionKey = UUID.randomUUID().toString();
        registrationChallenges.put(sessionKey, creationOptions);

        return objectMapper.writeValueAsString(
                Map.of("sessionKey", sessionKey, "options", creationOptions.toJson())
        );
    }

    @Transactional
    public void finishRegistration(User user, String sessionKey, String credentialJson, String nickname) throws Exception {
        PublicKeyCredentialCreationOptions options = registrationChallenges.remove(sessionKey);
        if (options == null) throw new BadRequestException("Registration session expired or not found. Please try again.");

        PublicKeyCredential<AuthenticatorAttestationResponse, ClientRegistrationExtensionOutputs> pkc =
                PublicKeyCredential.parseRegistrationResponseJson(credentialJson);

        RegistrationResult result;
        try {
            result = relyingParty.finishRegistration(FinishRegistrationOptions.builder()
                    .request(options)
                    .response(pkc)
                    .build());
        } catch (RegistrationFailedException e) {
            throw new BadRequestException("Passkey registration failed: " + e.getMessage());
        }

        // In Yubico webauthn-server-core 2.x, getSignatureCount() is on RegistrationResult
        long initialSignCount;
        try {
            initialSignCount = result.getSignatureCount();
        } catch (Exception e) {
            initialSignCount = 0L;
        }

        WebAuthnCredential credential = WebAuthnCredential.builder()
                .userId(user.getId())
                .credentialId(result.getKeyId().getId().getBase64Url())
                .publicKeyCose(result.getPublicKeyCose().getBase64Url())
                .signCount(initialSignCount)
                .nickname(nickname != null && !nickname.isBlank() ? nickname.trim() : "Passkey")
                .createdAt(LocalDateTime.now())
                .build();

        credentialRepo.save(credential);
    }

    // ──────────────────────── AUTHENTICATION ───────────────────────────────

    public String startAuthentication() throws Exception {
        AssertionRequest assertionRequest = relyingParty.startAssertion(StartAssertionOptions.builder().build());
        String sessionKey = UUID.randomUUID().toString();
        authenticationChallenges.put(sessionKey, assertionRequest);
        return objectMapper.writeValueAsString(
                Map.of("sessionKey", sessionKey, "options", assertionRequest.toJson())
        );
    }

    @Transactional
    public User finishAuthentication(String sessionKey, String assertionJson) throws Exception {
        AssertionRequest assertionRequest = authenticationChallenges.remove(sessionKey);
        if (assertionRequest == null) throw new BadRequestException("Authentication session expired. Please try again.");

        PublicKeyCredential<AuthenticatorAssertionResponse, ClientAssertionExtensionOutputs> pkc =
                PublicKeyCredential.parseAssertionResponseJson(assertionJson);

        AssertionResult result;
        try {
            result = relyingParty.finishAssertion(FinishAssertionOptions.builder()
                    .request(assertionRequest)
                    .response(pkc)
                    .build());
        } catch (AssertionFailedException e) {
            throw new BadRequestException("Fingerprint verification failed: " + e.getMessage());
        }

        if (!result.isSuccess()) throw new BadRequestException("Passkey authentication was not successful.");

        // Update last used time (sign count is handled by the library internally via CredentialRepository)
        String credentialId = result.getCredential().getCredentialId().getBase64Url();
        credentialRepo.findByCredentialId(credentialId).ifPresent(cred -> {
            cred.setLastUsedAt(LocalDateTime.now());
            credentialRepo.save(cred);
        });

        Long userId = bytesToLong(result.getCredential().getUserHandle().getBytes());
        return userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("User not found."));
    }

    // ──────────────────────── MANAGEMENT ───────────────────────────────────

    public List<WebAuthnCredential> listCredentials(Long userId) {
        return credentialRepo.findByUserId(userId);
    }

    @Transactional
    public void deleteCredential(Long id, Long userId) {
        credentialRepo.deleteByIdAndUserId(id, userId);
    }

    // ──────────────────────── HELPERS ──────────────────────────────────────

    private static byte[] longToBytes(long value) {
        byte[] bytes = new byte[8];
        for (int i = 7; i >= 0; i--) { bytes[i] = (byte)(value & 0xFF); value >>= 8; }
        return bytes;
    }

    private static long bytesToLong(byte[] bytes) {
        long value = 0;
        for (byte b : bytes) { value = (value << 8) | (b & 0xFF); }
        return value;
    }

    // ─────────────────── CREDENTIAL REPOSITORY IMPL ────────────────────────

    private record VrCredentialRepository(
            WebAuthnCredentialRepository credentialRepo,
            UserRepository userRepository
    ) implements CredentialRepository {

        private ByteArray fromBase64Url(String base64Url) {
            try {
                return ByteArray.fromBase64Url(base64Url);
            } catch (Exception e) {
                throw new RuntimeException("Failed to decode base64url data", e);
            }
        }

        @Override
        public Set<PublicKeyCredentialDescriptor> getCredentialIdsForUsername(String username) {
            Set<PublicKeyCredentialDescriptor> result = new HashSet<>();
            userRepository.findByEmailIgnoreCase(username).ifPresent(user ->
                credentialRepo.findByUserId(user.getId()).forEach(cred ->
                    result.add(PublicKeyCredentialDescriptor.builder()
                            .id(fromBase64Url(cred.getCredentialId()))
                            .build()))
            );
            return result;
        }

        @Override
        public Optional<ByteArray> getUserHandleForUsername(String username) {
            return userRepository.findByEmailIgnoreCase(username)
                    .map(u -> new ByteArray(longToBytes(u.getId())));
        }

        @Override
        public Optional<String> getUsernameForUserHandle(ByteArray userHandle) {
            long userId = bytesToLong(userHandle.getBytes());
            return userRepository.findById(userId).map(User::getEmail);
        }

        @Override
        public Optional<RegisteredCredential> lookup(ByteArray credentialId, ByteArray userHandle) {
            return credentialRepo.findByCredentialId(credentialId.getBase64Url())
                    .map(cred -> RegisteredCredential.builder()
                            .credentialId(fromBase64Url(cred.getCredentialId()))
                            .userHandle(userHandle)
                            .publicKeyCose(fromBase64Url(cred.getPublicKeyCose()))
                            .signatureCount(cred.getSignCount())   // v2.x uses signatureCount()
                            .build());
        }

        @Override
        public Set<RegisteredCredential> lookupAll(ByteArray credentialId) {
            Set<RegisteredCredential> result = new HashSet<>();
            credentialRepo.findByCredentialId(credentialId.getBase64Url()).ifPresent(cred ->
                result.add(RegisteredCredential.builder()
                        .credentialId(fromBase64Url(cred.getCredentialId()))
                        .userHandle(new ByteArray(longToBytes(cred.getUserId())))
                        .publicKeyCose(fromBase64Url(cred.getPublicKeyCose()))
                        .signatureCount(cred.getSignCount())   // v2.x uses signatureCount()
                        .build())
            );
            return result;
        }

        private static byte[] longToBytes(long value) {
            byte[] bytes = new byte[8];
            for (int i = 7; i >= 0; i--) { bytes[i] = (byte)(value & 0xFF); value >>= 8; }
            return bytes;
        }

        private static long bytesToLong(byte[] bytes) {
            long value = 0;
            for (byte b : bytes) { value = (value << 8) | (b & 0xFF); }
            return value;
        }
    }
}
