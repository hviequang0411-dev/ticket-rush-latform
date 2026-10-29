#!/usr/bin/env python3
"""End-to-end check of the phase 2 flow through the gateway, against the running docker-compose stack.

    docker compose -f docker-compose/docker-compose.yml -f scripts/e2e/compose.override.yml up -d --wait
    python scripts/e2e/phase2.py

Covers: catalog -> Kafka -> inventory stock sync, checkout through gateway -> order -> catalog/inventory,
idempotent replay, cancel, a concurrent sell-out over HTTP, and hold expiry -> Kafka -> order EXPIRED.
The token is a client-credentials token of the `internal` client, fetched inside the compose network so that
its issuer is http://keycloak:9080 like the services expect.
"""
import base64
import json
import os
import subprocess
import sys
import time
import urllib.error
import urllib.request
import uuid
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path

GATEWAY = os.environ.get("TICKETRUSH_GATEWAY", "http://localhost:8088")
CATALOG = f"{GATEWAY}/services/catalog/api"
INVENTORY = f"{GATEWAY}/services/inventory/api"
ORDER = f"{GATEWAY}/services/order/api"
ENV_FILE = Path(__file__).resolve().parents[2] / "docker-compose" / ".env"
NETWORK = "docker-compose_default"
CURL_IMAGE = "curlimages/curl:8.17.0"

failures = []


def check(name, condition, detail=""):
    print(f"  {'PASS' if condition else 'FAIL'}  {name}{'  (' + str(detail) + ')' if detail and not condition else ''}")
    if not condition:
        failures.append(name)


def env(name):
    for line in ENV_FILE.read_text(encoding="utf-8").splitlines():
        if line.startswith(name + "="):
            return line.split("=", 1)[1]
    raise SystemExit(f"{name} missing from {ENV_FILE}")


def fetch_token():
    out = subprocess.run(
        [
            "docker", "run", "--rm", "--network", NETWORK, CURL_IMAGE, "-s", "-X", "POST",
            "http://keycloak:9080/realms/jhipster/protocol/openid-connect/token",
            "-d", "grant_type=client_credentials", "-d", "client_id=internal",
            "-d", "client_secret=" + env("KEYCLOAK_INTERNAL_CLIENT_SECRET"), "-d", "scope=openid",
        ],
        capture_output=True, text=True, check=True,
    ).stdout
    return json.loads(out)["access_token"]


def claims(token):
    payload = token.split(".")[1]
    return json.loads(base64.urlsafe_b64decode(payload + "=" * (-len(payload) % 4)))


def call(method, url, token, body=None, headers=None):
    data = json.dumps(body).encode() if body is not None else None
    request = urllib.request.Request(url, data=data, method=method)
    request.add_header("Authorization", "Bearer " + token)
    request.add_header("Content-Type", "application/json")
    for key, value in (headers or {}).items():
        request.add_header(key, value)
    try:
        with urllib.request.urlopen(request, timeout=30) as response:
            raw = response.read()
            return response.status, json.loads(raw) if raw else None
    except urllib.error.HTTPError as error:
        raw = error.read()
        try:
            return error.code, json.loads(raw) if raw else None
        except ValueError:
            return error.code, raw.decode(errors="replace")


def wait_for(description, probe, timeout=60):
    deadline = time.time() + timeout
    while time.time() < deadline:
        value = probe()
        if value:
            return value
        time.sleep(1)
    raise SystemExit(f"Timed out waiting for {description}")


def stock_of(token, ticket_type_id):
    status, stocks = call("GET", f"{INVENTORY}/ticket-stocks?size=1000", token)
    if status != 200:
        return None
    return next((s for s in stocks if s["ticketTypeId"] == ticket_type_id), None)


def checkout(token, ticket_type_id, quantity, key=None):
    key = key or str(uuid.uuid4())
    return call("POST", f"{ORDER}/checkout", token, {"items": [{"ticketTypeId": ticket_type_id, "quantity": quantity}]}, {"Idempotency-Key": key})


