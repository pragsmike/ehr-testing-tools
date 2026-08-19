(ns ehrt.oracle.digest
  "Standing regression-oracle equipment (post-Wave-D cleanup session,
  2026-08-02, J1/J2, notes/ADRs.md ADR-0030). A regression-oracle claim
  in this repo means running THIS script -- fixed-seed golden runs for
  every pre-existing vendored root, written to files and SHA-256-
  digested by bin/regression-oracle's own worktree-and-diff harness --
  never a test-count or assertion-count comparison (the deviation D1b's
  own literal digest-across-a-worktree precedent set, then D2/D3 stood
  down from without a second literal check, per this session's own
  finding).

  Standing-equipment promotion (2026-08-05, `notes/ADRs.md` promotion
  ADR, AR-P-2): moved from `bin/oracle-src/ehrt/oracle/digest.clj` into
  this component, `components/oracle` -- each worktree `bin/
  regression-oracle` stands up now carries its OWN copy of this
  namespace, closing the J2 deferred row structurally (see the
  promotion ADR's own J2-closure narrative). The one licensed
  improvement this move makes: every require below repoints from a
  foreign component's IMPLEMENTATION namespace to its INTERFACE --
  `ehrt.sim-trajectory.gmf`/`gmf-interpreter` collapse into
  `ehrt.sim-trajectory.interface` (gaining `dob-epoch-day`, the one var
  not already there, added by the same session, evidenced by this
  file's own call sites); `ehrt.sim-engine.engine` repoints to
  `ehrt.sim-engine.interface` (already exposing `run` under the same
  name, so the `engine/run` call sites below are byte-unchanged).
  `run-walk`'s own 6-arg interpreter call becomes an 8-arg interface
  call with the SAME `{}`/`{}` defaults the impl's own 6-arg arity was
  already filling in internally (verified against
  `gmf_interpreter.clj`'s own arity chain) -- spelled out explicitly
  because the interface does not carry the 6-arg shorthand, not a
  behavior change. Every OTHER line of digest logic below is verbatim;
  Step 5 of the promotion session's own byte-identical oracle bracket
  is the proof.

  Deliberately independent of `ehrt.kernel.digest` and of any
  deps.edn alias landing in a historical commit: `bin/regression-
  oracle` runs THIS component (read from whichever worktree is under
  test, since the promotion -- previously always the current checkout,
  never a worktree's own copy, ADR-0030's own J2 design) with a
  synthetic, from-scratch classpath pointing every `:local/root` at
  that worktree -- so the SAME code here exercises two different
  component-code versions, and hashing itself happens in the calling
  shell (`sha256sum`), not in-process.

  CURRENT STATE, 2026-08-19 (ADR-0156, review-4 register row L1-5).
  `roots` holds 35 roots in two families:

    3 INTERPRETER-LAYER batches -- appendicitis/sore-throat/
      ear-infections. 100 well-mixed seeds x both sexes = 200 walks per
      root, concatenated into one deterministic EDN vector. The
      well-mixed-seed pattern every vendored-module test in this repo
      already uses: SEQUENTIAL small java.util.Random seeds are NOT
      well-distributed for their own first draw, confirmed repeatedly
      across GMF coverage waves.

   32 ENGINE-LAYER pairs -- engine/run plus emit-hl7/emit, ground truth
      AND emitted HL7 both captured, at the run-config each root's own
      vendored/engine test already established as producing real
      content.

  This paragraph replaced an opening that read `Six roots, matching this
  session's own J1 ruling verbatim` -- true when written and never
  updated past the two dated notes below, which carry the population to
  11. The other 24 arrived in `;;` comments inside the body. Nothing was
  false; a cold reader simply got a third of the population and no
  signal that it was a third. The count is gated now
  (`ehrt.docs-tooling.oracle-coverage-test`), and the COVERAGE block
  beside `roots` states what these 35 can and cannot witness.

  Dated note (2026-08-03, ADR-0033 AR-4b): three more roots join at the
  ENGINE layer -- ear-infections-engine/urinary-tract-infections-engine/
  total-joint-replacement-engine -- now that `engine.clj` threads a
  closure's own `:modules`/`:tables`/`:initial-attributes` through to
  `run-module` for real (J3 closed, `notes/ADRs.md` ADR-0033 AR-2/AR-3).
  This closes ADR-0032's own oracle-gap disclosure (its AR-4 dated
  correction: `total_joint_replacement` and the UTI closure could not be
  engine-layer digested before this session -- they threw or silenced).
  `ear-infections` (bare) stays the pre-existing INTERPRETER-layer batch,
  unchanged; `ear-infections-engine` is a genuinely new, separate root,
  not a replacement. These three are FIRST BASELINES, not a regression
  check against a prior digest -- there is no 'before' to compare
  against, since the round trip never completed before this session.
  Verifying this session's own claim that the SIX pre-existing roots
  stayed byte-identical across the ADR-0033 commits could not use this
  script directly, unmodified, across the baseline/target commit pair:
  this script's own header, above, documents that it is ALWAYS read
  from the current checkout (never a worktree's own copy) specifically
  so the SAME test code exercises two different component-code
  versions -- an assumption ADR-0033's own hard `:modules` shape switch
  (AR-2) falsified for the pre-existing `sinusitis`/`death-fixture`/
  `sepsis` producer functions, which now call `gmf/singleton-closure`
  (a function that does not exist before ADR-0033 -- a compile error at
  the baseline worktree, not a digest difference). The six-root identity
  check that session instead ran EACH commit's OWN `digest.clj` against
  its OWN worktree/classpath (not this file, fixed, across both) --
  disclosed as a deviation, not silently routed around; both digest
  tables are in that session's own record,
  `.agents/session-records/2026-08-03-engine-closure-context.md`.

  Dated note (2026-08-04, ADR-0042 AR-5, Wave H pre-roll): two MORE
  roots join, both FIRST BASELINES (`urinary-tract-infections-history-
  engine`/`ear-infections-history-engine`, below) -- `:history true`
  on the SAME closures/populations their own legacy (`:history`
  absent) siblings already digest, proving the opt-in path produces
  real, recordable content rather than re-checking an existing one.
  Unlike ADR-0033's own three, THIS session's own baseline/target pair
  IS runnable through `bin/regression-oracle` unmodified across both
  commits -- no hard API shape switch this time, `:history` is a purely
  additive config key and arity, absent at baseline (silently ignored,
  not read) and present at target -- so the nine PRE-EXISTING roots'
  own byte-identity is the real regression-oracle claim this session
  makes (AR-3's own gating argument, empirically confirmed, not merely
  asserted); the two NEW roots are EXPECTED to differ (baseline has no
  `:history` support to gate under, target does) and that difference is
  not a regression -- see the session record for the full manifest and
  this note's own disambiguation."
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [ehrt.sim-trajectory.interface :as sim-trajectory]
            [ehrt.sim-model.interface :as sim-model]
            [ehrt.sim-engine.interface :as engine]
            [ehrt.sim-emit-hl7.interface :as emit-hl7])
  (:import [java.util Random]))

(defn- person [seed sex] (assoc (sim-model/persona (Random. seed) {}) :sex sex))

(defn- mixed-seeds
  "The established mixer-RNG pattern (vendored_appendicitis_test.clj,
  reused verbatim across every GMF coverage wave since this repo's own
  M7 milestone) -- sequential small seeds are not well-distributed for
  java.util.Random's own first draw."
  [n mixer-seed]
  (let [mixer (Random. mixer-seed)]
    (repeatedly n #(.nextLong mixer))))

(defn- run-walk
  [module modules seed sex reg-offset-years horizon-years]
  (let [p (person seed sex)
        reg-t (+ (sim-trajectory/dob-epoch-day p) (* 365 reg-offset-years))
        end-t (+ reg-t (* 365 horizon-years))]
    (:trajectory (if modules
                   ;; the interface's own 8-arg arity, given the SAME
                   ;; {}/{} defaults gmf-interpreter's own 6-arg arity
                   ;; already filled in internally -- spelled out here
                   ;; only because the interface does not carry that
                   ;; shorthand; not a behavior change (this promotion
                   ;; session's own interface-repoint, disclosed in the
                   ;; namespace docstring above).
                   (sim-trajectory/run-module module (Random. seed) p reg-t end-t modules {} {})
                   (sim-trajectory/run-module module (Random. seed) p reg-t end-t)))))

(defn- interpreter-batch
  "100 well-mixed seeds x both sexes = 200 walks, concatenated into one
  deterministic EDN vector -- enough real content across enough of each
  module's own branches to make a silent behavior change unlikely to
  slip through undetected, without the runtime cost of the much larger
  seed pools each root's own vendored test uses to hunt one specific
  rare branch."
  [module modules mixer-seed reg-offset-years horizon-years]
  (vec
   (for [seed (mixed-seeds 100 mixer-seed)
         sex [:male :female]]
     (run-walk module modules seed sex reg-offset-years horizon-years))))

(defn- appendicitis-batch []
  (let [module (:payload (sim-trajectory/load-module "appendicitis" (slurp (io/resource "sim/modules/appendicitis.json"))))]
    (interpreter-batch module nil 20260727 70 80)))

(defn- sore-throat-batch []
  (let [module (:payload (sim-trajectory/load-module "sore-throat" (slurp (io/resource "sim/modules/sore_throat.json"))))]
    (interpreter-batch module nil 20260802 25 10)))

(defn- ear-infections-batch []
  (let [resolve-call-path (fn [call-path] (some-> (io/resource (str "sim/modules/" call-path ".json")) slurp))
        loaded (sim-trajectory/load-closure "ear-infections" (slurp (io/resource "sim/modules/ear_infections.json")) resolve-call-path)
        modules (:modules (:payload loaded))
        root (get modules "ear-infections")]
    (interpreter-batch root modules 20260802 25 10)))

(defn- engine-pair
  "Ground truth plus rendered HL7, both content this session's own oracle
  digests -- matching D1b's own literal-digest precedent (HL7 emission
  bytes included, not just the in-memory event log)."
  [run-config]
  (let [{:keys [ground-truth facility providers]} (engine/run run-config)
        messages (emit-hl7/emit ground-truth "2024-01-01" "+00:00" facility providers)]
    {:ground-truth ground-truth :hl7 (vec messages)}))

(defn- sinusitis-pair []
  (let [module (:payload (sim-trajectory/load-module "sinusitis" (slurp (io/resource "sim/modules/sinusitis.json"))))]
    (engine-pair {:seed 1 :patients 30 :pathway {:name "module-only" :steps []}
                  :modules [(sim-trajectory/singleton-closure module)] :module-assignment [{:module-id "sinusitis" :weight 1}]
                  :module-horizon-days 3650})))

(defn- death-fixture-pair []
  (let [module (:payload (sim-trajectory/load-module "death-fixture" (slurp (io/resource "ehrt/sim/fixtures/death-fixture.json"))))]
    (engine-pair {:seed 20260802 :patients 200 :pathway {:name "module-only" :steps []}
                  :modules [(sim-trajectory/singleton-closure module)] :module-assignment [{:module-id "death-fixture" :weight 1}]
                  :module-horizon-days 3650})))

(defn- sepsis-pair []
  (let [module (:payload (sim-trajectory/load-module "sepsis" (slurp (io/resource "sim/modules/sepsis.json"))))]
    (engine-pair {:seed 20260802 :patients 500 :pathway {:name "module-only" :steps []}
                  :modules [(sim-trajectory/singleton-closure module)] :module-assignment [{:module-id "sepsis" :weight 1}]
                  :module-horizon-days 36500})))

;; --- ADR-0033 AR-4b: engine-layer digest pairs for the three closure
;; roots -- FIRST BASELINES (see this namespace's own dated docstring
;; note, above), not a regression check against a prior digest. Same
;; `engine-pair` helper, same seed/population/horizon each root's own
;; `components/sim-emit-hl7/test/` round-trip test already established
;; as producing real content (the J3 pin conversions, ADR-0033).

(defn- ear-infections-engine-pair []
  (let [resolve-call-path (fn [call-path] (some-> (io/resource (str "sim/modules/" call-path ".json")) slurp))
        closure (:payload (sim-trajectory/load-closure "ear-infections" (slurp (io/resource "sim/modules/ear_infections.json")) resolve-call-path))]
    (engine-pair {:seed 20260802 :patients 300 :pathway {:name "module-only" :steps []}
                  :modules [closure] :module-assignment [{:module-id "ear-infections" :weight 1}]
                  :module-horizon-days 3650})))

(defn- urinary-tract-infections-engine-pair []
  (let [resolve-call-path (fn [call-path] (some-> (io/resource (str "sim/modules/" call-path ".json")) slurp))
        resolve-table-name (fn [table-name] (some-> (io/resource (str "sim/modules/lookup_tables/" table-name)) slurp))
        closure (:payload (sim-trajectory/load-closure "urinary-tract-infections"
                                            (slurp (io/resource "sim/modules/urinary_tract_infections.json"))
                                            resolve-call-path resolve-table-name))]
    (engine-pair {:seed 777 :patients 300 :pathway {:name "module-only" :steps []}
                  :modules [closure] :module-assignment [{:module-id "urinary-tract-infections" :weight 1}]
                  :module-horizon-days 36500})))

(defn- total-joint-replacement-engine-pair []
  (let [resolve-call-path (fn [call-path] (some-> (io/resource (str "sim/modules/" call-path ".json")) slurp))
        closure (:payload (sim-trajectory/load-closure "total-joint-replacement"
                                            (slurp (io/resource "sim/modules/total_joint_replacement.json"))
                                            resolve-call-path))
        ;; D2 H7's own authored, provenance-cited seed (ehrt.sim-trajectory.
        ;; vendored-tjr-test's own docstring) -- reused verbatim, not
        ;; re-derived, the same value the sim-emit-hl7 round-trip test uses.
        seeded-closure (assoc closure :initial-attributes {:total-joint-replacement/joint-replacement "knee"})]
    (engine-pair {:seed 20260802 :patients 300 :pathway {:name "module-only" :steps []}
                  :modules [seeded-closure] :module-assignment [{:module-id "total-joint-replacement" :weight 1}]
                  :module-horizon-days 21900})))

;; --- Wave H pre-roll (2026-08-04, ADR-0042 AR-5): the first two
;; history-mode baselines -- FIRST BASELINES exactly like AR-4b's own
;; three above (there is no 'before' to diff against; `:history` is
;; new this session). `:history` false (every root above) is the
;; identity-bracketed set AR-5 argues stays byte-identical BY THE
;; GATING ITSELF (engine.clj's own `:history` default `false` never
;; reaches this new code at all) -- these two are the opt-in, ordinary-
;; seed proof that turning it ON produces real, recordable content.
;; UTI: the SAME closure/population/horizon as the legacy
;; `urinary-tract-infections-engine` batch above (which keeps its own
;; seed 777 unchanged -- that batch is the byte-identity witness for
;; the LEGACY path, not touched by this addition), now `:history true`
;; with the ORDINARY seed ADR-0042 AR-4's own vendored-test retirement
;; already established (the straddle resolves by design, no seed-
;; picking needed). Ear-infections: same closure/horizon as its own
;; legacy `ear-infections-engine` batch, `:history true` -- the wellness-
;; tick-folding proof at oracle scale.

(defn- urinary-tract-infections-history-engine-pair []
  (let [resolve-call-path (fn [call-path] (some-> (io/resource (str "sim/modules/" call-path ".json")) slurp))
        resolve-table-name (fn [table-name] (some-> (io/resource (str "sim/modules/lookup_tables/" table-name)) slurp))
        closure (:payload (sim-trajectory/load-closure "urinary-tract-infections"
                                            (slurp (io/resource "sim/modules/urinary_tract_infections.json"))
                                            resolve-call-path resolve-table-name))]
    (engine-pair {:seed 20260802 :patients 300 :pathway {:name "module-only" :steps []}
                  :modules [closure] :module-assignment [{:module-id "urinary-tract-infections" :weight 1}]
                  :module-horizon-days 36500 :history true})))

(defn- ear-infections-history-engine-pair []
  (let [resolve-call-path (fn [call-path] (some-> (io/resource (str "sim/modules/" call-path ".json")) slurp))
        closure (:payload (sim-trajectory/load-closure "ear-infections" (slurp (io/resource "sim/modules/ear_infections.json")) resolve-call-path))]
    (engine-pair {:seed 20260802 :patients 300 :pathway {:name "module-only" :steps []}
                  :modules [closure] :module-assignment [{:module-id "ear-infections" :weight 1}]
                  :module-horizon-days 3650 :history true})))

;; --- Vendoring batch 1 (2026-08-07, ADR-0070, AR-VB1-4): five NEW
;; engine-layer roots -- FIRST BASELINES exactly like AR-4b's own three
;; above (there is no 'before' to diff against; each module is newly
;; vendored this session). Same `engine-pair` helper, same seed/
;; population/horizon each root's own `components/sim-emit-hl7/test/`
;; round-trip test already established as producing real content (this
;; session's own empirical tuning, seed 20260802, 300 patients, a
;; 100-year `:module-horizon-days`, 36500). `asthma-pair` follows the
;; `urinary-tract-infections-engine-pair` shape (a resolve-table-name
;; needed, its own closure's real therapeutic content is entirely
;; lookup-table-driven); the other four are single-file closures, no
;; CallSubmodule, no lookup tables -- `load-module`/`singleton-closure`,
;; the same idiom `sinusitis-pair`/`death-fixture-pair`/`sepsis-pair`
;; already establish for a bare, unclosed module. A sixth module,
;; `injuries.json`, was assessed this session and DEFERRED WHOLE (a
;; real `gmf-interpreter` max-steps gap the census's own narrow sample
;; missed, `components/sim/resources/sim/modules/NOTICE`'s own dated
;; entry) -- no root for it here, by design, not omission.

(defn- asthma-pair []
  (let [resolve-call-path (fn [call-path] (some-> (io/resource (str "sim/modules/" call-path ".json")) slurp))
        resolve-table-name (fn [table-name] (some-> (io/resource (str "sim/modules/lookup_tables/" table-name)) slurp))
        closure (:payload (sim-trajectory/load-closure "asthma"
                                            (slurp (io/resource "sim/modules/asthma.json"))
                                            resolve-call-path resolve-table-name))]
    (engine-pair {:seed 20260802 :patients 300 :pathway {:name "module-only" :steps []}
                  :modules [closure] :module-assignment [{:module-id "asthma" :weight 1}]
                  :module-horizon-days 36500})))

(defn- bronchitis-pair []
  (let [module (:payload (sim-trajectory/load-module "bronchitis" (slurp (io/resource "sim/modules/bronchitis.json"))))]
    (engine-pair {:seed 20260802 :patients 300 :pathway {:name "module-only" :steps []}
                  :modules [(sim-trajectory/singleton-closure module)] :module-assignment [{:module-id "bronchitis" :weight 1}]
                  :module-horizon-days 36500})))

(defn- sleep-apnea-pair []
  (let [module (:payload (sim-trajectory/load-module "sleep-apnea" (slurp (io/resource "sim/modules/sleep_apnea.json"))))]
    (engine-pair {:seed 20260802 :patients 300 :pathway {:name "module-only" :steps []}
                  :modules [(sim-trajectory/singleton-closure module)] :module-assignment [{:module-id "sleep-apnea" :weight 1}]
                  :module-horizon-days 36500})))

(defn- fibromyalgia-pair []
  (let [module (:payload (sim-trajectory/load-module "fibromyalgia" (slurp (io/resource "sim/modules/fibromyalgia.json"))))]
    (engine-pair {:seed 20260802 :patients 300 :pathway {:name "module-only" :steps []}
                  :modules [(sim-trajectory/singleton-closure module)] :module-assignment [{:module-id "fibromyalgia" :weight 1}]
                  :module-horizon-days 36500})))

(defn- dementia-pair []
  (let [module (:payload (sim-trajectory/load-module "dementia" (slurp (io/resource "sim/modules/dementia.json"))))]
    (engine-pair {:seed 20260802 :patients 300 :pathway {:name "module-only" :steps []}
                  :modules [(sim-trajectory/singleton-closure module)] :module-assignment [{:module-id "dementia" :weight 1}]
                  :module-horizon-days 36500})))

;; --- Vendoring batch 2 (2026-08-07, ADR-0071, AR-VB2-3): seven NEW
;; engine-layer roots -- FIRST BASELINES, same convention batch 1's own
;; five established (seed 20260802, 300 patients, a 100-year
;; `:module-horizon-days`, 36500), with two per-module deviations each
;; `components/sim-emit-hl7/test/`'s own round-trip test already
;; established empirically this session, disclosed in NOTICE's own
;; dated batch-2 entry: `attention-deficit-disorder-pair` carries
;; `:history true` (ADR-0042's own opt-in -- this closure's own
;; `Behavior_Therapy` loop can straddle the fixed registration instant);
;; `allergic-rhinitis-pair` runs 3000 patients, not 300 (this closure's
;; own low onset odds land in early childhood, always pre-registration
;; for an adult-sampled population at the batch convention's own
;; population size -- 3000 is where real post-registration, message-
;; rendering content first appears). `hypothyroidism-pair`/
;; `osteoarthritis-pair`/`dermatitis-pair` follow the `ear-infections-
;; engine-pair` shape (a resolve-call-path needed, no lookup tables);
;; `rheumatoid-arthritis-pair`/`osteoporosis-pair`/`attention-deficit-
;; disorder-pair` are single-file closures, the `bronchitis-pair`/etc.
;; idiom. An eighth module, `anemia___unknown_etiology.json`, was
;; assessed this session and DEFERRED WHOLE (a real `gmf-interpreter`
;; dangling-`:encounter-end` gap the census's own narrow sample missed,
;; `components/sim/resources/sim/modules/NOTICE`'s own dated entry) --
;; no root for it here, by design, not omission.

(defn- hypothyroidism-pair []
  (let [resolve-call-path (fn [call-path] (some-> (io/resource (str "sim/modules/" call-path ".json")) slurp))
        closure (:payload (sim-trajectory/load-closure "hypothyroidism"
                                            (slurp (io/resource "sim/modules/hypothyroidism.json"))
                                            resolve-call-path))]
    (engine-pair {:seed 20260802 :patients 300 :pathway {:name "module-only" :steps []}
                  :modules [closure] :module-assignment [{:module-id "hypothyroidism" :weight 1}]
                  :module-horizon-days 36500})))

(defn- rheumatoid-arthritis-pair []
  (let [module (:payload (sim-trajectory/load-module "rheumatoid-arthritis" (slurp (io/resource "sim/modules/rheumatoid_arthritis.json"))))]
    (engine-pair {:seed 20260802 :patients 300 :pathway {:name "module-only" :steps []}
                  :modules [(sim-trajectory/singleton-closure module)] :module-assignment [{:module-id "rheumatoid-arthritis" :weight 1}]
                  :module-horizon-days 36500})))

(defn- osteoarthritis-pair []
  (let [resolve-call-path (fn [call-path] (some-> (io/resource (str "sim/modules/" call-path ".json")) slurp))
        closure (:payload (sim-trajectory/load-closure "osteoarthritis"
                                            (slurp (io/resource "sim/modules/osteoarthritis.json"))
                                            resolve-call-path))]
    (engine-pair {:seed 20260802 :patients 300 :pathway {:name "module-only" :steps []}
                  :modules [closure] :module-assignment [{:module-id "osteoarthritis" :weight 1}]
                  :module-horizon-days 36500})))

(defn- osteoporosis-pair []
  (let [module (:payload (sim-trajectory/load-module "osteoporosis" (slurp (io/resource "sim/modules/osteoporosis.json"))))]
    (engine-pair {:seed 20260802 :patients 300 :pathway {:name "module-only" :steps []}
                  :modules [(sim-trajectory/singleton-closure module)] :module-assignment [{:module-id "osteoporosis" :weight 1}]
                  :module-horizon-days 36500})))

(defn- attention-deficit-disorder-pair []
  (let [module (:payload (sim-trajectory/load-module "attention-deficit-disorder" (slurp (io/resource "sim/modules/attention_deficit_disorder.json"))))]
    (engine-pair {:seed 20260802 :patients 300 :pathway {:name "module-only" :steps []}
                  :modules [(sim-trajectory/singleton-closure module)] :module-assignment [{:module-id "attention-deficit-disorder" :weight 1}]
                  :module-horizon-days 36500 :history true})))

(defn- allergic-rhinitis-pair []
  (let [resolve-call-path (fn [call-path] (some-> (io/resource (str "sim/modules/" call-path ".json")) slurp))
        closure (:payload (sim-trajectory/load-closure "allergic-rhinitis"
                                            (slurp (io/resource "sim/modules/allergic_rhinitis.json"))
                                            resolve-call-path))]
    (engine-pair {:seed 20260802 :patients 3000 :pathway {:name "module-only" :steps []}
                  :modules [closure] :module-assignment [{:module-id "allergic-rhinitis" :weight 1}]
                  :module-horizon-days 36500})))

(defn- dermatitis-pair []
  (let [resolve-call-path (fn [call-path] (some-> (io/resource (str "sim/modules/" call-path ".json")) slurp))
        closure (:payload (sim-trajectory/load-closure "dermatitis"
                                            (slurp (io/resource "sim/modules/dermatitis.json"))
                                            resolve-call-path))]
    (engine-pair {:seed 20260802 :patients 300 :pathway {:name "module-only" :steps []}
                  :modules [closure] :module-assignment [{:module-id "dermatitis" :weight 1}]
                  :module-horizon-days 36500})))

;; --- Vendoring batch 3 (2026-08-07, ADR-0072, AR-VB3-2): four new
;; engine-layer roots -- FIRST BASELINES, same convention batches 1-2
;; established.

(defn- metabolic-syndrome-care-pair []
  (let [resolve-call-path (fn [call-path] (some-> (io/resource (str "sim/modules/" call-path ".json")) slurp))
        closure (:payload (sim-trajectory/load-closure "metabolic-syndrome-care"
                                            (slurp (io/resource "sim/modules/metabolic_syndrome_care.json"))
                                            resolve-call-path))]
    (engine-pair {:seed 20260802 :patients 300 :pathway {:name "module-only" :steps []}
                  :modules [closure] :module-assignment [{:module-id "metabolic-syndrome-care" :weight 1}]
                  :module-horizon-days 36500})))

(defn- vhd-pulmonic-pair []
  (let [resolve-call-path (fn [call-path] (some-> (io/resource (str "sim/modules/" call-path ".json")) slurp))
        resolve-table-name (fn [table-name] (some-> (io/resource (str "sim/modules/lookup_tables/" table-name)) slurp))
        closure (:payload (sim-trajectory/load-closure "vhd-pulmonic"
                                            (slurp (io/resource "sim/modules/vhd_pulmonic.json"))
                                            resolve-call-path resolve-table-name))]
    (engine-pair {:seed 20260802 :patients 300 :pathway {:name "module-only" :steps []}
                  :modules [closure] :module-assignment [{:module-id "vhd-pulmonic" :weight 1}]
                  :module-horizon-days 36500})))

(defn- vhd-tricuspid-pair []
  (let [resolve-call-path (fn [call-path] (some-> (io/resource (str "sim/modules/" call-path ".json")) slurp))
        resolve-table-name (fn [table-name] (some-> (io/resource (str "sim/modules/lookup_tables/" table-name)) slurp))
        closure (:payload (sim-trajectory/load-closure "vhd-tricuspid"
                                            (slurp (io/resource "sim/modules/vhd_tricuspid.json"))
                                            resolve-call-path resolve-table-name))]
    (engine-pair {:seed 20260802 :patients 300 :pathway {:name "module-only" :steps []}
                  :modules [closure] :module-assignment [{:module-id "vhd-tricuspid" :weight 1}]
                  :module-horizon-days 36500})))

(defn- med-rec-pair []
  (let [module (:payload (sim-trajectory/load-module "med-rec" (slurp (io/resource "sim/modules/med_rec.json"))))]
    (engine-pair {:seed 20260802 :patients 300 :pathway {:name "module-only" :steps []}
                  :modules [(sim-trajectory/singleton-closure module)] :module-assignment [{:module-id "med-rec" :weight 1}]
                  :module-horizon-days 36500})))

;; --- Fidelity payoff (2026-08-08, ADR-0083, AR-FP-1/2): the
;; twenty-eighth root -- a FIRST BASELINE, deferred whole at vendoring
;; batch 2 (ADR-0071) on the same dangling-`:encounter-end` gap the
;; EncounterEnd fix (ADR-0082) closed structurally; this module's own
;; in-session proof there found violations fully extinguished at this
;; SAME seed/population. `:persona-config` carries the race-weighting
;; ADR-0071's own finding first required (no other root here reads
;; `:race`) -- the same shape `ehrt.sim-trajectory.census/default-
;; persona-config` and `vendored_anemia_test.clj` both already use.

(defn- anemia-pair []
  (let [resolve-call-path (fn [call-path] (some-> (io/resource (str "sim/modules/" call-path ".json")) slurp))
        closure (:payload (sim-trajectory/load-closure "anemia-unknown-etiology"
                                            (slurp (io/resource "sim/modules/anemia___unknown_etiology.json"))
                                            resolve-call-path))]
    (engine-pair {:seed 20260802 :patients 300 :pathway {:name "module-only" :steps []}
                  :modules [closure] :module-assignment [{:module-id "anemia-unknown-etiology" :weight 1}]
                  :module-horizon-days 36500
                  :persona-config {:race-weights [{:race "White" :weight 1.0} {:race "Black" :weight 1.0}
                                                   {:race "Hispanic" :weight 1.0} {:race "Asian" :weight 1.0}
                                                   {:race "Native" :weight 1.0} {:race "Other" :weight 1.0}]}})))

;; --- Colorectal payoff (2026-08-08, ADR-0087, AR-CP-3): the
;; twenty-ninth root -- a FIRST BASELINE, deferred whole at vendoring
;; batch 3 (ADR-0072) on a diagnosis later overturned (ADR-0083), then
;; diagnosed to its true cause (ADR-0085) and fixed (ADR-0086, the
;; straddle fix) -- this module's own in-session proof there found
;; violations fully extinguished at this SAME seed/population. No
;; `:persona-config` override -- unlike `anemia-pair`, this module's own
;; `Initial` state is not Race-gated (confirmed by inspection, ADR-0082).

(defn- colorectal-pair []
  (let [resolve-call-path (fn [call-path] (some-> (io/resource (str "sim/modules/" call-path ".json")) slurp))
        closure (:payload (sim-trajectory/load-closure "colorectal-cancer"
                                            (slurp (io/resource "sim/modules/colorectal_cancer.json"))
                                            resolve-call-path))]
    (engine-pair {:seed 20260802 :patients 300 :pathway {:name "module-only" :steps []}
                  :modules [closure] :module-assignment [{:module-id "colorectal-cancer" :weight 1}]
                  :module-horizon-days 36500})))

;; --- Vendoring batch 4 (2026-08-08, ADR-0090, AR-VB4-3): five new
;; engine-layer roots, the veteran family -- FIRST BASELINES, same
;; convention prior batches established (seed 20260802, 300 patients, a
;; 100-year `:module-horizon-days`, 36500). Every one of the five needs
;; `:initial-attributes {<root-id>/veteran true}` -- NOTICE's own dated
;; batch-4 entry has the full disclosed correction: `:persona-config`
;; (the anemia/colorectal precedent) only reaches PERSONA-level
;; condition types, never the generic `Attribute` condition type this
;; family's own `veteran` gate uses; `:initial-attributes` (the
;; `total-joint-replacement-engine-pair` precedent, above) is the real
;; established mechanism. Four other candidates assessed this session
;; were NOT vendored (two zero-substance, one a real population-scale
;; invariant violation, one a real interpreter max-steps exhaustion) --
;; no root for any of them here, by design, not omission.

(defn- veteran-lung-cancer-pair []
  (let [module (:payload (sim-trajectory/load-module "veteran-lung-cancer" (slurp (io/resource "sim/modules/veteran_lung_cancer.json"))))
        seeded-closure (assoc (sim-trajectory/singleton-closure module) :initial-attributes {:veteran-lung-cancer/veteran true})]
    (engine-pair {:seed 20260802 :patients 300 :pathway {:name "module-only" :steps []}
                  :modules [seeded-closure] :module-assignment [{:module-id "veteran-lung-cancer" :weight 1}]
                  :module-horizon-days 36500})))

(defn- veteran-prostate-cancer-pair []
  (let [module (:payload (sim-trajectory/load-module "veteran-prostate-cancer" (slurp (io/resource "sim/modules/veteran_prostate_cancer.json"))))
        seeded-closure (assoc (sim-trajectory/singleton-closure module) :initial-attributes {:veteran-prostate-cancer/veteran true})]
    (engine-pair {:seed 20260802 :patients 300 :pathway {:name "module-only" :steps []}
                  :modules [seeded-closure] :module-assignment [{:module-id "veteran-prostate-cancer" :weight 1}]
                  :module-horizon-days 36500})))

(defn- veteran-ptsd-pair []
  (let [module (:payload (sim-trajectory/load-module "veteran-ptsd" (slurp (io/resource "sim/modules/veteran_ptsd.json"))))
        seeded-closure (assoc (sim-trajectory/singleton-closure module) :initial-attributes {:veteran-ptsd/veteran true})]
    (engine-pair {:seed 20260802 :patients 300 :pathway {:name "module-only" :steps []}
                  :modules [seeded-closure] :module-assignment [{:module-id "veteran-ptsd" :weight 1}]
                  :module-horizon-days 36500})))

(defn- veteran-self-harm-pair []
  (let [resolve-call-path (fn [call-path] (some-> (io/resource (str "sim/modules/" call-path ".json")) slurp))
        closure (:payload (sim-trajectory/load-closure "veteran-self-harm"
                                            (slurp (io/resource "sim/modules/veteran_self_harm.json"))
                                            resolve-call-path))
        seeded-closure (assoc closure :initial-attributes {:veteran-self-harm/veteran true})]
    (engine-pair {:seed 20260802 :patients 300 :pathway {:name "module-only" :steps []}
                  :modules [seeded-closure] :module-assignment [{:module-id "veteran-self-harm" :weight 1}]
                  :module-horizon-days 36500})))

(defn- veteran-substance-abuse-treatment-pair []
  (let [module (:payload (sim-trajectory/load-module "veteran-substance-abuse-treatment" (slurp (io/resource "sim/modules/veteran_substance_abuse_treatment.json"))))
        seeded-closure (assoc (sim-trajectory/singleton-closure module) :initial-attributes {:veteran-substance-abuse-treatment/veteran true})]
    (engine-pair {:seed 20260802 :patients 300 :pathway {:name "module-only" :steps []}
                  :modules [seeded-closure] :module-assignment [{:module-id "veteran-substance-abuse-treatment" :weight 1}]
                  :module-horizon-days 36500})))

(defn- injuries-pair
  "Injuries arc close (2026-08-11, ADR-0107): FIRST BASELINE, not a
  regression check -- this closure never completed a round trip before
  this session (deferred WHOLE by ADR-0070, re-deferred narrower by
  ADR-0106). 18250-day (50-year) horizon, NOT the 36500-day convention
  most engine-layer roots use -- `broken_jaw.json`'s own `Wait for
  Dental Visit` loop needs the SHORTER, census-matching horizon
  (`ehrt.sim-emit-hl7.vendored-injuries-test`'s own dated note has the
  full arithmetic: a 100-year horizon crosses the interpreter's
  max-steps budget on this closure's own loop, a 50-year one does not)."
  []
  (let [resolve-call-path (fn [call-path] (some-> (io/resource (str "sim/modules/" call-path ".json")) slurp))
        closure (:payload (sim-trajectory/load-closure "injuries" (slurp (io/resource "sim/modules/injuries.json")) resolve-call-path))]
    (engine-pair {:seed 20260802 :patients 300 :pathway {:name "module-only" :steps []}
                  :modules [closure] :module-assignment [{:module-id "injuries" :weight 1}]
                  :module-horizon-days 18250})))

;; --- COVERAGE (2026-08-19, ADR-0156, register rows L1-1/L1-2/L1-3) ---
;;
;; WHAT `IDENTICAL` MEANS, AND WHAT IT DOES NOT. This block is INSIDE
;; the region `bin/regression-oracle`'s soundness check compares, on
;; purpose. A coverage claim that sat in the docstring above could drift
;; with no bracket noticing -- which is exactly how `Six roots` survived
;; to 35. Here, widening or narrowing coverage forces
;; `--declared-digest-change`, and the session that pays it has to say
;; which way coverage moved.
;;
;; THE VACUOUS SET -- surfaces no root can move, so no IDENTICAL verdict
;; says anything about them (review 4's instrumented reachability
;; battery over 78 named surfaces, all 35 roots, its output proven
;; byte-identical to the clean digest):
;;
;;   * 9 of 22 `decide` dispatches and 8 of 21 `evolve` dispatches.
;;   * 8 of the event contract's 21 closed kinds: :bed-swap,
;;     :cancel-admit, :cancel-discharge, :cancel-transfer, :merge,
;;     :order-placed, :result-available, :step-rejected.
;;   * The whole order->result path: `orm-message`, `oru-message`,
;;     `obx-segment`. NOTE the precision, because the summary version of
;;     this claim is wrong: ORU^R01 IS emitted, 1,768 times across 14
;;     roots -- by `observation-message` and `diagnostic-report-message`.
;;     It is `oru-message`, the :result-available order-result emitter,
;;     that no root reaches. ORM^O01 is emitted zero times.
;;   * `bed-swap-message`, `merge-message`, `mrg-segment`, the Z-segment
;;     pair, `plan-latency`/`emit-wire` (the second clock),
;;     `v2-replay/fold-message`, `churn/inject`+`strip`, `engine/replay`,
;;     and `sim-check` in its entirety.
;;
;; THE STRUCTURAL CAUSE, re-derived rather than inferred from the
;; instrumentation: all 35 roots pass `:pathway {:name "module-only"
;; :steps []}`, and 11 of 18 components plus `bases/cli` are off the
;; oracle classpath entirely (`bin/regression-oracle`'s own deps block).
;;
;; SITE PROFILES -- generalising ADR-0150 (a), which named only the
;; Z-segment quarter of this surface. The sole emitter call below is the
;; FIVE-arg arity, so `site-profile` is nil at ALL FOUR bind points: MSH
;; dialect, the :patient-class table, the :discharge-disposition table,
;; and Z-segments. `effective-msh` and `code-for` ARE invoked -- on their
;; nil-profile branch only. The oracle witnesses the ABSENT-PROFILE
;; IDENTITY and nothing else: `default-msh` and the standard code tables
;; are inside it, every override branch is outside it. Any site-profile
;; milestone must nominate a different witness up front.
;;
;; THE CAPACITY WITNESS IS ONE ROOT DEEP. `death-fixture` alone carries
;; the oracle's single `:transfer` -- and with it the only ADT^A02, the
;; only `:bed-ready true`, and all 13 of its ladder rung-3 placements.
;; Rung 4, `:forced` and `:exhausted` are zero across all 35. Lose that
;; root and four claims go dark together. `:medication-end` is one root
;; deep too (`injuries`, one occurrence).
;;
;; The two sets below are the committed claim, asserted against a FRESH
;; 35-root digest by `ehrt.integration.oracle-coverage-test` and checked
;; for shape, population, membership and location on every push by
;; `ehrt.docs-tooling.oracle-coverage-test`.

(def ^:private witnessed-event-kinds
  "The 13 of 21 closed event kinds any root can produce. Adding a root
  that reaches the capacity or order->result paths moves this set --
  that is R4-Q6 (ii) (b), rowed and priced, deliberately not taken."
  #{:admission :care-plan-end :care-plan-start :diagnostic-report
    :discharge :medication-end :medication-order :observation
    :outpatient-visit :outpatient-visit-end :procedure :registered
    :transfer})

(def ^:private witnessed-message-types
  "Every MSH-9 the 32 engine-layer roots emit. ADT^A02 is death-fixture's
  alone, once. ADT^A08/A11/A13/A34/A40 and ORM^O01 are emitted by no
  root at all."
  #{"ADT^A01" "ADT^A02" "ADT^A03" "ADT^A04" "ORU^R01"})

(def ^:private roots
  {"appendicitis"                       appendicitis-batch
   "sore-throat"                        sore-throat-batch
   "ear-infections"                     ear-infections-batch
   "sinusitis"                          sinusitis-pair
   "death-fixture"                      death-fixture-pair
   "sepsis"                             sepsis-pair
   "ear-infections-engine"              ear-infections-engine-pair
   "urinary-tract-infections-engine"    urinary-tract-infections-engine-pair
   "total-joint-replacement-engine"     total-joint-replacement-engine-pair
   "urinary-tract-infections-history-engine" urinary-tract-infections-history-engine-pair
   "ear-infections-history-engine"      ear-infections-history-engine-pair
   "asthma"                             asthma-pair
   "bronchitis"                         bronchitis-pair
   "sleep-apnea"                        sleep-apnea-pair
   "fibromyalgia"                       fibromyalgia-pair
   "dementia"                           dementia-pair
   "hypothyroidism"                     hypothyroidism-pair
   "rheumatoid-arthritis"               rheumatoid-arthritis-pair
   "osteoarthritis"                     osteoarthritis-pair
   "osteoporosis"                       osteoporosis-pair
   "attention-deficit-disorder"         attention-deficit-disorder-pair
   "allergic-rhinitis"                  allergic-rhinitis-pair
   "dermatitis"                         dermatitis-pair
   "metabolic-syndrome-care"            metabolic-syndrome-care-pair
   "vhd-pulmonic"                       vhd-pulmonic-pair
   "vhd-tricuspid"                      vhd-tricuspid-pair
   "med-rec"                            med-rec-pair
   "anemia"                             anemia-pair
   "colorectal"                         colorectal-pair
   "veteran-lung-cancer"                veteran-lung-cancer-pair
   "veteran-prostate-cancer"            veteran-prostate-cancer-pair
   "veteran-ptsd"                       veteran-ptsd-pair
   "veteran-self-harm"                  veteran-self-harm-pair
   "veteran-substance-abuse-treatment"  veteran-substance-abuse-treatment-pair
   "injuries"                           injuries-pair})

(defn -main
  "Writes one <root>.edn per root into out-dir (pr-str of the batch or
  the {:ground-truth :hl7} pair) -- bin/regression-oracle's own shell
  loop sha256sums each file itself; this process never hashes."
  [out-dir]
  (.mkdirs (io/file out-dir))
  (doseq [[root-name f] (sort-by key roots)]
    (println "running" root-name "...")
    (let [content (f)
          file (io/file out-dir (str root-name ".edn"))]
      (spit file (pr-str content))
      (println "  wrote" (.getPath file))))
  (println "done"))
