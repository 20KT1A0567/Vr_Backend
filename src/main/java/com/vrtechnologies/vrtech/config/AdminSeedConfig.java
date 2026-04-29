package com.vrtechnologies.vrtech.config;

import com.vrtechnologies.vrtech.entity.User;
import com.vrtechnologies.vrtech.entity.enums.Role;
import com.vrtechnologies.vrtech.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AdminSeedConfig {

    @Bean
    public CommandLineRunner seedAdminUser(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.admin.seed-enabled}") boolean seedEnabled,
            @Value("${app.admin.seed-email}") String adminEmail,
            @Value("${app.admin.seed-password}") String adminPassword,
            @Value("${app.admin.seed-name}") String adminName
    ) {
        return args -> {
            if (!seedEnabled || userRepository.existsByEmailIgnoreCase(adminEmail)) {
                return;
            }

            User admin = new User();
            admin.setName(adminName);
            admin.setEmail(adminEmail.trim().toLowerCase());
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setRole(Role.ADMIN);
            admin.setActive(true);
            userRepository.save(admin);
        };
    }
}
