(ns org.acme.ledger.logic.transfer
  (:require
   [org.acme.ledger.logic.entry :as entry]
   [org.acme.ledger.logic.money :as money]
   [org.acme.ledger.logic.posting :as posting]))

(defn transfer->entry
  "Translate a transfer command (from-account, to-account, money) into a
   balanced entry: debit the source, credit the destination."
  [{:keys [from-account-id to-account-id ^org.acme.ledger.logic.money.Money amount memo]}]
  (when-not (money/positive? amount)
    (throw (ex-info "Transfer amount must be positive" {:amount amount})))
  (when (= from-account-id to-account-id)
    (throw (ex-info "Cannot transfer to the same account"
                    {:account-id from-account-id})))
  (let [postings [(posting/debit  {:account-id to-account-id   :amount amount})
                  (posting/credit {:account-id from-account-id :amount amount})]]
    (entry/entry {:postings postings :memo memo})))
