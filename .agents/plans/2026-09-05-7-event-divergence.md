# The 7-event divergence, derived: a VERSION delta, not a path divergence

**Question.** `demos/scenarios/dense-7500/README.md:153` reports
**167,190** events for `corpus generate sim` at `--seed 20260824
--patients 7500 --churn` against `config.edn` (measured 2026-09-04).
`.agents/plans/2026-09-05-performance-measurement/measurements.md:164-178`
reports **167,197** for `sim run --format ground-truth` at `3114dbfe`
on the same seed, the same flags and a `cmp`-identical config, and left
it unchased under `rulings.md#R-measure-first`. Two readings were
available and they are not the same kind of thing:

1. **Path divergence.** The two invocations differ at one tip — which
   would break arc 4's emission-does-not-move-ground-truth law
   (`notes/adr/0175-arc-4-emission-add-ons.md` section 4), since both
   paths reach `sim/run-command` in-process through
   `components/corpus/src/ehrt/corpus/sim_adapter.clj:36-68` and the
   only thing the generate path adds is emission.
2. **Version delta.** The README's figure predates
   `notes/adr/0179-merge-transfer-semantics.md`, whose R-bed releases
   the absorbed record's bed at the instant of a merge.

`R-derive-first` (author, this session): *"diff the two paths at ONE
tip before any claim about either."* That is step 1, and it settles
which of the two questions is even live.

## Verdict

**VERSION DELTA.** The two paths are byte-identical at one tip; the
whole 7-event difference is ADR-0179.

## 1. One tip, one JVM each — the two paths do not diverge

Clone tip `28bf36b4`, `bin/preflight` clean (one PENDING CI run
disclosed, AR-CI-4). Seed 20260824, `--patients 7500 --churn`,
`demos/scenarios/dense-7500/config.edn`, one JVM per invocation:

| | invocation | artefact | bytes | sha256 | `bin/event-census` |
|---|---|---|---|---|---|
| **A** | `sim run --format ground-truth` | stdout | 67,324,581 | `3018299a0e0299c40ba73580793c8135674e8f988f9c332d18eedd45b70d3bd3` | **167,197 events, 28 kinds** |
| **B** | `corpus generate sim --out-dir` | `events.edn` | 67,324,580 | `096959857076f22901fa92a9fd71cca0edbc469b109118b6a469fcf0b49dc2b2` | **167,197 events, 28 kinds** |

Both exit 0. `cmp` reports the files differ, and the difference is
**exactly one byte**: A ends `}]\n`, B ends `e}]`. A's first 67,324,580
bytes hash to `096959857076f22901fa92a9fd71cca0edbc469b109118b6a469fcf0b49dc2b2`
— B's whole-file digest. **A is B plus a trailing newline**, which is
the CLI writing a line to stdout against
`components/corpus/src/ehrt/corpus/generators.clj:241`'s
`(spit ... (pr-str ground-truth))`. That is the documented envelope
difference this step allowed for, and it is the only one.

So the law holds. Emission does not move ground truth on this
configuration either, and the divergence recorded in `measurements.md`
was never between the two paths.

## 2. One path, two versions — the delta is ADR-0179, entirely

