#!/usr/bin/env bash
set -euo pipefail

# OWASP Dependency-Check via Podman with local caching and NVD API key auto-detection.
# - Reads NVD_API_KEY from environment or env.secrets
# - Caches DB under .dc-data to reduce NVD calls and flakiness
# - Writes HTML report to build/reports/dependency-check-report.html

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT_DIR"

# Defaults and CLI parsing
DELAY_MS="${DC_DELAY:-10000}"
LOG_FILE=""

print_usage() {
  cat <<EOF
Usage: bash run-dependency-check.sh [--log <path>] [--help]

Environment variables:
  NVD_API_KEY   NVD API key; if not set, will be read from env.secrets when present.
  DC_DELAY      Delay between NVD API calls in milliseconds (default: 10000).

Options:
  -l, --log FILE   Append console output to FILE while streaming to stdout.
  -h, --help       Show this help.
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    -l|--log)
      shift
      LOG_FILE="${1:-}"
      if [[ -z "$LOG_FILE" ]]; then
        echo "[dependency-check] Error: --log requires a file path" >&2
        exit 2
      fi
      shift
      ;;
    -h|--help)
      print_usage
      exit 0
      ;;
    *)
      echo "[dependency-check] Unknown arg: $1" >&2
      print_usage
      exit 2
      ;;
  esac
done

OUT_DIR="$ROOT_DIR/build/reports"
# Allow override of the data directory via DC_DATA
DATA_DIR="${DC_DATA:-$ROOT_DIR/.dc-data}"
mkdir -p "$OUT_DIR" "$DATA_DIR"

# Resolve NVD API key: env var > env.secrets
NVD_API_KEY_ENV="${NVD_API_KEY:-}"
if [[ -z "${NVD_API_KEY_ENV}" && -f "$ROOT_DIR/env.secrets" ]]; then
  # Extract last occurrence to allow overrides; strip quotes and whitespace
  NVD_API_KEY_ENV="$(grep -E '^[[:space:]]*NVD_API_KEY[[:space:]]*=' env.secrets | tail -n1 | cut -d= -f2- | sed -E "s/^[[:space:]]*[\"']?//; s/[\"']?[[:space:]]*$//")"
fi

if [[ -z "${NVD_API_KEY_ENV}" ]]; then
  echo "[dependency-check] Warning: NVD_API_KEY not found; scan may be rate-limited or fail to update." >&2
fi

if ! command -v podman >/dev/null 2>&1; then
  echo "[dependency-check] Error: podman is not installed or not in PATH." >&2
  echo "Install podman or run the image manually with your container engine." >&2
  exit 1
fi

# Add SELinux label if present (harmless if not needed)
VOL_LABEL=""
if [[ -e "/sys/fs/selinux" ]]; then
  VOL_LABEL=":Z"
fi

echo "[dependency-check] Running OWASP Dependency-Check (Podman) with cached DB at ${DATA_DIR} ..."
echo "[dependency-check] NVD delay: ${DELAY_MS} ms${NVD_API_KEY_ENV:+, API key detected}${LOG_FILE:+, logging to $LOG_FILE}"

set +e
if [[ -n "$LOG_FILE" ]]; then
  mkdir -p "$(dirname "$LOG_FILE")"
  set -o pipefail
  podman run --rm -v "$ROOT_DIR":/src${VOL_LABEL} owasp/dependency-check:latest \
    --scan /src \
    --format HTML \
    --out /src/build/reports \
    ${NVD_API_KEY_ENV:+--nvdApiKey "$NVD_API_KEY_ENV"} \
    --nvdApiDelay "${DELAY_MS}" \
    --data /src/"$(realpath --relative-to=\"$ROOT_DIR\" \"$DATA_DIR\")" 2>&1 | tee -a "$LOG_FILE"
  STATUS=${PIPESTATUS[0]}
else
  podman run --rm -v "$ROOT_DIR":/src${VOL_LABEL} owasp/dependency-check:latest \
    --scan /src \
    --format HTML \
    --out /src/build/reports \
    ${NVD_API_KEY_ENV:+--nvdApiKey "$NVD_API_KEY_ENV"} \
    --nvdApiDelay "${DELAY_MS}" \
    --data /src/"$(realpath --relative-to=\"$ROOT_DIR\" \"$DATA_DIR\")"
  STATUS=$?
fi
set -e

if [[ $STATUS -ne 0 ]]; then
  echo "[dependency-check] The container exited with status $STATUS" >&2
fi

REPORT_HTML="$OUT_DIR/dependency-check-report.html"
if [[ -f "$REPORT_HTML" ]]; then
  echo "[dependency-check] Report generated: $REPORT_HTML"
else
  # Some images write to a subfolder — surface both locations to the user
  ALT_HTML="$OUT_DIR/dependency-check/dependency-check-report.html"
  if [[ -f "$ALT_HTML" ]]; then
    echo "[dependency-check] Report generated: $ALT_HTML"
  else
    echo "[dependency-check] No report found; check Docker output above for errors." >&2
    exit 2
  fi
fi
