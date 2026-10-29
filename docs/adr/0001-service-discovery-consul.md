# 0001. Consul for service discovery and central configuration

- Status: accepted
- Date: 2026-10-29
- Deciders: TicketRush team

## Context

Services call each other by name (order calls `inventory` and `catalog`) and the gateway routes
`/services/{name}/**` to whichever instances are healthy. We need a registry, health checks, and a place for
configuration shared by all services.

JHipster 9.4 offers two options for `serviceDiscoveryType`:

| | Consul | JHipster Registry (Eureka + Spring Cloud Config) |
|---|---|---|
| Generator label | "recommended" | "legacy" |
| Health checks | Agent actively checks `/management/health` | Clients send heartbeats |
| Central config | Built-in key/value store | Separate Config Server |
| Runs as | One official image, `hashicorp/consul` | A Spring Boot app maintained by JHipster |
| Outside the JVM | DNS and HTTP API, usable by any language | Java clients |

## Decision

Use Consul (`hashicorp/consul:2.0.4`, dev mode in docker-compose) for discovery and for configuration shared by
all services, loaded from `docker-compose/central-server-config/` by `consul-config-loader`.

Secrets are not stored in Consul: they come from environment variables (`docker-compose/.env`). Values from
Consul override environment variables in Spring Cloud Consul, so a secret left in central config would silently
win over the one in `.env`; `scripts/harden-compose.py` removes the generated JWT secret from it.

Service-to-service HTTP calls use Spring Cloud LoadBalancer on top of Consul (`@LoadBalanced RestClient`).

## Consequences

- One less Spring application to build and run; Consul is what the generator recommends and maintains.
- Consul in dev mode keeps everything in memory; production needs a 3-node server cluster with ACLs.
- Keycloak is not registered in Consul. The load-balanced `RestClient.Builder` is therefore not a default
  candidate, so code that calls Keycloak or other plain URLs keeps Spring Boot's ordinary builder.
