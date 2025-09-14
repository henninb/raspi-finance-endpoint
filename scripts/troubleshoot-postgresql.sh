#!/bin/bash

# PostgreSQL Troubleshooting Script
# Diagnoses authentication and connection issues

set -e

echo "======================================"
echo "PostgreSQL Troubleshooting Script"
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
if [[ ! -f "env.secrets" ]] || [[ ! -f "env.prod" ]]; then
    error "env.secrets or env.prod file not found. Run this script from the project root directory."
    exit 1
fi

# Load environment variables
source env.prod
source env.secrets
echo "Environment variables loaded from env.prod and env.secrets"
echo

# Test 1: Container Status
echo "======================================"
echo "1. CONTAINER STATUS CHECK"
echo "======================================"

if ssh debian-dockerserver 'docker ps --filter name=postgresql-server --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"' | grep -q postgresql-server; then
    success "PostgreSQL container is running"
    ssh debian-dockerserver 'docker ps --filter name=postgresql-server --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"'
else
    error "PostgreSQL container is not running"
    info "Starting PostgreSQL container..."
    ssh debian-dockerserver 'docker start postgresql-server || echo "Container does not exist"'
fi
echo

# Test 2: Docker Network Check
echo "======================================"
echo "2. DOCKER NETWORK CHECK"
echo "======================================"

info "Checking Docker network configuration..."

# Get PostgreSQL container network
POSTGRESQL_NETWORKS=$(ssh debian-dockerserver 'docker inspect postgresql-server --format "{{range .NetworkSettings.Networks}}{{.NetworkID}} {{end}}"' 2>/dev/null || echo "FAILED")
if [[ "$POSTGRESQL_NETWORKS" == "FAILED" ]]; then
    error "Cannot inspect PostgreSQL container networks"
else
    info "PostgreSQL networks: $POSTGRESQL_NETWORKS"
fi

# Get application container network if it exists
APP_NETWORKS=$(ssh debian-dockerserver 'docker inspect raspi-finance-endpoint --format "{{range .NetworkSettings.Networks}}{{.NetworkID}} {{end}}"' 2>/dev/null || echo "NOT_RUNNING")
if [[ "$APP_NETWORKS" == "NOT_RUNNING" ]]; then
    warning "Application container not running - cannot check network"
else
    info "Application networks: $APP_NETWORKS"
fi

# Check if they share a network
if [[ "$POSTGRESQL_NETWORKS" != "FAILED" && "$APP_NETWORKS" != "NOT_RUNNING" ]]; then
    SHARED_NETWORK="false"
    for postgres_net in $POSTGRESQL_NETWORKS; do
        for app_net in $APP_NETWORKS; do
            if [[ "$postgres_net" == "$app_net" ]]; then
                SHARED_NETWORK="true"
                success "Containers share network: $postgres_net"
                break 2
            fi
        done
    done

    if [[ "$SHARED_NETWORK" == "false" ]]; then
        error "Containers are NOT on the same Docker network"
        warning "This explains why 'postgresql-server' hostname doesn't resolve"
    fi
fi

# Show detailed network info
info "Detailed network information:"
echo "PostgreSQL container networks:"
ssh debian-dockerserver 'docker inspect postgresql-server --format "{{json .NetworkSettings.Networks}}"' 2>/dev/null | jq -r 'to_entries[] | "  - \(.key): \(.value.IPAddress)"' 2>/dev/null || echo "  Unable to parse network info"

if [[ "$APP_NETWORKS" != "NOT_RUNNING" ]]; then
    echo "Application container networks:"
    ssh debian-dockerserver 'docker inspect raspi-finance-endpoint --format "{{json .NetworkSettings.Networks}}"' 2>/dev/null | jq -r 'to_entries[] | "  - \(.key): \(.value.IPAddress)"' 2>/dev/null || echo "  Unable to parse network info"
fi

echo

# Test 3: Network Connectivity
echo "======================================"
echo "3. NETWORK CONNECTIVITY CHECK"
echo "======================================"

info "Testing network connectivity to PostgreSQL..."

# Get PostgreSQL container IP
POSTGRES_IP=$(ssh debian-dockerserver 'docker inspect postgresql-server --format "{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}"' 2>/dev/null | head -1)

