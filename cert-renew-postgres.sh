#!/bin/bash
# PostgreSQL SSL Certificate Renewal Script

set -e

DRY_RUN=false
[[ "${1:-}" == "--dry-run" ]] && DRY_RUN=true

run() {
    if $DRY_RUN; then
        echo "[DRY-RUN] $*"
    else
        "$@"
    fi
}

REMOTE_HOST="debian-dockerserver"
PG_DATA_DIR="/home/henninb/postgresql/18/docker"
DB_HOST="192.168.10.10"
DB_NAME="finance_db"
DB_USER="henninb"
DB_PASSWORD="${PGPASSWORD:-monday1}"

echo "[$(date)] Starting PostgreSQL SSL certificate renewal"
$DRY_RUN && echo "[DRY-RUN] Dry-run mode enabled — no changes will be made"

# Check if we can reach the remote host
if ! ssh "$REMOTE_HOST" "echo 'Connection OK'" > /dev/null 2>&1; then
    echo "ERROR: Cannot connect to $REMOTE_HOST"
    exit 1
fi

# Report current certificate status
BACKUP_DIR="/home/henninb/postgresql-ssl-backups"
BACKUP_TIMESTAMP=$(date +%Y%m%d-%H%M%S)
BACKUP_EXISTS=false

echo ""
if ssh "$REMOTE_HOST" "podman unshare -- test -f $PG_DATA_DIR/server.crt" 2>/dev/null; then
    CERT_DATES=$(ssh "$REMOTE_HOST" "podman unshare -- openssl x509 -in $PG_DATA_DIR/server.crt -noout -dates")
    NOT_BEFORE=$(echo "$CERT_DATES" | grep notBefore | cut -d= -f2)
    NOT_AFTER=$(echo "$CERT_DATES"  | grep notAfter  | cut -d= -f2)
    ISSUED_EPOCH=$(date -d "$NOT_BEFORE" +%s)
    EXPIRY_EPOCH=$(date -d "$NOT_AFTER"  +%s)
    NOW_EPOCH=$(date +%s)
    AGE_DAYS=$(( (NOW_EPOCH - ISSUED_EPOCH) / 86400 ))
    DAYS_LEFT=$(( (EXPIRY_EPOCH - NOW_EPOCH) / 86400 ))
    echo "Current certificate:"
    echo "  Issued : $NOT_BEFORE ($AGE_DAYS days ago)"
    echo "  Expires: $NOT_AFTER ($DAYS_LEFT days remaining)"
else
    echo "No existing certificate found at $PG_DATA_DIR"
fi
echo ""

# Confirm before proceeding
if ! $DRY_RUN; then
    read -r -p "Proceed with certificate renewal? [y/N] " CONFIRM
    [[ "$CONFIRM" =~ ^[Yy]$ ]] || { echo "Aborted."; exit 0; }
    echo ""
fi

# Create backup directory
run mkdir -p "$BACKUP_DIR"

# Backup existing certificates from remote host
echo "Backing up existing certificates..."
if ssh "$REMOTE_HOST" "podman unshare -- test -f $PG_DATA_DIR/server.crt"; then
    BACKUP_EXISTS=true
    run ssh "$REMOTE_HOST" "podman unshare -- sh -c 'cp $PG_DATA_DIR/server.crt /tmp/server.crt.$BACKUP_TIMESTAMP && cp $PG_DATA_DIR/server.key /tmp/server.key.$BACKUP_TIMESTAMP && chown 0:0 /tmp/server.crt.$BACKUP_TIMESTAMP /tmp/server.key.$BACKUP_TIMESTAMP && chmod 644 /tmp/server.crt.$BACKUP_TIMESTAMP /tmp/server.key.$BACKUP_TIMESTAMP'"
    run scp "$REMOTE_HOST:/tmp/server.crt.$BACKUP_TIMESTAMP" "$BACKUP_DIR/"
    run scp "$REMOTE_HOST:/tmp/server.key.$BACKUP_TIMESTAMP" "$BACKUP_DIR/"
    echo "Old certificates backed up to $BACKUP_DIR"
