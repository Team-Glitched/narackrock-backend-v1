-- PostgreSQL. Apply before deploying with ddl-auto=validate or none.
ALTER TABLE project_clips
    ALTER COLUMN duration_tick TYPE bigint,
    ALTER COLUMN duration_ms TYPE bigint;
