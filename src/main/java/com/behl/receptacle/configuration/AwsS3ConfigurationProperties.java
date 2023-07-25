package com.behl.receptacle.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * Maps AWS S3 configuration values defined in active .yaml file to the instance variables defined
 * below. The configuration properties would be used to perform operations against the provisioned
 * Bucket in S3.
 * </p>
 * 
 * @see com.behl.receptacle.service.AwsStorageService
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "com.behl.receptacle.aws")
public class AwsS3ConfigurationProperties {

    @Valid
    private S3Bucket s3 = new S3Bucket();

    @Getter
    @Setter
    @Validated
    public class S3Bucket {

        /**
         * <p>
         * Name of the S3 bucket to be used as the destination for performing operations against.
         * This property corresponds to the key <code>com.behl.receptacle.aws.s3.bucket-name</code>
         * in the active .yaml configuration file.
         * </p>
         * 
         * @see https://docs.aws.amazon.com/AmazonS3/latest/userguide/creating-bucket.html
         */
        @NotBlank(message = "S3 bucket name must be configured")
        private String bucketName;
        
        /**
         * <p>
         * AWS region name where provisioned S3 bucket is located. This property corresponds to the
         * key <code>com.behl.receptacle.aws.s3.region</code> in the active .yaml configuration
         * file.
         * </p>
         */
        @NotBlank(message = "S3 bucket region must be configured")
        private String region;
        
        /**
         * <p>
         * Endpoint URL for the S3 service. This property corresponds to the key
         * <code>com.behl.receptacle.aws.s3.endpoint</code> in the active .yaml configuration file.
         * <p>
         * This property is only required when LocalStack is being used in the testing profiles to
         * emulate cloud communication. For other profiles or deployments, this property can be
         * omitted as the default AWS S3 service endpoint will be used.
         * </p>
         */
        private String endpoint;
        
        @Valid
        private PresignedUrl presignedUrl = new PresignedUrl();

        @Getter
        @Setter
        public class PresignedUrl {

            /**
             * <p>
             * Expiration time in seconds for the generated S3 presigned URLs. Post expiration, the
             * generated presigned-url will become invalid and unusable. This property corresponds
             * to the key <code>com.behl.receptacle.aws.s3.presigned-url.expiration-time</code> in the
             * active .yaml configuration file.
             * </p>
             */
            @NotNull(message = "S3 presigned URL expiration time must be specified")
            @Positive(message = "S3 presigned URL expiration time must be a positive value")
            private Integer expirationTime;

        }

    }

}
