package com.ledgerai.documents.adapter;

import com.ledgerai.documents.port.SignedUrl;
import com.ledgerai.documents.port.StorageUnavailableException;
import com.ledgerai.documents.port.StorageUpload;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Unit tests for the {@link SupabaseStorageAdapter} against a mocked HTTP endpoint. They pin the
 * provider contract the adapter speaks — the object PUT/sign/delete calls, the Bearer credential, the
 * opaque key, absolutising the signed URL — and that any provider failure becomes a domain
 * {@link StorageUnavailableException}. No live Supabase is involved; the provider details never leave
 * this class.
 */
class SupabaseStorageAdapterTest {
    
    private static final String BASE = "https://project.supabase.co/storage/v1";
    private static final String BUCKET = "documents";
    
    private RestClient.Builder builder;
    private MockRestServiceServer server;
    private SupabaseStorageAdapter adapter;
    
    @BeforeEach
    void setUp() {
        builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        adapter = new SupabaseStorageAdapter(builder, new StorageProperties(BASE, BUCKET, "service-key"));
    }
    
    @Test
    void storeUploadsWithAnOpaqueKeyAndBearerCredentialAndReturnsTheKey() {
        server.expect(requestTo(Matchers.startsWith(BASE + "/object/" + BUCKET + "/")))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer service-key"))
            .andExpect(header(HttpHeaders.CONTENT_TYPE, Matchers.containsString("application/pdf")))
            .andRespond(withSuccess());
        
        String reference = adapter.store(new StorageUpload(new byte[]{0x25, 0x50}, "application/pdf"));
        
        // Opaque key (a UUID), never derived from user input (SECURITY §9).
        assertThat(reference).matches("[0-9a-fA-F-]{36}");
        server.verify();
    }
    
    @Test
    void createDownloadUrlSignsAndAbsolutisesTheUrl() {
        server.expect(requestTo(BASE + "/object/sign/" + BUCKET + "/ref-123"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer service-key"))
            .andRespond(withSuccess(
                "{\"signedURL\":\"/object/sign/documents/ref-123?token=abc\"}", MediaType.APPLICATION_JSON));
        
        SignedUrl signed = adapter.createDownloadUrl("ref-123", Duration.ofMinutes(5));
        
        assertThat(signed.url()).isEqualTo(BASE + "/object/sign/documents/ref-123?token=abc");
        assertThat(signed.expiresAt()).isAfter(Instant.now());
        server.verify();
    }
    
    @Test
    void deleteRemovesTheObject() {
        server.expect(requestTo(BASE + "/object/" + BUCKET + "/ref-123"))
            .andExpect(method(HttpMethod.DELETE))
            .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer service-key"))
            .andRespond(withSuccess());
        
        adapter.delete("ref-123");
        
        server.verify();
    }
    
    @Test
    void mapsAStoreFailureToStorageUnavailable() {
        server.expect(requestTo(Matchers.startsWith(BASE + "/object/" + BUCKET + "/")))
            .andRespond(withServerError());
        
        assertThatThrownBy(() -> adapter.store(new StorageUpload(new byte[]{0x25}, "application/pdf")))
            .isInstanceOf(StorageUnavailableException.class);
    }
    
    @Test
    void mapsASignFailureToStorageUnavailable() {
        server.expect(requestTo(BASE + "/object/sign/" + BUCKET + "/ref-123"))
            .andRespond(withServerError());
        
        assertThatThrownBy(() -> adapter.createDownloadUrl("ref-123", Duration.ofMinutes(5)))
            .isInstanceOf(StorageUnavailableException.class);
    }
}
