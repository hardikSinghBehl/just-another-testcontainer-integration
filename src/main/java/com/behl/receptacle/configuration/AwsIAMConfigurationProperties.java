package com.behl.receptacle.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "com.behl.receptacle.aws")
public class AwsIAMConfigurationProperties {

    private String accessKey;
    private String secretAccessKey;

}
