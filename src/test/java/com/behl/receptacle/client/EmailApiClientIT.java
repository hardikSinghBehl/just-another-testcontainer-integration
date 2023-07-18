package com.behl.receptacle.client;

import static org.junit.Assert.assertThrows;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.verify.VerificationTimes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MockServerContainer;
import org.testcontainers.utility.DockerImageName;
import com.behl.receptacle.dto.EmailDispatchRequest;
import com.behl.receptacle.exception.ApiFailureException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import net.bytebuddy.utility.RandomString;

@SpringBootTest
@ActiveProfiles("test")
@EnableAutoConfiguration(exclude = FlywayAutoConfiguration.class)
public class EmailApiClientIT {
  
    @Autowired
    private EmailApiClient emailApiClient;  
  
    private static MockServerClient mockServerClient;
    private static final String EMAIL_SERVER_API_KEY  = RandomString.make(20);
  
    static {
        final var mockServerContainer = new MockServerContainer(DockerImageName.parse("mockserver/mockserver:5.15.0"));
        mockServerContainer.start();
        mockServerClient = new MockServerClient(mockServerContainer.getHost(), mockServerContainer.getServerPort());
    }
    
    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        final var baseUrl = "http://127.0.0.1:" + mockServerClient.getPort();
        registry.add("com.behl.receptacle.email.base-url", () -> baseUrl);
        registry.add("com.behl.receptacle.email.api-key", () -> EMAIL_SERVER_API_KEY);
    }
    
    @Test
    @SneakyThrows
    void shouldSendEmailNotificationForValidRequest() {
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
        
        // Send email notification
        emailApiClient.sendEmail(emailDispatchRequest);
        
        // Verify mock server interaction
        mockServerClient.verify(
            request()
                .withPath(EmailApiClient.SEND_EMAIL_API_PATH)
                .withHeader("Authorization", "Bearer " + EMAIL_SERVER_API_KEY)
                .withBody(emailDispatchJsonRequest).withMethod(HttpMethod.POST.name()),
            VerificationTimes.once()
        );
    }
    
    @Test
    @SneakyThrows
    void shouldThrowApiFailureExceptionIfEmailNotificationDispatchFails() {
        // Prepare test data
        final var recipient = RandomString.make(10) + "@domain.com";
        final var subject = RandomString.make(20);
        final var emailBody = RandomString.make(50);
        final var emailDispatchRequest = new EmailDispatchRequest(recipient, subject, emailBody);
        final var emailDispatchJsonRequest = new ObjectMapper().writeValueAsString(emailDispatchRequest);
        
        // Set up mock server to expect the egress HTTP request and return HttpStatus.503
        mockServerClient
            .when(request()
                .withPath(EmailApiClient.SEND_EMAIL_API_PATH)
                .withHeader("Authorization", "Bearer " + EMAIL_SERVER_API_KEY)
                .withBody(emailDispatchJsonRequest).withMethod(HttpMethod.POST.name()))
            .respond(response()
                .withStatusCode(503));
        
        // Send email notification and assert exception
        assertThrows(ApiFailureException.class, () -> emailApiClient.sendEmail(emailDispatchRequest));
        
        // Verify mock server interaction
        mockServerClient.verify(
            request()
                .withPath(EmailApiClient.SEND_EMAIL_API_PATH)
                .withHeader("Authorization", "Bearer " + EMAIL_SERVER_API_KEY)
                .withBody(emailDispatchJsonRequest).withMethod(HttpMethod.POST.name()),
            VerificationTimes.once()
        );
    }

}
