#!/usr/bin/env bash

if [ "$OSTYPE" = "linux-gnu" ]; then
  #export JAVA_HOME=$(dirname $(dirname $(readlink $(readlink $(which javac)))))
  export JAVA_HOME=$(dirname $(dirname $(readlink $(readlink $(which javac)) || readlink $(which javac))))
else
  # macos
  export JAVA_HOME=$(/usr/libexec/java_home)
  #export JAVA_HOME=/Library/Java/JavaVirtualMachines/openjdk-11.0.2.jdk/Contents/Home
fi

echo $JAVA_HOME

export PATH=${JAVA_HOME}/bin:${PATH}

if [ $# -ne 1 ]; then
  echo "Usage: $0 <prod or local>"
  exit 1
fi
ENV=$1

APP=raspi-finance-endpoint
TIMEZONE='America/Chicago'
USERNAME=henninb
HOST_BASEDIR=$(pwd)
GUEST_BASEDIR=/opt/${APP}

if [ $ENV = "prod" ]; then
  echo prod
elif [ $ENV = "local" ]; then
  echo local
else
  echo "Usage: $0 <prod or local>"
  exit 2
fi

if [ \( "$OS" = "Linux Mint" \) -o \(  "$OS" = "Ubuntu" \) ]; then
  HOST_IP=$(hostname -I | awk '{print $1}')
elif [ "$OS" = "Arch Linux" ]; then
  HOST_IP=$(hostname -i | awk '{print $1}')
  HOST_IP=192.168.100.208
elif [ "$OS" = "Fedora" ]; then
  HOST_IP=192.168.100.130
elif [ "$OS" = "Darwin" ]; then
  HOST_IP=$(ipconfig getifaddr en0)
elif [ "$OS" = "Gentoo" ]; then
  HOST_IP=$(hostname -i | awk '{print $1}')
else
  echo $OS is not yet implemented.
  exit 1
fi

mkdir -p src/main/java
mkdir -p src/main/scala
mkdir -p src/main/kotlin
mkdir -p src/test/unit/groovy
mkdir -p src/test/unit/java
mkdir -p src/test/integration/groovy
mkdir -p src/test/integration/java
mkdir -p src/test/functional/groovy
mkdir -p src/test/functional/java
mkdir -p src/test/performance/groovy
mkdir -p src/test/performance/java
mkdir -p logs
mkdir -p ssl
touch env.secrets

./gradlew clean build
if [ $? -ne 0 ]; then
  echo "gradle build failed."
  exit 1
fi

if [ -x "$(command -v docker)" ]; then
  docker build -t $APP --build-arg TIMEZONE=${TIMEZONE} --build-arg APP=${APP} --build-arg USERNAME=${USERNAME} .
  if [ $? -ne 0 ]; then
    echo "docker build -t $APP --build-arg TIMEZONE=${TIMEZONE} --build-arg APP=${APP} --build-arg USERNAME=${USERNAME} ."
    echo "docker build failed."
    exit 1
  fi

  docker run -it -h ${APP} --add-host hornsup:$HOST_IP -p 8081:8080 --env-file env.${ENV} --env-file env.secrets -v $HOST_BASEDIR/logs:$GUEST_BASEDIR/logs -v $HOST_BASEDIR/ssl:$GUEST_BASEDIR/ssl --rm --name ${APP} ${APP}
else
  set -a
  . /env.secrets
  . ./env.console
  set +a
  ./gradlew clean build bootRun
fi

exit 0
