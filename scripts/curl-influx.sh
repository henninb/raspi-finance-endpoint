#!/bin/sh

server=192.168.100.124
# curl -i -X POST "http://${server}:8086/query" -u "henninb:monday1" --data-urlencode "q=CREATE DATABASE metrics"

curl -s -G "http://${server}:8086/query?pretty=true" --data-urlencode "db=metrics" -u "henninb:monday1" --data-urlencode "q=SELECT * FROM method_timed" > metrics-method_timed.json

curl -s -G "http://${server}:8086/query?pretty=true" --data-urlencode "db=metrics" -u "henninb:monday1" --data-urlencode "q=SHOW series ON metrics" > metrics-series.json

curl -s -G "http://${server}:8086/query?pretty=true" --data-urlencode "db=metrics" -u "henninb:monday1" --data-urlencode "q=SELECT value from metrics" > metrics-values.json

curl -s -G "http://${server}:8086/query?pretty=true" --data-urlencode "db=metrics" -u "henninb:monday1" --data-urlencode "q=SHOW measurements ON metrics" > metrics-measurements.json

curl -s -G "http://${server}:8086/query?pretty=true" --data-urlencode "db=metrics" -u "henninb:monday1" --data-urlencode "q=select * from transaction_insert_timed" > transactions-inserted.json

# drop series from TRANSACTION_LIST_IS_EMPTY;
# drop series from TRANSACTION_RECEIPT_IMAGE;
# drop series from TRANSACTION_RECEIVED_EVENT_COUNTER;
# drop series from TRANSACTION_SUCCESSFULLY_INSERTED_COUNTER;
# drop series from TRANSACTION_UPDATE_CLEARED_COUNTER;
# drop series from exception_counter;
# select * from method_timed;
# select mean from method_timed where mean > 0;

exit 0
