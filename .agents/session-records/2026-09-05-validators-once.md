# 2026-09-05 -- validators built once: the schema gate at its real cost

`m/validate` compiles its schema on every call. Three predicates call
it PER RECORD over a stream, and since ADR-0178 put
`every-event-is-schema-valid` first in the check catalog, every
`check-all` in the workspace and every `ehrt sim check` a consumer runs
paid that compile once per event. Hoisting it is one `def` per schema.

**`check-all` over the dense-7500 log at 20 arrivals: 22.70 s ->
2.74 s. Full `make test`: 2,043 s -> 1,235 s, at the same 4,827 tests
and 26,851 assertions, to the assertion.** Output-identical per ruling
S1(a); no red owed, and the witnesses are measurement, the unchanged
counts, and the regression bracket.

Base `f6eeeba`. Ceremony: R30, commit and push at each checkpoint. No
sub-agents. Two commits:

| sha | commit |
|---|---|
| `642d70a` | perf(schema): validators built once at load (S1(a), output-identical) |
| this one | docs: validators-once session record (archives prompt) |

## 0. `bin/preflight`

Ran before any git operation. **No findings, exit 0.** Last five CI
runs on `main` green. Edit root `/home/mg/src/ehr-testing-tools`, not
under `/mnt/`; tree clean including untracked; local HEAD matched
`origin/main` at `f6eeeba`. One disclosure, and it is the correct
state: HEAD is not tagged `stable-*`, and no tag is paid.

## 1. Method

Per the P7 record's own method: a harness run with `clojure -M:dev -i`
from the workspace root, once before the change and once after,
nothing else running on the machine. OpenJDK 21.0.7 both times.

**The log is the same log, and that is shown rather than asserted.**
`demos/scenarios/dense-7500/config.edn`, seed 5, `--patients 20`,
`:churn true` (per `rulings.md#R-population`), regenerated in each JVM
because the path is deterministic, and the harness prints
`(count log)` and `(hash log)` so the runs can be compared:
**18,466 events, hash `-1628629845`**, in all three JVMs this session
ran. The 28-event `event-examples.edn` is the per-event fixture, the
same one P7 measured on.

Per-call cost is mean wall-ms over n calls after warm-ups: n=2000
after 200 warm-ups for the two per-record predicates, n=3 after 1 for
the whole-log one. `check-all` is a single call, timed, on the log
above with the config's own facility.

**No timing assertion enters the suite** -- S1(a). Nothing below is a
gate; it is a measurement with a date on it.

## 2. Before and after

| call | before | after | |
|---|---|---|---|
| `valid-event?` (per event) | **1.0136 ms** | **0.0135 ms** | 75x |
| `valid-persona?` (per persona) | **0.0239 ms** | **0.0023 ms** | 10x |
| `valid-ground-truth?` (whole 18,466-event log) | 44.881 ms | 49.071 ms | see 2a |
| `check-all` (dense-7500 @20) | **22.70 s** | **2.74 s** | **8.3x** |

**The per-event number predicts the wall, which is the check that says
the cost was where it was claimed to be.** 18,466 x 1.0136 ms is
18.7 s; the observed `check-all` saving is 19.96 s. The remaining
2.74 s is the other 45 catalog rows plus `run-t-monotone?`, and it is
the figure `roadmap.md#performance-residual-sites` should now be read
against -- its `engine/replay` row was written when the check phase
was 7.26 s, and schema validation had buried it.

`check-all` reports `:ok` on this log before and after, as it did for
P7.

### 2a. The whole-log predicate barely moves, and the reason is real

The before/after runs disagreed by **+9%** on `valid-ground-truth?` --
in the WRONG direction -- at n=3, which is not enough to say anything.
Rather than record a cross-JVM comparison as fact, the two forms were
timed against each other in ONE JVM at `642d70a`, interleaved, n=10
per round:

| round | interpreted `(m/validate GroundTruth log)` | compiled `valid-ground-truth?` |
|---|---|---|
| 0 | 43.937 ms | 42.016 ms |
| 1 | 44.226 ms | 40.674 ms |
| 2 | 46.700 ms | 39.531 ms |

**~8%, consistently, and the cross-JVM +9% was noise.** The small
effect is the correct one: `GroundTruth` is `[:vector Event]`, so
`m/validate` compiles it ONCE and applies the result to all 18,466
elements. The per-call compile was already amortized here; it never
was in `valid-event?`, and that is the whole difference between the
two rows. `valid-ground-truth?` is changed anyway, because it is the
same one line and leaving it interpreted would leave the next reader
to re-derive why one of a pair was hoisted and the other was not.

That same run also asserted the two forms agree (`agree? true`) on the
log, which is section 4's claim in miniature.

## 3. The change

Two files, 56 insertions, 3 deletions, and the payload is five lines:

