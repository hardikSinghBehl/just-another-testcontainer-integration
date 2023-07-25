package com.behl.receptacle.service;

import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;
import com.azure.storage.blob.BlobServiceClientBuilder;
import net.bytebuddy.utility.RandomString;

@SpringBootTest
@ActiveProfiles("test")
@SuppressWarnings("resource")
@TestInstance(Lifecycle.PER_CLASS)
@EnableAutoConfiguration(exclude = FlywayAutoConfiguration.class)
public class AzureStorageServiceIT {
  
    @Autowired
    private AzureStorageService azureStorageService;
  
    private static GenericContainer<?> azureBlobStorageContainer;
    
    private static final String CONTAINER_NAME = RandomString.make();
    private static final String CONTAINER_CONNECTION_STRING;
    
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
    }
    
    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("com.behl.receptacle.azure.blob-storage.container", () -> CONTAINER_NAME);
        registry.add("com.behl.receptacle.azure.blob-storage.connection-string", () -> CONTAINER_CONNECTION_STRING);
    }

}
