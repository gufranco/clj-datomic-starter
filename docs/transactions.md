# Transactions

## Mandatory metadata

Every transaction carries three attributes on the `datomic.tx` entity:

- `:tx/correlation-id` propagated from the originating HTTP request.
- `:tx/actor` the authenticated principal that authored the change.
- `:tx/source` an enum: `:http`, `:job`, `:seed`, `:test`, `:migration`.

The wrapper in `diplomat/datomic/tx.clj` refuses to commit if any of the three is missing.

## Compare-and-set

Account balances are mutated only via `:db/cas`. The classpath tx-fn `:ledger/post-entry` reads the current balance, computes the new value from the entry's postings, and emits one cas per affected account in a single transaction.

## Lookup refs

Cross-entity references use `[:account/id ...]` lookup refs instead of pre-resolving entids. This keeps tx-data declarative and allows the same data to be replayed against a forked or seeded database.

## Transaction functions

Classpath tx-fns live in `org.acme.ledger.diplomat.datomic.tx-fns`. They are referenced from EDN under `resources/tx-fns/` via the `#db/fn` reader and are loaded by the migrator. The body must be deterministic and side-effect-free: only `d/entid`, `d/entity`, and `d/q` are safe to call from inside a tx-fn.

## Same-tx hazards

Datomic Pro 1.0.7075 (per the Jepsen analysis) clarified that lookup refs and `:db/cas` ops within a single transaction resolve against the database value before the transaction starts. The implications for this codebase:

- Do not chain two `:db/cas` ops on the same attribute in one transaction. The second would see the pre-tx value, not the value the first cas would have set.
- When two cas ops in the same transaction must both succeed, they must target different attributes or different entities.
- `post-entry` honors this by emitting one cas per account, never two against the same account in the same transaction.
