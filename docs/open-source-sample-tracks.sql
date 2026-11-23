ALTER TABLE project_tracks
    ADD COLUMN IF NOT EXISTS origin_open_source_id BIGINT NULL;
