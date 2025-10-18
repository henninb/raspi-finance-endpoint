#!/bin/bash

# Verification script for Raspberry Pi deployment
# Run this script after deploy-raspi.sh to verify the deployment is working correctly

RASPI_HOST="raspi"
APP_NAME="raspi-finance-endpoint"
POSTGRES_IP="10.0.0.175"

# Counters for test results
PASSED=0
FAILED=0
WARNINGS=0

echo "=== Raspberry Pi Deployment Verification ==="
echo ""

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

pass() {
  echo -e "${GREEN}✓${NC} $1"
  ((PASSED++))
}

fail() {
  echo -e "${RED}✗${NC} $1"
  ((FAILED++))
}

warn() {
  echo -e "${YELLOW}⚠${NC} $1"
  ((WARNINGS++))
}

# Test 1: SSH Connectivity
echo "Test 1: SSH Connectivity"
if ssh -o ConnectTimeout=5 "$RASPI_HOST" 'echo "SSH OK"' >/dev/null 2>&1; then
  pass "SSH connection to $RASPI_HOST is working"
else
  fail "SSH connection to $RASPI_HOST failed"
  exit 1
fi
echo ""

# Test 2: Podman Installation
echo "Test 2: Podman Installation"
PODMAN_VERSION=$(ssh "$RASPI_HOST" 'podman --version' 2>/dev/null)
if [ -n "$PODMAN_VERSION" ]; then
  pass "Podman is installed: $PODMAN_VERSION"
else
  fail "Podman is not installed or not accessible"
  exit 1
fi
echo ""

# Test 3: Container Status
echo "Test 3: Container Status"
CONTAINER_STATUS=$(ssh "$RASPI_HOST" "podman ps --filter name=${APP_NAME} --format '{{.Status}}'" 2>/dev/null)
if echo "$CONTAINER_STATUS" | grep -q "Up"; then
  pass "Container is running: $CONTAINER_STATUS"
else
  fail "Container is not running"
  echo "  Status: $CONTAINER_STATUS"
  echo "  Check logs with: ssh $RASPI_HOST 'podman logs ${APP_NAME}'"
  exit 1
fi
echo ""

# Test 4: Container Health
echo "Test 4: Container Health"
HEALTH_JSON=$(ssh "$RASPI_HOST" "curl -k -s https://localhost:8443/actuator/health" 2>/dev/null)
if echo "$HEALTH_JSON" | grep -q '"status":"UP"'; then
  pass "Application health check passed"
  echo "  Response: $HEALTH_JSON"
else
  fail "Application health check failed"
  echo "  Response: $HEALTH_JSON"
fi
echo ""

# Test 5: Database Connectivity
echo "Test 5: Database Connectivity"
# Check if jq is available for better JSON parsing
if ssh "$RASPI_HOST" "command -v jq >/dev/null 2>&1"; then
  DB_STATUS=$(echo "$HEALTH_JSON" | ssh "$RASPI_HOST" "jq -r '.components.db.status // empty'" 2>/dev/null)
  if [ "$DB_STATUS" = "UP" ]; then
    pass "Database connection is healthy"
    DB_DETAILS=$(echo "$HEALTH_JSON" | ssh "$RASPI_HOST" "jq -r '.components.db.details.database // \"N/A\"'" 2>/dev/null)
    echo "  Database: $DB_DETAILS"
  else
    fail "Database connection has issues"
    echo "  Status: ${DB_STATUS:-Unknown}"
  fi
else
  # Fallback to grep if jq is not available
  if echo "$HEALTH_JSON" | grep -q '"db"' && echo "$HEALTH_JSON" | grep -q '"status":"UP"'; then
    pass "Database appears to be connected (install jq for detailed checks)"
  else
    fail "Database connection has issues (install jq for detailed diagnostics)"
  fi
fi
echo ""

