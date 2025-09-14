#!/bin/bash

# GCP InfluxDB Troubleshooting Script
# Diagnoses InfluxDB authentication and connection issues for GCP deployment
# Similar to troubleshoot-influxdb.sh but adapted for GCP environment

set -e

echo "======================================"
echo "GCP InfluxDB Troubleshooting Script"
echo "======================================"
echo

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

error() {
    echo -e "${RED}❌ ERROR: $1${NC}"
}

success() {
    echo -e "${GREEN}✅ SUCCESS: $1${NC}"
}

warning() {
    echo -e "${YELLOW}⚠️  WARNING: $1${NC}"
}

info() {
    echo -e "${BLUE}ℹ️  INFO: $1${NC}"
}

# Check if running from correct directory
if [[ ! -f "env.gcp" ]]; then
    error "env.gcp file not found. Run this script from the project root directory."
    exit 1
fi

# Load environment variables
if [[ -f "env.secrets" ]]; then
    source env.secrets
    echo "Environment variables loaded from env.secrets"
else
    warning "env.secrets file not found - some tests may fail"
fi
source env.gcp
echo "Environment variables loaded from env.gcp"
echo

# Function to set up SSH connectivity (similar to deploy-gcp.sh)
setup_gcp_ssh() {
    local KEY_PATH="$HOME/.ssh/id_rsa_gcp"

    info "Setting up SSH connectivity to GCP host..."

    # Extract hostname from SSH config
    local gcp_hostname
    gcp_hostname=$(ssh -G gcp-api | grep "^hostname " | awk '{print $2}')
    if [ -z "$gcp_hostname" ] || [ "$gcp_hostname" = "gcp-api" ]; then
        error "gcp-api not found in SSH config"
        info "Please add gcp-api to your ~/.ssh/config file"
        return 1
    fi
    info "SSH config found for gcp-api -> $gcp_hostname"

    # Test SSH port connectivity
    if ! nc -zv "$gcp_hostname" 22 >/dev/null 2>&1; then
        error "Cannot connect to SSH port 22 on $gcp_hostname"
        return 1
    fi
    success "SSH port 22 is accessible on $gcp_hostname"

    # Check SSH agent
    if ! ssh-add -l >/dev/null 2>&1; then
        info "Starting SSH agent and adding key..."
        if [ -f "$KEY_PATH" ]; then
            eval "$(ssh-agent -s)"
            export SSH_AUTH_SOCK SSH_AGENT_PID
            info "Please enter your SSH key passphrase when prompted:"
            if ssh-add "$KEY_PATH"; then
                success "SSH key added successfully"
            else
                error "Failed to add SSH key"
                return 1
            fi
        else
            error "SSH key not found at $KEY_PATH"
            return 1
        fi
    else
        success "SSH agent is running with keys loaded"
    fi

    # Test SSH connection and get user info
    info "Testing SSH connection and gathering user information..."
    if ! ssh -o ConnectTimeout=10 gcp-api 'echo "SSH connection successful"' >/dev/null 2>&1; then
        error "SSH connection test failed"
        return 1
    fi

    # Get user information from GCP host
    REMOTE_UID="$(ssh gcp-api id -u 2>/dev/null)"
    REMOTE_GID="$(ssh gcp-api id -g 2>/dev/null)"
    REMOTE_USERNAME="$(ssh gcp-api whoami 2>/dev/null)"

    if [ -z "$REMOTE_UID" ] || [ -z "$REMOTE_GID" ] || [ -z "$REMOTE_USERNAME" ]; then
        error "Failed to retrieve user information from gcp-api"
        return 1
    fi

    # Export variables for use throughout the script
    export REMOTE_UID REMOTE_GID REMOTE_USERNAME SSH_AUTH_SOCK SSH_AGENT_PID

    success "SSH connectivity verified, user info available:"
    info "UID: $REMOTE_UID, GID: $REMOTE_GID, Username: $REMOTE_USERNAME"
    return 0
}

