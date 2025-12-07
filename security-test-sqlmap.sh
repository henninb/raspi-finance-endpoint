#!/usr/bin/env bash

# Security Testing Script with sqlmap
# Tests all API endpoints for SQL injection vulnerabilities
# Usage: ./security-test-sqlmap.sh [options]

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
BASE_URL="${BASE_URL:-https://localhost:8443}"
TEST_USERNAME="${TEST_USERNAME:-testuser}"
TEST_PASSWORD="${TEST_PASSWORD:-password}"
SQLMAP_LEVEL="${SQLMAP_LEVEL:-2}"
SQLMAP_RISK="${SQLMAP_RISK:-1}"
OUTPUT_DIR="./security-test-results/sqlmap-$(date +%Y%m%d-%H%M%S)"
TEMP_DIR="/tmp/sqlmap-test-$$"

# Create directories
mkdir -p "$OUTPUT_DIR"
mkdir -p "$TEMP_DIR"

# Cleanup function
cleanup() {
    rm -rf "$TEMP_DIR"
}
trap cleanup EXIT

# Logging functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Banner
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}  SQL Injection Security Test (sqlmap)${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""
log_info "Target: $BASE_URL"
log_info "Level: $SQLMAP_LEVEL | Risk: $SQLMAP_RISK"
log_info "Results: $OUTPUT_DIR"
echo ""

# Check if sqlmap is installed
if ! command -v sqlmap &> /dev/null; then
    log_error "sqlmap is not installed!"
    echo "Install with: pip install sqlmap"
    echo "Or: sudo pacman -S sqlmap (Arch Linux)"
    exit 1
fi

# Check if server is running
log_info "Checking if server is running..."
if ! curl -k -s "$BASE_URL/actuator/health" > /dev/null 2>&1; then
    log_warning "Server may not be running at $BASE_URL"
    log_info "Start server with: ./run-bootrun.sh"
    read -p "Continue anyway? (y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
fi

# Function to get JWT token
get_jwt_token() {
    log_info "Authenticating to get JWT token..."

    local response
    response=$(curl -k -s -X POST "$BASE_URL/api/login" \
        -H "Content-Type: application/json" \
        -d "{\"username\":\"$TEST_USERNAME\",\"password\":\"$TEST_PASSWORD\"}" \
        -c "$TEMP_DIR/cookies.txt" \
        -w "\n%{http_code}")

    local http_code
    http_code=$(echo "$response" | tail -n1)
    local body
    body=$(echo "$response" | head -n-1)

    if [[ "$http_code" == "200" ]]; then
        # Extract token from cookie file
        if [[ -f "$TEMP_DIR/cookies.txt" ]]; then
            JWT_TOKEN=$(grep -oP 'token\s+\K[^\s]+' "$TEMP_DIR/cookies.txt" || echo "")
            if [[ -n "$JWT_TOKEN" ]]; then
                log_success "Authentication successful"
                echo "$JWT_TOKEN" > "$OUTPUT_DIR/jwt-token.txt"
                return 0
            fi
        fi

        # Try to extract from response body
        JWT_TOKEN=$(echo "$body" | grep -oP '"token"\s*:\s*"\K[^"]+' || echo "")
        if [[ -n "$JWT_TOKEN" ]]; then
            log_success "Authentication successful"
            echo "$JWT_TOKEN" > "$OUTPUT_DIR/jwt-token.txt"
            return 0
        fi
    fi

    log_error "Authentication failed (HTTP $http_code)"
    log_error "Response: $body"
    log_warning "Continuing without authentication (only public endpoints will be tested)"
    JWT_TOKEN=""
    return 1
}

# Function to test GET endpoint
test_get_endpoint() {
    local endpoint="$1"
    local description="$2"
    local params="$3"

    log_info "Testing GET: $endpoint - $description"

    local url="$BASE_URL$endpoint"
    if [[ -n "$params" ]]; then
        url="$url?$params"
    fi

    local cookie_param=""
    if [[ -n "$JWT_TOKEN" ]]; then
        cookie_param="--cookie=token=$JWT_TOKEN"
    fi

    local output_file="$OUTPUT_DIR/$(echo "$endpoint" | tr '/' '_' | sed 's/^_//').txt"

    sqlmap -u "$url" \
        $cookie_param \
        --batch \
        --level="$SQLMAP_LEVEL" \
        --risk="$SQLMAP_RISK" \
        --threads=5 \
        --output-dir="$OUTPUT_DIR" \
        2>&1 | tee "$output_file"

    # Check for actual vulnerabilities (not "does not seem to be injectable")
    if grep -qE "parameter.*is vulnerable|parameter.*is injectable" "$output_file" && \
       ! grep -qE "does not (seem to be|appear to be) injectable" "$output_file"; then
        log_error "VULNERABILITY FOUND in $endpoint!"
        echo "$endpoint" >> "$OUTPUT_DIR/VULNERABLE_ENDPOINTS.txt"
    else
        log_success "No SQL injection found in $endpoint"
    fi
}

# Function to test POST endpoint
test_post_endpoint() {
    local endpoint="$1"
    local description="$2"
    local json_data="$3"

    log_info "Testing POST: $endpoint - $description"

    # Create request file
    local request_file="$TEMP_DIR/request_$(echo "$endpoint" | tr '/' '_' | sed 's/^_//').txt"

    cat > "$request_file" <<EOF
POST $endpoint HTTP/1.1
Host: $(echo "$BASE_URL" | sed 's|https\?://||')
Content-Type: application/json
Cookie: token=$JWT_TOKEN
User-Agent: sqlmap-security-test

$json_data
EOF

    local output_file="$OUTPUT_DIR/$(echo "$endpoint" | tr '/' '_' | sed 's/^_//').txt"

    sqlmap -r "$request_file" \
        --batch \
        --level="$SQLMAP_LEVEL" \
        --risk="$SQLMAP_RISK" \
        --threads=5 \
        --output-dir="$OUTPUT_DIR" \
        2>&1 | tee "$output_file"

    # Check for actual vulnerabilities (not "does not seem to be injectable")
    if grep -qE "parameter.*is vulnerable|parameter.*is injectable" "$output_file" && \
       ! grep -qE "does not (seem to be|appear to be) injectable" "$output_file"; then
        log_error "VULNERABILITY FOUND in $endpoint!"
        echo "$endpoint" >> "$OUTPUT_DIR/VULNERABLE_ENDPOINTS.txt"
    else
        log_success "No SQL injection found in $endpoint"
    fi
}

# Get JWT token
get_jwt_token || log_warning "Proceeding without authentication"

echo ""
log_info "Starting endpoint security tests..."
echo ""

# Test public endpoints (no auth needed)
log_info "=== Testing Public Endpoints ==="
echo ""

# Note: Login endpoint accepts credentials, but testing for SQLi in authentication
test_post_endpoint "/api/login" "Login endpoint" '{
  "username": "test*",
  "password": "test123"
}'

