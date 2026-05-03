(ns org.acme.ledger.integration.transfers-flow-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [datomic.api :as d]
   [org.acme.ledger.diplomat.datomic.tx :as tx]
   [org.acme.ledger.helpers.datomock :as datomock]))

(defn- meta-stub [] {:correlation-id (str (java.util.UUID/randomUUID))
                     :actor          "test"
                     :source         :test})

(defn- open-account! [conn aid customer-id number]
  (tx/transact! conn
                [{:account/id            aid
                  :account/customer-id   customer-id
                  :account/number        number
                  :account/currency      "USD"
                  :account/kind          :asset
                  :account/status        :open
                  :account/balance-minor 0
                  :account/opened-at     (java.util.Date.)}]
                (meta-stub)))

(deftest transfer-moves-funds
  (testing "after a transfer, source decreases and destination increases by the same amount"
    (let [conn        (datomock/fresh-conn)
          customer-id (java.util.UUID/randomUUID)
          a1          (java.util.UUID/randomUUID)
          a2          (java.util.UUID/randomUUID)]
      (open-account! conn a1 customer-id "1001")
      (open-account! conn a2 customer-id "1002")
      ;; seed the source with a credit so we can transfer 500 minor units out
      (tx/transact! conn
                    [[:db/cas [:account/id a1] :account/balance-minor 0 1000]]
                    (meta-stub))
      (tx/transact! conn
                    [[:ledger/post-entry
                      {:entry-id    (java.util.UUID/randomUUID)
                       :postings    [{:posting-id (java.util.UUID/randomUUID)
                                      :account-id a2 :side :debit
                                      :amount-minor 500 :currency "USD"}
                                     {:posting-id (java.util.UUID/randomUUID)
                                      :account-id a1 :side :credit
                                      :amount-minor 500 :currency "USD"}]
                       :memo        "test transfer"
                       :occurred-at (java.util.Date.)}]]
                    (meta-stub))
      (let [db (d/db conn)]
        (is (= 500 (:account/balance-minor (d/pull db [:account/balance-minor] [:account/id a1]))))
        (is (= 500 (:account/balance-minor (d/pull db [:account/balance-minor] [:account/id a2]))))))))
