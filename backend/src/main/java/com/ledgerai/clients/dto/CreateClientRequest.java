package com.ledgerai.clients.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Create payload (API_SPEC §7.3): {@code { name, contactDetails?, notes? }}.
 *
 * <p>{@code @NotBlank} covers the "required, non-empty" half of VR-004 at the boundary
 * (BACKEND_CODING_STANDARDS §7 — request shape is validated by the controller). The length half is
 * checked in the service, because the limit is configuration rather than a constant.
 */
public record CreateClientRequest(
    @NotBlank String name,
    String contactDetails,
    String notes) {
}
