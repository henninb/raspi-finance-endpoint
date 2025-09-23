#!/usr/bin/env python3
"""
Simple HTTPS smoke tester for the Spring Boot app on localhost:8443.

- Logs in to /api/login to obtain a JWT set as the "token" cookie
- Performs only safe HTTP GET requests (no data manipulation)

Usage:
  python tools/smoke_test.py \
    --base-url https://localhost:8443 \
    --username henninb@gmail.com \
    --password 'Monday1!'

Notes:
- Skips TLS verification by default for local dev with self-signed certs.
- Set --verify to a CA bundle path if desired.
"""
from __future__ import annotations

import argparse
import sys
import textwrap
from typing import Iterable, Tuple
from urllib.parse import urlparse
from http.cookies import SimpleCookie

import requests
from requests import Session
from urllib3.exceptions import InsecureRequestWarning
import urllib3


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="HTTPS smoke tests for raspi-finance-endpoint",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog=textwrap.dedent(
            """
            Examples:
              python tools/smoke_test.py \
                --base-url https://localhost:8443 \
                --username henninb@gmail.com \
                --password 'Monday1!'
            """
        ),
    )
    parser.add_argument("--base-url", default="https://localhost:8443", help="Base URL of the app")
    parser.add_argument("--username", required=True, help="Login username (email or username)")
    parser.add_argument("--password", required=True, help="Login password")
    parser.add_argument(
        "--verify",
        default=False,
        help="TLS verification. False to skip, or path to CA bundle.",
    )
    parser.add_argument("--timeout", type=int, default=15, help="Request timeout in seconds")
    parser.add_argument("--show-bytes", type=int, default=400, help="Show first N bytes of response bodies")
    return parser.parse_args()


def login(session: Session, base_url: str, username: str, password: str, verify, timeout: int) -> Tuple[bool, str]:
    url = f"{base_url.rstrip('/')}/api/login"
    payload = {"username": username, "password": password}
    r = session.post(url, json=payload, headers={"Accept": "application/json"}, verify=verify, timeout=timeout)
    ok = r.status_code == 200

    # Try to obtain token cookie from the session first
    token_cookie = session.cookies.get("token") or ""

    # If not present (e.g., domain mismatch like .bhenning.com vs localhost), parse Set-Cookie header
    if not token_cookie:
        sc = r.headers.get("Set-Cookie")
        if sc:
            cookie = SimpleCookie()
            cookie.load(sc)
            morsel = cookie.get("token")
            if morsel and morsel.value:
                token_cookie = morsel.value
                # Attach cookie explicitly for the localhost base URL
                host = urlparse(base_url).hostname or "localhost"
                session.cookies.set("token", token_cookie, domain=host, path="/")

    return ok, token_cookie


def show_get(session: Session, url: str, verify, timeout: int, show_bytes: int) -> None:
    print(f"\n=== GET {url} ===")
    try:
        r = session.get(url, headers={"Accept": "application/json"}, verify=verify, timeout=timeout)
        print(f"HTTP {r.status_code}")
        body = r.content[:show_bytes]
        if body:
            try:
                # Try pretty JSON first
                print(r.text[:show_bytes])
            except Exception:
                # Fallback to raw bytes
                sys.stdout.buffer.write(body)
                print()
    except requests.RequestException as e:
        print(f"REQUEST ERROR: {e}")


def main() -> int:
    args = parse_args()

    # Silence insecure warnings if verify is False
    verify = args.verify
    if isinstance(verify, str):
        # If explicitly passed a string 'false' or 'False', treat as False
        if verify.lower() in ("false", "0", "no", "none"):
            verify = False
    if verify is False:
        urllib3.disable_warnings(category=InsecureRequestWarning)

    endpoints: Iterable[str] = (
        # Actuator
        "/actuator/health",
        "/actuator/info",

        # Auth/introspection
        "/api/me",

        # Accounts
        "/api/account/active",
        "/api/account/select/active",
        "/api/account/totals",
        "/api/account/payment/required",

        # Transactions
        "/api/transaction/active",

        # Payments
        "/api/payment/active",
        "/api/payment/select",

        # Pending Transactions
        "/api/pending/transaction/active",
        "/api/pending/transaction/all",

        # Categories
        "/api/category/active",
        "/api/category/select/active",
    )

    with requests.Session() as session:
        # 1) Login to obtain JWT cookie
        ok, token = login(session, args.base_url, args.username, args.password, verify, args.timeout)
        print(f"Login: {'OK' if ok else 'FAILED'}; token cookie present: {bool(token)}")
        if not ok:
            print("Aborting smoke test: login failed.")
            return 2

        # Also set Authorization header with Bearer token for endpoints that accept it
        if token:
            session.headers.update({"Authorization": f"Bearer {token}"})
            print("Authorization header set (Bearer token)")

        # 2) Perform only GET requests
        for ep in endpoints:
            url = f"{args.base_url.rstrip('/')}{ep}"
            show_get(session, url, verify, args.timeout, args.show_bytes)

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