# Test 1: SSH Connectivity Setup
echo "======================================"
echo "1. SSH CONNECTIVITY SETUP"
echo "======================================"

# Set up SSH connectivity with proper agent handling
if ! setup_gcp_ssh; then
    error "SSH setup failed - cannot continue with diagnostics"
    info "Please ensure:"
    info "1. SSH key exists at ~/.ssh/id_rsa_gcp"
    info "2. SSH config contains gcp-api entry"
    info "3. GCP host is accessible on port 22"
    exit 1
fi
echo

# Test 2: Docker Connectivity
echo "======================================"
echo "2. DOCKER CONNECTIVITY CHECK"
echo "======================================"

info "Testing Docker connectivity over SSH..."

# Use the SSH agent variables set up in the previous step
export DOCKER_HOST=ssh://gcp-api

# First check if Docker is running on the remote host
DOCKER_STATUS=$(ssh gcp-api 'systemctl is-active docker' 2>/dev/null || echo "unknown")
info "Docker daemon status: $DOCKER_STATUS"

if [[ "$DOCKER_STATUS" != "active" ]]; then
    error "Docker daemon is not active on GCP host"
    info "Start Docker: ssh gcp-api 'sudo systemctl start docker'"
    info "Enable Docker: ssh gcp-api 'sudo systemctl enable docker'"
    exit 1
fi

# Now test Docker client connectivity
info "Testing Docker client over SSH connection..."
if docker version >/dev/null 2>&1; then
    success "Docker connectivity working"
    DOCKER_VERSION=$(docker version --format '{{.Server.Version}}' 2>/dev/null)
    info "Docker version: $DOCKER_VERSION"

    # Test basic Docker functionality
    info "Testing Docker functionality..."
    DOCKER_INFO=$(docker info --format '{{.ServerVersion}} - {{.Name}}' 2>/dev/null || echo "INFO_FAILED")
    if [[ "$DOCKER_INFO" != "INFO_FAILED" ]]; then
        success "Docker info: $DOCKER_INFO"
    else
        warning "Docker info command failed"
    fi
else
    error "Cannot connect to Docker on GCP host"
    info "Troubleshooting steps:"
    info "1. Verify Docker is running: ssh gcp-api 'systemctl status docker'"
    info "2. Check Docker group membership: ssh gcp-api 'groups $REMOTE_USERNAME'"
    info "3. Test manual Docker: ssh gcp-api 'docker version'"
    info "4. Check Docker socket: ssh gcp-api 'ls -la /var/run/docker.sock'"
    exit 1
fi
echo

# Test 3: Container Status Check
echo "======================================"
echo "3. CONTAINER STATUS CHECK"
echo "======================================"

info "Checking raspi-finance-endpoint container status..."

CONTAINER_STATUS=$(docker ps -a --filter name=raspi-finance-endpoint --format "{{.Names}}\t{{.Status}}\t{{.Image}}" 2>/dev/null)

if [[ -z "$CONTAINER_STATUS" ]]; then
    error "raspi-finance-endpoint container does not exist"
    info "Container was never created - docker-compose deployment failed"

    info "Checking if any finance containers exist..."
    ALL_CONTAINERS=$(docker ps -a --format "{{.Names}}\t{{.Status}}" | grep -i finance || echo "none")
    echo "Finance-related containers: $ALL_CONTAINERS"

    warning "Run deployment first: ./deploy-gcp.sh"
else
    success "Container exists: $CONTAINER_STATUS"

    # Detailed container inspection
    info "Container detailed status:"
    docker inspect raspi-finance-endpoint --format '
  State: {{.State.Status}}
  Exit Code: {{.State.ExitCode}}
  Error: {{.State.Error}}
  Started: {{.State.StartedAt}}
  Finished: {{.State.FinishedAt}}
  Restart Count: {{.RestartCount}}
  PID: {{.State.Pid}}' 2>/dev/null
fi
echo

