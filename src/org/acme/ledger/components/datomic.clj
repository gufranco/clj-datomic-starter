(ns org.acme.ledger.components.datomic
  (:require
   [com.stuartsierra.component :as component]
   [datomic.api :as d]
   [taoensso.timbre :as log]))

(defrecord Datomic [config uri create? connection]
  component/Lifecycle
  (start [this]
    (if connection
      this
      (let [cfg     (:config config)
            uri     (or uri (get-in cfg [:datomic :uri]))
            create? (if (some? create?) create? (get-in cfg [:datomic :create?] false))]
        (when create?
          (d/create-database uri))
        (let [conn (d/connect uri)]
          (log/info {:event :datomic/connected :uri uri})
          (assoc this :uri uri :create? create? :connection conn)))))
  (stop [this]
    (when connection
      (d/release connection)
      (log/info {:event :datomic/released :uri uri}))
    (assoc this :connection nil)))

(defn new-datomic
  ([] (->Datomic nil nil nil nil))
  ([{:keys [uri create?]}] (->Datomic nil uri create? nil)))

(defn conn [datomic]
  (:connection datomic))

(defn db [datomic]
  (d/db (:connection datomic)))
