#!/usr/bin/env python3
"""Re-apply local changes to JHipster-generated docker-compose files.

JHipster overwrites docker-compose/ on every `jhipster jdl` run. Run this script right after
generating so that:
- secrets come from docker-compose/.env instead of being hard-coded
- the Keycloak client `internal` (service-to-service calls) carries ROLE_SERVICE in its tokens
- single-node Kafka can create its internal topics (without them no consumer group ever works)
- services only discover healthy instances, so a restarted service is not called at its old address
- the gateway has its own Redis for rate limiting

    python scripts/harden-compose.py

It is idempotent: running it twice leaves the files unchanged.
"""
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent / "docker-compose"
COMPOSE = ROOT / "docker-compose.yml"
REALM = ROOT / "realm-config" / "jhipster-realm.json"
CENTRAL = ROOT / "central-server-config" / "application.yml"

JWT_ENV = "      - JHIPSTER_SECURITY_AUTHENTICATION_JWT_BASE64_SECRET=${JHIPSTER_JWT_BASE64_SECRET:?}"


def harden_compose(text: str) -> str:
    text = text.replace("OIDC_CLIENT_SECRET=web_app", "OIDC_CLIENT_SECRET=${KEYCLOAK_WEB_APP_CLIENT_SECRET:?}")
    text = text.replace("OIDC_CLIENT_SECRET=internal", "OIDC_CLIENT_SECRET=${KEYCLOAK_INTERNAL_CLIENT_SECRET:?}")

    # Reactive gateway: R2DBC + Liquibase over JDBC share one password
    text = re.sub(
        r"^      - SPRING_R2DBC_PASSWORD=\n",
        "      - SPRING_R2DBC_PASSWORD=${GATEWAY_DB_PASSWORD:?}\n"
        "      - SPRING_LIQUIBASE_PASSWORD=${GATEWAY_DB_PASSWORD:?}\n" + JWT_ENV + "\n",
        text,
        flags=re.M,
    )

    # Servlet services: SPRING_DATASOURCE_URL=jdbc:postgresql://<app>-postgresql:5432/<db>
    text = re.sub(
        r"^      - SPRING_DATASOURCE_URL=(jdbc:postgresql://([a-z0-9-]+)-postgresql:5432/\S+)\n"
        r"      - SPRING_DATASOURCE_USERNAME=(\S+)\n"
        r"      - SPRING_DATASOURCE_PASSWORD=\n",
        lambda m: (
            f"      - SPRING_DATASOURCE_URL={m.group(1)}\n"
            f"      - SPRING_DATASOURCE_USERNAME={m.group(3)}\n"
            f"      - SPRING_DATASOURCE_PASSWORD=${{{m.group(2).upper()}_DB_PASSWORD:?}}\n" + JWT_ENV + "\n"
        ),
        text,
        flags=re.M,
    )

    # Postgres containers: password auth instead of trust
    text = re.sub(
        r"^      - POSTGRES_USER=(\S+)\n      - POSTGRES_HOST_AUTH_METHOD=trust\n",
        lambda m: f"      - POSTGRES_USER={m.group(1)}\n      - POSTGRES_PASSWORD=${{{m.group(1).upper()}_DB_PASSWORD:?}}\n",
        text,
        flags=re.M,
    )

    text = text.replace("KC_BOOTSTRAP_ADMIN_USERNAME=admin", "KC_BOOTSTRAP_ADMIN_USERNAME=${KEYCLOAK_ADMIN_USER:?}")
    text = text.replace(
        "      - KC_BOOTSTRAP_ADMIN_PASSWORD=admin\n",
        "      - KC_BOOTSTRAP_ADMIN_PASSWORD=${KEYCLOAK_ADMIN_PASSWORD:?}\n"
        "      - KEYCLOAK_WEB_APP_CLIENT_SECRET=${KEYCLOAK_WEB_APP_CLIENT_SECRET:?}\n"
        "      - KEYCLOAK_INTERNAL_CLIENT_SECRET=${KEYCLOAK_INTERNAL_CLIENT_SECRET:?}\n",
    )

    # Gateway rate limiter: its own Redis (catalog-redis belongs to catalog)
    if "  gateway-redis:\n" not in text:
        text = re.sub(
            r"^  gateway-postgresql:\n",
            "  gateway-redis:\n    image: redis:8.10.1\n  gateway-postgresql:\n",
            text,
            count=1,
            flags=re.M,
        )
        text = text.replace(
            "      - SPRING_LIQUIBASE_PASSWORD=${GATEWAY_DB_PASSWORD:?}\n",
            "      - SPRING_LIQUIBASE_PASSWORD=${GATEWAY_DB_PASSWORD:?}\n      - SPRING_DATA_REDIS_HOST=gateway-redis\n",
            1,
        )

    # apache/kafka-native drops its single-node defaults as soon as any KAFKA_* variable is set, so internal topics
    # fall back to replication factor 3 and can't be created on one broker: consumer groups then never get a
    # coordinator and silently receive nothing
    kafka_anchor = "      KAFKA_GROUP_INITIAL_REBALANCE_DELAY_MS: 0\n"
    if kafka_anchor in text and "KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR" not in text:
        text = text.replace(
            kafka_anchor,
            kafka_anchor
            + "      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1\n"
            "      KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR: 1\n"
            "      KAFKA_TRANSACTION_STATE_LOG_MIN_ISR: 1\n",
        )

    # Keycloak dev mode rebuilds Quarkus on boot (~1-2 min); don't count that as failures
    kc_health = '      test: "bash /opt/keycloak/health-check.sh"\n      interval: 5s\n      timeout: 5s\n      retries: 20\n'
    if kc_health in text and "start_period" not in text.split("keycloak:")[-1]:
        text = text.replace(
            kc_health,
            kc_health + "      # Keycloak dev mode rebuilds Quarkus on boot (~1-2 min); don't count that as failures\n"
            "      start_period: 180s\n",
        )
    return text


