## ADR-0074 — Vendoring arc close: the mix more than tripled, the bytes cannot lie, the front door is open

**Status:** Accepted (author-directed, autonomous session per R30), 2026-08-07.

### Context

Prior: the vendoring arc's fifth session, demos front door, landed and
was design-channel-verified (`notes/adr/0073-demos-front-door.md`, tip
`5e2afaf`). This session closes the vendoring arc (ADR-0069–0074) per
the standing close pattern (ADR-0068 is the model): rulings appends,
the dependency-review cadence, `state.md` regeneration, budget
re-derivation, Done rotation, and the closing ADR. Docs-only; anything
new found is next-arc intake, never an act.

**This close spans two Code sessions.** The first ran Step 0 (preflight
+ tag: verified `stable-20260807-demos-front-door` already present at
`5e2afaf`, peeled, disclosed, not re-created) and Step 1 (the rulings
appends and the dependency-review cadence, committed `beec395`, pushed,
verified) before an infrastructure-level API request block — a refusal
unrelated to the work's content — killed it mid-ceremony. No work was
lost: everything Step 0/1 landed was already committed and pushed. The
design channel verified the seam by fresh public-clone probe (tip
`beec395` confirmed as Step 1's own commit; the tag confirmed present
and peeled at `5e2afaf`; `.agents/rulings.md`'s new "From the vendoring
arc" section confirmed reading as specified) before authoring this
session's own resumption prompt, which named `AR-VAC-0`/`AR-VAC-1` and
Step 0/Step 1 READ-ONLY — verified, never redone — and re-ran
`AR-VAC-2`'s own `libs :outdated` report fresh (the killed session's
own report had been transcript-only and was lost; a real re-run,
required regardless). This session executes Steps 2–3 of the same
prompt.

The arc being closed, five sessions: census substance (ADR-0069,
`cd16fa9` — the qualifier, the ranked catalog: 84 `:ok-walked` modules,
51 `:zero-on-every-seed` / 33 `:produces-content`); batch 1 (ADR-0070,
`d41a278` — five landed, `injuries.json` deferred on a `gmf-
interpreter` `max-steps` runaway loop); batch 2 (ADR-0071, `96424f8` —
seven landed, `anemia___unknown_etiology.json` deferred on an
`EncounterEnd` idiom gap, the `demos/scenarios/`-predecessor
`components/sim/docs/scenarios/` home born); batch 3 (ADR-0072,
`721adb6` — the verbatim-law rider with its gate, four landed,
`colorectal_cancer.json` deferred on the SAME `EncounterEnd` gap);
demos front door (ADR-0073, `5e2afaf` — the operator surface at the
root, byte-witnessed, "See it run" in the README). Sixteen modules
vendored, twenty-three ailments in-tree, oracle roots 11→27, and one
law now mechanically enforced that was only prose before.

