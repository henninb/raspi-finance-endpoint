#!/bin/bash

# InfluxDB 2.x Troubleshooting Script
# Diagnoses authentication and connection issues

set -e

echo "======================================"
echo "InfluxDB 2.x Troubleshooting Script"
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
if [[ ! -f "env.influx" ]]; then
    error "env.influx file not found. Run this script from the project root directory."
    exit 1
fi

# Load environment variables
source env.influx
echo "Environment variables loaded from env.influx"
echo

# Test 1: Container Status
echo "======================================"
echo "1. CONTAINER STATUS CHECK"
echo "======================================"

if ssh debian-dockerserver 'docker ps --filter name=influxdb-server --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"' | grep -q influxdb-server; then
    success "InfluxDB container is running"
    ssh debian-dockerserver 'docker ps --filter name=influxdb-server --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"'
else
    error "InfluxDB container is not running"
    info "Starting InfluxDB container..."
    ssh debian-dockerserver 'docker start influxdb-server || echo "Container does not exist"'
fi
echo

# Test 2: Docker Network Check
echo "======================================"
echo "2. DOCKER NETWORK CHECK"
echo "======================================"

info "Checking Docker network configuration..."

# Get InfluxDB container network
INFLUXDB_NETWORKS=$(ssh debian-dockerserver 'docker inspect influxdb-server --format "{{range .NetworkSettings.Networks}}{{.NetworkID}} {{end}}"' 2>/dev/null || echo "FAILED")
if [[ "$INFLUXDB_NETWORKS" == "FAILED" ]]; then
    error "Cannot inspect InfluxDB container networks"
else
    info "InfluxDB networks: $INFLUXDB_NETWORKS"
fi

# Get application container network if it exists
APP_NETWORKS=$(ssh debian-dockerserver 'docker inspect raspi-finance-endpoint --format "{{range .NetworkSettings.Networks}}{{.NetworkID}} {{end}}"' 2>/dev/null || echo "NOT_RUNNING")
if [[ "$APP_NETWORKS" == "NOT_RUNNING" ]]; then
    warning "Application container not running - cannot check network"
else
    info "Application networks: $APP_NETWORKS"
fi

# Check if they share a network
if [[ "$INFLUXDB_NETWORKS" != "FAILED" && "$APP_NETWORKS" != "NOT_RUNNING" ]]; then
    SHARED_NETWORK="false"
    for influx_net in $INFLUXDB_NETWORKS; do
        for app_net in $APP_NETWORKS; do
            if [[ "$influx_net" == "$app_net" ]]; then
                SHARED_NETWORK="true"
                success "Containers share network: $influx_net"
                break 2
            fi
        done
    done

    if [[ "$SHARED_NETWORK" == "false" ]]; then
        error "Containers are NOT on the same Docker network"
        warning "This explains why 'influxdb-server' hostname doesn't resolve"
    fi
fi

# Show detailed network info
info "Detailed network information:"
echo "InfluxDB container networks:"
ssh debian-dockerserver 'docker inspect influxdb-server --format "{{json .NetworkSettings.Networks}}"' 2>/dev/null | jq -r 'to_entries[] | "  - \(.key): \(.value.IPAddress)"' 2>/dev/null || echo "  Unable to parse network info"

if [[ "$APP_NETWORKS" != "NOT_RUNNING" ]]; then
    echo "Application container networks:"
    ssh debian-dockerserver 'docker inspect raspi-finance-endpoint --format "{{json .NetworkSettings.Networks}}"' 2>/dev/null | jq -r 'to_entries[] | "  - \(.key): \(.value.IPAddress)"' 2>/dev/null || echo "  Unable to parse network info"
fi

echo

# Test 3: Network Connectivity
echo "======================================"
echo "3. NETWORK CONNECTIVITY CHECK"
echo "======================================"

info "Testing network connectivity to InfluxDB..."

