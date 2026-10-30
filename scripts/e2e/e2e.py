#!/usr/bin/env python3
"""End-to-end check through the gateway, against the running docker-compose stack.

    docker compose -f docker-compose/docker-compose.yml -f scripts/e2e/compose.override.yml up -d --wait
    python scripts/e2e/e2e.py

Covers: organiser APIs (event + ticket types in one call, drafts hidden and unsellable), catalog -> Kafka ->
inventory stock sync, checkout through gateway -> order -> catalog/inventory, idempotent replay, cancel, a
concurrent sell-out over HTTP, hold expiry -> Kafka -> order EXPIRED, the payment saga (paid -> CONFIRMED with
e-tickets, declined -> FAILED, paid after expiry -> REFUNDED, forged webhook rejected), gate check-in (admit once),
the waiting room (no pass -> 428, queue -> pass -> checkout) and the gateway's per-user rate limit on checkout.

Buyer calls use a client-credentials token of the `internal` client. Organiser calls need a real admin: the script
keeps an `e2e-admin` user in /Admins, gives it a fresh random password, and turns on the password grant of the
`web_app` client only while it signs in. Tokens are fetched inside the compose network so that their issuer is
http://keycloak:9080 like the services expect. Local dev stack only.
"""
import base64
import json
import os
import secrets
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


def curl(*args):
    """curl inside the compose network; returns (status, body)."""
    out = subprocess.run(
        ["docker", "run", "--rm", "--network", NETWORK, CURL_IMAGE, "-s", "-w", "\n%{http_code}", *args],
        capture_output=True, text=True, check=True,
    ).stdout
    body, _, status = out.rpartition("\n")
    return int(status), body


def fetch_admin_token():
    kc = "http://keycloak:9080"
    _, master = curl(
        "-X", "POST", f"{kc}/realms/master/protocol/openid-connect/token", "-d", "grant_type=password", "-d", "client_id=admin-cli",
        "-d", "username=" + env("KEYCLOAK_ADMIN_USER"), "-d", "password=" + env("KEYCLOAK_ADMIN_PASSWORD"),
    )
    auth = ["-H", "Authorization: Bearer " + json.loads(master)["access_token"], "-H", "Content-Type: application/json"]
    admin_api = f"{kc}/admin/realms/jhipster"

    _, users = curl(*auth, f"{admin_api}/users?username=e2e-admin&exact=true")
    if not json.loads(users):
        curl(*auth, "-X", "POST", f"{admin_api}/users", "-d", json.dumps({"username": "e2e-admin", "enabled": True, "email": "e2e-admin@localhost", "emailVerified": True, "firstName": "E2E", "lastName": "Admin", "groups": ["/Admins", "/Users"]}))
        _, users = curl(*auth, f"{admin_api}/users?username=e2e-admin&exact=true")
    user_id = json.loads(users)[0]["id"]
    password = secrets.token_urlsafe(18)
    curl(*auth, "-X", "PUT", f"{admin_api}/users/{user_id}/reset-password", "-d", json.dumps({"type": "password", "value": password, "temporary": False}))

    _, clients = curl(*auth, f"{admin_api}/clients?clientId=web_app")
    web_app = json.loads(clients)[0]
    curl(*auth, "-X", "PUT", f"{admin_api}/clients/{web_app['id']}", "-d", json.dumps({**web_app, "directAccessGrantsEnabled": True}))
    try:
        _, token = curl(
            "-X", "POST", f"{kc}/realms/jhipster/protocol/openid-connect/token", "-d", "grant_type=password", "-d", "client_id=web_app",
            "-d", "username=e2e-admin", "-d", "password=" + password, "-d", "scope=openid",
        )
        return json.loads(token)["access_token"]
    finally:
        curl(*auth, "-X", "PUT", f"{admin_api}/clients/{web_app['id']}", "-d", json.dumps({**web_app, "directAccessGrantsEnabled": False}))


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
    status, stocks = anonymous("GET", f"{INVENTORY}/public/availability?ticketTypeIds={ticket_type_id}")
    if status != 200 or not stocks:
        return None
    return stocks[0]


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


def checkout(token, ticket_type_id, quantity, key=None, queue_pass=None):
    key = key or str(uuid.uuid4())
    headers = {"Idempotency-Key": key}
    if queue_pass:
        headers["X-Queue-Pass"] = queue_pass
    return call("POST", f"{ORDER}/checkout", token, {"items": [{"ticketTypeId": ticket_type_id, "quantity": quantity}]}, headers)


