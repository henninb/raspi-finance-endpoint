# PostgreSQL SSL Management Scripts

## Available Scripts

### 1. check-postgres-ssl-expiry.sh

**Purpose**: Monitor PostgreSQL SSL certificate expiration

**Usage**:
```bash
./check-postgres-ssl-expiry.sh
```

**Output Example**:
```
PostgreSQL SSL Certificate Status:
  Server: debian-dockerserver
  Certificate: /home/henninb/postgresql-data/server.crt
  Expires: Oct 11 12:26:22 2026 GMT
  Days until expiry: 364
  Status: OK
```

**Exit Codes**:
- `0` - OK (more than 30 days until expiry)
- `1` - WARNING (7-30 days until expiry)
- `2` - CRITICAL (less than 7 days until expiry)

**Automation Options**:

Add to crontab for weekly checks:
```bash
crontab -e

# Add this line:
0 9 * * 1 /home/henninb/projects/github.com/henninb/raspi-finance-endpoint/check-postgres-ssl-expiry.sh || echo "PostgreSQL SSL cert expires soon!"
```

### 2. renew-postgres-ssl.sh

**Purpose**: Automatically renew PostgreSQL SSL certificate

**Usage**:
```bash
./renew-postgres-ssl.sh
```

**What it does**:
1. Backs up current certificates to `~/postgresql-ssl-backups/`
2. Generates new self-signed certificate (365 days validity)
3. Installs certificates on debian-dockerserver
4. Sets correct ownership (postgres:henninb) and permissions (644/600)
5. Restarts PostgreSQL container
6. Verifies SSL connectivity
7. Automatically rolls back if renewal fails

**When to run**:
- 30-60 days before certificate expires
- Can be run anytime for testing (creates new cert with 365 day validity)

**Scheduled Renewal**:

Add to crontab to run 30 days before expiration (September 11, 2026):
```bash
crontab -e

# Add this line:
0 2 11 9 * /home/henninb/projects/github.com/henninb/raspi-finance-endpoint/renew-postgres-ssl.sh >> /var/log/postgres-ssl-renewal.log 2>&1
```

## Configuration

Both scripts are configured to work with:
- **Remote Host**: debian-dockerserver
- **Certificate Location**: /home/henninb/postgresql-data/
- **Database**: 192.168.10.10:5432/finance_db
- **User**: henninb

## Prerequisites

1. **SSH Access**: Passwordless SSH to debian-dockerserver
   ```bash
   ssh debian-dockerserver "echo test"  # Should work without password prompt
   ```

2. **Sudo Access**: Remote sudo access for certificate operations
   ```bash
   ssh debian-dockerserver "sudo ls /home/henninb/postgresql-data/"
   ```

3. **OpenSSL**: Installed locally for certificate generation
   ```bash
   openssl version
   ```

4. **PostgreSQL Client**: For connection testing
   ```bash
   psql --version
   ```

## Testing

### Test Monitoring Script
```bash
cd ~/projects/github.com/henninb/raspi-finance-endpoint
./check-postgres-ssl-expiry.sh
```

### Test Renewal Script (Dry Run Simulation)
The renewal script can be safely tested - it will:
- Generate a new certificate
- Replace the existing one
- Restart PostgreSQL

**Note**: This will actually renew the certificate, but that's safe since it just resets the 365-day timer.

```bash
cd ~/projects/github.com/henninb/raspi-finance-endpoint
./renew-postgres-ssl.sh
```

## Troubleshooting

### "Cannot connect to debian-dockerserver"
**Solution**: Check SSH configuration
```bash
ssh debian-dockerserver "echo test"
```

### "Permission denied" when reading certificate
**Solution**: Ensure sudo access is configured
```bash
ssh debian-dockerserver "sudo ls /home/henninb/postgresql-data/server.crt"
```

### Renewal fails - "PostgreSQL not accepting SSL connections"
**Solution**:
- Check PostgreSQL logs: `ssh debian-dockerserver "docker logs postgresql-server"`
- Verify certificates exist: `ssh debian-dockerserver "sudo ls -la /home/henninb/postgresql-data/server.*"`
- Check certificate permissions: Should be 644 for .crt, 600 for .key, owned by uid 999

### Certificate backup location full
**Solution**: Clean up old backups
```bash
ls -lh ~/postgresql-ssl-backups/
# Remove backups older than 90 days
find ~/postgresql-ssl-backups/ -name "server.*" -mtime +90 -delete
```

## Quick Reference Commands

```bash
# Check certificate expiration
./check-postgres-ssl-expiry.sh

# Renew certificate
./renew-postgres-ssl.sh

# Manual verification from remote
ssh debian-dockerserver "sudo openssl x509 -in /home/henninb/postgresql-data/server.crt -noout -dates"

# Test SSL connection manually
PGPASSWORD=monday1 psql "host=192.168.10.10 dbname=finance_db user=henninb sslmode=require" -c "SELECT version();"

# Check if PostgreSQL SSL is enabled
ssh debian-dockerserver "docker exec postgresql-server psql -U postgres -c 'SHOW ssl;'"

# View certificate details
ssh debian-dockerserver "sudo openssl x509 -in /home/henninb/postgresql-data/server.crt -noout -text"
```

## Integration with CI/CD

Add to your deployment pipeline:

```yaml
# Example GitHub Actions workflow
- name: Check PostgreSQL SSL Certificate
  run: |
    cd ~/projects/github.com/henninb/raspi-finance-endpoint
    ./check-postgres-ssl-expiry.sh
    if [ $? -eq 2 ]; then
      echo "::warning::PostgreSQL SSL certificate expires soon!"
    fi
```

## Backup Strategy

Certificate backups are automatically created in:
```
~/postgresql-ssl-backups/
  ├── server.crt.20251011-123045
  ├── server.key.20251011-123045
  ├── server.crt.20260911-020000
  └── server.key.20260911-020000
```

**Retention Policy**: Keep backups for 90 days

**Cleanup**:
```bash
find ~/postgresql-ssl-backups/ -name "server.*" -mtime +90 -delete
```

## Security Notes

1. **Private Key Protection**: The renewal script handles keys securely
2. **Self-Signed Certificates**: Appropriate for internal database use
3. **Certificate Validation**: Applications use `sslmode=require` (encrypts but doesn't verify CA)
4. **No Password in Scripts**: Uses environment variable or ~/.pgpass

## Support

For issues or questions:
1. Check the main documentation: `POSTGRESQL_SSL_CERTIFICATE_MANAGEMENT.md`
2. Review PostgreSQL logs: `docker logs postgresql-server`
3. Test SSH connectivity: `ssh debian-dockerserver "echo test"`

## Version History

- **2025-10-11**: Initial version with monitoring and renewal scripts
  - Certificate expires: 2026-10-11
  - Scripts location: ~/projects/github.com/henninb/raspi-finance-endpoint/
