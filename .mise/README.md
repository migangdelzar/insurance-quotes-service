# Mise environments

## First clone: trust the configuration

Mise intentionally requires an explicit trust decision because these files can
set environment variables and run tasks. From the workspace containing both
repositories, run this once per checkout:

```bash
mise trust -y --all -C insurance-quotes-service
mise trust -y --all -C insurance-quotes-web
```

After that, `mise run demo` and directory activation work without the
`config.local.toml is not trusted` error. Only trust repositories whose
configuration you have reviewed.

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