else
    echo "No existing certificates to back up"
fi

# Generate new certificates locally
echo "Generating new SSL certificates..."
run openssl req -new -x509 -days 365 -nodes -text \
  -out /tmp/server.crt \
  -keyout /tmp/server.key \
  -subj "/CN=postgresql-server"

if ! $DRY_RUN && { [ ! -f /tmp/server.crt ] || [ ! -f /tmp/server.key ]; }; then
    echo "ERROR: Failed to generate certificates"
    exit 1
fi

# Copy new certificates to remote host
echo "Installing new certificates on $REMOTE_HOST..."
run scp /tmp/server.crt "$REMOTE_HOST:/tmp/server.crt"
run scp /tmp/server.key "$REMOTE_HOST:/tmp/server.key"

run ssh "$REMOTE_HOST" "podman unshare -- sh -c 'cp /tmp/server.crt $PG_DATA_DIR/server.crt && \
  cp /tmp/server.key $PG_DATA_DIR/server.key && \
  chown 999:999 $PG_DATA_DIR/server.crt $PG_DATA_DIR/server.key && \
  chmod 644 $PG_DATA_DIR/server.crt && \
  chmod 600 $PG_DATA_DIR/server.key'"

# Restart PostgreSQL via systemd (quadlet-managed)
echo "Restarting PostgreSQL to apply new certificates..."
run ssh "$REMOTE_HOST" "systemctl --user restart postgresql-server"

# Wait for PostgreSQL to start
echo "Waiting for PostgreSQL to start..."
$DRY_RUN || sleep 10

# Verify SSL is working
echo "Verifying SSL connectivity..."
if $DRY_RUN || PGPASSWORD="$DB_PASSWORD" psql "host=$DB_HOST dbname=$DB_NAME user=$DB_USER sslmode=require" -c "SELECT 'SSL renewal successful' AS status;" > /dev/null 2>&1; then
    echo "[$(date)] Certificate renewal completed successfully"

    # Show new expiration
    if ! $DRY_RUN; then
        NEW_EXPIRY=$(ssh "$REMOTE_HOST" "podman unshare -- openssl x509 -in $PG_DATA_DIR/server.crt -noout -enddate")
        echo "New certificate: $NEW_EXPIRY"
    fi

    # Clean up temp files
    run rm -f /tmp/server.crt /tmp/server.key
    run ssh "$REMOTE_HOST" "rm -f /tmp/server.crt /tmp/server.key /tmp/server.*.${BACKUP_TIMESTAMP}"

    echo ""
    echo "Certificate renewal complete!"
    $BACKUP_EXISTS && echo "Backup stored in: $BACKUP_DIR/server.crt.$BACKUP_TIMESTAMP"

    exit 0
else
    echo "[$(date)] ERROR: Certificate renewal failed - PostgreSQL not accepting SSL connections"

    if ! $BACKUP_EXISTS; then
        echo "No backup available to restore. Please investigate the issue manually."
        exit 1
    fi

    echo "Restoring backup certificates..."

    run scp "$BACKUP_DIR/server.crt.$BACKUP_TIMESTAMP" "$REMOTE_HOST:/tmp/server.crt.restore"
    run scp "$BACKUP_DIR/server.key.$BACKUP_TIMESTAMP" "$REMOTE_HOST:/tmp/server.key.restore"

    run ssh "$REMOTE_HOST" "podman unshare -- sh -c 'cp /tmp/server.crt.restore $PG_DATA_DIR/server.crt && \
      cp /tmp/server.key.restore $PG_DATA_DIR/server.key && \
      chown 999:999 $PG_DATA_DIR/server.crt $PG_DATA_DIR/server.key && \
      chmod 644 $PG_DATA_DIR/server.crt && \
      chmod 600 $PG_DATA_DIR/server.key'"

    run ssh "$REMOTE_HOST" "systemctl --user restart postgresql-server"

    echo "Backup certificates restored. Please investigate the issue."
    exit 1
fi
