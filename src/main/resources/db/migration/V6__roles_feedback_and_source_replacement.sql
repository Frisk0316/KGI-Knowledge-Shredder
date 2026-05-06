INSERT INTO trainers (trainer_id, display_name, role_name)
VALUES
    ('learner_001', 'Admin Builder', 'ADMIN'),
    ('learner_002', 'Learning User', 'USER')
ON CONFLICT (trainer_id) DO UPDATE
    SET display_name = EXCLUDED.display_name,
        role_name = EXCLUDED.role_name;

CREATE TABLE document_feedback (
    feedback_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    doc_id UUID NOT NULL REFERENCES source_documents(doc_id) ON DELETE CASCADE,
    version_id UUID REFERENCES document_versions(version_id),
    actor_id VARCHAR(64) NOT NULL,
    feedback_type VARCHAR(40) NOT NULL,
    comment_text TEXT,
    status VARCHAR(40) NOT NULL DEFAULT 'OPEN',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    resolved_at TIMESTAMPTZ
);

CREATE INDEX idx_document_feedback_doc_created
    ON document_feedback(doc_id, created_at DESC);

CREATE INDEX idx_document_feedback_actor
    ON document_feedback(actor_id, created_at DESC);
