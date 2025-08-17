#!/usr/bin/env bash

# Function to validate environment secrets
validate_env_secrets() {
  local env_file="env.secrets"
  local missing_keys=()
  local required_keys=(
    "DATASOURCE_PASSWORD"
  )

  echo "Validating environment secrets for Flyway repair..."

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
    echo "Please set values for these variables in $env_file before running Flyway repair."
    echo "Example format:"
    echo "  DATASOURCE_PASSWORD=your_database_password"
    echo ""
    exit 1
  fi

  echo "✓ All required environment secrets for Flyway repair are properly configured."
}

# Validate environment secrets before proceeding
validate_env_secrets

rm -rf env.flyway-repair
sed "s/\/opt\/raspi-finance-endpoint/./g" env.prod > env.flyway-repair

set -a
# shellcheck disable=SC1091
source env.flyway-repair
# shellcheck disable=SC1091
source env.secrets
set +a

echo "Running Flyway repair to fix schema history..."
echo "Database URL: $DATASOURCE"
echo "Database User: $DATASOURCE_USERNAME"
echo "Flyway Enabled: $FLYWAY_ENABLED"
echo ""
echo "This will mark the missing V03 migration as deleted in the schema history."
echo "After repair, you can run ./run-flyway.sh to apply the V04 migration."
echo ""

./gradlew flywayRepair --info

exit 0