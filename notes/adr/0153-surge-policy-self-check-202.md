## ADR-0153 — the bed-ready transfer obeyed no ladder: seed 202 under `--churn`, diagnosed, repro'd, fixed

**Status:** Accepted (author-directed, autonomous session per R30),
2026-08-18.

### Context

`roadmap.md#surge-policy-self-check-202`, PRIORITY 6, opened by
ADR-0150's own rowing of event-log-census finding S-5
(`.agents/plans/2026-08-16-event-log-census.md`, S-5): *"seed 202 under
`--churn` with the ed-tuesday facility exits `:status :error :category
:self-check-failed`, violation `:surge-only-when-earlier-rungs-
exhausted` at `t 78480`. Reproducible. Found while running the
event-log census, wholly outside that arc and disclosed rather than
pursued. Wanted: a repro test, then the fix."*

Author ruling, 2026-08-18: **"(a) now."** — this row, this session.

The census recorded the seed and the config but not the argv, and the
session prompt fenced a bounded search (12 runs, logged) before the
row's own premise would be declared failed. The search took **one
run**.

### Step 0 — baseline

`bin/preflight` findings, disclosed: last five CI runs on `main` all
green, `c1a40d0` itself green (`2026-08-18T20:28:03Z`); edit root
`/home/mg/src/ehr-testing-tools`, not under `/mnt/`; working tree
clean including untracked; local HEAD `c1a40d0` == `origin/main`; last
stable tag `stable-20260818-sim-theory-edn-hop` @ `c509e462`, **HEAD
untagged and no tag owed at Step 0**.

Baseline `make test`, unpiped, `MAKE_EXIT=0`: **348** zero-failure
blocks / **3,956** tests / **17,730** assertions — reconciling exactly
against ADR-0152 (348 / 3,956 / 17,730). `clojure -M:poly check`
**OK**. Reading sets, taken from the generated `state-derived.md`, all
green: `:corpus` 1801/2045, `:docs` 708/785, `:judge` 895/1000,
`:onboarding` 1392/1530, `:sim` 1247/1405.

**The repro, first attempt.** The census's own `edchurn-*` shape --
`sim run --seed N --patients 100 --churn --config
.../ed-tuesday/config.edn`, from that document's corpus table -- with
the row's seed:

    bin/ehrt sim run --seed 202 --patients 100 --churn \
      --config demos/scenarios/ed-tuesday/config.edn

    exit 2
    {:status :error, :category :self-check-failed,
     :payload {:violations [{:invariant :surge-only-when-earlier-rungs-exhausted,
                             :patient-id "PID-000040-e12ccf6f", :at 78480}]}}

`--format ground-truth` makes no difference; the run refuses to write
a corpus it cannot vouch for (`run.clj`'s `:self-check-failed`
branch), which is correct behaviour if the log is wrong.

**No oracle root runs under `--churn`**, re-derived against the tree
rather than taken from the prompt: `grep -rn churn components/oracle/src`
returns **0 hits**, and all **35** roots in `digest.clj`'s own `roots`
map are module-closure `sim run` shapes with no churn profile.

**The law the invariant encodes**, quoted from
`components/sim/docs/operational-models.md`'s allocation-ladder
section:

> **Allocation ladder.** Placing a patient tries rungs in order, each
> rung's choice among its candidates seeded:
> 1. **Home-ward licensed** — a free bed in the ward the pathway named.
> 2. **Home-ward surge** — a free surge slot in that ward, **once
>    licensed beds are full**.

### Step 1 — diagnosis: H2, and the artifacts that rule out H1 and H3

Instrumented in a throwaway `development/src/dev/scratch*` namespace
(gitignored, deleted at close), never in `src`.

