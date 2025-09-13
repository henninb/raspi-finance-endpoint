#!/bin/bash

# remote-fix-permissions.sh
# Script to run on the remote Docker server (debian-dockerserver or gcp-api)
# Fixes directory permissions for Docker volume mounts to work with non-root containers
#
# Usage on remote server:
#   ./remote-fix-permissions.sh
#
# Or run from local machine:
#   scp remote-fix-permissions.sh debian-dockerserver:~/
#   ssh debian-dockerserver './remote-fix-permissions.sh'

set -euo pipefail

echo "🔧 [$(hostname)] Fixing Docker volume directory permissions..."

# Get current user info on this remote server
CURRENT_UID=$(id -u)
CURRENT_GID=$(id -g)
USERNAME=$(whoami)

echo "📋 [$(hostname)] Using UID: $CURRENT_UID, GID: $CURRENT_GID, Username: $USERNAME"

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
    sudo chown -R "$CURRENT_UID:$CURRENT_GID" "$dir" || chown -R "$CURRENT_UID:$CURRENT_GID" "$dir"

    # Set appropriate permissions (755 for directories, 644 for files)
    echo "   🔧 Setting permissions"
    find "$dir" -type d -exec chmod 755 {} \;
    find "$dir" -type f -exec chmod 644 {} \;

    # Verify permissions
    local actual_owner=$(stat -c "%u:%g" "$dir")
    local actual_perms=$(stat -c "%a" "$dir")

    if [ "$actual_owner" = "$CURRENT_UID:$CURRENT_GID" ] && [ "$actual_perms" = "755" ]; then
        echo "   ✅ Permissions verified: $actual_owner (owner) $actual_perms (permissions)"
    else
        echo "   ❌ Permission verification failed: $actual_owner (owner) $actual_perms (permissions)"
        echo "   🔍 This might be expected if files already exist with different permissions"
    fi
}

# Find the project directory (usually in home directory)
PROJECT_DIRS=$(find ~ -maxdepth 2 -name "*raspi-finance-endpoint*" -type d 2>/dev/null || true)

if [ -z "$PROJECT_DIRS" ]; then
    echo "❌ Could not find raspi-finance-endpoint project directory"
    echo "🔍 Please run this script from within the raspi-finance-endpoint directory"
    echo "   or specify the project directory:"
    echo "   ./remote-fix-permissions.sh /path/to/raspi-finance-endpoint"
    exit 1
fi

# Use the first found project directory, or the argument if provided
if [ $# -eq 1 ]; then
    PROJECT_DIR="$1"
else
    PROJECT_DIR=$(echo "$PROJECT_DIRS" | head -n1)
fi

if [ ! -d "$PROJECT_DIR" ]; then
    echo "❌ Project directory not found: $PROJECT_DIR"
    exit 1
fi

echo "📁 Using project directory: $PROJECT_DIR"
cd "$PROJECT_DIR"

echo ""
echo "🚀 Fixing volume mount directories..."

# Fix all directories that will be mounted as volumes
# (These paths match the volume mounts in docker-compose-base.yml)

# Logs directory (mounted as: ./logs:/opt/raspi-finance-endpoint/logs:rw)
fix_directory_permissions "./logs" "Application logs directory"

# JSON input directory (mounted as: ./json_in:/opt/raspi-finance-endpoint/json_in:rw)
fix_directory_permissions "./json_in" "JSON input directory"

# SSL directory (mounted as: ./ssl:/opt/raspi-finance-endpoint/ssl:ro)
if [ -d "./ssl" ]; then
    fix_directory_permissions "./ssl" "SSL certificates directory"
else
    echo "📁 SSL directory not found - this is OK if SSL is configured differently"
fi

# PostgreSQL data directories (if using local bind mount)
for pg_dir in "./postgresql-data-secure" "./postgresql-data" "./postgresql-data-prod"; do
    if [ -d "$pg_dir" ]; then
        fix_directory_permissions "$pg_dir" "PostgreSQL data directory"
    fi
done

# InfluxDB data directories (if using local bind mount)
for influx_dir in "./influxdb-data-secure" "./influxdb-data" "./influxdb-data-prod"; do
    if [ -d "$influx_dir" ]; then
        fix_directory_permissions "$influx_dir" "InfluxDB data directory"
    fi
done

echo ""
echo "✅ All directory permissions have been fixed on $(hostname)!"
echo ""
echo "📋 Summary:"
echo "   - Server: $(hostname)"
echo "   - Project: $PROJECT_DIR"
echo "   - User: $USERNAME (UID: $CURRENT_UID, GID: $CURRENT_GID)"
echo "   - Logs directory: ./logs (writable)"
echo "   - JSON input: ./json_in (writable)"
echo "   - All subdirectories created and configured"
echo ""
echo "🐳 You can now safely start your Docker containers:"
echo "   cd $PROJECT_DIR"
echo "   docker compose -f docker-compose-secure.yml up -d"
echo ""
echo "🔍 To verify the fix worked, check container logs after startup:"
echo "   docker compose -f docker-compose-secure.yml logs raspi-finance-endpoint"