(ns org.acme.ledger.system
  (:require
   [com.stuartsierra.component :as component]
   [org.acme.ledger.components.config :as config]
   [org.acme.ledger.components.datomic :as datomic]
   [org.acme.ledger.components.http-server :as http-server]
   [org.acme.ledger.components.metrics :as metrics]
   [org.acme.ledger.components.migrator :as migrator]
   [org.acme.ledger.components.routes :as routes]
   [taoensso.timbre :as log])
  (:gen-class))

(def web-app-deps
  [:datomic :metrics])

(defn build-system
  ([]      (build-system :prod))
  ([profile]
   (component/system-map
    :config      (config/new-config profile)
    :datomic     (component/using (datomic/new-datomic) [:config])
    :migrator    (component/using (migrator/new-migrator) [:config :datomic])
    :metrics     (component/using (metrics/new-metrics) [:config])
    :routes      (component/using (routes/new-routes) web-app-deps)
    :http-server (component/using (http-server/new-http-server)
                                  (into [:config :routes] web-app-deps)))))

(defn- env-profile []
  (case (System/getenv "LEDGER_PROFILE")
    "dev"  :dev
    "test" :test
    "prod" :prod
    :prod))

(defn -main [& _args]
  (let [profile (env-profile)
        sys     (component/start (build-system profile))]
    (log/info {:event :system/started :profile profile})
    (.addShutdownHook (Runtime/getRuntime)
                      (Thread. ^Runnable
                       (fn []
                         (log/info {:event :system/stopping})
                         (component/stop sys))))
    @(promise)))
