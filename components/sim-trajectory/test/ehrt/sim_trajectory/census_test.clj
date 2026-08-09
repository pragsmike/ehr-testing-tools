(ns ehrt.sim-trajectory.census-test
  "Co-landing invariants for the GMF census tool (ADR-0034): the census's
  own verdicts carry the properties AR-2/AR-3/AR-4 claim for them, proven
  against small inline fixture modules (never against the real Synthea
  catalog -- that is Step 2's own committed artifact, not a unit test's
  job) -- one fixture per verdict class, plus the AR-3 substitution tag.

  Standing-equipment promotion (2026-08-05, `notes/ADRs.md` promotion
  ADR, AR-P-1): moved verbatim (namespace unchanged) from
  `development/test` into this component's own test tree -- equipment,
  not API. This move is the first time these 7 tests ever actually ran
  under `poly test`. **Citation correction (2026-08-05, scaffolding
  compaction A, `notes/ADRs.md` ADR-0044's own execution note):**
  this was originally attributed here to \"the roadmap's own Wave I
  finding\" -- no such row exists (GMF Wave I is a different, unrelated
  arc); the invisibility claim itself was independently confirmed by a
  live before/after `poly test` run (202 blocks before the promotion,
  0 census assertions among them; 204 after), so it stands on that
  evidence, not the retracted citation. Running them for real found
  two fixtures had gone stale in the interim -- see `load-failed-json`
  and `walk-failed-json`'s own dated notes below."
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [ehrt.sim-trajectory.census :as census]))

(defn- write-fixture! ^java.io.File [dir id json-text]
  (let [f (io/file dir (str id ".json"))]
    (io/make-parents f)
    (spit f json-text)
    f))

(defn- write-module-file! ^java.io.File [checkout-dir call-path json-text]
  (let [f (io/file checkout-dir "src" "main" "resources" "modules" (str call-path ".json"))]
    (io/make-parents f)
    (spit f json-text)
    f))

(defn- write-table-file! ^java.io.File [checkout-dir table-name csv-text]
  (let [f (io/file checkout-dir "src" "main" "resources" "modules" "lookup_tables" table-name)]
    (io/make-parents f)
    (spit f csv-text)
    f))

(def ^:private census-opts
  {:seed-count 3 :mixer-seed 20260803
   :registration-offset-years 30 :horizon-years 50})

(def ^:private ok-json
  "Trivially walkable regardless of persona/seed: Initial -> Terminal."
  (str "{\"name\": \"Census OK Fixture\","
       " \"states\": {"
       "   \"Initial\": {\"type\": \"Initial\", \"direct_transition\": \"Done\"},"
       "   \"Done\": {\"type\": \"Terminal\"}"
       " }}"))

(def ^:private load-failed-json
  "Standing-equipment promotion (2026-08-05, `notes/ADRs.md` promotion
  ADR): found live, moving this file under `poly test` for the first
  time ever (the citation-corrected invisibility finding above, see
  the namespace docstring's own 2026-08-05 dated note) -- VitalSign,
  this fixture's own prior 'still-deferred v1 state type', was ITSELF
  landed for real by GMF
  coverage Wave VS (2026-08-04, ADR-0039 AR-1) three sessions before
  this test file was ever actually exercised, so the fixture had gone
  stale silently: `census-one` now returns `:ok-walked` for it, not
  `:load-failed`. Swapped to a deliberately FICTIONAL state type name
  (`NoSuchStateType`, not upstream Synthea vocabulary, not a v1
  candidate) rather than another real-but-currently-deferred type --
  `gmf-type->keyword` (gmf.clj) is a closed whitelist by construction,
  so any string that is not a key stays permanently unrecognized,
  immune to the next coverage wave going stale the same way this one
  did. The same 'stale premise, not silently left' treatment
  `gmf-test`'s own deferred-type fixtures already document, one layer
  more future-proof."
  (str "{\"name\": \"Census Load-Failed Fixture\","
       " \"states\": {"
       "   \"Initial\": {\"type\": \"Initial\", \"direct_transition\": \"Scan\"},"
       "   \"Scan\": {\"type\": \"NoSuchStateType\", \"direct_transition\": \"Done\"},"
       "   \"Done\": {\"type\": \"Terminal\"}"
       " }}"))

