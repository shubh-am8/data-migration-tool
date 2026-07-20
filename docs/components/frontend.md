# Frontend Dashboard

Next.js 16 App Router + shadcn/ui + Tailwind CSS variables (light/dark via `next-themes`).

## Pages

| Route | Purpose |
|---|---|
| `/login` | Google OAuth |
| `/dashboard` | Stats cards, local clock, recent jobs (no marketplace) |
| `/infra` | API / Worker Actuator snapshot + Web build id (admin) |
| `/users` | Admin user list, revoke, delete (domain admins) |
| `/connectors/marketplace` | Install/uninstall classpath connectors |
| `/connections` | Paginated connections |
| `/connections/new` | ConnectionForm (+ pool min/max) |
| `/jobs` | Paginated jobs |
| `/jobs/new` | JobWizard with Test Job SSE + LiveLogTerminal |
| `/jobs/[id]` | Job detail |
| `/workers` | Paginated workers |
| `/settings` | Runtime config (incl. IP whitelist) |

## Shared UI

- `AppLoader`, `notify` (`lib/notify.ts`), pill `Button` variants (`success`/`warning`/`danger`/`info`)
- `PaginationBar` / page size 10–500
- `LiveLogTerminal` for streaming logs
- Theme toggle in shell; logout via user menu

## API Client

`lib/api-client.ts` — credentials + `logout()`.

Cursor rule: `.cursor/rules/ui-patterns.mdc`.

[Back to Documentation Index](../README.md)
