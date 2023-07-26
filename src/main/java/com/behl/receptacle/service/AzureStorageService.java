package com.behl.receptacle.service;

import java.io.IOException;
import java.util.Optional;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobStorageException;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AzureStorageService {

    private final BlobContainerClient blobContainerClient;

    /**
     * Saves the provided file to the configured Azure Blob Storage container.
     *
     * @param file The file to be saved.
     * @return {@code true} if the file is successfully saved to the Azure Blob Storage container,
     *         indicating a successful operation, and {@code false} if there was an error or the
     *         file couldn't be saved.
     * @throws IllegalArgumentException if the {@code file} parameter is {@code null}.
     */
    public Boolean save(@NonNull final MultipartFile file) {
        final var blobContainerName = blobContainerClient.getBlobContainerName();
        log.info("Saving file {} to azure blob container {}", file.getOriginalFilename(), blobContainerName);
        try {
            final var blobClient = blobContainerClient.getBlobClient(file.getOriginalFilename());
            blobClient.upload(file.getInputStream(), file.getSize(), true);
            log.info("File {} stored successfully in azure blob container {}", file.getOriginalFilename(), blobContainerName);
        } catch (final BlobStorageException | IOException exception) {
            log.error("Unable to store {} in azure blob container {} ", file.getOriginalFilename(), blobContainerName, exception);
            return Boolean.FALSE;
        }
        return Boolean.TRUE;
    }

    /**
     * Retrieves the blob with the specified key from the configured Azure Blob Storage container.
     *
     * @param blobKey The key of the blob to be retrieved.
     * @return An {@link Optional} containing the retrieved {@link InputStreamResource}, or an empty
     *         {@link Optional} if the blob is not found or encountered an error during retrieval.
     * @throws IllegalArgumentException if the {@code blobKey} parameter is {@code null}.
     */
    public Optional<InputStreamResource> retrieve(@NonNull final String blobKey) {
        final var blobContainerName = blobContainerClient.getBlobContainerName();
        log.info("Retrieving Blob {} from azure blob container {}", blobKey, blobContainerName);
        try {
            final var blobClient = blobContainerClient.getBlobClient(blobKey);
            final var inputStream = blobClient.downloadContent().toStream();
            return Optional.of(new InputStreamResource(inputStream));
        } catch (final BlobStorageException exception) {
            log.error("Unable to retreive blob {} from azure blob container {}", blobKey, blobContainerName, exception);
            return Optional.empty();
        }
    }

    /**
     * Deletes the blob with the specified key from the configured Azure Blob Storage container.
     *
     * @param blobKey The key of the blob to be deleted.
     * @throws IllegalArgumentException if the {@code blobKey} parameter is {@code null}.
     */
    public void delete(@NonNull final String blobKey) {
        final var blobContainerName = blobContainerClient.getBlobContainerName();
        log.info("Deleting blob {} from azure blob container {}", blobKey, blobContainerName);
        Boolean deletionPerformed;

        try {
            final var blobClient = blobContainerClient.getBlobClient(blobKey);
            deletionPerformed = blobClient.deleteIfExists(); 
        } catch (BlobStorageException exception) {
            log.error("Unable to delete blob {} from azure blob container {}", blobKey, blobContainerName, exception);
            throw exception;
        }

        if (Boolean.TRUE.equals(deletionPerformed)) {
            log.info("Blob {} deleted successfully from azure blob container {}", blobKey, blobContainerName);
        } else {
            log.warn("Blob {} not found in azure blob container {}. No deletion performed.", blobKey, blobContainerName);
        }
    }

}