## ADR-0079 — Lint family: the small gates land together

**Status:** Accepted (author-directed, autonomous session per R30), 2026-08-07.

### Context

Prior: `notes/adr/0078-result-or-loud.md` closed the findings
register's single highest-severity cluster (D4-1/D3-4/D8-2/D8-3) and
recorded all five of the author's rulings on the register verbatim as
AR-RL-5, distributing their execution across that session, this one,
and the arc close. This session is fix session 2: eight small,
mechanical cargo items, each closing one register row named in
AR-RL-5(1) and (3), plus D2-3/D2-6/D3-3/D7-1 (register rows the
driving prompt names directly, not themselves part of AR-RL-5's own
five numbered items — the tripwire and the seed are AR-RL-5(1)/(3);
the façade gate, both lints, the protection, and the citation fix are
this session's own cargo per the prompt, closing register rows the
prompt cites by ID).

R30 ceremony; CI-red policy unchanged from ADR-0078 (reds witnessed
in-session via git-stash-and-restore, never committed). Read-first
(this session): the register's D2-3, D2-4, D2-5, D2-6, D3-2, D3-3,
D7-1 rows in full; `.agents/state.md`'s header; `ehrt.sim.interface`;
`.agents/plans/roadmap.md`'s Deferred section (including the
compliant-disclosure precedent rows D7-3 blessed); the four
NOTICE/PROVENANCE-hashed fixture files and their tables;
`components/sim-engine/test/ehrt/sim_engine/engine_test.clj`'s flaked
spec; `io_vocabulary_lint_test.clj`'s own pattern; both `repo-review`
SKILL.md copies.

### Decision

Author rulings, recorded verbatim (design channel, 2026-08-07). `[A]`
author-ruled, `[C]` channel-inferred.

1. **AR-LF-0 `[A — tag law, case (ii); debt recorded in ADR-0078]`.**
   Annotated `stable-20260807-result-or-loud` at `758f3af`, message
   "result or loud landed, design-channel-verified 2026-08-07
   (ADR-0078)"; pushed; peeled ref verified against
   `758f3af298af79f7d6058255feee0258f83ec5cf`. **Executed Step 0.**

2. **AR-LF-1 `[A — D2-4 ruled adopted; C for shape]`.** The `state.md`
   staleness tripwire: `state_staleness_tripwire_test.clj` asserts
   `state.md`'s own cited regeneration ADR is the newest
   `*-arc-close.md` on disk. **Executed Step 1** — commit `9b5c2e1`.

3. **AR-LF-2 `[C — D2-3]`.** The façade surface-identity gate:
   `interface_surface_test.clj` freezes `ehrt.sim.interface`'s public
   var/arity surface against a committed baseline (AR-M4-3).
   **Executed Step 1** — commit `9b5c2e1`.

4. **AR-LF-3 `[C — D2-5 + D2-6]`.** Two lints, one family: (i) the
   Deferred in-place-closure lint
   (`roadmap_deferred_closure_lint_test.clj`); (ii) the test-source
   live-path lint (`test_source_live_path_lint_test.clj`). **Executed
   Step 1** — commit `9b5c2e1`. Both required a live design correction
   during authoring, disclosed inline in their own docstrings and
   below rather than folded in silently.

5. **AR-LF-4 `[C — D3-3]`.** `.gitattributes` gains `-text` for the
   four unprotected hash-recorded fixture files. **Executed Step 2** —
   commit `13cc046`.

6. **AR-LF-5 `[A — D3-2's ruled middle path]`.** The flaked engine
   spec's seed pinned. **Executed Step 2** — commit `13cc046`.

7. **AR-LF-6 `[C — D7-1 + the skill amendment]`.** `rulings.md`'s
   citation corrected; both `repo-review` SKILL.md mirrors amended.
   **Executed Step 2** — commit `13cc046`.

8. **AR-LF-7 `[C — scope]` (fences).** Held — see Fences, below.

### Design corrections found live, disclosed rather than folded in

Both AR-LF-3 lints were authored, run against the live tree, found to
false-positive, and narrowed — the pattern fixed, never the row/file
the false positive landed on (the same discipline the Deferred lint
itself enforces, applied to its own authoring):

