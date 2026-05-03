(ns org.acme.ledger.integration.controllers-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [org.acme.ledger.controllers.accounts :as accounts]
   [org.acme.ledger.controllers.statements :as statements]
   [org.acme.ledger.controllers.transfers :as transfers]
   [org.acme.ledger.helpers.datomock :as datomock]))

(defn- datomic-stub [conn]
  {:connection conn})

(deftest open-and-close-account
  (let [conn (datomock/fresh-conn)
        deps {:datomic (datomic-stub conn)}
        cid  (java.util.UUID/randomUUID)
        opened (accounts/open!
                (assoc deps :request {:body {:customer-id cid :number "5001"
                                             :currency "USD" :kind :asset}}))]
    (testing "open returns the new account"
      (is (= "5001" (:account/number (:created opened)))))
    (testing "list returns it for the customer"
      (let [r (accounts/list-by-customer
               (assoc deps :request {:path-params {:customer-id (str cid)}}))]
        (is (= 1 (count (:ok r))))))
    (testing "close marks status :closed"
      (let [r (accounts/close!
               (assoc deps :request {:body {:account-id (:account/id (:created opened))}}))]
        (is (= :closed (:account/status (:ok r))))))))

(deftest open-duplicate-number-returns-conflict
  (testing "second open with the same number returns a conflict anomaly"
    (let [conn (datomock/fresh-conn)
          deps {:datomic (datomic-stub conn)}
          cid  (java.util.UUID/randomUUID)
          _    (accounts/open!
                (assoc deps :request {:body {:customer-id cid :number "9999"
                                             :currency "USD" :kind :asset}}))
          dup  (accounts/open!
                (assoc deps :request {:body {:customer-id cid :number "9999"
                                             :currency "USD" :kind :asset}}))]
      (is (= :cognitect.anomalies/conflict (:cognitect.anomalies/category dup))))))

(deftest balance-controller-latest
  (let [conn (datomock/fresh-conn)
        deps {:datomic (datomic-stub conn)}
        cid  (java.util.UUID/randomUUID)
        {:keys [created]}
        (accounts/open! (assoc deps :request {:body {:customer-id cid :number "7001"
                                                     :currency "USD" :kind :asset}}))
        aid (:account/id created)
        r   (statements/balance
             (assoc deps :request {:path-params {:account-id (str aid)}
                                   :body {:latest true}}))]
    (is (= aid (-> r :ok :account-id)))
    (is (= 0   (-> r :ok :balance)))
    (is (number? (-> r :ok :basis-t)))))

(deftest balance-controller-rejects-bad-selector
  (let [conn (datomock/fresh-conn)
        deps {:datomic (datomic-stub conn)}
        r    (statements/balance
              (assoc deps :request {:path-params {:account-id (str (java.util.UUID/randomUUID))}
                                    :body {:latest true :t 1}}))]
    (is (= :cognitect.anomalies/incorrect (:cognitect.anomalies/category r)))))

(deftest movements-controller-returns-rows-key
  (let [conn (datomock/fresh-conn)
        deps {:datomic (datomic-stub conn)}
        cid  (java.util.UUID/randomUUID)
        {:keys [created]}
        (accounts/open! (assoc deps :request {:body {:customer-id cid :number "7002"
                                                     :currency "USD" :kind :asset}}))
        r    (statements/movements
              (assoc deps :request {:path-params {:account-id (str (:account/id created))}
                                    :body {}}))]
    (is (contains? (:ok r) :rows))))

(deftest transfer-controller-happy-path
  (let [conn (datomock/fresh-conn)
        deps {:datomic (datomic-stub conn)}
        cid  (java.util.UUID/randomUUID)
        a1   (:account/id (:created
                           (accounts/open!
                            (assoc deps :request {:body {:customer-id cid :number "T1"
                                                         :currency "USD" :kind :asset}}))))
        a2   (:account/id (:created
                           (accounts/open!
                            (assoc deps :request {:body {:customer-id cid :number "T2"
                                                         :currency "USD" :kind :asset}}))))
        r    (transfers/transfer!
              (assoc deps :request {:body {:from-account-id a1 :to-account-id a2
                                           :amount-minor 250 :currency "USD" :memo "ok"}}))]
    (is (some? (:created r)))
    (is (= 250 (:transfer/amount-minor (:created r))))))

(deftest transfer-controller-rejects-self
  (let [conn (datomock/fresh-conn)
        deps {:datomic (datomic-stub conn)}
        cid  (java.util.UUID/randomUUID)
        a1   (:account/id (:created
                           (accounts/open!
                            (assoc deps :request {:body {:customer-id cid :number "S1"
                                                         :currency "USD" :kind :asset}}))))
        r    (transfers/transfer!
              (assoc deps :request {:body {:from-account-id a1 :to-account-id a1
                                           :amount-minor 100 :currency "USD"}}))]
    (is (= :cognitect.anomalies/incorrect (:cognitect.anomalies/category r)))))
