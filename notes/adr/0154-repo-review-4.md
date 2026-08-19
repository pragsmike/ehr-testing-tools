## ADR-0154 — repo review 4: the assessment, run hybrid; 72 rows, 10 rulings owed, nothing fixed

**Status:** Accepted (author-directed, autonomous session per R30),
2026-08-18.

### Context

`roadmap.md#repo-review-4`, PRIORITY 2, chartered by ADR-0139's ruling
Q3 "a." — *the standing cadence rule is ADR count, not calendar; the
next review is chartered after roughly 15 ADRs*, which put review 4 at
approximately ADR-0154. This is that review, and it is ADR-0154.

**This is NOT an arc close.** The `repo-review` skill's steps 1-5 land
an assessment and a plan; step 6 (execution) and step 7 (the arc close)
follow the author's rulings, in separate sessions. Per the author's
ruling of 2026-08-18 — *"Q1 c, Q2 register and separate fix session"* —
review 4 hands the author a plan and stops. `roadmap.md#repo-review-4`
**stays OPEN**.

The window under review is **ADR-0140 through ADR-0153** — fourteen
ADRs, 2026-08-15 to 2026-08-18: the fence battery (0140), the event-log
contract arc (0141-0142), the compression arc (0143-0147), the
exercised-sources gate (0148), the traces gate (0149), the shape
defects and contract bumps to 1.1.0/1.2.0 (0150-0151), the sim-theory
head hop (0152), and the surge fix (0153).

### The shape, as ruled

**Q1 "c" — HYBRID.** The coordinating session ran the eight-dimension
battery **itself** under a probe budget of at most 12 probes per
dimension (96 cap), and dispatched **three sub-agents**, one per line
this window opened, each in its own fresh clone of `4d6ff78` with no
probe cap:

- **L-1, oracle coverage** — what `bin/regression-oracle`'s 35 roots
  actually witness; the coverage matrix and the vacuous set.
- **L-2, exit-code / harness truthfulness** — every place an exit code
  passes through a pipe, wrapper, subshell, `tee`, `|| true`,
  background job or make recipe.
- **L-3, generated-surface completeness** — for every generated
  artifact, what moves it (from the generator's source, not its
  header), and whether any header claims what its generator does not
  enforce.

**The provenance rule the prompt imposed, and what it produced.**
Sub-agent findings were transcript-witnessed until the coordinator
re-derived at least one cited artifact per finding **in its own clone**.
Rows surviving re-derivation entered the register as ordinary rows;
rows failing it would have been recorded as "sub-agent claim,
coordinator could not reproduce", never dropped and never promoted. The
register states which, per row. Final tally, mechanically extracted
from the rows: **20 fully re-derived, 17 re-derived in part, 0
could-not-reproduce.**

The rule earned its place immediately. **L-1's charter rested on three
prior findings and the coordinator's own re-derivation falsified two of
them** — see "The finding that corrects the record" below. Had the
sub-agent's report been taken at face value, the review would have
propagated a corrected claim without the evidence that makes it
actionable; had it been discarded for disagreeing with the standing
account, the review would have preserved a false one.

**Q2 — register and plan only.** No fix, no disposition beyond
PROPOSED, no skill amendment, no rulings amendment, no roadmap row
closed. Review 3 disposed in-session and handed 56 of 74 fences forward
unfixed; this run hands the author a plan and stops.

### Budgets used

12 probes per dimension, 96 cap. **No dimension exhausted its budget:**

| D1 | D2 | D3 | D4 | D5 | D6 | D7 | D8 | total |
|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| 5 | 6 | 4 | 4 | 4 | 3 | 5 | 4 | **35 / 96** |

The constraint on this review was never the budget — it was the
window's own shape. Probes that did not run are named per dimension in
the plan's Part 4, per the skill's "named, not dropped".

**ADR-0139's cadence rule delivered what it promised.** Review 3 had to
record D6-4 (the full-window deviation read) as PARTIAL: 44 ADRs read
at heading depth only. At fourteen ADRs, every one was read at heading
depth **plus** its Deviations / Findings / "things worth your
attention" sections in full. **D6-4 is discharged on evidence.**

### The register

`.agents/plans/2026-08-18-repo-review-findings.md` — **72 rows**, 35
from the coordinator's own battery across eight dimensions, 37 from the
three sub-agent lines.

