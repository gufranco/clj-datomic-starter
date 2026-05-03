(ns org.acme.ledger.integration.connection-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [org.acme.ledger.diplomat.datomic.connection :as connection]
   [org.acme.ledger.helpers.datomock :as datomock]))

(deftest connection-helpers
  (let [conn (datomock/fresh-conn)
        db   (connection/db conn)]
    (testing "db returns a database value"
      (is (some? db)))
    (testing "query-stats returns result and timing"
      (let [s (connection/query-stats '[:find ?e :where [?e :db/ident :db/ident]] db)]
        (is (seqable? (:result s)))
        (is (number? (:duration-ms s)))
        (is (not (neg? (:duration-ms s))))))
    (testing "release closes the connection"
      (connection/release conn)
      (is true))))

(deftest connect-roundtrip
  (let [uri (str "datomic:mem://conn-test-" (java.util.UUID/randomUUID))]
    (datomic.api/create-database uri)
    (let [conn (connection/connect uri)]
      (is (some? (connection/db conn)))
      (connection/release conn))))
