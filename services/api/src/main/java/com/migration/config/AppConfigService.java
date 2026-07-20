package com.migration.config;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AppConfigService {

    public static final List<String> EDITABLE_KEYS = RuntimeConfigCatalog.entries().stream()
        .filter(RuntimeConfigCatalog.Entry::editable)
        .map(RuntimeConfigCatalog.Entry::dbKey)
        .toList();

    private final AppConfigRepository repository;

    public AppConfigService(AppConfigRepository repository) {
        this.repository = repository;
    }

    @org.springframework.cache.annotation.Cacheable("appConfig")
    public Map<String, String> getAll() {
        return repository.findAll().stream()
            .collect(Collectors.toMap(AppConfigEntity::getKey, AppConfigEntity::getValue));
    }

    public String get(String key) {
        return getAll().getOrDefault(key, "");
    }

    @Transactional
    @org.springframework.cache.annotation.CacheEvict(value = "appConfig", allEntries = true)
    public Map<String, String> update(Map<String, String> updates) {
        var catalog = RuntimeConfigCatalog.byKey();
        
        for (var entry : updates.entrySet()) {
            var meta = catalog.get(entry.getKey());
            if (meta == null || !meta.editable()) continue;
            
            if (meta.sensitive() && "********".equals(entry.getValue())) {
                continue; // keep existing secret when UI submits masked placeholder
            }

            if (!meta.validator().test(entry.getValue())) {
                throw new IllegalArgumentException("Invalid value for " + entry.getKey());
            }
            
            AppConfigEntity entity = repository.findById(entry.getKey())
                .orElseGet(() -> {
                    AppConfigEntity e = new AppConfigEntity();
                    e.setKey(entry.getKey());
                    return e;
                });
            entity.setValue(entry.getValue());
            entity.setSource(ConfigSource.DASHBOARD);
            entity.setUpdatedAt(Instant.now());
            repository.save(entity);
        }
        return getAll();
    }

    public Map<String, String> getEditable() {
        Map<String, String> all = getAll();
        Map<String, String> editable = new LinkedHashMap<>();
        for (String key : EDITABLE_KEYS) {
            editable.put(key, all.getOrDefault(key, ""));
        }
        return editable;
    }

    public Map<String, ConfigEntryDto> getEditableWithMeta() {
        Map<String, ConfigEntryDto> result = new LinkedHashMap<>();
        
        for (RuntimeConfigCatalog.Entry entry : RuntimeConfigCatalog.entries()) {
            if (!entry.editable()) continue;
            
            repository.findById(entry.dbKey()).ifPresentOrElse(
                e -> {
                    String displayValue = entry.sensitive() ? maskedValue(e.getValue()) : e.getValue();
                    result.put(entry.dbKey(), new ConfigEntryDto(
                        displayValue,
                        e.getSource() != null ? e.getSource().name() : ConfigSource.FLYWAY.name(),
                        e.getUpdatedAt() != null ? e.getUpdatedAt().toString() : "",
                        entry.sensitive(),
                        entry.sensitive(),
                        entry.restartRequired()
                    ));
                },
                () -> {
                    String displayValue = entry.sensitive() ? maskedValue(entry.defaultValue()) : entry.defaultValue();
                    result.put(entry.dbKey(), new ConfigEntryDto(
                        displayValue,
                        ConfigSource.FLYWAY.name(),
                        "",
                        entry.sensitive(),
                        entry.sensitive(),
                        entry.restartRequired()
                    ));
                }
            );
        }
        return result;
    }

    private static String maskedValue(String raw) {
        return (raw == null || raw.isBlank()) ? "" : "********";
    }

    public String revealSensitiveValue(String key) {
        var catalog = RuntimeConfigCatalog.byKey();
        var entry = catalog.get(key);
        if (entry == null || !entry.sensitive()) {
            throw new IllegalArgumentException("Key not found or not sensitive: " + key);
        }
        return repository.findById(key)
            .map(AppConfigEntity::getValue)
            .orElse(entry.defaultValue());
    }

    public record ConfigEntryDto(
        String value,
        String source,
        String updatedAt,
        boolean sensitive,
        boolean masked,
        boolean restartRequired
    ) {}
}
