# Runtime OAuth + Duration Chunk Migration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Apply Google OAuth client id/secret and allowed email domain from Settings without restart; remove batch size from settings; add Approach-2 duration-based time chunking on jobs with human-readable Settings/Job UI labels.

**Architecture:** Catalog-driven runtime config feeds a dynamic `ClientRegistrationRepository` and live domain checks. Jobs store range/chunk fields; worker `TimeChunkPlanner` splits hot/cold windows into time predicates; JDBC row LIMIT stays an internal constant.

**Tech Stack:** Spring Boot, Spring Security OAuth2, JPA/Flyway, JUnit/Mockito, Next.js/React/TypeScript

## Global Constraints

- Env values are defaults; dashboard overrides must not be overwritten on restart unless `APP_CONFIG_FORCE_ENV=true`.
- OAuth/domain settings must apply without app restart.
- Settings UI headings must be human-readable (never raw snake_case).
- Remove `default_batch_size` from settings/catalog; do not expose row batch size in Settings.
- Internal JDBC row batch size constant is `5000`.
- Job chunking uses Approach 2: `rangeStart`, `rangeEndMode` (`NOW`|`FIXED`), optional `rangeEnd`, `hotBoundaryDays` (DB column `hot_days`), `minChunkDurationHours`, `maxChunkDurationHours`.
- Prefer max chunk hours when splitting; last chunk may be shorter than min if remainder is smaller.
- No new dependencies.
- Shortest working diff; follow existing package layout.

## File Structure

| File | Responsibility |
|---|---|
| `RuntimeConfigCatalog.java` | Editable keys metadata (no batch size; OAuth live) |
| `DynamicGoogleClientRegistrationRepository.java` | Live Google OAuth client from AppConfig |
| `OAuthDomainValidator.java` | Live domain from AppConfig |
| `GoogleOAuth2Config.java` | Live `hd` param from AppConfig |
| `V4__job_range_chunks.sql` | Job range/chunk columns |
| `JobEntity.java` | New fields |
| `RangeEndMode.java` | Enum NOW/FIXED |
| `TimeChunkPlanner.java` | Pure planner |
| `JobEntityHotCold.java` / `HotColdManager.java` / `BatchCopyEngine.java` / `JobQueueConsumer.java` | Chunked copy |
| `JobService.java` | Persist/validate new fields |
| `HotColdConfig.tsx` / `JobWizard.tsx` | Job form |
| `ConfigEditor.tsx` | Human labels; no batch size |

---

### Task 1: Drop batch size from catalog; mark OAuth keys live

**Files:**
- Modify: `services/api/src/main/java/com/migration/config/RuntimeConfigCatalog.java`
- Modify: `services/api/src/main/java/com/migration/config/AppConfigBootstrap.java` (remove `default_batch_size` from `FLYWAY_DEFAULTS` if present)
- Modify: `services/api/src/test/java/com/migration/config/AppConfigServiceTest.java`
- Create: `services/api/src/main/resources/db/migration/V4__drop_default_batch_size.sql` (only DELETE the config row; job columns come in Task 4)

**Interfaces:**
- Consumes: existing catalog API
- Produces: catalog without `default_batch_size`; `google_*` and `allowed_email_domain` with `restartRequired=false`

- [ ] **Step 1: Write the failing test**

```java
@Test
void catalogDropsBatchSizeAndMarksOauthLive() {
    var byKey = RuntimeConfigCatalog.byKey();
    assertFalse(byKey.containsKey("default_batch_size"));
    assertFalse(AppConfigService.EDITABLE_KEYS.contains("default_batch_size"));
    assertFalse(byKey.get("google_client_id").restartRequired());
    assertFalse(byKey.get("google_client_secret").restartRequired());
    assertFalse(byKey.get("allowed_email_domain").restartRequired());
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -pl services/api -Dtest=AppConfigServiceTest#catalogDropsBatchSizeAndMarksOauthLive test`
Expected: FAIL (batch size still present and/or restartRequired true)

- [ ] **Step 3: Write minimal implementation**

In `RuntimeConfigCatalog`, remove the `default_batch_size` entry and set `restartRequired` to `false` for `google_client_id`, `google_client_secret`, `allowed_email_domain`.

Create `V4__drop_default_batch_size.sql`:

