#!/bin/sh

if [ \( "$OS" = "Linux Mint" \) -o \(  "$OS" = "Ubuntu" \) ]; then
  HOST_IP=$(hostname -I | awk '{print $1}')
elif [ "$OS" = "Arch Linux" ]; then
  HOST_IP=$(hostname -i | awk '{print $1}')
elif [ "$OS" = "Darwin" ]; then
  HOST_IP=$(ipconfig getifaddr en0)
else
  echo $OS is not yet implemented.
  exit 1
fi

APP=raspi-finance-endpoint
mkdir -p logs ssl json_in
HOST_BASEDIR=$(pwd)
GUEST_BASEDIR=/opt/${APP}
LOGS=$BASEDIR/logs
touch env.secrets
./gradlew clean build
docker build -t ${APP} .
docker run -it -h ${APP} --add-host hornsup:$HOST_IP -p 8081:8080 --env-file env.local -v $HOST_BASEDIR/logs:$GUEST_BASEDIR/logs -v $HOST_BASEDIR/ssl:$GUEST_BASEDIR/ssl --rm --name ${APP} ${APP}

exit 0
