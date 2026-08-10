package com.aliahmed.Vercel.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.net.URI;

/**
 * Builds the S3 client pointed at R2. Only created when the R2 backend is active.
 */
@Configuration
@ConditionalOnProperty(name = "app.storage.type", havingValue = "r2")
public class R2Config {

    @Bean
    S3Client r2Client(R2Properties properties) {
        return S3Client.builder()
                .endpointOverride(URI.create(properties.getEndpoint()))
                // R2 ignores the region but the SDK requires one; "auto" is R2's convention.
                .region(Region.of("auto"))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(properties.getAccessKeyId(), properties.getSecretAccessKey())))
                // R2 works with path-style addressing.
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .build();
    }
}
