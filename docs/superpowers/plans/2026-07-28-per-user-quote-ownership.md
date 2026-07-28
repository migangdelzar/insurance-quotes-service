# Per-User Quote Ownership Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Scope every quote to the user who created it, add a read-only ADMIN role for cross-user oversight, and close the cache-key bug that ownership would otherwise reopen.

**Architecture:** Add `quotes.user_id` and `users.role` columns. Thread ownership through the existing hexagonal layers (domain → port → JPA adapter → application service → controller) using two distinct parameter shapes: a plain `UUID ownerId` for methods that must **always** be owner-scoped (create, updateCoverage, submit, and every submission-flow step), and a `RequestingUser(UUID id, boolean admin)` for the three read methods where an admin may see everything (`getQuote`, `listQuotes`, `getSummary`). The `admin` flag never reaches a mutation path, so an admin bypass can't leak into a write by accident.

**Tech Stack:** Java 17, Spring Boot, Spring Data JPA, Spring Security OAuth2 Resource Server (JWT), PostgreSQL/Flyway, Redis cache, JUnit 5 + Mockito + AssertJ, `@DataJpaTest` + Testcontainers, `@WebMvcTest` + MockMvc + `spring-security-test`.

## Global Constraints

- Follow `rules/principles.md` (functional-first, early returns, no enum-switch chains where the codebase already uses polymorphism, no dynamic/reflective magic beyond what's already in `JpaQuoteRepository`'s `Specification` builder).
- No behavior change to the OpenAPI-visible request/response DTOs (`QuoteView`, `LoginResponse`, `TokenPairResponse`, etc.) — ownership is derived entirely from the JWT, never from request bodies, so `docs/api/openapi.yaml` does not need regenerating.
- `spotless:check` runs on `mvn verify` and as a pre-commit hook — run `mvn -pl service spotless:apply` before committing if formatting drifts.
- Every existing test that currently compiles against `QuoteApi`/`SubmissionApi`/`QuoteRepository`/`Quote.createDraft`/`User.create` will fail to compile until its call site is updated in the same task — Java requires the whole module to compile, so these updates are *not* optional cleanup, they're part of making the task's tests runnable at all.
- Coverage gate: `jacoco:check` bound to `verify`, line ≥ 80% on `domain`/`application` packages. Don't skip writing tests for new branches (admin vs. owner-scoped) to keep this green.

---

### Task 1: Add a `role` to users, seed a demo admin, and carry `uid`/`role` claims on the access JWT

This task is self-contained — it does not touch `Quote` at all, so it compiles and its tests pass independently of Task 2.

**Files:**
- Create: `service/src/main/resources/db/migration/V5__add_role_to_users.sql`
- Create: `service/src/main/java/com/clara/insurancequotes/auth/domain/model/UserRole.java`
- Modify: `service/src/main/java/com/clara/insurancequotes/auth/domain/model/User.java`
- Modify: `service/src/main/java/com/clara/insurancequotes/auth/configuration/DemoUserProperties.java`
- Modify: `service/src/main/java/com/clara/insurancequotes/auth/application/service/DemoUserSeeder.java`
- Modify: `service/src/main/java/com/clara/insurancequotes/auth/application/service/TokenService.java`
- Modify: `service/src/main/java/com/clara/insurancequotes/auth/application/service/LoginService.java`
- Modify: `service/src/main/java/com/clara/insurancequotes/auth/adapter/in/web/controller/AuthController.java`
- Modify: `service/src/main/resources/application.yml`
- Modify: `README.md` (Development users and passkeys table)
- Test: `service/src/test/java/com/clara/insurancequotes/auth/application/service/TokenServiceTest.java` (new)
- Modify (test): `service/src/test/java/com/clara/insurancequotes/auth/application/service/DemoUserSeederTest.java`
- Modify (test): `service/src/test/java/com/clara/insurancequotes/auth/application/service/RefreshTokenServiceTest.java`
- Modify (test): `service/src/test/java/com/clara/insurancequotes/auth/application/service/WebAuthnServiceTest.java`

**Interfaces:**
- Produces: `UserRole` enum (`USER`, `ADMIN`); `User.role()` accessor; `User.create(String username, String passwordHash, UserRole role, Instant now)`; `TokenService.issueApiToken(User user)` (replaces the old `issueApiToken(String username)`); JWT claims `uid` (string UUID) and `role` (`USER`/`ADMIN`) on every access token issued via `issueApiToken`.

- [ ] **Step 1: Write the failing test for JWT claims**

Create `service/src/test/java/com/clara/insurancequotes/auth/application/service/TokenServiceTest.java`:

```java
package com.clara.insurancequotes.auth.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.clara.insurancequotes.auth.domain.model.User;
import com.clara.insurancequotes.auth.domain.model.UserRole;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.time.Duration;
import java.time.Instant;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

class TokenServiceTest {

    private static final String SECRET = "test-secret-that-is-32-bytes-long!!";
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final TokenService service = newService();

    private static TokenService newService() {
        var key = new SecretKeySpec(SECRET.getBytes(), HMAC_ALGORITHM);
        var encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
        var decoder = NimbusJwtDecoder.withSecretKey(key)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        return new TokenService(encoder, decoder, Duration.ofMinutes(30));
    }

    @Test
    void issueApiToken_embedsUserIdAndRoleClaims() {
        var user = User.create("demo-admin", "hash", UserRole.ADMIN, Instant.parse("2026-07-28T10:00:00Z"));

        var issued = service.issueApiToken(user);

        var key = new SecretKeySpec(SECRET.getBytes(), HMAC_ALGORITHM);
        var decoder = NimbusJwtDecoder.withSecretKey(key)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        var decoded = decoder.decode(issued.accessToken());

        assertThat(decoded.getClaimAsString("uid")).isEqualTo(user.id().toString());
        assertThat(decoded.getClaimAsString("role")).isEqualTo("ADMIN");
        assertThat(decoded.getClaimAsString("scope")).isEqualTo("api");
        assertThat(decoded.getSubject()).isEqualTo("demo-admin");
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `cd insurance-quotes-service && mvn -pl service test -Dtest=TokenServiceTest`
Expected: compile failure — `User.create` has no 4-arg overload taking `UserRole`, and `TokenService` has no `issueApiToken(User)` method yet.

- [ ] **Step 3: Add the `UserRole` enum**

Create `service/src/main/java/com/clara/insurancequotes/auth/domain/model/UserRole.java`:

```java
package com.clara.insurancequotes.auth.domain.model;

public enum UserRole {
    USER,
    ADMIN
}
```

- [ ] **Step 4: Add the `users.role` migration**

Create `service/src/main/resources/db/migration/V5__add_role_to_users.sql`:

```sql
ALTER TABLE users ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'USER';
```

- [ ] **Step 5: Add `role` to the `User` domain/JPA entity**

Modify `service/src/main/java/com/clara/insurancequotes/auth/domain/model/User.java` — full replacement:

```java
package com.clara.insurancequotes.auth.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected User() {}

    private User(UUID id, String username, String passwordHash, UserRole role, Instant createdAt) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.createdAt = createdAt;
    }

    public static User create(String username, String passwordHash, UserRole role, Instant now) {
        return new User(UUID.randomUUID(), username, passwordHash, role, now);
    }

    public UUID id() {
        return id;
    }

    public String username() {
        return username;
    }

    public String passwordHash() {
        return passwordHash;
    }

    public UserRole role() {
        return role;
    }
}
```

- [ ] **Step 6: Fix the three existing test call sites that construct `User.create(...)` without a role**

In `service/src/test/java/com/clara/insurancequotes/auth/application/service/DemoUserSeederTest.java`, change:

```java
                .thenReturn(Optional.of(User.create("existing", "existing-hash", clock.instant())));
```

to:

```java
                .thenReturn(Optional.of(
                        User.create("existing", "existing-hash", com.clara.insurancequotes.auth.domain.model.UserRole.USER, clock.instant())));
```

(add the import `com.clara.insurancequotes.auth.domain.model.UserRole;` instead of the fully-qualified reference if you prefer — either compiles).

In `service/src/test/java/com/clara/insurancequotes/auth/application/service/RefreshTokenServiceTest.java`, add the import `import com.clara.insurancequotes.auth.domain.model.UserRole;` and change:

```java
    private final User user = User.create("demo", "$2a$10$hash", NOW);
```

to:

```java
    private final User user = User.create("demo", "$2a$10$hash", UserRole.USER, NOW);
```

In `service/src/test/java/com/clara/insurancequotes/auth/application/service/WebAuthnServiceTest.java`, add the import `import com.clara.insurancequotes.auth.domain.model.UserRole;` and change:

```java
    private final User demo = User.create("demo", "$2a$10$hash", Instant.now());
```

to:

```java
    private final User demo = User.create("demo", "$2a$10$hash", UserRole.USER, Instant.now());
```

- [ ] **Step 7: Add `role` to demo user configuration, defaulting to `"USER"`**

Modify `service/src/main/java/com/clara/insurancequotes/auth/configuration/DemoUserProperties.java` — full replacement:

```java
package com.clara.insurancequotes.auth.configuration;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "auth.demo")
public record DemoUserProperties(List<User> users) {

    public DemoUserProperties {
        users = users == null ? List.of() : List.copyOf(users);
    }

    public record User(String username, String password, String role) {

        public User {
            role = role == null || role.isBlank() ? "USER" : role;
        }

        public User(String username, String password) {
            this(username, password, "USER");
        }
    }
}
```

This keeps every existing `new DemoUserProperties.User("demo", "demo-password")` call site (in `DemoUserSeederTest`) compiling unchanged, since the two-arg constructor still exists and defaults to `"USER"`.

- [ ] **Step 8: Pass the resolved role through when seeding demo users**

Modify `service/src/main/java/com/clara/insurancequotes/auth/application/service/DemoUserSeeder.java` — full replacement:

```java
package com.clara.insurancequotes.auth.application.service;

import com.clara.insurancequotes.auth.application.port.out.UserRepository;
import com.clara.insurancequotes.auth.configuration.DemoUserProperties;
import com.clara.insurancequotes.auth.domain.model.User;
import com.clara.insurancequotes.auth.domain.model.UserRole;
import java.time.Clock;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@EnableConfigurationProperties(DemoUserProperties.class)
public class DemoUserSeeder {

    @Bean
    public ApplicationRunner seedDemoUsers(
            UserRepository users, PasswordEncoder passwordEncoder, Clock clock, DemoUserProperties properties) {
        return args -> {
            properties.users().forEach(user -> seedUser(users, passwordEncoder, clock, user));
        };
    }

    private void seedUser(
            UserRepository users, PasswordEncoder passwordEncoder, Clock clock, DemoUserProperties.User user) {
        if (users.findByUsername(user.username()).isEmpty()) {
            users.save(User.create(
                    user.username(),
                    passwordEncoder.encode(user.password()),
                    UserRole.valueOf(user.role()),
                    clock.instant()));
        }
    }
}
```

- [ ] **Step 9: Run `DemoUserSeederTest` to confirm it still passes unchanged**

Run: `cd insurance-quotes-service && mvn -pl service test -Dtest=DemoUserSeederTest`
Expected: PASS (both existing tests, no source changes were needed to the test itself beyond Step 6).

- [ ] **Step 10: Make `TokenService.issueApiToken` take the full `User` and embed `uid`/`role` claims**

Modify `service/src/main/java/com/clara/insurancequotes/auth/application/service/TokenService.java` — full replacement:

```java
package com.clara.insurancequotes.auth.application.service;

import com.clara.insurancequotes.auth.domain.model.User;
import java.time.Duration;
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class TokenService {

    private final JwtEncoder jwtEncoder;
    private final JwtDecoder jwtDecoder;
    private final Duration ttl;

    public TokenService(JwtEncoder jwtEncoder, JwtDecoder jwtDecoder, @Value("${auth.jwt.ttl}") Duration ttl) {
        this.jwtEncoder = jwtEncoder;
        this.jwtDecoder = jwtDecoder;
        this.ttl = ttl;
    }

    public record IssuedAccess(String accessToken, long expiresInSeconds) {}

    public IssuedAccess issueApiToken(User user) {
        return issue(user.username(), "api", ttl, user);
    }

    public String issueMfaToken(String username) {
        return issue(username, "mfa-pending", Duration.ofMinutes(5), null).accessToken();
    }

    public String scopeOf(String token) {
        try {
            return jwtDecoder.decode(token).getClaimAsString("scope");
        } catch (JwtException exception) {
            throw new com.clara.insurancequotes.auth.api.exception.InvalidCredentialsException();
        }
    }

    private IssuedAccess issue(String username, String scope, Duration tokenTtl, User user) {
        var now = Instant.now();
        var claimsBuilder = JwtClaimsSet.builder()
                .subject(username)
                .issuedAt(now)
                .expiresAt(now.plus(tokenTtl))
                .claim("scope", scope);
        if (user != null) {
            claimsBuilder.claim("uid", user.id().toString()).claim("role", user.role().name());
        }
        var header = JwsHeader.with(MacAlgorithm.HS256).build();
        var token = jwtEncoder.encode(JwtEncoderParameters.from(header, claimsBuilder.build()));
        log.debug("Issued JWT for {}", username);
        return new IssuedAccess(token.getTokenValue(), tokenTtl.toSeconds());
    }
}
```

- [ ] **Step 11: Run `TokenServiceTest` to confirm it now passes**

Run: `cd insurance-quotes-service && mvn -pl service test -Dtest=TokenServiceTest`
Expected: PASS.

- [ ] **Step 12: Update the two production call sites that issue an API token**

Modify `service/src/main/java/com/clara/insurancequotes/auth/application/service/LoginService.java` — change the `issuePair` method body from:

```java
    @Transactional
    public TokenPairResponse issuePair(User user) {
        var access = tokenService.issueApiToken(user.username());
        var refresh = refreshTokens.issue(user);
        return new TokenPairResponse(access.accessToken(), refresh, access.expiresInSeconds());
    }
