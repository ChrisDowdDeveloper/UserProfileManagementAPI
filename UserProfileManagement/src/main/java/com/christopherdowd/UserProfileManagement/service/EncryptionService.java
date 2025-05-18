package com.christopherdowd.UserProfileManagement.service;

public interface EncryptionService {
    String encrypt(String data) throws Exception;
    String decrypt(String encryptedData) throws Exception;
}
