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

/* 
 * Service implementation for managing user profiles
 * Handles business logic related to CRUD operations, data encryption,
 * caching, and publishing events to Google Cloud Pub/Sub
*/
@Service
public class UserProfileServiceImpl implements UserProfileService {

    private static final Logger logger = LoggerFactory.getLogger(UserProfileServiceImpl.class);
    private final UserProfileRepository userRepository;
    private final UserProfileMapper mapper;
    private final EncryptionService crypto; // Service for encrypting/decrypting sensitive data
    private final PubSubTemplate pubSubTemplate; // Publishing messages to Pub/Sub
    private final ObjectMapper objectMapper; // Serializing objects to JSON for Pub/Sub messages

    // Injects Pub/Sub topic name from application.yml
    @Value("${pubsub.topic.users-creation-topic}")
    private String userCreationTopic;

    /*
     * Constructor for UserProfileServiceImpl
     * @param userRepository for UserProfile data access
     * @param mapper for converting between DTOs and entities
     * @param crypto for encryption operations
     * @param pubSubTemplate for interacting with Google Cloud Pub/Sub
     * @param objectMapper for JSON processing
     */
    public UserProfileServiceImpl(
            UserProfileRepository userRepository,
            UserProfileMapper mapper,
            EncryptionService crypto,
            PubSubTemplate pubSubTemplate,
            ObjectMapper objectMapper) {
        this.userRepository = userRepository;
        this.mapper = mapper;
        this.crypto = crypto;
        this.pubSubTemplate = pubSubTemplate;
        this.objectMapper = objectMapper;
    }

    /* 
     * Setter for user creation topic name
     * Used for testing purposes
     * @param userCreationTopic name of the pub/sub topic for user creation events
    */
    public void setUserCreationTopic(String userCreationTopic) {
        this.userCreationTopic = userCreationTopic;
    }

    /*
     * Retrieves user profile by ID
     * Results are cached in "profiles" cache
     * If user is not found, ResponseStatusException with HTTP 404 is thrown
     * @param id is the ID of user profile to retrieve
     * @return UserProfileResponseDto containing users data (exluding ssn)
     */
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

    /*
     * Retrieves all user profiles
     * @return a list of UserProfileResponseDto
     */
    @Override
    public List<UserProfileResponseDto> getAll() {
        logger.debug("Fetching all users from datastore");
        //Retrieves all user profile entities from repository
        Iterable<UserProfile> iterable = userRepository.findAll();
        //Converts iterable to List
        List<UserProfile> users = new ArrayList<>();
        // Equivalent to: For each item in iterable add to users array
        iterable.forEach(users::add);
        // Maps list of entities to list of response DTOs
        return mapper.toUserResponseDtoList(users);
    }

    /*
     * Creates new user profile
     */
    @Override
    public UserProfileResponseDto create(UserProfileRequestDto dto) {
        String encryptedSsn;

        try {
            // Encrypts the SSN
            encryptedSsn = crypto.encrypt(dto.getSocialSecurityNumber());
        } catch (Exception e) {
            logger.error("SSN encryption failed for user identified by email: {}", dto.getEmail(), e);
            throw new RuntimeException("Failed to encrypt Social Security Number during user creation.", e);
        }

        // Generates a unique ID for the new user
        String userId = UUID.randomUUID().toString();

        // Prepares message for Pub/Sub
        UserCreationMessageDto pubSubMessage = UserCreationMessageDto.builder()
            .id(userId)
            .username(dto.getUsername())
            .email(dto.getEmail())
            .encryptedSocialSecurityNumber(encryptedSsn) // uses encrypted ssn
            .build();

        try {
            // Serialize the message to JSON and publish to Pub/Sub
            String message = objectMapper.writeValueAsString(pubSubMessage);
            pubSubTemplate.publish(userCreationTopic, message);
            logger.info("Published user creation event for ID {} to Pub/Sub topic '{}'.", userId, userCreationTopic);
        } catch(Exception e) {
            logger.error("Failed to publish user creation event for email {}: {}", dto.getEmail(), e.getMessage(), e);
            throw new RuntimeException("Failed to publish user creation event.", e);
        }

        /*
         * Prepares and returns the immediate response to client
         * DTO confirms request was accepted, user persistance is asynchronous
         */
        UserProfileResponseDto responseDto = new UserProfileResponseDto();
        responseDto.setId(userId);
        responseDto.setUsername(dto.getUsername());
        responseDto.setEmail(dto.getEmail());

        logger.info("User creation request for ID {} accepted and published for asynchronous processing.", userId);
        return responseDto;
    }

    /*
     * Updates existing user profile by ID
     * Corresponding entry in the 'profiles' cache is evicted upon successful update
     * if user is not found, Response Status 404 is thrown
     * @param id the ID of the user profile to update
     * @param dto the UserProfileUpdateRequestDto containing the fields to update
     * @return UserProfileResponseDto of updated user
     */
    @Override
    @CacheEvict(value = "profiles", key = "#id")
    public UserProfileResponseDto update(String id, UserProfileUpdateRequestDto dto) {
        logger.debug("Updating user by ID: {}", id);
        // Finds existing user
        UserProfile user = userRepository.findById(id)
                .orElseThrow(() ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found: " + id
                    )
                );

        // Apply updates from DTO to the entity (SSN cannot be updated)
        mapper.updateUserFromDto(dto, user);

        // Save updated entity to repository
        UserProfile updatedUser = userRepository.save(user);

        logger.info("User profile with ID {} updated in Datastore.", updatedUser.getId());

        // Maps updated entity to response DTO
        return mapper.toUserResponseDto(updatedUser);
    }

    /* 
     * Deletes user profile by ID
     * Corresponding entry in 'profiles' cache is evicted upon successful deletion
     * If user is not found, Response Status 404 is thrown
     * @param id the ID of user profile to delete
    */
    @Override
    @CacheEvict(value = "profiles", key = "#id")
    public void delete(String id) {
        logger.debug("Deleting user by ID: {}", id);

        if(!userRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found for deletion: " + id);
        }

        userRepository.deleteById(id);

        logger.info("User profile with ID {} deleted from Datastore.", id);
    }
}