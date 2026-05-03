(ns org.acme.ledger.controllers.statements
  (:require
   [datomic.api :as d]
   [org.acme.ledger.adapters.inputs :as inputs]
   [org.acme.ledger.components.datomic :as datomic]
   [org.acme.ledger.diplomat.datomic.query :as query]))

(defn- db-at [conn {:keys [latest as-of t]}]
  (cond
    latest (d/db conn)
    as-of  (d/as-of (d/db conn) as-of)
    t      (d/as-of (d/db conn) t)
    :else  (d/db conn)))

(defn balance
  [{:keys [datomic request]}]
  (let [account-id (some-> (get-in request [:path-params :account-id])
                           java.util.UUID/fromString)
        selector   (or (:validated-body request) (:body request) {:latest true})
        v          (inputs/validate inputs/time-selector selector)]
    (if (:ok v)
      (let [conn (datomic/conn datomic)
            db'  (db-at conn (:ok v))]
        {:ok {:account-id account-id
              :balance    (query/balance-as-of db' account-id)
              :basis-t    (d/basis-t db')}})
      v)))

(defn movements
  [{:keys [datomic request]}]
  (let [account-id (some-> (get-in request [:path-params :account-id])
                           java.util.UUID/fromString)
        body       (or (:validated-body request) (:body request) {})
        from       (or (:from body) (java.util.Date. 0))
        to         (or (:to body)   (java.util.Date.))
        rows       (query/movements-between (datomic/db datomic) account-id from to)]
    {:ok {:account-id account-id :rows rows}}))
