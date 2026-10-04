package com.psyavocat.storage.r2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

@Configuration
public class R2Config {

    private static final Logger log = LoggerFactory.getLogger(R2Config.class);

    @Value("${r2.account-id:${R2_ACCOUNT_ID:}}")
    private String accountId;

    @Value("${r2.access-key-id:${R2_ACCESS_KEY_ID:}}")
    private String accessKeyId;

    @Value("${r2.secret-access-key:${R2_SECRET_ACCESS_KEY:}}")
    private String secretAccessKey;

    @Value("${r2.bucket-name:${R2_BUCKET_NAME:}}")
    private String bucketName;

    @Value("${r2.endpoint:${R2_ENDPOINT:}}")
    private String customEndpoint;

    public String resolveEndpoint() {
        if (customEndpoint != null && !customEndpoint.isBlank()) {
            return customEndpoint.trim();
        }
        if (accountId != null && !accountId.isBlank()) {
            return "https://" + accountId.trim() + ".r2.cloudflarestorage.com";
        }
        return null;
    }

    public boolean isConfigured() {
        return accessKeyId != null && !accessKeyId.isBlank() &&
               secretAccessKey != null && !secretAccessKey.isBlank() &&
               bucketName != null && !bucketName.isBlank() &&
               resolveEndpoint() != null;
    }

    @Bean
    public S3Client r2S3Client() {
        if (!isConfigured()) {
            log.warn("Cloudflare R2 n'est pas configuré (variables R2_* absentes). Le stockage des justificatifs restera en mode dégradé ou local.");
            return null;
        }

        String endpoint = resolveEndpoint();
        log.info("Initialisation du client S3 pour Cloudflare R2 sur l'endpoint : {} (bucket : {})", endpoint, bucketName);

        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKeyId.trim(), secretAccessKey.trim());

        return S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .region(Region.of("auto"))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .chunkedEncodingEnabled(false)
                        .build())
                .build();
    }

    @Bean
    public S3Presigner r2S3Presigner() {
        if (!isConfigured()) {
            return null;
        }

        String endpoint = resolveEndpoint();
        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKeyId.trim(), secretAccessKey.trim());

        return S3Presigner.builder()
                .endpointOverride(URI.create(endpoint))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .region(Region.of("auto"))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build())
                .build();
    }
}
