(ns org.acme.ledger.helpers.debug-logger
  (:require
   [com.stuartsierra.component :as component]))

(defonce ^:private last-error (atom nil))

(defn record! [ex ctx]
  (reset! last-error {:ex ex :ctx (select-keys ctx [:request :response])}))

(defn last-captured []
  @last-error)

(defn clear! []
  (reset! last-error nil))

(def error-info
  {:name  ::error-info
   :error (fn [ctx ex]
            (record! ex ctx)
            (assoc ctx ::recorded? true))})

(defrecord DebugLogger []
  component/Lifecycle
  (start [this] (clear!) this)
  (stop  [this] (clear!) this))

(defn new-debug-logger []
  (->DebugLogger))
