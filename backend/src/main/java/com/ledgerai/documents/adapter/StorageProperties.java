package com.ledgerai.documents.adapter;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for the Supabase Storage adapter (ADR-002). All values are environment-driven and
 * server-held; the service key is a secret and MUST be supplied by the environment, never committed
 * (SECURITY §13). These properties are provider-specific and live with the adapter, so nothing outside
 * it depends on them.
 *
 * @param url        the Supabase Storage REST base, e.g. {@code https://<project>.supabase.co/storage/v1}
 * @param bucket     the private bucket that holds document objects
 * @param serviceKey the server-side service key used to authorize storage operations
 */
@ConfigurationProperties(prefix = "storage")
public record StorageProperties(String url, String bucket, String serviceKey) {
}
