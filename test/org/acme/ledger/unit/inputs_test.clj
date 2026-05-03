(ns org.acme.ledger.unit.inputs-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [org.acme.ledger.adapters.inputs :as inputs]))

(deftest time-selector-validation
  (testing "valid selectors return :ok"
    (is (= {:ok {:latest true}} (inputs/validate inputs/time-selector {:latest true})))
    (is (contains? (inputs/validate inputs/time-selector {:as-of (java.util.Date.)}) :ok))
    (is (contains? (inputs/validate inputs/time-selector {:t 42}) :ok)))
  (testing "more than one selector is rejected"
    (let [r (inputs/validate inputs/time-selector {:latest true :t 1})]
      (is (= :cognitect.anomalies/incorrect (:cognitect.anomalies/category r)))))
  (testing "no selector is rejected"
    (let [r (inputs/validate inputs/time-selector {})]
      (is (= :cognitect.anomalies/incorrect (:cognitect.anomalies/category r)))))
  (testing "wrong types are rejected"
    (let [r (inputs/validate inputs/time-selector {:t "string"})]
      (is (= :cognitect.anomalies/incorrect (:cognitect.anomalies/category r))))))
