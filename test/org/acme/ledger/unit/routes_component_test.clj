(ns org.acme.ledger.unit.routes-component-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [com.stuartsierra.component :as component]
   [org.acme.ledger.components.routes :as routes]))

(deftest routes-component-resolves-var-on-build-service
  (testing "start captures a var and a deps map"
    (let [stub-fn (fn [deps] {:routes-marker deps})
          comp    (-> (routes/new-routes)
                      (assoc :service-map-var (atom stub-fn)
                             :datomic         :d
                             :metrics         :m)
                      component/start)]
      (is (= :d (get-in comp [:deps :datomic])))
      (is (= :m (get-in comp [:deps :metrics])))
      (testing "build-service derefs the var on every call"
        (is (= {:routes-marker {:datomic :d :metrics :m}}
               (routes/build-service comp))))
      (testing "stop clears deps"
        (is (nil? (:deps (component/stop comp))))))))

(deftest debug-logger-component-clears-state
  (let [debug-logger-ns (the-ns 'org.acme.ledger.helpers.debug-logger)]
    (require 'org.acme.ledger.helpers.debug-logger)
    (let [record!  (ns-resolve debug-logger-ns 'record!)
          last-cap (ns-resolve debug-logger-ns 'last-captured)
          new-c    (ns-resolve debug-logger-ns 'new-debug-logger)]
      (record! (Exception. "x") {})
      (is (some? (last-cap)))
      (let [c (component/start (new-c))]
        (is (nil? (last-cap)))
        (component/stop c)))))
