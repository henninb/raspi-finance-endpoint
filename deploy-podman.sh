#!/bin/bash

# Podman deployment script: rsync + SSH heredoc approach
# Parallel to deploy-proxmox.sh but uses rsync file sync and direct podman
# commands rather than CONTAINER_HOST tunneling.

REMOTE_HOST="debian-dockerserver"
REMOTE_USER="henninb"
REMOTE_DIR="/home/${REMOTE_USER}/raspi-finance-endpoint"
HOST_IP="192.168.10.10"
ENV_FILE="env.prod"

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

  if [ ! -d "ssl" ]; then
    log_error "SSL directory 'ssl/' not found!"
    log_error "Please ensure Let's Encrypt certificates are copied to ssl/ directory."
    return 1
  fi

  if [ ! -f "$cert_path" ]; then
    log_error "SSL certificate not found: $cert_path"
    return 1
  fi

  if [ ! -f "$key_path" ]; then
    log_error "SSL private key not found: $key_path"
    return 1
  fi

  log "Checking certificate expiration..."
  if ! openssl x509 -in "$cert_path" -noout -checkend 86400 >/dev/null 2>&1; then
    log_error "SSL certificate is expired or will expire within 24 hours!"
    openssl x509 -in "$cert_path" -noout -dates 2>/dev/null | grep -E "notAfter|notBefore" || true
    return 1
  fi

  local cert_expiry
  cert_expiry=$(openssl x509 -in "$cert_path" -noout -enddate 2>/dev/null | cut -d= -f2)
  log "✓ Certificate is valid until: $cert_expiry"

  log "Validating certificate and private key match..."
  local cert_hash key_hash
  cert_hash=$(openssl x509 -in "$cert_path" -noout -pubkey 2>/dev/null | openssl sha256 2>/dev/null)
  key_hash=$(openssl pkey -in "$key_path" -pubout 2>/dev/null | openssl sha256 2>/dev/null)

  if [ "$cert_hash" != "$key_hash" ]; then
    log_error "SSL certificate and private key do not match!"
    return 1
  fi
  log "✓ Certificate and private key match"

  if [ -f "$keystore_path" ]; then
    log "Existing keystore found, validating..."
    if keytool -list -keystore "$keystore_path" -alias "$keystore_alias" -storepass "$keystore_password" >/dev/null 2>&1; then
      local keystore_cert_hash
      keystore_cert_hash=$(keytool -exportcert -keystore "$keystore_path" -alias "$keystore_alias" -storepass "$keystore_password" -rfc 2>/dev/null | openssl x509 -noout -pubkey 2>/dev/null | openssl sha256 2>/dev/null)
      if [ "$cert_hash" = "$keystore_cert_hash" ]; then
        log "✓ Existing keystore is valid and up-to-date"
        return 0
      else
        log "Keystore certificate doesn't match current certificate, regenerating..."
        rm -f "$keystore_path"
      fi
    else
      log "Existing keystore is invalid or corrupted, regenerating..."
      rm -f "$keystore_path"
    fi
  fi

  log "Creating new PKCS12 keystore..."
  if ! openssl pkcs12 -export -in "$cert_path" -inkey "$key_path" -out "$keystore_path" -name "$keystore_alias" -passout "pass:$keystore_password" 2>/dev/null; then
    log_error "Failed to create PKCS12 keystore!"
    return 1
  fi

  if [ ! -f "$keystore_path" ]; then
    log_error "Keystore file was not created: $keystore_path"
    return 1
  fi

  log "Verifying keystore integrity..."
  if ! keytool -list -keystore "$keystore_path" -alias "$keystore_alias" -storepass "$keystore_password" >/dev/null 2>&1; then
    log_error "Keystore verification failed!"
    return 1
  fi

  chmod 600 "$keystore_path" 2>/dev/null || {
    log_error "Warning: Could not set secure permissions on keystore file"
  }

  log "✓ PKCS12 keystore created successfully: $keystore_path"

  local cert_subject
  cert_subject=$(openssl x509 -in "$cert_path" -noout -subject 2>/dev/null | sed 's/subject=//')
  log "✓ Certificate subject: $cert_subject"

  return 0
}

