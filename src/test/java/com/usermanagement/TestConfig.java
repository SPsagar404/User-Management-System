package com.usermanagement;

import com.usermanagement.event.UserEvent;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import static org.mockito.Mockito.mock;

@TestConfiguration
public class TestConfig {

    @SuppressWarnings("unchecked")
    @Bean
    public ProducerFactory<String, UserEvent> producerFactory() {
        return mock(ProducerFactory.class);
    }

    @Bean
    public KafkaTemplate<String, UserEvent> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}
