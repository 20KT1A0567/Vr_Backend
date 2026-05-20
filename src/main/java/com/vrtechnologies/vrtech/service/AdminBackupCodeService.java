package com.vrtechnologies.vrtech.service;

import com.vrtechnologies.vrtech.entity.AdminBackupCode;
import com.vrtechnologies.vrtech.entity.User;
import com.vrtechnologies.vrtech.exception.BadRequestException;
import com.vrtechnologies.vrtech.repository.AdminBackupCodeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class AdminBackupCodeService {

    private static final Logger log = LoggerFactory.getLogger(AdminBackupCodeService.class);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_GROUP_LENGTH = 4;
    private static final int CODE_GROUPS = 2;
    private static final int CODES_PER_BATCH = 10;

    private final AdminBackupCodeRepository repository;
    private final PasswordEncoder passwordEncoder;

    public AdminBackupCodeService(AdminBackupCodeRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public BackupCodeStatus status(User user) {
        long active = repository.countActive(user.getId());
        long total = repository.findByUserIdOrderByCreatedAtAsc(user.getId()).size();
        return new BackupCodeStatus(active, total);
    }

    @Transactional
    public List<String> regenerate(User user) {
        repository.deleteByUserId(user.getId());

        List<String> plain = new ArrayList<>(CODES_PER_BATCH);
        for (int i = 0; i < CODES_PER_BATCH; i++) {
            String code = generateCode();
            plain.add(code);

            AdminBackupCode entity = new AdminBackupCode();
            entity.setUserId(user.getId());
            entity.setCodeHash(passwordEncoder.encode(normalize(code)));
            entity.setLabel(code.substring(0, 2));
            repository.save(entity);
        }
        log.info("Regenerated {} backup codes for user {}", CODES_PER_BATCH, user.getEmail());
        return plain;
    }

    @Transactional
    public boolean redeem(Long userId, String code, String ipAddress) {
        if (code == null || code.isBlank()) {
            return false;
        }
        String normalized = normalize(code);
        List<AdminBackupCode> codes = repository.findByUserIdOrderByCreatedAtAsc(userId);
        for (AdminBackupCode candidate : codes) {
            if (candidate.getConsumedAt() != null) {
                continue;
            }
            if (passwordEncoder.matches(normalized, candidate.getCodeHash())) {
                candidate.setConsumedAt(java.time.LocalDateTime.now());
                candidate.setConsumedIp(truncate(ipAddress, 64));
                repository.save(candidate);
                return true;
            }
        }
        return false;
    }

    private String generateCode() {
        StringBuilder sb = new StringBuilder(CODE_GROUPS * (CODE_GROUP_LENGTH + 1));
        for (int g = 0; g < CODE_GROUPS; g++) {
            if (g > 0) sb.append('-');
            for (int i = 0; i < CODE_GROUP_LENGTH; i++) {
                sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
            }
        }
        return sb.toString();
    }

    private String normalize(String code) {
        return code.replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
    }

    private String truncate(String value, int max) {
        if (value == null) return null;
        return value.length() <= max ? value : value.substring(0, max);
    }

    public record BackupCodeStatus(long active, long total) {
        public boolean exists() {
            return total > 0;
        }
    }

    @SuppressWarnings("unused")
    public void enforceMustExistOrThrow(User user) {
        if (status(user).active <= 0) {
            throw new BadRequestException("No backup codes remain. Please regenerate.");
        }
    }
}
