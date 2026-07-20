package com.migration.connectors;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ConnectionDecryptorTest {

    private static final String KEY = "dev-encryption-key-32bytes-long!!!!";

    @Test
    void roundTripDecrypt() {
        String plain = "{\"host\":\"localhost\",\"port\":\"5432\"}";
        String encrypted = com.migration.security.AesGcmCipher.encrypt(plain, KEY);
        Map<String, String> config = ConnectionDecryptor.decryptConfig(encrypted, KEY);
        assertEquals("localhost", config.get("host"));
        assertEquals("5432", config.get("port"));
    }
}
