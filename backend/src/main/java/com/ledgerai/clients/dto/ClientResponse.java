package com.ledgerai.clients.dto;

import com.ledgerai.clients.domain.Client;
import com.ledgerai.clients.domain.ClientStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Outbound client representation (API_SPEC §17.3):
 * {@code { id, name, contactDetails?, notes?, status, archivedAt?, createdAt, updatedAt }}.
 *
 * <p>Note what is absent: {@code userId}. The contract does not list it, and every client a caller can
 * reach is their own by construction, so echoing the owner would add nothing but a field the API never
 * promised.
 *
 * <p>The static factory is this repository's established entity→DTO mapping convention (see
 * {@code UserResponse.from}), keeping the mapping beside the shape it produces.
 */
public record ClientResponse(
    UUID id,
    String name,
    String contactDetails,
    String notes,
    ClientStatus status,
    Instant archivedAt,
    Instant createdAt,
    Instant updatedAt) {
    
    public static ClientResponse from(Client client) {
        return new ClientResponse(
            client.getId(),
            client.getName(),
            client.getContactDetails(),
            client.getNotes(),
            client.getStatus(),
            client.getArchivedAt(),
            client.getCreatedAt(),
            client.getUpdatedAt());
    }
}
