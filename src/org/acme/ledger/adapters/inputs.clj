(ns org.acme.ledger.adapters.inputs
  (:require
   [malli.core :as m]
   [malli.error :as me]
   [org.acme.ledger.adapters.anomalies :as anom]))

(def time-selector
  "Exactly one of :latest, :as-of, :t."
  [:and
   [:map
    [:latest {:optional true} :boolean]
    [:as-of  {:optional true} inst?]
    [:t      {:optional true} pos-int?]]
   [:fn {:error/message "Exactly one of :latest, :as-of, :t is required."}
    (fn [m]
      (= 1 (count (filter some? ((juxt :latest :as-of :t) m)))))]])

(defn validate
  "Returns {:ok value} or an anomaly map."
  [schema value]
  (if (m/validate schema value)
    {:ok value}
    (anom/anom :cognitect.anomalies/incorrect
               "Invalid input"
               {:errors (-> (m/explain schema value) me/humanize)})))
