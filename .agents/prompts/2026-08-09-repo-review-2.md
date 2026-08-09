# 2026-08-09 — ehr-testing-tools: repo review 2 (survey session)

## Context

Archived 2026-08-09. Repo: `~/src/ehr-testing-tools` (ext4 clone).
Session opened at HEAD `451d159` (storefront fixture, ADR-0091) and
closed at `0daf26c` (ADR-0092, this record's own commit landing after
it). Original prompt follows verbatim; a deviation record follows
that.

## Original prompt (verbatim)

# 2026-08-09 -- ehr-testing-tools: repo review 2 (survey session)

## Context

Conventions read at HEAD `451d159` (storefront fixture, ADR-0091),
design channel, 2026-08-09, verified by fresh public clone. The author
ruled 2026-08-09, verbatim: **"Review 2"** -- the second periodic
quality review, on the author's cadence, opening a new arc. This
session is the SURVEY ONLY: repo-review skill steps 1-4 plus the step-5
plan draft. NOTHING MOVES -- no fixes however cheap, no roadmap
Deferred/Next content changes, no law appends; findings live in the
register until the author rules (the ADR-0049/0058 discipline, restated
by the skill itself). Fix sessions and the arc close follow the
author's rulings on what this session lands.

R30 ceremony, standing. Ext4 clone at its UNC path; fast-forward,
record HEAD (expect `451d159`; later escalates unless explained).
Commit messages ASCII-only (standing practice since batch 4).

## Read first

1. `.agents/skills/repo-review/SKILL.md` IN FULL -- the rubric, the
   probe battery, the register format, the prior-arithmetic
   re-derivation step. Byte-diff its two homes first (that diff is
   itself a D5 probe; record it).
2. `.agents/plans/2026-08-07-repo-review-findings.md` -- the prior
   register: the baseline scoreboard to carry forward, the watch items
   (D1-8's `judge`/`docs` margins; D6's census-sampling canonical
   miss), and the per-dimension rows whose arithmetic AR-RR2-2
   re-derives.
3. `notes/adr/0077-repo-review-1.md` and `0078-result-or-loud.md` --
   the prior review's landing shape and the fix arc that structurally
   closed D4's red.
4. The window's own record: `notes/adr/0081...0091`, the arc closes
   (0084, 0089) with their Deviations/intake sections, and the session
   records for the storefront (`2026-08-09-storefront-fixture.md`) and
   batch 4 -- the history scan's raw material.

## Author rulings

- **AR-RR2-0 [A]** (ADR-0091, "Successor tag debt"): tag
  `stable-20260809-storefront-fixture` at `451d159`, Step 0, ANNOTATED,
  standing ceremony (design-channel verified 2026-08-09).
  Verify-and-disclose if present.
- **AR-RR2-1 [A]** (scope, ruled "Review 2"): steps 1-4 of the skill
  plus the step-5 mitigation-plan DRAFT, echoed to the author and
  recorded in ADR-0092 -- rulings are the author's; this session
  proposes. The register lands at
  `.agents/plans/2026-08-09-repo-review-findings.md` in the
  established audit-register format: one row per finding -- dimension,
  evidence (gathered by the mechanism it recommends: re-derive,
  re-hash, re-run, never re-read), severity, proposed disposition
  (fix-session-candidate / ruling-needed / close-as-fine / intake).
  The scoreboard carries review 1's column beside this run's.
