# TicketRush

High-load event ticketing platform built as microservices. Work in progress: phase 1 (gateway + catalog).

## Versions

| Component | Version |
|---|---|
| JHipster | 9.4.0 |
| Spring Boot / Spring Cloud | 4.1.1 / 2025.1.3 |
| Java / Node | 21 / 24 |
| PostgreSQL | 18.6 |
| Keycloak | 26.7.4 |
| Consul | 2.0.4 |

## Run locally

Requirements: Java 21, Node 22.18+ or 24.11+, Docker.

1. Create the secrets file and fill in real values:

   ```bash
   cp docker-compose/.env.example docker-compose/.env
   ```

2. Add Keycloak to your hosts file, so the browser and the services agree on the token issuer
   (`http://keycloak:9080`):

   ```
   127.0.0.1 keycloak
   ```

3. Build the images:

   ```bash
   (cd catalog && ./mvnw -ntp -Pprod verify jib:dockerBuild)
   (cd gateway && ./mvnw -ntp -Pprod verify jib:dockerBuild)
   ```

4. Start everything:

   ```bash
   docker compose -f docker-compose/docker-compose.yml up -d --wait
   ```

Endpoints: gateway UI http://localhost:8080, Consul http://localhost:8500, Keycloak http://localhost:9080.
Keycloak takes 1-2 minutes to start in dev mode.

## Repository layout

| Path | Contents |
|---|---|
| `ticketrush.jdl` | Source of truth for generated apps and entities |
| `gateway/` | Spring Cloud Gateway (reactive) + React UI |
| `catalog/` | Venue, Event, TicketType |
| `docker-compose/` | Local stack: Consul, Keycloak, one PostgreSQL per service |