| disposition | count |
|---|---:|
| close-as-fine | 25 |
| fix-session-candidate | 27 |
| ruling-needed | 10 |
| intake | 9 |
| cross-reference (not tallied) | 1 |
| **total** | **72** |

**Review 3's own summary arithmetic re-derives EXACTLY** — 40 rows,
26/8/2/3 plus one cross-reference — and no correction is owed. That
result required a method correction worth recording, because the naive
run disagreed: extracted from the **live** register the figures come to
42 rows and a different tally, because this repo's fix sessions
**overwrite disposition cells in place** and ADR-0137 appended two rows
mid-arc. Re-derived against the register **as first committed**
(`bc6f46c`), every figure matches. That is now a standing method note
for review 5, in the same tradition as review 3's own note about
`**(new)**` markers.

### The scoreboard, and its delta

| dimension | review 3 | **review 4** | movement |
|---|---|---|---|
| D1 — Claim-reality coherence | YELLOW | **GREEN** | improved |
| D2 — Guard coverage | YELLOW | **RED** | **regressed** |
| D3 — Environment independence | YELLOW | **YELLOW** | unchanged |
| D4 — Error honesty | GREEN | **GREEN** | unchanged |
| D5 — Mirror and derivation drift | RED | **YELLOW** | improved |
| D6 — Sampling adequacy | GREEN | **YELLOW** | **regressed** |
| D7 — Continuity integrity | YELLOW | **YELLOW** | unchanged |
| D8 — Operator experience | YELLOW | **YELLOW** | unchanged |

Review 3 was 2 green / 5 yellow / 1 red. **Review 4 is 2 green / 5
yellow / 1 red.** The red moved rather than cleared.

D5 came off red because every one of review 3's unregistered
derivations is registered and gated, and the sessions that did it went
looking for more unprompted (ADR-0149's `demos/traces/**`, ADR-0152's
`sim-theory-equations.txt` — the second of which found a live invalid
`.edn` on the way). D1 came off yellow because the widened scan root is
a standing gate now, not a one-time sweep.

**D2 went red in their place, and it is the same debt one level up.**
The artifacts are all fresh — verified twice, byte for byte, by two
independent clones — and what is missing is anything that keeps them so
*as a class*. CI's own rule ("a new derived file goes on a make target
AND on the diff list, same commit") has closure assertions for **2 of
12** docsgen leaves and **2 of 19** diff-list paths; a sub-agent removed
two leaves and four paths and the full per-push lane returned exit 0
with zero failures. And `event-schema-baseline.edn`'s freeze — the
claim that keeps the repo's **only** schema-change gate non-vacuous —
is enforced by a sentence in a header and nothing else.

D6 regressed on one finding worth the colour: the repo's broadest
correctness property (`every-m1-run-satisfies-the-invariant-catalog`,
150 trials) runs against a **fixed** facility whose two wards are ED
(0 beds / 15 surge) and Renal (1 bed / 0 surge), with no churn.
ADR-0153's defect required one ward holding **both** a licensed bed and
a surge slot, plus churn. **No trial count would have found it** — the
configuration excludes the branch, not the sample size.

### The cross-dimension pattern

Review 3's thesis was *"a probe, gate, or tool whose population is a
registry rather than the tree"*, with five instances. This review found
the successor shape, six times: **a gate whose population is narrower
than the class it is read as enforcing.**

- `io_vocabulary_lint` forbids three calls; `R-io-result-or-loud` is a
  rule about I/O that can fail (13 `.mkdirs` sites discard their
  boolean, one inside the kernel itself).
- The exit-code law forbids a pipe on the *gate command*; the class is
  any construct that determines the reported exit — and the mask
  migrated to the wrapper's last command (ADR-0152).
- `R-audience-has-entry-path` has no gate at all.
- The invariant-catalog defspec's facility is fixed while its name
  claims every m1 run.
- `R-oracle-script-contract` says the script "aborts on an undeclared
  digest-source diff"; the check cannot see a `:require` change.
- And the meta-instance: the docsgen/diff-list obligation above, a
  class closed one artifact at a time, three times, never as a class.

Three of the eight proposed fix sessions therefore co-land a **widened**
gate rather than a new one. That is the plan's central bet.

