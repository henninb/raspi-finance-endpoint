#!/bin/sh

mkdir -p logs ssl json_in
HOST_BASEDIR=$(pwd)
GUEST_BASEDIR=/opt/raspi_finance_endpoint
#HOST_IP=$(ipconfig getifaddr en0)
HOST_IP=$(hostname -I | awk '{print $1}')
export LOGS=$BASEDIR/logs
touch env
echo ./mvnw package -Dmaven.test.skip=true
./gradlew clean build
docker build -t raspi_finance_endpoint .
docker run -it -h raspi_finance_endpoint --add-host hornsup:$HOST_IP -p 8083:8080 --env-file env -v $HOST_BASEDIR/logs:$GUEST_BASEDIR/logs -v $HOST_BASEDIR/ssl:$GUEST_BASEDIR/ssl -v $HOST_BASEDIR/json_in:$GUEST_BASEDIR/json_in --rm --name raspi_finance_endpoint raspi_finance_endpoint

exit 0
