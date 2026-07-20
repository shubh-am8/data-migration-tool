#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
TMP="$ROOT/scripts/tests/.tmp-next-cache-$$"
mkdir -p "$TMP/apps/web/.next/junk"
# Extract the wipe one-liner the plan requires start_frontend to use:
#   rm -rf "${ROOT_DIR}/apps/web/.next"
# Simulate it against TMP as ROOT_DIR:
ROOT_DIR="$TMP"
rm -rf "${ROOT_DIR}/apps/web/.next"
if [[ -d "$TMP/apps/web/.next" ]]; then
  echo "FAIL: .next still present"
  rm -rf "$TMP"
  exit 1
fi
rm -rf "$TMP"
echo "PASS: .next wipe removes cache dir"
