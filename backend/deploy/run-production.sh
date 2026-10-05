#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
stack_dir="${TNHC_SUPABASE_STACK_DIR:?Set TNHC_SUPABASE_STACK_DIR to the official self-hosted Supabase checkout}"

exec docker compose \
  --project-name tnhc-community-production \
  --env-file "$stack_dir/.env" \
  -f "$stack_dir/docker-compose.yml" \
  -f "$stack_dir/docker-compose.override.yml" \
  -f "$repo_root/backend/deploy/production-data-volumes.yml" \
  "$@"
