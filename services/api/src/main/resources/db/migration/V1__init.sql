-- Users (Google OAuth)
CREATE TABLE users (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email       VARCHAR(255) NOT NULL UNIQUE,
    name        VARCHAR(255),
    picture_url TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Live app configuration
CREATE TABLE app_config (
    config_key  VARCHAR(128) PRIMARY KEY,
    config_value TEXT NOT NULL,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_by  UUID REFERENCES users(id)
);

INSERT INTO app_config (config_key, config_value) VALUES
    ('ip_whitelist', '[]'),
    ('min_threads_per_job', '1'),
    ('max_threads_per_job', '8'),
    ('gspace_webhook_url', ''),
    ('default_batch_size', '5000');

-- Connector marketplace catalog
CREATE TABLE connector_plugins (
    id          VARCHAR(64) PRIMARY KEY,
    name        VARCHAR(128) NOT NULL,
    description TEXT,
    version     VARCHAR(32) NOT NULL,
    enabled     BOOLEAN NOT NULL DEFAULT TRUE,
    icon        VARCHAR(64),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

INSERT INTO connector_plugins (id, name, description, version, enabled, icon) VALUES
    ('postgresql', 'PostgreSQL', 'Connect to PostgreSQL databases for schema introspection and data transfer', '1.0.0', TRUE, 'database');

-- Saved connections
CREATE TABLE connections (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    plugin_id       VARCHAR(64) NOT NULL REFERENCES connector_plugins(id),
    name            VARCHAR(255) NOT NULL,
    config_encrypted TEXT NOT NULL,
    created_by      UUID REFERENCES users(id),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_connections_plugin ON connections(plugin_id);
CREATE INDEX idx_connections_created_by ON connections(created_by);

-- Job definitions
CREATE TYPE job_status AS ENUM (
    'DRAFT', 'PENDING', 'RUNNING', 'PAUSED', 'COMPLETED', 'FAILED', 'CANCELLED'
);

CREATE TYPE migration_mode AS ENUM ('HOT_ONLY', 'COLD_ONLY', 'HOT_THEN_COLD');
CREATE TYPE phase_type AS ENUM ('HOT', 'COLD');
CREATE TYPE phase_status AS ENUM ('PENDING', 'RUNNING', 'PAUSED', 'COMPLETED', 'FAILED');
CREATE TYPE conflict_mode AS ENUM ('DO_UPDATE', 'DO_NOTHING');

CREATE TABLE jobs (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name                    VARCHAR(255) NOT NULL,
    source_connection_id    UUID NOT NULL REFERENCES connections(id),
    dest_connection_id      UUID NOT NULL REFERENCES connections(id),
    config_json             JSONB NOT NULL DEFAULT '{}',
    migration_mode          migration_mode NOT NULL DEFAULT 'HOT_THEN_COLD',
    status                  job_status NOT NULL DEFAULT 'DRAFT',
    thread_count            INT NOT NULL DEFAULT 1,
    hot_days                INT,
    ts_column               VARCHAR(255),
    schema_name             VARCHAR(255),
    source_table            VARCHAR(255),
    is_partition            BOOLEAN NOT NULL DEFAULT FALSE,
    partition_name          VARCHAR(255),
    filters_json            JSONB NOT NULL DEFAULT '[]',
    conflict_columns        TEXT[] NOT NULL DEFAULT '{}',
    created_by              UUID REFERENCES users(id),
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_jobs_status ON jobs(status);
CREATE INDEX idx_jobs_created_by ON jobs(created_by);

-- Job phases (hot/cold)
CREATE TABLE job_phases (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id          UUID NOT NULL REFERENCES jobs(id) ON DELETE CASCADE,
    phase           phase_type NOT NULL,
    status          phase_status NOT NULL DEFAULT 'PENDING',
    conflict_mode   conflict_mode NOT NULL,
    rows_processed  BIGINT NOT NULL DEFAULT 0,
    rows_total      BIGINT,
    started_at      TIMESTAMPTZ,
    completed_at    TIMESTAMPTZ,
    UNIQUE (job_id, phase)
);

-- Batch checkpoints for resume
CREATE TABLE job_checkpoints (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id          UUID NOT NULL REFERENCES jobs(id) ON DELETE CASCADE,
    phase           phase_type NOT NULL,
    batch_key       VARCHAR(255) NOT NULL,
    last_cursor     TEXT,
    rows_processed  BIGINT NOT NULL DEFAULT 0,
    committed_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (job_id, phase, batch_key)
);

CREATE INDEX idx_checkpoints_job ON job_checkpoints(job_id);

-- Job event log
CREATE TABLE job_events (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id      UUID NOT NULL REFERENCES jobs(id) ON DELETE CASCADE,
    event_type  VARCHAR(64) NOT NULL,
    payload     JSONB,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_job_events_job ON job_events(job_id, created_at DESC);

-- Worker heartbeats
CREATE TABLE worker_heartbeats (
    worker_id       VARCHAR(128) PRIMARY KEY,
    active_threads  INT NOT NULL DEFAULT 0,
    current_job_id  UUID REFERENCES jobs(id),
    thread_details  JSONB NOT NULL DEFAULT '[]',
    last_seen       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- GSpace alert configuration per job
CREATE TABLE alert_configs (
    job_id                  UUID PRIMARY KEY REFERENCES jobs(id) ON DELETE CASCADE,
    lifecycle_enabled       BOOLEAN NOT NULL DEFAULT TRUE,
    progress_interval_min   INT,
    webhook_url_override    TEXT
);

-- Reconciliation counters per phase
CREATE TABLE job_reconciliation (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id          UUID NOT NULL REFERENCES jobs(id) ON DELETE CASCADE,
    phase           phase_type NOT NULL,
    source_count    BIGINT NOT NULL DEFAULT 0,
    dest_written    BIGINT NOT NULL DEFAULT 0,
    dest_skipped    BIGINT NOT NULL DEFAULT 0,
    dest_updated    BIGINT NOT NULL DEFAULT 0,
    passed          BOOLEAN,
    checked_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (job_id, phase)
);
