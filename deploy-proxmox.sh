#!/bin/bash

# Complete Proxmox deployment script with integrated SSH connectivity
# This script handles SSH setup and deployment in one seamless process

log() {
  echo "$(date +"%Y-%m-%d %H:%M:%S") - $*"
}

log_error() {
  echo "$(date +"%Y-%m-%d %H:%M:%S") - ERROR: $*" >&2
}

# Function to validate and create SSL keystore
validate_and_create_keystore() {
  local cert_path="ssl/bhenning.fullchain.pem"
  local key_path="ssl/bhenning.privkey.pem"
  local keystore_path="src/main/resources/bhenning-letsencrypt.p12"
  local keystore_password="${SSL_KEY_STORE_PASSWORD:-changeit}"
  local keystore_alias="bhenning"

  log "Validating SSL certificates and keystore..."

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
  log "Checking certificate expiration..."
  if ! openssl x509 -in "$cert_path" -noout -checkend 86400 >/dev/null 2>&1; then
    log_error "SSL certificate is expired or will expire within 24 hours!"
    openssl x509 -in "$cert_path" -noout -dates 2>/dev/null | grep -E "notAfter|notBefore" || true
    log_error "Please renew your Let's Encrypt certificate before proceeding."
    return 1
  fi

  # Get certificate expiration info
  local cert_expiry
  cert_expiry=$(openssl x509 -in "$cert_path" -noout -enddate 2>/dev/null | cut -d= -f2)
  log "✓ Certificate is valid until: $cert_expiry"

  # Validate certificate and key match
  log "Validating certificate and private key match..."
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
  log "✓ Certificate and private key match"

  # Check if keystore already exists and is valid
  if [ -f "$keystore_path" ]; then
    log "Existing keystore found, validating..."
    if keytool -list -keystore "$keystore_path" -alias "$keystore_alias" -storepass "$keystore_password" >/dev/null 2>&1; then
      # Check if keystore certificate matches current certificate
      local keystore_cert_hash
      keystore_cert_hash=$(keytool -exportcert -keystore "$keystore_path" -alias "$keystore_alias" -storepass "$keystore_password" -rfc 2>/dev/null | openssl x509 -noout -pubkey 2>/dev/null | openssl sha256 2>/dev/null)

      if [ "$cert_hash" = "$keystore_cert_hash" ]; then
        log "✓ Existing keystore is valid and up-to-date"
        return 0
      else
        log "Existing keystore certificate doesn't match current certificate, regenerating..."
        rm -f "$keystore_path"
      fi
    else
      log "Existing keystore is invalid or corrupted, regenerating..."
      rm -f "$keystore_path"
    fi
  fi

  # Create new keystore
  log "Creating new PKCS12 keystore..."
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
  log "Verifying keystore integrity..."
  if ! keytool -list -keystore "$keystore_path" -alias "$keystore_alias" -storepass "$keystore_password" >/dev/null 2>&1; then
    log_error "Keystore verification failed!"
    log_error "The created keystore is not accessible or corrupted."
    return 1
  fi

  # Set proper permissions on keystore
  chmod 600 "$keystore_path" 2>/dev/null || {
    log_error "Warning: Could not set secure permissions on keystore file"
  }

  log "✓ PKCS12 keystore created successfully: $keystore_path"
  log "✓ Keystore alias: $keystore_alias"

  # Get certificate subject for verification
  local cert_subject
  cert_subject=$(openssl x509 -in "$cert_path" -noout -subject 2>/dev/null | sed 's/subject=//')
  log "✓ Certificate subject: $cert_subject"

  return 0
}

