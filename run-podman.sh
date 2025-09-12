#!/usr/bin/env sh

# Log function for timestamped messages
log() {
  echo "$(date +"%Y-%m-%d %H:%M:%S") - $*"
}

# Function to validate environment secrets
validate_env_secrets() {
  local env_file="env.secrets"
  local missing_keys=""
  local required_keys="DATASOURCE_PASSWORD INFLUXDB_ADMIN_PASSWORD SSL_KEY_PASSWORD SSL_KEY_STORE_PASSWORD SYS_PASSWORD BASIC_AUTH_PASSWORD JWT_KEY"

  log "Validating environment secrets..."

  # Check if env.secrets file exists
  if [ ! -f "$env_file" ]; then
    log "ERROR: $env_file file not found!"
    log "Please create $env_file with the required environment variables."
    exit 1
  fi

  # Source the env.secrets file to check values
  # shellcheck disable=SC1090
  if [ -f "$env_file" ]; then
    . "./$env_file"
  fi

  # Check each required key (using sh-compatible approach)
  for key in $required_keys; do
    case $key in
      "DATASOURCE_PASSWORD")
        value="$DATASOURCE_PASSWORD" ;;
      "INFLUXDB_ADMIN_PASSWORD")
        value="$INFLUXDB_ADMIN_PASSWORD" ;;
      "SSL_KEY_PASSWORD")
        value="$SSL_KEY_PASSWORD" ;;
      "SSL_KEY_STORE_PASSWORD")
        value="$SSL_KEY_STORE_PASSWORD" ;;
      "SYS_PASSWORD")
        value="$SYS_PASSWORD" ;;
      "BASIC_AUTH_PASSWORD")
        value="$BASIC_AUTH_PASSWORD" ;;
      "JWT_KEY")
        value="$JWT_KEY" ;;
      *)
        value="" ;;
    esac

    if [ -z "$value" ] || [ "$value" = "" ]; then
      if [ -z "$missing_keys" ]; then
        missing_keys="$key"
      else
        missing_keys="$missing_keys $key"
      fi
    fi
  done

  # If any keys are missing, prompt user and exit
  if [ -n "$missing_keys" ]; then
    log "ERROR: The following required environment variables are missing or empty in $env_file:"
    for key in $missing_keys; do
      log "  - $key"
    done
    log ""
    log "Please set values for these variables in $env_file before running the application."
    log "Example format:"
    log "  DATASOURCE_PASSWORD=your_database_password"
    log "  JWT_KEY=your_jwt_secret_key"
    log ""
    exit 1
  fi

  log "✓ All required environment secrets are properly configured."
}

# Validate environment secrets before proceeding
validate_env_secrets

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

log "Starting deployment in '$env' environment using Podman."

KEY_PATH="$HOME/.ssh/id_rsa_gcp"
# Get the fingerprint of your key
KEY_FINGERPRINT=$(ssh-keygen -lf "$KEY_PATH" | awk '{print $2}')

if [ "$env" = "gcp" ]; then
  if ssh-add -l | grep -q "$KEY_FINGERPRINT"; then
    echo "SSH key already added."
  else
    echo ssh-add "$KEY_PATH"
    echo "Adding SSH key..."
    ssh-add "$KEY_PATH"
  fi
fi

# Set HOST_IP as the database IP depending on deployment target.
if [ "$env" = "proxmox" ]; then
  HOST_IP="192.168.10.10"
  export CURRENT_UID="$(ssh debian-dockerserver id -u)"
  export CURRENT_GID="$(ssh debian-dockerserver id -g)"
  export USERNAME="$(ssh debian-dockerserver whoami)"
  # Use env.prod for Proxmox (InfluxDB enabled)
  ENV_FILE="env.prod"
else
  export CURRENT_UID="$(ssh gcp-api id -u)"
  export CURRENT_GID="$(ssh gcp-api id -g)"
  export USERNAME="$(ssh gcp-api whoami)"
  HOST_IP="172.19.0.2"
  # Use env.gcp for GCP (InfluxDB disabled)
  ENV_FILE="env.gcp"
fi
export HOST_IP
export ENV_FILE
log "Database host (HOST_IP) set to: $HOST_IP"
log "Environment file (ENV_FILE) set to: $ENV_FILE"
log "USERNAME set to: $USERNAME"

# export APPNAME=raspi-finance-endpoint
log "Current UID: $CURRENT_UID, GID: $CURRENT_GID"