def main():
    token = fetch_token()
    print("Token")
    check("service token carries ROLE_SERVICE", "ROLE_SERVICE" in claims(token).get("roles", []), claims(token).get("roles"))

    print("Catalog -> Kafka -> inventory")
    run = uuid.uuid4().hex[:8]
    _, venue = call("POST", f"{CATALOG}/venues", token, {"name": "E2E Arena " + run, "address": "Me Tri", "city": "Ha Noi", "capacity": 1000})
    _, event = call(
        "POST", f"{CATALOG}/events", token,
        {"name": "E2E " + run, "slug": "e2e-" + run, "startTime": "2031-12-31T12:00:00Z", "status": "PUBLISHED", "venue": {"id": venue["id"]}},
    )

    def ticket_type(name, quantity, price):
        status, body = call(
            "POST", f"{CATALOG}/ticket-types", token,
            {"name": name, "price": price, "currency": "VND", "totalQuantity": quantity, "event": {"id": event["id"]}},
        )
        check(f"create ticket type {name}", status == 201, status)
        return body["id"]

    vip = ticket_type("VIP", 100, 2500000)
    started = time.time()
    stock = wait_for("VIP stock in inventory", lambda: stock_of(token, vip))
    check("stock created from catalog event", stock["totalQuantity"] == 100 and stock["availableQuantity"] == 100, stock)
    print(f"        (visible in inventory after {time.time() - started:.1f}s)")

    print("Checkout")
    key = str(uuid.uuid4())
    status, order = checkout(token, vip, 2, key)
    check("checkout returns 201 RESERVED", status == 201 and order["status"] == "RESERVED", (status, order))
    check("total priced by catalog", order and float(order["totalAmount"]) == 5000000, order)
    status, replay = checkout(token, vip, 2, key)
    check("retry with same Idempotency-Key returns 200 and the same order", status == 200 and replay["code"] == order["code"], (status, replay))
    check("seats held exactly once", stock_of(token, vip)["availableQuantity"] == 98)
    status, _ = checkout(token, vip, 1, "bad")
    check("short Idempotency-Key rejected", status == 400, status)

    status, cancelled = call("POST", f"{ORDER}/my-orders/{order['code']}/cancel", token)
    check("cancel returns CANCELLED", status == 200 and cancelled["status"] == "CANCELLED", (status, cancelled))
    check("cancel puts seats back", stock_of(token, vip)["availableQuantity"] == 100)

    print("Concurrent sell-out over HTTP")
    seats, buyers = 5, 40
    hot = ticket_type("Hot", seats, 900000)
    wait_for("Hot stock in inventory", lambda: stock_of(token, hot))
    with ThreadPoolExecutor(max_workers=buyers) as pool:
        statuses = [status for status, _ in pool.map(lambda _: checkout(token, hot, 1), range(buyers))]
    check(f"{buyers} buyers for {seats} seats -> {seats} x 201", statuses.count(201) == seats, sorted(statuses))
    check(f"the other {buyers - seats} get 409", statuses.count(409) == buyers - seats, sorted(statuses))
    check("stock at exactly 0", stock_of(token, hot)["availableQuantity"] == 0, stock_of(token, hot))

    print("Hold expiry -> Kafka -> order")
    status, order = checkout(token, vip, 3)
    check("checkout for expiry test", status == 201, status)
    expired = wait_for(
        "order to expire",
        lambda: (lambda r: r[1] if r[0] == 200 and r[1]["status"] == "EXPIRED" else None)(call("GET", f"{ORDER}/my-orders/{order['code']}", token)),
        timeout=90,
    )
    check("order EXPIRED after its hold ran out", expired["status"] == "EXPIRED")
    check("expired seats back on sale", stock_of(token, vip)["availableQuantity"] == 100, stock_of(token, vip))

    print()
    print("All checks passed" if not failures else f"{len(failures)} check(s) failed: {failures}")
    return 1 if failures else 0


if __name__ == "__main__":
    sys.exit(main())
