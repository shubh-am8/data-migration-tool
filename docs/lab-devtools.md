# Lab Dev Tools

The **`lab-devtools`** Marketplace item is a `TOOL` plugin (not a `ConnectorPlugin`). It is **not preinstalled** — install it from the Marketplace like any other catalog item.

## What it does

1. **Lab DDL** — On install, the API extracts the verified zip to `data/plugins/tools/lab-devtools/` and runs `sql/*.sql` against the lab Postgres instance (`migration_lab` on port **5433**).
2. **Sample tables** — Creates `app` and `test` schemas with `orders_cold` (COLD_ONLY) and `orders_hot_cold` (HOT/COLD scenarios).
3. **Simulation jobs** — Unlocks TEST-mode simulation jobs that seed/update sample rows in the lab DB (see [Job run modes](#job-run-modes)).

Platform metadata stays in `migration_app` (`:5432`). Lab data is isolated in Compose service `labdb`.

## Lab database layout

| Piece | Value |
|-------|-------|
| Compose service | `labdb` |
| Host port | `5433` |
| Database | `migration_lab` |
| User / password | `migration` / `migration` (dev defaults) |
| Schemas | `app` (source-like), `test` (sandbox destination) |

Compose init (`infra/labdb/init/`) creates the same schema/table shapes before plugin install; `lab-devtools` install is idempotent (`CREATE IF NOT EXISTS`).

## Install

1. Build local dist assets (offline dev): `marketplace/scripts/build-dist.sh`
2. Start the stack: `./run-local-dev.sh`
3. Open **Connectors → Marketplace** → **Lab Dev Tools** → **Install**

Install uses `marketplace/catalog.json` + SHA-256 verification (local `marketplace/dist/` or GitHub Release asset). TOOL installs are recorded in `marketplace_installs`, not `connector_plugins`.

## Job run modes

The job wizard requires **`runMode`**: `TEST` or `PRODUCTION`.

| Mode | Rules |
|------|-------|
| `TEST` | Source and dest connections must be **sandbox** (`connections.sandbox=true`). Target schema must be `app` or `test` on the lab DB. Simulation jobs allowed. |
| `PRODUCTION` | Cannot target schema `test`. Simulation jobs blocked. |

Connections: toggle **Sandbox connection** when creating/editing a connection in the UI.

## Simulation jobs

Simulation (`config_json.kind == "SIMULATE"`) runs only when `runMode=TEST`. The worker writes directly to `migration_lab` tables allowed by lab-devtools DDL (`app`/`test` × `orders_cold`/`orders_hot_cold`), not through source/dest JDBC connections.

Use simulation to practice hot/cold windows and batch behavior without touching production databases.

## Related

- [Marketplace](marketplace.md) — catalog install flow
- [Development Guide](development.md) — local ports and `./run-local-dev.sh`
- Source: `marketplace/plugins/lab-devtools/`

[Back to Documentation Index](README.md) | [Project README on GitHub](https://github.com/shubh-am8/data-migration-tool/blob/main/README.md)
