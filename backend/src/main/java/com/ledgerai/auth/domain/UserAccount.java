package com.ledgerai.auth.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * The account owner (accounting professional) — the root of all ownership (DATABASE §5.1).
 *
 * <p>Maps to the reserved-word table {@code "user"}. This is a persistence entity only; it never
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
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
