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

    /**
     * <p>
     * Registers the {@link com.amazonaws.services.s3.AmazonS3} bean in the Spring IOC container,
     * allowing it to be autowired into consuming classes for communication with Amazon Simple
     * Storage Service (S3). The bean is constructed using the IAM user security credentials defined
     * in the active .yaml configuration file
     * </p>
     * 
     * <p>
     * If the application is hosted in an EC2 Instance or ECS and an IAM Role/Instance profile is
     * used for authentication, the method <code>.withCredentials(new
     * DefaultAWSCredentialsProviderChain())</code> can be used instead of the
     * <code>AWSStaticCredentialsProvider</code> defined below. In this scenario, the
     * {@link AwsIAMConfigurationProperties} class can be discarded.
     * </p>
     * 
     * @see AwsIAMConfigurationProperties
     */
    @Bean
    @Profile("!test")
    public AmazonS3 amazonS3() {
        final var awsCredentials = new BasicAWSCredentials(awsIAMConfigurationProperties.getAccessKey(),
                awsIAMConfigurationProperties.getSecretAccessKey());

        return AmazonS3ClientBuilder.standard()
                .withRegion(Regions.fromName(awsS3ConfigurationProperties.getS3().getRegion()))
                .withCredentials(new AWSStaticCredentialsProvider(awsCredentials)).build();
    }
    
    /**
     * <p>
     * Registers the {@link com.amazonaws.services.s3.AmazonS3} bean in the Spring IOC container for
     * use in testing scenarios when LocalStack is used as a cloud service emulator. This bean
     * allows communication with Amazon Simple Storage Service (S3) running locally in the
     * LocalStack environment. All the required configuration properties needs to be added in the
     * active .yaml configuration file before context is started to run the test cases.
     * </p>
     *
     * @return The configured {@link com.amazonaws.services.s3.AmazonS3} client object for
     *         LocalStack.
     * @see AwsIAMConfigurationProperties
     */
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
