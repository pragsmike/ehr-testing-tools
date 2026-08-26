# 2026-08-26 — arc 3b design: the encounter horizon, scheduling state, and the bed-status cycle (ADR-0174)

## Scope

Asked for: one ride-along commit against ADR-0173 §2(d), then a design
ADR for arc 3b -- `R-mix-5` (scheduling is state) and `R-mix-6`
(bed-status is state) -- plus a recommendation on whether the
single-encounter horizon rowed as `[multi-encounter-horizon]` belongs
with them. Design only; no `components/*/src` change; rulings lettered.

Did: exactly that. Three commits.

* `b9d4d77` — ADR-0173 §2(d) gains its dated deviation and ruling (a).
* `b81528a` — ADR-0174, Proposed: a three-part census, a four-part
  design each with rejected alternatives, five lettered rulings with a
  recommendation each, and a "what this ADR does NOT design" section.
* `dbba732` — the two roadmap rows, and one disclosure the roadmap edit
  exposed, which also took ADR-0174 to its final 826 lines.

## Red→green evidence highlights

A docs-only session's proof is the suite staying green and untouched,
and this one adds a second kind of proof: **every figure in the ADR is
a measurement taken at this tip, not a claim.** Two corpora were
generated from the commands their own READMEs print and counted by
parsing the EDN, never grepping it.

| | ed-tuesday (seed 20260811, 100 arrivals, `--churn`) | clinic-decade (seed 20260807, 200 arrivals) |
|---|---|---|
| events | 695 | 1,136 |
| patients | 116 | 230 |
| encounter openers | 110 (`:admission` 110, `:outpatient-visit` 0) | 118 (`:admission` 90, `:outpatient-visit` 28) |
| **max openers per patient** | **1** | **1** |
| patients with zero openers | 6 | 112 |
| arrival ordinals that minted a patient | 78 of 100 | 161 of 200 |
| **repeat arrivals queueing NOTHING** | **22** | **39** |
| hook-minted patients | 38 (15 birth, 15 unidentified, 8 injury) | 69 (29, 28, 12) |
| hook encounters on an EXISTING patient | 1 (delivery) | 21 (17 delivery, 4 injury) |
| transfers / of which `:bed-ready` | 13 / 2 | 0 / 0 |
| beds touched | 22 | — |
| vacate→occupy transitions / **at zero seconds** | 102 / **7** | — |

Every column reconciles: 116 = 78 + 38 and 110 = 38 + 72; 230 = 161 +
69 and 118 = 69 + 21 + 28.

Two facts that a reader could not get any other way:

* **Both of ed-tuesday's `:bed-ready` transfers land at the same `t` as
  the discharge of the same bed, 2 of 2.** "Vacated IS ready" is
  measured, not inferred from `decide :discharge`'s shape.
* **PV1-19 is empty on the wire**, not merely unset in the source. The
  PV1 of `msg-001.hl7` renders seven populated fields and then 28
  blanks, so there is no encounter identifier anywhere on this
  project's v2 output.

**Suite: `make test` MAKE_EXIT=0, 4,284 tests / 20,098 assertions, 0
failures, 0 errors** (~19 min wall, a NOISY penny -- the Windows side
was not sampled, so this figure is not comparable to
`reference_make_test_runtime`'s quiet-penny baseline and is recorded
only as green). `clojure -M:poly check` OK. **Those are the SAME two
counts the arc 3a part 4 record carries at its own tip
(`2026-08-26-arc-3a-fold-part-4.md:32`)**, which is the proof this
session wanted: a docs-only session that moved neither figure. No
`src` and no `test` file was edited. `make integration` was not run and
was not owed -- nothing this session wrote is executable.

## Judgment calls and their ratification status

Everything in section 3 of the ADR is a recommendation awaiting the
author; the five rulings A-E are NOT ratified. Beyond those, five calls
this session made on its own:

1. **The ride-along's wording follows the TREE, not the prompt.** The
   prompt said the coincidence rule "was unreachable"; `person_fold/
   hook-kinds`'s own docstring says the rule STANDS, implemented as
   written, and is JOINED by its antecedent. The deviation is written
   the tree's way. Ratified by construction -- the code is the record.
2. **`:encounter-id` is minted at the encounter OPENER, not at
   arrival** as step 2(a) worded it. 6 of ed-tuesday's 116 patients and
   112 of clinic-decade's 230 register and never open an encounter;
   minting at arrival would hand every one of them an id for an
   encounter that does not exist. Unratified.
