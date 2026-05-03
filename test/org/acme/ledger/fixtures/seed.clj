(ns org.acme.ledger.fixtures.seed
  "Deterministic seed datasets keyed by name. Tests pull a named seed
   instead of constructing the world from scratch."
  (:require
   [org.acme.ledger.fixtures.world :as world]))

(def datasets
  {:two-empty-accounts
   [(world/account-fact "2001")
    (world/account-fact "2002")]

   :ten-accounts-mixed
   (mapv (fn [i] (world/account-fact (str (+ 3000 i))))
         (range 10))

   :one-multi-currency
   [(world/account-fact "4001" "USD")
    (world/account-fact "4002" "EUR")
    (world/account-fact "4003" "BRL")]})

(defn load!
  "Build a fresh datomock conn populated with the named dataset."
  [dataset-key]
  (world/populate! (get datasets dataset-key [])))
