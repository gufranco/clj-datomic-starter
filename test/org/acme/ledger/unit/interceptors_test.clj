(ns org.acme.ledger.unit.interceptors-test
  (:require
   [cheshire.core :as json]
   [clojure.test :refer [deftest is testing]]
   [org.acme.ledger.diplomat.http-server.interceptors :as interceptors]))

(deftest correlation-id-generated-when-absent
  (let [enter (:enter interceptors/correlation-id)
        ctx   (enter {:request {:headers {}}})]
    (is (string? (get-in ctx [:request :correlation-id])))
    (is (= (get-in ctx [:request :correlation-id])
           (get-in ctx [:response-headers "X-Correlation-Id"])))))

(deftest correlation-id-propagated-from-request
  (let [enter (:enter interceptors/correlation-id)
        ctx   (enter {:request {:headers {"x-correlation-id" "abc-123"}}})]
    (is (= "abc-123" (get-in ctx [:request :correlation-id])))))

(deftest correlation-id-leave-stamps-response
  (let [leave (:leave interceptors/correlation-id)
        ctx   (leave {:request  {:correlation-id "x"}
                      :response {:status 200 :headers {}}})]
    (is (= "x" (get-in ctx [:response :headers "X-Correlation-Id"])))))

(deftest timing-records-start-and-logs-on-leave
  (let [enter (:enter interceptors/timing)
        leave (:leave interceptors/timing)
        ctx0  (enter {:request {:uri "/x"}})
        ctx1  (leave (assoc ctx0 :response {:status 200}))]
    (is (some? (:org.acme.ledger.diplomat.http-server.interceptors/start ctx0)))
    (is (= 200 (get-in ctx1 [:response :status])))))

(deftest deps-injection
  (let [enter (:enter (interceptors/deps {:datomic :stub}))
        ctx   (enter {})]
    (is (= {:datomic :stub}
           (:org.acme.ledger.diplomat.http-server.handlers/deps ctx)))))

(deftest error-to-anomaly-builds-500
  (let [error-fn (:error interceptors/error-to-anomaly)
        ctx      (error-fn {} (Exception. "boom"))
        body     (json/parse-string (get-in ctx [:response :body]) true)]
    (is (= 500 (get-in ctx [:response :status])))
    (is (= "boom" (:message body)))))

(deftest malli-body-passes-valid
  (let [int' (interceptors/malli-body [:map [:n :int]])
        ctx  ((:enter int') {:request {:json-params {:n 1}}})]
    (is (= {:n 1} (get-in ctx [:request :validated-body])))))

(deftest malli-body-rejects-invalid
  (testing "invalid body short-circuits with 400 anomaly"
    (let [int' (interceptors/malli-body [:map [:n :int]])
          ctx  ((:enter int') {:request {:json-params {:n "x"}}})]
      (is (= 400 (get-in ctx [:response :status])))
      (let [body (json/parse-string (get-in ctx [:response :body]) true)]
        (is (= "cognitect.anomalies/incorrect" (:cognitect.anomalies/category body)))))))

(deftest malli-body-coerces-json-types
  (testing "string UUID is coerced from JSON"
    (let [int' (interceptors/malli-body [:map [:id :uuid]])
          uuid (str (java.util.UUID/randomUUID))
          ctx  ((:enter int') {:request {:json-params {:id uuid}}})]
      (is (= uuid (str (get-in ctx [:request :validated-body :id])))))))

(deftest kebab-keys-enter-converts-json-params
  (let [enter (:enter interceptors/kebab-keys)
        ctx   (enter {:request {:json-params {:customer_id "u-1"
                                              :nested      {:inner_field 1}}}})]
    (is (= {:customer-id "u-1" :nested {:inner-field 1}}
           (get-in ctx [:request :json-params])))))

(deftest kebab-keys-leave-converts-response-body
  (testing "map body keys become snake_case"
    (let [leave (:leave interceptors/kebab-keys)
          ctx   (leave {:response {:body {:customer-id "u-1"}}})]
      (is (= {:customer_id "u-1"} (get-in ctx [:response :body])))))
  (testing "string body is left untouched"
    (let [leave (:leave interceptors/kebab-keys)
          ctx   (leave {:response {:body "raw-text"}})]
      (is (= "raw-text" (get-in ctx [:response :body])))))
  (testing "missing body is a no-op"
    (let [leave (:leave interceptors/kebab-keys)
          ctx   (leave {:response {:status 204}})]
      (is (= {:status 204} (:response ctx))))))
