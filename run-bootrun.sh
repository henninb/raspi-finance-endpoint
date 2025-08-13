#!/usr/bin/env bash

# Function to validate environment secrets
validate_env_secrets() {
  local env_file="env.secrets"
  local missing_keys=()
  local required_keys=(
    "DATASOURCE_PASSWORD"
    "INFLUXDB_ADMIN_PASSWORD"
    "SSL_KEY_PASSWORD"
    "SSL_KEY_STORE_PASSWORD"
    "SYS_PASSWORD"
    "BASIC_AUTH_PASSWORD"
    "JWT_KEY"
  )

  echo "Validating environment secrets..."

  # Check if env.secrets file exists
  if [ ! -f "$env_file" ]; then
    echo "ERROR: $env_file file not found!"
    echo "Please create $env_file with the required environment variables."
    exit 1
  fi

  # Source the env.secrets file to check values
  set -a
  # shellcheck disable=SC1090
  source "$env_file"
  set +a

  # Check each required key
  for key in "${required_keys[@]}"; do
    # Use indirect variable expansion to get the value
    value="${!key}"
    if [ -z "$value" ] || [ "$value" = "" ]; then
      missing_keys+=("$key")
    fi
  done

  # If any keys are missing, prompt user and exit
  if [ ${#missing_keys[@]} -gt 0 ]; then
    echo "ERROR: The following required environment variables are missing or empty in $env_file:"
    for key in "${missing_keys[@]}"; do
      echo "  - $key"
    done
    echo ""
    echo "Please set values for these variables in $env_file before running the application."
    echo "Example format:"
    echo "  DATASOURCE_PASSWORD=your_database_password"
    echo "  JWT_KEY=your_jwt_secret_key"
    echo ""
    exit 1
  fi

  echo "✓ All required environment secrets are properly configured."
}

# Validate environment secrets before proceeding
validate_env_secrets

rm -rf env.bootrun
sed "s/\/opt\/raspi-finance-endpoint/./g" env.prod > env.bootrun
# sed -i "s/INFLUXDB_ENABLED=true/INFLUXDB_ENABLED=false/g" env.bootrun

set -a
# shellcheck disable=SC1091
source env.bootrun
# shellcheck disable=SC1091
source env.secrets
set +a

./gradlew clean build bootRun -x test

exit 0
