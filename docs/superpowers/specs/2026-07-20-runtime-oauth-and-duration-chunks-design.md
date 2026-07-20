# Runtime OAuth Settings + Duration-Based Chunk Migration

**Date:** 2026-07-20  
**Status:** Approved (Approach 2)  
**Related review:** Settings OAuth keys were DB-editable but Spring Security ignored them; `default_batch_size` was dead; hot/cold used a single `NOW()-N days` filter with no time chunks.

## Goals

1. Make `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, and `ALLOWED_EMAIL_DOMAIN` configurable from Settings UI and apply **without app restart**.
2. Env values remain defaults; dashboard overrides must not be overwritten on restart (unless `APP_CONFIG_FORCE_ENV=true`).
3. Settings UI uses human-readable labels (never raw `snake_case` headings). Sensitive values stay masked with Show/Hide.
4. Remove `default_batch_size` from settings. Row JDBC `LIMIT` stays an internal constant.
5. Jobs use Approach 2 time windows: `rangeStart`, `rangeEndMode` (`NOW`|`FIXED`), optional `rangeEnd`, `hotBoundaryDays`, `minChunkDurationHours`, `maxChunkDurationHours`. Worker plans duration chunks and copies each with a time predicate.

## Non-goals

- Recurring job scheduler / cron (re-run is still manual or external).
- Changing conflict modes or connector plugins beyond time-filter support.
- Admin role gating for settings (leave as any authenticated user for now).

## Architecture

### Runtime config

- `RuntimeConfigCatalog` remains the single source of truth for editable keys.
- Remove `default_batch_size` from catalog.
- Mark OAuth/domain keys `restartRequired=false`.
- Env bootstrap continues to seed missing keys and update only non-dashboard sources.
- `OAuthDomainValidator` reads allowed domain from `AppConfigService` **per validation call**.
- `OAuth2AuthorizationRequestResolver` reads domain from `AppConfigService` when customizing `hd`.
- Custom `ClientRegistrationRepository` builds Google registration from `AppConfigService` (`google_client_id` / `google_client_secret`) on each `findByRegistrationId`, falling back to env/bootstrap defaults already in DB.

### Duration chunks

At job run time:

1. `effectiveEnd` = `Instant.now()` if `rangeEndMode == NOW`, else `rangeEnd`.
2. `hotBoundary` = `effectiveEnd - hotBoundaryDays`.
3. Cold window = `[rangeStart, hotBoundary)`; hot window = `[hotBoundary, effectiveEnd)`.
4. `TimeChunkPlanner.plan(start, end, minHours, maxHours)` yields contiguous Instant pairs; step size = `maxHours` when range allows, last chunk may be shorter; reject if `min > max` or hours ≤ 0.
5. For each chunk, set `hotColdFilter` to `"tsCol" >= '...' AND "tsCol" < '...'` (ISO timestamps).
6. Internal row batch size constant `ROW_BATCH_SIZE = 5000` for JDBC LIMIT only.

### UI

- Settings: full human `LABELS` map for all catalog keys; drop batch size.
- Job form (`HotColdConfig`): range start, end mode + fixed end, hot window days, ts column, min/max chunk hours.

## Data model

New `jobs` columns (Flyway V4):

| Column | Type | Notes |
|---|---|---|
| `range_start` | TIMESTAMPTZ | nullable for HOT_ONLY with only hot window |
| `range_end_mode` | VARCHAR(16) NOT NULL DEFAULT `'NOW'` | `NOW` or `FIXED` |
| `range_end` | TIMESTAMPTZ | required when FIXED |
| `min_chunk_duration_hours` | INT | default 24 |
| `max_chunk_duration_hours` | INT | default 168 (7d) |

Keep existing `hot_days` column; API/UI name it **Hot window (days)** / `hotBoundaryDays` / `hotDays` field (same column).

## Validation

- `max_chunk_duration_hours >= min_chunk_duration_hours >= 1`
- If `rangeEndMode == FIXED`, `rangeEnd` required and `rangeEnd > rangeStart` when start present
- Modes needing cold require `rangeStart` and `tsColumn`
- Modes needing hot require `hotDays` / `hotBoundaryDays` and `tsColumn`

## Testing

- Catalog: no `default_batch_size`; OAuth keys not restart-required
- Domain validator uses AppConfigService value after update without restart simulation
- ClientRegistrationRepository returns updated client id from config
- TimeChunkPlanner unit tests (exact boundaries, last short chunk, empty range)
- HotColdManager iterates chunks
- UI labels present for Google/domain keys; HotColdConfig fields render

## Error handling

- Invalid chunk/range validation → 400 from JobService
- Blank Google client id at OAuth start → clear failure (registration missing credentials)
- Blank allowed domain → allow any Google account (existing behavior; document in settings help text)
