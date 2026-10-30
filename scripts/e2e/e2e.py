#!/usr/bin/env python3
"""End-to-end check through the gateway, against the running docker-compose stack.

    docker compose -f docker-compose/docker-compose.yml -f scripts/e2e/compose.override.yml up -d --wait
    python scripts/e2e/e2e.py

Covers: catalog -> Kafka -> inventory stock sync, checkout through gateway -> order -> catalog/inventory,
idempotent replay, cancel, a concurrent sell-out over HTTP, hold expiry -> Kafka -> order EXPIRED, the payment
saga (paid -> CONFIRMED, declined -> FAILED, paid after expiry -> REFUNDED, forged webhook rejected), and the
gateway's per-user rate limit on checkout.
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
PAYMENT_PUBLIC = f"{GATEWAY}/services/payment/api/public"
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


def anonymous(method, url):
    request = urllib.request.Request(url, data=b"" if method == "POST" else None, method=method)
    try:
        with urllib.request.urlopen(request, timeout=30) as response:
            raw = response.read()
            return response.status, json.loads(raw) if raw and response.headers.get_content_type() == "application/json" else raw
    except urllib.error.HTTPError as error:
        return error.code, error.read()


def order_status(token, code, expected, timeout=60):
    def probe():
        status, body = call("GET", f"{ORDER}/my-orders/{code}", token)
        return body if status == 200 and body["status"] == expected else None

    try:
        return wait_for(f"order {code} to become {expected}", probe, timeout)
    except SystemExit:
        return call("GET", f"{ORDER}/my-orders/{code}", token)[1]


def pay(order, result):
    ref = order["paymentUrl"].rstrip("/").rsplit("/", 1)[-1]
    return anonymous("POST", f"{PAYMENT_PUBLIC}/mock-provider/{ref}/complete?result={result}")


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
    check("checkout returns 201 AWAITING_PAYMENT with a payment URL", status == 201 and order["status"] == "AWAITING_PAYMENT" and order.get("paymentUrl"), (status, order))
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
    # 409 sold out, or 429 if an earlier run drained this account's checkout bucket: either way nothing else is sold
    check(f"the other {buyers - seats} are refused", statuses.count(409) + statuses.count(429) == buyers - seats, sorted(statuses))
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

    print("Payment saga")
    status, order = checkout(token, vip, 2)
    status, page = anonymous("GET", order["paymentUrl"].replace("http://localhost:8080", GATEWAY))
    check("mock provider page is public", status == 200, status)
    status, _ = pay(order, "success")
    check("mock provider accepts the payment", status == 200, status)
    confirmed = order_status(token, order["code"], "CONFIRMED")
    check("paid order becomes CONFIRMED (payment -> outbox -> Kafka -> saga -> inventory confirm)", confirmed["status"] == "CONFIRMED", confirmed)
    check("confirmed seats stay sold", stock_of(token, vip)["availableQuantity"] == 98, stock_of(token, vip))

    status, order = checkout(token, vip, 1)
    pay(order, "declined")
    failed = order_status(token, order["code"], "FAILED")
    check("declined payment fails the order", failed["status"] == "FAILED" and failed["failureReason"] == "PAYMENT_FAILED", failed)
    wait_for("declined seats back on sale", lambda: stock_of(token, vip)["availableQuantity"] == 98)
    check("declined seats back on sale", stock_of(token, vip)["availableQuantity"] == 98)

    status, order = checkout(token, vip, 1)
    order_status(token, order["code"], "EXPIRED", timeout=90)
    status, _ = pay(order, "success")
    refunded = order_status(token, order["code"], "REFUNDED")
    check("payment after the seats expired is refunded", refunded["status"] == "REFUNDED", refunded)

    status, _ = anonymous("POST", f"{PAYMENT_PUBLIC}/webhooks/mock?ref=X&result=success&amount=1&transactionId=F1&signature=bad")
    check("forged webhook through the gateway is rejected", status == 400, status)

    print("Gateway rate limit on checkout")
    # Invalid quantity: whatever gets past the limiter is rejected by order with 400, so nothing is bought
    burst = 80
    with ThreadPoolExecutor(max_workers=burst) as pool:
        statuses = [s for s, _ in pool.map(lambda _: call("POST", f"{ORDER}/checkout", token, {"items": [{"ticketTypeId": vip, "quantity": 0}]}, {"Idempotency-Key": str(uuid.uuid4())}), range(burst))]
    limited = statuses.count(429)
    # The bucket holds 60 (e2e override) and refills 1/s while the burst is in flight, so on a slow host fewer than
    # burst - 60 may be limited; what matters is that the limit kicks in
    check(f"{burst} rapid checkouts from one user: the limit kicks in (429)", limited > 0, sorted(set(statuses)))
    check("the rest reach order and get its 400", statuses.count(400) == burst - limited, sorted(set(statuses)))
    status, _ = call("GET", f"{ORDER}/my-orders", token)
    check("other routes are not limited by the checkout bucket", status == 200, status)

    print()
    print("All checks passed" if not failures else f"{len(failures)} check(s) failed: {failures}")
    return 1 if failures else 0


if __name__ == "__main__":
    sys.exit(main())