```

to:

```java
    @Transactional
    public TokenPairResponse issuePair(User user) {
        var access = tokenService.issueApiToken(user);
        var refresh = refreshTokens.issue(user);
        return new TokenPairResponse(access.accessToken(), refresh, access.expiresInSeconds());
    }
```

Modify `service/src/main/java/com/clara/insurancequotes/auth/adapter/in/web/controller/AuthController.java` — change the `refresh` method body from:

```java
    @PostMapping("/refresh")
    public TokenPairResponse refresh(@Valid @RequestBody RefreshRequest request) {
        var rotation = refreshTokenService.rotate(request.refreshToken());
        var user = users.findById(rotation.userId()).orElseThrow(InvalidRefreshTokenException::new);
        var access = tokenService.issueApiToken(user.username());
        return new TokenPairResponse(access.accessToken(), rotation.rawToken(), access.expiresInSeconds());
    }
```

to:

```java
    @PostMapping("/refresh")
    public TokenPairResponse refresh(@Valid @RequestBody RefreshRequest request) {
        var rotation = refreshTokenService.rotate(request.refreshToken());
        var user = users.findById(rotation.userId()).orElseThrow(InvalidRefreshTokenException::new);
        var access = tokenService.issueApiToken(user);
        return new TokenPairResponse(access.accessToken(), rotation.rawToken(), access.expiresInSeconds());
    }
```

- [ ] **Step 13: Seed a demo admin account**

Modify `service/src/main/resources/application.yml` — in the `auth.demo.users` list, add a fourth entry after `demo-three`:

```yaml
      - username: ${AUTH_DEMO_USERNAME_3:demo-three}
        password: ${AUTH_DEMO_PASSWORD_3:demo-password-three}
      - username: ${AUTH_DEMO_USERNAME_ADMIN:demo-admin}
        password: ${AUTH_DEMO_PASSWORD_ADMIN:demo-admin-password}
        role: ADMIN
```

- [ ] **Step 14: Document the new demo admin account in the README**

Modify `README.md` — in the "Development users and passkeys" table, add a row after `demo-three`:

```markdown
| demo-three | demo-password-three | Independent manual session |
| demo-admin | demo-admin-password | Read-only oversight across every user's quotes |
```

- [ ] **Step 15: Run the full auth test package**

Run: `cd insurance-quotes-service && mvn -pl service test -Dtest="com.clara.insurancequotes.auth.**"`
Expected: PASS, all auth unit tests green (including the 3 updated in Step 6, `TokenServiceTest`, and every other unmodified auth test).

- [ ] **Step 16: Commit**

```bash
cd insurance-quotes-service
git add service/src/main/resources/db/migration/V5__add_role_to_users.sql \
        service/src/main/java/com/clara/insurancequotes/auth/domain/model/UserRole.java \
        service/src/main/java/com/clara/insurancequotes/auth/domain/model/User.java \
        service/src/main/java/com/clara/insurancequotes/auth/configuration/DemoUserProperties.java \
        service/src/main/java/com/clara/insurancequotes/auth/application/service/DemoUserSeeder.java \
        service/src/main/java/com/clara/insurancequotes/auth/application/service/TokenService.java \
        service/src/main/java/com/clara/insurancequotes/auth/application/service/LoginService.java \
        service/src/main/java/com/clara/insurancequotes/auth/adapter/in/web/controller/AuthController.java \
        service/src/main/resources/application.yml \
        README.md \
        service/src/test/java/com/clara/insurancequotes/auth/application/service/TokenServiceTest.java \
        service/src/test/java/com/clara/insurancequotes/auth/application/service/DemoUserSeederTest.java \
        service/src/test/java/com/clara/insurancequotes/auth/application/service/RefreshTokenServiceTest.java \
        service/src/test/java/com/clara/insurancequotes/auth/application/service/WebAuthnServiceTest.java
git commit -m "feat(auth): add role to users, seed demo admin, carry uid/role JWT claims"
```

---

### Task 2: Owner-scoped quote API — schema, domain, ports, adapters, service, submission flow, controllers

This is one large, unavoidably atomic task: `Quote.createDraft`'s signature change ripples through every layer of the hexagon (`QuoteApi` → `QuoteService` → `QuoteController`; `SubmissionApi` → `SubmissionService`/`SubmissionFinalizer` → `SubmissionController`), and Java requires the whole module to compile before any test in it can run. Splitting it further would mean leaving the module non-compiling between commits, which is worse than one larger reviewable unit. Depends on Task 1 (`uid`/`role` JWT claims).

**Files:**
- Create: `service/src/main/resources/db/migration/V6__add_owner_to_quotes.sql`
- Create: `service/src/main/java/com/clara/insurancequotes/quote/api/usecase/RequestingUser.java`
- Create: `service/src/main/java/com/clara/insurancequotes/quote/application/port/out/StaleQuoteRef.java`
- Modify: `service/src/main/java/com/clara/insurancequotes/quote/domain/model/Quote.java`
- Modify: `service/src/main/java/com/clara/insurancequotes/quote/api/usecase/QuoteApi.java`
- Modify: `service/src/main/java/com/clara/insurancequotes/quote/application/port/out/QuoteRepository.java`
- Modify: `service/src/main/java/com/clara/insurancequotes/quote/adapter/out/persistence/SpringDataQuoteRepository.java`
- Modify: `service/src/main/java/com/clara/insurancequotes/quote/adapter/out/persistence/JpaQuoteRepository.java`
- Modify: `service/src/main/java/com/clara/insurancequotes/quote/application/service/QuoteService.java`
- Modify: `service/src/main/java/com/clara/insurancequotes/quote/application/service/DraftExpirationJob.java`
- Modify: `service/src/main/java/com/clara/insurancequotes/quote/domain/event/QuoteExpired.java`
- Modify: `service/src/main/java/com/clara/insurancequotes/quote/adapter/in/messaging/consumer/QuoteCacheEvictionListener.java`
- Modify: `service/src/main/java/com/clara/insurancequotes/quote/adapter/in/web/controller/QuoteController.java`
- Modify: `service/src/main/java/com/clara/insurancequotes/submission/api/usecase/SubmissionApi.java`
- Modify: `service/src/main/java/com/clara/insurancequotes/submission/application/service/SubmissionService.java`
- Modify: `service/src/main/java/com/clara/insurancequotes/submission/application/service/SubmissionFinalizer.java`
- Modify: `service/src/main/java/com/clara/insurancequotes/submission/adapter/in/web/controller/SubmissionController.java`
- Modify: `service/src/testFixtures/java/com/clara/insurancequotes/quote/domain/model/QuoteMother.java`
- Modify: `service/src/testFixtures/java/com/clara/insurancequotes/testsupport/InMemoryQuoteRepository.java`
- Modify (test): `service/src/test/java/com/clara/insurancequotes/quote/application/service/QuoteServiceTest.java`
- Modify (test): `service/src/test/java/com/clara/insurancequotes/quote/application/service/DraftExpirationJobTest.java`
- Modify (test): `service/src/test/java/com/clara/insurancequotes/quote/adapter/in/web/controller/QuoteControllerTest.java`
- Modify (test): `service/src/test/java/com/clara/insurancequotes/submission/application/service/SubmissionServiceTest.java`
- Modify (test): `service/src/test/java/com/clara/insurancequotes/submission/application/service/SubmissionFinalizerTest.java`
- Modify (test): `service/src/integrationTest/java/com/clara/insurancequotes/quote/adapter/out/persistence/QuoteRepositoryIT.java`
- Modify (test): `service/src/integrationTest/java/com/clara/insurancequotes/quote/application/service/QuoteCachingIT.java`
- Modify (test): `service/src/integrationTest/java/com/clara/insurancequotes/quote/application/service/DraftExpirationJobIT.java`
- Modify (test): `service/src/integrationTest/java/com/clara/insurancequotes/submission/SubmissionFlowIT.java`

**Interfaces:**
- Consumes: `TokenService.issueApiToken(User user)`, JWT claims `uid`/`role` (Task 1).
- Produces: `RequestingUser(UUID id, boolean admin)`; `QuoteApi` with the split signature described in the Architecture section; `SubmissionApi.submit(UUID quoteId, UUID ownerId)`; `StaleQuoteRef(UUID id, UUID ownerId)` and `QuoteRepository.findStaleDrafts(Instant cutoff)` replacing `findIdsToExpire`; `QuoteExpired(UUID quoteId, UUID ownerId)` replacing the single-arg event (Steps 25–35 — this closes a cache-eviction regression the ownership change would otherwise introduce for `DraftExpirationJob`).

- [ ] **Step 1: Write the failing repository-level ownership test**

Modify `service/src/integrationTest/java/com/clara/insurancequotes/quote/adapter/out/persistence/QuoteRepositoryIT.java` — full replacement:

```java
package com.clara.insurancequotes.quote.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.clara.insurancequotes.quote.api.query.QuoteQuery;
import com.clara.insurancequotes.quote.domain.model.QuoteMother;
import com.clara.insurancequotes.quote.domain.model.QuoteStatus;
import com.clara.insurancequotes.testsupport.Containers;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaQuoteRepository.class)
class QuoteRepositoryIT {

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        Containers.registerPostgres(registry);
        Containers.registerRedis(registry);
    }

    @Autowired
    private JpaQuoteRepository repository;

    @Test
    void savesAndReloadsAggregateWithHealthProfile() {
        var saved = repository.save(QuoteMother.submittableSeniorDraft());

        var reloaded = repository.findById(saved.id(), null).orElseThrow();

        assertThat(reloaded.healthProfile().conditions()).isNotEmpty();
        assertThat(reloaded.monthlyPremium()).isEqualByComparingTo("327.60");
        assertThat(reloaded.userId()).isEqualTo(QuoteMother.OWNER_ID);
    }

    @Test
    void markExpired_batchUpdatesOnlyGivenIds() {
        var stale = repository.save(QuoteMother.draft());
        var fresh = repository.save(QuoteMother.submittableDraft());

        var cutoff = QuoteMother.FIXED_NOW.plus(Duration.ofMinutes(31));
        var ids = repository.findIdsToExpire(cutoff);
        var updated = repository.markExpired(ids, cutoff);

        assertThat(updated).isEqualTo(2);
        assertThat(repository.findById(stale.id(), null).orElseThrow().status()).isEqualTo(QuoteStatus.EXPIRED);
        assertThat(repository.findById(fresh.id(), null).orElseThrow().status()).isEqualTo(QuoteStatus.EXPIRED);
    }

    @Test
    void findById_scopedToOtherOwner_returnsEmpty() {
        var saved = repository.save(QuoteMother.draft());

        var asOwner = repository.findById(saved.id(), QuoteMother.OWNER_ID);
        var asOtherUser = repository.findById(saved.id(), UUID.randomUUID());

        assertThat(asOwner).isPresent();
        assertThat(asOtherUser).isEmpty();
    }

    @Test
    void findPage_scopedToOwner_excludesOtherUsersQuotes() {
        var ownerA = UUID.randomUUID();
        var ownerB = UUID.randomUUID();
        repository.save(QuoteMother.draftForOwner(ownerA));
        repository.save(QuoteMother.draftForOwner(ownerB));

        var page = repository.findPage(QuoteQuery.defaults(), ownerA);

        assertThat(page.content()).hasSize(1);
        assertThat(page.content().get(0).userId()).isEqualTo(ownerA);
    }
}
```

- [ ] **Step 2: Run it to verify it fails**

Run: `cd insurance-quotes-service && mvn -pl service verify -Dtest=QuoteRepositoryIT -DskipUnitTests=true` (or your project's usual Testcontainers-backed integration test command)
Expected: compile failure — `Quote` has no `userId()`, `QuoteRepository.findById` still takes one argument, `QuoteMother` has no `OWNER_ID` or `draftForOwner`.

- [ ] **Step 3: Add the `quotes.user_id` migration**

Create `service/src/main/resources/db/migration/V6__add_owner_to_quotes.sql`:

```sql
ALTER TABLE quotes ADD COLUMN user_id UUID NOT NULL REFERENCES users(id);
CREATE INDEX idx_quotes_user_id ON quotes (user_id);
```

- [ ] **Step 4: Add `userId` to the `Quote` domain/JPA entity**

Modify `service/src/main/java/com/clara/insurancequotes/quote/domain/model/Quote.java` — full replacement:

```java
package com.clara.insurancequotes.quote.domain.model;

import com.clara.insurancequotes.pricing.api.type.CoverageType;
import com.clara.insurancequotes.quote.domain.exception.IncompleteQuoteException;
import com.clara.insurancequotes.quote.domain.exception.InvalidStateTransitionException;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "quotes")
public class Quote {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private int age;