# Create necessary directories
log "Creating necessary directories..."
mkdir -p 'influxdb-data'
mkdir -p 'grafana-data'
mkdir -p 'logs'
mkdir -p 'ssl'

# Fix directory permissions for Docker volume mounts (required for non-root containers)
log "Fixing directory permissions for Docker volume mounts..."
fix_docker_permissions() {
  local dir="$1"
  local description="$2"

  if [ -d "$dir" ]; then
    log "  Setting permissions for $description: $dir"
    # Create subdirectories as needed
    if [[ "$dir" == "logs" ]]; then
      mkdir -p "$dir/archive"
    fi
    # Set ownership and permissions
    if [ "$env" = "proxmox" ]; then
      ssh debian-dockerserver "cd ~/raspi-finance-endpoint && sudo chown -R $CURRENT_UID:$CURRENT_GID '$dir' && find '$dir' -type d -exec chmod 755 {} \; && find '$dir' -type f -exec chmod 644 {} \;"
    else
      ssh gcp-api "cd ~/raspi-finance-endpoint && sudo chown -R $CURRENT_UID:$CURRENT_GID '$dir' && find '$dir' -type d -exec chmod 755 {} \; && find '$dir' -type f -exec chmod 644 {} \;"
    fi
    log "  ✓ Fixed permissions for $description"
  fi
}

fix_docker_permissions "logs" "Application logs directory"
fix_docker_permissions "json_in" "JSON input directory"
fix_docker_permissions "influxdb-data" "InfluxDB data directory"
fix_docker_permissions "grafana-data" "Grafana data directory"
if [ -d "ssl" ]; then
  fix_docker_permissions "ssl" "SSL certificates directory"
fi

# Preserve local secret changes
log "Preserving local secret changes..."
git update-index --assume-unchanged env.secrets

chmod +x gradle/wrapper/gradle-wrapper.jar

cat <<  'EOF' > "nginx.tmp"
server_tokens off;

server {
  listen 443 ssl;
  server_name finance.bhenning.com;

  ssl_certificate /etc/nginx/certs/bhenning.fullchain.pem;
  ssl_certificate_key /etc/nginx/certs/bhenning.privkey.pem;

  location / {
    proxy_pass http://raspi-finance-endpoint:8443;
    proxy_set_header Origin $http_origin;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
  }
}

# vim: set ft=conf:
EOF

cat <<  'EOF' > "docker.tmp"
FROM nginx:1.27.3-alpine

COPY nginx.tmp /etc/nginx/conf.d/default.conf

