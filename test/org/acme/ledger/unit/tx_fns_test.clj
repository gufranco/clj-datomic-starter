(ns org.acme.ledger.unit.tx-fns-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [org.acme.ledger.diplomat.datomic.tx-fns :as tx-fns]))

(deftest iso-4217?-test
  (is (true?  (tx-fns/iso-4217? "USD")))
  (is (true?  (tx-fns/iso-4217? "BRL")))
  (is (false? (tx-fns/iso-4217? "ZZZ")))
  (is (false? (tx-fns/iso-4217? "")))
  (is (false? (tx-fns/iso-4217? nil)))
  (is (false? (tx-fns/iso-4217? 42))))

(deftest positive-long?-test
  (is (true?  (tx-fns/positive-long? 1)))
  (is (true?  (tx-fns/positive-long? 1000000)))
  (is (false? (tx-fns/positive-long? 0)))
  (is (false? (tx-fns/positive-long? -1)))
  (is (false? (tx-fns/positive-long? 1.5)))
  (is (false? (tx-fns/positive-long? "1"))))

(deftest entry-balanced?-test
  (testing "balanced single-currency entry"
    (is (true? (tx-fns/entry-balanced?
                nil
                {:entry/postings [{:posting/side :debit  :posting/amount-minor 100 :posting/currency "USD"}
                                  {:posting/side :credit :posting/amount-minor 100 :posting/currency "USD"}]}))))
  (testing "unbalanced rejected"
    (is (false? (tx-fns/entry-balanced?
                 nil
                 {:entry/postings [{:posting/side :debit  :posting/amount-minor 100 :posting/currency "USD"}
                                   {:posting/side :credit :posting/amount-minor 50  :posting/currency "USD"}]}))))
  (testing "mixed currencies rejected"
    (is (false? (tx-fns/entry-balanced?
                 nil
                 {:entry/postings [{:posting/side :debit  :posting/amount-minor 100 :posting/currency "USD"}
                                   {:posting/side :credit :posting/amount-minor 100 :posting/currency "EUR"}]}))))
  (testing "empty postings rejected"
    (is (not (tx-fns/entry-balanced? nil {:entry/postings []})))))
