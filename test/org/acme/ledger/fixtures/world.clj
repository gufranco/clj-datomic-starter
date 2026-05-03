(ns org.acme.ledger.fixtures.world
  "Test world: a populated database value built from a plain sequence of facts."
  (:require
   [datomic.api :as d]
   [org.acme.ledger.diplomat.datomic.tx :as tx]
   [org.acme.ledger.helpers.datomock :as datomock]))

(def default-customer #uuid "00000000-0000-0000-0000-0000000000aa")

(defn- meta-stub [source]
  {:correlation-id (str (java.util.UUID/randomUUID))
   :actor          "fixture"
   :source         source})

(defn account-fact
  ([number]               (account-fact number "USD" :asset 0))
  ([number currency]      (account-fact number currency :asset 0))
  ([number currency kind] (account-fact number currency kind 0))
  ([number currency kind balance-minor]
   {:account/id            (java.util.UUID/randomUUID)
    :account/customer-id   default-customer
    :account/number        number
    :account/currency      currency
    :account/kind          kind
    :account/status        :open
    :account/balance-minor balance-minor
    :account/opened-at     (java.util.Date.)}))

(defn populate!
  "Apply the given fact maps to a fresh datomock connection. Returns the conn."
  [facts]
  (let [conn (datomock/fresh-conn)]
    (when (seq facts)
      (tx/transact! conn (vec facts) (meta-stub :test)))
    conn))

(defn fresh-world
  "Two open accounts under one customer; the source pre-funded with `funded`."
  [{:keys [funded] :or {funded 1000}}]
  (let [a1   (account-fact "1001")
        a2   (account-fact "1002")
        conn (populate! [a1 a2])]
    (tx/transact! conn
                  [[:db/cas [:account/id (:account/id a1)]
                    :account/balance-minor 0 funded]]
                  (meta-stub :test))
    {:conn       conn
     :customer   default-customer
     :source-id  (:account/id a1)
     :sink-id    (:account/id a2)
     :funded     funded}))

(defn db [{:keys [conn]}] (d/db conn))
