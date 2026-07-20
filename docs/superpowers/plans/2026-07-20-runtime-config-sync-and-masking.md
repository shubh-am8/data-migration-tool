# Runtime Config Sync and Sensitive Masking Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make env values act as defaults that smartly seed DB config at startup (including future keys), preserve dashboard overrides across restarts, and support masked/unmasked handling of sensitive settings from the UI without forcing app restart for dynamic keys.

**Architecture:** Introduce a single backend metadata registry for runtime-editable settings, including env var mapping, sensitivity, and restart requirement. Bootstrap and API will use this registry so new keys are auto-seeded into DB without hardcoded duplication. Settings UI will consume metadata to show masked values by default with explicit per-key reveal flow.

**Tech Stack:** Spring Boot (Java), Flyway, JPA, Next.js/React, TypeScript, JUnit/Mockito

## Global Constraints

- Preserve existing precedence: dashboard-updated values must not be overwritten by env on restart unless `APP_CONFIG_FORCE_ENV=true`.
- Env values are defaults, not forced source of truth, unless force flag is true.
- Sensitive values must not be returned in plain text from bulk config API.
- Keep changes minimal and follow existing project patterns (no new dependency).
- Support future runtime config keys via metadata-driven env seeding (avoid hardcoded multi-file drift).

---

### Task 1: Centralize Runtime Config Metadata

**Files:**
- Create: `services/api/src/main/java/com/migration/config/RuntimeConfigCatalog.java`
- Modify: `services/api/src/main/java/com/migration/config/AppConfigService.java`
- Test: `services/api/src/test/java/com/migration/config/AppConfigServiceTest.java`

**Interfaces:**
- Consumes: existing `ConfigSource`, `AppConfigEntity`, `AppConfigRepository`
- Produces: `RuntimeConfigCatalog.Entry` with `dbKey()`, `envKey()`, `fallbackPropertyKey()`, `defaultValue()`, `editable()`, `sensitive()`, `restartRequired()`, `validator()`

- [ ] **Step 1: Write the failing test**

```java
@Test
void catalogContainsAuthAndWebhookKeysWithSensitivityMeta() {
    var byKey = RuntimeConfigCatalog.byKey();
    assertTrue(byKey.containsKey("gspace_webhook_url"));
    assertTrue(byKey.containsKey("google_client_secret"));
    assertTrue(byKey.get("google_client_secret").sensitive());
    assertTrue(byKey.get("allowed_email_domain").restartRequired());
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./mvnw -pl services/api -Dtest=AppConfigServiceTest test`
Expected: FAIL with missing `RuntimeConfigCatalog` symbol

- [ ] **Step 3: Write minimal implementation**

