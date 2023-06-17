package com.behl.receptacle.configuration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(
        value = {AwsIAMConfigurationProperties.class, AwsS3ConfigurationProperties.class})
public class AwsS3Configuration {

    private final AwsIAMConfigurationProperties awsIAMConfigurationProperties;
    private final AwsS3ConfigurationProperties awsS3ConfigurationProperties;

    @Bean
    @Profile("!test")
    public AmazonS3 amazonS3() {
        final var awsCredentials = new BasicAWSCredentials(awsIAMConfigurationProperties.getAccessKey(),
                awsIAMConfigurationProperties.getSecretAccessKey());

        return AmazonS3ClientBuilder.standard()
                .withRegion(Regions.fromName(awsS3ConfigurationProperties.getS3().getRegion()))
                .withCredentials(new AWSStaticCredentialsProvider(awsCredentials)).build();
    }
    
    @Bean
    @Profile("test")
    public AmazonS3 localStackAmazonS3() {
        final var s3Properties = awsS3ConfigurationProperties.getS3();
        final var endpointConfiguration = new EndpointConfiguration(s3Properties.getEndpoint(), s3Properties.getRegion());
        final var awsCredentials = new BasicAWSCredentials(awsIAMConfigurationProperties.getAccessKey(),
                awsIAMConfigurationProperties.getSecretAccessKey());

        return AmazonS3ClientBuilder.standard()
                .withEndpointConfiguration(endpointConfiguration)
                .withCredentials(new AWSStaticCredentialsProvider(awsCredentials)).build();
    }

}
