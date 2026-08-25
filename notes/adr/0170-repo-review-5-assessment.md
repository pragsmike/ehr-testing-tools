## ADR-0170 — repo review 5: the assessment lands, nothing is fixed

**Status:** Accepted (author-chartered in the design channel, 2026-08-25;
executed by the assessment session the same day).

### Context

`roadmap.md#repo-review-5` charters the fifth `repo-review` pass, and
`notes/adr/0159-review-4-arc-close.md:466-485` worked its due point
explicitly: *"This close is ADR-0159. Review 5 is chartered at
approximately ADR-0174."* That paragraph exists because the design
channel's own figure was ~ADR-0169 — fifteen measured from review 4's
CHARTER (0154) rather than from its CLOSE — and ADR-0159 recorded the
correction *"so the next session does not average them."*

**This review runs at ADR-0170, four ADRs before the tree's own computed
due point, on an author ruling of 2026-08-25.** That is recorded here as
an **OVERRIDE** of `rulings.md#R-review-cadence-in-adrs`, not as
compliance with it and not as a re-derivation of the cadence. The reason
the author gave: the arc-0 record is fresh and the review's named theme —
unearned specificity, ADR-0159's own errata (c) — has a dense new sample.
Per the rule's own text, **review 6's due point is computed from THIS
close, not from ADR-0174.**

The window under review is therefore **ten** ADRs (0160–0169,
2026-08-20 → 08-25) rather than the ~fifteen the rule specifies, plus
eleven session records including the throughput spike, which has no ADR
of its own by design and was reviewed as a record.

### Decision

The shape is the one the author ruled for review 4 on 2026-08-18 and has
not re-ruled: **"Q1 c, Q2 register and separate fix session."**

**(c) HYBRID.** The coordinating session ran the eight-dimension battery
itself under a probe budget of at most 12 per dimension (96 cap), and
dispatched three sub-agents for the three lines this window opened — L-1
gate vacuity, L-2 the premise-correction ledger, L-3 measurement
discipline — each in its own fresh clone of `f05f51a`, no cap. **Every
sub-agent row entered the register only through coordinator
re-derivation in its own tree, or with the "could not reproduce"
label.**

**Q2.** This session lands the **register** and the **plan** and nothing
else. No fix, no disposition beyond PROPOSED, no `rulings.md` amendment,
no skill amendment, no roadmap row closed, no tag paid.

### Budgets used, per dimension

| dimension | probes run | of 12 |
|---|---:|---:|
| D1 Claim-reality coherence | 8 | 12 |
| D2 Guard coverage | 6 | 12 |
| D3 Environment independence | 4 | 12 |
| D4 Error honesty | 3 | 12 |
| D5 Mirror and derivation drift | 5 | 12 |
| D6 Sampling adequacy | 5 | 12 |
| D7 Continuity integrity | 8 | 12 |
| D8 Operator experience | 6 | 12 |
| **total** | **45** | **96** |

**No dimension exhausted its budget**, so nothing was displaced by it.
The probes NOT run are enumerated per dimension in the register, with the
two limits worth the author's eye named: the **126-row rulings→gate map**
no review has ever built, and — new — the fact that **no probe in this
rubric can currently detect a stale prose citation**, which is why this
review's central pattern went eight instances deep before anything found
it.

### Sub-agent provenance

**45 sub-agent rows of 88 total.** Counted by extraction from the rows,
not tallied while writing: **29 fully RE-DERIVED** by the coordinator in
its own tree, **16 RE-DERIVED in part** (mechanism, population or cited
artifact confirmed here; multi-trial instrumentation and live-run
censuses labelled sub-agent-witnessed), **0 recorded as COORDINATOR COULD
NOT REPRODUCE**. Two rows were promoted from in-part to full only after
the timed suite finished and a JVM could be spent without perturbing it.

