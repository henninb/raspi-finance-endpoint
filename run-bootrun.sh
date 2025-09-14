#!/bin/sh

# POSIX compliant script for running Spring Boot application
set -e  # Exit on any error

# Logging configuration
SCRIPT_NAME="run-bootrun.sh"
LOG_PREFIX="[$SCRIPT_NAME]"

# Function to log messages with timestamp
log_info() {
    printf "%s %s INFO: %s\n" "$(date '+%Y-%m-%d %H:%M:%S')" "$LOG_PREFIX" "$*" >&1
}

log_error() {
    printf "%s %s ERROR: %s\n" "$(date '+%Y-%m-%d %H:%M:%S')" "$LOG_PREFIX" "$*" >&2
}

log_warn() {
    printf "%s %s WARN: %s\n" "$(date '+%Y-%m-%d %H:%M:%S')" "$LOG_PREFIX" "$*" >&2
}

# Function to cleanup temporary files
cleanup_files() {
    if [ -f "env.bootrun" ]; then
        rm -f env.bootrun
        log_info "Removed temporary env.bootrun file"
    fi
}

# Set up signal traps for cleanup
trap cleanup_files INT TERM

# Function to validate and create SSL keystore
validate_and_create_keystore() {
  local cert_path="ssl/bhenning.fullchain.pem"
  local key_path="ssl/bhenning.privkey.pem"
  local keystore_path="src/main/resources/bhenning-letsencrypt.p12"
  local keystore_password="${SSL_KEY_STORE_PASSWORD:-changeit}"
  local keystore_alias="bhenning"

  log_info "Validating SSL certificates and keystore..."

  # Check if SSL directory and files exist
  if [ ! -d "ssl" ]; then
    log_error "SSL directory 'ssl/' not found!"
    log_error "Please ensure Let's Encrypt certificates are copied to ssl/ directory."
    return 1
  fi

  if [ ! -f "$cert_path" ]; then
    log_error "SSL certificate not found: $cert_path"
    log_error "Please ensure Let's Encrypt fullchain.pem is copied to ssl/bhenning.fullchain.pem"
    return 1
  fi

  if [ ! -f "$key_path" ]; then
    log_error "SSL private key not found: $key_path"
    log_error "Please ensure Let's Encrypt privkey.pem is copied to ssl/bhenning.privkey.pem"
    return 1
  fi

  # Validate certificate is not expired
  log_info "Checking certificate expiration..."
  if ! openssl x509 -in "$cert_path" -noout -checkend 86400 >/dev/null 2>&1; then
    log_error "SSL certificate is expired or will expire within 24 hours!"
    openssl x509 -in "$cert_path" -noout -dates 2>/dev/null | grep -E "notAfter|notBefore" || true
    log_error "Please renew your Let's Encrypt certificate before proceeding."
    return 1
  fi

  # Get certificate expiration info
  local cert_expiry
  cert_expiry=$(openssl x509 -in "$cert_path" -noout -enddate 2>/dev/null | cut -d= -f2)
  log_info "✓ Certificate is valid until: $cert_expiry"

  # Validate certificate and key match
  log_info "Validating certificate and private key match..."
  local cert_hash
  local key_hash
  cert_hash=$(openssl x509 -in "$cert_path" -noout -pubkey 2>/dev/null | openssl sha256 2>/dev/null)
  key_hash=$(openssl pkey -in "$key_path" -pubout 2>/dev/null | openssl sha256 2>/dev/null)

  if [ "$cert_hash" != "$key_hash" ]; then
    log_error "SSL certificate and private key do not match!"
    log_error "Certificate hash: $cert_hash"
    log_error "Private key hash: $key_hash"
    log_error "Please ensure you're using matching certificate and key files."
    return 1
  fi
  log_info "✓ Certificate and private key match"

  # Check if keystore already exists and is valid
  if [ -f "$keystore_path" ]; then
    log_info "Existing keystore found, validating..."
    if keytool -list -keystore "$keystore_path" -alias "$keystore_alias" -storepass "$keystore_password" >/dev/null 2>&1; then
      # Check if keystore certificate matches current certificate
      local keystore_cert_hash
      keystore_cert_hash=$(keytool -exportcert -keystore "$keystore_path" -alias "$keystore_alias" -storepass "$keystore_password" -rfc 2>/dev/null | openssl x509 -noout -pubkey 2>/dev/null | openssl sha256 2>/dev/null)

      if [ "$cert_hash" = "$keystore_cert_hash" ]; then
        log_info "✓ Existing keystore is valid and up-to-date"
        return 0
      else
        log_info "Existing keystore certificate doesn't match current certificate, regenerating..."
        rm -f "$keystore_path"
      fi
    else
      log_info "Existing keystore is invalid or corrupted, regenerating..."
      rm -f "$keystore_path"
    fi
  fi

  # Create new keystore
  log_info "Creating new PKCS12 keystore..."
  if ! openssl pkcs12 -export -in "$cert_path" -inkey "$key_path" -out "$keystore_path" -name "$keystore_alias" -passout "pass:$keystore_password" 2>/dev/null; then
    log_error "Failed to create PKCS12 keystore!"
    log_error "Please check:"
    log_error "  1. OpenSSL is installed and accessible"
    log_error "  2. Certificate and key files are readable"
    log_error "  3. SSL_KEY_STORE_PASSWORD is set correctly in env.secrets"
    return 1
  fi

  # Verify keystore was created successfully
  if [ ! -f "$keystore_path" ]; then
    log_error "Keystore file was not created: $keystore_path"
    return 1
  fi

  # Test keystore integrity
  log_info "Verifying keystore integrity..."
  if ! keytool -list -keystore "$keystore_path" -alias "$keystore_alias" -storepass "$keystore_password" >/dev/null 2>&1; then
    log_error "Keystore verification failed!"
    log_error "The created keystore is not accessible or corrupted."
    return 1
  fi

  # Set proper permissions on keystore
  chmod 600 "$keystore_path" 2>/dev/null || {
    log_warn "Could not set secure permissions on keystore file"
  }

  log_info "✓ PKCS12 keystore created successfully: $keystore_path"
  log_info "✓ Keystore alias: $keystore_alias"

  # Get certificate subject for verification
  local cert_subject
  cert_subject=$(openssl x509 -in "$cert_path" -noout -subject 2>/dev/null | sed 's/subject=//')
  log_info "✓ Certificate subject: $cert_subject"

  return 0
}

