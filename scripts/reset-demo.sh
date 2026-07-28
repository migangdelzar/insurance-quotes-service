#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "$0")/.." && pwd)"
cd "$repo_root"

profiles="${SPRING_PROFILES_ACTIVE:-local}"
if [[ "${MISE_ENV:-}" == *prod* ]]; then
  echo "Refusing to reset a production mise environment." >&2
  exit 1
fi

IFS=',' read -r -a active_profiles <<< "$profiles"
for profile in "${active_profiles[@]}"; do
  if [[ "$profile" == *prod* ]]; then
    echo "Refusing to reset a production profile." >&2
    exit 1
  fi
done

if [[ "${DEMO_RESET_CONFIRM:-}" != "reset" ]]; then
  if [[ -t 0 ]]; then
    read -r -p "This deletes local demo data. Type 'reset' to continue: " confirmation
  else
    echo "DEMO_RESET_CONFIRM=reset is required for non-interactive demo resets." >&2
    exit 1
  fi

  if [[ "$confirmation" != "reset" ]]; then
    echo "Demo reset cancelled." >&2
    exit 1
  fi
fi

if docker compose version >/dev/null 2>&1; then
  compose=(docker compose)
elif command -v docker-compose >/dev/null 2>&1; then
  compose=(docker-compose)
else
  echo "Docker Compose is required." >&2
  exit 1
fi

compose_files=(
  -f deployment/compose/docker-compose.yml
  -f deployment/compose/docker-compose.jvm.yml
  -f deployment/compose/compose.fullstack.yml
  -f deployment/compose/compose.e2e.yml
)

echo "Resetting the local Clara demo state: PostgreSQL, Redis, and Kafka."
"${compose[@]}" "${compose_files[@]}" down --volumes --remove-orphans

echo "Starting a clean JVM full-stack demo."
mise run up jvm full e2e

cat <<'EOF'

Clara has been reset and restarted:
  Web:        http://localhost:3100
  API health: http://localhost:3100/api/actuator/health
EOF
