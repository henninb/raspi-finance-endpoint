#!/bin/sh

touch env.console
touch env.secrets

set -a
. ./env.secrets
. ./env.console
set +a

./gradlew clean build bootRun

exit 0
