# PostgreSQL SSL Certificate Management Guide

## Current Certificate Details

**Certificate Information:**
- **Type**: Self-signed certificate
- **Common Name (CN)**: postgresql-server
- **Issued**: October 11, 2025
- **Expires**: October 11, 2026 (365 days)
- **Location**: `/home/henninb/postgresql-data/server.crt` and `server.key`
- **Purpose**: Internal database encryption only

## Certificate Expiration Timeline

| Date | Days Until Expiry | Action |
|------|-------------------|--------|
| October 11, 2026 | 0 | Certificate expires |
| September 11, 2026 | 30 | **Renew certificate** (recommended) |
| August 11, 2026 | 60 | Prepare renewal plan |

## Automatic Expiration Monitoring

### Method 1: Create Monitoring Script

Create `/home/henninb/check-postgres-ssl-expiry.sh`:

```bash
#!/bin/bash
# PostgreSQL SSL Certificate Expiry Monitor

CERT_FILE="/home/henninb/postgresql-data/server.crt"
WARN_DAYS=30
CRIT_DAYS=7

if [ ! -f "$CERT_FILE" ]; then
    echo "ERROR: Certificate file not found: $CERT_FILE"
    exit 2
fi

# Get expiration date
EXPIRY_DATE=$(sudo openssl x509 -in "$CERT_FILE" -noout -enddate | cut -d= -f2)
EXPIRY_EPOCH=$(date -d "$EXPIRY_DATE" +%s)
CURRENT_EPOCH=$(date +%s)
DAYS_UNTIL_EXPIRY=$(( ($EXPIRY_EPOCH - $CURRENT_EPOCH) / 86400 ))

echo "PostgreSQL SSL Certificate Status:"
echo "  Expires: $EXPIRY_DATE"
echo "  Days until expiry: $DAYS_UNTIL_EXPIRY"

if [ $DAYS_UNTIL_EXPIRY -le $CRIT_DAYS ]; then
    echo "  Status: CRITICAL - Certificate expires in $DAYS_UNTIL_EXPIRY days!"
    exit 2
elif [ $DAYS_UNTIL_EXPIRY -le $WARN_DAYS ]; then
    echo "  Status: WARNING - Certificate expires in $DAYS_UNTIL_EXPIRY days"
    exit 1
else
    echo "  Status: OK"
    exit 0
fi
```

**Make it executable:**
```bash
chmod +x /home/henninb/check-postgres-ssl-expiry.sh
```

**Test it:**
```bash
./check-postgres-ssl-expiry.sh
```

### Method 2: Cron Job for Email Alerts

Add to your crontab (`crontab -e`):

```cron
# Check PostgreSQL SSL certificate expiration weekly (Mondays at 9 AM)
0 9 * * 1 /home/henninb/check-postgres-ssl-expiry.sh || echo "PostgreSQL SSL cert expires soon!" | mail -s "PostgreSQL SSL Alert" your-email@example.com

# Check daily starting 30 days before expiry
0 9 * * * /home/henninb/check-postgres-ssl-expiry.sh
```

### Method 3: Monitoring with InfluxDB/Grafana

Add certificate monitoring to your existing monitoring stack:

```bash
#!/bin/bash
# Add to your metrics collection script

CERT_FILE="/home/henninb/postgresql-data/server.crt"
EXPIRY_EPOCH=$(date -d "$(sudo openssl x509 -in "$CERT_FILE" -noout -enddate | cut -d= -f2)" +%s)
CURRENT_EPOCH=$(date +%s)
DAYS_UNTIL_EXPIRY=$(( ($EXPIRY_EPOCH - $CURRENT_EPOCH) / 86400 ))

# Send to InfluxDB
curl -X POST "http://influxdb-server:8086/api/v2/write?org=finance-org&bucket=metrics" \
  --header "Authorization: Token ${INFLUXDB_TOKEN}" \
  --data-raw "ssl_cert,service=postgresql days_until_expiry=$DAYS_UNTIL_EXPIRY"
```

## Certificate Renewal Process

### Automatic Renewal Script

Create `/home/henninb/renew-postgres-ssl.sh`:

