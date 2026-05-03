(ns org.acme.ledger.components.routes
  (:require
   [com.stuartsierra.component :as component]))

(defrecord Routes [datomic metrics service-map-var deps]
  component/Lifecycle
  (start [this]
    (let [v (or service-map-var
                (requiring-resolve 'org.acme.ledger.diplomat.http-server.routes/service-map))
          d {:datomic datomic :metrics metrics}]
      (assoc this
             :service-map-var v
             :deps            d)))
  (stop [this]
    (assoc this :deps nil)))

(defn build-service [{:keys [service-map-var deps]}]
  ((deref service-map-var) deps))

(defn new-routes []
  (->Routes nil nil nil nil))
