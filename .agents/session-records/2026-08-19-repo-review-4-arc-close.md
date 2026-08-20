# 2026-08-19 -- Repo-review-4 arc close: audited, residue dispositioned, review 5 chartered

**ADR:** [`notes/adr/0159-review-4-arc-close.md`](../../notes/adr/0159-review-4-arc-close.md)
**Prompt:** [`.agents/prompts/2026-08-19-repo-review-4-arc-close.md`](../prompts/2026-08-19-repo-review-4-arc-close.md)
**Mode:** R30, autonomous. **Docs and registers only** -- zero `src`,
zero `test`, zero `bin`, zero skill edits.

**A date note.** The prompt names the archive `2026-08-19-...` and this
record pairs with it; the session's own acts fall after local midnight,
so ADR-0159 and every dated register append read **2026-08-20**, which
is the commit date. The same skew, in the other direction, is on the
record at ADR-0155 (record `2026-08-18-*`, ADR dated 2026-08-19).

## Step 0

`bin/preflight` plain, **exit 0, no findings**, every check disclosed:

- last five CI runs on `main` all green: `e967fd7`, `6ed767c`,
  `d02a085`, `3c4e346`, `bdc10ee` (newest first,
  `2026-08-20T00:50:00Z` down to `2026-08-19T19:18:33Z`)
