#!/bin/bash
# PostgreSQL SSL Certificate Expiry Monitor

REMOTE_HOST="debian-dockerserver"
CONTAINER="postgresql-server"
CERT_FILE="/var/lib/postgresql/18/docker/server.crt"
WARN_DAYS=30
CRIT_DAYS=7

# Check certificate inside the Podman container on the remote host
CERT_INFO=$(ssh "$REMOTE_HOST" "podman exec $CONTAINER openssl x509 -in $CERT_FILE -noout -enddate 2>&1")

if [ $? -ne 0 ]; then
    echo "ERROR: Cannot read certificate from $CONTAINER:$CERT_FILE on $REMOTE_HOST"
    echo "$CERT_INFO"
    exit 2
fi

# Get expiration date
EXPIRY_DATE=$(echo "$CERT_INFO" | cut -d= -f2)
EXPIRY_EPOCH=$(date -d "$EXPIRY_DATE" +%s)
CURRENT_EPOCH=$(date +%s)
DAYS_UNTIL_EXPIRY=$(( ($EXPIRY_EPOCH - $CURRENT_EPOCH) / 86400 ))

echo "PostgreSQL SSL Certificate Status:"
echo "  Container: $REMOTE_HOST/$CONTAINER"
echo "  Certificate: $CERT_FILE"
echo "  Expires: $EXPIRY_DATE"
echo "  Days until expiry: $DAYS_UNTIL_EXPIRY"

if [ $DAYS_UNTIL_EXPIRY -le $CRIT_DAYS ]; then
    echo "  Status: CRITICAL - Certificate expires in $DAYS_UNTIL_EXPIRY days!"
    echo ""
    echo "Action required: Run ./renew-postgres-ssl.sh to renew the certificate"
    exit 2
elif [ $DAYS_UNTIL_EXPIRY -le $WARN_DAYS ]; then
    echo "  Status: WARNING - Certificate expires in $DAYS_UNTIL_EXPIRY days"
    echo ""
    echo "Recommended: Plan certificate renewal soon"
    exit 1
else
    echo "  Status: OK"
    exit 0
fi
