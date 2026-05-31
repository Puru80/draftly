package com.projects.draftly.auth.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class EncryptionService {

    private static final String ALGORITHM = "AES";

    @Value("${app.security.secret-key}")
    private String secretKey; // Must be exactly 32 characters/bytes for AES-256

    public String encrypt(String plainText) {
        try {
            byte[] decodedKey = HexFormat.of().parseHex(secretKey);
            SecretKeySpec keySpec = new SecretKeySpec(decodedKey, ALGORITHM);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            byte[] encryptedBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            throw new RuntimeException("Failed to encrypt token", e);
        }
    }

    public String decrypt(String encryptedText) {
        try {
            byte[] decodedKey = HexFormat.of().parseHex(secretKey);
            SecretKeySpec keySpec = new SecretKeySpec(decodedKey, ALGORITHM);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, keySpec);
            byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedText));
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Failed to decrypt token", e);
        }
    }
}
