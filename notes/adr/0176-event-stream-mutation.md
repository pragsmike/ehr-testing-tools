## ADR-0176 — event-stream mutation: the operator catalog, the injection contract, and the closed oracle loop

**Status:** Proposed (design session, 2026-09-01, HEAD `f402868`). NINE
LETTERED QUESTIONS AWAIT A RULING; nothing here is built, and this
session touched no `components/*/src` file. The roadmap row this record
serves is `roadmap.md#event-stream-mutation`, author-ruled 2026-08-29,
which names a design ADR as the first thing it wants and names its own
predecessor — `roadmap.md#engine-namespace-extraction-and-apply-unification`,
CLOSED 2026-09-01 — as the thing that had to land first.

**One recommendation departs from the channel's stated expectation.**
The row's prompt anticipated a post-decide, pre-apply transform on the
ground-truth log. Section 2(ii) recommends against it, on four
independent readings of the tree, and section 3 letters the choice so
the author rules on the evidence rather than on this paragraph.

### 1. The mutation surface as it stands, probed rather than recalled

Every line number below is at `f402868`, read this session.

**(a) The file-level operator catalog — what exists, and what it is
for.** `components/corpus/src/ehrt/corpus/operators.clj` holds ten
registered operators behind one Malli-validated `Operator` schema
(`operators.clj:54-79`): `:id`, `:version`, `:format`, an optional
`:doc`, a `:contract` of `{:type :violates|:preserves :target
"<sentence>"}`, `:locator-required?`, an optional `:default-locator`,
and `:fn`. Five are `:format :fhir` — `:remove-required-element`
(`:122`), `:duplicate-element` (`:134`), `:invalid-code-value` (`:148`),
`:malformed-date` (`:160`), `:wrong-type-value` (`:180`). Five are
`:format :v2` — `:blank-required-field` (`:203`),
`:corrupt-encoding-characters` (`:225`), `:malformed-datetime-value`
(`:242`), `:truncate-segment-fields` (`:256`), `:corrupt-segment-name`
(`:271`). The registry itself is a `defonce` atom keyed by `[id
version]` with `register!`/`lookup`/`entries`/`registry-snapshot`/
`reset-registry!` (`:81-109`).

**Every one of the ten is a LOWERING-LAYER fault, and the catalog says
so in its own words.** A blanked MSH-9, a three-character MSH-2, a
truncated segment, a `"2026-13-45"` date: none of these has an
event-level cause, because none of them is expressible in an event log
at all. They come into existence when the log is lowered to bytes.
`operators.clj:24-39` records the discipline that produced them — only
defects `judge-v2-hapi`'s base-structural tier was empirically
*witnessed* convicting were registered, and three plausible candidates
(dropping PID entirely, corrupting PID's own segment name, blanking
PID-7) were probed, found to produce `:pass`, and **recorded as
dropped rather than shipped unconvictable**. That precedent is
load-bearing for question Q6 below.

**(b) The mutate capability.** `components/corpus/src/ehrt/corpus/
mutate.clj` is 105 lines. `mutate` (`:87-105`) takes `(base-data
operator locator-envelope)` and dispatches on `(:format operator)` to
`mutate-fhir` (`:49`) or `mutate-v2` (`:67`); each validates that the
locator's path parses and RESOLVES before calling `(:fn operator)`, and
returns `kernel/ok {:mutant :lineage}` or a `kernel/rejected`. **Its own
docstring already anticipates this row**: *"adding a third format is a
new private `mutate-<format>` plus one `case` branch, not a structural
change here"* (`mutate.clj:10-12`). That sentence is the ground under
question Q2.

**(c) The lineage record.** `corpus/lineage.clj:40-48` — `build` takes
`{:parent :stage :transformation :produced}` and adds a self-verifying
`:id` (`valid-content-hash?`, `:50-55`). `mutate.clj:39-47` fills
`:stage :mutate` and `:transformation {:operator {:id :version}
:locator <envelope> :contract <the operator's contract>}`. **There is no
seed slot**, because no file-level operator draws.