setup_ssh() {
  log "Testing SSH connectivity to ${REMOTE_HOST}..."
  if ! ssh -q -o BatchMode=yes -o ConnectTimeout=5 "${REMOTE_HOST}" exit 2>/dev/null; then
    log_error "Cannot connect to ${REMOTE_HOST} via SSH"
    log "Please ensure:"
    log "  1. ${REMOTE_HOST} is defined in ~/.ssh/config"
    log "  2. SSH service is running on the target host"
    log "  3. SSH key authentication is configured"
    exit 1
  fi
  log "✓ SSH connectivity verified for ${REMOTE_HOST}"

  if ! ssh-add -l >/dev/null 2>&1; then
    local key_path="$HOME/.ssh/id_rsa"
    if [ -f "$key_path" ]; then
      log "Starting SSH agent and adding default key..."
      eval "$(ssh-agent -s)"
      export SSH_AUTH_SOCK SSH_AGENT_PID
      ssh-add "$key_path" 2>/dev/null || log "SSH key requires passphrase - please run: ssh-add $key_path"
    fi
  else
    log "✓ SSH agent is running with keys loaded"
  fi

  CURRENT_UID="$(ssh "${REMOTE_HOST}" id -u 2>/dev/null)"
  CURRENT_GID="$(ssh "${REMOTE_HOST}" id -g 2>/dev/null)"
  USERNAME="$(ssh "${REMOTE_HOST}" whoami 2>/dev/null)"

  if [ -z "$CURRENT_UID" ] || [ -z "$CURRENT_GID" ] || [ -z "$USERNAME" ]; then
    log_error "Failed to retrieve user information from ${REMOTE_HOST}"
    exit 1
  fi

  export CURRENT_UID CURRENT_GID USERNAME
  log "✓ Remote user: ${USERNAME} (UID=${CURRENT_UID}, GID=${CURRENT_GID})"
}

log "=== Podman Deployment to ${REMOTE_HOST} ==="

# --- Decrypt env.secrets ---
if [ -f "env.secrets.enc" ]; then
  if command -v sops >/dev/null 2>&1; then
    log "Decrypting env.secrets.enc with SOPS..."
    if sops -d --input-type dotenv --output-type dotenv env.secrets.enc > env.secrets; then
      chmod 600 env.secrets
      log "✓ env.secrets decrypted successfully"
    else
      log_error "SOPS decryption failed. Check that the age private key is available."
      log_error "Expected at: ~/.config/sops/age/keys.txt (or set SOPS_AGE_KEY_FILE)"
      exit 1
    fi
  else
    log_error "sops is not installed but env.secrets.enc exists."
    exit 1
  fi
elif [ ! -f "env.secrets" ]; then
  log_error "Neither env.secrets.enc nor env.secrets found."
  exit 1
fi

set -a
. "./env.secrets"
set +a

# --- Step 0: SSL validation ---
log "Step 0: SSL Certificate and Keystore Validation"
if ! validate_and_create_keystore; then
  log_error "SSL keystore validation/creation failed!"
  log_error ""
  log_error "Please ensure:"
  log_error "  1. Let's Encrypt certificates are current and not expired"
  log_error "  2. Certificate files exist in ssl/ directory:"
  log_error "     - ssl/bhenning.fullchain.pem"
  log_error "     - ssl/bhenning.privkey.pem"
  log_error "  3. SSL_KEY_STORE_PASSWORD is set in env.secrets"
  log_error "  4. OpenSSL and keytool are installed"
  exit 1
fi
log "✓ SSL keystore validation completed"

# --- Step 1: SSH setup ---
log "Step 1: SSH Setup"
setup_ssh

# --- Step 1.5: InfluxDB admin token ---
log "Step 1.5: Deploying InfluxDB admin token to ${REMOTE_HOST}..."
if [ -z "$INFLUXDB_TOKEN" ]; then
  log_error "INFLUXDB_TOKEN is not set in env.secrets"
  exit 1
fi
ssh "${REMOTE_HOST}" "sudo mkdir -p /opt/influxdb3 && sudo rm -f /opt/influxdb3/admin-token && printf '{\"token\": \"%s\", \"name\": \"_admin\"}' '${INFLUXDB_TOKEN}' | sudo tee /opt/influxdb3/admin-token > /dev/null && sudo chown 1000:1000 /opt/influxdb3/admin-token && sudo chmod 600 /opt/influxdb3/admin-token"
log "✓ InfluxDB admin token deployed to ${REMOTE_HOST}:/opt/influxdb3/admin-token"

# --- Step 2: Gradle build ---
log "Step 2: Building application with Gradle..."
if ! ./gradlew clean build -x test; then
  log_error "Gradle build failed"
  exit 1
fi
log "✓ Gradle build succeeded"

# --- Step 3: rsync ---
log "Step 3: Syncing project files to ${REMOTE_USER}@${REMOTE_HOST}:${REMOTE_DIR}..."
ssh "${REMOTE_USER}@${REMOTE_HOST}" "mkdir -p ${REMOTE_DIR}"
rsync -av \
  --exclude='.git/' \
  --exclude='influxdb-data/' \
  --exclude='grafana-data/' \
  --exclude='logs/' \
  --exclude='build/' \
  --exclude='.gradle/' \
  ./ "${REMOTE_USER}@${REMOTE_HOST}:${REMOTE_DIR}/"
