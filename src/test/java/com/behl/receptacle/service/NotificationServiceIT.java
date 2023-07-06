package com.behl.receptacle.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockserver.client.MockServerClient;
import org.mockserver.verify.VerificationTimes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.ActiveProfiles;
import com.behl.receptacle.TestContainerExtension;
import com.behl.receptacle.dto.EmailDispatchRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.bytebuddy.utility.RandomString;

@SpringBootTest
@ActiveProfiles("test")
@ExtendWith(TestContainerExtension.class)
public class NotificationServiceIT {
  
    @Autowired
    private NotificationService notificationService;  
  
    private static MockServerClient mockServerClient = TestContainerExtension.getMockServerClient();
    private static final String EMAIL_SERVER_API_KEY  = TestContainerExtension.getEmailServerApiKey();
    
    @Test
    void shouldSendEmailNotificationForValidRequest() throws Exception {
        // Prepare test data
        final var recipient = RandomString.make(10) + "@domain.com";
        final var subject = RandomString.make(20);
        final var emailBody = RandomString.make(50);
        final var emailDispatchRequest = new EmailDispatchRequest(recipient, subject, emailBody);
        final var emailDispatchJsonRequest = new ObjectMapper().writeValueAsString(emailDispatchRequest);
        
        // Set up mock server to expect the egress HTTP request
        mockServerClient
            .when(request()
                .withPath(NotificationService.SEND_EMAIL_API_PATH)
                .withHeader("Authorization", "Bearer " + EMAIL_SERVER_API_KEY)
                .withBody(emailDispatchJsonRequest).withMethod(HttpMethod.POST.name()))
            .respond(response()
                .withStatusCode(200));
        
        // Send email notification
        final var response = notificationService.sendEmail(emailDispatchRequest);
        
        // Verify response and mock server interaction
        assertThat(response).isTrue();
        mockServerClient.verify(
            request()
                .withPath(NotificationService.SEND_EMAIL_API_PATH)
                .withHeader("Authorization", "Bearer " + EMAIL_SERVER_API_KEY)
                .withBody(emailDispatchJsonRequest).withMethod(HttpMethod.POST.name()),
            VerificationTimes.once()
        );
    }
    
    @Test
    void shouldReturnFalseIfEmailNotificationDispatchFails() throws Exception {
        // Prepare test data
        final var recipient = RandomString.make(10) + "@domain.com";
        final var subject = RandomString.make(20);
        final var emailBody = RandomString.make(50);
        final var emailDispatchRequest = new EmailDispatchRequest(recipient, subject, emailBody);
        final var emailDispatchJsonRequest = new ObjectMapper().writeValueAsString(emailDispatchRequest);
        
        // Set up mock server to expect the egress HTTP request and return HttpStatus.503
        mockServerClient
            .when(request()
                .withPath(NotificationService.SEND_EMAIL_API_PATH)
                .withHeader("Authorization", "Bearer " + EMAIL_SERVER_API_KEY)
                .withBody(emailDispatchJsonRequest).withMethod(HttpMethod.POST.name()))
            .respond(response()
                .withStatusCode(503));
        
        // Send email notification
        final var response = notificationService.sendEmail(emailDispatchRequest);
        
        // Verify response and mock server interaction
        assertThat(response).isFalse();
        mockServerClient.verify(
            request()
                .withPath(NotificationService.SEND_EMAIL_API_PATH)
                .withHeader("Authorization", "Bearer " + EMAIL_SERVER_API_KEY)
                .withBody(emailDispatchJsonRequest).withMethod(HttpMethod.POST.name()),
            VerificationTimes.once()
        );
    }

}
