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

rm -rf env.flyway-repair
sed "s/\/opt\/raspi-finance-endpoint/./g;s/postgresql-server/192.168.10.10/g" env.prod > env.flyway-repair

set -a
# shellcheck disable=SC1091
source env.flyway-repair
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