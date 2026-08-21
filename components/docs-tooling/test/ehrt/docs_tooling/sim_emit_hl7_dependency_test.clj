(ns ehrt.docs-tooling.sim-emit-hl7-dependency-test
  "Register row S5 / AR-F2-2 (alignment fixes 2, 2026-08-05,
  `notes/adr/0051-alignment-fixes-2.md`): AGENTS.md's own Constraints
  section states, in prose only until now, `components/sim-emit-hl7`
  must never depend on anything but `components/sim-model` (never on
  `components/sim` or `components/patient-simulator` themselves) --
  enforced by `poly check`'s general brick-graph rules plus vigilance,
  never by a named test (S5's own finding: no `deftest` in this family
  checked it). This test promotes that prose constraint to a gate.

  Reads each source file's own `ns` form with the Clojure reader and
  walks its `:require` clause's entries -- never a regex over the raw
  file text, which would trip on the SAME namespace names appearing as
  plain prose inside a docstring (this component's own files cite
  `ehrt.sim-engine.engine/run` and kin conversationally, in docstrings,
  without ever `:require`-ing them; a naive substring scan would
  misreport those as violations)."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.java.io :as io]
            [clojure.string :as str]))

(defn- clj-files [dir]
  (->> (file-seq (io/file dir))
       (filter #(.isFile %))
       (filter #(str/ends-with? (.getName %) ".clj"))
       (map #(.getPath %))
       sort))

(defn- read-first-form
  "Reads only the first top-level form of the file at `path` -- the `ns`
  form itself, including its docstring, but never anything past it."
  [path]
  (with-open [rdr (java.io.PushbackReader. (io/reader path))]
    (read rdr)))

(defn- require-entries
  "The `(:require ...)` clause's own entries from a parsed `ns` form, or
  `nil` if none."
  [ns-form]
  (->> ns-form
       (filter #(and (seq? %) (= :require (first %))))
       first
       rest))

(defn- required-ehrt-namespaces
  "Every `ehrt.*` namespace symbol (as a string) a parsed `ns` form's own
  `:require` clause names -- vector-form (`[ns :as alias]`) and bare-
  symbol forms both handled."
  [ns-form]
  (->> (require-entries ns-form)
       (map #(str (if (vector? %) (first %) %)))
       (filter #(str/starts-with? % "ehrt."))))

(defn- sim-emit-hl7-allowed-require? [ns-str]
  (or (str/starts-with? ns-str "ehrt.sim-model.")
      (str/starts-with? ns-str "ehrt.sim-emit-hl7.")))

(deftest sim-emit-hl7-src-requires-nothing-beyond-sim-model-and-its-own-namespaces-test
  (doseq [path (clj-files "components/sim-emit-hl7/src")]
    (let [required (required-ehrt-namespaces (read-first-form path))
          disallowed (remove sim-emit-hl7-allowed-require? required)]
      (is (empty? disallowed)
          (str path " requires ehrt.* namespace(s) outside sim-model/its own component: " disallowed)))))

;; -- mechanism-sanity: prove the extraction/allow-list functions actually catch what they claim to --

(deftest required-ehrt-namespaces-extraction-is-actually-caught-test
  (let [form (read-string
               (str "(ns ehrt.sim-emit-hl7.scratch \"A docstring mentioning "
                    "ehrt.sim-engine.engine/run as prose, never a require.\" "
                    "(:require [ehrt.sim-model.interface :as sim-model] "
                    "[ehrt.corpus.interface :as corpus] "
                    "[clojure.string :as str]))"))]
    (is (= ["ehrt.sim-model.interface" "ehrt.corpus.interface"]
           (required-ehrt-namespaces form))
        "docstring prose must never be mistaken for a require, and non-ehrt requires must be filtered out")))

(deftest sim-emit-hl7-allowed-require-predicate-is-actually-caught-test
  (testing "sim-model and the component's own namespaces are allowed"
    (is (sim-emit-hl7-allowed-require? "ehrt.sim-model.interface"))
    (is (sim-emit-hl7-allowed-require? "ehrt.sim-emit-hl7.site-profile")))
  (testing "sim itself, patient-simulator, and any other domain namespace are disallowed"
    (is (not (sim-emit-hl7-allowed-require? "ehrt.sim.interface")))
    (is (not (sim-emit-hl7-allowed-require? "ehrt.patient-simulator.interface")))
    (is (not (sim-emit-hl7-allowed-require? "ehrt.corpus.interface")))))
