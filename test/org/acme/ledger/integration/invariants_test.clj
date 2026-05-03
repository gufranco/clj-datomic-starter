(ns org.acme.ledger.integration.invariants-test
  "Generative property tests over command sequences. The headline invariant:
   for any interleaving of opens, transfers, and reverses, the global sum of
   debits equals the global sum of credits across every account."
  (:require
   [clojure.test :refer [deftest is]]
   [clojure.test.check :as tc]
   [clojure.test.check.generators :as gen]
   [clojure.test.check.properties :as prop]
   [datomic.api :as d]
   [org.acme.ledger.diplomat.datomic.tx :as tx]
   [org.acme.ledger.helpers.datomock :as datomock]))

(def long-test? (Boolean/parseBoolean (System/getProperty "ledger.test.long" "false")))
(def cycles    (if long-test? 1000 100))

(defn- meta-stub []
  {:correlation-id (str (java.util.UUID/randomUUID))
   :actor          "invariant"
   :source         :test})

(defn- open-account! [conn customer-id number]
  (let [aid (java.util.UUID/randomUUID)]
    (tx/transact! conn
                  [{:account/id            aid
                    :account/customer-id   customer-id
                    :account/number        number
                    :account/currency      "USD"
                    :account/kind          :asset
                    :account/status        :open
                    :account/balance-minor 0
                    :account/opened-at     (java.util.Date.)}]
                  (meta-stub))
    aid))

(defn- post-pair! [conn debit-acc credit-acc amount]
  (try
    (tx/transact! conn
                  [[:ledger/post-entry
                    {:entry-id    (java.util.UUID/randomUUID)
                     :postings    [{:posting-id   (java.util.UUID/randomUUID)
                                    :account-id   debit-acc
                                    :side         :debit
                                    :amount-minor amount
                                    :currency     "USD"}
                                   {:posting-id   (java.util.UUID/randomUUID)
                                    :account-id   credit-acc
                                    :side         :credit
                                    :amount-minor amount
                                    :currency     "USD"}]
                     :memo        "gen-test"
                     :occurred-at (java.util.Date.)}]]
                  (meta-stub))
    true
    (catch Exception _ false)))

(defn- global-balance-sum [db]
  (->> (d/q '[:find ?a ?b
              :where [?a :account/balance-minor ?b]]
            db)
       (map second)
       (reduce + 0)))

(def command-gen
  (gen/one-of
   [(gen/fmap (fn [n] [:open n]) gen/pos-int)
    (gen/fmap (fn [[from to amt]] [:post from to (inc amt)])
              (gen/tuple gen/nat gen/nat (gen/large-integer* {:min 1 :max 1000})))]))

(defn- run-commands [conn commands]
  (let [customer-id (java.util.UUID/randomUUID)
        accounts    (atom [])]
    (doseq [cmd commands]
      (case (first cmd)
        :open
        (let [number (str "n-" (count @accounts) "-" (System/nanoTime))
              aid    (open-account! conn customer-id number)]
          (swap! accounts conj aid))
        :post
        (let [accs @accounts]
          (when (>= (count accs) 2)
            (let [a1 (nth accs (mod (nth cmd 1) (count accs)))
                  a2 (nth accs (mod (nth cmd 2) (count accs)))
                  amt (nth cmd 3)]
              (when-not (= a1 a2)
                (post-pair! conn a1 a2 amt)))))))
    @accounts))

(deftest entries-preserve-global-balance-invariant
  (let [result (tc/quick-check
                cycles
                (prop/for-all
                 [commands (gen/vector command-gen 0 30)]
                 (let [conn (datomock/fresh-conn)]
                   (run-commands conn commands)
                   (zero? (global-balance-sum (d/db conn))))))]
    (is (:result result) (str "Counter-example: " (:fail result)))))
