(ns org.acme.ledger.helpers.http
  "Minimal in-process HTTP test client. Builds a Pedestal service from the
   project's routes against a datomock-backed datomic component, and exposes
   `request` for state-flow style tests."
  (:require
   [cheshire.core :as json]
   [io.aviso.exception :as aviso]
   [io.pedestal.http :as http]
   [io.pedestal.test :as ptest]
   [org.acme.ledger.diplomat.http-server.routes :as routes]
   [org.acme.ledger.helpers.debug-logger :as debug-logger]))

(defn build-service
  "Build a Pedestal service map for testing. `deps` must contain :datomic and
   :metrics. Returns the service-fn ready for io.pedestal.test/response-for."
  [deps]
  (-> (routes/service-map deps)
      (assoc ::http/join? false
             ::http/type  :jetty
             ::http/port  0)
      http/default-interceptors
      http/dev-interceptors
      http/create-servlet
      ::http/service-fn))

(defn- ->bytes [body]
  (cond
    (string? body) body
    (nil?    body) ""
    :else          (json/generate-string body)))

(defn request
  "Issue an in-process request. Returns {:status ... :body ... :headers ...}.
   `body` may be nil, a string, or a map (auto-encoded as JSON)."
  ([service-fn method path]                (request service-fn method path nil nil))
  ([service-fn method path body]           (request service-fn method path body nil))
  ([service-fn method path body headers]
   (let [resp   (ptest/response-for service-fn method path
                                    :body    (->bytes body)
                                    :headers (merge {"Content-Type" "application/json"}
                                                    headers))
         status (:status resp)]
     (when (and status (>= status 400))
       (when-let [{:keys [ex]} (debug-logger/last-captured)]
         (binding [*out* *err*]
           (println (aviso/format-exception ex)))))
     (cond-> {:status status :headers (:headers resp)}
       (:body resp) (assoc :body (try (json/parse-string (:body resp) true)
                                      (catch Exception _ (:body resp))))))))
