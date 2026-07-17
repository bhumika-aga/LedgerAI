-- Document schema (DATABASE §5.3 Document, §6 Common Fields, §9 Indexing Strategy).
-- Additive, one logical change: the document table (ADR-016). The document_content table is created by
-- the OCR/extraction slice that populates it (its full-text index powers Search) — not needed here.

CREATE TABLE document
(
    id                uuid        NOT NULL,
    client_id         uuid        NOT NULL,
    original_filename text        NOT NULL,
    mime_type         text        NOT NULL,
    size_bytes        bigint      NOT NULL,
    storage_reference text,
    status            text        NOT NULL,
    extraction_method text,
    failure_reason    text,
    deleted_at        timestamptz,
    created_at        timestamptz NOT NULL,
    updated_at        timestamptz NOT NULL,
    CONSTRAINT pk_document PRIMARY KEY (id),
    CONSTRAINT fk_document_client FOREIGN KEY (client_id) REFERENCES client (id) ON DELETE CASCADE,
    CONSTRAINT chk_document_status CHECK (status IN
                                          ('UPLOADED', 'PROCESSING', 'OCR_PROCESSING', 'READY', 'FAILED', 'DELETED'))
);

-- Foreign-key index: drives per-client listing and ownership joins (DATABASE §9).
CREATE INDEX idx_document_client_id ON document (client_id);

-- Filter by lifecycle state (DATABASE §9).
CREATE INDEX idx_document_status ON document (status);

-- Partial index: the default read path lists a client's non-deleted documents (DATABASE §9, §8).
CREATE INDEX idx_document_client_id_active ON document (client_id) WHERE deleted_at IS NULL;
