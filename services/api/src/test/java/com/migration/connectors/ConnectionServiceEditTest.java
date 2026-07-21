package com.migration.connectors;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ConnectionServiceEditTest {

    @Test
    void maskSecretsReplacesPasswordFields() {
        Map<String, String> masked = ConnectionService.maskSecrets(Map.of(
            "host", "localhost",
            "password", "secret123"
        ));
        assertEquals("localhost", masked.get("host"));
        assertEquals("********", masked.get("password"));
    }

    @Test
    void mergeSecretsKeepsExistingWhenMaskedPlaceholder() {
        Map<String, String> existing = Map.of("host", "localhost", "password", "old-secret");
        Map<String, String> incoming = Map.of("host", "db.example.com", "password", "********");
        Map<String, String> merged = ConnectionService.mergeSecrets(existing, incoming);
        assertEquals("db.example.com", merged.get("host"));
        assertEquals("old-secret", merged.get("password"));
    }

    @Test
    void mergeSecretsUpdatesPasswordWhenNewValueProvided() {
        Map<String, String> existing = Map.of("password", "old-secret");
        Map<String, String> incoming = Map.of("password", "new-secret");
        Map<String, String> merged = ConnectionService.mergeSecrets(existing, incoming);
        assertEquals("new-secret", merged.get("password"));
    }
}