**(d) `check`'s finding vocabulary.** `components/sim-check/src/ehrt/
sim_check/check.clj`. Each invariant is a named function `(ground-truth)
-> seq of violation maps`, empty meaning it holds (`check.clj:8-9`). A
violation is `{:invariant <keyword> :patient-id <id> :at <t or [t t]>}`
or a small variant naming `:event` instead of `:patient-id`
(`:119`, `:130`, `:141`, `:174`). `check-all` (`:1896-1923`) runs four
catalogs — `catalog` (40 vars, `:1807-1877`), `facility-catalog` (2,
`:1879-1884`), `warmup-catalog` (1, `:1886-1889`), and
`order-profiles-catalog` (1, `:1891-1894`) — **forty-four invariants in
total**, and returns `result/ok {:invariants-checked :events}` or
`result/rejected :invariant-violation {:violations [...]}`. The finding
vocabulary is therefore already exactly what an expected-finding
declaration needs: a closed set of invariant names.

**(e) ADR-0166's test-side event mutations, the thing being promoted.**
`components/sim-check/test/ehrt/sim_check/check_test.clj:452-526`, nine
scripted logs. Five of the nine are DEFECT SHAPES and are the seed of
this row's catalog:

| shape | the mutation | line |
|---|---|---|
| phantom index | `:start-event-id 99` into a log that has no index 99 | `:452-455` |
| nil reference | `:start-event-id nil`, citation intact, no pre-horizon fact to excuse it — ADR-0163's own silent shape | `:457-463` |
| cross-patient | a well-formed index naming a `:care-plan-start` belonging to a DIFFERENT patient — ADR-0164's hazard | `:465-472` |
| inverted span | the start's `:t` after its own end's | `:474-477` |
| wrong kind | an index resolving to a real `:medication-order` where a `:care-plan-start` was meant | `:479-483` |

The other four are ACCEPTANCE cases (`:485-526`): the sound log, the
designed pre-horizon straddle, an unrelated pre-horizon fact not making
the checker permissive, and a `:medication-order` pre-horizon fact not
excusing a care-plan end. **They are hand-scripted logs, not mutations
of a real one**, and that is precisely the gap this row closes: the
shapes exist as fixtures for one invariant and cannot be pointed at any
corpus the repository actually ships.

**(f) The referential fields the schema has.** `components/sim-engine/
src/ehrt/sim_engine/event_schema.clj`. Seven fields name something else,
and they are NOT one kind:

* **Four are log indices** — `:cancels-event-id` (`:716`, `:723`,
  `:735`, plain `:int`), `:order-event-id` (`:857` `:int`, `:944`
  `[:maybe :int]`), `:start-event-id` (`:972`, `[:maybe :int]`),
  `:placeholder-event-id` (`:1037`, `[:maybe :int]`, and its own comment
  at `:1026-1031` says *"`:placeholder-event-id` IS a log index -- the
  one ... `:medication-end`'s `:order-event-id` already has"*).
* **Two are minted opaque ids** — `:encounter-id`, `:appointment-id`.
* **One is a STAMP, not a reference** — `:person-event-id` (`:669`,
  `:836`, `:1022`, `:1049`, `:string`), and the catalog says so by name:
  `person-scoped-provenance-is-a-stamp-not-a-reference`
  (`check.clj:1860`).

The `[:maybe :int]` / `:int` split matters: nulling a `[:maybe :int]`
leaves a schema-valid log, and nulling `:cancels-event-id` does not.
That is question Q9.

**(g) The injection point the predecessor row built, and what it
actually sees.** `fold/apply-events` (`components/sim-engine/src/ehrt/
sim_engine/fold.clj:439-475` docstring, body to `:607`) is the one fold
all three apply sites run: `run.clj:1350`, `log_index.clj:302`, and
`fold.clj:615`. In the run loop, `decide/decide` returns `{:keys [events
advance exhausted schedule-followup prepend-steps]}` at
`run.clj:1301-1303`, and `events` reaches `apply-events` at
`run.clj:1350` — **forty-seven lines later, with nothing between them
but a comment block.** That is the seam the channel had in mind, and it
is real.

Four things about it are also real:

1. **It sees ONE decide's batch, never the log.** `apply-events`' first
   argument is an accumulator and its second is `events`, the batch.
   Cross-patient repointing, phantom indices into the whole log, and
   span inversion all need the log, and four of ADR-0166's five shapes
   are therefore inexpressible at this seam.
2. **A mutation there is folded into the world.** `apply-events` writes
   `:world`, `:log`, `:state-history` and `:entries`
   (`fold.clj:447-453`). A mutated event does not merely land in the log
   wrong; it changes patient state, the three indexes, bed occupancy,
   and therefore every later `decide`.
3. **`:log-mirror` publishes the log back INTO the world for `decide`
   to read.** *"`:log-mirror` needs no slot of its own -- it publishes
   into `(:world acc')` under `:ground-truth`, which is where a mid-run
   `decide` reads the log back from"* (`fold.clj:455-457`). There is
   consequently **no seam here that mutates the log without mutating
   what `decide` sees** — the pre-apply and post-apply variants collapse
   into the same thing unless the two views are deliberately forked.
4. **The engine REPAIRS some injected faults.** `run.clj:1303-1317`:
   *"A `:rejected` decide outcome ... is NOT a run-halting condition ...
   it means THIS one step doesn't happen"*. Delete an `:admission` at
   this seam and the later `:discharge` decide simply declines to fire.
   The injected defect vanishes and the log is clean — a silent
   no-op, which is the worst possible outcome for a fault injector.

**(h) The census's declared permanent omission, §3e.**
`.agents/plans/apply-unification-census.md:263-329`. `2 x
:warm-up-mark` is the one cell of thirty-nine the arc does not claim,
because *"a LOG DOES NOT CARRY A WARM-UP WINDOW; only a run
configuration does"*, and a declared 0 was MEASURED to destroy the
log's own authoritative marks (2 of 9 entries, first differing byte
425). Two further findings sit there and bear on this row directly:
`:log-mirror` **reverses** on a world with no `:ground-truth` (it is
`(into nil events)`, a list in reverse order) unless a consumer seeds
`:ground-truth []` first; and only the two transient accumulators need a
slot. The §3e omission is why this record does not propose reaching
`apply-events` for a `:warm-up`-shaped fault: **the log is the
authority, and a stage that re-derives from configuration destroys what
the log already carries.** The same argument runs the whole length of
section 2(ii).

