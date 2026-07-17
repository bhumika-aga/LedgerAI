package com.ledgerai.documents.adapter;

import com.ledgerai.documents.port.SignedUrl;
import com.ledgerai.documents.port.StoragePort;
import com.ledgerai.documents.port.StorageUnavailableException;
import com.ledgerai.documents.port.StorageUpload;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Supabase Storage adapter (ADR-002) implementing the domain {@link StoragePort} (ADR-008).
 *
 * <p>This is the <strong>only</strong> place that knows the provider: its REST endpoints, headers, and
 * JSON shapes are confined here, and no Supabase/provider type crosses the port — callers see only
 * domain records ({@link StorageUpload}, {@link SignedUrl}) and domain exceptions. The active adapter
 * is selected by configuration (ARCHITECTURE §10); this one is absent from the {@code test} profile,
 * where an in-memory port stands in.
 *
 * <p>Object keys are opaque UUIDs, never derived from user input (SECURITY §9). Every provider failure
 * is mapped to {@link StorageUnavailableException} so the error taxonomy stays consistent (→ {@code 503}).
 */
@Component
@Profile("!test")
@EnableConfigurationProperties(StorageProperties.class)
public class SupabaseStorageAdapter implements StoragePort {
    
    private final RestClient restClient;
    private final StorageProperties properties;
    
    public SupabaseStorageAdapter(RestClient.Builder builder, StorageProperties properties) {
        this.properties = properties;
        this.restClient = builder
                              .baseUrl(properties.url())
                              .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.serviceKey())
                              .build();
    }
    
    @Override
    public String store(StorageUpload upload) {
        String objectKey = UUID.randomUUID().toString();
        try {
            restClient.post()
                .uri("/object/{bucket}/{key}", properties.bucket(), objectKey)
                .contentType(MediaType.parseMediaType(upload.contentType()))
                .body(upload.content())
                .retrieve()
                .toBodilessEntity();
            return objectKey;
        } catch (RestClientException e) {
            throw new StorageUnavailableException("The document could not be stored. Please try again.", e);
        }
    }
    
    @Override
    public SignedUrl createDownloadUrl(String storageReference, Duration ttl) {
        try {
            SignResponse response = restClient.post()
                                        .uri("/object/sign/{bucket}/{key}", properties.bucket(), storageReference)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .body(Map.of("expiresIn", ttl.toSeconds()))
                                        .retrieve()
                                        .body(SignResponse.class);
            if (response == null || response.signedURL() == null) {
                throw new StorageUnavailableException("The download link could not be created.", null);
            }
            return new SignedUrl(properties.url() + response.signedURL(), Instant.now().plus(ttl));
        } catch (RestClientException e) {
            throw new StorageUnavailableException("The download link could not be created.", e);
        }
    }
    
    @Override
    public void delete(String storageReference) {
        try {
            restClient.delete()
                .uri("/object/{bucket}/{key}", properties.bucket(), storageReference)
                // Idempotent: a missing object is not an error (port contract, API_SPEC §8.4).
                .retrieve()
                .onStatus(status -> status.value() == 404 || status.value() == 400, (req, res) -> {
                })
                .toBodilessEntity();
        } catch (RestClientException e) {
            throw new StorageUnavailableException("The document could not be removed from storage.", e);
        }
    }
    
    /**
     * Supabase's sign response: a bucket-relative {@code signedURL} the adapter turns absolute.
     */
    private record SignResponse(String signedURL) {
    }
}
