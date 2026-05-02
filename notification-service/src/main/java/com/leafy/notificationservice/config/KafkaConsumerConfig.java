package com.leafy.notificationservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;

/**
 * Configures Kafka listener container factories for the two-stage alert notification pipeline.
 *
 * <p>Both stages use {@link ContainerProperties.AckMode#MANUAL_IMMEDIATE} so that each
 * consumer controls exactly when the offset is committed:
 * <ul>
 *   <li>On success → {@code acknowledgment.acknowledge()} commits the offset.</li>
 *   <li>On failure → no ACK, RuntimeException re-thrown → Kafka retries the message.</li>
 * </ul>
 */
@Configuration
public class KafkaConsumerConfig {

    /**
     * Shared listener container factory used by both pipeline stages.
     * Spring Boot auto-configures the underlying {@link ConsumerFactory} from
     * {@code spring.kafka.consumer.*} properties.
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(
            ConsumerFactory<String, String> consumerFactory) {

        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        return factory;
    }
}
