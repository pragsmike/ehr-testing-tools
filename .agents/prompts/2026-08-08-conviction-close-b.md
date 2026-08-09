# 2026-08-08 — ehr-testing-tools: the conviction arc closes, session B (state + rotation + ADR-0089)

## Context

Conventions read at HEAD `a9c3abf` (conviction close A, design-channel
verified 2026-08-08 by fresh public clone: both commits, file sets,
law texts, annotated tag). This is **close session B** of the two the
author ruled ("Close. adopt, two close sessions."), scoped to the
ADR-0084 suggestion's second half: state regeneration, budgets,
rotation, and ADR-0089. The conviction arc = ADR-0085–0089: colorectal
investigation → straddle fix → colorectal payoff → pairing registry →
this close (sessions A + B).

R30 ceremony, standing. Ext4 clone at its UNC path; fast-forward,
record HEAD (expect `a9c3abf`; later escalates unless explained).
Commits land green.

## Read first

1. `notes/adr/0084-fidelity-arc-close.md` — Steps 2–3 are this
   session's template: the regeneration table format (claim → probe →
   disposition), the budget re-derivation mechanics, the tripwire
   sequencing disclosure (~line 175), the Done-rotation precedent.
2. `.agents/state.md` — every section; the regeneration target.
3. `.agents/session-records/2026-08-08-conviction-close-a.md` — the
   cadence report to carry into ADR-0089, and the tag fix-forward
   disclosure to cite.
4. `.agents/plans/roadmap.md` + `.agents/plans/roadmap-done-2026-08.md`
   — the rotation source and target; mirror the attic's existing
   dated-header pattern exactly.
5. `notes/adr/0085…0088` — the arc narrative's sources.

## Author rulings

