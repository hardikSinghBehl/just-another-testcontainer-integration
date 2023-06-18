package com.behl.receptacle.configuration;

import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
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
    
    @NotBlank
    private String customerRegisteredEvent;
  
    @NotBlank
    private String customerAccountRiskAssessment;
  
    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate(final KafkaProperties kafkaProperties) {
        final var kafkaProducerProperties = kafkaProperties.buildProducerProperties();
        final var producerFactory = new DefaultKafkaProducerFactory<String, Object>(kafkaProducerProperties);
        return new KafkaTemplate<>(producerFactory);
    }
    
    @Bean
    public ConsumerFactory<String, Object> consumerFactory(final KafkaProperties kafkaProperties) {
        final var kafkaConsumerProperties = kafkaProperties.buildConsumerProperties();
        return new DefaultKafkaConsumerFactory<>(kafkaConsumerProperties);
    }

}
