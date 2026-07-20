package com.migration.connectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
public class WorkerConnectionService {
    private final WorkerConnectionRepository repository;
    private final String encryptionKey;

    public WorkerConnectionService(WorkerConnectionRepository repository,
                                   @Value("${app.encryption-key}") String encryptionKey) {
        this.repository = repository;
        this.encryptionKey = encryptionKey;
    }

    public Map<String, String> loadConfig(UUID connectionId) {
        WorkerConnectionEntity entity = repository.findById(connectionId)
            .orElseThrow(() -> new IllegalArgumentException("Connection not found: " + connectionId));
        return ConnectionDecryptor.decryptConfig(entity.getConfigEncrypted(), encryptionKey);
    }

    public String pluginId(UUID connectionId) {
        return repository.findById(connectionId)
            .orElseThrow(() -> new IllegalArgumentException("Connection not found: " + connectionId))
            .getPluginId();
    }
}
