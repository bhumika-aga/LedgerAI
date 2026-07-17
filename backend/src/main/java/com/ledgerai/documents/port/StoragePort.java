package com.ledgerai.documents.port;

import java.time.Duration;

/**
 * The domain-owned Storage port (ADR-008, ARCHITECTURE §10). Business logic depends only on this
 * interface, expressed in domain terms — "store these bytes and return a handle", "give me a
 * short-lived link", "remove it". The concrete provider (Supabase Storage, ADR-002) is an adapter
 * selected by configuration; no provider type ever crosses this boundary.
 *
 * <p>Implementations translate failures into the domain's {@link StorageUnavailableException} so the
 * error taxonomy stays consistent (SRS §8, API_SPEC §8.1/§8.5 → {@code 503}).
 */
public interface StoragePort {
    
    /**
     * Stores the object and returns an <strong>opaque</strong> reference (SECURITY §9 — keys are never
     * derived from user input). The reference is what the database persists ({@code storage_reference}).
     */
    String store(StorageUpload upload);
    
    /**
     * Mints a short-lived, owner-scoped download link for a previously stored object (API_SPEC §8.5).
     * The caller is responsible for having authorized the request first (ownership is not the port's
     * concern).
     */
    SignedUrl createDownloadUrl(String storageReference, Duration ttl);
    
    /**
     * Removes the stored object. Best-effort and idempotent: deleting an unknown reference is not an
     * error, mirroring the idempotent delete contract (API_SPEC §8.4) and the compensating-cleanup
     * pattern (DATABASE §11).
     */
    void delete(String storageReference);
}
