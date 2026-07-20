-- Builtin connectors listed in marketplace; must be installed (enabled) before use.
ALTER TABLE connector_plugins ADD COLUMN IF NOT EXISTS builtin BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE connector_plugins SET builtin = TRUE, enabled = FALSE WHERE id = 'postgresql';

UPDATE connector_plugins SET builtin = FALSE WHERE builtin IS NULL;
