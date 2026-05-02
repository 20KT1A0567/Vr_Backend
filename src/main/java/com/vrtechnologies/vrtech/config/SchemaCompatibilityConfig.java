package com.vrtechnologies.vrtech.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class SchemaCompatibilityConfig {

    private static final Logger log = LoggerFactory.getLogger(SchemaCompatibilityConfig.class);

    @Bean
    @Order(0)
    CommandLineRunner normalizeLegacyEnumColumns(JdbcTemplate jdbcTemplate) {
        return args -> {
            migrate(jdbcTemplate, "ALTER TABLE users MODIFY COLUMN role VARCHAR(32) NOT NULL");
            migrate(jdbcTemplate, "ALTER TABLE users MODIFY COLUMN admin_status VARCHAR(32) NULL");
            migrate(jdbcTemplate, "ALTER TABLE users MODIFY COLUMN admin_role_key VARCHAR(64) NULL");

            migrate(jdbcTemplate, "ALTER TABLE admin_roles MODIFY COLUMN base_role VARCHAR(32) NOT NULL");

            migrate(jdbcTemplate, "ALTER TABLE role_permissions MODIFY COLUMN role VARCHAR(64) NOT NULL");
            migrate(jdbcTemplate, "ALTER TABLE role_permissions MODIFY COLUMN module VARCHAR(32) NOT NULL");
            migrate(jdbcTemplate, "ALTER TABLE role_permissions MODIFY COLUMN action VARCHAR(32) NOT NULL");

            migrate(jdbcTemplate, "ALTER TABLE admin_permissions MODIFY COLUMN module VARCHAR(32) NOT NULL");
            migrate(jdbcTemplate, "ALTER TABLE admin_permissions MODIFY COLUMN action VARCHAR(32) NOT NULL");

            migrate(jdbcTemplate, "ALTER TABLE admin_activity_logs MODIFY COLUMN module VARCHAR(32) NULL");
            migrate(jdbcTemplate, "ALTER TABLE admin_activity_logs MODIFY COLUMN action VARCHAR(32) NULL");

            migrate(jdbcTemplate, "ALTER TABLE orders MODIFY COLUMN status VARCHAR(32) NOT NULL DEFAULT 'PENDING'");
            migrate(jdbcTemplate, "ALTER TABLE orders MODIFY COLUMN delivery_type VARCHAR(32) NOT NULL DEFAULT 'PICKUP'");
            migrate(jdbcTemplate, "ALTER TABLE orders MODIFY COLUMN payment_method VARCHAR(32) NOT NULL DEFAULT 'CASH'");
            migrate(jdbcTemplate, "ALTER TABLE orders MODIFY COLUMN payment_status VARCHAR(32) NOT NULL DEFAULT 'PENDING'");
            migrate(jdbcTemplate, "ALTER TABLE payment_transactions MODIFY COLUMN gateway VARCHAR(24) NOT NULL");
            migrate(jdbcTemplate, "ALTER TABLE payment_transactions MODIFY COLUMN status VARCHAR(24) NOT NULL");
            migrate(jdbcTemplate, "ALTER TABLE order_timeline_events MODIFY COLUMN event_type VARCHAR(40) NOT NULL");

            migrate(jdbcTemplate, "ALTER TABLE enquiries MODIFY COLUMN status VARCHAR(32) NOT NULL DEFAULT 'NEW'");
            migrate(jdbcTemplate, "ALTER TABLE coupons MODIFY COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'");
            migrate(jdbcTemplate, "ALTER TABLE products MODIFY COLUMN product_condition VARCHAR(32) NULL");
        };
    }

    private void migrate(JdbcTemplate jdbcTemplate, String sql) {
        try {
            jdbcTemplate.execute(sql);
        } catch (Exception exception) {
            log.warn("Schema compatibility statement skipped or failed: {}", sql, exception);
        }
    }
}
