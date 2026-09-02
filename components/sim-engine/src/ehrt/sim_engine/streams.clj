(ns ehrt.sim-engine.streams
  "The RNG stream partition and the deterministic id minting that sits
  beside it -- `engine.clj`'s first extraction under
  `roadmap.md#engine-namespace-extraction-and-apply-unification`
  (author ruling C2(b)).

  Extracted OUTPUT-IDENTICAL: every form below is `engine.clj`'s own
  text, moved and not rewritten. `engine.clj` requires this namespace
  and keeps a delegating def for each var that was public there, so
  every existing requirer -- `ehrt.sim-engine.interface`, and every test
  reaching `ehrt.sim-engine.engine` directly -- still resolves against
  `engine.clj` (ruling C1(a)). Four vars were private in `engine.clj`
  and are public HERE only because a sibling namespace has to call them:
  `rand-int-in`, `uniform-choice`, `minted-encounter-id-field`,
  `minted-appointment-id-field`. They get no delegating def, so the
  engine's own public surface is unchanged.

  Three concerns, in the order they compose:

  * **Draw primitives** -- `rand-int-in`, `uniform-choice`: the two
    shapes of draw the engine takes off a `java.util.Random` beyond a
    bare `.nextDouble`.
  * **Mixing and identity** -- `mix64` and the three `*-id-for`
    functions built on it, plus the two ordinal counters and the two
    world-facing minted-field helpers. Every id here is a PURE function
    of the run's seed and a patient's ordinals, deliberately OFF the
    seeded streams, so identity generation adds no draws for
    `sim/ADR-0009`'s seed-stability accounting to track.
  * **The partition itself** (ADR-0171, arc 1) -- `stream-scheme`,
    `stream-family-tag`, `stream-seed`, `stream`, `newborn-id-tag`,
    `one-stream`: five families keyed on a stable id rather than on
    construction order, so a draw added to one patient's pathway moves
    that patient and no one else.

  The RNG-path law is unchanged by the move: a measurement claiming to
  characterize the simulator's output must still draw from the real
  seeded, threaded path (`rulings.md#R-measure-claimed-population`)."
  (:import [java.util Random]))

;; --- draw primitives ------------------------------------------------------

(defn rand-int-in
  "Uniform integer in [lo, hi] from the seeded RNG."
  [^Random rng lo hi]
  (+ lo (.nextInt rng (inc (- hi lo)))))

(defn uniform-choice
  [^Random rng candidates]
  (nth candidates (.nextInt rng (count candidates))))

;; --- mixing and identity --------------------------------------------------

(defn mix64
  "A fixed, fully-specified 64-bit mix of two longs (splitmix64-style
  constants) -- deliberately NOT an RNG draw. See `patient-id-for`.

  PUBLIC since ADR-0171 ruling A1 (\"reuse `engine.clj:225` `mix64` on
  `(family-tag, id-tag)`, unchanged, promoted from private to the
  sim-engine interface\"): the RNG stream partition derives every
  stream's seed with this same function on the same shape of key, so
  the partition adds no new numeric surface to specify or test. Its
  body is untouched by that promotion -- the constants are the ones
  `patient-id-for` has always used."
  ^long [^long a ^long b]
  (let [x (unchecked-add (unchecked-multiply a -7046029254386353131) b)
        x (unchecked-multiply (bit-xor x (unsigned-bit-shift-right x 30)) -4658895280553007687)
        x (unchecked-multiply (bit-xor x (unsigned-bit-shift-right x 27)) -7723592293110705685)]
    (bit-xor x (unsigned-bit-shift-right x 31))))

