# 2026-08-09 — ehr-testing-tools: gate permission-denied legs (judge family) + bare-level unknown flags (build session)

## Context

Archived 2026-08-09. Repo: `~/src/ehr-testing-tools` (ext4 clone).
Session opened at HEAD `558e6bf` (review-2 arc close, ADR-0097) and
closed at the two fix commits plus this record's own commit. Original
prompt follows verbatim; a deviation record follows that.

## Original prompt (verbatim)

Session prompt -- gate permission-denied legs (judge family) + bare-level unknown flags

## Context

You are Claude Code executing under R30 ceremony in the ehr-testing-tools
workspace (WSL2 clone, hook-enforced). This session executes the roadmap
Next row anchored at the review-2 arc close (ADR-0096 Finding 1 /
ADR-0097), WIDENED by author ruling to the full judge family, plus the
D8-4 rider. HEAD at handoff: 558e6bf. This session's ADR is ADR-0098.

The defect class: an exists-but-unreadable file (chmod 000) passes
`.isFile` checks (Java semantics, verified live by the design channel as
a non-root user: exists=true, isFile=true, canRead=false, and opening
the stream throws FileNotFoundException "(Permission denied)"), so the
raw exception escapes across component interfaces in three places:

1. `ehrt gate fhir PATH`: `ehrt.judge-fhir-official.fhir/gate-file` has
   NO path check at all -- `verdict-cache-lookup` calls
   `ehrt.kernel.digest/sha256-file` (fhir.clj:145), which throws raw.
   `gate-batch` (fhir.clj:445) runs its own `verdict-cache-lookup` per
   path and is equally exposed. The missing-file leg
   (`ehrt gate fhir /nonexistent.json`) is expected to be raw by the
   same mechanism -- capture what it ACTUALLY does in the red pass and
   report; if it is already categorized somewhere, that is a finding to
   disclose, not a failure.
2. `ehrt.judge-v2-hapi.v2/gate-file` (v2.clj ~198-203): `.isFile` guard
   then bare `(slurp f)` -- the guard passes chmod-000, the slurp
   throws raw. Its docstring promises `:file-not-found` "if path
   doesn't name a readable file" -- currently false for this leg.
3. `ehrt.judge-v2-nist.v2/gate-file` (v2.clj ~230-235): identical shape,
   identical docstring promise, identical defect.

The oracle bracket for this session, with its reasoning (earned from
the tree by the design channel, verify-then-act applies): pure identity
on all 34 roots is EXPECTED, because `bin/regression-oracle` digests
via `ehrt.oracle.digest`, whose requires are only sim-trajectory,
sim-model, sim-engine, and emit-hl7 interfaces -- no judge component,
no kernel.digest -- and this session's fence touches only judge
components and bases/cli. Any digest movement is STOP-AND-REPORT, not
a declared change.

Environment caveat: the chmod-000 red does NOT reproduce as root
(root bypasses permission bits). Run `whoami` before the red-evidence
step; if you are root, STOP-AND-REPORT rather than substituting a
different mechanism.

## Read first

- `.agents/plans/roadmap.md` -- the Next row for this session, verbatim
  (the `ehrt gate fhir PATH` permission-denied row with the D8-4 rider)
- `notes/adr/0097-review-2-arc-close.md` -- "This close's own
  mechanical debt" section (the tag ceremony below quotes it)
- `notes/adr/0096-*.md` -- Finding 1 (the original live red) and the
  cluster B fix shapes this session extends
- `components/judge-fhir-official/src/ehrt/judge_fhir_official/fhir.clj`
  -- gate-file, gate-batch, verdict-cache-lookup
- `components/judge-v2-hapi/src/ehrt/judge_v2_hapi/v2.clj` and
  `components/judge-v2-nist/src/ehrt/judge_v2_nist/v2.clj` -- gate-file
  entry guards and their docstrings
- `bases/cli/src/ehrt/cli/core.clj` -- `dispatch`'s cond (the
  `(:help opts)`, `(= group "help")`, `(nil? group)` branches short-
  circuit BEFORE `validate-known-flags` runs at ~line 2076),
  `unknown-flag-error`, `flag-validation-context`
- `bases/cli/src/ehrt/cli/help.clj` -- `global-flags` (the bare-level
  valid flag set)
- `.agents/rulings.md` -- tag law AR-T-1 (case ii), ASCII-first
  post-push verification, RNG-path law

## Author rulings, verbatim

- [A] The roadmap Next row itself, including: "Rider (D8-4, ruled
  2026-08-09, author verbatim 'I choose a.'): bare/`help`-level unknown
  flags ... route through the same `:unknown-flag` category, in the
  SAME fix session (same file family), with its own red->green
  evidence (a typo'd bare-level flag: before, help printed and exit 0;
  after, `:unknown-flag`, the subcommand exit semantics).
  `docs/cli.md` is not touched by this anchor."
