(ns ehrt.sim-engine.assignment
  "Weighted per-patient assignment: which pathway a patient walks and
  which module they carry, plus the cumulative-weight bucketing both
  share. `engine.clj`'s EIGHTH extraction under
  `roadmap.md#engine-namespace-extraction-and-apply-unification`, landed
  in the same session as `ehrt.sim-engine.config` and immediately after
  it: the census's own dependency order (`.agents/plans/engine-
  extraction-census.md` section 3a) puts both in the LEAF rank alongside
  `streams` and `state`, and neither depends on the other.

  THE FIXED-CONSUMPTION LAW IS WHAT THESE TWO FUNCTIONS ARE FOR, and it
  is why they read as one namespace rather than as two helpers of
  `run`'s pre-loop. Each ALWAYS consumes exactly one `.nextDouble`,
  whether or not the draw's own outcome is used, so adding one scripted
  override for patient N never shifts every OTHER patient's downstream
  draws (sim/ADR-0009's own rejected-alternative reasoning). Eight files
  across five bricks cite this pair by name as the worked precedent for
  that law -- `hazards.clj`, `persona.clj`, `order_profiles.clj`,
  `emit_hl7.clj`, `gmf_interpreter.clj`, `gmf.clj`, `sim/run.clj` and
  `docs/dev/simulator-architecture.md` -- and every one of those
  citations names the VARS, which `engine.clj`'s delegating defs keep
  resolving.

  A CODE-LEVEL LEAF, and it does NOT go through `ehrt.sim-engine.
  streams`. Each assigner takes `.nextDouble` off the
  `java.util.Random` its caller hands it rather than through
  `uniform-choice`, which is exactly what makes the one-draw law
  readable inside one function. The only name crossing out of this
  namespace is `java.util.Random`, imported below for the two type
  hints; nothing here calls anything `engine.clj` defines.

  Extracted OUTPUT-IDENTICAL: every form below is `engine.clj`'s own
  text, moved and not rewritten -- including both interior comment
  blocks, which travel with the forms they introduce. The second of
  them makes a POSITIONAL claim, that `assign-module` has \"the SAME
  shape/law as `assign-pathway` just above\", and it survives the move
  unedited because `assign-pathway` is still directly above it here.
  That is the first travelling positional claim of these eight
  extractions that needed no restatement.

  TWO OF THE THREE VARS WERE PUBLIC in `engine.clj` and keep delegating
  defs there under ruling C1(a). Neither is on `ehrt.sim-engine.
  interface`'s re-export list, and census constraint 4 names neither:
  what makes both defs load-bearing is `engine_test.clj`, which calls
  `engine/assign-pathway` seven times and `engine/assign-module` three,
  and which C1(a) forbids this session to touch.

  `weighted-pick` STAYS `defn-`, and it is the first private mover of
  these eight extractions to do so. Census constraint 5 has two halves.
  Its PROHIBITION -- a private mover must NOT gain a delegating def in
  `engine.clj` -- is honoured, and was verified live: `engine/weighted-
  pick` does not resolve. Its DESCRIPTION -- that a private var which
  moves becomes public in its new namespace -- was in every cluster
  before this one FORCED by call sites left behind: `streams`' four,
  `state`'s cycle breaker, `encounters`' ten and `log-index`'s nine all
  had to stay reachable FROM `engine.clj`, so widening was the only way
  to move them. `weighted-pick`'s only two callers are `assign-pathway`
  and `assign-module`, which travel with it, so nothing forces it here.
  Widening it anyway would enlarge this namespace's public surface for
  no caller at all, AND would falsify the very sentence this session's
  move commit repaired: `sim_model/persona.clj`'s docstring cites this
  function's PRIVACY to explain why persona keeps an independent copy
  of the same bucketing, and that commit repointed it from
  `ehrt.sim-engine.engine`'s namespace to this one -- a private mover
  having no delegating def to forward a citation."
  (:import [java.util Random]))

;; --- M3-adjacent: per-patient pathway assignment (roadmap.md's M3 entry,
;; SimHospital's percentage_of_patients analogue -- the distribution layer
;; M5's CompileTrajectory will also need, one pathway per patient) --------

(defn- weighted-pick
  "Which pool member `draw` (a uniform double in [0,1), already
  consumed by the caller) falls into, among `pool` ({value-key :weight}
  maps) -- cumulative-weight bucketing, falling through to the last
  member on any floating-point-boundary edge case rather than nil.
  `value-key` is which field names the resolved value -- :pathway for
  sim-model/PathwaysConfig, :module-id for M5b's own
  ehrt.patient-simulator.gmf/ModulesConfig -- the same pool shape, two resource
  kinds."
  [pool draw value-key]
  (let [total (reduce + (map :weight pool))
        target (* draw total)]
    (loop [members pool acc 0.0]
      (let [m (first members)
            more (rest members)
            acc' (+ acc (double (:weight m)))]
        (if (or (empty? more) (< target acc'))
          (get m value-key)
          (recur more acc'))))))

(defn assign-pathway
  "Resolves the pathway `pathways-config` (sim-model/
  PathwaysConfig) assigns to patient ordinal `i` (0-indexed arrival
  order): an explicit {:patient-ordinal i :pathway ...} entry when one
  names this ordinal, otherwise a weighted pick among the config's
  {:pathway :weight} pool entries. Today's single-:pathway `run` config
  is this function's degenerate case, expressed as a one-entry weighted
  pool with :weight 1 -- see `run`'s own docstring for why that case is
  NOT wired to skip the draw below (it would perturb the very law this
  paragraph states next).

  ALWAYS consumes exactly one `.nextDouble` from `rng`, whether the
  outcome is the explicit override or the weighted pick -- fixed RNG
  consumption per patient, sim/ADR-0009's own rejected-alternative reasoning
  extended here: making draw count depend on whether THIS patient
  happens to have an explicit override would mean adding one scripted
  override for patient N shifts every OTHER patient's downstream draws,
  the exact surprising coupling sim/ADR-0009 already rejected for bed
  choice (there: 'consumption changed once, for a documented reason' is
  the accepted property; making it depend on candidate count is not)."
  [^Random rng pathways-config i]
  (let [draw (.nextDouble rng)
        explicit (first (filter #(= i (:patient-ordinal %)) pathways-config))]
    (if explicit
      (:pathway explicit)
      (weighted-pick (filterv :weight pathways-config) draw :pathway))))

;; --- M5b: per-patient module assignment (ehrt.patient-simulator.gmf/
;; ModulesConfig) -- the SAME shape/law as assign-pathway just above,
;; extended to modules per components/patient-simulator/docs/gmf-interpreter.md's own Task 4 (module
;; assignment composes with :pathways -- both just IR entering the union).

(defn assign-module
  "Resolves the module id `modules-config` (ehrt.patient-simulator.gmf/
  ModulesConfig) assigns to patient ordinal `i` -- an explicit
  {:patient-ordinal i :module-id ...} entry when one names this ordinal,
  otherwise a weighted pick among the config's {:module-id :weight} pool
  entries, or nil when NEITHER covers this ordinal (unlike
  assign-pathway's own PathwaysConfig, a real population is expected to
  have patients with no assigned module at all -- most people don't have
  chronic sinusitis -- so an empty/non-covering pool is a legitimate,
  common case, not a caller error). ALWAYS consumes exactly one
  `.nextDouble` from `rng` regardless of outcome, the same fixed-
  consumption law `assign-pathway` already establishes, for the
  identical reason (sim/ADR-0009's own rejected-alternative reasoning)."
  [^Random rng modules-config i]
  (let [draw (.nextDouble rng)
        explicit (first (filter #(= i (:patient-ordinal %)) modules-config))
        pool (filterv :weight modules-config)]
    (cond
      explicit (:module-id explicit)
      (seq pool) (weighted-pick pool draw :module-id)
      :else nil)))
