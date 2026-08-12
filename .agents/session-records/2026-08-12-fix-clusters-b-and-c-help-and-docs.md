# 2026-08-12 — Fix clusters B and C: help enrichment, doc drift, scan roots

## Scope

Session prompt landing review-3's two remaining fix clusters
(chartered `notes/ADRs.md` ADR-0115): C (doc drift + lint scan roots,
commit 1) and B (help-surface enrichment, commit 2). R30 standing
ceremony (commit and push at each checkpoint, unattended) — the prompt
named no prepare-only override.

## Step 0 — Preflight + tag

Working directory confirmed the ext4 clone. `git fetch` confirmed
`origin/main` at `c68ec3e` (`c68ec3efcbe2421888071a23e7225c6716f7c6fa`,
ADR-0117 close) — matched the driving prompt's own stated premise
exactly. Last five `test`-lane runs on `main` (`gh run list --limit 5
--branch main`): all `completed`/`success`, no red. Tagged
`stable-20260812-fix-cluster-a` at `c68ec3e`, annotated; pushed; peeled
ref verified via `git ls-remote --tags origin` — exact match.

## Step 1 — Commit 1 (cluster C): docs drift + lint scan-root widening

Widened `invocation_lint_test.clj`'s own `scan-sources` to `demos/**`
and `.github/**` first — order matters, the widening IS the red. It
went RED on exactly one of the two drift classes the driving prompt
named (R3-B5-4's issue-template alias); R3-B5-3's own `demos/traces/**`
stale config-header drift did not trip anything, since it lives in
unfenced EDN comments the lint's own two checks (a literal `clojure
-M:cli` substring match, and fenced-\`\`\`bash/sh flag-value
resolution) structurally cannot see. **STOP-AND-REPORT raised**,
presenting the mismatch, its structural cause, and confirmation the
drift was still live (not "already gone"), plus one instance the
register itself never named (`demos/traces/module-mix/README.md:108`).
The user chose to proceed as a disclosed gap over stopping the session
or extending the lint's own content patterns beyond scan-root
widening.

Fixed by an extension-blind, un-truncated census grep over the newly
widened roots: 4 live instances total (the 3 the register named, plus
the 1 the census alone found). Re-ran the widened lint: green, 249
assertions. Full isolated `make test` (commit 1's exact diff, `help.clj`'s
in-progress commit-2 edits stashed out first to keep the checkpoint
clean) green before push. Commit `b711aa6`, pushed, CI
`completed`/`success` (3m37s).

## Step 2 — Commit 2 (cluster B): help enrichment

**B1 (R3-B3-2), verb-level help narrowing.** Red-first tests added to
`help_test.clj`/`core_test.clj` (14 real failures confirmed, after
fixing one test-authoring false positive of my own — a legitimate
cross-reference in `sim run`'s own `--format` doc string tripped a
bare-substring check). Implemented `help/render-verb-help`, `core.clj`'s
`group-takes-verbs?`/`verb-known?`/`verb-help-response`, and wired both
the `--help` and 3-arg `help <group> <verb>` dispatch branches to
narrow on a known [group verb] pair and reuse F6's own
`:unknown-command` treatment (ADR-0117) verbatim on an unknown one.
Verbless groups (check/version/doctor/show/play) confirmed unaffected.
Green: 0 failures, 1035 assertions.

**B2 (R3-B3-1), sourced per-group examples.** Content rule [C,
approved by dispatch of the driving prompt]: one witnessed, verbatim
invocation per group (README.md Quickstart, `docs/use-cases/*.md`, or
a demo README), never composed. Surveyed all three source classes for
all 9 groups; 7 had a real invocation, `version`/`doctor` had none
anywhere (confirmed by grep, not assumed) — recorded as a register
addendum rather than an invented example. Red-first tests added, then
`cli-spec` gained `:example` entries and `render-group` gained an
"Example:" section. **A real regression an existing gate caught**: the
first (unwrapped) implementation broke `help_wrap_test.clj`'s own
pre-existing width-fit property at non-default widths (7 failures at
40/60 columns) — fixed by routing the line through the same
`wrap-with-hanging-indent` every other field already uses. Full CLI
suite green after: 354 tests, 3948 assertions. `docs/cli.md`
regenerated twice (before and after the wrap fix), byte-identical both
times — it deliberately excludes worked invocations by design, and
neither B1 nor B2 changes `cli-spec`'s own shape.

Live-verified on the real binary (`bin/ehrt help sim run`, `bin/ehrt
sim run --help`, `bin/ehrt help sim frobnicate`, `bin/ehrt gate
frobnicate --help`, `bin/ehrt help artifact`, `bin/ehrt help version`,
`bin/ehrt check somedir --help`) — all matched the design. Full
isolated `make test` green before push. Commit `ab11d7b`, pushed, CI
`completed`/`success` (4m37s).

## Step 3 — Oracle, ADR + ceremony surfaces, commit 3

`bin/regression-oracle c68ec3e ab11d7b` → `IDENTICAL: every root's
digest matches`, all 35 roots — matching the pre-analysis exactly
(every touched file is help text, docs, or lint config; no oracle root
invokes `ehrt help` or reads any drifted file).

`notes/adr/0118-fix-clusters-b-and-c-help-and-docs.md` landed: context,
tag ceremony, both commits' own red/green evidence (including the
STOP-AND-REPORT's full disclosure and the census's exact 4 instances),
the oracle bracket, deviations, fences, index line. `notes/ADRs.md`
gained its index line; `notes/adr/README.md`'s own file count corrected
115→116. The roadmap's "Fix cluster B"/"Fix cluster C" rows moved to
RESOLVED; the review-3 arc note marked CLOSED except the
design-channel-draft queue (the B-3/B-4 carry-forward wording halves,
unchanged); the "User manual design pass" row flipped to READY — its
own sequenced predecessor closed. The roadmap's Done section gained one
pointer line. `.agents/rulings.md` gained "From ADR-0118": the
`.github/**` scan-root YES and B2's own sourcing rule, both [C,
un-vetoed/approved-by-dispatch]. The findings register gained four
dated `FIXED, ADR-0118` disposition-cell notes (R3-B5-3, R3-B5-4,
R3-B3-2, R3-B3-1) plus one addendum row for the `version`/`doctor`
no-witnessed-invocation gap — fix-forward, the summary table's own
tallies untouched per the ADR-0115 snapshot-table precedent. This
session record and its prompt archive land in the same commit, both
READMEs updated.

## Deviations, disclosed

The RED-mismatch in commit 1's own Step 2 (above) is this session's
only real deviation from the driving prompt's own stated expectation —
raised as STOP-AND-REPORT, resolved by the user's own explicit choice.
No other "current (verify)" claim failed verification; no other red
refused to go red; no regen delta landed outside B1/B2's own predicted
reach (`docs/cli.md`: zero delta, confirmed twice); no oracle
non-identity. Full record in `notes/adr/
0118-fix-clusters-b-and-c-help-and-docs.md`'s own Deviations section.

## Close-out echo

**Commit 1:** invocation lint's scan roots widened to `demos/**` +
`.github/**`; 4 stale references fixed (3 named, 1 census-found).

**Commit 2:** `ehrt help <group> <verb>` / `<group> <verb> --help` now
narrow to one verb's own usage text, with F6's own unknown-verb
treatment on a bad one; every group with a witnessed invocation (7 of
9) gets a sourced "Example:" line, wrapped like every other field;
`version`/`doctor` deliberately render none.

**`bin/regression-oracle c68ec3e ab11d7b`:** IDENTICAL, all 35 roots.

**`bin/verify-nist-lock`:** OK, all 6 hit-nexus-sourced coordinates
match `artifacts.lock.edn` exactly, both pushes.

**SHAs:** Step 0 tag `stable-20260812-fix-cluster-a` at `c68ec3e`.
Commit 1 `b711aa6`. Commit 2 `ab11d7b`. Commit 3: this record's own
landing commit.

**CI status:** `test` lane green on `main` at every prior commit
checked (last five runs, Step 0). Both of this session's own pushes
confirmed `completed`/`success` on their first run — no red window
this session.

## HEAD landed

`ab11d7b` (commit 2) — commit 3 (this record's own commit) lands after
this record, in the same push as the prompt archive.
