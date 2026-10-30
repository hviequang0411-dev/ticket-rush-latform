#!/usr/bin/env bash
# Deploys the stack to one VM over SSH, without a container registry.
#
#   deploy/deploy.sh <ssh-host>            # e.g. a Host entry in ~/.ssh/config
#   PLATFORM=linux/arm64 deploy/deploy.sh <ssh-host>   # ARM VMs (e.g. Oracle Ampere)
#
# Needs locally: Java 21, Docker, Python 3, deploy/.env.prod. Needs on the VM: Docker with the compose plugin.
# Images are built here with Jib and streamed over SSH (docker save | docker load).
set -euo pipefail

cd "$(dirname "$0")/.."
host="${1:?usage: deploy/deploy.sh <ssh-host>}"
platform="${PLATFORM:-linux/amd64}"
remote_dir="${REMOTE_DIR:-ticketrush}"
services=(gateway catalog inventory order payment)

[ -f deploy/.env.prod ] || { echo "deploy/.env.prod missing (see deploy/.env.prod.example)"; exit 1; }
domain="$(grep '^DOMAIN=' deploy/.env.prod | cut -d= -f2-)"

echo "==> Realm for the public demo at https://${domain}"
rm -rf deploy/build && mkdir -p deploy/build/realm-config
demo_admin_password="$(grep '^DEMO_ADMIN_PASSWORD=' deploy/.env.prod | cut -d= -f2-)"
[ -n "$demo_admin_password" ] || { echo "DEMO_ADMIN_PASSWORD missing in deploy/.env.prod"; exit 1; }
python3 - "$domain" "$demo_admin_password" <<'PY'
import json, shutil, sys
domain, admin_password = sys.argv[1:]
src = "docker-compose/realm-config"
realm = json.load(open(f"{src}/jhipster-realm.json", encoding="utf-8"))
web_app = next(c for c in realm["clients"] if c["clientId"] == "web_app")
web_app["redirectUris"] = sorted(set(web_app["redirectUris"]) | {f"https://{domain}/*", f"https://admin.{domain}/*"})
web_app["webOrigins"] = sorted(set(web_app["webOrigins"]) | {f"https://{domain}", f"https://admin.{domain}"})
# Anyone may sign up (into /Users, buyer only); the sample "user" account and the well-known admin password go
realm["registrationAllowed"] = True
realm["defaultGroups"] = ["/Users"]
realm["users"] = [u for u in realm["users"] if u["username"] != "user"]
admin = next(u for u in realm["users"] if u["username"] == "admin")
admin["credentials"] = [{"type": "password", "value": admin_password, "temporary": False}]
json.dump(realm, open("deploy/build/realm-config/jhipster-realm.json", "w", encoding="utf-8"), indent=2)
shutil.copy(f"{src}/keycloak-health-check.sh", "deploy/build/realm-config/keycloak-health-check.sh")
PY

echo "==> Building images for ${platform}"
for service in "${services[@]}"; do
  (cd "$service" && ./mvnw -ntp -q -Pprod -DskipTests verify jib:dockerBuild -Djib.from.platforms="$platform")
done

echo "==> Building the UI for ${platform}"
docker build -q --platform "$platform" -t ticketrush-ui:latest ui-prototype

echo "==> Copying configuration to ${host}:${remote_dir}"
ssh "$host" "mkdir -p ${remote_dir}"
tar czf - docker-compose observability deploy/compose.prod.yml deploy/Caddyfile deploy/build \
  | ssh "$host" "tar xzf - -C ${remote_dir}"
scp -q deploy/.env.prod "${host}:${remote_dir}/docker-compose/.env"

echo "==> Streaming images"
docker save "${services[@]}" ticketrush-ui:latest | gzip | ssh "$host" "gunzip | docker load"

echo "==> Starting"
ssh "$host" "cd ${remote_dir} && docker compose -f docker-compose/docker-compose.yml -f observability/compose.yml -f deploy/compose.prod.yml up -d --remove-orphans --wait --wait-timeout 900"
ssh "$host" "cd ${remote_dir} && docker compose -f docker-compose/docker-compose.yml ps --format 'table {{.Service}}\t{{.Status}}'"
echo "==> https://${domain}  (admin UI: https://admin.${domain}, Grafana: https://grafana.${domain})"