**(i) The consumer-facing statement of the hole.**
`docs/consuming-ground-truth.md:561-588`, "Fault injection, as it stands
today" — a three-row table (Event / `:churn-profile`, File / `ehrt
corpus mutate`, Transport / `:latency` + `ehrt corpus batch`), the
taxonomy sentence, and then *"There is no event-level mutation operator
catalog today"* with a pointer to this row. It also names the supported
workaround: mutate the vector yourself between `--format ground-truth`
and your consumer, with `ehrt sim check` as the oracle. **The design
below is that workaround, made a shipped stage.**

**(j) The standing rules this record must not break.**

* `rulings.md#R-transport-realism-vs-mutation` (ADR-0111) — transport
  realism simulates CORRECT behaviour deterministically; mutation
  injects INCORRECT content with an expected finding. **Message loss
  and duplication are recorded there as sitting on the boundary, a
  named unresolved taxonomy question** (`notes/adr/0111-*.md:164-173`,
  `:348-362`). Section 5 says what the docs rider does with it.
* `rulings.md#R-mllp-abandoned` (ADR-0102) — no transport work follows
  without a fresh ruling.
* `rulings.md#R-corpus-tools-foreign-corpora` (ADR-0111) — a
  corpus-level tool works on any directory of valid messages, foreign
  corpora included, never through sim-specific machinery.
* The opt-in-key law — `engine/config-keys`
  (`components/sim-engine/src/ehrt/sim_engine/config.clj:45-56ff`),
  under which **ABSENT ENTIRELY — not false, not nil — is the
  byte-identical path**, stated per key for `:persons`, `:encounters`,
  `:bed-cycle` and `:scheduling`, and stated as the arc-wide landing
  shape by ADR-0175 ruling E1.
* `rulings.md#R-measure-claimed-population` — a measurement claiming to
  characterize the simulator's output draws from the real seeded,
  threaded path.

### 2. The design

#### (i) The operator catalog

**An event operator is a pure function `(events, seed) -> events'`
carrying a NAMED DEFECT CLASS.** The defect class is not prose: it is
the set of `check` invariant names the operator is built to trip, drawn
from the closed vocabulary section 1(d) enumerates. Registration
therefore states, and the registry validates, three things the
file-level `Operator` schema does not yet carry:

* `:format :event` — the third value of the existing discriminator.
* `:expected-findings` — a non-empty set of invariant name keywords.
  This is the event analogue of `:contract/:target`, and it is
  MACHINE-CHECKABLE where the file-level target sentence is not, because
  `check`'s vocabulary is this repository's own.
* `:seed-consuming? true` and `:locator-required? false` — an event
  operator selects its own site by draw rather than being handed one.
  (Whether a locator SHOULD be accepted as an override is deliberately
  not proposed; it is a want, not a v1 need, and adding it later is
  additive.)

**The v1 family is DERIVED, not hand-listed.** Section 1(f) gives four
log-index reference fields; section 1(e) gives five defect shapes. The
cross product is the referential family, one operator per cell, each
naming the invariant that convicts it:

| reference field | invariant that convicts | catalog line |
|---|---|---|
| `:cancels-event-id` | `cancel-references-existing-uncancelled-event` | `check.clj:1826` |
| `:order-event-id` | `medication-end-references-existing-order-and-follows-it-in-time` (`:1838`), `result-references-existing-order-and-follows-it-in-time` (`:1832`) | two, by carrier kind |
| `:start-event-id` | `care-plan-end-references-existing-start-and-follows-it-in-time` | `check.clj:1843` |
| `:placeholder-event-id` | `identity-fill-references-its-placeholder-registration` | `check.clj:1850` |

Deriving rather than listing is the point of ADR-0166's own error
ledger: `:medication-end` got a referential invariant and its
structural twin `:care-plan-end` did not, and *"the asymmetry sat
unnoticed from 2026-08-02 to 2026-08-23"*. A hand-listed operator
catalog reproduces exactly that failure mode one layer up. A derived one
turns red the moment a fifth reference field is added without an
operator for it.

**Three structural operators sit beside the referential family**, each
convicting an invariant no referential shape reaches:

* `drop-event` — remove one event. Convicts
  `discharge-follows-admission` (`check.clj:1811`) when the dropped
  event is the admission.
* `clock-skew` — move one event's `:t` behind its predecessor's.
  Convicts `timestamps-monotone` (`check.clj:1810`).
* `orphan-participant` — replace a participant id with one the run never
  registered. Convicts `participant-ids-exist-in-run`
  (`check.clj:1813`).

**`:person-event-id` gets no referential operator**, because the catalog
itself rules it a stamp and not a reference
(`person-scoped-provenance-is-a-stamp-not-a-reference`,
`check.clj:1860`). Recorded here so a later reader does not read its
absence as an oversight.

