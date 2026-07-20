-- Track config origin for env vs dashboard precedence
ALTER TABLE app_config ADD COLUMN IF NOT EXISTS source VARCHAR(16) NOT NULL DEFAULT 'FLYWAY';

UPDATE app_config SET source = 'FLYWAY' WHERE source IS NULL OR source = '';
