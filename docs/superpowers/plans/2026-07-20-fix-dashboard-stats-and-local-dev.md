# Fix Dashboard Stats 500 and Local Dev Startup Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix `/api/dashboard/stats` 500 errors and local dev startup failures (stale ports, duplicate Next.js) so the dashboard loads after Google sign-in.

**Architecture:** Root cause is Hibernate binding Java enums as `VARCHAR` against PostgreSQL native enum columns (`job_status`, etc.) — `findAll()` works but `countByStatus()` fails. Fix with `PostgreSQLEnumJdbcType` on domain entities. Extend `run-local-dev.sh` stale-port cleanup to include `:3000`.

**Tech Stack:** Spring Boot 3.3.5, Hibernate 6.5, PostgreSQL 15, Next.js 16, bash dev scripts

## Global Constraints

- Minimal diff; reuse existing patterns
- No new dependencies
- Keep debug instrumentation until post-fix verification
- ponytail comments for intentional dev shortcuts

---

### Task 1: Fix PostgreSQL enum JPA mapping (root cause)

**Files:**
- Modify: `packages/domain/src/main/java/com/migration/jobs/JobEntity.java`
- Modify: `packages/domain/src/main/java/com/migration/jobs/JobPhaseEntity.java`
- Test: `services/api/src/test/java/com/migration/dashboard/DashboardStatsProbeTest.java`

**Interfaces:**
- Consumes: existing `JobRepository.countByStatus(JobStatus)`
- Produces: enum columns correctly bound to PostgreSQL types

- [x] **Step 1: Write failing test** — `DashboardStatsProbeTest` against live Postgres
- [x] **Step 2: Verify failure** — `operator does not exist: job_status = character varying`
- [x] **Step 3: Add `@JdbcType(PostgreSQLEnumJdbcType.class)` + `columnDefinition` on all PG enum fields**
- [x] **Step 4: Verify test passes**

Run: `cd services/api && mvn test -Dtest=DashboardStatsProbeTest`
Expected: PASS

### Task 2: Local dev stale port cleanup for frontend

**Files:**
- Modify: `scripts/run-local-dev.sh`
- Test: `scripts/test/run-local-dev_test.sh`

- [x] **Step 1: Rename `clear_stale_backend_ports` → `clear_stale_dev_ports`, include `:3000`**
- [x] **Step 2: Add `wait_for_port localhost 3000` after frontend start**
- [x] **Step 3: Extend script test to assert `:3000` cleanup**

Run: `bash scripts/test/run-local-dev_test.sh`
Expected: All PASS

### Task 3: Dashboard error UI (no raw JSON in console-only)

**Files:**
- Modify: `apps/web/src/app/dashboard/page.tsx`

- [x] **Step 1: Add `loadError` state and friendly message on fetch failure**

### Task 4: Post-fix verification

- [ ] **Step 1: Restart `./run-local-dev.sh`, sign in, confirm dashboard stats load**
- [ ] **Step 2: Check debug log for `stats success` entries**
- [ ] **Step 3: Remove debug instrumentation after user confirms**
