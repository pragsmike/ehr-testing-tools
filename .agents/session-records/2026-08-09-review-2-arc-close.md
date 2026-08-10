# 2026-08-09 — Review-2 arc close

## Scope

Session prompt naming AR-AC-0 through AR-AC-4, closing the review-2
arc (ADR-0092 survey; ADR-0093 rulings landing; ADR-0094 census fix;
ADR-0095 cluster A; ADR-0096 cluster B) in ONE session, per the
author's own explicit ruling ("Emit the prompt for one close
session") — a deliberate deviation from this repo's own two-session-
close standing default (ADR-0084's own intake). Docs-only: pays the
tag debt, anchors cluster B's own two disclosed-not-fixed findings in
`roadmap.md` under the arc's own new roadmap-anchor law (AR-RL2-3, its
first execution at an actual arc close), records D8-4's own ruled
disposition as a rider, and writes the arc-close ADR with review 3's
inheritance. Moves no scoreboard; re-scoring belongs to review 3.

## Step 0 — Preflight + tag (AR-AC-0)

Working directory confirmed the ext4 clone, HEAD `05b8624` (cluster B
parse guards, ADR-0096), branch up to date with `origin/main`, working
tree clean, no untracked files (`git status --porcelain
--untracked-files=all`, empty). `clojure -M:poly check`: OK. Oracle
pre-digest (`bin/regression-oracle 05b8624 05b8624`): all THIRTY-FOUR
roots IDENTICAL, soundness "yes outside ns form" — the expected
trivial tip-against-itself result.

CI lanes disclosed: `test`-lane last five runs on `main` all green
(`31340857607`, `31340341691`, `31330881843`, `31330580554`,
`31328812231`). `Integration`-lane's last three runs checked (not just
the latest): the two most recent (`31312458033`
2026-08-09T12:06:30Z, `31308023126` 2026-08-09T10:18:45Z, both
`workflow_dispatch`) green; the third (`31301880957`, `schedule`
trigger, 2026-08-09T07:45:13Z) shows `failure` — investigated before
disclosure. Its own `headSha` (`b7a1dc88`, "docs: session record and
prompt archive -- vendoring batch 4") predates `2088763` (the
classpath fix restoring `judge-v2-nist` to the `integration` project's
own composition, landed 2026-08-09T10:14:39Z UTC); its failure log
(`FileNotFoundException: ...judge_v2_nist/interface...`) is the exact,
already-disclosed H-4/`2088763` signature. A stale scheduled-run
checkout replaying already-fixed history, not a live regression — full
detail in the prompt archive's own deviation record.

Tagged `stable-20260809-cluster-b-parse-guards` at `05b8624`,
annotated, message "cluster B parse guards landed, design-channel-
verified 2026-08-09 (ADR-0096)" — the successor tag debt ADR-0096's
own "This session's own successor tag debt" section named; pushed;
peeled ref verified (`git ls-remote --tags origin` resolves the tag to
`05b8624df5fc4609299ddc4cd9033763ed8dfe4f` exactly).

## Step 1 — Self-archive first (standing law)

`.agents/prompts/2026-08-09-review-2-arc-close.md` written — verbatim
prompt plus a deviation-record placeholder — before any commit, per
this close's own explicit ordering (prompts archive at the START of
the close phase, so an interrupted close still leaves provenance).

## Step 2 — Anchors (AR-AC-1/2)

Two rows landed in `.agents/plans/roadmap.md`:

1. **`Next`** — `ehrt gate fhir PATH`'s own permission-denied leg
   (ADR-0096 Finding 1), with D8-4's own routing decision riding as a
   one-line rider (AR-AC-2, "I choose a" — bare/`help`-level unknown
   flags route through `:unknown-flag`, execution deferred to the same
   future fix session).
2. **`Deferred`** — `ehrt play`'s own bare reads (ADR-0096 Finding 2),
   named revisit trigger: the next session touching `ehrt play` or the
   corpus-player slices.

`docs/cli.md` untouched, per the close's own fence.

## Step 3 — ADR + ceremony surfaces + commit (AR-AC-3)

