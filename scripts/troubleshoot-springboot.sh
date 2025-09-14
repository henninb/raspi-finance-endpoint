#!/bin/bash

# Spring Boot Application Troubleshooting Script
# Diagnoses application health, configuration, and connectivity issues

set -e

echo "======================================"
echo "Spring Boot App Troubleshooting Script"
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

if ssh debian-dockerserver 'docker ps --filter name=raspi-finance-endpoint --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"' | grep -q raspi-finance-endpoint; then
    CONTAINER_STATUS=$(ssh debian-dockerserver 'docker ps --filter name=raspi-finance-endpoint --format "{{.Status}}"')
    success "Spring Boot application container is running"
    ssh debian-dockerserver 'docker ps --filter name=raspi-finance-endpoint --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"'

    # Check if healthy
    if echo "$CONTAINER_STATUS" | grep -q "healthy"; then
        success "Container health check is passing"
    elif echo "$CONTAINER_STATUS" | grep -q "unhealthy"; then
        error "Container health check is failing"
    else
        info "Container has no health check configured"
    fi
else
    error "Spring Boot application container is not running"
    info "Checking if container exists..."
    if ssh debian-dockerserver 'docker ps -a --filter name=raspi-finance-endpoint --format "{{.Names}}"' | grep -q raspi-finance-endpoint; then
        warning "Container exists but is stopped"
        info "Starting application container..."
        ssh debian-dockerserver 'docker start raspi-finance-endpoint'
    else
        error "Container does not exist"
    fi
fi
echo

# Test 2: Docker Network Check
echo "======================================"
echo "2. DOCKER NETWORK CHECK"
echo "======================================"

info "Checking Docker network configuration..."

# Get application container network
APP_NETWORKS=$(ssh debian-dockerserver 'docker inspect raspi-finance-endpoint --format "{{range .NetworkSettings.Networks}}{{.NetworkID}} {{end}}"' 2>/dev/null || echo "FAILED")
if [[ "$APP_NETWORKS" == "FAILED" ]]; then
    error "Cannot inspect application container networks"
else
    info "Application networks: $APP_NETWORKS"
fi

# Check network connectivity to dependencies
REQUIRED_SERVICES=("postgresql-server" "influxdb-server")
for service in "${REQUIRED_SERVICES[@]}"; do
    SERVICE_NETWORKS=$(ssh debian-dockerserver "docker inspect $service --format \"{{range .NetworkSettings.Networks}}{{.NetworkID}} {{end}}\"" 2>/dev/null || echo "NOT_RUNNING")
    if [[ "$SERVICE_NETWORKS" == "NOT_RUNNING" ]]; then
        warning "$service container not running"
    else
        info "$service networks: $SERVICE_NETWORKS"

        # Check if they share a network
        SHARED_NETWORK="false"
        for app_net in $APP_NETWORKS; do
            for svc_net in $SERVICE_NETWORKS; do
                if [[ "$app_net" == "$svc_net" ]]; then
                    SHARED_NETWORK="true"
                    success "Application shares network with $service: $app_net"
                    break 2
                fi
            done
        done

        if [[ "$SHARED_NETWORK" == "false" ]]; then
            error "Application and $service are NOT on the same Docker network"
        fi
    fi
done

# Show detailed network info
info "Detailed application network information:"
ssh debian-dockerserver 'docker inspect raspi-finance-endpoint --format "{{json .NetworkSettings.Networks}}"' 2>/dev/null | jq -r 'to_entries[] | "  - \(.key): \(.value.IPAddress)"' 2>/dev/null || echo "  Unable to parse network info"

echo

# Test 3: Application Health Check
echo "======================================"
echo "3. APPLICATION HEALTH CHECK"
echo "======================================"

info "Testing Spring Boot Actuator health endpoint..."

# Try internal health check first
HEALTH_RESPONSE=$(ssh debian-dockerserver 'docker exec raspi-finance-endpoint curl -s http://localhost:8443/actuator/health' 2>/dev/null || echo "FAILED")

if [[ "$HEALTH_RESPONSE" == "FAILED" ]]; then
    warning "Internal health check failed, trying external endpoint..."
    # Try external endpoint
    HEALTH_RESPONSE=$(ssh debian-dockerserver 'curl -s http://192.168.10.10:8443/actuator/health' 2>/dev/null || echo "FAILED")