- **AR-CB-0 [A]** (close A's AR-CA-4 debt, now channel-verified): tag
  `stable-20260808-conviction-appends` at `a9c3abf`, Step 0, ANNOTATED
  (close A's own lesson), standing ceremony. Verify-and-disclose if
  present.
- **AR-CB-1 [A — standing regeneration rule]**: regenerate `state.md`
  with every `[V]` claim probe-backed THIS session, citation
  `[V @<Step-1 commit>]`. Known drifts the regeneration must catch
  (probe, don't copy): vendored inventory (25 top-level modules, 71
  NOTICE rows), oracle roots (29), the round-trip test family (+
  colorectal), standing gates (+ taxonomy currency, registry schema
  cross-check, tier-one conviction), the new surfaces (pairing
  registry — 7 rows + 3 disclosed skips; `:suppressed-straddle-spans`
  and its pinned witness; the straddle span logic), ADR count, and the
  Live-work watch-list carried forward (sibling-flake soak run count,
  engine defspec pin `-60645`, the loopback flake, census
  closure-count refinement, wellness-encounters, `notice_verbatim`
  coverage gap). **Tripwire sequencing, the ADR-0080/0084 precedent
  verbatim:** the Step-1 state commit keeps its header citation at
  ADR-0084 (the newest arc-close ADR on disk at that boundary); the
  Step-2 commit that creates ADR-0089 moves the citation in the SAME
  commit. All touched gates green before each commit.
- **AR-CB-2 [A — standing budget rule]**: re-derive reading-set
  budgets from `git log 45eb2f4..HEAD --name-only` (the arc's own
  base: the fidelity close tip) against `.agents/reading-sets.edn`'s
  `:paths`; touch only sets with touched members; disclose the
  per-set disposition table.
- **AR-CB-3 [A — standing rotation rule]**: rotate the closed arc's
  Done pointers to a dated header in `roadmap-done-2026-08.md`,
  following the attic's own live pattern for WHICH pointers rotate
  (the prior close's pointer rotates with this arc, per precedent —
  read the attic, mirror it, disclose exactly what moved). ADR-0089's
  own pointer lands in Done and stays for the next arc.
- **AR-CB-4 [A]** (ADR-0089 contents): the arc narrative (six landed
  sessions, two loops closed); the pre-split adoption record (the
  ruling verbatim: "Close. adopt, two close sessions." — first
  executed by this close, including close A's tag fix-forward as its
  first observed benefit: a small blast radius when a ceremony detail
  goes wrong); the intake sweep — at minimum: the oracle blind-spot
  finding (ADR-0086: byte-identity cannot see malformed compiled
  shapes; the 27-root straddle sweep), the three skipped pairing
  cells (ADR-0088) as future-witnessing candidates for the storefront
  session, the cadence NOTE (hapi-fhir 8.2.0→8.10.1 now four arcs
  standing), and the channel's own two convicted errors this arc (the
  ADR-0082 `19` propagation; the AR-A-5 over-literal prompt wording)
  cited where they're already recorded; the horizon note VERBATIM
  from this prompt's own Close-out section; this close's own
  mechanical debt: `stable-20260808-conviction-close` at THIS
  session's closing tip, for the next arc's opener.
- **AR-CB-5 [C]**: no law appends this session (close A landed them);
  no roadmap row content changes beyond rotation and the ADR-0089
  pointer; discovering a failed re-probe during regeneration is a
  FINDING recorded in the regeneration table (and, if it invalidates
  work this arc built on, a STOP-AND-REPORT).

## Steps

**Step 0 — Preflight + tag (AR-CB-0).** Standard preflight (clean
tree, HEAD `a9c3abf`, untracked disclosure, `clojure -M:poly check`,
oracle pre-digest `a9c3abf a9c3abf` — 29 IDENTICAL, last-five CI
disclosed). Tag, annotated. No commit.

**Step 1 — State + budgets + rotation (AR-CB-1/2/3).** The
regeneration (with its claim → probe → disposition table captured for
ADR-0089), the budget table, the rotation. Full suite once green
before the commit (state cites suite shape; the loopback flake
disambiguates by one independent re-run, disclosed, untouched).
`gitleaks` clean. Commit:

    docs: the conviction arc's state is regenerated — every claim re-probed at the close (arc close B, AR-CB-1/2/3)

Push; verify message; watch CI to conclusion.

**Step 2 — ADR-0089 (AR-CB-4).** The ADR; `notes/ADRs.md` index line;
`notes/adr/README.md` count 86→87; the Done pointer; the state.md
citation move (same commit). Commit:

    docs: the conviction arc closes — two loops convicted on evidence, and the close itself splits by design (ADR-0089)

Push; verify; watch CI.

**Step 3 — Ceremony.** Session record + this prompt archived
(`2026-08-08-conviction-close-b.md`), both READMEs, same commit:

    docs: session record and prompt archive — conviction arc close B

Push; verify; watch CI.

## Fences

No src/test/deps touches. No law appends. No new intake beyond
AR-CB-4's sweep. No horizon rulings — the horizon note is for the
author, verbatim, unruled.

## Close-out

Echo to chat: the regeneration table (changed claims only), the budget
disposition table, what rotated, shas, CI status — and the horizon
note verbatim: "The horizon, for the author's ruling: vendoring batch
4 (the veteran family); the storefront fixture session (FHIR pairing
rows — the roadmap's named landing spot — plus the three skipped v2
cells as witnessing candidates); Wave E's risk-attribute/vital-sign
register; review 2 on the author's cadence (inherited watch-list);
publish-prep (F-5/F-6 + F-7); the author's two backlog rows (fixture
relocation; ADR-references-in-user-docs, unruled). Externals awaiting
the author alone: the NIST licensing gist, IG pinning, Clojars
F-5/F-6, the SETUP rewalk, the GitHub failure-notification toggle."

## Deviation record

**The AR-CB-4 citation mismatch ("AR-A-5 over-literal prompt
wording").** No citation matching that exact tag exists anywhere in
this arc's committed record — a direct grep across every
conviction-arc ADR, session record, and archived prompt found `AR-A-5`
used only to cite the STANDING relocation-with-notes law from
scaffolding compaction A (reused as shorthand in the straddle-fix and
colorectal-payoff prompts), never as an error label. Rather than
invent a matching citation, ADR-0089 names the mismatch directly and
cites the closest genuine, already-disclosed match instead: ADR-0087's
own AR-CP-2 finding (the colorectal-payoff prompt's own under-specified
"well-mixed seed sweep" wording, read too literally on the first
attempt, silently undercounting a rare real branch before an adapted
methodology succeeded). See `notes/adr/0089-conviction-arc-close.md`'s
own Intake section for the full disclosure. No other deviation from
this prompt's own steps or fences — all three commits landed as
specified, all CI runs watched to conclusion green, no STOP-AND-REPORT
fired during regeneration.
