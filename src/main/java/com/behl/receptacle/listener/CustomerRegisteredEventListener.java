package com.behl.receptacle.listener;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import com.behl.receptacle.configuration.KafkaConfiguration;
import com.behl.receptacle.dto.CustomerDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@EnableConfigurationProperties(KafkaConfiguration.class)
public class CustomerRegisteredEventListener {
  
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final KafkaConfiguration kafkaConfiguration;

    /**
     * Listens to the messages sent to the customer registration event topic and initiates the risk
     * assessment process for the corresponding registered customer record by making an asynchronous
     * call to the customer risk assessment topic.
     * 
     * @see KafkaConfiguration#getCustomerRegisteredEvent()
     * @see KafkaConfiguration#getCustomerAccountRiskAssessment()
     */
    @SneakyThrows
    @KafkaListener(topics = "${com.behl.receptacle.kafka.topic-name.customer-registered-event}",
            groupId = "customer-registered-event-consumer")
    public void execute(final String message) {
        final var customerDto = new ObjectMapper().readValue(message, CustomerDto.class);
        log.info("Received confirmation of account registration event for customer {}", customerDto.getId());
        
        final var initiateRiskAssessmentTopic = kafkaConfiguration.getCustomerAccountRiskAssessment();
        kafkaTemplate.send(initiateRiskAssessmentTopic, customerDto);
        log.info("Successfully sent command to initiate risk assessment for customer {}", customerDto.getId());
    }

}