```sql
DELETE FROM app_config WHERE config_key = 'default_batch_size';
```

Update `AppConfigBootstrap.FLYWAY_DEFAULTS` to omit `default_batch_size`.

Update `AppConfigServiceTest.editableKeysAreDefined` to stop asserting `default_batch_size`.

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -pl services/api -Dtest=AppConfigServiceTest test`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add services/api/src/main/java/com/migration/config/RuntimeConfigCatalog.java \
  services/api/src/main/java/com/migration/config/AppConfigBootstrap.java \
  services/api/src/main/resources/db/migration/V4__drop_default_batch_size.sql \
  services/api/src/test/java/com/migration/config/AppConfigServiceTest.java
git commit -m "fix: remove batch size setting and mark OAuth config live"
```

---

### Task 2: Live OAuth domain + Google client registration from AppConfig

**Files:**
- Create: `services/api/src/main/java/com/migration/security/DynamicGoogleClientRegistrationRepository.java`
- Modify: `services/api/src/main/java/com/migration/security/OAuthDomainValidator.java`
- Modify: `services/api/src/main/java/com/migration/security/GoogleOAuth2Config.java`
- Modify: `services/api/src/test/java/com/migration/security/OAuthDomainValidatorTest.java`
- Create: `services/api/src/test/java/com/migration/security/DynamicGoogleClientRegistrationRepositoryTest.java`

**Interfaces:**
- Consumes: `AppConfigService.get(String key)` for `allowed_email_domain`, `google_client_id`, `google_client_secret`
- Produces: `ClientRegistrationRepository` bean named for Google registration id `google`; domain validation that reflects latest DB value

- [ ] **Step 1: Write the failing tests**

```java
@Test
void domainValidatorUsesLatestConfigValue() {
    AppConfigService cfg = mock(AppConfigService.class);
    when(cfg.get("allowed_email_domain")).thenReturn("chatbot.team");
    OAuthDomainValidator v = new OAuthDomainValidator(cfg);
    OAuth2User user = mock(OAuth2User.class);
    when(user.getAttribute("email")).thenReturn("a@other.com");
    when(user.getAttribute("email_verified")).thenReturn(true);
    assertThrows(OAuth2AuthenticationException.class, () -> v.validate(user));
}

@Test
void registrationUsesClientIdFromConfig() {
    AppConfigService cfg = mock(AppConfigService.class);
    when(cfg.get("google_client_id")).thenReturn("id-from-db");
    when(cfg.get("google_client_secret")).thenReturn("secret-from-db");
    var repo = new DynamicGoogleClientRegistrationRepository(cfg);
    assertEquals("id-from-db", repo.findByRegistrationId("google").getClientId());
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `mvn -pl services/api -Dtest=OAuthDomainValidatorTest,DynamicGoogleClientRegistrationRepositoryTest test`
Expected: FAIL (constructor still `@Value`, class missing)

- [ ] **Step 3: Write minimal implementation**

```java
@Component
public class OAuthDomainValidator {
    private final AppConfigService appConfigService;
    public OAuthDomainValidator(AppConfigService appConfigService) {
        this.appConfigService = appConfigService;
    }
    public void validate(OAuth2User oauth) {
        String allowedDomain = appConfigService.get("allowed_email_domain");
        allowedDomain = allowedDomain == null ? "" : allowedDomain.trim().toLowerCase();
        if (allowedDomain.isBlank()) return;
        // existing email/verified checks...
    }
}
```

```java
@Component
public class DynamicGoogleClientRegistrationRepository implements ClientRegistrationRepository {
    private final AppConfigService appConfigService;
    public DynamicGoogleClientRegistrationRepository(AppConfigService appConfigService) {
        this.appConfigService = appConfigService;
    }
    @Override
    public ClientRegistration findByRegistrationId(String registrationId) {
        if (!"google".equals(registrationId)) return null;
        String clientId = appConfigService.get("google_client_id");
        String clientSecret = appConfigService.get("google_client_secret");
        return ClientRegistration.withRegistrationId("google")
            .clientId(clientId == null ? "" : clientId)
            .clientSecret(clientSecret == null ? "" : clientSecret)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
            .scope("openid", "profile", "email")
            .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
            .tokenUri("https://www.googleapis.com/oauth2/v4/token")
            .jwkSetUri("https://www.googleapis.com/oauth2/v3/certs")
            .userInfoUri("https://www.googleapis.com/oauth2/v3/userinfo")
            .userNameAttributeName("sub")
            .clientName("Google")
            .build();
    }
}
```

Update `GoogleOAuth2Config` to inject `AppConfigService` and set `hd` from `appConfigService.get("allowed_email_domain")` inside the customizer (read on each authorize request).

Ensure Spring uses the custom repository: `@Primary` on `DynamicGoogleClientRegistrationRepository` or exclude auto-config conflict — prefer `@Primary` `@Component` implementing `ClientRegistrationRepository`.

- [ ] **Step 4: Run tests to verify they pass**

Run: `mvn -pl services/api -Dtest=OAuthDomainValidatorTest,DynamicGoogleClientRegistrationRepositoryTest,SecurityConfigTest test`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add services/api/src/main/java/com/migration/security/ \
  services/api/src/test/java/com/migration/security/
git commit -m "feat: apply OAuth client and domain from settings without restart"
```

