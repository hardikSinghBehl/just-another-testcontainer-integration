package com.behl.receptacle.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
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
    private static final int PROXY_LISTENER_PORT = getRandomToxiproxyPort(); 
    private static final String LOOPBACK_ADDRESS =  InetAddress.getLoopbackAddress().getHostAddress();
    private static final String WILDCARD_ADDRESS = "0.0.0.0";
    private static final String REMOTE_PROXY_ADDRESS = getRandomValidIpAddress();
    
    private static final DockerImageName MOCKSERVER_IMAGE = DockerImageName.parse("mockserver/mockserver:5.15.0");
    private static final DockerImageName TOXIPROXY_IMAGE = DockerImageName.parse("ghcr.io/shopify/toxiproxy:2.5.0");
  
    static {
        final var network = Network.newNetwork();
      
        final var mockServerContainer = new MockServerContainer(MOCKSERVER_IMAGE).withNetwork(network);
        mockServerContainer.start();
        mockServerClient = new MockServerClient(mockServerContainer.getHost(), mockServerContainer.getServerPort());
    
        toxiproxyContainer = new ToxiproxyContainer(TOXIPROXY_IMAGE).withNetwork(network);
        toxiproxyContainer.start(); 
        toxiproxyClient = new ToxiproxyClient(toxiproxyContainer.getHost(), toxiproxyContainer.getControlPort());
    }
    
    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        // Configure the email client base URL as a remote address with the proxy port mapping
        final var baseUrl = String.format("http://%s:%d", REMOTE_PROXY_ADDRESS, toxiproxyContainer.getMappedPort(PROXY_LISTENER_PORT));
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
        
        // Prepare required addresses for proxy
        final var proxyListenerAddress = String.format("%s:%d", WILDCARD_ADDRESS, PROXY_LISTENER_PORT);
        final var mockServerAddress = String.format("%s:%d", LOOPBACK_ADDRESS, mockServerClient.getPort());
        
        // Set up toxiproxy and configure latency
        final var emailServerProxy = toxiproxyClient.createProxy("email-server", proxyListenerAddress, mockServerAddress);
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
    
    /**
     * Returns a random port from the available ports of ToxiProxyContainer. Currently,
     * ToxiProxyContainer will reserve 31 ports, starting at 8666.
     */
    private static int getRandomToxiproxyPort() {
        final var startPort = 8666;
        final var portCount = 31;
        return ThreadLocalRandom.current().nextInt(startPort, startPort + portCount);
    }
    
    /**
     * Returns a random valid IP address in the format "xxx.xxx.xxx.xxx", where
     * each "xxx" is a number between 0 and 255 inclusive.
     */
    private static String getRandomValidIpAddress() {
      final var random = new Random();
      final var maxOctetValue = 256;
      return String.format("%d.%d.%d.%d", random.nextInt(maxOctetValue), random.nextInt(maxOctetValue), random.nextInt(maxOctetValue), random.nextInt(maxOctetValue));
    }

}
