#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
SCRIPT="${ROOT}/scripts/run-local-dev.sh"

fail() { echo "FAIL: $1"; exit 1; }
pass() { echo "PASS: $1"; }

# --help exits 0
"${SCRIPT}" --help >/dev/null || fail "--help should exit 0"
pass "--help exits 0"

# unknown flag exits 1
if "${SCRIPT}" --bogus 2>/dev/null; then
  fail "unknown flag should exit 1"
else
  pass "unknown flag exits 1"
fi

for fn in pids_on_port kill_port_listeners listener_is_project_infra assert_ports_free; do
  grep -q "${fn}()" "${ROOT}/scripts/lib/common.sh" || fail "common.sh must define ${fn}"
done
pass "common.sh defines port helper functions"

# ponytail: guard against broken root-level spring-boot:run on aggregator POM
if grep -q 'mvn.*-pl services/api -am spring-boot:run' "${ROOT}/scripts/run-local-dev.sh"; then
  fail "run-local-dev.sh must not run spring-boot:run from root aggregator"
else
  pass "spring-boot:run invoked from service module dirs"
fi

grep -q 'prepare_dev_stack()' "${ROOT}/scripts/run-local-dev.sh" || fail "run-local-dev.sh must define prepare_dev_stack"
grep -q 'prepare_dev_stack "${MODE}"' "${ROOT}/scripts/run-local-dev.sh" || fail "main must call prepare_dev_stack before starts"
pass "prepare_dev_stack wired in main flow"

if grep -n 'stop_infra' "${ROOT}/scripts/run-local-dev.sh" | grep -v '#' | grep -q 'prepare_dev_stack\|on_exit'; then
  # will refine after implement — for RED, assert default KEEP_INFRA=true
  :
fi
if ! grep -q 'KEEP_INFRA=true' "${ROOT}/scripts/run-local-dev.sh"; then
  fail "KEEP_INFRA must default to true so Docker survives app exit"
else
  pass "KEEP_INFRA defaults to true"
fi
if grep -A20 'prepare_dev_stack()' "${ROOT}/scripts/run-local-dev.sh" | grep -q 'stop_infra'; then
  fail "prepare_dev_stack must not call stop_infra"
else
  pass "prepare_dev_stack leaves Docker alone"
fi
if grep -A15 'on_exit()' "${ROOT}/scripts/run-local-dev.sh" | grep -q 'stop_infra' && ! grep -q 'STOP_INFRA_ON_EXIT' "${ROOT}/scripts/run-local-dev.sh"; then
  fail "on_exit must not stop infra unless STOP_INFRA_ON_EXIT"
else
  pass "on_exit gated by STOP_INFRA_ON_EXIT"
fi

grep -q 'rm -rf "${ROOT_DIR}/apps/web/.next"' "${ROOT}/scripts/run-local-dev.sh" || fail "start_frontend must clear apps/web/.next before next dev"
pass "start_frontend clears apps/web/.next cache"

grep -A8 'case "${MODE}"' "${ROOT}/scripts/run-local-dev.sh" | grep -q 'wait' || fail "frontend mode must wait for dev server"
pass "frontend mode waits for dev server"

if ! grep -q 'kill -9' "${ROOT}/scripts/lib/common.sh"; then
  fail "port cleanup must force-kill stubborn processes when needed"
else
  pass "port cleanup has force-kill fallback in common.sh"
fi

echo "All script tests passed."
