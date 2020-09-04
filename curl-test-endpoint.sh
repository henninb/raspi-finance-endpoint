#!/bin/sh

echo account
echo
curl --header "Content-Type: application/json" http://localhost:8080/account/insert -X POST -d '{"accountNameOwner":"test_brian", "accountType":"credit", "moniker":"0000", "totals":0, "totalsBalanced":0, "dateClosed":0, "dateAdded":1593981072000, "dateUpdated":1593981072000,"activeStatus":true}'

echo
echo payment
echo
curl --header "Content-Type: application/json" http://localhost:8080/payment/insert -X POST -d '{"accountNameOwner": "test_brian", "amount":0.00, "transactionDate":1593981072000 }'

echo
echo transaction
echo
curl --header "Content-Type: application/json" http://localhost:8080/transaction/insert -X POST -d '{"accountId":0, "accountType":"credit", "transactionDate":"2020-09-04", "dateUpdated":1593981072000, "dateAdded":1593981072000,"guid":"4ea3be58-3993-46de-88a2-4ffc7f1d73bd", "accountNameOwner":"test_brian","description":"aliexpress.com", "category":"online","amount":0.00,"transactionState":"cleared","reoccurring":false, "notes":"my note to you", "activeStatus":"true"}'


exit 0
