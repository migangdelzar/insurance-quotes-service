#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."

if docker compose version >/dev/null 2>&1; then
  compose=(docker compose)
elif command -v docker-compose >/dev/null 2>&1; then
  compose=(docker-compose)
else
  echo "Docker Compose is required" >&2
  exit 1
fi

measure() {
  local runtime="$1"

  if [ "$runtime" = "native" ] && ! docker image inspect insurance-quotes-api:native >/dev/null 2>&1; then
    echo "$runtime | unavailable | unavailable (build image first with: mise run native)"
    return 0
  fi

  mise run up "$runtime" >/dev/null

  until curl -fsS http://localhost:8080/actuator/health >/dev/null; do
    sleep 1
  done

  local startup
  startup=$(docker logs insurance-quotes-api-1 2>&1 | grep -o 'Started Application in [0-9.]*' | tail -1 || true)
  local rss
  rss=$(docker stats --no-stream --format '{{.MemUsage}}' insurance-quotes-api-1)
  echo "$runtime | ${startup:-not reported} | $rss"

  "${compose[@]}" \
    -f deployment/compose/docker-compose.yml \
    -f "deployment/compose/docker-compose.${runtime}.yml" down >/dev/null
}

echo "runtime | startup | memory"
measure jvm
measure native
