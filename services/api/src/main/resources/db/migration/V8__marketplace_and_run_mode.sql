-- Tracks successful marketplace installs (CONNECTOR + TOOL kinds), independent of
-- connector_plugins (which only models CONNECTOR rows). run_mode (Task 5) may extend this file
-- or land in a follow-up migration if split.
CREATE TABLE IF NOT EXISTS marketplace_installs (
    id           VARCHAR(64) PRIMARY KEY,
    kind         VARCHAR(16) NOT NULL,
    version      VARCHAR(32) NOT NULL,
    installed_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
