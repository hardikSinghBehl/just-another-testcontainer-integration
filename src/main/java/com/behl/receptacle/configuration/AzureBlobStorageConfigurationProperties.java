package com.behl.receptacle.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * Maps Azure Blob Storage configuration values defined in the active .yaml file to the instance
 * variables defined below. The configuration properties would be used to perform operations against
 * the provisioned Blob Storage container in Azure.
 * </p>
 * 
 * @see com.behl.receptacle.configuration.AzureBlobStorageConfiguration
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "com.behl.receptacle.azure.blob-storage")
public class AzureBlobStorageConfigurationProperties {

    /**
     * <p>
     * Name of the Azure Blob Storage container to be used as the destination for performing
     * operations against. This property corresponds to the key
     * <code>com.behl.receptacle.azure.blob-storage.container</code> in the active .yaml configuration
     * file.
     * </p>
     */
    @NotBlank(message = "Azure Blob Storage container name must be configured")
	private String container;

    /**
     * <p>
     * Connection string for accessing the Azure Blob Storage. The connection string includes the
     * necessary credentials and information to authenticate and access the Blob Storage service.
     * This property corresponds to the key
     * <code>com.behl.receptacle.azure.blob-storage.connection-string</code> in the active .yaml
     * configuration file.
     * </p>
     * 
     * @see https://docs.microsoft.com/en-us/azure/storage/common/storage-configure-connection-string
     */
    @NotBlank(message = "Azure Blob Storage connection string must be configured")
	private String connectionString;

}