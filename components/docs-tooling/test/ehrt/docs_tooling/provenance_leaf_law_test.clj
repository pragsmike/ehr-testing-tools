(ns ehrt.docs-tooling.provenance-leaf-law-test
  "Register row S5 / AR-F2-3 (alignment fixes 2, 2026-08-05,
  `notes/adr/0051-alignment-fixes-2.md`): `components/provenance`'s own
  leaf law (`notes/adr/0043-sim-split-b-m1.md` AR-2 -- the single
  acyclic home both corpus and sim depend on for
  ManifestV0/V1/V1_1) has always meant provenance depends on nothing
  domain-shaped -- prose-and-vigilance only until now, the same gap S5
  found for sim-emit-hl7. This test promotes it to a gate: every
  `ehrt.*` namespace `components/provenance/src/` requires must itself
  be under `ehrt.provenance.*` (i.e. provenance may only require
  itself; it is a true leaf among the `ehrt.*` bricks).

  Scope note: this gate checks the src `:require` side only. Whether
  `components/provenance/deps.edn` declares no dependency beyond
  `metosin/malli` is `clojure -M:poly libs`/`poly check`'s own job
  (AR-AU-4's report-only-probes discipline), not duplicated here.

  Same reader-based extraction as `ehrt.docs-tooling.sim-emit-hl7-
  dependency-test` -- parses each source file's own `ns` form and walks
  its `:require` clause, never a regex over raw file text (which would
  risk tripping on an `ehrt.*`-shaped namespace name appearing only as
  docstring prose)."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.java.io :as io]
            [clojure.string :as str]))

(defn- clj-files [dir]
  (->> (file-seq (io/file dir))
       (filter #(.isFile %))
       (filter #(str/ends-with? (.getName %) ".clj"))
       (map #(.getPath %))
       sort))

(defn- read-first-form [path]
  (with-open [rdr (java.io.PushbackReader. (io/reader path))]
    (read rdr)))

(defn- require-entries [ns-form]
  (->> ns-form
       (filter #(and (seq? %) (= :require (first %))))
       first
       rest))

(defn- required-ehrt-namespaces [ns-form]
  (->> (require-entries ns-form)
       (map #(str (if (vector? %) (first %) %)))
       (filter #(str/starts-with? % "ehrt."))))

(defn- provenance-allowed-require? [ns-str]
  (str/starts-with? ns-str "ehrt.provenance."))

(deftest provenance-src-requires-nothing-beyond-its-own-namespaces-test
  (doseq [path (clj-files "components/provenance/src")]
    (let [required (required-ehrt-namespaces (read-first-form path))
          disallowed (remove provenance-allowed-require? required)]
      (is (empty? disallowed)
          (str path " requires ehrt.* namespace(s) outside provenance itself: " disallowed)))))

;; -- mechanism-sanity: prove the extraction/allow-list functions actually catch what they claim to --

(deftest required-ehrt-namespaces-extraction-is-actually-caught-test
  (let [form (read-string
               (str "(ns ehrt.provenance.scratch \"A docstring mentioning "
                    "ehrt.kernel.result/ok as prose, never a require.\" "
                    "(:require [ehrt.provenance.manifest :as manifest] "
                    "[ehrt.kernel.interface :as kernel] "
                    "[malli.core :as m]))"))]
    (is (= ["ehrt.provenance.manifest" "ehrt.kernel.interface"]
           (required-ehrt-namespaces form))
        "docstring prose must never be mistaken for a require, and non-ehrt requires must be filtered out")))

(deftest provenance-allowed-require-predicate-is-actually-caught-test
  (testing "provenance's own namespaces are allowed"
    (is (provenance-allowed-require? "ehrt.provenance.manifest"))
    (is (provenance-allowed-require? "ehrt.provenance.interface")))
  (testing "every other ehrt.* domain namespace is disallowed"
    (is (not (provenance-allowed-require? "ehrt.kernel.interface")))
    (is (not (provenance-allowed-require? "ehrt.corpus.interface")))
    (is (not (provenance-allowed-require? "ehrt.sim.interface")))))