**The offending event, verbatim, before the fix** — index 327 of 405:

    {:event :transfer, :t 78480,
     :home-ward "Renal",
     :location {:ward "Renal", :bed "RENAL-H02", :placement :surge},
     :from {:ward "Emergency", :bed "ED-H09", :placement :surge},
     :placement :surge, :forced false, :bed-ready true,
     :active-mrn "MRN000041", :attending "4044256558", :warm-up false,
     :participants [{:patient-id "PID-000040-e12ccf6f", :role :subject}]}

**`:bed-ready true` is the whole diagnosis.** That key is set at
exactly one construction site — `ehrt.sim-engine.engine`'s `decide
:discharge`, which, when a discharge frees a bed some boarder is
waiting on, emits a *second* event for that *other* patient. That site
never called `sim-model/allocate` at all: it built the transfer by
hand with `:location vacated-location`, carrying the vacated bed's own
`:placement` verbatim. Its `rng` parameter was named `_rng` — bound
and deliberately unused.

**The board at `world-before`, replayed** (bed -> patient-id, 15
occupied):

    CARDIOLOGY-01, ED-H03, ED-H05, ED-H07, ED-H08, ED-H09, ED-H10,
    ED-H12, ED-H13, ED-H14, ED-H15, RENAL-01, RENAL-02, RENAL-03,
    RENAL-H01

    Renal   licensed [01 02 03 04]  FREE: RENAL-04   surge FREE: RENAL-H02
    Emergency licensed []           FREE: six ED surge slots
    Cardiology                      FREE: three licensed, two surge

**RENAL-04 — rung 1 — was free**, and the patient was placed on rung 2.
The invariant is right.

**How rung 1 came to be free with a boarder waiting**, from the log
immediately preceding:

    324 :discharge  t 78060  PID-000073 leaves ED-H03 (Emergency surge)
    325 :transfer   t 78060  PID-000087 (home Emergency, boarding in
                             RENAL-04 by rung 3) is bed-ready-pulled
                             home into ED-H03 -- vacating RENAL-04
    326 :discharge  t 78480  PID-000025 leaves RENAL-H02 (Renal SURGE)
    327 :transfer   t 78480  PID-000040 (home Renal, boarding in
                             ED-H09) is handed RENAL-H02 -- the vacated
                             SURGE slot -- while RENAL-04 stands free

Event 325 is a bed-ready transfer whose own *origin* bed (RENAL-04)
triggers no second bed-ready search: only `decide :discharge` looks for
a waiting boarder. That is what leaves rung 1 free with a rung-4
boarder still waiting, and it is a real gap the fix below does not
close — see Step 4.

**`--churn` is how this seed reaches the state, not a precondition of
the mechanism.** Events 321 and 199 are churn `:bed-swap`s that put
these patients where they are; the 324/325 chain itself is pure
discharge coupling. Under `--churn` a `:cancel-admit` or
`:cancel-transfer` reaches the same state more directly, vacating a
licensed bed outright with no pull — which is the shape the minimal
repro uses.

**H1 (allocator) — ruled out.** `sim-model/allocate` was never called
for this event. Artifact: `:bed-ready true`, set at the one hand-built
construction site; `decide :discharge`'s `_rng`; and `facility.clj`'s
ladder, read rung by rung, ranks rung 1 above rung 2 correctly.

**H3 (checker/replay disagree about the board) — ruled out.** There is
no engine-side board to disagree with: the engine consulted none.
`world-before`'s RENAL-04 vacancy is a true fact about the log — event
325 vacates it, and no event between 325 and 327 refills it. The
checker's rung-2 branch is the right branch (`:home-ward` and
`[:location :ward]` are both `"Renal"`), and `earlier-rungs-exhausted?`
asks exactly what the law's rung 2 asks.

**H2 (engine decide site) — the evidence supports this one, and only
this one.** One defensible reading, so fix-forward with disclosure
rather than STOP (`rulings.md#R-stop-only-on-two-defensible-readings`).

### Step 2 — red

Two tests, both red at `ceedcfd`, green at `885b1c9`.

