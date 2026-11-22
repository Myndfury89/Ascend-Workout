#!/usr/bin/env bash
# Runs 07_age_check.sql against a THROWAWAY local Postgres (initdb in a temp dir) with a minimal
# Supabase shim. Never points at a real Supabase project. Requires postgres binaries (initdb, pg_ctl).
set -euo pipefail
here="$(cd "$(dirname "$0")" && pwd)"
sb="$here/.."
PGBIN="${PGBIN:-$(ls -d /usr/lib/postgresql/*/bin 2>/dev/null | sort -V | tail -1)}"
tmp="$(mktemp -d)"
port="${PGPORT_TEST:-55432}"
trap '"$PGBIN/pg_ctl" -D "$tmp/data" -m immediate stop >/dev/null 2>&1 || true; rm -rf "$tmp"' EXIT
"$PGBIN/initdb" -D "$tmp/data" -U postgres -A trust >/dev/null
"$PGBIN/pg_ctl" -D "$tmp/data" -o "-p $port -k $tmp -c listen_addresses=''" -l "$tmp/log" -w start >/dev/null
q() { PGOPTIONS="-c client_min_messages=warning" psql -X -q -v ON_ERROR_STOP=1 -h "$tmp" -p "$port" -U postgres -d postgres "$@"; }

q -f "$here/supabase_shim.sql"
q -f "$sb/p1/profiles.sql"
for f in 01_tables 02_functions 03_rls 04_lookup 05_cards 06_hardening 07_age_check 07_age_check; do
  q -f "$sb/p2a/$f.sql" # 07 applied twice: idempotency
done
# Re-running 07 must not reset a changed config row.
q -c "update public.age_check_config set enforcement_mode = 'log_only'"
q -f "$sb/p2a/07_age_check.sql"
q -tAc "select enforcement_mode from public.age_check_config" | grep -qx log_only
q -c "update public.age_check_config set enforcement_mode = 'off'"
echo "PASS: 07 idempotent; re-run preserves config"

PGOPTIONS="-c client_min_messages=notice" psql -X -q -v ON_ERROR_STOP=1 -h "$tmp" -p "$port" -U postgres \
  -d postgres -o /dev/null -f "$here/age_check_local_test.sql" 2>&1 | sed -E 's/^psql:[^ ]+ NOTICE: +//'
test "${PIPESTATUS[0]}" -eq 0

q -f "$sb/p2a/07_age_check_rollback.sql"
q -f "$sb/p2a/07_age_check_rollback.sql" # idempotent
n="$(q -tAc "select (select count(*) from pg_class where relname in ('age_checks','age_check_config'))
  + (select count(*) from pg_proc where proname like 'age_check_%')")"
[ "$n" = "0" ] || { echo "FAIL: rollback left objects"; exit 1; }
q -tAc "select count(*) from pg_trigger where tgname = 'profiles_guard_social_eligible'" | grep -qx 1
echo "PASS: rollback removes 07 objects only (06 trigger intact), idempotent"
q -f "$sb/p2a/07_age_check.sql"
echo "PASS: 07 re-applies cleanly after rollback"
