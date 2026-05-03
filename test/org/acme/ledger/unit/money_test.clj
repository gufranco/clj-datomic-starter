(ns org.acme.ledger.unit.money-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [clojure.test.check.clojure-test :refer [defspec]]
   [clojure.test.check.generators :as gen]
   [clojure.test.check.properties :as prop]
   [org.acme.ledger.logic.money :as money]))

(deftest constructor-rejects-bad-input
  (testing "non-integer minor"
    (is (thrown? Exception (money/money 1.5 "USD"))))
  (testing "negative minor"
    (is (thrown? Exception (money/money -1 "USD"))))
  (testing "wrong currency length"
    (is (thrown? Exception (money/money 100 "US")))))

(defspec add-is-commutative 100
  (prop/for-all
   [a (gen/large-integer* {:min 0 :max 1e9})
    b (gen/large-integer* {:min 0 :max 1e9})]
   (= (money/add (money/money a "USD") (money/money b "USD"))
      (money/add (money/money b "USD") (money/money a "USD")))))

(defspec add-respects-currency 100
  (prop/for-all
   [a (gen/large-integer* {:min 0 :max 1e9})
    b (gen/large-integer* {:min 0 :max 1e9})]
   (= (+ a b)
      (:minor (money/add (money/money a "USD") (money/money b "USD"))))))
