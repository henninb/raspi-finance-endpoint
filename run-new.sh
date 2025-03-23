#!/usr/bin/env sh

# Log function for timestamped messages
log() {
  echo "$(date +"%Y-%m-%d %H:%M:%S") - $*"
}

# Ensure exactly one argument is provided: local or remote
if [ $# -ne 1 ]; then
  log "Usage: $0 <local|remote>"
  exit 1
fi

env=$1

if [ "$env" != "local" ] && [ "$env" != "remote" ]; then
  log "Usage: $0 <local|remote>"
  exit 2
fi

log "Starting deployment in '$env' environment."

KEY_PATH="$HOME/.ssh/id_rsa_gcp"
# Get the fingerprint of your key
KEY_FINGERPRINT=$(ssh-keygen -lf "$KEY_PATH" | awk '{print $2}')

# Set HOST_IP as the database IP depending on deployment target.
if [ "$env" = "local" ]; then
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

# export APPNAME=raspi-finance-endpoint
log "Current UID: $CURRENT_UID, GID: $CURRENT_GID"

# Create necessary directories
log "Creating necessary directories..."
mkdir -p 'influxdb-data'
mkdir -p 'grafana-data'
mkdir -p 'logs'
mkdir -p 'ssl'
mkdir -p 'excel_in'

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

# For remote deployments, adjust the Docker host context.
if [ "$env" = "remote" ]; then
  log "Setting DOCKER_HOST for remote deployment to ssh://gcp-api..."
  export DOCKER_HOST=ssh://brianhenning@34.132.189.202
  export DOCKER_HOST=ssh://gcp-api
  docker context ls
  # ssh-add ~/.ssh/id_rsa_gcp
  # Check if the key is already added
  if ssh-add -l | grep -q "$KEY_FINGERPRINT"; then
    echo "SSH key already added."
  else
    echo "Adding SSH key..."
    ssh-add "$KEY_PATH"
  fi
else
  log "Setting DOCKER_HOST for local deployment..."
  export DOCKER_HOST=ssh://192.168.10.10
fi

# Docker-related commands
if [ -x "$(command -v docker)" ]; then
  log "Docker detected. Cleaning up dangling images and volumes..."
  docker rmi -f "$(docker images -q -f dangling=true)" 2> /dev/null
  docker volume prune -f 2> /dev/null

  if [ "$env" = "local" ]; then
    log "Local environment detected. Cleaning up existing nginx, varnish, and raspi containers..."

    nginx_container=$(docker ps -a -f 'name=nginx-server' --format "{{.ID}}") 2> /dev/null
    if [ -n "${nginx_container}" ]; then
      log "Stopping and removing existing nginx container(s)..."
      docker stop "${nginx_container}"
      docker rm -f "${nginx_container}" 2> /dev/null
    fi

    varnish_container=$(docker ps -a -f 'name=varnish-server' --format "{{.ID}}") 2> /dev/null
    if [ -n "${varnish_container}" ]; then
      log "Stopping and removing existing varnish container(s)..."
      docker stop "${varnish_container}"
      docker rm -f "${varnish_container}" 2> /dev/null
    fi

    raspi_container=$(docker ps -a -f 'name=raspi-finance-endpoint' --format "{{.ID}}") 2> /dev/null
    if [ -n "${raspi_container}" ]; then
      log "Stopping and removing existing raspi-finance-endpoint container..."
      docker stop "${raspi_container}"
      docker rm -f "${raspi_container}" 2> /dev/null
      docker rmi -f raspi-finance-endpoint
    fi

    log "Building images/deploying images using docker-compose (including varnish)..."
    if ! docker compose -f docker-compose-base.yml -f docker-compose-prod.yml -f docker-compose-varnish.yml up -d; then
      log "docker-compose build failed for local deployment."
    else
      log "docker-compose build succeeded for local deployment."
    fi
  else
    log "Remote environment detected. Cleaning up existing raspi-finance-endpoint container..."

    raspi_container=$(docker ps -a -f 'name=raspi-finance-endpoint' --format "{{.ID}}") 2> /dev/null
    if [ -n "${raspi_container}" ]; then
      log "Stopping and removing existing raspi-finance-endpoint container..."
      docker stop "${raspi_container}"
      docker rm -f "${raspi_container}" 2> /dev/null
      docker rmi -f raspi-finance-endpoint
    fi

    # log "change the port to 80"
    # sed -i 's/^\(SERVER_PORT=\)[0-9]\+/\180/' env.prod

    log "Building/deploying images using docker-compose (without nginx or varnish)..."
    if ! docker compose -f docker-compose-base.yml -f docker-compose-prod.yml up -d; then
      log "docker-compose build failed for remote deployment."
    else
      log "docker-compose build succeeded for remote deployment."
    fi
  fi
    # log "change the port to 8443"
    # sed -i 's/^\(SERVER_PORT=\)[0-9]\+/\18443/' env.prod

  log "Docker detected. Cleaning up dangling images and volumes..."
  docker rmi -f "$(docker images -q -f dangling=true)" 2> /dev/null
  docker volume prune -f 2> /dev/null
else
  log "Docker command not found. Exiting."
  exit 1
fi


# Run the raspi-finance-endpoint container.
# For remote, ensure any preexisting container is deleted.
# log "Deleting any preexisting raspi-finance-endpoint container..."
# docker rm -f raspi-finance-endpoint

# log "Running raspi-finance-endpoint container..."
# docker run --name=raspi-finance-endpoint -h raspi-finance-endpoint --restart unless-stopped -p 8443:8443 -d raspi-finance-endpoint

if ! docker network ls --filter "name=^finance-lan$" -q | grep -q .; then
  echo "Creating network finance-lan..."
  docker network create finance-lan
else
  echo "Network finance-lan already exists."
fi

# if [ "$env" = "local" ]; then
  # log "Running nginx container for local deployment..."
  # docker rm -f nginx-proxy-finance-server
  # docker run --name=nginx-proxy-finance-server -h nginx-proxy-finance-server --restart unless-stopped -p 9443:443 -d nginx-proxy-finance-server
# fi

docker network connect finance-lan raspi-finance-endpoint
log "list networks"
docker network ls

log "Deployment complete."
log "To follow logs, run: docker logs raspi-finance-endpoint --follow"
exit 0

