(ns org.acme.ledger.logic.entry
  (:require
   [org.acme.ledger.logic.posting :as posting]))

(defn balanced?
  "True iff postings sum to zero (debits equal credits) in a single currency."
  [postings]
  (and (seq postings)
       (zero? (reduce + 0 (map posting/signed-amount postings)))
       (apply = (map :currency postings))))

(defn entry
  "Construct a balanced entry. Throws if the postings are not balanced."
  [{:keys [postings memo occurred-at]}]
  (when-not (balanced? postings)
    (throw (ex-info "Entry not balanced"
                    {:postings postings})))
  {:entry-id    (java.util.UUID/randomUUID)
   :postings    postings
   :memo        (or memo "")
   :occurred-at (or occurred-at (java.util.Date.))})
