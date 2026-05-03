(ns org.acme.ledger.unit.handlers-test
  (:require
   [cheshire.core :as json]
   [clojure.test :refer [deftest is testing]]
   [org.acme.ledger.diplomat.http-server.handlers :as handlers]))

(defn- run [controller ctx]
  (let [interceptor (handlers/make-handler controller)
        result      ((:enter interceptor) ctx)]
    (:response result)))

(deftest ok-result-becomes-200
  (let [resp (run (fn [_] {:ok {:hello "world"}}) {})]
    (is (= 200 (:status resp)))
    (is (= {"Content-Type" "application/json"} (:headers resp)))
    (is (= {:hello "world"} (json/parse-string (:body resp) true)))))

(deftest created-result-becomes-201
  (let [resp (run (fn [_] {:created {:id 1}}) {})]
    (is (= 201 (:status resp)))))

(deftest plain-map-becomes-200
  (let [resp (run (fn [_] {:plain "data"}) {})]
    (is (= 200 (:status resp)))))

(deftest anomaly-becomes-error-response
  (let [resp (run (fn [_] {:cognitect.anomalies/category :cognitect.anomalies/conflict
                           :cognitect.anomalies/message  "duplicate"
                           :extra "info"})
                  {})]
    (is (= 409 (:status resp)))
    (let [body (json/parse-string (:body resp) true)]
      (is (= "duplicate" (:message body)))
      (is (contains? body :details)))))

(deftest deps-flow-through-context
  (testing "controller receives context-injected deps"
    (let [resp (run (fn [{:keys [request marker]}]
                      {:ok {:saw marker :uri (:uri request)}})
                    {:request {:uri "/x"}
                     :org.acme.ledger.diplomat.http-server.handlers/deps {:marker "yes"}})]
      (is (= 200 (:status resp)))
      (is (= {:saw "yes" :uri "/x"} (json/parse-string (:body resp) true))))))

(deftest handler-name-is-keyword
  (is (keyword? (:name (handlers/make-handler (fn [_] {:ok 1}))))))
