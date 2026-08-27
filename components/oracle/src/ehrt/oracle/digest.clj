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
  `ehrt.patient-simulator.gmf`/`gmf-interpreter` collapse into
  `ehrt.patient-simulator.interface` (gaining `dob-epoch-day`, the one var
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

  CURRENT STATE, 2026-08-27 (arc 3b sweep 3, ADR-0174 section 2(b)'s
  turn-on commit; before it, arc 3b sweep 2, ADR-0174 section 2(c);
  before that, arc 3b sweep 1, ADR-0174 ruling A1; before
  that, arc 3a part 4, ADR-0173 ruling D1's commit 2; previously
  2026-08-19, ADR-0156, review-4 register row L1-5).
  `roots` holds 39 roots in two families:

    3 INTERPRETER-LAYER batches -- appendicitis/sore-throat/
      ear-infections. 100 well-mixed seeds x both sexes = 200 walks per
      root, concatenated into one deterministic EDN vector. The
      well-mixed-seed pattern every vendored-module test in this repo
      already uses: SEQUENTIAL small java.util.Random seeds are NOT
      well-distributed for their own first draw, confirmed repeatedly
      across GMF coverage waves.

   36 ENGINE-LAYER pairs -- engine/run plus emit-hl7/emit, ground truth
      AND emitted HL7 both captured, at the run-config each root's own
      vendored/engine test already established as producing real
      content. The 33rd, `demographic-fold`, is the first root to turn
      `:persons` ON and is the only one covering the demographic fold,
      the two clinical hooks or the identification flow. The 34th,
      `encounter-horizon`, is the first to turn `:encounters` ON -- the
      only root where a patient has more than one encounter -- and the
      first to pass `:churn true` at all, which is why it brings three
      event kinds and three MSH-9s with it. The 35th, `bed-cycle`, is
      the first to turn `:bed-cycle` ON, and it is the first root of any
      kind to reach the allocation ladder's FOURTH RUNG -- the gap the
      COVERAGE block below said sweep 2 would have to close. The 36th,
      `scheduling`, is the first to turn `:scheduling` ON: the only root
      carrying `:appointment`, `:reschedule`, `:appointment-cancel` or
      `:no-show`, and the only one where a second encounter is produced
      BY BOOKING rather than by a person walking in again. It renders no
      message for any of the four -- ruling C, the v2.4 SIU structures
      against MSH-12 `2.3` -- so it adds four EVENT kinds and ZERO
      message types, the first root to widen one vocabulary without the
      other.

  This paragraph replaced an opening that read `Six roots, matching this
  session's own J1 ruling verbatim` -- true when written and never
  updated past the two dated notes below, which carry the population to
  11. The other 24 arrived in `;;` comments inside the body. Nothing was
  false; a cold reader simply got a third of the population and no
  signal that it was a third. The count is gated now
  (`ehrt.docs-tooling.oracle-coverage-test`), and the COVERAGE block
  beside `roots` states what these 39 can and cannot witness.

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
            [ehrt.kernel.interface :as kernel]
            [ehrt.patient-simulator.interface :as patient-simulator]
            [ehrt.sim.interface :as sim]
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
        reg-t (+ (patient-simulator/dob-epoch-day p) (* 365 reg-offset-years))
        end-t (+ reg-t (* 365 horizon-years))]
    (:trajectory (if modules
                   ;; the interface's own 8-arg arity, given the SAME
                   ;; {}/{} defaults gmf-interpreter's own 6-arg arity
                   ;; already filled in internally -- spelled out here
                   ;; only because the interface does not carry that
                   ;; shorthand; not a behavior change (this promotion
                   ;; session's own interface-repoint, disclosed in the
                   ;; namespace docstring above).
                   (patient-simulator/run-module module (Random. seed) p reg-t end-t modules {} {})
                   (patient-simulator/run-module module (Random. seed) p reg-t end-t)))))

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
  (let [module (:payload (patient-simulator/load-module "appendicitis" (slurp (io/resource "sim/modules/appendicitis.json"))))]
    (interpreter-batch module nil 20260727 70 80)))

(defn- sore-throat-batch []
  (let [module (:payload (patient-simulator/load-module "sore-throat" (slurp (io/resource "sim/modules/sore_throat.json"))))]
    (interpreter-batch module nil 20260802 25 10)))

