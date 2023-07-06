package com.behl.receptacle;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.mockserver.client.MockServerClient;
import org.springframework.context.annotation.Configuration;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MockServerContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.containers.localstack.LocalStackContainer.Service;
import org.testcontainers.redpanda.RedpandaContainer;
import org.testcontainers.utility.DockerImageName;
import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.utility.RandomString;

@Slf4j
@Configuration
public class TestContainerExtension implements BeforeAllCallback, ExtensionContext.Store.CloseableResource {

    private static boolean TEST_EXECUTION_STARTED = Boolean.FALSE;

    private static final String REDPANDA_IMAGE = "docker.redpanda.com/redpandadata/redpanda:v22.2.13";
    private static final String MYSQL_IMAGE = "mysql:8";
    private static final String REDIS_IMAGE = "redis:7.0.11-alpine3.18";
    private static final String LOCALSTACK_IMAGE = "localstack/localstack:2.1";
    private static final String MOCKSERVER_IMAGE = "mockserver/mockserver:5.15.0";

    private static final int redisPort = 6379;
    private static final String redisPassword = RandomString.make(10);
    private static final String BUCKET_NAME = RandomString.make(10).toLowerCase();
    private static final String EMAIL_SERVER_API_KEY  = RandomString.make(20);

    private static final RedpandaContainer kafkaContainer = new RedpandaContainer(DockerImageName.parse(REDPANDA_IMAGE));

    private static final MySQLContainer<?> mySQLContainer = new MySQLContainer<>(DockerImageName.parse(MYSQL_IMAGE));

    private static final GenericContainer<?> redisContainer = new GenericContainer<>(DockerImageName.parse(REDIS_IMAGE))
            .withExposedPorts(redisPort).withCommand("redis-server", "--requirepass", redisPassword);

    private static final LocalStackContainer localStackContainer = new LocalStackContainer(DockerImageName.parse(LOCALSTACK_IMAGE))
            .withServices(Service.S3);
    
    private static final MockServerContainer mockServerContainer = new MockServerContainer(DockerImageName.parse(MOCKSERVER_IMAGE));

    private static MockServerClient mockServerClient;
    
    @Override
    public void beforeAll(ExtensionContext context) {
        if (Boolean.FALSE.equals(TEST_EXECUTION_STARTED)) {
            log.info("Starting centralized message broker container");
            kafkaContainer.start();
            addKafkaProperties();
            log.info("Successfully initiated startup of centralized message broker container");

            log.info("Starting centralized datasource container");
            mySQLContainer.start();
            addDataSourceProperties();
            log.info("Successfully initiated startup of centralized datasource container");

            log.info("Starting centralized cache container");
            redisContainer.start();
            addCacheProperties();
            log.info("Successfully initiated startup of centralized cache container");

            log.info("Starting centralized AWS container");
            localStackContainer.start();
            addAwsProperties();
            log.info("Successfully initiated startup of centralized AWS container");
            
            log.info("Starting centralized Mockserver container");
            mockServerContainer.start();
            mockServerClient = new MockServerClient(mockServerContainer.getHost(), mockServerContainer.getServerPort());
            addMockserverProperties();
            log.info("Successfully initiated startup of centralized Mockserver container");

            TEST_EXECUTION_STARTED = Boolean.TRUE;
            context.getRoot().getStore(Namespace.GLOBAL).put(TestContainerExtension.class.getName(), this);
        }
    }

    @Override
    public void close() {
    }
    
    public static String getBootstrapServers() {
        return kafkaContainer.getBootstrapServers();
    }

    public static String getBucketName() {
        return BUCKET_NAME;
    }
    
    public static MockServerClient getMockServerClient() {
        return mockServerClient;
    }
    
    public static String getEmailServerApiKey() {
        return EMAIL_SERVER_API_KEY;
    }

    private void addKafkaProperties() {
        System.setProperty("spring.kafka.bootstrap-servers", kafkaContainer.getBootstrapServers());
    }

    private void addDataSourceProperties() {
        System.setProperty("spring.datasource.url", mySQLContainer.getJdbcUrl());
        System.setProperty("spring.datasource.username", mySQLContainer.getUsername());
        System.setProperty("spring.datasource.password", mySQLContainer.getPassword());
    }

    private void addCacheProperties() {
        System.setProperty("spring.data.redis.host", redisContainer.getHost());
        System.setProperty("spring.data.redis.port", String.valueOf(redisContainer.getMappedPort(redisPort)));
        System.setProperty("spring.data.redis.password", redisPassword);
    }

    private void addAwsProperties() {
        System.setProperty("com.behl.receptacle.aws.access-key", localStackContainer.getAccessKey());
        System.setProperty("com.behl.receptacle.aws.secret-access-key", localStackContainer.getSecretKey());
        System.setProperty("com.behl.receptacle.aws.s3.region", localStackContainer.getRegion());
        System.setProperty("com.behl.receptacle.aws.s3.endpoint", localStackContainer.getEndpoint().toString());
        System.setProperty("com.behl.receptacle.aws.s3.bucket-name", BUCKET_NAME);
    }
    
    private void addMockserverProperties() {
        final var baseUrl = "http://127.0.0.1:" + mockServerClient.getPort();
        System.setProperty("com.behl.receptacle.email.base-url", baseUrl);
        System.setProperty("com.behl.receptacle.email.api-key", EMAIL_SERVER_API_KEY);
    }

}