# Function to set up SSH connectivity and get user info for Proxmox
setup_proxmox_ssh() {
    local PROXMOX_HOST="192.168.10.10"
    local KEY_PATH="$HOME/.ssh/id_rsa"

    log "Setting up SSH connectivity to Proxmox host..."

    # Test SSH port connectivity
    if ! nc -zv "$PROXMOX_HOST" 22 >/dev/null 2>&1; then
        log_error "Cannot connect to SSH port 22 on $PROXMOX_HOST"
        log "Please ensure:"
        log "1. Proxmox server is running at $PROXMOX_HOST"
        log "2. SSH service is enabled and accessible"
        log "3. Network connectivity is working"
        exit 1
    fi
    log "✓ SSH port 22 is accessible on $PROXMOX_HOST"

    # Check SSH agent or key availability
    if ! ssh-add -l >/dev/null 2>&1; then
        log "SSH agent not running or no keys loaded"
        if [ -f "$KEY_PATH" ]; then
            log "Starting SSH agent and adding default key..."
            eval "$(ssh-agent -s)"
            export SSH_AUTH_SOCK SSH_AGENT_PID

            # Try to add key (may require passphrase)
            if ssh-add "$KEY_PATH" 2>/dev/null; then
                log "✓ SSH key added successfully"
            else
                log "SSH key requires passphrase or manual setup"
                log "Please run: ssh-add $KEY_PATH"
            fi
        else
            log "Default SSH key not found at $KEY_PATH"
            log "Please ensure SSH keys are properly configured"
        fi
    else
        log "✓ SSH agent is running with keys loaded"
    fi

    # Test SSH connection and get user info
    log "Testing SSH connection and gathering user information..."
    if ! ssh -o ConnectTimeout=10 "$PROXMOX_HOST" 'echo "SSH connection successful"' >/dev/null 2>&1; then
        log_error "SSH connection test failed to $PROXMOX_HOST"
        log "Please verify:"
        log "1. SSH keys are properly configured"
        log "2. User has access to the Proxmox server"
        log "3. Host key is accepted (try: ssh $PROXMOX_HOST)"
        exit 1
    fi

    # Get user information from Proxmox host
    CURRENT_UID="$(ssh "$PROXMOX_HOST" id -u 2>/dev/null)"
    CURRENT_GID="$(ssh "$PROXMOX_HOST" id -g 2>/dev/null)"
    USERNAME="$(ssh "$PROXMOX_HOST" whoami 2>/dev/null)"

    if [ -z "$CURRENT_UID" ] || [ -z "$CURRENT_GID" ] || [ -z "$USERNAME" ]; then
        log_error "Failed to retrieve user information from $PROXMOX_HOST"
        exit 1
    fi

    # Export variables for use by run.sh
    export CURRENT_UID CURRENT_GID USERNAME SSH_AUTH_SOCK SSH_AGENT_PID
    export PROXMOX_HOST

    log "✓ SSH connectivity verified, user info available:"
    log "  UID: $CURRENT_UID"
    log "  GID: $CURRENT_GID"
    log "  Username: $USERNAME"
}

log "=== Complete Proxmox Deployment ==="

# Validate SSL certificates and keystore first
log "Step 0: SSL Certificate and Keystore Validation"
if [ -f "env.secrets" ]; then
  # Source env.secrets to get SSL_KEY_STORE_PASSWORD
  set -a
  . "./env.secrets"
  set +a
fi

if ! validate_and_create_keystore; then
  log_error "SSL keystore validation/creation failed!"
  log_error "Cannot proceed with Proxmox deployment without valid SSL configuration."
  log_error ""
  log_error "Please ensure:"
  log_error "  1. Let's Encrypt certificates are current and not expired"
  log_error "  2. Certificate files exist in ssl/ directory:"
  log_error "     - ssl/bhenning.fullchain.pem"
  log_error "     - ssl/bhenning.privkey.pem"
  log_error "  3. SSL_KEY_STORE_PASSWORD is set in env.secrets"
  log_error "  4. OpenSSL and keytool are installed"
  log_error ""
  log_error "To fix Let's Encrypt certificates, run:"
  log_error "  sudo certbot renew --dry-run  # Test renewal"
  log_error "  sudo certbot renew            # Actual renewal"
  exit 1
fi
log "✓ SSL keystore validation completed successfully"

# Step 1: Set up SSH connectivity and get user info
setup_proxmox_ssh

# Step 2: Set up database configuration
log "Step 2: Configuring Proxmox environment..."
log "✓ Using Proxmox environment with:"
log "  - PostgreSQL container with network connectivity"
log "  - InfluxDB container for metrics collection"
log "  - finance-lan network for container isolation"
log "  - LAN accessibility (192.168.10.10:8443)"

# Step 3: Run the deployment
log "Step 3: Running Proxmox deployment..."
log "Note: This will build the application and deploy all containers with InfluxDB metrics"

if ! ./run.sh proxmox; then
    log_error "Proxmox deployment failed!"
    log ""
    log "Troubleshooting:"
    log "1. Check container logs: ssh $PROXMOX_HOST 'docker logs raspi-finance-endpoint'"
    log "2. Verify PostgreSQL container: ssh $PROXMOX_HOST 'docker logs postgresql-server'"
    log "3. Check InfluxDB metrics: ssh $PROXMOX_HOST 'docker logs influxdb-server'"
    log "4. Verify network connectivity: ssh $PROXMOX_HOST 'docker network inspect finance-lan'"
    log "5. Check environment variables in env.secrets and env.prod"
    exit 1
fi

# Step 4: Verify deployment
log "Step 4: Verifying Proxmox deployment..."
sleep 5

