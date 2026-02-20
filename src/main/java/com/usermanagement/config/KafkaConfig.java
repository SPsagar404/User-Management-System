package com.usermanagement.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;

@Configuration
@ConditionalOnBean(KafkaAdmin.class)
public class KafkaConfig {

    public static final String USER_REGISTRATION_TOPIC = "user.registration";
    public static final String USER_LOGIN_TOPIC = "user.login";

    @Bean
    public NewTopic registrationTopic() {
        return TopicBuilder.name(USER_REGISTRATION_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic loginTopic() {
        return TopicBuilder.name(USER_LOGIN_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