Regenerated at `007deea6` — the commit immediately before ADR-0179's
first code commit — in a **second clone** (`~/src/ehr-7event-007deea6`,
dissociated from its source's object store, not a worktree), with that
checkout's own `config.edn` `cmp`-identical to the tip's. Between
`007deea6` and `28bf36b4` the only `components/` or `bases/` diff is
ADR-0179's own six files plus their tests; `demos/scenarios/dense-7500/`
is untouched.

    A007  sha256 c5196fcfbd1e26271fbc38efce4279a1d09875377df112279eee6cc91c8ce275
          67,295,311 bytes, 167,190 events, 28 kinds

**167,190 — the README's own figure, reproduced to the event**, and
reproduced from the OTHER path (`sim run`, not `corpus generate`),
which is a second witness to section 1's finding. The README's basis is
not lost after all in the sense that matters: it is re-derivable at the
commit it was taken before.

### The per-event diff

Comparing `A007` (167,190) against `A` (167,197) element by element:

| kind | 007deea6 | 28bf36b4 | delta |
|---|---:|---:|---:|
| `:result-available` | 10,253 | 10,274 | **+21** |
| `:cancel-transfer` | 2,125 | 2,146 | **+21** |
| `:transfer` | 7,338 | 7,340 | **+2** |
| `:bed-status-change` | 41,415 | 41,399 | **-16** |
| `:step-rejected` | 200 | 179 | **-21** |
| every other kind (23 of them) | — | — | **0** |

`+21 +21 +2 -16 -21 = +7`. No kind appears or disappears; the
vocabulary is 28 on both sides.

**165,918 of the 167,190 compared positions differ, and the first is
index 1,180** — `007deea6` holds a `:procedure` at `t=20040` where
`28bf36b4` holds a `:result-available` at `t=19980`. A cascade of that
size from a seven-event delta is the same shape ADR-0179 measured on
`encounter-horizon` (110 of 173 positions, from three added events):
one differently-allocated bed re-indexes every later bed id, and every
event carrying a `:location` moves with it.

### Both of ADR-0179's executed rulings fire here, and R-queue is the surprise

**R-bed** accounts for the four allocation-side kinds. The 68 merges
this scenario's own census records as absorbing a bed-holder
(`measurements.md`'s scenario-census table, `a7500-persons` row) now
release those beds, so 21 transfer steps that used to be rejected for
want of a bed are admitted instead (`:step-rejected` -21,
`:cancel-transfer` +21), two more complete as real placements
(`:transfer` +2), and the bed-status ladder shortens by 16 rungs.

**R-queue is NOT blind on this corpus, and ADR-0179 said every corpus
it could see was blind.** That ADR's own words: *"The oracle is BLIND
to R-queue. Zero absorbed patient-ids had a follow-up pending, in every
corpus this repository gates — `:order-placed` and `:result-available`
are equinumerous in both fixture runs."* `dense-7500` is not a gated
corpus — `bin/demo-exerciser-dense-7500` is wired into neither
`make test` nor `make integration` — and it is the first committed
configuration in this tree where the population is non-empty:

|  | `007deea6` | `28bf36b4` |
|---|---:|---:|
| `:order-placed` | 10,274 | 10,274 |
| `:result-available` | **10,253** | **10,274** |
| results whose cited order names a DIFFERENT subject | **0** | **21** |
| ...of those, resolved by a `:merge` at `t` ≤ the result's `t`, survivor = the result's subject | 0 | **21** |

All 21 satisfy R-inv's own condition exactly, and the counts become
equinumerous. Before the fix this scenario lost 21 already-drawn
results to `run.clj:1289`; after it, it loses none. The first three:

    result t=19980 subject=PID-000239-3ce5b660 <- order idx=759  t=15420 subject=PID-000209-68309928 ; merge t=17700 survivor=PID-000239-3ce5b660
    result t=24960 subject=PID-000202-e0324624 <- order idx=1338 t=21600 subject=PID-000300-d92ddb85 ; merge t=22620 survivor=PID-000202-e0324624
    result t=25140 subject=PID-000076-e0d22357 <- order idx=1305 t=21180 subject=PID-000162-81b3143b ; merge t=24780 survivor=PID-000076-e0d22357

This does not contradict ADR-0179 — its claim was scoped to the corpora
the oracle and the downstream fixture cover, and it is true there. It
does mean the ADR's declared-oracle-change list was written against a
population that did not include this scenario, which is what section 3
of this session repairs.

**Note for a reader of `measurements.md`'s scenario census.** That
table's *after merge* column reads **0** on `a7500-persons` — the same
167,197-event log — and that is not in tension with the 21 above. The
census classifies a result by comparing its `:location` against its
SUBJECT's state as it lands; R-queue rewrites the subject to the
survivor, so a carried result is a result about a patient who was never
merged. The two instruments are asking different questions.

## 3. The other three cells, baselined rather than attributed

Section 2 settles the all-keys 7,500 cell. The scenario has three more,
and step 3's re-measurement moves all of them, so each got its own
`007deea6` baseline — same second clone, same `sim run --format
ground-truth`, counts only:

| cell | README, 2026-09-04 | `007deea6`, re-derived | `28bf36b4` | delta |
|---|---:|---:|---:|---:|
| `config.edn` @ 7,500 | 167,190 | **167,190** | 167,197 | **+7** |
| `config-nobed.edn` @ 7,500 | 125,825 | **125,825** | 125,642 | **-183** |
| `config-bare.edn` @ 7,500 | 100,884 | **100,884** | 100,868 | **-16** |
| `config.edn` @ 750 | 33,303 | **33,303** | 33,306 | **+3** |

**All four reproduce the README's own figures to the event.** That
makes every delta a derivation rather than an attribution, and it
settles a question the `963902d..007deea6` diff raises: ADR-0178
(`:window-close-t` absent, never nil) landed between the 2026-09-04
measurement and `007deea6`, touching `decide.clj` and the event schema,
and was a live candidate for part of the movement. It moved no count on
any of the four cells.

### The deltas do not share a sign, and the small ones are not the small effects

| kind | all-keys 7,500 | less `:bed-cycle` | bare | 750 |
|---|---:|---:|---:|---:|
| `:result-available` | +21 | +1 | **+87** | **+3** |
| `:step-rejected` | -21 | -7 | **-224** | 0 |
| `:cancel-transfer` | +21 | +2 | **+222** | 0 |
| `:transfer` | +2 | -33 | -123 | 0 |
| `:bed-status-change` | -16 | — | — | 0 |
| net | **+7** | **-183** | **-16** | **+3** |

(A dash means the kind does not exist in that cell at all -- `:bed-status-change`
is the bed cycle's own housekeeping and only `config.edn` turns it on.
The `config-nobed.edn` column has 22 moving kinds of 27, and the bare column
16 of 21; only the five above are shown. The 750 column is complete: it has
exactly ONE moving kind.)

**The 750 cell is the cleanest demonstration of R-queue in the tree.**
`:order-placed` is 1,028 on both sides and nothing else moves at all —
its entire three-event delta is three results that used to be dropped
at `run.clj:1289` and are now carried to the survivor. No bed in that
cell is reallocated in a way that reaches another patient, so R-bed
contributes nothing and R-queue is visible on its own.

**Every one of the four cells becomes order/result-equinumerous**, and
none of them was before:

| cell | `:order-placed` | `:result-available` at `007deea6` | at `28bf36b4` | results recovered |
|---|---:|---:|---:|---:|
| all-keys 7,500 | 10,274 | 10,253 | **10,274** | **21** |
| less `:bed-cycle` | 10,282 | 10,281 (of 10,301 orders) | **10,282** | **20** |
| bare | 12,389 | 12,302 (of 12,372 orders) | **12,389** | **70** |
| 750 | 1,028 | 1,025 | **1,028** | **3** |

The two middle rows need reading with care: `config-nobed.edn` and
`config-bare.edn` also move their ORDER counts (-19 and +17), because
their bed allocations differ and the patients who get admitted differ
with them. The recovered-results column is the order/result deficit
that closes, not a subtraction between the two `:result-available`
columns.

**`config-nobed.edn` LOSES 183 events on balance**, which is the one
result that looks wrong at a glance. With `:bed-cycle` off there is no
housekeeping ladder for a freed bed to shorten, so R-bed's effect
arrives entirely as different allocations: 7 fewer admissions, 33 fewer
transfers, 4 fewer merges and 38 fewer demographic updates, against
which R-queue's single recovered result cannot balance. Read the four
cells as one mechanism reaching four differently-configured scenarios,
not as four independent measurements — the sign of the net is a
property of the config, and only the per-kind columns say what happened.

## What this record does not do

It changes no engine code, proposes none, and takes no position on
whether R-loc (ADR-0179's open question about a carried result's
`:location`) should move. It is a derivation, and the docs consequence
— re-measuring the scenario at the tip — is section 3 of the session
that produced it.

Raw artefacts (`out/7event/`, gitignored, not committed): `A.edn`,
`gen/events.edn`, `A007.edn`, their `/usr/bin/time -v` files, both
censuses and `diff.md`. Re-derivable by the commands quoted above.
