(ns org.acme.ledger.diplomat.datomic.tx
  "Transaction wrapper that attaches mandatory metadata."
  (:require
   [datomic.api :as d]
   [taoensso.timbre :as log]))

(defn- tx-meta-datom
  [{:keys [correlation-id actor source]}]
  (cond-> {:db/id "datomic.tx"}
    correlation-id (assoc :tx/correlation-id correlation-id)
    actor          (assoc :tx/actor actor)
    source         (assoc :tx/source source)))

(defn transact!
  "Transact `tx-data` with mandatory metadata. Returns the transaction report.
   `meta` must include at least :correlation-id, :actor, :source."
  [conn tx-data {:keys [correlation-id actor source] :as meta}]
  (when-not (and correlation-id actor source)
    (throw (ex-info "Transaction metadata is mandatory: correlation-id, actor, source"
                    {:meta meta})))
  (let [start (System/nanoTime)
        full  (conj (vec tx-data) (tx-meta-datom meta))
        rpt   @(d/transact conn full)
        ms    (/ (- (System/nanoTime) start) 1e6)]
    (log/info {:event :tx/committed :correlation-id correlation-id
               :source source :duration-ms ms})
    rpt))
