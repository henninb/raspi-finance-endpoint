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

if [ "$OS" = "FreeBSD" ]; then
  HOST_IP="192.168.10.114"
elif [ "$OS" = "Darwin" ]; then
  HOST_IP=$(ipconfig getifaddr en0)
else
  HOST_IP=$(ip route get 1 | awk '{print $7}' | head -1)
fi

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

#if [ -z "${in_hosts}" ]; then
#  echo "The 'hornsup' hostname needs to be added to /etc/hosts."
#  exit 2
#fi

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

if [ -x "$(command -v docker)" ]; then
  # echo podman build --tag "$APPNAME" -f ./Dockerfile-podman
  echo docker run --rm -it --volume "$(pwd)/nginx.conf:/etc/nginx/conf.d/default.conf" nginx:1.21.5-alpinej
  # podman build --tag "$APPNAME" -f ./Dockerfile

  docker rmi -f "$(docker images -q -f dangling=true)" 2> /dev/null
  docker volume prune -f 2> /dev/null
  docker rmi -f raspi-finance-endpoint

  nginx_container=$(docker ps -a -f 'name=nginx-server' --format "{{.ID}}") 2> /dev/null
  if [ -n "${nginx_container}" ]; then
    docker stop "${nginx_container}"
    docker rm -f "${nginx_container}" 2> /dev/null
    # docker rmi -f nginx-server
  fi

  varnish_container=$(docker ps -a -f 'name=varnish-server' --format "{{.ID}}") 2> /dev/null
  if [ -n "${varnish_container}" ]; then
    docker stop "${varnish_container}"
    docker rm -f "${varnish_container}" 2> /dev/null
    # docker rmi -f varnish-server
  fi

  raspi_container=$(docker ps -a -f 'name=raspi-finance-endpoint' --format "{{.ID}}") 2> /dev/null
  if [ -n "${raspi_container}" ]; then
    docker stop "${raspi_container}"
    docker rm -f "${raspi_container}" 2> /dev/null
    docker rmi -f raspi-finance-endpoint
  fi

  # echo podman-compose -f docker-compose-base.yml -f "docker-compose-${env}.yml" -f docker-compose-varnish.yml up -d
  if ! docker compose -f docker-compose-base.yml -f "docker-compose-${env}.yml" -f docker-compose-varnish.yml up -d; then
    echo "docker compose up failed."
    if [ -x "$(command -v docker-compose)" ]; then
      docker-compose -f docker-compose-base.yml -f "docker-compose-${env}.yml" -f docker-compose-varnish.yml up -d
    else
      echo "docker-compose up failed"
      exit 1
    fi
  else
    echo "docker-compose up failed"
    exit 1
  fi
else
  echo "Install docker"
fi

echo docker logs raspi-finance-endpoint --follow

exit 0
