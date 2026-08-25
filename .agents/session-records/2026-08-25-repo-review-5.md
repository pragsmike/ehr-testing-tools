# Session record — repo review 5: the assessment (ADR-0170)

**Date:** 2026-08-25. **Skill:** `repo-review` (steps 1-5 only).
**Tip at start:** `f05f51a`, tree clean.
**Prompt:** [`.agents/prompts/2026-08-25-repo-review-5.md`](../prompts/2026-08-25-repo-review-5.md)
(archived FIRST, with an `## Errata` preamble recording the six premises
of its own that did not hold).

**Shape:** author-ruled HYBRID, carried unchanged from 2026-08-18 ("Q1 c,
Q2 register and separate fix session"). Chartered at **ADR-0170**, four
ADRs before the tree's own computed due point of ~ADR-0174, as a declared
author OVERRIDE of `rulings.md#R-review-cadence-in-adrs`.

## Step 0

`bin/preflight` **exit 0**, no findings. Last five CI runs on `main`:
`f05f51a` **in_progress** (run `32828026389`, DISCLOSED per AR-CI-4, not
counted red), then four green (`d49f1c6` ×2, `ff45ad1`, `7c1dfa5`). Edit
root `/home/mg/src/ehr-testing-tools`, not under `/mnt/`; `core.fileMode`
**true**; `core.ignorecase` unset; tree clean including untracked; local
HEAD == `origin/main`; last `stable-*` tag
`stable-20260821-patient-simulator-charter` @ `6ce2160`; **HEAD
untagged**.

**Arc mid-flight?** No. ADR-0169 closed in its own session and its
commits are pushed at `f05f51a`; arc 1 is a roadmap row with no session.

**Tag ledger, DISCLOSED not paid.** ADR-0163..0169 carry **no tag** —
seven ADRs. Enumerated from `git log` rather than from the ADR
numbering, that is **THREE** tagless arc closes, not the two the prompt's
own ledger implies: `68af03b` (ADR-0163/0164), `7c1dfa5` (ADR-0165/0166),
and `4772e73` (arc 0, ADR-0169; pushed tip `f05f51a`).
`R-arc-closes-in-own-session` says an arc closes "with its own tag".
Disposition proposed at the plan's Q-A; **this session pays none** (tags
are the author's). The correction from two to three is itself a premise
correction against this prompt, in L-2's class.

**Review 4's arithmetic, re-derived before drafting** (skill step 4's
standing sub-step), mechanically from the register as FIRST COMMITTED
(`git show 0a07195:…`) because fix sessions overwrite disposition cells
in place: **72 rows, 27 fix-session-candidate / 25 close-as-fine / 10
ruling-needed / 9 intake / 1 cross-reference**, every per-section cell
matching its own summary table; sub-agent provenance **20 full / 17 in
part / 0 could-not-reproduce**. **Both figures correct** — the first time
this standing sub-step has come back clean.

## Suite figures

Taken **after** the three sub-agents finished, deliberately: review 4's
own Step-0 suite ran at 21m13s under three-sub-agent contention and had
to be recorded as not comparable. Disclosed as a deviation from the
prompt's Step-0 ordering; the reason is ADR-0167's own lesson.

**Host-side health record, sampled at the moment of the figure** —
Windows `LoadPercentage` **1 / 4 / 3**; five `wslhost.exe`, largest
cumulative CPU **1.11 s**, no orphan; Linux 1-min load **0.20**, 12
logical CPUs, up 14h00m.

| run | MAKE_EXIT | wall | poly `Execution time` | blocks / tests / assertions |
|---|---|---|---|---|
| **Step 0** | 0 | **881 s (14m41s)** | **842 s (14m02s)** | 370 / 4,166 / 18,690 |
| **close** | 0 | **866 s (14m26s)** | **826 s (13m46s)** | 370 / 4,166 / 18,690 |

`grep -cE '^(FAIL|ERROR) in'` = **0** in both. `clojure -M:poly check`
OK; `bin/verify-nist-lock` OK (6 coordinates match). Both runs unpiped by
redirect, `MAKE_EXIT` captured in its own file, wrapper ending
`exit "$MAKE_EXIT"` (`R-full-suite-before-push`).

**Step-0 counts reconcile EXACTLY against ADR-0169's 370 / 4,166 /
18,690.** **Close delta vs Step 0: ZERO on all three counts** — 370 / 4,166 /
18,690 both runs, `grep -cE '^(FAIL|ERROR) in'` = 0 both, exactly as
predicted; no test was added or removed. On time the close run is
**15 s FASTER** on the wall (866 s vs 881 s) and **16 s faster** on poly
(826 s vs 842 s), the two clocks again agreeing to within a second of
each other — a docs-only close, on a machine sampled quieter than at
Step 0 (`LoadPercentage` 3/3/2 against 1/4/3; five `wslhost.exe`,
1.47 s cumulative CPU, no orphan; Linux 1-min load 0.45 after a
settle loop waited for it to fall below 0.5).

**And the timing comparison settles register row L3-1 empirically.**
Comparing like with like against the ADR-0167 post-reboot baseline:

| comparison | baseline | Step 0 | delta |
|---|---|---|---|
| **wall vs wall** | 878 s | 881 s | **+3 s** |
| **poly vs poly** | 839 s (13m59s) | 842 s (14m02s) | **+3 s** |

Both clocks agree to the second. The arc-0 record's `+27 s` is a **wall
minus a poly execution time**; wall-against-wall its own figures give
**−12 s**. That is L3-1's mechanism, demonstrated rather than argued, and
it is why the plan's Q-B rider carries "name the kind" as its
load-bearing clause rather than the host sample.

## Probes