**One sub-agent claim was contradicted by the coordinator's first probe
and confirmed by its second**, and the correction is the register's own
D6-4: a `grep` over an EDN log is not a census of that log. The first
pass reported 174 `:medication-end` events in a corpus that parses to
**zero** — the grep was matching events nested inside `:pre-horizon-facts`.
Every event count in the register was produced by parsing. The rubric's
standing law — *audit evidence uses the mechanism it recommends* — paid
for itself inside the audit that states it.

### The scoreboard delta

| dimension | review 4 | **review 5** | |
|---|---|---|---|
| D1 Claim-reality coherence | GREEN | **RED** | regressed two steps |
| D2 Guard coverage | RED | **YELLOW** | improved |
| D3 Environment independence | YELLOW | **YELLOW** | — |
| D4 Error honesty | GREEN | **GREEN** | — |
| D5 Mirror and derivation drift | YELLOW | **YELLOW** | — |
| D6 Sampling adequacy | YELLOW | **RED** | regressed |
| D7 Continuity integrity | YELLOW | **YELLOW** | — |
| D8 Operator experience | YELLOW | **YELLOW** | — |

**Review 4 was 2 green / 5 yellow / 1 red. Review 5 is 1 green / 5 yellow
/ 2 red — the worst scoreboard of the five, over the window containing
the best engineering.** Both statements are true and they are the same
statement. Every red is a **missing mechanism**, which is what
`rulings.md#R-severity-tracks-mechanism` instructs the score to track:
arc 0 proved a three-family refactor byte-identical at 104,851 events and
disclosed ten deviations by number, ADR-0166 witnessed its invariant red
in both directions, and ADR-0167 refused to blame the tree until it had
sampled the host.

**The cross-dimension pattern, one level up from review 4's.** Review 3
found *a population that is a registry rather than the tree*; review 4
found *a gate's population standing in for the class it enforces*; review
5 finds:

> **a claim that was TRUE when it was written, that nothing keeps true.**

`engine.clj:1490 → :1504 → :1564 → :1605`, four values in four days,
because arc 0's own commits moved the line. Nine-plus stale `engine.clj`
cites in a live onboarding doc. A docstring calling itself *"the ONLY
population-scale exercise of the `:care-plan-end` half"* of a thing it now
exercises **none** of, because the fix that closed the defect removed the
events. "The 14 independent `engine/replay` calls" — 14 then, 11 now, on
the PRIORITY 1 open row. All green. **The repo's gates ask "is this
true?" at authoring time; almost none ask "is this still true?" — and the
generated surfaces, which regenerate and diff on every push, are the one
part of this tree with nothing wrong in it at all.**

### Watch-list

Review 4 handed thirteen rows. **Six fired** (W-1, W-3, W-4, W-5, W-9,
W-13), **one fired sideways** (W-10 — the recorded defect rotated out of
the file the live row still says it is in), **one narrowly** (W-8), **two
did not fire** (W-2, W-11 — the latter could not, the window's
`rulings.md` diff being append-only), **one did not recur** (W-6 — but a
third technique, the mutation witness, reached its second use and is what
now owes the skill line), **one is answered and closed** (W-12), and
**one is answered for the new gates and open for the old** (W-7). Each is
re-derived row by row in the register's own table; none was carried.

### The plan

`.agents/plans/2026-08-25-repo-review-5-plan.md` — **ten** rulings as
lettered options with a recommendation each (the tag ledger; Q4's host
sample; `R-witness-population-is-counted`; the born-red/born-green
bifurcation; the second suite over the close commit; correcting a defect
rotated into the append-only attic; `R-premise-correction-is-a-finding`;
amend-and-quote as a correction's default landing; verification's home in
an ADR; and the cadence rule itself, given this review's own override) —
**six** proposed fix sessions batching all 38 fix-session candidates, each
naming its co-landed gate; what is deliberately fine; and the probes not
run.

**The plan goes to the author. Nothing in it executes.**

### Fences honoured

No `src` change. No `test` change. No fix of any finding, however small —
**nine** of the register's rows are one-line errata (D1-5, D1-7, D7-2,
D7-7, L2-12, L3-2, L3-5, L3-6, L3-8), extracted mechanically rather than
counted by eye, and all nine are plan
items, not acts. No skill amendment (the rubric would benefit from a
ninth probe class, *a claim with no clock*; that is stated in the plan and
not done). No `rulings.md` amendment. No roadmap row closed. No tag paid —
the tag ledger is DISCLOSED at Step 0 and its disposition is Q-A, with
one correction to the prompt's own ledger: enumerated from `git log`
rather than from the ADR numbering, the window holds **three** tagless
arc closes (`68af03b`, `7c1dfa5`, `4772e73`), not two.

