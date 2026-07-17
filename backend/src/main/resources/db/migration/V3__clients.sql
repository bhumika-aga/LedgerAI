-- Client schema (DATABASE §5.2 Client, §6 Common Fields, §9 Indexing Strategy).
-- Additive, one logical change: the client table (ADR-016).

CREATE TABLE client
(
    id              uuid        NOT NULL,
    user_id         uuid        NOT NULL,
    name            text        NOT NULL,
    contact_details text,
    notes           text,
    status          text        NOT NULL DEFAULT 'ACTIVE',
    archived_at     timestamptz,
    created_at      timestamptz NOT NULL,
    updated_at      timestamptz NOT NULL,
    CONSTRAINT pk_client PRIMARY KEY (id),
    CONSTRAINT fk_client_user FOREIGN KEY (user_id) REFERENCES "user" (id) ON DELETE CASCADE,
    CONSTRAINT chk_client_status CHECK (status IN ('ACTIVE', 'ARCHIVED'))
);

-- Foreign key index: drives every ownership filter (DATABASE §9).
CREATE INDEX idx_client_user_id ON client (user_id);

-- Composite index: "list a user's active clients quickly" — the default read path (DATABASE §9).
CREATE INDEX idx_client_user_id_status ON client (user_id, status);
