package com.behl.receptacle.configuration;

import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Validated
@Configuration
@ConfigurationProperties(prefix = "com.behl.receptacle.kafka.topic-name")
public class KafkaConfiguration {
    
    /**
     * <p>
     * The kafka topic name to be consumed for Customer registration event. This property corresponds
     * to the key <code>com.behl.receptacle.kafka.topic-name.customer-registered-event</code> in the
     * active .yaml configuration file.
     * </p>
     */
    @NotBlank(message = "customer registered event topic name must be configured")
    private String customerRegisteredEvent;
  
    /**
     * <p>
     * The kafka topic name to which customer account risk assessment initiation message is to be
     * published. This property corresponds to the key
     * <code>com.behl.receptacle.kafka.topic-name.customer-account-risk-assessment</code> in the
     * active .yaml configuration file.
     * </p>
     */
    @NotBlank(message = "customer account risk assessment topic name must be configured")
    private String customerAccountRiskAssessment;
  
    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate(final KafkaProperties kafkaProperties) {
        final var kafkaProducerProperties = kafkaProperties.buildProducerProperties();
        final var producerFactory = new DefaultKafkaProducerFactory<String, Object>(kafkaProducerProperties);
        producerFactory.setValueSerializer(new JsonSerializer<>());
        return new KafkaTemplate<>(producerFactory);
    }
    
    @Bean
    public ConsumerFactory<String, Object> consumerFactory(final KafkaProperties kafkaProperties) {
        final var kafkaConsumerProperties = kafkaProperties.buildConsumerProperties();
        return new DefaultKafkaConsumerFactory<>(kafkaConsumerProperties);
    }

}
