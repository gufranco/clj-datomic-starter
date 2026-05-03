(ns org.acme.ledger.integration.http-routes-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [com.stuartsierra.component :as component]
   [org.acme.ledger.components.metrics :as metrics-c]
   [org.acme.ledger.helpers.datomock :as datomock]
   [org.acme.ledger.helpers.http :as http]))

(defn- service-fn []
  (let [conn    (datomock/fresh-conn)
        metrics (component/start (metrics-c/new-metrics))]
    (http/build-service {:datomic {:connection conn} :metrics metrics})))

(deftest health-endpoint
  (let [svc  (service-fn)
        resp (http/request svc :get "/health")]
    (is (= 200 (:status resp)))))

(deftest metrics-endpoint
  (let [svc  (service-fn)
        resp (http/request svc :get "/metrics")]
    (is (= 200 (:status resp)))
    (is (string? (:body resp)))))

(deftest open-account-endpoint
  (let [svc  (service-fn)
        resp (http/request svc :post "/v1/accounts"
                           {:customer-id (str (java.util.UUID/randomUUID))
                            :number      "H-1001"
                            :currency    "USD"
                            :kind        "asset"})]
    (is (#{200 201} (:status resp)))))

(deftest open-account-rejects-bad-body
  (testing "malli interceptor returns 400"
    (let [svc  (service-fn)
          resp (http/request svc :post "/v1/accounts" {:bad "shape"})]
      (is (= 400 (:status resp))))))

(deftest correlation-id-header-roundtrip
  (let [svc  (service-fn)
        resp (http/request svc :get "/health" nil {"X-Correlation-Id" "fixed-cid"})]
    (is (= "fixed-cid" (get-in resp [:headers "X-Correlation-Id"])))))
