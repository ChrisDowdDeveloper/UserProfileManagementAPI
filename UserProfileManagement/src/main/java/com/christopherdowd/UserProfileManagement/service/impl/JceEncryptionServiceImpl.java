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

/* 
 * Implementation of EncryptionService using Java Cryptography Extension (JCE)
 * Provides methods to encrypt and decrypt data using AES/GCM/NoPadding
 * Encryption key is stored in application.yml
*/
@Service("jceEncryptionService")
public class JceEncryptionServiceImpl implements EncryptionService {

    // Encryption algorithm (Advanced Encryption Standard)
    private static final String ALGORITHM = "AES";

    // Defines the cipher transformation string: AES with Galois/Counter Mode (GCM) and no padding
    private static final String CIPHER_TRANSFORMATION = "AES/GCM/NoPadding";

    // Length of initialization vector for GCM mode
    private static final int GCM_IV_LENGTH = 12;

    // Length of GCM authentication tag in bits
    private static final int GCM_TAG_LENGTH = 128;

    private final SecretKey secretKey; // Holds the secret key for encryption and decryption

    /*
     * Constructor for JceEncryptionServiceImpl
     * Initializes the secret key from a Base64 encoded string provided in application.yml
     * @param base64EncodedKey is the Base64 encoded encryption key 
     */
    public JceEncryptionServiceImpl(@Value("${encryption.jce.key}") String base64EncodedKey) {
        // Decode the key string into a byte array
        byte[] decodedKey = Base64.getDecoder().decode(base64EncodedKey);
        // Creates SecretKeySpec from decoded byes, specifying the AES algorithm
        this.secretKey = new SecretKeySpec(decodedKey, 0, decodedKey.length, ALGORITHM);
    }

    /*
     * Encryptes the given text data
     * Initialization Vector (iv) is generated randomly for each encryption operation and prepended to ciphertext
     * @param data is the plaintext string to encrypt
     * @return a Base64 encoded string representing the IV prepended to the cipher text
     * @throws an exception if any error occurs during encryption
     */
    @Override
    public String encrypt(String data) throws Exception {
        // Gets a Cipher instance for the specified transformation
        Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);

        // Generate a random Initialization Vector (IV). GCM requires a unique IV for each encryption with same key
        byte[] iv = new byte[GCM_IV_LENGTH];
        SecureRandom random = new SecureRandom(); // Uses cryptographically strong random number generator
        random.nextBytes(iv);

        // Creates GCMParameterSpec with the IV and tag length.
        GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        // Initialize the cipher for encryption mode with the secret key and GCM parameters
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmParameterSpec);

        // Performs encryption on the UTF-8 bytes of plaintext data
        byte[] encryptedBytes = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));

        // Prepend the IV to the encrypted bytes. The IV is needed for decryption
        byte[] encryptedPayload = new byte[iv.length + encryptedBytes.length];
        System.arraycopy(iv, 0, encryptedPayload, 0, iv.length); // Copies the beginning of the payload
        System.arraycopy(encryptedBytes, 0, encryptedPayload, iv.length, encryptedBytes.length); // Copy ciphertext after IV

        // Returns the IV + ciphertext as a Base64 encoded string
        return Base64.getEncoder().encodeToString(encryptedPayload);
    }


    /* 
     * Decrypts Base64 encoded encrypted data
     * Encrypted data expected to have the IV prepended to the ciphertext
     * @param encryptedData a base64 encoded string containing the IV and ciphertext
     * @return the original plaintext string
     * @throws exception if any error occurs during decryption
    */
    @Override
    public String decrypt(String encryptedData) throws Exception {
        // Decodes the Base64 encrypted data
        byte[] encryptedPayload = Base64.getDecoder().decode(encryptedData);

        // Extracts IV from the beginning of the encrypted payload
        byte[] iv = new byte[GCM_IV_LENGTH];
        System.arraycopy(encryptedPayload, 0, iv, 0, iv.length);

        // Extracts ciphertext
        byte[] actualEncryptedBytes = new byte[encryptedPayload.length - iv.length];
        System.arraycopy(encryptedPayload, iv.length, actualEncryptedBytes, 0, actualEncryptedBytes.length);

        // Gets Cipher instance for the specified transformation
        Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
        // Creates GCMParameterSpec with the extracted IV and tag length
        GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        // Initialize the cipher for decryption mode with secret key and GCM parameters
        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmParameterSpec);

        // Performs decryption
        // Throws eception if auth tag doesn't match or key/iv is incorrect
        byte[] decryptedBytes = cipher.doFinal(actualEncryptedBytes);
        // Converts decrypted bytes back to string using UTF-8 encoding
        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }
}