fi

if [[ "$HEALTH_RESPONSE" == "FAILED" ]]; then
    error "Health endpoint is not responding"
    info "This could indicate:"
    echo "  - Application is not fully started"
    echo "  - Actuator endpoints are disabled"
    echo "  - Network connectivity issues"
    echo "  - Application crashed during startup"
else
    echo "Health Response: $HEALTH_RESPONSE"
    if echo "$HEALTH_RESPONSE" | jq -e '.status == "UP"' >/dev/null 2>&1; then
        success "Application health check passed"

        # Check component health
        if echo "$HEALTH_RESPONSE" | jq -e '.components' >/dev/null 2>&1; then
            info "Component health status:"
            echo "$HEALTH_RESPONSE" | jq -r '.components | to_entries[] | "  - \(.key): \(.value.status)"' 2>/dev/null || echo "  Could not parse components"
        fi
    else
        error "Application health check failed"
        echo "$HEALTH_RESPONSE" | jq '.' 2>/dev/null || echo "Could not parse health response"
    fi
fi
echo

# Test 4: Application Startup Logs
echo "======================================"
echo "4. APPLICATION STARTUP ANALYSIS"
echo "======================================"

info "Analyzing recent application logs..."

# Check for startup completion
STARTUP_LOGS=$(ssh debian-dockerserver 'docker logs --tail 50 raspi-finance-endpoint 2>&1')

# Check for Spring Boot startup completion - handle different log formats
if echo "$STARTUP_LOGS" | grep -qi "started.*application.*in.*seconds"; then
    STARTUP_TIME=$(echo "$STARTUP_LOGS" | grep -i "started.*application.*in.*seconds" | tail -1 | sed -n 's/.*in \([0-9.]*\) seconds.*/\1/p')
    success "Application started successfully in ${STARTUP_TIME}s"
elif ssh debian-dockerserver 'docker logs raspi-finance-endpoint 2>&1' | grep -qi "started.*application.*in.*seconds"; then
    # Check full logs if not in recent logs
    STARTUP_TIME=$(ssh debian-dockerserver 'docker logs raspi-finance-endpoint 2>&1' | grep -i "started.*application.*in.*seconds" | tail -1 | sed -n 's/.*in \([0-9.]*\) seconds.*/\1/p')
    success "Application started successfully in ${STARTUP_TIME}s (found in full logs)"

    # Convert to integer for comparison (multiply by 10 to handle decimals)
    STARTUP_TIME_INT=$(echo "$STARTUP_TIME * 10" | bc 2>/dev/null | cut -d. -f1 || echo "999")

    if [[ $STARTUP_TIME_INT -lt 300 ]]; then  # < 30.0 seconds
        success "Excellent startup time (<30s)"
    elif [[ $STARTUP_TIME_INT -lt 600 ]]; then  # < 60.0 seconds
        info "Good startup time (<60s)"
    else
        warning "Slow startup time (>60s)"
    fi
else
    error "Application startup may not have completed successfully"
fi

# Check for errors in logs - fix multiline issue
ERROR_COUNT=$(echo "$STARTUP_LOGS" | grep -c "ERROR\|FATAL" 2>/dev/null || echo "0" | head -1 | tr -d '\n')
WARN_COUNT=$(echo "$STARTUP_LOGS" | grep -c "WARN" 2>/dev/null || echo "0" | head -1 | tr -d '\n')