(def ^:private walk-failed-json
  "Loads clean (no state-type/schema gate fires -- gmf.clj's own loader
  does not validate condition-type vocabulary, only state types) but a
  Guard whose :allow names an unrecognized condition type throws at
  `evaluate-condition`'s own default case the moment the walk reaches
  it -- every fixture seed reaches Initial's own unconditional
  transition into the Guard first, so every seed throws.

  Standing-equipment promotion (2026-08-05, `notes/ADRs.md` promotion
  ADR): the SAME staleness this file's `load-failed-json` fixture,
  above, found live -- ':vital-sign' (raw JSON 'Vital Sign') was
  ITSELF landed by GMF coverage Wave VS (2026-08-04, ADR-0039 AR-1/AR-4,
  `vital-sign-condition-holds?`) before this test file was ever
  actually exercised under `poly test`, so this fixture no longer
  throws either -- it now walks clean to `:ok-walked`. Swapped to a
  fictional condition type ('No Such Condition Type', slugging to
  `:no-such-condition-type`) for the same reason: `evaluate-condition`'s
  `case` dispatch (gmf_interpreter.clj) throws on its default branch for
  ANY keyword not one of its named clauses, so a fictional name stays
  permanently unrecognized rather than going stale at the next wave
  that adds a condition type."
  (str "{\"name\": \"Census Walk-Failed Fixture\","
       " \"states\": {"
       "   \"Initial\": {\"type\": \"Initial\", \"direct_transition\": \"Blocked\"},"
       "   \"Blocked\": {\"type\": \"Guard\","
       "     \"allow\": {\"condition_type\": \"No Such Condition Type\"},"
       "     \"direct_transition\": \"Done\"},"
       "   \"Done\": {\"type\": \"Terminal\"}"
       " }}"))

(def ^:private wellness-json
  "A bare `wellness: true` Encounter with no `encounter_class` key.
  ADR-0031 AR-5(b)'s own timing-substitution trigger -- GMF coverage
  Wave G (2026-08-03, ADR-0037 AR-3) retires the substitution this
  fixture used to prove the tag fired on: `wellness: true` now loads as
  its own `:wellness-wait` state type and the interpreter genuinely
  waits (`next-wellness-tick`), still landing `:ok-walked` (a real wait
  is well within this census's own 50-year horizon)."
  (str "{\"name\": \"Census Wellness Fixture\","
       " \"states\": {"
       "   \"Initial\": {\"type\": \"Initial\", \"direct_transition\": \"Visit\"},"
       "   \"Visit\": {\"type\": \"Encounter\", \"wellness\": true, \"direct_transition\": \"End\"},"
       "   \"End\": {\"type\": \"EncounterEnd\", \"direct_transition\": \"Done\"},"
       "   \"Done\": {\"type\": \"Terminal\"}"
       " }}"))

(def ^:private zero-content-json
  "Same shape as `ok-json` (Initial -> Terminal, no event-producing state in
  between) -- walks clean on every seed but emits no trajectory event at
  all, the `:zero-on-every-seed` substance case (census substance, AR-VC-2)."
  (str "{\"name\": \"Census Zero-Content Fixture\","
       " \"states\": {"
       "   \"Initial\": {\"type\": \"Initial\", \"direct_transition\": \"Done\"},"
       "   \"Done\": {\"type\": \"Terminal\"}"
       " }}"))

(def ^:private produces-content-json
  "An unconditional, non-wellness ambulatory Encounter/EncounterEnd pair --
  both direct transitions, no wait, no RNG-gated branch -- so every seed
  deterministically emits two trajectory events, the `:produces-content`
  substance case (census substance, AR-VC-2)."
  (str "{\"name\": \"Census Content Fixture\","
       " \"states\": {"
       "   \"Initial\": {\"type\": \"Initial\", \"direct_transition\": \"Visit\"},"
       "   \"Visit\": {\"type\": \"Encounter\", \"encounter_class\": \"ambulatory\","
       "     \"direct_transition\": \"End\"},"
       "   \"End\": {\"type\": \"EncounterEnd\", \"direct_transition\": \"Done\"},"
       "   \"Done\": {\"type\": \"Terminal\"}"
       " }}"))

