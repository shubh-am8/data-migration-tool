# API Service

Spring Boot REST API on port 8080.

## Key Endpoints

| Method | Path | Description |
|---|---|---|
| GET | `/api/health` | Health check |
| GET | `/api/marketplace` | List connector plugins |
| GET/POST | `/api/connections` | Connection CRUD |
| GET/POST | `/api/jobs` | Job management |
| POST | `/api/jobs/{id}/start` | Enqueue job |
| GET/PUT | `/api/config` | App configuration |
| GET | `/api/workers` | Worker registry |

## Auth

- Google OAuth2 login → JWT in httpOnly cookie
- Dev mode: `SecurityConfig` permits all requests (documented for local dev)

## Security Features

- **AES-256-GCM** — connection passwords encrypted via `SecretCipher`
- **Config bootstrap** — `AppConfigBootstrap` seeds from env on first boot

## Database

Flyway migrations in `src/main/resources/db/migration/`.

[Back to Documentation Index](../README.md) | [Project README](../../README.md)