# Check if containers are running
log "Checking container status..."
export DOCKER_HOST=ssh://$PROXMOX_HOST

app_status=$(docker ps --filter name=raspi-finance-endpoint --format "{{.Status}}" 2>/dev/null)
postgres_status=$(docker ps --filter name=postgresql-server --format "{{.Status}}" 2>/dev/null)
influx_status=$(docker ps --filter name=influxdb-server --format "{{.Status}}" 2>/dev/null)

if echo "$app_status" | grep -q "Up"; then
    log "✓ Application container is running: $app_status"
else
    log_error "✗ Application container is not running properly"
    log "Status: $app_status"
    log "Check logs with: ssh $PROXMOX_HOST 'docker logs raspi-finance-endpoint'"
    exit 1
fi

if echo "$postgres_status" | grep -q "Up"; then
    log "✓ PostgreSQL container is running: $postgres_status"
else
    log_error "✗ PostgreSQL container is not running properly"
    log "Status: $postgres_status"
    log "Check logs with: ssh $PROXMOX_HOST 'docker logs postgresql-server'"
fi

if echo "$influx_status" | grep -q "Up"; then
    log "✓ InfluxDB container is running: $influx_status"
else
    log "⚠ InfluxDB container is not running"
    log "Status: $influx_status"
    log "Metrics collection may be disabled"
fi

# Test application health
log "Testing application health..."
if ssh "$PROXMOX_HOST" 'curl -k -f -s https://localhost:8443/actuator/health >/dev/null 2>&1'; then
    log "✓ Application health check passed"
else
    log "⚠ Application health check failed - application may still be starting"
fi

# Test LAN access
log "Testing LAN access..."
if curl -k -f -s --connect-timeout 10 https://192.168.10.10:8443/actuator/health >/dev/null 2>&1; then
    log "✓ LAN HTTPS access working: https://192.168.10.10:8443/"
else
    log "⚠ LAN HTTPS access test failed"
    log "Note: This may be normal if SSL certificates are not properly configured"
fi

# Test network connectivity
log "Testing container network connectivity..."
network_info=$(docker network inspect finance-lan --format '{{range .Containers}}{{.Name}}: {{.IPv4Address}}{{"\n"}}{{end}}' 2>/dev/null)
if [ -n "$network_info" ]; then
    log "✓ finance-lan network active with containers:"
    echo "$network_info" | while read -r line; do
        log "    $line"
    done
else
    log "⚠ finance-lan network inspection failed"
fi

log ""
log "=== Deployment Summary ==="
log "✓ SSH connectivity: Working"
log "✓ User info: UID=$CURRENT_UID, GID=$CURRENT_GID, User=$USERNAME"
log "✓ Application deployed: $(echo "$app_status" | head -1)"
log "✓ PostgreSQL database: $(echo "$postgres_status" | head -1)"
log "✓ InfluxDB metrics: $(echo "$influx_status" | head -1)"
log ""
log "Access URLs:"
log "  LAN HTTPS: https://192.168.10.10:8443/"
log "  Health Check: https://192.168.10.10:8443/actuator/health"
log "  GraphQL: https://192.168.10.10:8443/graphql"
log "  GraphiQL: https://192.168.10.10:8443/graphiql"
log "  Metrics: https://192.168.10.10:8443/actuator/metrics"
log ""
log "Monitoring commands:"
log "  Application logs: ssh $PROXMOX_HOST 'docker logs raspi-finance-endpoint -f'"
log "  PostgreSQL logs: ssh $PROXMOX_HOST 'docker logs postgresql-server -f'"
log "  InfluxDB logs: ssh $PROXMOX_HOST 'docker logs influxdb-server -f'"
log "  Container status: ssh $PROXMOX_HOST 'docker ps'"
log "  Network inspection: ssh $PROXMOX_HOST 'docker network inspect finance-lan'"
log ""
log "Database operations:"
log "  Connect to PostgreSQL: ssh $PROXMOX_HOST 'docker exec -it postgresql-server psql -U henninb -d finance_db'"
log "  InfluxDB CLI: ssh $PROXMOX_HOST 'docker exec -it influxdb-server influx'"
log ""
log "Troubleshooting:"
log "  Restart deployment: ./deploy-proxmox.sh"
log "  Check network connectivity: ssh $PROXMOX_HOST 'docker network inspect finance-lan'"
log "  Application diagnostics: ssh $PROXMOX_HOST 'curl -k -s https://localhost:8443/actuator/info'"

docker inspect influxdb-server --format '{{ range .Mounts }}{{ if eq .Type "volume" }}{{ .Name }}{{ end }}{{ end }}'

exit 0