# Test hostname resolution first
info "Testing DNS resolution of 'postgresql-server'..."
HOSTNAME_IP=$(ssh debian-dockerserver 'docker exec raspi-finance-endpoint nslookup postgresql-server 2>/dev/null | grep "Address:" | tail -1 | cut -d" " -f2' 2>/dev/null || echo "FAILED")
if [[ "$HOSTNAME_IP" == "FAILED" ]]; then
    error "DNS resolution failed for 'postgresql-server'"
    info "Trying alternative resolution methods..."

    # Try getent hosts
    GETENT_IP=$(ssh debian-dockerserver 'docker exec raspi-finance-endpoint getent hosts postgresql-server 2>/dev/null | cut -d" " -f1' || echo "FAILED")
    if [[ "$GETENT_IP" != "FAILED" ]]; then
        success "getent resolved postgresql-server to: $GETENT_IP"
    else
        error "getent also failed to resolve postgresql-server"
    fi

    # Try direct IP connection if we have the IP
    if [[ -n "$POSTGRES_IP" ]]; then
        info "Testing direct IP connection to $POSTGRES_IP..."
        if ssh debian-dockerserver "docker exec raspi-finance-endpoint nc -z $POSTGRES_IP 5432" >/dev/null 2>&1; then
            success "Direct IP connection works!"
            error "Problem is DNS resolution, not network connectivity"
        else
            error "Direct IP connection also failed"
        fi
    fi
else
    success "DNS resolved postgresql-server to: $HOSTNAME_IP"
fi

# Test hostname connectivity using PostgreSQL's own tools
if ssh debian-dockerserver 'docker exec postgresql-server pg_isready -h postgresql-server -p 5432' >/dev/null 2>&1; then
    success "Network connectivity to PostgreSQL is working"
else
    error "Cannot connect to PostgreSQL at postgresql-server:5432 from within network"
    info "Testing external connectivity..."
    if ssh debian-dockerserver 'docker exec postgresql-server pg_isready -h 192.168.10.10 -p 5432' >/dev/null 2>&1; then
        success "External connectivity works"
    else
        error "External connectivity also failed"
    fi

    # Try connectivity test from application container using a different method
    info "Testing application container connectivity..."
    APP_CONN_TEST=$(ssh debian-dockerserver 'docker exec raspi-finance-endpoint timeout 5 bash -c "</dev/tcp/postgresql-server/5432"' 2>/dev/null && echo "SUCCESS" || echo "FAILED")
    if [[ "$APP_CONN_TEST" == "SUCCESS" ]]; then
        success "Application container can reach PostgreSQL via /dev/tcp"
    else
        warning "Application container connectivity test failed"
    fi
fi
echo

# Test 4: PostgreSQL Health Check
echo "======================================"
echo "4. POSTGRESQL HEALTH CHECK"
echo "======================================"

info "Checking PostgreSQL server status..."

# Use PostgreSQL container directly (has pg_isready built-in)
HEALTH_RESPONSE=$(ssh debian-dockerserver 'docker exec postgresql-server pg_isready -h localhost -p 5432 -U postgres' 2>/dev/null || echo "FAILED")

if [[ "$HEALTH_RESPONSE" == "FAILED" ]]; then
    error "Health check failed - PostgreSQL may not be responding"
else
    echo "Health Response: $HEALTH_RESPONSE"
    if echo "$HEALTH_RESPONSE" | grep -q "accepting connections"; then
        success "PostgreSQL health check passed"
    else
        warning "PostgreSQL health check returned non-accepting status"
    fi
fi
echo

# Test 5: Authentication Test
echo "======================================"
echo "5. AUTHENTICATION TEST"
echo "======================================"

# Extract database info from DATASOURCE URL
DB_NAME=$(echo "$DATASOURCE" | sed 's/.*\///' || echo "finance_db")
DB_USER="$DATASOURCE_USERNAME"
DB_PASS="$DATASOURCE_PASSWORD"

info "Testing authentication with configured credentials..."
info "Database: $DB_NAME"
info "Username: $DB_USER"
info "Password: ${DB_PASS:0:3}***"

# Test authentication using PostgreSQL container directly
AUTH_TEST=$(ssh debian-dockerserver "PGPASSWORD='$DB_PASS' docker exec postgresql-server psql -h localhost -p 5432 -U '$DB_USER' -d '$DB_NAME' -c 'SELECT version();' -t" 2>&1 || echo "FAILED")

if echo "$AUTH_TEST" | grep -q "PostgreSQL"; then
    success "Authentication successful"
    echo "PostgreSQL Version:"
    echo "$AUTH_TEST" | head -1 | sed 's/^ */  /'
else
    error "Authentication failed"
    echo "Error details: $AUTH_TEST"

    # Try with postgres superuser
    info "Trying with postgres superuser..."
    AUTH_TEST_ALT=$(ssh debian-dockerserver "docker exec postgresql-server psql -h localhost -p 5432 -U postgres -d '$DB_NAME' -c 'SELECT 1;' -t" 2>&1 || echo "FAILED")

    if echo "$AUTH_TEST_ALT" | grep -q "1"; then
        success "Superuser authentication worked"
    else
        error "Superuser authentication also failed: $AUTH_TEST_ALT"
    fi
