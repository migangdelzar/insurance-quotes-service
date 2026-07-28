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

report_path="${RUNTIME_REPORT_PATH:-runtime-comparison.md}"
declare -a rows=()

cleanup() {
  "${compose[@]}" \
    -f deployment/compose/docker-compose.yml \
    -f deployment/compose/docker-compose.jvm.yml down --volumes --remove-orphans >/dev/null 2>&1 || true
  "${compose[@]}" \
    -f deployment/compose/docker-compose.yml \
    -f deployment/compose/docker-compose.native.yml down --volumes --remove-orphans >/dev/null 2>&1 || true
}
trap cleanup EXIT

measure() {
  local runtime="$1"
  local overlay="deployment/compose/docker-compose.${runtime}.yml"
  local image="insurance-quotes-api:${runtime}"

  if [ "$runtime" = "native" ] && ! docker image inspect "$image" >/dev/null 2>&1; then
    echo "${runtime}: unavailable; build the image first with 'mise run native'" >&2
    return 0
  fi

  local started_epoch
  started_epoch=$(date +%s)
  mise run up "$runtime" >/dev/null

  local container_id
  container_id=$("${compose[@]}" -f deployment/compose/docker-compose.yml -f "$overlay" ps -q api)
  if [ -z "$container_id" ]; then
    echo "Unable to resolve the ${runtime} API container" >&2
    return 1
  fi

  local health_seconds=""
  for attempt in $(seq 1 60); do
    health_seconds=$(curl --silent --show-error --output /dev/null \
      --write-out '%{time_total}' http://localhost:8080/actuator/health || true)
    if curl --fail --silent http://localhost:8080/actuator/health | grep -q '"status":"UP"'; then
      break
    fi
    if [ "$attempt" -eq 60 ]; then
      echo "${runtime} API did not become healthy" >&2
      "${compose[@]}" -f deployment/compose/docker-compose.yml -f "$overlay" logs api >&2
      return 1
    fi
    sleep 1
  done

  local startup
  startup=$(docker logs "$container_id" 2>&1 | grep -o 'Started Application in [0-9.]*' | tail -1 || true)
  local rss
  rss=$(docker stats --no-stream --format '{{.MemUsage}}' "$container_id")
  local image_ref
  image_ref=$(docker inspect -f '{{.Config.Image}}' "$container_id")
  local image_size
  image_size=$(docker image inspect -f '{{.Size}}' "$image_ref" 2>/dev/null || echo "unavailable")
  local elapsed_seconds
  elapsed_seconds=$(( $(date +%s) - started_epoch ))

  rows+=("| ${runtime} | ${startup:-not reported} | ${elapsed_seconds}s | ${health_seconds:-not reported}s | ${rss} | ${image_size} bytes |")
  echo "${runtime}: startup=${startup:-not reported}, compose_elapsed=${elapsed_seconds}s, health=${health_seconds:-not reported}s, rss=${rss}, image=${image_size} bytes"

  "${compose[@]}" -f deployment/compose/docker-compose.yml -f "$overlay" down --volumes --remove-orphans >/dev/null
}

echo "Measuring JVM runtime"
measure jvm
echo "Measuring native runtime"
measure native

{
  echo "# JVM vs native runtime comparison"
  echo
  echo "Generated on $(date -u '+%Y-%m-%dT%H:%M:%SZ'). RSS is the Docker container resident-memory reading at the health-check point."
  echo
  echo "| Runtime | Spring startup log | Compose elapsed | Health latency | RSS | Image size |"
  echo "|---|---:|---:|---:|---:|---:|"
  printf '%s\n' "${rows[@]}"
} > "$report_path"

echo "Wrote ${report_path}"
