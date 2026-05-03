(ns org.acme.ledger.unit.json-test
  (:require
   [cheshire.core :as json]
   [clojure.test :refer [deftest is testing]]
   [org.acme.ledger.diplomat.http-server.json :as json-encoders]))

(deftest install!-registers-encoders
  (json-encoders/install!)
  (testing "UUID encodes to its string form"
    (let [u (java.util.UUID/randomUUID)]
      (is (= (str "\"" u "\"") (json/generate-string u)))))
  (testing "Date encodes to ISO instant"
    (let [d (java.util.Date. 0)
          s (json/generate-string d)]
      (is (re-find #"1970-01-01T00:00:00Z" s))))
  (testing "Instant encodes to ISO instant"
    (let [i (java.time.Instant/ofEpochMilli 0)
          s (json/generate-string i)]
      (is (re-find #"1970-01-01T00:00:00Z" s)))))

(deftest read-json-converts-snake-to-kebab
  (testing "scalar keys"
    (is (= {:customer-id "u-1" :account-number "A-100"}
           (json-encoders/read-json
            "{\"customer_id\":\"u-1\",\"account_number\":\"A-100\"}"))))
  (testing "nested maps"
    (is (= {:outer-key {:inner-key 1}}
           (json-encoders/read-json
            "{\"outer_key\":{\"inner_key\":1}}"))))
  (testing "nil input is nil"
    (is (nil? (json-encoders/read-json nil)))))

(deftest write-json-converts-kebab-to-snake
  (testing "keyword keys"
    (is (= "{\"customer_id\":\"u-1\"}"
           (json-encoders/write-json {:customer-id "u-1"}))))
  (testing "string keys"
    (is (= "{\"customer_id\":\"u-1\"}"
           (json-encoders/write-json {"customer-id" "u-1"}))))
  (testing "round-trip preserves data"
    (let [m {:customer-id "u" :nested {:inner-field 42}}]
      (is (= m (json-encoders/read-json (json-encoders/write-json m)))))))
