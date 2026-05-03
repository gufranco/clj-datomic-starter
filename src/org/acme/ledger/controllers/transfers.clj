(ns org.acme.ledger.controllers.transfers
  (:require
   [datomic.api :as d]
   [org.acme.ledger.adapters.anomalies :as anom]
   [org.acme.ledger.components.datomic :as datomic]
   [org.acme.ledger.diplomat.datomic.tx :as tx]
   [org.acme.ledger.diplomat.datomic.tx-fns :as tx-fns]
   [org.acme.ledger.logic.money :as money]
   [org.acme.ledger.logic.transfer :as transfer]))

(defn- entry->tx-fn-arg [entry]
  {:entry-id    (:entry-id entry)
   :postings    (:postings entry)
   :memo        (:memo entry)
   :occurred-at (:occurred-at entry)})

(defn transfer!
  [{:keys [datomic request]}]
  (let [{:keys [from-account-id to-account-id amount-minor currency memo
                correlation-id actor]
         :or   {actor "system"}}
        (or (:validated-body request) (:body request) request)
        conn (datomic/conn datomic)]
    (try
      (let [amount     (money/money amount-minor currency)
            entry      (transfer/transfer->entry
                        {:from-account-id from-account-id
                         :to-account-id   to-account-id
                         :amount          amount
                         :memo            memo})
            transfer-id  (java.util.UUID/randomUUID)
            entry-tempid (str "ledger.entry/" (:entry-id entry))
            entry-tx     (tx-fns/post-entry-impl
                          (d/db conn)
                          (assoc (entry->tx-fn-arg entry)
                                 :entry-tempid entry-tempid))
            tx-data      (conj (vec entry-tx)
                               {:transfer/id           transfer-id
                                :transfer/from         [:account/id from-account-id]
                                :transfer/to           [:account/id to-account-id]
                                :transfer/amount-minor amount-minor
                                :transfer/currency     currency
                                :transfer/memo         (or memo "")
                                :transfer/entry        entry-tempid})]
        (tx/transact! conn tx-data
                      {:correlation-id (or correlation-id (str (java.util.UUID/randomUUID)))
                       :actor          actor
                       :source         :http})
        {:created (d/pull (d/db conn)
                          '[:transfer/id :transfer/amount-minor :transfer/currency :transfer/memo]
                          [:transfer/id transfer-id])})
      (catch clojure.lang.ExceptionInfo e
        (anom/anom :cognitect.anomalies/incorrect
                   (.getMessage e)
                   (ex-data e)))
      (catch Exception e
        (anom/anom :cognitect.anomalies/fault (.getMessage e))))))
