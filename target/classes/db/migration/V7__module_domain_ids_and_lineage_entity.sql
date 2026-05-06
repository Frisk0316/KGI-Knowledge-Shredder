ALTER TABLE micro_modules ADD COLUMN domain_ids JSONB NOT NULL DEFAULT '[]'::jsonb;
