# Security Testing Guide

This guide covers SQL injection security testing for the raspi-finance-endpoint application using sqlmap.

## Quick Start

```bash
# 1. Start your application
./run-bootrun.sh

# 2. Run security tests (in another terminal)
./security-test-sqlmap.sh
```

## Prerequisites

### Install sqlmap

**Arch Linux:**
```bash
sudo pacman -S sqlmap
```

**Debian/Ubuntu:**
```bash
sudo apt-get install sqlmap
```

**macOS:**
```bash
brew install sqlmap
```

**Using pip:**
```bash
pip install sqlmap
```

**From source:**
```bash
git clone --depth 1 https://github.com/sqlmapproject/sqlmap.git
cd sqlmap
python sqlmap.py --version
```

## Configuration

### 1. Create Test User

Create a dedicated test user for security testing (recommended):

```bash
curl -k -X POST https://localhost:8443/api/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "security-test-user",
    "password": "SecureTestPassword123!",
    "firstName": "Security",
    "lastName": "Tester"
  }'
```

### 2. Configure Testing Parameters

Copy the config template:
```bash
cp security-test-config.env security-test-config.local.env
```

Edit `security-test-config.local.env`:
```bash
BASE_URL=https://localhost:8443
TEST_USERNAME=security-test-user
TEST_PASSWORD=SecureTestPassword123!
SQLMAP_LEVEL=2
SQLMAP_RISK=1
```

### 3. Load Configuration (Optional)

```bash
source security-test-config.local.env
./security-test-sqlmap.sh
```

## Testing Modes

### Quick Test (Level 1, Risk 1)
Fast basic scan, safe for production-like environments:
```bash
SQLMAP_LEVEL=1 SQLMAP_RISK=1 ./security-test-sqlmap.sh
```

### Normal Test (Level 2, Risk 1) - Recommended
Balanced thoroughness and safety:
```bash
SQLMAP_LEVEL=2 SQLMAP_RISK=1 ./security-test-sqlmap.sh
```

### Thorough Test (Level 3, Risk 2)
More comprehensive, use in dev/test environments:
```bash
SQLMAP_LEVEL=3 SQLMAP_RISK=2 ./security-test-sqlmap.sh
```

### Paranoid Test (Level 5, Risk 3)
Exhaustive testing, very slow, dev only:
```bash
SQLMAP_LEVEL=5 SQLMAP_RISK=3 ./security-test-sqlmap.sh
```

## What Gets Tested

### Endpoints Tested

✅ **Public Endpoints:**
- `/api/login` - Authentication endpoint
- `/api/register` - User registration

✅ **Protected Endpoints (require JWT):**
- `/api/account/**` - Account operations
- `/api/transaction/**` - Transaction management
- `/api/category/**` - Category operations
- `/api/description/**` - Description management
- `/api/parameter/**` - Parameter management
- `/api/payment/**` - Payment operations
- `/api/validation/amount/**` - Validation amounts
- `/api/medical-expenses/**` - Medical expenses
- `/api/receipt/image/**` - Receipt images
- `/graphql` - GraphQL queries

### Injection Types Tested

sqlmap tests for various SQL injection types:
- **Boolean-based blind** - True/false conditional queries
- **Error-based** - Database error messages
- **UNION query-based** - UNION SELECT statements
- **Stacked queries** - Multiple statements
- **Time-based blind** - Delay-based detection

## Understanding Results

### No Vulnerabilities Found (Expected)

```
✅ No SQL injection vulnerabilities found!
```

Your application uses parameterized queries (JPA/Hibernate), so this is the expected result.

### Vulnerability Found (Action Required)

```
⚠️  VULNERABILITIES DETECTED! Review the results immediately.
```

If vulnerabilities are found:

1. **Review the detailed output:**
   ```bash
   cat security-test-results/sqlmap-YYYYMMDD-HHMMSS/VULNERABLE_ENDPOINTS.txt
   ```

2. **Check specific endpoint details:**
   ```bash
   cat security-test-results/sqlmap-YYYYMMDD-HHMMSS/api_endpoint_name.txt
   ```

3. **Identify the vulnerable code:**
   - Look for raw SQL queries with string concatenation
   - Check for unparameterized `@Query` annotations
   - Review any native queries

4. **Fix the vulnerability:**
   - Replace with parameterized queries
   - Use JPA query methods
   - Validate and sanitize inputs