# Test hostname resolution first
info "Testing DNS resolution of 'influxdb-server'..."
HOSTNAME_IP=$(ssh debian-dockerserver 'docker exec raspi-finance-endpoint nslookup influxdb-server 2>/dev/null | grep "Address:" | tail -1 | cut -d" " -f2' 2>/dev/null || echo "FAILED")
if [[ "$HOSTNAME_IP" == "FAILED" ]]; then
    error "DNS resolution failed for 'influxdb-server'"
    info "Trying alternative resolution methods..."

    # Try getent hosts
    GETENT_IP=$(ssh debian-dockerserver 'docker exec raspi-finance-endpoint getent hosts influxdb-server 2>/dev/null | cut -d" " -f1' || echo "FAILED")
    if [[ "$GETENT_IP" != "FAILED" ]]; then
        success "getent resolved influxdb-server to: $GETENT_IP"
    else
        error "getent also failed to resolve influxdb-server"
    fi

    # Try direct IP connection
    info "Testing direct IP connection to 172.19.0.2..."
    if ssh debian-dockerserver 'docker exec raspi-finance-endpoint curl -s --connect-timeout 5 http://172.19.0.2:8086/ping' >/dev/null 2>&1; then
        success "Direct IP connection works!"
        error "Problem is DNS resolution, not network connectivity"
    else
        error "Direct IP connection also failed"
    fi
else
    success "DNS resolved influxdb-server to: $HOSTNAME_IP"
fi

# Test hostname connectivity
if ssh debian-dockerserver 'docker exec raspi-finance-endpoint curl -s --connect-timeout 5 http://influxdb-server:8086/ping' >/dev/null 2>&1; then
    success "Network connectivity to InfluxDB is working"
else
    error "Cannot connect to InfluxDB at http://influxdb-server:8086"
    info "Testing external connectivity..."
    if ssh debian-dockerserver 'curl -s --connect-timeout 5 http://192.168.10.10:8086/ping' >/dev/null 2>&1; then
        success "External connectivity works"
    else
        error "External connectivity also failed"
    fi
fi
echo

# Test 4: InfluxDB Health Check
echo "======================================"
echo "4. INFLUXDB HEALTH CHECK"
echo "======================================"

info "Checking InfluxDB health status..."

# Try internal hostname first (from application container)
HEALTH_RESPONSE=$(ssh debian-dockerserver 'docker exec raspi-finance-endpoint curl -s http://influxdb-server:8086/health' 2>/dev/null || echo "FAILED")

if [[ "$HEALTH_RESPONSE" == "FAILED" ]]; then
    warning "Internal health check failed, trying external IP..."
    # Try external IP from host
    HEALTH_RESPONSE=$(ssh debian-dockerserver 'curl -s http://192.168.10.10:8086/health' 2>/dev/null || echo "FAILED")
fi

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
echo

# Test 5: Authentication Test
echo "======================================"
echo "5. AUTHENTICATION TEST"
echo "======================================"

info "Testing authentication with configured token..."
info "Organization: $INFLUXDB_ORG"
info "Bucket: $INFLUXDB_BUCKET"
info "Token: ${INFLUXDB_TOKEN:0:10}...${INFLUXDB_TOKEN: -10}"

# Test authentication from application container (same network context)
AUTH_TEST=$(ssh debian-dockerserver "docker exec raspi-finance-endpoint curl -s -w '%{http_code}' -H 'Authorization: Token $INFLUXDB_TOKEN' http://influxdb-server:8086/api/v2/buckets?org=$INFLUXDB_ORG" 2>/dev/null || echo "FAILED000")

HTTP_CODE=$(echo "$AUTH_TEST" | tail -c 4)
RESPONSE_BODY=$(echo "$AUTH_TEST" | head -c -4)

echo "HTTP Response Code: $HTTP_CODE"
echo "Response Body: $RESPONSE_BODY"