`ehrt.sim-engine.engine-test/bed-ready-transfer-obeys-the-allocation-
ladder` — the minimal repro, sibling to the two existing surge tests in
`check_test.clj` and to `bed-ready-transfer-scripted-two-patients`
beside it. Three patients against a Renal ward of exactly one licensed
bed and one surge slot, plus an ED to board into: the ladder fills rung
1, rung 2, then boards P3 in ED surge; a `:cancel-admit` frees RENAL-01
with no bed-ready pull of its own; the RENAL-H01 occupant discharges.
It asserts the placement AND runs
`check/surge-only-when-earlier-rungs-exhausted` over the produced log,
so the test fails on the same invariant the seed-202 run fails.

`ehrt.sim.run-test/ed-tuesday-churn-seed-202-self-checks-clean` — the
run level, at the exact reproducing invocation. **Per-push tier, not
integration:** the whole run is ~1s in a warm JVM (measured, twice: 989
ms then 552 ms), and the demo config it reads is tracked content, so
`rulings.md#R-tests-build-own-dirs` is satisfied without a fixture
copy. The integration tier was the wrong home anyway — it gates on
`ehrt artifact fetch` (Synthea, the FHIR validator) and CI's per-push
lane skips it, so a pure-sim regression parked there would not run.

Red output, all five failures:

    FAIL in (bed-ready-transfer-obeys-the-allocation-ladder) (engine_test.clj:297)
    expected: (= {:ward "Renal", :bed "RENAL-01", :placement :licensed} (:location transfer))
      actual: (not (= {:ward "Renal", :bed "RENAL-01", :placement :licensed}
                      {:ward "Renal", :bed "RENAL-H01", :placement :surge}))

    FAIL in (bed-ready-transfer-obeys-the-allocation-ladder) (engine_test.clj:299)
    expected: (= :licensed (:placement transfer))
      actual: (not (= :licensed :surge))

    FAIL in (bed-ready-transfer-obeys-the-allocation-ladder) (engine_test.clj:301)
    expected: (empty? (check/surge-only-when-earlier-rungs-exhausted log one-bed-one-surge-facility))
      actual: (not (empty? ({:invariant :surge-only-when-earlier-rungs-exhausted,
                             :patient-id "P3", :at 40})))

    FAIL in (ed-tuesday-churn-seed-202-self-checks-clean) (run_test.clj:399)
    expected: (result/ok? r)
      actual: (not (result/ok? {:status :error, :category :self-check-failed,
                :payload {:violations [{:invariant :surge-only-when-earlier-rungs-exhausted,
                          :patient-id "PID-000040-e12ccf6f", :at 78480}]}}))

    FAIL in (ed-tuesday-churn-seed-202-self-checks-clean) (run_test.clj:400)
    expected: (seq (:ground-truth (:payload r)))
      actual: (not (seq nil))

### Step 3 — green: one helper, in one file

`components/sim-engine/src/ehrt/sim_engine/engine.clj`, +48/-11, and
nothing else in `src`. One new private `bed-ready-location`, and
`decide :discharge`'s `_rng` becomes `rng`.

The rule it states: **the bed-ready coupling names the bed WITHIN its
rung; it never licenses a rung the ladder would not have reached.** A
vacated *licensed* bed in the boarder's home ward is rung 1, the top
rung, always legal — handed over unchanged, no RNG drawn, byte-identical
behaviour. A vacated *surge* slot is rung 2, legal only once licensed
beds are full; when a home-ward licensed bed is free at that instant,
`allocate` decides instead, drawing its own seeded bed choice the way
every other placement does. `allocate` cannot return `:exhausted`
there: the vacated bed is in the boarder's own home ward by the way
`waiting-id` is chosen, so rung 1 or rung 2 always has a candidate —
and rung 1 is free by the branch we are in, so the result is always a
licensed bed in that same ward.

