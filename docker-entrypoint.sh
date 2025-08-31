#!/bin/sh
set -e

# Ensure required env vars have defaults (they are set via Dockerfile ENV normally)
: "${APP:=raspi-finance-endpoint}"
: "${USERNAME:=appuser}"
: "${CURRENT_UID:=1000}"
: "${CURRENT_GID:=1000}"
: "${TIMEZONE:=UTC}"

APP_DIR="/opt/${APP}"

# Prepare directories that are bind-mounted and must be writable
mkdir -p "${APP_DIR}/logs" "${APP_DIR}/json_in" || true

# Fix ownership when running as root (common with bind mounts)
if [ "$(id -u)" = "0" ]; then
  chown -R "${CURRENT_UID}:${CURRENT_GID}" "${APP_DIR}/logs" "${APP_DIR}/json_in" 2>/dev/null || true
  # Ensure binaries and ssl are readable by app user
  chown -R "${CURRENT_UID}:${CURRENT_GID}" "${APP_DIR}/bin" "${APP_DIR}/ssl" 2>/dev/null || true
fi

# Default command: run the app as non-root using su-exec
JAVA_CMD="java -Duser.timezone=${TIMEZONE} -Xmx2048m -jar ${APP_DIR}/bin/${APP}.jar"

if [ "$#" -gt 0 ]; then
  exec su-exec "${USERNAME}" "$@"
else
  exec su-exec "${USERNAME}" sh -c "${JAVA_CMD}"
fi