    @Column(name = "zip_code", nullable = false)
    private String zipCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "coverage_type")
    private CoverageType coverageType;

    @Embedded
    private HealthProfile healthProfile;

    @Column(name = "monthly_premium")
    private BigDecimal monthlyPremium;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QuoteStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    protected Quote() {}

    private Quote(UUID id, UUID userId, String name, String email, int age, String zipCode, Instant now) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.age = age;
        this.zipCode = zipCode;
        this.status = QuoteStatus.DRAFT;
        this.healthProfile = HealthProfile.none();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public static Quote createDraft(UUID userId, String name, String email, int age, String zipCode, Instant now) {
        return new Quote(UUID.randomUUID(), userId, name, email, age, zipCode, now);
    }

    public void updateCoverage(
            CoverageType coverageType, HealthProfile healthProfile, BigDecimal premium, Instant now) {
        if (!status.allowsCoverageUpdate()) {
            throw new InvalidStateTransitionException(id, status.name(), "COVERAGE_UPDATE");
        }
        this.coverageType = coverageType;
        this.healthProfile = healthProfile;
        this.monthlyPremium = premium;
        this.updatedAt = now;
    }

    public void ensureSubmittable() {
        if (!status.allowsSubmission()) {
            throw new InvalidStateTransitionException(id, status.name(), "SUBMIT");
        }
        if (coverageType == null) {
            throw new IncompleteQuoteException(id, "coverage type not selected");
        }
    }

    public void markSubmitted(Instant now) {
        ensureSubmittable();
        this.status = QuoteStatus.SUBMITTED;
        this.updatedAt = now;
    }

    public void markSubmissionFailed(Instant now) {
        if (!status.allowsSubmission()) {
            throw new InvalidStateTransitionException(id, status.name(), "SUBMISSION_FAILURE");
        }
        this.status = QuoteStatus.SUBMISSION_FAILED;
        this.updatedAt = now;
    }

    public void expire(Instant now) {
        if (!status.allowsExpiration()) {
            throw new InvalidStateTransitionException(id, status.name(), "EXPIRE");
        }
        this.status = QuoteStatus.EXPIRED;
        this.updatedAt = now;
    }

    public UUID id() {
        return id;
    }

    public UUID userId() {
        return userId;
    }

    public String name() {
        return name;
    }

    public String email() {
        return email;
    }

    public int age() {
        return age;
    }

    public String zipCode() {
        return zipCode;
    }

    public CoverageType coverageType() {
        return coverageType;
    }

    public HealthProfile healthProfile() {
        return healthProfile;
    }

    public BigDecimal monthlyPremium() {
        return monthlyPremium;
    }

    public QuoteStatus status() {
        return status;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}
```

- [ ] **Step 5: Update `QuoteMother` to supply owners**

Modify `service/src/testFixtures/java/com/clara/insurancequotes/quote/domain/model/QuoteMother.java` — full replacement:

```java
package com.clara.insurancequotes.quote.domain.model;

import com.clara.insurancequotes.pricing.api.type.CoverageType;
import com.clara.insurancequotes.quote.api.type.HealthCondition;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public final class QuoteMother {

    public static final Instant FIXED_NOW = Instant.parse("2026-07-22T10:00:00Z");
    public static final UUID OWNER_ID = UUID.fromString("a1111111-0000-0000-0000-000000000001");

    private QuoteMother() {}

    public static Quote draft() {
        return draftForOwner(OWNER_ID);
    }

    public static Quote draftForOwner(UUID ownerId) {
        return Quote.createDraft(ownerId, "Jane Roe", "jane@example.com", 34, "06600", FIXED_NOW);
    }

    public static Quote seniorDraft() {
        return Quote.createDraft(OWNER_ID, "John Elder", "john@example.com", 70, "06600", FIXED_NOW);
    }

    public static Quote submittableDraft() {
        var quote = draft();
        quote.updateCoverage(CoverageType.STANDARD, HealthProfile.none(), new BigDecimal("100.00"), FIXED_NOW);
        return quote;
    }

    public static Quote submittableSeniorDraft() {
        var quote = seniorDraft();
        var health = new HealthProfile(true, Set.of(HealthCondition.DIABETES), false, true, true);
        quote.updateCoverage(CoverageType.STANDARD, health, new BigDecimal("327.60"), FIXED_NOW);
        return quote;
    }
}
```

- [ ] **Step 6: Run `QuoteTest` (pure domain unit test) to confirm it still passes unchanged**

Run: `cd insurance-quotes-service && mvn -pl service test -Dtest=QuoteTest`
Expected: PASS — `QuoteTest` only calls `QuoteMother` factory methods and never references `createDraft` or `userId` directly, so it needs no source changes.

- [ ] **Step 7: Add `RequestingUser`**

Create `service/src/main/java/com/clara/insurancequotes/quote/api/usecase/RequestingUser.java`:

```java
package com.clara.insurancequotes.quote.api.usecase;

import java.util.UUID;

public record RequestingUser(UUID id, boolean admin) {}
```

- [ ] **Step 8: Change the `QuoteApi` contract**

Modify `service/src/main/java/com/clara/insurancequotes/quote/api/usecase/QuoteApi.java` — full replacement:

```java
package com.clara.insurancequotes.quote.api.usecase;

import com.clara.insurancequotes.quote.api.command.CreateQuoteCommand;
import com.clara.insurancequotes.quote.api.command.UpdateCoverageCommand;
import com.clara.insurancequotes.quote.api.query.QuoteQuery;
import com.clara.insurancequotes.quote.api.result.QuotePageView;
import com.clara.insurancequotes.quote.api.result.QuoteSummaryView;
import com.clara.insurancequotes.quote.api.result.QuoteView;
import java.util.UUID;

public interface QuoteApi {

    QuoteView create(CreateQuoteCommand command, UUID ownerId);

    QuoteView updateCoverage(UUID id, UpdateCoverageCommand command, UUID ownerId);

    QuoteView getQuote(UUID id, RequestingUser requester);

    QuotePageView listQuotes(QuoteQuery query, RequestingUser requester);

    QuoteSummaryView getSummary(RequestingUser requester);

    QuoteView getOwnedQuote(UUID id, UUID ownerId);

    QuoteView ensureSubmittable(UUID id, UUID ownerId);

    QuoteView markSubmitted(UUID id, UUID ownerId);

    QuoteView markSubmissionFailed(UUID id, UUID ownerId);
}
```

- [ ] **Step 9: Change the `QuoteRepository` port**

Modify `service/src/main/java/com/clara/insurancequotes/quote/application/port/out/QuoteRepository.java` — full replacement:

```java
package com.clara.insurancequotes.quote.application.port.out;

import com.clara.insurancequotes.quote.api.query.QuoteQuery;
import com.clara.insurancequotes.quote.domain.model.Quote;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QuoteRepository {

    Quote save(Quote quote);

    /** {@code ownerId == null} means unscoped (admin); non-null scopes to that owner. */
    Optional<Quote> findById(UUID id, UUID ownerId);

    QuoteSearchResult findPage(QuoteQuery query, UUID ownerId);

    QuoteSummaryData findSummary(Instant now, UUID ownerId);

    List<UUID> findIdsToExpire(Instant cutoff);

    int markExpired(List<UUID> ids, Instant now);
}
```

- [ ] **Step 10: Add owner-scoped derived queries to `SpringDataQuoteRepository`**

Modify `service/src/main/java/com/clara/insurancequotes/quote/adapter/out/persistence/SpringDataQuoteRepository.java` — full replacement:

```java
package com.clara.insurancequotes.quote.adapter.out.persistence;

import com.clara.insurancequotes.pricing.api.type.CoverageType;
import com.clara.insurancequotes.quote.domain.model.Quote;
import com.clara.insurancequotes.quote.domain.model.QuoteStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface SpringDataQuoteRepository extends JpaRepository<Quote, UUID>, JpaSpecificationExecutor<Quote> {

    Optional<Quote> findByIdAndUserId(UUID id, UUID userId);

    long countByStatus(QuoteStatus status);

    long countByStatusAndUserId(QuoteStatus status, UUID userId);

    long countByCoverageType(CoverageType coverageType);

    long countByCoverageTypeAndUserId(CoverageType coverageType, UUID userId);

    long countByUserId(UUID userId);

    long countByMonthlyPremiumIsNotNull();

    long countByMonthlyPremiumIsNotNullAndUserId(UUID userId);

    @Query("select coalesce(sum(q.monthlyPremium), 0) from Quote q")
    BigDecimal sumMonthlyPremium();

    @Query("select coalesce(sum(q.monthlyPremium), 0) from Quote q where q.userId = :userId")
    BigDecimal sumMonthlyPremiumForUser(@Param("userId") UUID userId);

    @Query("select coalesce(avg(q.monthlyPremium), 0) from Quote q")
    BigDecimal averageMonthlyPremium();

    @Query("select coalesce(avg(q.monthlyPremium), 0) from Quote q where q.userId = :userId")
    BigDecimal averageMonthlyPremiumForUser(@Param("userId") UUID userId);

    @Query("select q.createdAt, q.updatedAt, q.status from Quote q "
            + "where (q.createdAt >= :trendStart and q.createdAt <= :now) "
            + "or (q.updatedAt >= :trendStart and q.updatedAt <= :now)")
    List<Object[]> findTrendRows(@Param("trendStart") Instant trendStart, @Param("now") Instant now);

    @Query("select q.createdAt, q.updatedAt, q.status from Quote q "
            + "where ((q.createdAt >= :trendStart and q.createdAt <= :now) "
            + "or (q.updatedAt >= :trendStart and q.updatedAt <= :now)) and q.userId = :userId")
    List<Object[]> findTrendRowsForUser(
            @Param("trendStart") Instant trendStart, @Param("now") Instant now, @Param("userId") UUID userId);

    @Query("select q.id from Quote q where q.status = :status and q.createdAt < :cutoff")
    List<UUID> findIdsToExpire(@Param("status") QuoteStatus status, @Param("cutoff") Instant cutoff);

    default List<UUID> findIdsToExpire(Instant cutoff) {
        return findIdsToExpire(QuoteStatus.DRAFT, cutoff);
    }

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Quote q set q.status = :status, q.updatedAt = :now where q.id in :ids")
    int markExpired(@Param("status") QuoteStatus status, @Param("ids") List<UUID> ids, @Param("now") Instant now);

    default int markExpired(List<UUID> ids, Instant now) {
        return markExpired(QuoteStatus.EXPIRED, ids, now);
    }
}
```

- [ ] **Step 11: Implement owner-scoping in `JpaQuoteRepository`**

Modify `service/src/main/java/com/clara/insurancequotes/quote/adapter/out/persistence/JpaQuoteRepository.java` — full replacement:

```java
package com.clara.insurancequotes.quote.adapter.out.persistence;

import com.clara.insurancequotes.pricing.api.type.CoverageType;
import com.clara.insurancequotes.quote.api.query.QuoteQuery;
import com.clara.insurancequotes.quote.api.query.SortDirection;
import com.clara.insurancequotes.quote.application.port.out.QuoteRepository;
import com.clara.insurancequotes.quote.application.port.out.QuoteSearchResult;
import com.clara.insurancequotes.quote.application.port.out.QuoteSummaryData;
import com.clara.insurancequotes.quote.domain.model.Quote;
import com.clara.insurancequotes.quote.domain.model.QuoteStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
@Slf4j
public class JpaQuoteRepository implements QuoteRepository {

    private final SpringDataQuoteRepository delegate;

    @Override
    public Quote save(Quote quote) {
        return delegate.save(quote);
    }

    @Override
    public Optional<Quote> findById(UUID id, UUID ownerId) {
        return ownerId == null ? delegate.findById(id) : delegate.findByIdAndUserId(id, ownerId);
    }