# Ensure we have valid integers
ERROR_COUNT=${ERROR_COUNT//[^0-9]/}
WARN_COUNT=${WARN_COUNT//[^0-9]/}
ERROR_COUNT=${ERROR_COUNT:-0}
WARN_COUNT=${WARN_COUNT:-0}

info "Log analysis: $ERROR_COUNT errors, $WARN_COUNT warnings"

if [[ $ERROR_COUNT -gt 0 ]]; then
    error "Recent errors found in logs:"
    echo "$STARTUP_LOGS" | grep "ERROR\|FATAL" | tail -5 | while read -r line; do
        echo "  $line"
    done
fi

if [[ $WARN_COUNT -gt 10 ]]; then
    warning "High number of warnings found ($WARN_COUNT)"
    echo "Recent warnings:"
    echo "$STARTUP_LOGS" | grep "WARN" | tail -3 | while read -r line; do
        echo "  $line"
    done
fi

echo

# Test 5: Database Connectivity
echo "======================================"
echo "5. DATABASE CONNECTIVITY TEST"
echo "======================================"

info "Testing database connectivity from application..."

# Extract database info from DATASOURCE URL
DB_NAME=$(echo "$DATASOURCE" | sed 's/.*\///' || echo "finance_db")
DB_HOST=$(echo "$DATASOURCE" | sed 's|jdbc:postgresql://||' | sed 's|:.*||')

info "Database configuration:"
info "  Host: $DB_HOST"
info "  Database: $DB_NAME"
info "  User: $DATASOURCE_USERNAME"

# Test database connectivity via application
DB_CONN_TEST=$(ssh debian-dockerserver 'docker exec raspi-finance-endpoint curl -s http://localhost:8443/actuator/health/db' 2>/dev/null || echo "FAILED")

if [[ "$DB_CONN_TEST" == "FAILED" ]]; then
    error "Cannot test database connectivity via application"
else
    if echo "$DB_CONN_TEST" | jq -e '.status == "UP"' >/dev/null 2>&1; then
        success "Database connectivity test passed"
    else
        error "Database connectivity test failed"
        echo "DB Health Response: $DB_CONN_TEST"
    fi
fi

# Check HikariCP connection pool
HIKARI_INFO=$(echo "$STARTUP_LOGS" | grep -i "hikari\|connection.*pool" | tail -3)
if [[ -n "$HIKARI_INFO" ]]; then
    info "HikariCP connection pool information:"
    echo "$HIKARI_INFO" | while read -r line; do
        echo "  $line"
    done
fi

echo

# Test 6: InfluxDB Connectivity
echo "======================================"
echo "6. INFLUXDB CONNECTIVITY TEST"
echo "======================================"

info "Testing InfluxDB connectivity from application..."

# Check InfluxDB health via application - try multiple paths
FULL_HEALTH=$(ssh debian-dockerserver 'docker exec raspi-finance-endpoint curl -s http://localhost:8443/actuator/health' 2>/dev/null || echo "FAILED")

if [[ "$FULL_HEALTH" != "FAILED" ]]; then
    # Try different possible component names
    INFLUX_HEALTH=$(echo "$FULL_HEALTH" | jq -r '.components.influx.status // .components.influxDb.status // .components.influxdb.status // "NOT_FOUND"' 2>/dev/null)

    case $INFLUX_HEALTH in
        "UP")
            success "InfluxDB connectivity test passed"
            ;;
        "DOWN")
            error "InfluxDB connectivity test failed"
            INFLUX_DETAILS=$(echo "$FULL_HEALTH" | jq -r '.components.influx.details // .components.influxDb.details // .components.influxdb.details // {}' 2>/dev/null)
            if [[ "$INFLUX_DETAILS" != "{}" ]]; then
                echo "InfluxDB error details: $INFLUX_DETAILS"
            fi
            ;;
        "NOT_FOUND"|"null")
            warning "InfluxDB health component not configured in Spring Boot"
            info "This is normal if InfluxDB health indicators are not enabled"

            # Alternative test - check if metrics are being published
            info "Checking InfluxDB metrics publishing instead..."
            METRICS_TEST=$(ssh debian-dockerserver 'docker exec raspi-finance-endpoint curl -s http://localhost:8443/actuator/metrics' 2>/dev/null || echo "FAILED")
            if [[ "$METRICS_TEST" != "FAILED" ]] && echo "$METRICS_TEST" | jq -e '.names[]' | grep -q "influx"; then
                success "InfluxDB metrics are available"
            else
                info "No InfluxDB-specific metrics found"
            fi
            ;;
        *)
            info "InfluxDB status: $INFLUX_HEALTH"
            ;;
    esac
else
    error "Cannot access application health endpoint"
fi

# Check for InfluxDB metrics publishing
INFLUX_LOGS=$(echo "$STARTUP_LOGS" | grep -i "influx")
if [[ -n "$INFLUX_LOGS" ]]; then
    info "InfluxDB integration logs:"
    echo "$INFLUX_LOGS" | tail -3 | while read -r line; do
        if echo "$line" | grep -qi "error\|fail"; then
            error "  $line"
        else
            echo "  $line"
        fi
    done
