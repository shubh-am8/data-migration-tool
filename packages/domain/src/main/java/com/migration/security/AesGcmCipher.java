package com.migration.security;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/** Shared AES-256-GCM encrypt/decrypt for connection secrets. */
public final class AesGcmCipher {
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    private AesGcmCipher() {}

    public static String encrypt(String plaintext, String encryptionKey) {
        byte[] keyBytes = encryptionKey.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalArgumentException("ENCRYPTION_KEY must be at least 32 bytes");
        }
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(keyBytes, 0, 32, "AES"),
                new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            ByteBuffer buf = ByteBuffer.allocate(iv.length + encrypted.length);
            buf.put(iv).put(encrypted);
            return Base64.getEncoder().encodeToString(buf.array());
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    public static String decrypt(String ciphertext, String encryptionKey) {
        byte[] keyBytes = encryptionKey.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalArgumentException("ENCRYPTION_KEY must be at least 32 bytes");
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(ciphertext);
            ByteBuffer buf = ByteBuffer.wrap(decoded);
            byte[] iv = new byte[GCM_IV_LENGTH];
            buf.get(iv);
            byte[] encrypted = new byte[buf.remaining()];
            buf.get(encrypted);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(keyBytes, 0, 32, "AES"),
                new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed", e);
        }
    }
}
