#!/usr/bin/env bash
# Builds local marketplace dist assets (connector JARs + tool zips) for `app.marketplace.mode=local`
# (dev/CI, offline) and prints the SHA-256 lines to paste into marketplace/catalog.json.
#
# Usage: marketplace/scripts/build-dist.sh
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MARKETPLACE_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
ROOT_DIR="$(cd "${MARKETPLACE_DIR}/.." && pwd)"
DIST_DIR="${MARKETPLACE_DIR}/dist"

sha256_of() {
  if command -v sha256sum >/dev/null 2>&1; then
    sha256sum "$1" | awk '{print $1}'
  else
    shasum -a 256 "$1" | awk '{print $1}'
  fi
}

mkdir -p "${DIST_DIR}"

echo "== Building marketplace connector JAR(s) =="
# packages/connector-sdk must already be installed to the local repo (root reactor install).
mvn -f "${MARKETPLACE_DIR}/pom.xml" -q package -DskipTests

PG_JAR_SRC="$(find "${MARKETPLACE_DIR}/connectors/postgresql/target" -maxdepth 1 -name 'postgresql-connector-*.jar' ! -name '*-sources.jar' ! -name '*-javadoc.jar' | head -n1)"
if [[ -z "${PG_JAR_SRC}" ]]; then
  echo "postgresql-connector JAR not found under marketplace/connectors/postgresql/target" >&2
  exit 1
fi
PG_JAR_DEST="${DIST_DIR}/postgresql-connector-0.1.0.jar"
cp "${PG_JAR_SRC}" "${PG_JAR_DEST}"

echo "== Zipping lab-devtools tool =="
LAB_DEVTOOLS_ZIP="${DIST_DIR}/lab-devtools-0.1.0.zip"
rm -f "${LAB_DEVTOOLS_ZIP}"
(cd "${MARKETPLACE_DIR}/plugins/lab-devtools" && zip -qr "${LAB_DEVTOOLS_ZIP}" .)

PG_SHA="$(sha256_of "${PG_JAR_DEST}")"
LAB_SHA="$(sha256_of "${LAB_DEVTOOLS_ZIP}")"

echo ""
echo "== Dist artifacts =="
echo "${PG_JAR_DEST}"
echo "${LAB_DEVTOOLS_ZIP}"
echo ""
echo "== Update marketplace/catalog.json with =="
echo "postgresql:    \"sha256\": \"${PG_SHA}\""
echo "lab-devtools:  \"sha256\": \"${LAB_SHA}\""
echo ""
echo "(${ROOT_DIR}/marketplace/catalog.json)"
