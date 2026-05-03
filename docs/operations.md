# Operations

## JVM flags

The transactor and the app run with G1GC, a 50 ms pause target, and `-XX:+ExitOnOutOfMemoryError`. The compose file sets the heap explicitly: 1 GB for the transactor, 1 GB for the app. Adjust both `-Xms` and `-Xmx` to the same value to avoid heap resize pauses under load.

## Object cache

`object-cache-max=512m` is set explicitly in `docker/transactor.properties` rather than relying on the 50% default. Sizing rule of thumb: leave at least 25% of heap for working set; raise the cache when datom traffic is read-heavy.

## Storage

The default stack uses Postgres for storage, configured via JDBC. The transactor is the only writer; peers are read-only against storage. The compose `postgres-init.sql` creates the `datomic_kvs` table the transactor expects.

## Backup and restore

Backups are produced with the `bin/datomic backup-db` and `bin/datomic restore-db` commands shipped with the Datomic distribution. The connection URI for backup must be the same SQL URI the transactor uses. Schedule backups out-of-band; do not bake them into compose.

## Transactor supervisor

The compose file does not provide a supervisor. Production should run the transactor under systemd or an orchestrator that restarts on crash and on storage timeouts. The transactor self-kills when storage is unreachable for longer than the configured threshold; that exit must be turned into a restart, not a stop.

## Health and readiness

The app exposes `/health`. The endpoint requires a current database value and returns 503 if the peer cannot read storage. Pair `/health` with a Prometheus scrape against `/metrics` (port 9090) for liveness and saturation visibility.
