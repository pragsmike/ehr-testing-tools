# 2026-09-01 — sim check learns its facility

## 1. Scope

Asked for: **a config input for `ehrt sim check`**, so the taught pipe
stops convicting the shipped demo's own clean log — under ruling
**Q14(a)**, which licenses widening the check CLI surface while leaving
**Q11(a)** (no `sim-check.interface` widening) standing. Plus two
riders: **Q13(a)**, the `:mutation` family-6 reservation, and
**Q15(a)**, the P6 row's rotation to Done.

Did all of it, in four commits from `188e26a`, red before green.

1. `adeab2b` — RED. Three CLI tests; the false positive shown.
2. `3ec147f` — GREEN. `--config PATH`, the thread, `docs/cli.md`.
3. `cfea631` — Q13(a). The family-6 reservation.
4. this record, its paired prompt archive, the docs sweep, the roadmap
   rotation, the AR-M4-3 baseline edit and its ADR amendment, and the
   state-derived regeneration, last.

**The headline is that the STOP fired — from a fence the prompt did not
name.** `ehrt.sim-check.interface`, the seam Q11(a) fenced and the prompt
told this session to watch, needed no widening at all: it already carries
every arity Q14(a) wants, which is why the check CLI change is small. The
seam that actually stopped the session was `ehrt.sim.interface`,
permanently frozen by AR-M4-3 out of a different arc, and it was its own
gate that caught the drift — at the final suite run rather than at step 1,
where a sweep for frozen-surface gates on every seam the thread crosses
would have found it. Escalated and ruled rather than absorbed; section 11
carries the decision and what the alternative would have cost.

## 2. Step 1 — the derivation, and the interface question answered

`ehrt.sim-check.interface` already re-exports `check-all` at **all four
arities**, `facility-config` included. So does `ehrt.sim.interface`. The
seam Q11(a) fenced was already wide enough to carry Q14(a), and no
widening of it was proposed, considered or taken.

The narrow points were two, and neither is that seam:

| layer | before | changed |
|---|---|---|
| `sim-check.check/check-all` | 4 arities | no |
| `ehrt.sim-check.interface/check-all` | **all 4 re-exported** | **no — the fenced seam** |
| `ehrt.sim.interface/check-all` | all 4 | no |
| `corpus.sim-adapter/check!` | called the **1-arity**, dropped opts | **yes** |
| `cli/sim-check-command` | `[_opts]`, dropped opts | **yes** |
| `cli-spec` parsing | `--config` already parsed generically | no |

Two derivation findings shaped the fix rather than merely describing it.

**(D1) The facility is echoed back VERBATIM, so the agreement is exact.**
`sim-engine.run` returns `:facility facility` — the destructured
parameter itself. Only *providers* are materialized. A `:facility` read
straight off the config file is therefore byte-identical to the one the
run's own self-check was handed, which is why `check --config X` and
`run --config X`'s self-check agree by construction and not by
coincidence.

**(D2) Mirror the run's 3-arity, NOT the 4-arity.** `ehrt.sim.run`'s own
self-check call is `check/check-all ground-truth facility
warm-up-seconds` — facility and warm-up, deliberately not order
profiles. Threading `:order-profiles` here as well would let
`check --config X` convict a log that `run --config X` had already
passed — breaking the demo pipe in the *opposite* direction from the
defect being fixed. So `result-analytes-match-order-profile` still reads
the shipped defaults on both sides. That is a real, pre-existing,
**symmetric** gap; it is documented rather than half-closed.

Home: `ehrt.sim.run/check-command`, beside `run-command`'s own
self-check call. Same orchestration step, and colocating them is what
stops the two from drifting about the same corpus. It reuses
`merge-config-file`, so `--config` gives ONE named rejection across
`run`/`identifiers`/`check` — the discipline ADR-0176 already applied to
this verb's three stdin rejections.

## 3. Red-green evidence

**The RED, shown rather than asserted.** At `adeab2b`, two of the three
new tests failed, and each for the right reason:

* the with-config test reported **15 `:occupancy-within-capacity`
  findings, every one `:capacity 6` against `:occupied 7` or `8`** — the
  default 6-slot Emergency ward convicting a 16-slot corpus, which is
  the defect exactly and not a proxy for it;
* the `--config` missing-path test failed with `:invariant-violation`
  rather than `:config-not-found`, because the flag was parsed and then
  dropped on the floor.

The third test **passed at RED and passes at GREEN**, deliberately: it
pins the documented default — no flag still means the shipped defaults.
That is the fence asserted rather than promised.