```java
public final class RuntimeConfigCatalog {
  public record Entry(
      String dbKey,
      String envKey,
      String fallbackPropertyKey,
      String defaultValue,
      boolean editable,
      boolean sensitive,
      boolean restartRequired,
      Predicate<String> validator
  ) {}

  private static final List<Entry> ENTRIES = List.of(
      entry("min_threads_per_job", "MIN_THREADS_PER_JOB", null, "1", true, false, false, RuntimeConfigValidators::positiveInt),
      entry("max_threads_per_job", "MAX_THREADS_PER_JOB", null, "8", true, false, false, RuntimeConfigValidators::positiveInt),
      entry("default_batch_size", "DEFAULT_BATCH_SIZE", null, "5000", true, false, false, RuntimeConfigValidators::positiveInt),
      entry("gspace_webhook_url", "GSPACE_WEBHOOK_URL", "app.gspace-webhook-url", "", true, true, false, v -> true),
      entry("google_client_id", "GOOGLE_CLIENT_ID", null, "", true, false, true, v -> true),
      entry("google_client_secret", "GOOGLE_CLIENT_SECRET", null, "", true, true, true, v -> true),
      entry("allowed_email_domain", "ALLOWED_EMAIL_DOMAIN", "app.auth.allowed-email-domain", "", true, false, true, v -> true)
  );
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./mvnw -pl services/api -Dtest=AppConfigServiceTest test`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add services/api/src/main/java/com/migration/config/RuntimeConfigCatalog.java services/api/src/main/java/com/migration/config/AppConfigService.java services/api/src/test/java/com/migration/config/AppConfigServiceTest.java
git commit -m "refactor: centralize runtime config metadata for editable keys"
```

### Task 2: Make Bootstrap Metadata-Driven and Future-Key Smart

**Files:**
- Modify: `services/api/src/main/java/com/migration/config/AppConfigBootstrap.java`
- Modify: `services/api/src/main/java/com/migration/config/AppConfigService.java`
- Test: `services/api/src/test/java/com/migration/config/AppConfigBootstrapTest.java`

**Interfaces:**
- Consumes: `RuntimeConfigCatalog.entries()`, `Environment`
- Produces: startup env mapping generated from catalog; insert missing DB rows using env value or default value

- [ ] **Step 1: Write the failing test**

```java
@Test
void bootstrapSeedsMissingCatalogKeysWithoutOverwritingDashboard() {
    var env = new StubEnv(Map.of(
        "GOOGLE_CLIENT_SECRET", "env-secret",
        "ALLOWED_EMAIL_DOMAIN", "chatbot.team"
    ));
    var bootstrap = new AppConfigBootstrap(repository, env, false);

    AppConfigEntity dashboard = new AppConfigEntity();
    dashboard.setKey("allowed_email_domain");
    dashboard.setValue("custom.domain");
    dashboard.setSource(ConfigSource.DASHBOARD);

    when(repository.findById("allowed_email_domain")).thenReturn(Optional.of(dashboard));
    when(repository.findById("google_client_secret")).thenReturn(Optional.empty());

    bootstrap.run(new DefaultApplicationArguments(new String[0]));

    verify(repository, never()).save(argThat(e -> "allowed_email_domain".equals(e.getKey()) && "chatbot.team".equals(e.getValue())));
    verify(repository).save(argThat(e -> "google_client_secret".equals(e.getKey()) && "env-secret".equals(e.getValue())));
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./mvnw -pl services/api -Dtest=AppConfigBootstrapTest test`
Expected: FAIL because new keys are not seeded by current hardcoded `envMappings()`

- [ ] **Step 3: Write minimal implementation**

```java
Map<String, String> envMappings() {
  Map<String, String> m = new LinkedHashMap<>();
  for (RuntimeConfigCatalog.Entry entry : RuntimeConfigCatalog.entries()) {
    String value = firstNonBlank(
        environment.getProperty(entry.envKey()),
        entry.fallbackPropertyKey() != null ? environment.getProperty(entry.fallbackPropertyKey()) : null
    );
    if (value != null && !value.isBlank()) {
      m.put(entry.dbKey(), value);
    } else if (!entry.defaultValue().isBlank()) {
      m.putIfAbsent(entry.dbKey(), entry.defaultValue());
    }
  }
  return m;
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./mvnw -pl services/api -Dtest=AppConfigBootstrapTest test`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add services/api/src/main/java/com/migration/config/AppConfigBootstrap.java services/api/src/main/java/com/migration/config/AppConfigService.java services/api/src/test/java/com/migration/config/AppConfigBootstrapTest.java
git commit -m "feat: auto-seed runtime config keys from env metadata"
```

### Task 3: Add Safe Settings API for Masked/Reveal and Validation

**Files:**
- Modify: `services/api/src/main/java/com/migration/config/AppConfigService.java`
- Modify: `services/api/src/main/java/com/migration/config/AppConfigController.java`
- Create: `services/api/src/test/java/com/migration/config/AppConfigControllerTest.java`
- Test: `services/api/src/test/java/com/migration/config/AppConfigServiceTest.java`

**Interfaces:**
- Consumes: `RuntimeConfigCatalog`
- Produces: `ConfigEntryDto(value, source, updatedAt, sensitive, masked, restartRequired)` and `GET /api/config/{key}/reveal`

- [ ] **Step 1: Write the failing test**

```java
@Test
void sensitiveValuesAreMaskedInListAndRevealedPerKey() throws Exception {
  mockMvc.perform(get("/api/config"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.gspace_webhook_url.masked").value(true))
      .andExpect(jsonPath("$.gspace_webhook_url.value").value("********"));

  mockMvc.perform(get("/api/config/gspace_webhook_url/reveal"))
      .andExpect(status().isOk())
      .andExpect(content().string("https://chat.googleapis.com/..."));
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./mvnw -pl services/api -Dtest=AppConfigControllerTest,AppConfigServiceTest test`
Expected: FAIL because DTO lacks masking metadata and reveal endpoint does not exist

- [ ] **Step 3: Write minimal implementation**

```java
public record ConfigEntryDto(
    String value,
    String source,
    String updatedAt,
    boolean sensitive,
    boolean masked,
    boolean restartRequired
) {}

private static String maskedValue(String raw) {
  return (raw == null || raw.isBlank()) ? "" : "********";
}

@GetMapping("/{key}/reveal")
public String revealConfig(@PathVariable String key) {
  return appConfigService.revealSensitiveValue(key);
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./mvnw -pl services/api -Dtest=AppConfigControllerTest,AppConfigServiceTest test`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add services/api/src/main/java/com/migration/config/AppConfigService.java services/api/src/main/java/com/migration/config/AppConfigController.java services/api/src/test/java/com/migration/config/AppConfigControllerTest.java services/api/src/test/java/com/migration/config/AppConfigServiceTest.java
git commit -m "feat: mask sensitive settings and add explicit reveal endpoint"
```

### Task 4: Update Settings UI for Mask/Unmask and Restart Hint

**Files:**
- Modify: `apps/web/src/components/shared/ConfigEditor.tsx`
- Modify: `apps/web/src/app/settings/page.tsx`
- Test: `apps/web/src/components/shared/ConfigEditor.test.tsx`

**Interfaces:**
- Consumes: `GET /api/config` DTO fields and `GET /api/config/{key}/reveal`
- Produces: masked display with per-row Show/Hide control and restart badge for restart-bound keys

- [ ] **Step 1: Write the failing test**

```tsx
it("renders masked sensitive value and reveals on click", async () => {
  render(<ConfigEditor config={{
    gspace_webhook_url: { value: "********", source: "ENV", sensitive: true, masked: true, restartRequired: false }
  }} ... />);
  expect(screen.getByDisplayValue("********")).toBeInTheDocument();
  await user.click(screen.getByRole("button", { name: /show/i }));
  expect(mockRevealHandler).toHaveBeenCalledWith("gspace_webhook_url");
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `pnpm --filter web test ConfigEditor.test.tsx`
Expected: FAIL due to missing sensitive metadata props and reveal action

- [ ] **Step 3: Write minimal implementation**

```tsx
{entry.sensitive && (
  <Button type="button" variant="ghost" onClick={() => onReveal(key)}>
    {entry.masked ? "Show" : "Hide"}
  </Button>
)}
{entry.restartRequired && <Badge variant="outline">Restart required</Badge>}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `pnpm --filter web test ConfigEditor.test.tsx`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add apps/web/src/components/shared/ConfigEditor.tsx apps/web/src/app/settings/page.tsx apps/web/src/components/shared/ConfigEditor.test.tsx
git commit -m "feat: add sensitive show-hide controls in settings UI"
```

### Task 5: End-to-End Regression Coverage for Precedence and Future-Key Sync

**Files:**
- Modify: `services/api/src/test/java/com/migration/config/AppConfigBootstrapTest.java`
- Modify: `services/api/src/test/java/com/migration/config/AppConfigServiceTest.java`
- Test: `services/api/src/test/java/com/migration/config/AppConfigControllerTest.java`

**Interfaces:**
- Consumes: all prior tasks
- Produces: coverage for “UI override wins”, “new metadata key auto-seeds”, “invalid update rejected”

- [ ] **Step 1: Write the failing test**

```java
@Test
void dashboardOverrideSurvivesRestartAndFutureKeyAutoSeeds() {
  // arrange dashboard override on first run, then simulate second bootstrap
  // assert dashboard value unchanged and new catalog key gets inserted
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./mvnw -pl services/api -Dtest=AppConfigBootstrapTest,AppConfigServiceTest,AppConfigControllerTest test`
Expected: FAIL before final behavior wiring is complete

- [ ] **Step 3: Write minimal implementation**

```java
// tighten validators in AppConfigService.update:
if (!entry.validator().test(value)) {
  throw new IllegalArgumentException("Invalid value for " + key);
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./mvnw -pl services/api -Dtest=AppConfigBootstrapTest,AppConfigServiceTest,AppConfigControllerTest test`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add services/api/src/test/java/com/migration/config/AppConfigBootstrapTest.java services/api/src/test/java/com/migration/config/AppConfigServiceTest.java services/api/src/test/java/com/migration/config/AppConfigControllerTest.java services/api/src/main/java/com/migration/config/AppConfigService.java
git commit -m "test: cover config precedence, masking, and future key seeding"
```

