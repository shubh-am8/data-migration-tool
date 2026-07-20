# Worker Migration Domain Resolution Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ensure the worker module resolves `com.migration:migration-domain` reliably by pinning the internal module version explicitly in the worker POM.

**Architecture:** Keep dependency resolution simple and local to the affected module. Update only `services/worker/pom.xml` to declare the version for `migration-domain` explicitly using `${project.version}` so Maven does not depend solely on inherited dependency management for this internal module reference.

**Tech Stack:** Maven, Java 21, Spring Boot multi-module build

## Global Constraints

- Keep the fix minimal and scoped to `services/worker/pom.xml`.
- Do not add new dependencies or plugins.
- Verify with Maven compile commands from both repository root and worker module.

---

### Task 1: Pin Worker Internal Dependency Version

**Files:**
- Modify: `services/worker/pom.xml`
- Test: N/A (build verification via Maven commands)

**Interfaces:**
- Consumes: root version property `${project.version}` from parent POM.
- Produces: explicit dependency coordinate `com.migration:migration-domain:${project.version}` in worker module.

- [ ] **Step 1: Write the failing check**

```bash
cd /Users/shubh/developer/data-migration-tool/services/worker
mvn -DskipTests compile
```

Expected: FAIL with missing artifact `com.migration:migration-domain:jar:0.1.0-SNAPSHOT` when dependency cannot be resolved.

- [ ] **Step 2: Update dependency declaration with explicit version**

```xml
<dependency>
    <groupId>com.migration</groupId>
    <artifactId>migration-domain</artifactId>
    <version>${project.version}</version>
</dependency>
```

- [ ] **Step 3: Run module verification**

```bash
cd /Users/shubh/developer/data-migration-tool/services/worker
mvn -q -DskipTests compile
```

Expected: PASS (exit code 0).

- [ ] **Step 4: Run root reactor verification**

```bash
cd /Users/shubh/developer/data-migration-tool
mvn -q -DskipTests compile
```

Expected: PASS (exit code 0).

- [ ] **Step 5: Commit**

```bash
git add services/worker/pom.xml docs/superpowers/plans/2026-07-20-worker-migration-domain-resolution.md
git commit -m "fix: pin worker migration-domain version for reliable resolution"
```
