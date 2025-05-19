package com.christopherdowd.UserProfileManagement;


import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import com.christopherdowd.UserProfileManagement.domain.UserProfile;
import com.christopherdowd.UserProfileManagement.dto.UserProfileRequestDto;
import com.christopherdowd.UserProfileManagement.dto.UserProfileResponseDto;

/**
 * Utility class for creating test data for UserProfile tests.
 */
public final class TestDataUtil {

    // Private constructor to prevent instantiation
    private TestDataUtil() {
    }

    // --- Common User Data Constants ---

    // User 1
    public static final String USER_1_ID = "d349c760-426b-4951-8427-d393e0d1a792";
    public static final String USER_1_USERNAME = "AliceWonder";
    public static final String USER_1_EMAIL = "alice.wonder@example.com";
    public static final String USER_1_SSN_RAW = "167984240";
    public static final String USER_1_SSN_ENCRYPTED = "encryptedSsnForAliceWonder==";

    // User 2
    public static final String USER_2_ID = "356ef43c-0885-4e24-82fb-956e1f59ceff";
    public static final String USER_2_USERNAME = "BobTheBuilder";
    public static final String USER_2_EMAIL = "bob.builder@example.com";
    public static final String USER_2_SSN_RAW = "119075002";
    public static final String USER_2_SSN_ENCRYPTED = "encryptedSsnForBobTheBuilder==";

    // User 3
    public static final String USER_3_ID = "124017d2-c3b5-4eec-a3ea-6c4dd3421d0d";
    public static final String USER_3_USERNAME = "CarolDanvers";
    public static final String USER_3_EMAIL = "carol.danvers@example.com";
    public static final String USER_3_SSN_RAW = "438981583";
    public static final String USER_3_SSN_ENCRYPTED = "encryptedSsnForCarolDanvers==";

    // User 4
    public static final String USER_4_ID = "e398dbe8-6422-4a71-8f89-4f8340122f3d";
    public static final String USER_4_USERNAME = "DavidCopperfield";
    public static final String USER_4_EMAIL = "david.copperfield@example.com";
    public static final String USER_4_SSN_RAW = "874166323";
    public static final String USER_4_SSN_ENCRYPTED = "encryptedSsnForDavidCopperfield==";

    // --- UserProfileRequestDto Builders ---

    public static UserProfileRequestDto createAliceRequestDto() {
        return UserProfileRequestDto.builder()
                .username(USER_1_USERNAME)
                .email(USER_1_EMAIL)
                .socialSecurityNumber(USER_1_SSN_RAW)
                .build();
    }

    public static UserProfileRequestDto createBobRequestDto() {
        return UserProfileRequestDto.builder()
                .username(USER_2_USERNAME)
                .email(USER_2_EMAIL)
                .socialSecurityNumber(USER_2_SSN_RAW)
                .build();
    }

    public static UserProfileRequestDto createCarolRequestDto() {
        return UserProfileRequestDto.builder()
                .username(USER_3_USERNAME)
                .email(USER_3_EMAIL)
                .socialSecurityNumber(USER_3_SSN_RAW)
                .build();
    }

    public static UserProfileRequestDto createDavidRequestDto() {
        return UserProfileRequestDto.builder()
                .username(USER_4_USERNAME)
                .email(USER_4_EMAIL)
                .socialSecurityNumber(USER_4_SSN_RAW)
                .build();
    }

    public static List<UserProfileRequestDto> createSampleRequestDtos() {
        return Arrays.asList(createAliceRequestDto(), createBobRequestDto(), createCarolRequestDto(), createDavidRequestDto());
    }

    // --- UserProfile Entity Builders ---

    public static UserProfile createAliceEntity() {
        return UserProfile.builder()
                .id(USER_1_ID)
                .username(USER_1_USERNAME)
                .email(USER_1_EMAIL)
                .encryptedSocialSecurityNumber(USER_1_SSN_ENCRYPTED)
                .build();
    }

