#!/usr/bin/env bash

ENV=$1
APP=raspi-finance-endpoint

if [ $# -ne 1 ]; then
  echo "Usage: $0 <prod|func|perf>"
  exit 1
fi

if [ "$ENV" = "prod" ] || [ "$ENV" = "func" ] || [ "$ENV" = "perf" ]; then
  echo "${ENV}"
else
  echo "Usage: $0 <prod|func|perf>"
  exit 2
fi

if [ ! -x "$(command -v ./os-env)" ]; then
  echo "./os-env is need to set the environment variable OS."
  exit 3
fi

./os-env

if [ "$OS" = "Linux Mint" ] || [ "$OS" = "Ubuntu" ] || [ "$OS" = "Raspbian GNU/Linux" ]; then
  HOST_IP=$(ip route get 1.2.3.4 | awk '{print $7}')
elif [ "$OS" = "Arch Linux" ]; then
  HOST_IP=$(ip route get 1.2.3.4 | awk '{print $7}')
elif [ "$OS" = "openSUSE Tumbleweed" ]; then
  HOST_IP=$(ip route get 1.2.3.4 | awk '{print $7}')
elif [ "$OS" = "Solus" ]; then
  HOST_IP=$(ip route get 1.2.3.4 | awk '{print $7}')
elif [ "$OS" = "Fedora" ]; then
  HOST_IP=$(ip route get 1.2.3.4 | awk '{print $7}')
elif [ "$OS" = "Darwin" ]; then
  HOST_IP=$(ipconfig getifaddr en0)
elif [ "$OS" = "void" ]; then
  HOST_IP=$(ip route get 1.2.3.4 | awk '{print $7}')
elif [ "$OS" = "Gentoo" ]; then
  HOST_IP=$(hostname -i | awk '{print $1}')
else
  echo "$OS is not yet implemented."
  exit 1
fi

export HOST_IP
export CURRENT_UID="$(id -u)"
export CURRENT_GID="$(id -g)"

mkdir -p 'src/main/kotlin'
mkdir -p 'src/test/unit/groovy'
mkdir -p 'src/test/integration/groovy'
mkdir -p 'src/test/functional/groovy'
mkdir -p 'src/test/performance/groovy'
mkdir -p 'postgresql-data'
mkdir -p 'influxdb-data'
mkdir -p 'grafana-data'
mkdir -p 'logs'
mkdir -p 'ssl'
mkdir -p 'excel_in'

chmod +x gradle/wrapper/gradle-wrapper.jar

if [ -x "$(command -v ctags)" ]; then
  git ls-files | ctags --links=no --languages=groovy,kotlin -L-
fi

if [ "$ENV" = "prod" ]; then
  if ! ./gradlew clean build functionalTest; then
    echo "gradle build failed."
    exit 1
  fi
else
  if ! ./gradlew clean build -x test; then
    echo "gradle build failed."
    exit 1
  fi
fi

INFLUX_CONTAINER=$(docker ps -a -f 'name=influxdb-server' --format "{{.ID}}") 2> /dev/null
if [ -n "${INFLUX_CONTAINER}" ]; then
  echo docker rm -f "${INFLUX_CONTAINER}"
  docker rm -f "${INFLUX_CONTAINER}" 2> /dev/null
fi

POSTGRESQL_CONTAINER=$(docker ps -a -f 'name=postgresql-server' --format "{{.ID}}") 2> /dev/null
if [ -n "${POSTGRESQL_CONTAINER}" ]; then
  echo docker rm -f "${POSTGRESQL_CONTAINER}"
  docker rm -f "${POSTGRESQL_CONTAINER}" 2> /dev/null
fi

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
