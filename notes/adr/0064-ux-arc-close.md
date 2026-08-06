## ADR-0064 — UX arc close: the founding incident is mechanically impossible — appended, regenerated, rotated

**Status:** Accepted (author-directed, autonomous session per R30), 2026-08-06.

### Context

Prior: ux fixes 5 landed and was design-channel-verified (`f5af489`,
`notes/adr/0063-ux-fixes-5.md`); every fix cluster of the UX arc
(ADR-0056 through ADR-0063, tag-law included) is landed and verified.
This session closes the arc per the standing close pattern (ADR-0055
is the model): rulings appends, the dependency-review cadence's
execution, `state.md` regeneration, budget re-derivation, Done
rotation, the register's final tally, and the closing ADR. Docs-only;
anything new found is next-arc intake, never an act. R30 ceremony.

### Decision

Ruled 2026-08-06, recorded verbatim (author rulings, this session's
own prompt):

**AR-UC-0 (tag, standing ceremony).** Annotated
`stable-20260806-ux-fixes-5` at `f5af489`, message "ux fixes 5 landed,
design-channel-verified 2026-08-06 (ADR-0063)"; push; verify.

**AR-UC-1 (rulings appends).** Three, under "From the UX arc
(ADR-0056–0064)", each citing its recording ADR, wording condensed
from those ADRs' verbatim text: (a) two voices, two homes, standing —
user-facing surfaces (help, errors, command-bearing docs) speak
operator language; maintainer content (citations, milestone history,
internal names) lives in source comments and dev docs, relocated never
deleted (brief §3, executed by ADR-0062); (b) errors name their
artifact, standing — every operational error names the concrete thing
it could not find or parse, with a next step where one exists; unknown
input is rejected by name, never silently accepted (ADR-0060,
ADR-0061); (c) audit evidence uses the mechanism it recommends,
standing — fence verification resolves paths rather than parsing
grammar, and string inventories walk the data the gate will walk
rather than grepping source; two same-arc instances of the cheaper
method being wrong: AR-U2-R's non-resolving fences (ADR-0060) and the
38-vs-36 token count (ADR-0062).

**AR-UC-2 (cadence execution).** `clojure -M:poly libs :outdated` per
the standing dependency-review rule; the dated report lands in
ADR-0064; no edit of any kind follows from it; an urgent-looking
upgrade is an intake note.

**AR-UC-3 (state.md regenerates).** Per its contract and the ADR-0055
pattern: every `[V]` claim probe-backed THIS session; skeleton
preserved; content updates at minimum — the gate inventory (now grown
by: invocation-lint with path resolution, unknown-flag validation, the
voice gate, the wrap width+content-preservation tests, tag-law's
phrase gate, license-text pointer); the tag law's current form
(sessions execute stable tags under license or standing ceremony,
AGENTS.md/skill/register all reconciled, tag count by fresh `git tag`
census); the CLI's user-facing posture (Result-vocabulary errors with
did-you-mean, spec-derived flag validation, operator-voice help with
80-column wrap — the founding incident's four failures each now
gate-guarded); both audit registers' pointers with final-tally status;
Deferred/Next counts by fresh count; component graph re-probed
(expected unchanged). Regeneration table (claim → old → new → probe)
in ADR-0064; a wrong-in-kind discrepancy is STOP-AND-REPORT.

**AR-UC-4 (budgets).** Re-derive per the standing formula every
reading set whose members changed during this arc (enumerate against
`git log 12d3aa3..HEAD --name-only`; `AGENTS.md` changed via ADR-0057
and `state.md` regenerates here, so `:onboarding` at minimum), AFTER
the regeneration lands.

