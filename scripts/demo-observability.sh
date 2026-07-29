#!/usr/bin/env bash
set -euo pipefail

mode="${1:-follow}"
tail_lines="${LOG_TAIL:-200}"
since="${LOG_SINCE:-15m}"
compose_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/../deployment/compose" && pwd)"

if docker compose version >/dev/null 2>&1; then
  compose=(docker compose)
elif command -v docker-compose >/dev/null 2>&1; then
  compose=(docker-compose)
else
  echo "Docker Compose is required" >&2
  exit 1
fi

compose_files=(
  -f "$compose_dir/docker-compose.yml"
  -f "$compose_dir/docker-compose.jvm.yml"
  -f "$compose_dir/compose.fullstack.yml"
  -f "$compose_dir/compose.e2e.yml"
)

compose_cmd() {
  "${compose[@]}" "${compose_files[@]}" "$@"
}

api_container="$(compose_cmd ps -q api 2>/dev/null || true)"
if [[ -z "$api_container" ]]; then
  echo "The JVM demo API is not running. Start it with: mise run demo" >&2
  exit 1
fi

case "$mode" in
  follow)
    compose_cmd logs --tail="$tail_lines" --follow api
    ;;
  all)
    compose_cmd logs --tail="$tail_lines" --follow
    ;;
  errors)
    compose_cmd logs --tail="$tail_lines" api 2>&1 \
      | grep -Ei '"level":"(WARN|ERROR)"|\b(WARN|ERROR)\b|Exception|failed|refused|timeout' \
      || true
    ;;
  check)
    failed=0
    compose_cmd ps

    if curl --fail --silent --show-error --max-time 5 \
      http://localhost:3100/api/actuator/health >/dev/null; then
      echo "PASS API health: http://localhost:3100/api/actuator/health"
    else
      echo "FAIL API health is not UP" >&2
      failed=1
    fi

    insurer_url="$(docker inspect --format '{{range .Config.Env}}{{println .}}{{end}}' "$api_container" \
      | sed -n 's/^INSURER_BASE_URL=//p' | head -1)"
    tracing_enabled="$(docker inspect --format '{{range .Config.Env}}{{println .}}{{end}}' "$api_container" \
      | sed -n 's/^OTEL_SDK_ENABLED=//p' | head -1)"
    echo "Insurer boundary: ${insurer_url:-not exposed}"
    echo "Tracing exporter: ${tracing_enabled:-application default}"

    if [[ "$insurer_url" == *wiremock* ]]; then
      echo "PASS deterministic insurer: WireMock"
    elif [[ "$insurer_url" == *httpstat.us* ]]; then
      echo "INFO external insurer: httpstat.us"
    else
      echo "WARN insurer endpoint is not recognized: ${insurer_url:-unknown}"
    fi

    recent_logs="$(compose_cmd logs --since="$since" api 2>&1 || true)"
    if printf '%s\n' "$recent_logs" | grep -Eiq \
      'Failed to export spans|UnknownHostException: tempo|Failed to publish metrics to OTLP receiver|Unable to acquire JDBC|Flyway.*(error|failed)|KafkaException|RedisConnection.*(exception|failure)'; then
      echo "FAIL infrastructure errors found in recent API logs:" >&2
      printf '%s\n' "$recent_logs" \
        | grep -Ei 'Failed to export spans|UnknownHostException: tempo|Failed to publish metrics to OTLP receiver|Unable to acquire JDBC|Flyway.*(error|failed)|KafkaException|RedisConnection.*(exception|failure)' \
        | tail -20 >&2
      failed=1
    else
      echo "PASS no known infrastructure misconfiguration in the last ${since}"
    fi

    if [[ "$tracing_enabled" == "true" ]]; then
      if compose_cmd ps --status running tempo 2>/dev/null | grep -q tempo; then
        echo "PASS Tempo is running for trace export"
      else
        echo "FAIL tracing is enabled but the Tempo service is not running" >&2
        failed=1
      fi
    else
      echo "INFO tracing export is disabled for the normal demo"
    fi

    echo "Note: 4xx responses for invalid credentials or an unregistered passkey can be expected user-flow errors."
    exit "$failed"
    ;;
  *)
    echo "Usage: $0 [follow|all|errors|check]" >&2
    exit 2
    ;;
esac
