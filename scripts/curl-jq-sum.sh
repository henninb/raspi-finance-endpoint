#!/bin/sh

# curl -k -X GET 'https://localhost:8443/transaction/account/select/chase_kari' | jq '.[] | .amount' | awk 'BEGIN {sum=0} {for (i = 1; i <= NF; i++) sum+=$i} END {print sum}'

# curl -k -X GET 'https://localhost:8443/transaction/account/select/chase_kari' | jq '.[] | .amount' | awk '{s+=$1} END {print s}'

curl -k -s -X GET 'https://localhost:8443/transaction/account/select/chase_kari' | jq 'map(.amount) | add'
curl -k -s -X GET 'https://localhost:8443/transaction/account/select/chase_brian' | jq 'map(.amount) | add'
curl -k -s -X GET 'https://localhost:8443/transaction/account/select/amazon_brian' | jq 'map(.amount) | add' | awk {' printf "%.2f\n",$1 '}

# curl -k -s -X GET 'https://localhost:8443/transaction/all' | jq 'map(.amount) | add'
curl -k -s -X GET 'https://localhost:8443/transaction/all' | jq '.[] | select(.accountType == "credit")| .amount' | awk '{s+=$1} END {printf "$%.2f\n",s}'
curl -k -s -X GET 'https://localhost:8443/transaction/all' | jq '.[] | select(.accountType == "debit")| .amount' | awk '{s+=$1} END {printf "$%.2f\n",s}'

curl -k -s -X GET 'https://localhost:8443/transaction/page/all?page=1&per_page=1' | jq
curl -k -s -X GET 'https://localhost:8443/transaction/page/all?page=1&per_page=2' | jq

curl -k -s -X GET 'https://localhost:8443/account/select/active' | jq

curl -k -s -X GET 'https://localhost:8443/account/select/active' | jq '.[] | .accountNameOwner' | tr -d '"'

exit 0
