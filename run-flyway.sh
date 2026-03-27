#!/usr/bin/env bash

# Load secrets from gopass
if ! command -v gopass >/dev/null 2>&1; then
  echo "ERROR: gopass is not installed."
  exit 1
fi
echo "Loading secrets from gopass..."
DATASOURCE_PASSWORD=$(gopass show -o raspi-finance-endpoint/postgresql)
export DATASOURCE_PASSWORD
echo "✓ Secrets loaded from gopass"

rm -rf env.flyway
sed "s/\/opt\/raspi-finance-endpoint/./g" env.prod > env.flyway

set -a
# shellcheck disable=SC1091
source env.flyway
set +a

echo "Running Flyway migration with prod profile..."
echo "Database URL: $DATASOURCE"
echo "Database User: $DATASOURCE_USERNAME"
echo "Flyway Enabled: $FLYWAY_ENABLED"

./gradlew flywayMigrate --info

exit 0