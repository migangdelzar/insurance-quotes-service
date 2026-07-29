# Mise environments

## First clone: trust the configuration

Mise intentionally requires an explicit trust decision because these files can
set environment variables and run tasks. From the workspace containing both
repositories, run this once per checkout:

```bash
mise trust -y insurance-quotes-service/mise.toml
mise trust -y insurance-quotes-service/.mise/config.local.toml
mise trust -y insurance-quotes-web/mise.toml
```

After that, `mise run demo` and directory activation work without the
`config.local.toml is not trusted` error. These commands trust only the files
used by the default local demo. If you intentionally select another backend
profile, review and trust its matching `.mise/config.<profile>.toml` file
first.

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