### The finding that corrects the record

**ADR-0153's stated reason for the oracle's IDENTICAL verdict is wrong
about the only event it describes**, and the standing claim that the
oracle is blind to capacity pressure is wrong as stated. Both were
established by the coordinator's own 35-root pre-digest, not accepted
from a sub-agent.

Measured over all 35 roots: **one** `:bed-ready true` event and **one**
`:transfer`, both in `death-fixture`; ladder rung 1 = 48
(`total-joint-replacement-engine`), rung 2 = 381 (9 roots), **rung 3 =
13** (`death-fixture`), rung 4 / `:forced` / `:exhausted` = **0**. The
transfer, extracted verbatim, moves a patient `:from {:ward
"Cardiology", :bed "CARDIOLOGY-02", :placement :licensed}` into
`:location {:ward "Emergency", :bed "ED-H02", :placement :surge}` — a
**surge** bed, vacated by the preceding `:discharge`.

ADR-0153 says the verdict holds because *"a vacated LICENSED bed is
handed over exactly as before"*. It is a surge bed, so the guard's
surge branch evaluates **true** on it. The verdict is right; the reason
is not. IDENTICAL holds structurally instead: `sim_model/config.clj:41`
gives the Emergency ward `:beds 0`, so `home-licensed-free?` is
identically false for every ED boarder, and the ED is where the
oracle's only bed-ready transfer lives.

ADR-0153 itself wrote *"a right answer for the wrong reason is worth
catching once"* — and then supplied a second wrong reason. **The
correction makes the underlying advice stronger, not weaker:** every
capacity witness the oracle has is one root deep, so `death-fixture` is
a single point of failure for `:transfer`, `ADT^A02`, `:bed-ready` and
rung 3 simultaneously. A dated addendum on ADR-0153 is offered as
ruling R4-Q6(i); this ADR does not make it, per the fence.

### The plan

`.agents/plans/2026-08-18-repo-review-4-plan.md` — **eight** proposed
fix sessions covering all 27 candidates, each naming its co-landed
gate, and **ten** rulings as lettered options with a recommendation
each: the `--amend` precedent (R4-Q1), `bin/preflight`'s exit code and
its false-green (R4-Q2), `post-push-verify` check 3 (R4-Q3), the R-F8
fence rule now carrying its measured number — **38 bare fences on the
reader path** (R4-Q4), the six hand-regenerated manual assets (R4-Q5),
the oracle's coverage claim in three parts (R4-Q6), the cold-clone
probe's third answer (R4-Q7), a rubric amendment (R4-Q8), two register
rows owed (R4-Q9), and the docsgen/diff-list closure rule (R4-Q10).

Two findings the author may want first regardless of queue order,
because both are cheap and both remove a way for a gate to lie:
**L2-2**, a tracked skill that *teaches* the exit-masking idiom
(`extraction-stage/SKILL.md:95`, both mirrors), and **L3-2**, two lines
asserting `event-schema-freeze` is not a `docsgen` prerequisite.

### Deviations

1. **The prompt's D4 probe does not match the rubric's D4.** The prompt
   assigns `make test` timings to D4; the rubric's D4 is *error
   honesty*. Both ran — the error-honesty probes under D4, the timing
   under Step 0 with its contention caveat — so neither was dropped.