3. **The v2 contract question is answered from the tree's own
   dependency, not from a chapter.** No HL7 v2 standard text is
   vendored here, so section 2(d) reads `hapi-structures-v24` 2.6.0
   (`components/judge-v2-hapi/deps.edn:9`) directly: `2.4.properties`
   maps SIU_S13..S26 to SIU_S12; SIU_S12 is `[MSH SCH NTE PATIENT
   RESOURCES]` with SCH-1/SCH-2 the `EI` appointment ids; **ADT_A20 is
   `[MSH EVN NPU]` and NPU has exactly two fields** -- no PID, no PV1.
   The alternative was to assert the chapter from memory, which this
   channel's error ledger tracks as unearned specificity. Unratified as
   a design input; the readings themselves are reproducible.
4. **The bed cycle's events reach GROUND TRUTH but no message**, and
   the reason given is the invariants rather than the consumer: every
   `check.clj` function takes `[ground-truth]`, so a cycle held only in
   `world` is a cycle nothing can judge. `R-mix-6`'s bed-board clause
   points the same way but is weaker, since the board reads messages.
   Unratified; it is ruling C's second half.
5. **The `:world`/`:patient` split for scheduling draws.** Position
   (scheduled-vs-walk-in, lead time) is `:world` because arrivals are;
   outcome (kept/rescheduled/cancelled/no-show) is `:patient` because
   it reads no other patient's state. Unratified.

## Findings and HEAD landed

**Three findings, none of them fixable inside this session's fences.**

1. **`:bed-swap` must be excluded from the new
   `no-assignment-to-a-non-ready-bed` invariant.** `decide :bed-swap`
   (`engine.clj:1294`) picks a peer who is already `:admitted` with a
   `:location` and exchanges the two locations; it calls `allocate` at
   no point, and **both target beds are occupied by construction**. An
   unqualified "assignment" reading of the invariant would go red on
   every swap in every corpus. Named in section 2(c) with its reason,
   so the implementing session does not have to find it by going red.
2. **`roadmap.md:143` describes a slice that landed.** The
   `[corpus-player-slices]` row calls the ADR-0014 bed-board sink
   unpriced and unrowed; it landed 2026-08-07 under ADR-0067 (*"Player
   board: the whiteboard exists"*) and ships as `bin/ehrt play --board`
   (`corpus/board.clj`, `bases/cli`'s `board-sink`). This is the
   review-5 pattern -- a claim true when written that nothing keeps
   true. The fence allows two roadmap edits and this is neither, so it
   is disclosed here for the author rather than rewritten.
3. **`surge-only-when-earlier-rungs-exhausted` will change meaning
   silently unless someone says so.** Under a bed cycle, "rung 1 was
   exhausted" becomes "no rung-1 bed was READY" rather than "no rung-1
   bed was empty" -- and `bed-ready-location`'s hand proof of rung-1
   availability (`engine.clj:1129-1133`) is written against the old
   predicate. Both are in the ADR's Consequences.

**One tension named and NOT escalated to a STOP.** `R-mix-6` names the
corpus-player bed board as a consumer of the bed cycle; the board
consumes HL7 messages only (`help.clj:256`: *":play-board-unsupported-
for-events on an event log"*), and the ADR recommends emitting no new
message family in v1. So `R-mix-6`'s consumer clause is not fully
honoured by the recommended v1. It is not a STOP because it is a
DEFERRAL, not a contradiction: the events exist in ground truth, the
invariants judge them, and ruling C is exactly where the author can
close the gap early by taking ADT^A20. Named in sections 2(c), 2(d) and
ruling C so it cannot be mistaken for an oversight.

**One seed disambiguation, added to the ADR in its own commit.** The
`[multi-encounter-horizon]` row cites 0 and 23 parent-delivery
encounters for the same corpus pair; those are the GATE seeds (`ed-202`,
`cd-424242`, tabled at
`.agents/session-records/2026-08-26-arc-3a-fold-part-4.md:168`), while
this session measured the DEMO seeds each README prints and got 1 and
17. Both say the same thing about the same mechanism; neither
supersedes the other, and a reader meeting both numbers should not have
to derive that.

**HEAD landed:** see the CI line below. Three commits on `main`,
pushed. No tag (`rulings.md#R-tag-law`, RETIRED). No `rulings.md` row
(FROZEN, de-scaffold ruling 2026-08-25).
