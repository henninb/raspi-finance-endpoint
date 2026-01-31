#!/bin/sh
# Check PostgreSQL SSL certificate by connecting directly to the server.
# Uses openssl s_client with STARTTLS to negotiate TLS on the PostgreSQL wire protocol.

set -e

PG_HOST="${1:-192.168.10.10}"
PG_PORT="${2:-5432}"
WARN_DAYS=30

cert=$(printf '' | openssl s_client -starttls postgres -connect "${PG_HOST}:${PG_PORT}" 2>/dev/null)

if [ -z "$cert" ]; then
    printf 'ERROR: Could not retrieve certificate from %s:%s\n' "$PG_HOST" "$PG_PORT" >&2
    exit 1
fi

subject=$(printf '%s\n' "$cert" | openssl x509 -noout -subject 2>/dev/null)
issuer=$(printf '%s\n' "$cert"  | openssl x509 -noout -issuer  2>/dev/null)
start=$(printf '%s\n' "$cert"   | openssl x509 -noout -startdate 2>/dev/null | sed 's/^notBefore=//')
expiry=$(printf '%s\n' "$cert"  | openssl x509 -noout -enddate   2>/dev/null | sed 's/^notAfter=//')

if [ -z "$expiry" ]; then
    printf 'ERROR: Could not parse certificate dates\n' >&2
    exit 1
fi

now_epoch=$(date +%s)
exp_epoch=$(date -d "$expiry" +%s 2>/dev/null || date -j -f '%b %d %T %Y %Z' "$expiry" +%s 2>/dev/null)
days_remaining=$(( (exp_epoch - now_epoch) / 86400 ))

self_signed="no"
subj_val=$(printf '%s\n' "$subject" | sed 's/^subject=//')
issr_val=$(printf '%s\n' "$issuer"  | sed 's/^issuer=//')
if [ "$subj_val" = "$issr_val" ]; then
    self_signed="yes"
fi

printf 'PostgreSQL SSL Certificate (%s:%s)\n' "$PG_HOST" "$PG_PORT"
printf '%-20s %s\n' "Subject:" "$subj_val"
printf '%-20s %s\n' "Issuer:" "$issr_val"
printf '%-20s %s\n' "Self-signed:" "$self_signed"
printf '%-20s %s\n' "Valid from:" "$start"
printf '%-20s %s\n' "Valid until:" "$expiry"
printf '%-20s %d\n' "Days remaining:" "$days_remaining"

if [ "$days_remaining" -le 0 ]; then
    printf '\nCRITICAL: Certificate has expired.\n' >&2
    exit 2
elif [ "$days_remaining" -le "$WARN_DAYS" ]; then
    printf '\nWARNING: Certificate expires within %d days.\n' "$WARN_DAYS" >&2
    exit 1
else
    printf '\nOK: Certificate is valid.\n'
fi