No invariant was removed from the catalog; `operational-models.md`'s
law sentence is unchanged (the engine now obeys it); no event-schema
change; no `--churn` semantics change.

**READ-BACK — the seed-202 run, before and after:**

| | events | self-check | event at `t 78480` |
|---|---|---|---|
| before (`c1a40d0`) | **405** | `:error :self-check-failed` | `:location {:ward "Renal" :bed "RENAL-H02" :placement :surge}`, `:placement :surge` |
| after (`885b1c9`) | **407** | **`:ok`** | `:location {:ward "Renal" :bed "RENAL-04" :placement :licensed}`, `:placement :licensed` |

The **+2 events** are downstream of the one extra RNG draw this run now
makes at `t 78480`. Declared, not silent.

**Oracle: predicted IDENTICAL, and asserted.**
`bin/regression-oracle c1a40d0 HEAD`, exit 0:
`IDENTICAL: every root's digest matches between c1a40d0 and HEAD`,
`--- declared-digest-change: no (soundness: yes outside ns form) ---`,
all **35** roots. The prediction the prompt carried rested on "no
oracle root runs under `--churn`", which is true but is NOT the reason
this holds — the bed-ready coupling needs no churn to fire, and the
`boarding-transfer` demo proves it fires without it. It holds because
the fix draws no RNG and changes no bytes on the branch every existing
root takes: a vacated LICENSED bed is handed over exactly as before.
Recorded because a right answer for the wrong reason is worth catching
once.

**`demos/traces/`: did NOT move.** `make traces` regenerates every
derived file under it byte-identically (`TRACES_EXIT=0`, `git status`
showing only `engine.clj` modified). The `boarding-transfer` trace is
the one that exercises this coupling directly, and its vacated bed —
`RENAL-04`, licensed — takes the unchanged branch.

**The census's `--churn` corpus shapes, run at both refs and digested**
(`.agents/plans/2026-08-16-event-log-census.md`'s corpus table; a
throwaway worktree at `c1a40d0` for the baseline side):

| shape | before | after | moved? |
|---|---|---|---|
| `edchurn-3` | `eadc6455e53ea97c` 159,468 B | same | no |
| `edchurn-17` | `582d0d1d1a2725d1` 177,568 B | same | no |
| `edchurn-55` | `31be176cade3b104` 173,542 B | same | no |
| `edchurn-777` | `8b08276aa886c84a` 176,420 B | same | no |
| `edchurn-1234` | `34242d11cd4dbe04` 163,316 B | same | no |
| `warmup-3` (`--warm-up-seconds 3600`) | `272435b20240d065` 159,460 B | same | no |
| `ed-tuesday` demo (`corpus generate sim --seed 20260811 …`) | 285 files | 285 files | **see below** |
| `edchurn-202` | exit 2, 173 B error payload | exit 0, 179,078 B ground truth | **yes — this is the fix** |

The ed-tuesday demo tree digest moves, and the movement is **not**
this fix: 284 of its 285 files are byte-identical, and the one that
differs is `manifest.edn`'s `:generator :sha256`, which
`ehrt.sim.version/generator-sha256` documents as SHA-256 of the
producing commit id — a provenance stamp, never an engine output.
Proved rather than assumed: the "after" side ran at `6e100da` (this
fix, before a message-only `--amend` to `885b1c9` carried the oracle
result into the commit message — same tree, disclosed), and
`printf %s 6e100da128680cc385731785ba385cef8bd7da6a | sha256sum`
reproduces the value written into that manifest exactly --
`5e2efb85a7281954f234e3bc5e4655d82734610db6e51f7940a0e08ed7b36485` --
with no git object needed to re-check it. The baseline side's
all-zero value is the documented no-readable-`.git` placeholder the
detached worktree produced. **The demo corpus's ground truth did not
move.**

### Step 4 — register hygiene, and the class the fix did not close

