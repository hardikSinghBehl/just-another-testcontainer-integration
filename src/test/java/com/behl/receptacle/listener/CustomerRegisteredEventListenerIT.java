package com.behl.receptacle.listener;

import static org.assertj.core.api.Assertions.assertThat;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.redpanda.RedpandaContainer;
import org.testcontainers.utility.DockerImageName;
import com.behl.receptacle.KafkaTestUtil;
import com.behl.receptacle.configuration.KafkaConfiguration;
import com.behl.receptacle.dto.CustomerDto;
import lombok.SneakyThrows;
import net.bytebuddy.utility.RandomString;

@SpringBootTest
@ActiveProfiles("test")
@EnableConfigurationProperties(KafkaConfiguration.class)
@EnableAutoConfiguration(exclude = FlywayAutoConfiguration.class)
public class CustomerRegisteredEventListenerIT {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private KafkaConfiguration kafkaConfiguration;

    private static RedpandaContainer kafkaContainer;

    static {
        kafkaContainer = new RedpandaContainer(DockerImageName.parse("docker.redpanda.com/redpandadata/redpanda:v22.2.13"));
        kafkaContainer.start();
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafkaContainer::getBootstrapServers);
    }

    @Test
    @SneakyThrows
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