**The log is ed-tuesday's canonical invocation** (`README.md`'s own
seed, reference-date, `--churn` and config) at **20 patients rather than
100**. Disclosed in the namespace and here rather than silent: the
reduction is for test cost, and 20 is the smallest count measured this
session that still carries the conviction. Measured this session at the
same seed: 20 → 15 findings, 40 → 16, 100 → 115 (the last reproducing
`docs/consuming-ground-truth.md`'s own standing figure exactly).

## 4. What running it at a real shell found

**`help.clj`'s `:flags` vector is not documentation — it is the flag
whitelist.** `validate-known-flags` (AR-U3-2) reads the same `cli-spec`
the help page renders, so a verb with `:flags []` rejects every flag by
name at exit 2. The first real invocation of the fixed pipe returned
`{:category :unknown-flag, :flag "--config", :verb "sim check"}` while
every in-process test passed, because those call `sim-check-command`
directly and never cross `main!`.

Found by running the exerciser instead of trusting the suite. Fixed by
declaring the flag in `cli-spec` — one spec, so the help page, the docs
page and the whitelist cannot disagree — and pinned by a new
`main!`-level test, so the capability and its whitelist cannot diverge
again.

## 5. The exerciser, end to end

`ed-tuesday` config, seed 20260811, 40 patients, real subprocesses:

| pipe | exit | findings |
|---|---|---|
| `run --config X \| check --config X` | **0** | — |
| `run --config X \| check` | 1 | 16 spurious `:occupancy-within-capacity` |
| `run --config X \| mutate clock-skew \| check --config X` | 1 | **exactly `#{:timestamps-monotone}`** |
| `run --config X \| mutate clock-skew \| check` | 1 | that one, buried under the same 16 |

**The last pair is the point.** ADR-0176's Q5(a) contract is set
EQUALITY — the findings observed equal the findings declared, in both
directions. On any config-overriding scenario that contract was
**unverifiable at the shell**, because the declared finding arrived
inside a crowd of spurious ones. The lineage sidecar's own
`:expected-findings #{:timestamps-monotone}` now matches what the shell
reports, exactly.

## 6. Q13(a) — the family-6 reservation

A **comment, not a row**. `ehrt sim mutate` draws from its own seed and
touches no run stream, so a sixth entry in `stream-family-tag` would
name a family with no stream behind it. The NUMBER is what needs
holding, so a later session that does want a run-seed-derived mutation
stream adds row 6 rather than re-keying the table and reshuffling every
existing stream — the same reason ADR-0171 declared `:person` with zero
draw sites.

ADR-0176 section 2(iii) already asserted this reservation and cited
`streams.clj` for it. The citation was aspirational; this makes it true.

## 7. Oracle

**IDENTICAL across the session's whole span**, `188e26a` → `cfea631`:
every one of the **41** roots' digests matches, `bin/regression-oracle`
exit 0. Expected by construction — `check` reads and never writes, and
the only `components/sim-engine` edit in the span is a comment — and run
rather than assumed, because that is what step 4's gate asked for and
because a comment edit inside a table that seeds every RNG stream is
exactly the kind of "obviously safe" change worth spending an oracle on.

## 8. Docs — every teaching site, derived rather than assumed

Swept for `sim check` across `README.md`, `docs/`, `demos/`, `bin/` and
the Makefile:

* **`docs/cli.md`** — regenerated from the spec (`make cli-doc`).
* **`docs/consuming-ground-truth.md`** — the real teaching site, and it
  had documented the DEFECT as settled behavior. Its worked example is
  the ed-tuesday pipe verbatim, and its prose said `sim check` "has no
  flags at all, so it cannot know what `--config` produced the log" and
  advised readers to *trust the run's own self-check over a piped `sim
  check`*. Rewritten, not appended to: the example gains the flag, the
  measured 115 stays as what the starvation costs, and the standing
  advice now survives only for `:order-profiles`, where it is still the
  best on offer.
* **`README.md` teaches no `sim check` pipe at all.** Its ed-tuesday
  invocation is a bare `--format ground-truth` render with no check
  stage. Reported rather than edited — the prompt anticipated a
  front-door pipe here, and there is none to widen. Inventing one would
  be unearned specificity.
* `docs/formats.md`, `docs/glossary.md`, `docs/operators.md` and
  `docs/README.md` mention the verb but state nothing about its config
  behavior, so none needed a change.
* The mutation-loop examples in `consuming-ground-truth.md` run
  **clinic-decade**, which overrides none of `:facility`,
  `:warm-up-seconds` or `:order-profiles` — checked, not assumed — so
  their "exits 0" claim was already honest and stays as written.

## 9. What this session deliberately did NOT do

* **No `sim-check.interface` widening**, and none was needed — Q11(a)
  untouched, the step-1 STOP not fired.
* **No finding-vocabulary change**, and no `:expected-findings`
  registration cross-check: that is the piece Q11(a) blocks, and it is
  now its own live row rather than a sentence inside a closed one.
* **No `:order-profiles` threading** — see (D2). Half-closing it would
  have been the worse outcome, not the smaller one.
* **No `engine/run`, emitter or `fold/apply-events` edit** beyond the
  one comment line Q13(a) licensed.
* **No catalog-wide gate.** Buildable now, and rowed.

## 10. Roadmap (Q15(a))

`roadmap.md#event-stream-mutation` (P6) rotated to `## Done`, closing at
`cfea631` with the narrative Q15(a) named — catalog at twelve, the loop
and its set-equality contract, the three ADR-0176 corrections the ledger
forced, and the three gap kinds kept apart.

**Live follow-ons stayed live, and one is new.** The two things P6 still
owed had no row of their own — they existed only as sentences inside the
row being closed. Closing P6 without giving them one would have retired
live work by prose. New row `roadmap.md#event-mutation-catalog-gate`
(PRIORITY 6) carries both: the catalog-wide gate, and the
`:expected-findings` vocabulary check, marked BLOCKED on a Q11(a)
re-ruling rather than merely unscheduled.
`roadmap.md#referential-corpus-population` (the 14 population-gapped
cells) is untouched.

**Also closed, beyond the prompt's literal instruction:**
`roadmap.md#sim-check-takes-no-facility-config` — the row describing the
very defect this session fixed. Leaving it OPEN would have left a false
claim in the tree. Flagged here because it is a scope call, not a silent
one.

## 11. The fence the prompt did not anticipate — and the ruling taken

**`ehrt.sim.interface` is permanently frozen by AR-M4-3** (ADR-0043),
and `ehrt.sim.interface-surface-test` enforces it by comparing the live
`ns-publics` surface against a committed baseline literal. Adding
`check-command` to the façade turned that gate red — the only failing
assertion in the whole suite, with the oracle already IDENTICAL and the
pipe already working end to end.

**Step 1 answered the question it was asked, and the question it was not
asked went unasked.** The prompt's STOP was scoped to
`ehrt.sim-check.interface`, and that seam genuinely needed no widening.
`ehrt.sim.interface` is a *different* frozen surface, carrying its own
standing ruling from a different arc, and Q14(a) licensed "the check CLI
surface" rather than this one. The step-1 derivation should have swept
for a frozen-surface gate on **every** seam the thread crosses, not only
the one the prompt named. It did not, and the gate caught it instead —
which is the gate working, but it is cheaper to know at step 1 than at
the final suite run.

**Escalated rather than absorbed.** The session stopped and put the
question to the author, because the alternative was to move a literal
labelled "permanently frozen" on the strength of a ruling about a
different surface. Two things made it a real question rather than a
formality:

* **AR-M4-3's own escalation clause names THINNING** — "any future
  thinning of the façade itself is a SEPARATE, explicit author-ruled
  decision — never a side effect of some other session's own work" — and
  its stated harm model is `corpus` depending on the façade's stability
  in-process (ADR-0012). An addition cannot break that.
* **The gate is deliberately stricter than the ruling.** It is an
  equality check, so additions trip it too, and its own failure message
  says a deliberate author-ruled widening should update the baseline.
  Strictness there is a feature: it is what made this a decision instead
  of a side effect.

**Ruled: take the widening, update the baseline.** So `check-command`
(arity 2) is now in the baseline literal with a dated citation, and
AR-M4-3 carries a dated amendment where a reader actually meets the
ruling rather than only in this record. It is an ADDITION and only an
addition — nothing renamed, removed or re-arited — and the five vars
`corpus` actually calls (`run-command`, `check-all`,
`identifiers-command`, `git-sha`, `version`) are byte-identical.

**What the alternative would have cost**, recorded because it was
weighed rather than dismissed: `merge-config-file` is sim-internal and
reachable only through this façade, so keeping the façade untouched
meant a second config-read implementation in `corpus.sim-adapter`. That
forfeits the single-source rejection property — `sim run`,
`sim identifiers` and `sim check` all answering a bad `--config` with
one `:config-not-found` — and drops the `:did-you-mean` sibling hint,
so the two verbs would have answered the same typo'd path differently.
Three verbs drifting apart on the same file is precisely the shape
ADR-0176 spent a commit collapsing for this verb's three stdin
rejections.

## 12. Close
