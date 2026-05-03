(ns org.acme.ledger.diplomat.datomic.query
  (:require
   [datomic.api :as d]))

(def account-pull
  '[:account/id :account/customer-id :account/number :account/currency
    :account/kind :account/status :account/balance-minor
    :account/opened-at :account/closed-at])

(def posting-pull
  '[:posting/id :posting/amount-minor :posting/side :posting/currency
    {:posting/account [:account/id :account/number]}])

(def entry-pull
  '[:entry/id :entry/occurred-at :entry/memo
    {:entry/postings [:posting/id :posting/amount-minor :posting/side :posting/currency
                      {:posting/account [:account/id :account/number]}]}])

(def transfer-pull
  '[:transfer/id :transfer/amount-minor :transfer/currency :transfer/memo
    {:transfer/from [:account/id :account/number]}
    {:transfer/to   [:account/id :account/number]}
    {:transfer/entry [:entry/id :entry/occurred-at]}])

(def parents-rule
  '[[(parents-of ?child ?parent)
     [?child :account/parent ?parent]]
    [(parents-of ?child ?ancestor)
     [?child :account/parent ?p]
     (parents-of ?p ?ancestor)]])

(defn account-by-id [db account-id]
  (d/pull db account-pull [:account/id account-id]))

(defn accounts-of-customer [db customer-id]
  (->> (d/q '[:find [?e ...]
              :in $ ?cid
              :where [?e :account/customer-id ?cid]]
            db customer-id)
       (mapv #(d/pull db account-pull %))))

(defn entry-by-id [db entry-id]
  (d/pull db entry-pull [:entry/id entry-id]))

(defn balance-as-of
  "Balance of an account at a database value (use d/as-of for time travel)."
  [db account-id]
  (:account/balance-minor
   (d/pull db [:account/balance-minor] [:account/id account-id])))

(defn movements-between
  "Postings affecting `account-id` between two instants."
  [db account-id from-inst to-inst]
  (d/q '[:find ?p ?amount ?side ?txi
         :in $ ?account-id ?from ?to
         :where
         [?a :account/id ?account-id]
         [?p :posting/account ?a]
         [?p :posting/amount-minor ?amount]
         [?p :posting/side ?side]
         [?p :posting/amount-minor _ ?tx]
         [?tx :db/txInstant ?txi]
         [(>= ?txi ?from)]
         [(< ?txi ?to)]]
       db account-id from-inst to-inst))
