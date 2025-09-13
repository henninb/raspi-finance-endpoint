#!/bin/bash

# Diagnostic script for GCP deployment issues
# This script helps identify why raspi-finance-endpoint container is not starting

log() {
  echo "$(date +"%Y-%m-%d %H:%M:%S") - $*"
}

log_error() {
  echo "$(date +"%Y-%m-%d %H:%M:%S") - ERROR: $*" >&2
}

log "=== GCP Deployment Diagnostic ==="

# Set DOCKER_HOST for GCP
export DOCKER_HOST=ssh://gcp-api

log "1. Checking Docker connectivity to GCP..."
if ! docker version >/dev/null 2>&1; then
    log_error "Cannot connect to Docker on GCP host"
    log "Please check SSH connectivity: ssh gcp-api"
    exit 1
fi
log "✓ Docker connectivity working"

log "2. Checking if raspi-finance-endpoint container exists..."
container_status=$(docker ps -a --filter name=raspi-finance-endpoint --format "{{.Names}}\t{{.Status}}\t{{.Image}}" 2>/dev/null)
if [ -z "$container_status" ]; then
    log_error "✗ raspi-finance-endpoint container does not exist!"
    log "The application container was never created."

    log "3. Checking docker-compose deployment..."
    if docker compose -f docker-compose-gcp.yml ps 2>/dev/null; then
        log "Docker-compose services status shown above"
    else
        log_error "Docker-compose is not running or has issues"
    fi

    log "4. Checking if docker-compose was run..."
    log "Recent Docker operations:"
    docker ps -a --format "table {{.Names}}\t{{.Status}}\t{{.Image}}\t{{.CreatedAt}}" | head -10

    log "DIAGNOSIS: The raspi-finance-endpoint container was never created."
    log "This suggests docker-compose up failed or was never executed."
    log "Check the run.sh output for docker-compose errors."
    exit 1
fi

log "✓ Container exists: $container_status"

log "3. Checking container detailed status..."
docker inspect raspi-finance-endpoint --format '
Container Status: {{.State.Status}}
Exit Code: {{.State.ExitCode}}
Error: {{.State.Error}}
Started At: {{.State.StartedAt}}
Finished At: {{.State.FinishedAt}}
Restart Count: {{.RestartCount}}
' 2>/dev/null

log "4. Checking container logs (last 50 lines)..."
log "--- Container Logs Start ---"
docker logs raspi-finance-endpoint --tail 50 2>&1 || log_error "Failed to get container logs"
log "--- Container Logs End ---"

log "5. Checking network connectivity..."
network_info=$(docker network inspect finance-gcp-secure --format '{{range .Containers}}{{.Name}}: {{.IPv4Address}}{{"\n"}}{{end}}' 2>/dev/null)
if [ -n "$network_info" ]; then
    log "Network containers:"
    echo "$network_info"
else
    log_error "finance-gcp-secure network not found or empty"
fi

log "6. Checking environment file..."
if [ -f env.gcp ]; then
    log "env.gcp contents (sensitive data masked):"
    sed 's/PASSWORD=.*/PASSWORD=***MASKED***/' env.gcp
else
    log_error "env.gcp file not found!"
fi

log "7. Checking database connectivity (if container is running)..."
if docker ps --filter name=raspi-finance-endpoint --filter status=running --format "{{.Names}}" | grep -q raspi-finance-endpoint; then
    log "Testing database connection from container..."
    db_host=$(grep "^DATASOURCE=" env.gcp 2>/dev/null | cut -d'/' -f3 | cut -d':' -f1 || echo "unknown")
    log "Database host from config: $db_host"

    if [ "$db_host" != "unknown" ]; then
        docker exec raspi-finance-endpoint nslookup "$db_host" 2>/dev/null || log "DNS resolution failed for $db_host"
        docker exec raspi-finance-endpoint ping -c 1 "$db_host" 2>/dev/null || log "Ping failed for $db_host"
    fi
else
    log "Container not running - cannot test database connectivity"
fi

log "8. Checking SSL certificates..."
if [ -d ssl ]; then
    log "SSL certificates status:"
    ls -la ssl/bhenning.* 2>/dev/null || log "SSL certificates not found"
else
    log_error "ssl directory not found"
fi

log "9. Checking Docker resources..."
docker system df
docker stats --no-stream --format "table {{.Container}}\t{{.CPUPerc}}\t{{.MemUsage}}\t{{.MemPerc}}" 2>/dev/null | head -5

log "=== Diagnostic Complete ==="
log ""
log "Common issues and solutions:"
log "1. Database connection failure:"
log "   - Set GCP_DB_HOST environment variable"
log "   - Check database credentials in env.secrets"
log "   - Verify database is accessible from GCP instance"
log ""
log "2. Container exits immediately:"
log "   - Check container logs above for Java/Spring Boot errors"
log "   - Verify environment variables are correct"
log "   - Check SSL certificate availability"
log ""
log "3. Docker-compose deployment failed:"
log "   - Re-run: ./run.sh gcp"
log "   - Check for build errors in gradle output"
log "   - Verify all dependencies are available"

exit 0