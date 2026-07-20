# Changelog

All notable changes to this project are documented here.

## [Unreleased]

### Added

- Open-sourced under Apache-2.0 with LICENSE, CONTRIBUTING, SECURITY, and CODE_OF_CONDUCT
- In-app docs at `/docs/[slug]` with allowlisted registry and Mermaid diagram rendering

## [0.7.0] - 2026-07-20

### Added

- Inter as standard UI font; shared `DataTable` + skeleton loaders across list pages
- Settings: IP mode Select, labeled IP whitelist editor (hidden when OPEN), Show/Hide remask for secrets including Google Client ID
- Connector JAR marketplace: bundled PostgreSQL install, custom JAR upload, search/filters; connections gated on installed plugins
- 3h in-memory metrics ring (`MetricSampleService`) with Infra/Dashboard charts; health badges show UP/DOWN only
- Users table Google avatars; Cursor rules for UI + connectors

### Changed

- API/Worker no longer compile against `postgresql-connector`; plugins load from `app.plugins.dir`

## [0.6.0] - 2026-07-20

### Added

- Logout UI, theme toggle (light/dark), `AppLoader`, `notify` toaster helper, pill button variants
- Cursor rule `.cursor/rules/ui-patterns.mdc` for reusable UI patterns
- Domain-admin users (`ALLOWED_EMAIL_DOMAIN`): `/api/admin/users` list/revoke/delete + Users page
- JWT `ver` / `token_version` for session revoke; `last_login_at` / `last_seen_at`
- Dashboard-editable IP whitelist filter (`ip_whitelist_mode` / `ip_whitelist`) without restart
- Infra page + `/api/admin/infra` aggregating API/worker Actuator metrics
- Richer dashboard stats (users, pool, CPU/RAM samples) + local clock; marketplace removed from home
- Paginated list APIs (`page`/`size` 10–500) for jobs, connections, workers, users
- Marketplace install/uninstall (enable classpath plugins); connection min/max pool size (default max 10)
- Job checkpoints + worker `job_events`; SSE `/api/jobs/test` + LiveLogTerminal; structured GSpace alerts
- Worker Actuator (`health,info,metrics`); `WORKER_METRICS_URL` for API aggregator

### Security

- Actuator (non-health) requires authentication when `AUTH_ENFORCED=true`
- Admin routes require domain-admin

## [0.5.0] - 2026-07-20

### Added

- Caddy reverse proxy replaces nginx for path routing in the single Docker image

### Removed

- App-level IP whitelisting (API filter, Next.js middleware checks, Settings UI, nginx geo sync)
- `IpWhitelistFilter`, `ClientIpResolver`, `IpMatcher`, `NginxWhitelistSync`
- Env vars: `IP_WHITELIST*`, `NGINX_*`, `TRUST_PROXY`, `TRUSTED_PROXY_CIDRS`

### Changed

- Settings UI: thread limits, batch size, and GSpace webhook only
- K8s manifests simplified (no IP whitelist config)

## [0.4.0] - 2026-07-20

### Added

- `ClientIpResolver` — trusted `X-Forwarded-For` parsing for ALB/K8s (anti-spoof when proxy untrusted)
- nginx `map` + `geo $real_client_ip` for IP whitelist behind load balancer
- Next.js `resolveClientIp()` aligned with API trust model
- Spring `server.forward-headers-strategy: framework` for HTTPS/proxy headers
- K8s manifests (`infra/k8s/`) with ALB ingress annotations
- `TRUST_PROXY` and `TRUSTED_PROXY_CIDRS` env vars

### Fixed

- IP whitelist matched ALB IP instead of real client when behind AWS ALB
- Spoofable `X-Forwarded-For` when pod reachable without going through ALB

## [0.3.0] - 2026-07-20

### Added

- JWT enforcement on API (`AUTH_ENFORCED`, default true)
- Domain-restricted Google OAuth (`ALLOWED_EMAIL_DOMAIN`, `hd` param, server validation)
- IP whitelist modes: `OPEN` vs `RESTRICTED` with dashboard toggle
- Correct IPv4 CIDR matching (`IpMatcher`)
- nginx UI IP restriction via generated geo config (Docker prod)
- Next.js auth middleware + `AuthGuard` for route protection
- Logout endpoint (`POST /api/auth/logout`)
- Secure cookie attributes (`SameSite=Lax`, optional `Secure`)

### Fixed

- `/api/auth/me` works with JWT cookie (was broken for post-login sessions)
- RESTRICTED mode fail-closed when whitelist empty or JSON invalid
- OAuth success path exempt from IP filter

### Changed

- Settings UI: IP mode dropdown + conditional IP list field

## [0.2.0] - 2026-07-20

### Added

- `./run-local-dev.sh` — one-command local dev with `--frontend`, `--backend`, `--keep-infra` flags
- Single all-in-one Docker image (`infra/Dockerfile`) — API + Worker + Web + nginx on port 80
- Smart env-to-DB config bootstrap with dashboard precedence (`AppConfigBootstrap`)
- `IP_WHITELIST` env seeding to `app_config` table
- Settings UI shows config source badge (env / dashboard / default)
- Marketplace section on dashboard overview
- Full documentation suite under `docs/`

### Fixed

- Worker loads real connection credentials from DB via `ConnectionDecryptor` (no hardcoded creds)

### Changed

- `GET /api/config` returns `{ value, source, updatedAt }` per key
- README slimmed to quick-start hub linking to docs

## [0.1.0] - 2026-07-20

### Added

- Initial platform: Next.js dashboard, Spring Boot API/worker, PostgreSQL connector
- OAuth login, job wizard, hot/cold migration engine, GSpace alerts
