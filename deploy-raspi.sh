#!/bin/bash

# Raspberry Pi Podman deployment script
# Deploys raspi-finance-endpoint to Raspberry Pi using podman and ARM64 architecture
# Key differences from deploy-proxmox.sh:
# - Uses podman instead of docker
# - Builds for ARM64 architecture
# - PostgreSQL remains external at 192.168.10.10
# - Remote deployment via SSH to raspi

log() {
  echo "$(date +"%Y-%m-%d %H:%M:%S") - $*"
}

log_error() {
  echo "$(date +"%Y-%m-%d %H:%M:%S") - ERROR: $*" >&2
}

# Configuration
RASPI_HOST="raspi"
# PostgreSQL IP - can be overridden with environment variable
POSTGRES_IP="${POSTGRES_IP:-192.168.10.10}"
APP_NAME="raspi-finance-endpoint"
CONTAINER_PORT=8443
ENV_FILE="env.raspi"

log "=== Raspberry Pi Podman Deployment ==="

# Step 0: Validate SSL certificates and keystore
log "Step 0: SSL Certificate and Keystore Validation"
if [ -f "env.secrets" ]; then
  set -a
  . "./env.secrets"
  set +a
fi

# Function to validate and create SSL keystore
validate_and_create_keystore() {
  local cert_path="ssl/bhenning.fullchain.pem"
  local key_path="ssl/bhenning.privkey.pem"
  local keystore_path="src/main/resources/bhenning-letsencrypt.p12"
  local keystore_password="${SSL_KEY_STORE_PASSWORD:-changeit}"
  local keystore_alias="bhenning"

  log "Validating SSL certificates and keystore..."

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

  return 0
}

if ! validate_and_create_keystore; then
  log_error "SSL keystore validation failed!"
  log_error "Cannot proceed with deployment without valid SSL configuration."
  exit 1
fi
log "✓ SSL keystore validation completed successfully"

# Step 1: Set up SSH connectivity and verify Raspberry Pi
log "Step 1: Setting up SSH connectivity to Raspberry Pi..."

# Test SSH connection directly (more reliable than nc)
if ! ssh -o ConnectTimeout=10 "$RASPI_HOST" 'echo "SSH connection successful"' >/dev/null 2>&1; then
  log_error "SSH connection test failed to $RASPI_HOST"
  log "Please verify:"
  log "1. SSH keys are properly configured"
  log "2. User has access to the Raspberry Pi"
  log "3. Host key is accepted (try: ssh $RASPI_HOST)"
  exit 1
fi

# Get user information from Raspberry Pi
log "Getting user information from Raspberry Pi..."
RASPI_UID="$(ssh "$RASPI_HOST" id -u 2>/dev/null)"
RASPI_GID="$(ssh "$RASPI_HOST" id -g 2>/dev/null)"
RASPI_USER="$(ssh "$RASPI_HOST" whoami 2>/dev/null)"

if [ -z "$RASPI_UID" ] || [ -z "$RASPI_GID" ] || [ -z "$RASPI_USER" ]; then
  log_error "Failed to retrieve user information from $RASPI_HOST"
  exit 1
fi

log "✓ SSH connectivity verified, user info available:"
log "  UID: $RASPI_UID"
log "  GID: $RASPI_GID"
log "  Username: $RASPI_USER"

# Verify Raspberry Pi architecture
RASPI_ARCH="$(ssh "$RASPI_HOST" uname -m 2>/dev/null)"
log "  Architecture: $RASPI_ARCH"

if [ "$RASPI_ARCH" != "aarch64" ] && [ "$RASPI_ARCH" != "armv7l" ]; then
  log "⚠ Warning: Raspberry Pi architecture is $RASPI_ARCH (expected aarch64 or armv7l)"
  log "Deployment may fail if not ARM-compatible"
fi

# Step 2: Verify podman is installed on Raspberry Pi
log "Step 2: Verifying podman installation..."

if ! ssh "$RASPI_HOST" 'command -v podman >/dev/null 2>&1'; then
  log_error "Podman is not installed on $RASPI_HOST"
  log_error "Install podman with:"
  log_error "  ssh $RASPI_HOST 'sudo apt-get update && sudo apt-get install -y podman'"
  exit 1
fi

PODMAN_VERSION="$(ssh "$RASPI_HOST" podman --version 2>/dev/null)"
log "✓ Podman installed: $PODMAN_VERSION"

# Step 3: Verify PostgreSQL connectivity from Raspberry Pi
log "Step 3: Verifying PostgreSQL connectivity..."
log "  Testing connection to PostgreSQL at $POSTGRES_IP:5432..."