R30 ceremony. Read-first (this session, resuming): `git log -3` (the
seam); `.agents/rulings.md`'s new section (what Step 1 landed);
`notes/adr/0068-player-arc-close.md` (the close pattern, regeneration-
table format); `.agents/state.md` in full (regenerated at the player
close, five sessions stale by this point); `.agents/plans/roadmap.md`
(Done holding six pointers to rotate); ADR-0069 through 0073 in full
(their findings feed the intake list); the working tree for any
residue the killed session might have left (none found — working tree
was clean at this session's own Step 0, confirmed by `git status`).

### Decision

Author rulings, recorded verbatim (this session's own driving prompt,
2026-08-07 — restated from the original close prompt, whose remaining
rulings the resumption prompt names in full). `[A]` author-ruled, `[C]`
channel-inferred.

**AR-VAC-0 `[A — tag law ADR-0057 AR-T-1 case (ii); debt recorded in
ADR-0073]`.** Annotated `stable-20260807-demos-front-door` at
`5e2afaf`, message "demos front door landed, design-channel-verified
2026-08-07 (ADR-0073)"; pushed; peeled ref verified. **Executed by the
killed session; verified, not re-created, by this session's own
predecessor design-channel probe and reconfirmed here.**

**AR-VAC-1 `[C — both appends are channel consolidations of this arc's
executed discipline; the author may strike either]` (rulings appends).**
Under "From the vendoring arc (ADR-0069–0074)" in `.agents/
rulings.md`: (a) vendored bytes are law, standing — upstream content
vendors byte-verbatim at its named pin, `-text`-protected from git
normalization, NOTICE-hashed per file, and gate-verified on every test
run (`notice_verbatim_test`); an edit-tempting vendored file is
STOP-AND-REPORT, never a fix. (b) the population-scale gate outranks
the census sample, standing — a module joins the tree only with a
witnessed content-producing engine-layer round trip at population
scale; zero-substance modules are not vendorable; a census verdict is
evidence for curation, never a vendoring license. **Executed by the
killed session (commit `beec395`); verified this session by direct read
— both appends read exactly as specified, no discrepancy.**

**AR-VAC-2 `[A — standing cadence, rulings register A-3]`.** `clojure
-M:poly libs :outdated`; dated report in this ADR; no edit follows;
urgencies are intake notes. **Re-run THIS session** (the killed
session's own output was transcript-only and lost with it) — see
Execution record, Step 3, below.

**AR-VAC-3 `[A — state.md's regeneration contract, AR-C-1]`** (state
regenerates). Every `[V]` claim probe-backed THIS session; skeleton
preserved; content updates at minimum: the vendored-module inventory,
the gate inventory, the oracle posture, the demos/scenarios geometry
(top-level `demos/` with `traces/`/`scenarios/`, the three pointer
stubs, the `.gitattributes` `-text` protections at both protected
trees), the deferred-modules picture, the suite/tag/ADR/Deferred/Next
counts, all by fresh count. Regeneration table below; a wrong-in-kind
discrepancy is STOP-AND-REPORT.

**AR-VAC-4 `[A — standing budget rule]`** (budgets). Re-derive every
reading set whose member paths changed across `b7ed686..HEAD`, AFTER
the regeneration lands.

**AR-VAC-5 `[C — rotation mechanics per the three prior closes]`**
(rotation). Done holds six pointers (0068–0073). Rotate: ADR-0068's
pointer joins the attic's player-arc section with a dated leftover
note; ADR-0069–0073 rotate under a new `## Vendoring arc — closed
2026-08-07 (ADR-0069–0074)` header. ADR-0074's pointer lands as the
sole current entry (Step 3, after the index line — the sentinel-
avoidance every close exercises). The Now section refreshes to one
line. Relocation-not-rewrite throughout.

**AR-VAC-6 `[A for cited/ratified items; C for composition]`** (the
closing ADR). This entry: rulings verbatim; the arc summary; the arc
narrative; the intake list, each item cited; the open Externals
restated unchanged; this close's own successor tag debt recorded IN
THE ADR; the horizon note verbatim; PLUS the two-session deviation
record (this Context section, above).

### Step 0 — preflight + tag (executed by the killed session; verified, not redone)

Cwd confirmed `~/src/ehr-testing-tools` (ext4); tip `5e2afaf`; working
tree clean. `clojure -M:poly check`: OK. Oracle pre-digest: all
twenty-seven roots IDENTICAL. `stable-20260807-demos-front-door` did
not exist; created annotated at `5e2afaf`, message "demos front door
landed, design-channel-verified 2026-08-07 (ADR-0073)"; pushed;
verified — peeled ref resolves to `5e2afaf` exactly. **This session's
own re-verification** (`git cat-file -p stable-20260807-demos-front-
door`): tag object confirmed, peels to `5e2afafba0fc5cace0211c35638a
595383b7f281` exactly, message intact.

### Step 1 — appends + cadence (executed by the killed session; verified, not redone)

Two rulings landed in `.agents/rulings.md` under "From the vendoring
arc (ADR-0069–0074)": vendored bytes are law (standing); the
population-scale gate outranks the census sample (standing). Committed
`beec395` ("docs: the vendoring arc's law is appended -- bytes are law,
the gate outranks the sample (arc close, AR-VAC-1/2)"), pushed. **This
session's own re-verification** (`grep -n "From the vendoring arc"
-A 15 .agents/rulings.md`): both appends present, reading exactly as
AR-VAC-1 specifies. The `libs :outdated` output the killed session
would have captured here was transcript-only and did not survive its
death — **AR-VAC-2 is discharged fresh in Step 3, below, per the
resumption prompt's own explicit instruction, not backfilled from
memory.**

### Step 2 — state + budgets + rotation (AR-VAC-3/4/5)

**State.md regeneration table (claim → probe → disposition).** Held
claims are not re-listed exhaustively; this table records every claim
that changed since the prior regeneration (`f9e4afc`, ADR-0068) or that
this session's own fresh probe corrected:

| # | Claim | Probe | Disposition |
|---|---|---|---|
| 1 | Component graph — new brick/edge | `git log b7ed686..HEAD --name-only`; grep for `deps.edn`/`interface.clj` touches | **HELD — zero new bricks, zero new edges.** The arc's own src touch was `sim-trajectory/census.clj` (an internal function addition), not component wiring. |
| 2 | Vendored-module inventory | `grep -c '^\| \`' NOTICE`; fresh `find` of `vendored_*_test.clj`; direct read of ADR-0070/71/72's own deferred-module sections | **NEW section, confirmed live.** 69 NOTICE rows; 27 round-trip test files; 16 modules landed across three batches; three deferred whole (`injuries.json`, `anemia___unknown_etiology.json`, `colorectal_cancer.json` — the latter two now a two-module blocker on the SAME `EncounterEnd` gap); three zero-substance family siblings recorded not-vendorable. |
| 3 | `docs-tooling` gate-family file count | `ls components/docs-tooling/test/ehrt/docs_tooling/ \| wc -l` | **UPDATED 22→23** — one new gate this arc: `notice_verbatim_test.clj` (batch 3, AR-VB3-R1). |
| 4 | Vendored round-trip family | `find . -iname "vendored_*_test.clj" \| wc -l` | **NEW claim, confirmed live** — 27 files (20 `sim-emit-hl7`, 7 `sim-trajectory`). |
| 5 | Oracle root count | `bin/regression-oracle beec395 beec395` | **UPDATED 11→27** — sixteen new first-baseline roots, this arc's own delivery; all confirmed IDENTICAL at this session's own Step 0. |
| 6 | Demos/scenarios geometry | `find demos -maxdepth 3`; `cat .gitattributes`; `find components/sim/docs/{scenarios,demos} components/sim-emit-hl7/docs/demos` | **NEW section, confirmed live.** Top-level `demos/scenarios/`(`busy-tuesday/`) + `demos/traces/` (six trace dirs); three pointer READMEs in the vacated component-local homes; `.gitattributes` `-text` protection confirmed present at BOTH protected trees in the same file. |
| 7 | Suite posture | `clojure -M:poly test :all skip:integration` | **UPDATED 227→261 namespaces**, 0 failures/0 errors throughout — the 34 new namespaces are the vendored round-trip family, the census substance tests, and `notice_verbatim_test.clj`. |
| 8 | `stable-*` continuity tag count | `git tag -l 'stable-*'`, excluding the three frozen legacy tags | **UPDATED 20→26** — six new: `-player-close`, `-census-substance`, `-vendoring-batch-1`, `-vendoring-batch-2`, `-vendoring-batch-3`, `-demos-front-door`. |
| 9 | ADR file count | `ls notes/adr/*.md \| grep -v README \| wc -l` | **UPDATED 66→71** — five landed this arc (0069–0073); this ADR's own file makes it 72 once it lands. |
| 10 | Deferred row count | `awk '/^## Deferred/,/^## Done/' roadmap.md \| grep -c '^- '` | **UPDATED 11→12** — one new row this arc: `EncounterEnd` no-op-when-nothing-open (opened batch 2, dated-noted batch 3). |
| 11 | Next row count | `awk '/^## Next/,/^## Externals/' roadmap.md \| grep -c '^- '` | **HELD at 9** — the vendoring arc touched zero Next rows; membership unchanged since the player arc's own close. |
| 12 | Vital-sign channel row evidence | Direct read of roadmap's own dated note (AR-VB1-5) | **CORRECTED, already recorded live in roadmap** — post-Wave-VS, `congestive-heart-failure`/`contraceptives` now `:produces-content`; `covid19` alone still `:zero-on-every-seed`. Carried into `.agents/state.md`'s own Live Work section unchanged from roadmap's own text. |
| 13 | Reading-set budgets | Diff every set's `:paths` against `git log b7ed686..HEAD --name-only` | **`:onboarding` re-derived** (AR-VAC-4, below) — the one set with a touched member path; `:corpus`/`:sim`/`:judge`/`:docs` HELD unchanged. |

Full regenerated content landed in `.agents/state.md` (this session's
own commit `2f474b8`); see that file directly for the complete text,
not reproduced here per this arc's own inherited "session narrative
hierarchy" discipline.

**Budget re-derivation (AR-VAC-4).** `git log b7ed686..HEAD
--name-only` (`b7ed686` = the player arc's own closing commit, the base
since which `state.md`/`reading-sets.edn` were last touched) diffed
against every reading set's own `:paths`: one set has a touched member.
`:onboarding` — `.agents/plans/roadmap.md` (Now/Done churn across five
vendoring-arc sessions, shrunk again this session by AR-VAC-5's own
rotation) plus growth in all five `.agents/*/README.md` files (indexing
five new sessions' own records/prompts). Fresh actual, measured AFTER
the rotation landed: 284 (`AGENTS.md`) + 49 + 57 + 147 + 95 + 33 (the
five `.agents/*/README.md` files) + 241 (`roadmap.md`, post-rotation) +
172 (`build-session/SKILL.md`) = **1078**. Re-applying the standing
formula (actual × 1.15, rounded up to the nearest 5): 1078 × 1.15 =
1239.7 → **1240**. Budget moves **1180 → 1240** — an increase, since
five sessions' worth of index/Now/Done churn outpaced this close's own
rotation (unlike the player close's own decrease). `:corpus`/`:sim`/
`:judge`/`:docs` confirmed untouched by the diff — no re-derivation.
Both landed in `.agents/reading-sets.edn` (this session's own commit
`2f474b8`), a dated comment block matching the file's own established
re-derivation-note convention.

**Done rotation (AR-VAC-5).** ADR-0068's own pointer — the player
arc's own closing ADR, left as the live roadmap's sole current entry at
that arc's own close — relocates into the attic's EXISTING `## Player
arc — closed 2026-08-07 (ADR-0066–0068)` section, with a dated append
note, the same disclosed-leftover class every prior close has handled
for its own predecessor. A new `## Vendoring arc — closed 2026-08-07
(ADR-0069–0074)` header holds ADR-0069–0073's own Done pointers,
relocated verbatim. The live roadmap's own Done section holds an
HTML-comment marker (not a pointer) recording that this ADR's own
pointer is deferred to Step 3 — the same dangling-reference sentinel-
avoidance ADR-0055's own AR-AC-5, ADR-0064's own AR-UC-5, and ADR-0068's
own AR-PC-5 have all disclosed, applied preemptively this time rather
than caught live. The Now section's own stale text refreshed to one
line: nothing in progress at this close.

Full suite green throughout (261 `Test results:` lines, 0 failures/0
errors, matching Step 0's own baseline shape exactly — a docs-only
step). `clojure -M:poly check`: OK.

Committed `2f474b8` ("docs: the state regenerates, the budgets
re-derive, four arcs rest in the attic (arc close, AR-VAC-3/4/5)"),
pushed. Post-push verification: one delta against the message file,
the known harmless trailing-newline artifact.

### Step 3 (this entry) — record, and AR-VAC-2's fresh report

**AR-VAC-2 — the `libs :outdated` report, captured fresh THIS session,
2026-08-07, against tip `2f474b8`** (the killed session's own report
was transcript-only and lost with it; this is a real re-run, not a
recollection):

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

**Unchanged from the player arc's own AR-PC-2 report** (`notes/adr/
0068-player-arc-close.md`) — every coordinate, version, and `latest`
value identical: no new upstream release surfaced during the entire
vendoring arc (five sessions, none of them touched `deps.edn`). The
same three coordinates still show a newer `latest`: `ca.uhn.hapi.fhir/
hapi-fhir-base` and `ca.uhn.hapi.fhir/hapi-fhir-structures-r4`
(8.2.0→8.10.1), and `org.babashka/cli` (0.12.79→0.12.86,
dev-tooling-only). No listed upgrade reads as security-relevant — a
NOTE for next-arc intake per AR-VAC-2's own fence, not an act. No
`deps.edn` edit made or considered.

This ADR lands; `notes/ADRs.md` gains its index line; `notes/adr/
README.md`'s own file count corrected 71→72 ("as of ADR-0074"),
verified by `ls`, not arithmetic. Roadmap gets its Done pointer, the
sole current entry:

```
- 2026-08-07 — vendoring-arc-close — ADR-0074
```

**Oracle bracket** (`bin/regression-oracle 5e2afaf <this session's own
tip>`, spanning BOTH sessions' commits): this arc's own close touched
no `src/`, no `test/`, no `deps.edn`, no `workspace.edn`, no Makefile —
docs/plans/rulings/state only. All twenty-seven vendored-root batches
confirmed byte-identical; see Verification, below.

### The arc narrative

The catalog opened honest: the census substance qualifier (ADR-0069)
turned 84 walking modules into 51 that produce nothing and 33 that
produce real content — curation over the raw walk-set had been
impossible before this session, since "walks without throwing" and
"walks and emits" had never been distinguished. The design channel's
curation pass batched the 33 by clinical family, and three sessions
(ADR-0070–0072) landed sixteen of them, byte-verbatim at Synthea's own
pinned commit, hashed, and round-trip-tested at population scale — the
real filter the census's own three-seed sample cannot substitute for,
a lesson the arc learned twice (`injuries.json`, then `anemia___
unknown_etiology.json`) before naming it a standing law. The SAME gate
that refused those two modules — and a third, `colorectal_cancer.json`,
on the identical shared-submodule defect — also caught a genuine
pre-arc bug: `uti_recurrence.csv` had silently drifted from its own
recorded NOTICE hash since the day it landed, normalized CRLF→LF by a
git rule nobody had thought to exempt. The fix (an `-text` rule) and
the mechanism that caught it (`notice_verbatim_test.clj`, re-hashing
every vendored byte against its own NOTICE row on every test run) both
landed in the same session — the verbatim law, previously prose, is now
mechanically enforced, and it enforced itself against a real defect
within the same batch that gave it teeth. The arc closed by moving the
whole surface — the scenarios, the traces, the front door itself — out
of component-local docs trees and up to a top-level `demos/`, so a
stranger reaches a running bed board in two commands without reading a
line of source, and README.md now shows that, not just claims it.

### Intake for the next arc

- **The `anemia_sub` `EncounterEnd` gap — a TWO-module blocker now**
  (anemia, ADR-0071; colorectal, ADR-0072) — the strongest ruled-fix
  candidate for the next arc, and the first candidate interpreter
  change since the coverage waves: `ehrt.sim-trajectory.gmf-
  interpreter/emit-and-advance`'s own `:encounter-end` case never
  checks whether an encounter is actually open before emitting a
  discharge, compiling upstream's "close if open, else no-op" idiom as
  an unconditional close. A design pass precedes any fix (open
  question: silently drop the event, or attach a `:no-op true`
  marker). Revisit trigger: a future session willing to extend that
  one case.
- **The census refinements** (ADR-0070/0071 intake, standing item (b)
  unfired): population-scale walk checking and data-file closure
  counting — the `:closure-file-count` metric counts JSON modules only,
  never lookup-table CSVs, an undercount `asthma.json` (batch 1) and
  the vhd pair (batch 3) both hit; the three-seed sample missed two
  independent population-scale failures this arc alone. Revisit
  trigger: a future session extending the census tool itself, not a
  vendoring session.
- **Wellness-encounters** (the named design item, ADR-0070): upstream's
  own wellness machinery collides with this engine's own
  wellness-cadence design; waits its own pass, never routine vendoring.
- **Batch 4's remainder** (the veteran family: `veteran` +
  hyperlipidemia + substance-abuse-treatment, concurred-plan
  composition) — held, unruled, unscheduled; batch 4 as a whole never
  opened this arc.
- **The zero-substance families and the 51-module zero list**, awaiting
  Wave E's own risk-attribute/vital-sign register: `metabolic_syndrome_
  disease.json`, `vhd_aortic.json`, `vhd_mitral.json` (batch 3's own
  three zero-substance siblings) join the census's own 51-module
  `:zero-on-every-seed` list, all recorded not-vendorable-under-the-gate
  — with CHF/contraceptives' own partial-substance evidence (post-
  Wave-VS, both now `:produces-content`; `covid19` alone stays fully
  blocked) as the register's own nearest unblocking candidate. Revisit
  trigger: multi-module patient assignment (Wave G) giving family
  pairing a runtime meaning, or Wave E's own register landing.
- **The sim event-log input adapter** (player future, ADR-0068 intake,
  `notes/adr/0014-corpus-player.md`) — still Next, still unbuilt,
  untouched by this arc.
- **Pairing-as-data** (design channel, landing spot `judge`, ADR-0050
  D-3) — carried through three closes now, still undone, said plainly.

### Open Externals, restated unchanged

**NIST licensing inquiry** — narrowed, not resolved; still "maintained
privately by the author," sending it is AUTHOR ACTION. **IG pinning**
— the profile-tier conformance target still undecided. **Clojars
publish** — ruled, deferred; F-5/F-6 remain open decisions; F-7's own
close-as-fine disposition still carries its standing forward note.
**SETUP rewalk** — still owed, an unspoiled human reader. `/mnt/c`
disposition — closed (ADR-0047 AR-C-3), unchanged. None of these five
rows was touched by the vendoring arc; restated here, not re-decided,
per this session's own docs-only fence.

### This close's own mechanical debt, recorded here

**The next arc's opening session tags `stable-20260807-vendoring-close`
at THIS session's own closing tip under standing ceremony.** No tag is
created by this session for its own closing tip — the tag law's own
case (ii) licenses a session to tag its PREDECESSOR's verified stable
point, not its own mid-flight tip; the next session inherits that debt
exactly as every prior close has passed it forward.

### The two-session deviation record

**Deviation:** this close did not run as a single Code session, as
every prior arc close has. An infrastructure-level API request block —
a refusal unrelated to the work's own content, not a bug in this
repository or its ceremony — terminated the first session immediately
after its own Step 1 landed and pushed (`beec395`). **Disposition:** no
work was lost. Everything the first session completed (Step 0's tag,
Step 1's rulings appends and commit) was already committed and pushed
before the block occurred, and both were independently re-verified by
the design channel via a fresh public clone before this session's own
resumption prompt was authored — the tip, the tag's peeled ref, and the
rulings append text were each re-read against the live remote, not
assumed from the dead session's own transcript. The one casualty was
non-mechanical: AR-VAC-2's own `libs :outdated` output had been
captured only in the dead session's transcript, which does not survive
termination — this session re-ran the command fresh rather than
recalling or reconstructing the lost output, per the resumption
prompt's own explicit instruction. This session's own prompt marked
Step 0 and Step 1 READ-ONLY: their own landed commits were verified by
direct read and re-probe, never re-executed or re-committed. The
standing discipline this instance confirms, for future sessions that
might be similarly interrupted: a killed session's own landed, pushed,
and independently verified work is never repeated on a hunch — only
re-probed, and the interruption itself is disclosed plainly, in both
the closing ADR and the session record, not buried in one or the other.

### The horizon note (verbatim, per this session's own prompt)

"The horizon, for the author's ruling: the EncounterEnd interpreter
design pass (unblocks two vendored-ready modules), Wave E's own
risk-attribute/vital-sign register (unblocks the attribute-blocked
cluster), vendoring batch 4 (the veteran family), the pairing-as-data
design pass, publish-prep (F-5/F-6 + F-7). `sim-emit-cda` on its
trigger."

### Verification

- `clojure -M:poly check`: OK, every step this session.
- `gitleaks`: clean at every scan this session (baseline `detect`,
  every staged scan, every push).
- Post-push message verification, every checkpoint this session: one
  delta each against the message file, the known harmless
  trailing-newline artifact prior sessions already name.
- Full suite (`clojure -M:poly test :all skip:integration`): green at
  this session's own Step 0 baseline (261 namespaces, 0/0) and again
  after Step 2's own edits (261 namespaces, 0/0, identical shape) —
  matching the docs-only step's own expected shape exactly.
- `bin/regression-oracle beec395 beec395` (this session's own Step 0):
  all twenty-seven vendored-root batches IDENTICAL, soundness "yes
  outside ns form."
- `bin/regression-oracle 5e2afaf <this session's own closing tip>`
  (Step 3, spanning BOTH sessions' commits): all twenty-seven roots
  IDENTICAL — confirmed docs-only across the full two-session bracket,
  no STOP-AND-ESCALATE trigger.
- Tag verification: `stable-20260807-demos-front-door` peeled ref
  resolves to `5e2afaf` exactly (re-confirmed this session, not merely
  inherited from the killed session's own transcript claim).

### Fences

Docs-only: no `src/`, `test/`, `deps.edn`, `workspace.edn`, or Makefile
touched this session; no gate changes (every gate cited this session
was read, not edited). No new design work: the horizon note above
RESTATES ruled directions, it does not extend them; the intake list
NAMES unruled candidates, it decides nothing. Frozen archives untouched
except the sanctioned acts: this ADR's own new file, and the live-attic
appends to `.agents/plans/roadmap-done-2026-08.md` (AR-VAC-5, the same
act every prior arc close has exercised for its own predecessor's
pointer). Step 0 and Step 1's own landed work stayed READ-ONLY this
session — verified by direct probe, never redone.

### Consequence

The vendoring arc — five sessions, sixteen modules landed under a gate
that refused three, one pre-arc defect the same gate then caught, and
the whole operator surface moved to a top-level front door — is
complete, closed across two Code sessions with no work lost to the
interruption between them. `.agents/state.md` regenerates with thirteen
corrected or newly-probed claims, every one backed by a probe run this
session, including two entirely new sections (the vendored module
inventory, the demos/scenarios geometry) this file never carried
before. The `:onboarding` reading-set budget re-derives to reflect the
final tree (1180→1240, an increase — five sessions' own churn outpaced
this close's own rotation). The arc's own five Done pointers rotate to
a new attic header, the player arc's own disclosed leftover (ADR-0068)
joins its own arc's section, and the live roadmap's Done section holds
only this ADR's own pointer. Twenty-three ailments now walk this
project's own engine, byte-verbatim at their upstream pin, hashed, and
mechanically re-verified on every test run — a stranger cloning this
repository today reaches a running bed board, built from real vendored
clinical content, in two commands from the README. The next arc opens
with seven named, unruled intake items (the EncounterEnd design pass
first among them, now blocking two modules instead of one) and one
piece of mechanical debt (the `stable-20260807-vendoring-close` tag),
both recorded here rather than left to be rediscovered — and one
disclosed process lesson: this close itself, interrupted and resumed
across two sessions without losing a single landed commit.

### Index summary (moved verbatim from notes/ADRs.md by ADR-0143, 2026-08-16)

Vendoring arc close: the mix more than tripled, the bytes cannot lie, the front door is open — closed across two Code sessions after an infrastructure block, no work lost; state and budgets regenerate, six Done pointers rotate to the attic, `libs :outdated` re-run fresh, the EncounterEnd two-module blocker named as next-arc's strongest candidate
