#!/usr/bin/env bash

env=$1
APP=raspi-finance-endpoint

if [ $# -ne 1 ]; then
  echo "Usage: $0 <prod|stage|prodora>"
  exit 1
fi

if [ "$env" = "prod" ] || [ "$env" = "stage" ] || [ "$env" = "prodora" ]; then
  echo "${env}"
else
  echo "Usage: $0 <prod|stage|prodora>"
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
  export JAVA_HOME=/opt/openjdk-bin-11
  export PATH=$JAVA_HOME/bin:$PATH
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
rm -rf docker-compose.yml

if [ ! -f "ssl/hornsup-raspi-finance-keystore.jks" ]; then
# cp -v "$HOME/ssl/${SERVERNAME}-${APP}-keystore.jks" "$HOME/projects/${APP}-endpoint/ssl"
  echo "need to install the certs, cert-install.sh"
  exit 1
fi

# git will not pick up changes to oracle config
git update-index --assume-unchanged src/main/kotlin/finance/configurations/OracleConfig.kt
git update-index --assume-unchanged env.secrets
# undo
# git update-index --no-assume-unchanged src/main/kotlin/finance/configurations/OracleConfig.kt

chmod +x gradle/wrapper/gradle-wrapper.jar

if [ -x "$(command -v ctags)" ]; then
  git ls-files | ctags --links=no --languages=groovy,kotlin -L-
fi

if [ "$env" = "prod" ]; then
  if ! ./gradlew clean build; then
    echo "gradle build failed."
    exit 1
  fi
else
  if ! ./gradlew clean build -x test; then
    echo "gradle build failed."
    exit 1
  fi
fi

docker rmi -f $(docker images -q -f dangling=true) 2> /dev/null
docker volume prune -f

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

ORACLE_CONTAINER=$(docker ps -a -f 'name=oracle-database-server' --format "{{.ID}}") 2> /dev/null
if [ -n "${ORACLE_CONTAINER}" ]; then
  echo docker rm -f "${ORACLE_CONTAINER}"
  docker rm -f "${ORACLE_CONTAINER}" 2> /dev/null
fi

# echo look to use the COMPOSE_FILE=docker-compose.yml:./optional/docker-compose.prod.yml
if [ -x "$(command -v docker-compose)" ]; then

  if ! docker-compose -f docker-compose-base.yml -f "docker-compose-${env}.yml" config > docker-compose.yml; then
    echo "docker-compose config failed."
    exit 1
  fi

  if ! docker-compose build; then
    echo "docker-compose build failed."
    exit 1
  fi

  if ! docker-compose up; then
    echo "docker-compose up failed."
    exit 1
  fi
  # rm docker-compose-run.yml
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
