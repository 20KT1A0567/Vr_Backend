package com.vrtechnologies.vrtech.config;

import com.vrtechnologies.vrtech.entity.AdminRole;
import com.vrtechnologies.vrtech.entity.enums.Role;
import com.vrtechnologies.vrtech.repository.AdminRoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import java.util.LinkedHashMap;
import java.util.Map;

@Configuration
public class AdminRoleSeedConfig {

    @Bean
    @Order(1)
    CommandLineRunner adminRoleSeeder(AdminRoleRepository adminRoleRepository) {
        return args -> {
            Map<Role, String> descriptions = new LinkedHashMap<>();
            descriptions.put(Role.SUPER_ADMIN, "Full platform access with control over administrators, roles, and every operational module.");
            descriptions.put(Role.ADMIN, "Legacy all-access operational role retained for backward compatibility.");
            descriptions.put(Role.MANAGER, "Full business operations with product, order, enquiry, and reporting access.");
            descriptions.put(Role.STORE_MANAGER, "Scoped day-to-day control for assigned stores.");
            descriptions.put(Role.SALES_EXECUTIVE, "Sales, customer, and enquiry handling for frontline commerce operations.");
            descriptions.put(Role.SUPPORT_AGENT, "Customer support and service workflow access.");
            descriptions.put(Role.INVENTORY_MANAGER, "Inventory, catalog structure, and stock control access.");
            descriptions.put(Role.CONTENT_MANAGER, "Website content and merchandising control.");
            descriptions.put(Role.ACCOUNTANT, "Read-heavy financial and reporting access.");

            for (Map.Entry<Role, String> entry : descriptions.entrySet()) {
                String roleKey = entry.getKey().name();
                AdminRole role = adminRoleRepository.findById(roleKey).orElseGet(AdminRole::new);
                role.setRoleKey(roleKey);
                role.setDisplayName(formatDisplayName(entry.getKey()));
                role.setDescription(entry.getValue());
                role.setBaseRole(entry.getKey());
                role.setActive(true);
                role.setSystemRole(true);
                role.setProtectedRole(true);
                adminRoleRepository.save(role);
            }
        };
    }

    private String formatDisplayName(Role role) {
        return switch (role) {
            case SUPER_ADMIN -> "Super Admin";
            case STORE_MANAGER -> "Store Manager";
            case SALES_EXECUTIVE -> "Sales Executive";
            case SUPPORT_AGENT -> "Support Agent";
            case INVENTORY_MANAGER -> "Inventory Manager";
            case CONTENT_MANAGER -> "Content Manager";
            default -> {
                String normalized = role.name().replace('_', ' ').toLowerCase();
                yield Character.toUpperCase(normalized.charAt(0)) + normalized.substring(1);
            }
        };
    }
}
