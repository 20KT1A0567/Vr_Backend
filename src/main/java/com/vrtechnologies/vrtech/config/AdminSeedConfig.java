
package com.vrtechnologies.vrtech.config;

import com.vrtechnologies.vrtech.entity.User;
import com.vrtechnologies.vrtech.entity.enums.AdminStatus;
import com.vrtechnologies.vrtech.entity.enums.Role;
import com.vrtechnologies.vrtech.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AdminSeedConfig {

    private static final Logger log = LoggerFactory.getLogger(AdminSeedConfig.class);

    @Bean
    @Order(2)
    public CommandLineRunner seedAdminUser(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.admin.seed-enabled}") boolean seedEnabled,
            @Value("${app.admin.seed-email}") String adminEmail,
            @Value("${app.admin.seed-password}") String adminPassword,
            @Value("${app.admin.seed-name}") String adminName,
            @Value("${app.admin.seed-overwrite:true}") boolean seedOverwrite
    ) {
        return args -> {
            if (!seedEnabled) {
                return;
            }
            if (adminEmail == null || adminEmail.isBlank() || adminPassword == null || adminPassword.isBlank() || adminName == null || adminName.isBlank()) {
                throw new IllegalStateException("Super admin seed is enabled, but APP_ADMIN_SEED_EMAIL, APP_ADMIN_SEED_PASSWORD, and APP_ADMIN_SEED_NAME are not all set");
            }

            String normalizedEmail = adminEmail.trim().toLowerCase();
            User admin = userRepository.findByEmailIgnoreCase(normalizedEmail).orElse(null);

            if (admin == null) {
                admin = new User();
                admin.setEmail(normalizedEmail);
                admin.setName(adminName);
                admin.setPassword(passwordEncoder.encode(adminPassword));
                admin.setRole(Role.SUPER_ADMIN);
                admin.setAdminRoleKey(Role.SUPER_ADMIN.name());
                admin.setActive(true);
                admin.setAdminStatus(AdminStatus.ACTIVE);
                admin.setTwoFactorEnabled(true);
                admin.setFailedLoginAttempts(0);
                admin.setLockedUntil(null);
                userRepository.save(admin);
                log.info("Seeded SUPER_ADMIN account: {}", normalizedEmail);
                return;
            }

            if (!seedOverwrite) {
                log.info("SUPER_ADMIN seed already present ({}), overwrite disabled — leaving as-is", normalizedEmail);
                return;
            }

            boolean changed = false;
            if (admin.getRole() != Role.SUPER_ADMIN) {
                admin.setRole(Role.SUPER_ADMIN);
                admin.setAdminRoleKey(Role.SUPER_ADMIN.name());
                changed = true;
            }
            if (admin.getAdminRoleKey() == null || admin.getAdminRoleKey().isBlank()) {
                admin.setAdminRoleKey(Role.SUPER_ADMIN.name());
                changed = true;
            }
            if (!admin.isActive() || admin.effectiveAdminStatus() != AdminStatus.ACTIVE) {
                admin.setActive(true);
                admin.setAdminStatus(AdminStatus.ACTIVE);
                changed = true;
            }
            if (!admin.isTwoFactorEnabled()) {
                admin.setTwoFactorEnabled(true);
                changed = true;
            }
            if (!passwordEncoder.matches(adminPassword, admin.getPassword())) {
                admin.setPassword(passwordEncoder.encode(adminPassword));
                changed = true;
            }
            if (admin.getName() == null || admin.getName().isBlank()) {
                admin.setName(adminName);
                changed = true;
            }
            if (admin.getLockedUntil() != null || admin.getFailedLoginAttempts() > 0) {
                admin.setLockedUntil(null);
                admin.setFailedLoginAttempts(0);
                changed = true;
            }

            if (changed) {
                userRepository.save(admin);
                log.info("Refreshed SUPER_ADMIN seed for {} (password/role/2FA reset to seed values)", normalizedEmail);
            } else {
                log.info("SUPER_ADMIN seed already up-to-date for {}", normalizedEmail);
            }
        };
    }
}
