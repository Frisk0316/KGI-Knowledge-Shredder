CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE trainers (
    trainer_id VARCHAR(64) PRIMARY KEY,
    display_name VARCHAR(255) NOT NULL,
    role_name VARCHAR(64) NOT NULL DEFAULT 'TRAINER',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE knowledge_domains (
    domain_id BIGINT PRIMARY KEY,
    domain_name VARCHAR(80) NOT NULL UNIQUE,
    description TEXT NOT NULL
);

CREATE TABLE source_documents (
    doc_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    trainer_id VARCHAR(64) NOT NULL REFERENCES trainers(trainer_id),
    original_filename VARCHAR(512) NOT NULL,
    content_hash CHAR(64) NOT NULL,
    current_version_id UUID,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE document_versions (
    version_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    doc_id UUID NOT NULL REFERENCES source_documents(doc_id),
    version_number INTEGER NOT NULL,
    raw_text TEXT NOT NULL,
    redacted_text TEXT,
    classification VARCHAR(80),
    processing_status VARCHAR(40) NOT NULL DEFAULT 'PENDING',
    parser_name VARCHAR(80) NOT NULL,
    failure_reason TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (doc_id, version_number)
);

ALTER TABLE source_documents
    ADD CONSTRAINT fk_source_documents_current_version
    FOREIGN KEY (current_version_id) REFERENCES document_versions(version_id);

CREATE TABLE document_domain_map (
    map_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    doc_id UUID NOT NULL REFERENCES source_documents(doc_id) ON DELETE CASCADE,
    domain_id BIGINT NOT NULL REFERENCES knowledge_domains(domain_id),
    UNIQUE (doc_id, domain_id)
);

CREATE UNIQUE INDEX ux_source_documents_trainer_hash_active
    ON source_documents (trainer_id, content_hash)
    WHERE is_deleted = FALSE;

CREATE INDEX idx_source_documents_trainer ON source_documents(trainer_id);
CREATE INDEX idx_document_versions_doc_id ON document_versions(doc_id);
CREATE INDEX idx_document_domain_map_domain ON document_domain_map(domain_id);
