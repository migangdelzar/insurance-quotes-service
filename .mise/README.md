# Mise environments

Profile configuration is kept under `.mise/config.<environment>.toml` so
profile selection is explicit and does not silently overwrite a caller's
environment in the repository-wide task configuration.

Available environments:

- `local`: host-run Spring Boot with local defaults.
- `dev`: Spring DevTools, LiveReload, and WireMock-backed insurer calls.
- `test`: local test profile with WireMock-backed insurer calls.
- `prod`: production profile marker; it intentionally contains no demo
  credentials or local infrastructure values.

Select an environment for a command with `MISE_ENV`:

```bash
MISE_ENV=dev mise run dev
MISE_ENV=test mise run test
MISE_ENV=prod mise run reset-demo # refused by the reset safety guard
```

The standard `mise run demo` command supplies the Docker profile through the
Compose overlay and does not require a host profile selection.