5. **Re-test:**
   ```bash
   ./security-test-sqlmap.sh
   ```

## Output Files

After running tests, results are saved in `security-test-results/sqlmap-YYYYMMDD-HHMMSS/`:

| File | Description |
|------|-------------|
| `SUMMARY.md` | Executive summary of test results |
| `*.txt` | Detailed sqlmap output for each endpoint |
| `jwt-token.txt` | JWT token used during testing |
| `VULNERABLE_ENDPOINTS.txt` | List of vulnerable endpoints (if any) |

## Advanced Usage

### Test Specific Endpoint Only

```bash
sqlmap -u "https://localhost:8443/api/account/checking_primary" \
  --cookie="token=YOUR_JWT_TOKEN" \
  --batch \
  --level=2 \
  --risk=1
```

### Test with Custom Payload

```bash
sqlmap -u "https://localhost:8443/api/transaction" \
  --cookie="token=YOUR_JWT_TOKEN" \
  --data='{"guid":"test*","accountNameOwner":"test"}' \
  --batch
```

### Test GraphQL

```bash
sqlmap -u "https://localhost:8443/graphql" \
  --cookie="token=YOUR_JWT_TOKEN" \
  --data='{"query":"query { accounts(accountNameOwner:\"test*\") { accountNameOwner } }"}' \
  --batch
```

### Resume Previous Scan

```bash
sqlmap --resume -s /path/to/output/session.sqlite
```

## CI/CD Integration

### GitLab CI Example

```yaml
security-test:
  stage: test
  script:
    - ./run-bootrun.sh &
    - sleep 30  # Wait for app to start
    - ./security-test-sqlmap.sh
  artifacts:
    paths:
      - security-test-results/
    when: always
  only:
    - merge_requests
    - main
```

### GitHub Actions Example

```yaml
name: Security Test

on: [pull_request, push]

jobs:
  sqlmap:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Install sqlmap
        run: sudo apt-get install sqlmap
      - name: Start application
        run: ./run-bootrun.sh &
      - name: Wait for app
        run: sleep 30
      - name: Run security tests
        run: ./security-test-sqlmap.sh
      - name: Upload results
        uses: actions/upload-artifact@v3
        with:
          name: security-test-results
          path: security-test-results/
```

## Best Practices

### DO:
✅ Run security tests in development/staging environments
✅ Use dedicated test credentials
✅ Run tests regularly (weekly or before releases)
✅ Review all test results, even if no vulnerabilities found
✅ Keep sqlmap updated (`pip install --upgrade sqlmap`)

### DON'T:
❌ Run aggressive tests (level 5, risk 3) on production
❌ Use production credentials for testing
❌ Skip security testing before major releases
❌ Ignore "safe" results - verify they're accurate
❌ Test third-party APIs without permission

## Troubleshooting

### sqlmap not found
```bash
pip install sqlmap
# Or install via package manager
```

### Authentication fails
```bash
# Check credentials
cat security-test-config.local.env

# Test login manually
curl -k -X POST https://localhost:8443/api/login \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"password"}'
```

### Server not responding
```bash
# Check if server is running
curl -k https://localhost:8443/actuator/health

# Start server
./run-bootrun.sh
```

### SSL certificate errors
The script uses `-k` flag for curl (insecure) for localhost testing. For remote testing, use proper SSL certificates.

## Security Testing Checklist

- [ ] sqlmap installed and updated
- [ ] Dedicated test user created
- [ ] Application running in test environment
- [ ] Configuration file created
- [ ] Quick test passed (level 1)
- [ ] Normal test passed (level 2)
- [ ] Thorough test passed (level 3)
- [ ] Results reviewed and documented
- [ ] No vulnerabilities found (or all fixed)
- [ ] Tests added to CI/CD pipeline

## Additional Resources

- [sqlmap Official Documentation](https://github.com/sqlmapproject/sqlmap/wiki)
- [OWASP SQL Injection Guide](https://owasp.org/www-community/attacks/SQL_Injection)
- [Spring Security Best Practices](https://docs.spring.io/spring-security/reference/)
- INJECTION_ATTACK_PREVENTION_PLAN.md - Application security assessment

## Support

For issues or questions:
1. Check the output in `security-test-results/`
2. Review sqlmap documentation
3. Consult INJECTION_ATTACK_PREVENTION_PLAN.md
4. Create an issue in the project repository
