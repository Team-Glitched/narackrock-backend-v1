-- PostgreSQL. Apply before deployment when ddl-auto=validate/none.
-- Existing samples keep NULL: their instrument cannot be inferred from the stored audio.
ALTER TABLE open_sources ADD COLUMN IF NOT EXISTS instrument varchar(40);
