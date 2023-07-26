package com.behl.receptacle.configuration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(AzureBlobStorageConfigurationProperties.class)
public class AzureBlobStorageConfiguration {

    private final AzureBlobStorageConfigurationProperties azureBlobStorageConfigurationProperties;

    /**
     * <p>
     * Registers the {@link com.azure.storage.blob.BlobContainerClient} bean in the Spring IOC container
     * for communication with the Azure Blob Storage service. The client object is initialized with the
     * connection string and container name defined in the active .yaml configuration file.
     * </p>
     * 
     * @see com.behl.receptacle.configuration.AzureBlobStorageConfigurationProperties
     */
    @Bean
    public BlobContainerClient blobContainerClient() {
        final var connectionString = azureBlobStorageConfigurationProperties.getConnectionString();
        final var containerName = azureBlobStorageConfigurationProperties.getContainer();
        final var blobServiceClient = new BlobServiceClientBuilder().connectionString(connectionString).buildClient();
        return blobServiceClient.getBlobContainerClient(containerName);
    }

}