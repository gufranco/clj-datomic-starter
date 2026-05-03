(ns org.acme.ledger.components.http-server
  (:require
   [com.stuartsierra.component :as component]
   [io.pedestal.http :as http]
   [org.acme.ledger.components.routes :as comp.routes]
   [taoensso.timbre :as log]))

(defn- dev? [config]
  (= :dev (get-in config [:config :profile])))

(defn- dev-init [service-map]
  (-> service-map
      (assoc ::http/secure-headers
             {:content-security-policy-settings {:object-src "none"}}
             ::http/allowed-origins
             {:creds true :allowed-origins (constantly true)})
      http/dev-interceptors))

(defn- prod-init [service-map]
  service-map)

(defrecord HttpServer [config datomic metrics routes service-fn server]
  component/Lifecycle
  (start [this]
    (if server
      this
      (let [cfg      (:config config)
            port     (get-in cfg [:http :port])
            host     (get-in cfg [:http :host])
            join?    (get-in cfg [:http :join?])
            service  (cond
                       service-fn (service-fn {:datomic datomic :metrics metrics})
                       routes     (comp.routes/build-service routes)
                       :else      (comp.routes/build-service
                                   {:service-map-var (requiring-resolve
                                                      'org.acme.ledger.diplomat.http-server.routes/service-map)
                                    :deps            {:datomic datomic :metrics metrics}}))
            init-fn  (if (dev? config) dev-init prod-init)
            srv      (-> service
                         (assoc ::http/port port
                                ::http/host host
                                ::http/type :jetty
                                ::http/join? join?)
                         init-fn
                         http/create-server
                         http/start)]
        (log/info {:event :http/started :port port :profile (get-in cfg [:profile])})
        (assoc this :server srv))))
  (stop [this]
    (when server
      (http/stop server)
      (log/info {:event :http/stopped}))
    (assoc this :server nil)))

(defn new-http-server
  ([] (->HttpServer nil nil nil nil nil nil))
  ([service-fn] (->HttpServer nil nil nil nil service-fn nil)))
