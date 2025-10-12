#!/bin/bash
# PostgreSQL SSL Certificate Renewal Script

set -e

REMOTE_HOST="debian-dockerserver"
PG_DATA_DIR="/home/henninb/postgresql-data"
DB_HOST="192.168.10.10"
DB_NAME="finance_db"
DB_USER="henninb"
DB_PASSWORD="${PGPASSWORD:-monday1}"

echo "[$(date)] Starting PostgreSQL SSL certificate renewal"

# Check if we can reach the remote host
if ! ssh "$REMOTE_HOST" "echo 'Connection OK'" > /dev/null 2>&1; then
    echo "ERROR: Cannot connect to $REMOTE_HOST"
    exit 1
fi

# Create backup directory
BACKUP_DIR="/home/henninb/postgresql-ssl-backups"
mkdir -p "$BACKUP_DIR"
BACKUP_TIMESTAMP=$(date +%Y%m%d-%H%M%S)

# Backup existing certificates from remote host
echo "Backing up existing certificates..."
if ssh "$REMOTE_HOST" "[ -f $PG_DATA_DIR/server.crt ]"; then
    ssh "$REMOTE_HOST" "sudo cp $PG_DATA_DIR/server.crt /tmp/server.crt.$BACKUP_TIMESTAMP && sudo chmod 644 /tmp/server.crt.$BACKUP_TIMESTAMP"
    ssh "$REMOTE_HOST" "sudo cp $PG_DATA_DIR/server.key /tmp/server.key.$BACKUP_TIMESTAMP && sudo chmod 644 /tmp/server.key.$BACKUP_TIMESTAMP"
    scp "$REMOTE_HOST:/tmp/server.crt.$BACKUP_TIMESTAMP" "$BACKUP_DIR/"
    scp "$REMOTE_HOST:/tmp/server.key.$BACKUP_TIMESTAMP" "$BACKUP_DIR/"
    echo "Old certificates backed up to $BACKUP_DIR"
fi

# Generate new certificates locally
echo "Generating new SSL certificates..."
openssl req -new -x509 -days 365 -nodes -text \
  -out /tmp/server.crt \
  -keyout /tmp/server.key \
  -subj "/CN=postgresql-server"

if [ ! -f /tmp/server.crt ] || [ ! -f /tmp/server.key ]; then
    echo "ERROR: Failed to generate certificates"
    exit 1
fi

# Copy new certificates to remote host
echo "Installing new certificates on $REMOTE_HOST..."
scp /tmp/server.crt "$REMOTE_HOST:/tmp/server.crt"
scp /tmp/server.key "$REMOTE_HOST:/tmp/server.key"

ssh "$REMOTE_HOST" "sudo cp /tmp/server.crt $PG_DATA_DIR/server.crt && \
  sudo cp /tmp/server.key $PG_DATA_DIR/server.key && \
  sudo chown 999:1000 $PG_DATA_DIR/server.crt $PG_DATA_DIR/server.key && \
  sudo chmod 644 $PG_DATA_DIR/server.crt && \
  sudo chmod 600 $PG_DATA_DIR/server.key"

# Restart PostgreSQL
echo "Restarting PostgreSQL to apply new certificates..."
ssh "$REMOTE_HOST" "docker restart postgresql-server"

# Wait for PostgreSQL to start
echo "Waiting for PostgreSQL to start..."
sleep 10

# Verify SSL is working
echo "Verifying SSL connectivity..."
if PGPASSWORD="$DB_PASSWORD" psql "host=$DB_HOST dbname=$DB_NAME user=$DB_USER sslmode=require" -c "SELECT 'SSL renewal successful' AS status;" > /dev/null 2>&1; then
    echo "[$(date)] Certificate renewal completed successfully"

    # Show new expiration
    NEW_EXPIRY=$(ssh "$REMOTE_HOST" "sudo openssl x509 -in $PG_DATA_DIR/server.crt -noout -enddate")
    echo "New certificate: $NEW_EXPIRY"

    # Clean up temp files
    rm -f /tmp/server.crt /tmp/server.key
    ssh "$REMOTE_HOST" "rm -f /tmp/server.crt /tmp/server.key /tmp/server.*.${BACKUP_TIMESTAMP}"

    echo ""
    echo "Certificate renewal complete!"
    echo "Backup stored in: $BACKUP_DIR/server.crt.$BACKUP_TIMESTAMP"

    exit 0
else
    echo "[$(date)] ERROR: Certificate renewal failed - PostgreSQL not accepting SSL connections"
    echo "Restoring backup certificates..."

    # Restore from backup
    scp "$BACKUP_DIR/server.crt.$BACKUP_TIMESTAMP" "$REMOTE_HOST:/tmp/server.crt.restore"
    scp "$BACKUP_DIR/server.key.$BACKUP_TIMESTAMP" "$REMOTE_HOST:/tmp/server.key.restore"

    ssh "$REMOTE_HOST" "sudo cp /tmp/server.crt.restore $PG_DATA_DIR/server.crt && \
      sudo cp /tmp/server.key.restore $PG_DATA_DIR/server.key && \
      sudo chown 999:1000 $PG_DATA_DIR/server.crt $PG_DATA_DIR/server.key && \
      sudo chmod 644 $PG_DATA_DIR/server.crt && \
      sudo chmod 600 $PG_DATA_DIR/server.key"

    ssh "$REMOTE_HOST" "docker restart postgresql-server"

    echo "Backup certificates restored. Please investigate the issue."
    exit 1
fi
