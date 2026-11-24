ALTER TABLE project_tracks
    ADD COLUMN IF NOT EXISTS origin_open_source_id BIGINT NULL;

CREATE INDEX IF NOT EXISTS idx_tracks_project_origin_open_source
    ON project_tracks (project_id, origin_open_source_id);