# Test 4: Network Configuration Check
echo "======================================"
echo "4. NETWORK CONFIGURATION CHECK"
echo "======================================"

info "Checking Docker network setup..."

# Check if finance-gcp-secure network exists
if docker network inspect finance-gcp-secure >/dev/null 2>&1; then
    success "finance-gcp-secure network exists"

    # Show network containers
    info "Containers on finance-gcp-secure network:"
    docker network inspect finance-gcp-secure --format '{{range .Containers}}{{.Name}}: {{.IPv4Address}}{{"\n"}}{{end}}' 2>/dev/null || warning "Cannot inspect network containers"

    # Check if our container is connected
    CONTAINER_NETWORK=$(docker inspect raspi-finance-endpoint --format '{{range .NetworkSettings.Networks}}{{.NetworkID}} {{end}}' 2>/dev/null || echo "FAILED")
    if [[ "$CONTAINER_NETWORK" != "FAILED" ]]; then
        info "Container network connections: $CONTAINER_NETWORK"
    else
        warning "Cannot inspect container network connections"
    fi
else
    error "finance-gcp-secure network does not exist"
    info "Create network: docker network create finance-gcp-secure"
fi

# Check port bindings
info "Checking port bindings..."
PORT_INFO=$(docker port raspi-finance-endpoint 2>/dev/null || echo "FAILED")
if [[ "$PORT_INFO" != "FAILED" ]]; then
    success "Port bindings: $PORT_INFO"
    if echo "$PORT_INFO" | grep -q "127.0.0.1:8443"; then
        success "Port 8443 correctly bound to localhost only (security)"
    else
        warning "Port 8443 not bound to localhost - check security"
    fi
else
    warning "Cannot retrieve port binding information"
fi
echo

# Test 5: InfluxDB Environment Configuration Check
echo "======================================"
echo "5. INFLUXDB ENVIRONMENT CHECK"
echo "======================================"

info "Checking InfluxDB environment variables in container..."

if docker ps --filter name=raspi-finance-endpoint --filter status=running --format "{{.Names}}" | grep -q raspi-finance-endpoint; then
    success "Container is running - checking environment variables"

    # Check InfluxDB environment variables inside the container
    info "InfluxDB configuration from container environment:"

    CONTAINER_INFLUXDB_ENABLED=$(docker exec raspi-finance-endpoint printenv INFLUXDB_ENABLED 2>/dev/null || echo "NOT_SET")
    CONTAINER_INFLUXDB_URL=$(docker exec raspi-finance-endpoint printenv INFLUXDB_URL 2>/dev/null || echo "NOT_SET")
    CONTAINER_INFLUXDB_ORG=$(docker exec raspi-finance-endpoint printenv INFLUXDB_ORG 2>/dev/null || echo "NOT_SET")
    CONTAINER_INFLUXDB_BUCKET=$(docker exec raspi-finance-endpoint printenv INFLUXDB_BUCKET 2>/dev/null || echo "NOT_SET")
    CONTAINER_INFLUXDB_TOKEN=$(docker exec raspi-finance-endpoint printenv INFLUXDB_TOKEN 2>/dev/null || echo "NOT_SET")

    echo "  INFLUXDB_ENABLED: $CONTAINER_INFLUXDB_ENABLED"
    echo "  INFLUXDB_URL: $CONTAINER_INFLUXDB_URL"
    echo "  INFLUXDB_ORG: $CONTAINER_INFLUXDB_ORG"
    echo "  INFLUXDB_BUCKET: $CONTAINER_INFLUXDB_BUCKET"
    echo "  INFLUXDB_TOKEN: ${CONTAINER_INFLUXDB_TOKEN:0:10}...${CONTAINER_INFLUXDB_TOKEN: -10}"

    # Analyze the configuration
    if [[ "$CONTAINER_INFLUXDB_ENABLED" == "false" ]]; then
        warning "INFLUXDB_ENABLED is set to 'false'"
        info "InfluxDB metrics are disabled - connection errors are expected"
        info "If you want InfluxDB metrics, set INFLUXDB_ENABLED=true in env.gcp"
        echo
        echo "Since InfluxDB is disabled, remaining tests are informational only."
        INFLUXDB_DISABLED=true
    elif [[ "$CONTAINER_INFLUXDB_ENABLED" == "true" ]]; then
        success "INFLUXDB_ENABLED is set to 'true'"
        INFLUXDB_DISABLED=false
    else
        error "INFLUXDB_ENABLED is not set properly: '$CONTAINER_INFLUXDB_ENABLED'"
        info "Set INFLUXDB_ENABLED=true or false in env.gcp"
        INFLUXDB_DISABLED=true
    fi

    # Check if required variables are set when enabled
    if [[ "$INFLUXDB_DISABLED" == "false" ]]; then
        if [[ "$CONTAINER_INFLUXDB_URL" == "NOT_SET" ]]; then
            error "INFLUXDB_URL is not set"
        fi
        if [[ "$CONTAINER_INFLUXDB_ORG" == "NOT_SET" ]]; then
            error "INFLUXDB_ORG is not set"
        fi
        if [[ "$CONTAINER_INFLUXDB_BUCKET" == "NOT_SET" ]]; then
            error "INFLUXDB_BUCKET is not set"
        fi
        if [[ "$CONTAINER_INFLUXDB_TOKEN" == "NOT_SET" ]]; then
            error "INFLUXDB_TOKEN is not set"
        fi
    fi
