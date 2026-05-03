(ns org.acme.ledger.unit.posting-test
  (:require
   [clojure.test :refer [deftest is]]
   [org.acme.ledger.logic.money :as money]
   [org.acme.ledger.logic.posting :as posting]))

(deftest debit-and-credit-have-opposite-signs
  (let [aid (java.util.UUID/randomUUID)
        m   (money/money 100 "USD")
        d   (posting/debit  {:account-id aid :amount m})
        c   (posting/credit {:account-id aid :amount m})]
    (is (= 100  (posting/signed-amount d)))
    (is (= -100 (posting/signed-amount c)))))
