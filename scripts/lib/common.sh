#!/usr/bin/env bash
# Shared helpers for dev scripts

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
DEV_DIR="${ROOT_DIR}/.dev"
PID_FILE="${DEV_DIR}/pids"
COMPOSE_FILE="${ROOT_DIR}/infra/docker-compose.dev.yml"

load_env() {
  if [[ -f "${ROOT_DIR}/.env" ]]; then
    set -a
    # shellcheck disable=SC1091
    source "${ROOT_DIR}/.env"
    set +a
  else
    echo "Warning: .env not found — using defaults. Copy .env.example to .env"
  fi
}

wait_for_port() {
  local host=$1 port=$2 max=${3:-60} i=0
  while ! (echo >/dev/tcp/"${host}"/"${port}") 2>/dev/null; do
    i=$((i + 1))
    if [[ $i -ge $max ]]; then
      echo "Timeout waiting for ${host}:${port}"
      return 1
    fi
    sleep 1
  done
}

ensure_dev_dir() {
  mkdir -p "${DEV_DIR}"
  : > "${PID_FILE}"
}

record_pid() {
  echo "$1" >> "${PID_FILE}"
}

cleanup_pids() {
  if [[ -f "${PID_FILE}" ]]; then
    while read -r pid; do
      [[ -n "${pid}" ]] && kill "${pid}" 2>/dev/null || true
    done < "${PID_FILE}"
    rm -f "${PID_FILE}"
  fi
}

start_infra() {
  echo "Starting Postgres + Redis..."
  docker compose -f "${COMPOSE_FILE}" up appdb redis -d
  wait_for_port localhost 5432 90
  wait_for_port localhost 6379 30
  echo "Infrastructure ready."
}

pids_on_port() {
  local port=$1
  lsof -ti :"${port}" 2>/dev/null | sort -u || true
}

kill_port_listeners() {
  local port pid pids remaining
  for port in "$@"; do
    pids="$(pids_on_port "${port}")"
    [[ -z "${pids}" ]] && continue
    echo "Stopping listeners on :${port}..."
    while read -r pid; do
      [[ -n "${pid}" ]] && kill "${pid}" 2>/dev/null || true
    done <<< "${pids}"
    sleep 1
    remaining="$(pids_on_port "${port}")"
    if [[ -n "${remaining}" ]]; then
      echo "Force-stopping stubborn listeners on :${port}: ${remaining}"
      while read -r pid; do
        [[ -n "${pid}" ]] && kill -9 "${pid}" 2>/dev/null || true
      done <<< "${remaining}"
    fi
  done
}

listener_is_project_infra() {
  local port=$1 pid=$2
  local comm args
  comm="$(ps -p "${pid}" -o comm= 2>/dev/null || true)"
  args="$(ps -p "${pid}" -o args= 2>/dev/null || true)"

  # ponytail: name-based heuristic for local dev; upgrade path = docker inspect by published port
  case "${comm}" in
    *docker*|*com.docker*|*OrbStack*|*vpnkit*) return 0 ;;
  esac

  if docker compose -f "${COMPOSE_FILE}" ps -q 2>/dev/null | grep -q .; then
    return 0
  fi

  case "${port}" in
    5432) [[ "${args}" == *postgres* && "${args}" == *migration* ]] && return 0 ;;
    6379) [[ "${args}" == *redis* ]] && return 0 ;;
  esac

  return 1
}

assert_ports_free() {
  local port pids
  for port in "$@"; do
    pids="$(pids_on_port "${port}")"
    if [[ -n "${pids}" ]]; then
      echo "Port :${port} is still in use (PIDs: ${pids})."
      echo "Stop the conflicting process or see docs/development.md#troubleshooting"
      return 1
    fi
  done
}

stop_infra() {
  echo "Stopping Postgres + Redis..."
  docker compose -f "${COMPOSE_FILE}" down --remove-orphans
}
