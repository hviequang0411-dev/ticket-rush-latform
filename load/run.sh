#!/usr/bin/env bash
# Runs a k6 scenario inside the compose network against the running stack.
#
#   load/run.sh on-sale            # SEATS=500 BUYERS=2000 VUS=200 by default
#   SEATS=1000 BUYERS=5000 VUS=500 load/run.sh on-sale
#   load/run.sh browse             # RATE=200 req/s for DURATION=2m
#
# Start the stack with the load-test override first, e.g.
#   TRACING_SAMPLING_PROBABILITY=0.05 docker compose -f docker-compose/docker-compose.yml \
#     -f observability/compose.yml -f load/compose.loadtest.yml up -d --wait
set -euo pipefail

cd "$(dirname "$0")/.."
scenario="${1:?usage: load/run.sh on-sale|browse}"
secret="$(grep '^KEYCLOAK_INTERNAL_CLIENT_SECRET=' docker-compose/.env | cut -d= -f2-)"
stamp="$(date +%Y%m%d-%H%M%S)"
network="$(docker inspect -f '{{range $k, $v := .NetworkSettings.Networks}}{{$k}} {{end}}' "$(docker compose -f docker-compose/docker-compose.yml ps -q gateway)" | awk '{print $1}')"

# MSYS_NO_PATHCONV keeps Git Bash on Windows from rewriting the container paths
MSYS_NO_PATHCONV=1 docker run --rm \
  --network "$network" \
  -v "$PWD/load/k6:/scripts:ro" \
  -v "$PWD/load/results:/results" \
  -e KEYCLOAK_INTERNAL_CLIENT_SECRET="$secret" \
  -e RESULT_FILE="/results/${scenario}-${stamp}.json" \
  -e SEATS -e BUYERS -e VUS -e P95_MS -e RATE -e DURATION \
  grafana/k6:2.3.0 run "/scripts/${scenario}.js"
