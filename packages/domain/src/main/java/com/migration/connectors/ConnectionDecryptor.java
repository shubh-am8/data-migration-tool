package com.migration.connectors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.migration.security.AesGcmCipher;

import java.util.Map;

public final class ConnectionDecryptor {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ConnectionDecryptor() {}

    public static Map<String, String> decryptConfig(String configEncrypted, String encryptionKey) {
        try {
            String json = AesGcmCipher.decrypt(configEncrypted, encryptionKey);
            return MAPPER.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            throw new RuntimeException("Failed to decrypt connection config", e);
        }
    }
}
