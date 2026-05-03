(ns org.acme.ledger.diplomat.http-server.routes
  (:require
   [io.pedestal.http :as http]
   [io.pedestal.http.body-params :as body-params]
   [io.pedestal.http.route :as route]
   [org.acme.ledger.components.datomic :as comp.datomic]
   [org.acme.ledger.components.metrics :as comp.metrics]
   [org.acme.ledger.controllers.accounts :as accounts]
   [org.acme.ledger.controllers.statements :as statements]
   [org.acme.ledger.controllers.transfers :as transfers]
   [org.acme.ledger.diplomat.http-server.handlers :as handlers]
   [org.acme.ledger.diplomat.http-server.interceptors :as interceptors]
   [org.acme.ledger.diplomat.http-server.json :as json]))

(def open-account-schema
  [:map
   [:customer-id :uuid]
   [:number      [:string {:min 1}]]
   [:currency    [:string {:min 3 :max 3}]]
   [:kind        [:enum :asset :liability :equity :revenue :expense]]])

(def transfer-schema
  [:map
   [:from-account-id :uuid]
   [:to-account-id   :uuid]
   [:amount-minor    [:int {:min 1}]]
   [:currency        [:string {:min 3 :max 3}]]
   [:memo {:optional true} :string]])

(defn- health-handler [{:keys [datomic]}]
  (try
    (let [_ (comp.datomic/db datomic)]
      {:ok {:status :up}})
    (catch Exception e
      {:cognitect.anomalies/category :cognitect.anomalies/unavailable
       :cognitect.anomalies/message  (.getMessage e)})))

(defn- metrics-handler [{:keys [metrics]}]
  {:status  200
   :headers {"Content-Type" "text/plain; version=0.0.4"}
   :body    (comp.metrics/scrape metrics)})

(defn routes-table [deps]
  (let [common [(interceptors/deps deps)
                interceptors/correlation-id
                interceptors/timing
                interceptors/error-to-anomaly
                (body-params/body-params)
                http/json-body
                interceptors/kebab-keys]]
    (route/expand-routes
     #{["/health"  :get  (conj common (handlers/make-handler health-handler))]
       ["/metrics" :get  (conj common (handlers/make-raw-handler metrics-handler))]
       ["/v1/accounts" :post
        (conj common
              (interceptors/malli-body open-account-schema)
              (handlers/make-handler accounts/open!))]
       ["/v1/accounts/:account-id/close" :post
        (conj common (handlers/make-handler accounts/close!))]
       ["/v1/customers/:customer-id/accounts" :get
        (conj common (handlers/make-handler accounts/list-by-customer))]
       ["/v1/transfers" :post
        (conj common
              (interceptors/malli-body transfer-schema)
              (handlers/make-handler transfers/transfer!))]
       ["/v1/accounts/:account-id/balance" :post
        (conj common (handlers/make-handler statements/balance))]
       ["/v1/accounts/:account-id/movements" :post
        (conj common (handlers/make-handler statements/movements))]})))

(defn service-map [deps]
  (json/install!)
  {::http/routes (routes-table deps)})