COPY ssl/bhenning.fullchain.pem /etc/nginx/certs/
COPY ssl/bhenning.privkey.pem /etc/nginx/certs/
EOF

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
  export CONTAINER_HOST=ssh://brianhenning@34.132.189.202
  export CONTAINER_HOST=ssh://gcp-api
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

  if ! podman network ls --filter "name=^finance-lan$" -q | grep -q .; then
    echo "Creating network finance-lan..."
    podman network create finance-lan
  else
    echo "Network finance-lan already exists."
  fi

  log "Podman detected. Cleaning up dangling images and volumes..."
  podman rmi -f "$(podman images -q -f dangling=true)" 2> /dev/null
  podman volume prune -f 2> /dev/null

  if [ "$env" = "proxmox" ]; then
    log "Proxmox environment detected. Cleaning up existing nginx, raspi, and influxdb containers..."

    nginx_container=$(podman ps -a -f 'name=nginx-server' --format "{{.ID}}") 2> /dev/null
    if [ -n "${nginx_container}" ]; then
      log "Stopping and removing existing nginx container(s)..."
      podman stop "${nginx_container}"
      podman rm -f "${nginx_container}" 2> /dev/null
    fi

    # varnish_container=$(podman ps -a -f 'name=varnish-server' --format "{{.ID}}") 2> /dev/null
    # if [ -n "${varnish_container}" ]; then
    #   log "Stopping and removing existing varnish container(s)..."
    #   podman stop "${varnish_container}"
    #   podman rm -f "${varnish_container}" 2> /dev/null
    # fi

    raspi_container=$(podman ps -a -f 'name=raspi-finance-endpoint' --format "{{.ID}}") 2> /dev/null
    if [ -n "${raspi_container}" ]; then
      log "Stopping and removing existing raspi-finance-endpoint container..."
      podman stop "${raspi_container}"
      podman rm -f "${raspi_container}" 2> /dev/null
      podman rmi -f raspi-finance-endpoint
    fi

    influxdb_container=$(podman ps -a -f 'name=influxdb-server' --format "{{.ID}}") 2> /dev/null
    if [ -n "${influxdb_container}" ]; then
      log "Stopping and removing existing influxdb-server container..."
      podman stop "${influxdb_container}"
      podman rm -f "${influxdb_container}" 2> /dev/null
      podman rmi -f influxdb:1.11.8
    fi

    log "Building images/deploying images using podman-compose..."
    if ! podman-compose -f docker-compose-base.yml -f docker-compose-prod.yml -f docker-compose-influxdb.yml up -d; then
      log "podman-compose build failed for proxmox deployment."
    else
      log "podman-compose build succeeded for proxmox deployment."
    fi
  else
    log "GCP environment detected. Cleaning up existing raspi-finance-endpoint and influxdb containers..."

    raspi_container=$(podman ps -a -f 'name=raspi-finance-endpoint' --format "{{.ID}}") 2> /dev/null
    if [ -n "${raspi_container}" ]; then
      log "Stopping and removing existing raspi-finance-endpoint container..."
      podman stop "${raspi_container}"
      podman rm -f "${raspi_container}" 2> /dev/null
      podman rmi -f raspi-finance-endpoint
    fi

    influxdb_container=$(podman ps -a -f 'name=influxdb-server' --format "{{.ID}}") 2> /dev/null
    if [ -n "${influxdb_container}" ]; then
      log "Stopping and removing existing influxdb-server container..."
      podman stop "${influxdb_container}"
      podman rm -f "${influxdb_container}" 2> /dev/null
      podman rmi -f influxdb:1.11.8
    fi

    log "GCP environment detected. Cleaning up existing nginx-gcp-proxy..."
    nginx_container=$(podman ps -a -f 'name=nginx-gcp-proxy' --format "{{.ID}}") 2> /dev/null
    if [ -n "${nginx_container}" ]; then
      log "Stopping and removing existing  nginx-gcp-proxy container..."
      podman stop nginx-gcp-proxy
      podman rm -f nginx-gcp-proxy 2> /dev/null
      podman rmi -f nginx-gcp-proxy
    fi

    log "Building/deploying images using podman-compose (without nginx or varnish)..."
    if ! podman-compose -f docker-compose-base.yml -f docker-compose-prod.yml -f docker-compose-influxdb.yml up -d; then
      log "podman-compose build failed for gcp deployment."
    else
      log "podman-compose build succeeded for gcp deployment."
    fi

    log "build gcp proxy server..."
    podman build -f docker.tmp -t nginx-gcp-proxy .
    log "run gcp proxy server..."
    podman run -dit --restart unless-stopped --network finance-lan -p 443:443 --name nginx-gcp-proxy -h nginx-gcp-proxy  nginx-gcp-proxy
  fi
    # log "change the port to 8443"
    # sed -i 's/^\(SERVER_PORT=\)[0-9]\+/\18443/' env.prod

  log "Podman detected. Cleaning up dangling images and volumes..."
  podman rmi -f "$(podman images -q -f dangling=true)" 2> /dev/null
  podman volume prune -f 2> /dev/null
else
  log "Podman command not found. Exiting."
  exit 1
fi

echo podman network connect finance-lan postgresql-server
# Run the raspi-finance-endpoint container.
# For gcp, ensure any preexisting container is deleted.
# log "Deleting any preexisting raspi-finance-endpoint container..."
# podman rm -f raspi-finance-endpoint

# log "Running raspi-finance-endpoint container..."
# podman run --name=raspi-finance-endpoint -h raspi-finance-endpoint --restart unless-stopped -p 8443:8443 -d raspi-finance-endpoint


# if [ "$env" = "proxmox" ]; then
  # log "Running nginx container for proxmox deployment..."
  # podman rm -f nginx-proxy-finance-server
  # podman run --name=nginx-proxy-finance-server -h nginx-proxy-finance-server --restart unless-stopped -p 9443:443 -d nginx-proxy-finance-server
# fi

podman network connect finance-lan raspi-finance-endpoint
log "list networks"
podman network ls

log "Running podman system prune to clean up unused resources..."
podman system prune -f

log "Deployment complete."
log "To follow logs, run: podman logs raspi-finance-endpoint --follow"
log "ssh gcp-api 'podman logs raspi-finance-endpoint -f'"
exit 0