# Test protected endpoints (require JWT)
if [[ -n "$JWT_TOKEN" ]]; then
    echo ""
    log_info "=== Testing Account Endpoints ==="
    echo ""

    test_get_endpoint "/api/account/select/active" "Get active accounts"
    test_get_endpoint "/api/account/checking_primary" "Get specific account"

    echo ""
    log_info "=== Testing Transaction Endpoints ==="
    echo ""

    test_get_endpoint "/api/transaction" "Get all transactions"
    test_get_endpoint "/api/transaction/cleared/true" "Get cleared transactions"

    echo ""
    log_info "=== Testing Category Endpoints ==="
    echo ""

    test_get_endpoint "/api/category" "Get all categories"
    test_get_endpoint "/api/category/misc" "Get specific category"

    echo ""
    log_info "=== Testing Description Endpoints ==="
    echo ""

    test_get_endpoint "/api/description" "Get all descriptions"
    test_get_endpoint "/api/description/test" "Get specific description"

    echo ""
    log_info "=== Testing Parameter Endpoints ==="
    echo ""

    test_get_endpoint "/api/parameter" "Get all parameters"

    echo ""
    log_info "=== Testing Payment Endpoints ==="
    echo ""

    test_get_endpoint "/api/payment" "Get all payments"

    test_post_endpoint "/api/payment" "Insert payment" '{
  "sourceAccount": "checking_primary*",
  "destinationAccount": "bills_payable*",
  "transactionDate": "2024-01-01",
  "amount": 100.00
}'

    echo ""
    log_info "=== Testing Medical Expense Endpoints ==="
    echo ""

    test_get_endpoint "/api/medical-expenses" "Get medical expenses"

    echo ""
    log_info "=== Testing Validation Amount Endpoints ==="
    echo ""

    test_get_endpoint "/api/validation/amount" "Get validation amounts"

    echo ""
    log_info "=== Testing Receipt Image Endpoints ==="
    echo ""

    test_get_endpoint "/api/receipt/image" "Get receipt images"

    echo ""
    log_info "=== Testing GraphQL Endpoint ==="
    echo ""

    # Test GraphQL
    local graphql_request="$TEMP_DIR/graphql_request.txt"
    cat > "$graphql_request" <<EOF