---

### Task 3: Settings UI human labels (no snake_case, no batch size)

**Files:**
- Modify: `apps/web/src/components/shared/ConfigEditor.tsx`
- Modify: `apps/web/src/components/shared/ConfigEditor.test.tsx`
- Modify: `apps/web/src/app/settings/page.tsx` (description: remove misleading “restart” implications if any)

**Interfaces:**
- Consumes: config keys from API
- Produces: labels for all known keys including Google/domain; no `default_batch_size` label needed

- [ ] **Step 1: Write the failing test**

```tsx
it("shows human labels for google and domain keys", () => {
  render(<ConfigEditor config={{
    google_client_id: { value: "x", source: "ENV", sensitive: false, masked: false, restartRequired: false },
    allowed_email_domain: { value: "chatbot.team", source: "ENV", sensitive: false, masked: false, restartRequired: false },
  }} onChange={jest.fn()} onSave={jest.fn()} onReveal={jest.fn()} />);
  expect(screen.getByText("Google Client ID")).toBeInTheDocument();
  expect(screen.getByText("Allowed email domain")).toBeInTheDocument();
  expect(screen.queryByText("google_client_id")).not.toBeInTheDocument();
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `pnpm --filter web test ConfigEditor.test.tsx`
Expected: FAIL (label missing)

- [ ] **Step 3: Write minimal implementation**

```tsx
const LABELS: Record<string, string> = {
  min_threads_per_job: "Min threads per job",
  max_threads_per_job: "Max threads per job",
  gspace_webhook_url: "GSpace webhook URL",
  google_client_id: "Google Client ID",
  google_client_secret: "Google Client Secret",
  allowed_email_domain: "Allowed email domain",
};
```

Remove `default_batch_size` from LABELS. Prefer `LABELS[key] ?? titleCase(key)` only as last resort — for known keys always use LABELS. Do not render `restartRequired` badge when false; if true still show (should be unused for OAuth).

Update settings page description to: `App configuration — dashboard edits apply immediately where supported.`

- [ ] **Step 4: Run test to verify it passes**

Run: `pnpm --filter web test ConfigEditor.test.tsx`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add apps/web/src/components/shared/ConfigEditor.tsx \
  apps/web/src/components/shared/ConfigEditor.test.tsx \
  apps/web/src/app/settings/page.tsx
git commit -m "fix: use human-readable settings labels for OAuth keys"
```

---

### Task 4: Job schema + entity fields for range and chunk duration

**Files:**
- Create: `services/api/src/main/resources/db/migration/V5__job_range_chunks.sql`
- Create: `packages/domain/src/main/java/com/migration/jobs/RangeEndMode.java`
- Modify: `packages/domain/src/main/java/com/migration/jobs/JobEntity.java`
- Modify: `services/api/src/main/java/com/migration/jobs/JobService.java` (apply/validate/toDto)
- Test: `services/api/src/test/java/com/migration/jobs/JobServiceValidationTest.java` (create if missing)

**Interfaces:**
- Consumes: JSON body keys `rangeStart`, `rangeEndMode`, `rangeEnd`, `hotDays`, `minChunkDurationHours`, `maxChunkDurationHours`, `tsColumn`
- Produces: persisted columns; validation errors for invalid chunk/range

- [ ] **Step 1: Write the failing test**