else
    error "Container is not running"
    info "Cannot check environment variables"
    exit 1
fi
echo

# Test 6: InfluxDB Network Connectivity Check
echo "======================================"
echo "6. INFLUXDB NETWORK CONNECTIVITY"
echo "======================================"

if [[ "$INFLUXDB_DISABLED" == "true" ]]; then
    info "InfluxDB is disabled - skipping connectivity tests"
else
    info "Testing InfluxDB connectivity..."
    info "InfluxDB URL: $CONTAINER_INFLUXDB_URL"

    # Extract host and port from URL
    INFLUX_HOST=$(echo "$CONTAINER_INFLUXDB_URL" | sed 's|http://||' | sed 's|https://||' | cut -d':' -f1)
    INFLUX_PORT=$(echo "$CONTAINER_INFLUXDB_URL" | sed 's|http://||' | sed 's|https://||' | cut -d':' -f2 | cut -d'/' -f1)

    info "InfluxDB host: $INFLUX_HOST"
    info "InfluxDB port: $INFLUX_PORT"

    # Check if required networking tools are available in container
    info "Checking available networking tools in container..."

    HAS_CURL=$(docker exec raspi-finance-endpoint which curl >/dev/null 2>&1 && echo "yes" || echo "no")
    HAS_NC=$(docker exec raspi-finance-endpoint which nc >/dev/null 2>&1 && echo "yes" || echo "no")
    HAS_NSLOOKUP=$(docker exec raspi-finance-endpoint which nslookup >/dev/null 2>&1 && echo "yes" || echo "no")

    info "Available tools: curl=$HAS_CURL, nc=$HAS_NC, nslookup=$HAS_NSLOOKUP"

    # Test DNS resolution if nslookup is available
    if [[ "$HAS_NSLOOKUP" == "yes" ]]; then
        info "Testing DNS resolution of $INFLUX_HOST from app container..."
        DNS_TEST=$(docker exec raspi-finance-endpoint nslookup "$INFLUX_HOST" 2>/dev/null || echo "FAILED")

        if [[ "$DNS_TEST" != "FAILED" ]]; then
            success "DNS resolution working"
            INFLUX_IP=$(echo "$DNS_TEST" | grep "Address:" | tail -1 | awk '{print $2}')
            info "InfluxDB server IP: $INFLUX_IP"
        else
            error "DNS resolution failed for $INFLUX_HOST"
        fi
    else
        warning "nslookup not available - cannot test DNS resolution"
    fi

    # Test port connectivity if nc is available
    if [[ "$HAS_NC" == "yes" ]]; then
        info "Testing port connectivity to $INFLUX_HOST:$INFLUX_PORT..."
        PORT_TEST=$(docker exec raspi-finance-endpoint nc -zv "$INFLUX_HOST" "$INFLUX_PORT" 2>&1 || echo "FAILED")

        if echo "$PORT_TEST" | grep -q "succeeded\|open"; then
            success "Port $INFLUX_PORT is accessible"
        else
            error "Cannot connect to InfluxDB port $INFLUX_PORT"
            echo "Port test output: $PORT_TEST"
        fi
    else
        warning "nc (netcat) not available - cannot test port connectivity"
    fi

    # Test HTTP connectivity if curl is available
    if [[ "$HAS_CURL" == "yes" ]]; then
        info "Testing HTTP connectivity to InfluxDB..."
        HTTP_TEST=$(docker exec raspi-finance-endpoint curl -s --connect-timeout 5 "$CONTAINER_INFLUXDB_URL/ping" 2>/dev/null || echo "FAILED")

        if [[ "$HTTP_TEST" != "FAILED" ]]; then
            success "HTTP connectivity to InfluxDB is working"
        else
            error "Cannot connect to InfluxDB via HTTP"
        fi
    else
        warning "curl not available - cannot test HTTP connectivity"
    fi