# Test PostgreSQL connectivity using bash built-in TCP test (no nc required)
if ! ssh "$RASPI_HOST" "timeout 5 bash -c 'cat < /dev/null > /dev/tcp/$POSTGRES_IP/5432' 2>/dev/null"; then
  log "⚠ Warning: Raspberry Pi cannot connect to PostgreSQL at $POSTGRES_IP:5432"
  log ""
  log "Network Information:"
  log "  Raspberry Pi is on: 10.0.0.175/24"
  log "  PostgreSQL server: $POSTGRES_IP"
  log ""
  log "Options to resolve this:"
  log "  1. Run PostgreSQL as a container on Raspberry Pi"
  log "  2. Configure network routing or VPN to reach $POSTGRES_IP"
  log "  3. Use a PostgreSQL server on the same network as Raspberry Pi"
  log "  4. Set POSTGRES_IP environment variable to a different server"
  log ""
  log "To skip this check and continue anyway, run:"
  log "  SKIP_POSTGRES_CHECK=true ./deploy-raspi.sh"
  log ""

  # Allow skipping this check
  if [ "${SKIP_POSTGRES_CHECK}" != "true" ]; then
    exit 1
  else
    log "⚠ Skipping PostgreSQL connectivity check (SKIP_POSTGRES_CHECK=true)"
  fi
else
  log "✓ PostgreSQL connectivity verified: $POSTGRES_IP:5432"
fi

# Step 4: Build application locally
log "Step 4: Building application with Gradle..."

if ! ./gradlew clean build -x test; then
  log_error "Gradle build failed"
  exit 1
fi

log "✓ Gradle build succeeded"

# Step 5: Create necessary directories on Raspberry Pi
log "Step 5: Creating necessary directories on Raspberry Pi..."

ssh "$RASPI_HOST" << 'EOF_DIRS'
mkdir -p ~/raspi-finance-endpoint/ssl
mkdir -p ~/raspi-finance-endpoint/logs/archive
mkdir -p ~/raspi-finance-endpoint/json_in
mkdir -p ~/raspi-finance-endpoint/build/libs
EOF_DIRS

log "✓ Directories created on Raspberry Pi"

# Step 6: Transfer build artifacts to Raspberry Pi
log "Step 6: Transferring build artifacts to Raspberry Pi..."

# Transfer JAR file
log "  Transferring JAR file..."
if ! scp build/libs/${APP_NAME}.jar "${RASPI_HOST}:~/raspi-finance-endpoint/build/libs/"; then
  log_error "Failed to transfer JAR file"
  exit 1
fi