- **The Deferred lint**, first draft: a case-insensitive
  `\b(resolved|closed|fixed)\b` match. Run against the live Deferred
  section, it flagged `- UTI's own ed_bundle.json ...` — a row
  containing the ordinary prose "a real bug found and fixed mid-step,"
  describing an unrelated incident, not this row closing. Narrowed to
  a case-SENSITIVE, all-caps-only match (`RESOLVED|CLOSED|FIXED`),
  matching the D2-5 finding's own literal quoted shape and the
  convention the two real compliant rows actually use ("**CLOSED
  2026-08-07...**", "CLOSED this session — see Done"). Re-run: zero
  false positives, both compliant rows pass, the injected-and-reverted
  probe violation (below) still caught.

- **The live-path lint**, first draft: ANY `.listFiles`/`.list(` call
  in a test source outside `ehrt.docs-tooling.*`. Run against the live
  tree, it flagged five files: `run_test.clj` itself (the FIXED test,
  listing its own `temp-dir-path*` for a debug message),
  `display_test.clj` (listing the allowlisted `test-fixtures` root),
  and three `corpus`/`corpus-io` tests listing their own `out-dir`
  scratch directories — every hit a test listing a directory IT built
  or an allowlisted root, none the busy-weekday hazard. Narrowed to
  literal-string-argument calls only (`(io/file "...")` or a bare
  string) — a dynamically-bound symbol (`out-dir`, `fixture-dir`,
  `dir-file`) is, in every live case, a test-built temp dir or a
  threaded-in fixture path; a literal string naming a live repo path
  is the actual hazard shape. Re-run: zero false positives against the
  live tree.

Both corrections are recorded in the landed test files' own
docstrings (`AGENTS.md`'s "the design lives beside the code" style),
not only here.

### Red transcripts, witnessed in-session

All four gates witnessed red via the same technique: a temporary,
uncommitted edit against the live tree (backed up, applied, tested,
restored byte-for-byte, re-tested green), never a committed red.

**The state.md tripwire.** `state.md`'s cited ADR temporarily changed
from `0074` to `0068` (an older arc close):

```
FAIL in (state-md-cites-the-newest-arc-close-as-its-own-regeneration-point-test)
.agents/state.md's header cites ADR-0068 as its own regeneration point, but
the newest arc-close ADR on disk is ADR-0074 -- .agents/state.md is stale
(AR-C-1, D2-4): an arc close landed without regenerating it.
```

Restored byte-for-byte (`diff` confirmed clean); re-run: 4 assertions,
0 failures.

**The façade surface-identity gate.** `ehrt.sim.interface` temporarily
gained one extra public var (`extra-fn`):

```
FAIL in (sim-interface-surface-matches-its-frozen-baseline-test)
ehrt.sim.interface's live public surface has drifted from its frozen
baseline (AR-M4-3, lint family AR-LF-2, D2-3) ...
actual: (not (= {...9 keys...} {...10 keys, "extra-fn" #{1}, ...}))
```

Restored byte-for-byte; re-run: 4 assertions, 0 failures.

**The Deferred in-place-closure lint.** A probe-only violating row
temporarily inserted at the top of the live Deferred section:

```
FAIL in (deferred-rows-that-close-in-place-disclose-their-own-relocation-test)
Deferred row(s) close in place ('RESOLVED'/'CLOSED'/'FIXED') without
disclosing where the closed content relocated to (D2-5, D7-3's compliant
shape) ...
actual: (not (empty? ("- **A probe-only violating row** CLOSED this
session, no disclosure of where it went.")))
```

Restored byte-for-byte; re-run: 7 assertions, 0 failures.

**The test-source live-path lint.** A probe-only `deftest` temporarily
appended to `run_test.clj` reading `config/busy-weekday.md` by literal
path:

```
FAIL in (no-literal-live-path-directory-listings-outside-allowlist-test)
components/sim/test/ehrt/sim/run_test.clj (ns ehrt.sim.run-test) lists a
literal, live repo path directly (["config/busy-weekday.md"]) -- build a
temp dir instead (AR-BB2-R, lint family AR-LF-3(ii), D2-6) ...
```

Restored byte-for-byte; re-run: 118 assertions, 0 failures.

### New lint findings against the live tree (disclosed, not fixed here)

Per AR-LF-7's own fence: both new lints were run against the live tree
BEFORE landing. Both came back clean (zero hits) once narrowed — no
new finding to disclose from either. The Deferred lint's own live run
found exactly the two known-compliant rows (census tool refinements,
lookup-table `time`), no violation. The live-path lint's own live run
found zero literal-path `.listFiles`/`.list` calls outside the
allowlist anywhere in the tree.

**One register-style gap named, not fixed (AR-LF-4's own instruction):**
`notice_verbatim_test`'s scope does not cover the v2-nist NOTICE.md
table (a 2-column `| File | sha256 |` shape, not the 5-column
provenance-table header the gate recognizes) or the simhospital
PROVENANCE.md hash (prose, not a table, in a file not named NOTICE/
NOTICE.md at all). Both files' hashes are STILL manually verified
correct (re-checked before and after this session's own `.gitattributes`
edit, matching their tables exactly) — this is a coverage gap in the
standing gate, not an active drift. Extending the gate to recognize a
second table shape and a differently-named file was judged to balloon
past "lands small" (two new parsing shapes, not one), so it stays a
named finding for the arc close / next review rather than built here.

### Verification

- `clojure -M:poly check`: OK, both checkpoints.
- Full suite (`clojure -M:poly test :all skip:integration`), Step 0
  baseline against `758f3af` (before this session's own edits): 511
  assertions, 0 failures, 0 errors (development/conformance project
  lane; the `ehrt-cli` project lane's own count matches). Re-run after
  landing both commits, including all four new gate tests and the
  pinned-seed spec (seed `-60645` confirmed printed in the run output
  for `every-churned-run-satisfies-the-invariant-catalog`, both
  project lanes): 0 failures, 0 errors.
- `gitleaks git --staged -v`: clean, both commits this session
  (`9b5c2e1`, `13cc046`); the pre-push hook ran it again on both
  pushes, clean.
- Post-push message verification: both commits' pushed messages
  diffed against their own message files — only the known `git log
  --format=%B`-trailing-blank-line artifact, no other mismatch.
- `bin/regression-oracle 758f3af 13cc046`: **IDENTICAL** — every one of
  the twenty-seven roots' digests matches between baseline and this
  session's own closing tip; soundness "yes outside ns form" (expected
  — every edit this session is test-only, `.gitattributes`, or docs;
  no `src/` happy-path code changed).
- CI, both pushes, watched to conclusion (not assumed), both watched
  live start-to-finish: `9b5c2e1` **success** (run `31242100588`,
  3m8s); `13cc046` **success** (run `31242152597`, 3m12s). Last-five
  CI conclusions on `main` at Step 0
  preflight: `758f3af` success, `3684a30` success, `90432ad` success,
  `93bd9a6` success, `075db9b` success — all green, no red window to
  disclose (quality riders AR-QR-3's own widened five-run check).

### Fences

Src/test edits landed ONLY in the eight cargo items AR-LF-1 through
AR-LF-6 name: the four new gate/lint test files, `.gitattributes`, the
seed pin, `rulings.md`'s citation, both SKILL.md mirrors. No sweep of
anything either lint newly flagged beyond what the driving prompt
named — both came back clean against the live tree, so nothing was
found to sweep. Standing untracked files untouched. Oracle: all
twenty-seven roots IDENTICAL (no src-affecting change
landed this session — every edit is test-only, `.gitattributes`, or
docs — so no happy-path output could have changed).

### Successor tag debt, recorded here

**The next session that opens fresh work tags
`stable-20260807-lint-family` at THIS session's own closing tip
(`13cc046`), under standing ceremony** — the same tag-law case (ii)
pattern ADR-0078 used for its own predecessor.

### Index line

```
- 2026-08-07 — lint-family — ADR-0079
```

(appended to `.agents/plans/roadmap.md`'s own Done section.)

`notes/adr/README.md`'s own file count corrects 76→77, verified by
`ls notes/adr/*.md | grep -v README | wc -l`, not arithmetic.

### The horizon, restated unchanged

This session closed eight small, mechanical register rows. Untouched,
carried forward unchanged: the EncounterEnd design pass, Wave E's own
register, vendoring batch 4, publish-prep, the generalized multi-
surface-law drift scaffold (AR-RL-5(2), deferred with a named
trigger), pairing-as-data (ruled in, design pass continuing in the
design channel in parallel with this session per AR-RL-5(4)), the
wellness-encounters re-surface per D7-6, the intake watch-list for
review 2, and the notice-verbatim coverage gap named above.

**What DOES change:** after design-channel verification of this
session's own landing, the ARC CLOSE follows (ADR-0080): rulings
appends (the multi-seed codification, AR-RL-5(5); plausibly "I/O
speaks Result or fails loud" as this arc's own standing law); state
regeneration; budgets; rotation; the wellness-encounters re-surface
per D7-6; the intake watch-list for review 2.

### Consequence

Four small gates now stand where session discipline alone stood
before: `state.md` staleness is caught the same run an arc close skips
regenerating it; `ehrt.sim.interface`'s frozen surface breaks loudly
instead of only as a downstream compile failure; a Deferred row
closing in place without disclosure is caught the same way
`myocardial_infarction.json`'s own incident would have been, had this
gate existed then; a test reaching for a bare live-path directory
listing fails immediately instead of waiting for the busy-weekday
class to recur. The one spec that has actually flaked reproduces
deterministically from here forward. Two citations now resolve
exactly where they claim to.

### Index summary (moved verbatim from notes/ADRs.md by ADR-0143, 2026-08-16)

Lint family: the small gates land together — fix session 2 lands eight small, mechanical cargo items: the `state.md` staleness tripwire, the `ehrt.sim.interface` façade surface-identity gate, two Deferred-section/live-path lints (each corrected live against a real false positive found during authoring, disclosed in their own docstrings), `-text` protection for four unprotected hash-recorded fixture files, the flaked engine spec's seed pinned, and two small citation/skill-doc fixes — all four gates' reds witnessed via git-stash-and-restore before landing green
