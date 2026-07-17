package com.ledgerai.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The account owner (accounting professional) — the root of all ownership
 * (DATABASE §5.1).
 *
 * <p>
 * Maps to the reserved-word table {@code "user"}. This is a persistence entity
 * only; it never
 * crosses the API boundary and never exposes the password hash outward.
 */
@Entity
@Table(name = "`user`")
public class UserAccount {
    
    @Id
    private UUID id;
    
    @Column(nullable = false, unique = true)
    private String email;
    
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;
    
    @Column(name = "full_name")
    private String fullName;
    
    @Column(name = "professional_details")
    private String professionalDetails;
    
    /**
     * Free-form preferences (DATABASE §5.1: jsonb, "Basic UI/app preferences"). No document defines the
     * keys, so the shape is deliberately opaque — whatever object the owner stores is round-tripped
     * unchanged. A future slice can define concrete keys additively without a schema change.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "preferences")
    private Map<String, Object> preferences;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    
    protected UserAccount() {
        // for JPA
    }
    
    public static UserAccount create(String email, String passwordHash, String fullName) {
        UserAccount user = new UserAccount();
        user.id = UUID.randomUUID();
        user.email = email;
        user.passwordHash = passwordHash;
        user.fullName = fullName;
        Instant now = Instant.now();
        user.createdAt = now;
        user.updatedAt = now;
        return user;
    }
    
    /**
     * Applies a partial profile edit (FR-PROF-002, FR-PROF-003). A {@code null} argument means "not
     * supplied — leave unchanged"; a supplied value replaces the current one. {@code updated_at} moves
     * only when something actually changed.
     */
    public void applyProfileUpdate(String fullName, String professionalDetails, Map<String, Object> preferences) {
        boolean changed = false;
        if (fullName != null && !fullName.equals(this.fullName)) {
            this.fullName = fullName;
            changed = true;
        }
        if (professionalDetails != null && !professionalDetails.equals(this.professionalDetails)) {
            this.professionalDetails = professionalDetails;
            changed = true;
        }
        if (preferences != null && !preferences.equals(this.preferences)) {
            // A LinkedHashMap copy, not Map.copyOf: the blob is opaque, so a null value is legal JSON
            // ({"theme": null}) and Map.copyOf would reject it.
            this.preferences = new LinkedHashMap<>(preferences);
            changed = true;
        }
        if (changed) {
            this.updatedAt = Instant.now();
        }
    }
    
    public UUID getId() {
        return id;
    }
    
    public String getEmail() {
        return email;
    }
    
    public String getPasswordHash() {
        return passwordHash;
    }
    
    public String getFullName() {
        return fullName;
    }
    
    public String getProfessionalDetails() {
        return professionalDetails;
    }
    
    public Map<String, Object> getPreferences() {
        return preferences == null ? null : Collections.unmodifiableMap(new LinkedHashMap<>(preferences));
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
