(ns org.acme.ledger.diplomat.http-server.interceptors
  (:require
   [cheshire.core :as json]
   [clojure.string :as str]
   [clojure.walk :as walk]
   [malli.core :as m]
   [malli.error :as me]
   [malli.transform :as mt]
   [org.acme.ledger.adapters.anomalies :as anom]
   [taoensso.timbre :as log]))

(defn- rename-keys-deep [f form]
  (walk/postwalk
   (fn [x]
     (if (map? x)
       (into {} (map (fn [[k v]] [(f k) v])) x)
       x))
   form))

(defn- snake->kebab [k]
  (cond
    (keyword? k) (keyword (namespace k) (str/replace (name k) #"_" "-"))
    (string?  k) (str/replace k #"_" "-")
    :else        k))

(defn- kebab->snake [k]
  (cond
    (keyword? k) (keyword (namespace k) (str/replace (name k) #"-" "_"))
    (string?  k) (str/replace k #"-" "_")
    :else        k))

(def kebab-keys
  {:name ::kebab-keys
   :enter (fn [ctx]
            (cond-> ctx
              (get-in ctx [:request :json-params])
              (update-in [:request :json-params] #(rename-keys-deep snake->kebab %))))
   :leave (fn [ctx]
            (let [body (get-in ctx [:response :body])]
              (cond-> ctx
                (and body (or (map? body) (sequential? body)))
                (update-in [:response :body] #(rename-keys-deep kebab->snake %)))))})

(def correlation-id
  {:name ::correlation-id
   :enter (fn [ctx]
            (let [cid (or (get-in ctx [:request :headers "x-correlation-id"])
                          (str (java.util.UUID/randomUUID)))]
              (-> ctx
                  (assoc-in [:request :correlation-id] cid)
                  (assoc-in [:response-headers "X-Correlation-Id"] cid))))
   :leave (fn [ctx]
            (let [cid (get-in ctx [:request :correlation-id])]
              (cond-> ctx
                cid (assoc-in [:response :headers "X-Correlation-Id"] cid))))})

(def timing
  {:name ::timing
   :enter (fn [ctx] (assoc ctx ::start (System/nanoTime)))
   :leave (fn [ctx]
            (let [start  (::start ctx)
                  ms     (when start (/ (- (System/nanoTime) start) 1e6))
                  status (get-in ctx [:response :status])
                  uri    (get-in ctx [:request :uri])]
              (log/info {:event :http/request :status status :uri uri :duration-ms ms})
              ctx))})

(defn deps [d]
  {:name  ::deps
   :enter (fn [ctx] (assoc ctx :org.acme.ledger.diplomat.http-server.handlers/deps d))})

(def error-to-anomaly
  {:name ::error-to-anomaly
   :error (fn [ctx ex]
            (log/error {:event :http/error :ex (.getMessage ^Throwable ex)})
            (assoc ctx :response
                   {:status  500
                    :headers {"Content-Type" "application/json"}
                    :body    (json/generate-string
                              {:error   :cognitect.anomalies/fault
                               :message (.getMessage ^Throwable ex)})}))})

(defn malli-body [schema]
  (let [decoder (m/decoder schema (mt/json-transformer))]
    {:name ::malli-body
     :enter (fn [ctx]
              (let [raw     (get-in ctx [:request :json-params])
                    decoded (decoder raw)]
                (if (m/validate schema decoded)
                  (assoc-in ctx [:request :validated-body] decoded)
                  (assoc ctx :response
                         {:status  400
                          :headers {"Content-Type" "application/json"}
                          :body    (json/generate-string
                                    (anom/anom :cognitect.anomalies/incorrect
                                               "Invalid request body"
                                               {:errors (-> (m/explain schema decoded) me/humanize)}))}))))}))