#### (ii) The injection contract — and why it is NOT at `apply-events`

**RECOMMENDED: the mutation is a POST-RUN, WHOLE-LOG STAGE, outside
`engine/run` entirely.** `(events, seed) -> events'`, taking the
complete log `run` returned and handing back a mutant. The engine does
not know it exists; `engine/config-keys` gains nothing; the oracle is
byte-identical by construction rather than by proof, which is the
`:latency` precedent ADR-0175 §4 names verbatim.

**This departs from the channel's stated expectation, and the four
reasons are all in section 1(g).**

1. **The seam cannot express the shapes.** `apply-events` receives one
   decide's batch. Four of ADR-0166's five defect shapes need the log.
2. **A mutation there is not a content fault; it is a WORLD fault.**
   `apply-events` folds into `:world`, and `:log-mirror` publishes that
   world's `:ground-truth` back to `decide` (`fold.clj:455-457`). The
   fault propagates into every later decision. `check` then reports a
   cascade, and the oracle loop's "class X and NOTHING else" cannot
   close — not because the design is imprecise, but because the run
   really did diverge.
3. **The engine repairs some of them silently** (`run.clj:1303-1317`),
   which is a fault injector reporting success while injecting nothing.
4. **The doctrine already says which layer this is.**
   `R-transport-realism-vs-mutation`: churn simulates CORRECT behaviour
   the world really produced; mutation injects INCORRECT CONTENT. A
   content fault means **the record is wrong and the world was right** —
   so the world must not be re-derived from the mutant. Injecting at
   `apply-events` is a request for a wrong WORLD, and this repository
   already ships that: it is `:churn-profile`.

**"All emitters and all three apply sites see one mutated truth" is
satisfied, and more cleanly.** Every emitter takes a log as input
(`sim-emit-hl7/emit.clj:77`, `emit-wire` at `:111`; `sim-emit-fhir`'s
own state-based renderer folds the same log), so an emitter handed the
mutant inherits the mutation with no emitter change at all — which is
the whole reason this row exists. All three apply sites likewise read
whatever log they are handed. There is exactly ONE mutated log because
the stage produces exactly one, and nothing downstream can see an
unmutated one.

**One consumer note carried forward from census §3e**: a caller folding
a mutant through `replay` or `reinstated-state` must seed
`:ground-truth []`, or `:log-mirror` builds a REVERSED list. That is a
pre-existing property of those sites, not of this design, and it is
named here because a mutation stage is exactly the new consumer that
will meet it first.

#### (iii) Lineage

**A mutated corpus records its parent run, its operator, and its seed.**
`lineage/build`'s existing shape carries all of it with one addition:

```clojure
{:parent   "<sha256 of the clean log's canonical serialization>"
 :stage    :mutate
 :transformation {:operator {:id :dangling-start-event-id :version "1"}
                  :seed 424242
                  :contract {:type :violates
                             :target "<the sentence>"}
                  :expected-findings
                    #{:care-plan-end-references-existing-start-and-follows-it-in-time}}
 :produced "<sha256 of the mutant>"
 :id       "<self-verifying content hash of the above>"}
```

`:seed` is the new slot; everything else is `mutate.clj:39-47`'s
existing record with `:locator` replaced by `:seed`. The mutant's
MANIFEST additionally carries the parent run's own identity (seed,
config, `:stream-scheme`, `:event-schema-version`), so a mutant is
reproducible from `(parent-run-identity, operator, seed)` and nothing
else.

**Draw consumption, and the opt-in-key law.** The stage is outside
`run`, so the law is satisfied trivially and in its strongest form:
**there is no key to be absent.** `engine/config-keys` is untouched, no
draw is taken on any of the five existing RNG families
(`streams.clj:233-267`), and every corpus this repository ships is
byte-identical whether or not the stage exists. The operator's own draws
come from its own seed.

**One draw, one site.** An operator application mutates EXACTLY ONE
site, chosen by one draw over the candidate sites the log offers. That
is the file-level operators' own shape — one locator, one edit — and it
is what keeps a defect class unambiguous and a lineage record exact.
Multiplicity comes from applying an operator N times with N seeds, never
from one application mutating N sites.

**`:mutation` is reserved as RNG family tag 6** in
`streams.clj:266-271`, unused, for the same reason ADR-0171 declared
`:person` with zero draw sites: so a later session that DOES want a
run-seed-derived mutation stream adds rows rather than re-keying the
table and reshuffling every existing stream.

#### (iv) The oracle loop — stated as the acceptance test

**Inject class X, expect finding class X and nothing else.** Written out
as the gate, for one operator `op`, one clean log `L`, one seed `s`:

```
1. (check/check-all L)            => :ok                      ; the parent is clean
2. (op L s)                       => L'                       ; total, pure, one site
3. (m/validate EventLog L')       => true                     ; still schema-valid  [Q9]
4. (check/check-all L')           => :rejected :invariant-violation
5. (set (map :invariant (:violations ...))) = (:expected-findings op)   ; EQUALITY  [Q5]
6. (op L s) = (op L s)                                        ; determinism
7. (not= L' L)                                                ; it actually did something
```

