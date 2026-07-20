# Fix JWT Filter Startup NPE Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make `migration-api` start successfully by stopping Tomcat from initializing a CGLIB-proxied `JwtAuthFilter` and by keeping security filters out of the servlet filter chain.

**Architecture:** Security filters stay as Spring beans injected into `SecurityFilterChain` only. Disable Boot’s auto `FilterRegistrationBean` for each. Move last-seen DB writes off `@Transactional` on the filter into `UserService` so no AOP proxy wraps `OncePerRequestFilter`.

**Tech Stack:** Spring Boot 3.3, Spring Security, Spring Data JPA, JUnit 5, Mockito

## Global Constraints

- Do not add new dependencies
- Prefer fewest files; reuse `UserService` / existing filter tests
- Filters must remain wired in `SecurityConfig` as today: IP whitelist before JWT before `UsernamePasswordAuthenticationFilter`
- Keep JWT version/revoke checks and throttled last-seen behavior
- No commits that include secrets (`.env`)

## File map

| File | Role |
|------|------|
| `services/api/src/main/java/com/migration/security/JwtAuthFilter.java` | Drop `@Transactional`; call `UserService.touchLastSeen` |
| `services/api/src/main/java/com/migration/auth/UserService.java` | Add `@Transactional touchLastSeen(UserEntity)` |
| `services/api/src/main/java/com/migration/security/SecurityFilterRegistration.java` | New: disable servlet registration for both filters |
| `services/api/src/test/java/com/migration/security/JwtAuthFilterTest.java` | Construct filter with `UserService` mock / real deps as needed |
| `services/api/src/test/java/com/migration/security/SecurityConfigTest.java` | Keep import wiring working |

---

### Task 1: Move last-seen off filter `@Transactional`

**Files:**
- Modify: `services/api/src/main/java/com/migration/security/JwtAuthFilter.java`
- Modify: `services/api/src/main/java/com/migration/auth/UserService.java`
- Modify: `services/api/src/test/java/com/migration/security/JwtAuthFilterTest.java`
- Modify: `services/api/src/test/java/com/migration/auth/AuthControllerTest.java` (constructor if `UserService` unchanged — only if compile breaks)
- Test: `services/api/src/test/java/com/migration/security/JwtAuthFilterTest.java`

**Interfaces:**
- Consumes: `UserRepository.findById`, `JwtService.parseToken` / `createToken` / `cookieName`
- Produces: `UserService.touchLastSeen(UserEntity user)` — `@Transactional`, updates `lastSeenAt` at most once per 60s, then `save`

- [ ] **Step 1: Write the failing test**

Add to `JwtAuthFilterTest.java`:

```java
@Test
void touchLastSeenGoesThroughUserService() throws Exception {
    UUID id = UUID.randomUUID();
    UserEntity user = new UserEntity();
    user.setId(id);
    user.setEmail("alice@test.com");
    user.setTokenVersion(0);

    UserRepository repo = mock(UserRepository.class);
    when(repo.findById(id)).thenReturn(Optional.of(user));

    UserService users = mock(UserService.class);
    JwtAuthFilter filter = new JwtAuthFilter(jwtService, repo, users);
    String token = jwtService.createToken(id, "alice@test.com", 0);
    MockHttpServletRequest req = new MockHttpServletRequest();
    req.setCookies(new Cookie("migration_token", token));

    filter.doFilter(req, new MockHttpServletResponse(), (r, s) -> {});

    verify(users).touchLastSeen(user);
    assertNotNull(SecurityContextHolder.getContext().getAuthentication());
}
```

Update existing tests to pass a `UserService` mock as the third constructor arg.

- [ ] **Step 2: Run test to verify it fails**

Run: `cd services/api && mvn -q test -Dtest=JwtAuthFilterTest#touchLastSeenGoesThroughUserService`

Expected: FAIL (compile error: constructor arity / method missing) or assertion fail on `verify`.

- [ ] **Step 3: Write minimal implementation**

In `UserService.java` add:

```java
@Transactional
public void touchLastSeen(UserEntity user) {
    Instant now = Instant.now();
    if (user.getLastSeenAt() == null || user.getLastSeenAt().isBefore(now.minus(60, ChronoUnit.SECONDS))) {
        user.setLastSeenAt(now);
        userRepository.save(user);
    }
}
```

In `JwtAuthFilter.java`:

- Remove `@Transactional` import and annotation from `doFilterInternal`
- Add `UserService` field + constructor param
- Replace private `touchLastSeen` with `userService.touchLastSeen(user)`
- Keep `@Component`

```java
@Component
public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final UserService userService;

    public JwtAuthFilter(JwtService jwtService, UserRepository userRepository, UserService userService) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.userService = userService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // ... same claim/user checks ...
                    userService.touchLastSeen(user);
        // ...
        filterChain.doFilter(request, response);
    }
}
```

Update `JwtAuthFilterTest` constructors to `new JwtAuthFilter(jwtService, repo, mock(UserService.class))`.

