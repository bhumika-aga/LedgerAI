-- AI Summary schema (DATABASE §5.5 AIRequest, §5.6 AIOutput, §6 Common Fields, §9 Indexing Strategy).
-- Additive, one logical change: the ai_request and ai_output tables (ADR-016, ADR-010). Written only by
-- the AI Summary slice; this slice creates rows of type SUMMARY only, but the type/status CHECKs carry
-- the full documented value sets (DATABASE §5.5) so future AI capabilities need no constraint change.

CREATE TABLE ai_request
(
    id             uuid        NOT NULL,
    user_id        uuid        NOT NULL,
    document_id    uuid,
    type           text        NOT NULL,
    status         text        NOT NULL,
    prompt         text,
    failure_reason text,
    created_at     timestamptz NOT NULL,
    updated_at     timestamptz NOT NULL,
    CONSTRAINT pk_ai_request PRIMARY KEY (id),
    CONSTRAINT fk_ai_request_user FOREIGN KEY (user_id) REFERENCES "user" (id) ON DELETE CASCADE,
    CONSTRAINT fk_ai_request_document FOREIGN KEY (document_id) REFERENCES document (id) ON DELETE CASCADE,
    CONSTRAINT ck_ai_request_type CHECK (type IN ('SUMMARY', 'CHAT', 'EMAIL', 'REPORT')),
    CONSTRAINT ck_ai_request_status CHECK (status IN ('REQUESTED', 'IN_PROGRESS', 'COMPLETED', 'FAILED'))
);

-- Access paths (DATABASE §5.5): a document's summary is read by document_id; owner scoping by user_id.
CREATE INDEX ix_ai_request_document_id ON ai_request (document_id);
CREATE INDEX ix_ai_request_user_id ON ai_request (user_id);

CREATE TABLE ai_output
(
    id            uuid        NOT NULL,
    ai_request_id uuid        NOT NULL,
    content       text        NOT NULL,
    edited        boolean     NOT NULL DEFAULT false,
    created_at    timestamptz NOT NULL,
    updated_at    timestamptz NOT NULL,
    CONSTRAINT pk_ai_output PRIMARY KEY (id),
    CONSTRAINT uq_ai_output_ai_request_id UNIQUE (ai_request_id),
    CONSTRAINT fk_ai_output_ai_request FOREIGN KEY (ai_request_id) REFERENCES ai_request (id) ON DELETE CASCADE
);
