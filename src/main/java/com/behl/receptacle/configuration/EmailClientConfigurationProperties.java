package com.behl.receptacle.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * Maps the Email Client properties from the active .yaml configuration file to the instance
 * variables defined below. These properties are used to connect and authenticate with the
 * provisioned email server to send email notifications.
 * </p>
 *
 * <p>
 * Example .yaml code snippet:
 * <pre>
 * {@code
 * com:
 *   behl:
 *     receptacle:
 *       email:
 *         base-url: email-server-base-url
 *         api-key: email-server-api-key
 * }
 * </pre>
 * </p>
 *
 * @see com.behl.receptacle.client.EmailApiClient
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "com.behl.receptacle.email")
public class EmailClientConfigurationProperties {

    /**
     * <p>
     * The Base URL of the Email Server. This property corresponds to the key
     * <code>com.behl.receptacle.email.base-url</code> in the active .yaml configuration file.
     * <p>
     */
    private String baseUrl;
    
    /**
     * <p>
     * The API key required to authenticate with the Email Server using the Bearer
     * Authentication. This property corresponds to the key
     * <code>com.behl.receptacle.email.api-key</code> in the active .yaml configuration file.
     * </p>
     */
    private String apiKey;

}