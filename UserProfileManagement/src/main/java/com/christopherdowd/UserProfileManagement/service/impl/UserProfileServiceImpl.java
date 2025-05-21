package com.christopherdowd.UserProfileManagement.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.christopherdowd.UserProfileManagement.domain.UserProfile;
import com.christopherdowd.UserProfileManagement.dto.UserCreationMessageDto;
import com.christopherdowd.UserProfileManagement.dto.UserProfileRequestDto;
import com.christopherdowd.UserProfileManagement.dto.UserProfileResponseDto;
import com.christopherdowd.UserProfileManagement.dto.UserProfileUpdateRequestDto;
import com.christopherdowd.UserProfileManagement.mapper.UserProfileMapper;
import com.christopherdowd.UserProfileManagement.repository.UserProfileRepository;
import com.christopherdowd.UserProfileManagement.service.EncryptionService;
import com.christopherdowd.UserProfileManagement.service.UserProfileService;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class UserProfileServiceImpl implements UserProfileService {

    private static final Logger logger = LoggerFactory.getLogger(UserProfileServiceImpl.class); // Corrected logger class
    private final UserProfileRepository userRepository;
    private final UserProfileMapper mapper;
    private final EncryptionService crypto;
    private final PubSubTemplate pubSubTemplate;
    private final ObjectMapper objectMapper;

    @Value("${pubsub.topic.users-creation-topic}")
    private String userCreationTopic;

    public UserProfileServiceImpl(
            UserProfileRepository userRepository,
            UserProfileMapper mapper,
            EncryptionService crypto,
            PubSubTemplate pubSubTemplate,
            ObjectMapper objectMapper) { // Added ObjectMapper to constructor params
        this.userRepository = userRepository;
        this.mapper = mapper;
        this.crypto = crypto;
        this.pubSubTemplate = pubSubTemplate;
        this.objectMapper = objectMapper;
    }

    // Setter method for testing purposes
    public void setUserCreationTopic(String userCreationTopic) {
        this.userCreationTopic = userCreationTopic;
    }

    @Override
    @Cacheable("profiles")
    public UserProfileResponseDto getById(String id) {
        logger.debug("Fetching user by ID: {} from datastore (cache miss or direct call)", id);

        UserProfile user = userRepository.findById(id)
                .orElseThrow(() ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found: " + id
                    )
                );

        return mapper.toUserResponseDto(user);
    }

    @Override
    public List<UserProfileResponseDto> getAll() {
        logger.debug("Fetching all users from datastore");

        Iterable<UserProfile> iterable = userRepository.findAll();

        List<UserProfile> users = new ArrayList<>();

        iterable.forEach(users::add);

        return mapper.toUserResponseDtoList(users);
    }

    @Override
    public UserProfileResponseDto create(UserProfileRequestDto dto) {
        String encryptedSsn;

        try {
            // 1) Encrypt the SSN
            encryptedSsn = crypto.encrypt(dto.getSocialSecurityNumber());
        } catch (Exception e) {
            logger.error("SSN encryption failed for user identified by email: {}", dto.getEmail(), e);
            throw new RuntimeException("Failed to encrypt Social Security Number during user creation.", e);
        }

        String userId = UUID.randomUUID().toString();

        UserCreationMessageDto pubSubMessage = UserCreationMessageDto.builder()
            .id(userId)
            .username(dto.getUsername())
            .email(dto.getEmail())
            .encryptedSocialSecurityNumber(encryptedSsn)
            .build();

        try {
            String message = objectMapper.writeValueAsString(pubSubMessage);
            pubSubTemplate.publish(userCreationTopic, message); // Ensure userCreationTopic is initialized
            logger.info("Published user creation event for ID {} to Pub/Sub topic '{}'.", userId, userCreationTopic);
        } catch(Exception e) {
            logger.error("Failed to publish user creation event for email {}: {}", dto.getEmail(), e.getMessage(), e);
            throw new RuntimeException("Failed to publish user creation event.", e);
        }

        UserProfileResponseDto responseDto = new UserProfileResponseDto();
        responseDto.setId(userId);
        responseDto.setUsername(dto.getUsername());
        responseDto.setEmail(dto.getEmail());

        logger.info("User creation request for ID {} accepted and published for asynchronous processing.", userId);

        return responseDto;
    }

    @Override
    @CacheEvict(value = "profiles", key = "#id")
    public UserProfileResponseDto update(String id, UserProfileUpdateRequestDto dto) {
        logger.debug("Updating user by ID: {}", id);
        UserProfile user = userRepository.findById(id)
                .orElseThrow(() ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found: " + id
                    )
                );

        mapper.updateUserFromDto(dto, user);

        UserProfile updatedUser = userRepository.save(user);

        logger.info("User profile with ID {} updated in Datastore.", updatedUser.getId());

        return mapper.toUserResponseDto(updatedUser);
    }

    @Override
    @CacheEvict(value = "profiles", key = "#id")
    public void delete(String id) {
        logger.debug("Deleting user by ID: {}", id);

        if(!userRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found for deletion: " + id);
        }

        userRepository.deleteById(id);

        logger.info("User profilee with ID {} deleted from Datastore.", id); // Typo: "profilee"
    }
}