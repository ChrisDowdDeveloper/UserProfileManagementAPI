package com.christopherdowd.UserProfileManagement.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.christopherdowd.UserProfileManagement.domain.UserProfile;
import com.christopherdowd.UserProfileManagement.dto.UserProfileRequestDto;
import com.christopherdowd.UserProfileManagement.dto.UserProfileResponseDto;
import com.christopherdowd.UserProfileManagement.dto.UserProfileUpdateRequestDto;
import com.christopherdowd.UserProfileManagement.mapper.UserProfileMapper;
import com.christopherdowd.UserProfileManagement.repository.UserProfileRepository;
import com.christopherdowd.UserProfileManagement.service.EncryptionService;
import com.christopherdowd.UserProfileManagement.service.UserProfileService;

@Service
public class UserProfileServiceImpl implements UserProfileService {

    private static final Logger logger = LoggerFactory.getLogger(UserProfileService.class);
    private final UserProfileRepository userRepository;
    private final UserProfileMapper mapper;
    private final EncryptionService crypto;
    private final PubSubTemplate pubSubTemplate;

    public UserProfileServiceImpl(
            UserProfileRepository userRepository,
            UserProfileMapper mapper,
            EncryptionService crypto,
            PubSubTemplate pubSubTemplate) {
        this.userRepository = userRepository;
        this.mapper = mapper;
        this.crypto = crypto;
        this.pubSubTemplate = pubSubTemplate;
    }

    @Override
    @Cacheable("profiles")
    public UserProfileResponseDto getById(String id) {
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
            // Log the error with more details
            logger.error("SSN encryption failed for user identified by email: {}", dto.getEmail(), e);
            // Wrap the original checked exception in a runtime exception.
            // This allows higher-level exception handlers (e.g., Spring's @ControllerAdvice)
            // to catch it and translate it into an appropriate HTTP response (like 500 Internal Server Error).
            // You could also define a custom runtime exception, e.g., SsnEncryptionException.
            throw new RuntimeException("Failed to encrypt Social Security Number during user creation.", e);
        }

        // 2) Map DTO → entity, set ID and encrypted SSN
        UserProfile user = mapper.toUser(dto);
        user.setId(UUID.randomUUID().toString());
        user.setEncryptedSocialSecurityNumber(encryptedSsn);

        // 3) Save to Datastore
        UserProfile savedUser = userRepository.save(user);
        logger.info("User profile with ID {} saved to Datastore.", savedUser.getId());

        // 4) Publish “user created” event to Pub/Sub
        // Consider the content of the message. Publishing just the ID is fine if the
        // subscriber is meant to be a simple notification or if it re-fetches details.
        // If the subscriber needs the full user data, you might publish the DTO or parts of it.
        pubSubTemplate.publish("users-create-topic", savedUser.getId().getBytes()); // Example: publishing user ID
        logger.info("Published user creation event for ID {} to Pub/Sub topic 'users-create-topic'.", savedUser.getId());


        // 5) Return the response DTO (without SSN)
        return mapper.toUserResponseDto(savedUser);
    }
    
    @Override
    @CacheEvict(value = "profiles", key = "#id")
    public UserProfileResponseDto update(String id, UserProfileUpdateRequestDto dto) {
        UserProfile user = userRepository.findById(id)
                .orElseThrow(() ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found: " + id
                    )
                );

        mapper.updateUserFromDto(dto, user);
        userRepository.save(user);
        return mapper.toUserResponseDto(user);
    }

    @Override
    @CacheEvict(value = "profiles", key = "#id")
    public void delete(String id) {
        userRepository.deleteById(id);
    }
}
