# `:allergy-onset` joins the log and the registration facts: the AllergyOnset fact becomes ground truth (event schema 1.11.0, describe 1.2.0, ADR-0184)

2026-09-30. Base `6641c017`. Ceremony: R30, taken from the session
prompt, which stated no prepare-only mode. `bin/preflight` exit 0, no
findings (last five CI runs green; HEAD matched origin/main; tree
clean; HEAD not tagged stable-*, disclosed). All commits were held
locally and pushed together with this record, so the red commit
`d373bcab` reached the remote only alongside its green successors and
no red tip reached CI.

## For downstream

**Event schema 1.11.0**: a new kind, `:allergy-onset` — `:active-mrn`,
`:codes` (the substance), and three fields each present **only when the
source states them** (absent, never nil): `:allergy-type` (string, e.g.
`"allergy"`/`"intolerance"`), `:category` (string, e.g. `"food"`,
`"environment"`, `"medication"`), and `:reactions`, a vector of
`{:codes [...]}` — **the reactions the source lists for the substance,
with no severity, not a claim about which manifested**. `:citation`
(optional, present when compiled from a module), plus the four keys
every event carries and the `:encounter-id` stamp when `:encounters` is
on. Legal only inside an open encounter. No HL7 message and no FHIR
resource yet.

**A pre-horizon allergy rides `:registered`'s `:pre-horizon-facts`**,
as `{:event :allergy-onset :codes [...] :citation ... :references ...}`
— substance codes only. That widens the nested `:event` enum, which is
why this bump was OWED (`classify-change` called it breaking): a
consumer dispatching exhaustively on a fact's `:event` meets a new
value. Five nested values now collide with top-level kinds. A 1.10.0
log validates unchanged against 1.11.0.

**No existing corpus byte moves** (both instruments below: one added
line each, every existing root identical). No shipped scenario produces
the kind; **author the step in your own pathway to produce it today** —
`{:type :allergy-onset :codes [{:system :snomed :code "762952008" :display "Peanut (substance)"}] :category "food"}`
inside an open encounter.

## Author rulings

- 2026-09-30, "Allergy": the slice lands now. Everything else in the
  prompt was channel expectation, corrected from the tree below.

## Premise mismatches, fixed forward with disclosure

