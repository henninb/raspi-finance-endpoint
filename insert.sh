#!/bin/sh

#curl --header "Content-Type: application/json" --request POST --data '{"accountNameOwner":"test_brian", "accountType": "credit", "activeStatus": "true","moniker": "0000", "totals": 0.00, "totalsBalanced": 0.00, "dateClosed": 0, "dateUpdated": 0, "dateAdded": 0}' http://localhost:8080/account/insert


curl --header "Content-Type: application/json" http://localhost:8080/transaction/insert -X POST -d '{"accountId":0, "accountType":"credit", "transactionDate":1553645394, "dateUpdated":1593981072000, "dateAdded":1593981072000,"guid":"4ea3be58-3993-46de-88a2-4ffc7f1d73bd", "accountNameOwner":"chase_brian","description":"aliexpress.com", "category":"online","amount":3.14,"cleared":1,"reoccurring":false, "notes":"my note to you", "sha256":"963e35c37ea59f3f6fa35d72fb0ba47e1e1523fae867eeeb7ead64b55ff22b77"}'

curl --header "Content-Type: application/json" http://localhost:8080/payment/insert -X POST -d '{"accountNameOwner": "test_brian", "amount":1.54, "transactionDate":1593981072000 }'
exit 0