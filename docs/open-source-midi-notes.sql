ALTER TABLE open_sources
  ADD COLUMN IF NOT EXISTS midi_notes text,
  ADD COLUMN IF NOT EXISTS midi_ppq integer,
  ADD COLUMN IF NOT EXISTS midi_bpm integer;
