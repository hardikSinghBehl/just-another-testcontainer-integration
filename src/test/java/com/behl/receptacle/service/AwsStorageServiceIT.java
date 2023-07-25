package com.behl.receptacle.service;

import static com.amazonaws.HttpMethod.GET;
import static com.amazonaws.HttpMethod.PUT;
import static org.assertj.core.api.Assertions.assertThat;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.containers.localstack.LocalStackContainer.Service;
import org.testcontainers.utility.DockerImageName;
import com.amazonaws.services.s3.AmazonS3;
import com.behl.receptacle.configuration.AwsS3ConfigurationProperties;
import lombok.SneakyThrows;
import net.bytebuddy.utility.RandomString;

@SpringBootTest
@ActiveProfiles("test")
@TestInstance(Lifecycle.PER_CLASS)
@EnableAutoConfiguration(exclude = FlywayAutoConfiguration.class)
@EnableConfigurationProperties(AwsS3ConfigurationProperties.class)
class AwsStorageServiceIT {
    
    @Autowired
    private AwsStorageService awsStorageService;  
    
    @Autowired
    private AmazonS3 amazonS3;
    
    @Autowired
    private AwsS3ConfigurationProperties awsS3ConfigurationProperties;
    
    private static LocalStackContainer localStackContainer;
    private static String FILE_CONTENT_TYPE = "text/plain";
    private static String BUCKET_NAME = RandomString.make(10).toLowerCase();

    static { 
        localStackContainer = new LocalStackContainer(DockerImageName.parse("localstack/localstack:2.1"))
                .withServices(Service.S3);
        localStackContainer.start();
    }
    
    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("com.behl.receptacle.aws.access-key", localStackContainer::getAccessKey);
        registry.add("com.behl.receptacle.aws.secret-access-key", localStackContainer::getSecretKey);
        registry.add("com.behl.receptacle.aws.s3.region", localStackContainer::getRegion);
        registry.add("com.behl.receptacle.aws.s3.endpoint", localStackContainer::getEndpoint);
        registry.add("com.behl.receptacle.aws.s3.bucket-name", () -> BUCKET_NAME);
    }
    
    @BeforeAll
    void setUp() {
        amazonS3.createBucket(BUCKET_NAME);
    }
    
    @Test
    void shouldSaveFileSuccessfullyToBucket() {
        // Prepate test file to upload
        final var key = RandomString.make(10) + ".txt";
        final var fileContent = RandomString.make(50);
        final var fileToUpload = createTextFile(key, fileContent);
        
        // Save the generated file to the storage service
        final var result = awsStorageService.save(fileToUpload);
        
        // Verify that the file is saved successfully by checking if it exists in the bucket
        final var savedObjects = amazonS3.listObjects(BUCKET_NAME).getObjectSummaries();
        assertThat(result).isTrue();
        assertThat(savedObjects).anyMatch(objectSummary -> objectSummary.getKey().equals(key));
    }
    
    @Test
    void shouldNotSaveFileToNonexistentBucket() {
        // Prepate test file to upload
        final var key = RandomString.make(10) + ".txt";
        final var fileContent = RandomString.make(50);
        final var fileToUpload = createTextFile(key, fileContent);
        
        // Configure a non-existent bucket name
        final var nonExistingBucketName = RandomString.make(20).toLowerCase();
        awsS3ConfigurationProperties.getS3().setBucketName(nonExistingBucketName);
        
        // Save the generated file to the storage service
        final var result = awsStorageService.save(fileToUpload);
        
        // Verify that the file is not saved
        assertThat(result).isFalse();
        
        // Reset the bucket name to the original value
        awsS3ConfigurationProperties.getS3().setBucketName(BUCKET_NAME);
    }
    
    @Test
    void shouldFetchSavedFileSuccessfullyFromBucketForValidKey() {
        // Prepate test file and upload to storage service
        final var key = RandomString.make(10) + ".txt";
        final var fileContent = RandomString.make(50);
        final var fileToUpload = createTextFile(key, fileContent);
        awsStorageService.save(fileToUpload);
        
        // Retrieve the file from the storage service using prepared key
        final var retrievedObject = awsStorageService.retrieve(key);
        
        // Read the retrieved content and assert integrity
        final var retrievedContent = new BufferedReader(new InputStreamReader(retrievedObject.get().getObjectContent()))
            .lines().collect(Collectors.joining("\n"));
        assertThat(retrievedObject.isPresent()).isTrue();
        assertThat(retrievedObject.get().getKey()).isEqualTo(key);
        assertThat(retrievedContent).isEqualTo(fileContent);    
        assertThat(retrievedObject.get().getObjectMetadata().getContentType()).isEqualTo(FILE_CONTENT_TYPE);
        assertThat(retrievedObject.get().getObjectMetadata().getContentDisposition()).isEqualTo(key);
    }
    
    @Test
    void shouldReturnEmptyObjectFromBucketForInvalidKey() {
        // Generate an invalid key
        final var key = RandomString.make(10) + ".txt";
        
        // Retrieve the file from the storage service using the invalid key
        final var retrievedObject = awsStorageService.retrieve(key);
       
        // Verify that the retrieved object is empty
        assertThat(retrievedObject.isEmpty()).isTrue();
    }
    
    @Test
    @SneakyThrows
    void shouldGeneratePresignedUrlAndUploadObjectToBucket() {
        // Prepate test file to upload
        final var key = RandomString.make(10) + ".txt";
        final var fileContent = RandomString.make(50);
        final var fileToUpload = createTextFile(key, fileContent);
        
        // Generate a presigned URL for uploading the object to the bucket
        final var presignedUrl = awsStorageService.generatePresignedUrl(key, PUT);
        
        // Upload the test file using the presgined URL and verify that it's saved in the bucket
        final var httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.TEXT_PLAIN);
        final var requestEntity = new RequestEntity<>(fileToUpload.getBytes(), httpHeaders, HttpMethod.PUT, new URI(presignedUrl));
        final var responseEntity = new RestTemplate().exchange(requestEntity, Void.class);
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        final var savedObjects = amazonS3.listObjects(BUCKET_NAME).getObjectSummaries();
        assertThat(savedObjects).anyMatch(objectSummary -> objectSummary.getKey().equals(key));
    }
    
    @Test
    @SneakyThrows
    void shouldGeneratePresignedUrlToFetchStoredObjectFromBucket() {
        // Prepate test file and upload to storage service
        final var key = RandomString.make(10) + ".txt";
        final var fileContent = RandomString.make(50);
        final var fileToUpload = createTextFile(key, fileContent);
        awsStorageService.save(fileToUpload);

        // Generate a presigned URL for fetching the stored object from the bucket
        final var presignedUrl = awsStorageService.generatePresignedUrl(key, GET);

        // Perform a GET request to the presigned URL and verify the retrieved content matches the expected file content.
        final var restTemplate = new RestTemplate();
        final var requestEntity = new RequestEntity<>(HttpMethod.GET, new URI(presignedUrl));
        final var responseEntity = restTemplate.exchange(requestEntity, byte[].class);
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        final var retrievedContent = new String(responseEntity.getBody(), StandardCharsets.UTF_8);
        assertThat(retrievedContent).isEqualTo(fileContent);
    }
    
    @SneakyThrows
    private MultipartFile createTextFile(final String fileName, final String content) {
        byte[] fileContentBytes = content.getBytes();
        InputStream inputStream = new ByteArrayInputStream(fileContentBytes);
  
        return new MockMultipartFile(fileName, fileName, FILE_CONTENT_TYPE, inputStream);
    }

}
