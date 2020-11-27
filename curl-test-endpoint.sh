#!/bin/sh

uuid=$(uuidgen)

cat > /tmp/transaction-insert <<EOF
{
  "accountId": 0,
  "accountType": "credit",
  "transactionDate": "2020-09-04",
  "guid": "$(uuidgen)",
  "accountNameOwner": "test_brian",
  "description": "aliexpress.com",
  "category": "online",
  "amount": 0,
  "transactionState": "cleared",
  "reoccurring": false,
  "notes": "my note to you",
  "activeStatus": "true"
}
EOF

cat > /tmp/account-insert <<EOF
{
  "accountNameOwner": "test_brian",
  "accountType": "credit",
  "moniker": "0000",
  "totals": 0,
  "totalsBalanced": 0,
  "dateClosed": 0,
  "activeStatus": true
}
EOF

cat /tmp/transaction-insert
cat /tmp/account-insert

exit 0


echo account
echo
curl -k --header "Content-Type: application/json" https://localhost:8080/account/insert -X POST -d '{"accountNameOwner":"test_brian", "accountType":"credit", "moniker":"0000", "totals":0, "totalsBalanced":0, "dateClosed":0, "activeStatus":true}'

echo
echo payment
echo
curl -k --header "Content-Type: application/json" https://localhost:8080/payment/insert -X POST -d '{"accountNameOwner": "test_brian", "amount":0.00, "transactionDate":1593981072000 }'

echo
echo transaction
echo
curl -k --header "Content-Type: application/json" https://localhost:8080/transaction/insert -X POST -d '{"accountId":0, "accountType":"credit", "transactionDate":"2020-09-04", "guid":"$(uuidgen)", "accountNameOwner":"test_brian","description":"aliexpress.com", "category":"online","amount":0.00,"transactionState":"cleared","reoccurring":false, "notes":"my note to you", "activeStatus":"true"}'

exit 0
