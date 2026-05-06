CREATE TABLE generation_jobs (
    job_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    doc_id UUID NOT NULL REFERENCES source_documents(doc_id),
    version_id UUID NOT NULL REFERENCES document_versions(version_id),
    trainer_id VARCHAR(64) NOT NULL REFERENCES trainers(trainer_id),
    status VARCHAR(40) NOT NULL,
    stage1_output TEXT,
    stage2_output JSONB,
    validation_output JSONB,
    validation_passed BOOLEAN,
    error_message TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE document_chunks (
    chunk_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    version_id UUID NOT NULL REFERENCES document_versions(version_id) ON DELETE CASCADE,
    chunk_index INTEGER NOT NULL,
    chunk_text TEXT NOT NULL,
    chunk_strategy VARCHAR(40) NOT NULL,
    vector_store_id UUID,
    UNIQUE (version_id, chunk_index)
);

CREATE TABLE micro_modules (
    module_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    version_id UUID NOT NULL REFERENCES document_versions(version_id),
    job_id UUID REFERENCES generation_jobs(job_id),
    sequence_order INTEGER NOT NULL,
    title VARCHAR(500) NOT NULL,
    module_content TEXT NOT NULL,
    key_takeaway TEXT,
    reading_time_minutes NUMERIC(4,2) NOT NULL,
    validation_score NUMERIC(3,2),
    validated BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (version_id, sequence_order)
);

CREATE TABLE query_log (
    query_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    trainer_id VARCHAR(64) NOT NULL REFERENCES trainers(trainer_id),
    query_text TEXT NOT NULL,
    retrieved_chunks JSONB NOT NULL,
    token_count_in INTEGER,
    token_count_out INTEGER,
    latency_ms INTEGER,
    model_used VARCHAR(120),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE audit_events (
    event_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type VARCHAR(80) NOT NULL,
    entity_id VARCHAR(120) NOT NULL,
    trainer_id VARCHAR(64),
    event_payload JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_generation_jobs_trainer ON generation_jobs(trainer_id);
CREATE INDEX idx_generation_jobs_doc ON generation_jobs(doc_id);
CREATE INDEX idx_micro_modules_version ON micro_modules(version_id);
CREATE INDEX idx_query_log_trainer_created ON query_log(trainer_id, created_at DESC);
CREATE INDEX idx_audit_events_type_created ON audit_events(event_type, created_at DESC);
