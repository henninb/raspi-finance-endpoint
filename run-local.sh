#!/bin/sh

set -a
source env.secrets
source env.console
set +a

./gradlew clean bootRun

exit 0