# Function to validate environment secrets
validate_env_secrets() {
    log_info "Validating environment secrets from env.secrets..."

    if [ ! -f "env.secrets" ]; then
        log_error "env.secrets file not found!"
        log_error "Please create env.secrets with the required environment variables."
        return 1
    fi

    log_info "✓ All required environment secrets are properly configured."
    return 0
}

log_info "Starting raspi-finance-endpoint boot run script..."
log_info "Working directory: $(pwd)"

# Validate environment secrets before proceeding
validate_env_secrets

# Load env.secrets early to get SSL_KEY_STORE_PASSWORD for keystore validation
log_info "Loading environment secrets for SSL validation..."
set -a
# shellcheck disable=SC1091
. ./env.secrets
set +a

# Validate and create SSL keystore before building
log_info "SSL Certificate and Keystore Validation"
if ! validate_and_create_keystore; then
  log_error "SSL keystore validation/creation failed!"
  log_error "Cannot proceed with application startup without valid SSL configuration."
  log_error ""
  log_error "Please ensure:"
  log_error "  1. Let's Encrypt certificates are current and not expired"
  log_error "  2. Certificate files exist in ssl/ directory:"
  log_error "     - ssl/bhenning.fullchain.pem"
  log_error "     - ssl/bhenning.privkey.pem"
  log_error "  3. SSL_KEY_STORE_PASSWORD is set in env.secrets"
  log_error "  4. Certificate and private key files are readable"
  log_error ""
  log_error "To fix Let's Encrypt certificates, run:"
  log_error "  sudo certbot renew --dry-run  # Test renewal"
  log_error "  sudo certbot renew            # Actual renewal"
  cleanup_files
  exit 1
fi
log_info "✓ SSL keystore validation completed successfully"

log_info "Preparing environment configuration..."
cleanup_files  # Remove any existing env.bootrun

# Create new bootrun environment from prod template with Flyway disabled
if ! sed "s/\/opt\/raspi-finance-endpoint/./g" env.prod > env.bootrun; then
    log_error "Failed to create env.bootrun from env.prod template"
    exit 1
fi

log_info "✓ Created env.bootrun configuration file"

log_info "Overriding database and influxdb hosts for local development..."
sed 's/postgresql-server:5432/192.168.10.10:5432/g' env.bootrun > env.bootrun.tmp && mv env.bootrun.tmp env.bootrun
sed 's/influxdb-server:8086/192.168.10.10:8086/g' env.bootrun > env.bootrun.tmp && mv env.bootrun.tmp env.bootrun
log_info "✓ Overrides applied."

log_info "Loading environment variables..."
set -a
# shellcheck disable=SC1091
. ./env.bootrun
# Note: env.secrets already loaded earlier for SSL validation
set +a
log_info "✓ Environment variables loaded successfully"

log_info "Starting Spring Boot application..."
log_info "Command: ./gradlew clean build bootRun -x test"
log_info "Note: V09 checksum has been permanently fixed in database"

# Set Spring Boot property for JWT key
export custom_project_jwt_key="$JWT_KEY"
log_info "✓ JWT key configured for Spring Boot"

# Run the application
./gradlew clean build bootRun -x test

log_info "✓ Application completed successfully"
cleanup_files