fi
echo

# Test 6: Database Verification
echo "======================================"
echo "6. DATABASE VERIFICATION"
echo "======================================"

info "Checking if database '$DB_NAME' exists..."
DB_CHECK=$(ssh debian-dockerserver "PGPASSWORD='$DB_PASS' docker exec postgresql-server psql -h localhost -p 5432 -U '$DB_USER' -l" 2>/dev/null | grep "$DB_NAME" || echo "NOT_FOUND")

if [[ "$DB_CHECK" != "NOT_FOUND" ]]; then
    success "Database '$DB_NAME' exists"
    echo "Database info: $DB_CHECK"
else
    error "Database '$DB_NAME' does not exist"
    info "Available databases:"
    ssh debian-dockerserver "PGPASSWORD='$DB_PASS' docker exec postgresql-server psql -h localhost -p 5432 -U '$DB_USER' -l" 2>/dev/null | grep -E '^ [a-zA-Z]' | head -10 || echo "  Could not list databases"
fi
echo

# Test 7: Connection Pool Test
echo "======================================"
echo "7. CONNECTION POOL TEST"
echo "======================================"

info "Testing connection pool and active connections..."
CONNECTION_TEST=$(ssh debian-dockerserver "PGPASSWORD='$DB_PASS' docker exec postgresql-server psql -h localhost -p 5432 -U '$DB_USER' -d '$DB_NAME' -c \"SELECT count(*) FROM pg_stat_activity WHERE datname='$DB_NAME';\" -t" 2>/dev/null || echo "FAILED")

if [[ "$CONNECTION_TEST" != "FAILED" ]]; then
    CONN_COUNT=$(echo "$CONNECTION_TEST" | tr -d ' ')
    success "Connection pool test successful - $CONN_COUNT active connections"

    # Test max connections
    MAX_CONN=$(ssh debian-dockerserver "PGPASSWORD='$DB_PASS' docker exec postgresql-server psql -h localhost -p 5432 -U '$DB_USER' -d '$DB_NAME' -c 'SHOW max_connections;' -t" 2>/dev/null | tr -d ' ' || echo "FAILED")
    if [[ "$MAX_CONN" != "FAILED" ]]; then
        info "Max connections configured: $MAX_CONN"
        if [[ $CONN_COUNT -gt $(($MAX_CONN * 80 / 100)) ]]; then
            warning "Connection usage is high: $CONN_COUNT/$MAX_CONN (>80%)"
        fi
    fi
else
    error "Connection pool test failed"
fi
echo

# Test 8: Table Structure Test
echo "======================================"
echo "8. TABLE STRUCTURE TEST"
echo "======================================"

info "Checking key application tables..."
TABLE_TEST=$(ssh debian-dockerserver "PGPASSWORD='$DB_PASS' docker exec postgresql-server psql -h localhost -p 5432 -U '$DB_USER' -d '$DB_NAME' -c \"SELECT table_name FROM information_schema.tables WHERE table_schema = 'public' ORDER BY table_name;\" -t" 2>/dev/null || echo "FAILED")

if [[ "$TABLE_TEST" != "FAILED" ]]; then
    success "Table structure query successful"
    echo "Available tables:"
    echo "$TABLE_TEST" | while read -r table; do
        if [[ -n "$table" ]]; then
            echo "  - $(echo $table | tr -d ' ')"
        fi
    done

    # Check for key tables
    KEY_TABLES=("account" "transaction_account" "category" "payment")
    for table in "${KEY_TABLES[@]}"; do
        if echo "$TABLE_TEST" | grep -q "$table"; then
            success "Key table '$table' found"
        else
            warning "Key table '$table' missing"
        fi
    done
else
    error "Table structure test failed"
fi
echo

# Test 9: Container Logs
echo "======================================"
echo "9. CONTAINER LOGS ANALYSIS"
echo "======================================"

info "Checking recent PostgreSQL container logs for errors..."
ssh debian-dockerserver 'docker logs --tail 20 postgresql-server 2>&1' | while IFS= read -r line; do
    if echo "$line" | grep -qi "error\|fail\|fatal"; then
        error "LOG: $line"
    elif echo "$line" | grep -qi "warn"; then
        warning "LOG: $line"
    else
        echo "LOG: $line"
    fi
done
echo

# Test 10: Application Configuration Check
echo "======================================"
echo "10. APPLICATION CONFIGURATION CHECK"
echo "======================================"

