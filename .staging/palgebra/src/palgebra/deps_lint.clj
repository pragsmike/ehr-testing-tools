(ns palgebra.deps-lint
  "Dependency-direction lint (design D9, docs/palgebra-design.md §I.7):
  `palgebra.*` never requires `ehr-testing-tools.*`, not even in
  tests -- the discipline that keeps palgebra extractable as its own
  repo without an EHR-shaped dependency to untangle first. Parses
  every `.clj` file's leading `ns` form under a root directory
  (default `palgebra`, so `palgebra/src` and `palgebra/test` are both
  covered by one scan) and fails on any require of an
  `ehr-testing-tools.*` namespace.

  Reads only the first form of each file (Clojure convention: the `ns`
  form is always first) with `*read-eval*` bound off -- this namespace
  never evaluates the files it scans, only reads their syntax."
  (:require [clojure.java.io :as io]
            [clojure.string :as str]))

(defn- clj-files
  [root]
  (->> (file-seq (io/file root))
       (filter (fn [f] (.isFile f)))
       (filter (fn [f] (str/ends-with? (.getName f) ".clj")))))

(defn- read-ns-form
  [file]
  (with-open [rdr (java.io.PushbackReader. (io/reader file))]
    (binding [*read-eval* false]
      (read rdr false nil))))

(defn- required-symbols
  "Every namespace symbol named in an `ns` form's `:require` clause --
  handles both bare-symbol and `[ns-sym & opts]` vector clauses."
  [ns-form]
  (->> ns-form
       (filter #(and (seq? %) (= :require (first %))))
       (mapcat rest)
       (map (fn [clause] (if (vector? clause) (first clause) clause)))))

(defn- ehr-namespace?
  [sym]
  (str/starts-with? (str sym) "ehr-testing-tools"))

(defn violations
  "Every {:file :required} pair where a palgebra `.clj` file under
  `root` requires an `ehr-testing-tools.*` namespace."
  [root]
  (->> (clj-files root)
       (mapcat (fn [file]
                 (let [ns-form (read-ns-form file)
                       bad (filter ehr-namespace? (required-symbols ns-form))]
                   (map (fn [sym] {:file (.getPath file) :required (str sym)}) bad))))
       vec))

(defn lint
  "Returns {:ok? bool :violations [{:file :required} ...]}."
  ([] (lint {}))
  ([{:keys [root] :or {root "palgebra"}}]
   (let [vs (violations root)]
     {:ok? (empty? vs) :violations vs})))

(defn lint-deps!
  "-X-invokable: runs the dependency-direction lint over the real
  palgebra/ tree, prints a summary, and exits non-zero on any
  violation -- `make lint-deps` fails the build the way any other
  lint would."
  [_]
  (let [{:keys [ok? violations]} (lint)]
    (if ok?
      (println "lint-deps: OK -- no palgebra.* namespace requires ehr-testing-tools.*")
      (do (println "lint-deps: FAILED")
          (doseq [{:keys [file required]} violations]
            (println (str "  " file " requires " required)))
          (System/exit 1)))))
