#!/bin/bash

# fix-docker-permissions.sh
# Fixes host directory permissions for non-root Docker containers
# This script must be run before starting Docker containers to ensure
# mounted volumes have correct permissions for the non-root user

set -euo pipefail

echo "🔧 Fixing Docker host directory permissions..."

# Get current user info (these should match the Docker build args)
CURRENT_UID=${CURRENT_UID:-$(id -u)}
CURRENT_GID=${CURRENT_GID:-$(id -g)}
USERNAME=${USERNAME:-$(whoami)}

echo "📋 Using UID: $CURRENT_UID, GID: $CURRENT_GID, Username: $USERNAME"

# Function to create and fix directory permissions
fix_directory_permissions() {
    local dir="$1"
    local description="$2"

    echo "📁 $description: $dir"

    # Create directory if it doesn't exist
    if [ ! -d "$dir" ]; then
        echo "   ✅ Creating directory: $dir"
        mkdir -p "$dir"
    else
        echo "   ✅ Directory exists: $dir"
    fi

    # Create archive subdirectory for logs
    if [[ "$dir" == *"logs"* ]]; then
        mkdir -p "$dir/archive"
        echo "   ✅ Created archive subdirectory: $dir/archive"
    fi

    # Fix ownership
    echo "   🔧 Setting ownership to $CURRENT_UID:$CURRENT_GID"
    chown -R "$CURRENT_UID:$CURRENT_GID" "$dir"

    # Set appropriate permissions
    echo "   🔧 Setting permissions to 755"
    chmod -R 755 "$dir"

    # Verify permissions
    local actual_owner=$(stat -c "%u:%g" "$dir")
    local actual_perms=$(stat -c "%a" "$dir")

    if [ "$actual_owner" = "$CURRENT_UID:$CURRENT_GID" ] && [ "$actual_perms" = "755" ]; then
        echo "   ✅ Permissions verified: $actual_owner (owner) $actual_perms (permissions)"
    else
        echo "   ❌ Permission verification failed: $actual_owner (owner) $actual_perms (permissions)"
        exit 1
    fi
}

# Fix all directories that will be mounted as volumes
echo ""
echo "🚀 Fixing volume mount directories..."

# Logs directory (mounted in docker-compose-base.yml)
fix_directory_permissions "./logs" "Application logs directory"

# JSON input directory (mounted in docker-compose-base.yml)
fix_directory_permissions "./json_in" "JSON input directory"

# SSL directory (mounted in docker-compose-base.yml) - this should be read-only, but needs proper permissions
if [ -d "./ssl" ]; then
    fix_directory_permissions "./ssl" "SSL certificates directory"
fi

# PostgreSQL data directory (if using local bind mount)
if [ -d "./postgresql-data-secure" ] || [ -d "./postgresql-data" ]; then
    for pg_dir in "./postgresql-data-secure" "./postgresql-data"; do
        if [ -d "$pg_dir" ]; then
            fix_directory_permissions "$pg_dir" "PostgreSQL data directory"
        fi
    done
fi

# InfluxDB data directory (if using local bind mount)
if [ -d "./influxdb-data-secure" ] || [ -d "./influxdb-data" ]; then
    for influx_dir in "./influxdb-data-secure" "./influxdb-data"; do
        if [ -d "$influx_dir" ]; then
            fix_directory_permissions "$influx_dir" "InfluxDB data directory"
        fi
    done
fi

echo ""
echo "✅ All directory permissions have been fixed!"
echo ""
echo "📋 Summary:"
echo "   - Logs directory: ./logs (UID: $CURRENT_UID, GID: $CURRENT_GID, Permissions: 755)"
echo "   - JSON input: ./json_in (UID: $CURRENT_UID, GID: $CURRENT_GID, Permissions: 755)"
echo "   - All subdirectories created and configured"
echo ""
echo "🐳 You can now safely start your Docker containers:"
echo "   docker compose -f docker-compose-secure.yml up -d"
echo ""
echo "🔍 To verify the fix worked, check container logs after startup:"
echo "   docker compose -f docker-compose-secure.yml logs raspi-finance-endpoint"