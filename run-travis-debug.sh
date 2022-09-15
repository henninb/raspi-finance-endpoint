#!/bin/sh

id=552119510
token=
echo 'gem install travis && travis login && travis token'

echo curl -s -X POST \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -H "Travis-API-Version: 3" \
  -H "Authorization: token ${token}" \
  -d "{\"quiet\": true}" \
  https://api.travis-ci.com/job/${id}/debug


curl -s -X POST \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -H "Travis-API-Version: 3" \
  -H "Authorization: token ${token}" \
  -d "{\"quiet\": true}" \
  https://api.travis-ci.com/job/${id}/debug

exit 0