case $HTTP_CODE in
    200)
        success "Authentication successful"
        echo "Available buckets:"
        echo "$RESPONSE_BODY" | jq -r '.buckets[] | "  - \(.name) (ID: \(.id))"' 2>/dev/null || echo "$RESPONSE_BODY"
        ;;
    401)
        error "Authentication failed - invalid token or credentials"
        ;;
    404)
        error "Organization '$INFLUXDB_ORG' not found"
        ;;
    *)
        error "Unexpected response code: $HTTP_CODE"
        ;;
esac
echo

# Test 6: Bucket Verification
echo "======================================"
echo "6. BUCKET VERIFICATION"
echo "======================================"

info "Checking if bucket '$INFLUXDB_BUCKET' exists..."
BUCKET_CHECK=$(ssh debian-dockerserver "docker exec raspi-finance-endpoint curl -s -H 'Authorization: Token $INFLUXDB_TOKEN' 'http://influxdb-server:8086/api/v2/buckets?org=$INFLUXDB_ORG&name=$INFLUXDB_BUCKET'" 2>/dev/null)

if echo "$BUCKET_CHECK" | jq -e '.buckets | length > 0' >/dev/null 2>&1; then
    success "Bucket '$INFLUXDB_BUCKET' exists"
    BUCKET_ID=$(echo "$BUCKET_CHECK" | jq -r '.buckets[0].id')
    info "Bucket ID: $BUCKET_ID"
else
    error "Bucket '$INFLUXDB_BUCKET' does not exist"
    info "Available buckets:"
    ssh debian-dockerserver "docker exec raspi-finance-endpoint curl -s -H 'Authorization: Token $INFLUXDB_TOKEN' http://influxdb-server:8086/api/v2/buckets?org=$INFLUXDB_ORG" | jq -r '.buckets[] | "  - \(.name)"' 2>/dev/null
fi
echo

# Test 7: Write Test
echo "======================================"
echo "7. WRITE PERMISSION TEST"
echo "====================================="

info "Testing write permissions to bucket '$INFLUXDB_BUCKET'..."
TEST_DATA="test_metric,host=troubleshoot value=1.0 $(date +%s)000000000"

WRITE_TEST=$(ssh debian-dockerserver "docker exec raspi-finance-endpoint curl -s -w '%{http_code}' -X POST -H 'Authorization: Token $INFLUXDB_TOKEN' -H 'Content-Type: text/plain' --data-binary '$TEST_DATA' 'http://influxdb-server:8086/api/v2/write?org=$INFLUXDB_ORG&bucket=$INFLUXDB_BUCKET&precision=ns'" 2>/dev/null)

WRITE_HTTP_CODE=$(echo "$WRITE_TEST" | tail -c 4)
WRITE_RESPONSE=$(echo "$WRITE_TEST" | head -c -4)

echo "Write HTTP Code: $WRITE_HTTP_CODE"

case $WRITE_HTTP_CODE in
    204)
        success "Write test successful - permissions are correct"
        ;;
    401)
        error "Write failed - authentication/authorization issue"
        ;;
    404)
        error "Write failed - bucket or organization not found"
        ;;
    *)
        error "Write failed with code: $WRITE_HTTP_CODE"
        echo "Response: $WRITE_RESPONSE"
        ;;
esac
echo

# Test 8: Container Logs
echo "======================================"
echo "8. CONTAINER LOGS ANALYSIS"
echo "====================================="

info "Checking recent InfluxDB container logs for errors..."
ssh debian-dockerserver 'docker logs --tail 20 influxdb-server 2>&1' | while IFS= read -r line; do
    if echo "$line" | grep -qi "error\|fail\|unauthorized"; then
        error "LOG: $line"
    elif echo "$line" | grep -qi "warn"; then
        warning "LOG: $line"
    else
        echo "LOG: $line"
    fi
done
echo

# Test 9: Application Configuration Check
echo "======================================"
echo "9. APPLICATION CONFIGURATION CHECK"
echo "====================================="

