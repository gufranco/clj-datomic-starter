(ns org.acme.ledger.adapters.anomalies
  "Mapping table from Cognitect anomaly categories to HTTP status codes.")

(def category->status
  {:cognitect.anomalies/unavailable    503
   :cognitect.anomalies/interrupted    503
   :cognitect.anomalies/incorrect      400
   :cognitect.anomalies/forbidden      403
   :cognitect.anomalies/unsupported    501
   :cognitect.anomalies/not-found      404
   :cognitect.anomalies/conflict       409
   :cognitect.anomalies/fault          500
   :cognitect.anomalies/busy           429})

(defn status-for [anomaly]
  (or (category->status (:cognitect.anomalies/category anomaly))
      500))

(defn anomaly?
  [x]
  (and (map? x) (contains? x :cognitect.anomalies/category)))

(defn anom
  ([category msg]      (anom category msg nil))
  ([category msg data]
   (cond-> {:cognitect.anomalies/category category
            :cognitect.anomalies/message  msg}
     data (merge data))))
