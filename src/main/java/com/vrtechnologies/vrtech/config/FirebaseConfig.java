package com.vrtechnologies.vrtech.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Configuration
public class FirebaseConfig {

    private static final Logger log = LoggerFactory.getLogger(FirebaseConfig.class);

    @Value("${app.firebase.credentials-path:}")
    private String credentialsPath;

    @Value("${app.firebase.credentials-json:}")
    private String credentialsJson;

    @Value("${app.firebase.project-id:}")
    private String projectId;

    @PostConstruct
    public void init() {
        if (!FirebaseApp.getApps().isEmpty()) {
            return;
        }

        try {
            if (credentialsJson != null && !credentialsJson.isBlank()) {
                try (InputStream stream = credentialsJsonStream(credentialsJson)) {
                    initialize(stream);
                    return;
                }
            }

            if (credentialsPath == null || credentialsPath.isBlank()) {
                log.warn("Firebase credentials are not set; phone OTP login will be unavailable");
                return;
            }

            Resource resource = new DefaultResourceLoader().getResource(credentialsPath);
            if (!resource.exists()) {
                log.warn("Firebase credentials file not found at {}; phone OTP login will be unavailable", credentialsPath);
                return;
            }

            try (InputStream stream = resource.getInputStream()) {
                initialize(stream);
            }
        } catch (Exception ex) {
            log.warn("Failed to initialize FirebaseApp: {} - phone OTP login will be unavailable", ex.getMessage());
        }
    }

    private void initialize(InputStream stream) throws Exception {
        FirebaseOptions.Builder builder = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(stream));
        if (projectId != null && !projectId.isBlank()) {
            builder.setProjectId(projectId);
        }
        FirebaseApp.initializeApp(builder.build());
        log.info("FirebaseApp initialized for project {}", projectId == null || projectId.isBlank() ? "(default)" : projectId);
    }

    private InputStream credentialsJsonStream(String value) {
        String trimmed = value.trim();
        if (trimmed.startsWith("{")) {
            return new ByteArrayInputStream(trimmed.getBytes(StandardCharsets.UTF_8));
        }
        return new ByteArrayInputStream(Base64.getDecoder().decode(trimmed));
    }
}
