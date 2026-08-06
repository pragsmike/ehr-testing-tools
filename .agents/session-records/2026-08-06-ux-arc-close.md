# 2026-08-06 — UX arc close: the founding incident is mechanically impossible

## Scope

Session prompt naming AR-UC-0 through AR-UC-6, closing the UX arc per
the ADR-0055 close pattern. Prior: ux fixes 5 landed and was
design-channel-verified (`f5af489`, `notes/adr/0063-ux-fixes-5.md`).
Docs-only — no `src/`, `test/`, `deps.edn`, `workspace.edn`, or
Makefile touched at any point. Full account, rulings, the regeneration
table, the register tally, and the founding-incident closure narrative:
`notes/ADRs.md` ADR-0064.

Step 0 (preflight) confirmed the working directory is the ext4 clone
(`~/src/ehr-testing-tools`, `/dev/sdd`), tip `f5af489`, working tree
clean apart from the pre-existing untracked `config/busy-weekday.md`.
Baseline: `clojure -M:poly check` OK; full suite green (222 `Test
results:` lines, 0 failures/0 errors); `gitleaks detect -v` clean (685
commits); oracle pre-digest (`bin/regression-oracle f5af489 f5af489`)
all eleven roots IDENTICAL. AR-UC-0 executed directly (current tag law
licenses a session to tag its own predecessor's verified stable point
under standing ceremony, without further license): `stable-20260806-
ux-fixes-5` created annotated at `f5af489`, pushed, verified — peeled
ref resolves exactly.

Step 1 (`85d0130`, AR-UC-1/AR-UC-2) appended three standing rulings to
`.agents/rulings.md` under "From the UX arc (ADR-0056–0064)" (two
voices/two homes; errors name their artifact; audit evidence uses the
mechanism it recommends) and ran the dependency-review cadence
(`clojure -M:poly libs :outdated`) — unchanged from the alignment
arc's own report, no new upstream release surfaced across the entire
UX arc, no `deps.edn` edit.

Step 2 (`7662714`, AR-UC-3/AR-UC-4/AR-UC-5) regenerated
`.agents/state.md` in full against the live tree (fourteen claims
re-probed, a fourteen-row regeneration table in ADR-0064), re-derived
the `:onboarding` reading-set budget (1160→1205, the only set with
touched members this arc), and rotated the Done section: ADR-0055's
own disclosed leftover pointer joined the attic's existing
alignment-arc section with a dated append note; the UX arc's own eight
pointers (ADR-0056–0063) relocated under a new dated header. A live
near-miss surfaced mid-step (see Deviations) — caught by the gate
itself before any commit landed.

Step 3 (this record) authored `notes/adr/0064-ux-arc-close.md`
directly, appended its own index line to `notes/ADRs.md`, corrected
`notes/adr/README.md`'s own stale file count (61→62), added the Done
pointer (`- 2026-08-06 — ux-arc-close — ADR-0064`) in the same commit
as the index line, ran the closing oracle bracket, archived this
prompt, and recorded this session.

## Red→green evidence highlights

A docs-only session's proof is the suite staying green and untouched,
not a red→green cycle — confirmed at every checkpoint: 222 `Test
results:` lines, 0 failures/0 errors, identical shape to the Step 0
baseline throughout. The one genuine red this session hit was a
process gate, not a test: `ehrt.docs-tooling.done-pointer-adr-test`
fired live when this session briefly added ADR-0064's own Done pointer
before ADR-0064 existed in the index (see Deviations) — caught before
commit, not shipped red. The founding-incident live re-probe (Step 0)
is this session's own headline verification: all four founding
failures (stale invocation, opaque config crash, silent typo,
agent-voice help) confirmed mechanically impossible against the BUILT
`bin/ehrt`, not only `clojure.test` — full transcript in ADR-0064's
own "founding-incident closure narrative."

## Judgment calls and their ratification status

- **The UX register's own fresh-count check (AR-UC-6's own "check for
  the same [undercounting]" instruction) came back negative.** Unlike
  the alignment register's own disclosed 51-vs-47 internal drift, the
  UX register's own "21 total rows carrying a disposition" and its
  own bucket-sum arithmetic both verify clean on direct count. Not a
  judgment call requiring ratification — a verification that found
  nothing wrong, recorded so a future reader doesn't re-run the same
  check believing it was skipped.
- **A-1/A-3's own "close-as-fine, optional" dispositions were
  spot-checked rather than taken on the register's own word** (the
  bare `clojure -M:ehrt` mention, still absent; the cosmetic
  placeholder wrapping at `docs/simulate-your-facility.md:202`, still
  present) — both confirmed correct, no action needed, no ratification
  required since nothing changed.
- **The intake list's "module vendoring widening the ailment mix"
  item is recorded `[unverified]`**, per the standing "transcript-
  witnessed is not repo-recorded" rule (`.agents/rulings.md`, from
  ADR-0048) — this session's own driving prompt names it as arising
  from a design-channel conversation, but no repo artifact across the
  UX arc's own eight ADRs independently cites it. Author ratification
  (or correction) owed whenever that candidate is actually taken up.

## Findings and HEAD landed

No findings outside this session's own fence — the two intake items
(the `--width`/COLUMNS affordance, module vendoring) were already
named by prior sessions' own disclosures, not discovered fresh here;
restated in ADR-0064's own "Intake for the next arc" section rather
than treated as new.

Commits, in order: `85d0130` (Step 1, rulings appends + cadence
report), `7662714` (Step 2, state regeneration + budgets + rotation),
and this session's own closing records commit (Step 3).

## Verification

- `bin/regression-oracle f5af489 <this session's own closing commit>`:
  all eleven vendored-root batches IDENTICAL, exactly as expected for
  a docs-only session. No `--declared-digest-change` licensed or
  needed.
- Full suite (`clojure -M:poly test :all skip:integration`): green at
  Step 0 baseline (222/0/0) and again after Step 2's own edits
  (222/0/0, identical shape).
- `clojure -M:poly check`: OK, every step.
- `gitleaks git --staged -v`: clean, every commit this session;
  `gitleaks detect -v` (full history): clean at Step 0 baseline (685
  commits).
- Post-push message verification, both Step 1 and Step 2: one delta
  each against the message file, the known harmless trailing-newline
  artifact.
- Tag verification: `stable-20260806-ux-fixes-5` peeled ref resolves
  to `f5af489` exactly.

## Deviations, disclosed

- **The scoped `clojure -M:poly test :dev :ehrt/docs-tooling` command
  (ADR-0060's own documented shortcut) returned zero selected tests**
  against this session's own workspace state — not investigated (out
  of this session's own docs-only fence); the full-suite run served as
  the primary verification at every checkpoint instead, and no claim
  in ADR-0064 rests on the scoped command's own output.
- **A live near-miss in Step 2, caught by the gate itself.** This
  session briefly added ADR-0064's own Done pointer to the live
  roadmap in the same step as the Done-section rotation, then ran the
  full suite as usual — `done-pointer-adr-test` genuinely failed,
  citing an ADR number not yet present in the index. The line was
  removed and the pointer deferred to Step 3, the same
  sentinel-avoidance ADR-0055's own AR-AC-5 had already disclosed for
  its own equivalent moment. Full account: `.agents/prompts/
  2026-08-06-ux-arc-close.md`'s own deviation record.
