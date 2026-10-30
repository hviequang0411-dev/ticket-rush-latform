# 0002. Orchestrate the order saga in the order service

- Status: accepted
- Date: 2026-10-30
- Deciders: TicketRush team

## Context

Buying a ticket spans three services, each with its own database: inventory holds seats, payment takes money,
order records the purchase. There is no distributed transaction, so a failure halfway must be undone by
compensating steps: seats released, a payment cancelled, money refunded.

Two ways to coordinate that:

| | Choreography | Orchestration |
|---|---|---|
| Who decides the next step | Each service reacts to the others' events | One orchestrator sends commands and reacts to replies |
| Where the flow is written down | Nowhere in one place; it emerges from subscriptions | In one class, as a state machine |
| Adding a step (e.g. a fraud check) | Change several services' subscriptions | Change the orchestrator |
| Coupling | Services know each other's events | Services only know the orchestrator's requests |
| Debugging "why is this order stuck?" | Correlate events across services | Read one row: the order's state |

The hard cases are the ones that race. The seat hold can expire (10 minutes) while the buyer is on the payment
page, so a payment can succeed after its seats went back on sale. With choreography, inventory, payment and order
would each need to know about that race; with orchestration only the order saga does.

## Decision

`OrderSaga` in the order service orchestrates every order after its seats are held:

```
RESERVED --payment opened--> AWAITING_PAYMENT --paid, seats confirmed--> CONFIRMED
                                 |  payment failed / expired: release seats --> FAILED / EXPIRED
                                 |  seats expired: cancel payment ------------> EXPIRED
                                 |  paid but seats gone: refund --------------> REFUNDED
                                 |  buyer cancels: cancel payment, release ---> CANCELLED
```

- **State lives in the order row** (`ticket_order.status`, optimistic locking). A restart resumes where it stopped;
  there is no separate saga log to keep consistent.
- **Requests that need an immediate answer are synchronous REST**: reserve seats (the buyer must know at once if the
  show is sold out) and open the payment (the buyer needs the payment URL). Order calls them as a service
  (client-credentials token, `ROLE_SERVICE`), never with the buyer's token.
- **Outcomes that arrive later are Kafka events**: `payment.result.v1` from payment, `inventory.hold-expired.v1`
  from inventory. Both are published through a transactional outbox (ADR 0004).
- **Every remote step is idempotent at the callee** (reserve and create-payment are keyed by order id; confirm,
  release, cancel and refund are no-ops when already done), so the saga can repeat a step after a redelivered event
  or a retry (Resilience4j) without side effects.
- **Steps that call other services run outside any database transaction.** Afterwards the state change, the record
  that the event was processed, and the outgoing `order.events.v1` event commit together.

## Consequences

- One place answers "what happens if…": `OrderSaga` and `OrderSagaIT`, which exercises each branch, including a
  payment that succeeds after the seats expired (refunded) and redelivered results (applied once).
- Order depends on inventory's and payment's request APIs; adding a participant means changing the orchestrator.
- Synchronous steps make order sensitive to slow dependencies. Timeouts, retries, circuit breakers and bulkheads
  (Resilience4j) bound that; if payment is down at checkout the seats are released and the order fails fast.
- A paid order whose seats expired is refunded rather than honoured. Confirming seats that were already sold to
  someone else would oversell (ADR 0003), which is worse than a refund.
