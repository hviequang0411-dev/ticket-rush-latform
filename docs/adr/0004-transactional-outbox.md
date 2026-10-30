# 0004. Publish events through a transactional outbox

- Status: accepted
- Date: 2026-10-30
- Deciders: TicketRush team

## Context

Services change their database and tell others about it on Kafka: catalog announces ticket types, inventory
announces expired holds, payment announces results, order announces final states. Writing to PostgreSQL and to
Kafka are two separate systems with no shared transaction:

| Approach | Failure mode |
|---|---|
| Send to Kafka, then commit | Kafka has an event for a change that rolled back |
| Commit, then send (phase 2: `@TransactionalEventListener`) | A crash or Kafka outage between the two loses the event; e.g. a hold expires but the order never hears of it |
| Kafka transactions | Still not atomic with PostgreSQL |
| Change data capture (Debezium) | Correct, but adds Kafka Connect and Debezium to run and monitor |

## Decision

Each publishing service has an `outbox_event` table. `Outbox.append()` writes the event **in the same transaction**
as the change it describes; it refuses to run without one. A relay then moves events to Kafka:

- **Order**: oldest first; a batch stops at the first failure, so events never overtake each other.
- **Concurrency**: batches are taken with `SELECT ... FOR UPDATE SKIP LOCKED`, so every instance can relay without
  sending an event twice.
- **Acknowledgement**: producers are synchronous; an event is marked published only after Kafka acknowledged it.
- **Keys**: messages are keyed by aggregate id (ticket type, order), so one aggregate's events stay in order on one
  partition, and carry an `eventId` header.
- **Retention**: published events are deleted after 7 days.

Delivery is **at least once**: a crash after sending but before marking resends. Consumers therefore record
processed event ids (`processed_event`) in the same transaction as their effect and skip ids they have seen.

The outbox code is copied into each service (about 150 lines) rather than shared as a library, so services keep
building and deploying independently.

## Consequences

- An event exists if and only if its change committed. Tests prove both directions: a rolled-back change leaves no
  event (`TicketTypeOutboxIT`), and a redelivered stale event cannot roll a stock total back
  (`TicketTypeEventsConsumerIT`).
- Events are delayed by up to one poll interval (500 ms by default).
- Each relay polls its database, a small constant load.
- Without Debezium the relay is application code we own and must test; that trade is worth it at this size.