    @Override
    public QuoteSearchResult findPage(QuoteQuery query, UUID ownerId) {
        Specification<Quote> specification = (root, criteriaQuery, criteriaBuilder) -> criteriaBuilder.conjunction();
        if (ownerId != null) {
            specification = specification.and((root, criteriaQuery, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("userId"), ownerId));
        }
        if (query.status() != null) {
            specification = specification.and((root, criteriaQuery, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("status"), query.status()));
        }
        if (query.coverage() != null) {
            specification = specification.and((root, criteriaQuery, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("coverageType"), query.coverage()));
        }
        if (query.search() != null) {
            var pattern = "%" + query.search().toLowerCase(Locale.ROOT) + "%";
            specification = specification.and((root, criteriaQuery, criteriaBuilder) -> criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), pattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("email")), pattern)));
        }
        var direction = query.direction() == SortDirection.ASC ? Sort.Direction.ASC : Sort.Direction.DESC;
        var sort = Sort.by(direction, query.sortBy().property()).and(Sort.by(Sort.Direction.ASC, "id"));
        var page = delegate.findAll(specification, PageRequest.of(query.page(), query.size(), sort));
        return new QuoteSearchResult(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements());
    }

    @Override
    public QuoteSummaryData findSummary(Instant now, UUID ownerId) {
        var endDate = now.atZone(ZoneOffset.UTC).toLocalDate();
        var startDate = endDate.minusDays(6);
        var statusCounts = new EnumMap<QuoteStatus, Long>(QuoteStatus.class);
        for (var status : QuoteStatus.values()) {
            statusCounts.put(
                    status,
                    ownerId == null
                            ? delegate.countByStatus(status)
                            : delegate.countByStatusAndUserId(status, ownerId));
        }
        var coverageCounts = new EnumMap<CoverageType, Long>(CoverageType.class);
        for (var coverage : CoverageType.values()) {
            coverageCounts.put(
                    coverage,
                    ownerId == null
                            ? delegate.countByCoverageType(coverage)
                            : delegate.countByCoverageTypeAndUserId(coverage, ownerId));
        }
        var trendBuckets = new LinkedHashMap<LocalDate, long[]>();
        for (var date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            trendBuckets.put(date, new long[3]);
        }
        var trendStart = startDate.atStartOfDay(ZoneOffset.UTC).toInstant();
        var trendRows = ownerId == null
                ? delegate.findTrendRows(trendStart, now)
                : delegate.findTrendRowsForUser(trendStart, now, ownerId);
        for (var row : trendRows) {
            var createdAt = (Instant) row[0];
            var updatedAt = (Instant) row[1];
            var status = (QuoteStatus) row[2];
            var createdDate = createdAt.atZone(ZoneOffset.UTC).toLocalDate();
            var createdBucket = trendBuckets.get(createdDate);
            if (createdBucket != null) {
                createdBucket[0]++;
            }
            var updatedDate = updatedAt.atZone(ZoneOffset.UTC).toLocalDate();
            var updatedBucket = trendBuckets.get(updatedDate);
            if (updatedBucket != null && status == QuoteStatus.SUBMITTED) {
                updatedBucket[1]++;
            } else if (updatedBucket != null && status == QuoteStatus.SUBMISSION_FAILED) {
                updatedBucket[2]++;
            }
        }
        var trend = new ArrayList<QuoteSummaryData.TrendPoint>();
        trendBuckets.forEach(
                (date, values) -> trend.add(new QuoteSummaryData.TrendPoint(date, values[0], values[1], values[2])));
        return new QuoteSummaryData(
                ownerId == null ? delegate.count() : delegate.countByUserId(ownerId),
                statusCounts,
                coverageCounts,
                ownerId == null
                        ? delegate.countByMonthlyPremiumIsNotNull()
                        : delegate.countByMonthlyPremiumIsNotNullAndUserId(ownerId),
                ownerId == null ? delegate.sumMonthlyPremium() : delegate.sumMonthlyPremiumForUser(ownerId),
                ownerId == null ? delegate.averageMonthlyPremium() : delegate.averageMonthlyPremiumForUser(ownerId),
                trend);
    }

    @Override
    public List<UUID> findIdsToExpire(Instant cutoff) {
        return delegate.findIdsToExpire(cutoff);
    }

    @Override
    public int markExpired(List<UUID> ids, Instant now) {
        var updated = delegate.markExpired(ids, now);
        log.debug("Marked {} quotes as expired", updated);
        return updated;
    }
}
```

- [ ] **Step 12: Update `InMemoryQuoteRepository` to match the new port**

Modify `service/src/testFixtures/java/com/clara/insurancequotes/testsupport/InMemoryQuoteRepository.java` — replace the `findById`, `findPage`, and `findSummary` methods (keep everything else, including `save`, `findIdsToExpire`, `markExpired`, `containsSearch`, `comparatorFor`, unchanged):

```java
    @Override
    public Optional<Quote> findById(UUID id, UUID ownerId) {
        return Optional.ofNullable(store.get(id))
                .filter(quote -> ownerId == null || quote.userId().equals(ownerId));
    }

    @Override
    public QuoteSearchResult findPage(QuoteQuery query, UUID ownerId) {
        var filtered = store.values().stream()
                .filter(quote -> ownerId == null || quote.userId().equals(ownerId))
                .filter(quote -> query.status() == null || quote.status() == query.status())
                .filter(quote -> query.coverage() == null || quote.coverageType() == query.coverage())
                .filter(quote -> query.search() == null || containsSearch(quote, query.search()))
                .sorted(comparatorFor(query))
                .toList();
        var from = Math.min(query.page() * query.size(), filtered.size());
        var to = Math.min(from + query.size(), filtered.size());
        return new QuoteSearchResult(filtered.subList(from, to), query.page(), query.size(), filtered.size());
    }

    @Override
    public QuoteSummaryData findSummary(Instant now, UUID ownerId) {
        var scoped = store.values().stream()
                .filter(quote -> ownerId == null || quote.userId().equals(ownerId))
                .toList();
        var statusCounts = new EnumMap<QuoteStatus, Long>(QuoteStatus.class);
        for (var status : QuoteStatus.values()) {
            statusCounts.put(status, scoped.stream().filter(quote -> quote.status() == status).count());
        }
        var coverageCounts = new EnumMap<CoverageType, Long>(CoverageType.class);
        for (var coverage : CoverageType.values()) {
            coverageCounts.put(
                    coverage, scoped.stream().filter(quote -> quote.coverageType() == coverage).count());
        }
        var pricedQuotes =
                scoped.stream().filter(quote -> quote.monthlyPremium() != null).count();
        var totalPremium = scoped.stream()
                .map(Quote::monthlyPremium)
                .filter(value -> value != null)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        var averagePremium = pricedQuotes == 0
                ? java.math.BigDecimal.ZERO
                : totalPremium.divide(java.math.BigDecimal.valueOf(pricedQuotes), 2, java.math.RoundingMode.HALF_UP);

        var endDate = now.atZone(ZoneOffset.UTC).toLocalDate();
        var startDate = endDate.minusDays(6);
        var trendBuckets = new LinkedHashMap<LocalDate, long[]>();
        for (var date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            trendBuckets.put(date, new long[3]);
        }
        scoped.forEach(quote -> {
            var createdBucket =
                    trendBuckets.get(quote.createdAt().atZone(ZoneOffset.UTC).toLocalDate());
            if (createdBucket != null) {
                createdBucket[0]++;
            }
            var updatedBucket =
                    trendBuckets.get(quote.updatedAt().atZone(ZoneOffset.UTC).toLocalDate());
            if (updatedBucket != null && quote.status() == QuoteStatus.SUBMITTED) {
                updatedBucket[1]++;
            } else if (updatedBucket != null && quote.status() == QuoteStatus.SUBMISSION_FAILED) {
                updatedBucket[2]++;
            }
        });
        var trend = new ArrayList<QuoteSummaryData.TrendPoint>();
        trendBuckets.forEach(
                (date, values) -> trend.add(new QuoteSummaryData.TrendPoint(date, values[0], values[1], values[2])));
        return new QuoteSummaryData(
                scoped.size(), statusCounts, coverageCounts, pricedQuotes, totalPremium, averagePremium, trend);
    }
```

- [ ] **Step 13: Thread ownership and admin-awareness through `QuoteService`**

Modify `service/src/main/java/com/clara/insurancequotes/quote/application/service/QuoteService.java` — full replacement:

```java
package com.clara.insurancequotes.quote.application.service;

import com.clara.insurancequotes.config.BusinessMetrics;
import com.clara.insurancequotes.pricing.api.command.PricingInput;
import com.clara.insurancequotes.pricing.api.type.CoverageType;
import com.clara.insurancequotes.pricing.api.usecase.PremiumCalculator;
import com.clara.insurancequotes.quote.api.command.CreateQuoteCommand;
import com.clara.insurancequotes.quote.api.command.UpdateCoverageCommand;
import com.clara.insurancequotes.quote.api.query.QuoteQuery;
import com.clara.insurancequotes.quote.api.result.QuoteDistributionView;
import com.clara.insurancequotes.quote.api.result.QuotePageView;
import com.clara.insurancequotes.quote.api.result.QuoteSummaryView;
import com.clara.insurancequotes.quote.api.result.QuoteTrendPointView;
import com.clara.insurancequotes.quote.api.result.QuoteView;
import com.clara.insurancequotes.quote.api.usecase.QuoteApi;
import com.clara.insurancequotes.quote.api.usecase.RequestingUser;
import com.clara.insurancequotes.quote.application.exception.QuoteNotFoundException;
import com.clara.insurancequotes.quote.application.port.out.QuoteRepository;
import com.clara.insurancequotes.quote.configuration.CacheConfig;
import com.clara.insurancequotes.quote.domain.exception.HealthDataNotAllowedException;
import com.clara.insurancequotes.quote.domain.model.HealthProfile;
import com.clara.insurancequotes.quote.domain.model.Quote;
import com.clara.insurancequotes.quote.domain.model.QuoteStatus;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.util.Arrays;
import java.util.UUID;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuoteService implements QuoteApi {

    private static final int HEALTH_DATA_AGE_THRESHOLD = 65;

    private final QuoteRepository repository;
    private final PremiumCalculator premiumCalculator;
    private final Clock clock;
    private final BusinessMetrics metrics;

    @Override
    @Transactional
    public QuoteView create(CreateQuoteCommand command, UUID ownerId) {
        var quote = Quote.createDraft(
                ownerId, command.name(), command.email(), command.age(), command.zipCode(), clock.instant());
        var saved = repository.save(quote);
        metrics.quoteCreated();
        log.debug("Created quote {}", saved.id());
        return QuoteView.from(saved);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = CacheConfig.QUOTES_CACHE, key = "#id + '|' + #ownerId", beforeInvocation = true)
    public QuoteView updateCoverage(UUID id, UpdateCoverageCommand command, UUID ownerId) {
        try {
            var quote = load(id, ownerId);
            rejectHealthDataForNonSeniors(quote, command);
            var premium =
                    metrics.timePremiumCalculation(() -> premiumCalculator.calculate(pricingInputOf(quote, command)));
            quote.updateCoverage(command.coverageType(), healthProfileOf(command), premium.monthly(), clock.instant());
            var view = QuoteView.from(repository.save(quote));
            metrics.coverageUpdated("success", command.coverageType().name());
            return view;
        } catch (HealthDataNotAllowedException exception) {
            metrics.coverageUpdated("rejected", command.coverageType().name());
            throw exception;
        } catch (RuntimeException exception) {
            metrics.coverageUpdated("failed", command.coverageType().name());
            throw exception;
        }
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheConfig.QUOTES_CACHE, key = "#id + '|' + #requester.id()")
    public QuoteView getQuote(UUID id, RequestingUser requester) {
        return QuoteView.from(load(id, requester.admin() ? null : requester.id()));
    }

    @Override
    @Transactional(readOnly = true)
    public QuotePageView listQuotes(QuoteQuery query, RequestingUser requester) {
        var result = repository.findPage(query, requester.admin() ? null : requester.id());
        return new QuotePageView(
                result.content().stream().map(QuoteView::from).toList(),
                result.page(),
                result.size(),
                result.totalElements(),
                result.totalPages(),
                result.hasNext(),
                result.hasPrevious());
    }

    @Override
    @Transactional(readOnly = true)
    public QuoteSummaryView getSummary(RequestingUser requester) {
        var data = repository.findSummary(clock.instant(), requester.admin() ? null : requester.id());
        var submitted = data.statusCounts().getOrDefault(QuoteStatus.SUBMITTED, 0L);
        var failed = data.statusCounts().getOrDefault(QuoteStatus.SUBMISSION_FAILED, 0L);
        var attempts = submitted + failed;
        var submissionRate = attempts == 0
                ? BigDecimal.ZERO.setScale(2)
                : BigDecimal.valueOf(submitted)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(attempts), 2, RoundingMode.HALF_UP);
        var statusDistribution = Arrays.stream(QuoteStatus.values())
                .map(status -> new QuoteDistributionView(
                        status.name(), data.statusCounts().getOrDefault(status, 0L)))
                .toList();
        var coverageDistribution = Arrays.stream(CoverageType.values())
                .map(coverage -> new QuoteDistributionView(
                        coverage.name(), data.coverageCounts().getOrDefault(coverage, 0L)))
                .toList();
        var trend = data.trend().stream()
                .map(point -> new QuoteTrendPointView(point.date(), point.created(), point.submitted(), point.failed()))
                .toList();
        return new QuoteSummaryView(
                data.totalQuotes(),
                data.statusCounts().getOrDefault(QuoteStatus.DRAFT, 0L),
                submitted,
                failed,
                data.statusCounts().getOrDefault(QuoteStatus.EXPIRED, 0L),
                data.pricedQuotes(),
                data.totalMonthlyPremium(),
                data.averageMonthlyPremium(),
                submissionRate,
                statusDistribution,
                coverageDistribution,
                trend);
    }

    @Override
    @Transactional(readOnly = true)
    public QuoteView getOwnedQuote(UUID id, UUID ownerId) {
        return QuoteView.from(load(id, ownerId));
    }

    @Override
    @Transactional(readOnly = true)
    public QuoteView ensureSubmittable(UUID id, UUID ownerId) {
        var quote = load(id, ownerId);
        quote.ensureSubmittable();
        return QuoteView.from(quote);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = CacheConfig.QUOTES_CACHE, key = "#id + '|' + #ownerId", beforeInvocation = true)
    public QuoteView markSubmitted(UUID id, UUID ownerId) {
        return transition(id, ownerId, quote -> quote.markSubmitted(clock.instant()));
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = CacheConfig.QUOTES_CACHE, key = "#id + '|' + #ownerId", beforeInvocation = true)
    public QuoteView markSubmissionFailed(UUID id, UUID ownerId) {
        return transition(id, ownerId, quote -> quote.markSubmissionFailed(clock.instant()));
    }

    private QuoteView transition(UUID id, UUID ownerId, Consumer<Quote> change) {
        var quote = load(id, ownerId);
        change.accept(quote);
        return QuoteView.from(repository.save(quote));
    }

    private Quote load(UUID id, UUID ownerId) {
        return repository.findById(id, ownerId).orElseThrow(() -> new QuoteNotFoundException(id));
    }

    private static void rejectHealthDataForNonSeniors(Quote quote, UpdateCoverageCommand command) {
        if (quote.age() <= HEALTH_DATA_AGE_THRESHOLD && command.carriesHealthData()) {
            throw new HealthDataNotAllowedException(quote.age());
        }
    }

    private static HealthProfile healthProfileOf(UpdateCoverageCommand command) {
        if (!command.carriesHealthData()) {
            return HealthProfile.none();
        }
        return new HealthProfile(
                command.hasPreexistingConditions(),
                command.conditions(),
                command.takesPrescriptionMedication(),
                command.usesTobacco(),
                command.needsSpouseCoverage());
    }

    private static PricingInput pricingInputOf(Quote quote, UpdateCoverageCommand command) {
        return new PricingInput(
                command.coverageType(),
                quote.age(),
                Boolean.TRUE.equals(command.hasPreexistingConditions()),
                Boolean.TRUE.equals(command.usesTobacco()),
                Boolean.TRUE.equals(command.needsSpouseCoverage()));
    }
}
```

- [ ] **Step 14: Update `SubmissionApi`**

Modify `service/src/main/java/com/clara/insurancequotes/submission/api/usecase/SubmissionApi.java` — full replacement:

```java
package com.clara.insurancequotes.submission.api.usecase;

