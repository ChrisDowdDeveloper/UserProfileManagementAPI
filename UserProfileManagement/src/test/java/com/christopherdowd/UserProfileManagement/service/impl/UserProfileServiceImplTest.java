package com.christopherdowd.UserProfileManagement.service.impl;

import com.christopherdowd.UserProfileManagement.domain.UserProfile;
import com.christopherdowd.UserProfileManagement.dto.UserCreationMessageDto;
import com.christopherdowd.UserProfileManagement.dto.UserProfileRequestDto;
import com.christopherdowd.UserProfileManagement.dto.UserProfileResponseDto;
import com.christopherdowd.UserProfileManagement.repository.UserProfileRepository;
import com.christopherdowd.UserProfileManagement.service.EncryptionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UserProfileServiceImplTest {

    @Mock
    private UserProfileRepository userRepository; 

    // If UserProfileMapper is not directly used by the 'create' method to build the response DTO,
    // (i.e., the response DTO is manually constructed), then this mock might not be needed for this specific test.
    // @Mock
    // private UserProfileMapper mapper; 

    @Mock
    private EncryptionService crypto;

    @Mock
    private PubSubTemplate pubSubTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private UserProfileServiceImpl userProfileService;

    private final String TEST_TOPIC_NAME = "test-user-creation-topic";

    @BeforeEach
    void setUp() {
        // Manually set the topic name for the service instance, 
        // as @Value won't be processed in unit test without Spring context
        userProfileService.setUserCreationTopic(TEST_TOPIC_NAME);
    }

    @Test
    void create_shouldEncryptSsnPublishToPubSubAndReturnDto_whenSuccessful() throws Exception {
        // Arrange
        UserProfileRequestDto requestDto = UserProfileRequestDto.builder()
                .username("testuser")
                .email("test@example.com")
                .socialSecurityNumber("123-45-6789")
                .build();

        String rawSsn = requestDto.getSocialSecurityNumber();
        String expectedEncryptedSsn = "encryptedSsn123";
        
        // Mock encryption
        when(crypto.encrypt(rawSsn)).thenReturn(expectedEncryptedSsn);

        // Mock ObjectMapper to return a predictable JSON string and allow capturing the DTO
        when(objectMapper.writeValueAsString(any(UserCreationMessageDto.class)))
            .thenAnswer(invocation -> {
                UserCreationMessageDto msgDto = invocation.getArgument(0);
                // Construct a JSON string based on the captured DTO for verification
                return String.format(
                    "{\"id\":\"%s\",\"username\":\"%s\",\"email\":\"%s\",\"encryptedSocialSecurityNumber\":\"%s\"}",
                    msgDto.getId(), 
                    msgDto.getUsername(), 
                    msgDto.getEmail(), 
                    msgDto.getEncryptedSocialSecurityNumber()
                );
            });

        // Act
        UserProfileResponseDto responseDto = userProfileService.create(requestDto);

        // Assert
        // 1. Verify SSN Encryption
        verify(crypto, times(1)).encrypt(rawSsn);

        // 2. Capture and Verify Pub/Sub Message Content (UserCreationMessageDto)
        ArgumentCaptor<UserCreationMessageDto> messageDtoCaptor = ArgumentCaptor.forClass(UserCreationMessageDto.class);
        verify(objectMapper, times(1)).writeValueAsString(messageDtoCaptor.capture());
        UserCreationMessageDto capturedMessageDto = messageDtoCaptor.getValue();

        assertNotNull(capturedMessageDto.getId(), "Generated User ID should not be null in Pub/Sub message");
        assertEquals(requestDto.getUsername(), capturedMessageDto.getUsername());
        assertEquals(requestDto.getEmail(), capturedMessageDto.getEmail());
        assertEquals(expectedEncryptedSsn, capturedMessageDto.getEncryptedSocialSecurityNumber());

        // 3. Verify Pub/Sub Publishing Action
        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(pubSubTemplate, times(1)).publish(topicCaptor.capture(), payloadCaptor.capture());
        
        assertEquals(TEST_TOPIC_NAME, topicCaptor.getValue());
        String actualPublishedPayload = payloadCaptor.getValue();
        assertTrue(actualPublishedPayload.contains("\"id\":\"" + capturedMessageDto.getId() + "\""));
        assertTrue(actualPublishedPayload.contains("\"username\":\"" + requestDto.getUsername() + "\""));
        assertTrue(actualPublishedPayload.contains("\"email\":\"" + requestDto.getEmail() + "\""));
        assertTrue(actualPublishedPayload.contains("\"encryptedSocialSecurityNumber\":\"" + expectedEncryptedSsn + "\""));

        // 4. Verify No Direct Save to Repository in this method
        verify(userRepository, never()).save(any(UserProfile.class));

        // 5. Verify Returned DTO
        assertNotNull(responseDto);
        assertEquals(capturedMessageDto.getId(), responseDto.getId(), "ID in response DTO should match generated ID for Pub/Sub message");
        assertEquals(requestDto.getUsername(), responseDto.getUsername());
        assertEquals(requestDto.getEmail(), responseDto.getEmail());
    }

    @Test
    void create_shouldThrowRuntimeException_whenEncryptionFails() throws Exception {
        // Arrange
        UserProfileRequestDto requestDto = UserProfileRequestDto.builder()
                .username("testuser")
                .email("test@example.com")
                .socialSecurityNumber("123-45-6789")
                .build();
        
        when(crypto.encrypt(requestDto.getSocialSecurityNumber())).thenThrow(new Exception("Encryption error"));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userProfileService.create(requestDto);
        });
        
        assertEquals("Failed to encrypt Social Security Number during user creation.", exception.getMessage());
        verify(crypto, times(1)).encrypt(requestDto.getSocialSecurityNumber());
        // Ensure further operations like serialization or publishing are not attempted
        verify(objectMapper, never()).writeValueAsString(any());
        verify(pubSubTemplate, never()).publish(anyString(), anyString());
    }

    @Test
    void create_shouldThrowRuntimeException_whenPublishingFails() throws Exception {
        // Arrange
        UserProfileRequestDto requestDto = UserProfileRequestDto.builder()
                .username("testuser")
                .email("test@example.com")
                .socialSecurityNumber("123-45-6789")
                .build();

        String expectedEncryptedSsn = "encryptedSsn123";
        String jsonPayload = "{\"id\":\"some-uuid\",\"username\":\"testuser\",\"email\":\"test@example.com\",\"encryptedSocialSecurityNumber\":\"encryptedSsn123\"}";

        when(crypto.encrypt(requestDto.getSocialSecurityNumber())).thenReturn(expectedEncryptedSsn);
        when(objectMapper.writeValueAsString(any(UserCreationMessageDto.class))).thenReturn(jsonPayload);
        doThrow(new RuntimeException("Pub/Sub publish error")).when(pubSubTemplate).publish(eq(TEST_TOPIC_NAME), eq(jsonPayload));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userProfileService.create(requestDto);
        });

        assertEquals("Failed to publish user creation event.", exception.getMessage());
        verify(crypto, times(1)).encrypt(requestDto.getSocialSecurityNumber());
        verify(objectMapper, times(1)).writeValueAsString(any(UserCreationMessageDto.class));
        verify(pubSubTemplate, times(1)).publish(eq(TEST_TOPIC_NAME), eq(jsonPayload));
    }
}