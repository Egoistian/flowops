#!/usr/bin/env bash
set -euo pipefail

project_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd -P)"
fixture_bin="$project_root/scripts/test/fixtures"

result="$(PATH="$fixture_bin:/usr/bin:/bin" "$project_root/scripts/check-local-demo-network.sh")"
if [[ "$result" != "PASS local-demo-network" ]]; then
  printf 'unexpected result: %s\n' "$result" >&2
  exit 1
fi

printf 'PASS compose-plugin-compatibility\n'
