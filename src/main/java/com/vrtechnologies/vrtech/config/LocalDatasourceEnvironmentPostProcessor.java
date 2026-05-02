package com.vrtechnologies.vrtech.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.core.env.Profiles;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

public class LocalDatasourceEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final String LOCAL_PROFILE = "local";
    private static final String LOCAL_PROPERTIES_NAME = "application-local.properties";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (!environment.acceptsProfiles(Profiles.of(LOCAL_PROFILE))) {
            return;
        }

        Properties localProperties = loadLocalProperties();
        if (localProperties.isEmpty()) {
            return;
        }

        Map<String, Object> overrides = new LinkedHashMap<>();
        addOverrideIfBlank(environment, localProperties, overrides, "spring.datasource.url");
        addOverrideIfBlank(environment, localProperties, overrides, "spring.datasource.username");
        addOverrideIfBlank(environment, localProperties, overrides, "spring.datasource.password");

        if (!overrides.isEmpty()) {
            environment.getPropertySources().addFirst(new MapPropertySource("localDatasourceOverrides", overrides));
        }
    }

    private void addOverrideIfBlank(
            ConfigurableEnvironment environment,
            Properties localProperties,
            Map<String, Object> overrides,
            String key
    ) {
        String resolvedValue = environment.getProperty(key);
        String localValue = localProperties.getProperty(key);

        if (!StringUtils.hasText(resolvedValue) && StringUtils.hasText(localValue)) {
            overrides.put(key, localValue.trim());
        }
    }

    private Properties loadLocalProperties() {
        Resource resource = new ClassPathResource(LOCAL_PROPERTIES_NAME);
        if (!resource.exists()) {
            return new Properties();
        }

        try {
            return PropertiesLoaderUtils.loadProperties(resource);
        } catch (IOException ignored) {
            return new Properties();
        }
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
