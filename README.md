# ledger

A double-entry accounting service in Clojure on Datomic Pro. Clone, bring up
the stack, and you have something close to a real production layout to read.

Layout:

- `logic/` pure domain (money, postings, entries, transfers)
- `controllers/` orchestration
- `diplomat/` adapters for Datomic, HTTP, JSON
- `components/` lifecycle and wiring

## Quickstart

Prerequisites: Docker, Docker Compose, a Datomic Pro license key.

```
export DATOMIC_LICENSE_KEY="..."
docker compose up
curl localhost:8080/health
```

The stack brings up Postgres, the Datomic transactor, and the app.

## Local development

```
clj -M:dev:repl
```

Then in the REPL:

```
(user/start)
(user/reset)
(user/open-portal)
```

## Tests

```
clj -X:test
clj -X:test:coverage
clj -M:long-test -X:test
```

## Docs

`docs/` covers architecture, schema growth, transactions, queries, testing,
operations, and the upgrade path for the migrator and validation library.
