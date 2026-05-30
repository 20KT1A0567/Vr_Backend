package com.vrtechnologies.vrtech;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.admin.SpringApplicationAdminJmxAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication(exclude = SpringApplicationAdminJmxAutoConfiguration.class)
public class VrTechApplication {

    public static void main(String[] args) {
        loadEnv();
        SpringApplication.run(VrTechApplication.class, args);
    }

    private static void loadEnv() {
        try {
            java.io.File envFile = new java.io.File(".env");
            if (!envFile.exists()) {
                envFile = new java.io.File("../.env");
            }
            if (envFile.exists()) {
                java.nio.file.Files.lines(envFile.toPath())
                    .map(String::trim)
                    .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                    .forEach(line -> {
                        int index = line.indexOf('=');
                        if (index > 0) {
                            String key = line.substring(0, index).trim();
                            String value = line.substring(index + 1).trim();
                            if (value.startsWith("\"") && value.endsWith("\"") && value.length() >= 2) {
                                value = value.substring(1, value.length() - 1);
                            } else if (value.startsWith("'") && value.endsWith("'") && value.length() >= 2) {
                                value = value.substring(1, value.length() - 1);
                            }
                            System.setProperty(key, value);
                        }
                    });
            }
        } catch (Exception e) {
            System.err.println("Could not load .env file: " + e.getMessage());
        }
    }
}
