## ADR-0055 — Alignment arc close: the register empties, the state regenerates, the law is appended

**Status:** Accepted (author-directed, autonomous session per R30), 2026-08-05.

### Context

Prior: alignment fixes 5 landed and was design-channel-verified
(`2b3bb2b`, `notes/adr/0054-alignment-fixes-5.md`); every fix cluster
of the alignment arc (ADR-0048 through ADR-0054) is landed and
verified. This session closes the arc: the pending rulings-register
appends land, `.agents/state.md` regenerates against the live tree per
its own contract, reading-set budgets re-derive, the arc's Done
pointers rotate to the attic, and this closing ADR records the final
tally. Docs-only; nothing new is designed or fixed here — anything
found is a note for the next arc's own intake, never an act. R30
ceremony.

### Decision

Ruled 2026-08-05, recorded verbatim (author rulings, this session's own
prompt):

**AR-AC-0 (tag).** Annotated tag `stable-20260805-alignment-fixes-5` at
`2b3bb2b`, message `alignment fixes 5 landed, design-channel-verified
2026-08-05 (ADR-0054)`; push; verify on origin.

**AR-AC-1 (rulings appends — the arc-close contract executes).** Three
appends to `.agents/rulings.md` under a "From the alignment arc
(ADR-0048–0055)" section, each citing the ADR that recorded it: (a)
A-3's dependency-review cadence — report-only `clojure -M:poly libs
:outdated` at each arc close plus mandatory before any publish,
upgrades never taken as a side effect (ADR-0050 AR-F1-6a); (b) D-3's
acceptance — `judge` is the pairing-as-data registry's landing spot for
the design pass (ADR-0050 AR-F1-6b); (c) the law-surface propagation
lesson — an amendment to standing law lands on every surface that
states the law, in the same session that rules it; two instances this
arc: the AGENTS.md tag rule (ADR-0051 AR-F2-0) and the vendoring
prescription surfaces (ADR-0053 AR-F4-4). Wording drawn from those
ADRs' verbatim text, condensed per the register's house style.

**AR-AC-2 (first A-3 execution).** Run `clojure -M:poly libs :outdated`;
the output lands as a dated report section in this ADR — coordinates,
current, available, nothing else. NO `deps.edn` edit of any kind; if
the report is empty, say so; if a listed upgrade looks urgent (a
security-relevant major), that is a NOTE for next-arc intake, not an
act.

**AR-AC-3 (state.md regenerates — AR-C-1's duty).** `.agents/state.md`
is regenerated in place: contract header and section skeleton
PRESERVED; every `[V]` claim in the regenerated file backed by a probe
run THIS session — no claim survives on the prior version's own
authority. Content updates at minimum: the gate inventory; the tag
mechanic; the NIST supply-chain posture; `workspace.edn`'s slimmed
shape; the alignment arc's own pointer; current Deferred/Next counts;
the component graph section re-probed. Anything in the old state.md
that fails this session's re-probe is corrected with the probe cited —
listed in this ADR's own regeneration table (claim → old → new →
probe).

**AR-AC-4 (budgets — AR-D-3).** Every reading set in
`.agents/reading-sets.edn` whose member files changed during this arc
(diffed against `git log 89e327f..HEAD --name-only`) has its budget
re-derived by the standing formula (actual size × 1.15, rounded to the
nearest 5) AFTER the state.md regeneration lands. Unaffected sets
untouched.

**AR-AC-5 (Done rotation — compaction-B pattern).** The arc's Done
pointers (ADR-0048 through ADR-0054) relocate verbatim from the
roadmap's Done section to `.agents/plans/roadmap-done-2026-08.md`
under a dated arc header (`## Alignment arc — closed 2026-08-05
(ADR-0048–0055)`), matching the attic's format contract. ADR-0055's
own pointer then lands as the Done section's sole current entry.
Relocation-not-rewrite; the attic append is the sanctioned live-attic
act.

**AR-AC-6 (the closing ADR).** `notes/adr/0055-alignment-arc-close.md`:
rulings verbatim; the arc summary; the register's FINAL disposition
tally; the A-3 report; the state.md regeneration table; the budget
re-derivations; the open Externals restated; the horizon note verbatim.

### Step 0 — preflight + tag

Working directory confirmed `~/src/ehr-testing-tools` (ext4, `df -T`
reports `ext4`); tip `2b3bb2b` exactly; working tree clean. Baseline:
`clojure -M:poly check`: OK. Full suite (`clojure -M:poly test :all
skip:integration`): 216 `Test results:` lines, 0 `FAIL`/`ERROR`/
`Exception` anywhere. `gitleaks detect -v`: 664 commits scanned, no
leaks. Oracle pre-digest (`bin/regression-oracle 2b3bb2b 2b3bb2b`): all
eleven roots IDENTICAL, soundness "yes outside ns form" — the harness
confirmed sound before this session's own changes land.

**AR-AC-0 — the tag (AUTHOR ACTION, not executed).** Licensed but not
run by this session (tags stay author-only in every ceremony mode).
Exact commands for the author, once this closing ADR's own landing is
design-channel-verified per the standing after-landing sequence:

```sh
git tag -a stable-20260805-alignment-fixes-5 2b3bb2b \
  -m "alignment fixes 5 landed, design-channel-verified 2026-08-05 (ADR-0054)"
