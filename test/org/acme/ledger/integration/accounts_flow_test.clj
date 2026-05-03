(ns org.acme.ledger.integration.accounts-flow-test
  (:require
   [clojure.test :refer [deftest is]]
   [org.acme.ledger.diplomat.datomic.tx :as tx]
   [org.acme.ledger.helpers.datomock :as datomock]))

(deftest customer-number-uniqueness
  (let [conn        (datomock/fresh-conn)
        customer-id (java.util.UUID/randomUUID)
        a1          (java.util.UUID/randomUUID)
        a2          (java.util.UUID/randomUUID)
        meta        {:correlation-id (str (java.util.UUID/randomUUID))
                     :actor "test" :source :test}]
    (tx/transact! conn
                  [{:account/id            a1
                    :account/customer-id   customer-id
                    :account/number        "1001"
                    :account/currency      "USD"
                    :account/kind          :asset
                    :account/status        :open
                    :account/balance-minor 0
                    :account/opened-at     (java.util.Date.)}]
                  meta)
    (is (thrown?
         Exception
         (tx/transact! conn
                       [{:account/id            a2
                         :account/customer-id   customer-id
                         :account/number        "1001"
                         :account/currency      "USD"
                         :account/kind          :asset
                         :account/status        :open
                         :account/balance-minor 0
                         :account/opened-at     (java.util.Date.)}]
                       meta)))))
