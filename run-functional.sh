#!/usr/bin/env bash

echo psql finance_test_db -U henninb -h localhost -p 5432
echo execute run-create-finance_test_db.sh localhost 5432
set -a
# shellcheck disable=SC1091
# source env.bootrun
# shellcheck disable=SC1091
# source env.secrets
set +a

./gradlew clean build functionalTest

exit 0
