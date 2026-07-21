package com.migration.connectors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.migration.common.PageResponse;
import com.migration.security.SecretCipher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ConnectionService {
    private final ConnectionRepository connectionRepository;
    private final ConnectorPluginRepository pluginRepository;
    private final ConnectorPluginRegistry pluginRegistry;
    private final SecretCipher secretCipher;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ConnectionService(ConnectionRepository connectionRepository,
                             ConnectorPluginRepository pluginRepository,
                             ConnectorPluginRegistry pluginRegistry,
                             SecretCipher secretCipher) {
        this.connectionRepository = connectionRepository;
        this.pluginRepository = pluginRepository;
        this.pluginRegistry = pluginRegistry;
        this.secretCipher = secretCipher;
    }

    public List<Map<String, Object>> listAll() {
        return connectionRepository.findAll().stream().map(this::toPublicDto).toList();
    }

    public PageResponse<Map<String, Object>> list(Integer page, Integer size) {
        int p = PageResponse.clampPage(page);
        int s = PageResponse.clampSize(size);
        var result = connectionRepository.findAll(PageRequest.of(p, s, Sort.by(Sort.Direction.DESC, "createdAt")));
        return PageResponse.of(result.getContent().stream().map(this::toPublicDto).toList(), p, s, result.getTotalElements());
    }

    @Transactional
    public Map<String, Object> create(String pluginId, String name, Map<String, String> config) {
        return create(pluginId, name, config, false);
    }

    @Transactional
    public Map<String, Object> create(String pluginId, String name, Map<String, String> config, boolean sandbox) {
        requireInstalled(pluginId);
        Map<String, String> normalized = withPoolDefaults(config);
        ConnectorPlugin plugin = pluginRegistry.require(pluginId);
        var validation = plugin.validate(normalized);
        if (!validation.valid()) {
            throw new IllegalArgumentException(String.join(", ", validation.errors()));
        }
        ConnectionEntity entity = new ConnectionEntity();
        entity.setPluginId(pluginId);
        entity.setName(name);
        entity.setConfigEncrypted(secretCipher.encrypt(toJson(normalized)));
        entity.setSandbox(sandbox);
        entity = connectionRepository.save(entity);
        return toPublicDto(entity);
    }

    @Transactional
    public Map<String, Object> update(UUID id, String name, Map<String, String> config) {
        return update(id, name, config, null);
    }

    @Transactional
    public Map<String, Object> update(UUID id, String name, Map<String, String> config, Boolean sandbox) {
        ConnectionEntity entity = connectionRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Connection not found"));
        requireInstalled(entity.getPluginId());
        if (name != null) entity.setName(name);
        if (config != null) {
            Map<String, String> normalized = withPoolDefaults(config);
            ConnectorPlugin plugin = pluginRegistry.require(entity.getPluginId());
            var validation = plugin.validate(normalized);
            if (!validation.valid()) {
                throw new IllegalArgumentException(String.join(", ", validation.errors()));
            }
            entity.setConfigEncrypted(secretCipher.encrypt(toJson(normalized)));
        }
        if (sandbox != null) entity.setSandbox(sandbox);
        entity.setUpdatedAt(Instant.now());
        return toPublicDto(connectionRepository.save(entity));
    }

    private void requireInstalled(String pluginId) {
        ConnectorPluginEntity catalog = pluginRepository.findById(pluginId)
            .orElseThrow(() -> new IllegalArgumentException("Unknown connector: " + pluginId));
        if (!catalog.isEnabled()) {
            throw new IllegalArgumentException(
                "Connector '" + pluginId + "' is not installed — install it from the Marketplace first");
        }
        if (pluginRegistry.get(pluginId).isEmpty()) {
            throw new IllegalArgumentException(
                "Connector '" + pluginId + "' is enabled but JAR is not loaded — reinstall from Marketplace");
        }
    }

    /** Ensures minPoolSize/maxPoolSize (default max 10) live in encrypted config JSON. */
    static Map<String, String> withPoolDefaults(Map<String, String> config) {
        Map<String, String> out = new LinkedHashMap<>(config != null ? config : Map.of());
        out.putIfAbsent("minPoolSize", "1");
        String max = out.getOrDefault("maxPoolSize", "10");
        try {
            int m = Integer.parseInt(max);
            if (m < 1) m = 1;
            if (m > 100) m = 100;
            out.put("maxPoolSize", String.valueOf(m));
        } catch (NumberFormatException e) {
            out.put("maxPoolSize", "10");
        }
        return out;
    }

    @Transactional
    public void delete(UUID id) {
        connectionRepository.deleteById(id);
    }

    public ConnectionTestResult test(UUID id) {
        ConnectionEntity entity = connectionRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Connection not found"));
        return pluginRegistry.require(entity.getPluginId())
            .testConnection(decryptConfig(entity), Duration.ofSeconds(5));
    }

    public ConnectionTestResult testConfig(Map<String, String> config, String pluginId) {
        requireInstalled(pluginId);
        return pluginRegistry.require(pluginId).testConnection(config, Duration.ofSeconds(5));
    }

    public Map<String, String> loadConfig(UUID id) {
        ConnectionEntity entity = connectionRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Connection not found"));
        Map<String, String> config = decryptConfig(entity);
        config.put("_id", id.toString());
        config.put("_pluginId", entity.getPluginId());
        return config;
    }

    public ConnectionEntity getEntity(UUID id) {
        return connectionRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Connection not found"));
    }

    public Map<String, String> decryptConfig(ConnectionEntity entity) {
        try {
            return objectMapper.readValue(secretCipher.decrypt(entity.getConfigEncrypted()),
                new TypeReference<>() {});
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Map<String, Object> toPublicDto(ConnectionEntity entity) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", entity.getId());
        dto.put("pluginId", entity.getPluginId());
        dto.put("name", entity.getName());
        dto.put("sandbox", entity.isSandbox());
        dto.put("config", Map.of("password", "REDACTED"));
        dto.put("createdAt", entity.getCreatedAt());
        dto.put("updatedAt", entity.getUpdatedAt());
        return dto;
    }

    private String toJson(Map<String, String> config) {
        try {
            return objectMapper.writeValueAsString(config);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
