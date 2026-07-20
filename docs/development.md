# Development Guide

## Prerequisites

- Java 21, Maven 3.9+
- Node.js 22+, npm
- Docker Desktop (for Postgres + Redis)

## Quick Start

```bash
cp .env.example .env
# Edit .env with Google OAuth credentials if needed

./run-local-dev.sh
```

Open http://localhost:3000

## Script Flags

| Command | What starts |
|---|---|
| `./run-local-dev.sh` | Postgres + Redis + API + Worker + Frontend |
| `./run-local-dev.sh --backend` | Postgres + Redis + API + Worker |
| `./run-local-dev.sh --frontend` | Frontend only (API must already run) |
| `./run-local-dev.sh --stop-infra-on-exit` | On Ctrl+C, stop Postgres/Redis via docker compose down |
| `./run-local-dev.sh --keep-infra` | Deprecated no-op (infra stays running by default) |
| `./run-local-dev.sh --help` | Show usage |

## Dev Script Flow

On every start, `prepare_dev_stack` clears stale listeners on that mode's app ports (8080/8081/3000). Docker infra (Postgres/Redis) is left running across restarts; use `--stop-infra-on-exit` to tear it down on exit.

```mermaid
flowchart TD
  start[run-local-dev.sh] --> parse[Parse flags]
  parse --> env[Load .env + ensure_dev_dir]
  env --> prepare[prepare_dev_stack clear ports]
  prepare --> mode{Mode?}
  mode -->|frontend| fe[start_frontend :3000]
  mode -->|backend or all| infra[start_infra Postgres + Redis]
  infra --> build[build_backend seed bundled JAR]
  build --> api[start_api :8080]
  api --> worker[start_worker :8081]
  worker --> allCheck{Mode = all?}
  allCheck -->|yes| fe
  allCheck -->|no| wait[Wait for Ctrl+C]
  fe --> wait
```

## Ports

| Service | Port |
|---|---|
| Frontend | 3000 |
| API | 8080 |
| Worker | 8081 |
| PostgreSQL | 5432 |
| Redis | 6379 |

## Tests

```bash
mvn test
scripts/test/run-local-dev_test.sh
cd apps/web && npm run build
```

## Troubleshooting

- **Port already in use (8080/8081/3000):** Re-run `./run-local-dev.sh` — it clears stale listeners automatically
- **Port 5432/6379 blocked by non-project process:** Script refuses to kill unrelated Postgres/Redis; stop the other service or change compose port mapping
- **OAuth redirect fails:** Set `GOOGLE_CLIENT_ID/SECRET` in `.env`; redirect URI must match Google console
- **Domain login rejected:** Set `ALLOWED_EMAIL_DOMAIN=yourcompany.com` and use a Workspace account
- **401 on all API calls:** Set `AUTH_ENFORCED=false` in `.env` for dev, or log in via Google first
- **API/Worker won't start:** Run `./run-local-dev.sh` from repo root — the script builds modules then starts each service from `services/api` and `services/worker` (root `mvn spring-boot:run` fails on the aggregator POM)
- **API won't start:** Ensure Postgres is healthy: `docker compose -f infra/docker-compose.dev.yml ps`

[Back to Documentation Index](README.md) | [Project README on GitHub](https://github.com/shubh-am8/data-migration-tool/blob/main/README.md)