```
components/sim-engine/src/ehrt/sim_engine/event_schema.clj:1093-1094
  (def ^:private event-validator        (m/validator Event))
  (def ^:private ground-truth-validator (m/validator GroundTruth))
components/sim-model/src/ehrt/sim_model/persona.clj:151
  (def ^:private persona-validator      (m/validator Persona))
```

with the three predicates delegating. Everything else in the diff is
the comment that says why.

**Load order verified**: each validator is def'd immediately after its
own schema (`Event`, then `GroundTruth`, then both validators;
`Persona`, then its validator), so no forward reference exists to get
wrong, and a wrong one would be a load-time failure rather than a
silent fallback. `^:private` throughout -- no public surface moves, so
`ehrt.sim-engine.interface`'s frozen-surface baseline (AR-M4-3) is
untouched and `(def valid-event? event-schema/valid-event?)` there
still copies the same var's value.

## 4. R-bracket

Both against `f6eeeba`, both exit 0, both **IDENTICAL**.

**`bin/regression-oracle f6eeeba 642d70a`** -- SHA-256 digests of the
`{:ground-truth :hl7}` pair for every vendored root, produced from a
synthetic from-scratch classpath in two disposable worktrees:
`IDENTICAL: every root's digest matches between f6eeeba and 642d70a`,
**41 roots**. This is the claim `notes/ADRs.md` ADR-0030 J2 defines,
made by naming this script's own output rather than a test count.

**`bin/ground-truth-bracket f6eeeba 642d70a`** -- its sibling, the
`:ground-truth` half alone, which is explicitly NOT a regression-oracle
claim (ADR-0175 E1, and the script prints that line itself):
`IDENTICAL: every digested root's :ground-truth matches between
f6eeeba and 642d70a (38 roots)`, with 3 roots skipped for carrying no
`:ground-truth` key at all (`appendicitis.edn`, `ear-infections.edn`,
`sore-throat.edn`).

Both reported `declared-digest-change: no`, which is the correct
declaration: this change declares none.

**IDENTICAL was the ruling's expectation, and it is also the
load-bearing half of it.** R-bracket makes any delta a STOP, because a
compiled validator disagreeing with the interpreted one would be a
malli defect rather than a fix. Nothing disagreed -- across 41 roots
here, and directly on the 18,466-event log in section 2a.

## 5. Every other `m/validate` site in the tree, classified

R-scope fences this session to the three predicates and their defs.
The other sites stay interpreted, and here is each one with what it
validates and how often, so the next hot one is recognised rather than
rediscovered. **Per-record** means once per element of a stream or
collection; **load-time** means once per config, registry, manifest or
report.

### Deliberately left interpreted although per-record

| site | what | why unchanged |
|---|---|---|
| `sim-engine/event_schema.clj:1097` `explain-event` | per VIOLATION | cold by construction -- called only after `valid-event?` already failed; `m/explain` has no `m/validator` analogue |
| `sim-model/persona.clj:154` `explain-persona` | per VIOLATION | same |
| `corpus/check/schemas.clj:60` `valid-against?` | per DATUM, from `check.clj:311` | the schema is `(:schema entry)`, resolved from the registry per assertion -- a `def` cannot hoist it; it needs a per-entry validator cache, which is a design, not a one-liner. THE NEXT ONE, if a corpus check ever runs at log scale |
| `sim-engine/state.clj:400` `valid-patient?` | per patient state | NO src caller: `engine_test.clj:1971,1981` only. Costs nothing in production because nothing in production calls it |

### Per-record, but over collections that are small by construction

`judge/finding.clj:78` `valid-cause-pairing?` and `:91` `valid?` (per
finding); `judge/report.clj:52` `valid?` (per report);
`corpus/lineage.clj:60` `valid?` (per lineage record);
`corpus/intake.clj:116` `valid-catalog-entry?` and `:120`
`valid-intake-record?`; `kernel/result.clj:45` `valid?` and
`kernel/invocation.clj:30` `valid?` (per command envelope);
`kernel/locator.clj:20` `valid?`; `palgebra/signature.clj:65`
`valid-stage?` (dynamic schema, `(stage-schema kinds)`).

### Load-time

`sim-model/config.clj:76,119,120,218,255,283,323,367,447` (facility,
provider template, provider, latency, chatter, charges, ladder, SIU,
fan-out profiles); `sim-model/pathway.clj:276,299`;
`sim-engine/order_profiles.clj:64`; `sim-engine/churn.clj:79`;
`sim-engine/config.clj:175,187`; `sim-emit-hl7/site_profile.clj:324`;
`patient-simulator/gmf.clj:1351,2001`;
`provenance/manifest.clj:34,60,110`;
`corpus-io/source_sink.clj:189,193,206` and
`operation_manifest.clj:47`; `corpus/check.clj:109,113`;
`corpus/generators.clj:46,81`; `corpus/operators.clj:170,173`;
`corpus/check/schemas.clj:33`; `kernel/canonical.clj:29`;
`kernel/artifact.clj:64`; `judge/pairing.clj:70`;
`docs-tooling/exercised_sources.clj:61` and
`usecases.clj:104,108,112`; `palgebra/signature.clj:69,73,77`.