fi
echo

# Test 7: InfluxDB Health Check
echo "======================================"
echo "7. INFLUXDB HEALTH CHECK"
echo "======================================"

if [[ "$INFLUXDB_DISABLED" == "true" ]]; then
    info "InfluxDB is disabled - skipping health checks"
elif [[ "$HAS_CURL" == "no" ]]; then
    warning "curl not available - cannot perform InfluxDB health checks"
else
    info "Checking InfluxDB health status..."

    # Try health endpoint
    HEALTH_RESPONSE=$(docker exec raspi-finance-endpoint curl -s --connect-timeout 5 "$CONTAINER_INFLUXDB_URL/health" 2>/dev/null || echo "FAILED")

    if [[ "$HEALTH_RESPONSE" == "FAILED" ]]; then
        error "Health check failed - InfluxDB may not be responding"
    else
        echo "Health Response: $HEALTH_RESPONSE"
        if echo "$HEALTH_RESPONSE" | grep -q '"status":"pass"'; then
            success "InfluxDB health check passed"
        else
            warning "InfluxDB health check returned non-pass status"
        fi
    fi

    # Try ping endpoint
    info "Testing InfluxDB ping endpoint..."
    PING_RESPONSE=$(docker exec raspi-finance-endpoint curl -s --connect-timeout 5 "$CONTAINER_INFLUXDB_URL/ping" 2>/dev/null || echo "FAILED")

    if [[ "$PING_RESPONSE" == "FAILED" ]]; then
        error "Ping failed - InfluxDB may not be responding"
    else
        success "InfluxDB ping successful"
    fi
fi
echo

# Test 8: InfluxDB Authentication Test
echo "======================================"
echo "8. INFLUXDB AUTHENTICATION TEST"
echo "======================================"

if [[ "$INFLUXDB_DISABLED" == "true" ]]; then
    info "InfluxDB is disabled - skipping authentication tests"
elif [[ "$HAS_CURL" == "no" ]]; then
    warning "curl not available - cannot test InfluxDB authentication"
