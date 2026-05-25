#!/usr/bin/env sh

if command -v rsync; then
  rsync -arvz build/libs/raspi-finance-endpoint.jar hornsup:/home/henninb/projects/github.com/henninb/raspi-finance-endpoint/build/libs/raspi-finance-endpoint.jar
else
  echo "rsync is not installed."
fi
# scp -p build/libs/raspi-finance-endpoint.jar 

exit 0