SERVICE_ROLE = "ROLE_SERVICE"


def harden_realm(text: str) -> str:
    text = text.replace('"secret": "web_app"', '"secret": "${KEYCLOAK_WEB_APP_CLIENT_SECRET}"')
    text = text.replace('"secret": "internal"', '"secret": "${KEYCLOAK_INTERNAL_CLIENT_SECRET}"')
    realm = json.loads(text)

    roles = realm["roles"]["realm"]
    if not any(role["name"] == SERVICE_ROLE for role in roles):
        roles.append(
            {
                "name": SERVICE_ROLE,
                "description": "TicketRush services calling each other's internal APIs",
                "composite": False,
                "clientRole": False,
                "containerId": realm["id"],
                "attributes": {},
            }
        )

    internal = next(client for client in realm["clients"] if client["clientId"] == "internal")
    # The `jhipster` scope maps realm roles to the `roles` claim the services read
    if "jhipster" not in internal["defaultClientScopes"]:
        internal["defaultClientScopes"].append("jhipster")

    users = realm.setdefault("users", [])
    if not any(user.get("serviceAccountClientId") == "internal" for user in users):
        users.append(
            {
                "username": "service-account-internal",
                "enabled": True,
                "serviceAccountClientId": "internal",
                "realmRoles": ["default-roles-jhipster", SERVICE_ROLE],
            }
        )
    return json.dumps(realm, indent=2, ensure_ascii=False) + "\n"


CONSUL_DISCOVERY = """
spring:
  cloud:
    consul:
      discovery:
        # Only return instances whose health check passes: after a restart the old registration lingers as
        # 'critical' and the load balancer would otherwise keep picking it
        query-passing: true
        # Remove registrations of instances that died without deregistering
        health-check-critical-timeout: 1m
"""


GATEWAY_CENTRAL = ROOT / "central-server-config" / "gateway.yml"

GATEWAY_DISCOVERY = """# Gateway-only overrides of application.yml (loaded into Consul as config/gateway/data)
spring:
  cloud:
    consul:
      discovery:
        # The discovery route locator builds /services/{name}/** routes from the service list. With query-passing a
        # restarted service is missing from that list while its first health check is pending, its route is dropped,
        # and nothing re-adds it: Consul does not signal health changes as catalog changes.
        query-passing: false
"""


def harden_central(text: str) -> str:
    if "query-passing" not in text:
        text = text.rstrip("\n") + "\n" + CONSUL_DISCOVERY
    return re.sub(
        r"\njhipster:\n  security:\n    authentication:\n      jwt:\n        base64-secret: \S+\n",
        "\n# jhipster.security.authentication.jwt.base64-secret is injected per service via the\n"
        "# JHIPSTER_SECURITY_AUTHENTICATION_JWT_BASE64_SECRET env var (see ../.env.example).\n",
        text,
    )


def main() -> int:
    for path, fn in ((COMPOSE, harden_compose), (REALM, harden_realm), (CENTRAL, harden_central)):
        before = path.read_text(encoding="utf-8")
        after = fn(before)
        path.write_text(after, encoding="utf-8", newline="\n")
        print(f"{'updated ' if after != before else 'unchanged'} {path.relative_to(ROOT.parent)}")

    if not GATEWAY_CENTRAL.exists() or GATEWAY_CENTRAL.read_text(encoding="utf-8") != GATEWAY_DISCOVERY:
        GATEWAY_CENTRAL.write_text(GATEWAY_DISCOVERY, encoding="utf-8", newline="\n")
        print(f"updated  {GATEWAY_CENTRAL.relative_to(ROOT.parent)}")

    leftovers = re.findall(
        r"^.*(?:PASSWORD=$|HOST_AUTH_METHOD=trust|CLIENT_SECRET=(?:web_app|internal)|ADMIN_PASSWORD=admin).*$",
        COMPOSE.read_text(encoding="utf-8"),
        flags=re.M,
    )
    if leftovers:
        print("Hard-coded values still present:", *leftovers, sep="\n  ")
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
