#!/bin/sh
# PostgreSQL SSL certificate check — two complementary methods:
#   1. Live STARTTLS connection (validates the cert actually served)
#   2. SSH into the container to read the cert file directly

set -e

PG_HOST="${1:-192.168.10.10}"
PG_PORT="${2:-5432}"
REMOTE_HOST="debian-dockerserver"
CONTAINER="postgresql-server"
CERT_FILE="/var/lib/postgresql/18/docker/server.crt"
WARN_DAYS=30
CRIT_DAYS=7

rc=0

# --- Check 1: live STARTTLS connection ---
printf '=== Live STARTTLS check (%s:%s) ===\n' "$PG_HOST" "$PG_PORT"
cert=$(printf '' | openssl s_client -starttls postgres -connect "${PG_HOST}:${PG_PORT}" 2>/dev/null)

if [ -z "$cert" ]; then
    printf 'ERROR: Could not retrieve certificate from %s:%s\n' "$PG_HOST" "$PG_PORT" >&2
    rc=2
else
    subject=$(printf '%s\n' "$cert" | openssl x509 -noout -subject 2>/dev/null)
    issuer=$(printf '%s\n' "$cert"  | openssl x509 -noout -issuer  2>/dev/null)
    start=$(printf '%s\n' "$cert"   | openssl x509 -noout -startdate 2>/dev/null | sed 's/^notBefore=//')
    expiry=$(printf '%s\n' "$cert"  | openssl x509 -noout -enddate   2>/dev/null | sed 's/^notAfter=//')

    subj_val=$(printf '%s\n' "$subject" | sed 's/^subject=//')
    issr_val=$(printf '%s\n' "$issuer"  | sed 's/^issuer=//')
    self_signed="no"
    [ "$subj_val" = "$issr_val" ] && self_signed="yes"

    now_epoch=$(date +%s)
    exp_epoch=$(date -d "$expiry" +%s 2>/dev/null || date -j -f '%b %d %T %Y %Z' "$expiry" +%s 2>/dev/null)
    days_remaining=$(( (exp_epoch - now_epoch) / 86400 ))

    printf '%-20s %s\n' "Subject:"        "$subj_val"
    printf '%-20s %s\n' "Issuer:"         "$issr_val"
    printf '%-20s %s\n' "Self-signed:"    "$self_signed"
    printf '%-20s %s\n' "Valid from:"     "$start"
    printf '%-20s %s\n' "Valid until:"    "$expiry"
    printf '%-20s %d\n' "Days remaining:" "$days_remaining"

    if [ "$days_remaining" -le 0 ]; then
        printf 'Status: CRITICAL - Certificate has expired.\n' >&2
        rc=2
    elif [ "$days_remaining" -le "$CRIT_DAYS" ]; then
        printf 'Status: CRITICAL - Expires in %d days. Run ./renew-postgres-ssl.sh\n' "$days_remaining" >&2
        rc=2
    elif [ "$days_remaining" -le "$WARN_DAYS" ]; then
        printf 'Status: WARNING - Expires in %d days. Plan renewal soon.\n' "$days_remaining" >&2
        [ "$rc" -lt 1 ] && rc=1
    else
        printf 'Status: OK\n'
    fi
fi

printf '\n'

# --- Check 2: cert file inside container (via SSH) ---
printf '=== Container cert file check (%s/%s:%s) ===\n' "$REMOTE_HOST" "$CONTAINER" "$CERT_FILE"
cert_info=$(ssh "$REMOTE_HOST" "podman exec $CONTAINER openssl x509 -in $CERT_FILE -noout -enddate 2>&1")
ssh_rc=$?

if [ "$ssh_rc" -ne 0 ]; then
    printf 'ERROR: Cannot read certificate from %s:%s on %s\n' "$CONTAINER" "$CERT_FILE" "$REMOTE_HOST" >&2
    printf '%s\n' "$cert_info" >&2
    rc=2
else
    expiry_date=$(printf '%s\n' "$cert_info" | cut -d= -f2)
    expiry_epoch=$(date -d "$expiry_date" +%s)
    current_epoch=$(date +%s)
    days_until=$(( (expiry_epoch - current_epoch) / 86400 ))

    printf '%-20s %s\n' "Certificate:" "$CERT_FILE"
    printf '%-20s %s\n' "Expires:"     "$expiry_date"
    printf '%-20s %d\n' "Days remaining:" "$days_until"

    if [ "$days_until" -le "$CRIT_DAYS" ]; then
        printf 'Status: CRITICAL - Expires in %d days. Run ./renew-postgres-ssl.sh\n' "$days_until" >&2
        rc=2
    elif [ "$days_until" -le "$WARN_DAYS" ]; then
        printf 'Status: WARNING - Expires in %d days. Plan renewal soon.\n' "$days_until" >&2
        [ "$rc" -lt 1 ] && rc=1
    else
        printf 'Status: OK\n'
    fi
fi

exit "$rc"
