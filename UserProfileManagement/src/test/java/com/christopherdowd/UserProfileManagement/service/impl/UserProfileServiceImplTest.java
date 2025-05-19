package com.christopherdowd.UserProfileManagement.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.christopherdowd.UserProfileManagement.TestDataUtil;
import com.christopherdowd.UserProfileManagement.domain.UserProfile;
import com.christopherdowd.UserProfileManagement.dto.UserProfileRequestDto;
import com.christopherdowd.UserProfileManagement.dto.UserProfileResponseDto;
import com.christopherdowd.UserProfileManagement.mapper.UserProfileMapper;
import com.christopherdowd.UserProfileManagement.repository.UserProfileRepository;
import com.christopherdowd.UserProfileManagement.service.EncryptionService;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
// Assuming your service class is UserProfileServiceImpl and other classes are correctly named and imported.
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class) // If using JUnit 5
public class UserProfileServiceImplTest { // Assuming your service is UserProfileServiceImpl

    @Mock
    private EncryptionService crypto; // Assuming EncryptionService is the type
    @Mock
    private UserProfileMapper mapper; // Assuming UserProfileMapper is the type
    @Mock
    private UserProfileRepository repo; // Assuming UserProfileRepository is the type
    @Mock
    private PubSubTemplate pubSub; // Assuming PubSubTemplate is the type (or your specific pubsub interface)

    @InjectMocks
    private UserProfileServiceImpl service; // Your service class implementation

    @Test
    void create_encryptsSsnAndSavesAndPublishesAndReturnsDto() throws Exception { // Renamed for clarity
        // GIVEN
        UserProfileRequestDto requestDto = TestDataUtil.createAliceRequestDto();
        String expectedEncryptedSsn = "encSSN_for_Alice"; // Make it slightly more descriptive

        // Mock the entity that mapper.toUser would return.
        // This entity will have username and email from the requestDto.
        // The service will then set its ID and encryptedSocialSecurityNumber.
        UserProfile entityFromMapper = new UserProfile();
        entityFromMapper.setUsername(requestDto.getUsername());
        entityFromMapper.setEmail(requestDto.getEmail());

        when(crypto.encrypt(requestDto.getSocialSecurityNumber()))
            .thenReturn(expectedEncryptedSsn);

        when(mapper.toUser(requestDto)).thenReturn(entityFromMapper);

        // Mock the repository save to return the entity that was passed to it.
        // The entity passed will be 'entityFromMapper' after its ID and encryptedSsn are set by the service.
        when(repo.save(any(UserProfile.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // This is the DTO we expect the service to return.
        // Note: Its ID is from TestDataUtil, which might differ from the randomly generated ID.
        UserProfileResponseDto expectedResponseDto = TestDataUtil.createAliceResponseDto();
        // Mock the mapper to return this expected DTO when called with any UserProfile.
        // This means 'actual' in the test will be 'expectedResponseDto'.
        when(mapper.toUserResponseDto(any(UserProfile.class)))
            .thenReturn(expectedResponseDto);

        // WHEN
        UserProfileResponseDto actualResponseDto = service.create(requestDto);

        // THEN
        // 1. Assert the returned DTO
        // assertEquals is generally preferred for value objects over assertSame.
        // This will pass because mapper.toUserResponseDto is mocked to return expectedResponseDto.
        assertEquals(expectedResponseDto, actualResponseDto);
        // assertSame(expectedResponseDto, actualResponseDto); // Also works due to the mock

        // 2. Verify encryption service was called
        verify(crypto).encrypt(requestDto.getSocialSecurityNumber());

        // 3. Capture the entity passed to repo.save() and verify its state
        ArgumentCaptor<UserProfile> userEntityCaptor = ArgumentCaptor.forClass(UserProfile.class);
        verify(repo).save(userEntityCaptor.capture());
        UserProfile savedEntity = userEntityCaptor.getValue();

        assertNotNull(savedEntity.getId(), "Saved entity ID should not be null");
        assertEquals(expectedEncryptedSsn, savedEntity.getEncryptedSocialSecurityNumber(), "Encrypted SSN mismatch");
        assertEquals(requestDto.getUsername(), savedEntity.getUsername(), "Username mismatch");
        assertEquals(requestDto.getEmail(), savedEntity.getEmail(), "Email mismatch");

        // 4. Verify Pub/Sub was called with the CORRECT ID (the one from the saved entity)
        assertNotNull(savedEntity.getId(), "ID used for Pub/Sub publish should not be null");
        verify(pubSub).publish(eq("users-create-topic"), eq(savedEntity.getId().getBytes()));

        // 5. Verify the mapper was called to produce the response DTO
        // We pass the 'savedEntity' (which is the same instance as 'entityFromMapper' after modification)
        // to this verification.
        verify(mapper).toUserResponseDto(savedEntity);
    }
}