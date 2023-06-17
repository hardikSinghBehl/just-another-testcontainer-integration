package com.behl.receptacle.service;

import java.io.IOException;
import java.util.Optional;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.behl.receptacle.configuration.AwsS3ConfigurationProperties;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
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

        try {
            amazonS3.putObject(bucketName, key, file.getInputStream(), objectMetaData);
        } catch (final SdkClientException | IOException exception) {
            log.error("Unable to store {} in S3 bucket {} ", file.getOriginalFilename(), bucketName, exception);
            return Boolean.FALSE;
        }
        return Boolean.TRUE;
    }

    public Optional<S3Object> retrieve(@NonNull final String objectKey) {
        final var bucketName = awsS3ConfigurationProperties.getS3().getBucketName();
        try {
            final var s3Object = amazonS3.getObject(bucketName, objectKey);
            return Optional.of(s3Object);
        } catch (final SdkClientException exception) {
            log.error("Unable to retreive object {} from S3 Bucket {}", objectKey, bucketName, exception);
            return Optional.empty();
        }
    }

    private ObjectMetadata constructMetadata(final MultipartFile file) {
        final var metadata = new ObjectMetadata();
        metadata.setContentType(file.getContentType());
        metadata.setContentDisposition(file.getOriginalFilename());
        return metadata;
    }

}