#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
DIST="$ROOT/marketplace/dist"
PLUGIN_DIR="$ROOT/marketplace/plugins/lab-devtools"
ZIP="$DIST/lab-devtools-0.1.0.zip"

mkdir -p "$DIST"

# Connector JAR (if built)
if [[ -f "$ROOT/marketplace/connectors/postgresql/target/postgresql-connector-0.1.0.jar" ]]; then
  cp "$ROOT/marketplace/connectors/postgresql/target/postgresql-connector-0.1.0.jar" \
    "$DIST/postgresql-connector-0.1.0.jar"
fi

# Lab devtools TOOL zip
rm -f "$ZIP"
(cd "$PLUGIN_DIR" && zip -r "$ZIP" .)

echo "Built $ZIP"
if command -v shasum >/dev/null 2>&1; then
  echo "SHA-256: $(shasum -a 256 "$ZIP" | awk '{print $1}')"
elif command -v sha256sum >/dev/null 2>&1; then
  echo "SHA-256: $(sha256sum "$ZIP" | awk '{print $1}')"
fi
