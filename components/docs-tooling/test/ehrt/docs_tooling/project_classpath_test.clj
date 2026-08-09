(ns ehrt.docs-tooling.project-classpath-test
  "Cluster A, D2-18 (repo review 2, `.agents/plans/2026-08-09-repo-
  review-findings.md`; `notes/adr/0092-repo-review-2.md`; landed
  `notes/adr/0095-cluster-a-gate-wiring.md`): no gate on the push lane
  (or anywhere routinely watched) ever checked a test file's own
  `:require` against the composing project's own classpath -- the
  `2088763` incident class (H-4): `components/judge/test/ehrt/judge/
  pairing_conviction_test.clj` (landed `948f5e5`) required
  `ehrt.judge-v2-nist.interface` directly, but `projects/integration/
  deps.edn` had dropped `poly/judge-v2-nist` on the premise that
  nothing on its classpath required it. `poly check` did not catch it
  (Polylith's own brick-graph reachability check is a SOURCE-tree
  check; this repo's own docstrings elsewhere explicitly disclose
  test-context cross-brick requires as deliberate and precedented,
  which is exactly what makes that graph blind to this hazard class).
  Only the next scheduled `Integration` run caught it, up to a day
  later.

  For every project named in `workspace.edn` (`development` included
  -- it composes the whole workspace, so it passes trivially, cheap
  generality rather than a special case), this test parses each
  composed brick's own TEST-tree `:require` forms, resolves every
  required `ehrt.<name>.*` namespace back to its owning brick, and
  asserts that brick is part of the composing project's own documented
  composition (its `deps.edn`, or -- for `development`, which has no
  `deps.edn` of its own -- the root `deps.edn`'s own alias named by
  its `workspace.edn` entry; `:necessary` overrides folded in too, so
  a documented-but-graph-invisible edge like `development`'s own
  `oracle` entry is never mistaken for a violation).

  Same reader-based extraction as `ehrt.docs-tooling.sim-emit-hl7-
  dependency-test`: parses each source file's own `ns` form with the
  Clojure reader and walks its `:require` clause's entries, never a
  regex over the raw file text, which would risk tripping on an
  `ehrt.*`-shaped namespace name appearing only as docstring prose."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.java.io :as io]
            [clojure.edn :as edn]
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

(defn- require-entries [ns-form]
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

(defn- owning-brick
  "The component/base name a `ehrt.<name>.*` namespace string names --
  its second dot-segment, exactly matching this repo's own namespace-
  mirrors-directory convention (`ehrt.judge-v2-nist.interface` ->
  `judge-v2-nist`)."
  [ns-str]
  (second (str/split ns-str #"\.")))

(defn- brick-names-from-deps-map
  "Every `poly/<name>` key's own `<name>`, string form, from a deps map
  (a project's own `:deps`, or a root-alias's own `:extra-deps`) --
  non-`poly` coordinates (`org.clojure/clojure`, cloverage, etc.) never
  mistaken for a brick."
  [deps-map]
  (->> (keys deps-map)
       (filter #(= "poly" (namespace %)))
       (map name)
       set))

(defn- workspace-edn [] (edn/read-string (slurp "workspace.edn")))
(defn- root-deps-edn [] (edn/read-string (slurp "deps.edn")))

(defn- project-composed-bricks
  "The brick-name set actually pulled onto `project-name`'s own
  classpath: its own `projects/<name>/deps.edn` `:deps`, or -- for a
  project with no `deps.edn` of its own (`development`) -- the root
  `deps.edn`'s own alias named by `project-config`'s own `:alias`."
  [project-name project-config]
  (let [own-deps-path (str "projects/" project-name "/deps.edn")]
    (if (.isFile (io/file own-deps-path))
      (brick-names-from-deps-map (:deps (edn/read-string (slurp own-deps-path))))
      (let [alias-kw (keyword (:alias project-config))]
        (brick-names-from-deps-map
          (get-in (root-deps-edn) [:aliases alias-kw :extra-deps]))))))

(defn- project-documented-bricks
  "`project-composed-bricks` unioned with `project-config`'s own
  `:necessary` override list (`workspace.edn`) -- a documented-but-
  graph-invisible composition edge is never mistaken for a violation."
  [project-name project-config]
  (into (project-composed-bricks project-name project-config)
        (:necessary project-config)))

(defn- brick-test-dir
  "`components/<name>/test` or `bases/<name>/test`, whichever exists;
  `nil` if `name` is neither (never expected in practice -- every
  brick name here came from a real `deps.edn` entry)."
  [brick-name]
  (cond
    (.isDirectory (io/file (str "components/" brick-name))) (str "components/" brick-name "/test")
    (.isDirectory (io/file (str "bases/" brick-name))) (str "bases/" brick-name "/test")
    :else nil))

(defn- violation?
  "`true` when `required-ns`'s own owning brick is absent from
  `documented-bricks` -- the pure predicate the `2088763` incident
  trips: judge's test tree required `ehrt.judge-v2-nist.interface`
  while `integration`'s own documented composition lacked
  `judge-v2-nist`."
  [documented-bricks required-ns]
  (not (documented-bricks (owning-brick required-ns))))

(defn- project-violations
  "Every violation triple for `project-name`: its own documented
  bricks' TEST-tree `:require` forms, each `ehrt.*` namespace checked
  against that SAME project's own documented composition."
  [project-name project-config]
  (let [documented (project-documented-bricks project-name project-config)]
    (for [brick documented
          :let [test-dir (brick-test-dir brick)]
          :when (and test-dir (.isDirectory (io/file test-dir)))
          path (clj-files test-dir)
          required-ns (required-ehrt-namespaces (read-first-form path))
          :when (violation? documented required-ns)]
      {:project project-name
       :test-file path
       :required-ns required-ns
       :owning-brick (owning-brick required-ns)})))

(deftest every-project-composed-test-tree-requires-only-documented-bricks-test
  (let [{:keys [projects]} (workspace-edn)
        violations (mapcat (fn [[project-name project-config]]
                              (project-violations project-name project-config))
                            projects)]
    (is (empty? violations)
        (str "test-tree require(s) naming a brick absent from the composing "
             "project's own deps.edn/:necessary list -- the `2088763` "
             "classpath-break class:\n"
             (str/join "\n"
               (for [{:keys [project test-file required-ns owning-brick]} violations]
                 (str "  " test-file " requires " required-ns " (brick "
                      owning-brick "), but project " project
                      " does not compose it")))))))

;; -- mechanism-sanity: prove the extraction/resolution functions actually catch what they claim to --

(deftest required-ehrt-namespaces-extraction-is-actually-caught-test
  (let [form (read-string
               (str "(ns ehrt.judge.scratch \"A docstring mentioning "
                    "ehrt.judge-v2-nist.interface as prose, never a require.\" "
                    "(:require [ehrt.judge-v2-nist.interface :as nist] "
                    "[ehrt.kernel.interface :as kernel] "
                    "[clojure.string :as str]))"))]
    (is (= ["ehrt.judge-v2-nist.interface" "ehrt.kernel.interface"]
           (required-ehrt-namespaces form))
        "docstring prose must never be mistaken for a require, and non-ehrt requires must be filtered out")))

(deftest owning-brick-extraction-is-actually-caught-test
  (is (= "judge-v2-nist" (owning-brick "ehrt.judge-v2-nist.interface")))
  (is (= "kernel" (owning-brick "ehrt.kernel.result")))
  (is (= "docs-tooling" (owning-brick "ehrt.docs-tooling.interface"))))

(deftest brick-names-from-deps-map-extraction-is-actually-caught-test
  (is (= #{"kernel" "judge-v2-nist"}
         (brick-names-from-deps-map
           {'poly/kernel        {:local/root "../../components/kernel"}
            'poly/judge-v2-nist {:local/root "../../components/judge-v2-nist"}
            'org.clojure/clojure {:mvn/version "1.12.5"}}))
      "a non-poly coordinate (org.clojure/clojure) must not be mistaken for a brick"))

(deftest violation-predicate-reproduces-the-2088763-incident-test
  (let [integration-bricks-before-fix
        #{"kernel" "judge" "judge-v2-hapi" "judge-fhir-official" "provenance"
          "corpus" "corpus-io" "sim" "sim-engine" "sim-model" "sim-trajectory"
          "sim-emit-hl7" "sim-emit-fhir" "sim-check"}
        integration-bricks-after-fix
        (conj integration-bricks-before-fix "judge-v2-nist")]
    (testing "before the fix (judge-v2-nist absent), judge's own pairing-conviction require trips"
      (is (violation? integration-bricks-before-fix "ehrt.judge-v2-nist.interface")))
    (testing "after the fix (judge-v2-nist re-added), the same require no longer trips"
      (is (not (violation? integration-bricks-after-fix "ehrt.judge-v2-nist.interface"))))))