1. **The loader did not drop the three fields.** The prompt: "the loader
   reads only `codes` and `target_encounter` ... dead past the loader";
   step 1(a) "red because the loader drops them". Measured (a REPL load
   of upstream's own Peanut_Allergy shape): the state schema is an open
   malli map, so `:allergy-type "allergy"`, `:category "food"` and
   `:reactions` all survived — undeclared, and each reaction in
   upstream's raw `{:reaction {:system "SNOMED-CT" ...}
   :possible-severities [...]}` shape, its Code un-normalized. One
   defensible reading: the red test asserts the NORMALIZED reaction
   Codes (red because the system is still the string `"SNOMED-CT"`), and
   step 2 declares the three fields and normalizes the reaction Code.
   What was truly dead was everything after the loader.
2. **The bump was owed, not merely taken.** `classify-change` against
   the frozen 1.10.0 baseline, before the re-freeze:
   `{:additive? false, :breaking [":registered: key changed: :pre-horizon-facts (value schema changed)"]}`.
   The prompt anticipated 1.10.0 -> 1.11.0, and that is what landed; the
   reason in the changelog is "owed", not 1.7.0's "taken anyway".
3. **No substance or reaction code was in the tree** (the prompt allowed
   "added with display"). Each was added with the display upstream's
   `allergies/*_incidence.json` modules carry at pin `7e08387c`, read
   from the local upstream checkout.

## A semantic finding the prompt did not name

Upstream's `ReactionProbabilities.generateSeverity` draws one level per
reaction from `possible_severities`, and a `none` draw DROPS the
reaction from the record (`AllergyOnset.diagnose`). With no draw taken,
`:reactions` cannot mean "what this patient experienced"; it means "what
the source lists for the substance". The schema `:doc`, the field
comment, gmf-interpreter.md and ADR-0184 decision 3 all say so. An
empty `reactions` list is not stated (upstream's own `isEmpty` guard).

## Step 1 — red (`d373bcab`)

Poly halts at the first failing namespace, so the red was taken by
namespace with `clojure.test` (`clojure -M:dev:test`, the six test
namespaces): **7 failures, 5 errors**, every one for its stated reason —

- pathway-test (2 FAIL): `:allergy-onset` is not valid IR.
- gmf-test (1 FAIL): reaction Codes `{:system "SNOMED-CT" ...}`, not
  `{:system :snomed ...}`.
- gmf-interpreter-test (1 FAIL): the event carries `{:codes}` only.
- compile-trajectory-test (3 FAIL): steps `[:outpatient-visit
  :outpatient-visit-end]` (the `:else` drop), so no allergy steps, and
  `registration-facts` `[]`.
- engine-test (2 ERROR): the pathways config assert refuses the step;
  `No method in multimethod 'decide' for dispatch value: :allergy-onset`.
- check-test (3 ERROR): `No method in multimethod 'evolve' for dispatch
  value: :allergy-onset`.

Controls green at the baseline, by design: the bare-state and
empty-reactions interpreter tests, `allergy-onset-consumes-no-rng`, the
IR refusal test, the loader's bare-state half.

## Step 2 — the kind (`e6cd9b68`)

As the commit message lists. Invariants: `run.clj` diff empty; step 1
green (592 tests, 1,868 assertions, 0 failures, by namespace); brick
sim-model, patient-simulator, sim-engine, sim-check, corpus,
person-simulator, sim-emit-hl7 green; docs-tooling RED once —
`the-nested-event-warning-names-every-colliding-name` pinned "4 of
them", and its own message says "the count is derived; if it changes,
the sentence must too" — moved to 5 with `:allergy-onset` added to its
colliding list, then green. `poly check` OK. The examples diff adds the
`:allergy-onset` example and moves nothing else.

## Step 3 — the producer (`8529abde`)

`allergy-fixture.json`, config `{:seed 20260930 :patients 12
:arrival-gap 90}`, ordinals 0-3 on the fixture, the rest on an authored
Renal stay with `{:type :allergy-onset :codes [Latex] :category
"environment"}` — exactly as predicted: 52 events (12 `:registered`, 8
`:admission`, 8 `:discharge`, 4 `:outpatient-visit`, 4
`:outpatient-visit-end`, 16 `:allergy-onset`); of the 16, 8 compiled (4
`:peanut-allergy` carrying all three optional fields, 2 reactions each;
4 `:grass-pollen-allergy` carrying none) and 8 authored (`:category`
only); 4 Bee venom `:pre-horizon-facts` entries, no Bee venom log event;
`check-all` ok; no `:transfer`.

Landed as the oracle's 43rd root, `allergy` (`engine-pair`, a FIRST
BASELINE; `witnessed-event-kinds` 29 of 30 -> 30 of 30,
`witnessed-message-types` unmoved; root-count pins 42 -> 43 and 39 ->
40) and as `ehrt.sim-engine.engine-test/all-three-allergy-routes-reach-the-log-and-self-check-clean`.

**The instruments**, `6641c017` -> `8529abde`:

* `bin/ground-truth-bracket 6641c017 8529abde --declared-digest-change`
  — **DIFFERS, exit 1, and the diff is one added line**:
  `+52b0629a… allergy.edn`. Every existing ground-truth root IDENTICAL;
  3 interpreter batches skipped by name on both sides (appendicitis,
  ear-infections, sore-throat). Its coverage banner reads "39 roots
  carry :ground-truth" — the BASELINE side's count; the target side
  lists 40.
* `bin/regression-oracle 6641c017 8529abde` — first run WITHOUT the flag
  stopped at its soundness check (digest.clj differs outside its
  docstring: the new root), exit 1, as designed. Rerun with
  `--declared-digest-change` — **DIFFERS, exit 1, and the diff is one
  added line**: `+d9fa2879… allergy.edn`. All 42 existing roots
  IDENTICAL.

Roots whose digest moved: none. Roots added: `allergy`, target side
only. That is the stated invariant.

## Step 4 — describe 1.2.0 (`18fb702b`)

The `:opt-in :allergy-onset` row, witness field `:category`;
`during-encounter-kinds` gains the kind. The golden's whole diff: the
version string and one row
(`{:family :allergy-onset :group :opt-in :configured :unknown :observed 0 :witnesses [] ...}`).
Two pins named the version beyond the golden, both moved: sim's
`run-test` ("describe 1.1.0" in the text format) and formats.md's
describe section. brick:sim-check green; `ehrt.sim.run-test` green.

## Step 5 — docs (`b97ccab7`) and step 6 — ADR-0184 (`71df6a7d`)

As the commit messages list. The three "additive does not bump" copies
now read "does not OWE a bump", and name 1.11.0 as owed. The
gmf-interpreter.md AllergyOnset row is ADDED (the table never carried
one; the Vaccine row's sentence saying so is dated, not deleted). No
touched doc is a hand-owned asset (`hand-owned-assets.edn` checked).

`make docsgen && git diff --exit-code`: clean at `71df6a7d` (DOCSGEN_EXIT=0, DIFF_EXIT=0), run immediately before the suite below.

## Step 7 — the suite

Full `make test`, unpiped, wrapper ending `exit "$MAKE_EXIT"`, at
`71df6a7d` (all six commits, before this record existed): **MAKE_EXIT=0**
— 432 `Test results` lines, 29,467 passes, 0 failures, 0 errors. Rerun
with this record and the prompt archive on disk, after `make docsgen`:
**MAKE_EXIT=0** again — 432 lines, 29,467 passes, 0 failures, 0
errors; that `make docsgen` (exit 0) moved nothing beyond the two record
INDEX lines and `.agents/state-derived.md`'s counts, committed with this
record. Both runs were taken by the wrapper alone; no `poly test`
substitute preceded the push.

## Findings, not acted on

1. **`target_encounter` is not honoured**, and upstream's own allergy
   incidence modules fire AllergyOnset with no encounter open. Vendoring
   one today would compile onsets outside an encounter and trip
   `clinical-content-only-when-admitted`. ADR-0184 names the deferral as
   owed before any such module is vendored.
2. **The loader's state schemas are open maps**, so an undeclared field
   on any GMF state passes silently — this slice's own premise mismatch
   1. Not swept here.
3. **gmf.clj's `:vaccine` schema comment** still says the interpreter
   "supplies the same zero-default", which ADR-0182 withdrew. Stale
   comment, outside this slice.

CI green on the pushed tip is this session's close marker.
