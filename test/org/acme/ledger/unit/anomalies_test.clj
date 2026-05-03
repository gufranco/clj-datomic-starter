(ns org.acme.ledger.unit.anomalies-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [org.acme.ledger.adapters.anomalies :as anom]))

(deftest status-for-mapping
  (testing "known categories map to documented HTTP statuses"
    (is (= 503 (anom/status-for {:cognitect.anomalies/category :cognitect.anomalies/unavailable})))
    (is (= 503 (anom/status-for {:cognitect.anomalies/category :cognitect.anomalies/interrupted})))
    (is (= 400 (anom/status-for {:cognitect.anomalies/category :cognitect.anomalies/incorrect})))
    (is (= 403 (anom/status-for {:cognitect.anomalies/category :cognitect.anomalies/forbidden})))
    (is (= 501 (anom/status-for {:cognitect.anomalies/category :cognitect.anomalies/unsupported})))
    (is (= 404 (anom/status-for {:cognitect.anomalies/category :cognitect.anomalies/not-found})))
    (is (= 409 (anom/status-for {:cognitect.anomalies/category :cognitect.anomalies/conflict})))
    (is (= 500 (anom/status-for {:cognitect.anomalies/category :cognitect.anomalies/fault})))
    (is (= 429 (anom/status-for {:cognitect.anomalies/category :cognitect.anomalies/busy}))))
  (testing "unknown or missing category falls back to 500"
    (is (= 500 (anom/status-for {})))
    (is (= 500 (anom/status-for {:cognitect.anomalies/category :nope})))))

(deftest anomaly?-test
  (is (true?  (anom/anomaly? {:cognitect.anomalies/category :cognitect.anomalies/fault})))
  (is (false? (anom/anomaly? {})))
  (is (false? (anom/anomaly? "string")))
  (is (false? (anom/anomaly? nil))))

(deftest anom-builder
  (testing "two-arg form sets category and message"
    (let [a (anom/anom :cognitect.anomalies/incorrect "bad")]
      (is (= :cognitect.anomalies/incorrect (:cognitect.anomalies/category a)))
      (is (= "bad" (:cognitect.anomalies/message a)))))
  (testing "three-arg form merges extra data"
    (let [a (anom/anom :cognitect.anomalies/conflict "dup" {:k 1})]
      (is (= 1 (:k a)))
      (is (= "dup" (:cognitect.anomalies/message a))))))