- [A] 2026-08-09, charter width, author verbatim "Q1 a.": the charter
  widens to all three engines -- fhir, v2-hapi, v2-nist -- in this one
  session.
- [A] 2026-08-09, category shape, author verbatim "Q2 a.": keep
  `:file-not-found` as the single family category for both the missing
  and unreadable legs, adding a distinguishing payload key on the
  unreadable leg (`:reason :permission-denied`). No new category,
  family parity (ruled 2026-07-31) preserved; the docstrings' existing
  "readable file" wording becomes true as written.
- [C] Channel-inferred, verify before acting: the fhir guard belongs at
  the component entry (a shared private check used by BOTH gate-file
  and gate-batch, before any verdict-cache-lookup), never inside
  kernel.digest/sha256-file -- sha256-file returns a bare hex string
  consumed at six sites across four components, and the roadmap's own
  words are "applied where THIS read actually lives."

## Steps

1. **Tag ceremony (tag law case ii).** Per ADR-0097's mechanical-debt
   section, verbatim: tag `stable-20260809-review-2-arc-close` at
   `558e6bf` and push the tag. Verify HEAD is 558e6bf first; if the
   remote has moved, STOP-AND-REPORT.

2. **Red evidence, pasted verbatim into the ADR.** Run `whoami`
   (non-root required, see caveat). Then capture, before any fix:
   a. `ehrt gate fhir <chmod-000 .json file>` -- the raw
      FileNotFoundException stack, plus `echo $?`.
   b. `ehrt gate fhir /nonexistent/no.json` -- whatever it actually
      does today (expected raw; report the actual).
   c. Component-level reds for the siblings: invoke
      `judge-v2-hapi/gate-file` and `judge-v2-nist/gate-file` (REPL or
      a temporarily-failing test) on a chmod-000 file; capture the raw
      throw from each.
   d. Rider red: a typo'd bare-level flag (e.g. `ehrt --hlep`) --
      capture the help output landing and `echo $?` (expected 0), and
      the same for `ehrt help --hlep`.

3. **Fix commit 1 -- judge family.** In judge-fhir-official: add a
   private entry check (isFile, then canRead) returning
   `kernel/error :file-not-found {:path ...}` for missing and
   `kernel/error :file-not-found {:path ... :reason :permission-denied}`
   for exists-but-unreadable; call it at the top of BOTH `gate-file`
   and `gate-batch` (gate-batch fail-fast on the first bad path,
   matching its existing first-failing-step contract). gate-dir is
   covered via gate-file. In judge-v2-hapi and judge-v2-nist: extend
   the existing `.isFile` entry guard with the canRead leg, same
   category and payload key. Update all three docstrings to name the
   `:reason` key. Move, don't improve: no other reshaping. Co-landed
   tests in the same commit: per engine, a test that creates a temp
   file, chmods it 000, asserts the categorized error (guard the test:
   if `(.canRead f)` is still true after chmod -- root environment --
   skip with an explicit message rather than fail); plus a
   missing-file test for the fhir leg (its first-ever entry check).
   Green evidence: the new tests passing, plus re-runs of 2a/2b/2c
   showing categorized results and the subcommand exit code.
   Land `notes/adr/0098-*.md` per the cluster B precedent -- verify
   with `git show a2c31c8 --stat` which commit carried the ADR-0096
   file, and match that placement.
   Commit message (ASCII only):
   `fix: permission-denied gate legs categorized across judge family (ADR-0098)`