(defn patient-id-for
  "The internal, deterministic patient-id (sim/ADR-0010): a PURE function
  of this run's seed and the patient's arrival ordinal (0-indexed) --
  never reassigned, never re-derived elsewhere, and deliberately OFF
  the seeded RNG stream (identity needs no stochastic behavior, only
  spread across seeds -- keeping it off the RNG means identity
  generation adds no new draws for sim/ADR-0009's accounting to track).
  Distinct format from :mrn (\"PID-\" prefix, never \"MRN\") so the two
  id spaces are never visually confusable; the zero-padded ordinal
  leads so patient-id's lexical order matches arrival order exactly
  the way :mrn's already did -- load-bearing for the bed-ready
  tiebreak (docs/patient-state-model.md), which sorts on
  [:admitted-at patient-id]."
  [seed ordinal]
  (format "PID-%06d-%08x" ordinal (bit-and (mix64 seed ordinal) 0xffffffff)))

(defn encounter-id-for
  "The internal, deterministic encounter-id (ADR-0174 ruling B1, arc 3b
  sweep 1): `patient-id-for`'s own contract applied one level down -- a
  PURE function of this run's seed, the patient's arrival ordinal
  (0-indexed) and that patient's own 0-indexed encounter ordinal, and
  deliberately OFF the seeded RNG streams, so lifting the
  single-encounter horizon adds no draws for sim/ADR-0009's accounting
  to track.

  It takes the ORDINAL the patient-id already encodes rather than
  hashing the patient-id STRING, because `stream-family-tag`'s own
  docstring is explicit that a hash this repo does not own must not be
  load-bearing -- and `mix64` is this repo's own (PUBLIC since ADR-0171
  ruling A1). Distinct prefix from :mrn and from patient-id (`ENC-`,
  never `PID-` or `MRN`) so the three id spaces are never visually
  confusable, and the zero-padded arrival ordinal leads for the same
  lexical-order reason patient-id's does.

  The two REJECTED derivations, kept here because what was declined is
  why this one means anything (ADR-0174 ruling B): a run-scoped
  monotonic counter is order-dependent, the exact property ADR-0171
  rejected `SplittableRandom`'s split order for; the opening event's LOG
  INDEX is free and unique but brittle, and a visit number is an
  IDENTIFIER a consumer persists, not a REFERENCE like
  `:order-event-id`/`:placeholder-event-id` -- any reshuffle would
  renumber every one of them."
  [seed ordinal encounter-ordinal]
  (format "ENC-%06d-%02d-%08x" ordinal encounter-ordinal
          (bit-and (mix64 (mix64 seed ordinal) encounter-ordinal) 0xffffffff)))

(defn next-encounter-ordinal
  "The 0-indexed ordinal this patient's NEXT encounter takes: every
  encounter they have ever opened, counted. Monotone by construction --
  a cancelled encounter STAYS in `:encounters` (marked `:cancelled`)
  precisely so its ordinal is never handed out twice, which is what
  stops `:cancel-admit` followed by a re-admission from minting one id
  for two openers."
  [patient]
  (+ (count (:encounters patient)) (if (:encounter patient) 1 0)))

(defn appointment-id-for
  "The internal, deterministic appointment-id (ADR-0174 section 2(b),
  ruling B1's law applied one level sideways, arc 3b sweep 3).

  EXACTLY `encounter-id-for`'s contract with a different prefix: a PURE
  function of this run's seed, the patient's arrival ordinal (0-indexed)
  and that patient's own 0-indexed appointment ordinal, deliberately OFF
  the seeded RNG streams -- so an appointment costs sim/ADR-0009's draw
  accounting nothing, exactly as an encounter id does.

  It takes the ORDINAL rather than hashing the patient-id STRING for
  `encounter-id-for`'s own stated reason: `stream-family-tag`'s docstring
  forbids a hash this repo does not own from being load-bearing, and
  `mix64` is this repo's own. `APT-` is a fourth distinct prefix beside
  `PID-`, `MRN` and `ENC-`, so no two id spaces are ever visually
  confusable, and the zero-padded arrival ordinal leads for the same
  lexical-order reason the other three do.

  A RESCHEDULE KEEPS THIS ID rather than minting a second one and
  pointing back at the first (ADR-0174 section 2(b)): SCH-1/SCH-2 are
  stable placer/filler ids across the SIU family, and `:prior-value`/
  `:value` on ONE record is already this repo's shape for a change."
  [seed ordinal appointment-ordinal]
  (format "APT-%06d-%02d-%08x" ordinal appointment-ordinal
          (bit-and (mix64 (mix64 (mix64 seed ordinal) appointment-ordinal) 0x4150545F)
                   0xffffffff)))

(defn next-appointment-ordinal
  "The 0-indexed ordinal this patient's NEXT appointment takes: every
  appointment they have ever opened, counted -- `next-encounter-ordinal`
  applied to the appointment records.

  Monotone by construction for the same reason: NOTHING EVER LEAVES
  `:appointments`, so an ordinal cannot be handed out twice.

  THAT INVARIANT WAS FOUND, NOT ASSUMED, and the way it failed is worth
  the sentence. Appointments CAN OVERLAP -- a repeat arrival books at its
  own instant, which may fall while a previous encounter is still open,
  and a follow-up books at a discharge -- so `:appointment` (the open
  slot) can be displaced by a second booking. When a displaced record was
  simply dropped, `:appointments` did not grow, this function returned the
  SAME ordinal a second time, and one id ended up naming two appointments:
  `bin/demo-exerciser-ed-tuesday` reported
  `appointment-reaches-at-most-one-terminal` with `:terminals [:kept :kept
  :no-show]` on a single id. `evolve :appointment` now ARCHIVES the
  displaced record (outcome absent -- it never resolved) instead of
  discarding it, which is what makes this count monotone."
  [patient]
  (+ (count (:appointments patient)) (if (:appointment patient) 1 0)))

