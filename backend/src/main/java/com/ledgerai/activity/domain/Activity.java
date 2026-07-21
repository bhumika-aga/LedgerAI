package com.ledgerai.activity.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * An immutable, timestamped record of a significant action, for the timeline (DATABASE §5.8, NFR-012).
 *
 * <p><strong>Append-only.</strong> There is deliberately no update or delete path and no
 * {@code updated_at} — immutability is the point (BR-016, FR-TMLN-004, DATABASE §5.8, DIR-008). Rows are
 * owner-scoped by {@code userId} (BR-006) and optionally reference a client and/or document; those FKs
 * are {@code ON DELETE SET NULL} so history survives when the referenced row is removed.
 *
 * <p>{@code metadata} is optional structured context and MUST NOT contain secrets or sensitive content
 * (NFR-013). Persistence entity only; it never crosses the API boundary (a DTO does).
 */
@Entity
@Table(name = "activity")
public class Activity {
    
    @Id
    private UUID id;
    
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;
    
    @Column(name = "client_id", updatable = false)
    private UUID clientId;
    
    @Column(name = "document_id", updatable = false)
    private UUID documentId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, updatable = false)
    private ActivityType actionType;
    
    @Column(name = "summary", updatable = false)
    private String summary;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", updatable = false)
    private Map<String, Object> metadata;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    protected Activity() {
        // for JPA
    }
    
    /**
     * Records an action. {@code clientId}/{@code documentId}/{@code summary}/{@code metadata} are optional
     * (null where not applicable). {@code createdAt} is stamped at the moment the action is recorded.
     */
    public static Activity record(ActivityType actionType, UUID userId, UUID clientId, UUID documentId,
                                  String summary, Map<String, Object> metadata) {
        Activity activity = new Activity();
        activity.id = UUID.randomUUID();
        activity.userId = userId;
        activity.clientId = clientId;
        activity.documentId = documentId;
        activity.actionType = actionType;
        activity.summary = summary;
        activity.metadata = metadata == null ? null : new LinkedHashMap<>(metadata);
        activity.createdAt = Instant.now();
        return activity;
    }
    
    public UUID getId() {
        return id;
    }
    
    public UUID getUserId() {
        return userId;
    }
    
    public UUID getClientId() {
        return clientId;
    }
    
    public UUID getDocumentId() {
        return documentId;
    }
    
    public ActivityType getActionType() {
        return actionType;
    }
    
    public String getSummary() {
        return summary;
    }
    
    public Map<String, Object> getMetadata() {
        return metadata == null ? null : new LinkedHashMap<>(metadata);
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
}
