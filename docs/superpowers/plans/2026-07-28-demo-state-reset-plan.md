# Demo State Reset Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans or superpowers:subagent-driven-development to implement this plan task-by-task.

**Goal:** Add a guarded `mise run reset-demo` command that recreates the local demo dependencies and starts the JVM full-stack demo from clean state.
**Status:** Complete

**Architecture:** Keep reset behavior outside the Spring application. A shell script resolves the available Compose CLI, checks explicit confirmation and non-production profiles, then runs the existing Compose stop/start workflow. The application has no reset endpoint and no automatic reset behavior.

**Tech Stack:** Bash, Docker Compose, mise, PostgreSQL named volume, Redis container state, Kafka container state.

## Global Constraints

- The command is local-demo-only and must refuse profiles and mise
  environments containing `prod`.
- A destructive reset requires the exact confirmation value `reset`.
- The command must use the existing JVM/full-stack Compose files and existing `mise run up jvm full e2e` workflow.
- The per-user quote ownership migration must preserve existing data by
  backfilling legacy rows before enforcing the new non-null foreign key.
- Validation is required after the user-isolation changes settle.

---

### Task 1: Add the guarded reset script

**Files:**
- Create: `scripts/reset-demo.sh`
- Test: deferred manual verification after user-isolation work completes

**Interfaces:**
- Consumes: `docker compose` or `docker-compose`; optional `DEMO_RESET_CONFIRM`; optional `MISE_ENV` or `SPRING_PROFILES_ACTIVE`.
- Produces: a clean local Compose demo and a restarted stack.

- [x] **Step 1: Create the script with explicit safety checks**

  The script must use `set -euo pipefail`, resolve the Compose executable, reject
  `MISE_ENV` or `SPRING_PROFILES_ACTIVE` value containing `prod`, and require either an
  interactive `reset` confirmation or `DEMO_RESET_CONFIRM=reset`.

- [x] **Step 2: Implement reset and restart**

  From the backend repository root, run the equivalent of:

  ```bash
  cd deployment/compose
  "${compose[@]}" \
    -f docker-compose.yml \
    -f docker-compose.jvm.yml \
    -f compose.fullstack.yml \
    -f compose.e2e.yml \
    down --volumes --remove-orphans
  cd ../..
  mise run up jvm full e2e
  ```

  Print the reset scope and the resulting application URL.

- [x] **Step 3: Make the script executable**

  ```bash
  chmod +x scripts/reset-demo.sh
  ```

- [x] **Step 4: Commit**

  ```bash
  git add scripts/reset-demo.sh
  git commit -m "feat(dev): add guarded demo state reset script"
  ```

### Task 2: Expose the reset through mise

**Files:**
- Modify: `mise.toml`

**Interfaces:**
- Consumes: `scripts/reset-demo.sh`.
- Produces: `mise run reset-demo` as the single documented reset command.

- [x] **Step 1: Add the task definition**

  Add a `reset-demo` task that executes `./scripts/reset-demo.sh` from the
  repository root and preserves the caller's environment variables.

- [x] **Step 2: Commit**

  ```bash
  git add mise.toml
  git commit -m "chore(dev): expose demo reset through mise"
  ```

### Task 3: Document reset usage and safety

**Files:**
- Modify: `README.md`
- Modify: `docs/superpowers/specs/2026-07-28-demo-state-reset-design.md`

**Interfaces:**
- Consumes: `mise run reset-demo` and `DEMO_RESET_CONFIRM=reset`.
- Produces: reviewer-facing setup and recovery instructions.

- [x] **Step 1: Add the interactive command**

  Document:

  ```bash
  mise run reset-demo
  ```

  Explain that the command asks for `reset`, deletes local demo data, and
  restarts the stack.

- [x] **Step 2: Add the non-interactive command**

  Document:

  ```bash
  DEMO_RESET_CONFIRM=reset mise run reset-demo
  ```

  State that this is suitable for a local scripted setup but not for
  production.

- [x] **Step 3: Commit**

  ```bash
  git add README.md docs/superpowers/specs/2026-07-28-demo-state-reset-design.md
  git commit -m "docs: document demo state reset command"
  ```

### Task 4: Verify reset after user isolation

**Files:**
- No source changes expected.

- [x] **Step 1: Verify refusal without confirmation**

  Run `mise run reset-demo` non-interactively without confirmation and expect a
  non-zero exit without Compose teardown.

- [x] **Step 2: Verify production refusal**

  Run `MISE_ENV=prod DEMO_RESET_CONFIRM=reset mise run reset-demo`
  and expect a non-zero exit before Compose teardown.

- [x] **Step 3: Verify clean restart**

  Run `DEMO_RESET_CONFIRM=reset mise run reset-demo`, wait for the health check,
  and verify the browser can log in with the seeded demo account.

- [x] **Step 4: Verify state boundaries**

  Confirm old PostgreSQL quotes and passkeys are gone, Redis starts empty, and
  Kafka starts with no stale demo publications.

- [x] **Step 5: Commit verification notes**

  Record the commands and results in the implementation PR description or
  project setup documentation. The verification was run after the
  user-isolation migration was ready.

## Definition of Done

- [x] `mise run reset-demo` is documented and guarded.
- [x] Production profiles are rejected.
- [x] Reset requires exact confirmation.
- [x] PostgreSQL, Redis, and Kafka demo state are recreated through Compose.
- [x] The demo restarts using the existing JVM/full-stack/e2e overlays.
- [x] Verification is completed after user-isolation work is ready.
- [x] Existing ownership data is safely backfilled during migration.