**Step 1 is not ceremony.** It is what makes step 5 a statement about
the operator rather than about the corpus, and it is the step ADR-0166's
own red-2A/red-2B discipline would have demanded.

**Step 5 is EQUALITY, not containment.** A subset check would let a
cascade hide behind a declared finding, which is the exact failure this
design's whole injection-contract argument exists to avoid. Some
operators legitimately trip two invariants — inverting a span's `:t`
trips both `timestamps-monotone` and the span's own referential
invariant — so the declaration is a SET and the gate is set equality.

**Step 7 is the ADR-0165 lesson.** A generator-side coverage meter
existed because the arc's own fix had removed the only exercise of both
end types. An operator that silently mutates nothing on the corpus it is
run against is the same silence, and step 7 is where it turns red.

**The population.** The gate runs the whole catalog against a fixed set
of clean logs — the natural population is the gated corpora
`bin/ground-truth-bracket` already digests, so an operator that finds no
candidate site in ANY of them is reported rather than tolerated
(`rulings.md#R-empty-population-is-red`, and the population-closure law,
`rulings.md#R-population-closure`).

#### (v) Explicitly out of scope

* **Lowering-layer faults stay with the file-level operators.** All ten
  of section 1(a) remain exactly where they are. A malformed MSH-2, a
  truncated segment, a wrong-typed JSON value: none has an event-level
  cause, and pushing them down to the event layer would produce nothing.
  The two catalogs are not competing; they are two layers, and this
  record adds the upper one.
* **Foreign-corpus mutation is NOT proposed here.** An event log this
  simulator did not produce has no `check` oracle behind it — the
  catalog is a statement about THIS engine's own internal consistency.
  Mutating a foreign corpus needs a different oracle entirely, and it
  goes to `docs/future-features.md` (section 5) as a consumer-facing
  want, not into this catalog.
* **Stream-layer and transport-layer faults are NOT proposed here.**
  Drop, duplicate, reorder and delay over an EMITTED SEQUENCE, and MLLP
  framing corruption, are different layers with different oracles. Both
  go to the rider. `R-mllp-abandoned` binds the transport one: it needs
  a fresh ruling before any work, and the rider says so.
* **No `components/*/src` change lands with this ADR**, no event kind is
  proposed, no field is added to any existing kind, and
  `engine/config-keys` is not touched.

### 3. The lettered questions

Nine. Each states the options, the recommendation, and why the declined
ones are declined. **Q1 is the one that matters most**, because it is
the one where the recommendation departs from what the row's prompt
expected.

---

**Q1 — Where the mutation is applied.**

* **(a) RECOMMENDED — a POST-RUN, WHOLE-LOG stage, outside
  `engine/run`.** `(events, seed) -> events'` over the complete log.
  Every argument is in section 2(ii): the seam cannot express four of
  the five shapes; a pre-apply mutation is a world fault, not a content
  fault; the engine repairs some injections silently; and the doctrine
  already assigns wrong-world to `:churn-profile`. Byte-identity is
  free rather than proven.
* **(b) Post-decide, pre-apply, at `run.clj:1350` — the channel's
  stated expectation.** Declined on the four readings above. Its one
  real advantage is that the fault becomes part of the simulated world's
  history, which is a coherent thing to want — but it is a different
  want, and `:churn-profile` is where it already lives.
* **(c) Post-decide, POST-apply — mutate only what lands in `:log`,
  leaving the fold clean.** This is the most tempting middle, and the
  tree closes it: `:log-mirror` publishes the world's `:ground-truth`
  for mid-run `decide` to read (`fold.clj:455-457`), so keeping the
  world clean means deliberately forking the log into two disagreeing
  views inside the choke point the predecessor row just spent three
  sessions unifying. Declined on that ground specifically.

---

**Q2 — Where the catalog lives.**

* **(a) RECOMMENDED — join the existing registry as `:format :event`,
  with a third `case` branch in `corpus.mutate`.** `mutate.clj:10-12`
  literally specifies this as the extension shape. One registry means
  one `docs/operators.md` (generated from `entries`), one `Operator`
  schema, one `register!` validation path, and one place a consumer
  looks. `components/corpus` may depend on `components/sim`
  (AGENTS.md Constraints), so a corpus → sim-engine edge for the event
  schema is dependency-legal and adds no cycle.
* **(b) A new component, `components/sim-mutate`.** Cleaner layering on
  paper — the stage is sim-shaped, not corpus-shaped — but it forks the
  operator registry in two, and `docs/operators.md`'s generator would
  have to learn about both. The fat-component disclosure does not apply
  here: `corpus`'s interface is ADR-0018's designed-from-consumers
  exception, so adding to it is design intent, not drift.
* **(c) Inside `components/sim-check`, beside the catalog it inverts.**
  Appealing symmetry, and rejected on dependency grounds:
  `sim-check` must never depend on `corpus` (AGENTS.md Constraints), so
  this fork is permanent, and the mutate stage would then be unable to
  reuse `lineage`.

---

**Q3 — How many sites one operator application mutates.**