```java
@Test
void rejectsMaxChunkLessThanMin() {
    JobEntity job = new JobEntity();
    job.setMigrationMode(MigrationMode.HOT_THEN_COLD);
    job.setTsColumn("created_at");
    job.setHotDays(7);
    job.setRangeStart(Instant.parse("2024-01-01T00:00:00Z"));
    job.setRangeEndMode(RangeEndMode.NOW);
    job.setMinChunkDurationHours(48);
    job.setMaxChunkDurationHours(24);
    assertThrows(IllegalArgumentException.class, () -> JobService.validateRangeChunks(job));
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -pl services/api -Dtest=JobServiceValidationTest test`
Expected: FAIL (missing types/method)

- [ ] **Step 3: Write minimal implementation**

`V5__job_range_chunks.sql`:

```sql
ALTER TABLE jobs
  ADD COLUMN IF NOT EXISTS range_start TIMESTAMPTZ,
  ADD COLUMN IF NOT EXISTS range_end_mode VARCHAR(16) NOT NULL DEFAULT 'NOW',
  ADD COLUMN IF NOT EXISTS range_end TIMESTAMPTZ,
  ADD COLUMN IF NOT EXISTS min_chunk_duration_hours INT NOT NULL DEFAULT 24,
  ADD COLUMN IF NOT EXISTS max_chunk_duration_hours INT NOT NULL DEFAULT 168;
```

```java
public enum RangeEndMode { NOW, FIXED }
```

Add fields + getters/setters on `JobEntity`. In `JobService.applyBody` / `toDto` / `validateJob` call `validateRangeChunks`:

```java
static void validateRangeChunks(JobEntity job) {
    int min = job.getMinChunkDurationHours() == null ? 24 : job.getMinChunkDurationHours();
    int max = job.getMaxChunkDurationHours() == null ? 168 : job.getMaxChunkDurationHours();
    if (min < 1 || max < min) throw new IllegalArgumentException("invalid chunk duration hours");
    RangeEndMode mode = job.getRangeEndMode() == null ? RangeEndMode.NOW : job.getRangeEndMode();
    if (mode == RangeEndMode.FIXED && job.getRangeEnd() == null) {
        throw new IllegalArgumentException("rangeEnd required when rangeEndMode is FIXED");
    }
    if (job.getMigrationMode() != MigrationMode.HOT_ONLY) {
        if (job.getRangeStart() == null) throw new IllegalArgumentException("rangeStart required for cold migration");
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -pl services/api -Dtest=JobServiceValidationTest test`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add packages/domain/src/main/java/com/migration/jobs/ \
  services/api/src/main/resources/db/migration/V5__job_range_chunks.sql \
  services/api/src/main/java/com/migration/jobs/JobService.java \
  services/api/src/test/java/com/migration/jobs/JobServiceValidationTest.java
