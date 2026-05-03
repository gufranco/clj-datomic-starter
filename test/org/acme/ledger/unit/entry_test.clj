(ns org.acme.ledger.unit.entry-test
  (:require
   [clojure.test :refer [deftest is]]
   [clojure.test.check.clojure-test :refer [defspec]]
   [clojure.test.check.generators :as gen]
   [clojure.test.check.properties :as prop]
   [org.acme.ledger.logic.entry :as entry]
   [org.acme.ledger.logic.money :as money]
   [org.acme.ledger.logic.posting :as posting]))

(defn- balanced-pair [amount]
  (let [m  (money/money amount "USD")
        a1 (java.util.UUID/randomUUID)
        a2 (java.util.UUID/randomUUID)]
    [(posting/debit  {:account-id a1 :amount m})
     (posting/credit {:account-id a2 :amount m})]))

(deftest balanced-pair-is-balanced
  (is (entry/balanced? (balanced-pair 100))))

(deftest unbalanced-throws
  (is (thrown? Exception
               (entry/entry {:postings [(posting/debit {:account-id (java.util.UUID/randomUUID)
                                                        :amount     (money/money 100 "USD")})]}))))

(defspec entries-are-always-balanced 100
  (prop/for-all
   [amount (gen/large-integer* {:min 1 :max 1e9})]
   (entry/balanced? (balanced-pair amount))))
