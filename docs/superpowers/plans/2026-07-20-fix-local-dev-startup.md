# Fix Local Dev Startup Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development task-by-task.

**Goal:** Fix `./run-local-dev.sh` so API and Worker actually start locally.

**Architecture:** Build backend modules once with Maven reactor (`-pl ... -am install`), then run each Spring Boot app from its module directory (`cd services/api && mvn spring-boot:run`). Wait for ports before declaring success.

**Tech Stack:** Bash, Maven multi-module, Spring Boot 3.3.

## Global Constraints

- Do not change production Docker/Caddy path
- Minimal diff — fix dev script only
- Fail loudly if API/Worker ports never open

---

### Task 1: Fix Maven invocations in run-local-dev.sh

**Files:**
- Modify: `scripts/run-local-dev.sh`

**Root cause:** `mvn -pl services/api -am spring-boot:run` executes `spring-boot:run` on root POM `data-migration-tool` (packaging `pom`, no main class).

**Fix:**
```bash
build_backend() {
  echo "Building backend modules..."
  (cd "${ROOT_DIR}" && mvn -q -pl services/api,services/worker -am install -DskipTests)
}

start_api() {
  echo "Starting API (port 8080)..."
  (cd "${ROOT_DIR}/services/api" && mvn -q spring-boot:run) &
  record_pid $!
  wait_for_port localhost 8080 120 || { echo "API failed to start on :8080"; exit 1; }
}
```

Same pattern for worker on :8081.

---

### Task 2: Remove stale ip-matcher test + extend script test

**Files:**
- Delete: `scripts/test/ip-matcher_test.mjs`
- Modify: `scripts/test/run-local-dev_test.sh` — assert script does NOT use broken `-pl ... spring-boot:run` from root

---

### Task 3: Docs + verification

**Files:**
- Modify: `docs/development.md` — note Maven must build modules first (script handles it)
- Modify: `services/worker/src/main/java/com/migration/WorkerApplication.java` — add `com.migration.queue` to `@EntityScan` (worker failed with "Not a managed type: WorkerHeartbeatEntity")

**Verify:**
- `scripts/test/run-local-dev_test.sh`
- `mvn -q -pl services/api,services/worker -am install -DskipTests`
- Manual: `./run-local-dev.sh --backend` → API :8080 and Worker :8081 listening
