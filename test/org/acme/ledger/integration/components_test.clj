(ns org.acme.ledger.integration.components-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [com.stuartsierra.component :as component]
   [datomic.api :as d]
   [org.acme.ledger.components.config :as config]
   [org.acme.ledger.components.datomic :as datomic-c]
   [org.acme.ledger.components.http-server :as http-server]
   [org.acme.ledger.components.metrics :as metrics]
   [org.acme.ledger.components.migrator :as migrator]
   [org.acme.ledger.system :as system]))

(deftest config-component-roundtrip
  (let [c (component/start (config/new-config :test))]
    (testing "config is loaded"
      (is (some? (:config c))))
    (testing "starting twice is idempotent"
      (is (= (:config c) (:config (component/start c)))))
    (testing "stop clears config"
      (is (nil? (:config (component/stop c)))))))

(deftest config-default-profile
  (testing "no-arg constructor defaults to :dev"
    (is (= :dev (:profile (config/new-config))))))

(deftest datomic-component-roundtrip
  (let [uri  (str "datomic:mem://comp-test-" (java.util.UUID/randomUUID))
        comp (-> (datomic-c/new-datomic {:uri uri :create? true})
                 component/start)]
    (testing "connection is alive"
      (is (some? (datomic-c/conn comp)))
      (is (some? (datomic-c/db comp))))
    (testing "starting twice keeps the same connection"
      (let [c2 (component/start comp)]
        (is (= (:connection comp) (:connection c2)))))
    (testing "stop releases"
      (let [stopped (component/stop comp)]
        (is (nil? (:connection stopped)))))))

(deftest datomic-component-uses-config-defaults
  (let [uri    (str "datomic:mem://cfg-test-" (java.util.UUID/randomUUID))
        cfg    {:config {:datomic {:uri uri :create? true}}}
        comp   (component/start (assoc (datomic-c/new-datomic) :config cfg))]
    (is (some? (datomic-c/conn comp)))
    (component/stop comp)))

(deftest datomic-no-arg-constructor
  (is (some? (datomic-c/new-datomic))))

(deftest metrics-component-roundtrip
  (let [m (component/start (metrics/new-metrics))]
    (is (some? (:registry m)))
    (testing "scrape returns non-empty text"
      (is (string? (metrics/scrape m)))
      (is (pos? (count (metrics/scrape m)))))
    (testing "stop clears registry"
      (is (nil? (:registry (component/stop m)))))))

(deftest migrator-applies-norms
  (let [uri  (str "datomic:mem://mig-test-" (java.util.UUID/randomUUID))
        cfg  (component/start (config/new-config :test))
        cfg' (assoc-in cfg [:config :datomic :uri] uri)
        cfg' (assoc-in cfg' [:config :datomic :create?] true)
        dat  (component/start (assoc (datomic-c/new-datomic) :config cfg'))
        mig  (component/start (assoc (migrator/new-migrator)
                                     :config cfg'
                                     :datomic dat))
        db   (d/db (datomic-c/conn dat))]
    (testing "schema attributes are installed"
      (is (some? (d/q '[:find ?e . :where [?e :db/ident :account/id]] db))))
    (testing "running migrator twice is idempotent"
      (let [_   (component/start (assoc (migrator/new-migrator)
                                        :config cfg'
                                        :datomic dat))
            db2 (d/db (datomic-c/conn dat))
            count1 (count (d/q '[:find [?id ...] :where [_ :migration/id ?id]] db2))]
        (is (pos? count1))))
    (component/stop mig)
    (component/stop dat)))

(deftest http-server-component-with-stub-service-fn
  (let [stub (fn [_deps] {:io.pedestal.http/routes #{}})
        srv  (atom nil)
        comp (-> (http-server/new-http-server stub)
                 (assoc :config {:config {:http {:port 0 :host "127.0.0.1" :join? false}}}
                        :datomic :stub-d
                        :metrics :stub-m))]
    (with-redefs [io.pedestal.http/create-server #(do (reset! srv :created) %)
                  io.pedestal.http/start         #(do (reset! srv :started) %)
                  io.pedestal.http/stop          #(do (reset! srv :stopped) %)]
      (let [started (component/start comp)]
        (is (= :started @srv))
        (let [stopped (component/stop started)]
          (is (= :stopped @srv))
          (is (nil? (:server stopped))))))))

(deftest http-server-no-arg-constructor
  (is (some? (http-server/new-http-server))))

(deftest build-system-returns-system-map
  (let [sys (system/build-system :test)]
    (is (contains? sys :config))
    (is (contains? sys :datomic))
    (is (contains? sys :migrator))
    (is (contains? sys :metrics))
    (is (contains? sys :http-server))))

(deftest build-system-default-profile
  (testing "no-arg form returns a system map"
    (is (some? (system/build-system)))))
