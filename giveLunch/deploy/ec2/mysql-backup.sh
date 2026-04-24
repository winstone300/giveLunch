#!/usr/bin/env bash
set -euo pipefail

ENV_FILE="${GIVELUNCH_ENV_FILE:-/etc/givelunch/givelunch.env}"
if [[ -f "$ENV_FILE" ]]; then
  set -a
  # shellcheck disable=SC1090
  source "$ENV_FILE"
  set +a
fi

DB_HOST="${GIVELUNCH_DB_HOST:-127.0.0.1}"
DB_NAME="${GIVELUNCH_DB_NAME:-givelunch}"
DB_USER="${SPRING_DATASOURCE_USERNAME:-givelunch}"
DB_PASSWORD="${SPRING_DATASOURCE_PASSWORD:-}"
BACKUP_DIR="${GIVELUNCH_BACKUP_DIR:-/var/backups/givelunch}"
RETENTION_DAYS="${GIVELUNCH_BACKUP_RETENTION_DAYS:-7}"
TIMESTAMP="$(date +%Y%m%d-%H%M%S)"
BACKUP_FILE="$BACKUP_DIR/givelunch-$TIMESTAMP.sql.gz"

install -d -m 750 "$BACKUP_DIR"

MYSQL_PWD="$DB_PASSWORD" mysqldump \
  --host="$DB_HOST" \
  --user="$DB_USER" \
  --single-transaction \
  --routines \
  --triggers \
  "$DB_NAME" | gzip > "$BACKUP_FILE"

find "$BACKUP_DIR" -type f -name 'givelunch-*.sql.gz' -mtime "+$RETENTION_DAYS" -delete

if [[ -n "${GIVELUNCH_BACKUP_S3_URI:-}" ]]; then
  aws s3 cp "$BACKUP_FILE" "$GIVELUNCH_BACKUP_S3_URI/"
fi

echo "Created backup: $BACKUP_FILE"
