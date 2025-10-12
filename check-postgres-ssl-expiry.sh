#!/bin/bash
# PostgreSQL SSL Certificate Expiry Monitor

REMOTE_HOST="debian-dockerserver"
CERT_FILE="/home/henninb/postgresql-data/server.crt"
WARN_DAYS=30
CRIT_DAYS=7

# Check certificate on remote host
CERT_INFO=$(ssh "$REMOTE_HOST" "sudo openssl x509 -in $CERT_FILE -noout -enddate 2>&1")

if [ $? -ne 0 ]; then
    echo "ERROR: Cannot read certificate from $REMOTE_HOST:$CERT_FILE"
    echo "$CERT_INFO"
    exit 2
fi

# Get expiration date
EXPIRY_DATE=$(echo "$CERT_INFO" | cut -d= -f2)
EXPIRY_EPOCH=$(date -d "$EXPIRY_DATE" +%s)
CURRENT_EPOCH=$(date +%s)
DAYS_UNTIL_EXPIRY=$(( ($EXPIRY_EPOCH - $CURRENT_EPOCH) / 86400 ))

echo "PostgreSQL SSL Certificate Status:"
echo "  Server: $REMOTE_HOST"
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
