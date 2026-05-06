CREATE TABLE module_attempts (
    attempt_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    module_id UUID NOT NULL REFERENCES micro_modules(module_id),
    learner_id VARCHAR(64) NOT NULL,
    trainer_id VARCHAR(64) NOT NULL REFERENCES trainers(trainer_id),
    score NUMERIC(4,2) NOT NULL,
    interaction_seconds INTEGER NOT NULL,
    answered_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE learner_memory_state (
    state_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    module_id UUID NOT NULL REFERENCES micro_modules(module_id),
    learner_id VARCHAR(64) NOT NULL,
    ease_factor NUMERIC(4,2) NOT NULL DEFAULT 2.50,
    interval_days INTEGER NOT NULL DEFAULT 1,
    repetitions INTEGER NOT NULL DEFAULT 0,
    next_review_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (module_id, learner_id)
);

CREATE TABLE document_incidents (
    incident_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    doc_id UUID NOT NULL REFERENCES source_documents(doc_id),
    version_id UUID REFERENCES document_versions(version_id),
    trainer_id VARCHAR(64) NOT NULL REFERENCES trainers(trainer_id),
    incident_type VARCHAR(80) NOT NULL,
    severity VARCHAR(40) NOT NULL,
    details JSONB NOT NULL,
    resolved BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    resolved_at TIMESTAMPTZ
);

CREATE TABLE document_lineage (
    lineage_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    source_doc_id UUID NOT NULL REFERENCES source_documents(doc_id),
    derived_doc_id UUID NOT NULL REFERENCES source_documents(doc_id),
    relationship_type VARCHAR(80) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (source_doc_id, derived_doc_id, relationship_type)
);

CREATE INDEX idx_module_attempts_learner ON module_attempts(learner_id, answered_at DESC);
CREATE INDEX idx_learner_memory_due ON learner_memory_state(next_review_at);
CREATE INDEX idx_document_incidents_doc ON document_incidents(doc_id, resolved);
