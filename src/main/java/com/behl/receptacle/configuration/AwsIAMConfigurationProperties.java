package com.behl.receptacle.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * Maps AWS IAM user security credentials configured in active .yaml file to the instance variables
 * defined below. The permissions attached to the configured IAM user will be used in the policy
 * evaluation logic when making API calls to AWS.
 * </p>
 * 
 * <p>
 * The {@link com.amazonaws.services.s3.AmazonS3} bean object is created using the configured
 * credentials and can be autowired in consuming classes to interact with Amazon Simple Storage
 * Service (S3).
 * </p>
 * 
 * <p>
 * Example .yaml code snippet:
 * <pre>
 * {@code
 * com:
 *   behl:
 *     receptacle:
 *       aws:
 *         access-key: iam-access-key
 *         secret-access-key: iam-secret-access-key
 * }
 * </pre>
 * </p>
 * 
 * @see AwsS3Configuration#amazonS3()
 * @see com.amazonaws.services.s3.AmazonS3
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "com.behl.receptacle.aws")
public class AwsIAMConfigurationProperties {

    /**
     * <p>
     * Access key ID of the IAM user. This property corresponds to the key
     * <code>com.behl.receptacle.aws.access-key</code> in the active .yaml configuration file.
     * </p>
     */
    private String accessKey;
    
    
    /**
     * Secret Access key of the IAM user. This property corresponds to the key
     * <code>com.behl.receptacle.aws.secret-access-key</code> in the active .yaml configuration file.
     */
    private String secretAccessKey;

}