def create_event(admin, slug, ticket_types, publish=True, waiting_room=False):
    status, event = call(
        "POST", f"{CATALOG}/admin/events?publish={'true' if publish else 'false'}", admin,
        {
            "name": "E2E " + slug, "slug": slug, "startTime": "2031-12-31T12:00:00Z", "waitingRoom": waiting_room,
            "venue": {"name": "E2E Arena", "address": "Me Tri", "city": "Ha Noi"},
            "ticketTypes": [{"name": name, "price": price, "totalQuantity": quantity} for name, quantity, price in ticket_types],
        },
    )
    check(f"organiser creates {slug}", status == 201, (status, event))
    return event, {t["name"]: t["id"] for t in event["ticketTypes"]}


def queued_event_id(token, ticket_type_id):
    status, body = anonymous("GET", f"{CATALOG}/public/events?size=100")
    for event in body if status == 200 else []:
        if any(t["id"] == ticket_type_id for t in event["ticketTypes"]):
            return event["id"]
    raise SystemExit("waiting room event not listed")


def main():
    token = fetch_token()
    print("Token")
    check("service token carries ROLE_SERVICE", "ROLE_SERVICE" in claims(token).get("roles", []), claims(token).get("roles"))

    admin = fetch_admin_token()
    check("organiser token carries ROLE_ADMIN", "ROLE_ADMIN" in claims(admin).get("roles", []), claims(admin).get("roles"))
    status, _ = call("POST", f"{CATALOG}/events", token, {"name": "x", "slug": "x", "startTime": "2031-12-31T12:00:00Z", "status": "PUBLISHED"})
    check("buyers and services cannot write the catalog", status == 403, status)

    print("Organiser -> catalog -> Kafka -> inventory")
    run = uuid.uuid4().hex[:8]
    started = time.time()
    event, types = create_event(admin, "e2e-" + run, [("VIP", 100, 2500000), ("Hot", 5, 900000)])
    vip, hot = types["VIP"], types["Hot"]
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

    print("Drafts")
    _, draft_types = create_event(admin, "e2e-draft-" + run, [("Draft", 10, 100000)], publish=False)
    wait_for("draft stock in inventory", lambda: stock_of(token, draft_types["Draft"]))
    status, _ = anonymous("GET", f"{CATALOG}/public/events/e2e-draft-{run}")
    check("drafts are hidden from buyers", status == 404, status)
    status, _ = checkout(token, draft_types["Draft"], 1)
    check("drafts cannot be bought by ticket type id", status == 400, status)

    print("Concurrent sell-out over HTTP")
    seats, buyers = 5, 40
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

    print("E-tickets and check-in")
    status, tickets = call("GET", f"{ORDER}/my-tickets", token)
    mine = [t for t in tickets or [] if t["orderCode"] == order["code"]]
    check("confirmed order issued one ticket per seat", status == 200 and len(mine) == 2, (status, mine))
    qr = mine[0]["qrPayload"] if mine else "none"
    status, _ = call("POST", f"{ORDER}/admin/check-in", token, {"qrPayload": qr})
    check("buyers cannot scan tickets", status == 403, status)
    with ThreadPoolExecutor(max_workers=8) as pool:
        outcomes = [body["outcome"] for _, body in pool.map(lambda _: call("POST", f"{ORDER}/admin/check-in", admin, {"qrPayload": qr}), range(8))]
    check("8 gates scanning one ticket at once admit it exactly once", outcomes.count("ADMITTED") == 1 and outcomes.count("ALREADY_USED") == 7, outcomes)
    _, forged = call("POST", f"{ORDER}/admin/check-in", admin, {"qrPayload": qr[:-4] + "AAAA"})
    check("an edited QR code is invalid", forged["outcome"] == "INVALID", forged)

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

    print("Waiting room")
    _, queued_types = create_event(admin, "e2e-queue-" + run, [("Queue", 10, 300000)], waiting_room=True)
    queued = queued_types["Queue"]
    wait_for("waiting room stock in inventory", lambda: stock_of(token, queued))
    status, _ = checkout(token, queued, 1)
    check("no pass -> 428", status == 428, status)
    queue = f"{GATEWAY}/api/waiting-room/{queued_event_id(token, queued)}"
    status, joined = call("POST", queue, token)
    check("join the queue through the gateway", status == 200 and joined["inQueue"], (status, joined))
    admitted = wait_for("admission", lambda: (lambda r: r[1] if r[0] == 200 and r[1]["admitted"] else None)(call("GET", queue, token)), timeout=30)
    status, order = checkout(token, queued, 1, queue_pass=admitted["pass"])
    check("with the pass the checkout goes through", status == 201, (status, order))
    status, _ = checkout(token, queued, 1, queue_pass=admitted["pass"][:-2] + "xx")
    check("a tampered pass is refused", status == 428, status)

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
