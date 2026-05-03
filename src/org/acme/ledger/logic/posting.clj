(ns org.acme.ledger.logic.posting
  (:require
   [org.acme.ledger.logic.money :as money]))

(defn debit
  "Build a debit posting against the given account."
  [{:keys [account-id ^org.acme.ledger.logic.money.Money amount]}]
  {:posting-id   (java.util.UUID/randomUUID)
   :account-id   account-id
   :side         :debit
   :amount-minor (:minor amount)
   :currency     (:currency amount)})

(defn credit
  "Build a credit posting against the given account."
  [{:keys [account-id ^org.acme.ledger.logic.money.Money amount]}]
  {:posting-id   (java.util.UUID/randomUUID)
   :account-id   account-id
   :side         :credit
   :amount-minor (:minor amount)
   :currency     (:currency amount)})

(defn signed-amount
  "Return the signed minor-unit value of the posting."
  [{:keys [side amount-minor]}]
  (case side
    :debit  (long amount-minor)
    :credit (- (long amount-minor))))

(defn money-of [{:keys [amount-minor currency]}]
  (money/money amount-minor currency))