**AR-UC-5 (rotation).** The Done section currently holds ADR-0055
through ADR-0063 (design-channel probed). Rotate: ADR-0055's pointer
appends to the attic's EXISTING alignment-arc section with a one-line
dated note (its own close left it as the then-current entry; disclosed
leftover, same class ADR-0055 itself disclosed for the compaction
pointers); ADR-0056–0063 rotate under a new `## UX arc — closed
2026-08-06 (ADR-0056–0064)` header. ADR-0064's pointer then lands as
the sole current entry. Relocation-not-rewrite throughout.

**AR-UC-6 (the closing ADR).** `notes/adr/0064-ux-arc-close.md`:
rulings verbatim; the arc summary one line per session (riders,
tag-law, audit, fixes 1–5) with ADRs and tips; the UX register's final
disposition tally (fresh count against the register itself — the
alignment close caught its register undercounting; check for the same);
the founding-incident closure narrative, four failures to four
mechanisms: stale invocation → lint+path gates; opaque config crash →
named Result with did-you-mean; silent typo → spec-derived rejection;
agent-voice help → operator voice, gated, wrapped; the intake list for
the next arc (any `--width`/COLUMNS note ADR-0063 carried; the
module-vendoring candidate from the founding conversation, 2026-08-06,
feature-shaped; anything the sessions noted); the open Externals
restated unchanged (NIST licensing inquiry, IG pinning, SETUP rewalk);
and the horizon note: "The horizon is feature-shaped: corpus-player
slices (roadmap Next, ADR-0014), the pairing-as-data design pass
(design channel, landing spot `judge`), module vendoring widening the
ailment mix (intake, unruled), `sim-emit-cda` on its trigger.
Publish-prep gates: F-5/F-6 decisions plus the alignment register's
F-7 checklist."

### Step 0 — preflight + tag

Working directory confirmed `~/src/ehr-testing-tools` (ext4, `df -T`
reports `ext4` on `/dev/sdd`); tip `f5af489` exactly; working tree
clean apart from `config/busy-weekday.md` (the unrelated pre-existing
untracked founding-incident fixture, left alone as every session since
the incident has). Baseline: `clojure -M:poly check`: OK. Full suite
(`clojure -M:poly test :all skip:integration`): 222 `Test results:`
lines, 0 `FAIL`/`ERROR`/`Exception` anywhere. `gitleaks detect -v`: 685
commits scanned, no leaks. Oracle pre-digest
(`bin/regression-oracle f5af489 f5af489`): all eleven roots IDENTICAL,
soundness "yes outside ns form" — the harness confirmed sound before
this session's own changes land.

**AR-UC-0 — the tag, executed.** `stable-20260806-ux-fixes-5` did not
exist locally or on origin (checked both); created annotated at
`f5af489`, message "ux fixes 5 landed, design-channel-verified
2026-08-06 (ADR-0063)"; pushed; verified — peeled ref resolves to
`f5af489` exactly (`git ls-remote --tags origin`, `git tag -v`).

### Step 1 — appends + report (AR-UC-1/2)

