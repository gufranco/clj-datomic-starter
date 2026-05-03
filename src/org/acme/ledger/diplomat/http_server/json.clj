(ns org.acme.ledger.diplomat.http-server.json
  (:require
   [cheshire.core :as cheshire]
   [cheshire.generate :as cgen]
   [clojure.string :as str]
   [clojure.walk :as walk])
  (:import
   (datomic Datom)
   (datomic.db Db)
   (java.time Instant)
   (java.util Date UUID)))

(defn- snake->kebab [^String s]
  (str/replace s #"_" "-"))

(defn- kebab->snake [^String s]
  (str/replace s #"-" "_"))

(defn- transform-keys [f m]
  (walk/postwalk
   (fn [x]
     (cond
       (map? x)     (into {} (map (fn [[k v]] [(f k) v])) x)
       :else        x))
   m))

(defn read-json
  "Parse a JSON string. String keys become kebab-case keywords."
  [^String s]
  (some->> s
           (#(cheshire/parse-string % false))
           (transform-keys (fn [k]
                             (if (string? k)
                               (keyword (snake->kebab k))
                               k)))))

(defn write-json
  "Serialize `value` to JSON. Keyword/string keys are converted to snake_case."
  [value]
  (cheshire/generate-string
   (transform-keys (fn [k]
                     (cond
                       (keyword? k) (kebab->snake (name k))
                       (string? k)  (kebab->snake k)
                       :else        k))
                   value)))

(defn install! []
  (cgen/add-encoder Db
                    (fn [^Db db json-gen]
                      (cgen/encode-map
                       {:basis-t (.basisT db)} json-gen)))
  (cgen/add-encoder Datom
                    (fn [^Datom d json-gen]
                      (cgen/encode-seq
                       [(.e d) (.a d) (.v d) (.tx d) (.added d)] json-gen)))
  (cgen/add-encoder Date
                    (fn [^Date d json-gen]
                      (cgen/encode-str (.toString (.toInstant d)) json-gen)))
  (cgen/add-encoder Instant
                    (fn [^Instant i json-gen]
                      (cgen/encode-str (.toString i) json-gen)))
  (cgen/add-encoder UUID
                    (fn [^UUID u json-gen]
                      (cgen/encode-str (.toString u) json-gen))))
