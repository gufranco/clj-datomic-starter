# Queries

## Pull templates

The pull patterns in `diplomat/datomic/query.clj` are the single source of truth for what each surface returns. Controllers compose them; they never spell out attribute lists inline.

| Template | Returns |
|----------|---------|
| `account-pull` | account header and current balance |
| `posting-pull` | single posting, plus the account it affects |
| `entry-pull` | entry plus all of its component postings |
| `transfer-pull` | transfer header plus references to source, destination, and entry |

## Datalog rules

Account hierarchies, when introduced, use the `parents-of` rule defined in `query.clj`. Recursive rules let callers traverse arbitrary depths without hand-rolled joins.

## Time travel

- `d/as-of` for a frozen historical view: balance at a given instant or basis-T.
- `d/since` for "what changed after T".
- `d/with` for speculative writes: "what if a chargeback posted now?". The speculative database is discarded after the call.
- `d/history` for audit reads that span all assertions and retractions.

## Clause ordering

Clauses are ordered by selectivity per Harper's optimization rules: bind the most selective leading variable first, push range and predicate clauses to the bottom. The query helper in `connection.clj` exposes `query-stats` to time queries during development.

## Query stats

`(query/query-stats query db & args)` returns `{:result ... :duration-ms ...}`. Wrap any new query during development to confirm it is below the project's latency budget before committing.
