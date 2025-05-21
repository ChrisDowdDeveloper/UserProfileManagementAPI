package com.christopherdowd.UserProfileManagement.pubsub;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.christopherdowd.UserProfileManagement.domain.UserProfile;
import com.christopherdowd.UserProfileManagement.dto.UserCreationMessageDto;
import com.christopherdowd.UserProfileManagement.repository.UserProfileRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.cloud.spring.pubsub.support.BasicAcknowledgeablePubsubMessage;

/*
 * Component responsible for subscribing to Google Cloud Pub/Sub topic
 * for user creation events and processing these messages.
 * It listens for messages indicating a new user has been requested for creation
 * then persists the user profile to the Datastore 
 */
@Component
public class UserCreatedSubscriber {

    private static final Logger logger = LoggerFactory.getLogger(UserCreatedSubscriber.class);

    private final PubSubTemplate pubSubTemplate;
    private final String subscriptionName;
    private final UserProfileRepository userRepository;
    private final ObjectMapper objectMapper; // used to deserialize the message

    /* 
     * Constructor for UserCreatedSubscriber
     * @param pubSubTemplate for interacting with Google Cloud Pub/Sub
     * @param subscriptionName of the Pub/Sub subscription, injection from application properties
     * @param userRepository for saving user profiles to Datastore
     * @param objectMapper for JSON processing
    */
    public UserCreatedSubscriber(
            PubSubTemplate pubSubTemplate,
            @Value("${pubsub.subscription}") String subscriptionName, 
            UserProfileRepository userRepository, 
            ObjectMapper objectMapper) {
        this.pubSubTemplate   = pubSubTemplate;
        this.subscriptionName = subscriptionName;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    /* 
     * Defines the logic for handling the incoming creation message
    */
    @PostConstruct
    public void start() {
        logger.info("Subscribing to Pub/Sub subscription: {}", subscriptionName);

        // Subscribe to the specified subscription
        pubSubTemplate.subscribe(subscriptionName, (BasicAcknowledgeablePubsubMessage msg) -> {

            // Extracts message as UTF-8 string
            String message = msg.getPubsubMessage().getData().toStringUtf8();

            logger.info("Received message from Pub/Sub subscription '{}'. Payload: {}", subscriptionName, message);

            try {
                // Deserializes JSON into a UserCreationMessageDto
                UserCreationMessageDto userDto = objectMapper.readValue(message, UserCreationMessageDto.class);

                logger.info("Processing new-user event for ID: {}. Username: {}", userDto.getId(), userDto.getUsername());

                // Creates a UserProfile entity from the deserialized DTO
                UserProfile userProfile = new UserProfile();
                userProfile.setId(userDto.getId());
                userProfile.setUsername(userDto.getUsername());
                userProfile.setEmail(userDto.getEmail());
                // SSN is encrypted by publisher in the service layer
                userProfile.setEncryptedSocialSecurityNumber(userDto.getEncryptedSocialSecurityNumber());

                // Save new user to the Datastore
                UserProfile savedUser = userRepository.save(userProfile);
                logger.info("User profile with ID {} successfully saved to Datastore by subscriber.", savedUser.getId());

                // Acknowledges the message to remove it from subscription
                msg.ack();
                logger.debug("Message acknowledged for ID: {}", userDto.getId());
            } catch(Exception e) {
                // Logs error if message fails to process
                logger.error("Error processing user creation message from Pub/Sub. Payload: {}. Error: {}", message, e.getMessage(), e);
                
                // Negatively acknowledges the message, so Pub/Sub can try to redeliver
                msg.nack();
                logger.warn("Message NACKed due to processing error. It may be redelivered");
            }
        });
    }
}