(defn- ear-infections-batch []
  (let [resolve-call-path (fn [call-path] (some-> (io/resource (str "sim/modules/" call-path ".json")) slurp))
        loaded (patient-simulator/load-closure "ear-infections" (slurp (io/resource "sim/modules/ear_infections.json")) resolve-call-path)
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
  (let [module (:payload (patient-simulator/load-module "sinusitis" (slurp (io/resource "sim/modules/sinusitis.json"))))]
    (engine-pair {:seed 1 :patients 30 :pathway {:name "module-only" :steps []}
                  :modules [(patient-simulator/singleton-closure module)] :module-assignment [{:module-id "sinusitis" :weight 1}]
                  :module-horizon-days 3650})))

(defn- death-fixture-pair []
  (let [module (:payload (patient-simulator/load-module "death-fixture" (slurp (io/resource "ehrt/sim/fixtures/death-fixture.json"))))]
    (engine-pair {:seed 20260802 :patients 200 :pathway {:name "module-only" :steps []}
                  :modules [(patient-simulator/singleton-closure module)] :module-assignment [{:module-id "death-fixture" :weight 1}]
                  :module-horizon-days 3650})))

(defn- sepsis-pair []
  (let [module (:payload (patient-simulator/load-module "sepsis" (slurp (io/resource "sim/modules/sepsis.json"))))]
    (engine-pair {:seed 20260802 :patients 500 :pathway {:name "module-only" :steps []}
                  :modules [(patient-simulator/singleton-closure module)] :module-assignment [{:module-id "sepsis" :weight 1}]
                  :module-horizon-days 36500})))

;; --- ADR-0033 AR-4b: engine-layer digest pairs for the three closure
;; roots -- FIRST BASELINES (see this namespace's own dated docstring
;; note, above), not a regression check against a prior digest. Same
;; `engine-pair` helper, same seed/population/horizon each root's own
;; `components/sim-emit-hl7/test/` round-trip test already established
;; as producing real content (the J3 pin conversions, ADR-0033).

(defn- ear-infections-engine-pair []
  (let [resolve-call-path (fn [call-path] (some-> (io/resource (str "sim/modules/" call-path ".json")) slurp))
        closure (:payload (patient-simulator/load-closure "ear-infections" (slurp (io/resource "sim/modules/ear_infections.json")) resolve-call-path))]
    (engine-pair {:seed 20260802 :patients 300 :pathway {:name "module-only" :steps []}
                  :modules [closure] :module-assignment [{:module-id "ear-infections" :weight 1}]
                  :module-horizon-days 3650})))

(defn- urinary-tract-infections-engine-pair []
  (let [resolve-call-path (fn [call-path] (some-> (io/resource (str "sim/modules/" call-path ".json")) slurp))
        resolve-table-name (fn [table-name] (some-> (io/resource (str "sim/modules/lookup_tables/" table-name)) slurp))
        closure (:payload (patient-simulator/load-closure "urinary-tract-infections"
                                            (slurp (io/resource "sim/modules/urinary_tract_infections.json"))
                                            resolve-call-path resolve-table-name))]
    (engine-pair {:seed 777 :patients 300 :pathway {:name "module-only" :steps []}
                  :modules [closure] :module-assignment [{:module-id "urinary-tract-infections" :weight 1}]
                  :module-horizon-days 36500})))

(defn- total-joint-replacement-engine-pair []
  (let [resolve-call-path (fn [call-path] (some-> (io/resource (str "sim/modules/" call-path ".json")) slurp))
        closure (:payload (patient-simulator/load-closure "total-joint-replacement"
                                            (slurp (io/resource "sim/modules/total_joint_replacement.json"))
                                            resolve-call-path))
        ;; D2 H7's own authored, provenance-cited seed (ehrt.patient-simulator.
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
        closure (:payload (patient-simulator/load-closure "urinary-tract-infections"
                                            (slurp (io/resource "sim/modules/urinary_tract_infections.json"))
                                            resolve-call-path resolve-table-name))]
    (engine-pair {:seed 20260802 :patients 300 :pathway {:name "module-only" :steps []}
                  :modules [closure] :module-assignment [{:module-id "urinary-tract-infections" :weight 1}]
                  :module-horizon-days 36500 :history true})))

(defn- ear-infections-history-engine-pair []
  (let [resolve-call-path (fn [call-path] (some-> (io/resource (str "sim/modules/" call-path ".json")) slurp))
        closure (:payload (patient-simulator/load-closure "ear-infections" (slurp (io/resource "sim/modules/ear_infections.json")) resolve-call-path))]
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
        closure (:payload (patient-simulator/load-closure "asthma"
                                            (slurp (io/resource "sim/modules/asthma.json"))
                                            resolve-call-path resolve-table-name))]
    (engine-pair {:seed 20260802 :patients 300 :pathway {:name "module-only" :steps []}
                  :modules [closure] :module-assignment [{:module-id "asthma" :weight 1}]
                  :module-horizon-days 36500})))

(defn- bronchitis-pair []
  (let [module (:payload (patient-simulator/load-module "bronchitis" (slurp (io/resource "sim/modules/bronchitis.json"))))]
    (engine-pair {:seed 20260802 :patients 300 :pathway {:name "module-only" :steps []}
                  :modules [(patient-simulator/singleton-closure module)] :module-assignment [{:module-id "bronchitis" :weight 1}]
                  :module-horizon-days 36500})))

(defn- sleep-apnea-pair []
  (let [module (:payload (patient-simulator/load-module "sleep-apnea" (slurp (io/resource "sim/modules/sleep_apnea.json"))))]
    (engine-pair {:seed 20260802 :patients 300 :pathway {:name "module-only" :steps []}
                  :modules [(patient-simulator/singleton-closure module)] :module-assignment [{:module-id "sleep-apnea" :weight 1}]
                  :module-horizon-days 36500})))

(defn- fibromyalgia-pair []
  (let [module (:payload (patient-simulator/load-module "fibromyalgia" (slurp (io/resource "sim/modules/fibromyalgia.json"))))]
    (engine-pair {:seed 20260802 :patients 300 :pathway {:name "module-only" :steps []}
                  :modules [(patient-simulator/singleton-closure module)] :module-assignment [{:module-id "fibromyalgia" :weight 1}]
                  :module-horizon-days 36500})))

(defn- dementia-pair []
  (let [module (:payload (patient-simulator/load-module "dementia" (slurp (io/resource "sim/modules/dementia.json"))))]
    (engine-pair {:seed 20260802 :patients 300 :pathway {:name "module-only" :steps []}
                  :modules [(patient-simulator/singleton-closure module)] :module-assignment [{:module-id "dementia" :weight 1}]
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
        closure (:payload (patient-simulator/load-closure "hypothyroidism"
                                            (slurp (io/resource "sim/modules/hypothyroidism.json"))
                                            resolve-call-path))]
    (engine-pair {:seed 20260802 :patients 300 :pathway {:name "module-only" :steps []}
                  :modules [closure] :module-assignment [{:module-id "hypothyroidism" :weight 1}]
                  :module-horizon-days 36500})))

(defn- rheumatoid-arthritis-pair []
  (let [module (:payload (patient-simulator/load-module "rheumatoid-arthritis" (slurp (io/resource "sim/modules/rheumatoid_arthritis.json"))))]
    (engine-pair {:seed 20260802 :patients 300 :pathway {:name "module-only" :steps []}
                  :modules [(patient-simulator/singleton-closure module)] :module-assignment [{:module-id "rheumatoid-arthritis" :weight 1}]
                  :module-horizon-days 36500})))

(defn- osteoarthritis-pair []
  (let [resolve-call-path (fn [call-path] (some-> (io/resource (str "sim/modules/" call-path ".json")) slurp))
        closure (:payload (patient-simulator/load-closure "osteoarthritis"
                                            (slurp (io/resource "sim/modules/osteoarthritis.json"))
                                            resolve-call-path))]
    (engine-pair {:seed 20260802 :patients 300 :pathway {:name "module-only" :steps []}
                  :modules [closure] :module-assignment [{:module-id "osteoarthritis" :weight 1}]
                  :module-horizon-days 36500})))

(defn- osteoporosis-pair []
  (let [module (:payload (patient-simulator/load-module "osteoporosis" (slurp (io/resource "sim/modules/osteoporosis.json"))))]
    (engine-pair {:seed 20260802 :patients 300 :pathway {:name "module-only" :steps []}
                  :modules [(patient-simulator/singleton-closure module)] :module-assignment [{:module-id "osteoporosis" :weight 1}]
                  :module-horizon-days 36500})))

(defn- attention-deficit-disorder-pair []
  (let [module (:payload (patient-simulator/load-module "attention-deficit-disorder" (slurp (io/resource "sim/modules/attention_deficit_disorder.json"))))]
    (engine-pair {:seed 20260802 :patients 300 :pathway {:name "module-only" :steps []}
                  :modules [(patient-simulator/singleton-closure module)] :module-assignment [{:module-id "attention-deficit-disorder" :weight 1}]
                  :module-horizon-days 36500 :history true})))

(defn- allergic-rhinitis-pair []
  (let [resolve-call-path (fn [call-path] (some-> (io/resource (str "sim/modules/" call-path ".json")) slurp))
        closure (:payload (patient-simulator/load-closure "allergic-rhinitis"
                                            (slurp (io/resource "sim/modules/allergic_rhinitis.json"))
                                            resolve-call-path))]
    (engine-pair {:seed 20260802 :patients 3000 :pathway {:name "module-only" :steps []}
                  :modules [closure] :module-assignment [{:module-id "allergic-rhinitis" :weight 1}]
                  :module-horizon-days 36500})))

(defn- dermatitis-pair []
  (let [resolve-call-path (fn [call-path] (some-> (io/resource (str "sim/modules/" call-path ".json")) slurp))
        closure (:payload (patient-simulator/load-closure "dermatitis"
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
        closure (:payload (patient-simulator/load-closure "metabolic-syndrome-care"
                                            (slurp (io/resource "sim/modules/metabolic_syndrome_care.json"))
                                            resolve-call-path))]
    (engine-pair {:seed 20260802 :patients 300 :pathway {:name "module-only" :steps []}
                  :modules [closure] :module-assignment [{:module-id "metabolic-syndrome-care" :weight 1}]
                  :module-horizon-days 36500})))

(defn- vhd-pulmonic-pair []
  (let [resolve-call-path (fn [call-path] (some-> (io/resource (str "sim/modules/" call-path ".json")) slurp))
        resolve-table-name (fn [table-name] (some-> (io/resource (str "sim/modules/lookup_tables/" table-name)) slurp))
        closure (:payload (patient-simulator/load-closure "vhd-pulmonic"
                                            (slurp (io/resource "sim/modules/vhd_pulmonic.json"))
                                            resolve-call-path resolve-table-name))]
    (engine-pair {:seed 20260802 :patients 300 :pathway {:name "module-only" :steps []}
                  :modules [closure] :module-assignment [{:module-id "vhd-pulmonic" :weight 1}]
                  :module-horizon-days 36500})))

(defn- vhd-tricuspid-pair []
  (let [resolve-call-path (fn [call-path] (some-> (io/resource (str "sim/modules/" call-path ".json")) slurp))
        resolve-table-name (fn [table-name] (some-> (io/resource (str "sim/modules/lookup_tables/" table-name)) slurp))
        closure (:payload (patient-simulator/load-closure "vhd-tricuspid"
                                            (slurp (io/resource "sim/modules/vhd_tricuspid.json"))
                                            resolve-call-path resolve-table-name))]
    (engine-pair {:seed 20260802 :patients 300 :pathway {:name "module-only" :steps []}
                  :modules [closure] :module-assignment [{:module-id "vhd-tricuspid" :weight 1}]
                  :module-horizon-days 36500})))

(defn- med-rec-pair []
  (let [module (:payload (patient-simulator/load-module "med-rec" (slurp (io/resource "sim/modules/med_rec.json"))))]
    (engine-pair {:seed 20260802 :patients 300 :pathway {:name "module-only" :steps []}
                  :modules [(patient-simulator/singleton-closure module)] :module-assignment [{:module-id "med-rec" :weight 1}]
                  :module-horizon-days 36500})))

;; --- Fidelity payoff (2026-08-08, ADR-0083, AR-FP-1/2): the
;; twenty-eighth root -- a FIRST BASELINE, deferred whole at vendoring
;; batch 2 (ADR-0071) on the same dangling-`:encounter-end` gap the
;; EncounterEnd fix (ADR-0082) closed structurally; this module's own
;; in-session proof there found violations fully extinguished at this
;; SAME seed/population. `:persona-config` carries the race-weighting
;; ADR-0071's own finding first required (no other root here reads
;; `:race`) -- the same shape `ehrt.patient-simulator.census/default-
;; persona-config` and `vendored_anemia_test.clj` both already use.

(defn- anemia-pair []
  (let [resolve-call-path (fn [call-path] (some-> (io/resource (str "sim/modules/" call-path ".json")) slurp))
        closure (:payload (patient-simulator/load-closure "anemia-unknown-etiology"
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
        closure (:payload (patient-simulator/load-closure "colorectal-cancer"
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
  (let [module (:payload (patient-simulator/load-module "veteran-lung-cancer" (slurp (io/resource "sim/modules/veteran_lung_cancer.json"))))
        seeded-closure (assoc (patient-simulator/singleton-closure module) :initial-attributes {:veteran-lung-cancer/veteran true})]
    (engine-pair {:seed 20260802 :patients 300 :pathway {:name "module-only" :steps []}
                  :modules [seeded-closure] :module-assignment [{:module-id "veteran-lung-cancer" :weight 1}]
                  :module-horizon-days 36500})))

(defn- veteran-prostate-cancer-pair []
  (let [module (:payload (patient-simulator/load-module "veteran-prostate-cancer" (slurp (io/resource "sim/modules/veteran_prostate_cancer.json"))))
        seeded-closure (assoc (patient-simulator/singleton-closure module) :initial-attributes {:veteran-prostate-cancer/veteran true})]
    (engine-pair {:seed 20260802 :patients 300 :pathway {:name "module-only" :steps []}
                  :modules [seeded-closure] :module-assignment [{:module-id "veteran-prostate-cancer" :weight 1}]
                  :module-horizon-days 36500})))

(defn- veteran-ptsd-pair []
  (let [module (:payload (patient-simulator/load-module "veteran-ptsd" (slurp (io/resource "sim/modules/veteran_ptsd.json"))))
        seeded-closure (assoc (patient-simulator/singleton-closure module) :initial-attributes {:veteran-ptsd/veteran true})]
    (engine-pair {:seed 20260802 :patients 300 :pathway {:name "module-only" :steps []}
                  :modules [seeded-closure] :module-assignment [{:module-id "veteran-ptsd" :weight 1}]
                  :module-horizon-days 36500})))

(defn- veteran-self-harm-pair []
  (let [resolve-call-path (fn [call-path] (some-> (io/resource (str "sim/modules/" call-path ".json")) slurp))
        closure (:payload (patient-simulator/load-closure "veteran-self-harm"
                                            (slurp (io/resource "sim/modules/veteran_self_harm.json"))
                                            resolve-call-path))
        seeded-closure (assoc closure :initial-attributes {:veteran-self-harm/veteran true})]
    (engine-pair {:seed 20260802 :patients 300 :pathway {:name "module-only" :steps []}
                  :modules [seeded-closure] :module-assignment [{:module-id "veteran-self-harm" :weight 1}]
                  :module-horizon-days 36500})))

(defn- veteran-substance-abuse-treatment-pair []
  (let [module (:payload (patient-simulator/load-module "veteran-substance-abuse-treatment" (slurp (io/resource "sim/modules/veteran_substance_abuse_treatment.json"))))
        seeded-closure (assoc (patient-simulator/singleton-closure module) :initial-attributes {:veteran-substance-abuse-treatment/veteran true})]
    (engine-pair {:seed 20260802 :patients 300 :pathway {:name "module-only" :steps []}
                  :modules [seeded-closure] :module-assignment [{:module-id "veteran-substance-abuse-treatment" :weight 1}]
                  :module-horizon-days 36500})))

(defn- demographic-fold-pair
  "Arc 3a part 4's own root (2026-08-26, ADR-0173 ruling D1's commit 2):
  the FIRST oracle root that turns `:persons` ON, and so the first one
  whose digest covers the demographic fold, the two clinical hooks and
  the identification flow at all. FIRST BASELINE -- there is no
  `before` to diff against, because no ref before this commit has a
  root here.

  IT IS ALSO WHY THIS SESSION'S ORACLE RUN CARRIES
  `--declared-digest-change`: the manifest gains a file the baseline
  side cannot produce. The 35 pre-existing roots stay IDENTICAL and
  that is the claim; a difference in any ONE of them would be a STOP.

  IT IS THE ONE ROOT THAT DOES NOT USE `engine-pair`, and the reason is
  a dependency one rather than a preference. `:persons` reaches
  `engine/run` TRANSLATED -- the engine may not require the component
  that draws the person stream (ADR-0172 limitations row 10), so
  somebody on the caller's side has to translate, and that somebody is
  `ehrt.sim.run`. Reimplementing its two-pass ordering here would put
  ruling C1's resolution in two places; widening `ehrt.sim.interface`
  to export it would break that façade's own frozen-surface gate, which
  exists because `components/corpus` depends on its stability
  (ADR-0012) and which is not this session's to re-rule. So this root
  goes through `run-command`, which is ALREADY on that façade and
  already returns exactly the pair every other root captures --
  `:ground-truth` plus, with `:emit` set to hl7, `:messages` rendered
  by `emit-hl7/emit` against the run's own facility and providers.

  A SIDE EFFECT WORTH HAVING: this is the first oracle root to exercise
  `ehrt.sim.run/run-command` at all. Every other one calls `engine/run`
  directly, so the CLI's own orchestration layer -- config merge, module
  resolution, the self-check -- has never been inside an IDENTICAL
  verdict until now.

  THE COVERAGE THIS ROOT ADDS is stated in the block below and is the
  reason it exists: three closed event kinds and one MSH-9 that no root
  could reach before it."
  []
  (let [r (sim/run-command
           {:seed 20260826 :patients 120 :pathway {:name "module-only" :steps []}
            :modules ["sinusitis"]
            :module-assignment [{:module-id "sinusitis" :weight 1}]
            :module-horizon-days 3650
            :emit "hl7" :reference-date "2024-01-01" :utc-offset "+00:00"
            ;; Twice the arrival count and twenty years, the same sizing
            ;; the two gated scenario configs carry and for the same
            ;; measured reasons.
            :persons {:count 240 :years 20}})]
    (when-not (= :ok (:status r))
      (throw (ex-info "demographic-fold root did not run cleanly" {:result r})))
    {:ground-truth (:ground-truth (:payload r))
     :hl7 (vec (:messages (:payload r)))}))

(defn- encounter-horizon-pair
  "Arc 3b sweep 1's own root (ADR-0174 section 2(a), rulings A1/B1/C1),
  and the FIRST to turn `:encounters` on. FIRST BASELINE, not a
  regression check: no side before this commit has an `:encounters` key
  to run under at all.

  IT GOES THROUGH `run-command` for `demographic-fold`'s own reason and
  no other: `:persons` reaches `engine/run` TRANSLATED, and a repeat
  arrival is the whole point here -- a second encounter needs a person
  the pool has already sent once. `:encounters` itself needs no
  translation and would have been an `engine-pair` away otherwise.

  WHY THIS CONFIG. Sixty arrivals over a pool of TWENTY people makes
  better than half of them repeats, which is exactly the traffic the
  horizon used to discard; a real admit/delay/discharge pathway gives
  each repeat something to queue (an empty pathway would queue nothing
  and the lift would be invisible); `--churn` is on because the churn
  family is where the encounter's sharp edges live -- a `:cancel-admit`
  spends an ordinal, a `:cancel-discharge` re-opens an encounter rather
  than minting a second, and a `:bed-swap` is the one kind that names
  TWO encounters and carries neither at top level.

  Measured at the config below, not predicted: 170 events, 106 messages,
  33 patients over 36 encounter openers, FOURTEEN patients with more
  than one encounter, SIXTEEN encounters the pre-sweep engine would have
  discarded, and a maximum of THREE encounters on one patient -- against
  a maximum of ONE at every corpus this repo had before the sweep
  (ADR-0174 section 1's own census table).

  THE COVERAGE IT ADDS is stated in the block below and is larger than
  the encounter: three event kinds and three MSH-9s no root could reach
  before it."
  []
  (let [r (sim/run-command
           {:seed 20260827 :patients 60 :churn true
            :pathway {:name "return-visits"
                      :steps [{:type :admission :location "Renal"}
                              {:type :delay :from 240 :to 900}
                              {:type :discharge}]}
            :emit "hl7" :reference-date "2024-01-01" :utc-offset "+00:00"
            :persons {:count 20 :years 20}
            :encounters true})]
    (when-not (= :ok (:status r))
      (throw (ex-info "encounter-horizon root did not run cleanly" {:result r})))
    {:ground-truth (:ground-truth (:payload r))
     :hl7 (vec (:messages (:payload r)))}))

(defn- bed-cycle-pair
  "Arc 3b sweep 2's own root (ADR-0174 section 2(c), plus ruling C's
  ADT^A20), and the FIRST to turn `:bed-cycle` on. FIRST BASELINE, not a
  regression check: no side before this commit has a `:bed-cycle` key to
  run under at all.

  IT GOES THROUGH `run-command` for the SELF-CHECK, not for a
  translation: unlike `:persons`, `:bed-cycle` reaches `engine/run`
  untranslated and an `engine-pair` would have rendered the same bytes.
  What `run-command` adds is `check-all`, and this root is the only
  place the cycle's three new invariants are exercised at population
  scale by the oracle rather than by a unit fixture.

  WHY THIS CONFIG, and every number in it was PROBED rather than
  guessed. A ward small enough to CONTEND is the whole point -- the
  cycle is invisible in a facility that never runs out of ready beds,
  and it is contention that produces boarding, the outlier rung, and the
  bed-ready transfers whose timing this sweep moves. Three earlier
  candidates were rejected because they EXHAUSTED the ladder outright
  (`:error :capacity-exhausted`, which halts a run rather than degrading
  it -- see the coverage note below), which is exactly the effective-
  capacity risk ADR-0174 section 2(c) item 4 names. `--churn` is on
  because the reinstatement arc lives in that family and because it is
  the only way `:bed-swap` -- the kind invariant 1 must EXCLUDE -- occurs
  at all.

  Measured at the config below, not predicted: 527 events, 467 messages,
  295 `:bed-status-change` events over 26 distinct beds (99 dirty, 98
  cleaning, 98 ready -- the tail difference is beds still mid-cycle when
  the run ends), 43 transfers of which 39 are BED-READY, and ZERO of
  those 39 sharing the instant of the discharge that vacated the bed.
  The ladder reaches all four rungs: 45 rung-1, 17 rung-2, 10 rung-3 and
  THIRTY-ONE rung-4 placements.

  THE COVERAGE IT ADDS is stated in the block below and is larger than
  the cycle: it is the first root to reach rung 4 at all, the first to
  produce a `:cancel-discharge`, and the first to emit ADT^A13 or
  ADT^A20."
  []
  (let [r (sim/run-command
           {:seed 20260828 :patients 60 :churn true :arrival-gap 90
            :facility {:id :bed-cycle-oracle
                       :wards [{:id :ed :name "Emergency" :beds 0 :surge-slots 16
                                :surge-format "%s-H%02d" :class :ed
                                :turnaround-minutes [5 15]}
                               {:id :renal :name "Renal" :beds 4 :surge-slots 2
                                :surge-format "%s-H%02d" :class :inpatient
                                :turnaround-minutes [15 30]}
                               {:id :cardiology :name "Cardiology" :beds 4 :surge-slots 2
                                :surge-format "%s-H%02d" :class :inpatient
                                :turnaround-minutes [15 30]}]}
            :pathways [{:pathway {:name "renal-stay"
                                  :steps [{:type :admission :location "Renal"}
                                          {:type :delay :from 180 :to 900}
                                          {:type :discharge}]}
                        :weight 1}
                       {:pathway {:name "cardio-stay"
                                  :steps [{:type :admission :location "Cardiology"}
                                          {:type :delay :from 180 :to 900}
                                          {:type :discharge}]}
                        :weight 1}]
            :emit "hl7" :reference-date "2024-01-01" :utc-offset "+00:00"
            :bed-cycle true})]
    (when-not (= :ok (:status r))
      (throw (ex-info "bed-cycle root did not run cleanly" {:result r})))
    {:ground-truth (:ground-truth (:payload r))
     :hl7 (vec (:messages (:payload r)))}))

(defn- scheduling-pair
  "Arc 3b sweep 3's own root (ADR-0174 section 2(b)), and the FIRST to
  turn `:scheduling` on. FIRST BASELINE, not a regression check: no side
  before this commit has a `:scheduling` key to run under at all.

  IT GOES THROUGH `run-command` for the SELF-CHECK, for `bed-cycle-pair`'s
  own reason: `:scheduling` reaches `engine/run` untranslated and an
  `engine-pair` would render the same bytes, so what `run-command` adds is
  `check-all` -- and this root is the only place scheduling's FOUR new
  invariants are exercised at population scale by the oracle rather than
  by a unit fixture.

  IT IS `bed-cycle-pair`'s FACILITY AND PATHWAYS VERBATIM, at a different
  seed, and that is deliberate rather than lazy: holding the operational
  half fixed makes every byte of the delta between the two roots
  attributable to scheduling alone, and it means this root inherits a
  facility already PROBED to contend without exhausting. `--churn` is on
  for the same reason it is there -- it is the only way `:bed-swap` and
  the reinstating cancels occur at all.

  `:encounters` IS ON AND IS LOAD-BEARING, not decoration. Without sweep
  1's horizon a follow-up's return visit would open nothing, and
  `scheduled-encounter-follows-its-appointment` -- the one invariant the
  ADR says is non-vacuous ONLY because sweep 1 landed -- would be judging
  an empty set here.

  Measured at the config below, not predicted: 487 events, 331 messages,
  60 appointments over 60 patients, with all four outcomes witnessed --
  4 cancelled, 4 no-showed, 4 rescheduled, and 51 kept. 79 encounter
  openers of which 51 carry an `:appointment-id`, and TWENTY-ONE of those
  are `:outpatient-visit` openers, i.e. SCHEDULED SECOND ENCOUNTERS: 21
  distinct patients hold more than one encounter, which no root in this
  repository could produce before sweep 1 and none produced BY BOOKING
  before this one.

  THE COVERAGE IT ADDS: the four scheduling kinds, which no other root
  emits, and the first population-scale exercise of a follow-up-produced
  second encounter. It adds NO message type -- none of the four reaches
  the wire (ruling C, MSH-12 vs the v2.4 SIU structures), which is why
  its message count is lower than its event count by more than the bed
  cycle's alone accounts for."
  []
  (let [r (sim/run-command
           {:seed 20260829 :patients 60 :churn true :arrival-gap 90
            :facility {:id :scheduling-oracle
                       :wards [{:id :ed :name "Emergency" :beds 0 :surge-slots 16
                                :surge-format "%s-H%02d" :class :ed
                                :turnaround-minutes [5 15]}
                               {:id :renal :name "Renal" :beds 4 :surge-slots 2
                                :surge-format "%s-H%02d" :class :inpatient
                                :turnaround-minutes [15 30]}
                               {:id :cardiology :name "Cardiology" :beds 4 :surge-slots 2
                                :surge-format "%s-H%02d" :class :inpatient
                                :turnaround-minutes [15 30]}]}
            :pathways [{:pathway {:name "renal-stay"
                                  :steps [{:type :admission :location "Renal"}
                                          {:type :delay :from 180 :to 900}
                                          {:type :discharge}]}
                        :weight 1}
                       {:pathway {:name "cardio-stay"
                                  :steps [{:type :admission :location "Cardiology"}
                                          {:type :delay :from 180 :to 900}
                                          {:type :discharge}]}
                        :weight 1}]
            :emit "hl7" :reference-date "2024-01-01" :utc-offset "+00:00"
            :bed-cycle true :encounters true
            :scheduling {:scheduled-fraction 0.55
                         :lead-time-days [1 14]
                         :no-show-rate 0.12
                         :reschedule-rate 0.10
                         :cancel-rate 0.08
                         :follow-up {:rate 0.45 :interval-days [14 90]}}})]
    (when-not (= :ok (:status r))
      (throw (ex-info "scheduling root did not run cleanly" {:result r})))
    {:ground-truth (:ground-truth (:payload r))
     :hl7 (vec (:messages (:payload r)))}))

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
        closure (:payload (patient-simulator/load-closure "injuries" (slurp (io/resource "sim/modules/injuries.json")) resolve-call-path))]
    (engine-pair {:seed 20260802 :patients 300 :pathway {:name "module-only" :steps []}
                  :modules [closure] :module-assignment [{:module-id "injuries" :weight 1}]
                  :module-horizon-days 18250})))

;; --- COVERAGE (2026-08-19, ADR-0156, register rows L1-1/L1-2/L1-3;
;;     WIDENED 2026-08-26 by arc 3a part 4's own root) ---
;;
;; WHAT `IDENTICAL` MEANS, AND WHAT IT DOES NOT. This block is INSIDE
;; the region `bin/regression-oracle`'s soundness check compares, on
;; purpose. A coverage claim that sat in the docstring above could drift
;; with no bracket noticing -- which is exactly how `Six roots` survived
;; to 35. Here, widening or narrowing coverage forces
;; `--declared-digest-change`, and the session that pays it has to say
;; which way coverage moved.
;;
;; WHICH WAY IT MOVED, 2026-08-26: WIDER, by one root. `demographic-fold`
;; turns `:persons` on and lifts five surfaces out of the vacuous set
;; below -- `:merge` (the KIND), `merge-message`, `mrg-segment`, the
;; MSH-9 `ADT^A40`, and the two kinds the fold itself mints. Measured on
;; that root's own output, not predicted: 671 events, 134 messages,
;; kinds `#{:admission :coverage-change :demographic-update :discharge
;; :medication-order :merge :outpatient-visit :outpatient-visit-end
;; :registered}`, MSH-9s `#{"ADT^A01" "ADT^A03" "ADT^A04" "ADT^A40"}`.
;; The A40 arrives by the IDENTIFICATION merge, which is churn's own
;; `:merge` shape with a `:cause` -- so the oracle now witnesses the
;; merge emitter for the first time, though still not churn's own
;; `decide :merge`, whose lottery no root turns on.
;;
;; THE VACUOUS SET -- surfaces no root can move, so no IDENTICAL verdict
;; says anything about them (review 4's instrumented reachability
;; battery over 78 named surfaces, all 35 roots, its output proven
;; byte-identical to the clean digest):
;;
;;   * `evolve` dispatches: 7 of 23 unreached, which is the complement
;;     of the kind list immediately below (that multi dispatches on
;;     `:event`, so the two are the same measurement). It was 8 of 21
;;     before 2026-08-26: the contract gained two kinds at 1.3.0 and
;;     BOTH are now witnessed, and `:merge` left the unreached set.
;;   * `decide` dispatches: NOT RE-INSTRUMENTED this session, and said
;;     so rather than restated. Review 4's battery measured 9 of 22
;;     unreached; since then the multi has gained five methods
;;     (`:demographic-update`, `:coverage-change`, `:person-encounter`,
;;     `:identity-fill`, `:identification-merge`) and `demographic-fold`
;;     reaches all five, so both the numerator and the denominator
;;     moved. Re-running that battery is its own piece of work and is
;;     not smuggled into this bullet as an updated-looking number.
;;   * 4 of the event contract's 23 closed kinds: :cancel-discharge,
;;     :order-placed, :result-available, :step-rejected. `:merge` LEFT
;;     this set on 2026-08-26 -- `demographic-fold`'s identification
;;     merges are the first any root has produced -- and `:bed-swap`,
;;     `:cancel-admit` and `:cancel-transfer` left it the same day, by
;;     `encounter-horizon`, the first root to run `--churn` over a
;;     pathway that admits. `:cancel-discharge` STAYED: churn's own
;;     lottery did not draw one at that seed, so the cancel family is
;;     two-thirds witnessed and not whole.
;;   * The whole order->result path: `orm-message`, `oru-message`,
;;     `obx-segment`. NOTE the precision, because the summary version of
;;     this claim is wrong: ORU^R01 IS emitted, 1,768 times across 14
;;     roots -- by `observation-message` and `diagnostic-report-message`.
;;     It is `oru-message`, the :result-available order-result emitter,
;;     that no root reaches. ORM^O01 is emitted zero times.
;;   * The Z-segment pair, `plan-latency`/`emit-wire` (the second
;;     clock), `v2-replay/fold-message` and `engine/replay`.
;;     `merge-message` and `mrg-segment` LEFT this list on 2026-08-26,
;;     by `demographic-fold` and its A40; `bed-swap-message` and
;;     `churn/inject`+`strip` left it the same day by
;;     `encounter-horizon`, which is the first root to pass
;;     `:churn true` at all.
;;   * `sim-check` was on this list "in its entirety" and is NO LONGER,
;;     with a precision that matters: `demographic-fold` goes through
;;     `ehrt.sim.run/run-command`, which SELF-CHECKS, so `check-all`
;;     runs over that root's log inside the bracket and a firing
;;     invariant would make the root throw rather than write a file.
;;     What is still outside is its FINDINGS -- no digest here captures
;;     a check result -- so an IDENTICAL verdict says the catalog passed
;;     on one root, never what it reported.
;;
;; THE STRUCTURAL CAUSE, re-derived rather than inferred from the
;; instrumentation: 36 of the 39 roots pass `:pathway {:name
;; "module-only" :steps []}` -- `encounter-horizon`, `bed-cycle` and
;; `scheduling` are the three exceptions and pass real
;; admit/delay/discharge pathways,
;; which is why the churn family and the allocation ladder reach them --
;; and 8 of 19 components plus `bases/cli` are off the
;; oracle classpath entirely (`bin/regression-oracle`'s own deps block).
;; THREE components joined that classpath on 2026-08-26 -- person-
;; simulator, sim-check and sim-emit-fhir -- none because `digest.clj`
;; requires them, all because `ehrt.sim.run/run-command` does, and
;; `demographic-fold` goes through it.
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
;; THE CAPACITY WITNESS WAS ONE ROOT DEEP AND IS NOW TWO.
;; `death-fixture` used to carry the oracle's only `:transfer`, its only
;; ADT^A02, its only `:bed-ready true` and all 13 of its ladder rung-3
;; placements. `encounter-horizon` (2026-08-26) carries transfers and
;; A02s of its own, so those four claims no longer die with one root.
;; RUNG 4 IS NO LONGER ZERO: `bed-cycle` (2026-08-27) reaches all four
;; rungs -- 45 rung-1, 17 rung-2, 10 rung-3 and 31 rung-4 placements --
;; which is what the sweep-1 note said sweep 2 would have to move, and it
;; moved for a reason worth keeping: a `:ready` gate makes a small ward
;; CONTEND where an ungated one did not. `:forced` and `:exhausted` are
;; STILL zero across all 38, and `:exhausted` is worth naming precisely
;; -- it is not a degraded outcome a root could carry, it HALTS the run
;; (`:error :capacity-exhausted`), so a root witnessing it would be a
;; root that produces no corpus at all. Three earlier candidate configs
;; for `bed-cycle` did exactly that and were rejected for it.
;; `:medication-end` is one root
;; deep too (`injuries`, one occurrence) -- and so, as of 2026-08-26,
;; are `:demographic-update` and `:coverage-change`, both of them
;; `demographic-fold`'s alone -- `:merge` and ADT^A40 left that list on
;; 2026-08-26 when `encounter-horizon` began producing merges of its
;; own. That root is still the single point of failure for the
;; identification flow specifically, stated here rather than discovered
;; later. And ADT^A11, ADT^A12 and ADT^A17 are now one root deep in
;; their turn -- `encounter-horizon`'s alone -- which is the price of
;; adding a root rather than a hole it closes. ADT^A13 and ADT^A20 join
;; them as of 2026-08-27, one root deep and `bed-cycle`'s alone -- and
;; ADT^A20 unavoidably so, since it is the only root that can emit one.
;;
;; The two sets below are the committed claim, asserted against a FRESH
;; 38-root digest by `ehrt.integration.oracle-coverage-test` and checked
;; for shape, population, membership and location on every push by
;; `ehrt.docs-tooling.oracle-coverage-test`.

(def ^:private witnessed-event-kinds
  "The 26 of 28 closed event kinds any root can produce. Adding a root
  that reaches the order->result paths moves this set -- that is
  R4-Q6 (ii) (b), rowed and priced, deliberately not taken.

  WIDENED AGAIN 2026-08-27, 21 of 24 -> 26 of 28, by `scheduling`. The
  DENOMINATOR grew by the four kinds that root exists for
  (`:appointment`, `:reschedule`, `:appointment-cancel`, `:no-show`,
  contract 1.7.0), and the NUMERATOR by those four PLUS ONE THAT WAS NOT
  PREDICTED: `:step-rejected`, which no root had ever witnessed and which
  the next paragraph listed as unreachable.

  THAT FIFTH KIND IS THE INTERESTING ONE, and it is recorded rather than
  quietly absorbed. `:step-rejected` is what a churned step looks like
  when it was legal when INSERTED and illegal by the time it EXECUTED --
  and scheduling is the first thing in this repository that puts real
  distance between those two instants. A lead time displaces a whole
  arrival by days, so a churn step injected against the world at booking
  meets a world days older when it runs. The order->result path is STILL
  unwitnessed: `:order-placed` and `:result-available` are what is left,
  two of 28 rather than three of 24.

  WIDENED AGAIN 2026-08-27, 19 of 23 -> 21 of 24, by `bed-cycle`. The
  DENOMINATOR grew by the kind that root exists for
  (`:bed-status-change`, contract 1.6.0), and the NUMERATOR by that kind
  plus `:cancel-discharge` -- which sweep 1 left unwitnessed and named
  as such rather than rounding up. THE CHURN FAMILY IS NOW WHOLE. What
  was left unwitnessed AT THAT POINT was the order->result path alone:
  `:order-placed`, `:result-available` and `:step-rejected` -- the last
  of which the sweep after this one reached, see above.

  WIDENED AGAIN 2026-08-26, 16 of 23 -> 19 of 23, by
  `encounter-horizon`: `:bed-swap`, `:cancel-admit` and
  `:cancel-transfer` all arrive at once, because that root is the first
  to run `--churn` on a pathway that admits. The CHURN FAMILY leaves the
  unreached set entirely except for `:cancel-discharge`, which its
  lottery did not draw at this seed and which is therefore still
  unwitnessed -- said plainly rather than rounded up to \"churn is
  covered\".

  WIDENED 2026-08-26, 13 of 21 -> 16 of 23, by `demographic-fold`: the
  denominator grew by the two kinds contract 1.3.0 added, and the
  numerator by those two plus `:merge`."
  #{:admission :appointment :appointment-cancel :bed-status-change
    :bed-swap :cancel-admit
    :cancel-discharge :cancel-transfer :care-plan-end
    :care-plan-start :coverage-change
    :demographic-update :diagnostic-report
    :discharge :medication-end :medication-order :merge :no-show
    :observation
    :outpatient-visit :outpatient-visit-end :procedure :registered
    :reschedule :step-rejected
    :transfer})

(def ^:private witnessed-message-types
  "Every MSH-9 the 36 engine-layer roots emit. ADT^A02 is death-fixture's
  alone, once. ADT^A08/A34 and ORM^O01 are emitted by no root at all.

  WIDENED AGAIN 2026-08-27 by `bed-cycle`: ADT^A20 (bed status update),
  the message family ruling C added to this arc and the FIRST this
  project emits that carries no PID and no PV1; and ADT^A13 (cancel
  discharge), which arrives with the `:cancel-discharge` sweep 1 could
  not draw. A13 was named in this docstring as emitted by no root; it is
  now emitted by exactly one.

  WIDENED AGAIN 2026-08-26 by `encounter-horizon`, which is the first
  root to run the churn family over a pathway that admits: ADT^A11
  (cancel admit), ADT^A12 (cancel transfer) and ADT^A17 (bed swap) all
  arrive with it, and it produces A40s of its own, so the merge emitter
  is no longer one root deep. ADT^A13 (cancel discharge) did NOT arrive
  -- churn's lottery did not draw one at that seed -- and is still
  emitted by no root, which is why this docstring names it rather than
  claiming the cancel family whole.

  WIDENED 2026-08-26 by `demographic-fold`'s identification merges --
  the first A40 any root has produced, and with it the first
  `merge-message` and `mrg-segment`. NOTE what it does NOT buy: the two
  kinds the fold itself mints render no message of their own in 1.4.0
  (an A08 for `:demographic-update` is a later arc's candidate, named
  in `message-type-registry`'s own comment), so the fold reaches this
  set only through the merge."
  #{"ADT^A01" "ADT^A02" "ADT^A03" "ADT^A04" "ADT^A11" "ADT^A12"
    "ADT^A13" "ADT^A17" "ADT^A20" "ADT^A40" "ORU^R01"})

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
   "encounter-horizon"                  encounter-horizon-pair
   "bed-cycle"                          bed-cycle-pair
   "scheduling"                         scheduling-pair
   "veteran-self-harm"                  veteran-self-harm-pair
   "veteran-substance-abuse-treatment"  veteran-substance-abuse-treatment-pair
   "injuries"                           injuries-pair
   "demographic-fold"                   demographic-fold-pair})

(defn -main
  "Writes one <root>.edn per root into out-dir (pr-str of the batch or
  the {:ground-truth :hl7} pair) -- bin/regression-oracle's own shell
  loop sha256sums each file itself; this process never hashes."
  [out-dir]
  (kernel/mkdirs! (io/file out-dir))
  (doseq [[root-name f] (sort-by key roots)]
    (println "running" root-name "...")
    (let [content (f)
          file (io/file out-dir (str root-name ".edn"))]
      (spit file (pr-str content))
      (println "  wrote" (.getPath file))))
  (println "done"))
