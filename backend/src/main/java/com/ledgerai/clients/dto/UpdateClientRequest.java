package com.ledgerai.clients.dto;

/**
 * Update payload (API_SPEC §7.4): {@code { name?, contactDetails?, notes? }}.
 *
 * <p>Every field is optional — a PATCH is a "partial update" (API_SPEC §2.3), so an omitted field
 * leaves the stored value untouched. {@code name} therefore cannot carry {@code @NotBlank}: absent is
 * legal, but *present and blank* is not, and only the service can tell those apart (VR-004).
 */
public record UpdateClientRequest(
    String name,
    String contactDetails,
    String notes) {
}
