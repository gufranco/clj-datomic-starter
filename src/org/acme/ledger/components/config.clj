(ns org.acme.ledger.components.config
  (:require
   [aero.core :as aero]
   [clojure.java.io :as io]
   [com.stuartsierra.component :as component]
   [taoensso.timbre :as log]))

(defn load-config
  "Reads `resources/config.edn` resolved against the given Aero profile."
  [profile]
  (aero/read-config (io/resource "config.edn") {:profile profile}))

(defrecord Config [profile config]
  component/Lifecycle
  (start [this]
    (if config
      this
      (let [cfg (load-config profile)]
        (log/info {:event :config/loaded :profile profile})
        (assoc this :config cfg))))
  (stop [this]
    (assoc this :config nil)))

(defn new-config
  ([] (new-config :dev))
  ([profile] (->Config profile nil)))