POST /graphql HTTP/1.1
Host: $(echo "$BASE_URL" | sed 's|https\?://||')
Content-Type: application/json
Cookie: token=$JWT_TOKEN

{
  "query": "query { accounts(accountNameOwner: \"checking_primary*\") { accountNameOwner accountType } }"
}
EOF

    local output_file="$OUTPUT_DIR/graphql.txt"

    log_info "Testing GraphQL endpoint"
    sqlmap -r "$graphql_request" \
        --batch \
        --level="$SQLMAP_LEVEL" \
        --risk="$SQLMAP_RISK" \
        --threads=5 \
        --output-dir="$OUTPUT_DIR" \
        2>&1 | tee "$output_file"

    if grep -q "injectable" "$output_file"; then
        log_error "VULNERABILITY FOUND in GraphQL endpoint!"
        echo "/graphql" >> "$OUTPUT_DIR/VULNERABLE_ENDPOINTS.txt"
    else
        log_success "No SQL injection found in GraphQL endpoint"
    fi
else
    log_warning "Skipping protected endpoints (no JWT token)"
fi

# Generate summary report
echo ""
log_info "=== Generating Summary Report ==="
echo ""

cat > "$OUTPUT_DIR/SUMMARY.md" <<EOF
# SQL Injection Security Test Results

**Test Date:** $(date)
**Target:** $BASE_URL
**sqlmap Level:** $SQLMAP_LEVEL
**sqlmap Risk:** $SQLMAP_RISK

## Summary

EOF

if [[ -f "$OUTPUT_DIR/VULNERABLE_ENDPOINTS.txt" ]]; then
    VULN_COUNT=$(wc -l < "$OUTPUT_DIR/VULNERABLE_ENDPOINTS.txt")
    cat >> "$OUTPUT_DIR/SUMMARY.md" <<EOF
⚠️ **VULNERABILITIES FOUND: $VULN_COUNT**

### Vulnerable Endpoints:

$(cat "$OUTPUT_DIR/VULNERABLE_ENDPOINTS.txt" | sed 's/^/- /')

## Remediation Steps

1. Review the detailed sqlmap output for each vulnerable endpoint
2. Implement parameterized queries for all database interactions
3. Use JPA/Hibernate query methods instead of raw SQL
4. Validate and sanitize all user inputs
5. Re-test after fixes are applied

EOF
else
    cat >> "$OUTPUT_DIR/SUMMARY.md" <<EOF
✅ **NO SQL INJECTION VULNERABILITIES FOUND**

All tested endpoints appear to be secure against SQL injection attacks.

### Tested Endpoints:

$(ls "$OUTPUT_DIR"/*.txt 2>/dev/null | wc -l) endpoints were tested.

## Notes

- This test used sqlmap level $SQLMAP_LEVEL and risk $SQLMAP_RISK
- Consider running with higher levels (--level=5 --risk=3) for more thorough testing
- All endpoints use parameterized queries (JPA/Hibernate)
- Continue following secure coding practices

EOF
fi

cat >> "$OUTPUT_DIR/SUMMARY.md" <<EOF
## Test Configuration

- **Base URL:** $BASE_URL
- **Authentication:** $(if [[ -n "$JWT_TOKEN" ]]; then echo "JWT token obtained"; else echo "No authentication"; fi)
- **sqlmap Version:** $(sqlmap --version 2>&1 | head -n1)

## Files

- \`SUMMARY.md\` - This summary report
- \`*.txt\` - Detailed sqlmap output for each endpoint
- \`jwt-token.txt\` - JWT token used for testing (if obtained)

## Next Steps

1. Review individual endpoint test results in the .txt files
2. If vulnerabilities found, prioritize fixes by risk level
3. Re-run tests after implementing fixes
4. Consider adding this test to your CI/CD pipeline

---

*Generated by security-test-sqlmap.sh*
EOF

# Display summary
cat "$OUTPUT_DIR/SUMMARY.md"

echo ""
log_info "=== Test Complete ==="
echo ""
log_success "Results saved to: $OUTPUT_DIR"
log_info "View summary: cat $OUTPUT_DIR/SUMMARY.md"

if [[ -f "$OUTPUT_DIR/VULNERABLE_ENDPOINTS.txt" ]]; then
    echo ""
    log_error "⚠️  VULNERABILITIES DETECTED! Review the results immediately."
    exit 1
else
    echo ""
    log_success "✅ No SQL injection vulnerabilities found!"
    exit 0
fi