`notes/adr/0097-review-2-arc-close.md` landed: arc narrative; the
register's FINAL disposition tally, independently re-derived a third
time (57/8/5/5/1 = 76); the deviations sweep, both sides, fix-forward
— including a THIRD design-channel unearned-specificity instance this
close's own preflight caught in the driving prompt itself ("the
veteran family arc (Batch 4) opens next," corrected against the live
tree: batch 4 closed 2026-08-08, before review 2 even opened) and the
prompt-archive transport-reflow finding (this close's own probe:
`.agents/prompts/2026-08-09-repo-review-2.md`'s own archived prompt
hard-wrapped at ~80 chars against its four siblings' own long,
unwrapped lines — content-verbatim, not byte-verbatim, measured
directly via `awk '{print length}' | sort -rn`); review 3's
inheritance (the four carried items restated, the two new anchors, the
lint allowlist named as a watch item, D8-4 marked ruled-not-carried);
open Externals restated unchanged; this close's own mechanical debt
(the successor tag, the one-session-close deviation sentence); the
horizon note. `notes/ADRs.md` gained its index line; `notes/adr/
README.md`'s own file count corrected 94→95 (`ls`-verified, 95).
`roadmap.md`'s Done section gained one pointer.

Oracle bracket over this commit's own changes (`bin/regression-oracle
05b8624 92b3bbc`): all THIRTY-FOUR roots IDENTICAL — pure identity, as
predicted for a docs-only close. `git diff --cached --stat` reviewed
before staging: exactly the five files the fence named (`roadmap.md`,
the prompt archive, `notes/ADRs.md`, the new ADR file,
`notes/adr/README.md`) — nothing else. `gitleaks git --staged -v`:
clean. Committed `92b3bbc` ("docs: review-2 arc closes -- plan
executed in full, two findings anchored (ADR-0097)").

**Push sequencing, corrected before either push — disclosed as a
deviation, full text in the prompt archive.** Running the full local
suite at this step, as instructed, surfaced a real mechanical conflict
in the driving prompt's own two-commit split:
`ehrt.docs-tooling.prompt-record-pairing-test` failed with the prompt
archive present (landed in this commit) and no paired session record
yet (Step 4's own job) —
`#{"2026-08-09-review-2-arc-close"}` reported as an orphan, not a
hypothetical. Pushing this commit alone would carry that same orphan
to CI's own push-lane run and go red there, exactly what this close's
own Fences section names as outranking the close. Resolved
mechanically: this commit landed locally WITHOUT an immediate push
(preserving the stated provenance goal — the prompt archive reaches
git history in the main commit even if the session is interrupted
before Step 4); Step 4's own commit follows immediately; both push
together in ONE `git push`, so CI's own push-lane run only ever
evaluates the final tip, which carries both files. The full local
suite was re-run once both commits' own files existed on disk (below).

## Step 4 — Ceremony (this record)

**A second, independent gate went red at the first full-suite run with
both Step 3 and Step 4's own files present — found, diagnosed, and
mechanically resolved before this commit, disclosed here and in
ADR-0097's own Dated-append section.**
`ehrt.docs-tooling.state-staleness-tripwire-test` failed:
`notes/adr/0097-review-2-arc-close.md` is named `*-arc-close.md` and
is therefore the newest file the tripwire's own regex tracks;
`.agents/state.md`'s header still cited ADR-0089 (its last full
regeneration, conviction arc close, 2026-08-08). The tripwire's own
docstring reads "Deliberately narrow: this checks CURRENCY... not
CONTENT" — read directly before acting, not assumed. Resolved to
exactly that narrow scope: `state.md`'s header citation sentence
updated to ADR-0097, content otherwise untouched (still reflecting its
last full regeneration, six ADRs behind as of this close) — a
disclosed, deliberate narrower act than AR-C-1's full standing
contract, named as a new watch item for a future session's own ruled
regeneration, not silently absorbed or silently expanded into a full
regeneration this close's own fence never licensed.

Full local suite (`clojure -M:poly test :all skip:integration`),
re-run after both this fix and this record/both READMEs existed on
disk: 296 namespace test blocks, 0 failures, 0 errors anywhere
(grepped the entire log for any nonzero failure/error count — none
found; both previously-red gates — `prompt-record-pairing-test` and
`state-staleness-tripwire-test` — now pass). `clojure -M:poly check`:
OK.

`notes/adr/0097-review-2-arc-close.md`'s own Verification section
placeholders filled in with this record's own findings (a normal dated
fix-forward landing in this commit, never an amend to `92b3bbc` — this
repo never amends a landed commit): the full-suite result above, and
the post-push/ASCII-check/CI results recorded below once available.

