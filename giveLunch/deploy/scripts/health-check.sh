#!/usr/bin/env bash

set -euo pipefail

HEALTHCHECK_URL="${1:-http://127.0.0.1/actuator/health}"
MAX_ATTEMPTS="${2:-20}"
SLEEP_SECONDS="${3:-3}"

attempt=1
while [[ "${attempt}" -le "${MAX_ATTEMPTS}" ]]; do
  if curl --fail --silent --show-error "${HEALTHCHECK_URL}" >/dev/null; then
    echo "Health check passed: ${HEALTHCHECK_URL}"
    exit 0
  fi

  echo "Health check attempt ${attempt}/${MAX_ATTEMPTS} failed"
  sleep "${SLEEP_SECONDS}"
  attempt=$((attempt + 1))
done

echo "Health check failed after ${MAX_ATTEMPTS} attempts"
exit 1
