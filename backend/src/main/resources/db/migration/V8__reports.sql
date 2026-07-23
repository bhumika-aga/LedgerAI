-- Report schema (DATABASE §5.7 Report, §9 Indexing Strategy). Additive, one logical change: the report
-- table (ADR-016). A report is a first-class, editable resource whose content is AI-generated; reports are
-- hard-deleted (DATABASE §8), so there is no soft-delete column.

CREATE TABLE report
(
    id          uuid        NOT NULL,
    document_id uuid        NOT NULL,
    user_id     uuid        NOT NULL,
    title       text,
    content     text        NOT NULL,
    status      text        NOT NULL,
    created_at  timestamptz NOT NULL,
    updated_at  timestamptz NOT NULL,
    CONSTRAINT pk_report PRIMARY KEY (id),
    CONSTRAINT fk_report_document FOREIGN KEY (document_id) REFERENCES document (id) ON DELETE CASCADE,
    CONSTRAINT fk_report_user FOREIGN KEY (user_id) REFERENCES "user" (id) ON DELETE CASCADE,
    CONSTRAINT ck_report_status CHECK (status IN ('DRAFT', 'SAVED'))
);

-- Access paths (DATABASE §5.7, §9): a document's reports by document_id; owner scoping and the account-level
-- list by user_id.
CREATE INDEX idx_report_document_id ON report (document_id);
CREATE INDEX idx_report_user_id ON report (user_id);
