(ns org.acme.ledger.diplomat.http-server.handlers
  (:require
   [cheshire.core :as json]
   [io.pedestal.interceptor :as interceptor]
   [org.acme.ledger.adapters.anomalies :as anom]))

(defn- json-response [status body]
  {:status  status
   :headers {"Content-Type" "application/json"}
   :body    (json/generate-string body)})

(defn- ->response [result]
  (cond
    (anom/anomaly? result)
    (json-response (anom/status-for result)
                   {:error   (:cognitect.anomalies/category result)
                    :message (:cognitect.anomalies/message result)
                    :details (dissoc result
                                     :cognitect.anomalies/category
                                     :cognitect.anomalies/message)})

    (and (map? result) (contains? result :ok))
    (json-response 200 (:ok result))

    (and (map? result) (contains? result :created))
    (json-response 201 (:created result))

    :else
    (json-response 200 result)))

(defn make-handler
  [controller-fn]
  (interceptor/interceptor
   {:name  (keyword "org.acme.ledger.diplomat.http-server.handlers"
                    (str "handler-" (gensym)))
    :enter (fn [ctx]
             (let [request (:request ctx)
                   deps    (::deps ctx {})
                   result  (controller-fn (assoc deps :request request))]
               (assoc ctx :response (->response result))))}))

(defn make-raw-handler
  [controller-fn]
  (interceptor/interceptor
   {:name  (keyword "org.acme.ledger.diplomat.http-server.handlers"
                    (str "raw-handler-" (gensym)))
    :enter (fn [ctx]
             (let [request (:request ctx)
                   deps    (::deps ctx {})
                   result  (controller-fn (assoc deps :request request))]
               (assoc ctx :response result)))}))
