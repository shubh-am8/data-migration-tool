-- IP whitelist policy mode: OPEN (allow all) or RESTRICTED (enforce ip_whitelist)
INSERT INTO app_config (config_key, config_value, source)
VALUES ('ip_whitelist_mode', 'OPEN', 'FLYWAY')
ON CONFLICT (config_key) DO NOTHING;
