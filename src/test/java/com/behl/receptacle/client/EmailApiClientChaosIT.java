package com.behl.receptacle.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.ResourceAccessException;
import org.testcontainers.containers.MockServerContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.ToxiproxyContainer;
import org.testcontainers.utility.DockerImageName;
import com.behl.receptacle.dto.EmailDispatchRequest;
import com.behl.receptacle.exception.ApiUnreachableException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.rekawek.toxiproxy.ToxiproxyClient;
import eu.rekawek.toxiproxy.model.ToxicDirection;
import lombok.SneakyThrows;
import net.bytebuddy.utility.RandomString;

@SpringBootTest
@ActiveProfiles("test")
@SuppressWarnings("resource")
@EnableAutoConfiguration(exclude = FlywayAutoConfiguration.class)
public class EmailApiClientChaosIT {
  
    @Autowired
    private EmailApiClient emailApiClient;  
  
    private static MockServerClient mockServerClient;
    private static ToxiproxyClient toxiproxyClient;
    private static ToxiproxyContainer toxiproxyContainer;
    
    private static final String EMAIL_SERVER_API_KEY  = RandomString.make(20);
  
    static {
        final var network = Network.newNetwork();
      
        final var mockServerContainer = new MockServerContainer(DockerImageName.parse("mockserver/mockserver:5.15.0")).withNetwork(network);
        mockServerContainer.start();
        mockServerClient = new MockServerClient(mockServerContainer.getHost(), mockServerContainer.getServerPort());
    
        toxiproxyContainer = new ToxiproxyContainer(DockerImageName.parse("ghcr.io/shopify/toxiproxy:2.5.0")).withNetwork(network);
        toxiproxyContainer.start(); 
        toxiproxyClient = new ToxiproxyClient(toxiproxyContainer.getHost(), toxiproxyContainer.getControlPort());
    }
    
    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        final var baseUrl = "http://127.0.0.2:" + toxiproxyContainer.getMappedPort(8666);
        registry.add("com.behl.receptacle.email.base-url", () -> baseUrl);
        registry.add("com.behl.receptacle.email.api-key", () -> EMAIL_SERVER_API_KEY);
    }
    
    @Test
    @SneakyThrows
    void shouldThrowApiUnreachableExceptionIfNotificationDispatchTimesOut() {
        // Prepare test data
        final var recipient = RandomString.make(10) + "@domain.com";
        final var subject = RandomString.make(20);
        final var emailBody = RandomString.make(50);
        final var emailDispatchRequest = new EmailDispatchRequest(recipient, subject, emailBody);
        final var emailDispatchJsonRequest = new ObjectMapper().writeValueAsString(emailDispatchRequest);
        
        // Set up mock server to expect the egress HTTP request
        mockServerClient
            .when(request()
                .withPath(EmailApiClient.SEND_EMAIL_API_PATH)
                .withHeader("Authorization", "Bearer " + EMAIL_SERVER_API_KEY)
                .withBody(emailDispatchJsonRequest).withMethod(HttpMethod.POST.name()))
            .respond(response()
                .withStatusCode(200));
        
        // Set up toxiproxy and configure latency
        final var emailServerProxy = toxiproxyClient.createProxy("email-server", "127.0.0.2:8666", "127.0.0.1:" + mockServerClient.getPort());
        emailServerProxy.toxics().latency("email-server-latency", ToxicDirection.DOWNSTREAM, Duration.ofSeconds(30).toMillis());
        
        // Send email notification and verify exception thrown
        final var stopWatch = new StopWatch();
        stopWatch.start();
        final var exception = assertThrows(ApiUnreachableException.class, () -> emailApiClient.sendEmail(emailDispatchRequest));
        stopWatch.stop();
        
        // Verify exception type 
        assertTrue(exception.getCause() instanceof ResourceAccessException);
        assertTrue(exception.getCause().getCause() instanceof SocketTimeoutException);
        assertThat(exception.getMessage()).contains("Connect timed out");
        
        // Verify time between invocation of API and timeout exception, corresponds to timeout set in RestTemplate
        // com.behl.receptacle.configuration.RestTemplateConfiguration
        assertTrue(stopWatch.getTime(TimeUnit.SECONDS) >= 10);
    }

}
