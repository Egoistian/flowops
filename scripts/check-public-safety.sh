#!/usr/bin/env bash
set -euo pipefail

flowops_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd -P)"
cd "$flowops_root"

rules=(
  'private-key::BEGIN (RSA |EC |OPENSSH )?PRIVATE KEY'
  'local-home-path::/Users/[^/]+/|/home/[^/]+/|C:\\Users\\[^\\]+\\'
  'revenue-workflow-file::daily_revenue_tracker|approval_queue|lead_queue|candidate_packets|next_run_handoff'
  'credential-shape::Authorization:[[:space:]]*Bearer|JSESSIONID=|AKIA[0-9A-Z]{16}|gh[pousr]_[A-Za-z0-9_]+'
)

failed=0
for entry in "${rules[@]}"; do
  rule="${entry%%::*}"
  pattern="${entry#*::}"
  files="$(rg -l -i --hidden \
    --glob '!.git/**' \
    --glob '!scripts/check-public-safety.sh' \
    --glob '!backend/build/**' \
    --glob '!frontend/dist/**' \
    --glob '!**/node_modules/**' \
    "$pattern" . || true)"
  if [[ -n "$files" ]]; then
    printf 'FAIL %s\n%s\n' "$rule" "$files"
    failed=1
  fi
done

email_files="$(rg -l --pcre2 --hidden \
  --glob '!.git/**' \
  --glob '!**/node_modules/**' \
  --glob '!frontend/dist/**' \
  '(?i)[a-z0-9._%+-]+@(?!northstar\.example\.com|acme\.example\.com|example\.com)[a-z0-9.-]+\.[a-z]{2,}' . || true)"
if [[ -n "$email_files" ]]; then
  printf 'FAIL non-example-email\n%s\n' "$email_files"
  failed=1
fi

tracked_forbidden="$(git ls-files \
  | rg '(^|/)(\.env($|\.)|.*\.log$|postgres-data|minio-data|playwright-report|test-results|trace\.zip$)' \
  | rg -v '(^|/)\.env\.example$' || true)"
if [[ -n "$tracked_forbidden" ]]; then
  printf 'FAIL forbidden-tracked-artifact\n%s\n' "$tracked_forbidden"
  failed=1
fi

history_paths="$(mktemp -t flowops-history-paths.XXXXXX)"
trap 'rm -f "$history_paths"' EXIT
for commit in $(git rev-list --all); do
  if git grep -I -l -E \
    'BEGIN (RSA |EC |OPENSSH )?PRIVATE KEY|/Users/[^/]+/|daily_revenue_tracker|candidate_packets|Authorization:[[:space:]]*Bearer' \
    "$commit" -- . ':(exclude)scripts/check-public-safety.sh' >"$history_paths" 2>/dev/null; then
    printf 'FAIL git-history commit=%s\n' "$commit"
    sed 's/^[^:]*://' "$history_paths" | sort -u
    failed=1
  fi
done

if [[ "$failed" -eq 0 ]]; then
  printf 'PASS public-safety\n'
fi
exit "$failed"
