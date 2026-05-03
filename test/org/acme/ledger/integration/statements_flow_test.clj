(ns org.acme.ledger.integration.statements-flow-test
  (:require
   [clojure.test :refer [deftest is]]
   [datomic.api :as d]
   [org.acme.ledger.diplomat.datomic.tx :as tx]
   [org.acme.ledger.helpers.datomock :as datomock]))

(deftest as-of-sees-historical-balance
  (let [conn (datomock/fresh-conn)
        aid  (java.util.UUID/randomUUID)
        meta {:correlation-id (str (java.util.UUID/randomUUID))
              :actor "test" :source :test}]
    (tx/transact! conn
                  [{:account/id            aid
                    :account/customer-id   (java.util.UUID/randomUUID)
                    :account/number        "9001"
                    :account/currency      "USD"
                    :account/kind          :asset
                    :account/status        :open
                    :account/balance-minor 0
                    :account/opened-at     (java.util.Date.)}]
                  meta)
    (let [t-before (d/basis-t (d/db conn))]
      (tx/transact! conn
                    [[:db/cas [:account/id aid] :account/balance-minor 0 5000]]
                    meta)
      (is (= 0    (:account/balance-minor
                   (d/pull (d/as-of (d/db conn) t-before)
                           [:account/balance-minor] [:account/id aid]))))
      (is (= 5000 (:account/balance-minor
                   (d/pull (d/db conn) [:account/balance-minor] [:account/id aid])))))))