import com.clara.insurancequotes.quote.api.result.QuoteView;
import java.util.UUID;

public interface SubmissionApi {

    QuoteView submit(UUID quoteId, UUID ownerId);
}
```

- [ ] **Step 15: Update `SubmissionService` to use the always-owner-scoped path**

Modify `service/src/main/java/com/clara/insurancequotes/submission/application/service/SubmissionService.java` — full replacement:

```java
package com.clara.insurancequotes.submission.application.service;

import com.clara.insurancequotes.config.BusinessMetrics;
import com.clara.insurancequotes.quote.api.result.QuoteView;
import com.clara.insurancequotes.quote.api.usecase.QuoteApi;
import com.clara.insurancequotes.submission.api.exception.InsurerUnavailableException;
import com.clara.insurancequotes.submission.api.usecase.SubmissionApi;
import com.clara.insurancequotes.submission.application.port.out.InsurerGateway;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** Orchestrates the external insurer call without holding a database transaction open. */
@Service
@RequiredArgsConstructor
@Slf4j
public class SubmissionService implements SubmissionApi {

    private final QuoteApi quoteApi;
    private final InsurerGateway insurerGateway;
    private final SubmissionFinalizer finalizer;
    private final BusinessMetrics metrics;

    @Override
    public QuoteView submit(UUID quoteId, UUID ownerId) {
        var current = quoteApi.getOwnedQuote(quoteId, ownerId);
        if (current.status().alreadySubmitted()) {
            log.debug("Ignoring duplicate submission for quote {}", quoteId);
            return current;
        }
        quoteApi.ensureSubmittable(quoteId, ownerId);
        callInsurerRecordingFailure(quoteId, ownerId);
        var completed = finalizer.completeSubmission(quoteId, ownerId);
        metrics.submissionSucceeded();
        return completed;
    }

    private void callInsurerRecordingFailure(UUID quoteId, UUID ownerId) {
        try {
            metrics.timeInsurerCall(() -> {
                insurerGateway.submit(quoteId);
                return null;
            });
        } catch (InsurerUnavailableException exception) {
            metrics.submissionFailed();
            quoteApi.markSubmissionFailed(quoteId, ownerId);
            throw exception;
        }
    }
}
```

- [ ] **Step 16: Update `SubmissionFinalizer`**

Modify `service/src/main/java/com/clara/insurancequotes/submission/application/service/SubmissionFinalizer.java` — full replacement:

```java
package com.clara.insurancequotes.submission.application.service;

import com.clara.insurancequotes.config.BusinessMetrics;
import com.clara.insurancequotes.quote.api.result.QuoteView;
import com.clara.insurancequotes.quote.api.usecase.QuoteApi;
import com.clara.insurancequotes.submission.api.event.QuoteSubmitted;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Commits the final quote state and its durable outbox event atomically. */
@Component
@RequiredArgsConstructor
@Slf4j
public class SubmissionFinalizer {

    private final QuoteApi quoteApi;
    private final ApplicationEventPublisher events;
    private final BusinessMetrics metrics;

    @Transactional
    public QuoteView completeSubmission(UUID quoteId, UUID ownerId) {
        var view = quoteApi.markSubmitted(quoteId, ownerId);
        events.publishEvent(new QuoteSubmitted(view.id(), view.monthlyPremium(), view.updatedAt()));
        metrics.domainEventPublished("quote_submitted");
        log.debug("Finalized quote submission {}", quoteId);
        return view;
    }
}
```

- [ ] **Step 17: Wire `QuoteController` to extract the requester from the JWT**

Modify `service/src/main/java/com/clara/insurancequotes/quote/adapter/in/web/controller/QuoteController.java` — full replacement:

```java
package com.clara.insurancequotes.quote.adapter.in.web.controller;

import com.clara.insurancequotes.quote.adapter.in.web.request.CreateQuoteRequest;
import com.clara.insurancequotes.quote.adapter.in.web.request.UpdateCoverageRequest;
import com.clara.insurancequotes.quote.api.query.QuoteQuery;
import com.clara.insurancequotes.quote.api.result.QuotePageView;
import com.clara.insurancequotes.quote.api.result.QuoteSummaryView;
import com.clara.insurancequotes.quote.api.result.QuoteView;
import com.clara.insurancequotes.quote.api.usecase.QuoteApi;
import com.clara.insurancequotes.quote.api.usecase.RequestingUser;
import com.clara.insurancequotes.quote.application.exception.InvalidQuoteQueryException;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/quotes", version = "1.0")
@RequiredArgsConstructor
@Slf4j
public class QuoteController {

    private static final String ADMIN_ROLE = "ADMIN";

    private final QuoteApi quoteApi;

    @PostMapping
    public ResponseEntity<QuoteView> create(
            @Valid @RequestBody CreateQuoteRequest request, @AuthenticationPrincipal Jwt jwt) {
        var view = quoteApi.create(request.toCommand(), requester(jwt).id());
        log.debug("Created quote response {}", view.id());
        return ResponseEntity.created(URI.create("/quotes/" + view.id())).body(view);
    }

    @PatchMapping("/{id}/coverage")
    public QuoteView updateCoverage(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCoverageRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return quoteApi.updateCoverage(id, request.toCommand(), requester(jwt).id());
    }

    @GetMapping("/{id}")
    public QuoteView getQuote(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        return quoteApi.getQuote(id, requester(jwt));
    }

    @GetMapping
    public QuotePageView listQuotes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String coverage,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            @AuthenticationPrincipal Jwt jwt) {
        try {
            return quoteApi.listQuotes(
                    QuoteQuery.of(page, size, search, status, coverage, sortBy, direction), requester(jwt));
        } catch (IllegalArgumentException exception) {
            throw new InvalidQuoteQueryException(exception.getMessage());
        }
    }

    @GetMapping("/summary")
    public QuoteSummaryView getSummary(@AuthenticationPrincipal Jwt jwt) {
        return quoteApi.getSummary(requester(jwt));
    }

    private static RequestingUser requester(Jwt jwt) {
        return new RequestingUser(
                UUID.fromString(jwt.getClaimAsString("uid")), ADMIN_ROLE.equals(jwt.getClaimAsString("role")));
    }
}
```

- [ ] **Step 18: Wire `SubmissionController`**

Modify `service/src/main/java/com/clara/insurancequotes/submission/adapter/in/web/controller/SubmissionController.java` — full replacement:

```java
package com.clara.insurancequotes.submission.adapter.in.web.controller;

import com.clara.insurancequotes.quote.api.result.QuoteView;
import com.clara.insurancequotes.submission.api.usecase.SubmissionApi;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
public class SubmissionController {

    private final SubmissionApi submissionApi;

