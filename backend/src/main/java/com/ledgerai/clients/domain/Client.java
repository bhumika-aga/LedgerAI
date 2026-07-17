package com.ledgerai.clients.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * The organizing container for a professional's customer (DATABASE §5.2).
 *
 * <p>Every Client belongs to exactly one owning User (BR-003, FR-CLNT-005); {@code userId} is the
 * ownership anchor that {@link com.ledgerai.common.security.OwnershipGuard} checks. This is a
 * persistence entity only — it never crosses the API boundary (ARCHITECTURE §5.2).
 */
@Entity
@Table(name = "client")
public class Client {
    
    @Id
    private UUID id;
    
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;
    
    @Column(name = "name", nullable = false)
    private String name;
    
    @Column(name = "contact_details")
    private String contactDetails;
    
    @Column(name = "notes")
    private String notes;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ClientStatus status;
    
    @Column(name = "archived_at")
    private Instant archivedAt;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    
    protected Client() {
        // for JPA
    }
    
    /**
     * FR-CLNT-001: a new client always starts ACTIVE and owned by its creator.
     */
    public static Client create(UUID userId, String name, String contactDetails, String notes) {
        Client client = new Client();
        client.id = UUID.randomUUID();
        client.userId = userId;
        client.name = name;
        client.contactDetails = contactDetails;
        client.notes = notes;
        client.status = ClientStatus.ACTIVE;
        Instant now = Instant.now();
        client.createdAt = now;
        client.updatedAt = now;
        return client;
    }
    
    /**
     * FR-CLNT-003: applies a partial edit (API_SPEC §7.4). A {@code null} argument means "not supplied —
     * leave unchanged"; {@code updated_at} moves only when something actually changed.
     */
    public void applyUpdate(String name, String contactDetails, String notes) {
        boolean changed = false;
        if (name != null && !name.equals(this.name)) {
            this.name = name;
            changed = true;
        }
        if (contactDetails != null && !contactDetails.equals(this.contactDetails)) {
            this.contactDetails = contactDetails;
            changed = true;
        }
        if (notes != null && !notes.equals(this.notes)) {
            this.notes = notes;
            changed = true;
        }
        if (changed) {
            this.updatedAt = Instant.now();
        }
    }
    
    /**
     * FR-CLNT-004: archives the client (soft — DATABASE §8). Idempotent, because API_SPEC §7.5 requires
     * the archive action to be. Documents are untouched (BR-002, FR-CLNT-007).
     */
    public void archive() {
        if (status == ClientStatus.ARCHIVED) {
            return;
        }
        status = ClientStatus.ARCHIVED;
        Instant now = Instant.now();
        archivedAt = now;
        updatedAt = now;
    }
    
    public UUID getId() {
        return id;
    }
    
    public UUID getUserId() {
        return userId;
    }
    
    public String getName() {
        return name;
    }
    
    public String getContactDetails() {
        return contactDetails;
    }
    
    public String getNotes() {
        return notes;
    }
    
    public ClientStatus getStatus() {
        return status;
    }
    
    public Instant getArchivedAt() {
        return archivedAt;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