else
    info "Testing authentication with configured token..."
    info "Organization: $CONTAINER_INFLUXDB_ORG"
    info "Bucket: $CONTAINER_INFLUXDB_BUCKET"
    info "Token: ${CONTAINER_INFLUXDB_TOKEN:0:10}...${CONTAINER_INFLUXDB_TOKEN: -10}"

    # Test authentication by listing buckets
    AUTH_TEST=$(docker exec raspi-finance-endpoint curl -s -w '%{http_code}' -H "Authorization: Token $CONTAINER_INFLUXDB_TOKEN" "$CONTAINER_INFLUXDB_URL/api/v2/buckets?org=$CONTAINER_INFLUXDB_ORG" 2>/dev/null || echo "FAILED000")

    HTTP_CODE=$(echo "$AUTH_TEST" | tail -c 4)
    RESPONSE_BODY=$(echo "$AUTH_TEST" | head -c -4)

    echo "HTTP Response Code: $HTTP_CODE"
    echo "Response Body: $RESPONSE_BODY"

    case $HTTP_CODE in
        200)
            success "Authentication successful"
            if command -v jq >/dev/null 2>&1; then
                echo "Available buckets:"
                echo "$RESPONSE_BODY" | jq -r '.buckets[] | "  - \(.name) (ID: \(.id))"' 2>/dev/null || echo "$RESPONSE_BODY"
            else
                echo "Raw response: $RESPONSE_BODY"
            fi
            ;;
        401)
            error "Authentication failed - invalid token or credentials"
            ;;
        404)
            error "Organization '$CONTAINER_INFLUXDB_ORG' not found"
            ;;
        *)
            error "Unexpected response code: $HTTP_CODE"
            ;;
    esac
fi
echo

# Test 9: InfluxDB Container Logs Analysis
echo "======================================"
echo "9. INFLUXDB LOGS ANALYSIS"
echo "======================================"

info "Analyzing container logs for InfluxDB-related errors..."

if docker ps -a --filter name=raspi-finance-endpoint --format "{{.Names}}" | grep -q raspi-finance-endpoint; then
    info "Searching for InfluxDB-related log entries (last 50 lines)..."
    echo "--- InfluxDB Logs Start ---"

    docker logs raspi-finance-endpoint --tail 50 2>&1 | grep -i "influx" | while IFS= read -r line; do
        if echo "$line" | grep -qi "error\|exception\|fail\|fatal"; then
            error "LOG: $line"
        elif echo "$line" | grep -qi "warn"; then
            warning "LOG: $line"
        else
            echo "LOG: $line"
        fi
    done

    echo "--- InfluxDB Logs End ---"

    # Check for specific InfluxDB error patterns
    info "Checking for InfluxDB-specific error patterns..."

    if docker logs raspi-finance-endpoint 2>&1 | grep -qi "influx.*connection.*refused"; then
        error "InfluxDB connection refused errors found in logs"
    fi

    if docker logs raspi-finance-endpoint 2>&1 | grep -qi "influx.*timeout"; then
        error "InfluxDB timeout errors found in logs"
    fi

    if docker logs raspi-finance-endpoint 2>&1 | grep -qi "influx.*authentication"; then
        error "InfluxDB authentication errors found in logs"
    fi

    if docker logs raspi-finance-endpoint 2>&1 | grep -qi "metrics.*registry.*started"; then
        success "InfluxDB metrics registry started (found in logs)"
    fi

    # Count InfluxDB-related log entries
    INFLUX_LOG_COUNT=$(docker logs raspi-finance-endpoint 2>&1 | grep -ci "influx" || echo "0")
    info "Total InfluxDB-related log entries: $INFLUX_LOG_COUNT"
else
    warning "Container does not exist - cannot analyze logs"
fi
echo


# InfluxDB Recommendations
echo "======================================"
echo "10. INFLUXDB RECOMMENDATIONS & SOLUTIONS"
echo "======================================"

echo "Based on the InfluxDB diagnostics above, here are the recommended actions:"
echo

# InfluxDB disabled
if [[ "$INFLUXDB_DISABLED" == "true" ]]; then
    info "InfluxDB Status: DISABLED"
    echo "  → InfluxDB metrics collection is currently disabled"
    echo "  → Connection errors in logs are expected and can be ignored"
    echo
    echo "If you want to enable InfluxDB metrics:"
    echo "  1. Set INFLUXDB_ENABLED=true in env.gcp"
    echo "  2. Configure proper InfluxDB_URL, ORG, BUCKET, TOKEN"
    echo "  3. Ensure InfluxDB server is accessible"
    echo "  4. Redeploy: ./deploy-gcp.sh"
    echo
