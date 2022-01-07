#!/usr/bin/env sh

env=$1
test_flag=$2
datastore=$3
in_hosts="$(grep -n hornsup /etc/hosts | cut -f1 -d:)"
APPNAME=raspi-finance-endpoint
CURRENT_UID="$(id -u)"
CURRENT_GID="$(id -g)"

if [ $# -ne 1 ] && [ $# -ne 2 ] && [ $# -ne 3 ]; then
  echo "Usage: $0 <prod|stage|prodora> [test_flag] [datastore]"
  exit 1
fi

if [ "$env" = "prod" ] || [ "$env" = "stage" ] || [ "$env" = "prodora" ]; then
  echo "${env}"
else
  echo "Usage: $0 <prod|stage|prodora>"
  exit 2
fi

if [ -z "${test_flag}" ]; then
  test_flag=false
fi

if [ -z "${datastore}" ]; then
  datastore=postgresql
fi

if [ "$env" = "prodora" ]; then
  datastore=oracle
fi

if [ ! -x "$(command -v ./os-env)" ]; then
  echo "./os-env is need to set the environment variable OS."
  exit 3
fi

. ./os-env

if [ "$OS" = "Linux Mint" ] || [ "$OS" = "Ubuntu" ] || [ "$OS" = "Debian GNU/Linux" ]; then
  sudo apt install -y iproute2
  HOST_IP=$(ip route get 1.2.3.4 | awk '{print $7}')
elif [ "$OS" = "Arch Linux" ] || [ "$OS" = "ArcoLinux" ]; then
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

export APPNAME
export HOST_IP
export CURRENT_UID
export CURRENT_GID

mkdir -p 'src/main/kotlin'
mkdir -p 'src/test/unit/groovy'
mkdir -p 'src/test/integration/groovy'
mkdir -p 'src/test/functional/groovy'
mkdir -p 'src/test/performance/groovy'
# mkdir -p 'postgresql-data'
mkdir -p 'influxdb-data'
mkdir -p 'grafana-data'
mkdir -p 'logs'
mkdir -p 'ssl'
mkdir -p 'excel_in'
#rm -rf docker-compose.yml

if [ -z "${in_hosts}" ]; then
  echo "The 'hornsup' hostname needs to be added to /etc/hosts."
  exit 2
fi

# preserve local secret changes
git update-index --assume-unchanged env.secrets

chmod +x gradle/wrapper/gradle-wrapper.jar

# if [ -x "$(command -v ctags)" ]; then
#   git ls-files | ctags --links=no --languages=groovy,kotlin -L-
# fi

if [ "${test_flag}" = "true" ]; then
  if ! ./gradlew clean build test integrationTest functionalTest; then
    echo "gradle build failed."
    exit 1
  fi
else
  if ! ./gradlew clean build -x test; then
    echo "gradle build failed."
    exit 1
  fi
fi

docker stop raspi-finance-endpoint varnish-server
docker rm -f raspi-finance-endpoint varnish-server
docker rmi -f raspi-finance-endpoint varnish-server

docker rmi -f "$(docker images -q -f dangling=true)" 2> /dev/null
docker volume prune -f 2> /dev/null

INFLUX_CONTAINER=$(docker ps -a -f 'name=influxdb-server' --format "{{.ID}}") 2> /dev/null
if [ -n "${INFLUX_CONTAINER}" ]; then
  echo docker rm -f "${INFLUX_CONTAINER}"
  docker rm -f "${INFLUX_CONTAINER}" 2> /dev/null
fi

# POSTGRESQL_CONTAINER=$(docker ps -a -f 'name=postgresql-server' --format "{{.ID}}") 2> /dev/null
# if [ -n "${POSTGRESQL_CONTAINER}" ]; then
#   echo docker rm -f "${POSTGRESQL_CONTAINER}"
#   docker rm -f "${POSTGRESQL_CONTAINER}" 2> /dev/null
# fi

ORACLE_CONTAINER=$(docker ps -a -f 'name=oracle-database-server' --format "{{.ID}}") 2> /dev/null
if [ -n "${ORACLE_CONTAINER}" ]; then
  echo docker rm -f "${ORACLE_CONTAINER}"
  docker rm -f "${ORACLE_CONTAINER}" 2> /dev/null
fi

# echo look to use the COMPOSE_FILE=docker-compose.yml:./optional/docker-compose.prod.yml
if [ -x "$(command -v docker-compose)" ]; then

  # if ! docker-compose -f docker-compose-base.yml -f docker-compose-${datastore}.yml -f "docker-compose-${env}.yml" build; then
  #   echo "docker-compose build failed."
  #   exit 1
  # fi

  # exit 0

  # if ! docker-compose -f docker-compose-base.yml -f "docker-compose-${datastore}.yml" -f "docker-compose-${env}.yml" -f docker-compose-varnish.yml -f docker-compose-elk.yml up -d; then
  if ! docker-compose -f docker-compose-base.yml -f "docker-compose-${env}.yml" -f docker-compose-varnish.yml up -d; then
    echo "docker-compose up failed."
    exit 1
  fi
else
  rm -rf env.bootrun
  sed "s/\/opt\/raspi-finance-endpoint/./g" env.prod > env.bootrun
  set -a
  . ./env.bootrun
  . ./env.secrets
  set +a

  ./gradlew clean build bootRun -x test
fi

exit 0