- **AR-RR2-2 [A -- the skill's own standing step]**: before drafting
  this run's register, re-derive the PRIOR register's summary
  arithmetic directly from its per-dimension rows (the review-1
  precedent: its own summary was off by 1/2/1 and was corrected
  fix-forward). Record the re-derivation, whatever it shows.
- **AR-RR2-3 [C]** (the history-scan window): the prior register's
  date to HEAD -- the fidelity arc (0081-0084), the conviction arc
  (0085-0089), batch 4 (0090), the storefront (0091). Sweep EVERY
  disclosed deviation and incident into dimension-classified rows, at
  minimum: the two-session close resumptions and the ruled pre-split's
  first execution; the sleep-apnea licensed mover and the oracle
  blind-spot intake (byte-identity cannot see malformed compiled
  shapes -- ADR-0086); the `2088763` classpath break (a brick test
  file requiring a component absent from a composing project's
  classpath, latent because the `integration` lane is scheduled-only
  -- D2's "checks that run only where nobody looks" probe now has a
  live specimen) and the requires-vs-classpath static-gate candidate;
  the `cd08b20` red push (warm-artifact-cache false-green locally --
  the SECOND warm-cache incident in the local-state-is-not-clone-state
  family -- D3) and its hermeticity split's cold-cache verification
  method; the em-dash commit-message flattening and the session-side
  verification that was structurally blind to it; the ADR-0089
  prompt-citation mismatch (a prompt cited a transcript-only event --
  D7); the two gitleaks false positives and their resolution pattern;
  the three skipped NIST pairing cells; the loopback flake's
  occurrence count across the window. Repeat incident classes RAISE
  the owning dimension's severity -- the warm-cache pair is the
  canonical case.
- **AR-RR2-4 [C]** (headline re-scores, addressed explicitly in the
  register and ADR): **D4** -- the baseline's RED; re-run the D4
  probe battery FRESH (nil-flowing I/O, category-less catches, silent
  caps, defaulting parses) and score on this run's evidence, with the
  result-or-loud gates' presence verified by reading the gates AND by
  the probes' own results, never by the fix's existence alone.
  **D2** -- enumerate all CI workflow lanes and their triggers; map
  which gates run in which lanes and which run nowhere a red is
  routinely seen; map the standing rulings added since review 1 (the
  conviction-arc pair) to enforcing gates or name the gap. **D3** --
  the fresh-clone full-suite probe runs with artifact caches
  GENUINELY cold (the c690ec3 verification method, now the standard);
  any test green only under a warm cache is a finding. **D7** --
  carried-item aging: name every intake/design item now carried two
  or more closes with its blocker (candidates the scan must check:
  wellness-encounters, the census closure-count refinement, the
  `notice_verbatim` coverage gap, the engine defspec pin, the NIST
  licensing external, the SETUP rewalk, the author's two backlog
  rows).
- **AR-RR2-5 [C]** (probe hygiene): every probe row records
  dimension, command or method, expected, observed, verdict --
  negative results included as review 3's inheritance. An ACTIVE
  defect discovered by any probe (not a coverage gap -- a live wrong
  behavior) is a register row AND, if it invalidates work the last
  two arcs built on, an immediate STOP-AND-REPORT.

## Steps

**Step 0 -- Preflight + tag (AR-RR2-0).** Standard preflight (clean
tree, HEAD `451d159`, untracked disclosure, `clojure -M:poly check`,
oracle pre-digest `451d159 451d159` -- 34 roots IDENTICAL, ALL
workflow lanes' latest conclusions disclosed -- not just the push
lane; the lane-visibility lesson applies to this session's own
ceremony first). Tag. No commit.

**Step 1 -- Prior arithmetic + history scan (AR-RR2-2/3).** The
re-derivation recorded; the window's incidents extracted and
dimension-classified. No commit.

**Step 2 -- Probe battery (AR-RR2-4/5).** All eight dimensions, the
headline re-scores included; the cold-cache fresh-clone suite run
budgeted early (it is the longest probe). No commit.

**Step 3 -- Register + ADR + plan.** The register (with both
scoreboard columns); `notes/adr/0092-repo-review-2.md` (the survey
narrative, the headline movements, the step-5 plan draft: proposed
fix-session clusters each with its co-landed gate, rulings-needed
with options and a recommendation each, deliberately-fine items named,
this session's own successor tag debt); index line; README count
89->90; roadmap Done pointer ONLY. Commit:

    docs: repo review 2 surveyed -- the scoreboard moves, nothing else does (ADR-0092)

Push; verify message; watch CI to conclusion (all lanes noted).

**Step 4 -- Ceremony.** Session record + prompt archived
(`2026-08-09-repo-review-2.md`), both READMEs, same commit:

    docs: session record and prompt archive -- repo review 2

## Fences

No fixes -- a one-character fix is still a fix. No roadmap
Deferred/Next content edits. No law appends. No state.md
regeneration. No src/test/deps touches anywhere. The register and the
plan are the session's entire footprint beyond ceremony.

## Close-out

Echo to chat: the two-column scoreboard; the D4 verdict with its
evidence in one paragraph; the D2/D3/D7 headline findings; the full
plan draft (clusters, rulings-needed with recommendations,
deliberately-fine list); the prior-arithmetic re-derivation result;
shas, CI status across all lanes.

## Deviation record

None. The session ran the four steps as prompted: Step 0's preflight
and tag, Step 1's re-derivation and history scan (delegated to a
dedicated sub-agent under the same re-derive-never-re-read discipline
review 1 used), Step 2's eight-dimension probe battery (D1/D3/D5 run
directly by the landing session, matching review 1's own precedent
that single-command-shaped dimensions don't benefit from delegation;
D2/D4/D6/D7/D8 run as five independent, parallel, read-only sub-agents
— one of which, D8, was resumed mid-flight after a background `make
quickstart` probe outran the session's own patience window; the kill
itself is disclosed as a D8 finding rather than silently retried), and
Step 3/4's register, ADR, ceremony, commit, and push. No fix, however
small, landed outside the register/ADR/ceremony files AR-RR2-1's own
fence names. `git status --porcelain` was confirmed clean before this
session's first tool call and after every live-execution sub-agent's
own work (D8's `make quickstart`/`make integration`/`play` runs
included).
