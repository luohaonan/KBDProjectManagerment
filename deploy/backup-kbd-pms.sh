#!/usr/bin/env bash
set -euo pipefail

# Weekly backup script for KBD PMS.
# It backs up both MySQL data and uploaded files, and keeps only the latest 2 backups.

APP_DIR="/opt/kbd-pms"
BACKUP_DIR="/opt/kbd-pms-backups"
KEEP_COUNT=2

cd "$APP_DIR"
mkdir -p "$BACKUP_DIR"

TS="$(date +%Y%m%d-%H%M%S)"
WORK_DIR="$BACKUP_DIR/kbd-pms-$TS"
mkdir -p "$WORK_DIR"

echo "[1/4] Backup MySQL database..."
sudo docker compose --env-file .env.server exec -T mysql sh -c \
  'mysqldump -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" --single-transaction --routines --triggers --events --no-tablespaces --databases "$MYSQL_DATABASE"' \
  > "$WORK_DIR/database.sql"

echo "[2/4] Backup uploaded files volume..."
sudo docker run --rm \
  --entrypoint sh \
  --user 0:0 \
  -v kbd-pms_kbd-pms-uploads:/data:ro \
  -v "$WORK_DIR":/backup \
  kbd-pms/backend:latest \
  -c 'cd /data && tar -czf /backup/uploads.tar.gz .'

echo "[3/4] Create final archive..."
tar -czf "$BACKUP_DIR/kbd-pms-$TS.tar.gz" -C "$BACKUP_DIR" "kbd-pms-$TS"
rm -rf "$WORK_DIR"

echo "[4/4] Keep latest $KEEP_COUNT backups only..."
ls -1t "$BACKUP_DIR"/kbd-pms-*.tar.gz 2>/dev/null | tail -n +$((KEEP_COUNT + 1)) | xargs -r rm -f

echo "Backup completed: $BACKUP_DIR/kbd-pms-$TS.tar.gz"