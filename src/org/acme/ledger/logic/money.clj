(ns org.acme.ledger.logic.money
  "Minor-units integer math. No floats. Currency is part of the value."
  (:refer-clojure :exclude [zero?]))

(defrecord Money [minor currency])

(defn money
  "Construct a Money value. Rejects non-integer or negative amounts."
  [minor currency]
  (when-not (integer? minor)
    (throw (ex-info "Money amount must be integer (minor units)"
                    {:minor minor :currency currency})))
  (when (neg? minor)
    (throw (ex-info "Money amount must be non-negative"
                    {:minor minor :currency currency})))
  (when-not (and (string? currency) (= 3 (count currency)))
    (throw (ex-info "Currency must be a 3-letter code"
                    {:currency currency})))
  (->Money (long minor) currency))

(defn same-currency? [a b]
  (= (:currency a) (:currency b)))

(defn- assert-same! [a b op]
  (when-not (same-currency? a b)
    (throw (ex-info (str "Currency mismatch in " op)
                    {:a a :b b}))))

(defn add [a b]
  (assert-same! a b "add")
  (->Money (+ (:minor a) (:minor b)) (:currency a)))

(defn subtract [a b]
  (assert-same! a b "subtract")
  (->Money (- (:minor a) (:minor b)) (:currency a)))

(defn zero? [m]
  (clojure.core/zero? (:minor m)))

(defn positive? [m]
  (pos? (:minor m)))
