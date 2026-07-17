package com.ledgerai.documents.support;

import com.ledgerai.documents.port.SignedUrl;
import com.ledgerai.documents.port.StoragePort;
import com.ledgerai.documents.port.StorageUpload;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Test-profile {@link StoragePort} — the in-memory stand-in for the Supabase adapter, which is absent
 * from the {@code test} profile (ARCHITECTURE §10 — the active adapter is chosen by configuration).
 *
 * <p>It exists only so integration tests exercise the real service/persistence path without a live
 * provider; it is not a production adapter. It keeps stored bytes in a map so tests can assert an
 * object was stored, signed, and removed.
 */
@Component
@Profile("test")
public class InMemoryStoragePort implements StoragePort {
    
    private final Map<String, byte[]> objects = new ConcurrentHashMap<>();
    
    @Override
    public String store(StorageUpload upload) {
        String reference = UUID.randomUUID().toString();
        objects.put(reference, upload.content());
        return reference;
    }
    
    @Override
    public SignedUrl createDownloadUrl(String storageReference, Duration ttl) {
        return new SignedUrl("https://storage.test/download/" + storageReference, Instant.now().plus(ttl));
    }
    
    @Override
    public void delete(String storageReference) {
        objects.remove(storageReference);
    }
    
    public boolean contains(String storageReference) {
        return objects.containsKey(storageReference);
    }
    
    public int storedCount() {
        return objects.size();
    }
}
