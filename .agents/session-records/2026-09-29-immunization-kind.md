# `:immunization` joins the log: the Vaccine fact becomes a ground-truth kind (event schema 1.10.0, ADR-0182)

2026-09-29. Base `01a87e11` (the tip also carried the author's own
`3d1228a2`, a handoff note with no code, pushed with this session).
Ceremony: R30, taken from the session prompt, which stated no
prepare-only mode. All commits were held locally and pushed together
with this record, so no red tip reached CI.

## For downstream

**Event schema 1.10.0**: a new kind, `:immunization` —
`:active-mrn`, `:codes` (the coded product, CVX), `:series` (integer,
present **only when the source states one** — absent, never 0, never
nil), `:citation` (optional, present when compiled from a module), plus
the four keys every event carries and the `:encounter-id` stamp when
`:encounters` is on. Legal only inside an open encounter. No HL7
message and no FHIR resource yet. A 1.9.0 log validates unchanged
against 1.10.0.

**No existing corpus byte moves** (both instruments below, IDENTICAL on
every existing root). No shipped scenario produces the kind; **author
the step in your own pathway to produce it today** —
`{:type :immunization :codes [{:system :cvx :code "115" :display "Tdap vaccine"}] :series 1}`
inside an open encounter.

## Author rulings

- 2026-09-29, "For B, before describe": the slice lands now.
- 2026-09-29, step 3 (after the first premise halt): "test-only gated
  run, with one addition: the run exercises BOTH producers" — a
  hand-authored fixture module on explicit ordinals plus a weighted
  authored admission/discharge pathway carrying `:immunization`, landing
  as a FIRST BASELINE oracle root; invariant "IDENTICAL on every
  EXISTING root; the new root appears on the target side only".
- 2026-09-29, step 3 (after the second premise halt): "oracle root +
  engine test" — `digest.clj` root `immunization` via `engine-pair`, and
  a sim-engine engine-test deftest through `run/run` + `check-all`; OUT
  of `gated-runs`. Downstream line: "no existing corpus byte moves;
  author the step in your own pathway to produce it today."

## Commits

| sha | what |
|---|---|
| `dbdb1fe2` | RED — step 1's gates in four bricks |
| `57ccd136` | the kind, schema 1.10.0 |
| `d986bf5c` | the `immunization` oracle root + the engine producer test |
| `f5ca2b55` | ADR-0182 |
| `6f2f60e6` | docs |
| this commit | record + prompt archive |

## Step 1 — red, for the stated reasons

At `01a87e11` plus the tests only (brick runs, one brick per poly
invocation; poly halts a project at the first failing namespace, so the
patient-simulator interpreter test was witnessed by a direct run):

* **sim-model** `pathway-test`: 2 failures — `:immunization` is not
  valid IR, with and without `:series`/`:citation`. The companion
  refusal test (nil `:series`, missing `:codes`) passes at the baseline
  — a control, since an unknown step type is refused whatever its shape.
* **patient-simulator** `compile-trajectory-test`: 3 failures — the
  trajectory events carry `[1 0]` (the zero-default) where `[1 absent]`
  is required; the steps are `[:outpatient-visit :outpatient-visit-end]`
  (the `:else` drop); no `:immunization` steps. The pre-horizon-drop test
  is a control, green before and after.
* **patient-simulator** `gmf-interpreter-test`: the Wave I test pinning
  the zero-default (`vaccine-with-no-series-defaults-to-zero`) was
  REWRITTEN to `vaccine-with-no-series-carries-no-series-key` — red, the
  key is present. This supersedes ADR-0040 AR-5's default, on the
  prompt's own step 2.
* **sim-engine** `engine-test`: 740 passes, 2 errors — "No method in
  multimethod 'decide' for dispatch value: :immunization", and the
  pathways-config assert refusing the step.
* **sim-check** `check-test`: 150 passes, 3 errors — "No method in
  multimethod 'evolve' for dispatch value: :immunization": the law
  cannot replay a log holding the kind.

## Step 2 — the kind

As the prompt specified, plus what the tree asked for. `classify-change`
against the frozen 1.9.0 baseline, before the re-freeze, verbatim:

    {:additive? true, :breaking []}

**No bump owed; 1.10.0 taken on 1.7.0's precedent** (a consumer's only
handle on which kinds a log can contain). Changelog in `schema-version`.

Beyond the prompt's list, each found by a gate or by reading:

* `evolve :immunization` (identity) — sim-check's red said so.
* `:vaccine` joins `pre-horizon-dropped-types`: without it a history-
  phase vaccine, dropped before by `:else`, would have compiled to a
  step outside any encounter.
* The event fleet's clinical fixture gains one Vaccine state —
  `event-schema-test` requires the fleet to produce every declared kind.
  Placed AFTER every state an end cites back to; the regenerated
  examples diff ADDS the `:immunization` example and moves nothing else.
* Kind-count pins 28 → 29: `event-schema-test`, `event-log-doc-test`,
  docs-tooling `oracle-coverage-test`, person-simulator
  `limitations-test`.
* `emittable-events-test`'s literal log-kind list gains `:immunization`.
* `event-conformance-test`'s no-message set 10 → 11, with the registry
  comment its own failure text asks for.
* `docs/consuming-ground-truth.md`'s manifest pin `"1.10.0"`.

Invariants: `git diff 01a87e11 -- components/sim-engine/src/ehrt/sim_engine/run.clj`
empty; `decide :immunization` takes `_streams` — no new draw site.
Gates: brick:sim-model, patient-simulator, sim-engine, sim-check,
corpus, person-simulator, sim-emit-hl7, docs-tooling green
(`skip:integration`); `poly check` OK.