Three appends landed in `.agents/rulings.md` under "From the UX arc
(ADR-0056–0064)": two voices/two homes; errors name their artifact;
audit evidence uses the mechanism it recommends. Committed `85d0130`
("docs: the ux arc's law is appended — two voices, named artifacts,
honest evidence (arc close, AR-UC-1/2)"), pushed. Post-push
verification: one delta against the message file, the known harmless
trailing-newline artifact.

**AR-UC-2 — the `libs :outdated` report, captured 2026-08-06 against
tip `f5af489`:**

```
library                                   version  latest   type      KB
------------------------------------------------------------------------
ca.uhn.hapi.fhir/hapi-fhir-base           8.2.0    8.10.1   maven  1,164
ca.uhn.hapi.fhir/hapi-fhir-structures-r4  8.2.0    8.10.1   maven     29
ca.uhn.hapi/hapi-base                     2.6.0             maven    653
ca.uhn.hapi/hapi-structures-v24           2.6.0             maven  1,446
gov.nist/hl7-v2-parser                    1.7.3             maven    229
gov.nist/hl7-v2-profile                   1.7.3             maven    123
gov.nist/hl7-v2-validation                1.7.3             maven  1,051
io.github.cognitect-labs/test-runner      dfb30dd           git       26
metosin/malli                             0.20.1            maven     97
org.babashka/cli                          0.12.79  0.12.86  maven     35
org.clojars.cmiles74/clojure-hl7-parser   3.5.1             maven     18
org.clojure/clojure                       1.12.5            maven  4,129
org.clojure/data.json                     2.5.2             maven      9
org.clojure/test.check                    1.1.3             maven     39
org.slf4j/slf4j-nop                       2.0.17            maven      4
```

**Unchanged from the alignment arc's own AR-AC-2 report** (`notes/adr/
0055-alignment-arc-close.md`) — every coordinate, version, and
`latest` value identical: no new upstream release surfaced during the
entire UX arc. The same three coordinates still show a newer `latest`:
`ca.uhn.hapi.fhir/hapi-fhir-base` and `ca.uhn.hapi.fhir/hapi-fhir-
structures-r4` (8.2.0→8.10.1, minor-version bumps within the same
major), and `org.babashka/cli` (0.12.79→0.12.86, a patch bump,
dev-tooling-only). No listed upgrade reads as a security-relevant
major — this is a NOTE for next-arc intake per AR-UC-2's own fence,
not an act. No `deps.edn` edit made or considered.

### Step 2 — state + budgets + rotation (AR-UC-3/4/5)

**State.md regeneration table (claim → probe → disposition).** Held
claims are not re-listed exhaustively; this table records every claim
that changed since the prior regeneration (`992e0a5`, ADR-0055) or
that this session's own fresh probe corrected:

| # | Claim | Probe | Disposition |
|---|---|---|---|
| 1 | `notes/adr/` file count | `ls notes/adr/*.md \| grep -v README \| wc -l` | **UPDATED 52→61** — nine attic files added this arc (ADR-0055 through ADR-0063). Will be 62 once this ADR's own file lands, the exact staleness-at-count-instant pattern both prior regenerations named. |
| 2 | `notes/ADRs.md` line count | `wc -l notes/ADRs.md` | **UPDATED 99→108** — eight new index lines (ADR-0056–0063). |
| 3 | `docs-tooling` gate-family file count | `ls components/docs-tooling/test/ehrt/docs_tooling/ \| wc -l` | **UPDATED 19→21** — two new gates this arc (`invocation_lint_test.clj`, created ux-fixes-1/ADR-0059, extended with fence-path-resolution ux-fixes-2/ADR-0060; `tag_law_test.clj`, created tag-law/ADR-0057), each red→green witnessed in its own ADR. |
| 4 | CLI-base gate files (`bases/cli/test/ehrt/cli/`) | `ls bases/cli/test/ehrt/cli/` | **NEW claim, confirmed live.** Two gates landed OUTSIDE the docs-tooling family: `help_voice_test.clj` (ux-fixes-4/ADR-0062), `help_wrap_test.clj` (ux-fixes-5/ADR-0063). Unknown-flag rejection (ux-fixes-3/ADR-0061) and the config-crash Result vocabulary (ux-fixes-2/ADR-0060) landed as new `deftest`s inside the base's own pre-existing test files, not new gate files. |
| 5 | `stable-*` continuity tag count | `git tag -l 'stable-*'`, excluding the three frozen legacy tags | **UPDATED 8→16** — the UX arc's own eight (`-tag-law`, `-ux-riders`, `-ux-audit`, `-ux-fixes-1` through `-5`), the last created by this session's own AR-UC-0. |
| 6 | Tag mechanic reconciliation | Fresh read of `AGENTS.md`, both `build-session` `SKILL.md` copies, `.agents/state.md`, `.agents/rulings.md` | **HELD, now exercised.** The tag-law session's own sweep (ADR-0057) still holds on every surface — no drift found — and this session's own AR-UC-0 exercised case (ii) of the law directly, proving it live under use, not merely re-read. |
| 7 | `roadmap-done-2026-08.md` Done-header count | `grep -c "^## Done" .agents/plans/roadmap-done-2026-08.md` | **HELD at 34** — this session's own rotation (AR-UC-5, below) adds two NEW dated-arc headers (an append to the existing alignment-arc section, and a new UX-arc section), neither of which is a `## Done (this session, ...)` entry, so the 34-count is unaffected, matching the prior regeneration's own disposition for its own differently-shaped rotation header. |
| 8 | Deferred row count | `awk '/^## Deferred/,/^## Done/' roadmap.md \| grep -c '^- '` | **HELD at 13**, unchanged — the UX arc touched zero Deferred rows (a `bases/cli`/docs arc, not sim-trajectory). |
| 9 | Next row count | `awk '/^## Next/,/^## Externals/' roadmap.md \| grep -c '^- '` | **NEW claim, confirmed live: 11**, unchanged in membership — the UX arc's own register rows landed as fix sessions or standing rulings, never a Next-section backlog addition. |
| 10 | `workspace.edn` line count | `wc -l workspace.edn` | **HELD at 33**, untouched this arc. |
| 11 | `bin/verify-nist-lock` wiring | `grep -n verify-nist-lock Makefile` | **HELD**, `make test`'s own third line, untouched this arc. |
| 12 | Component graph (count, sim-engine callers, sim-emit-hl7 require form, provenance deps, resource nesting) | `ls -d components/*/`, `ls -d bases/*/`, fresh grep/read of each cited file | **HELD, entirely unchanged** — the UX arc's five src-behavior sessions (ADR-0060/0061/0062/0063) edited `bases/cli` only, never a component boundary; every claim reconfirmed byte-for-byte against the prior regeneration. |
| 13 | Founding-incident live status (all four failures) | Live `bin/ehrt` probes: the founding command shape, bare invocation, `--patiens` typo, `bin/ehrt sim`, `bin/ehrt help` token/width scan | **NEW claim, confirmed live** — see "The founding-incident closure narrative," below, for the full probe transcript. All four now gate-guarded, re-verified against the BUILT binary, not only `clojure.test`. |
| 14 | Both audit registers' final-tally status | Fresh row count against each register (`grep -E`), cross-checked against each register's own summary paragraph | **NEW claim, confirmed live** — see "The UX register's final disposition tally," below. Unlike the alignment register (51-vs-47 internal drift, caught at that arc's own close), the UX register's own summary arithmetic checks out clean on fresh count — no drift found. |

Full regenerated content landed in `.agents/state.md` (this session's
own commit `7662714`); see that file directly for the complete text,
not reproduced here per this arc's own "session record narrates
ceremony, the artifact itself is the content" discipline (inherited
from the alignment arc's own close).

**Budget re-derivation (AR-UC-4).** `git log 12d3aa3..HEAD
--name-only` (`12d3aa3` = the alignment arc's own closing commit, the
base since which `state.md`/`reading-sets.edn` were last touched)
diffed against every reading set's own `:paths`: `:onboarding` is the
only set with members touched — six of its eight paths (`AGENTS.md`,
`.agents/plans/README.md`, `.agents/session-records/README.md`,
`.agents/prompts/README.md`, `.agents/plans/roadmap.md`,
`.agents/skills/build-session/SKILL.md`). No `:corpus`/`:sim`/
`:judge`/`:docs` member path appears in that diff, so those four
budgets are untouched. Fresh actual, measured AFTER the rotation
landed (so the number reflects the final tree): 284 (`AGENTS.md`) + 49
+ 57 + 137 + 85 + 33 (the five `.agents/*/README.md` files) + 228
(`roadmap.md`, post-rotation) + 172 (`build-session/SKILL.md`) =
**1045**. Re-applying the standing formula (actual × 1.15, rounded up
to the nearest 5): 1045 × 1.15 = 1201.75 → **1205**. Budget moves
**1160 → 1205**. Landed in `.agents/reading-sets.edn` (this session's
own commit `7662714`), a dated comment block matching the file's own
established re-derivation-note convention.

**Done rotation (AR-UC-5).** ADR-0055's own pointer — the alignment
arc's own closing ADR, deliberately left as the live roadmap's sole
current entry at that arc's own close (a disclosed leftover: AR-AC-5's
own ruling named only ADR-0048 through ADR-0054 for relocation, the
same disclosed-leftover class ADR-0055 itself named for the
scaffolding-compaction pointers) — relocates into the attic's EXISTING
`## Alignment arc — closed 2026-08-05 (ADR-0048–0055)` section, with a
dated append note explaining the leftover and closing that section's
own named range for real (the header's own `(ADR-0048–0055)` range
finally matches its own list). The UX arc's own eight Done pointers
(ADR-0056 through ADR-0063) relocate verbatim into a NEW `## UX arc —
closed 2026-08-06 (ADR-0056–0064)` header. **No leftover this time —
the pattern was checked for and closed, not repeated:** the live
roadmap's own Done section, mid-step, would have cited ADR-0064 before
that number existed in `notes/ADRs.md`'s own index (the exact
dangling-reference hazard `ehrt.docs-tooling.done-pointer-adr-test`
exists to catch — witnessed live this session: adding the line early
red-lit the gate; removed and deferred to Step 3, alongside this ADR's
own index line, the sentinel-avoidance ADR-0055's own AR-AC-5 already
disclosed). The live roadmap's Done section carries a code-comment
marker recording the deferral in place of the pointer between Step 2
and Step 3.