```bash
#!/bin/bash
# PostgreSQL SSL Certificate Renewal Script

set -e

echo "[$(date)] Starting PostgreSQL SSL certificate renewal"

# Backup old certificates
BACKUP_DIR="/home/henninb/postgresql-ssl-backups"
mkdir -p "$BACKUP_DIR"
BACKUP_TIMESTAMP=$(date +%Y%m%d-%H%M%S)

if [ -f /home/henninb/postgresql-data/server.crt ]; then
    sudo cp /home/henninb/postgresql-data/server.crt "$BACKUP_DIR/server.crt.$BACKUP_TIMESTAMP"
    sudo cp /home/henninb/postgresql-data/server.key "$BACKUP_DIR/server.key.$BACKUP_TIMESTAMP"
    echo "Old certificates backed up to $BACKUP_DIR"
fi

# Generate new certificates
echo "Generating new SSL certificates..."
openssl req -new -x509 -days 365 -nodes -text \
  -out /tmp/server.crt \
  -keyout /tmp/server.key \
  -subj "/CN=postgresql-server"

# Copy to PostgreSQL data directory
sudo cp /tmp/server.crt /home/henninb/postgresql-data/server.crt
sudo cp /tmp/server.key /home/henninb/postgresql-data/server.key

# Set correct permissions
sudo chown 999:1000 /home/henninb/postgresql-data/server.crt
sudo chown 999:1000 /home/henninb/postgresql-data/server.key
sudo chmod 644 /home/henninb/postgresql-data/server.crt
sudo chmod 600 /home/henninb/postgresql-data/server.key

# Restart PostgreSQL
echo "Restarting PostgreSQL to apply new certificates..."
ssh debian-dockerserver "docker restart postgresql-server"

# Wait for PostgreSQL to start
sleep 10

# Verify SSL is working
if PGPASSWORD=monday1 psql "host=192.168.10.10 dbname=finance_db user=henninb sslmode=require" -c "SELECT 'SSL renewal successful' AS status;" > /dev/null 2>&1; then
    echo "[$(date)] Certificate renewal completed successfully"

    # Verify new expiration
    NEW_EXPIRY=$(sudo openssl x509 -in /home/henninb/postgresql-data/server.crt -noout -enddate)
    echo "New certificate: $NEW_EXPIRY"

    # Clean up temp files
    rm -f /tmp/server.crt /tmp/server.key

    exit 0
else
    echo "[$(date)] ERROR: Certificate renewal failed - PostgreSQL not accepting SSL connections"
    echo "Restoring backup certificates..."

    sudo cp "$BACKUP_DIR/server.crt.$BACKUP_TIMESTAMP" /home/henninb/postgresql-data/server.crt
    sudo cp "$BACKUP_DIR/server.key.$BACKUP_TIMESTAMP" /home/henninb/postgresql-data/server.key
    ssh debian-dockerserver "docker restart postgresql-server"

    exit 1
fi
```

**Make it executable:**
```bash
chmod +x /home/henninb/renew-postgres-ssl.sh
```

### Scheduled Automatic Renewal

Add to crontab for automatic renewal 30 days before expiration:

```cron
# Renew PostgreSQL SSL certificate on September 11, 2026 (30 days before expiry)
0 2 11 9 * /home/henninb/renew-postgres-ssl.sh >> /var/log/postgres-ssl-renewal.log 2>&1
```

## Manual Renewal Process

If you prefer to renew manually:

```bash
# 1. Generate new certificate
openssl req -new -x509 -days 365 -nodes -text \
  -out /tmp/server.crt \
  -keyout /tmp/server.key \
  -subj "/CN=postgresql-server"

# 2. Copy to PostgreSQL data directory (on debian-dockerserver)
ssh debian-dockerserver "sudo cp /tmp/server.crt /home/henninb/postgresql-data/server.crt && \
  sudo cp /tmp/server.key /home/henninb/postgresql-data/server.key && \
  sudo chown 999:1000 /home/henninb/postgresql-data/server.* && \
  sudo chmod 644 /home/henninb/postgresql-data/server.crt && \
  sudo chmod 600 /home/henninb/postgresql-data/server.key"

# 3. Restart PostgreSQL
ssh debian-dockerserver "docker restart postgresql-server"

# 4. Verify SSL is working
PGPASSWORD=monday1 psql "host=192.168.10.10 dbname=finance_db user=henninb sslmode=require" -c "SELECT version();"
```

## Notification Options

### Option 1: Email Alerts

Configure your system to send email when certificate is expiring:

