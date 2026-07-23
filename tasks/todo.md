# Backend core refactor checkpoint

- [x] Standardize modules to DDD + Hexagonal Modulith boundaries.
- [x] Separate API contracts, application services, domain models, and directional adapters.
- [x] Keep domain exceptions transport-neutral and map them at inbound web boundaries.
- [x] Apply Lombok constructor/logging conventions without changing domain behavior.
- [x] Verify unit, integration, Modulith, Docker image, and Compose smoke-test behavior.

## Results

The backend now follows the responsibility-based package structure defined by the module templates. Quote lookup exceptions live in the application layer, domain rules remain framework-neutral, and HTTP error translation is isolated in inbound adapters.