* **(a) RECOMMENDED — exactly ONE, chosen by one draw.** Keeps the
  defect class unambiguous, keeps step 5's set equality achievable,
  keeps lineage exact, and matches the file-level operators' own
  one-locator-one-edit shape.
* **(b) A `--rate` or `--count` parameter.** Realistic for a soak test,
  and it makes step 5 a statement about a population rather than about
  an injection. Additive later; not v1.

---

**Q4 — The seed and the RNG family.**

* **(a) RECOMMENDED — the operator carries its OWN seed, independent of
  the run's master seed; `:mutation` is RESERVED as family tag 6 and
  left unused.** The stage must work on any log, including one whose
  master seed the caller does not have. Reserving the tag costs nothing
  and follows ADR-0171's own `:person` precedent.
* **(b) Derive from the run's master seed via a live sixth family.**
  Makes a mutant a pure function of the run seed alone, which is
  genuinely attractive for reproducibility — and it makes the stage
  sim-only, which forecloses the foreign-corpus growth path in section
  2(v) before it is ruled on.

---

**Q5 — What "and nothing else" means.**

* **(a) RECOMMENDED — each operator declares an expected finding SET;
  the gate asserts observed = declared, exactly.** Accommodates the
  operators that legitimately trip two.
* **(b) Singleton only; an operator tripping two is refused
  registration.** Sharper, and it would refuse `inverted-span`, which is
  one of the five shapes being promoted.
* **(c) Subset — observed ⊇ declared.** Declined outright: it is the
  one reading under which a cascade passes the gate.

---

**Q6 — An operator the catalog cannot convict.**

* **(a) RECOMMENDED — refuse registration, and record the candidate as
  a CATALOG GAP with its own roadmap row.** The v2 catalog's precedent
  (`operators.clj:24-39`) is *"recorded as dropped, not shipped
  unconvictable"*, and this is that rule with one difference that
  matters: HAPI is a third party this repository cannot extend, while
  `check`'s catalog is its OWN. An unconvictable event operator is
  therefore evidence of a hole in the catalog, and ADR-0166 is the
  standing proof that such holes sit unnoticed for weeks. The finding
  belongs against `check`, not in a shipped operator.
* **(b) Register it with an empty expected-finding set, as a named
  blind spot gated to fail the moment the catalog grows to cover it.**
  Keeps the knowledge in executable form rather than in prose — a real
  advantage — but ships a "mutation operator" that provably detects
  nothing, which is exactly the overclaim the v2 discipline exists to
  prevent.
* **(c) Drop it silently.** No. Named only to be refused.

---

**Q7 — The CLI surface.**

* **(a) RECOMMENDED — a new `ehrt sim mutate` verb, a stdin → stdout
  filter.** It slots into the idiom the help text already teaches:
  `ehrt sim run --format ground-truth | ehrt sim check`
  (`help.clj:225`) becomes `ehrt sim run --format ground-truth | ehrt
  sim mutate --operator-id X --seed N | ehrt sim check`. That pipe IS
  the oracle loop, typeable by a consumer.
* **(b) Extend `ehrt corpus mutate` with a `--format ground-truth`
  branch.** Keeps one verb for one concept, and it fights the existing
  verb's whole shape: `corpus mutate` is directory-and-locator driven
  (`cli/core.clj:648-678`), and an event mutation has no locator and no
  directory.
* **(c) A flag on `ehrt sim run`.** Declined: it would put mutation
  inside the run, which is Q1(b) arriving through the CLI door.

**Q2 and Q7 interact but do not constrain each other.** `bases/cli`
composes every component, so a catalog living in `components/corpus`
under Q2(a) is reachable from an `ehrt sim mutate` verb under Q7(a)
without a new edge. The group name is a UX choice; the home is a
dependency choice.

---

**Q8 — The v1 catalog's membership.**

* **(a) RECOMMENDED — the DERIVED referential family (4 reference
  fields × 5 shapes, minus the cells the schema forbids), plus the
  three structural operators of section 2(i).** Derivation is what
  makes a fifth reference field turn the gate red instead of silently
  going uncovered — ADR-0166's error ledger applied one layer up.
* **(b) A hand-picked list spanning the catalog's variety.** Faster to
  land, and it reproduces exactly the asymmetry ADR-0166 spent a session
  closing.
* **(c) One operator per invariant, all forty-four.** The honest
  end state and not a v1: a dozen of the forty-four are vacuous on logs
  that do not opt in (`consuming-ground-truth.md:398` counts seven), and
  several would need a config the log does not carry.

---

**Q9 — Whether a mutant stays schema-valid.**

* **(a) RECOMMENDED for v1 — every event operator produces a
  SCHEMA-VALID log.** `check` is the declared oracle; a schema-invalid
  log is convicted by the Malli schema, not by the catalog, so the loop
  would close on the wrong instrument and the operator's
  `:expected-findings` would name nothing that fires. Concretely, this
  excludes nulling `:cancels-event-id` (plain `:int`,
  `event_schema.clj:716`) while permitting the same shape on
  `:start-event-id` / `:order-event-id` / `:placeholder-event-id` (all
  `[:maybe :int]`) — the matrix has holes, and they are principled.
