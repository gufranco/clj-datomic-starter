(ns org.acme.ledger.controllers.accounts
  (:require
   [datomic.api :as d]
   [org.acme.ledger.adapters.anomalies :as anom]
   [org.acme.ledger.components.datomic :as datomic]
   [org.acme.ledger.diplomat.datomic.query :as query]
   [org.acme.ledger.diplomat.datomic.tx :as tx]))

(defn open!
  "Open a new account. `cmd` keys: :customer-id :number :currency :kind."
  [{:keys [datomic request]}]
  (let [{:keys [customer-id number currency kind correlation-id actor]
         :or   {actor "system"}} (or (:validated-body request) (:body request) request)
        conn      (datomic/conn datomic)
        account-id (java.util.UUID/randomUUID)
        tx-data   [{:account/id            account-id
                    :account/customer-id   customer-id
                    :account/number        number
                    :account/currency      currency
                    :account/kind          kind
                    :account/status        :open
                    :account/balance-minor 0
                    :account/opened-at     (java.util.Date.)}]]
    (try
      (tx/transact! conn tx-data
                    {:correlation-id (or correlation-id (str (java.util.UUID/randomUUID)))
                     :actor          actor
                     :source         :http})
      {:created (query/account-by-id (d/db conn) account-id)}
      (catch Exception e
        (anom/anom :cognitect.anomalies/conflict
                   (.getMessage e)
                   {:account-number number :customer-id customer-id})))))

(defn close!
  [{:keys [datomic request]}]
  (let [{:keys [account-id correlation-id actor] :or {actor "system"}}
        (or (:validated-body request) (:body request) request)
        conn (datomic/conn datomic)]
    (tx/transact! conn
                  [{:account/id      account-id
                    :account/status  :closed
                    :account/closed-at (java.util.Date.)}]
                  {:correlation-id (or correlation-id (str (java.util.UUID/randomUUID)))
                   :actor          actor
                   :source         :http})
    {:ok (query/account-by-id (datomic/db datomic) account-id)}))

(defn list-by-customer
  [{:keys [datomic request]}]
  (let [customer-id (some-> (get-in request [:path-params :customer-id])
                            java.util.UUID/fromString)]
    {:ok (query/accounts-of-customer (datomic/db datomic) customer-id)}))
