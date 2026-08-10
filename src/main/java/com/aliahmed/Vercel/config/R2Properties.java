package com.aliahmed.Vercel.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Cloudflare R2 (S3-compatible) connection settings. Only used when
 * {@code app.storage.type=r2}. Every value comes from the environment.
 */
@ConfigurationProperties(prefix = "r2")
@Getter
@Setter
public class R2Properties {

    /** The account's S3 API endpoint, e.g. https://<account>.r2.cloudflarestorage.com */
    private String endpoint;

    private String bucket;

    private String accessKeyId;

    private String secretAccessKey;
}
