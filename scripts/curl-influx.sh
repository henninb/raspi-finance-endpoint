#!/bin/sh

server=192.168.100.124
# curl -i -X POST "http://${server}:8086/query" -u "henninb:monday1" --data-urlencode "q=CREATE DATABASE metrics"


curl -s -G "http://${server}:8086/query?pretty=true" --data-urlencode "db=metrics" -u "henninb:monday1" --data-urlencode "q=SELECT value FROM stuff" > stuff-values.json

curl -s -G "http://${server}:8086/query?pretty=true" --data-urlencode "db=metrics" -u "henninb:monday1" --data-urlencode "q=SHOW SERIES ON metrics" > metrics-series.json

curl -s -G "http://${server}:8086/query?pretty=true" --data-urlencode "db=metrics" -u "henninb:monday1" --data-urlencode "q=SELECT value from metrics" > metrics-values.json

curl -s -G "http://${server}:8086/query?pretty=true" --data-urlencode "db=metrics" -u "henninb:monday1" --data-urlencode "q=SHOW measurements on metrics" > metrics-measurements.json

exit 0
