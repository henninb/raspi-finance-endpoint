#!/bin/bash

# Complete GCP deployment script with integrated SSH connectivity
# This script handles SSH setup and deployment in one seamless process

log() {
  echo "$(date +"%Y-%m-%d %H:%M:%S") - $*"
}

log_error() {
  echo "$(date +"%Y-%m-%d %H:%M:%S") - ERROR: $*" >&2
}

# Function to set up SSH connectivity and get user info
setup_gcp_ssh() {
    local KEY_PATH="$HOME/.ssh/id_rsa_gcp"

    log "Setting up SSH connectivity to GCP host..."

    # Extract hostname from SSH config
    local gcp_hostname
    gcp_hostname=$(ssh -G gcp-api | grep "^hostname " | awk '{print $2}')
    if [ -z "$gcp_hostname" ] || [ "$gcp_hostname" = "gcp-api" ]; then
        log_error "gcp-api not found in SSH config"
        log "Please add gcp-api to your ~/.ssh/config file"
        exit 1
    fi
    log "✓ SSH config found for gcp-api -> $gcp_hostname"

    # Test SSH port connectivity
    if ! nc -zv "$gcp_hostname" 22 >/dev/null 2>&1; then
        log_error "Cannot connect to SSH port 22 on $gcp_hostname"
        exit 1
    fi
    log "✓ SSH port 22 is accessible on $gcp_hostname"

    # Check SSH agent
    if ! ssh-add -l >/dev/null 2>&1; then
        log "Starting SSH agent and adding key..."
        if [ -f "$KEY_PATH" ]; then
            eval "$(ssh-agent -s)"
            export SSH_AUTH_SOCK SSH_AGENT_PID
            log "Please enter your SSH key passphrase when prompted:"
            if ssh-add "$KEY_PATH"; then
                log "✓ SSH key added successfully"
            else
                log_error "Failed to add SSH key"
                exit 1
            fi
        else
            log_error "SSH key not found at $KEY_PATH"
            exit 1
        fi
    else
        log "✓ SSH agent is running with keys loaded"
    fi

    # Test SSH connection and get user info
    log "Testing SSH connection and gathering user information..."
    if ! ssh -o ConnectTimeout=10 gcp-api 'echo "SSH connection successful"' >/dev/null 2>&1; then
        log_error "SSH connection test failed"
        exit 1
    fi

    # Get user information from GCP host
    CURRENT_UID="$(ssh gcp-api id -u 2>/dev/null)"
    CURRENT_GID="$(ssh gcp-api id -g 2>/dev/null)"
    USERNAME="$(ssh gcp-api whoami 2>/dev/null)"

    if [ -z "$CURRENT_UID" ] || [ -z "$CURRENT_GID" ] || [ -z "$USERNAME" ]; then
        log_error "Failed to retrieve user information from gcp-api"
        exit 1
    fi

    # Export variables for use by run.sh
    export CURRENT_UID CURRENT_GID USERNAME SSH_AUTH_SOCK SSH_AGENT_PID

    log "✓ SSH connectivity verified, user info available:"
    log "  UID: $CURRENT_UID"
    log "  GID: $CURRENT_GID"
    log "  Username: $USERNAME"
}

log "=== Complete GCP Deployment ==="

# Step 1: Set up SSH connectivity and get user info
setup_gcp_ssh

# Step 2: Set up database configuration
log "Step 2: Configuring database connection..."
if [ -z "$GCP_DB_HOST" ]; then
    log "⚠ GCP_DB_HOST not set in environment"
    log "Using default: postgresql-server (Docker container)"
    log "Set GCP_DB_HOST in env.secrets if you need a different database host"
    export GCP_DB_HOST="postgresql-server"
else
    log "✓ Using database host: $GCP_DB_HOST"
fi

# Step 3: Run the deployment
log "Step 3: Running GCP deployment..."
log "Note: This will build the application and deploy all containers"

if ! ./run.sh gcp; then
    log_error "GCP deployment failed!"
    log ""
    log "Troubleshooting:"
    log "1. Check application container logs: ./diagnose-gcp-deployment.sh"
    log "2. Verify database connectivity from GCP instance"
    log "3. Check environment variables in env.secrets"
    log "4. Review Docker logs: ssh gcp-api 'docker logs raspi-finance-endpoint'"
    exit 1
fi

# Step 4: Verify deployment
log "Step 4: Verifying deployment..."
sleep 5

# Check if containers are running
log "Checking container status..."
export DOCKER_HOST=ssh://gcp-api

app_status=$(docker ps --filter name=raspi-finance-endpoint --format "{{.Status}}" 2>/dev/null)
nginx_status=$(docker ps --filter name=nginx-gcp-proxy --format "{{.Status}}" 2>/dev/null)

if echo "$app_status" | grep -q "Up"; then
    log "✓ Application container is running: $app_status"
else
    log_error "✗ Application container is not running properly"
    log "Status: $app_status"
    log "Check logs with: ./diagnose-gcp-deployment.sh"
    exit 1
fi

if echo "$nginx_status" | grep -q "Up"; then
    log "✓ Nginx proxy is running: $nginx_status"
else
    log_error "✗ Nginx proxy is not running properly"
    log "Status: $nginx_status"
fi

# Test application health
log "Testing application health..."
if ssh gcp-api 'curl -f -s http://localhost:8443/actuator/health >/dev/null 2>&1'; then
    log "✓ Application health check passed"
else
    log "⚠ Application health check failed - application may still be starting"
fi

# Test public access
log "Testing public HTTPS access..."
local_ip=$(curl -s https://ipinfo.io/ip 2>/dev/null || echo "unknown")
if curl -k -f -s --connect-timeout 10 https://34.132.189.202/actuator/health >/dev/null 2>&1; then
    log "✓ Public HTTPS access working: https://34.132.189.202/"
else
    log "⚠ Public HTTPS access test failed"
fi

log ""
log "=== Deployment Summary ==="
log "✓ SSH connectivity: Working"
log "✓ User info: UID=$CURRENT_UID, GID=$CURRENT_GID, User=$USERNAME"
log "✓ Application deployed: $(echo "$app_status" | head -1)"
log "✓ Nginx proxy deployed: $(echo "$nginx_status" | head -1)"
log ""
log "Access URLs:"
log "  Public HTTPS: https://34.132.189.202/"
log "  Health Check: https://34.132.189.202/actuator/health"
log "  GraphQL: https://34.132.189.202/graphql"
log "  GraphiQL: https://34.132.189.202/graphiql"
log ""
log "Monitoring commands:"
log "  Application logs: ssh gcp-api 'docker logs raspi-finance-endpoint -f'"
log "  Nginx logs: ssh gcp-api 'docker logs nginx-gcp-proxy -f'"
log "  Container status: ssh gcp-api 'docker ps'"
log ""
log "Troubleshooting:"
log "  Full diagnostics: ./diagnose-gcp-deployment.sh"
log "  Restart deployment: ./deploy-gcp.sh"

exit 0