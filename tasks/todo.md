# Backend core refactor checkpoint

- [x] Standardize modules to DDD + Hexagonal Modulith boundaries.
- [x] Separate API contracts, application services, domain models, and directional adapters.
- [x] Keep domain exceptions transport-neutral and map them at inbound web boundaries.
- [x] Apply Lombok constructor/logging conventions without changing domain behavior.
- [x] Verify unit, integration, Modulith, Docker image, and Compose smoke-test behavior.

## Results

The backend now follows the responsibility-based package structure defined by the module templates. Quote lookup exceptions live in the application layer, domain rules remain framework-neutral, and HTTP error translation is isolated in inbound adapters.

## Redis, responsive UI, and dynamic locale checkpoint

- [x] Reconcile the written implementation plan with the completed backend and frontend changes.
- [x] Verify backend unit, integration, formatting, and coverage checks.
- [x] Verify frontend unit tests, build, lint, and targeted responsive browser checks.
- [x] Validate Compose configuration and Redis health.
- [ ] Commit and push all intended backend changes.
- [ ] Commit and push all intended frontend changes.
- [ ] Verify remote branch tips and clean working trees.

### Acceptance criteria

- Redis is the shared, ephemeral store for quote caching and one-time WebAuthn ceremonies.
- Browser locale detection is normalized and propagated as `Accept-Language`.
- Responsive checks pass at 320, 375, 768, and 1280 pixels without horizontal overflow.
- Verification limitations are recorded in the implementation plan.

### Working notes

- PostgreSQL remains the durable source of truth.
- The existing full E2E journey suite needs a clean demo database because the shared database is already passkey/MFA-mutated.
- Docker image rebuild is environment-limited by the local Buildx/Maven build stage; Maven and Compose checks are available.
