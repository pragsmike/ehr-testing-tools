## ADR-0093 — Review 2's rulings land: three laws append, four anchors, the front door discloses

**Status:** Accepted (author-directed, autonomous session per R30), 2026-08-09.

### Context

Prior: `notes/adr/0092-repo-review-2.md` closed repo review 2's own
survey — 76 rows across eight dimensions, six items needing the
author's own ruling (five `ruling-needed` register rows plus the
plan draft's own successor-tag debt), nothing moved beyond the
register, that ADR, and ceremony files (AR-RR2-1's fence). The author
ruled on all six in the design channel, 2026-08-09, verbatim: **"1 a.
2 a. 3 a. 4 b. 5 (a)-lite. 6 a."** This session lands everything those
rulings make docs/law-only: three standing-ruling appends, four
roadmap rows, and the front door's own honesty disclosure, plus
cluster C's trivial README path fix. It does NOT run any fix cluster
(A or B), does NOT touch the census tool itself (ruling 6 schedules
that as its own future session), and does NOT decide D8-4 (bare-level
unknown-flag tolerance — still awaiting the author's own call, parked
for cluster B).

R30 ceremony. Read first: `notes/adr/0092-repo-review-2.md` (the six
rulings' own full options text); `.agents/plans/2026-08-09-repo-
review-findings.md` (rows D8-6, D8-7, D7-7, D7-8, D7-13, D6-2, D6-1,
H-6 — the evidence each edit cites); `.agents/rulings.md` (the tail
section's own dated, tagged, provenance-marked shape); `.agents/
plans/roadmap.md` (Next and Deferred section conventions, revisit
triggers); `README.md`'s "See it run" and `demos/scenarios/busy-
tuesday/README.md` (the disclosure prose to carry up, and the honest
sibling wording it comes from).

### Decision

Author rulings, recorded verbatim (design channel, 2026-08-09): **"1
a. 2 a. 3 a. 4 b. 5 (a)-lite. 6 a."** Mapped to ADR-0092's own
numbered rulings-needed list, `[A]` author-ruled throughout:

**AR-RL2-0 `[A — this session's own successor tag debt, ADR-0092]`.**
Annotated `stable-20260809-repo-review-2` tagged at `eefce23`
(ADR-0092's own closing tip), message "repo review 2 landed,
design-channel-verified 2026-08-09 (ADR-0092)"; pushed; peeled ref
verified (`git ls-remote --tags origin` resolves
`stable-20260809-repo-review-2^{}` to `eefce2314f557ddb9ac3ff1a1e132
575795f232f` exactly). **Executed Step 0.**

**AR-RL2-1 `[A — ruling 1 = D8-6, "a"]`.** README's "See it run"
front door gains the sibling `demos/scenarios/busy-tuesday/README.md`'s
own honesty disclosure, carried up in the README's own voice: the
board mostly idle-skips forward through a decade-spanning stream, only
one inpatient is ever admitted, and the sparseness is genuine to the
scenario's own population, not a player defect — citing the sibling
README for the full closing-summary numbers rather than restating
them. **Executed.** No demo parameter, config, or witnessed-figure
change — the byte-reproducible fence ADR-0073 built holds untouched.
Option (b) (a busier demo riding a future bed-board/census sink slice,
ADR-0014) is explicitly NOT ruled; noted on the horizon below, not
acted on.

**AR-RL2-2 `[A — ruling 2 = D6-2, "a"]`.** Appended to `.agents/
rulings.md`: "Measurements sample the claimed population, standing" —
a sweep or sample claiming to measure a population must draw from that
population's own RNG path/generation mechanism, never an independent
synthetic path assumed equivalent; a zero measured against a
known-nonzero branch is the tripwire (ADR-0087's own self-caught miss
is the precedent). **Executed**, under a new dated section "From
review 2's rulings (ADR-0092/0093)."

**AR-RL2-3 `[A — ruling 3 = D7-7/D7-8 policy, "a"]`.** Appended to
`.agents/rulings.md`: "Horizon items anchor in the roadmap, standing"
— any item surviving past ONE arc close purely in horizon-note prose
gains a `roadmap.md` Deferred or Next row in the SAME close that first
restates it; horizon notes narrate, the roadmap remembers (ADR-0092's
own D7-7/D7-8 A/B evidence is the precedent). **Executed**, same
section. This session also executes the law's own first two instances
(Step 2, below): wellness-encounters and the `notice_verbatim_test`
coverage gap each gain a `roadmap.md` Deferred row for the first time.

**AR-RL2-4 `[A — ruling 4 = D7-13, "b"]`.** Wave E (vital-sign/CHF/
contraceptives/covid19 cluster) parks. Verified by grep first (the
design channel's own claim that no Wave-E row existed; evidence
outranks it): `roadmap.md` carried a "Vital-sign channel" Deferred row
naming the same three modules and the underlying vital-sign-register
blocker, but no row naming the D7-13 aging pattern or carrying the
ruling's own named trigger. **Executed** — one new Deferred row added
(Step 2, below), cross-referencing rather than duplicating the
existing "Vital-sign channel" row, naming `covid19`'s own
`:zero-on-every-seed` as the genuinely blocked member and "the next
content-vendoring session with a vital-sign-adjacent candidate" as the
revisit trigger, per D7-13's own text.

**AR-RL2-5 `[A — ruling 5 = H-6, "(a)-lite"]`.** Appended to `.agents/
rulings.md`: "Post-push verification includes the ASCII check,
standing" — the standing post-push ceremony adds one mechanical line,
`git log --format=%B -1 | LC_ALL=C grep -n '[^ -~]'`, expected EMPTY;
any hit is disclosed in-session, not discovered by channel report
later. **Executed**, same section. This is ceremony boilerplate for
session prompts and session practice — NO new repo test, NO workflow
change, NO gate file landed; the "-lite" qualifier is the author's own
narrowing of option (a) to exactly this scope, short of option (a)'s
full byte-level disclosure-step machinery.

**AR-RL2-6 `[A — ruling 6 = D6-1, "a"]`.** The census tool's own
`:closure-file-count` fix is SCHEDULED, not executed here. **Executed**
— one `roadmap.md` Next row added (Step 2, below): a small census-tool
session, extending closure-file counting from the JSON-module resolver
to the CSV lookup-table resolver, citing the 3x undercount record
(`notes/ADRs.md` ADR-0074, register D6-1) and review 1's own
unactioned "escalate priority" ask. No `src` touched this session.

**AR-RL2-7 `[C — cluster C rider]`.** README.md's own dangling
`docs/adr/0091-storefront-fixture.md` link corrected to `notes/adr/`
(register D8-7). One line. D8-4 (bare/`help`-level unknown-flag
tolerance) is explicitly NOT in scope — still awaiting the author's
own call, parked for a future cluster-B session.

### What landed where

- `.agents/rulings.md`: three standing-ruling appends (AR-RL2-2,
  AR-RL2-3, AR-RL2-5), new section "From review 2's rulings
  (ADR-0092/0093)."
- `.agents/plans/roadmap.md`: four rows — wellness-encounters
  (Deferred anchor, ruling 3's first execution), the
  `notice_verbatim_test` coverage gap (Deferred anchor, ruling 3's
  second execution), Wave E (Deferred, parked, ruling 4), the census
  `:closure-file-count` fix (Next, scheduled, ruling 6) — plus this
  ADR's own Done pointer.
- `README.md`: the busy-Tuesday front door's own honesty disclosure
  (ruling 1) and the dangling storefront-fixture link fix (cluster C
  rider).
- `notes/adr/0093-review-2-rulings-landing.md` (this file),
  `notes/ADRs.md`'s own index line, `notes/adr/README.md`'s own file
  count (90→91).

### Explicitly deferred, not decided here

- **D8-4** (bare/`help`-level unknown-flag tolerance, cluster B) — no
  ruling yet; still awaiting the author's own call.
- **Option (b) of ruling 1** (a busier front-door demo, riding a
  future bed-board/census sink slice per ADR-0014) — not ruled, noted
  as a possible future rider, not acted on.
- **Ruling 6's own census-tool fix** — scheduled (the Next row above),
  not executed; a future small session's own work.
- **Fix clusters A and B in full** (D2-18/D2-4; D4-5/D4-6/D4-7/D8-3;
  D8-4) — untouched, awaiting their own sessions.

### This session's own successor tag debt

The next session that opens fresh work tags
`stable-20260809-review-2-rulings-landing` at THIS session's own
closing tip, under standing ceremony — the tag-law case (ii) pattern.

### Verification

- `clojure -M:poly check`: OK, Step 0.
- Oracle pre-digest (`bin/regression-oracle eefce23 eefce23`): all
  THIRTY-FOUR roots confirmed IDENTICAL, soundness "yes outside ns
  form" — the expected trivial result of a tip-against-itself bracket.
- Full local suite (`clojure -M:poly test :all skip:integration`):
  [recorded at Step 4's own commit].
- `bin/verify-nist-lock`, run explicitly (D2-4's own still-open
  finding — not fixed this session, only disclosed by name): [result
  recorded at Step 4].
- Last five `test`-lane runs (`gh run list --limit 5 --branch main`):
  all green at Step 0 (commits through ADR-0092's own close). Latest
  `Integration` lane run: green (`31312458033`, 2026-08-09,
  `workflow_dispatch`).
- `gitleaks git --staged -v`: clean at every commit this session.
- Post-push message verification, every commit: [recorded at Step 4/5].
- Post-push ASCII check (AR-RL2-5, this session's own first
  application): `git log --format=%B -1 | LC_ALL=C grep -n '[^ -~]'`
  against each pushed commit — [recorded at Step 4/5].
- Tag verification: `stable-20260809-repo-review-2` peeled ref
  resolves to `eefce2314f557ddb9ac3ff1a1e132575795f232f` exactly.
- `git status --porcelain`: clean before this session's first tool
  call, clean at each commit boundary.

### Fences

No `src`/test/deps touches anywhere. No census-tool changes (scheduled
only, ruling 6). No fix-cluster work (A, B, and the D8-4 call all
await rulings/sessions of their own). No demo parameter or config
changes — the busy-tuesday fence holds byte-for-byte. No new tests,
gates, or workflow edits — AR-RL2-5 is ceremony text, not mechanism.
No roadmap content moves beyond the four named rows and the Done
pointer. Law appends are exactly the three named (AR-RL2-2, AR-RL2-3,
AR-RL2-5).

### Index line

```
- 2026-08-09 — review-2-rulings-landing — ADR-0093
```

(appended to `.agents/plans/roadmap.md`'s own Done section.)

`notes/adr/README.md`'s own file count corrects 90→91, verified by
`ls notes/adr/*.md | grep -v README | wc -l`, not arithmetic.

### The horizon, restated

Untouched, carried forward from ADR-0092: fix clusters A and B in
full, D8-4's own unruled call, ruling 1's own unruled option (b), the
oracle's own blind-spot intake (H-3), the two remaining `defspec`
flake watch items (D3-2), the ADR-footnote-fork backlog row (D7-14),
`make quickstart`'s own untimed full run (D8-8), the two deferred
veteran modules under their true names, and publish-prep Externals (7
closes, correctly parked). What's new on the horizon: the census
tool's own scheduled small session (ruling 6); a future
content-vendoring session with a vital-sign-adjacent candidate is
Wave E's own named revisit trigger; a future session ready to
reconcile upstream's own wellness machinery with this engine's
wellness-cadence design; a future session willing to extend
`notice_verbatim_test`'s own parser to the v2-nist and simhospital
shapes it does not yet recognize.

### Consequence

Everything review 2's own rulings made docs/law-only is landed: three
standing rulings now live in `.agents/rulings.md` rather than only in
ADR prose, two horizon-note-only items (wellness-encounters, the
`notice_verbatim_test` gap) get their own roadmap anchor for the first
time — the exact structural fix ruling 3's own evidence called for —
and a third, aged item (Wave E) gets parked rather than left to age a
fifth close unscheduled. The front door's own "busy Tuesday" framing
now discloses what a stranger actually sees, in the README's own
voice, without touching a single byte of the demo it describes. Six
rulings executed, two decisions explicitly still open (D8-4, ruling
1's option b), one session scheduled but not run (the census fix) —
nothing smoothed over, nothing taken beyond what was ruled.

### Index summary (moved verbatim from notes/ADRs.md by ADR-0143, 2026-08-16)

Review 2's rulings land: three laws append, four anchors, the front door discloses — the author's own six rulings on ADR-0092's register execute their docs/law-only surface: three standing rulings append to `.agents/rulings.md` (measurements sample the claimed population; horizon items anchor in the roadmap; post-push verification includes the ASCII check); wellness-encounters and the `notice_verbatim_test` coverage gap each gain a `roadmap.md` Deferred row for the first time (the new law's own first two instances); Wave E parks with a named revisit trigger; the census `:closure-file-count` fix is scheduled, not run; README's "See it run" front door carries up the sibling busy-tuesday README's own honesty disclosure without touching a byte of the demo; D8-4 and ruling 1's own option (b) stay explicitly unruled