fi

# InfluxDB enabled but has issues
if [[ "$INFLUXDB_DISABLED" == "false" ]]; then
    if [[ "$HTTP_TEST" == "FAILED" ]] || [[ "$PING_RESPONSE" == "FAILED" ]]; then
        error "CRITICAL: InfluxDB connectivity issues"
        echo "  → Application cannot connect to InfluxDB server"
        echo "  → Check InfluxDB server availability and network configuration"
        echo
        echo "Solutions:"
        echo "  1. Verify InfluxDB server is running at: $CONTAINER_INFLUXDB_URL"
        echo "  2. Test from GCP host: ssh gcp-api 'curl $CONTAINER_INFLUXDB_URL/ping'"
        echo "  3. Check firewall rules and network connectivity"
        echo "  4. Verify InfluxDB server configuration"
        echo
    fi

    if [[ "$HTTP_CODE" == "401" ]]; then
        error "CRITICAL: InfluxDB authentication failed"
        echo "  → Token authentication is failing"
        echo "  → Token may be invalid or expired"
        echo
        echo "Solutions:"
        echo "  1. Verify INFLUXDB_TOKEN in env.secrets"
        echo "  2. Check token permissions in InfluxDB UI"
        echo "  3. Generate new token if needed"
        echo "  4. Ensure token has read/write access to bucket"
        echo
    fi

    if [[ "$HTTP_CODE" == "404" ]]; then
        error "CRITICAL: InfluxDB organization not found"
        echo "  → Organization '$CONTAINER_INFLUXDB_ORG' does not exist"
        echo "  → Check organization name spelling"
        echo
        echo "Solutions:"
        echo "  1. Verify INFLUXDB_ORG in env.gcp matches InfluxDB setup"
        echo "  2. List organizations: influx org list"
        echo "  3. Create organization if needed"
        echo
    fi

    # Missing environment variables
    if [[ "$CONTAINER_INFLUXDB_URL" == "NOT_SET" ]] || [[ "$CONTAINER_INFLUXDB_TOKEN" == "NOT_SET" ]]; then
        error "CRITICAL: Missing InfluxDB configuration"
        echo "  → Required environment variables are not set"
        echo
        echo "Solutions:"
        echo "  1. Set INFLUXDB_URL in env.gcp"
        echo "  2. Set INFLUXDB_TOKEN in env.secrets"
        echo "  3. Set INFLUXDB_ORG and INFLUXDB_BUCKET in env.gcp"
        echo "  4. Redeploy container after fixing configuration"
        echo
    fi

    # Missing tools
    if [[ "$HAS_CURL" == "no" ]]; then
        warning "curl not available in container"
        echo "  → Cannot perform HTTP-based InfluxDB tests"
        echo "  → This may indicate a container image issue"
        echo
    fi
fi

# General recommendations
echo "InfluxDB monitoring commands:"
echo "  InfluxDB logs: docker logs raspi-finance-endpoint -f | grep -i influx"
echo "  Test connectivity: docker exec raspi-finance-endpoint curl $CONTAINER_INFLUXDB_URL/ping"
echo "  Check metrics: curl -k https://34.132.189.202/actuator/metrics"
echo
echo "InfluxDB troubleshooting commands:"
echo "  Test from host: ssh gcp-api 'curl $CONTAINER_INFLUXDB_URL/health'"
echo "  Container environment: docker exec raspi-finance-endpoint printenv | grep INFLUX"
echo "  Restart for config changes: docker restart raspi-finance-endpoint"
echo
if [[ "$INFLUXDB_DISABLED" == "true" ]]; then
    info "✅ No action needed - InfluxDB is intentionally disabled"
else
    info "Re-run this script after making changes to verify InfluxDB fixes"
fi
echo "======================================"

exit 0