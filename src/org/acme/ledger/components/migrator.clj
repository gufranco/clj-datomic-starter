(ns org.acme.ledger.components.migrator
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [com.stuartsierra.component :as component]
   [datomic.api :as d]
   [datomic.function :as df]
   [org.acme.ledger.components.datomic :as datomic]
   [taoensso.timbre :as log]))

(def ^:private edn-readers {'db/fn df/construct})

(def ^:private bootstrap-norm
  [{:db/ident       :migration/id
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one
    :db/unique      :db.unique/identity
    :db/doc         "Filename of the applied migration norm."}
   {:db/ident       :migration/applied-at
    :db/valueType   :db.type/instant
    :db/cardinality :db.cardinality/one
    :db/doc         "Wall-clock instant the norm was transacted."}
   {:db/ident       :migration/sha
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc         "SHA-256 of the norm body for drift detection."}])

(defn- list-resource-files [dir]
  (when-let [url (io/resource dir)]
    (let [protocol (.getProtocol url)]
      (case protocol
        "file"
        (->> (file-seq (io/file url))
             (filter #(.isFile ^java.io.File %))
             (map #(.getName ^java.io.File %))
             (filter #(str/ends-with? % ".edn"))
             (sort))
        "jar"
        (let [jar-path (subs (.getPath url) 5 (.indexOf (.getPath url) "!"))]
          (with-open [jar (java.util.jar.JarFile. jar-path)]
            (->> (enumeration-seq (.entries jar))
                 (map #(.getName ^java.util.jar.JarEntry %))
                 (filter #(and (str/starts-with? % (str dir "/"))
                               (str/ends-with? % ".edn")))
                 (map #(subs % (inc (count dir))))
                 (sort))))))))

(defn- read-norm [dir filename]
  (with-open [r (io/reader (io/resource (str dir "/" filename)))]
    (edn/read {:readers edn-readers} (java.io.PushbackReader. r))))

(defn- sha256 [^String s]
  (let [md (java.security.MessageDigest/getInstance "SHA-256")
        bs (.digest md (.getBytes s "UTF-8"))]
    (apply str (map #(format "%02x" %) bs))))

(defn- already-applied? [db migration-id]
  (some? (d/q '[:find ?e .
                :in $ ?id
                :where [?e :migration/id ?id]]
              db migration-id)))

(defn- apply-norm! [conn dir filename]
  (let [body  (read-norm dir filename)
        sha   (sha256 (pr-str body))
        mig-id (str dir "/" filename)]
    (if (already-applied? (d/db conn) mig-id)
      (log/debug {:event :migration/skipped :id mig-id})
      (do
        (log/info {:event :migration/applying :id mig-id})
        @(d/transact conn body)
        @(d/transact conn [{:migration/id         mig-id
                            :migration/applied-at (java.util.Date.)
                            :migration/sha        sha}])))))

(defn- run-norms! [conn dir]
  (when-let [files (seq (list-resource-files dir))]
    (doseq [f files]
      (apply-norm! conn dir f))))

(defrecord Migrator [config datomic]
  component/Lifecycle
  (start [this]
    (let [conn       (datomic/conn datomic)
          schema-dir (get-in (:config config) [:migrations :schema-dir] "schema")
          tx-fns-dir (get-in (:config config) [:migrations :tx-fns-dir] "tx-fns")]
      @(d/transact conn bootstrap-norm)
      (run-norms! conn schema-dir)
      (run-norms! conn tx-fns-dir)
      (log/info {:event :migrator/done})
      this))
  (stop [this] this))

(defn new-migrator []
  (->Migrator nil nil))