log "✓ Files synced to ${REMOTE_DIR}"

# --- Step 4: Remote deployment ---
log "Step 4: Building and deploying containers on ${REMOTE_HOST}..."
ssh -T "${REMOTE_USER}@${REMOTE_HOST}" \
  REMOTE_DIR="${REMOTE_DIR}" \
  CURRENT_UID="${CURRENT_UID}" \
  CURRENT_GID="${CURRENT_GID}" \
  USERNAME="${USERNAME}" \
  HOST_IP="${HOST_IP}" \
  ENV_FILE="${ENV_FILE}" \
  'bash -s' << 'ENDSSH'
set -e

cd "${REMOTE_DIR}"

echo "Creating finance-lan network if needed..."
if ! podman network ls --filter "name=^finance-lan$" -q | grep -q .; then
  podman network create finance-lan
  echo "✓ Created finance-lan network"
else
  echo "✓ finance-lan network already exists"
fi

echo "Stopping and removing existing containers..."
for container in raspi-finance-endpoint influxdb-server; do
  if podman ps -a --format "{{.Names}}" | grep -q "^${container}$"; then
    echo "  Stopping and removing ${container}..."
    podman stop "${container}" 2>/dev/null || true
    podman rm -f "${container}" 2>/dev/null || true
  fi
done

echo "Removing old raspi-finance-endpoint image..."
podman rmi -f raspi-finance-endpoint 2>/dev/null || true

echo "Cleaning up dangling images..."
dangling=$(podman images -q -f dangling=true 2>/dev/null)
if [ -n "${dangling}" ]; then
  podman rmi -f ${dangling} 2>/dev/null || true
fi

echo "Deploying with podman-compose..."
export CURRENT_UID="${CURRENT_UID}"
export CURRENT_GID="${CURRENT_GID}"
export USERNAME="${USERNAME}"
export HOST_IP="${HOST_IP}"
export ENV_FILE="${ENV_FILE}"

if ! podman-compose -f docker-compose-base.yml -f docker-compose-prod.yml -f docker-compose-influxdb.yml up -d; then
  echo "ERROR: podman-compose failed"
  exit 1
fi
echo "✓ podman-compose up succeeded"

echo "Discovering influxdb volume name..."
INFLUXDB_VOLUME=$(podman volume ls --format "{{.Name}}" | grep "influxdb" | head -1)
if [ -z "${INFLUXDB_VOLUME}" ]; then
  INFLUXDB_VOLUME="raspi-finance-endpoint_influxdb-data"
fi
echo "  Using influxdb volume: ${INFLUXDB_VOLUME}"

echo "Writing Quadlet files for auto-start on boot..."
mkdir -p ~/.config/containers/systemd

cat > ~/.config/containers/systemd/influxdb-server.container << EOF
[Unit]
Description=InfluxDB Server
After=network-online.target

[Container]
Image=influxdb:3-core
ContainerName=influxdb-server
HostName=influxdb-server
PublishPort=192.168.10.10:8086:8086
Volume=${INFLUXDB_VOLUME}:/var/lib/influxdb3:rw
Volume=/opt/influxdb3/admin-token:/run/influxdb3/admin-token:ro
Network=finance-lan
Environment=INFLUXDB3_ADMIN_TOKEN_FILE=/run/influxdb3/admin-token
EnvironmentFile=${REMOTE_DIR}/env.influx
EnvironmentFile=${REMOTE_DIR}/env.secrets
Exec=serve --node-id influxdb-server --http-bind 0.0.0.0:8086 --package-manager disabled --disable-authz ping --wal-snapshot-size 300
NoNewPrivileges=true
DropCapability=ALL
User=1000:1000
Tmpfs=/tmp:noexec,nosuid,size=50m

[Service]
Restart=always
TimeoutStartSec=120

[Install]
WantedBy=default.target
EOF

cat > ~/.config/containers/systemd/raspi-finance-endpoint.container << EOF
[Unit]
Description=Raspi Finance Endpoint
After=network-online.target influxdb-server.service

