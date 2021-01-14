#!/bin/sh

# curl -i -X POST 'http://localhost:8086/query' -u "henninb:monday1" --data-urlencode "q=CREATE DATABASE metrics"

curl -s -G 'http://localhost:8086/query?pretty=true' --data-urlencode "db=metrics" -u "henninb:monday1" --data-urlencode "q=SELECT value FROM stuff" > metrics-values.json

curl -s -G 'http://localhost:8086/query?pretty=true' --data-urlencode "db=metrics" -u "henninb:monday1" --data-urlencode "q=SHOW SERIES ON metrics" > metrics-series.json

curl -s -G 'http://localhost:8086/query?pretty=true' --data-urlencode "db=metrics" -u "henninb:monday1" --data-urlencode "q=SHOW measurements on metrics" > metrics-measurements.json
