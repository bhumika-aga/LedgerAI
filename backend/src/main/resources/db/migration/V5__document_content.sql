-- Extracted-text schema (DATABASE §5.4 DocumentContent, §6 Common Fields, §9 Indexing Strategy).
-- Additive, one logical change: the document_content table (ADR-016). Populated only by OCR/native
-- extraction (the OCR Processing slice); holds the Extracted Text separated from hot document metadata.

CREATE TABLE document_content
(
    id                 uuid        NOT NULL,
    document_id        uuid        NOT NULL,
    extracted_text     text,
    extraction_quality text,
    char_count         integer,
    created_at         timestamptz NOT NULL,
    updated_at         timestamptz NOT NULL,
    CONSTRAINT pk_document_content PRIMARY KEY (id),
    CONSTRAINT uq_document_content_document_id UNIQUE (document_id),
    CONSTRAINT fk_document_content_document FOREIGN KEY (document_id) REFERENCES document (id) ON DELETE CASCADE
);
-- Note: DATABASE §5.4 defines no CHECK on extraction_quality (unlike document.status); the HIGH|LOW|
-- UNKNOWN set is enforced by the JPA enum mapping. No undocumented constraint is added here.

-- Full-text search index over extracted text (DATABASE §9). Powers Global Search (a later slice);
-- created here because the column and its index are defined together in the schema.
CREATE INDEX gin_document_content_extracted_text
    ON document_content USING GIN (to_tsvector('english', coalesce(extracted_text, '')));
