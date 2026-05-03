# Testing

## Layers

- **Unit tests** in `test/org/acme/ledger/unit/` exercise pure functions in `logic/`. They use `clojure.test.check` for properties and never touch a connection.
- **Integration tests** in `test/org/acme/ledger/integration/` exercise controllers and tx-fns against an in-memory connection produced by `datomock`. The connection is seeded with the project's schema and tx-fns by `helpers/datomock/fresh-conn`.
- **Generative invariant tests** drive arbitrary command sequences (open, transfer, reverse) and assert that the global invariant holds: across all accounts, sum of debits equals sum of credits.

## Tools

- `state-flow` for diplomat-level integration tests with composable steps.
- `matcher-combinators` for assertions that match shape rather than equality.
- `datomock` for fork-a-connection patterns: each test sets up a base world and forks per scenario.
- `mockfn` only when crossing into truly external systems (Kafka producers, third-party HTTP). No mocks of internal infrastructure.

## Quick-check budget

Default property tests cap at 100 cases. The `:long-test` alias enables the `ledger.test.long` flag, which raises the cap to 1000 for invariant tests.

## Running

```
clj -X:test
clj -X:test:coverage
clj -M:long-test -X:test
```
