# Demo State Reset Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans or superpowers:subagent-driven-development to implement this plan task-by-task.

**Goal:** Add a guarded `mise run reset-demo` command that recreates the local demo dependencies and starts the JVM full-stack demo from clean state.

**Architecture:** Keep reset behavior outside the Spring application. A shell script resolves the available Compose CLI, checks explicit confirmation and non-production profiles, then runs the existing Compose stop/start workflow. The application has no reset endpoint and no automatic reset behavior.

**Tech Stack:** Bash, Docker Compose, mise, PostgreSQL named volume, Redis container state, Kafka container state.

## Global Constraints

- The command is local-demo-only and must refuse profiles containing `prod`.
- A destructive reset requires the exact confirmation value `reset`.
- The command must use the existing JVM/full-stack Compose files and existing `mise run up jvm full e2e` workflow.
- Do not modify or stage the in-progress per-user quote ownership migration.
- Do not run validation until the user authorizes it after the user-isolation changes settle.

---

### Task 1: Add the guarded reset script

**Files:**
- Create: `scripts/reset-demo.sh`
- Test: deferred manual verification after user-isolation work completes

**Interfaces:**
- Consumes: `docker compose` or `docker-compose`; optional `DEMO_RESET_CONFIRM`; optional `SPRING_PROFILES_ACTIVE`.
- Produces: a clean local Compose demo and a restarted stack.

- [ ] **Step 1: Create the script with explicit safety checks**

  The script must use `set -euo pipefail`, resolve the Compose executable, reject
  `SPRING_PROFILES_ACTIVE` values containing `prod`, and require either an
  interactive `reset` confirmation or `DEMO_RESET_CONFIRM=reset`.

- [ ] **Step 2: Implement reset and restart**

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

- [ ] **Step 3: Make the script executable**

  ```bash
  chmod +x scripts/reset-demo.sh
  ```

- [ ] **Step 4: Commit**

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

- [ ] **Step 1: Add the task definition**

  Add a `reset-demo` task that executes `./scripts/reset-demo.sh` from the
  repository root and preserves the caller's environment variables.

- [ ] **Step 2: Commit**

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

- [ ] **Step 1: Add the interactive command**

  Document:

  ```bash
  mise run reset-demo
  ```

  Explain that the command asks for `reset`, deletes local demo data, and
  restarts the stack.

- [ ] **Step 2: Add the non-interactive command**

  Document:

  ```bash
  DEMO_RESET_CONFIRM=reset mise run reset-demo
  ```

  State that this is suitable for a local scripted setup but not for
  production.

- [ ] **Step 3: Commit**

  ```bash
  git add README.md docs/superpowers/specs/2026-07-28-demo-state-reset-design.md
  git commit -m "docs: document demo state reset command"
  ```

### Task 4: Deferred verification after user isolation

**Files:**
- No source changes expected.

- [ ] **Step 1: Verify refusal without confirmation**

  Run `mise run reset-demo` non-interactively without confirmation and expect a
  non-zero exit without Compose teardown.

- [ ] **Step 2: Verify production refusal**

  Run `SPRING_PROFILES_ACTIVE=prod DEMO_RESET_CONFIRM=reset mise run reset-demo`
  and expect a non-zero exit before Compose teardown.

- [ ] **Step 3: Verify clean restart**

  Run `DEMO_RESET_CONFIRM=reset mise run reset-demo`, wait for the health check,
  and verify the browser can log in with the seeded demo account.

- [ ] **Step 4: Verify state boundaries**

  Confirm old PostgreSQL quotes and passkeys are gone, Redis starts empty, and
  Kafka starts with no stale demo publications.

- [ ] **Step 5: Commit verification notes**

  Record the commands and results in the implementation PR description or
  project setup documentation. Do not run this task until the user-isolation
  migration is ready.

## Definition of Done

- [ ] `mise run reset-demo` is documented and guarded.
- [ ] Production profiles are rejected.
- [ ] Reset requires exact confirmation.
- [ ] PostgreSQL, Redis, and Kafka demo state are recreated through Compose.
- [ ] The demo restarts using the existing JVM/full-stack/e2e overlays.
- [ ] Verification is completed after user-isolation work is ready.
- [ ] Only reset-related files are committed; concurrent migration work remains untouched.
