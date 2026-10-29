# 0003. Prevent overselling with a conditional UPDATE, not a Redis lock

- Status: accepted
- Date: 2026-10-29
- Deciders: TicketRush team

## Context

A popular concert puts thousands of buyers on the same few rows of `ticket_stock` at the same instant.
Selling one seat more than exists is the worst failure this system can have: it means refunding a
customer who already believes they have a ticket.

The original plan was "Redis distributed lock + optimistic locking (`@Version`)". Before building it we
compared the options against the one invariant that matters:

> For every ticket type, `0 <= available_quantity <= total_quantity`, at every moment.

| Option | How it works | Problems |
|---|---|---|
| Read, check, write in Java | `SELECT available`, compare, `UPDATE available = x` | Lost updates under concurrency: two buyers read 1, both write 0, two seats sold |
| Optimistic locking only | Same, with `@Version`; losers get `OptimisticLockException` and retry | Correct, but under contention almost every request fails and retries; throughput collapses on hot rows |
| Redis lock (Redisson) around read-check-write | Take `lock:stock:{id}`, then read-check-write | Correct only if the lock is correct: lease expiry during a GC pause lets two holders in. Adds a network hop per item and a second system that must be up to sell anything |
| `SELECT ... FOR UPDATE` | Pessimistic row lock, then write | Correct, but holds the lock across application code and makes it easy to deadlock multi-item orders |
| **Conditional UPDATE** | `UPDATE ticket_stock SET available = available - :q WHERE id = :id AND available >= :q` | The check and the write are one statement. PostgreSQL serializes writers on the row lock for the duration of a single statement |

## Decision

1. **The decrement is a single conditional UPDATE** (`TicketStockRepository.tryTake`). One affected row
   means the seats were taken, zero means sold out. No application lock, no read-then-write.
2. **Multi-item orders are all-or-nothing** inside one transaction; any shortfall throws and rolls back the
   seats already taken. Rows are always updated **in ascending id order**, so two orders that want the same
   ticket types cannot deadlock each other.
3. **The database enforces the invariant itself**: `CHECK (available_quantity >= 0 AND available_quantity <= total_quantity)`.
   A bug elsewhere in the code fails loudly instead of overselling.
4. **`@Version` stays**, but only for admin edits of a stock row, so an edit based on a stale read gets a 409
   instead of overwriting reservations made in the meantime. Every conditional UPDATE also bumps the version.
5. **Idempotency**: unique `(order_id, stock_id)` on `stock_hold`. A retried reserve call returns the existing
   holds; the same order asking for different seats is rejected.
6. **Expiry** uses `SELECT ... FOR UPDATE SKIP LOCKED` in batches, so several inventory instances can run the
   job at once without blocking each other or releasing a hold twice.

Redis is still used where it fits: the catalog read cache and (later) the gateway rate limiter and waiting room.

## Evidence

`ReservationServiceIT`, against real PostgreSQL via Testcontainers, with real concurrent transactions
(no test-managed transaction, a 20-connection pool so requests actually race):

| Scenario | Result |
|---|---|
| 200 buyers, 50 seats, all start at the same instant | exactly 50 holds, 150 sold out, `available = 0` |
| 100 two-item orders, half of them listing items in reverse order | no deadlock, exactly 30 succeed, both stocks at 0 |
| Same order reserved twice | one set of holds, stock decremented once |
| Hold past its 10 minutes | released exactly once, can no longer be confirmed |

Mutation check: removing `AND available >= :quantity` from the UPDATE makes three of these tests fail, and
PostgreSQL rejects the oversell with `ck_ticket_stock__available_in_range`. Both layers catch it independently.

## Consequences

- Correctness depends only on PostgreSQL, which we already need. Selling keeps working if Redis is down.
- The hot row is still a serialization point: throughput per ticket type is bounded by how fast PostgreSQL
  commits single-row updates. The phase 4 k6 test measures this; if it becomes the bottleneck, the next steps
  are the gateway waiting room (to cap concurrent buyers) and splitting a ticket type's stock into several
  rows (buckets), not a distributed lock.
- Admin edits racing with sales get a 409 and must be retried.
