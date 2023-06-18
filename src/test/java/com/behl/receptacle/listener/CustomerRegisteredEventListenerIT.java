package com.behl.receptacle.listener;

import static org.assertj.core.api.Assertions.assertThat;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.utility.DockerImageName;
import com.behl.receptacle.IntroduceDelay;
import com.behl.receptacle.KafkaTestUtil;
import com.behl.receptacle.configuration.KafkaConfiguration;
import com.behl.receptacle.dto.CustomerDto;
import lombok.SneakyThrows;
import net.bytebuddy.utility.RandomString;

@SpringBootTest
@ActiveProfiles("test")
@ExtendWith(IntroduceDelay.Extension.class)
@EnableConfigurationProperties(KafkaConfiguration.class)
public class CustomerRegisteredEventListenerIT {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private KafkaConfiguration kafkaConfiguration;

    private static KafkaContainer kafkaContainer;

    static {
        kafkaContainer = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.0"));
        kafkaContainer.start();
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafkaContainer::getBootstrapServers);
    }

    @Test
    @SneakyThrows
    @IntroduceDelay(seconds = 5)
    void shouldConsumeCustomerRegisteredEventAndInitiateRiskAssessment() {
        final var customerRegisteredEventTopic = kafkaConfiguration.getCustomerRegisteredEvent();
        final var initiateRiskAssessmentTopic = kafkaConfiguration.getCustomerAccountRiskAssessment();
        final var customerDto = new CustomerDto();
        customerDto.setId(RandomString.make(10));
        customerDto.setFirstName(RandomString.make(10));
        customerDto.setLastName(RandomString.make(10));
        customerDto.setEmailId(RandomString.make() + "@domain.com");

        kafkaTemplate.send(customerRegisteredEventTopic, customerDto);

        TimeUnit.SECONDS.sleep(1);

        final var kafkaConsumer = KafkaTestUtil.getConsumer(kafkaContainer.getBootstrapServers(), initiateRiskAssessmentTopic);
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