(defn minted-appointment-id-field
  "What an appointment's own `decide` merges in: `{:appointment-id ...}`
  for a run that opted into `:scheduling`, `{}` for one that did not.

  Deliberately a SEPARATE world key from `:encounter-minting` rather than
  a shared one: `:scheduling` and `:encounters` are independent opt-ins,
  and a run may take either without the other. The hand-built-world
  tolerance is on the KEY and never on a missing ordinal entry -- every
  patient `run` creates is in `:ordinals`, so a nil there is a defect and
  reads as one (`project_equivalence_proof_pattern`'s own rule, and the
  same shape `minted-encounter-id-field` uses)."
  [world patient-id]
  (if-let [{:keys [seed ordinals]} (:appointment-minting world)]
    {:appointment-id (appointment-id-for seed (get ordinals patient-id)
                                         (next-appointment-ordinal
                                          (get-in world [:patients patient-id])))}
    {}))

(defn minted-encounter-id-field
  "What an encounter OPENER's own `decide` merges in: `{:encounter-id
  ...}` for a run that opted into `:encounters`, `{}` for one that did
  not (ADR-0174's opt-in law -- absent means today's bytes, so the field
  is not merely nil, it is not there).

  `run` puts `:encounter-minting {:seed .. :ordinals ..}` into `world`
  IFF the run opted in, and the hand-built-world tolerance is on THAT
  KEY and never on a missing ordinal entry: every patient `run` creates
  is in `:ordinals`, so a nil there is a defect and reads as one rather
  than as a silently id-less encounter."
  [world patient-id]
  (if-let [{:keys [seed ordinals]} (:encounter-minting world)]
    {:encounter-id (encounter-id-for seed (get ordinals patient-id)
                                     (next-encounter-ordinal (get-in world [:patients patient-id])))}
    {}))

;; --- the stream partition (ADR-0171, arc 1) -------------------------------

(def stream-scheme
  "The RNG stream partition's own version marker (ADR-0171 ruling D1),
  stamped top-level into every sim manifest as `:stream-scheme`,
  sibling of `:event-schema-version`.

  It is a DISCRIMINATOR, not a warranty. sim/ADR-0009 decision 1 states
  seed stability as a WITHIN-version guarantee and decision 2 names
  `:generator {:version ...}` as the cross-version key; this marker adds
  nothing to either. What it buys is legibility: two corpora with the
  same seed, config and generator version cannot differ, while two with
  the same seed and config and DIFFERENT stream schemes are expected to,
  and the marker says so on the artifact's face instead of making a
  reader resolve a generator version against a changelog.

  \"1.0\" is the partition itself -- the first scheme there has ever been.
  Everything generated before it carries no `:stream-scheme` key at all,
  which is exactly how a pre-migration corpus is told apart from a
  post-migration one."
  "1.0")

(def ^:private stream-family-tag
  "Family -> its fixed tag long (ADR-0171 section 2(b)). A compile-time
  constant table, deliberately NOT `(hash keyword)`: a hash this repo
  does not own would put the derivation's stability in someone else's
  hands, against `rulings.md#R-no-derivation-through-nondeterminism`'s
  spirit and against `gmf.clj`'s own hash-order caution.

  The five families are the census's five scopes (ADR-0171 section 1):

  * `:patient`  -- this patient's own clinical trajectory. Keyed by
                   arrival ordinal, the same key `patient-id-for` uses.
  * `:person`   -- arc 2's demographic/life-arc layer. ZERO draw sites
                   today; declared now so arc 2 adds rows rather than a
                   family, and so `newborn-id-tag` below has a family to
                   name.
  * `:world`    -- arrivals, and every cross-patient decision: all four
                   `allocate` calls, `bed-ready-location`, the bed-swap
                   and merge partner picks. Run-scoped (id-tag 0), because
                   their DRAW COUNTS are conditional on the population and
                   no per-patient stream can own them without making one
                   patient's consumption depend on another's state.
  * `:facility` -- `materialize-providers`, `choose-attending`, and
                   `:outpatient-visit`'s uniform provider pick: draws that
                   read no patient state at all. Run-scoped, and distinct
                   from `:world` (ruling E1) so adding a ward or a provider
                   template does not shift arrival gaps or bed choices.
  * `:emission` -- rendering-time latency planning (`ehrt.sim.run`), which
                   never enters ground truth. Ruling C1: it used to be
                   `(java.util.Random. seed)`, the master seed VERBATIM, so
                   the latency stream replayed the engine's own first draws."
  {:patient  1
   :person   2
   :world    3
   :facility 4
   :emission 5
   ;; 6 is RESERVED for `:mutation` (ADR-0176 section 2(iii), ruling
   ;; Q4(a); tree-recorded here by ruling Q13(a), 2026-09-01, after the
   ;; spine and breadth sessions each carried it forward unwritten).
   ;; A reservation and not a row, deliberately: `ehrt sim mutate` draws
   ;; from its OWN seed and touches no run stream, so a row today would
   ;; name a family with no stream behind it. The NUMBER is what needs
   ;; holding -- so a later session that does want a run-seed-derived
   ;; mutation stream adds row 6 rather than re-keying this table and
   ;; reshuffling every existing stream, the same reason `:person` was
   ;; declared with zero draw sites (ADR-0171).
   })

