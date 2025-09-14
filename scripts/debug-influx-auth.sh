#!/bin/bash

# Load environment variables from env.influx
if [ -f "$(dirname "$0")/../env.influx" ]; then
    echo "Loading environment variables from env.influx..."
    source "$(dirname "$0")/../env.influx"
else
    echo "Error: env.influx file not found."
    exit 1
fi

# Check for required variables
if [ -z "$INFLUXDB_URL" ] || [ -z "$INFLUXDB_TOKEN" ]; then
    echo "Error: INFLUXDB_URL or INFLUXDB_TOKEN is not set in env.influx."
    exit 1
fi

# Replace hostname with IP for host-based testing
export INFLUXDB_URL_HOST=${INFLUXDB_URL//influxdb-server/192.168.10.10}

echo "--- InfluxDB 2.x Authentication Debugger ---"
echo "Original InfluxDB URL (for containers): $INFLUXDB_URL"
echo "Using InfluxDB URL (for host): $INFLUXDB_URL_HOST"
echo "InfluxDB Org: $INFLUXDB_ORG"
echo "InfluxDB Bucket: $INFLUXDB_BUCKET"
echo "-------------------------------------------"

# 1. Check network connectivity and server health
echo
echo "Step 1: Pinging the InfluxDB server..."
if curl -s "$INFLUXDB_URL_HOST/health" | grep -q '"status":"pass"'; then
    echo "Success: InfluxDB server is reachable and healthy."
else
    echo "Failure: Could not connect to InfluxDB at $INFLUXDB_URL_HOST."
    echo "Please check the following:"
    echo "  - Is the InfluxDB container running? (docker ps)"
    echo "  - Is the IP address 192.168.10.10 correct and reachable from this machine?"
    echo "  - Is the port correct? (default is 8086)"
    echo "  - Are there any firewalls blocking the connection?"
    exit 1
fi

# 2. Verify the authentication token
echo
echo "Step 2: Verifying the authentication token..."
HTTP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" -H "Authorization: Token $INFLUXDB_TOKEN" "$INFLUXDB_URL_HOST/api/v2/buckets")

if [ "$HTTP_STATUS" -eq 200 ]; then
    echo "Success: Authentication token is valid. The application should be able to connect."
    echo "The error might be in the application's configuration. Check your application.yml to ensure it's using the token."
elif [ "$HTTP_STATUS" -eq 401 ]; then
    echo "Failure: Authentication failed (HTTP 401 Unauthorized)."
    echo "The INFLUXDB_TOKEN in your env.influx file is incorrect or lacks permissions."
    echo "Please verify the token is correct and has the necessary read/write permissions for the '$INFLUXDB_BUCKET' bucket."
else
    echo "Warning: Received an unexpected HTTP status code: $HTTP_STATUS."
    echo "This might indicate a server-side issue or a misconfiguration."
fi

echo
echo "Debug script finished."