`make state-derived` → tree byte-clean. `make docsgen` (all twelve
leaves, **2m12s**, `MAKE_EXIT=0`) → tree byte-clean. `bin/fence-census`
→ 28 exercised / 3 exempt / 46 bare of 77, identical to ADR-0158.
`diff -r .agents/skills .claude/skills` → IDENTICAL, 59 files each side.
**Reading sets re-measured at the close** (`R-register-hygiene-at-close`):
`:corpus` 1836/2045, `:docs` 743/785, `:judge` 926/1000, **`:onboarding`
1498/1530 (headroom 34 → 32)**, `:sim` 1278/1405. The two lines are this
session's own star-bullets in `.agents/plans/README.md`, an `:onboarding`
path; under budget, so `R-budget-stop` compels no compaction and none was
taken.
`make ci-parity` → **exit 0**, 873 s wall, poly 13m52s, 370 / 4,166 / 18,690 reconciling exactly with the edit-root suite — green from a real fresh clone with a cold artifact cache. CLI matrix and the README's own
demo path executed against the built binary, all green. `out/` cleared
before every run.

**Budget: 45 probes of 96.** D1 8, D2 6, D3 4, D4 3, D5 5, D6 5, D7 8,
D8 6. No dimension exhausted its 12; the un-run probes are enumerated per
dimension in the register.

## Sub-agents

Three, dispatched at Step 0 in parallel with the coordinator's own
battery, each in its own fresh clone of `f05f51a` with no probe cap:
**L-1** gate vacuity, **L-2** the premise-correction ledger, **L-3**
measurement discipline. **45 sub-agent rows of 88 total: 29 fully
RE-DERIVED by the coordinator in its own tree, 16 RE-DERIVED in part, 0
could-not-reproduce.**

**One sub-agent claim was contradicted by the coordinator's first probe
and confirmed by its second.** L-1 reported the seed-424242 gated corpus
carrying **zero** `:medication-end`; a `grep -o ':event :[a-z-]*'` over
the committed fixture returned **174**. Parsing the same file with
`clojure.edn` returns **343 top-level events and zero** — the grep was
matching events nested inside `:pre-horizon-facts`. The sub-agent was
right and the coordinator's instrument was wrong, in the dimension whose
own law is *audit evidence uses the mechanism it recommends*. Recorded as
register row D6-4; every event count in the register was produced by
parsing.

## Deviations

1. **The timed Step-0 suite ran after the sub-agents, not before them.**
   The prompt's Step 0 orders it first. Taking it under three-sub-agent
   contention would have produced a figure with no comparability — review
   4's own Step 0 is the precedent and its register says so. Disclosed
   with the full health record; the figure reconciles to +3 s on both
   clocks against the ADR-0167 baseline, which a contended run could not
   have shown.
2. **`.agents/plans/README.md` is touched, and the prompt's
   expected-files list does not name it.**
   `ehrt.docs-tooling.index-completeness-test` requires a star-bullet in
   that README for every real file in `.agents/plans`, so landing two
   plan files without it leaves the tree red. Fix-forward under
   `R-stop-only-on-two-defensible-readings` — one defensible reading —
   and recorded as a premise correction against this prompt, in L-2's own
   class.
3. **The `#repo-review-5` roadmap row was AT the six-line cap**, so its
   mandated one-line pointer required compacting the row rather than
   appending to it. Register row D7-7; 25 of 31 OPEN rows are at the cap.
4. **The prompt archive carries an `## Errata` preamble.** Three of the
   window's eleven archives do this and no rule requires it; the prompt's
   own "premise corrections are findings" fence is what makes it owed
   here. It records all **eight**, including the two —
   `.agents/handoffs/` and `engine.clj:1504` — that are this review's own
   flagship example of the pattern it found, and the one prompt premise
   the register *endorses* rather than contradicts (the cadence
   correction, which is true and doubly landed).
5. **Eight premises of the prompt did not hold** and are register rows
   or disclosures rather than silent adaptations: `.agents/handoffs/`
   does not exist and `engine.clj`'s `Random` is at `:1605` not `:1504`
   (L2-10); `run_test.clj:386-440` is wrong at both ends (L2-12); arc-0's
   F-3 makes a different correction than the one attributed to it
   (L2-11); "the three new defspecs PLUS the naive-vs-fast one" implies
   four where the tree has three (D6-1); none of the ten ADRs has a
   `Verification` section (D1-7); the expected-files list omits
   `.agents/plans/README.md` (deviation 2); and the tag ledger's "two
   arc closes" is **three** when enumerated from `git log` (`68af03b`,
   `7c1dfa5`, `4772e73`). None was adapted around silently.

## Fences honoured

No `src`, no `test`, no fix of any finding — **nine** of the register's
rows are one-line errata (D1-5, D1-7, D7-2, D7-7, L2-12, L3-2, L3-5, L3-6, L3-8),
extracted mechanically rather than counted by eye, and all nine are plan
items. No skill amendment
(the rubric would benefit from a ninth probe class, *a claim with no
clock*; stated in the plan, not done). No `rulings.md` amendment. No
roadmap row closed. **No tag.** Exit codes unpiped throughout. Every
timed figure carries a host-side health record. `out/` cleared before
every run. All artifacts dated by this session.

## Files touched

Two plans (register + plan), `notes/adr/0170-repo-review-5-assessment.md`,
this record, the prompt archive, `.agents/plans/roadmap.md` (one row,
compacted in place, STAYS OPEN), and `.agents/plans/README.md` (deviation
2). Generated by `make`: `notes/ADRs.md`, `.agents/state-derived.md`,
`.agents/session-records/INDEX.md`, `.agents/prompts/INDEX.md`.

## Close

COMMIT_PLACEHOLDER_2