    public static UserProfile createBobEntity() {
        return UserProfile.builder()
                .id(USER_2_ID)
                .username(USER_2_USERNAME)
                .email(USER_2_EMAIL)
                .encryptedSocialSecurityNumber(USER_2_SSN_ENCRYPTED)
                .build();
    }

    public static UserProfile createCarolEntity() {
        return UserProfile.builder()
                .id(USER_3_ID)
                .username(USER_3_USERNAME)
                .email(USER_3_EMAIL)
                .encryptedSocialSecurityNumber(USER_3_SSN_ENCRYPTED)
                .build();
    }

    public static UserProfile createDavidEntity() {
        return UserProfile.builder()
                .id(USER_4_ID)
                .username(USER_4_USERNAME)
                .email(USER_4_EMAIL)
                .encryptedSocialSecurityNumber(USER_4_SSN_ENCRYPTED)
                .build();
    }

    /**
     * Creates a UserProfile entity with a given ID and placeholder encrypted SSN.
     * Useful for tests where the ID is known or needs to be specific.
     */
    public static UserProfile createEntityWithId(String id, String username, String email, String encryptedSsn) {
        return UserProfile.builder()
                .id(id)
                .username(username)
                .email(email)
                .encryptedSocialSecurityNumber(encryptedSsn)
                .build();
    }

    public static List<UserProfile> createSampleEntities() {
        return Arrays.asList(createAliceEntity(), createBobEntity(), createCarolEntity(), createDavidEntity());
    }


    // --- UserProfileResponseDto Builders ---

    public static UserProfileResponseDto createAliceResponseDto() {
        return UserProfileResponseDto.builder()
                .id(USER_1_ID)
                .username(USER_1_USERNAME)
                .email(USER_1_EMAIL)
                .build();
    }

    public static UserProfileResponseDto createBobResponseDto() {
        return UserProfileResponseDto.builder()
                .id(USER_2_ID)
                .username(USER_2_USERNAME)
                .email(USER_2_EMAIL)
                .build();
    }

    public static UserProfileResponseDto createCarolResponseDto() {
        return UserProfileResponseDto.builder()
                .id(USER_3_ID)
                .username(USER_3_USERNAME)
                .email(USER_3_EMAIL)
                .build();
    }

    public static UserProfileResponseDto createDavidResponseDto() {
        return UserProfileResponseDto.builder()
                .id(USER_4_ID)
                .username(USER_4_USERNAME)
                .email(USER_4_EMAIL)
                .build();
    }

    public static List<UserProfileResponseDto> createSampleResponseDtos() {
        return Arrays.asList(createAliceResponseDto(), createBobResponseDto(), createCarolResponseDto(), createDavidResponseDto());
    }


    // --- Generic Builders (Useful for dynamic test data) ---

    /**
     * Creates a generic UserProfileRequestDto.
     */
    public static UserProfileRequestDto createGenericRequestDto(String username, String email, String ssn) {
        return UserProfileRequestDto.builder()
                .username(username)
                .email(email)
                .socialSecurityNumber(ssn)
                .build();
    }

    /**
     * Creates a generic UserProfile entity.
     * ID is auto-generated using UUID.randomUUID().toString().
     * Encrypted SSN is a placeholder.
     */
    public static UserProfile createGenericEntity(String username, String email, String placeholderEncryptedSsn) {
        return UserProfile.builder()
                .id(UUID.randomUUID().toString())
                .username(username)
                .email(email)
                .encryptedSocialSecurityNumber(placeholderEncryptedSsn)
                .build();
    }

    /**
     * Creates a generic UserProfileResponseDto.
     */
    public static UserProfileResponseDto createGenericResponseDto(String id, String username, String email) {
        return UserProfileResponseDto.builder()
                .id(id)
                .username(username)
                .email(email)
                .build();
    }
}