info "Checking application configuration..."
echo "Current environment configuration:"
echo "  DATASOURCE: ${DATASOURCE:-not set}"
echo "  DATASOURCE_USERNAME: ${DATASOURCE_USERNAME:-not set}"
echo "  DATASOURCE_PASSWORD: ${DATASOURCE_PASSWORD:+set (${#DATASOURCE_PASSWORD} chars)}${DATASOURCE_PASSWORD:-not set}"
echo "  Parsed DB_NAME: $DB_NAME"

# Check if application config uses the right values
if grep -q "postgresql-server" src/main/resources/application-prod.yml 2>/dev/null; then
    success "Application is configured to use postgresql-server hostname"
else
    warning "Application may not be configured for postgresql-server hostname"
    info "Check src/main/resources/application-prod.yml for database configuration"
fi
echo

# Test 11: Performance Test
echo "======================================"
echo "11. PERFORMANCE TEST"
echo "======================================"

info "Running basic performance test..."
PERF_START=$(date +%s%3N)
PERF_TEST=$(ssh debian-dockerserver "PGPASSWORD='$DB_PASS' docker exec postgresql-server psql -h localhost -p 5432 -U '$DB_USER' -d '$DB_NAME' -c 'SELECT COUNT(*) FROM information_schema.columns;' -t" 2>/dev/null || echo "FAILED")
PERF_END=$(date +%s%3N)

if [[ "$PERF_TEST" != "FAILED" ]]; then
    PERF_TIME=$((PERF_END - PERF_START))
    success "Performance test completed in ${PERF_TIME}ms"

    if [[ $PERF_TIME -lt 100 ]]; then
        success "Excellent response time (<100ms)"
    elif [[ $PERF_TIME -lt 500 ]]; then
        info "Good response time (<500ms)"
    elif [[ $PERF_TIME -lt 1000 ]]; then
        warning "Slow response time (<1000ms)"
    else
        error "Very slow response time (>1000ms) - investigate performance"
    fi
else
    error "Performance test failed"
fi
echo

# Recommendations
echo "======================================"
echo "12. RECOMMENDATIONS"
echo "======================================"

echo "Based on the diagnostics above, here are the recommended actions:"
echo

if [[ "$SHARED_NETWORK" == "false" ]]; then
    error "CRITICAL: Network connectivity issue"
    echo "  → Containers are not on the same Docker network"
    echo "  → Application cannot resolve 'postgresql-server' hostname"
    echo
    echo "Solutions:"
    echo "  1. Connect both containers to the same Docker network"
    echo "     ssh debian-dockerserver 'docker network connect finance-lan raspi-finance-endpoint'"
    echo "  2. OR: Update DATABASE_URL to use external IP: jdbc:postgresql://192.168.10.10:5432/finance_db"
    echo
elif [[ "$HOSTNAME_IP" == "FAILED" ]]; then
    error "CRITICAL: DNS resolution issue"
    echo "  → Containers are on same network but hostname resolution failing"
    echo "  → Docker internal DNS may have issues"
    echo
    echo "Solutions:"
    echo "  1. Update DATABASE_URL to use direct IP: jdbc:postgresql://$POSTGRES_IP:5432/finance_db"
    echo "  2. OR: Restart both containers to refresh DNS"
    echo "     ssh debian-dockerserver 'docker restart postgresql-server raspi-finance-endpoint'"
    echo "  3. OR: Use external IP: jdbc:postgresql://192.168.10.10:5432/finance_db"
    echo
fi

if echo "$AUTH_TEST" | grep -q "FAILED\|authentication failed\|password"; then
    error "CRITICAL: Authentication is failing"
    echo "  → Check if PostgreSQL credentials are correct"
    echo "  → Verify the password in env.secrets matches PostgreSQL setup"
    echo "  → Consider checking pg_hba.conf authentication methods"
    echo
    echo "Commands to check:"
    echo "  ssh debian-dockerserver 'docker exec postgresql-server cat /var/lib/postgresql/data/pg_hba.conf'"
    echo "  ssh debian-dockerserver 'docker logs postgresql-server | grep -i auth'"
fi

if [[ "$DB_CHECK" == "NOT_FOUND" ]]; then
    warning "Database missing or inaccessible"
    echo "  → The target database may not exist or is not accessible"
    echo "  → Check if database was created during initialization"
fi

if [[ "$CONNECTION_TEST" == "FAILED" ]]; then
    warning "Connection pool issues detected"
    echo "  → Check HikariCP configuration in application.yml"
    echo "  → Monitor connection leaks and timeout settings"
fi

echo
info "Re-run this script after making changes to verify fixes"
echo "======================================"