(def ^:private physiology-json
  "GMF coverage Wave G (2026-08-03, ADR-0037 AR-5): `Physiology` is a
  real, unsupported v1 state type (Synthea's own ODE-based physiology
  engine, found in `gallstones.json`) -- RULED out of scope, not a
  load gap still to close. This fixture's ENTIRE load gap is exactly
  this one unrecognized type, nothing else -- the single-cause shape
  `out-of-scope-by-ruling?` classifies."
  (str "{\"name\": \"Census Physiology Fixture\","
       " \"states\": {"
       "   \"Initial\": {\"type\": \"Initial\", \"direct_transition\": \"Model\"},"
       "   \"Model\": {\"type\": \"Physiology\", \"direct_transition\": \"Done\"},"
       "   \"Done\": {\"type\": \"Terminal\"}"
       " }}"))

(deftest ok-walked-module-censuses-clean
  (let [dir (io/file (System/getProperty "java.io.tmpdir") "census-test-ok")
        file (write-fixture! dir "census-ok-fixture" ok-json)
        entry (census/census-one dir census-opts {:id "census-ok-fixture" :file file})]
    (is (= :ok-walked (:verdict entry)))
    (is (= [] (:disclosed-substitutions entry)))
    (is (= 3 (count (:walks entry))))
    (is (every? :digest (:walks entry)))
    (is (empty? (:walk-errors (:gap entry))))))

(deftest load-failed-module-names-the-unrecognized-state-type
  (let [dir (io/file (System/getProperty "java.io.tmpdir") "census-test-load-failed")
        file (write-fixture! dir "census-load-failed-fixture" load-failed-json)
        entry (census/census-one dir census-opts {:id "census-load-failed-fixture" :file file})]
    (is (= :load-failed (:verdict entry)))
    (is (= [] (:walks entry)))
    (is (contains? (get-in entry [:gap :unrecognized-state-types]) "NoSuchStateType"))))

(deftest walk-failed-module-names-every-throwing-seed
  (let [dir (io/file (System/getProperty "java.io.tmpdir") "census-test-walk-failed")
        file (write-fixture! dir "census-walk-failed-fixture" walk-failed-json)
        entry (census/census-one dir census-opts {:id "census-walk-failed-fixture" :file file})]
    (is (= :walk-failed (:verdict entry)))
    (is (= 3 (count (:walks entry))))
    (testing "every seed throws on the same unrecognized condition type -- caught, recorded, not propagated"
      (is (= 3 (count (get-in entry [:gap :walk-errors]))))
      (is (every? #(= :no-such-condition-type (get-in % [:error :data :condition-type]))
                  (get-in entry [:gap :walk-errors]))))))

(deftest wellness-substitution-detector-is-retired-no-tag-ever-emitted
  (testing "GMF coverage Wave G (2026-08-03, ADR-0037 AR-5): the
            substitution `wellness-substitution?` scanned for is GONE
            (ADR-0037 AR-3 retired it) -- `census-one` no longer calls
            it, so `:disclosed-substitutions` stays empty even against
            the SAME raw shape that used to trigger the tag. The
            underlying scan function itself is kept as history, not
            deleted, and still behaves exactly as documented if called
            directly"
    (is (true? (census/wellness-substitution? {"root" wellness-json})))
    (is (false? (census/wellness-substitution? {"root" ok-json})))
    (is (false? (census/wellness-substitution? {"root" load-failed-json}))))
  (testing "but a real censused module -- even one carrying the old
            trigger shape -- emits NO substitution tag anymore"
    (let [dir (io/file (System/getProperty "java.io.tmpdir") "census-test-wellness")
          file (write-fixture! dir "census-wellness-fixture" wellness-json)
          entry (census/census-one dir census-opts {:id "census-wellness-fixture" :file file})]
      (is (= :ok-walked (:verdict entry)))
      (is (= [] (:disclosed-substitutions entry))))))

