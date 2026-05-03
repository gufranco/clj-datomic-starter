(ns org.acme.ledger.unit.transfer-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [org.acme.ledger.logic.money :as money]
   [org.acme.ledger.logic.transfer :as transfer]))

(deftest transfer->entry-builds-balanced-postings
  (let [from (java.util.UUID/randomUUID)
        to   (java.util.UUID/randomUUID)
        amt  (money/money 1000 "USD")
        e    (transfer/transfer->entry
              {:from-account-id from :to-account-id to :amount amt :memo "x"})]
    (is (= 2 (count (:postings e))))
    (is (= "x" (:memo e)))
    (is (uuid? (:entry-id e)))))

(deftest transfer->entry-rejects-non-positive
  (let [a (java.util.UUID/randomUUID) b (java.util.UUID/randomUUID)]
    (is (thrown? Exception
                 (transfer/transfer->entry
                  {:from-account-id a :to-account-id b
                   :amount (money/money 0 "USD")})))))

(deftest transfer->entry-rejects-self-transfer
  (let [a (java.util.UUID/randomUUID)]
    (is (thrown? Exception
                 (transfer/transfer->entry
                  {:from-account-id a :to-account-id a
                   :amount (money/money 100 "USD")})))))

(deftest money-helpers
  (testing "subtract"
    (is (= 50 (:minor (money/subtract (money/money 150 "USD") (money/money 100 "USD"))))))
  (testing "subtract rejects mismatched currency"
    (is (thrown? Exception
                 (money/subtract (money/money 100 "USD") (money/money 100 "EUR")))))
  (testing "zero? and positive?"
    (is (true?  (money/zero? (money/money 0 "USD"))))
    (is (false? (money/zero? (money/money 1 "USD"))))
    (is (true?  (money/positive? (money/money 1 "USD"))))
    (is (false? (money/positive? (money/money 0 "USD")))))
  (testing "same-currency?"
    (is (true?  (money/same-currency? (money/money 1 "USD") (money/money 2 "USD"))))
    (is (false? (money/same-currency? (money/money 1 "USD") (money/money 2 "EUR"))))))