Session record (this file) and both READMEs
(`.agents/session-records/README.md`, `.agents/prompts/README.md`)
staged together with the ADR's own verification fill-in. `git diff
--cached --stat` reviewed: exactly those four files. `gitleaks git
--staged -v`: clean. Committed — see HEAD landed, below, for the real
SHA.

## Deviations, disclosed

See the prompt archive's own deviation record
(`.agents/prompts/2026-08-09-review-2-arc-close.md`) for full text on
each: (1) the already-stale Integration-lane scheduled-run red,
investigated and resolved to already-known history at Step 0; (2) the
design channel's own two earlier-arc unearned-specificity instances
restated in ADR-0097's own deviations sweep (the "no Wave-E row" claim,
the Done-notes fence wording); (3) cluster B's own charter premise
mismatch (`gate fhir`) and its mid-session ruling, restated; (4) the
`play` regression caught in-flight by cluster B's own full suite,
restated; (5) the prompt-archive transport reflow, newly measured by
this close's own probe; (6) the design channel's own CI-API
verification gap, named; (7) a THIRD unearned-specificity instance —
this close's own driving prompt's stale "Batch 4 opens next" horizon
claim — caught and corrected before landing rather than repeated; (8)
the Step 3/Step 4 push-sequencing conflict, resolved mechanically
(above); (9) the `state.md` staleness tripwire, tripped by this ADR's
own `*-arc-close.md` filename, resolved to the narrowest fix the gate
itself demands (citation only, above) and named as a fresh watch item
rather than expanded into an out-of-fence full regeneration. Items 8
and 9 are the two this session itself originated (real gate reds this
session's own full-suite runs actually produced), not restated from an
earlier one.

All fences held otherwise: no `src`/`test`/config/`.gitattributes`
touched; no fixes of any kind beyond the one disclosed, mechanically-
forced `state.md` citation line (item 9); the two anchored findings
themselves are a roadmap row, not a fix; no register edits; no law
appends; no scoreboard movement; no `docs/cli.md` touch; roadmap moved
exactly the two named rows plus the Done pointer. `git status
--porcelain` clean before this session's first tool call, clean at
each commit boundary.

## Close-out echo

**The anchor rows, as landed** (`.agents/plans/roadmap.md`):
- `Next`: `ehrt gate fhir PATH`'s own permission-denied leg (ADR-0096
  Finding 1), carrying D8-4's own rider ("I choose a" — bare/`help`-
  level unknown flags route through `:unknown-flag`, execution rides
  the same future fix session).
- `Deferred`: `ehrt play`'s own bare reads (ADR-0096 Finding 2), named
  revisit trigger.

**D8-4's disposition:** ruled by the author 2026-08-09 ("I choose a"),
landed as a rider on the gate-fhir `Next` row — not carried open, not
a review-3 item.

**Deviations, both sides:** the design channel's own three unearned-
specificity instances (two from earlier in the arc, restated; one
caught by this close itself, "Batch 4 opens next," corrected before
landing); cluster B's own charter premise mismatch and mid-session
ruling; the `play` regression caught in-flight; the prompt-archive
transport reflow, newly measured; the design channel's own CI-API
verification gap; this close's own Step 3/Step 4 push-sequencing
conflict, resolved mechanically and disclosed.

**Review 3's inheritance, in brief:** D3-2, H-3, D7-14, D8-8 (restated
from ADR-0092) plus the two new anchors, the lint allowlist as a named
watch item, and D8-4 marked ruled-not-carried.

**The disposition tally's four numbers:** 57 close-as-fine, 8
fix-session-candidate (landed or anchored), 5 ruling-needed (all
ruled), 5 intake (2 resolved in-arc, 3 carried) — summing to 75, plus
1 non-tallied cross-reference (D6-4) = 76, matching the register's own
row count exactly.

**Shas:** tag `stable-20260809-cluster-b-parse-guards` at `05b8624`;
Step 3 commit `92b3bbc`; Step 4 (this record's own) commit — see HEAD
landed, below.

**CI status, all lanes:** recorded below once the combined push is
watched to conclusion.

## HEAD landed

[recorded after the combined push — see this record's own dated
verification fill-in, landed the same commit]
