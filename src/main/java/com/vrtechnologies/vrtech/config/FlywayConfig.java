package com.vrtechnologies.vrtech.config;

import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FlywayConfig {

    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy() {
        return flyway -> {
            // Repair schema history table (cleans up any failed migration records)
            flyway.repair();
            // Execute the migrations
            flyway.migrate();
        };
    }
}
