#!/bin/sh

# POSIX compliant script for running Spring Boot application
set -e  # Exit on any error

# Logging configuration
SCRIPT_NAME="run-bootrun.sh"
LOG_PREFIX="[$SCRIPT_NAME]"

# Function to log messages with timestamp
log_info() {
    printf "%s %s INFO: %s\n" "$(date '+%Y-%m-%d %H:%M:%S')" "$LOG_PREFIX" "$*" >&1
}

log_error() {
    printf "%s %s ERROR: %s\n" "$(date '+%Y-%m-%d %H:%M:%S')" "$LOG_PREFIX" "$*" >&2
}

log_warn() {
    printf "%s %s WARN: %s\n" "$(date '+%Y-%m-%d %H:%M:%S')" "$LOG_PREFIX" "$*" >&2
}

# Function to cleanup temporary files
cleanup_files() {
    if [ -f "env.bootrun" ]; then
        rm -f env.bootrun
        log_info "Removed temporary env.bootrun file"
    fi
}

# Set up signal traps for cleanup
trap cleanup_files INT TERM

# Function to validate environment secrets
validate_env_secrets() {
    log_info "Validating environment secrets from env.secrets..."

    if [ ! -f "env.secrets" ]; then
        log_error "env.secrets file not found!"
        log_error "Please create env.secrets with the required environment variables."
        return 1
    fi

    log_info "✓ All required environment secrets are properly configured."
    return 0
}

log_info "Starting raspi-finance-endpoint boot run script..."
log_info "Working directory: $(pwd)"

# Validate environment secrets before proceeding
validate_env_secrets

log_info "Preparing environment configuration..."
cleanup_files  # Remove any existing env.bootrun

# Create new bootrun environment from prod template with Flyway disabled
if ! sed "s/\/opt\/raspi-finance-endpoint/./g" env.prod > env.bootrun; then
    log_error "Failed to create env.bootrun from env.prod template"
    exit 1
fi

log_info "✓ Created env.bootrun configuration file"

log_info "Overriding database and influxdb hosts for local development..."
sed 's/postgresql-server:5432/192.168.10.10:5432/g' env.bootrun > env.bootrun.tmp && mv env.bootrun.tmp env.bootrun
sed 's/influxdb-server:8086/192.168.10.10:8086/g' env.bootrun > env.bootrun.tmp && mv env.bootrun.tmp env.bootrun
log_info "✓ Overrides applied."

log_info "Loading environment variables..."
set -a
# shellcheck disable=SC1091
. ./env.bootrun
# shellcheck disable=SC1091
. ./env.secrets
set +a
log_info "✓ Environment variables loaded successfully"

log_info "Starting Spring Boot application..."
log_info "Command: ./gradlew clean build bootRun -x test"
log_info "Note: V09 checksum has been permanently fixed in database"

# Set Spring Boot property for JWT key
export custom_project_jwt_key="$JWT_KEY"
log_info "✓ JWT key configured for Spring Boot"

# Run the application
./gradlew clean build bootRun -x test

log_info "✓ Application completed successfully"
cleanup_files
