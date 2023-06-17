package com.behl.receptacle.service;

import java.io.IOException;
import java.net.URL;
import java.util.Optional;
import org.joda.time.LocalDateTime;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.amazonaws.HttpMethod;
import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.behl.receptacle.configuration.AwsS3ConfigurationProperties;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@EnableConfigurationProperties(value = AwsS3ConfigurationProperties.class)
public class StorageService {

    private final AmazonS3 amazonS3;
    private final AwsS3ConfigurationProperties awsS3ConfigurationProperties;

    public Boolean save(@NonNull final MultipartFile file) {
        final var bucketName = awsS3ConfigurationProperties.getS3().getBucketName();
        final var key = file.getOriginalFilename();
        final var objectMetaData = constructMetadata(file);
        log.info("Saving file {} to S3 bucket {}", file.getOriginalFilename(), bucketName);

        try {
            amazonS3.putObject(bucketName, key, file.getInputStream(), objectMetaData);
            log.info("File {} stored successfully in S3 bucket {}", file.getOriginalFilename(), bucketName);
        } catch (final SdkClientException | IOException exception) {
            log.error("Unable to store {} in S3 bucket {} ", file.getOriginalFilename(), bucketName, exception);
            return Boolean.FALSE;
        }
        return Boolean.TRUE;
    }

    public Optional<S3Object> retrieve(@NonNull final String objectKey) {
        final var bucketName = awsS3ConfigurationProperties.getS3().getBucketName();
        log.info("Retrieving object {} from S3 bucket {}", objectKey, bucketName);
        try {
            final var s3Object = amazonS3.getObject(bucketName, objectKey);
            log.info("Object {} retrieved successfully from S3 bucket {}", objectKey, bucketName);
            return Optional.of(s3Object);
        } catch (final SdkClientException exception) {
            log.error("Unable to retreive object {} from S3 Bucket {}", objectKey, bucketName, exception);
            return Optional.empty();
        }
    }
    
    @SneakyThrows
    public String generatePresignedUrl(@NonNull final String objectKey, @NonNull final HttpMethod httpMethod) {
        final var bucketName = awsS3ConfigurationProperties.getS3().getBucketName();
        final var expirationSeconds = awsS3ConfigurationProperties.getS3().getPresignedUrl().getExpirationTime();
        final var presignedUrlGenerationRequest = new GeneratePresignedUrlRequest(bucketName, objectKey, httpMethod);
        presignedUrlGenerationRequest.setExpiration(new LocalDateTime().plusSeconds(expirationSeconds).toDate());
        log.info("Generating presigned URL to {} object '{}'", httpMethod, objectKey);

        URL presignedUrl = amazonS3.generatePresignedUrl(presignedUrlGenerationRequest);
        log.info("Successfully generated {} presigned URL for object '{}'", httpMethod,  objectKey);
        
        return presignedUrl.toURI().toString();
    }

    private ObjectMetadata constructMetadata(final MultipartFile file) {
        final var metadata = new ObjectMetadata();
        metadata.setContentType(file.getContentType());
        metadata.setContentDisposition(file.getOriginalFilename());
        return metadata;
    }

}