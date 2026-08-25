# 2026-08-25 — arc 1b: the RNG stream partition, migrated

ADR-0171 executed. The author ruled on 2026-08-25: **A1 B1 C1 D1 E1 F1,
and LOCALITY option (a)** — every section-4 recommendation, plus the
locality ruling section 3 asked for without lettering. Ceremony mode:
R30 standing default (commit and push at each checkpoint, unattended);
no tag (de-scaffold ruling, 2026-08-25). Prompt archived at
[`../prompts/2026-08-25-arc-1b-stream-partition-migration.md`](../prompts/2026-08-25-arc-1b-stream-partition-migration.md).
Base `97f22fd`.

## Preflight

`bin/preflight` exited **1**, one finding, disclosed rather than passed
over: *"a red (completed, non-success) run appears among the last five"*
— `e189418c`, the de-scaffold commit CI reddened, already fixed by
`6106b775` and green at the three commits since. Every other check OK:
edit root not under `/mnt/`, `core.fileMode` true, tree clean including
untracked, local HEAD == `origin/main`, HEAD not tagged `stable-*`
(disclosed, and no tag is owed).

## The families and their tags

`stream-seed(family, id) = (mix64 (mix64 master family-tag) id-tag)`,
using `engine.clj`'s own `mix64` unchanged (ruling A1). Tags are a
compile-time constant table, never `(hash keyword)`.

