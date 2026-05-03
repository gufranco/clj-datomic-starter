(ns org.acme.ledger.diplomat.datomic.schema
  "Helpers to load schema and tx-fn EDN from the classpath. Used by tests
   and by the migrator component."
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [datomic.function :as df]))

(def ^:private readers
  {'db/fn df/construct})

(defn read-resource [path]
  (with-open [r (io/reader (io/resource path))]
    (edn/read {:readers readers} (java.io.PushbackReader. r))))
