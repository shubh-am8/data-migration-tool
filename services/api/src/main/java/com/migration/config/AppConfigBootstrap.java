package com.migration.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class AppConfigBootstrap implements ApplicationRunner {

    static final Map<String, String> FLYWAY_DEFAULTS = Map.of(
        "ip_whitelist_mode", "OPEN",
        "ip_whitelist", "[]",
        "min_threads_per_job", "1",
        "max_threads_per_job", "8",
        "gspace_webhook_url", ""
    );

    private final AppConfigRepository repository;
    private final Environment environment;
    private final boolean forceEnv;

    public AppConfigBootstrap(AppConfigRepository repository,
                              Environment environment,
                              @Value("${app.config.force-env:false}") boolean forceEnv) {
        this.repository = repository;
        this.environment = environment;
        this.forceEnv = forceEnv;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        for (var entry : envMappings().entrySet()) {
            String dbKey = entry.getKey();
            String envValue = entry.getValue();
            if (envValue == null || envValue.isBlank()) continue;

            repository.findById(dbKey).ifPresentOrElse(
                entity -> maybeUpdate(entity, envValue),
                () -> insert(dbKey, envValue, ConfigSource.ENV)
            );
        }
    }

    void maybeUpdate(AppConfigEntity entity, String envValue) {
        if (forceEnv) {
            apply(entity, envValue, ConfigSource.ENV);
            return;
        }
        ConfigSource source = entity.getSource() != null ? entity.getSource() : ConfigSource.FLYWAY;
        if (source == ConfigSource.DASHBOARD || entity.getUpdatedBy() != null) {
            return; // dashboard wins
        }
        if (source == ConfigSource.FLYWAY) {
            apply(entity, envValue, ConfigSource.ENV);
            return;
        }
        if (source == ConfigSource.ENV && !envValue.equals(entity.getValue())) {
            apply(entity, envValue, ConfigSource.ENV);
        }
    }

    private void apply(AppConfigEntity entity, String value, ConfigSource source) {
        entity.setValue(value);
        entity.setSource(source);
        entity.setUpdatedAt(Instant.now());
        repository.save(entity);
    }

    private void insert(String key, String value, ConfigSource source) {
        AppConfigEntity entity = new AppConfigEntity();
        entity.setKey(key);
        entity.setValue(value);
        entity.setSource(source);
        entity.setUpdatedAt(Instant.now());
        repository.save(entity);
    }

    Map<String, String> envMappings() {
        Map<String, String> m = new LinkedHashMap<>();
        for (RuntimeConfigCatalog.Entry entry : RuntimeConfigCatalog.entries()) {
            String value = firstNonBlank(
                environment.getProperty(entry.envKey()),
                entry.fallbackPropertyKey() != null ? environment.getProperty(entry.fallbackPropertyKey()) : null
            );
            if (value != null && !value.isBlank()) {
                m.put(entry.dbKey(), value);
            } else if (!entry.defaultValue().isBlank()) {
                m.put(entry.dbKey(), entry.defaultValue());
            }
        }
        return m;
    }

    private static String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) return v;
        }
        return null;
    }
}
