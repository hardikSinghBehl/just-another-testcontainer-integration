package com.behl.receptacle.service;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import com.behl.receptacle.configuration.EmailClientConfigurationProperties;
import com.behl.receptacle.dto.EmailDispatchRequest;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@EnableConfigurationProperties(EmailClientConfigurationProperties.class)
public class NotificationService {
  
    private final RestTemplate restTemplate;
    private final EmailClientConfigurationProperties emailClientConfigurationProperties;
  
    protected static final String SEND_EMAIL_API_PATH = "/api/v1/send-email";
  
    /**
     * Sends an email notification to the specified recipient using the configured Email API client.
     *
     * @return {@code true} if the email notification is successfully sent, {@code false} otherwise.
     * @throws IllegalArgumentException if the {@link emailDispatchRequest} parameter is {@code null}.
     * 
     * @see com.behl.receptacle.configuration.EmailClientConfigurationProperties
     */
    public boolean sendEmail(@NonNull final EmailDispatchRequest emailDispatchRequest) { 
        log.info("Sending email notification to {}", emailDispatchRequest.getRecipient());
        
        final var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(emailClientConfigurationProperties.getApiKey());

        final var apiUrl = emailClientConfigurationProperties.getBaseUrl() + SEND_EMAIL_API_PATH;
        final var requestEntity = new HttpEntity<EmailDispatchRequest>(emailDispatchRequest, headers);
        
        try {
            restTemplate.exchange(apiUrl, HttpMethod.POST, requestEntity, Void.class); 
        } catch (HttpClientErrorException | HttpServerErrorException exception) {
            log.error("Unable to send email notification to {}", emailDispatchRequest.getRecipient(), exception);
            return Boolean.FALSE;
        }
        log.info("Successfully sent email notification to {}", emailDispatchRequest.getRecipient());
        return Boolean.TRUE;
    }

}
