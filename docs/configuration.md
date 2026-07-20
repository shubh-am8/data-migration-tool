# Configuration Guide

## Two Layers of Configuration

| Layer | Storage | Examples | Editable from dashboard? |
|---|---|---|---|
| Secrets | Environment only | `JWT_SECRET`, `ENCRYPTION_KEY`, OAuth, `ALLOWED_EMAIL_DOMAIN` | No |
| App config | PostgreSQL `app_config` table | Thread limits, GSpace URL, batch size | Yes |

## Authentication

| Variable | Default | Description |
|---|---|---|
| `ALLOWED_EMAIL_DOMAIN` | (empty) | Only `@domain` Google accounts may log in |
| `AUTH_ENFORCED` | `true` | Require JWT on `/api/**`; set `false` for local dev |
| `AUTH_COOKIE_SECURE` | `false` | Set `true` in HTTPS production |

### Google Workspace setup

1. Google Cloud Console → OAuth consent screen → **Internal** (Workspace org only)
2. Create OAuth Web client with redirect URI:
   - Dev: `http://localhost:8080/login/oauth2/code/google`
   - Prod (Caddy): `http://localhost/login/oauth2/code/google` or your domain
3. Set `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `ALLOWED_EMAIL_DOMAIN=yourcompany.com`

Server-side enforcement rejects unverified emails and non-domain accounts even if Google consent is External.

## Env → DB Bootstrap

On API startup, `AppConfigBootstrap` seeds DB config from env **only when safe** (dashboard edits win unless `APP_CONFIG_FORCE_ENV=true`).

## Force Reset from Env

```bash
APP_CONFIG_FORCE_ENV=true
```

Restart API. Overwrites editable DB config keys from env.

## GSpace Webhook Priority

1. Per-job override (job wizard)
2. Dashboard `gspace_webhook_url` (DB)
3. Env `GSPACE_WEBHOOK_URL`

[Back to Documentation Index](README.md) | [Project README](../README.md)
