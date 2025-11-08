package br.com.flowlinkerAPI.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.security.MessageDigest;

@Service
public class CdnUploadService {

    @Value("${cdn.s3.bucket}")
    private String bucket;

    @Value("${cdn.s3.region}")
    private String region;

    @Value("${cdn.s3.accessKey}")
    private String accessKey;

    @Value("${cdn.s3.secretKey}")
    private String secretKey;

    @Value("${cdn.s3.endpoint:}")
    private String endpoint; // opcional (para R2/MinIO)

    @Value("${cdn.publicBaseUrl:}")
    private String publicBaseUrl; // opcional (ex.: https://cdn.seudominio.com)

    public static class UploadResult {
        public final String key;
        public final String url;
        public final String sha256Hex;
        public final long sizeBytes;
        public UploadResult(String key, String url, String sha256Hex, long sizeBytes) {
            this.key = key;
            this.url = url;
            this.sha256Hex = sha256Hex;
            this.sizeBytes = sizeBytes;
        }
    }

    public UploadResult uploadBytes(String key, byte[] bytes, String contentType) {
        S3Client s3 = buildClient();
        PutObjectRequest put = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType != null ? contentType : "application/octet-stream")
                .build();
        s3.putObject(put, RequestBody.fromBytes(bytes));

        String url = resolvePublicUrl(key);
        String sha256 = sha256Hex(bytes);
        return new UploadResult(key, url, sha256, bytes.length);
    }

    private S3Client buildClient() {
        var creds = StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey));
        var builder = S3Client.builder()
                .credentialsProvider(creds);
        if (endpoint != null && !endpoint.isBlank()) {
            builder = builder
                    .endpointOverride(java.net.URI.create(endpoint))
                    .region(Region.of(region))
                    .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build());
        } else {
            builder = builder.region(Region.of(region));
        }
        return builder.build();
    }

    public String presignGetUrl(String key, java.time.Duration expiry) {
        var creds = StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey));
        var presignerBuilder = software.amazon.awssdk.services.s3.presigner.S3Presigner.builder()
                .credentialsProvider(creds)
                .region(Region.of(region));
        if (endpoint != null && !endpoint.isBlank()) {
            presignerBuilder = presignerBuilder.endpointOverride(java.net.URI.create(endpoint));
        }
        var presigner = presignerBuilder.build();
        var getObjectRequest = software.amazon.awssdk.services.s3.model.GetObjectRequest.builder()
                .bucket(bucket).key(key).build();
        var presignRequest = software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest.builder()
                .signatureDuration(expiry)
                .getObjectRequest(getObjectRequest)
                .build();
        var presigned = presigner.presignGetObject(presignRequest);
        presigner.close();
        return presigned.url().toString();
    }

    private String resolvePublicUrl(String key) {
        if (publicBaseUrl != null && !publicBaseUrl.isBlank()) {
            String base = publicBaseUrl.endsWith("/") ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1) : publicBaseUrl;
            return base + "/" + key;
        }
        // fallback padrão S3
        return "https://" + bucket + ".s3." + region + ".amazonaws.com/" + key;
    }

    private String sha256Hex(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(data);
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 calculation failed", e);
        }
    }
}