fi

echo

# Test 7: REST API Endpoints
echo "======================================"
echo "7. REST API ENDPOINTS TEST"
echo "======================================"

info "Testing key REST API endpoints..."

# Test basic endpoints
ENDPOINTS=("/actuator/info" "/actuator/metrics" "/api/accounts" "/api/categories")

for endpoint in "${ENDPOINTS[@]}"; do
    ENDPOINT_TEST=$(ssh debian-dockerserver "curl -s -w '%{http_code}' -o /dev/null http://localhost:8443$endpoint" 2>/dev/null || echo "FAILED")

    case $ENDPOINT_TEST in
        "200")
            success "$endpoint - OK"
            ;;
        "401"|"403")
            warning "$endpoint - Authentication required"
            ;;
        "404")
            error "$endpoint - Not found"
            ;;
        "500")
            error "$endpoint - Internal server error"
            ;;
        "FAILED")
            error "$endpoint - Connection failed"
            ;;
        *)
            info "$endpoint - HTTP $ENDPOINT_TEST"
            ;;
    esac
done

echo

# Test 8: GraphQL Endpoint
echo "======================================"
echo "8. GRAPHQL ENDPOINT TEST"
echo "======================================"

info "Testing GraphQL endpoint..."

# Test GraphQL endpoint availability
GRAPHQL_TEST=$(ssh debian-dockerserver "curl -s -w '%{http_code}' -X POST -H 'Content-Type: application/json' -d '{\"query\":\"query { __typename }\"}' http://localhost:8443/graphql" 2>/dev/null || echo "FAILED")

GRAPHQL_CODE=$(echo "$GRAPHQL_TEST" | tail -c 4)
case $GRAPHQL_CODE in
    "200")
        success "GraphQL endpoint is working"
        ;;
    "400")
        warning "GraphQL endpoint available but query invalid"
        ;;
    "404")
        error "GraphQL endpoint not found"
        ;;
    *)
        error "GraphQL endpoint test failed: $GRAPHQL_CODE"
        ;;
esac

# Check GraphiQL interface
GRAPHIQL_TEST=$(ssh debian-dockerserver "curl -s -w '%{http_code}' -o /dev/null http://localhost:8443/graphiql" 2>/dev/null || echo "FAILED")
if [[ "$GRAPHIQL_TEST" == "200" ]]; then
    success "GraphiQL interface is available"
else
    warning "GraphiQL interface not accessible"
fi

echo

# Test 9: JVM and Performance
echo "======================================"
echo "9. JVM AND PERFORMANCE CHECK"
echo "======================================"

info "Checking JVM metrics and performance..."

# Get JVM metrics if available
JVM_METRICS=$(ssh debian-dockerserver 'docker exec raspi-finance-endpoint curl -s http://localhost:8443/actuator/metrics/jvm.memory.used' 2>/dev/null || echo "FAILED")

if [[ "$JVM_METRICS" != "FAILED" ]]; then
    if echo "$JVM_METRICS" | jq -e '.measurements[0].value' >/dev/null 2>&1; then
        MEMORY_USED=$(echo "$JVM_METRICS" | jq -r '.measurements[0].value' | cut -d. -f1)
        # Ensure we have a valid number
        if [[ "$MEMORY_USED" =~ ^[0-9]+$ ]]; then
            MEMORY_MB=$((MEMORY_USED / 1024 / 1024))
            info "JVM Memory Usage: ${MEMORY_MB}MB"

            if [[ $MEMORY_MB -lt 256 ]]; then
                success "Low memory usage (<256MB)"
            elif [[ $MEMORY_MB -lt 512 ]]; then
                info "Moderate memory usage (<512MB)"
            elif [[ $MEMORY_MB -lt 1024 ]]; then
                warning "High memory usage (<1GB)"
            else
                error "Very high memory usage (>1GB)"
            fi
        else
            warning "Could not parse memory usage value: $MEMORY_USED"
        fi
    fi
fi

# Check for memory/performance issues in logs
PERF_ISSUES=$(echo "$STARTUP_LOGS" | grep -i "memory\|gc\|heap\|performance\|slow" | wc -l)
if [[ $PERF_ISSUES -gt 0 ]]; then
    warning "Found $PERF_ISSUES performance-related log entries"
