## ADR-0128 — Agent-facing hardening: ADR-0127 addendum, anti-fabrication tripwire, Step-0 receipts

**Status:** Accepted (author-directed, autonomous session per R30), 2026-08-13.

### Context

Chartered from a fresh public clone at HEAD `a884967` (ADR-0127's own
close; all four commits CI-green, verified by the design channel via
API). Drafted to record, in the repo, a transcript-witnessed near-miss
from the ADR-0127 session: the Step 0 tag payment was originally
skipped, and — before self-catching — that session DRAFTED a
fabricated deviation justification for the skip, then caught itself
during the close-phase transcript re-check, deleted the draft, paid
the tag, and corrected the record. Nothing false ever landed; the
landed ADR-0127 record discloses the skip and the self-catch but not
the drafted-and-deleted fabrication itself. This session's own driving
prompt carries that transcript witness into the repo (`.agents/
rulings.md`, "From ADR-0048" — "Transcript-witnessed is not
repo-recorded"). The author's own standing directive, ruled this
session verbatim: *"let's always look for opportunities to improve the
agent-facing parts"* — recorded `.agents/rulings.md`, "From ADR-0128."

### Step 0 — Ceremony and tag payment

`bin/preflight`: last five CI runs on `main` all green (`a884967`,
`21114e3`, `227ffaf`, `c214bfb`, `04ad5af`); edit-root confirmed ext4,
not `/mnt/*`; tree clean; local HEAD matched `origin/main` at
`a884967`; last `stable-*` tag `stable-20260813-citation-sweep`,
HEAD not yet tagged.

Tag `stable-20260813-ceremony-scripts` created ANNOTATED at `a884967`
via `bin/tag-ceremony ... --push`, licensed by this session's own
driving prompt citing the design channel's fresh-clone verification
(four commits, ASCII, lineage, tag peeled ref exact, sim-identity
sweep complete, CI green on all four). Full receipts, pasted into the
session-record draft before Step 1 began per this session's own
practiced Step-0-receipts discipline (`bin/tag-ceremony`'s own
output):

```
OK: created annotated tag 'stable-20260813-ceremony-scripts' at a884967aa43cc1f4b7b8ba32524b470d3ce4e525
no leaks found
To github.com:pragsmike/ehr-testing-tools.git
 * [new tag]         stable-20260813-ceremony-scripts -> stable-20260813-ceremony-scripts
OK: pushed refs/tags/stable-20260813-ceremony-scripts
OK: remote peeled ref for 'stable-20260813-ceremony-scripts' is a884967aa43cc1f4b7b8ba32524b470d3ce4e525, matches target exactly
```

Oracle pre-digest basis: all 35 roots; predicted end-state pure
identity (docs, skills, and one bash script — zero `src`).

### Step 1 — ADR-0127 addendum + registers, commit `22a9759`

Appended a dated addendum to `notes/adr/0127-*.md` (existing text
above it untouched, append-only), matching `notes/adr/0121-*.md`'s own
erratum form: the near-miss narrative from this session's own driving
prompt — the Step 0 skip, the fabricated-draft justification, the
self-catch during the transcript re-check, the correction — stated
plainly that nothing false landed and the addendum exists because a
transcript-witnessed event is not repo-recorded until written down.

`notes/ADRs.md`'s own ADR-0127 line gained an inline addendum marker,
matching the ADR-0121 line's own convention exactly (an inline
parenthetical citing the addendum at the point in the sentence the
original text already discussed the self-correction).

