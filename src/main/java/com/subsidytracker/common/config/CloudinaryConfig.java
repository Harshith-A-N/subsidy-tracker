package com.subsidytracker.common.config;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures the Cloudinary client bean.
 *
 * Credentials are read from application properties; real values must be set in
 * the git-ignored application-local.properties (or as environment variables).
 * The "secure" flag ensures all generated asset URLs use HTTPS.
 */
@Configuration
public class CloudinaryConfig {

    @Value("${cloudinary.cloud-name:test-stub}")
    private String cloudName;

    @Value("${cloudinary.api-key:test-stub}")
    private String apiKey;

    @Value("${cloudinary.api-secret:test-stub}")
    private String apiSecret;

    @Bean
    public Cloudinary cloudinary() {
        return new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key",    apiKey,
                "api_secret", apiSecret,
                "secure",     true
        ));
    }
}
