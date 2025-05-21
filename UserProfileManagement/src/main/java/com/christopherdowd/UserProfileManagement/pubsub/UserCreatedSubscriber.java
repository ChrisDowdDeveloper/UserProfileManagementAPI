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

@Component
public class UserCreatedSubscriber {

    private static final Logger logger = LoggerFactory.getLogger(UserCreatedSubscriber.class);

    private final PubSubTemplate pubSubTemplate;
    private final String subscriptionName;
    private final UserProfileRepository userRepository;
    private final ObjectMapper objectMapper;

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

    @PostConstruct
    public void start() {
        logger.info("Subscribing to Pub/Sub subscription: {}", subscriptionName);
        pubSubTemplate.subscribe(subscriptionName, (BasicAcknowledgeablePubsubMessage msg) -> {
            String message = msg.getPubsubMessage().getData().toStringUtf8();
            logger.info("Received message from Pub/Sub subscription '{}'. Payload: {}", subscriptionName, message);

            try {
                UserCreationMessageDto userDto = objectMapper.readValue(message, UserCreationMessageDto.class);

                logger.info("Processing new-user event for ID: {}. Username: {}", userDto.getId(), userDto.getUsername());

                UserProfile userProfile = new UserProfile();
                userProfile.setId(userDto.getId());
                userProfile.setUsername(userDto.getUsername());
                userProfile.setEmail(userDto.getEmail());
                userProfile.setEncryptedSocialSecurityNumber(userDto.getEncryptedSocialSecurityNumber());

                UserProfile savedUser = userRepository.save(userProfile);
                logger.info("User profile with ID {} successfully saved to Datastore by subscriber.", savedUser.getId());

                msg.ack();
                logger.debug("Message acknowledged for ID: {}", userDto.getId());
            } catch(Exception e) {
                logger.error("Error processing user creation message from Pub/Sub. Payload: {}. Error: {}", message, e.getMessage(), e);
                msg.nack();
                logger.warn("Message NACKed due to processing error. It may be redelivered");
            }
        });
    }
}
