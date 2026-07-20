# Architecture Overview

## System Diagram

```mermaid
flowchart TB
  subgraph client [Browser]
    WebUI[Next.js Dashboard]
  end

  subgraph app [Application - single Docker image]
    Caddy[Caddy :80]
    API[Spring Boot API :8080]
    Worker[Spring Boot Worker :8081]
    Next[Next.js :3000]
  end

  subgraph infra [Infrastructure]
    PG[(PostgreSQL appdb)]
    Redis[(Redis)]
  end

  subgraph plugins [Connector Plugins]
    PGPlugin[PostgreSQL Plugin]
  end

  WebUI --> Caddy
  Caddy -->|"/api/*"| API
  Caddy -->|"/"| Next
  API --> PG
  API --> Redis
  Worker --> PG
  Worker --> Redis
  Worker --> PGPlugin
  API --> PGPlugin
```

## Module Map

| Module | Path | Responsibility |
|---|---|---|
| Frontend | `apps/web/` | Dashboard, job wizard, settings, marketplace UI |
| API | `services/api/` | Auth, config, connectors, jobs, orchestration |
| Worker | `services/worker/` | Batch copy, hot/cold phases, reconciliation |
| Connector SDK | `packages/connector-sdk/` | Plugin interface, filter types |
| Domain | `packages/domain/` | Shared JPA entities, AES decrypt |
| PostgreSQL Plugin | `connectors/postgresql/` | PG introspection, batch read/write |

## Data Flow: Job Execution

1. User creates job via wizard → stored in `jobs` table
2. User clicks Start → API pushes job ID to Redis queue
3. Worker polls queue → loads source/dest connection configs (decrypted)
4. HotColdManager runs HOT then COLD phases via BatchCopyEngine
5. Checkpoints written after each batch commit
6. Reconciliation compares source vs destination counts
7. GSpace notifications on lifecycle events

```mermaid
sequenceDiagram
  participant U as User
  participant API as API
  participant R as Redis
  participant W as Worker
  participant DB as Target DB
  U->>API: Start job
  API->>R: Enqueue jobId
  W->>R: Poll
  W->>DB: Batch copy HOT/COLD
  W->>API: Status updates via DB
```

## Config Storage

- **Env-only secrets:** JWT, encryption key, OAuth credentials
- **DB-backed config:** Thread limits, GSpace URL, batch size
- **Dashboard edits** persist in `app_config` with `source=DASHBOARD`

[Back to Documentation Index](README.md) | [Project README on GitHub](https://github.com/shubh-am8/data-migration-tool/blob/main/README.md)
