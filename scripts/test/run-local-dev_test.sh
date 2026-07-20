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


# frontend mode must invoke stale-port cleanup
if ! grep -q 'frontend).*clear_stale_dev_ports_for_mode "frontend"' "${ROOT}/scripts/run-local-dev.sh"; then
  fail "frontend mode must clear stale frontend process/port before start"
else
  pass "frontend mode clears stale frontend process/port"
fi

# cleanup must have force-kill fallback for stubborn processes
if ! grep -q 'kill -9' "${ROOT}/scripts/run-local-dev.sh"; then
  fail "stale port cleanup must force-kill stubborn processes when needed"
else
  pass "stale port cleanup has force-kill fallback"
fi

echo "All script tests passed."

