-- PostgreSQL. Apply before deploying the frontend when ddl-auto is validate/none.
CREATE TABLE IF NOT EXISTS editor_drafts (
    id UUID PRIMARY KEY,
    owner_id BIGINT NOT NULL,
    project_id BIGINT NOT NULL,
    track_id BIGINT NOT NULL,
    target_clip_id BIGINT,
    content JSONB NOT NULL,
    base_clip JSONB,
    revision BIGINT NOT NULL,
    mutation_id UUID NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    status VARCHAR(16) NOT NULL,
    committed_clip_id BIGINT
);
CREATE INDEX IF NOT EXISTS idx_editor_drafts_owner_project_status
    ON editor_drafts (owner_id, project_id, status);
