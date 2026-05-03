(ns org.acme.ledger.components.metrics
  (:require
   [com.stuartsierra.component :as component]
   [iapetos.collector.jvm :as jvm]
   [iapetos.core :as prometheus]
   [iapetos.export :as export]
   [taoensso.timbre :as log]))

(defn- new-registry []
  (-> (prometheus/collector-registry)
      (jvm/initialize)
      (prometheus/register
       (prometheus/counter :ledger/http-requests-total
                           {:description "Count of HTTP requests."
                            :labels [:method :route :status]})
       (prometheus/histogram :ledger/http-request-duration-seconds
                             {:description "HTTP request duration."
                              :labels [:method :route]})
       (prometheus/counter :ledger/datomic-tx-total
                           {:description "Datomic transactions."
                            :labels [:source :outcome]})
       (prometheus/histogram :ledger/datomic-tx-duration-seconds
                             {:description "Datomic transaction duration."
                              :labels [:source]}))))

(defrecord Metrics [config registry]
  component/Lifecycle
  (start [this]
    (if registry
      this
      (let [reg (new-registry)]
        (log/info {:event :metrics/started})
        (assoc this :registry reg))))
  (stop [this]
    (assoc this :registry nil)))

(defn new-metrics []
  (->Metrics nil nil))

(defn scrape [metrics]
  (export/text-format (:registry metrics)))
