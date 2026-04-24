#!/bin/sh

# POSIX shell script to check TLS certificate expiry and optionally
# confirm that a certificate matches a given private key.
#
# Usage:
#   sh scripts/check-cert-expiry.sh <path1> [<path2> ...]
#
# - You can pass certificate files (e.g., fullchain.pem) directly, or
#   private keys (e.g., *.privkey.pem). For keys, the script will try
#   to find a nearby matching certificate.
# - Exits with code 0 if no certificates are expired, 1 if any are expired,
#   and 2 if there was an error processing inputs.

any_expired=0
had_error=0

err() {
  printf '%s\n' "$*" >&2
}

is_cert() {
  # Returns 0 if file contains a CERTIFICATE block
  grep -q "BEGIN CERTIFICATE" "$1" 2>/dev/null
}

is_key() {
  # Returns 0 if file contains a PRIVATE KEY block
  grep -Eq "BEGIN (RSA |EC )?PRIVATE KEY|BEGIN ENCRYPTED PRIVATE KEY" "$1" 2>/dev/null
}

########################
# Public key comparison #
########################

pubkey_hash_from_cert() {
  # Produce a stable SHA-256 hash of the certificate's public key
  # Avoids awk-dependent parsing to maximize portability
  openssl x509 -in "$1" -pubkey -noout 2>/dev/null \
    | openssl pkey -pubin -outform DER 2>/dev/null \
    | openssl dgst -sha256 -r 2>/dev/null \
    | sed 's/ .*//'
}

pubkey_hash_from_key() {
  # Produce a stable SHA-256 hash of the private key's public component
  openssl pkey -in "$1" -pubout -outform DER 2>/dev/null \
    | openssl dgst -sha256 -r 2>/dev/null \
    | sed 's/ .*//'
}

check_cert_expiry() {
  cert="$1"
  if ! dates=$(openssl x509 -in "$cert" -noout -dates 2>/dev/null); then
    err "ERROR: Failed to read certificate: $cert"
    had_error=1
    return 2
  fi

  notbefore=$(printf '%s\n' "$dates" | sed -n 's/^notBefore=//p')
  notafter=$(printf '%s\n' "$dates" | sed -n 's/^notAfter=//p')

  if openssl x509 -in "$cert" -noout -checkend 0 >/dev/null 2>&1; then
    expired=0
    status="VALID"
  else
    expired=1
    status="EXPIRED"
  fi

  printf 'Certificate: %s\n' "$cert"
  printf '  Status: %s\n' "$status"
  printf '  Not Before: %s\n' "$notbefore"
  printf '  Not After : %s\n' "$notafter"

  if [ "$expired" -eq 1 ]; then
    any_expired=1
  fi

  return 0
}

match_key_to_cert() {
  key="$1"
  cert="$2"

  key_hash=$(pubkey_hash_from_key "$key") || key_hash=
  cert_hash=$(pubkey_hash_from_cert "$cert") || cert_hash=

  if [ -z "$key_hash" ] || [ -z "$cert_hash" ]; then
    printf '  Key/Cert Match: UNKNOWN (could not derive public keys)\n'
    return 2
  fi

  if [ "$key_hash" = "$cert_hash" ]; then
    printf '  Key/Cert Match: YES\n'
    return 0
  else
    printf '  Key/Cert Match: NO\n'
    return 1
  fi
}

find_cert_for_key() {
  key="$1"
  dir=$(dirname "$key")
  base=$(basename "$key")
  stem=${base%.*}

  # Common naming patterns next to the key
  candidates=
  # Replace privkey with fullchain/cert variants
  case "$base" in
    *privkey*.pem)
      c1=$(printf '%s\n' "$base" | sed 's/privkey/fullchain/g')
      c2=$(printf '%s\n' "$base" | sed 's/privkey/cert/g')
      candidates="$candidates $dir/$c1 $dir/$c2"
      ;;
  esac

  # Same stem with typical cert suffixes
  candidates="$candidates $dir/${stem}.fullchain.pem $dir/${stem}.cert.pem $dir/${stem}.crt $dir/${stem}.pem"

  # Very common generic names next to the key
  candidates="$candidates $dir/fullchain.pem $dir/cert.pem $dir/chain.pem"

  for c in $candidates; do
    if [ -f "$c" ] && is_cert "$c"; then
      printf '%s\n' "$c"
      return 0
    fi
  done

  return 1
}

process_file() {
  f="$1"

  if [ ! -f "$f" ]; then
    err "ERROR: Not a file: $f"
    had_error=1
    return 2
  fi

  if is_cert "$f"; then
    check_cert_expiry "$f"
    printf '\n'
    return $?
  fi

  if is_key "$f"; then
    cert=$(find_cert_for_key "$f")
    if [ -n "$cert" ] && [ -f "$cert" ]; then
      printf 'Private Key: %s\n' "$f"
      check_cert_expiry "$cert"
      match_key_to_cert "$f" "$cert" >/dev/null 2>&1 || true
      # Re-print match result visibly
      match_key_to_cert "$f" "$cert"
      printf '\n'
      return 0
    else
      printf 'Private Key: %s\n' "$f"
      printf '  Certificate: NOT FOUND (looked next to key)\n\n'
      had_error=1
      return 2
    fi
  fi

  err "WARNING: Unknown file type (not key or cert): $f"
  return 2
}

if [ "$#" -eq 0 ]; then
  err "Usage: $0 <path-to-key-or-cert> [more ...]"
  exit 2
fi

for p in "$@"; do
  process_file "$p"
done

if [ "$had_error" -eq 1 ]; then
  # Prefer signaling errors (2) over expiry (1) if both happened
  exit 2
fi

if [ "$any_expired" -eq 1 ]; then
  exit 1
fi

exit 0