git commit -m "feat: add job range and chunk duration fields"
```

---

### Task 5: TimeChunkPlanner + worker hot/cold chunk execution

**Files:**
- Create: `services/worker/src/main/java/com/migration/engine/TimeChunkPlanner.java`
- Create: `services/worker/src/test/java/com/migration/engine/TimeChunkPlannerTest.java`
- Modify: `services/worker/src/main/java/com/migration/engine/JobEntityHotCold.java`
- Modify: `services/worker/src/main/java/com/migration/engine/HotColdManager.java`
- Modify: `services/worker/src/main/java/com/migration/engine/BatchCopyEngine.java`
- Modify: `services/worker/src/main/java/com/migration/queue/JobQueueConsumer.java`
- Modify: `services/worker/src/test/java/com/migration/engine/HotColdManagerTest.java`

**Interfaces:**
- Consumes: job range fields; `Instant now`
- Produces: `record TimeChunk(Instant start, Instant end)`; `List<TimeChunk> plan(Instant start, Instant end, int minHours, int maxHours)`; filter SQL per chunk; `ROW_BATCH_SIZE = 5000`

- [ ] **Step 1: Write the failing test**

```java
@Test
void plansMaxSizedChunksWithShortTail() {
    Instant start = Instant.parse("2024-01-01T00:00:00Z");
    Instant end = start.plus(Duration.ofHours(50));
    List<TimeChunkPlanner.TimeChunk> chunks = TimeChunkPlanner.plan(start, end, 1, 24);
    assertEquals(3, chunks.size());
    assertEquals(start, chunks.get(0).start());
    assertEquals(start.plus(Duration.ofHours(24)), chunks.get(0).end());
    assertEquals(Duration.ofHours(2), Duration.between(chunks.get(2).start(), chunks.get(2).end()));
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -pl services/worker -Dtest=TimeChunkPlannerTest test`
Expected: FAIL (class missing)

- [ ] **Step 3: Write minimal implementation**

```java
public final class TimeChunkPlanner {
    public record TimeChunk(Instant start, Instant end) {}
    public static List<TimeChunk> plan(Instant start, Instant end, int minHours, int maxHours) {
        if (start == null || end == null || !end.isAfter(start)) return List.of();
        if (minHours < 1 || maxHours < minHours) throw new IllegalArgumentException("invalid chunk hours");
        Duration step = Duration.ofHours(maxHours);
        List<TimeChunk> out = new ArrayList<>();
        Instant cursor = start;
        while (cursor.isBefore(end)) {
            Instant next = cursor.plus(step);
            if (next.isAfter(end)) next = end;
            out.add(new TimeChunk(cursor, next));
            cursor = next;
        }
        return out;
    }
}
```

Update `JobEntityHotCold`:

```java
static Instant effectiveEnd(JobEntity job, Instant now) {
    if (job.getRangeEndMode() == RangeEndMode.FIXED && job.getRangeEnd() != null) return job.getRangeEnd();
    return now;
}
static Instant hotBoundary(JobEntity job, Instant effectiveEnd) {
    int days = job.getHotDays() == null ? 0 : job.getHotDays();
    return effectiveEnd.minus(Duration.ofDays(days));
}
static String timeRangeFilter(String tsColumn, Instant start, Instant end) {
    String col = "\"" + tsColumn.replace("\"", "") + "\"";
    return col + " >= '" + start + "' AND " + col + " < '" + end + "'";
}
```

`HotColdManager.runJob`: for each phase, compute window, plan chunks, call `batchCopyEngine.copyPhase(..., filter, ROW_BATCH_SIZE)` summing rows. Replace hardcoded 5000 in `JobQueueConsumer` with `HotColdManager.ROW_BATCH_SIZE` or constant in manager.

`BatchCopyEngine.copyPhase`: accept explicit `hotColdFilter` string (or Instant pair) instead of only building from old NOW()-N formula.

- [ ] **Step 4: Run tests to verify they pass**

Run: `mvn -pl services/worker -Dtest=TimeChunkPlannerTest,HotColdManagerTest test`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add services/worker/src/main/java/com/migration/engine/ \
  services/worker/src/main/java/com/migration/queue/JobQueueConsumer.java \
  services/worker/src/test/java/com/migration/engine/
git commit -m "feat: plan and copy hot/cold data in duration time chunks"
```

---

### Task 6: Job creation UI for range and chunk duration

**Files:**
- Modify: `apps/web/src/components/jobs/HotColdConfig.tsx`
- Modify: `apps/web/src/components/jobs/JobWizard.tsx`
- Create: `apps/web/src/components/jobs/HotColdConfig.test.tsx` (if web test runner already present)

**Interfaces:**
- Consumes: parent state for new fields
- Produces: payload including `rangeStart`, `rangeEndMode`, `rangeEnd`, `hotDays`, `minChunkDurationHours`, `maxChunkDurationHours`, `tsColumn`

- [ ] **Step 1: Write the failing test**

```tsx
it("renders range and chunk duration fields", () => {
  render(<HotColdConfig
    migrationMode="HOT_THEN_COLD" hotDays={7} tsColumn="created_at"
    rangeStart="2024-01-01T00:00" rangeEndMode="NOW" rangeEnd=""
    minChunkDurationHours={24} maxChunkDurationHours={168}
    onChange={jest.fn()}
  />);
  expect(screen.getByText("Range start")).toBeInTheDocument();
  expect(screen.getByText("Min chunk duration (hours)")).toBeInTheDocument();
  expect(screen.getByText("Max chunk duration (hours)")).toBeInTheDocument();
  expect(screen.getByText("Hot window (days)")).toBeInTheDocument();
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `pnpm --filter web test HotColdConfig.test.tsx`
Expected: FAIL (props/labels missing)

- [ ] **Step 3: Write minimal implementation**

Extend `HotColdConfig` props and form fields:

- Range start (`datetime-local`)
- End mode select: “Always now” / “Fixed end”
- Range end (`datetime-local`) when FIXED
- Hot window (days) — rename from “Hot data days (N)”
- Timestamp column
- Min/Max chunk duration (hours)

Show range/chunk fields whenever mode is not purely unconstrained; for `HOT_ONLY` still show hot window + ts + chunks; for cold modes require range start.

Wire `JobWizard` state and submit body.

- [ ] **Step 4: Run test to verify it passes**

Run: `pnpm --filter web test HotColdConfig.test.tsx ConfigEditor.test.tsx`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add apps/web/src/components/jobs/HotColdConfig.tsx \
  apps/web/src/components/jobs/JobWizard.tsx \
  apps/web/src/components/jobs/HotColdConfig.test.tsx
git commit -m "feat: add range and chunk duration fields to job form"
```

---

### Task 7: Regression glue — API DTO + worker domain jar alignment

**Files:**
- Modify: `services/api/src/main/java/com/migration/jobs/JobService.java` (ensure toDto exposes new fields; remove any remaining batch-size docs in code comments)
- Modify: `services/api/src/main/java/com/migration/jobs/JobService.java` `buildHotColdFilter` — deprecate old NOW()-N for count estimates OR update to use same window math with `Instant.now()` for preview
- Test: `services/worker/src/test/java/com/migration/engine/TimeChunkPlannerTest.java`
- Test: `services/api/src/test/java/com/migration/config/AppConfigServiceTest.java`

- [ ] **Step 1: Write the failing test**

```java
@Test
void buildHotColdFilterForHotUsesRangeWhenPresent() {
    JobEntity job = new JobEntity();
    job.setTsColumn("created_at");
    job.setHotDays(7);
    job.setRangeEndMode(RangeEndMode.NOW);
    job.setMinChunkDurationHours(24);
    job.setMaxChunkDurationHours(24);
    // API estimate path should not use bare NOW()-N only; assert filter contains timestamp literals when using shared helper
    String filter = JobEntityHotCold.timeRangeFilter("created_at",
        Instant.parse("2024-01-10T00:00:00Z"), Instant.parse("2024-01-11T00:00:00Z"));
    assertTrue(filter.contains("2024-01-10"));
}
```

- [ ] **Step 2: Run test to verify it fails if helper missing on API side**

If API cannot depend on worker class, duplicate a tiny shared helper in `packages/domain` as `TimeRangeFilters.java` used by both API and worker — preferred:

Create `packages/domain/src/main/java/com/migration/jobs/TimeRangeFilters.java` with `timeRangeFilter` and move planner to domain if both need it; **YAGNI:** keep planner in worker only; API estimate may keep simplified filter. For Task 7, only ensure JobService toDto/applyBody round-trip new fields and API tests pass.

- [ ] **Step 3: Minimal glue**

Ensure `applyBody` parses:

```java
if (body.containsKey("rangeStart")) job.setRangeStart(Instant.parse((String) body.get("rangeStart")));
if (body.containsKey("rangeEndMode")) job.setRangeEndMode(RangeEndMode.valueOf((String) body.get("rangeEndMode")));
if (body.containsKey("rangeEnd")) job.setRangeEnd(body.get("rangeEnd") == null ? null : Instant.parse((String) body.get("rangeEnd")));
if (body.containsKey("minChunkDurationHours")) job.setMinChunkDurationHours(((Number) body.get("minChunkDurationHours")).intValue());
if (body.containsKey("maxChunkDurationHours")) job.setMaxChunkDurationHours(((Number) body.get("maxChunkDurationHours")).intValue());
```

DTO map includes the same keys.

- [ ] **Step 4: Run combined tests**

Run: `mvn -pl services/api,services/worker -am -Dtest=AppConfigServiceTest,JobServiceValidationTest,TimeChunkPlannerTest,HotColdManagerTest,OAuthDomainValidatorTest,DynamicGoogleClientRegistrationRepositoryTest test`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add services/api/src/main/java/com/migration/jobs/JobService.java \
  packages/domain/src/main/java/com/migration/jobs/ \
  services/api/src/test/java/com/migration/jobs/
git commit -m "feat: wire job API DTO for range and chunk settings"
```
