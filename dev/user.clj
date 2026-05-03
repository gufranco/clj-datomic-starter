(ns user
  (:require
   [clojure.tools.namespace.repl :as tn]
   [com.stuartsierra.component :as component]
   [portal.api :as portal]))

(tn/set-refresh-dirs "src" "dev" "resources")

(defonce system (atom nil))
(defonce portal-instance (atom nil))

(defn init []
  (require 'org.acme.ledger.system)
  (let [build-system (resolve 'org.acme.ledger.system/build-system)]
    (reset! system (build-system :dev))
    :initialized))

(defn start []
  (when-not @system
    (init))
  (swap! system component/start)
  :started)

(defn stop []
  (when-let [sys @system]
    (component/stop sys)
    (reset! system nil))
  :stopped)

(defn go []
  (init)
  (start))

(defn reset []
  (stop)
  (tn/refresh :after 'user/go))

(defn open-portal []
  (let [p (portal/open)]
    (add-tap #'portal/submit)
    (reset! portal-instance p)
    p))

(defn close-portal []
  (remove-tap #'portal/submit)
  (portal/close)
  (reset! portal-instance nil))