## Step 3 — the producer, and two premise halts

**Halt 1.** clinic-decade authors no steps (`:pathway {:steps []}`);
every visit in it is module-compiled; its follow-up visit's steps are
hardcoded in `decide :discharge` (decide.clj:1263); no shipped scenario
authors an `:outpatient-visit`; no oracle root reads the config; and any
added visit draws the shared `:facility` stream for its attending, so
"by exactly the new events" was unsatisfiable. Asked; ruled (above).

**Halt 2.** The amended ruling put the run into `gated-runs` "and
therefore the oracle". `gated-runs` does not feed the oracle (digest.clj
defines its roots itself); a member owes a committed arc-0 baseline and
an ADR-0171 pre-partition digest
(`gated-corpora-re-pin-exactly-once-under-the-new-scheme` asserts
`(some? pre)`); and `run-command` resolves modules only by name from
`sim/modules/`. Asked; ruled (above).

**The fixture.** `immunization-fixture.json` beside `death-fixture.json`:
a wellness visit, Tdap (CVX 115) stating `"series": 1`, PPSV23 (CVX 33)
stating none — both codes already in the tree (`gmf_test.clj`). **Its
first draft compiled nothing**: it held the walk with a `Date` Guard, and
the interpreter jumps time for `:age` guards only
(`age-guard-jump-days`) — a failing `:date` guard blocks the walk for
good (measured: `:status :blocked`, `:t` = DOB). It now loops 365 days
at a time until the registration year.

**Measured**, config `{:seed 20260929 :patients 12 :arrival-gap 90}`,
ordinals 0-3 on the fixture, the rest on an authored Renal stay with
`{:type :immunization ... :series 1}` — exactly as predicted: 52 events
(12 `:registered`, 8 `:admission`, 8 `:discharge`, 4
`:outpatient-visit`, 4 `:outpatient-visit-end`, 16 `:immunization`);
of the 16, 8 compiled (4 `:series 1` on `:tdap-dose`, 4 with no key on
`:pneumococcal-dose`) and 8 authored (all `:series 1`); `check-all` ok;
MSH-9s 8 ADT^A01, 8 ADT^A03, 4 ADT^A04; no `:transfer`.

Landed as the oracle's 42nd root, `immunization` (`engine-pair`, a FIRST
BASELINE; `witnessed-event-kinds` 28 of 29 → 29 of 29,
`witnessed-message-types` unmoved; root-count pins 41 → 42 and 38 → 39)
and as `ehrt.sim-engine.engine-test/both-producers-reach-the-log-and-self-check-clean`.
`.agents/state-derived.md` regenerated (it parses the roots map).

**The instruments**, `01a87e11` → `d986bf5c`, `--declared-digest-change`
(the digest.clj body changed by the new root and its coverage claim):

* `bin/ground-truth-bracket` — **DIFFERS, exit 1, and the diff is one
  added line**: `+16b8c630… immunization.edn`. The 38 existing
  ground-truth roots IDENTICAL; the same 3 interpreter batches skipped
  by name on both sides (appendicitis, ear-infections, sore-throat).
* `bin/regression-oracle` — **DIFFERS, exit 1, and the diff is one added
  line**: `+59456db7… immunization.edn`. All 41 existing roots
  IDENTICAL.

Roots whose digest moved: none. Roots added: `immunization`, target side
only. That is the ruled invariant.

## Step 4 — docs

`make docsgen` carries the schema `:doc` into formats.md's generated
event-log section (version 1.10.0, 29 kinds, the `:immunization`
section with the fleet's real example). By hand: formats.md's schema-
provenance row "**29** today"; use-cases.edn's two "28 event kinds";
patient-state-model.md's validity row; sim-theory.edn's compile-stage
contract; gmf-interpreter.md section 1 — **a Vaccine row ADDED, not
updated** (premise mismatch: the table never carried one, nor an
AllergyOnset row). future-features.md does not list immunization.
`make docsgen && git diff --exit-code`: clean at `6f2f60e6`; rerun with this record and the prompt archive on disk, it moves only their own INDEX lines and `.agents/state-derived.md`'s counts, committed here. exercised-sources gate green (brick:docs-tooling).

## Step 5 — ADR-0182

Accepted; `notes/ADRs.md` regenerated. Follow-ons named there, not
rowed: VXU^V04, FHIR Immunization, AllergyOnset by the same path, a
shipped generator.

## Step 6 — the suite

Full `make test`, unpiped, wrapper ending `exit "$MAKE_EXIT"`, run after `make docsgen` with this record and the prompt archive on disk: **MAKE_EXIT=0** — 430 `Test results` lines, 28,717 passes, 0 failures, 0 errors.

## Findings, not acted on

1. **A `:date` Guard blocks a GMF walk permanently** — only `:age`
   guards jump time. Any vendored module whose mandatory path holds on a
   bare Date guard before its own year would silently stop producing
   content. Not measured whether any of the 31 does.
2. **use-cases.edn's custom-emitter note says "additive change (a new
   kind, a new optional key) does not bump [the version]"**, which
   1.7.0 and 1.10.0 both contradict (each took a MINOR bump for a new
   kind). Left untouched: outside this slice's own claims.
3. **AllergyOnset** remains the other half of the pair: a real
   trajectory event dropped at `:else`, with no gmf-interpreter.md
   section 1 row.

CI green on the pushed tip is this session's close marker.