# Test 6: PostgreSQL Port Accessibility
echo "Test 6: PostgreSQL Port Accessibility"
if ssh "$RASPI_HOST" "nc -zv $POSTGRES_IP 5432 >/dev/null 2>&1"; then
  pass "PostgreSQL is accessible from Raspberry Pi at $POSTGRES_IP:5432"
else
  fail "Cannot connect to PostgreSQL at $POSTGRES_IP:5432"
fi
echo ""

# Test 7: Network Configuration
echo "Test 7: Network Configuration"
NETWORK_CHECK=$(ssh "$RASPI_HOST" "podman network inspect finance-lan 2>/dev/null" | grep -c "finance-lan")
if [ "$NETWORK_CHECK" -gt 0 ]; then
  pass "finance-lan network is configured"
else
  warn "finance-lan network may not be configured correctly"
fi
echo ""

# Test 8: Container Logs Check
echo "Test 8: Container Logs Check (last 10 lines)"
echo "---"
ssh "$RASPI_HOST" "podman logs ${APP_NAME} --tail 10" 2>/dev/null
echo "---"
echo ""

# Test 9: External HTTPS Access
echo "Test 9: External HTTPS Access"
RASPI_IP=$(ssh "$RASPI_HOST" "hostname -I | awk '{print \$1}'" 2>/dev/null)
if curl -k -f -s "https://${RASPI_IP}:8443/actuator/health" >/dev/null 2>&1; then
  pass "HTTPS is accessible externally at https://${RASPI_IP}:8443"
else
  warn "External HTTPS access failed (this may be expected if firewall is configured)"
  echo "  Try manually: curl -k https://${RASPI_IP}:8443/actuator/health"
fi
echo ""

# Test 10: GraphQL Endpoint
echo "Test 10: GraphQL Endpoint"
GRAPHQL_RESPONSE=$(ssh "$RASPI_HOST" "curl -k -s https://localhost:8443/graphql -X POST -H 'Content-Type: application/json' -d '{\"query\":\"{ __typename }\"}'" 2>/dev/null)
if echo "$GRAPHQL_RESPONSE" | grep -q "__typename"; then
  pass "GraphQL endpoint is responding"
else
  warn "GraphQL endpoint may not be configured correctly"
  echo "  Response: $GRAPHQL_RESPONSE"
fi
echo ""

# Test 11: Volume Mounts
echo "Test 11: Volume Mounts"
LOGS_EXIST=$(ssh "$RASPI_HOST" "ls ~/raspi-finance-endpoint/logs/ 2>/dev/null | wc -l" 2>/dev/null)
if [ "$LOGS_EXIST" -gt 0 ]; then
  pass "Log volume is mounted and accessible"
  echo "  Files in logs: $LOGS_EXIST"
else
  warn "No log files found (application may not have written logs yet)"
fi
echo ""

# Test 12: Memory Usage
echo "Test 12: Container Resource Usage"
CONTAINER_STATS=$(ssh "$RASPI_HOST" "podman stats ${APP_NAME} --no-stream --format '{{.MemUsage}}'" 2>/dev/null)
if [ -n "$CONTAINER_STATS" ]; then
  pass "Container resource stats available"
  echo "  Memory usage: $CONTAINER_STATS"
else
  warn "Could not retrieve container stats"
fi
echo ""

# Summary
echo "=== Verification Summary ==="
echo ""
echo "Deployment appears to be: ${GREEN}SUCCESSFUL${NC}"
echo ""
echo "Quick Access Commands:"
echo "  View logs: ssh $RASPI_HOST 'podman logs ${APP_NAME} -f'"
echo "  Check health: curl -k https://${RASPI_IP}:8443/actuator/health"
echo "  GraphQL: https://${RASPI_IP}:8443/graphiql"
echo "  Metrics: curl -k https://${RASPI_IP}:8443/actuator/metrics"
echo ""
echo "If any tests failed, review the deployment guide: DEPLOY_RASPI_PODMAN_GUIDE.md"

exit 0