    @PostMapping(value = "/quotes/{id}/submit", version = "1.0")
    public QuoteView submit(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        log.debug("Submitting quote {}", id);
        return submissionApi.submit(id, UUID.fromString(jwt.getClaimAsString("uid")));
    }
}
```

- [ ] **Step 19: Update `QuoteServiceTest` call sites**

Modify `service/src/test/java/com/clara/insurancequotes/quote/application/service/QuoteServiceTest.java`. Add `import java.util.UUID;` and `import com.clara.insurancequotes.quote.api.usecase.RequestingUser;` if not already present, add a fixed test owner constant, and update every call. Full replacement:

```java
package com.clara.insurancequotes.quote.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.clara.insurancequotes.config.BusinessMetrics;
import com.clara.insurancequotes.pricing.api.result.Premium;
import com.clara.insurancequotes.pricing.api.type.CoverageType;
import com.clara.insurancequotes.pricing.api.usecase.PremiumCalculator;
import com.clara.insurancequotes.quote.api.command.CreateQuoteCommand;
import com.clara.insurancequotes.quote.api.command.UpdateCoverageCommand;
import com.clara.insurancequotes.quote.api.query.QuoteQuery;
import com.clara.insurancequotes.quote.api.type.HealthCondition;
import com.clara.insurancequotes.quote.api.usecase.RequestingUser;
import com.clara.insurancequotes.quote.application.exception.QuoteNotFoundException;
import com.clara.insurancequotes.quote.domain.exception.HealthDataNotAllowedException;
import com.clara.insurancequotes.quote.domain.model.QuoteStatus;
import com.clara.insurancequotes.testsupport.InMemoryQuoteRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class QuoteServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-22T10:00:00Z");
    private static final UUID OWNER = UUID.randomUUID();
    private static final UUID OTHER_OWNER = UUID.randomUUID();
    private static final RequestingUser AS_OWNER = new RequestingUser(OWNER, false);
    private static final RequestingUser AS_ADMIN = new RequestingUser(UUID.randomUUID(), true);

    private final InMemoryQuoteRepository repository = new InMemoryQuoteRepository();
    private final PremiumCalculator calculator = input -> new Premium(new BigDecimal("100.00"));
    private final SimpleMeterRegistry metricsRegistry = new SimpleMeterRegistry();
    private final QuoteService service = new QuoteService(
            repository, calculator, Clock.fixed(NOW, ZoneOffset.UTC), new BusinessMetrics(metricsRegistry));

    private static final CreateQuoteCommand ADULT = new CreateQuoteCommand("Jane Roe", "jane@example.com", 34, "06600");
    private static final CreateQuoteCommand SENIOR =
            new CreateQuoteCommand("John Elder", "john@example.com", 70, "06600");

    private static UpdateCoverageCommand plainCoverage() {
        return new UpdateCoverageCommand(CoverageType.STANDARD, null, null, null, null, null);
    }

    private static UpdateCoverageCommand seniorCoverage() {
        return new UpdateCoverageCommand(
                CoverageType.STANDARD,
                true,
                Set.of(HealthCondition.DIABETES, HealthCondition.HYPERTENSION),
                false,
                true,
                true);
    }

    @Test
    void create_persistsDraftAndReturnsView() {
        var view = service.create(ADULT, OWNER);

        assertThat(view.status()).isEqualTo(QuoteStatus.DRAFT);
        assertThat(repository.findById(view.id(), OWNER)).isPresent();
    }

    @Test
    void updateCoverage_computesPremiumServerSide() {
        var id = service.create(ADULT, OWNER).id();

        var view = service.updateCoverage(id, plainCoverage(), OWNER);

        assertThat(view.monthlyPremium()).isEqualByComparingTo("100.00");
        assertThat(view.coverageType()).isEqualTo(CoverageType.STANDARD);
        assertThat(metricsRegistry
                        .get("quotes.coverage.updates")
                        .tag("outcome", "success")
                        .tag("coverage_type", "standard")
                        .counter()
                        .count())
                .isEqualTo(1);
    }

    @Test
    void updateCoverage_healthDataAtAge65OrBelow_isRejected() {
        var id = service.create(ADULT, OWNER).id();

        assertThatThrownBy(() -> service.updateCoverage(id, seniorCoverage(), OWNER))
                .isInstanceOf(HealthDataNotAllowedException.class);
        assertThat(metricsRegistry
                        .get("quotes.coverage.updates")
                        .tag("outcome", "rejected")
                        .tag("coverage_type", "standard")
                        .counter()
                        .count())
                .isEqualTo(1);
    }

    @Test
    void updateCoverage_healthDataOver65_isAccepted() {
        var id = service.create(SENIOR, OWNER).id();

        var view = service.updateCoverage(id, seniorCoverage(), OWNER);

        assertThat(view.usesTobacco()).isTrue();
        assertThat(view.conditions()).containsExactlyInAnyOrder(HealthCondition.DIABETES, HealthCondition.HYPERTENSION);
    }

    @Test
    void getQuote_unknownId_throwsNotFound() {
        assertThatThrownBy(() -> service.getQuote(UUID.randomUUID(), AS_OWNER)).isInstanceOf(QuoteNotFoundException.class);
    }

    @Test
    void getQuote_ownedByOtherUser_throwsNotFound() {
        var id = service.create(ADULT, OTHER_OWNER).id();

        assertThatThrownBy(() -> service.getQuote(id, AS_OWNER)).isInstanceOf(QuoteNotFoundException.class);
    }

    @Test
    void getQuote_asAdmin_seesAnyUsersQuote() {
        var id = service.create(ADULT, OTHER_OWNER).id();

        var view = service.getQuote(id, AS_ADMIN);

        assertThat(view.id()).isEqualTo(id);
    }

    @Test
    void updateCoverage_ownedByOtherUser_throwsNotFoundEvenForAdmin() {
        var id = service.create(ADULT, OTHER_OWNER).id();
        var adminOwnId = AS_ADMIN.id();

        assertThatThrownBy(() -> service.updateCoverage(id, plainCoverage(), adminOwnId))
                .isInstanceOf(QuoteNotFoundException.class);
    }

    @Test
    void markSubmitted_transitionsAndPersists() {
        var id = service.create(ADULT, OWNER).id();
        service.updateCoverage(id, plainCoverage(), OWNER);

        var view = service.markSubmitted(id, OWNER);

        assertThat(view.status()).isEqualTo(QuoteStatus.SUBMITTED);
        assertThat(repository.findById(id, OWNER).orElseThrow().status()).isEqualTo(QuoteStatus.SUBMITTED);
    }

    @Test
    void listQuotes_returnsFilteredOrderedPageMetadata() {
        var jane = service.create(ADULT, OWNER);
        service.updateCoverage(jane.id(), plainCoverage(), OWNER);
        service.markSubmitted(jane.id(), OWNER);
        service.create(SENIOR, OWNER);

        var result =
                service.listQuotes(QuoteQuery.of(0, 1, "jane", "SUBMITTED", "STANDARD", "name", "asc"), AS_OWNER);

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).name()).isEqualTo("Jane Roe");
        assertThat(result.page()).isZero();
        assertThat(result.size()).isEqualTo(1);
        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.totalPages()).isEqualTo(1);
        assertThat(result.hasNext()).isFalse();
        assertThat(result.hasPrevious()).isFalse();
    }

    @Test
    void listQuotes_excludesOtherUsersQuotesForNonAdmin() {
        service.create(ADULT, OWNER);
        service.create(SENIOR, OTHER_OWNER);

        var result = service.listQuotes(QuoteQuery.defaults(), AS_OWNER);

        assertThat(result.content()).hasSize(1);
        assertThat(result.totalElements()).isEqualTo(1);
    }

    @Test
    void listQuotes_includesEveryUsersQuotesForAdmin() {
        service.create(ADULT, OWNER);
        service.create(SENIOR, OTHER_OWNER);

        var result = service.listQuotes(QuoteQuery.defaults(), AS_ADMIN);

        assertThat(result.totalElements()).isEqualTo(2);
    }

    @Test
    void getSummary_returnsAggregateMetricsAndSevenDayTrend() {
        var draft = service.create(ADULT, OWNER);
        var submitted = service.create(SENIOR, OWNER);
        service.updateCoverage(submitted.id(), plainCoverage(), OWNER);
        service.markSubmitted(submitted.id(), OWNER);
        var failed = service.create(new CreateQuoteCommand("Failed Quote", "failed@example.com", 40, "06600"), OWNER);
        service.markSubmissionFailed(failed.id(), OWNER);

        var result = service.getSummary(AS_OWNER);

        assertThat(result.totalQuotes()).isEqualTo(3);
        assertThat(result.draftQuotes()).isEqualTo(1);
        assertThat(result.submittedQuotes()).isEqualTo(1);
        assertThat(result.submissionFailedQuotes()).isEqualTo(1);
        assertThat(result.expiredQuotes()).isZero();
        assertThat(result.pricedQuotes()).isEqualTo(1);
        assertThat(result.totalMonthlyPremium()).isEqualByComparingTo("100.00");
        assertThat(result.averageMonthlyPremium()).isEqualByComparingTo("100.00");
        assertThat(result.submissionRate()).isEqualByComparingTo("50.00");
        assertThat(result.statusDistribution())
                .extracting("key")
                .containsExactly("DRAFT", "SUBMITTED", "SUBMISSION_FAILED", "EXPIRED");
        assertThat(result.coverageDistribution()).extracting("key").containsExactly("BASIC", "STANDARD", "PREMIUM");
        assertThat(result.trend()).hasSize(7);
        assertThat(result.trend().get(6).created()).isEqualTo(3);
        assertThat(result.trend().get(6).submitted()).isEqualTo(1);
        assertThat(result.trend().get(6).failed()).isEqualTo(1);
        assertThat(draft.status()).isEqualTo(QuoteStatus.DRAFT);
    }
}
```

- [ ] **Step 20: Update `SubmissionServiceTest`**

Modify `service/src/test/java/com/clara/insurancequotes/submission/application/service/SubmissionServiceTest.java` — full replacement:

```java
package com.clara.insurancequotes.submission.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.clara.insurancequotes.config.BusinessMetrics;
import com.clara.insurancequotes.quote.api.result.QuoteView;
import com.clara.insurancequotes.quote.api.usecase.QuoteApi;
import com.clara.insurancequotes.quote.domain.model.QuoteStatus;
import com.clara.insurancequotes.submission.api.exception.InsurerUnavailableException;
import com.clara.insurancequotes.submission.application.port.out.InsurerGateway;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SubmissionServiceTest {

    private final QuoteApi quoteApi = mock(QuoteApi.class);
    private final InsurerGateway insurerGateway = mock(InsurerGateway.class);
    private final SubmissionFinalizer finalizer = mock(SubmissionFinalizer.class);
    private final SubmissionService service =
            new SubmissionService(quoteApi, insurerGateway, finalizer, new BusinessMetrics(new SimpleMeterRegistry()));

    private static final UUID QUOTE_ID = UUID.randomUUID();
    private static final UUID OWNER_ID = UUID.randomUUID();

    private static QuoteView viewWithStatus(QuoteStatus status) {
        return new QuoteView(
                QUOTE_ID,
                "Jane Roe",
                "jane@example.com",
                34,
                "06600",
                null,
                null,
                null,
                null,
                null,
                null,
                new BigDecimal("100.00"),
                status,
                Instant.now(),
                Instant.now());
    }

    @Test
    void submit_alreadySubmitted_isIdempotentAndSkipsInsurer() {
        when(quoteApi.getOwnedQuote(QUOTE_ID, OWNER_ID)).thenReturn(viewWithStatus(QuoteStatus.SUBMITTED));

        var view = service.submit(QUOTE_ID, OWNER_ID);

        assertThat(view.status()).isEqualTo(QuoteStatus.SUBMITTED);
        verify(insurerGateway, never()).submit(QUOTE_ID);
        verify(finalizer, never()).completeSubmission(QUOTE_ID, OWNER_ID);
    }

    @Test
    void submit_insurerAccepts_finalizes() {
        when(quoteApi.getOwnedQuote(QUOTE_ID, OWNER_ID)).thenReturn(viewWithStatus(QuoteStatus.DRAFT));
        when(quoteApi.ensureSubmittable(QUOTE_ID, OWNER_ID)).thenReturn(viewWithStatus(QuoteStatus.DRAFT));
        when(finalizer.completeSubmission(QUOTE_ID, OWNER_ID)).thenReturn(viewWithStatus(QuoteStatus.SUBMITTED));

        var view = service.submit(QUOTE_ID, OWNER_ID);

        assertThat(view.status()).isEqualTo(QuoteStatus.SUBMITTED);
        verify(insurerGateway).submit(QUOTE_ID);
    }

    @Test
    void submit_insurerFails_marksFailedAndRethrows() {
        when(quoteApi.getOwnedQuote(QUOTE_ID, OWNER_ID)).thenReturn(viewWithStatus(QuoteStatus.DRAFT));
        when(quoteApi.ensureSubmittable(QUOTE_ID, OWNER_ID)).thenReturn(viewWithStatus(QuoteStatus.DRAFT));
        doThrow(new InsurerUnavailableException("boom")).when(insurerGateway).submit(QUOTE_ID);

        assertThatThrownBy(() -> service.submit(QUOTE_ID, OWNER_ID)).isInstanceOf(InsurerUnavailableException.class);

        verify(quoteApi).markSubmissionFailed(QUOTE_ID, OWNER_ID);
        verify(finalizer, never()).completeSubmission(QUOTE_ID, OWNER_ID);
    }
}
```

- [ ] **Step 21: Update `SubmissionFinalizerTest`**

Modify `service/src/test/java/com/clara/insurancequotes/submission/application/service/SubmissionFinalizerTest.java` — full replacement:

```java
package com.clara.insurancequotes.submission.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.clara.insurancequotes.config.BusinessMetrics;
import com.clara.insurancequotes.quote.api.result.QuoteView;
import com.clara.insurancequotes.quote.api.usecase.QuoteApi;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

class SubmissionFinalizerTest {

    private static final UUID QUOTE_ID = UUID.randomUUID();
    private static final UUID OWNER_ID = UUID.randomUUID();

    private final QuoteApi quoteApi = mock(QuoteApi.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final SubmissionFinalizer finalizer =
            new SubmissionFinalizer(quoteApi, events, new BusinessMetrics(registry));

    @Test
    void completeSubmission_publishesDurableBusinessEventAndRecordsMetric() {
        var view = mock(QuoteView.class);
        when(view.id()).thenReturn(QUOTE_ID);
        when(view.monthlyPremium()).thenReturn(null);
        when(view.updatedAt()).thenReturn(Instant.parse("2026-07-26T08:00:00Z"));
        when(quoteApi.markSubmitted(QUOTE_ID, OWNER_ID)).thenReturn(view);

        finalizer.completeSubmission(QUOTE_ID, OWNER_ID);

        verify(events).publishEvent(org.mockito.ArgumentMatchers.<Object>any());
        assertThat(registry.get("domain.events")
                        .tag("event_type", "quote_submitted")
                        .counter()
                        .count())
                .isEqualTo(1);
    }
}
```

- [ ] **Step 22: Update `QuoteControllerTest`**

Modify `service/src/test/java/com/clara/insurancequotes/quote/adapter/in/web/controller/QuoteControllerTest.java`. Add a helper that attaches `uid`/`role` claims to every `jwt()` post-processor, and update every `quoteApi` stub/verification to the new signatures. Full replacement:

```java
package com.clara.insurancequotes.quote.adapter.in.web.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.clara.insurancequotes.auth.configuration.JwtConfig;
import com.clara.insurancequotes.auth.configuration.SecurityConfig;
import com.clara.insurancequotes.pricing.api.type.CoverageType;
import com.clara.insurancequotes.quote.adapter.in.web.advice.QuoteExceptionHandler;
import com.clara.insurancequotes.quote.api.result.QuotePageView;
import com.clara.insurancequotes.quote.api.result.QuoteSummaryView;
import com.clara.insurancequotes.quote.api.result.QuoteView;
import com.clara.insurancequotes.quote.api.usecase.QuoteApi;
import com.clara.insurancequotes.quote.api.usecase.RequestingUser;
import com.clara.insurancequotes.quote.domain.exception.HealthDataNotAllowedException;
import com.clara.insurancequotes.quote.domain.model.QuoteStatus;
import com.clara.insurancequotes.shared.configuration.I18nConfig;
import com.clara.insurancequotes.shared.error.GlobalExceptionHandler;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(QuoteController.class)
@Import({
    SecurityConfig.class,
    JwtConfig.class,
    I18nConfig.class,
    GlobalExceptionHandler.class,
    QuoteExceptionHandler.class
})
@TestPropertySource(properties = {"auth.jwt.secret=test-secret-that-is-32-bytes-long!!"})
class QuoteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private QuoteApi quoteApi;

    private static final UUID QUOTE_ID = UUID.fromString("f7d9a1c2-0000-0000-0000-000000000001");
    private static final UUID OWNER_ID = UUID.fromString("b2222222-0000-0000-0000-000000000002");

    private static JwtRequestPostProcessor asOwner() {
        return jwt().authorities(new SimpleGrantedAuthority("SCOPE_api"))
                .jwt(builder -> builder.claim("uid", OWNER_ID.toString()).claim("role", "USER"));
    }

    private static RequestingUser requestingOwner() {
        return new RequestingUser(OWNER_ID, false);
    }

    private static QuoteView draftView() {
        return new QuoteView(
                QUOTE_ID,
                "Jane Roe",
                "jane@example.com",
                34,
                "06600",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                QuoteStatus.DRAFT,
                Instant.now(),
                Instant.now());
    }

