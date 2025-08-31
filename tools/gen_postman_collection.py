#!/usr/bin/env python3
import json

base = {
  "info": {
    "_postman_id": "c2c6b7e2-0bff-4a9c-8f1e-raspi-finance-endpoint",
    "name": "finance-app-localhost",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "variable": [
    {"key": "baseUrl", "value": "http://localhost:8433"},
    {"key": "baseProtocol", "value": "http"},
    {"key": "baseHost", "value": "localhost"},
    {"key": "basePort", "value": "8433"},
    {"key": "username", "value": "henninb"},
    {"key": "password", "value": "monday1"},
    {"key": "token", "value": ""},
    {"key": "accountNameOwner", "value": "chase_brian"},
    {"key": "guid", "value": "00000000-0000-0000-0000-000000000000"},
    {"key": "category_name", "value": "ach"},
    {"key": "description_name", "value": "amazon"},
    {"key": "parameterName", "value": "payment_account"},
    {"key": "receipt_image_id", "value": "1"},
    {"key": "transactionStateValue", "value": "cleared"},
    {"key": "owner", "value": "brian"},
    {"key": "relationship", "value": "SELF"},
    {"key": "medicalExpenseId", "value": "1"},
    {"key": "transactionId", "value": "1"},
    {"key": "accountId", "value": "1"},
    {"key": "providerId", "value": "1"},
    {"key": "familyMemberId", "value": "1"},
    {"key": "startDate", "value": "2024-01-01"},
    {"key": "endDate", "value": "2024-12-31"},
    {"key": "claimStatus", "value": "OPEN"},
    {"key": "year", "value": "2024"},
    {"key": "procedureCode", "value": "99213"},
    {"key": "diagnosisCode", "value": "E11.9"}
  ],
  "event": [
    {
      "listen": "prerequest",
      "script": {
        "type": "text/javascript",
        "exec": [
          "const jar = pm.cookies.jar();",
          "const base = pm.variables.replaceIn('{{baseUrl}}');",
          "const t = pm.collectionVariables.get('token');",
          "if (t) { jar.set(base, 'token', t, function (err) { if (err) console.log('cookie set error', err); }); }"
        ]
      }
    }
  ],
  "item": []
}

def _path_segments(path: str):
    path_only = path.split('?', 1)[0]
    return [p for p in path_only.lstrip('/').split('/') if p]

def _query_params(path: str):
    if '?' not in path:
        return None
    q = path.split('?', 1)[1]
    items = []
    for part in q.split('&'):
        if not part:
            continue
        if '=' in part:
            k, v = part.split('=', 1)
        else:
            k, v = part, ''
        items.append({"key": k, "value": v})
    return items

def _ensure_folder(name: str):
    for f in base["item"]:
        if f.get("name") == name and isinstance(f.get("item"), list):
            return f
    folder = {"name": name, "item": []}
    base["item"].append(folder)
    return folder

def add_get(name, path, allow_404=False, json_check=True, folder: str | None = None):
    if allow_404:
        status_test = "pm.test('Status 2xx or 404', ()=>pm.expect([200,404]).to.include(pm.response.code));"
    else:
        status_test = "pm.test('Status 2xx', ()=>pm.expect(pm.response.code).to.be.within(200,299));"
    tests = [status_test]
    if json_check:
        tests.append("pm.test('JSON when body',()=>{const ct=(pm.response.headers.get('Content-Type')||'').toLowerCase(); if(pm.response.text().length>0) pm.expect(ct).to.include('application/json');});")
    url_obj = {
        "raw": "{{baseUrl}}" + path,
        "protocol": "{{baseProtocol}}",
        "host": ["{{baseHost}}"],
        "port": "{{basePort}}",
        "path": _path_segments(path)
    }
    qp = _query_params(path)
    if qp:
        url_obj["query"] = qp

    target = _ensure_folder(folder) if folder else base
    items = target["item"] if folder else base["item"]
    items.append({
        "name": name,
        "request": {
            "method": "GET",
            "header": [{"key": "Accept", "value": "application/json"}],
            "url": url_obj
        },
        "event": [{
            "listen": "test",
            "script": {"type": "text/javascript", "exec": tests}
        }]
    })

def add_post(name, path, body_raw=None, folder: str | None = None, tests=None):
    url_obj = {
        "raw": "{{baseUrl}}" + path,
        "protocol": "{{baseProtocol}}",
        "host": ["{{baseHost}}"],
        "port": "{{basePort}}",
        "path": _path_segments(path)
    }
    req = {
        "method": "POST",
        "header": [{"key": "Content-Type", "value": "application/json"}],
        "url": url_obj
    }
    if body_raw is not None:
        req["body"] = {"mode": "raw", "raw": body_raw}
    ev = []
    if tests:
        ev = [{"listen": "test", "script": {"type": "text/javascript", "exec": tests}}]
    target = _ensure_folder(folder) if folder else base
    items = target["item"] if folder else base["item"]
    items.append({"name": name, "request": req, "event": ev})

# Auth
add_post(
    "login",
    "/api/login",
    body_raw='{"username":"{{username}}","password":"{{password}}"}',
    folder="Auth",
    tests=[
        "pm.test('Status 200', ()=>pm.expect(pm.response.code).to.eql(200));",
        "const tk = pm.cookies.get('token'); if (tk) { pm.collectionVariables.set('token', tk); }",
        "pm.test('Token cookie present', ()=>pm.expect(pm.cookies.get('token')).to.exist);"
    ]
)
add_get("me", "/api/me", folder="Auth")
add_post(
    "logout",
    "/api/logout",
    body_raw='{}',
    folder="Auth",
    tests=[
        "pm.collectionVariables.unset('token');",
        "pm.test('Status 200/204', ()=>pm.expect([200,204]).to.include(pm.response.code));"
    ]
)

# Account
add_get("totals", "/account/totals", folder="Account")
add_get("payment-required", "/account/payment/required", allow_404=True, json_check=False, folder="Account")
add_get("select-active", "/account/select/active", folder="Account")
add_get("select-by-owner", "/account/select/{{accountNameOwner}}", allow_404=True, folder="Account")

# Category
add_get("select-active", "/category/select/active", folder="Category")
add_get("select-by-name", "/category/select/{{category_name}}", allow_404=True, folder="Category")

# Description
add_get("select-active", "/description/select/active", folder="Description")
add_get("select-by-name", "/description/select/{{description_name}}", allow_404=True, folder="Description")

# Parameter
add_get("select-active", "/parameter/select/active", folder="Parameter")
add_get("select-by-name", "/parameter/select/{{parameterName}}", allow_404=True, folder="Parameter")

# Payment
add_get("select", "/payment/select", folder="Payment")
# Transfer
add_get("select", "/transfer/select", folder="Transfer")
# Pending
add_get("all", "/pending/transaction/all", allow_404=True, folder="Pending Transactions")

# Receipt Image
add_get("select-by-id", "/receipt/image/select/{{receipt_image_id}}", allow_404=True, folder="Receipt Image")
# Validation Amount
add_get("select", "/validation/amount/select/{{accountNameOwner}}/{{transactionStateValue}}", allow_404=True, folder="Validation Amount")

# Transaction
add_get("by-account-owner", "/transaction/account/select/{{accountNameOwner}}", allow_404=True, folder="Transaction")
add_get("account-totals-by-owner", "/transaction/account/totals/{{accountNameOwner}}", allow_404=True, folder="Transaction")
add_get("select-by-guid", "/transaction/select/{{guid}}", allow_404=True, folder="Transaction")
add_get("by-category", "/transaction/category/{{category_name}}", allow_404=True, folder="Transaction")
add_get("by-description", "/transaction/description/{{description_name}}", allow_404=True, folder="Transaction")

# Family Members
add_get("all", "/family-members", folder="Family Members")
add_get("by-id", "/family-members/{{familyMemberId}}", allow_404=True, folder="Family Members")
add_get("by-owner", "/family-members/owner/{{owner}}", folder="Family Members")
add_get("by-owner-relationship", "/family-members/owner/{{owner}}/relationship/{{relationship}}", folder="Family Members")

# Medical Expenses
add_get("all", "/medical-expenses", folder="Medical Expenses")
add_get("by-id", "/medical-expenses/{{medicalExpenseId}}", allow_404=True, folder="Medical Expenses")
add_get("by-transaction-id", "/medical-expenses/transaction/{{transactionId}}", allow_404=True, folder="Medical Expenses")
add_get("by-account-id", "/medical-expenses/account/{{accountId}}", folder="Medical Expenses")
add_get("by-account-date-range", "/medical-expenses/account/{{accountId}}/date-range?startDate={{startDate}}&endDate={{endDate}}", folder="Medical Expenses")
add_get("by-provider-id", "/medical-expenses/provider/{{providerId}}", folder="Medical Expenses")
add_get("by-family-member-id", "/medical-expenses/family-member/{{familyMemberId}}", folder="Medical Expenses")
add_get("by-family-member-date-range", "/medical-expenses/family-member/{{familyMemberId}}/date-range?startDate={{startDate}}&endDate={{endDate}}", folder="Medical Expenses")
add_get("by-claim-status", "/medical-expenses/claim-status/{{claimStatus}}", folder="Medical Expenses")
add_get("out-of-network", "/medical-expenses/out-of-network", folder="Medical Expenses")
add_get("outstanding-balances", "/medical-expenses/outstanding-balances", folder="Medical Expenses")
add_get("open-claims", "/medical-expenses/open-claims", folder="Medical Expenses")
add_get("totals-by-year", "/medical-expenses/totals/year/{{year}}", folder="Medical Expenses")
add_get("claim-status-counts", "/medical-expenses/claim-status-counts", folder="Medical Expenses")
add_get("by-procedure-code", "/medical-expenses/procedure-code/{{procedureCode}}", folder="Medical Expenses")
add_get("by-diagnosis-code", "/medical-expenses/diagnosis-code/{{diagnosisCode}}", folder="Medical Expenses")
add_get("by-date-range", "/medical-expenses/date-range?startDate={{startDate}}&endDate={{endDate}}", folder="Medical Expenses")

with open('finance-app.postman_collection.json', 'w') as f:
    json.dump(base, f, indent=2)

print('Wrote finance-app.postman_collection.json with', len(base['item']), 'requests')