2. **The D1 count-chain probe was re-scoped, and the re-scoping found
   the defect.** The prompt asks to re-derive every stated count in the
   fourteen ADRs' Verification sections "against `git show <sha>`";
   fourteen full suites at fourteen commits is not a session. What ran
   instead was the chain's internal coherence plus one live anchor
   (this session's own Step 0) — which found **D1-1**: four ADRs cite a
   close figure to an ADR that does not carry it (the figure lives in
   the session record).
3. **The prompt repeats the class D1-1 names.** It asks to reconcile
   against "ADR-0153's 348 / 3,960 / 17,758"; ADR-0153 carries neither
   3,960 nor 17,758 —
   `.agents/session-records/2026-08-18-surge-policy-self-check-202.md:83`
   does. The substance reconciled **exactly**; the citation did not.
   Recorded as this window's fifth channel erratum rather than adapted
   around silently.
4. **The standing arithmetic sub-step needed a method correction** — see
   "The register" above.
5. **Two of this document's companion register's own draft summary lines
   were wrong** (the L-2 and L-3 provenance splits, stated inverted) and
   were corrected by mechanical extraction before the register landed,
   with the correction disclosed in the register's own summary rather
   than quietly fixed.
6. **`bin/preflight` was run once at Step 0 and its output believed.**
   L-2 then found that a failed `gh` query renders as
   `OK: last five runs all green`. This session's run listed five green
   runs explicitly, so the OK was earned rather than fabricated — but
   the review's own Step 0 depends on a script it went on to find can
   lie, and that is disclosed rather than left implicit.

### Fences honoured

No `src` change. No test change. No fix of any finding, however small.
No skill amendment and no rulings amendment (the rubric amendment D4-4
proposes is ruling R4-Q8, a plan item). No roadmap row closed —
`#repo-review-4` stays OPEN and gained exactly one line, taking it to
its six-line cap. Sub-agent rows entered the register only through
coordinator re-derivation, with per-row provenance stated. Probe budget
enforced and reported. Exit codes captured by redirect, never a pipe.
`out/` cleared before the CLI probes, per ADR-0140's own stale-`out/`
incident class.

**Files touched:** two plans (new), one ADR (new), one prompt archive
(new), one session record (new), `.agents/plans/README.md` (two index
entries), `.agents/plans/roadmap.md` (one line), plus
`.agents/state-derived.md` and `notes/ADRs.md` regenerated by `make`.

### Consequence

The author holds a plan; nothing executes until it is ruled. The next
review is chartered by the same cadence rule at roughly ADR-0169 — but
**review 5's window should be measured from the last FIX session of
this arc, not from ADR-0154**, since the arc this assessment opens has
not run yet.

### Addendum, 2026-08-18 — the close tag was paid in session, and the run's own artifact recorded

The prompt's tag licence had two branches: pay in session if this
session's tip run concludes `success` while the session is open, else
leave it to the next Step 0, saying which. **The first branch was
taken.**

CI run **32208219862** at `0a07195` (this session's only commit)
concluded **`success`** while the session was still open, read with
`gh run view` per `rulings.md#R-session-verifies-ci-via-gh`. All four
substantive steps green:

    success  poly check
    success  poly test :all skip:integration
    success  verify-nist-lock (supply-chain integrity)
    success  generated-doc freshness (regen + diff)

The licence was therefore payable and was paid rather than deferred —
deferring a licensed tag is itself the deviation (`rulings.md#R-tag-law`).

`bin/tag-ceremony stable-20260818-repo-review-4 0a071959… --push`, its
own output:

    OK: created annotated tag 'stable-20260818-repo-review-4' at 0a071959…
    OK: pushed refs/tags/stable-20260818-repo-review-4
    OK: remote peeled ref for 'stable-20260818-repo-review-4' is 0a071959…, matches target exactly

Annotated, pushed, and verified by the remote peeled ref, not by the
local tag object alone.

`bin/post-push-verify` over `4d6ff783..0a071959` reported all three
checks green: remote tip matches HEAD, every commit message in the range
is pure ASCII, and the CI run reported once (in progress at the time,
disclosed there as not awaited, and awaited separately here).

**The freshness step is worth one sentence of its own.** This session
regenerated `notes/ADRs.md` and `.agents/state-derived.md` plus both
record `INDEX.md` files, and the `generated-doc freshness (regen + diff)`
step re-ran `make docsgen` on CI's own JVM and found no diff — so the
regenerations this ADR's fence permits are confirmed byte-correct from a
cold checkout, not merely locally.

**The run's own oracle artifact, recorded here so it outlives the
session's scratch directory.** The Step-0 pre-digest wrote 35 `.edn`
files totalling **19,967,292 bytes**; the `sha256sum` manifest over them
is 35 lines whose own sha256 is

    036180dcc2833f324706937a3f51dfee1b786e63947b8a58738051acc70247c9

It is not committed (the fence names the files this session may touch,
and a 20 MB digest tree is not among them). Recorded as an identity so a
future session re-running the same pre-digest at `4d6ff78` can check its
manifest against this line rather than re-deriving the comparison from
nothing — and so the register's "kept as this run's artifact" is a claim
with a checkable referent rather than a pointer to a directory that no
longer exists.
