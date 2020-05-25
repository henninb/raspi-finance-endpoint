#!/usr/bin/env bash

if [ "$OSTYPE" = "linux-gnu" ]; then
  #export JAVA_HOME=$(dirname $(dirname $(readlink -f $(readlink -f $(which javac)))))
  export JAVA_HOME=$(dirname $(dirname $(readlink -f $(readlink -f $(which javac)) || readlink -f $(which javac))))
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

if [ "$ENV" = "prod" ]; then
  echo prod
elif [ "$ENV" = "local" ]; then
  echo local
else
  echo "Usage: $0 <prod or local>"
  exit 2
fi

INFLUX_CONTAINER=$(docker ps -a -f 'name=influxdb' --format "{{.ID}}") 2> /dev/null

if [ ! -z "${INFLUX_CONTAINER}" ]; then
  echo docker rm $INFLUX_CONTAINER
  docker rm $INFLUX_CONTAINER  2> /dev/null
fi

# "$OSTYPE" == "darwin"*
if [ "$OS" = "Linux Mint" ] || [ "$OS" = "Ubuntu" ] || [ "$OS" = "Raspbian GNU/Linux" ]; then
  HOST_IP=$(hostname -I | awk '{print $1}')
elif [ "$OS" = "Arch Linux" ]; then
  echo 'ip route list | grep default'
  HOST_IP=192.168.100.207
elif [ "$OS" = "openSUSE Tumbleweed" ]; then
  HOST_IP=192.168.100.193
elif [ "$OS" = "Fedora" ]; then
  HOST_IP=192.168.100.130
elif [ "$OS" = "Darwin" ]; then
  HOST_IP=$(ipconfig getifaddr en0)
elif [ "$OS" = "void" ]; then
  HOST_IP=127.0.0.1
elif [ "$OS" = "Gentoo" ]; then
  HOST_IP=$(hostname -i | awk '{print $1}')
else
  echo "$OS is not yet implemented."
  exit 1
fi

echo "$HOST_IP"

mkdir -p src/main/java
mkdir -p src/main/scala
mkdir -p src/main/kotlin
mkdir -p src/test/unit/groovy
mkdir -p src/test/unit/java
mkdir -p src/test/integration/groovy
mkdir -p src/test/integration/java
mkdir -p src/test/functional/groovy
mkdir -p 'src/test/functional/java'
mkdir -p 'src/test/performance/groovy'
mkdir -p 'src/test/performance/java'
mkdir -p logs
mkdir -p ssl
touch env.secrets

chmod a+x gradle/wrapper/gradle-wrapper.jar

if ! ./gradlew -x test clean build; then
  echo "gradle build failed."
  exit 1
fi

if [ -x "$(command -v docker-compose)" ]; then
  if ! docker-compose -f docker-compose.yml -f "docker-compose-${ENV}.yml" build; then
    echo "docker-compose build failed."
    exit 1
  fi
  if ! docker-compose -f docker-compose.yml -f "docker-compose-${ENV}.yml" up; then
    echo "docker-compose up failed."
    exit 1
  fi
else
  set -a
  . /env.secrets
  . ./env.console
  set +a
  ./gradlew clean build bootRun
fi

exit 0