Full suite green throughout (222 `Test results:` lines, 0
failures/0 errors, matching Step 0's own baseline shape exactly — a
docs-only step). `clojure -M:poly check`: OK. Oracle bracket
(`bin/regression-oracle f5af489 7662714`, re-run after this step): all
eleven roots IDENTICAL — no `src/` touched, exactly as expected.

Committed `7662714` ("docs: the state regenerates, the budgets
re-derive, two arcs rest in the attic (arc close, AR-UC-3/4/5)"),
pushed. Post-push verification: one delta against the message file,
the known harmless trailing-newline artifact.

### The UX register's final disposition tally (AR-UC-6)

**Fresh count, checked against the register's own summary for the
same undercounting class the alignment close found.** A direct count
of every row carrying a disposition in the live register (`.agents/
plans/2026-08-06-ux-audit-findings.md`, `grep -E '^\| [A-Z]-?[0-9]'`
over the table rows) finds **21 rows** — U1/U4/U5 (3, seeded) +
A-1/A-2/A-3 (3) + B-1..B-6 (6) + C-1..C-5 (5) + D-1..D-4 (4) = 21,
matching the register's own stated "21 total rows carrying a
disposition" exactly. Bucket sums (D-1 dedups against B-5's identical
finding, D-3 against B-6's; B-4 contributes to two buckets, its own
mechanism/content split): close-as-fine 8 + ruling-needed 1 +
fix-session-candidate 6 + design-channel-draft 4 + incomplete 1 = 20,
also matching. **Unlike the alignment register (51-vs-47, already
internally inconsistent at that arc's own close), the UX register's
own summary arithmetic is clean — no drift found**, the check AR-UC-6
asked for came back negative.

**Closed this arc (10 distinct rows, 6 fix sessions + rulings), by
session:**

| Row(s) | Finding | Closed by |
|---|---|---|
| U1 | demo/facility-doc invocation sweep | ADR-0059 (ux-fixes-1) |
| U1 (rider) | fences resolve, not just parse (A-2's own gap) | ADR-0060 AR-U2-R (ux-fixes-2) |
| U4 | near-miss suggestion, folded into C-1 | ADR-0060 AR-U2-2 (ux-fixes-2) |
| C-1 | `--config` crash adopts the Result vocabulary | ADR-0060 AR-U2-1 (ux-fixes-2) |
| B-5 / D-1 | bare invocation exits 0 (ruled, then executed) | ADR-0060 AR-U2-4 (ux-fixes-2) |
| B-6 / D-3 | unknown-command hint names the real group | ADR-0060 AR-U2-3 (ux-fixes-2) |
| C-4 | unknown flags rejected by name, near-miss named | ADR-0061 (ux-fixes-3) |
| B-1, B-2, B-3, B-4 (content) | the help-spec voice rewrite lands | ADR-0062 (ux-fixes-4) |
| B-4 (mechanism) | the line-wrap render mechanism lands | ADR-0063 (ux-fixes-5) |

**Already close-as-fine at audit landing, unchanged, no arc action
owed (8):** U5 (subsumed by U1), A-1 (optional `clojure -M:ehrt`
mention — spot-checked this session, still absent, correctly
optional), A-2 (grammar-validity confirmed by U1's own mechanical
fix), A-3 (cosmetic placeholder wrapping — spot-checked this session,
`docs/simulate-your-facility.md:202` still carries angle-bracket
placeholders, correctly optional and untouched), C-2, C-3, C-5
(positive controls, cited by C-1's own recommendation, never touched).

**Standing note, not actionable (1):** D-4 (the artifact-fetch-
dependent Quickstart remainder, disclosed incomplete by its own
framing — a judgment row confirming RUN output is clean, not a
defect; no arc action owed; next-session intake only if full
Quickstart fidelity is ever wanted).

**Seeded rows folding into other areas without an independent
disposition of their own (2, not double-counted in the 21):** U2
(folds into Area B in full), U3 (resolves into C-1).

### The founding-incident closure narrative

The 2026-08-06 founding conversation named four concrete failures.
Live-probed against the built `bin/ehrt` this session, Step 0, all
four are now mechanically impossible — not merely fixed once, but
gate-guarded against recurrence:

1. **Stale invocation** (`clojure -M:cli run ...` taught across demo
   docs) → swept to `bin/ehrt` form (ux-fixes-1, ADR-0059) AND gated so
   the fences actually RUN, not just parse (ux-fixes-2's own rider,
   AR-U2-R, ADR-0060 — `invocation_lint_test.clj`'s fence-path-
   resolution assertion). Confirmed live this session: zero
   `clojure -M:cli` occurrences anywhere in the doc tree.
2. **Opaque config crash** (`--config <missing-or-malformed>` → a raw
   JVM stack trace, exit 1, the documented code for "operational
   error" never reached) → `merge-config-file` adopts the Result
   vocabulary (ux-fixes-2, ADR-0060 AR-U2-1/AR-U2-2). Confirmed live,
   the founding incident's own exact command shape:
   `bin/ehrt sim run --seed 1 --patients 1 --config
   config/busy-weekday.edn` → `{:status :error, :category
   :config-not-found, :payload {:path "config/busy-weekday.edn",
   :did-you-mean "config/busy-weekday.md"}}`, exit 2.
3. **Silent typo** (`--patiens` absorbed into `:opts`, the run
   succeeds with the wrong value silently defaulted underneath) →
   spec-derived flag validation rejects by name with a near-miss
   suggestion (ux-fixes-3, ADR-0061). Confirmed live:
   `bin/ehrt sim run --patiens 200 --seed 1` → `{:status :error,
   :category :unknown-flag, :payload {:flag "--patiens", :verb "sim
   run", :did-you-mean "--patients"}}`, exit 2.
4. **Agent-voice help** (24 ADR + 14 milestone + 3 ruling citations, 3
   bare EDN keywords, 2 internal-namespace leaks, 4 strings over 250
   characters with no wrap structure, soft-wrapping illegibly at any
   real terminal width) → the author-approved rewrite relocates all 36
   real token hits (the draft's own stated 38 reconciled to the gate's
   live count, ADR-0062's own disclosed evidence-method correction) to
   source comments, zero deleted (ux-fixes-4, ADR-0062); a real
   hanging-indent line-wrap mechanism replaces raw terminal soft-wrap
   at 80 columns, content byte-preserved (ux-fixes-5, ADR-0063).
   Confirmed live this session: `bin/ehrt help | grep -oE
   'ADR-[0-9]{4}|ruling [0-9]|D1[0-9]'` — zero hits; every rendered
   line ≤80 columns (`awk '{print length}' | sort -rn | head`).

### Open Externals, restated unchanged

**NIST licensing inquiry** — narrowed, not resolved, by `components/
corpus/docs/experiments/EXP-SBOM-results.md`'s own per-coordinate
license classification; the inquiry draft itself stays "maintained
privately by the author," not a repo artifact — sending it is still
AUTHOR ACTION. **IG pinning** — the profile-tier conformance target
still undecided. **SETUP rewalk** — still owed, an unspoiled human
reader. **Clojars publish** — ruled, deferred; F-5/F-6 remain its own
open decisions; F-7's own close-as-fine disposition still carries its
standing forward note (re-run its three-point pre-publish checklist
immediately before the actual first Clojars publish). `/mnt/c`
disposition — closed (ADR-0047 AR-C-3), unchanged. None of these five
rows was touched by the UX arc; restated here, not re-decided, per
this session's own docs-only fence.

### Intake for the next arc

- **A `--width`/COLUMNS CLI affordance** (ADR-0063's own named note):
  terminal width stayed a constant (`default-wrap-width`, 80, a
  private `def`, no CLI-facing knob) throughout the wrap-mechanism
  session, disclosed there as out of scope, not built.
- **Module vendoring widening the ailment mix** — named in this
  session's own driving prompt as arising from the founding
  conversation (2026-08-06), feature-shaped. No repo artifact across
  the UX arc's own eight ADRs cites this candidate independently —
  `[unverified]` per the standing "transcript-witnessed is not
  repo-recorded" discipline (`.agents/rulings.md`, from ADR-0048);
  named here as intake, not ruled, exactly as the horizon note below
  frames it.
- No other next-arc-intake note was found disclosed across the UX
  arc's own eight ADRs (fresh grep, all eight files, for "NOTE for"/
  "next arc"/"later session"/"out of scope" framings) beyond the two
  above and the register's own already-tallied D-4 (a standing,
  non-actionable judgment row, not an intake item — see the register
  tally, above).

### The horizon note (verbatim, per this session's own prompt)

"The horizon is feature-shaped: corpus-player slices (roadmap Next,
ADR-0014), the pairing-as-data design pass (design channel, landing
spot `judge`), module vendoring widening the ailment mix (intake,
unruled), `sim-emit-cda` on its trigger. Publish-prep gates: F-5/F-6
decisions plus the alignment register's F-7 checklist."

### Step 3 (this entry) — record

This ADR lands; `notes/ADRs.md` gains its index line;
`notes/adr/README.md`'s own file count corrected 61→62 ("as of
ADR-0064"). Roadmap gets its Done pointer, the sole current entry (no
leftover this time, per AR-UC-5's own disclosure above):

```
- 2026-08-06 — ux-arc-close — ADR-0064
```

**Oracle bracket** (`bin/regression-oracle f5af489 <this session's own
tip>`): this session touched no `src/`, no `test/`, no `deps.edn`, no
`workspace.edn`, no Makefile — docs/plans/rulings/state only. All
eleven vendored-root batches expected and confirmed byte-identical;
see Verification, below, for the actual recorded output.

### Verification

- `clojure -M:poly check`: OK, every step this session.
- `gitleaks`: clean at every scan this session (baseline `detect`,
  every staged scan, every push).
- Post-push message verification, every checkpoint: one delta each
  against the message file, the known harmless trailing-newline
  artifact prior sessions already name.
- Full suite (`clojure -M:poly test :all skip:integration`): 222 `Test
  results:` lines, 0 `FAIL`/`ERROR`/`Exception` anywhere, unchanged in
  shape from Step 0's own baseline at every checkpoint this session
  ran it — expected, docs-only.
- `bin/regression-oracle f5af489 f5af489` (Step 0), re-run after Step
  2 (`f5af489 7662714`), and after this record's own closing commit:
  all eleven vendored-root batches (`appendicitis`, `death-fixture`,
  `ear-infections`, `ear-infections-engine`,
  `ear-infections-history-engine`, `sepsis`, `sinusitis`,
  `sore-throat`, `total-joint-replacement-engine`,
  `urinary-tract-infections-engine`,
  `urinary-tract-infections-history-engine`) IDENTICAL every time;
  soundness check "yes outside ns form." No `--declared-digest-change`
  licensed or needed — this session's own fence (docs-only) makes any
  digest change STOP-AND-ESCALATE, and none occurred.
- Founding-incident live re-probe (Step 2's own regeneration table, row
  13): all four failures confirmed mechanically impossible against the
  BUILT `bin/ehrt`, not only `clojure.test` — full transcript in "The
  founding-incident closure narrative," above.
- Register fresh-count cross-check (row 14): both audit registers'
  own summary arithmetic verified against a direct row/bucket count;
  the UX register's own summary holds clean, no drift found (contrast
  with the alignment register's own disclosed 51-vs-47 drift at that
  arc's close).

### Fences

Docs-only: no `src/`, `test/`, `deps.edn`, `workspace.edn`, or
Makefile touched; no gate changes (every gate cited this session was
read, not edited). The UX audit register (`.agents/plans/
2026-08-06-ux-audit-findings.md`) is untouched — read-only, per its
own contract; final dispositions live here, the register stays a
dated artifact. No new design work: the horizon note above RESTATES
ruled directions, it does not extend them; the intake list NAMES
unruled candidates, it decides nothing. Frozen archives untouched
except the sanctioned acts: this ADR's own new file, and the
live-attic appends to `.agents/plans/roadmap-done-2026-08.md` (AR-UC-5,
the same act ADR-0046's own compaction-B pattern licensed, and
ADR-0055's own AR-AC-5 exercised for the first rotation of this arc's
own predecessor pointer).

### Consequence

The UX arc — riders (ADR-0056), the tag-law reconciliation (ADR-0057),
the audit (ADR-0058), and five fix-cluster sessions (ADR-0059 through
ADR-0063) — is complete. Of the register's own 21 disposition-carrying
rows: 10 distinct findings closed across six fix/rider sessions, 8
needed no action (already clean at audit landing, spot-checked this
session and still correct), 1 stands as a disclosed, non-actionable
judgment row, and 2 seeded rows folded into other areas without an
independent disposition — no arc action was silently dropped.
`.agents/state.md` regenerates with fourteen corrected or newly-probed
claims, every one backed by a probe run this session — including a
negative check on the alignment arc's own undercounting pattern (the
UX register's summary holds clean). The `:onboarding` reading-set
budget re-derives to reflect the final tree (1160→1205). The arc's own
eight Done pointers rotate to the attic under a dated header, its own
predecessor's disclosed leftover (ADR-0055's own pointer) finally
joins the alignment-arc section it always belonged to, and the live
roadmap's Done section holds only this ADR's own pointer — no leftover
carried forward this time. Four gate-guarded mechanisms — invocation
lint with path resolution, a named Result vocabulary with
did-you-mean, spec-derived flag rejection, and an operator-voice,
80-column-wrapped help surface — now stand between a stranger and each
of the founding incident's own four failures; none of the four can
recur silently, each is enforced by a red test, not a memory. The next
arc opens feature-shaped, per the horizon note above, with two named,
unruled intake items (the `--width`/COLUMNS affordance, module
vendoring widening the ailment mix) waiting for a future session's own
ruling.
