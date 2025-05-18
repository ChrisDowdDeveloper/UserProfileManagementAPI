package com.christopherdowd.UserProfileManagement.service.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.christopherdowd.UserProfileManagement.service.EncryptionService;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Service("jceEncryptionService") // Give it a qualifier if you might have multiple implementations
public class JceEncryptionServiceImpl implements EncryptionService {

    private static final String ALGORITHM = "AES";
    private static final String CIPHER_TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12; // 12 bytes for GCM IV is recommended
    private static final int GCM_TAG_LENGTH = 128; // In bits

    private final SecretKey secretKey;

    // Load the encryption key securely (e.g., from application.properties or environment variable)
    // IMPORTANT: This key MUST be a securely generated, 16, 24, or 32 byte random string, Base64 encoded.
    public JceEncryptionServiceImpl(@Value("${encryption.jce.key}") String base64EncodedKey) {
        byte[] decodedKey = Base64.getDecoder().decode(base64EncodedKey);
        this.secretKey = new SecretKeySpec(decodedKey, 0, decodedKey.length, ALGORITHM);
    }

    @Override
    public String encrypt(String data) throws Exception {
        Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
        byte[] iv = new byte[GCM_IV_LENGTH];
        SecureRandom random = new SecureRandom();
        random.nextBytes(iv); // Generate a random IV

        GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmParameterSpec);

        byte[] encryptedBytes = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));

        // Prepend IV to the ciphertext for decryption. IV must be the same for decryption.
        byte[] encryptedPayload = new byte[iv.length + encryptedBytes.length];
        System.arraycopy(iv, 0, encryptedPayload, 0, iv.length);
        System.arraycopy(encryptedBytes, 0, encryptedPayload, iv.length, encryptedBytes.length);

        return Base64.getEncoder().encodeToString(encryptedPayload);
    }

    @Override
    public String decrypt(String encryptedData) throws Exception {
        byte[] encryptedPayload = Base64.getDecoder().decode(encryptedData);

        // Extract IV from the beginning of the encrypted payload
        byte[] iv = new byte[GCM_IV_LENGTH];
        System.arraycopy(encryptedPayload, 0, iv, 0, iv.length);

        byte[] actualEncryptedBytes = new byte[encryptedPayload.length - iv.length];
        System.arraycopy(encryptedPayload, iv.length, actualEncryptedBytes, 0, actualEncryptedBytes.length);

        Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
        GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmParameterSpec);

        byte[] decryptedBytes = cipher.doFinal(actualEncryptedBytes);
        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }
}