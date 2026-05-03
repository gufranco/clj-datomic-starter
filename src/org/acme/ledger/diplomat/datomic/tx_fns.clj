(ns org.acme.ledger.diplomat.datomic.tx-fns
  "Implementations invoked by classpath tx-fns and attribute/entity predicates."
  (:require
   [datomic.api :as d]))

(def iso-4217-set
  #{"USD" "EUR" "GBP" "JPY" "BRL" "CHF" "CAD" "AUD" "NZD" "MXN"
    "ARS" "CLP" "COP" "PEN" "UYU" "CNY" "HKD" "SGD" "INR" "ZAR"
    "SEK" "NOK" "DKK" "PLN" "CZK" "HUF" "TRY" "ILS"})

(defn iso-4217? [v]
  (boolean (and (string? v) (contains? iso-4217-set v))))

(defn positive-long? [v]
  (and (integer? v) (pos? v)))

(defn- posting-signed [{:posting/keys [side amount-minor]}]
  (case side
    :debit  (long amount-minor)
    :credit (- (long amount-minor))
    0))

(defn entry-balanced? [_db entry]
  (let [postings (:entry/postings entry)]
    (and (seq postings)
         (zero? (reduce + 0 (map posting-signed postings)))
         (apply = (map :posting/currency postings)))))

(defn post-entry-impl
  "Returns tx-data that records the entry's postings and atomically updates
   each affected account balance via :db/cas. Called inside the transactor."
  [db {:keys [postings memo occurred-at entry-id entry-tempid] :as _entry}]
  (let [account->delta (->> postings
                            (group-by :account-id)
                            (reduce-kv
                             (fn [acc account-id ps]
                               (assoc acc account-id
                                      (reduce + 0
                                              (map (fn [{:keys [side amount-minor]}]
                                                     (case side
                                                       :debit  (long amount-minor)
                                                       :credit (- (long amount-minor))))
                                                   ps))))
                             {}))
        cas-ops (mapv
                 (fn [[account-id delta]]
                   (let [eid     (d/entid db [:account/id account-id])
                         current (long (or (:account/balance-minor (d/entity db eid)) 0))]
                     [:db/cas eid :account/balance-minor current (+ current delta)]))
                 account->delta)
        entry-tempid (or entry-tempid
                         (str "ledger.entry/" (or entry-id (java.util.UUID/randomUUID))))
        posting-data (mapv
                      (fn [{:keys [posting-id account-id amount-minor side currency]}]
                        {:db/id                (d/tempid :db.part/user)
                         :posting/id           (or posting-id (java.util.UUID/randomUUID))
                         :posting/account      [:account/id account-id]
                         :posting/amount-minor (long amount-minor)
                         :posting/side         side
                         :posting/currency     currency})
                      postings)
        entry-data   {:db/id            entry-tempid
                      :db/ensure        :entry/spec
                      :entry/id         (or entry-id (java.util.UUID/randomUUID))
                      :entry/occurred-at (or occurred-at (java.util.Date.))
                      :entry/postings   (mapv :db/id posting-data)
                      :entry/memo       (or memo "")}]
    (into (conj posting-data entry-data) cas-ops)))