info "Checking application configuration..."
echo "Current env.influx configuration:"
echo "  INFLUXDB_URL: $INFLUXDB_URL"
echo "  INFLUXDB_ORG: $INFLUXDB_ORG"
echo "  INFLUXDB_BUCKET: $INFLUXDB_BUCKET"
echo "  INFLUXDB_ENABLED: $INFLUXDB_ENABLED"
echo "  INFLUXDB_TOKEN: ${INFLUXDB_TOKEN:0:10}...${INFLUXDB_TOKEN: -10}"

# Check if application config matches
if grep -q "api-version: v2" src/main/resources/application-prod.yml; then
    success "Application is configured for InfluxDB 2.x native API"
else
    error "Application may not be configured for InfluxDB 2.x native API"
    info "Check src/main/resources/application-prod.yml for 'api-version: v2'"
fi
echo

# Test 10: Token Validation
echo "======================================"
echo "10. TOKEN VALIDATION"
echo "====================================="

info "Validating token format and permissions..."
TOKEN_INFO=$(ssh debian-dockerserver "docker exec raspi-finance-endpoint curl -s -H 'Authorization: Token $INFLUXDB_TOKEN' http://influxdb-server:8086/api/v2/authorizations" 2>/dev/null)

if echo "$TOKEN_INFO" | jq -e '.authorizations | length > 0' >/dev/null 2>&1; then
    success "Token is valid and has permissions"
    echo "Token permissions:"
    echo "$TOKEN_INFO" | jq -r '.authorizations[] | "  - \(.description // "No description") - Status: \(.status)"' 2>/dev/null
else
    error "Token validation failed or has no permissions"
fi
echo

# Recommendations
echo "======================================"
echo "11. RECOMMENDATIONS"
echo "====================================="

echo "Based on the diagnostics above, here are the recommended actions:"
echo

if [[ "$SHARED_NETWORK" == "false" ]]; then
    error "CRITICAL: Network connectivity issue"
    echo "  → Containers are not on the same Docker network"
    echo "  → Application cannot resolve 'influxdb-server' hostname"
    echo
    echo "Solutions:"
    echo "  1. Update INFLUXDB_URL to use external IP: http://192.168.10.10:8086"
    echo "  2. OR: Connect both containers to the same Docker network"
    echo "     docker network connect finance-lan raspi-finance-endpoint"
    echo
elif [[ "$HOSTNAME_IP" == "FAILED" ]]; then
    error "CRITICAL: DNS resolution issue"
    echo "  → Containers are on same network but hostname resolution failing"
    echo "  → Docker internal DNS may have issues"
    echo
    echo "Solutions:"
    echo "  1. Update INFLUXDB_URL to use direct IP: http://172.19.0.2:8086"
    echo "  2. OR: Restart both containers to refresh DNS"
    echo "     ssh debian-dockerserver 'docker restart influxdb-server raspi-finance-endpoint'"
    echo "  3. OR: Use external IP: http://192.168.10.10:8086"
    echo
fi

if [[ $HTTP_CODE != "200" ]]; then
    error "CRITICAL: Authentication is failing"
    echo "  → Check if the InfluxDB container was properly initialized"
    echo "  → Verify the token in env.influx matches the InfluxDB setup"
    echo "  → Consider recreating the InfluxDB container with fresh initialization"
    echo
    echo "Commands to fix:"
    echo "  ssh debian-dockerserver 'docker stop influxdb-server && docker rm influxdb-server && docker volume rm influxdb-data'"
    echo "  ssh debian-dockerserver 'cd /path/to/compose && docker compose -f docker-compose-influxdb.yml up -d'"
fi

if [[ $WRITE_HTTP_CODE != "204" ]]; then
    warning "Write permissions may be insufficient"
    echo "  → The token may not have write access to the bucket"
    echo "  → Check if the bucket was created during initialization"
fi

echo
info "Re-run this script after making changes to verify fixes"
echo "======================================"