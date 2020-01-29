#!/bin/sh

touch env.console
touch env.secrets

set -a
. ./env.secrets
. ./env.console
set +a

./gradlew clean bootRun

exit 0
