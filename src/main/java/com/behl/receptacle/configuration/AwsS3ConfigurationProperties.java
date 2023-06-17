package com.behl.receptacle.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "com.behl.receptacle.aws")
public class AwsS3ConfigurationProperties {

    private S3Bucket s3 = new S3Bucket();

    @Getter
    @Setter
    public class S3Bucket {

        private String bucketName;
        private String region;
        private String endpoint;
        private PresignedUrl presignedUrl = new PresignedUrl();

        @Getter
        @Setter
        public class PresignedUrl {

            private Integer expirationTime;

        }

    }

}
