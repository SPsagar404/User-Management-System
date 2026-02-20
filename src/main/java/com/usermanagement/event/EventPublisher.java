package com.usermanagement.event;

import com.usermanagement.config.KafkaConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventPublisher {

    private final KafkaTemplate<String, UserEvent> kafkaTemplate;

    @Async
    public void publishRegistrationEvent(UserEvent event) {
        try {
            kafkaTemplate.send(
                    KafkaConfig.USER_REGISTRATION_TOPIC,
                    event.getEmail(),
                    event);
            log.info("Published registration event for user: {}", event.getEmail());
        } catch (Exception e) {
            log.error("Failed to publish registration event for user: {}", event.getEmail(), e);
        }
    }

    @Async
    public void publishLoginEvent(UserEvent event) {
        try {
            kafkaTemplate.send(
                    KafkaConfig.USER_LOGIN_TOPIC,
                    event.getEmail(),
                    event);
            log.info("Published login event for user: {}", event.getEmail());
        } catch (Exception e) {
            log.error("Failed to publish login event for user: {}", event.getEmail(), e);
        }
    }
}