# Transfer SSL certificates
log "  Transferring SSL certificates..."
if ! scp -r ssl/* "${RASPI_HOST}:~/raspi-finance-endpoint/ssl/"; then
  log_error "Failed to transfer SSL certificates"
  exit 1
fi

# Transfer Dockerfile.arm64
log "  Transferring Dockerfile..."
if ! scp Dockerfile.arm64 "${RASPI_HOST}:~/raspi-finance-endpoint/Dockerfile"; then
  log_error "Failed to transfer Dockerfile"
  exit 1
fi

# Transfer environment files
log "  Transferring environment files..."
if ! scp env.raspi env.secrets "${RASPI_HOST}:~/raspi-finance-endpoint/"; then
  log_error "Failed to transfer environment files"
  exit 1
fi

log "✓ Build artifacts transferred successfully"

# Step 7: Build ARM64 container image on Raspberry Pi
log "Step 7: Building ARM64 container image on Raspberry Pi..."
log "  This may take several minutes on ARM hardware..."

ssh "$RASPI_HOST" << EOF_BUILD
cd ~/raspi-finance-endpoint

# Build the ARM64 image using podman with host networking
# Note: --network=host fixes slirp4netns issues in rootless podman
echo "Building container image with podman (using host network)..."
podman build \
  --network=host \
  --platform linux/arm64 \
  --build-arg TIMEZONE="America/Chicago" \
  --build-arg APP="${APP_NAME}" \
  --build-arg USERNAME="${RASPI_USER}" \
  --build-arg CURRENT_UID="${RASPI_UID}" \
  --build-arg CURRENT_GID="${RASPI_GID}" \
  -f Dockerfile \
  -t ${APP_NAME}:latest \
  --no-cache \
  .

if [ \$? -ne 0 ]; then
  echo "ERROR: Container build failed with host network"
  echo "Trying alternative: building with sudo (rootful podman)..."

  # Try with rootful podman as fallback
  sudo podman build \
    --platform linux/arm64 \
    --build-arg TIMEZONE="America/Chicago" \
    --build-arg APP="${APP_NAME}" \
    --build-arg USERNAME="${RASPI_USER}" \
    --build-arg CURRENT_UID="${RASPI_UID}" \
    --build-arg CURRENT_GID="${RASPI_GID}" \
    -f Dockerfile \
    -t ${APP_NAME}:latest \
    --no-cache \
    .

  if [ \$? -ne 0 ]; then
    echo "ERROR: Container build failed even with sudo"
    exit 1
  fi

  echo "✓ Container image built successfully (using rootful podman)"
else
  echo "✓ Container image built successfully"
fi
EOF_BUILD

if [ $? -ne 0 ]; then
  log_error "Remote build failed on Raspberry Pi"
  log "Check logs with: ssh $RASPI_HOST 'cd ~/raspi-finance-endpoint && podman images'"
  exit 1
fi

log "✓ ARM64 container image built successfully"

# Step 8: Stop and remove existing container
log "Step 8: Cleaning up existing container..."

ssh "$RASPI_HOST" << 'EOF_CLEANUP'
# Stop existing container if running (check both rootless and rootful)
if podman ps -a --format "{{.Names}}" | grep -q "^raspi-finance-endpoint$"; then
  echo "Stopping existing container (rootless)..."
  podman stop raspi-finance-endpoint 2>/dev/null || true
  podman rm -f raspi-finance-endpoint 2>/dev/null || true
fi

if sudo podman ps -a --format "{{.Names}}" 2>/dev/null | grep -q "^raspi-finance-endpoint$"; then
  echo "Stopping existing container (rootful)..."
  sudo podman stop raspi-finance-endpoint 2>/dev/null || true
  sudo podman rm -f raspi-finance-endpoint 2>/dev/null || true
fi

# Remove old images from both storages
echo "Pruning old images..."
podman image prune -f 2>/dev/null || true
sudo podman image prune -f 2>/dev/null || true
EOF_CLEANUP

log "✓ Cleanup completed"

# Step 9: Create podman network if not exists
log "Step 9: Setting up podman network..."

ssh "$RASPI_HOST" << 'EOF_NETWORK'
# Create finance-lan network in both rootless and rootful storage
# (we don't know yet which one will be used to run the container)

# Rootless network
if ! podman network exists finance-lan 2>/dev/null; then
  echo "Creating finance-lan network (rootless)..."
  podman network create finance-lan
else
  echo "finance-lan network already exists (rootless)"
fi

# Rootful network
if ! sudo podman network exists finance-lan 2>/dev/null; then
  echo "Creating finance-lan network (rootful)..."
  sudo podman network create finance-lan
else
  echo "finance-lan network already exists (rootful)"
fi
EOF_NETWORK

log "✓ Podman network configured"

# Step 10: Run the container
log "Step 10: Starting container on Raspberry Pi..."

ssh "$RASPI_HOST" << 'EOF_RUN'
cd ~/raspi-finance-endpoint

# Load environment variables
set -a
. ./env.raspi
. ./env.secrets
set +a

# Check if image exists in user's podman storage
if podman images | grep -q "raspi-finance-endpoint"; then
  echo "Image found in user podman storage, running with rootless podman..."
  USE_SUDO=""
elif sudo podman images | grep -q "raspi-finance-endpoint"; then
  echo "Image found in root podman storage, running with sudo..."
  USE_SUDO="sudo"
else
  echo "ERROR: Image not found in either user or root podman storage"
  exit 1
fi

# Run container with podman (with or without sudo)
# Use --env-file to pass all environment variables at once
# This is more reliable than passing individual -e flags
$USE_SUDO podman run -d \
  --name raspi-finance-endpoint \
  --hostname raspi-finance-endpoint \
  --restart unless-stopped \
  --network finance-lan \
  -p 8443:8443 \
  -v ~/raspi-finance-endpoint/logs:/opt/raspi-finance-endpoint/logs \
  -v ~/raspi-finance-endpoint/json_in:/opt/raspi-finance-endpoint/json_in \
  --env-file ~/raspi-finance-endpoint/env.raspi \
  --env-file ~/raspi-finance-endpoint/env.secrets \
  raspi-finance-endpoint:latest

if [ $? -ne 0 ]; then
  echo "ERROR: Failed to start container"
  $USE_SUDO podman logs raspi-finance-endpoint 2>&1 || true
  exit 1
fi

echo "✓ Container started successfully"
EOF_RUN

if [ $? -ne 0 ]; then
  log_error "Failed to start container on Raspberry Pi"
  log "Check logs with: ssh $RASPI_HOST 'podman logs ${APP_NAME}'"
  exit 1
fi

log "✓ Container started successfully"

# Step 11: Verify deployment
log "Step 11: Verifying deployment..."
log "  Waiting for application startup (30 seconds)..."
sleep 30

# Check container status (try both rootless and rootful)
CONTAINER_STATUS=$(ssh "$RASPI_HOST" "podman ps --filter name=${APP_NAME} --format '{{.Status}}' 2>/dev/null || sudo podman ps --filter name=${APP_NAME} --format '{{.Status}}' 2>/dev/null")

if echo "$CONTAINER_STATUS" | grep -q "Up"; then
  log "✓ Container is running: $CONTAINER_STATUS"
else
  log_error "✗ Container is not running properly"
  log "Status: $CONTAINER_STATUS"
  log "Check logs with: ssh $RASPI_HOST 'podman logs ${APP_NAME}' or 'sudo podman logs ${APP_NAME}'"
  exit 1
fi

# Test application health endpoint
log "Testing application health endpoint..."

if ssh "$RASPI_HOST" "curl -k -f -s https://localhost:${CONTAINER_PORT}/actuator/health >/dev/null 2>&1"; then
  log "✓ Application health check passed"
  # Get actual health status
  HEALTH_STATUS=$(ssh "$RASPI_HOST" "curl -k -s https://localhost:${CONTAINER_PORT}/actuator/health" 2>/dev/null)
  log "  Health status: $HEALTH_STATUS"
else
  log "⚠ Application health check failed - application may still be starting"
  log "Wait 30 seconds and check manually:"
  log "  ssh $RASPI_HOST 'curl -k https://localhost:${CONTAINER_PORT}/actuator/health'"
fi

# Get Raspberry Pi IP for external access instructions
RASPI_IP=$(ssh "$RASPI_HOST" "hostname -I | awk '{print \$1}'" 2>/dev/null)

# Step 12: Display deployment summary
log ""
log "=== Deployment Summary ==="
log "✓ Target: $RASPI_HOST ($RASPI_ARCH)"
log "✓ Container Runtime: Podman ($PODMAN_VERSION)"
log "✓ Application: $CONTAINER_STATUS"
log "✓ PostgreSQL: $POSTGRES_IP:5432 (external)"
log "✓ Environment: $ENV_FILE"
log ""
log "Access URLs:"
log "  HTTPS (local): https://${RASPI_IP}:${CONTAINER_PORT}/"
log "  Health: https://${RASPI_IP}:${CONTAINER_PORT}/actuator/health"
log "  GraphQL: https://${RASPI_IP}:${CONTAINER_PORT}/graphql"
log "  GraphiQL: https://${RASPI_IP}:${CONTAINER_PORT}/graphiql"
log "  Metrics: https://${RASPI_IP}:${CONTAINER_PORT}/actuator/metrics"
log ""
log "Monitoring Commands:"
log "  Container logs: ssh $RASPI_HOST 'podman logs ${APP_NAME} -f'"
log "  Container status: ssh $RASPI_HOST 'podman ps'"
log "  Container inspect: ssh $RASPI_HOST 'podman inspect ${APP_NAME}'"
log "  Network inspect: ssh $RASPI_HOST 'podman network inspect finance-lan'"
log ""
log "Management Commands:"
log "  Stop container: ssh $RASPI_HOST 'podman stop ${APP_NAME}'"
log "  Start container: ssh $RASPI_HOST 'podman start ${APP_NAME}'"
log "  Restart container: ssh $RASPI_HOST 'podman restart ${APP_NAME}'"
log "  Restart deployment: ./deploy-raspi.sh"
log ""
log "Database Operations:"
log "  Connect to PostgreSQL: ssh $POSTGRES_IP 'psql -U henninb -d finance_db'"
log "  Test DB from Raspi: ssh $RASPI_HOST 'timeout 5 bash -c \"cat < /dev/null > /dev/tcp/$POSTGRES_IP/5432\"'"
log ""
log "Troubleshooting:"
log "  View logs: ssh $RASPI_HOST 'podman logs ${APP_NAME} --tail 100'"
log "  Shell access: ssh $RASPI_HOST 'podman exec -it ${APP_NAME} /bin/bash'"
log "  Database test: ssh $RASPI_HOST 'timeout 5 bash -c \"cat < /dev/null > /dev/tcp/$POSTGRES_IP/5432\"'"
log "  Check health: curl -k https://${RASPI_IP}:${CONTAINER_PORT}/actuator/health"

exit 0
