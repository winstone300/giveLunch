#!/usr/bin/env bash

set -euo pipefail

APP_DIR="${APP_DIR:-/opt/givelunch}"
RELEASES_DIR="${RELEASES_DIR:-${APP_DIR}/releases}"
CURRENT_LINK="${CURRENT_LINK:-${APP_DIR}/current}"
SERVICE_NAME="${SERVICE_NAME:-givelunch}"
HEALTHCHECK_URL="${HEALTHCHECK_URL:-http://127.0.0.1/actuator/health}"
TARGET_RELEASE="${1:-}"

if [[ -z "${TARGET_RELEASE}" ]]; then
  TARGET_RELEASE="$(find "${RELEASES_DIR}" -mindepth 1 -maxdepth 1 -type d | sort | tail -n 2 | head -n 1)"
fi

if [[ -z "${TARGET_RELEASE}" || ! -d "${TARGET_RELEASE}" ]]; then
  echo "Rollback target not found"
  exit 1
fi

ln -sfn "${TARGET_RELEASE}" "${CURRENT_LINK}"
sudo systemctl restart "${SERVICE_NAME}"
"$(dirname "$0")/health-check.sh" "${HEALTHCHECK_URL}"

echo "Rollback completed to ${TARGET_RELEASE}"
