-- Session revoke + presence for admin user management
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS token_version INT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS last_login_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS last_seen_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS revoked_at TIMESTAMPTZ;
