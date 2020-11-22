#!/usr/bin/env bash

ENV=$1
APP=raspi-finance-convert

if [ $# -ne 1 ]; then
  echo "Usage: $0 <prod|local|stage|perf>"
  exit 1
fi

if [ "$ENV" = "prod" ] || [ "$ENV" = "local" ] || [ "$ENV" = "stage" ] || [ "$ENV" = "perf" ]; then
  echo "${ENV}"
else
  echo "Usage: $0 <prod|local|stage|perf>"
  exit 2
fi

# "$OSTYPE" == "darwin"*
if [ "$OS" = "Linux Mint" ] || [ "$OS" = "Ubuntu" ] || [ "$OS" = "Raspbian GNU/Linux" ]; then
  HOST_IP=$(hostname -I | awk '{print $1}')
elif [ "$OS" = "Arch Linux" ]; then
  HOST_IP=192.168.100.207
elif [ "$OS" = "openSUSE Tumbleweed" ]; then
  HOST_IP=192.168.100.193
elif [ "$OS" = "Solus" ]; then
  HOST_IP=192.168.100.118
elif [ "$OS" = "Fedora" ]; then
  HOST_IP=192.168.100.130
elif [ "$OS" = "Darwin" ]; then
  HOST_IP=$(ipconfig getifaddr en0)
  # echo "lsof -nP | grep LISTEN"
elif [ "$OS" = "void" ]; then
  HOST_IP=127.0.0.1
elif [ "$OS" = "Gentoo" ]; then
  HOST_IP=$(hostname -i | awk '{print $1}')
else
  echo "$OS is not yet implemented."
  exit 1
fi

export HOST_IP
export CURRENT_UID="$(id -u)"
export CURRENT_GID="$(id -g)"

mkdir -p 'src/main/scala'
mkdir -p 'src/main/java'
mkdir -p 'src/main/kotlin'
mkdir -p 'src/test/unit/groovy'
mkdir -p 'src/test/unit/java'
mkdir -p 'src/test/integration/groovy'
mkdir -p 'src/test/integration/java'
mkdir -p 'src/test/functional/groovy'
mkdir -p 'src/test/functional/java'
mkdir -p 'src/test/performance/groovy'
mkdir -p 'src/test/performance/java'
mkdir -p 'database-data'
mkdir -p 'logs'
mkdir -p 'ssl'
mkdir -p 'excel_in'

#git ls-files | ctags --language=java
#find . -name "*.java" | xargs ctags --language=java

touch env.secrets

chmod +x gradle/wrapper/gradle-wrapper.jar

INFLUX_CONTAINER=$(docker ps -a -f 'name=influxdb' --format "{{.ID}}") 2> /dev/null

./gradlew clean build -x test

if [ -n "${INFLUX_CONTAINER}" ]; then
  echo docker rm -f "${INFLUX_CONTAINER}"
  docker rm -f "${INFLUX_CONTAINER}"  2> /dev/null
fi

echo docker run -it -h influxdb-server --net=raspi-bridge -p 8086:8086 --rm --name influxdb-server -d influxdb
echo curl -i -X POST http://localhost:8086/query -u "henninb:monday1" --data-urlencode "q=CREATE DATABASE metrics"

echo curl -G 'http://localhost:8086/query?pretty=true' --data-urlencode "db=metrics" -u "henninb:monday1" --data-urlencode "q=SELECT \"value\" FROM \"stuff\""

echo curl -G 'http://localhost:8086/query?pretty=true' --data-urlencode "db=metrics" -u "henninb:monday1" --data-urlencode "q=SHOW SERIES ON metrics"

echo curl -G 'http://localhost:8086/query?pretty=true' --data-urlencode "db=metrics" -u "henninb:monday1" --data-urlencode "q=SHOW measurements on metrics"

if [ -x "$(command -v docker-compose)" ]; then
  if ! docker-compose -f docker-compose.yml -f "docker-compose-${ENV}.yml" config > docker-compose-run.yml; then
    echo "docker-compose config failed."
    exit 1
  fi

  if ! docker-compose -f docker-compose-run.yml build; then
    echo "docker-compose build failed."
    exit 1
  fi

  if ! docker-compose -f docker-compose-run.yml up; then
    echo "docker-compose up failed."
    exit 1
  fi
  rm docker-compose-run.yml
else
  set -a
  # shellcheck disable=SC1091
  source env.prod
  # shellcheck disable=SC1091
  source env.secrets
  set +a

  ./gradlew clean build bootRun
fi

exit 0
