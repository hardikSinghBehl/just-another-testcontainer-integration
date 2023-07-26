package com.behl.receptacle.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.multipart.MultipartFile;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import lombok.SneakyThrows;
import net.bytebuddy.utility.RandomString;

@SpringBootTest
@ActiveProfiles("test")
@SuppressWarnings("resource")
@EnableAutoConfiguration(exclude = FlywayAutoConfiguration.class)
public class AzureStorageServiceIT {
  
    @Autowired
    private AzureStorageService azureStorageService;
    
    @Autowired
    private BlobContainerClient blobContainerClient;
    
    private static final AzureStorageService invalidAzureStorageService;
    
    private static GenericContainer<?> azureBlobStorageContainer;
    
    private static final String CONTAINER_NAME = RandomString.make().toLowerCase();
    private static final String CONTAINER_CONNECTION_STRING;
    private static final String FILE_CONTENT_TYPE = "text/plain";

    static {
        // Initialize and start Azurite Docker Container
        azureBlobStorageContainer = new GenericContainer<>(DockerImageName.parse("mcr.microsoft.com/azure-storage/azurite:3.23.0"))
            .withExposedPorts(10000).withCommand("azurite-blob", "--skipApiVersionCheck", "--blobHost", "0.0.0.0");
        azureBlobStorageContainer.start();
        
        // Construct connection string for Azure Blob Storage container
        final var defaultAzuriteConnectionString = "DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;AccountKey=Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==;BlobEndpoint=http://127.0.0.1:%s/devstoreaccount1;";
        CONTAINER_CONNECTION_STRING = String.format(defaultAzuriteConnectionString, azureBlobStorageContainer.getMappedPort(10000));
    
        // Create container in Azure Blob Storage
        final var blobServiceClient = new BlobServiceClientBuilder().connectionString(CONTAINER_CONNECTION_STRING).buildClient();
        blobServiceClient.createBlobContainer(CONTAINER_NAME);
        
        // Constructing invalid Azure Storage Service for testing negative scenarios
        final var invalidContainerName = RandomString.make().toLowerCase();
        final var invalidBlobContainerClient = blobServiceClient.getBlobContainerClient(invalidContainerName);
        invalidAzureStorageService = new AzureStorageService(invalidBlobContainerClient);
    }
    
    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("com.behl.receptacle.azure.blob-storage.container", () -> CONTAINER_NAME);
        registry.add("com.behl.receptacle.azure.blob-storage.connection-string", () -> CONTAINER_CONNECTION_STRING);
    }
    
    @Test
    void shouldSaveBlobSuccessfullyToContainer() {
        // Prepate test file to upload
        final var fileName = RandomString.make() + ".txt";
        final var fileContent = RandomString.make(50);
        final var fileToUpload = createTextFile(fileName, fileContent);
        
        // Save the generated file using the azure storage service
        final var result = azureStorageService.save(fileToUpload);
        
        // Verify that the file is saved successfully by checking if it exists in the container
        final var blobClient = blobContainerClient.getBlobClient(fileName);
        assertThat(result).isTrue();
        assertThat(blobClient.exists()).isTrue();        
        assertEquals(fileContent, new String(blobClient.downloadContent().toBytes()));
    }
    
    @Test
    void shouldNotSaveBlobToNonExistentContainer() {
        // Prepate test file to upload
        final var fileName = RandomString.make() + ".txt";
        final var fileContent = RandomString.make(50);
        final var fileToUpload = createTextFile(fileName, fileContent);
        
        // Save the generated file using invalid azure storage service
        final var result = invalidAzureStorageService.save(fileToUpload);
        
        // Verify that the file is not saved in azure blob storage container
        assertThat(result).isFalse();
    }
    
    @Test
    @SneakyThrows
    void shouldFetchSavedBlobSuccessfullyFromContainerForValidFileName() {
        // Prepate test file and upload to azure blob container
        final var fileName = RandomString.make() + ".txt";
        final var fileContent = RandomString.make(50);
        final var fileToUpload = createTextFile(fileName, fileContent);
        azureStorageService.save(fileToUpload);
        
        // Retrieve the blob from the storage service using fileName
        final var retrievedBlob = azureStorageService.retrieve(fileName);
        
        // Read the retrieved content and assert integrity
        assertThat(retrievedBlob.isPresent()).isTrue();
        assertEquals(fileContent, new String(retrievedBlob.get().getContentAsByteArray()));
    }
    
    @Test
    void shouldReturnEmptyObjectFromContainerForInvalidKey() {
        // Generate an invalid key
        final var fileName = RandomString.make() + ".txt";
        
        // Retrieve the file from the azure storage service using invalid fileName
        final var retrievedBlob = azureStorageService.retrieve(fileName);
       
        // Verify that the retrieved blob is empty
        assertThat(retrievedBlob.isEmpty()).isTrue();
    }
    
    @Test
    void shouldDeleteBlobFromContainerSuccessfully() {
        // Prepate test file and upload to azure blob container
        final var fileName = RandomString.make() + ".txt";
        final var fileContent = RandomString.make(50);
        final var fileToUpload = createTextFile(fileName, fileContent);
        azureStorageService.save(fileToUpload);
        
        // Verify that the blob has been saved in the container
        final var retrievedBlob = azureStorageService.retrieve(fileName);
        assertThat(retrievedBlob.isPresent()).isTrue();

        // Delete Blob from container using azure storage service
        azureStorageService.delete(fileName);
        
        // Verify that the blob does not exist in the container post deletion
        final var retrievedBlobAfterDeletion = azureStorageService.retrieve(fileName);
        assertThat(retrievedBlobAfterDeletion.isEmpty()).isTrue();
    }
    
    @SneakyThrows
    private MultipartFile createTextFile(final String fileName, final String content) {
        byte[] fileContentBytes = content.getBytes();
        InputStream inputStream = new ByteArrayInputStream(fileContentBytes);
  
        return new MockMultipartFile(fileName, fileName, FILE_CONTENT_TYPE, inputStream);
    }

}