```bash
# Install mail utilities if not present
sudo apt-get install mailutils

# Test email
echo "Test email from PostgreSQL SSL monitor" | mail -s "Test" your-email@example.com
```

### Option 2: Slack/Discord Webhook

Add to monitoring script:

```bash
# Send to Slack
WEBHOOK_URL="https://hooks.slack.com/services/YOUR/WEBHOOK/URL"
curl -X POST -H 'Content-type: application/json' \
  --data "{\"text\":\"PostgreSQL SSL certificate expires in $DAYS_UNTIL_EXPIRY days!\"}" \
  $WEBHOOK_URL
```

### Option 3: Grafana Dashboard Alert

Create a Grafana alert rule:
- **Metric**: `ssl_cert{service="postgresql"}.days_until_expiry`
- **Condition**: `< 30` (days)
- **Alert via**: Email, Slack, PagerDuty, etc.

## Certificate Validity Check Commands

**Quick check from any machine:**
```bash
# Check expiration
ssh debian-dockerserver "sudo openssl x509 -in /home/henninb/postgresql-data/server.crt -noout -dates"

# Check if SSL is enabled in PostgreSQL
ssh debian-dockerserver "docker exec postgresql-server psql -U postgres -c 'SHOW ssl;'"

# Test SSL connection
PGPASSWORD=monday1 psql "host=192.168.10.10 dbname=finance_db user=henninb sslmode=require" -c "SELECT ssl_is_used();"
```

## Troubleshooting

### Certificate Expired

**Symptoms:**
- Application can't connect to database
- Error: "SSL certificate verification failed"

**Solution:**
```bash
# Renew immediately
/home/henninb/renew-postgres-ssl.sh

# Or temporarily disable SSL requirement while you fix it
# Update env.prod: sslmode=require -> sslmode=prefer
```

### Certificate Warning Not Received

**Check monitoring:**
```bash
# Test monitoring script
/home/henninb/check-postgres-ssl-expiry.sh

# Check cron is running
sudo systemctl status cron

# Check cron logs
grep postgres /var/log/syslog
```

## Best Practices

1. **Set calendar reminders** for 60 days before expiration
2. **Test renewal process** annually
3. **Keep backups** of old certificates for 90 days
4. **Document changes** when renewing
5. **Monitor expiration** via automated alerts
6. **Consider longer validity** (730 days) for internal certs:
   ```bash
   openssl req -new -x509 -days 730 -nodes ...
   ```

## Integration with Let's Encrypt (Optional Future Enhancement)

If you want to use Let's Encrypt certificates instead of self-signed:

```bash
# Extract from your existing PKCS12
openssl pkcs12 -in bhenning-letsencrypt.p12 -out server.crt -clcerts -nokeys
openssl pkcs12 -in bhenning-letsencrypt.p12 -out server.key -nocerts -nodes

# Let's Encrypt auto-renews every 60 days
# Create symlinks to Let's Encrypt certs
ln -sf /etc/letsencrypt/live/bhenning.com/fullchain.pem /home/henninb/postgresql-data/server.crt
ln -sf /etc/letsencrypt/live/bhenning.com/privkey.pem /home/henninb/postgresql-data/server.key
```

## Summary Checklist

- [ ] Certificate monitoring script created and tested
- [ ] Cron job configured for expiration checks
- [ ] Renewal script created and tested
- [ ] Calendar reminder set for renewal date
- [ ] Backup process documented
- [ ] Team notified of certificate management process
- [ ] Monitoring dashboard updated (if using Grafana)

## Quick Reference

| Task | Command |
|------|---------|
| Check expiration | `ssh debian-dockerserver "sudo openssl x509 -in /home/henninb/postgresql-data/server.crt -noout -dates"` |
| Days until expiry | `/home/henninb/check-postgres-ssl-expiry.sh` |
| Renew certificate | `/home/henninb/renew-postgres-ssl.sh` |
| Test SSL connection | `PGPASSWORD=monday1 psql "host=192.168.10.10 dbname=finance_db user=henninb sslmode=require" -c "SELECT version();"` |
| Verify SSL enabled | `ssh debian-dockerserver "docker exec postgresql-server psql -U postgres -c 'SHOW ssl;'"` |

## Last Updated
2025-10-11 - Initial SSL setup with self-signed certificate (expires 2026-10-11)
