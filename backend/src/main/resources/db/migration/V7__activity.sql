-- Activity Timeline schema (DATABASE §5.8 Activity, §9 Indexing Strategy). Additive, one logical change:
-- the append-only activity table (ADR-016). Written only by the mutating modules via the shared
-- ActivityService; there is no update/delete path (BR-016, DIR-008 — immutability is the point), so the
-- table deliberately has no updated_at.

CREATE TABLE activity
(
    id          uuid        NOT NULL,
    user_id     uuid        NOT NULL,
    client_id   uuid,
    document_id uuid,
    action_type text        NOT NULL,
    summary     text,
    metadata    jsonb,
    created_at  timestamptz NOT NULL,
    CONSTRAINT pk_activity PRIMARY KEY (id),
    CONSTRAINT fk_activity_user FOREIGN KEY (user_id) REFERENCES "user" (id) ON DELETE CASCADE,
    -- History survives when a referenced client/document is removed (DATABASE §5.8): SET NULL, not CASCADE.
    CONSTRAINT fk_activity_client FOREIGN KEY (client_id) REFERENCES client (id) ON DELETE SET NULL,
    CONSTRAINT fk_activity_document FOREIGN KEY (document_id) REFERENCES document (id) ON DELETE SET NULL
);
-- Note: DATABASE §5.8 defines no CHECK on action_type (unlike document.status); the value set is enforced
-- by the JPA enum mapping. No undocumented constraint is added here.

-- The timeline is inherently "latest first, per user" (DATABASE §5.8, §9); this composite index backs the
-- default ordering. A separate (client_id) index backs the per-client view.
CREATE INDEX idx_activity_user_id_created_at ON activity (user_id, created_at DESC);
CREATE INDEX idx_activity_client_id ON activity (client_id);