* **(b) Permit schema-invalid mutants, with `:expected-findings` able to
  name a schema clause instead of an invariant.** The honest end state,
  and it needs a second oracle and a second finding vocabulary; named as
  the growth path rather than declined.
* **(c) Two sub-catalogs, split by convicting instrument.** The same
  work as (b) with a taxonomy on top; premature before (b) is wanted.

### 4. Consequences

**Nothing moves until the ruling.** No corpus, no digest, no gate. When
the implementing session runs, `bin/ground-truth-bracket` must report
IDENTICAL on every root at every commit of that arc, and it will do so
by construction rather than by proof under Q1(a): the stage is outside
`run`, so there is no path by which a shipped corpus can move.

**`docs/operators.md` grows a third format section**, generated from the
registry as the two existing sections already are. Under Q2(a) that is a
renderer question, not a new document.

**`docs/consuming-ground-truth.md:561-588` becomes stale on the
implementing commit**, in both of its halves: the three-row table gains
a fourth row, and the paragraph beginning *"There is no event-level
mutation operator catalog today"* is retired outright. Named here so
that session does not have to find it.

**ADR-0111's named taxonomy question moves closer to an answer without
this record ruling on it.** The rider (section 5) states the
layer-boundary semantic — a duplicate EVENT means the world had two
occurrences and a receiver must keep both; a duplicate MESSAGE means the
world had one and a receiver must dedupe — which is the shape a
resolution would take. It is offered as a design stance in a
future-features menu, not as a ruling on ADR-0111's open question.

**What this record deliberately does not do.** It proposes no event
kind, no field on any existing kind, no `engine/config-keys` entry, no
change to any `components/*/src` file, and no operator implementation.
It lifts no limitations row. It does not resolve ADR-0111's taxonomy
question, and it does not touch `R-mllp-abandoned`.

### 5. The docs rider that lands with this record

`docs/future-features.md`, consumer-voiced, framed as a torture kit for
HL7-handling systems across three fault layers — CONTENT (bytes wrong
inside a message), STREAM (sequence wrong, messages intact), TRANSPORT
(framing and connection). One design stance per entry, no sizing, no
internal register language. It is linked from one line in `README.md`
and one sentence in `docs/consuming-ground-truth.md`'s own exclusions.

**Its load-bearing sentence is the layer boundary**, and it is what
makes the kit coherent rather than a list: **a duplicate EVENT means the
world had two occurrences and the receiver must keep both; a duplicate
MESSAGE means the world had one and the receiver must dedupe.** Same
observable bytes, opposite correct behaviour — which is precisely why
the layers cannot be collapsed into one injector, and why this record's
catalog stops at the content layer.

### 6. Dated addendum, 2026-09-01 — three claims corrected by measurement

Entered by event-stream mutation implementation 2 (the breadth session),
under author ruling **Q10(a)**: *ship operators ONLY where a real
population exists in a generatable log; columns without one are recorded
as POPULATION GAPS — a kind distinct from Q6's catalog gaps, convictable
in principle but unwitnessable today — and the population work is rowed
as its own priced item, not begun in that session.* ADRs append and
never rewrite, so sections 1–5 above stand as written; what follows says
where the tree refuted them. The evidence is
`.agents/plans/2026-09-01-event-mutation-population-ledger.md`.