`roadmap.md#surge-policy-self-check-202` -> `CLOSED 2026-08-18
ADR-0153` under `## Done`. Census S-5 marked closed and dated, citing
this ADR.

**One NEW row**, because the diagnosis exposed a class this fix does
not close: `roadmap.md#bed-ready-vacancy-cascade`. A bed-ready
transfer vacates its own origin bed, and nothing looks for a boarder
waiting on *that* ward — only `decide :discharge` runs the search. Event
325 above is exactly this: RENAL-04 freed at `t 78060` with a Renal
boarder still in ED surge at `t 78480`. It is a realism gap, not an
invariant violation (this fix means the boarder now takes the licensed
bed when a *later* discharge finally pulls them), so it is rowed, not
fixed in passing.

### Addendum, 2026-08-18 — the close tag was paid in session

CI run **32195652221** at `5563f71` (the close commit) concluded
**`success`** while this session was still open, meeting the tag
licence's own condition, so
`stable-20260818-surge-policy-self-check-202` was created and pushed
in-session via `bin/tag-ceremony` and its remote peeled ref verified
against `5563f71f7a43780ab58c1d8ed9193bd6ceb41a28` exactly. Deferring a
licensed tag is the deviation (`rulings.md#R-tag-law`), so this is
ceremony, not a judgement call.

`bin/post-push-verify c1a40d0 5563f71` reported all three of its checks
green: remote tip matches HEAD, every commit message in the range is
pure ASCII, and the CI run reported once (queued at the time, disclosed
as not awaited there and awaited separately here).

### Addendum, 2026-08-19 — the reason given for IDENTICAL was wrong

Review 4's register row L1-1 re-derived this section's own oracle claim
and found the verdict right and its stated reason wrong. The sentence
above — *"It holds because the fix draws no RNG and changes no bytes on
the branch every existing root takes: a vacated LICENSED bed is handed
over exactly as before"* — is superseded by this addendum
(`rulings.md#R-dated-addendum-not-silent-edit`: the original text stays,
the correction is dated beside it).

**What is wrong with it.** The oracle has exactly one `:bed-ready`
transfer across all 35 roots, in `death-fixture`, and the bed it hands
over is a **surge** bed, not a licensed one:

```
{:home-ward "Emergency", :bed-ready true, ...
 :from {:ward "Cardiology", :bed "CARDIOLOGY-02", :placement :licensed},
 :placement :surge,
 :location {:ward "Emergency", :bed "ED-H02", :placement :surge},
 :forced false}
```

preceded by a `:discharge` vacating exactly
`{:ward "Emergency", :bed "ED-H02", :placement :surge}`. So the
`(= :surge (:placement vacated-location))` half of this session's own
new guard evaluates **true** on the only event the sentence describes.

**The real reason, which is stronger.** `config.clj:41` gives the
Emergency ward `:beds 0 :surge-slots 6`, so `home-licensed-free?` is
identically false for every ED boarder — and the ED is where the
oracle's only bed-ready transfer lives. IDENTICAL holds structurally,
not by which placement happened to be vacated.

**Recorded as an instance, not just a correction.** This ADR wrote *"a
right answer for the wrong reason is worth catching once"* and then gave
a second wrong reason in the same paragraph. Both were arrived at the
same way: reasoning about the oracle's coverage from memory of what the
roots do, instead of reading a digest. ADR-0156 is the fix that makes
that harder — `digest.clj` now carries a COVERAGE block, inside the
region the soundness check compares, naming what no root can move.

**Coverage restated, from ADR-0156 Step 0 (b)'s own fresh 35-root
digest.** The standing "the oracle is blind to capacity pressure" claim
is false as stated; the honest claim is **thin, not zero**, and one root
deep: 1 `:bed-ready` / 1 `:transfer` / 1 `ADT^A02` / 13 rung-3
placements, all `death-fixture`; 48 rung-1, 381 rung-2; rung 4,
`:forced` and `:exhausted` all zero.
