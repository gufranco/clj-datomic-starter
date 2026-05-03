(ns org.acme.ledger.diplomat.datomic.connection
  (:require
   [datomic.api :as d]))

(defn connect [uri] (d/connect uri))
(defn release [conn] (d/release conn))
(defn db      [conn] (d/db conn))

(defn query-stats
  "Run query with timing. Returns {:result ... :duration-ms ...}."
  [query db & args]
  (let [start  (System/nanoTime)
        result (apply d/q query db args)
        ms     (/ (- (System/nanoTime) start) 1e6)]
    {:result result :duration-ms ms}))