[Container]
Image=localhost/raspi-finance-endpoint
ContainerName=raspi-finance-endpoint
HostName=hornsup-endpoint
PublishPort=192.168.10.10:8443:8443
Volume=${REMOTE_DIR}/json_in:/opt/raspi-finance-endpoint/json_in:rw
Volume=${REMOTE_DIR}/ssl:/opt/raspi-finance-endpoint/ssl:ro
Network=finance-lan
EnvironmentFile=${REMOTE_DIR}/env.secrets
EnvironmentFile=${REMOTE_DIR}/env.prod
AddHost=hornsup:${HOST_IP}
AddHost=raspi:192.168.10.25
AddHost=finance-db.lan:${HOST_IP}
PullPolicy=never
NoNewPrivileges=true
DropCapability=ALL
AddCapability=CHOWN
AddCapability=SETGID
AddCapability=SETUID
User=${CURRENT_UID}:${CURRENT_GID}
Tmpfs=/tmp:noexec,nosuid,size=100m

[Service]
Restart=always
TimeoutStartSec=120

[Install]
WantedBy=default.target
EOF

echo "✓ Quadlet files written to ~/.config/containers/systemd/"

export XDG_RUNTIME_DIR=${XDG_RUNTIME_DIR:-/run/user/$(id -u)}
export DBUS_SESSION_BUS_ADDRESS=${DBUS_SESSION_BUS_ADDRESS:-unix:path=/run/user/$(id -u)/bus}
systemctl --user daemon-reload 2>/dev/null || true
echo "✓ Systemd user daemon reloaded"
echo "NOTE: Run 'sudo loginctl enable-linger ${USERNAME}' on this host to enable auto-start on reboot."

podman ps -a
ENDSSH

# --- Step 5: Verify ---
log "Step 5: Verifying deployment..."
sleep 5

log "Checking container status..."
app_status=$(ssh "${REMOTE_HOST}" "podman ps --filter name=raspi-finance-endpoint --format '{{.Status}}'" 2>/dev/null)
influx_status=$(ssh "${REMOTE_HOST}" "podman ps --filter name=influxdb-server --format '{{.Status}}'" 2>/dev/null)

if echo "$app_status" | grep -q "Up"; then
  log "✓ raspi-finance-endpoint is running: $app_status"
else
  log_error "✗ raspi-finance-endpoint is not running"
  log "Check logs: ssh ${REMOTE_HOST} 'podman logs raspi-finance-endpoint'"
  exit 1
fi

if echo "$influx_status" | grep -q "Up"; then
  log "✓ influxdb-server is running: $influx_status"
else
  log "⚠ influxdb-server is not running: $influx_status"
fi

log "Testing application health..."
if ssh "${REMOTE_HOST}" 'curl -k -f -s https://localhost:8443/actuator/health >/dev/null 2>&1'; then
  log "✓ Application health check passed"
else
  log "⚠ Health check failed - application may still be starting"
fi

log "Testing LAN access..."
if curl -k -f -s --connect-timeout 10 "https://${HOST_IP}:8443/actuator/health" >/dev/null 2>&1; then
  log "✓ LAN HTTPS access working: https://${HOST_IP}:8443/"
else
  log "⚠ LAN HTTPS access test failed (may be normal if DNS/routing not configured)"
fi

log ""
log "=== Deployment Summary ==="
log "✓ Host: ${REMOTE_HOST} (${HOST_IP})"
log "✓ Remote directory: ${REMOTE_DIR}"
log "✓ Application: $(echo "$app_status" | head -1)"
log "✓ InfluxDB: $(echo "$influx_status" | head -1)"
log ""
log "Access URLs:"
log "  HTTPS:   https://${HOST_IP}:8443/"
log "  Health:  https://${HOST_IP}:8443/actuator/health"
log "  GraphQL: https://${HOST_IP}:8443/graphql"
log "  GraphiQL: https://${HOST_IP}:8443/graphiql"
log "  Metrics: https://${HOST_IP}:8443/actuator/metrics"
log ""
log "Monitoring commands:"
log "  App logs:     ssh ${REMOTE_HOST} 'podman logs raspi-finance-endpoint -f'"
log "  InfluxDB logs: ssh ${REMOTE_HOST} 'podman logs influxdb-server -f'"
log "  Container status: ssh ${REMOTE_HOST} 'podman ps'"
log "  Network info: ssh ${REMOTE_HOST} 'podman network inspect finance-lan'"
log ""
log "Quadlet (auto-start on reboot):"
log "  Files: ssh ${REMOTE_HOST} 'ls ~/.config/containers/systemd/'"
log "  Enable linger: ssh ${REMOTE_HOST} 'sudo loginctl enable-linger ${USERNAME}'"
log ""
log "Troubleshooting:"
log "  Re-run deployment: ./deploy-podman.sh"
log "  App diagnostics: ssh ${REMOTE_HOST} 'curl -k -s https://localhost:8443/actuator/info'"

exit 0
