(ns seed
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [datomic.api :as d]
   [org.acme.ledger.diplomat.datomic.tx :as tx]))

(defn load-seed! [conn]
  (let [data (edn/read-string (slurp (io/resource "seed/dev.edn")))]
    (tx/transact! conn data
                  {:correlation-id (str (java.util.UUID/randomUUID))
                   :actor          "seed"
                   :source         :seed})))
