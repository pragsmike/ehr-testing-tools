(ns ehrt.judge-v2-nist.taxonomy-currency-test
  "The currency gate for `resources/judge-v2-nist/taxonomy.edn`
  (pairing-as-data, ADR-0088, AR-PD-3): re-derives the NIST engine's
  own classification/category display names directly from the
  RESOLVED jar's own packaged `reference.conf` (via the transitive
  `com.typesafe.config` dependency judge-v2-nist's own deps.edn already
  names, ADR-0012) on every run, and fails on drift against the
  committed snapshot -- the `notice_verbatim` shape (vendoring arc)
  applied to a vocabulary rather than a byte-for-byte artifact: a
  version bump that adds, removes, or renames a classification or
  category breaks this test instead of leaving the committed snapshot
  silently stale.

  Second gate, same file: every `:expected` member of every registry
  row whose `:judge` is `:judge-v2-nist` must be a category name this
  snapshot actually declares (AR-PD-3's own \"schema-level check\") --
  checked here, at the test-tier boundary that can legitimately cross
  into `ehrt.judge.interface` (`judge` and `judge-v2-nist` are
  siblings; neither's own `deps.edn` gains a new edge from this)."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [ehrt.judge.interface :as judge])
  (:import [com.typesafe.config ConfigFactory Config]))

(def ^:private snapshot-resource "judge-v2-nist/taxonomy.edn")

(defn- committed-snapshot []
  (edn/read-string (slurp (io/resource snapshot-resource))))

(defn- unwrap-str-map [^Config c path]
  (into (sorted-map)
        (map (fn [[k v]] [(str k) (str v)]))
        (.unwrapped (.root (.getConfig c path)))))

(defn- derive-from-jar []
  (let [report-cfg (.getConfig (ConfigFactory/load "reference.conf") "report")]
    {:classifications (unwrap-str-map report-cfg "classification")
     :categories (unwrap-str-map report-cfg "category")}))

(deftest committed-taxonomy-matches-the-resolved-jar-test
  (testing "names-only: classifications and categories, nothing else -- no
            template text, no per-detection config bodies"
    (let [committed (committed-snapshot)
          derived (derive-from-jar)]
      (is (= (:classifications derived) (:classifications committed))
          "the resolved jar's own classification display names drifted from the committed snapshot")
      (is (= (:categories derived) (:categories committed))
          "the resolved jar's own category display names drifted from the committed snapshot"))))

(deftest registry-nist-rows-draw-expected-from-the-taxonomy-test
  (let [{:keys [categories]} (committed-snapshot)
        category-names (set (vals categories))
        rows (judge/load-pairing-registry)
        nist-rows (filter #(= :judge-v2-nist (:judge %)) rows)]
    (testing "at least one row was actually witnessed against judge-v2-nist"
      (is (seq nist-rows)))
    (doseq [{:keys [operator expected]} nist-rows]
      (testing (str (:id operator) "'s :expected classes are known NIST category names")
        (is (every? category-names expected)
            (str "unknown category name(s): " (remove category-names expected)))))))