| family | tag | id-tag | draw call sites (live tree) |
|---|---|---|---|
| `:patient` | 1 | arrival ordinal | **8** |
| `:person` | 2 | — | **0** (arc 2's; declared so arc 2 adds rows, not a family) |
| `:world` | 3 | 0 | **7** |
| `:facility` | 4 | 0 | **3** |
| `:emission` | 5 | 0 | **1** |

Counted from the tree at HEAD, not from the ADR:

* **`:patient` (8)** — `assign-pathway` (`engine.clj:1813`),
  `churn/inject` (`:1819`), `assign-module` (`:1835`),
  `sim-model/persona` (`:493`), `patient-simulator/run-module` (`:500`,
  the whole GMF walk — 21 further draw sites inside
  `gmf_interpreter.clj`, all reached only through this one call),
  `:delay`'s `rand-int-in` (`:592`), `:order`'s turnaround (`:879`),
  `sample-analyte-value` (`:882`).
* **`:world` (7)** — arrivals (`:1806`), `allocate` ×4 (`:561`
  `:admission`, `:599` `:transfer`, `:651` inside `bed-ready-location`,
  `:781` `:transfer-in-error`), `uniform-choice` ×2 (`:814` bed-swap
  partner, `:843` merge partner).
* **`:facility` (3)** — `materialize-providers` (`:1797`),
  `choose-attending` (`:565`), `:outpatient-visit`'s provider pick
  (`:945`).
* **`:emission` (1)** — `plan-latency`'s stream (`sim/run.clj`), which
  used the master seed VERBATIM until ruling C1.

`decide`'s first argument is now a stream MAP. `engine/one-stream`
collapses every family onto one `Random` for a caller with no run behind
it, which is exactly pre-partition behaviour — the 85 unit `decide` call
sites in `engine_test`/`emit_hl7_test` use it and are byte-unchanged in
outcome.

## What re-pinned

Everything ADR-0171 section 3 listed, plus four re-derivations the
reshuffle forced that section 3 did not anticipate.

**On the list.**

| artifact | before → after |
|---|---|
| `arc0_gated_seed_202_ed_tuesday.edn` | 407 → 393 events; `584a5f01…` → `469a26d7…` |
| `arc0_gated_seed_424242_clinic_decade.edn` | 343 → 343 events; `793910e0…` → `5386957e…` |
| `arc0_gated_seed_5_clinic_decade.edn` | 363 → 363 events; `dd0beff7…` → `03201649…` |
| `arc0_gated_adhd_seed_2.edn` → `…_130.edn` | see below; `34f5faf0…` → `e6f44fe8…` |
| `pinned_seed_42_patients_5.edn` | third deliberate regeneration, header rewritten |
| reinstating-cancel witness (`run_test`) | 10 → **9**, still `pos?` |
| cited-end witnesses (`run_test`) | **1 and 1, unchanged** — preserved by the seed re-point |
| `sim-v2-gate-baseline.edn` | 44 → 53 messages, all `:pass`, no verdict change |
| `sim-v2-full-capability-baseline.edn` | **did NOT move** — see findings |
| `make traces` | 14 captures + 1 command + all 6 READMEs |
| `make event-schema-examples` | `event-examples.edn`, and `docs/formats.md` with it |
| 35 regression-oracle roots | 32 differ, 3 identical — see findings |

**Not on the list, and why each was unavoidable.**

1. **`:adhd-seed-2` → `:adhd-seed-130`.** Post-partition, seed 2 over ten
   patients produces ten `:registered` events and nothing else. The
   straddle it carried — one patient whose ADHD care plan and Ritalin
   order both fall in history phase with both ends landing in horizon —
   is a knife-edge, and the reshuffle took it. Both cited-end witnesses
   (`run_test:930`, `:972`) would have gone vacuous at once. This is
   *precisely* the failure repo review 5 predicted for this run
   (`2026-08-25-repo-review-findings.md`, L1-7: "a reshuffle in
   adhd-seed-2 takes both end types dark at once"). Replacement found
   ADR-0165's own way — a seed sweep under the LIVE engine filtering for
   at least one CITED `:medication-end` and one CITED `:care-plan-end`:
   seeds 0–399 at ten patients yield exactly **four** (130, 158, 233,
   331), each with the identical 12-event, one-of-each shape seed 2 used
   to have. 130 is the smallest and is taken for that reason alone.
2. **`check_test`'s mutation fixture, seed 27 → 18.** Seed 27 now yields
   ONE merge and ONE distinct double-occupied bed, so
   `the-mutations-actually-make-all-six-invariants-fire`'s interleaving
   assertion went red rather than the mutation quietly going inert —
   the deftest doing to itself what it exists to do to the defspec
   above it. Swept seeds 0–119 under the live engine for a run that is
   self-check clean, not exhausted, fires all six mutations, and carries
   >1 merge, >1 double-occupied bed and >1 zombie patient-id: **38 of
   120 qualify**. Seed 18 is taken because its shape matches the old
   fixture's documented character — 3 merges, 6 cancels, 5 transfers,
   195 events against the old 196.
3. **`vendored_veteran_self_harm` gate seeds `[20260802 1]` → `[11 42]`.**
   That module's clinical content is genuinely rare: of seventeen seeds
   swept at 300 patients, **three** produce it (11, 42, 202); the other
   fourteen produce `#{:registered}`. Both old gate seeds happened to
   land on content and no longer do. Not a coverage regression — the
   same sweep shows the same rate, and both replacements are check-all
   clean with no gate flag, exactly as AR-VB4-1 requires.
4. **`vendored_dermatitis`, 20260802 → 42.** The converse case: nine of
   ten seeds swept produce real content, and 20260802 became the single
   unlucky one. 42 is the RICHEST of the nine — seven event kinds
   including both the medication and care-plan pairs, six rendered
   messages — so the Observation-submodule claim is exercised further
   than before, not merely restored.

**Three counters re-pinned MEASURED, two of them one seed from vacuity.**
`vendored_veteran_prostate_cancer` `{20260802 2, 1 0, 42 0}` → `{2 1, 1
0, 42 0}`; `vendored_veteran_ptsd` `{20260802 0, 1 1, 42 0}` →
`{20260802 0, 3 1, 42 0}`; `vendored_colorectal` `{20260802 2, 1 3, 42
3}` → `{20260802 0, 1 0, 42 2}`; `vendored_injuries`'s auto-close
counter `5` → `3`. The first two would have become ALL-ZERO pins, which
prove a counter is READ and never that it can COUNT. All three straddle
gates now carry an explicit `(pos? (reduce + (vals …)))` assertion so
the next reshuffle cannot slide them into vacuity in silence.

**`demos/traces/boarding-transfer` needed its command changed.** At
`--arrival-gap 20` the reshuffled seed 1 now exits
`:capacity-exhausted` on Renal — the fifth rung the ladder deliberately
does not have — so `make traces` could not run at all. Widening the gap
to 22 is the smallest change that keeps the demo's subject intact, and
it holds it level: 14 bed-ready transfers and 9 Emergency-surge
boardings in 89 events, against the pre-partition capture's 15 and 9 in
90. All six trace READMEs were then re-derived token by token against
their own regenerated captures (a scripted sweep asserts every
`PID-…`/`MRN…`/`:t N`/bed/timestamp token in each README resolves
inside that directory's own committed artifacts; it is clean).

## The oracle

```
bin/regression-oracle 97f22fd HEAD --declared-digest-change
```
Soundness check: *"IDENTICAL outside the leading docstring — proceeding."*
Verdict line: **`DIFFERS: digests diverge between 97f22fd and HEAD`**,
exit 1 — the DECLARED outcome for a draw-affecting migration, not a
finding.

**32 of 35 roots differ. 3 are identical, and they are the right 3.**
`appendicitis.edn`, `sore-throat.edn`, `ear-infections.edn` — exactly
`digest.clj`'s own three INTERPRETER-LAYER batches, which drive
`patient-simulator` module walks directly from the oracle's own mixer
`Random`s and never call `engine/run`. ADR-0171 section 1e ruled those
out of scope by name ("the regression oracle's own fixture harness, not
the run path"). All **32 ENGINE-LAYER pairs** moved. See the finding
below: the session prompt's "all 35 roots MUST differ" is not a claim
the tree can satisfy.

## The locality witness

`perturbing-one-patients-own-draws-moves-only-that-patient` (RED-first)
and `mutating-one-patients-stream-seed-moves-only-that-patient`
(born green, disclosed). Config: seed 424242, 8 patients, arrival-gap
45, `crowded-facility` — one licensed Renal bed, so
`bed-ready-location` actually fires (3 bed-ready transfers in the
baseline run).

* **PATIENT-SCOPED moved set: exactly `#{3}`** — the perturbed ordinal
  and nobody else, under both perturbations.
* **WHOLE-EVENT moved set: `#{3 6 7}` — 3 of 8**, pinned. The extra two
  are the disclosed WORLD coupling and nothing more.
* **The fields that differ on those two: exactly `#{:from :location}`**,
  both in `run-scoped-event-fields`, both written by the WORLD family.
  Pinned as a set, so an exclusion that stopped carrying weight, or a
  field the ruling never excluded, is a red either way.
* **RED on `97f22fd`**: the PATIENT-scoped moved set was `#{3 4 5}` —
  the perturbed ordinal AND every ordinal still drawing after it — and
  the fields differing on other patients were `#{:bed-ready :event
  :forced :from :home-ward :location :placement :t}`. Other patients'
  `:event`, `:t` and `:bed-ready` moving is what proves the
  pre-partition coupling was never only about beds.

## Gates

| gate | result |
|---|---|
| `clojure -M:poly check` | OK |
| `make test` (unpiped, `MAKE_EXIT` captured) | **MAKE_EXIT=0**, 0 failures, 0 errors |
| `make integration` (Makefile:52 — `make test` skips that tier) | **MAKE_EXIT=0**, 0 `FAIL:` lines, tree clean |
| `bin/regression-oracle` | DIFFERS, declared, 32/35 |

Machine sampled at the moment of the run from the WINDOWS side (Linux
idle lies about WSL2 contention): `\Processor(_Total)\% Processor Time`
= 18.5% then 9.9% at the first `make test`, 4.7% then 3.7% at the final
green pair.

**`make integration` cost four rounds, and every red it found was real.**
It is the tier `make test` skips, and it caught three things nothing
else could — register row W-1 (repo review 4) paying off exactly as
written:

1. `demo-exerciser-ed-tuesday` — `expected 34 ':verified true' entries,
   got 32`. The exerciser asserts a narrative README's own witnessed
   numbers; the reshuffle moved them. README re-derived (`3efe23f`).
2. `demo-exerciser-clinic-decade` — `not every board-snapshot line
   contains "inpatients: 0"`. The scenario gained one sepsis ED
   admission across its decade. README AND the gate's own claim SHAPE
   re-derived, universal → counted split (`9c4324d`).
3. `usecase-custom-emitter` — the seed-42 jsonl fixture no longer
   byte-matched. Same run `pinned_seed_42_patients_5.edn` pins; bed
   choices and dwell times moved, record shape did not (`e319760`).

Plus one self-inflicted round: the exercisers' own tree-clean
postcondition (ADR-0005) fires on any uncommitted file, so a run
started with the close-out still in the working tree fails on that
rather than on anything it measured.

**Suite reconciliation against `97f22fd`**, measured by running the same
target in a worktree at that commit rather than estimated:

| | tests | assertions | failures |
|---|---|---|---|
| `97f22fd` | 4,046 | 18,066 | 0 |
| HEAD | 4,056 | 18,172 | 0 |
| delta | **+10** | **+106** | 0 |

`+10` reconciles exactly: **5 new deftests, each namespace run twice**
by `poly test :all` (engine-test 79 → 83 deftests, run-test 32 → 33).
Three are the RED trio ADR-0171 section 3 owes; two are the born-green
pair the partition itself makes possible. The `+106` assertions
reconcile per namespace with no residue:

| namespace | Δ assertions (×2 runs) | why |
|---|---|---|
| `sim-engine.engine-test` | +24 (+48) | the four new deftests |
| `sim.run-test` | +20 (+40) | the continuity deftest |
| `sim.manifest-test` | +4 (+8) | ruling D1's marker assertions |
| `vendored-colorectal` / `-veteran-ptsd` / `-veteran-prostate-cancer` | +1 each (+6) | the new `pos?` vacuity guards |
| `sim-check.event-conformance-test` | +1 (+2) | data-driven `doseq` over a sampled ground truth that gained one event |
| `sim.version-test` | +1 (+2) | **measurement artifact, not a tree change**: `generator-sha256-is-not-the-all-zero-placeholder-when-git-is-present` is guarded by `(when (version/git-sha) …)`, and the baseline ran in a git WORKTREE where that read differs |

`R-defspec-seed-policy` honoured: **no defspec was re-pinned.** The three
seeded `defspec`s (`engine_test.clj:1476`/`:1598`, `check_test.clj:958`,
all `20260825`) and arc 0's naive-vs-fast equivalence properties are
green untouched, which is the correct outcome — a `test.check` seed pins
generator SAMPLING, not generator OUTPUT, and both implementations move
together under the partition.

## Findings

1. **The prompt's "all 35 oracle roots MUST differ" is not satisfiable.**
   `digest.clj` is 32 engine-layer pairs plus 3 interpreter-layer
   batches; the latter never touch `engine/run`. 32/35 with those exact
   3 identical is the correct result, and it is stronger evidence than
   35/35 would have been — it shows the partition moved the run path and
   only the run path.
2. **`sim-v2-full-capability-baseline.edn` is not a mover**, against
   ADR-0171 section 3's "definite movers" list. Its 210 messages
   reshuffled in CONTENT, but the report records file paths, verdicts
   and finding codes only, and all 210 still pass at the same 210 paths,
   so the report VALUE is `=` to the committed one. Left untouched and
   the reason recorded in its sibling's header.
3. **ADR-0171 section 3's named locality test could not be red for the
   reason section 3 gives.** A stream SEED is derived from the master
   seed and the arrival ordinal alone, so no config perturbs one
   patient's seed; the only route is `with-redefs` on `engine/stream`, a
   var the partition itself introduces, so that test could only have
   been red by failing to COMPILE — the one red reason section 3 rules
   out. Resolved by landing both: a config-level perturbation (an
   explicit one-ordinal `:pathways` override, which `assign-pathway`'s
   fixed-consumption law keeps to that patient's own draws) red-first
   under its own name, and the ADR's named test born green beside it,
   disclosed in its own docstring.
4. **Two stale citations ADR-0171 section 1f recorded and could not fix
   are fixed here**, because the migration touches both files anyway:
   `simulator-architecture.md`'s `AR-RL2-2` → `rulings.md#R-measure-
   claimed-population` (ADR-0093), and `emit_hl7.clj:961`'s
   `engine.clj:1165-1183` → a BY-NAME citation of `assign-pathway`,
   since that line has now moved twice (arc 0, then this arc) — the
   exact species ADR-0170 named.
5. **`make integration` caught what `make test` structurally could
   not, and it was a narrative document.**
   `bin/demo-exerciser-ed-tuesday` asserts
   `demos/scenarios/ed-tuesday/README.md`'s own witnessed numbers, and
   the reshuffle moved them: `expected 34 ':verified true' entries, got
   32`. `make test` skips that tier (Makefile:49), so nothing before
   the deliberate integration run could have seen it. The README was
   re-derived against its own regenerated run — 375 events, 275
   messages, 33 snapshots, 32 batches, and a phantom-re-admission
   anecdote re-derived on a new patient (MRN000013 Walker → MRN000020
   Jones) — and `docs/manual/assets/straddle-timeline.svg` was redrawn
   by hand for the same reason. This is register row W-1 (repo review
   4) paying off exactly as written: a gate can land unexecuted under
   `make test`.
6. **Five hand-owned narrative documents still quote pre-partition sim
   output and are NOT fixed here** — outside ADR-0171 section 3's list,
   ungated, and re-deriving five narratives at the tail of this
   migration would trade a bounded, nameable staleness for an unbounded
   risk of new error. Censused by token count:
   `docs/manual/05-batch-delivery.md` (10),
   `docs/manual/01-what-this-is.md` (8),
   `docs/manual/04-time-on-the-wire.md` (4),
   `docs/manual/00-front.md` (1),
   `docs/use-cases/supply-batch-straddling-traffic.md` (1) — 24 tokens
   over 5 files. Rowed in `roadmap.md#post-partition-narrative-refresh`.
   `demos/scenarios/ed-tuesday/README.md` (19) WAS fixed, because its
   own gate went red. `docs/formats.md` (50 tokens) is GENERATED and was
   regenerated; `notes/adr/**` and `.agents/session-records/**` are
   historical records and correctly left alone.
7. **Nine other live documents asserted "the run's single seeded RNG"**
   and were swept in the same commit (`sim-theory.md`,
   `event-sourcing.md` ×2, `gmf-interpreter.md` ×3,
   `trajectory-computation.md`, `engine.clj` ×2). Every remaining
   occurrence in the tree is now an explicit historical reference
   ("…before that arc"), verified by grep.
8. **`gt-emitters.svg`'s freshness witness was bumped in this session
   rather than after CI** — `e189418c` → `75cde83f` — with the review
   the row asks for: both hunks in `simulator-architecture.md` land in
   sections 2 and 3, section 4 begins at line 191 and is byte-identical,
   and the trigger names section 4's EQUATIONS. Not stale. The previous
   review's own lesson (this tripwire reads `git log -1` and cannot see
   an uncommitted edit, so local `make test` stays green over a tree CI
   reddens) is why the bump rides as the migration's immediate
   successor, pushed together. `straddle-timeline.svg`'s row was bumped
   the same way one commit later, and there the trigger genuinely FIRED
   — the asset was redrawn rather than marked `:stale`, because the
   redraw is four text values and the depicted fact did not move.

## HEAD landed

| commit | what |
|---|---|
| `6cdfee6` | ADR-0171 flipped to Accepted, the seven rulings quoted where they land; `notes/ADRs.md` regenerated |
| `979ffe7` | RED — the locality, witness-count and continuity gates, all three failing on `97f22fd` for the reason ADR-0171 section 3 gives |
| `75cde83` | the migration: partition, from = to skip, every re-pin, all four re-derivations, the docstring sweep |
| `3efe23f` | ed-tuesday re-derived against its own post-partition run, plus the straddle-timeline redraw and `gt-emitters.svg`'s witness bump |
| `67b985e` | close-out: this record, the archived prompt, the roadmap rows, `straddle-timeline.svg`'s witness bump, regenerated indexes |
| `1c160e4` | the two live surfaces citing the removed `roadmap.md#stream-partition-design` slug |
| `9c4324d` | clinic-decade gains an inpatient, and its gate learns to count a split instead of asserting a universal |
| `4f70ede` | `state-derived.md` regenerated after the roadmap and AGENTS.md edits |
| `e319760` | the custom-emitter's seed-42 encounter fixture re-pinned |
| `HEAD` | this record's own gate results, written after both tiers came back green |

Pushed as one range, `97f22fd..f392a11`; the red-first commit rides with
its green successor (`rulings.md#R-red-pushed-with-green`). No tag paid.

`bin/post-push-verify 97f22fd f392a11`, its three checks:
`origin/main` matches the tip, every commit message in the range is
pure ASCII, and the CI run at the tip was reported once
(`32885636423`).

**CI at the pushed tip: run `32885636423`, `completed` /
`success`** — the close marker under the de-scaffold rules
(`rulings.md#R-session-verifies-ci-via-gh`, retired as a TAG condition,
kept as the marker), watched to conclusion rather than reported
in-progress.

