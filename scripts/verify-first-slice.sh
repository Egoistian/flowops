#!/usr/bin/env bash
set -euo pipefail

project_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd -P)"

if docker compose version >/dev/null 2>&1; then
  compose=(docker compose)
elif command -v docker-compose >/dev/null 2>&1; then
  compose=(docker-compose)
else
  printf 'FAIL docker-compose-unavailable\n' >&2
  exit 1
fi

docker_host="$(docker context inspect "$(docker context show)" --format '{{.Endpoints.docker.Host}}')"
if [[ "$docker_host" == unix://* ]]; then
  export DOCKER_HOST="$docker_host"
  export TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE='/var/run/docker.sock'
fi

printf 'VERIFY backend-tests\n'
(cd "$project_root/backend" && ./gradlew test --rerun-tasks --console=plain)

printf 'VERIFY frontend-lint-test-build\n'
(cd "$project_root/frontend" && npm run lint && npm test -- --run && npm run build)

printf 'VERIFY compose-build-health\n'
(cd "$project_root" && "${compose[@]}" up --build --detach)
flowops_ready=0
for attempt in {1..30}; do
  if curl -fsS 'http://127.0.0.1:4173/actuator/health' 2>/dev/null | grep -q '"status":"UP"'; then
    flowops_ready=1
    break
  fi
  sleep 1
done
if [[ "$flowops_ready" -ne 1 ]]; then
  printf 'FAIL compose-health-timeout\n' >&2
  exit 1
fi

printf 'VERIFY playwright\n'
(cd "$project_root/e2e" && npm test)

printf 'PASS first-slice\n'