**(a) Section 2(iv)'s declared population is EMPTY.** That section names
`bin/ground-truth-bracket`'s gated corpora as "the natural population"
for the catalog-wide gate. Measured first by the spine session (its
record's finding F1) and confirmed here: every engine-layer oracle root
runs a `module-only` pathway and carries ZERO carriers of all four
log-index reference fields, because those fields are minted by the full
sim path — scheduling, identification, medication spans — which those
roots never exercise.

**The population is the opt-in demo configs**, derived by grep rather
than recalled: `demos/scenarios/clinic-decade/config.edn` and
`demos/scenarios/ed-tuesday/config.edn` are the only two distinct logs
in the tree carrying candidate sites at all (`config-latency.edn` is
byte-identical to `ed-tuesday`'s, `:latency` being emission config that
reaches no member of `engine/config-keys`). Measured at each config's
own documented invocation, they carry sites in **two of five carrier
columns**; the other three are empty in both and are rowed as population
gaps.

**(b) Section 2(i)'s matrix arithmetic UNDER-COUNTS.** The text says
"four reference fields × five defect shapes"; the table in that same
section already splits `:order-event-id` into "two, by carrier kind",
and the split is real — `:result-available` carries it as a plain `:int`
convicted by `result-references-existing-order-and-follows-it-in-time`,
`:medication-end` as `[:maybe :int]` convicted by
`medication-end-references-existing-order-and-follows-it-in-time`. The
matrix is therefore **5 carrier columns × 5 shapes = 25 cells, minus the
2 that Q9(a) forbids = 23**, not 20; the spine record's forward price of
"19 referential operators remaining" reads 22.

**(c) Section 2(i)'s three structural operators are each given ONE
convicting invariant, and all three claims are false as worded.**
Measured across sampled sites on both logs:

| as worded here | claimed | measured |
|---|---|---|
| `drop-event` (drop an `:admission`) | `discharge-follows-admission` | 2–6 distinct finding sets, 4–8 invariants each |
| `clock-skew` | `timestamps-monotone` | 1 set on clinic-decade, 2 on ed-tuesday |
| `orphan-participant` | `participant-ids-exist-in-run` | 8 distinct sets, 1–9 invariants each |

A structural edit is not a content fault confined to one field, so it
cascades through the state machine, and two mechanisms behind that are
properties of the LOG FORMAT rather than of these operators — worth
recording here because any later structural operator meets them too:

1. **Dropping an event RENUMBERS the log**, silently repointing every
   log-index reference past the drop point one event earlier. A drop
   therefore injects referential faults it never declared unless the
   indices are repaired as part of the same edit.
2. **Renaming a participant MOVES the event into a phantom patient's
   timeline**, where every patient-scoped invariant convicts the phantom
   for having no `:registered` first event — correct, and not the
   declared class.

**This does not weaken Q5(a).** Set equality stands; what gives is the
breadth of `:candidate-sites`, which is what a candidate-site predicate
is for. All three ship NARROWED, each with a set measured identical
across every sampled site of both logs — `:clock-skew` unchanged in
name, `drop-event` replaced by `:drop-registration`, and
`:orphan-participant` scoped to therapeutic-intent clinical content by
deriving its kind list from `check`'s own
`clinical-content-only-when-admitted` rather than hand-listing it. The
ledger states each narrowing and the measurement behind it.

**A third gap kind follows from (c)**, and this addendum names it so
later sessions do not have to re-derive the distinction: a **shape gap**
is a candidate whose observed finding set varies site to site, so no set
can honestly be declared. It is neither a Q6 catalog gap (the catalog
convicts it — too well, and ambiguously) nor a Q10 population gap (sites
are plentiful). Its remedy is narrowing or nothing, never a declared set
chosen from the modal case.

**One finding outside this record's scope, recorded where it was
found.** `check-all`'s 1-arity defaults `facility-config` to
`sim-model/default-facility`, and `ehrt sim check` exposes no way to
pass another — so ed-tuesday's own clean log, whose config bumps its ED
ward from 6 surge slots to 16, reads at the shell as violating
`:occupancy-within-capacity`. The corpus is sound; the checker is
config-starved. Rowed on `roadmap.md`; not fixed by the breadth session,
whose fences and Q11(a) both forbid widening that surface.

### 7. Dated addendum, 2026-09-05 — the population gaps are zero

Entered by P7 (`roadmap.md#referential-corpus-population`). Addendum (a)
above records that two of the five referential carrier columns had a
population and three did not, and the ledger rowed those three as
FOURTEEN population gaps. **All fourteen are now shipped operators**,
and section 6 above stands unrewritten because it was true of the
corpora that existed when it was written.

**Nothing was authored to close them.** The row that carried them
PRICED the work as corpus authoring — a scenario exercising the cancel
family and closing its medication and care-plan spans — and that price
was never paid. `demos/scenarios/dense-7500/config.edn`, committed
2026-09-04 for the Scale table and for no reason connected to this,
carries all three columns at seed 5 with `--patients 20 --churn`: 4
`:cancel-transfer`, 6 `:medication-end`, 8 `:care-plan-end`. The
columns turned straight into their cells, which is exactly what the
ledger predicted would happen the day a population appeared — the
contract, the shapes and the convicting invariants were already fixed,
so each cell is a `:candidate-sites` predicate and a `:fn`.

**One correction to section 2(i) that measurement forced, and it is a
fourth after (a)–(c).** The section scopes the referential family to a
column per carrier and derives one operator id per `(shape, field)`
pair. Two columns share a field: `:order-event-id` rides both
`:result-available` (B1) and `:medication-end` (B2), which is addendum
(b)'s own point. Deriving ids from the field alone therefore collides,
and `register!` is a bare `swap! registry assoc` — so B2's entries
would have SILENTLY REPLACED four of B1's, with no refusal, no catalog
gap and no symptom. A column now declares an optional id stem
(`:slug`), and a bijection test between the loop rows and the
registered event operators is the gate over it.

**And one shape the ADR's `:target` could not express.** Column A's
`:cancels-event-id` rides three cancel kinds that cancel three
DIFFERENT event classes, so its target is not one event kind. `:target`
is now a keyword OR a map from carrier kind to target kind, read
through one `target-kind` helper every shape calls; a keyword column's
per-site answer is its column-wide one, so columns D and B1 are
unchanged by it.

**Q5(a) held throughout, and refused a cell before it shipped one.**
Column A's `inverted-span` was measured unwitnessable on this very
population — `cancel-references-existing-uncancelled-event` had no time
clause at all, so moving a cancel behind its referent convicted
`:timestamps-monotone` and nothing referential — and was REFUSED rather
than shipped as a duplicate `:clock-skew` wearing a reference field's
name. ADR-0178 (R-time) added the fifth disjunct, and the cell ships
because the CHECKER gained a clause, not because a corpus gained a
site.
