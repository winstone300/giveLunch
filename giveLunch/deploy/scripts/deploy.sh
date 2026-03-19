#!/usr/bin/env bash

set -euo pipefail

APP_DIR="${APP_DIR:-/opt/givelunch}"
RELEASES_DIR="${RELEASES_DIR:-${APP_DIR}/releases}"
CURRENT_LINK="${CURRENT_LINK:-${APP_DIR}/current}"
BACKUP_DIR="${BACKUP_DIR:-${APP_DIR}/backup}"
SERVICE_NAME="${SERVICE_NAME:-givelunch}"
HEALTHCHECK_URL="${HEALTHCHECK_URL:-http://127.0.0.1/actuator/health}"
ARTIFACT_PATH="${1:?usage: deploy.sh <artifact-path>}"

mkdir -p "${RELEASES_DIR}" "${BACKUP_DIR}"

timestamp="$(date +%Y%m%d%H%M%S)"
release_dir="${RELEASES_DIR}/${timestamp}"
artifact_name="$(basename "${ARTIFACT_PATH}")"

mkdir -p "${release_dir}"
cp "${ARTIFACT_PATH}" "${release_dir}/${artifact_name}"

if [[ -L "${CURRENT_LINK}" ]] || [[ -e "${CURRENT_LINK}" ]]; then
  current_target="$(readlink -f "${CURRENT_LINK}")"
  if [[ -n "${current_target}" && -e "${current_target}" ]]; then
    cp "${current_target}"/*.jar "${BACKUP_DIR}/" 2>/dev/null || true
  fi
fi

ln -sfn "${release_dir}" "${CURRENT_LINK}"
sudo systemctl restart "${SERVICE_NAME}"

"$(dirname "$0")/health-check.sh" "${HEALTHCHECK_URL}"

echo "Deployment completed with artifact ${artifact_name}"