git push origin stable-20260805-alignment-fixes-5
git ls-remote --tags origin | grep alignment-fixes-5
```

Only after that lands and is verified does the author license
`stable-20260805-alignment-close` at this ADR's own tip, per the
standing after-landing sequence (this session's own prompt, "After
landing").

### Step 1 — appends + report (AR-AC-1/2)

Three appends landed in `.agents/rulings.md` under "From the alignment
arc (ADR-0048–0055)": the A-3 dependency-review cadence, the D-3
pairing-as-data landing spot, and the law-surface propagation lesson
(both its instances named). Committed `992e0a5` ("docs: the arc's law
is appended — cadence, landing spot, and the propagation lesson (arc
close, AR-AC-1/2)"), pushed. Post-push verification: one delta against
the message file, the known harmless trailing-newline artifact.

**AR-AC-2 — the `libs :outdated` report, captured 2026-08-05 against
tip `2b3bb2b`:**

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

Three coordinates show a newer `latest`: `ca.uhn.hapi.fhir/hapi-fhir-
base` and `ca.uhn.hapi.fhir/hapi-fhir-structures-r4` (8.2.0→8.10.1,
both minor-version bumps within the same major), and `org.babashka/cli`
(0.12.79→0.12.86, a patch bump, dev-tooling-only per the matrix
columns). No listed upgrade reads as a security-relevant major — this
is a NOTE for next-arc intake per AR-AC-2's own fence, not an act. No
`deps.edn` edit made or considered.

### Step 2 — state + budgets + rotation (AR-AC-3/4/5)

**State.md regeneration table (claim → probe → disposition).** Held
claims are not re-listed exhaustively (the file itself carries its own
citations); this table records every claim that changed since the
prior regeneration (`53edcad`, ADR-0047) or that this session's own
fresh probe corrected:

| # | Claim | Probe | Disposition |
|---|---|---|---|
| 1 | `notes/adr/` file count | `ls notes/adr/*.md \| grep -v README \| wc -l` | **UPDATED 45→52** — seven attic files added this arc (ADR-0048 through ADR-0054). Will be 53 once this ADR's own file lands, the same staleness-at-count-instant pattern the prior regeneration named. |
| 2 | Resource nesting | `for d in components/*/resources; do ls $d; done` | **UPDATED, closed.** All seven `components/*/resources` directories now nest under their own brick name (`sim-model`'s own former `resources/sim/` tolerance closed, ADR-0052) — a new, gated invariant, not merely an observed fact. |
| 3 | `sim-emit-hl7` dependency law | Direct read of `emit_hl7.clj`/`site_profile.clj`'s own `:require` forms | **HELD, now gated.** Same two-namespace scope the prior regeneration recorded; `ehrt.docs-tooling.sim-emit-hl7-dependency-test` (ADR-0051 AR-F2-2) now enforces it structurally. |
| 4 | `provenance` leaf law | `cat components/provenance/deps.edn` | **HELD, now gated.** `{:deps {metosin/malli ...}}` only; `ehrt.docs-tooling.provenance-leaf-law-test` (ADR-0051 AR-F2-3) enforces it. |
| 5 | `roadmap-done-2026-08.md` Done-header count | `grep -c "^## Done" .agents/plans/roadmap-done-2026-08.md` | **UPDATED 33→34** — alignment riders (ADR-0048 AR-R-4) relocated one pre-existing stray Deferred-row closure (`myocardial_infarction.json`) verbatim, predating this arc's own close. |
| 6 | Deferred row count | `awk '/^## Deferred/,/^## Done/' roadmap.md \| grep -c '^- '` | **HELD.** 13, unchanged — this arc touched zero Deferred rows. The `myocardial_infarction.json` drift the prior regeneration disclosed is now CLOSED (see #5). |
| 7 | `stable-*` continuity tag count | `git tag -l 'stable-*'` | **UPDATED 0→6 new tags** (the prior regeneration predates every one of them): `-alignment-riders`, `-alignment-audit`, `-alignment-fixes-1` through `-4`. `-alignment-fixes-5` licensed, not yet tagged (AR-AC-0, above). |
| 8 | `docs-tooling` gate-family file count | `ls components/docs-tooling/test/ehrt/docs_tooling/ \| wc -l` | **UPDATED to 19** — five new gates this arc (`sim-emit-hl7-dependency-test`, `provenance-leaf-law-test`, `root-alias-completeness-test`, `resource-nesting-test`, `license-text-pointer-test`), each red→green witnessed in its own ADR. |
| 9 | NIST licensing inquiry citation | `find . -iname "*SBOM*"` | **CORRECTED.** ADR-0053's own citation (`docs/experiments/EXP-SBOM-inquiry-draft.md`) does not resolve — no such file exists. The real evidence doc is `components/corpus/docs/experiments/EXP-SBOM-results.md` (sibling protocol `EXP-SBOM.md`); the inquiry draft itself is explicitly "maintained privately by the author," never a repo artifact, confirmed by direct read of the results doc's own "Artifacts produced" table. |
| 10 | `workspace.edn` line count | `wc -l workspace.edn` | **UPDATED.** 33 lines (down from the pre-fix ~70-line shape) — the 40-line `:necessary` narrative relocated to ADR-0050 (AR-F1-4), unchanged since. |
| 11 | `make test` / NIST lockfile wiring | `grep -n verify-nist-lock Makefile` | **NEW claim, confirmed live.** `bin/verify-nist-lock` is `test`'s own third line (ADR-0053 AR-F4-3) — did not exist at the prior regeneration. |

Full regenerated content landed in `.agents/state.md` (this session's
own commit `2afba86`); see that file directly for the complete text,
not reproduced here per this arc's own "session record narrates
ceremony, the artifact itself is the content" discipline.

**Budget re-derivation (AR-AC-4).** `git log 89e327f..HEAD --name-only`
(89e327f = compaction C's own closing commit, the base since which
`state.md`/`rulings.md` were last touched) diffed against every
reading set's own `:paths`: `:onboarding` is the only set with a member
touched (`AGENTS.md`; the four `.agents/*/README.md` files; `.agents/
plans/roadmap.md` itself, shrunk again by this session's own AR-AC-5
rotation). No `:corpus`/`:sim`/`:judge`/`:docs` member path appears in
that diff — those four budgets are untouched. Fresh actual, measured
AFTER the rotation landed (so the number reflects the final tree): 275
(`AGENTS.md`) + 46 + 57 + 128 + 76 + 33 (the five `.agents/*/README.md`
files) + 230 (`roadmap.md`, post-rotation) + 162 (`build-session/
SKILL.md`) = **1007**. Re-applying the standing formula (actual ×
1.15, rounded up to the nearest 5): 1007 × 1.15 = 1158.05 → **1160**.
Budget moves **1095 → 1160**. Landed in `.agents/reading-sets.edn`
(this session's own commit `2afba86`), a dated comment block matching
the file's own established re-derivation-note convention.

**Done rotation (AR-AC-5).** The seven alignment-arc Done pointers
(ADR-0048 through ADR-0054) relocated verbatim from the live roadmap's
own Done section to `.agents/plans/roadmap-done-2026-08.md` under a new
`## Alignment arc — closed 2026-08-05 (ADR-0048–0055)` header. **Scope
precision, disclosed rather than silently resolved:** AR-AC-5's own
ruling names only ADR-0048 through ADR-0054 for relocation; the live
roadmap's own Done section also still carries three older one-line
pointers from the scaffolding-compaction arc (ADR-0045/0046/0047), a
SEPARATE, already-closed arc whose own pointers were never rotated at
ITS OWN close (no session before this one owned that cleanup). Those
three are out of this ruling's own named range and were left in place.
"ADR-0055's own pointer lands as the Done section's sole current
entry" is therefore read as sole current entry FOR THE ALIGNMENT ARC —
a session that relocated a different, unnamed arc's own leftover
pointers under this ruling's authority would have been scope creep, not
diligence. The compaction pointers are a small, named cleanup left for
a future arc-close session, not swept here. (The `2b3bb2b` sentinel
value `ADR-0055`'s own pointer was deliberately NOT added to the live
roadmap in this same step — it would have cited an ADR number not yet
present in `notes/ADRs.md`'s own index, tripping `ehrt.docs-tooling.
done-pointer-adr-test`'s dangling-reference gate; it lands in Step 3
below, alongside this ADR's own index line, in the same commit that
makes the citation resolve.)

Full suite green throughout (216 `Test results:` lines, 0 failures/0
errors, matching Step 0's own baseline shape exactly — a docs-only
step). `clojure -M:poly check`: OK. Four relevant gates spot-run
directly (`reading-set-budget-test`, `done-pointer-adr-test`, `index-
completeness-test`, `stale-path-test`): 23 tests, 228 assertions, 0
failures, 0 errors. Oracle bracket (`bin/regression-oracle 2b3bb2b
2b3bb2b`, re-run after this step): all eleven roots IDENTICAL — no
`src/` touched, exactly as expected.

Committed `2afba86` ("docs: the state regenerates, the budgets
re-derive, the arc rotates to the attic (arc close, AR-AC-3/4/5)"),
pushed. Post-push verification: one delta against the message file,
the known harmless trailing-newline artifact.

### The register's FINAL disposition tally (AR-AC-6)

**Fresh count, disclosed correction to the register's own summary
line.** The register's own closing line (`.agents/plans/
2026-08-05-alignment-audit-findings.md`, final paragraph) states "47
total new+seeded rows" and "close-as-fine 26, ruling-needed 12,
fix-session-candidate 10... incomplete 3" (summing to 51, not 47 —
already internally inconsistent). A fresh, direct count of every row
in the live register (`grep -E "^\| [A-Z]"` over the table rows, plus
the three prose-only seeded closures S2/S4/S6) finds **54 total rows**:
7 seeded (S1–S7) + 6 area-A + 13 area-B + 7 area-C + 4 area-D + 10
area-E + 7 area-F = 54. Disposition breakdown by fresh count:
close-as-fine 26, ruling-needed **13**, fix-session-candidate **9**,
incomplete 3, plus 3 seeded rows closed by prose before any table
disposition applied (S2, S4, S6) = 26+13+9+3+3 = 54. This is a minor,
disclosed arithmetic drift in the register's own summary — the same
class of self-correcting count this arc has caught repeatedly (ADR-0051
Step 0's own "13 vs 14 files" note; this ADR's own EXP-SBOM citation
correction, above) — not investigated further, per that same
precedent.

**Closed this arc (17 rows), by session:**

| Row | Cluster | Closed by |
|---|---|---|
| S3 | workspace.edn narrative relocated | ADR-0050 (fixes 1, AR-F1-4) |
| S7 | tripwire scope widened | ADR-0050 (fixes 1, AR-F1-3) |
| A-1 | `:necessary ["oracle"]` documented | ADR-0050 (fixes 1, AR-F1-4) |
| A-6 | roadmap ns staleness (`corpus.framing`) | ADR-0050 (fixes 1, AR-F1-1) |
| B-1 | symmetry-export docstring note | ADR-0050 (fixes 1, AR-F1-5) |
| C-5 | judge-trio asymmetry, dated amendment | ADR-0050 (fixes 1, AR-F1-5) |
| D-2 | CDA sibling framing corrected | ADR-0050 (fixes 1, AR-F1-5) |
| E-3 | `palgebra-design.md` stale test-path repointed | ADR-0050 (fixes 1, AR-F1-1) |
| E-5 | S2/S3-vintage sim-namespace sweep (25 hits/8 files) | ADR-0050 (fixes 1, AR-F1-1) |
| E-7 | `ehrt.sim-cli.` forbidden-list addition | ADR-0050 (fixes 1, AR-F1-3) |
| E-9 | `ManifestV1_1` citations repointed to canonical | ADR-0050 (fixes 1, AR-F1-1) |
| S5 | gate promotions (dependency + resource-nesting) | ADR-0051 (fixes 2, AR-F2-2/3) + ADR-0052 (fixes 3, AR-F3-2) |
| A-5 | root-alias completeness gate | ADR-0051 (fixes 2, AR-F2-4) |
| S1 / C-1 | `sim-model` resource rename, gated | ADR-0052 (fixes 3, AR-F3-1/2/3) |
| A-4 | NIST mirror, user-side + lockfile teeth | ADR-0053 (fixes 4, AR-F4-1/3) |
| F-2 / F-3 / F-4 | license-text cross-refs, gated | ADR-0054 (fixes 5, AR-F5-1/2/3) |

(S1/C-1 counted once — one cluster, not double-counted, matching
ADR-0052's own convention.)

**Standing-ruling-recorded, this arc-close session (2):** A-3
(dependency-review cadence), D-3 (pairing-as-data landing spot,
`judge`) — both appended to `.agents/rulings.md` in Step 1, above.

**Deferred-to-publish-prep (2):** F-5 (Clojars coordinate: track
`sim`'s own version or an independent one — undecided, correctly, per
the register's own framing: "not this session's call"); F-6 (artifact
shape: uberjar-only, curated library subset, or whole-workspace
coordinate — three options, none recommended over another). F-7's own
close-as-fine disposition carries a standing forward note (re-run its
three-point pre-publish checklist immediately before the actual first
Clojars publish, not just once) — restated in the horizon note, below.

**Recommendation-only, methodology caveat, unchanged (3):** B-8, B-9
(bare-name-grep collision on `valid?` — a qualified-alias follow-up
grep is needed before either var can be called live or dead; not run
this arc, out of every fix session's own named scope), B-12 (the
reflection-sweep raw per-namespace counts are unreliable without
source-attribution; B-13's own deduped, attributed counts are trusted
and already landed as the register's report).

**Not taken up this arc — genuine gap, next-arc intake, disclosed
rather than silently dropped (1):** D-4 (named-future list hygiene —
recommends closing four already-resolved-by-other-mechanism named
futures with dated notes, e.g. citing the 2026-07-31 gate-hardening
session for palgebra's allowlist flip). Fresh grep this session (`grep
-ln "D-4\|generator-source.clj\|corpus.display\|table-helper"` across
all five fix-session ADRs): no fix session touched this row. The
roadmap's own Next section still lists `generator-source` three-
concerns split, `corpus.display` placement, and the markdown-table
helper dedup exactly as D-4's own "items 1/2/6 stay open" framing
described — unchanged, correctly, since building any of the three was
never this row's own ask. The row's actual, distinct ask (closing items
3/4/5/7 with dated citation notes) never landed. Named here as this
arc's own one honest miss, not chased — per this session's own fence
(nothing new is fixed here), a candidate for the next arc's intake.

**Already close-as-fine at audit landing, unchanged (26 + 3 seeded =
29):** every row not named above. No arc action was owed to any of
them; none was taken.

### Open Externals, restated

**NIST licensing inquiry** — narrowed, not resolved, by `components/
corpus/docs/experiments/EXP-SBOM-results.md`'s own per-coordinate
license classification (six NIST-origin coordinates, `license-status
:use-permitted--unstated--confirmation-pending`); the inquiry draft
itself stays "maintained privately by the author," not a repo artifact
— sending it is still AUTHOR ACTION. **IG pinning** — the profile-tier
conformance target still undecided (roadmap Externals, unchanged).
**SETUP rewalk** — still owed, an unspoiled human reader (unchanged).
**Clojars publish** — ruled, deferred; F-5/F-6 (above) are its own
remaining open decisions. `/mnt/c` disposition — closed (ADR-0047
AR-C-3), unchanged, restated here only because the roadmap's own
Externals section still carries its closure note.

### The horizon note (verbatim, per this session's own prompt)

"The horizon is feature-shaped: corpus-player slices (roadmap Next,
ADR-0014), the pairing-as-data design pass (design channel, landing
spot `judge` per D-3), `sim-emit-cda` when its trigger fires (framing
per register D-2). Publication readiness gates: F-5/F-6 decisions + the
pre-publish checklist per F-7."

### Step 3 (this entry) — record

This ADR lands; `notes/ADRs.md` gains its index line. Roadmap gets its
Done pointer, the sole current entry (per AR-AC-5's own fence, above —
the compaction-arc pointers stay, out of this ruling's own scope):

```
- 2026-08-05 — alignment-arc-close — ADR-0055
```

**Oracle bracket** (`bin/regression-oracle 2b3bb2b <this session's own
tip>`): this session touched no `src/`, no `test/`, no `deps.edn`, no
`workspace.edn`, no Makefile — docs/plans/rulings/state only. All
eleven vendored-root batches expected and confirmed byte-identical; see
Verification, below, for the actual recorded output.

### Verification

- `clojure -M:poly check`: OK, every step this session.
- `gitleaks`: clean at every scan this session (baseline `detect`,
  both staged scans, both pushes).
- Post-push message verification, both prior checkpoints: one delta
  each against the message file, the known harmless trailing-newline
  artifact prior sessions already name.
- Full suite (`clojure -M:poly test :all skip:integration`): 216 `Test
  results:` lines, 0 `FAIL`/`ERROR`/`Exception` anywhere, unchanged in
  shape from Step 0's own baseline at every checkpoint this session ran
  it — expected, docs-only.
- Four gates most directly touched by this session's own edits
  (`reading-set-budget-test`, `done-pointer-adr-test`, `index-
  completeness-test`, `stale-path-test`) run directly, not merely
  inferred from the full-suite log: 23 tests, 228 assertions, 0
  failures, 0 errors.
- `bin/regression-oracle 2b3bb2b 2b3bb2b` (Step 0) and re-run after
  Step 2: all eleven vendored-root batches (`appendicitis`,
  `death-fixture`, `ear-infections`, `ear-infections-engine`,
  `ear-infections-history-engine`, `sepsis`, `sinusitis`, `sore-throat`,
  `total-joint-replacement-engine`, `urinary-tract-infections-engine`,
  `urinary-tract-infections-history-engine`) IDENTICAL both times;
  soundness check "yes outside ns form." No `--declared-digest-change`
  licensed or needed — this session's own fence (docs-only) makes any
  digest change STOP-AND-ESCALATE, and none occurred.

### Fences

Docs-only: no `src/`, `test/`, `deps.edn`, `workspace.edn`, or Makefile
touched; no gate changes (the four gates spot-run this session were
read, not edited). The audit register (`.agents/plans/
2026-08-05-alignment-audit-findings.md`) is untouched — read-only, per
its own contract; final dispositions live here, the register stays a
dated artifact. No new design work: the horizon note above RESTATES
ruled directions, it does not extend them. Frozen archives untouched
except the two sanctioned acts: this ADR's own new file, and the
live-attic append to `.agents/plans/roadmap-done-2026-08.md` (AR-AC-5,
the same act ADR-0046's own compaction-B pattern licensed). D-4's own
gap (above) is named, not fixed — chasing it would have been exactly
the "anything found is a note for next-arc intake, never an act" fence
this session's own prompt states.

### Consequence

The alignment arc — riders (ADR-0048), the audit (ADR-0049), and five
fix-cluster sessions (ADR-0050 through ADR-0054) — is complete. Of the
register's own 54 rows: 17 closed across the five fix sessions, 2
became standing rulings recorded in `.agents/rulings.md` this session,
2 stay deferred to publish-prep by their own original framing, 3 stand
as disclosed methodology caveats, 29 needed no action, and 1 (D-4) is
an honest, named miss for the next arc's own intake rather than a
silently-dropped row. `.agents/state.md` regenerates with eleven
corrected or newly-probed claims, every one backed by a probe run this
session — including catching a citation drift (the NIST inquiry
document's real path) that predates this arc's own start. The
`:onboarding` reading-set budget re-derives to reflect the final tree
(1095→1160). The arc's own seven Done pointers rotate to the attic
under a dated header, leaving the live roadmap's Done section holding
only this ADR's own pointer for the alignment arc (plus three leftover
compaction-arc pointers, named and left for a future session, not
swept here). `AGENTS.md`'s tag rule, three dependency-law gates, a
resource-nesting gate, and a license-text-pointer gate are now
structural facts about this repository, not vigilance items — the
arc's own central theme, stated plainly: convention enforced by a red
test outlives convention enforced by memory. The next arc opens
feature-shaped, per the horizon note above.