If `SecurityConfigTest` `@Import` still works with `@Component` scan of filter via Import of class — no change needed unless constructor injection in test context fails; then `@MockBean UserService` or provide a test `@Bean`.

- [ ] **Step 4: Run tests to verify they pass**

Run: `cd services/api && mvn -q test -Dtest=JwtAuthFilterTest,AuthControllerTest,SecurityConfigTest`

Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add services/api/src/main/java/com/migration/security/JwtAuthFilter.java \
  services/api/src/main/java/com/migration/auth/UserService.java \
  services/api/src/test/java/com/migration/security/JwtAuthFilterTest.java \
  services/api/src/test/java/com/migration/auth/AuthControllerTest.java \
  services/api/src/test/java/com/migration/security/SecurityConfigTest.java
git commit -m "$(cat <<'EOF'
fix: stop proxying JwtAuthFilter with @Transactional

Move last-seen updates to UserService so Tomcat can init the filter.
EOF
)"
```

---

### Task 2: Disable servlet FilterRegistrationBean for security filters

**Files:**
- Create: `services/api/src/main/java/com/migration/security/SecurityFilterRegistration.java`
- Test: `services/api/src/test/java/com/migration/security/SecurityFilterRegistrationTest.java`
- Modify only if needed: `SecurityConfigTest.java`

**Interfaces:**
- Consumes: `JwtAuthFilter`, `IpWhitelistFilter` beans
- Produces: two `FilterRegistrationBean<?>` beans with `setEnabled(false)`

- [ ] **Step 1: Write the failing test**

Create `SecurityFilterRegistrationTest.java`:

```java
package com.migration.security;

import org.junit.jupiter.api.Test;
import org.springframework.boot.web.servlet.FilterRegistrationBean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class SecurityFilterRegistrationTest {

    @Test
    void disablesServletRegistrationForSecurityFilters() {
        SecurityFilterRegistration reg = new SecurityFilterRegistration();
        JwtAuthFilter jwt = mock(JwtAuthFilter.class);
        IpWhitelistFilter ip = mock(IpWhitelistFilter.class);

        FilterRegistrationBean<JwtAuthFilter> jwtReg = reg.jwtAuthFilterRegistration(jwt);
        FilterRegistrationBean<IpWhitelistFilter> ipReg = reg.ipWhitelistFilterRegistration(ip);

        assertFalse(jwtReg.isEnabled());
        assertFalse(ipReg.isEnabled());
        assertSame(jwt, jwtReg.getFilter());
        assertSame(ip, ipReg.getFilter());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd services/api && mvn -q test -Dtest=SecurityFilterRegistrationTest`

Expected: FAIL — class not found

- [ ] **Step 3: Write minimal implementation**

Create `SecurityFilterRegistration.java`:

```java
package com.migration.security;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SecurityFilterRegistration {

    @Bean
    FilterRegistrationBean<JwtAuthFilter> jwtAuthFilterRegistration(JwtAuthFilter filter) {
        FilterRegistrationBean<JwtAuthFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    FilterRegistrationBean<IpWhitelistFilter> ipWhitelistFilterRegistration(IpWhitelistFilter filter) {
        FilterRegistrationBean<IpWhitelistFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
}
```

- [ ] **Step 4: Run unit tests**

Run: `cd services/api && mvn -q test -Dtest=SecurityFilterRegistrationTest,JwtAuthFilterTest,SecurityConfigTest,IpWhitelistFilterTest`

Expected: PASS

- [ ] **Step 5: Verify API context starts (smoke)**

With Postgres/Redis already up from `./run-local-dev.sh` infra (or start infra only), run:

```bash
cd services/api && mvn -q -DskipTests spring-boot:run
```

Expected log lines include `Started ApiApplication` and **must not** include `Exception starting filter [jwtAuthFilter]` or `this.logger is null`.

Stop the process after success (Ctrl+C). If ports conflict, kill the stale API first.

- [ ] **Step 6: Commit**

```bash
git add services/api/src/main/java/com/migration/security/SecurityFilterRegistration.java \
  services/api/src/test/java/com/migration/security/SecurityFilterRegistrationTest.java
git commit -m "$(cat <<'EOF'
fix: keep JWT/IP filters out of the servlet filter chain

Disable Boot FilterRegistrationBean so filters run only in SecurityFilterChain.
EOF
)"
```

---

## Spec coverage (self-review)

| Symptom / requirement | Task |
|----------------------|------|
| NPE `logger` null on `jwtAuthFilter` init | Task 1 (no CGLIB) + Task 2 (no Tomcat servlet init) |
| CGLIB warning on `OncePerRequestFilter` | Task 1 |
| Double filter registration | Task 2 |
| last-seen still updated | Task 1 `UserService.touchLastSeen` |
| IP before JWT order unchanged | No change to `SecurityConfig` order |

## Placeholder scan

None — concrete code and commands included.

## Type consistency

`JwtAuthFilter(JwtService, UserRepository, UserService)` · `UserService.touchLastSeen(UserEntity)` · `FilterRegistrationBean.setEnabled(false)`
