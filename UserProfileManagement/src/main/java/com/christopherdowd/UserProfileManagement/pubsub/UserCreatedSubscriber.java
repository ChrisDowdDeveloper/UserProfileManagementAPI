package com.christopherdowd.UserProfileManagement.pubsub;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.cloud.spring.pubsub.support.BasicAcknowledgeablePubsubMessage;

@Component
public class UserCreatedSubscriber {

    private final PubSubTemplate pubSubTemplate;
    private final String subscriptionName;

    public UserCreatedSubscriber(
            PubSubTemplate pubSubTemplate,
            @Value("${pubsub.subscription}") String subscriptionName) {
        this.pubSubTemplate   = pubSubTemplate;
        this.subscriptionName = subscriptionName;
    }

    @PostConstruct
    public void start() {
        pubSubTemplate.subscribe(subscriptionName, (BasicAcknowledgeablePubsubMessage msg) -> {
            String userId = msg.getPubsubMessage().getData().toStringUtf8();
            System.out.println("Received new-user event for ID: " + userId);
            msg.ack();
        });
    }
}