**Eight premises of this session's own prompt did not hold**, and every
one is a register row or a disclosed deviation rather than a silent
adaptation. The prompt's archive carries them as an `## Errata` preamble
— a practice three of the window's eleven archives follow and no rule
requires, which is itself register row L2-8. Two of the eight are this
review's own flagship example of the pattern it found: `.agents/handoffs/`
does not exist, and the `Random` the prompt places at `engine.clj:1504`
is at `:1605` — a citation that was **correct when written** and that
walked `1490 → 1504 → 1564 → 1605` in four days because arc 0's own
commits moved it.

**Files touched:** two plans (the register and the plan), this ADR, one
prompt archive, one session record, `.agents/plans/roadmap.md` (the
`#repo-review-5` row, compacted in place to fit its pointer inside the
six-line cap — the row was AT the cap, which is register row D7-7), and
`.agents/plans/README.md`, which the session prompt's own expected-files
list does not name and which
`ehrt.docs-tooling.index-completeness-test` **requires**: every real file
in `.agents/plans` must carry a star-bullet in that README. Recorded as a
premise correction against this session's own prompt, in the class L-2
enumerates. Generated files (`notes/ADRs.md`, `.agents/state-derived.md`,
both record `INDEX.md`) regenerate by `make`; that is not a breach.

**Close-phase suite delta vs Step 0: ZERO on all three counts.** Step 0
and close both `MAKE_EXIT=0`, both **370 zero-failure blocks / 4,166
tests / 18,690 assertions**, both `grep -cE '^(FAIL|ERROR) in'` = 0,
reconciling exactly against ADR-0169's own figures. No test was added or
removed, so ZERO was the prediction and ZERO is the outcome. Walls: Step
0 881 s (poly 842 s), close 866 s (poly 826 s) — both clocks agreeing to
within a second of each other, which is the whole of register row L3-1's
recommendation demonstrated rather than argued. `make ci-parity` green
from a real fresh clone with a cold artifact cache, at the same
370 / 4,166 / 18,690.

**A THIRD full run was owed and taken**, and the reason belongs in the
reasoning-of-record rather than only in the ceremony log: the close-phase
run was started while this session was still editing the register, the
plan, this ADR and the record, so it did not cover the tree about to be
pushed. `R-full-suite-before-push` says a push is *preceded by* a full
`make test`; a run that predates the tree is not that run. The commit was
made first to freeze the tree, the suite re-run over the **committed**
state (`MAKE_EXIT=0`, 865 s wall / 825 s poly, the same 370 / 4,166 /
18,690), and only then the push. **Every run agreed to the assertion,
and wall and poly track each other to within a second in every one of
them.**

The rule this session held itself to is recorded as a **property, not a
count**: *every push was licensed by a full `make test` over the tree
that push carried, run after that tree was frozen by its commit.* A
count would not survive its own recording — a session that writes "three
runs" owes a fourth to record the fourth, and a fifth to record that.
That regress is the sharpest argument for the plan's Q-E, and it reframes
the question: not whether a close commit deserves its own suite run, but
**over which tree the run that licenses the push actually executed**.

**CI verified in session** (`R-session-verifies-ci-via-gh`): run
**`32836518635`** at `754503d` concluded **`success`**. The run
`bin/preflight` disclosed as pending at Step 0, `32828026389` at
`f05f51a`, also concluded `success`, so the last five runs on `main` are
green with nothing outstanding.
