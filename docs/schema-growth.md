# Schema Growth

Halloway's ten rules of schema growth, applied to this codebase.

| Rule | Application here |
|------|------------------|
| Grow your schema, never break it | Schema norms are append-only files in `resources/schema/`. Renaming an attribute is forbidden; aliases are added instead. |
| Never remove a name | Removed concepts are deprecated, not deleted. The attribute stays, with a `:db/doc` describing the deprecation. |
| Never reuse a name | Once `:account/status` exists, no other meaning ever attaches to that ident. |
| Use namespaced keys | All idents live under domain namespaces (`:account/...`, `:transfer/...`, `:tx/...`). |
| Make new names additive | New variants are new idents. `:account/kind` enumerated values use the keyword set, not strings, to make additions trivial. |
| Use cardinality-many sparingly | Only `:entry/postings` is many, and it is component-owned. |
| Component pieces have one parent | `:entry/postings` is `:db/isComponent true`. Postings cannot exist outside an entry. |
| Composite uniqueness for natural keys | `:account/customer+number` enforces one account number per customer with `:db/tupleAttrs`. |
| Predicates for invariants | `:db.attr/preds` checks ISO 4217 currency codes. `:db.entity/preds` enforces debits-equal-credits via `:entry/balanced?`. |
| Migrations are facts | `:migration/applied-at` and `:migration/sha` records prove which norms ran and when. |

## File layout

Every `resources/schema/NNNN-name.edn` is loaded in lexical order by the migrator. Filenames are zero-padded for stable ordering. New norms get the next available number and are never edited after they ship.
