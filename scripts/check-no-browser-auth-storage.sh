#!/usr/bin/env bash
set -euo pipefail

flowops_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd -P)"
matches="$(rg -l 'localStorage|sessionStorage|Authorization.{0,20}Bearer|persist(Auth|Session|Token)' "$flowops_root/frontend/src" || true)"
if [[ -n "$matches" ]]; then
  printf 'FAIL browser-auth-storage\n%s\n' "$matches"
  exit 1
fi
printf 'PASS browser-auth-storage\n'
