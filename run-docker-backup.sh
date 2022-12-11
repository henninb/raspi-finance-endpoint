#!/bin/sh
date=$(date '+%Y-%m-%d')

docker save -o raspi-finance-endpoint-${date}.tar raspi-finance-endpoint
docker save -o raspi-finance-react-${date}.tar raspi-finance-react

echo docker load -i raspi-finance-endpoint-${date}.tar
echo docker load -i raspi-finance-react-${date}.tar

exit 0
