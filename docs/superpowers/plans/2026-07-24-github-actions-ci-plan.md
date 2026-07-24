# GitHub Actions CI Plan

| Field | Detail |
|---|---|
| **Design specification** | [2026-07-24-github-actions-ci-design.md](../specs/2026-07-24-github-actions-ci-design.md) |
| **Date** | 2026-07-24 |
| **Status** | In progress |

## Tasks

| # | Task | Status |
|---|---|---|
| 1 | Add backend Java/Testcontainers/Docker/Compose CI workflow with branch-run cancellation. | ⬚ Not Started |
| 2 | Add backend full-stack JVM Compose smoke workflow with cleanup. | ⬚ Not Started |
| 3 | Add frontend Bun/test/lint/format/build/responsive-browser workflow with branch-run cancellation. | ⬚ Not Started |
| 4 | Document workflow triggers, port `3100`, and native-image policy. | ⬚ Not Started |
| 5 | Validate workflow YAML, local commands, Compose startup, and clean Git state. | ⬚ Not Started |

## Definition of done

- [ ] All workflows trigger on pushes and pull requests.
- [ ] Concurrency cancellation is configured in every workflow.
- [ ] Backend and frontend checks are represented in Actions.
- [ ] Full-stack JVM smoke verification is represented in Actions.
- [ ] Native-image files remain unchanged.
- [ ] Workflows and documentation are committed and pushed.
