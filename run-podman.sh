#!/bin/sh

env=prod
APPNAME=raspi-finance-endpoint
CURRENT_UID="$(id -u)"
CURRENT_GID="$(id -g)"
podman-compose -f docker-compose-base.yml -f "docker-compose-${env}.yml" -f docker-compose-varnish.yml up -d

exit 0
