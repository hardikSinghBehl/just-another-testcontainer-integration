package com.behl.receptacle.listener;

import static org.assertj.core.api.Assertions.assertThat;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import com.behl.receptacle.KafkaTestUtil;
import com.behl.receptacle.TestContainerExtension;
import com.behl.receptacle.configuration.KafkaConfiguration;
import com.behl.receptacle.dto.CustomerDto;
import lombok.SneakyThrows;
import net.bytebuddy.utility.RandomString;

@SpringBootTest
@ActiveProfiles("test")
@ExtendWith(TestContainerExtension.class)
@EnableConfigurationProperties(KafkaConfiguration.class)
public class CustomerRegisteredEventListenerIT {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private KafkaConfiguration kafkaConfiguration;

    @Test
    @SneakyThrows
    void shouldConsumeCustomerRegisteredEventAndInitiateRiskAssessment() {
        // Fetch the topic names defined in the configuration file
        final var customerRegisteredEventTopic = kafkaConfiguration.getCustomerRegisteredEvent();
        final var initiateRiskAssessmentTopic = kafkaConfiguration.getCustomerAccountRiskAssessment();
        
        // create message object
        final var customerDto = new CustomerDto();
        customerDto.setId(RandomString.make(10));
        customerDto.setFirstName(RandomString.make(10));
        customerDto.setLastName(RandomString.make(10));
        customerDto.setEmailId(RandomString.make() + "@domain.com");

        // Send a message to customer registered event topic
        kafkaTemplate.send(customerRegisteredEventTopic, customerDto);

        // Verify that a message is received on the initiate risk assessment topic and contains the expected data
        final var kafkaConsumer = KafkaTestUtil.getConsumer(TestContainerExtension.getBootstrapServers(), initiateRiskAssessmentTopic);
        final var receivedMessages = kafkaConsumer.poll(Duration.ofMillis(5000));
        assertThat(receivedMessages.count()).isEqualTo(1);
        receivedMessages.forEach(receivedMessage -> {
            String receivedValue = receivedMessage.value().toString();
            assertThat(receivedValue).contains(customerDto.getId());
            assertThat(receivedValue).contains(customerDto.getFirstName());
            assertThat(receivedValue).contains(customerDto.getLastName());
            assertThat(receivedValue).contains(customerDto.getEmailId());
        });
    }

}