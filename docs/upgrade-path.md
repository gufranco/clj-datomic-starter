# Upgrade Path

## Migration runner

The default runner is in-tree: a small component that reads numbered EDN files and records `:migration/applied` facts. To swap to `gethop-dev/stork`:

1. Add `gethop-dev/stork` to `deps.edn`.
2. Replace `components/migrator.clj` with the stork integration. The schema files do not change; stork uses the same numbered-EDN convention.
3. Drop the bootstrap norm in `migrator.clj` once stork's own tracking is in place.

## Boundary validation

The default validator is Malli. Teams accustomed to the older Plumatic Schema idiom can migrate as follows:

1. Add `prismatic/schema` to `deps.edn`.
2. Translate the route schemas in `diplomat/http_server/routes.clj` into Plumatic's `s/defschema` shape.
3. Replace the Malli interceptor in `interceptors.clj` with one that calls `s/validate`.

The boundary contract is identical; only the schema language changes.

## Containerization

The default uberjar is built with `tools.build` and consumed by the Dockerfile.
Swap in any layered-image builder (jib, vessel, ko-style) by replacing the
`build` target in `build.clj` with one that emits an OCI image directly and
adjusting the Dockerfile to consume it.