- edit root `/home/mg/src/ehr-testing-tools`, not under `/mnt/`;
  `core.fileMode` **true**; `core.ignorecase` **unset** (ADR-0157's two
  checks, still holding after the author's payment at ADR-0158)
- working tree clean, untracked included
- local HEAD `e967fd7c5dde7070bedc4f983c039f279fd6eec7` ==
  `origin/main`
- last `stable-*` tag
  `stable-20260819-review-4-fix-4-sampling-and-provenance` at
  `6ed767ccbcbfeecb10f502abc47283dfa4f35f5f`; **HEAD untagged,
  DISCLOSED, and no tag owed** -- as the prompt states

Baseline `make test`, **redirect not pipe**, `MAKE_EXIT` written to its
own file, wrapper ending `exit "$MAKE_EXIT"`:

    MAKE_EXIT=0
    364 zero-failure blocks / 4,070 tests / 18,304 assertions
    grep -cE '^(FAIL|ERROR) in'  ->  0

**Reconciles exactly** against ADR-0158's own recorded CLOSE figure --
which that ADR carries itself, the practice `build-session` step 14
landed this arc, working on its first use. `clojure -M:poly check` and
`bin/verify-nist-lock` green inside the target.

Reading sets at Step 0, from the generated `state-derived.md`:
`:corpus` 1832/2045, `:docs` 735/785, `:judge` 922/1000, `:onboarding`
**1484/1530**, `:sim` 1274/1405. All under budget, no baseline moved.
**ADR-0158 records `:onboarding` 1482 for this same tree; the generated
register says 1484.** Recorded as errata (8), not corrected (`R-RP`).

## The audit, in four counts

| | count |
|---|---|
| (a) FIXED-append **misses** | **0** of 38 pairs (31 name the row id, 7 carried by substance, each verified by reading) |
| (b) residue **partition** | **34** = 24 close-as-fine confirmed + 8 intake carried + 1 intake also rowed + 1 superseded |
| (c) **unrecorded** errata | **1** of the prompt's seven (the four-reading-sets claim), **+3** found by this close |
| (d) **ledger delta** | **0**, in every session and in the total (12 + 11 + 2 + 13 = 38) |

Plus (e): **all ten R4-Q rulings landed** -- ADR-0155 three, ADR-0156
four, ADR-0158 three, ADR-0157 none (the arc's one pure fix session).

Extraction was scripted over all 72 rows, not sampled, and reproduces
the register's own 72 and its per-section counts. The disposition cell
is the LAST cell in the eight dimension tables and the SECOND-TO-LAST in
the three sub-agent tables, which carry an extra `provenance` column --
a naive last-cell read reports the provenance text as the disposition
for 37 of 72 rows, which is how the first extraction went wrong here.

**The prompt's own arithmetic corrected**, in ADR-0139's tradition: it
states "48 `FIXED ADR-015x` appends; residue 24-ish". The mechanical
count is **37 occurrences** (35 plain, 2 `PARTLY FIXED`), all inside row
cells, **38 rows moved** counting D8-1's `CARRIED INTO`, **34 residue**.
The likeliest origin of 48 is 38 rows + 10 rulings.

## Cheap re-derivations run at this tip (six close-as-fine rows)

| row | result |
|---|---|
| D1-3 | 16/16 `state.md` paths resolve (five `.agents/`-relative, `sim-theory.edn` under `components/sim/docs/`) |
| D1-4 | `state.md` **119/120**, unchanged across the arc |
| D2-3 | mirror byte-identical, same file set, zero orphans -- **59** files, not the row's 60 |
| D3-3 | **0 CR bytes** in all six `demos/traces/**/ground-truth.edn`; six `-text` rationales intact |
| D5-4 | no second mirrored pair; repeats D2-3's 60 |
| L2-11 | zero live gate commands piped into `tee`/`tail`/`head` across `bin`, `.githooks`, `Makefile`, `.github`, `.agents/skills` |

## Findings opened, all rowed, none fixed

- **F-1** -- `c509e46` (ADR-0152's close) inserted its roadmap row
  *inside* ADR-0150's row; five continuation lines now sit under the
  wrong slug. `roadmap-lint-test` is green on both, because it gates row
  SHAPE, not row OWNERSHIP. Proven with `git log -L`.
- **F-2** -- ADR-0155 gave `R-full-suite-before-push` its whole
  `exit "$MAKE_EXIT"` clause and **deleted** the row's previous inline
  citation without adding one; the arc's other two widenings name their
  ADR inline. `rulings-lint-test` is green.
- **F-3** -- the plan says `state-derived.md` is "counted by four reading
  sets". It is in **zero**.
- **F-4** -- three figures disagree with the tree: `:onboarding` 1482 vs
  1484, the mirror's 60 vs 59, the `4/4` denominator after three `x/5`s.

F-1 and F-2 share one roadmap row,
`roadmap.md#register-gate-row-ownership` (PRIORITY 3), in ADR-0158's
D7-3 shape. F-3 and F-4 are errata standing under `R-RP`, recorded in
the register's close note and the plan.

## The fence, widened by one file and disclosed

`.agents/state.md` is **not** on the prompt's list and was regenerated
anyway. `state-staleness-tripwire-test` asserts state.md cites the
newest arc-close ADR on disk; this ADR's own heading says "arc close",
so state.md's ADR-0147 citation went stale the moment the file existed.
Renaming the heading to fall outside the gate's population is forbidden
by `rulings.md#R-never-dodge-a-gate-by-population` -- the ruling that
exists because ADR-0139 stood at this exact fork -- and
`rulings.md#R-state-regeneration` requires the regeneration at every arc
close regardless. One defensible reading, so **fix-forward with
disclosure** (`R-stop-only-on-two-defensible-readings`), not a STOP.

**119 lines before, 119 after**: four paragraphs compacted to pay for
the header's new citation, review 4's errata count, the population
bullet restated as the class both reviews found, and ADR-0157's
environment additions. `R-budget-stop` makes raising the cap
unavailable, and D1-4 named that one line of headroom as a tripwire for
exactly this session.

## Registers

- **Register** (`2026-08-18-repo-review-findings.md`): 34 dated appends,
  one per residue row, in the fix sessions' own idiom; an `ARC CLOSED`
  header line; a close note carrying the final tally. **No review-day
  row rewritten, no score edited.**
- **Plan**: Part 2's all-landed line, plus one dated erratum for the
  four-reading-sets claim.
- **Roadmap**: `#repo-review-4` CLOSED under `## Done`;
  `#repo-review-5` opened at PRIORITY 2 with the due point;
  `#register-gate-row-ownership` opened at PRIORITY 3;
  `#oracle-coverage-roots` **3 -> 22**, moved to the end of `## Next`
  below live work (channel-proposed 2026-08-19, author-seen). Every row
  within the six-line cap, priorities ascending.
- **`rulings.md`: untouched.** Grepped before asserting: the arc landed
  two rows and widened two in place, 113 -> 115, and every law it made
  has a row. One further priority move (`#attic-rotation-law`) is
  **proposed in the ADR, not taken**.

## Close verification

Full `make test`, redirect not pipe, `MAKE_EXIT` in its own file,
wrapper ending `exit "$MAKE_EXIT"`:

    MAKE_EXIT=0
    364 zero-failure blocks / 4,070 tests / 18,304 assertions
    grep -cE '^(FAIL|ERROR) in'  ->  0

**Delta against Step 0: ZERO in all three figures**, which is the
prediction this session stated up front and the right answer for a
commit that changes only prose and the three generated files that
follow from it. `clojure -M:poly check` and `bin/verify-nist-lock`
green inside the target.

**Gates this session's own edits had to satisfy**, all green:
`roadmap-lint-test` (four new/moved rows, every one within the six-line
cap, priorities 2,3,4,5,6,8..22 ascending), `state-residue-test` (119
lines, under the 120 cap), `state-staleness-tripwire-test` (state.md now
cites ADR-0159, the newest arc close on disk),
`every-arc-close-adr-carries-the-filename-convention-test`
(`0159-review-4-arc-close.md` matches the convention),
`adr-index-test`, `index-completeness-test`,
`prompt-record-pairing-test`, `state-derived-test`.

Regenerated once, at the close: `make adr-index` and `make state-derived`
(which also rewrote both `INDEX.md` and re-ran the `pipeline` prerequisite,
leaving `docs/dev/pipeline.md` byte-identical). `make traces` was not
run and owes nothing: no input under `demos/`, `bin/` or `components/`
moved.

**Reading sets at close** (`R-register-hygiene-at-close`), all five
green, no baseline moved:

| set | Step 0 | close | budget | headroom |
|---|---|---|---|---|
| `:corpus` | 1832 | 1832 | 2045 | 213 |
| `:docs` | 735 | 735 | 785 | 50 |
| `:judge` | 922 | 922 | 1000 | 78 |
| `:onboarding` | 1484 | **1502** | 1530 | **28** |
| `:sim` | 1274 | 1274 | 1405 | 131 |

`:onboarding` is the only mover: **+18** from `roadmap.md`'s THREE new
rows (`#repo-review-5`, `#register-gate-row-ownership`, and F-5's
`#oracle-coverage-gate-integration-half`, added after the first push)
and the closed row's move to `## Done`, offset by `state.md` holding at
119. **28 lines of headroom**, tightest of the five for the second review
running, which is why it is watch row W-13 -- and W-13's bar of "under
~30 lines" is already met at this close's own tip.

**The oracle is untouched and UNRUN.** No `src`, no `test`, no
resource, no digest source moved, so no root can have moved. Per
`rulings.md#R-oracle-script-contract` an unrun oracle is **UNCLAIMED,
not asserted-identical**: this session makes no regression-oracle claim
and none is owed. `R-red-pushed-with-green` is **n/a** -- docs-only, no
red planted, no enforcement test added, so there is no red-first commit
to pair.

## A fifth finding, after the push: the nightly went red

`bin/preflight` re-run after the addendum push **exited 1** on a real
FINDING: `Integration` run **32344505291**, scheduled, at `e967fd7c`,
**completed / failure** 2026-08-20T07:32:42Z -- before either of this
session's commits, so not this session's doing.

**F-5.** `projects/integration/test/ehrt/integration/oracle_coverage_test.clj:95`
NPEs before asserting anything: it searches `"(def <name>"` while
`digest.clj:617,626` writes `(def ^:private <name>`. Proven without
running: `grep -c '(def witnessed-event-kinds'` = **0**,
`grep -c '(def \^:private witnessed-event-kinds'` = **1**.

ADR-0156 fixed this bug in the OTHER half of the same gate
(`docs-tooling/.../oracle_coverage_test.clj:71-72` tries both forms) --
its deviation 2 records the refinement -- and left the integration half
broken. `make test`, `make ci-parity` and CI's `test` workflow all pass
`skip:integration`, so four fix sessions and this close's three full
suites were green over it. Last nightly before the arc: **32228155848 at
`7d998f01`, success**. The file landed at `079fe80` and never changed.
**Today's nightly is its first-ever run.**

**Rowed, not fixed**, twice-fenced (no audit finding fixed here; the
repair is in `test`): `roadmap.md#oracle-coverage-gate-integration-half`,
**PRIORITY 1**. The session that takes it owes a `make integration` run,
not a `make test` one.

It also sharpens watch row **W-1**: a gate born in a tier its landing
session never executed is worse than one born red, because there is no
red to disposition. Recorded in ADR-0159's second addendum.

## Read-back against the prompt's fence

Files touched, against the prompt's own list -- `.agents/plans/2026-08-18-repo-review-findings.md`,
`.agents/plans/2026-08-18-repo-review-4-plan.md`, `.agents/plans/roadmap.md`,
`notes/adr/0159-review-4-arc-close.md`, the prompt archive, this record,
and the generated `.agents/state-derived.md` + both `INDEX.md` +
`notes/ADRs.md`. **`rulings.md` untouched**, as the prompt predicted.
**Beyond the list, and disclosed above: `.agents/state.md`**, forced by
the arc-close tripwire and required by `R-state-regeneration`.
**Zero `src`, zero `test`, zero `bin`, zero skill edits, zero fixes of
any audit finding.**

The prompt's four counts: **(a) misses 0**, **(b) partition
24/8/1/1 = 34**, **(c) unrecorded errata 1 of the prompt's seven, plus
3 this close found**, **(d) ledger delta 0**. **Close-suite delta:
ZERO.**

Findings opened by this close: **five** -- F-1 through F-4 above, plus
**F-5** after the push. Fixed: **zero**. Rowed: **all five**, across
`#register-gate-row-ownership` (F-1, F-2),
`#oracle-coverage-gate-integration-half` (F-5), and the register's own
close note plus the plan's dated erratum (F-3, F-4).

## Post-push verification

`bin/post-push-verify` after `0e72ed4`, exit 0, all three green:

- remote tip `0e72ed43c71750781b48b59e0363d607739e865b` == HEAD
- every commit message in `e967fd7c..0e72ed43` pure ASCII
- CI run `32347912626` reported once, not awaited (AR-CI-4)

That run later concluded **completed / success** at
`0e72ed43c71750781b48b59e0363d607739e865b`, confirmed by this session's
own `gh run view` while open.

## Tag

**None owed at Step 0** (`bin/preflight` check 5, disclosed above). The
arc-close tag was licensed by the prompt, payable in session if the tip
run concluded success while the session stayed open. **It did, and the
tag is PAID** -- `rulings.md#R-session-verifies-ci-via-gh`'s condition
met by this session's own `gh run view 32347912626` = completed/success:

    bin/tag-ceremony stable-20260820-review-4-arc-close \
      0e72ed43c71750781b48b59e0363d607739e865b <message-file> --push

    OK: created annotated tag at 0e72ed43c71750781b48b59e0363d607739e865b
    OK: pushed refs/tags/stable-20260820-review-4-arc-close
    OK: remote peeled ref matches target exactly

**No tag is owed at the next Step 0, and no mechanical debt is carried**
-- unlike ADR-0139, whose own close tag went local-only and unpushed for
two sessions. Recorded also in ADR-0159's addendum.
