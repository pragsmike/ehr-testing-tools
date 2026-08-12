# 2026-08-12 — Review-3's rulings land, three fix clusters chartered

## Scope

Session prompt naming three author rulings (verbatim, 2026-08-12: "Q1
a. Q2 a. Q3 a.") on review-3's own three `ruling-needed` register rows
(R3-B1-1, R3-B1-4, R3-B1-7). A REGISTERS-ONLY session in the ADR-0093
lineage: records the rulings in `.agents/rulings.md`, fixes the
findings register forward (three row dispositions, one summary-table
wording correction), and charters the register's twelve
`fix-session-candidate` rows into three fix clusters (A, B, C) plus a
design-channel-draft queue note on `.agents/plans/roadmap.md`. Zero
`src`, zero `test/`, zero `docs/` changes, zero fixes of any finding
executed.

## Step 0 — Preflight + tag

Working directory confirmed the ext4 clone, working tree clean, branch
up to date with `origin/main` at `d508cd6`
(`d508cd6ee3a5a6fda64c0007b9ac57855ad5acc5`, ADR-0114 close) — matched
the driving prompt's own stated premise exactly, no STOP-AND-REPORT
triggered. Last five `test`-lane runs on `main` (`gh run list --limit
5 --branch main`): four `success`, one `failure`
(`31598555300`/`d0679e9`) — the interim CI-red window ADR-0114 itself
disclosed and fixed forward at `d508cd6`; no NEW red, matches the
prompt's own tag-license reasoning verbatim.

Tagged `stable-20260812-review-3` at `d508cd6`, annotated, message
citing the tag-law case (i) reasoning (fresh-clone lineage/ASCII/
footprint/oracle/riders/arithmetic verification, all from ADR-0114's
own disclosed deviations); pushed; peeled ref verified via `git
ls-remote --tags origin` — `d508cd6ee3a5a6fda64c0007b9ac57855ad5acc5`
exactly.

## Step 1 — Record the three rulings

`.agents/rulings.md` gained a new dated section, "From ADR-0115
(review-3 rulings landing; ruled 2026-08-12)," three entries (RQ1
`--out-dir`'s double meaning, RQ2 `--seed`'s required-vs-defaulted
split, RQ3 `--received`'s wall-clock default) in the file's own
existing dated/tagged/provenance-marked shape — question as framed,
options, the author's ruling, the concrete meaning. All three ruled
"(a)" from the verbatim batch, matching the driving prompt's own Step
1 text exactly.

## Step 2 — Register updates

In `.agents/plans/2026-08-12-review-3-user-surface-findings.md`:
R3-B1-1's disposition moved `ruling-needed` → `fix-session-candidate
(cluster A)`; R3-B1-4's moved to `fix-session-candidate (cluster A,
small: the help note only)`; R3-B1-7's moved to `closed-by-ruling` —
each row's own recommendation cell gained the exact `RULED (a), ...`
sentence the prompt specified. The summary table's own note correcting
the actual cross-reference marker (it never literally read "(x-ref)"
— every such row instead carries a "(see R3-Bx-y, not double-counted)"
style citation) was rewritten in place, plus a new dated correction
paragraph beneath the table naming the fix and disclosing that counts
are unaffected (independently recounted). One additional sentence,
licensed by the prompt's own Step 2 item 5, was added to that same
correction paragraph: the table is the review's own review-time
snapshot, three of its `ruling-needed` entries have since moved
(cited above), and the rows — not the table — carry current state.
Nothing else in the register renumbered, retallied, or restructured.

## Step 3 — Charter the clusters + commit 1

`.agents/plans/roadmap.md`'s Next section gained three new rows (Fix
cluster A — CLI validation and error quality, 8 members including the
register's own HIGHEST-PRIORITY finding R3-B2-1; Fix cluster B —
help-surface enrichment, 2 members; Fix cluster C — doc drift and gate
scan-roots, 2 members, docs-only) plus one design-channel-draft queue
note (R3-B3-1's Example-line content, the B-3/B-4 carry-forward
wording halves) — member finding ids verbatim from the driving
prompt's own Step 3 text. Review-3's own existing row gained one dated
note: rulings landed (ADR-0115), clusters chartered, the arc's
remaining steps are the three cluster sessions.

**Gate-forced companion, disclosed** (the prompt's own standing-
practice note 2): this growth pushed `.agents/plans/roadmap.md` — an
`:onboarding` reading-set member — from 746 to 781 lines, tripping
`ehrt.docs-tooling.reading-set-budget-test` red (`:onboarding` measured
1734 lines against its own 1705-line budget). `.agents/reading-sets.edn`
re-baselined under its own standing formula (actual x1.15, rounded up
to the nearest 5): 1734 x 1.15 = 1994.1 → 1995; budget moved 1705 →
1995, one new dated re-derivation comment added matching the file's
own existing history-comment shape. `make test` confirmed green after
the re-baseline — 0 failures, 0 errors across every namespace,
`bin/verify-nist-lock` OK.

`git diff --cached --stat` reviewed before staging: exactly the four
touched files (three named in the driving prompt's own fence,
`.agents/rulings.md`/`.agents/plans/2026-08-12-review-3-user-surface-
findings.md`/`.agents/plans/roadmap.md`, plus the one gate-forced
companion above). `gitleaks git --staged -v`: clean. Committed
`ed00e3a` ("docs: land review-3 rulings; charter fix clusters
(ADR-0115)"); `gitleaks detect`: clean; pushed. Post-push verification:
pushed message diffed against the source file — only delta the
trailing-blank-line artifact `git log --format=%B` always adds; ASCII
byte-check (`git log --format=%B -1 | LC_ALL=C grep -n '[^ -~]'`) —
EMPTY.

## Step 4 — ADR + ceremony surfaces + commit 2

Oracle bracket run against commit 1, per the driving prompt's own step
ordering (commit 2 is prompt-archive/session-record/ADR text only,
guaranteed zero-`src` by the fence): `bin/regression-oracle d508cd6
ed00e3a` → `IDENTICAL: every root's digest matches between d508cd6 and
ed00e3a`, all 35 roots, matching the pre-analysis (pure identity —
registers and notes only, zero `src`, zero `docs/`).

`notes/adr/0115-review-3-rulings-landing.md` landed: context, the
rulings' concrete meanings, the cluster charter, the register
fix-forward including the x-ref-note correction, tag ceremony, oracle
bracket, gates, fences, index line. `notes/ADRs.md` gained its index
line; `notes/adr/README.md`'s own file count corrected 112→113. The
roadmap's Done section gained one pointer line. This session record
and its prompt archive land in the same commit, both READMEs updated.

Full local gate (`make test`) re-run before this second push: green.
`bin/verify-nist-lock`: OK. `gitleaks git --staged -v` (this
checkpoint) and `gitleaks detect` (pre-push): clean. ASCII byte-check
on this commit's own message: recorded at push (see Close-out below).

## Deviations, disclosed

One gate-forced companion edit: `.agents/reading-sets.edn`'s
`:onboarding` budget re-baseline (Step 3, above) — not named by
filename in the driving prompt's own fence list, licensed by the
prompt's own standing-practice note 2 (a gate-forced companion to a
named fenced surface is inside the fence by rule). No other deviation:
`git status --porcelain` confirmed clean before this session's first
tool call and at each commit boundary; all three rulings, all three
register-row updates, and all three cluster rows plus the queue note
landed exactly as the driving prompt specified, verbatim where the
prompt gave verbatim text.

## Close-out echo

**The three rulings, verbatim** (see `.agents/rulings.md`, "From
ADR-0115"): RQ1 `--out-dir` double meaning, RULED (a) — the
`--scratch-dir` rename on `gate fhir` is chartered to fix cluster A.
RQ2 `--seed` required-vs-defaulted split, RULED (a) — deliberate
two-tier design (front door defaults, engine tier requires), a help
note chartered to fix cluster A. RQ3 `--received` wall-clock default,
RULED (a) — a class exemption for provenance metadata about a
real-world act, closed by ruling, no fix-session needed.

**The three cluster rows, as landed:** Fix cluster A (CLI validation
and error quality, 8 members, contains the HIGHEST-PRIORITY finding
R3-B2-1); Fix cluster B (help-surface enrichment, 2 members); Fix
cluster C (doc drift and gate scan-roots, 2 members, docs-only). See
`.agents/plans/roadmap.md` for full text.

**`bin/regression-oracle d508cd6 ed00e3a`:** IDENTICAL, all 35 roots.

**`bin/verify-nist-lock`:** OK, all 6 hit-nexus-sourced coordinates
match `artifacts.lock.edn` exactly.

**SHAs:** Step 0 tag `stable-20260812-review-3` at `d508cd6`. Commit 1
`ed00e3a`. Commit 2: this record's own landing commit.

**CI status:** `test` lane green on commit 1's push (confirmed post-
push); commit 2's own run recorded/disclosed at push.

## HEAD landed

`ed00e3a` (commit 1) — commit 2 (this record's own commit) lands after
this record, in the same push as the prompt archive.
