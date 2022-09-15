#!/usr/bin/env bash

rm -rf env.bootrun
sed "s/\/opt\/raspi-finance-endpoint/./g" env.prod > env.bootrun

sed -i "s/SSL_ENABLED=false/SSL_ENABLED=true/g" env.bootrun

set -a
# shellcheck disable=SC1091
source env.bootrun
# shellcheck disable=SC1091
source env.secrets
set +a

# ./gradlew clean build bootRun -x test
java -jar build/libs/raspi-finance-endpoint.jar

exit 0