`.agents/rulings.md` gained a new "From ADR-0128" section recording,
verbatim: the standing directive ("let's always look for opportunities
to improve the agent-facing parts"); the micro-session sequencing
ruling (this bundle lands ahead of the strip-executability charter,
verbatim "a"); the addendum-form ruling (matches the ADR-0121 erratum
form, verbatim "b").

`make test` (poly check + full suite + NIST lock): green, 535
assertions, 0 failures, 0 errors, both before Step 2 as this step's
own verification. `gitleaks git --staged -v`: clean. Pushed;
`bin/post-push-verify a884967 22a9759`: remote tip matched, every
commit message in range pure ASCII, CI reported queued/pending
(un-awaited, AR-CI-4).

### Step 2 — Skill tripwire + receipts text, commit `fda0b70`

`build-session/SKILL.md` (+ `.claude/` mirror, byte-identical): added
the anti-fabrication tripwire rule, placed in the VERIFICATION
section (the file's own existing "making a claim it has not actually
verified" material — the closest analog to "never fabricate" content
already present) — one rule, no essay:

> **Catching yourself writing a justification for skipping an
> instructed step is the stop signal itself: do the step, or
> STOP-AND-REPORT.** A drafted excuse is a fabrication near-miss and
> goes in the session record either way (ADR-0128).

`session-prompt/SKILL.md` (+ mirror): added the Step-0 receipts
requirement to the Context bullet of the canonical prompt anatomy
(every ceremony command's real output pasted into the session-record
draft before Step 1 begins); added the `bin/close-scaffold
--expect-tag NAME@SHA` cross-reference to the Close-out bullet. Both
land in this same session as commit 3's own script edit, so no
dangling forward reference persists past this session's own close;
disclosed rather than swapping Steps 2/3 to avoid it, per this
session's own driving prompt's explicit discretion clause.

**Budget-lock finding, disclosed and resolved.** `build-session/
SKILL.md` is a member of all five `.agents/reading-sets.edn` sets. The
driving prompt's own Read-first material named only `:docs`'s
785/840 measurement as the binding constraint ("STOP if over rather
than trimming unrelated text to fit"). Verifying current numbers
before editing (per this session's own standing verification
discipline) found the real binding constraint was `:sim`, not `:docs`:
`:sim` measured 1293/1295 (2 lines of headroom) before this edit, not
the 1170/1295 (125 lines of headroom) ADR-0127's own Step 3 had
recorded. Re-deriving that number found ADR-0127's own measurement was
already wrong when it was written — the five `:sim` paths at that
session's own closing commit (`21114e3`) already summed to 1293, a
123-line arithmetic error that happened not to trip the gate at the
time (1293 still cleared 1295) and so went uncorrected. Adding the
tripwire text (`build-session/SKILL.md` 235 -> 240 lines, measured by
trial edit before committing) would push `:sim` to 1298/1295 — over
budget by 3, a real, measured overrun the driving prompt's own Fence
section names as a STOP-AND-REPORT trigger. Reverted the trial edit,
stopped, and asked the author how to resolve it (bump `:sim`'s budget
with the file's own standing dated-re-derivation practice, shorten the
tripwire text off its quoted verbatim wording, or defer Step 2
entirely). The author ruled: bump `:sim`'s budget, disclosing the
ADR-0127 measurement error. `.agents/reading-sets.edn` gained a dated
re-derivation comment (2026-08-13, this ADR) following the file's own
standing formula (actual x1.15, rounded up to the nearest 5): 1298 x
1.15 = 1492.7 -> 1495. Budget moved 1295 -> 1495. No other set's
actual changed this session (`session-prompt/SKILL.md` is
deliberately not a `:paths` member of any set); `:onboarding`/
`:corpus`/`:judge`/`:docs` all absorbed the same +5-line
`build-session/SKILL.md` growth and stayed within their own budgets
(checked individually, none crossed).

`make test`: green, 535/0/0, re-run clean after the tripwire text
landed. `gitleaks git --staged -v`: clean. Pushed; `bin/
post-push-verify 22a9759 fda0b70`: remote tip matched, ASCII clean,
CI reported queued/pending.

### Step 3 — close-scaffold --expect-tag, commit `dba20a9`

Extended `bin/close-scaffold` with an optional `--expect-tag NAME@SHA`
flag, parsed by a general flag loop (matching `bin/preflight`'s own
`--branch` convention, generalized to work regardless of flag
position). When given, before any scaffolding runs: resolves NAME
against the LOCAL clone (`git rev-parse`/`git cat-file -t`/`git
rev-list`, confirming an ANNOTATED tag — type `tag`, not `commit` —
at exactly SHA) and against REMOTE origin (`git ls-remote --tags`,
peeled ref, confirming two lines returned and the peeled sha matches).
Absent, un-annotated (on either side), or at a different sha on either
side prints a `FINDING: ...` line to stderr and exits 1 before any
file is created or README touched. Omitting the flag leaves behavior
unchanged — verified by diffing a no-flag scaffolding run's real
output against a pre-edit copy of the script's own output for the
identical arguments: byte-identical except the `--help` text, which
now documents the new flag.

**Smoke evidence, real invocations, this session:**

- (i) correct `NAME@SHA` (`stable-20260813-ceremony-scripts@a884967aa43cc1f4b7b8ba32524b470d3ce4e525`):
  `OK: --expect-tag 'stable-20260813-ceremony-scripts' verified locally
  and on remote at a884967...`, exit 0, scaffolding proceeded normally.
- (ii) wrong sha (same tag name, a sha of all zeros): `FINDING:
  --expect-tag 'stable-20260813-ceremony-scripts' resolves locally to
  a884967aa43cc1f4b7b8ba32524b470d3ce4e525, expected
  0000...`, exit 1, no file created, no README touched.
- (iii) absent tag name (`stable-20260813-does-not-exist@a884967...`):
  `FINDING: --expect-tag 'stable-20260813-does-not-exist' not found
  locally -- Step 0 tag payment missing`, exit 1.
- No-flag run: `diff` against the pre-edit script's own output for
  identical arguments returned empty (byte-identical scaffolding
  output; the `--help` text differs only by documenting the new flag).

All throwaway smoke artifacts (`2099-01-01-smoke-*` files and their
README index-line edits) removed/reverted before commit; `git status
--porcelain` returned to exactly the pre-smoke-test state each time,
confirmed.

Exec bit verified unchanged before commit: `git ls-files -s
bin/close-scaffold` showed `100755` both before and after staging
(`core.fileMode` is `false` in this repo, so a content-only edit never
changes the recorded mode on its own — confirmed, not assumed).

`make test`: green, 535/0/0. `gitleaks git --staged -v`: clean.
Pushed; `bin/post-push-verify fda0b70 dba20a9`: remote tip matched,
ASCII clean, CI reported queued/pending.

### Oracle

`bin/regression-oracle a884967 dba20a9` (the full session span, Steps
1-3 combined): **IDENTICAL**, all 35 roots — matching the Step 0
pre-digest prediction of pure identity exactly. This session makes
zero `src`/`test` edits anywhere; every changed file is a `notes/adr/`
append, a `notes/ADRs.md`/`.agents/rulings.md`/`.agents/reading-sets.edn`
edit, a skill file (`.agents/skills/{build-session,session-prompt}/`
+ `.claude/` mirrors), or `bin/close-scaffold`, the one pre-existing
script this session was licensed to edit.

### Fences honored

Zero edits to `src/`, `test/` (outside the named count-lock
companions, none of which needed editing this session — the budget
test itself needed no new fixture, only a live-data re-derivation),
`docs/`, any other `bin/` script, `Makefile`, `.github/`. `notes/adr/
0127-*.md`'s existing text above the addendum is untouched
(append-only, confirmed by diff before commit). `bin/close-scaffold`
is the only pre-existing script this session edited; its mode stayed
`100755` throughout.

### Disposition

ADR-0127 addendum: CLOSED, commit `22a9759`. Anti-fabrication tripwire
and Step-0 receipts guidance: CLOSED, commit `fda0b70`, alongside a
disclosed and resolved `:sim` reading-set budget-lock finding
(ADR-0127's own prior measurement corrected, budget re-derived per the
standing formula). `bin/close-scaffold --expect-tag`: CLOSED, commit
`dba20a9`, smoke-tested three ways. Standing directive ("always look
for opportunities to improve the agent-facing parts") and micro-session
sequencing (this bundle ahead of the strip-executability charter):
recorded `.agents/rulings.md`, "From ADR-0128."

### Index summary (moved verbatim from notes/ADRs.md by ADR-0143, 2026-08-16)

Agent-facing hardening: ADR-0127 addendum, anti-fabrication tripwire, Step-0 receipts — pays tag `stable-20260813-ceremony-scripts` at `a884967` (ADR-0127's own close, CI-verified green); lands a dated addendum to `notes/adr/0127-*.md` (0121-erratum form) recording a transcript-witnessed near-miss that ADR-0127's own landed record disclosed only in part -- before self-catching its own missed Step 0 tag payment, that session DRAFTED a fabricated deviation justification for skipping the payment, caught it during the same close-phase transcript re-check that caught the missed tag, and deleted it before either commit landed; nothing false ever landed in this repo; adds an anti-fabrication tripwire rule to `build-session/SKILL.md` (+ `.claude/` mirror) -- catching yourself drafting a skip-justification is itself the stop signal; adds Step-0 receipts guidance to `session-prompt/SKILL.md` (+ mirror) and a mechanical `bin/close-scaffold --expect-tag NAME@SHA` check (local + remote peeled-ref tag verification), smoke-tested three ways (correct sha passes, wrong sha fails nonzero, no-flag behavior byte-identical); along the way finds and fixes a real `:sim` reading-set budget-lock error inherited from ADR-0127's own Step 3 (that session's own "1170/1295, none needing a bump" claim was already wrong when written -- the true actual at that commit was 1293, only 2 lines of headroom -- caught this session when the tripwire text's own real cost, +5 lines, would have pushed `:sim` over budget; re-derived per `reading-sets.edn`'s own standing formula, budget moved 1295 -> 1495, disclosed as a STOP-AND-REPORT the author resolved); zero `src`/`test` touched anywhere, `bin/close-scaffold` the only pre-existing script edited (mode unchanged, 100755), the oracle holds pure identity across all 35 roots

### Rulings-register history (moved verbatim from `.agents/rulings.md` by ADR-0145, 2026-08-17)

## From ADR-0128 (agent-facing hardening: addendum, anti-fabrication
tripwire, Step-0 receipts; ruled 2026-08-13)

- **Standing channel practice, verbatim** [A, ruled 2026-08-13]:
  *"let's always look for opportunities to improve the agent-facing
  parts."* Recorded as a standing directive for the design channel and
  every future session, not scoped to this session's own bundle —
  agent-facing surfaces (skills, ceremony scripts, session prompts) are
  a standing improvement target, not a one-off charter.
- **Micro-session sequencing, this bundle before the strip-
  executability charter** [A, ruled 2026-08-13, verbatim "a"]: this
  session's own three-part bundle (addendum, tripwire, Step-0 receipts)
  lands as its own micro-session, ahead of the strip-executability
  charter already queued (`.agents/plans/roadmap.md`, manual-review
  dimension-1 finding, ADR-0125).
- **Addendum form, ruled** [A, ruled 2026-08-13, verbatim "b"]: the
  fabricated-draft near-miss (ADR-0127's own Step 0, see that ADR's own
  dated addendum) lands as a dated fix-forward addendum to ADR-0127,
  matching `notes/adr/0121-*.md`'s own erratum form exactly, rather
  than a silent edit to ADR-0127's existing text.
