#!/usr/bin/env sh

# Log function for timestamped messages
log() {
  echo "$(date +"%Y-%m-%d %H:%M:%S") - $*"
}

# Ensure exactly one argument is provided: proxmox or gcp
if [ $# -ne 1 ]; then
  log "Usage: $0 <proxmox|gcp>"
  exit 1
fi

env=$1

if [ "$env" != "proxmox" ] && [ "$env" != "gcp" ]; then
  log "Usage: $0 <proxmox|gcp>"
  exit 2
fi

log "Starting deployment in '$env' environment."

KEY_PATH="$HOME/.ssh/id_rsa_gcp"
# Get the fingerprint of your key
KEY_FINGERPRINT=$(ssh-keygen -lf "$KEY_PATH" | awk '{print $2}')

# Set HOST_IP as the database IP depending on deployment target.
if [ "$env" = "proxmox" ]; then
  HOST_IP="192.168.10.10"
  export CURRENT_UID="$(ssh debian-dockerserver id -u)"
  export CURRENT_GID="$(ssh debian-dockerserver id -g)"
  export USERNAME="$(ssh debian-dockerserver whoami)"
else
  export CURRENT_UID="$(ssh gcp-api id -u)"
  export CURRENT_GID="$(ssh gcp-api id -g)"
  export USERNAME="$(ssh gcp-api whoami)"
  HOST_IP="172.19.0.2"
fi
export HOST_IP
log "Database host (HOST_IP) set to: $HOST_IP"
log "USERNAME set to: $USERNAME"
log "Current UID: $CURRENT_UID, GID: $CURRENT_GID"

# Create necessary directories
log "Creating necessary directories..."
mkdir -p 'influxdb-data'
mkdir -p 'grafana-data'
mkdir -p 'logs'
mkdir -p 'ssl'

# Preserve local secret changes
log "Preserving local secret changes..."
git update-index --assume-unchanged env.secrets

chmod +x gradle/wrapper/gradle-wrapper.jar

# Build the project with gradle (excluding tests)
log "Building project with gradle..."
if ! ./gradlew clean build -x test; then
  log "Gradle build failed."
  exit 1
fi
log "Gradle build succeeded."

# For gcp deployments, adjust the Podman host context.
if [ "$env" = "gcp" ]; then
  log "Setting CONTAINER_HOST for gcp deployment to ssh://gcp-api..."
  export CONTAINER_HOST=ssh://gcp-api
  podman system connection list
  # Check if the key is already added
  if ssh-add -l | grep -q "$KEY_FINGERPRINT"; then
    echo "SSH key already added."
  else
    echo "Adding SSH key..."
    ssh-add "$KEY_PATH"
  fi
else
  log "Setting CONTAINER_HOST for proxmox deployment..."
  export CONTAINER_HOST=ssh://192.168.10.10
fi

# Podman-related commands
if [ -x "$(command -v podman)" ]; then
  log "Podman detected. Cleaning up dangling images and volumes..."
  podman rmi -f "$(podman images -q -f dangling=true)" 2> /dev/null
  podman volume prune -f 2> /dev/null

  if [ "$env" = "proxmox" ]; then
    log "Proxmox environment detected. Cleaning up existing nginx, varnish, and raspi containers..."

    nginx_container=$(podman ps -a -f 'name=nginx-server' --format "{{.ID}}") 2> /dev/null
    if [ -n "${nginx_container}" ]; then
      log "Stopping and removing existing nginx container(s)..."
      podman stop "${nginx_container}"
      podman rm -f "${nginx_container}" 2> /dev/null
    fi

    varnish_container=$(podman ps -a -f 'name=varnish-server' --format "{{.ID}}") 2> /dev/null
    if [ -n "${varnish_container}" ]; then
      log "Stopping and removing existing varnish container(s)..."
      podman stop "${varnish_container}"
      podman rm -f "${varnish_container}" 2> /dev/null
    fi

    raspi_container=$(podman ps -a -f 'name=raspi-finance-endpoint' --format "{{.ID}}") 2> /dev/null
    if [ -n "${raspi_container}" ]; then
      log "Stopping and removing existing raspi-finance-endpoint container..."
      podman stop "${raspi_container}"
      podman rm -f "${raspi_container}" 2> /dev/null
      podman rmi -f raspi-finance-endpoint
    fi

    log "Building images/deploying images using podman-compose (including varnish)..."
    if ! podman compose -f docker-compose-base.yml -f docker-compose-prod.yml -f docker-compose-varnish.yml up -d; then
      log "podman-compose build failed for proxmox deployment."
    else
      log "podman-compose build succeeded for proxmox deployment."
    fi
  else
    log "GCP environment detected. Cleaning up existing raspi-finance-endpoint container..."

    raspi_container=$(podman ps -a -f 'name=raspi-finance-endpoint' --format "{{.ID}}") 2> /dev/null
    if [ -n "${raspi_container}" ]; then
      log "Stopping and removing existing raspi-finance-endpoint container..."
      podman stop "${raspi_container}"
      podman rm -f "${raspi_container}" 2> /dev/null
      podman rmi -f raspi-finance-endpoint
    fi

    log "Building/deploying images using podman-compose (without nginx or varnish)..."
    if ! podman compose -f docker-compose-base.yml -f docker-compose-prod.yml up -d; then
      log "podman-compose build failed for gcp deployment."
    else
      log "podman-compose build succeeded for gcp deployment."
    fi
  fi

  log "Podman detected. Cleaning up dangling images and volumes..."
  podman rmi -f "$(podman images -q -f dangling=true)" 2> /dev/null
  podman volume prune -f 2> /dev/null
else
  log "Podman command not found. Exiting."
  exit 1
fi

# Ensure the custom network exists, then connect the raspi-finance-endpoint container to it.
if ! podman network ls --filter "name=^finance-lan$" -q | grep -q .; then
  echo "Creating network finance-lan..."
  podman network create finance-lan
else
  echo "Network finance-lan already exists."
fi

podman network connect finance-lan raspi-finance-endpoint
log "List networks"
podman network ls

log "Deployment complete."
log "To follow logs, run: podman logs raspi-finance-endpoint --follow"
exit 0