(deftest physiology-only-gap-classifies-as-out-of-scope-by-ruling
  (testing "GMF coverage Wave G (2026-08-03, ADR-0037 AR-5): a
            :load-failed closure whose ENTIRE gap is the ruled-out
            Physiology type reclassifies to :out-of-scope-by-ruling,
            never :load-failed -- a genuine load gap this project has
            no equivalent for BY RULING, not one still to close"
    (let [dir (io/file (System/getProperty "java.io.tmpdir") "census-test-physiology")
          file (write-fixture! dir "census-physiology-fixture" physiology-json)
          entry (census/census-one dir census-opts {:id "census-physiology-fixture" :file file})]
      (is (= :out-of-scope-by-ruling (:verdict entry)))
      (is (= [] (:walks entry)))
      (is (contains? (get-in entry [:gap :unrecognized-state-types]) "Physiology")))))

(deftest out-of-scope-by-ruling-predicate-requires-a-single-clean-cause
  (testing "out-of-scope-by-ruling? is a single-cause classifier --
            proven directly against hand-built gap maps, since
            `load-module`'s own short-circuiting `cond` (first bad
            state wins) makes a genuinely mixed gap hard to construct
            honestly through the loader alone (only one unrecognized
            state type can ever surface from a single root file)"
    (is (true? (census/out-of-scope-by-ruling?
                {:unrecognized-state-types #{"Physiology"}
                 :unresolved-submodules #{} :unresolved-tables #{}
                 :malformed-lookup-table-ranges #{} :attribute-collisions #{}
                 :cyclic-closure nil :other-rejections []}))
        "the clean case: the ENTIRE gap is the ruled-out type")
    (is (false? (census/out-of-scope-by-ruling?
                 {:unrecognized-state-types #{"Physiology" "VitalSign"}
                  :unresolved-submodules #{} :unresolved-tables #{}
                  :malformed-lookup-table-ranges #{} :attribute-collisions #{}
                  :cyclic-closure nil :other-rejections []}))
        "a SECOND, non-ruled unrecognized type keeps it :load-failed")
    (is (false? (census/out-of-scope-by-ruling?
                 {:unrecognized-state-types #{"Physiology"}
                  :unresolved-submodules #{"some/missing"} :unresolved-tables #{}
                  :malformed-lookup-table-ranges #{} :attribute-collisions #{}
                  :cyclic-closure nil :other-rejections []}))
        "a genuinely unrelated gap (a missing submodule) alongside the
         ruled-out type ALSO keeps it :load-failed, never silently
         absorbed into the ruled-out bucket")
    (is (false? (census/out-of-scope-by-ruling? {:unrecognized-state-types #{}})) "an empty gap is not this classifier's business")))

(deftest verify-pin-falls-back-to-content-hash-with-no-git-checkout
  (let [dir (io/file (System/getProperty "java.io.tmpdir") "census-test-no-git")]
    (write-fixture! (io/file dir "src" "main" "resources" "modules") "x" ok-json)
    (let [result (census/verify-pin dir census/synthea-pin)]
      (is (= :ok (:status result)))
      (is (= :sha256-content (:method (:payload result))))
      (is (true? (:pin-unverified-by-git (:payload result)))))))

;; --- Census substance (2026-08-07, ADR-0069, AR-VC-2) ---------------------
;;
;; A module that produces zero trajectory events on every smoke-walk seed
;; censuses `:ok-walked` identically to one with rich content (roadmap
;; "Census tool refinements" row (a), `gmf-interpreter-findings.md` §15's
;; own AR-8b substance note). `:substance` is ADDITIVE on an `:ok-walked`
;; row only -- the verdict enum itself does not change.

(deftest ok-walked-zero-content-module-carries-the-zero-on-every-seed-substance-tag
  (let [dir (io/file (System/getProperty "java.io.tmpdir") "census-test-zero-content")
        file (write-fixture! dir "census-zero-content-fixture" zero-content-json)
        entry (census/census-one dir census-opts {:id "census-zero-content-fixture" :file file})]
    (is (= :ok-walked (:verdict entry)))
    (is (= :zero-on-every-seed (:substance entry)))
    (is (= [0 0 0] (:event-counts entry)))))

(deftest ok-walked-content-producing-module-carries-the-produces-content-substance-tag
  (let [dir (io/file (System/getProperty "java.io.tmpdir") "census-test-produces-content")
        file (write-fixture! dir "census-produces-content-fixture" produces-content-json)
        entry (census/census-one dir census-opts {:id "census-produces-content-fixture" :file file})]
    (is (= :ok-walked (:verdict entry)))
    (is (= :produces-content (:substance entry)))
    (is (= [2 2 2] (:event-counts entry)))))

(deftest non-ok-walked-rows-carry-no-substance-tag
  (let [dir (io/file (System/getProperty "java.io.tmpdir") "census-test-load-failed-substance")
        file (write-fixture! dir "census-load-failed-fixture" load-failed-json)
        entry (census/census-one dir census-opts {:id "census-load-failed-fixture" :file file})]
    (is (= :load-failed (:verdict entry)))
    (is (nil? (:substance entry)))
    (is (nil? (:event-counts entry)))))

(deftest summarize-tallies-ok-walked-by-substance
  (let [dir (io/file (System/getProperty "java.io.tmpdir") "census-test-summarize-substance")
        zero-file (write-fixture! dir "census-zero-content-fixture" zero-content-json)
        content-file (write-fixture! dir "census-produces-content-fixture" produces-content-json)
        modules [(census/census-one dir census-opts {:id "census-zero-content-fixture" :file zero-file})
                 (census/census-one dir census-opts {:id "census-produces-content-fixture" :file content-file})]
        summary (census/summarize modules)]
    (is (= {:zero-on-every-seed 1 :produces-content 1} (:ok-walked-by-substance summary)))))

;; --- Census artifact filename disambiguation (2026-08-07, ADR-0069, AR-VC-3) --
;;
;; Roadmap "Census tool refinements" row (c): the artifact filename has no
;; same-calendar-day disambiguation -- worked around by hand-appending a
;; wave suffix in both the F0 and F re-runs, never fixed in the tool. An
;; optional label makes the filename disambiguate itself.

(deftest artifact-filename-without-label-is-unchanged
  (is (= "2026-08-07-synthea-7e08387.edn"
         (census/artifact-filename "2026-08-07" "7e08387" nil))))

(deftest artifact-filename-with-label-appends-it
  (is (= "2026-08-07-synthea-7e08387-substance.edn"
         (census/artifact-filename "2026-08-07" "7e08387" "substance"))))

;; --- closure-file-count counts lookup tables too (ADR-0094, ruling 6 =
;; D6-1, "a") -- both branches had converged on JSON-modules-only: the
;; ok-walked branch's `(count modules)` never touched `tables`, the
;; load-failed branch's `(count @fetched)` was populated only by
;; `make-resolve-fn` (JSON module reads), never `make-table-resolve-fn`.
;; Three real disclosed undercounts (asthma 3 vs 11, vhd-pulmonic and
;; vhd-tricuspid 2 vs 4 each -- register D6-1, ADR-0074) motivate these
;; gates. -----------------------------------------------------------------

(def ^:private closure-count-leaf-json
  (str "{\"name\": \"ClosureCountLeaf\", \"states\": {"
       "  \"Initial\": {\"type\": \"Initial\", \"direct_transition\": \"Done\"},"
       "  \"Done\": {\"type\": \"Terminal\"}}}"))

(def ^:private closure-count-good-table-csv
  "age,gender,A,B\n15-24,F,0.9,0.1\n15-24,M,0.2,0.8\n")

(deftest ok-walked-module-with-submodule-and-table-counts-all-distinct-files
  (testing "1 root + 1 submodule + 1 lookup table = 3 -- the ok-walked
            branch's own `:closure-file-count` must count `tables` too,
            not only `modules`"
    (let [dir (io/file (System/getProperty "java.io.tmpdir") "census-test-closure-count-ok")
          root-json (str "{\"name\": \"ClosureCountRoot\", \"states\": {"
                         "  \"Initial\": {\"type\": \"Initial\", \"direct_transition\": \"Call\"},"
                         "  \"Call\": {\"type\": \"CallSubmodule\", \"submodule\": \"closure-count-leaf\","
                         "    \"direct_transition\": \"Pick\"},"
                         "  \"Pick\": {\"type\": \"Simple\", \"lookup_table_transition\": ["
                         "    {\"transition\": \"A\", \"default_probability\": 0.5,"
                         "     \"lookup_table_name\": \"closure-count-good.csv\"},"
                         "    {\"transition\": \"B\", \"default_probability\": 0.5,"
                         "     \"lookup_table_name\": \"closure-count-good.csv\"}]},"
                         "  \"A\": {\"type\": \"Terminal\"},"
                         "  \"B\": {\"type\": \"Terminal\"}}}")
          file (write-fixture! dir "closure-count-root" root-json)]
      (write-module-file! dir "closure-count-leaf" closure-count-leaf-json)
      (write-table-file! dir "closure-count-good.csv" closure-count-good-table-csv)
      (let [entry (census/census-one dir census-opts {:id "closure-count-root" :file file})]
        (is (= :ok-walked (:verdict entry)))
        (is (= 3 (:closure-file-count (:gap entry)))
            "1 root + 1 submodule + 1 table")))))

(deftest load-failed-closure-counts-a-table-successfully-read-before-the-failure
  (testing "root names two tables -- the first resolves, the second is
            missing and fails the closure. The table read BEFORE the
            failure must still be counted: `make-table-resolve-fn` must
            record into the same `fetched` atom `make-resolve-fn` already
            uses, under a collision-proof key so a table read and a
            module read of the same call-path never collide"
    (let [dir (io/file (System/getProperty "java.io.tmpdir") "census-test-closure-count-load-failed")
          root-json (str "{\"name\": \"ClosureCountLoadFailedRoot\", \"states\": {"
                         "  \"Initial\": {\"type\": \"Initial\", \"direct_transition\": \"Pick\"},"
                         "  \"Pick\": {\"type\": \"Simple\", \"lookup_table_transition\": ["
                         "    {\"transition\": \"A\", \"default_probability\": 0.5,"
                         "     \"lookup_table_name\": \"closure-count-good.csv\"},"
                         "    {\"transition\": \"B\", \"default_probability\": 0.5,"
                         "     \"lookup_table_name\": \"closure-count-missing.csv\"}]},"
                         "  \"A\": {\"type\": \"Terminal\"},"
                         "  \"B\": {\"type\": \"Terminal\"}}}")
          file (write-fixture! dir "closure-count-load-failed-root" root-json)]
      (write-table-file! dir "closure-count-good.csv" closure-count-good-table-csv)
      (let [entry (census/census-one dir census-opts {:id "closure-count-load-failed-root" :file file})]
        (is (= :load-failed (:verdict entry)))
        (is (contains? (get-in entry [:gap :unresolved-tables]) "closure-count-missing.csv"))
        (is (= 2 (:closure-file-count (:gap entry)))
            "1 root + the 1 table successfully read before the failure")))))

(deftest ok-walked-module-with-submodule-and-no-tables-still-counts-only-modules
  (testing "regression guard: a closure naming no lookup tables reports
            EXACTLY its module count (1 root + 1 submodule = 2), the
            no-table case's own semantics staying unchanged by the fix"
    (let [dir (io/file (System/getProperty "java.io.tmpdir") "census-test-closure-count-no-tables")
          root-json (str "{\"name\": \"ClosureCountNoTablesRoot\", \"states\": {"
                         "  \"Initial\": {\"type\": \"Initial\", \"direct_transition\": \"Call\"},"
                         "  \"Call\": {\"type\": \"CallSubmodule\", \"submodule\": \"closure-count-leaf\","
                         "    \"direct_transition\": \"Done\"},"
                         "  \"Done\": {\"type\": \"Terminal\"}}}")
          file (write-fixture! dir "closure-count-no-tables-root" root-json)]
      (write-module-file! dir "closure-count-leaf" closure-count-leaf-json)
      (let [entry (census/census-one dir census-opts {:id "closure-count-no-tables-root" :file file})]
        (is (= :ok-walked (:verdict entry)))
        (is (= 2 (:closure-file-count (:gap entry)))
            "1 root + 1 submodule, no tables")))))
