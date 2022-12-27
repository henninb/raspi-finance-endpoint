#!/bin/sh
date=$(date '+%Y-%m-%d')

docker save -o raspi-finance-endpoint-${date}.tar raspi-finance-endpoint
docker save -o raspi-finance-react-${date}.tar raspi-finance-react

echo docker load -i raspi-finance-endpoint-${date}.tar
echo docker load -i raspi-finance-react-${date}.tar

scp raspi-finance-endpoint-${date}.tar henninb@192.168.10.10:/home/henninb
scp raspi-finance-react-${date}.tar henninb@192.168.10.10:/home/henninb

echo docker run -dit --restart unless-stopped  --add-host raspi:192.168.10.25 -p 8443:8443 --env-file env.secrets --env-file env.prod --name raspi-finance-endpoint -h raspi-finance-endpoint raspi-finance-endpoint
echo docker run -dit --restart unless-stopped -p 3000:443 --name raspi-finance-react -h raspi-finance-react raspi-finance-react

exit 0