(defn stream-seed
  "The seed of one stream: `(mix64 (mix64 master family-tag) id-tag)`
  (ADR-0171 section 2(b), ruling A1). `id-tag` is the patient's arrival
  ordinal for `:patient`, and 0 for the run-scoped families.

  Collisions are cosmetic at this project's scale and are not engineered
  around: two patients sharing a stream seed share a draw sequence, which
  is a DUPLICATE trajectory, not a corrupt one, and at 10^6 ids over a
  64-bit mixed space the expected number of colliding pairs is ~2.7e-8.

  Order-free by construction, which is the whole point: a stream is keyed
  by a STABLE id, never by how many streams were built before it (the
  reason ADR-0171 rejected `SplittableRandom`'s split order)."
  ^long [^long master family ^long id-tag]
  (let [tag (get stream-family-tag family)]
    (when (nil? tag)
      (throw (ex-info "unknown RNG stream family"
                      {:family family :known (set (keys stream-family-tag))})))
    (mix64 (mix64 master (long tag)) id-tag)))

(defn stream
  "A fresh `java.util.Random` for one stream -- `stream-seed`'s value,
  handed to the one constructor the engine has ever used."
  ;; Deliberately UNHINTED, unlike `stream-seed` above: primitive-long
  ;; parameter hints compile callers to an IFn$LOLO call site, which a
  ;; plain `with-redefs` replacement cannot satisfy -- and the locality
  ;; test's whole mechanism is redefining the var `run` calls, which
  ;; since the extraction is `ehrt.sim-engine.engine/stream`, this
  ;; function's delegating def, not this one. `run` calls it a handful
  ;; of times per run (twice, plus once per patient), so there is no
  ;; arithmetic here worth hinting either way.
  [master family id-tag]
  (Random. (stream-seed (long master) family (long id-tag))))

(defn newborn-id-tag
  "The `:person`-family id-tag for a newborn (ADR-0171 section 2(c),
  ruling B1): a birth's stream is derived from the PARENT's stable id and
  a birth ordinal, never from a global counter, so a birth occurring
  anywhere in the run perturbs no other person's stream.

  The ordinal is the PAIR `(parity-index, within-delivery-index)`, mixed
  in that order, with `within-delivery-index` pinned at 0 for as long as
  multiples are a named v1 limitation. Ruling B1 took the pair from the
  start deliberately: admitting twins later would otherwise have to widen
  a bare parity index, renumbering every existing singleton's stream and
  costing a full newborn-stream reshuffle.

  NO CALLER TODAY. Arc 2 owns the newborn path; this function exists now
  so arc 2 inherits the key rather than choosing it, and its only gate is
  `engine-test/the-stream-partition-derives-what-adr-0171-specifies`."
  ^long [^long parent-id-tag ^long parity-index ^long within-delivery-index]
  (mix64 (mix64 parent-id-tag parity-index) within-delivery-index))

(defn one-stream
  "Every family bound to ONE `Random` -- the degenerate stream map a
  caller with no `run` behind it needs (a single `decide` call in a
  test). Collapsing the families is EXACTLY the pre-partition behaviour,
  so a lone `decide` call's draw order is unchanged by ADR-0171; what
  moved is which stream `run` hands each family, and `run` builds that
  map itself."
  [^Random rng]
  {:patient rng :person rng :world rng :facility rng :emission rng})

