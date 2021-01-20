#!/usr/bin/env bash

rm -rf env.bootrun
sed "s/\/opt\/raspi-finance-endpoint/./g" env.prod > env.bootrun
sed -i "s/INFLUXDB_ENABLED=true/INFLUXDB_ENABLED=false/g" env.bootrun

set -a
# shellcheck disable=SC1091
source env.bootrun
# shellcheck disable=SC1091
source env.secrets
set +a

./gradlew clean build bootRun -x test

exit 0
