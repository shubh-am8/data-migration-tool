#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=lib/common.sh
source "${SCRIPT_DIR}/lib/common.sh"

MODE="all"
KEEP_INFRA=false

usage() {
  cat <<EOF
Usage: ./run-local-dev.sh [OPTIONS]

Run the Data Migration Platform in local development mode.

Options:
  --frontend     Start Next.js dev server only (API must already be running)
  --backend      Start infra + API + Worker (no frontend)
  --keep-infra   On exit, leave Postgres/Redis running
  --help         Show this help

With no flags: starts infra + API + Worker + Frontend (full stack).

Examples:
  ./run-local-dev.sh
  ./run-local-dev.sh --backend
  ./run-local-dev.sh --frontend
EOF
}

parse_args() {
  while [[ $# -gt 0 ]]; do
    case "$1" in
      --frontend) MODE="frontend" ;;
      --backend)  MODE="backend" ;;
      --keep-infra) KEEP_INFRA=true ;;
      --help|-h)  usage; exit 0 ;;
      *) echo "Unknown option: $1"; usage; exit 1 ;;
    esac
    shift
  done
}

build_backend() {
  echo "Building backend modules..."
  (cd "${ROOT_DIR}" && mvn -q -pl services/api,services/worker,connectors/postgresql -am install -DskipTests)
  mkdir -p "${ROOT_DIR}/data/plugins/bundled" "${ROOT_DIR}/data/plugins/installed"
  PG_JAR="${ROOT_DIR}/connectors/postgresql/target/postgresql-connector-0.1.0-SNAPSHOT.jar"
  if [[ -f "${PG_JAR}" ]]; then
    cp -f "${PG_JAR}" "${ROOT_DIR}/data/plugins/bundled/postgresql.jar"
    echo "Seeded data/plugins/bundled/postgresql.jar"
  fi
}

clear_stale_infra_ports() {
  local port pid
  for port in 5432 6379; do
    local pids
    pids="$(pids_on_port "${port}")"
    [[ -z "${pids}" ]] && continue

    local safe=true
    while read -r pid; do
      [[ -z "${pid}" ]] && continue
      if ! listener_is_project_infra "${port}" "${pid}"; then
        safe=false
        echo "Refusing to kill non-project listener on :${port} (PID ${pid})."
      fi
    done <<< "${pids}"

    if [[ "${safe}" == "true" ]]; then
      kill_port_listeners "${port}"
    else
      assert_ports_free "${port}"
    fi
  done
}

prepare_dev_stack() {
  local mode=$1
  case "${mode}" in
    frontend)
      kill_port_listeners 3000
      assert_ports_free 3000
      ;;
    backend)
      stop_infra
      clear_stale_infra_ports
      kill_port_listeners 8080 8081
      assert_ports_free 5432 6379 8080 8081
      ;;
    all)
      stop_infra
      clear_stale_infra_ports
      kill_port_listeners 8080 8081 3000
      assert_ports_free 5432 6379 8080 8081 3000
      ;;
  esac
}

start_api() {
  echo "Starting API (port 8080)..."
  (cd "${ROOT_DIR}/services/api" && PLUGINS_DIR="${ROOT_DIR}/data/plugins" mvn -q spring-boot:run) &
  record_pid $!
  wait_for_port localhost 8080 120 || { echo "API failed to start on :8080"; exit 1; }
}

start_worker() {
  echo "Starting Worker (port 8081)..."
  (cd "${ROOT_DIR}/services/worker" && PLUGINS_DIR="${ROOT_DIR}/data/plugins" mvn -q spring-boot:run) &
  record_pid $!
  wait_for_port localhost 8081 120 || { echo "Worker failed to start on :8081"; exit 1; }
}

start_frontend() {
  echo "Starting Frontend (port 3000)..."
  (cd "${ROOT_DIR}/apps/web" && npm run dev) &
  record_pid $!
  wait_for_port localhost 3000 60 || { echo "Frontend failed to start on :3000"; exit 1; }
}

on_exit() {
  echo ""
  echo "Shutting down..."
  cleanup_pids
  if [[ "${KEEP_INFRA}" == "false" && "${MODE}" != "frontend" ]]; then
    stop_infra || true
  fi
}

main() {
  parse_args "$@"
  load_env
  ensure_dev_dir
  trap on_exit INT TERM EXIT
  prepare_dev_stack "${MODE}"

  case "${MODE}" in
    frontend)
      start_frontend
      echo "Frontend running: http://localhost:3000"
      wait
      ;;
    backend)
      start_infra
      build_backend
      start_api
      start_worker
      echo "Backend running. API: http://localhost:8080  Worker: http://localhost:8081"
      wait
      ;;
    all)
      start_infra
      build_backend
      start_api
      start_worker
      start_frontend
      echo ""
      echo "Full stack running:"
      echo "  Web:    http://localhost:3000"
      echo "  API:    http://localhost:8080"
      echo "  Worker: http://localhost:8081"
      wait
      ;;
  esac
}

main "$@"
