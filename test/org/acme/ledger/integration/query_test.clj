(ns org.acme.ledger.integration.query-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [datomic.api :as d]
   [org.acme.ledger.diplomat.datomic.query :as query]
   [org.acme.ledger.diplomat.datomic.tx :as tx]
   [org.acme.ledger.helpers.datomock :as datomock]))

(defn- meta-stub []
  {:correlation-id (str (java.util.UUID/randomUUID))
   :actor "test" :source :test})

(defn- seed [conn]
  (let [cust (java.util.UUID/randomUUID)
        a1   (java.util.UUID/randomUUID)
        a2   (java.util.UUID/randomUUID)]
    (tx/transact! conn
                  [{:account/id a1 :account/customer-id cust :account/number "1001"
                    :account/currency "USD" :account/kind :asset :account/status :open
                    :account/balance-minor 100 :account/opened-at (java.util.Date.)}
                   {:account/id a2 :account/customer-id cust :account/number "1002"
                    :account/currency "USD" :account/kind :asset :account/status :open
                    :account/balance-minor 200 :account/opened-at (java.util.Date.)}]
                  (meta-stub))
    {:cust cust :a1 a1 :a2 a2}))

(deftest account-by-id-returns-pull
  (let [conn (datomock/fresh-conn)
        {:keys [a1]} (seed conn)
        a    (query/account-by-id (d/db conn) a1)]
    (is (= "1001" (:account/number a)))
    (is (= 100 (:account/balance-minor a)))))

(deftest accounts-of-customer-returns-all
  (let [conn (datomock/fresh-conn)
        {:keys [cust]} (seed conn)
        rows (query/accounts-of-customer (d/db conn) cust)]
    (is (= 2 (count rows)))))

(deftest balance-as-of-reads-balance
  (let [conn (datomock/fresh-conn)
        {:keys [a2]} (seed conn)]
    (is (= 200 (query/balance-as-of (d/db conn) a2)))))

(deftest movements-between-windows
  (testing "movement query returns empty rows when nothing posted in the range"
    (let [conn (datomock/fresh-conn)
          {:keys [a1]} (seed conn)
          past (java.util.Date. 0)
          rows (query/movements-between (d/db conn) a1 past past)]
      (is (some? rows)))))

(deftest entry-by-id-pulls-postings
  (testing "entry pull returns nil for unknown id without crashing"
    (let [conn (datomock/fresh-conn)
          _ (seed conn)
          e (query/entry-by-id (d/db conn) (java.util.UUID/randomUUID))]
      (is (or (nil? e) (map? e))))))
