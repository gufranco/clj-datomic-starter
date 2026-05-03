(ns org.acme.ledger.helpers.datomock
  "In-memory Datomic peer connection helper. Named `datomock` for symmetry
   with the project's reference plan; the implementation uses datomic:mem://
   because datomock 0.2.2 lags behind newer Datomic peer interface methods.
   Forking semantics are emulated via fresh-conn + replay of base facts."
  (:require
   [datomic.api :as d]
   [org.acme.ledger.diplomat.datomic.schema :as schema]))

(def schema-files
  ["schema/0001-account.edn"
   "schema/0002-posting.edn"
   "schema/0003-entry.edn"
   "schema/0004-transfer.edn"
   "schema/0005-tx-metadata.edn"
   "schema/0006-composite-tuples.edn"
   "schema/0007-attribute-predicates.edn"
   "schema/0008-entity-specs.edn"])

(def tx-fn-files
  ["tx-fns/0001-post-entry.edn"])

(defn- fresh-uri []
  (str "datomic:mem://ledger-test-" (java.util.UUID/randomUUID)))

(defn fresh-conn
  "Returns a fresh in-memory connection seeded with the project's schema and
   tx-fns. Each call creates an isolated database."
  []
  (let [uri (fresh-uri)]
    (d/create-database uri)
    (let [conn (d/connect uri)]
      (doseq [f schema-files]
        @(d/transact conn (schema/read-resource f)))
      (doseq [f tx-fn-files]
        @(d/transact conn (schema/read-resource f)))
      conn)))
