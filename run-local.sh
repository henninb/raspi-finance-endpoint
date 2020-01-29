#!/bin/sh

touch env.console
touch env.secrets

set -a
source env.secrets
source env.console
set +a

./gradlew clean bootRun

exit 0