4. **Fix commit 2 -- rider (D8-4).** Bare/`help`-level unknown flags
   route through `unknown-flag-error` (same `:unknown-flag` category,
   same did-you-mean machinery) instead of being swallowed by the
   help short-circuits. Valid set at that level: the top level's own
   declared global flags (help.clj `global-flags`, plus `--width` if
   declared there -- read the source, don't assume). Acceptance, per
   the row: `ehrt --hlep` before = help + exit 0, after =
   `:unknown-flag` with the subcommand exit semantics (and
   `did-you-mean --help`); `ehrt --help` alone still prints help and
   exits 0; `ehrt help` and `ehrt help <group>` unchanged for valid
   invocations. If the minimal wiring forces changes outside
   bases/cli, STOP-AND-REPORT. Co-landed tests. `docs/cli.md` is NOT
   touched (author ruling in the row).
   Commit message (ASCII only):
   `fix: bare-level unknown flags route through unknown-flag category (ADR-0098, D8-4)`

5. **Oracle bracket.** Run `bin/regression-oracle` from the prior
   stable point to the post-fix tip. Expected: pure identity on all
   34 roots, per the reasoning in Context. Any movement:
   STOP-AND-REPORT with the manifest diff, no declared-change flag.

6. **Full gate.** `poly check`, full local test suite, the CLI
   parse-guard lint, `bin/verify-nist-lock` -- all green.

7. **Close phase.** FIRST: self-archive this prompt to
   `.agents/prompts/` (interrupted sessions must leave provenance).
   Then: ADR-0098 finalized with red/green evidence verbatim and any
   deviations in a dated appendix; roadmap Next row moved to Done
   (note the Q1 widening); `.agents/rulings.md` records the two
   2026-08-09 rulings ("Q1 a." / "Q2 a.") under this session;
   `notes/ADRs.md` index row; README ADR count 95 -> 96;
   `.agents/state.md` citation-only updates for touched claims (its
   content staleness is a review-3 watch item -- do not regenerate).
   Commit message (ASCII only):
   `docs: session record and prompt archive -- permission legs and bare flags (ADR-0098)`

8. **Push and verify.** Push at each checkpoint per R30. Post-push,
   ASCII check FIRST on every commit message this session created:
   `git log --format=%B -1 <sha> | LC_ALL=C grep -n '[^ -~]'`
   (expected empty), then CI confirmation.

## Fences

- Touch ONLY: `components/judge-fhir-official/{src,test}`,
  `components/judge-v2-hapi/{src,test}`,
  `components/judge-v2-nist/{src,test}`, `bases/cli/{src,test}`,
  `notes/adr/0098-*.md`, `notes/ADRs.md`, `README.md` (count only),
  `.agents/state.md`, `.agents/rulings.md`, `.agents/plans/roadmap.md`,
  `.agents/prompts/`.
- Nothing in kernel, corpus, sim, engine, oracle src. In particular
  `ehrt.kernel.digest` is untouched -- the oracle expectation above
  depends on it.
- `docs/cli.md` untouched.
- No history rewrites; deviations disclosed in the ADR's dated
  appendix; STOP-AND-REPORT over improvisation -- widening anything
  further is the author's call, not yours.
- Channel claims in this prompt (line numbers, caller sets, Java
  semantics) are verify-then-act: confirm against the tree before
  building on them.

## Deviations, disclosed

- **"README.md (count only)" resolved to `notes/adr/README.md`, not
  the root `README.md`.** The root `README.md` carries no ADR-count
  text (checked directly, `grep -n "ADR" README.md` for any count
  phrasing: none); `notes/adr/README.md` is the file that actually
  carries the "N of them, as of ADR-NNNN" sentence both ADR-0096 and
  ADR-0097 updated. Resolved by precedent, not by asking: both prior
  cluster sessions updated `notes/adr/README.md`'s own count line, and
  this session matched that, updating it 95→96 (verified by `ls
  notes/adr/*.md | grep -v README | wc -l`).
- **ADR-0098's file landed with fix commit 1, not deferred to the
  close-phase commit; the D8-4 rider's own red/green evidence and this
  session's own closing ceremony (Verification in full, Deviations
  appendix, Consequence, Index line) were appended to that same file
  during the close phase, after both fix commits existed.** Matches
  the cluster B precedent's OWN placement (`git show a2c31c8 --stat`:
  ADR file + `notes/ADRs.md` index + `notes/adr/README.md` count
  bundled with the fix commit) while still honoring the driving
  prompt's own explicit "ADR-0098 finalized ... " instruction under
  Step 7 — the ADR file is a single append-only artifact across all
  three commits, per this repo's own AR-B-4 convention ("new
  execution-record appends go directly to the per-ADR file"), not
  three separate ADR files.
- **`.agents/state.md`: no touched-claim edit made.** Grepped for
  `ADR-0097`, `ADR-0096`, `gate fhir`, `permission-denied`, `D8-4`,
  `bare-level`, `unknown flag` — zero hits. The staleness tripwire
  itself (`ehrt.docs-tooling.state-staleness-tripwire-test`) only
  tracks the newest `*-arc-close.md` filename; `0098-permission-legs-
  and-bare-flags.md` doesn't match that pattern, so unlike ADR-0097's
  own session, this one never trips the gate. Confirmed by reading the
  test's own regex before concluding "nothing to do," not assumed.
- **`.agents/rulings.md`: Q1 recorded as a one-off (mid-arc, this
  session's own widening decision), not marked `standing`; Q2 recorded
  as `standing` (a general category-shape convention any future
  component can reuse).** A judgment call in how to phrase the two
  entries, not itself directed by the prompt's own wording (which just
  says "records the two ... rulings"). Placed under a new "From
  ADR-0098" heading, mirroring the "mid-arc append, author-licensed"
  precedent ADR-0048 already set for this file, rather than waiting
  for a future arc close.

No other deviations from the driving prompt's own steps, fences, or
rulings. Every channel-inferred claim in the prompt (line numbers,
the `.isFile`/`.canRead` Java semantics, the missing-file leg's actual
behavior, the `:else` branch's flag-validation ordering) was verified
against the live tree before being built on and held exactly as
stated.
