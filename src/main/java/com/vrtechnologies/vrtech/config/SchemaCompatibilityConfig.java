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
            migrate(jdbcTemplate, "ALTER TABLE users ADD COLUMN failed_login_attempts INT NOT NULL DEFAULT 0");
            migrate(jdbcTemplate, "ALTER TABLE users ADD COLUMN locked_until DATETIME NULL");

            migrate(jdbcTemplate, "ALTER TABLE admin_roles MODIFY COLUMN base_role VARCHAR(32) NOT NULL");
            migrate(jdbcTemplate, "UPDATE admin_roles SET protected_role = 0, system_role = 0 WHERE role_key <> 'SUPER_ADMIN'");
            migrate(jdbcTemplate, "UPDATE admin_roles SET protected_role = 1, system_role = 1, active = 1 WHERE role_key = 'SUPER_ADMIN'");

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
            migrate(jdbcTemplate, "ALTER TABLE orders ADD COLUMN tax_amount DECIMAL(10,2) NOT NULL DEFAULT 0");
            migrate(jdbcTemplate, "ALTER TABLE orders ADD COLUMN delivery_charge DECIMAL(10,2) NOT NULL DEFAULT 0");
            migrate(jdbcTemplate, "ALTER TABLE orders ADD COLUMN delivery_state VARCHAR(80) NULL");
            migrate(jdbcTemplate, "ALTER TABLE orders MODIFY COLUMN user_id BIGINT NULL");
            migrate(jdbcTemplate, "ALTER TABLE orders ADD COLUMN guest_checkout BIT NOT NULL DEFAULT 0");
            migrate(jdbcTemplate, "ALTER TABLE payment_transactions MODIFY COLUMN gateway VARCHAR(24) NOT NULL");
            migrate(jdbcTemplate, "ALTER TABLE payment_transactions MODIFY COLUMN status VARCHAR(24) NOT NULL");
            migrate(jdbcTemplate, "ALTER TABLE payment_transactions ADD COLUMN refund_id VARCHAR(128) NULL");
            migrate(jdbcTemplate, "ALTER TABLE payment_transactions ADD COLUMN refunded_amount DECIMAL(10,2) NULL");
            migrate(jdbcTemplate, "ALTER TABLE payment_transactions ADD COLUMN refund_reason TEXT NULL");
            migrate(jdbcTemplate, "ALTER TABLE payment_transactions ADD COLUMN refund_status VARCHAR(32) NULL");
            migrate(jdbcTemplate, "ALTER TABLE order_timeline_events MODIFY COLUMN event_type VARCHAR(40) NOT NULL");

            migrate(jdbcTemplate, "ALTER TABLE enquiries MODIFY COLUMN status VARCHAR(32) NOT NULL DEFAULT 'NEW'");
            migrate(jdbcTemplate, "ALTER TABLE coupons MODIFY COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'");
            migrate(jdbcTemplate, "ALTER TABLE products MODIFY COLUMN product_condition VARCHAR(32) NULL");
            migrate(jdbcTemplate, "ALTER TABLE product_reviews MODIFY COLUMN status VARCHAR(24) NOT NULL DEFAULT 'PENDING'");
            migrate(jdbcTemplate, "ALTER TABLE site_settings ADD COLUMN pickup_enabled BIT NOT NULL DEFAULT 1");
            migrate(jdbcTemplate, "ALTER TABLE site_settings ADD COLUMN delivery_enabled BIT NOT NULL DEFAULT 1");
            migrate(jdbcTemplate, "ALTER TABLE site_settings ADD COLUMN standard_delivery_charge DECIMAL(10,2) NOT NULL DEFAULT 0");
            migrate(jdbcTemplate, "ALTER TABLE site_settings ADD COLUMN free_delivery_threshold DECIMAL(10,2) NULL");
            migrate(jdbcTemplate, "ALTER TABLE site_settings ADD COLUMN state_delivery_charges VARCHAR(2000) NULL");
            migrate(jdbcTemplate, "ALTER TABLE site_settings ADD COLUMN state_delivery_windows VARCHAR(2000) NULL");
            migrate(jdbcTemplate, "ALTER TABLE site_settings ADD COLUMN estimated_delivery_days INT NOT NULL DEFAULT 5");
            migrate(jdbcTemplate, "ALTER TABLE site_settings ADD COLUMN gst_enabled BIT NOT NULL DEFAULT 1");
            migrate(jdbcTemplate, "ALTER TABLE site_settings ADD COLUMN gst_rate DECIMAL(5,2) NOT NULL DEFAULT 18");
            migrate(jdbcTemplate, "ALTER TABLE site_settings ADD COLUMN gst_number VARCHAR(40) NULL");
            migrate(jdbcTemplate, "ALTER TABLE site_settings ADD COLUMN logo_url VARCHAR(500) NULL");
            migrate(jdbcTemplate, "ALTER TABLE site_settings ADD COLUMN favicon_url VARCHAR(500) NULL");
            migrate(jdbcTemplate, "ALTER TABLE site_settings ADD COLUMN tagline VARCHAR(180) NULL");
            migrate(jdbcTemplate, "ALTER TABLE site_settings ADD COLUMN footer_description VARCHAR(1000) NULL");
            migrate(jdbcTemplate, "ALTER TABLE site_settings ADD COLUMN facebook_url VARCHAR(500) NULL");
            migrate(jdbcTemplate, "ALTER TABLE site_settings ADD COLUMN instagram_url VARCHAR(500) NULL");
            migrate(jdbcTemplate, "ALTER TABLE site_settings ADD COLUMN x_url VARCHAR(500) NULL");
            migrate(jdbcTemplate, "ALTER TABLE site_settings ADD COLUMN linkedin_url VARCHAR(500) NULL");
            migrate(jdbcTemplate, "ALTER TABLE site_settings ADD COLUMN youtube_url VARCHAR(500) NULL");

            migrate(jdbcTemplate, "CREATE TABLE IF NOT EXISTS back_in_stock_requests (id BIGINT NOT NULL AUTO_INCREMENT, product_id BIGINT NOT NULL, email VARCHAR(255) NOT NULL, phone VARCHAR(255) NULL, status VARCHAR(30) NOT NULL DEFAULT 'WAITING', created_at DATETIME NOT NULL, updated_at DATETIME NOT NULL, PRIMARY KEY (id))");
            migrate(jdbcTemplate, "ALTER TABLE back_in_stock_requests ADD COLUMN product_id BIGINT NOT NULL");
            migrate(jdbcTemplate, "ALTER TABLE back_in_stock_requests ADD COLUMN email VARCHAR(255) NOT NULL");
            migrate(jdbcTemplate, "ALTER TABLE back_in_stock_requests ADD COLUMN phone VARCHAR(255) NULL");
            migrate(jdbcTemplate, "ALTER TABLE back_in_stock_requests ADD COLUMN status VARCHAR(30) NOT NULL DEFAULT 'WAITING'");
            migrate(jdbcTemplate, "ALTER TABLE back_in_stock_requests ADD COLUMN created_at DATETIME NOT NULL");
            migrate(jdbcTemplate, "ALTER TABLE back_in_stock_requests ADD COLUMN updated_at DATETIME NOT NULL");

            migrate(jdbcTemplate, "ALTER TABLE notification_logs ADD COLUMN attempts INT NOT NULL DEFAULT 0");
            migrate(jdbcTemplate, "ALTER TABLE notification_logs ADD COLUMN max_attempts INT NOT NULL DEFAULT 3");
            migrate(jdbcTemplate, "ALTER TABLE notification_logs ADD COLUMN next_attempt_at DATETIME NULL");
            migrate(jdbcTemplate, "ALTER TABLE notification_logs ADD COLUMN sent_at DATETIME NULL");
            migrate(jdbcTemplate, "ALTER TABLE notification_logs ADD COLUMN last_error TEXT NULL");

            migrate(jdbcTemplate, "CREATE TABLE IF NOT EXISTS product_store_stock (id BIGINT NOT NULL AUTO_INCREMENT, product_id BIGINT NOT NULL, store_id BIGINT NOT NULL, stock_quantity INT NOT NULL DEFAULT 0, created_at DATETIME NOT NULL, updated_at DATETIME NOT NULL, PRIMARY KEY (id), UNIQUE KEY uk_product_store_stock (product_id, store_id))");

            migrate(jdbcTemplate, "CREATE TABLE IF NOT EXISTS payment_webhook_events (id BIGINT NOT NULL AUTO_INCREMENT, gateway_event_id VARCHAR(128) NULL UNIQUE, gateway VARCHAR(64) NOT NULL DEFAULT 'RAZORPAY', event_type VARCHAR(80) NULL, status VARCHAR(32) NULL, gateway_order_id VARCHAR(128) NULL, gateway_payment_id VARCHAR(128) NULL, user_agent VARCHAR(255) NULL, payload LONGTEXT NULL, error_message TEXT NULL, processed_at DATETIME NULL, created_at DATETIME NOT NULL, updated_at DATETIME NOT NULL, PRIMARY KEY (id))");
            migrate(jdbcTemplate, "CREATE TABLE IF NOT EXISTS refund_transactions (id BIGINT NOT NULL AUTO_INCREMENT, order_id BIGINT NOT NULL, payment_transaction_id BIGINT NULL, refund_id VARCHAR(128) NULL, amount DECIMAL(10,2) NOT NULL DEFAULT 0, status VARCHAR(32) NOT NULL DEFAULT 'RECORDED', reason TEXT NULL, refunded_at DATETIME NULL, created_at DATETIME NOT NULL, updated_at DATETIME NOT NULL, PRIMARY KEY (id))");
            migrate(jdbcTemplate, "CREATE TABLE IF NOT EXISTS recently_viewed_products (id BIGINT NOT NULL AUTO_INCREMENT, user_id BIGINT NULL, product_id BIGINT NOT NULL, anonymous_id VARCHAR(128) NULL, created_at DATETIME NOT NULL, updated_at DATETIME NOT NULL, PRIMARY KEY (id))");
            migrate(jdbcTemplate, "CREATE TABLE IF NOT EXISTS price_drop_alerts (id BIGINT NOT NULL AUTO_INCREMENT, product_id BIGINT NOT NULL, email VARCHAR(255) NOT NULL, phone VARCHAR(40) NULL, target_price DECIMAL(10,2) NULL, status VARCHAR(24) NOT NULL DEFAULT 'WAITING', notified_at DATETIME NULL, created_at DATETIME NOT NULL, updated_at DATETIME NOT NULL, PRIMARY KEY (id))");
            migrate(jdbcTemplate, "ALTER TABLE return_requests ADD COLUMN pickup_scheduled_at DATETIME NULL");
            migrate(jdbcTemplate, "ALTER TABLE return_requests ADD COLUMN picked_up_at DATETIME NULL");
            migrate(jdbcTemplate, "ALTER TABLE return_requests ADD COLUMN inspected_at DATETIME NULL");
            migrate(jdbcTemplate, "ALTER TABLE return_requests ADD COLUMN pickup_agent VARCHAR(120) NULL");
            migrate(jdbcTemplate, "ALTER TABLE return_requests ADD COLUMN pickup_tracking_number VARCHAR(80) NULL");
            migrate(jdbcTemplate, "ALTER TABLE return_requests ADD COLUMN inspection_note TEXT NULL");

            migrate(jdbcTemplate, "ALTER TABLE products ADD COLUMN hsn_code VARCHAR(40) NULL");
            migrate(jdbcTemplate, "ALTER TABLE products ADD COLUMN gst_rate_percent DECIMAL(5,2) NULL");
            migrate(jdbcTemplate, "ALTER TABLE products ADD COLUMN taxable BIT NOT NULL DEFAULT 1");
            migrate(jdbcTemplate, "ALTER TABLE categories ADD COLUMN seo_title VARCHAR(255) NULL");
            migrate(jdbcTemplate, "ALTER TABLE categories ADD COLUMN seo_description TEXT NULL");
            migrate(jdbcTemplate, "ALTER TABLE categories ADD COLUMN seo_keywords TEXT NULL");
            migrate(jdbcTemplate, "ALTER TABLE categories ADD COLUMN og_image_url VARCHAR(255) NULL");
            migrate(jdbcTemplate, "ALTER TABLE categories ADD COLUMN canonical_url VARCHAR(255) NULL");
            migrate(jdbcTemplate, "CREATE TABLE IF NOT EXISTS cms_pages (id BIGINT NOT NULL AUTO_INCREMENT, slug VARCHAR(80) NOT NULL UNIQUE, title VARCHAR(160) NOT NULL, meta_title VARCHAR(200) NULL, meta_description TEXT NULL, eyebrow VARCHAR(80) NULL, hero_title VARCHAR(200) NULL, hero_description TEXT NULL, body TEXT NULL, sections_json LONGTEXT NULL, faq_items_json LONGTEXT NULL, active BIT NOT NULL DEFAULT 1, created_at DATETIME NOT NULL, updated_at DATETIME NOT NULL, PRIMARY KEY (id))");
            migrate(jdbcTemplate, "CREATE TABLE IF NOT EXISTS navigation_items (id BIGINT NOT NULL AUTO_INCREMENT, menu_location VARCHAR(20) NOT NULL, label VARCHAR(120) NOT NULL, url VARCHAR(255) NOT NULL, visible BIT NOT NULL DEFAULT 1, sort_order INT NOT NULL DEFAULT 0, created_at DATETIME NOT NULL, updated_at DATETIME NOT NULL, PRIMARY KEY (id))");
            migrate(jdbcTemplate, "CREATE TABLE IF NOT EXISTS seo_settings (id BIGINT NOT NULL AUTO_INCREMENT, target_type VARCHAR(40) NOT NULL, target_id BIGINT NULL, target_slug VARCHAR(120) NULL, page_title VARCHAR(220) NULL, meta_description VARCHAR(500) NULL, meta_keywords VARCHAR(1000) NULL, og_image_url VARCHAR(500) NULL, canonical_url VARCHAR(500) NULL, no_index BIT NOT NULL DEFAULT 0, sitemap_enabled BIT NOT NULL DEFAULT 1, created_at DATETIME NOT NULL, updated_at DATETIME NOT NULL, PRIMARY KEY (id))");
            migrate(jdbcTemplate, "ALTER TABLE site_settings ADD COLUMN homepage_builder_json LONGTEXT NULL");
        };
    }

    private void migrate(JdbcTemplate jdbcTemplate, String sql) {
        try {
            jdbcTemplate.execute(sql);
        } catch (Exception exception) {
            if (isDuplicateColumn(exception)) {
                log.debug("Schema compatibility column already exists; skipped: {}", sql);
                return;
            }
            log.warn("Schema compatibility statement skipped or failed: {} ({})", sql, rootMessage(exception));
        }
    }

    private boolean isDuplicateColumn(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            String message = current.getMessage();
            if (message != null && message.toLowerCase().contains("duplicate column")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage();
    }
}