**None of these is worth hoisting on today's evidence**, and that is a
claim about how often they run, not an assertion that they are cheap
per call.

## 6. What the fence kept me from touching

`components/corpus/test/ehrt/corpus/event_mutate_test.clj:139-160`
defines a private `schema-valid?` built from `(m/validator
engine/Event)`, with a docstring saying the one-line fix "is
`sim-engine` src and outside the P7 fence". **That fix is now landed,
and the helper is now redundant with `engine/valid-event?` itself.**
The docstring is not wrong -- it is P7's history, and it was true when
written -- but a reader arriving at it today will find it describing a
gap that no longer exists. R-scope fences this session to the three
predicates and their defs, so it is disclosed here rather than edited,
the same treatment `check.clj:573`'s seam sentence got.

Nothing else was touched: no schema content, no catalog row, no gate,
no test.

## 7. The close gate

**Full `make test`, exit 0: 4,827 tests, 26,851 assertions, 0
failures, 0 errors**, over both projects (`conformance`, `ehrt-cli`),
in **1,235 s** wall against the prior session's **2,043 s** -- 808 s,
40%, off a suite that changed by nothing.

**The counts are the second output-identical witness, and they are
exact.** 4,827 / 26,851 is what
`.agents/session-records/2026-09-05-p7-referential-columns.md` recorded
at `f6eeeba`, to the assertion. Not one test moved, not one assertion
moved, and the suite runs 40% faster. That is what an output-identical
change looks like from the gate's own side.

`clojure -M:poly check` green. `bin/verify-nist-lock` OK on all six
hit-nexus coordinates. Step 2's own narrower gate ran first:
`sim-engine`, `sim-model` and `sim-check` across all three projects
that carry them, nine runs, 0 failures and 0 errors in each.

**808 s is more than the 15 `check-all` calls in `event_mutate_test`
account for, and that is expected rather than surprising**: every
`event_conformance_test` in `sim-emit-fhir`, `sim-emit-hl7` and
`sim-check`, plus `event_schema_test` itself, runs `valid-event?` over
a whole fixture log.

### 7a. CI, and why it is the weaker witness

Verified with `gh run view`, per
`rulings.md#R-session-verifies-ci-via-gh`. Run `33958532397` at
`1737bee`: **success**, one `test` job, **878 s** wall, against the
prior run (`33939095121` at `f6eeeba`), **1,271 s**. The perf commit's
own run (`33956722405` at `642d70a`) was **844 s**.

**That pair reads well and it should not be leaned on, so the spread is
given rather than hidden.** The last fourteen runs on `main` span 436 s
to 1,302 s. The four between ADR-0178 landing and this fix were 872,
898, 1,271 and 1,302 s; the two since are 844 and 878 s -- at the
bottom of that band, but overlapping it. CI's own run-to-run variance
is the same order as the effect, and its job does more than the suite
(`clojure -M:poly check`, `bin/verify-nist-lock`, `make docsgen` and a
freshness diff).

**So the local before/after in section 7 is the measurement, and CI is
the gate.** CI says the change is green. It does not, on this evidence,
say how much faster anything got, and this record does not claim it
does.

## 8. Roadmap

`roadmap.md#performance-residual-sites` stays **OPEN** and keeps every
site it names. It gains one dated clause recording that the schema
compile was paid, because that row is where a future session will look
for what the check phase costs, and the figure it carries (`~40%` of a
7.26 s check phase) was measured before ADR-0178 put a 22.70 s gate in
front of it.

## 9. Findings

- **`valid-ground-truth?` was never a hot site**, and the before/after
  table alone would have suggested it got slower. `[:vector Event]`
  amortizes the compile over the whole log; only the per-element
  predicate ever paid it 18,466 times. Section 2a.
- **P7's figures were colder than this session's, and the ratio is
  smaller here rather than larger.** P7 recorded 2.288 ms per
  `valid-event?` against 0.0063 ms prebuilt (365x); this session
  measures 1.0136 ms against 0.0135 ms (75x) on the same population and
  the same machine. Both sides warm up under 200 warm-up calls, so both
  numbers fall and the ratio compresses. The MECHANISM, the fix and the
  wall-clock consequence are identical; only the multiplier is regime-
  dependent, and this record's numbers are the warmed ones because a
  `check-all` over 18,466 events is warm long before its last event.
  Stated rather than quietly replacing P7's.
- **`corpus/check/schemas.clj:60` is the one remaining per-record
  `m/validate` with a real caller**, and it cannot be fixed the same
  way -- its schema comes from the registry per assertion. Section 5.
