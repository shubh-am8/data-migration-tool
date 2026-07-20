package com.migration.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SecretCipher {
    private final String encryptionKey;

    public SecretCipher(@Value("${app.encryption-key}") String key) {
        this.encryptionKey = key;
    }

    public String encrypt(String plaintext) {
        return AesGcmCipher.encrypt(plaintext, encryptionKey);
    }

    public String decrypt(String ciphertext) {
        return AesGcmCipher.decrypt(ciphertext, encryptionKey);
    }
}
