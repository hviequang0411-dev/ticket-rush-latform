# JHipster generated Docker-Compose configuration

## Usage

Launch all your infrastructure by running: `docker compose up -d`.

## Configured Docker services

### Service registry and configuration server:

- [Consul](http://localhost:8500)

### Applications and dependencies:

- gateway (gateway application)
- gateway's postgresql database
- catalog (microservice application)
- catalog's postgresql database
- inventory (microservice application)
- inventory's postgresql database
- order (microservice application)
- order's postgresql database

### Additional Services:

- Kafka
- [Keycloak server](http://localhost:9080)
