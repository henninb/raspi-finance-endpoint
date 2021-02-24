#!/bin/sh

if [ ! -x "$(command -v uuidgen)" ]; then
  echo "install uuidgen for your system."
  exit 1
fi

cat > /tmp/transaction-insert <<EOF
{
  "accountType": "credit",
  "transactionDate": "2020-09-04",
  "guid": "$(uuidgen)",
  "accountNameOwner": "foo_brian",
  "description": "newegg.com",
  "category": "online",
  "amount": 0,
  "transactionState": "cleared",
  "reoccurring": false,
  "notes": "",
  "activeStatus": "true"
}
EOF

cat > /tmp/transaction-insert-debit <<EOF
{
  "accountType": "debit",
  "transactionDate": "2020-09-04",
  "guid": "$(uuidgen)",
  "accountNameOwner": "bank_brian",
  "description": "newegg.com",
  "category": "online",
  "amount": 0,
  "transactionState": "cleared",
  "reoccurring": false,
  "notes": "",
  "activeStatus": "true"
}
EOF

cat > /tmp/account-insert <<EOF
{
  "accountNameOwner": "test_brian",
  "accountType": "credit",
  "moniker": "0000",
  "activeStatus": true
}
EOF

echo
echo payment required
curl -k 'https://hornsup:8080/account/payment/required'
echo
echo payment required
curl -k 'https://hornsup:8080/transaction/payment/required' | jq

echo
echo account
echo
curl -k --header "Content-Type: application/json" 'https://localhost:8080/account/insert' -X POST -d '{"accountNameOwner":"bank_brian", "accountType":"debit", "moniker":"0000", "activeStatus":true, "totals":0, "totalsBalanced":0, "dateClosed":0}'
echo
curl -k --header "Content-Type: application/json" 'https://localhost:8080/account/insert' -X POST -d '{"accountNameOwner":"chase_brian", "accountType":"credit", "moniker":"0000", "activeStatus":true, "totals":0, "totalsBalanced":0, "dateClosed":0}'

echo
echo parameter setup
curl -k --header "Content-Type: application/json" -X POST -d '{"parameterName":"payment_account","parameterValue":"bank_brian", "activeStatus":true}' 'https://localhost:8080/parm/insert'
curl -k --header "Content-Type: application/json" -X POST -d '{"parameterName":"timezone","parameterValue":"America/Chicago", "activeStatus":true}' 'https://localhost:8080/parm/insert'

exit 0
# echo these break the code
echo account
curl -k --header "Content-Type: application/json" 'https://localhost:8080/account/insert' -X POST -d '{"accountNameOwner":"test_brian", "accountType":"credit", "moniker":"0000", "activeStatus":true}'


echo
echo transaction
echo
curl -k --header "Content-Type: application/json" 'https://localhost:8080/transaction/insert' -X POST --data-binary @/tmp/transaction-insert

echo
echo payment
echo
curl -k --header "Content-Type: application/json" 'https://localhost:8080/payment/insert' -X POST -d '{"accountNameOwner": "test_brian", "amount":0.00, "transactionDate":1593981072000 }'

echo
echo transaction
echo
curl -k --header "Content-Type: application/json" 'https://localhost:8080/transaction/insert' -X POST -d '{"accountId":0, "accountType":"credit", "transactionDate":"2020-09-04", "guid":"$(uuidgen)", "accountNameOwner":"test_brian","description":"aliexpress.com", "category":"online","amount":0.00,"transactionState":"cleared","reoccurring":false, "notes":"my note to you", "activeStatus":"true"}'


echo
echo transaction
curl -k --header "Content-Type: application/json" 'https://localhost:8080/transaction/insert' -X POST -d '{ "accountId":1001, "accountType":"credit", "transactionDate":"2020-10-01", "dueDate":"2020-10-07", "guid":"$(uuidgen)", "accountNameOwner":"amazon-store_brian", "description":"aliexpress.com", "category":"online", "amount":0.00, "transactionState":"cleared", "reoccurring":false, "reoccurringType":"undefined", "notes":"my note to you" }'

exit 0