fi

echo

# Test 10: Configuration Check
echo "======================================"
echo "10. CONFIGURATION CHECK"
echo "======================================"

info "Validating application configuration..."

# Check environment variables
CONFIG_ISSUES=()

if [[ -z "$DATASOURCE" ]]; then
    CONFIG_ISSUES+=("DATASOURCE not configured")
fi

if [[ -z "$DATASOURCE_USERNAME" || -z "$DATASOURCE_PASSWORD" ]]; then
    CONFIG_ISSUES+=("Database credentials not configured")
fi

if [[ -z "$JWT_KEY" ]]; then
    CONFIG_ISSUES+=("JWT_KEY not configured")
fi

if [[ -z "$SPRING_PROFILES_ACTIVE" ]]; then
    CONFIG_ISSUES+=("SPRING_PROFILES_ACTIVE not set")
fi

if [[ ${#CONFIG_ISSUES[@]} -eq 0 ]]; then
    success "Core configuration appears valid"
else
    error "Configuration issues found:"
    for issue in "${CONFIG_ISSUES[@]}"; do
        echo "  - $issue"
    done
fi

# Show active configuration
info "Active configuration:"
echo "  Profile: $SPRING_PROFILES_ACTIVE"
echo "  Server Port: $SERVER_PORT"
echo "  SSL Enabled: $SSL_ENABLED"
echo "  Flyway Enabled: $FLYWAY_ENABLED"
echo "  InfluxDB Enabled: $INFLUXDB_ENABLED"

echo

# Test 11: Security Check
echo "======================================"
echo "11. SECURITY CONFIGURATION CHECK"
echo "======================================"

info "Checking security configuration..."

# Test security endpoints
SECURITY_TEST=$(ssh debian-dockerserver 'curl -s -w "%{http_code}" -o /dev/null http://localhost:8443/api/accounts' 2>/dev/null || echo "FAILED")

case $SECURITY_TEST in
    "401"|"403")
        success "Security is properly configured (authentication required)"
        ;;
    "200")
        warning "Endpoints may be publicly accessible"
        ;;
    *)
        info "Security test result: $SECURITY_TEST"
        ;;
esac

# Check JWT configuration
if [[ ${#JWT_KEY} -gt 30 ]]; then
    success "JWT key is properly configured"
else
    error "JWT key may be too short or missing"
fi

echo

# Recommendations
echo "======================================"
echo "12. RECOMMENDATIONS"
echo "======================================"

echo "Based on the diagnostics above, here are the recommended actions:"
echo

if [[ "$HEALTH_RESPONSE" == "FAILED" ]]; then
    error "CRITICAL: Application health check failing"
    echo "  → Check application startup logs for errors"
    echo "  → Verify all required services are running"
    echo "  → Check memory and resource constraints"
    echo
    echo "Commands to investigate:"
    echo "  ssh debian-dockerserver 'docker logs --tail 100 raspi-finance-endpoint'"
    echo "  ssh debian-dockerserver 'docker stats raspi-finance-endpoint'"
fi

if [[ $ERROR_COUNT -gt 0 ]]; then
    error "Application errors detected in logs"
    echo "  → Review error messages in application logs"
    echo "  → Check database and InfluxDB connectivity"
    echo "  → Verify configuration parameters"
fi

if [[ ${#CONFIG_ISSUES[@]} -gt 0 ]]; then
    error "Configuration issues detected"
    echo "  → Review env.prod and env.secrets files"
    echo "  → Ensure all required environment variables are set"
    echo "  → Verify credentials are correct"
fi

if [[ "$GRAPHQL_CODE" != "200" ]]; then
    warning "GraphQL endpoint issues"
    echo "  → Check GraphQL schema configuration"
    echo "  → Verify GraphQL dependencies are working"
fi

echo
info "Application monitoring URLs (if accessible):"
echo "  Health: http://192.168.10.10:8443/actuator/health"
echo "  Metrics: http://192.168.10.10:8443/actuator/metrics"
echo "  Info: http://192.168.10.10:8443/actuator/info"
echo "  GraphiQL: http://192.168.10.10:8443/graphiql"

echo
info "Re-run this script after making changes to verify fixes"
echo "======================================"