CREATE TYPE lab_seed_status AS ENUM ('PAUSED', 'RUNNING', 'STOPPED');

CREATE TABLE lab_seed_sessions (
  job_id UUID PRIMARY KEY REFERENCES jobs(id) ON DELETE CASCADE,
  schema_name VARCHAR(64) NOT NULL DEFAULT 'test_source',
  table_name VARCHAR(255) NOT NULL,
  scenario VARCHAR(32) NOT NULL DEFAULT 'HOT_THEN_COLD',
  inserts_per_minute INT NOT NULL DEFAULT 60,
  updates_per_minute INT NOT NULL DEFAULT 12,
  status lab_seed_status NOT NULL DEFAULT 'PAUSED',
  last_tick_at TIMESTAMPTZ,
  rows_inserted BIGINT NOT NULL DEFAULT 0,
  rows_updated BIGINT NOT NULL DEFAULT 0,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