    @Test
    void createQuote_valid_returns201WithId() throws Exception {
        when(quoteApi.create(any(), eq(OWNER_ID))).thenReturn(draftView());

        mockMvc.perform(post("/quotes")
                        .with(asOwner())
                        .contentType("application/json")
                        .content(
                                "{\"name\":\"Jane Roe\",\"email\":\"jane@example.com\",\"age\":34,\"zipCode\":\"06600\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(QUOTE_ID.toString()))
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    @Test
    void createQuote_missingFields_returns400WithFieldErrors() throws Exception {
        mockMvc.perform(post("/quotes")
                        .with(asOwner())
                        .contentType("application/json")
                        .content("{\"name\":\"\",\"email\":\"not-an-email\",\"age\":0,\"zipCode\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    @Test
    void updateCoverage_healthDataRejected_returns422() throws Exception {
        when(quoteApi.updateCoverage(eq(QUOTE_ID), any(), eq(OWNER_ID)))
                .thenThrow(new HealthDataNotAllowedException(34));

        mockMvc.perform(patch("/quotes/{id}/coverage", QUOTE_ID)
                        .with(asOwner())
                        .contentType("application/json")
                        .content("{\"coverageType\":\"STANDARD\",\"usesTobacco\":true}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("QUOTE_HEALTH_DATA_NOT_ALLOWED"));
    }

    @Test
    void updateCoverage_valid_returnsPremium() throws Exception {
        var view = new QuoteView(
                QUOTE_ID,
                "Jane Roe",
                "jane@example.com",
                34,
                "06600",
                CoverageType.STANDARD,
                null,
                null,
                null,
                null,
                null,
                new BigDecimal("100.00"),
                QuoteStatus.DRAFT,
                Instant.now(),
                Instant.now());
        when(quoteApi.updateCoverage(eq(QUOTE_ID), any(), eq(OWNER_ID))).thenReturn(view);

        mockMvc.perform(patch("/quotes/{id}/coverage", QUOTE_ID)
                        .with(asOwner())
                        .contentType("application/json")
                        .content("{\"coverageType\":\"STANDARD\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monthlyPremium").value(100.00));
    }

    @Test
    void getQuote_ownedByOtherUser_returns404() throws Exception {
        when(quoteApi.getQuote(eq(QUOTE_ID), eq(requestingOwner())))
                .thenThrow(new com.clara.insurancequotes.quote.application.exception.QuoteNotFoundException(QUOTE_ID));

        mockMvc.perform(get("/quotes/{id}", QUOTE_ID).with(asOwner()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("QUOTE_NOT_FOUND"));
    }

    @Test
    void getQuotes_withoutJwt_returns401() throws Exception {
        mockMvc.perform(get("/quotes")).andExpect(status().isUnauthorized());
    }

    @Test
    void unsupportedApiVersion_isRejected() throws Exception {
        mockMvc.perform(get("/quotes").with(asOwner()).header("API-Version", "9.0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void supportedApiVersion_routesToController() throws Exception {
        doReturn(new QuotePageView(List.of(), 0, 20, 0, 0, false, false))
                .when(quoteApi)
                .listQuotes(any(), eq(requestingOwner()));

        mockMvc.perform(get("/quotes").with(asOwner()).header("API-Version", "1.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.totalPages").value(0))
                .andExpect(jsonPath("$.hasNext").value(false))
                .andExpect(jsonPath("$.hasPrevious").value(false));
    }

    @Test
    void listQuotes_acceptsFilteringAndOrderingParameters() throws Exception {
        doReturn(new QuotePageView(List.of(), 1, 10, 1, 1, false, true))
                .when(quoteApi)
                .listQuotes(any(), eq(requestingOwner()));

        mockMvc.perform(get("/quotes")
                        .with(asOwner())
                        .header("API-Version", "1.0")
                        .queryParam("page", "1")
                        .queryParam("size", "10")
                        .queryParam("search", "jane")
                        .queryParam("status", "SUBMITTED")
                        .queryParam("coverage", "STANDARD")
                        .queryParam("sortBy", "name")
                        .queryParam("direction", "desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(10));
    }

    @Test
    void getQuoteSummary_returnsAnalyticsEnvelope() throws Exception {
        when(quoteApi.getSummary(requestingOwner()))
                .thenReturn(new QuoteSummaryView(
                        3,
                        1,
                        1,
                        1,
                        0,
                        1,
                        new BigDecimal("100.00"),
                        new BigDecimal("100.00"),
                        new BigDecimal("50.00"),
                        List.of(),
                        List.of(),
                        List.of()));

        mockMvc.perform(get("/quotes/summary").with(asOwner()).header("API-Version", "1.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalQuotes").value(3))
                .andExpect(jsonPath("$.submissionRate").value(50.00))
                .andExpect(jsonPath("$.trend").isArray());
    }

    @Test
    void listQuotes_invalidQuery_returns400WithQuoteError() throws Exception {
        mockMvc.perform(get("/quotes").with(asOwner()).queryParam("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("QUOTE_INVALID_QUERY"));
    }
}
```

- [ ] **Step 23: Update `QuoteCachingIT` to prove cache isolation between requesters**

Modify `service/src/integrationTest/java/com/clara/insurancequotes/quote/application/service/QuoteCachingIT.java` — full replacement:

```java
package com.clara.insurancequotes.quote.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.clara.insurancequotes.pricing.api.type.CoverageType;
import com.clara.insurancequotes.quote.api.command.CreateQuoteCommand;
import com.clara.insurancequotes.quote.api.command.UpdateCoverageCommand;
import com.clara.insurancequotes.quote.api.usecase.RequestingUser;
import com.clara.insurancequotes.quote.application.exception.QuoteNotFoundException;
import com.clara.insurancequotes.testsupport.Containers;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(properties = {"spring.kafka.bootstrap-servers=localhost:1"})
class QuoteCachingIT {

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        Containers.registerPostgres(registry);
        Containers.registerRedis(registry);
    }

    @Autowired
    private QuoteService service;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private StringRedisTemplate redis;

    private static final UUID OWNER = UUID.randomUUID();

    @Test
    void getQuote_populatesCache_updateCoverageEvicts() {
        var id = service.create(new CreateQuoteCommand("Jane Roe", "jane@example.com", 34, "06600"), OWNER)
                .id();
        var cache = cacheManager.getCache("quotes");
        var owner = new RequestingUser(OWNER, false);

        service.getQuote(id, owner);
        assertThat(redis.keys("*")).isNotEmpty();
        assertThat(cache.get(id + "|" + OWNER)).isNotNull();

        service.updateCoverage(id, new UpdateCoverageCommand(CoverageType.BASIC, null, null, null, null, null), OWNER);
        assertThat(cache.get(id + "|" + OWNER)).isNull();
    }

    @Test
    void getQuote_cacheEntryIsIsolatedPerRequester() {
        var id = service.create(new CreateQuoteCommand("Jane Roe", "jane@example.com", 34, "06600"), OWNER)
                .id();
        var admin = new RequestingUser(UUID.randomUUID(), true);
        var otherUser = new RequestingUser(UUID.randomUUID(), false);

        service.getQuote(id, admin);

        assertThatThrownBy(() -> service.getQuote(id, otherUser)).isInstanceOf(QuoteNotFoundException.class);
    }
}
```

- [ ] **Step 24: Update `SubmissionFlowIT`**

Modify `service/src/integrationTest/java/com/clara/insurancequotes/submission/SubmissionFlowIT.java`. Add `import java.util.UUID;` (already imported) plus a fixed `OWNER` constant, and thread it through every call. Change the `submittableQuote()` helper and every `quoteApi`/`submissionApi` call:

```java
    private static final UUID OWNER = UUID.randomUUID();

    private UUID submittableQuote() {
        var id = quoteApi
                .create(new CreateQuoteCommand("Jane Roe", "jane@example.com", 34, "06600"), OWNER)
                .id();
        quoteApi.updateCoverage(
                id, new UpdateCoverageCommand(CoverageType.STANDARD, null, null, null, null, null), OWNER);
        return id;
    }

    @Test
    void successfulSubmission_setsSubmitted_andPublishesKafkaEvent() {
        INSURER.stubFor(post(urlEqualTo("/submit")).willReturn(aResponse().withStatus(200)));
        var id = submittableQuote();

        var view = submissionApi.submit(id, OWNER);

        assertThat(view.status()).isEqualTo(QuoteStatus.SUBMITTED);
        try (var consumer = newConsumer()) {
            consumer.subscribe(List.of("quote-submitted"));
            Awaitility.await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
                var records = consumer.poll(Duration.ofMillis(500));
                assertThat(records.records("quote-submitted"))
                        .anySatisfy(record -> assertThat(record.value()).contains(id.toString()));
            });
        }
    }

    @Test
    void failedSubmission_marksFailed_thenRetrySucceeds() {
        INSURER.stubFor(post(urlEqualTo("/submit")).willReturn(aResponse().withStatus(500)));
        var id = submittableQuote();

        assertThatThrownBy(() -> submissionApi.submit(id, OWNER)).isInstanceOf(InsurerUnavailableException.class);
        assertThat(quoteApi.getOwnedQuote(id, OWNER).status()).isEqualTo(QuoteStatus.SUBMISSION_FAILED);

        INSURER.stubFor(post(urlEqualTo("/submit")).willReturn(aResponse().withStatus(200)));
        assertThat(submissionApi.submit(id, OWNER).status()).isEqualTo(QuoteStatus.SUBMITTED);
    }

    @Test
    void submittingTwice_isIdempotent() {
        INSURER.stubFor(post(urlEqualTo("/submit")).willReturn(aResponse().withStatus(200)));
        var id = submittableQuote();

        submissionApi.submit(id, OWNER);
        var second = submissionApi.submit(id, OWNER);

        assertThat(second.status()).isEqualTo(QuoteStatus.SUBMITTED);
        assertThat(INSURER.getAllServeEvents()).hasSize(1);
    }
```

(the class declaration, WireMock/Testcontainers setup, `@Autowired` fields, and `newConsumer()` helper are unchanged — only the body shown above changes).

- [ ] **Step 25: Write the failing test for owner-aware draft expiration**

`QuoteCacheEvictionListener` (`quote/adapter/in/messaging/consumer`) currently evicts the cache by `event.quoteId()` alone when a draft expires. Once the cache key becomes `id|ownerId` (Step 13), that plain-ID eviction stops matching any real entry, so draft expiration would silently stop invalidating the cache for every requester. Fix this now, before it ships as a regression.

Modify `service/src/test/java/com/clara/insurancequotes/quote/application/service/DraftExpirationJobTest.java` — change the assertion:

```java
        assertThat(publishedEvents).containsExactly(new QuoteExpired(stale.id()));
```

to:

```java
        assertThat(publishedEvents).containsExactly(new QuoteExpired(stale.id(), stale.userId()));
```

- [ ] **Step 26: Run it to verify it fails**

Run: `cd insurance-quotes-service && mvn -pl service test -Dtest=DraftExpirationJobTest`
Expected: compile failure — `QuoteExpired` still has only a one-argument constructor.

- [ ] **Step 27: Add `StaleQuoteRef` and rename the expiration query on the port**

Create `service/src/main/java/com/clara/insurancequotes/quote/application/port/out/StaleQuoteRef.java`:

```java
package com.clara.insurancequotes.quote.application.port.out;

import java.util.UUID;

public record StaleQuoteRef(UUID id, UUID ownerId) {}
```

Modify `service/src/main/java/com/clara/insurancequotes/quote/application/port/out/QuoteRepository.java` — change:

```java
    List<UUID> findIdsToExpire(Instant cutoff);
```

to:

```java
    List<StaleQuoteRef> findStaleDrafts(Instant cutoff);
```

- [ ] **Step 28: Update `SpringDataQuoteRepository` to select the owner alongside stale IDs**

Modify `service/src/main/java/com/clara/insurancequotes/quote/adapter/out/persistence/SpringDataQuoteRepository.java` — change:

```java
    @Query("select q.id from Quote q where q.status = :status and q.createdAt < :cutoff")
    List<UUID> findIdsToExpire(@Param("status") QuoteStatus status, @Param("cutoff") Instant cutoff);

    default List<UUID> findIdsToExpire(Instant cutoff) {
        return findIdsToExpire(QuoteStatus.DRAFT, cutoff);
    }
```

to:

```java
    @Query("select q.id, q.userId from Quote q where q.status = :status and q.createdAt < :cutoff")
    List<Object[]> findStaleDraftRows(@Param("status") QuoteStatus status, @Param("cutoff") Instant cutoff);

    default List<Object[]> findStaleDraftRows(Instant cutoff) {
        return findStaleDraftRows(QuoteStatus.DRAFT, cutoff);
    }
```

- [ ] **Step 29: Update `JpaQuoteRepository` to map rows into `StaleQuoteRef`**

Modify `service/src/main/java/com/clara/insurancequotes/quote/adapter/out/persistence/JpaQuoteRepository.java` — add the import `com.clara.insurancequotes.quote.application.port.out.StaleQuoteRef;` and change:

```java
    @Override
    public List<UUID> findIdsToExpire(Instant cutoff) {
        return delegate.findIdsToExpire(cutoff);
    }
```

to:

```java
    @Override
    public List<StaleQuoteRef> findStaleDrafts(Instant cutoff) {
        return delegate.findStaleDraftRows(cutoff).stream()
                .map(row -> new StaleQuoteRef((UUID) row[0], (UUID) row[1]))
                .toList();
    }
```

This rename also breaks `QuoteRepositoryIT.markExpired_batchUpdatesOnlyGivenIds()` (written in Step 1), which calls `repository.findIdsToExpire(cutoff)` directly on `JpaQuoteRepository`. Modify `service/src/integrationTest/java/com/clara/insurancequotes/quote/adapter/out/persistence/QuoteRepositoryIT.java` — add the import `com.clara.insurancequotes.quote.application.port.out.StaleQuoteRef;` and change:

```java
        var cutoff = QuoteMother.FIXED_NOW.plus(Duration.ofMinutes(31));
        var ids = repository.findIdsToExpire(cutoff);
        var updated = repository.markExpired(ids, cutoff);
```

to:

```java
        var cutoff = QuoteMother.FIXED_NOW.plus(Duration.ofMinutes(31));
        var ids = repository.findStaleDrafts(cutoff).stream().map(StaleQuoteRef::id).toList();
        var updated = repository.markExpired(ids, cutoff);
```

- [ ] **Step 30: Update `InMemoryQuoteRepository` to match**

Modify `service/src/testFixtures/java/com/clara/insurancequotes/testsupport/InMemoryQuoteRepository.java` — add the import `com.clara.insurancequotes.quote.application.port.out.StaleQuoteRef;` and change:

```java
    @Override
    public List<UUID> findIdsToExpire(Instant cutoff) {
        return store.values().stream()
                .filter(quote -> quote.status().allowsExpiration())
                .filter(quote -> quote.createdAt().isBefore(cutoff))
                .map(Quote::id)
                .toList();
    }
```

to:

```java
    @Override
    public List<StaleQuoteRef> findStaleDrafts(Instant cutoff) {
        return store.values().stream()
                .filter(quote -> quote.status().allowsExpiration())
                .filter(quote -> quote.createdAt().isBefore(cutoff))
                .map(quote -> new StaleQuoteRef(quote.id(), quote.userId()))
                .toList();
    }
```

- [ ] **Step 31: Add `ownerId` to the `QuoteExpired` event**

Modify `service/src/main/java/com/clara/insurancequotes/quote/domain/event/QuoteExpired.java` — full replacement:

```java
package com.clara.insurancequotes.quote.domain.event;

import java.util.UUID;

/** In-memory completed fact consumed by the local cache listener. */
public record QuoteExpired(UUID quoteId, UUID ownerId) {}
```

- [ ] **Step 32: Publish the owner on each expiration event**

Modify `service/src/main/java/com/clara/insurancequotes/quote/application/service/DraftExpirationJob.java` — add the import `com.clara.insurancequotes.quote.application.port.out.StaleQuoteRef;` and change the `expireStaleDrafts` method body from:

```java
    @Transactional
    public int expireStaleDrafts() {
        var now = clock.instant();
        var staleIds = repository.findIdsToExpire(now.minus(draftTtl));
        if (staleIds.isEmpty()) {
            return 0;
        }
        var expired = repository.markExpired(staleIds, now);
        staleIds.forEach(id -> events.publishEvent(new QuoteExpired(id)));
        metrics.quotesExpired(expired);
        log.info("Expired {} stale draft quotes", expired);
        return expired;
    }
```

to:

```java
    @Transactional
    public int expireStaleDrafts() {
        var now = clock.instant();
        var staleDrafts = repository.findStaleDrafts(now.minus(draftTtl));
        if (staleDrafts.isEmpty()) {
            return 0;
        }
        var staleIds = staleDrafts.stream().map(StaleQuoteRef::id).toList();
        var expired = repository.markExpired(staleIds, now);
        staleDrafts.forEach(ref -> events.publishEvent(new QuoteExpired(ref.id(), ref.ownerId())));
        metrics.quotesExpired(expired);
        log.info("Expired {} stale draft quotes", expired);
        return expired;
    }
```

- [ ] **Step 33: Fix the cache eviction listener to use the composite key**

Modify `service/src/main/java/com/clara/insurancequotes/quote/adapter/in/messaging/consumer/QuoteCacheEvictionListener.java` — change:

```java
    @EventListener
    public void onQuoteExpired(QuoteExpired event) {
        var cache = cacheManager.getCache(CacheConfig.QUOTES_CACHE);
        if (cache != null) {
            cache.evict(event.quoteId());
            log.debug("Evicted expired quote {} from cache", event.quoteId());
        }
    }
```

to:

```java
    @EventListener
    public void onQuoteExpired(QuoteExpired event) {
        var cache = cacheManager.getCache(CacheConfig.QUOTES_CACHE);
        if (cache != null) {
            cache.evict(event.quoteId() + "|" + event.ownerId());
            log.debug("Evicted expired quote {} from cache", event.quoteId());
        }
    }
```

- [ ] **Step 34: Run `DraftExpirationJobTest` to confirm it now passes**

Run: `cd insurance-quotes-service && mvn -pl service test -Dtest=DraftExpirationJobTest`
Expected: PASS.

- [ ] **Step 35: Update `DraftExpirationJobIT` for the new signatures and composite cache key**

Modify `service/src/integrationTest/java/com/clara/insurancequotes/quote/application/service/DraftExpirationJobIT.java` — full replacement:

```java
package com.clara.insurancequotes.quote.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.clara.insurancequotes.quote.api.command.CreateQuoteCommand;
import com.clara.insurancequotes.quote.api.usecase.QuoteApi;
import com.clara.insurancequotes.quote.api.usecase.RequestingUser;
import com.clara.insurancequotes.quote.domain.model.QuoteStatus;
import com.clara.insurancequotes.testsupport.Containers;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(properties = {"spring.kafka.bootstrap-servers=localhost:1"})
class DraftExpirationJobIT {

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        Containers.registerPostgres(registry);
        Containers.registerRedis(registry);
    }

    @Autowired
    private QuoteApi quoteApi;

    @Autowired
    private DraftExpirationJob job;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private CacheManager cacheManager;

    private static final UUID OWNER = UUID.randomUUID();

    @Test
    void staleDraft_getsExpired_andItsCacheEntryEvicted() {
        var requester = new RequestingUser(OWNER, false);
        var id = quoteApi
                .create(new CreateQuoteCommand("Jane Roe", "jane@example.com", 34, "06600"), OWNER)
                .id();
        jdbcTemplate.update(
                "update quotes set created_at = ? where id = ?",
                OffsetDateTime.now().minusHours(2),
                id);
        quoteApi.getQuote(id, requester);

        var expired = job.expireStaleDrafts();

        assertThat(expired).isGreaterThanOrEqualTo(1);
        assertThat(cacheManager.getCache("quotes").get(id + "|" + OWNER)).isNull();
        assertThat(quoteApi.getQuote(id, requester).status()).isEqualTo(QuoteStatus.EXPIRED);
    }
}
```

- [ ] **Step 36: Run the full unit test suite**

Run: `cd insurance-quotes-service && mvn -pl service test`
Expected: PASS — every unit test in `quote` and `submission` packages, including the new ownership/admin tests added in Steps 1, 19, 22, and 25.

- [ ] **Step 37: Run the integration test suite**

Run: `cd insurance-quotes-service && mvn -pl service verify -Pintegration` (or the project's documented `mise run itest` — check `mise.toml` for the exact task name if this differs)
Expected: PASS — `QuoteRepositoryIT`, `QuoteCachingIT`, `SubmissionFlowIT`, `DraftExpirationJobIT`, and `OpenApiExportIT` all green. `OpenApiExportIT` should need no changes since no request/response DTO changed shape.

- [ ] **Step 38: Run `mvn verify` for the full gate (spotless + JaCoCo)**

Run: `cd insurance-quotes-service && mvn -pl service verify`
Expected: BUILD SUCCESS, JaCoCo coverage ≥ 80% on `domain`/`application` packages maintained.

- [ ] **Step 39: Commit**

```bash
cd insurance-quotes-service
git add service/src/main/resources/db/migration/V6__add_owner_to_quotes.sql \
        service/src/main/java/com/clara/insurancequotes/quote \
        service/src/main/java/com/clara/insurancequotes/submission \
        service/src/testFixtures/java/com/clara/insurancequotes/quote/domain/model/QuoteMother.java \
        service/src/testFixtures/java/com/clara/insurancequotes/testsupport/InMemoryQuoteRepository.java \
        service/src/test/java/com/clara/insurancequotes/quote \
        service/src/test/java/com/clara/insurancequotes/submission \
        service/src/integrationTest/java/com/clara/insurancequotes/quote \
        service/src/integrationTest/java/com/clara/insurancequotes/submission
git commit -m "feat(quote): scope quotes to their owner with read-only admin oversight"
```

---

### Task 3: Cross-user isolation and admin-oversight regression tests

**Status:** Implementation completed in commit `c61eaa6`. The controller regression test exists; full-suite verification remains intentionally deferred until the user-isolation migration work is ready for validation.

Most of the ownership-specific tests were already added inline in Task 2 (Steps 1, 19, 22, 23) because TDD on a signature cascade means the new assertions have to compile against the new signatures from the start. This task closes the remaining gap: a controller-level proof that the submit endpoint itself is owner-scoped (Task 2 only added this at the service level via `SubmissionServiceTest`).

**Files:**
- Test: `service/src/test/java/com/clara/insurancequotes/submission/adapter/in/web/controller/SubmissionControllerTest.java` (new)

**Interfaces:**
- Consumes: `SubmissionApi.submit(UUID quoteId, UUID ownerId)` (Task 2).

- [x] **Step 1: Write the failing controller test**

Create `service/src/test/java/com/clara/insurancequotes/submission/adapter/in/web/controller/SubmissionControllerTest.java`:

```java
package com.clara.insurancequotes.submission.adapter.in.web.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.clara.insurancequotes.auth.configuration.JwtConfig;
import com.clara.insurancequotes.auth.configuration.SecurityConfig;
import com.clara.insurancequotes.quote.application.exception.QuoteNotFoundException;
import com.clara.insurancequotes.shared.configuration.I18nConfig;
import com.clara.insurancequotes.shared.error.GlobalExceptionHandler;
import com.clara.insurancequotes.submission.adapter.in.web.advice.SubmissionExceptionHandler;
import com.clara.insurancequotes.submission.api.usecase.SubmissionApi;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SubmissionController.class)
@Import({
    SecurityConfig.class,
    JwtConfig.class,
    I18nConfig.class,
    GlobalExceptionHandler.class,
    SubmissionExceptionHandler.class
})
@TestPropertySource(properties = {"auth.jwt.secret=test-secret-that-is-32-bytes-long!!"})
class SubmissionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SubmissionApi submissionApi;

    private static final UUID QUOTE_ID = UUID.fromString("f7d9a1c2-0000-0000-0000-000000000001");
    private static final UUID OWNER_ID = UUID.fromString("b2222222-0000-0000-0000-000000000002");

    @Test
    void submit_onAnotherUsersQuote_returns404() throws Exception {
        when(submissionApi.submit(eq(QUOTE_ID), eq(OWNER_ID))).thenThrow(new QuoteNotFoundException(QUOTE_ID));

        mockMvc.perform(post("/quotes/{id}/submit", QUOTE_ID)
                        .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_api"))
                                .jwt(builder -> builder.claim("uid", OWNER_ID.toString()).claim("role", "USER"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("QUOTE_NOT_FOUND"));
    }
}
```

- [ ] **Step 2: Run it to verify it fails (or discover it already passes)**

Run: `cd insurance-quotes-service && mvn -pl service test -Dtest=SubmissionControllerTest`
Expected: This should actually PASS immediately if Task 2 was completed correctly, since `SubmissionController` already delegates to `SubmissionApi.submit(id, ownerId)` and `QuoteNotFoundException` is already mapped to 404 by the existing `QuoteExceptionHandler` (verify it's picked up — if `QuoteNotFoundException` isn't handled without importing `QuoteExceptionHandler` too, add it to the `@Import` list alongside `SubmissionExceptionHandler`). If it fails because `QuoteNotFoundException` isn't mapped, add `com.clara.insurancequotes.quote.adapter.in.web.advice.QuoteExceptionHandler` to the `@Import({...})` list and rerun.

- [ ] **Step 3: Run the full test suite one more time to confirm nothing regressed**

Run: `cd insurance-quotes-service && mvn -pl service test`
Expected: PASS.

- [x] **Step 4: Commit**

```bash
cd insurance-quotes-service
git add service/src/test/java/com/clara/insurancequotes/submission/adapter/in/web/controller/SubmissionControllerTest.java
git commit -m "test(submission): prove submit is owner-scoped at the controller boundary"
```

---

### Task 4: Update the AIUP documentation to reflect ownership and the admin role

**Status:** Documentation changes completed; validation of the integrated application remains deferred.

The reverse-engineered `docs/use_cases/UC-003..005-*.md` and `docs/entity_model.md` currently describe the pre-ownership shared-pool model. This task brings them in line with what Tasks 1–3 built, closing the follow-up flagged in the design doc's §9.

**Files:**
- Modify: `docs/entity_model.md`
- Modify: `docs/use_cases.puml`
- Modify: `docs/use_cases/UC-003-request-an-insurance-quote.md`
- Modify: `docs/use_cases/UC-004-submit-quote-to-insurer.md`
- Modify: `docs/use_cases/UC-005-review-quote-history-and-analytics.md`

- [x] **Step 1: Add `QUOTE.user_id` and `USER.role` to the entity model**

Modify `docs/entity_model.md`: add a `user_id` row to the `QUOTE` attribute table (`Foreign Key (USER.id)`, `Not Null`) directly after `id`; add a `role` row to the `USER` attribute table (`String`, length 20, `Not Null, Values: USER, ADMIN`) directly after `username`; add `USER ||--o{ QUOTE : "creates"` to the Mermaid diagram; remove the "Notes" paragraph stating quotes and users are unrelated subgraphs (no longer true).

- [x] **Step 2: Add the Administrator actor to the use case diagram**

Modify `docs/use_cases.puml`: add `actor "Administrator" as admin` and connect it to `UC005` (`admin --> UC005`), since admin oversight only applies to the read/history use case.

- [x] **Step 3: Update UC-003, UC-004, UC-005 with ownership and admin business rules**

Modify `docs/use_cases/UC-003-request-an-insurance-quote.md`: add to the Success Postconditions that the draft quote is owned by the requesting user; add `BR-023: Quotes Are Owned by Their Creator` under Business Rules.

Modify `docs/use_cases/UC-004-submit-quote-to-insurer.md`: add `BR-024: Submission Is Owner-Scoped, Admin Included` — an administrator cannot submit or edit another user's quote, only the owner can.

Modify `docs/use_cases/UC-005-review-quote-history-and-analytics.md`: change the Primary Actor line to `Registered User / Administrator (read-only)`; rewrite the note that previously said quotes are a shared pool visible to any signed-in user, replacing it with: a regular user's list/detail/analytics are scoped to quotes they created; an Administrator sees every user's quotes and global analytics, but cannot create, edit, or submit on another user's behalf. Add `BR-025: Administrator Read-Only Oversight`.

- [x] **Step 4: Commit**

```bash
cd insurance-quotes-service
git add docs/entity_model.md docs/use_cases.puml docs/use_cases/UC-003-request-an-insurance-quote.md \
        docs/use_cases/UC-004-submit-quote-to-insurer.md docs/use_cases/UC-005-review-quote-history-and-analytics.md
git commit -m "docs: reflect per-user quote ownership and admin oversight in AIUP artifacts"
```
