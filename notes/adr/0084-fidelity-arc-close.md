## ADR-0084 — Fidelity arc close: the interpreter tells upstream's truth, and the record tells its own

**Status:** Accepted (author-directed, autonomous session per R30), 2026-08-08.

### Context

Prior: `notes/adr/0083-fidelity-payoff.md` closed the fidelity arc's
own payoff rider — `anemia___unknown_etiology.json` vendored clean,
`colorectal_cancer.json`'s misdiagnosis corrected to its own true,
still-undiagnosed blocker. This session CLOSES the fidelity arc
(ADR-0081–0084) per the standing close pattern (ADR-0080/0074 the
models): rulings appends, the dependency-review cadence, `state.md`
regeneration, budget re-derivation, Done rotation, and the closing
ADR. Docs-only; anything new found is intake.

**This close ran across two Code sessions — the second time this repo
has needed the resumption pattern, and the second time in a row an arc
close specifically has been the casualty.** The first session executed
Step 0 (AR-FC-0: verified `stable-20260808-fidelity-payoff` already
present at `a13bc0b`, disclosed, not re-created) and Step 1 (AR-FC-1/2:
the rulings appends plus the `libs :outdated` cadence, committed
`e7961b9`, pushed, verified, CI watched green), then was killed
mid-ceremony by an infrastructure-level API request block unrelated to
the work's own content — before Step 2 began. No work was lost: both
of the first session's own commits were already pushed and CI-green.
The design channel verified the seam by fresh public-clone probe (tip,
tag, rulings-append text, all re-read against the live remote) before
authoring this session's own resumption prompt, which marked Step 0/
Step 1 READ-ONLY; this session executed Steps 2–3 only.

This session's own preflight (re-run, not inherited): working directory
confirmed the ext4 clone (`~/src/ehr-testing-tools`, `uname -a` shows
Linux/WSL2), tip `e7961b9` exactly, working tree clean. Baseline:
`clojure -M:poly check` OK; oracle pre-digest (`bin/regression-oracle
e7961b9 e7961b9`) all twenty-eight roots IDENTICAL, soundness "yes
outside ns form"; last-five CI runs on `main` disclosed, all green
(`e7961b9`, `a13bc0b`, `85ba040`, `841df9a`, `82d1753` — no red window).
Full suite (`clojure -M:poly test :all skip:integration`) run twice:
the first run surfaced a genuinely new, unrelated finding —
`ehrt.conformance.mutate-stdout-stdin-loopback-test`'s own
`^:integration`-tagged loopback test failed once
(`:malformed-mllp-frame`) under heavy concurrent JVM load (this
session's own parallel background oracle/suite runs); the identical
piped shell command succeeds standalone (verified twice, directly), and
a second, independent full-suite run came back clean at 275 namespaces
/ 521 assertions / 0 failures / 0 errors — the disclosed disambiguation
proving a load-sensitive flake in a subprocess-piping test, not a
regression from this arc's own work (untouched by any fidelity-arc
commit; `git log --all` shows no touch to that test file since the
pre-monorepo era). Named in `.agents/state.md`'s own Live work section
as intake for the next session that owns test-suite hygiene — this
session's own docs-only fence forbids fixing or further investigating
it.

R30 ceremony. Read-first (this session): `notes/adr/0080-quality-arc-
close.md` (the pattern); `.agents/rulings.md` in full (confirmed the
fidelity-arc append already landed, Step 1); `.agents/state.md` in full
(the quality-close regeneration, now stale in exactly the ways this
session's own regeneration table corrects); `.agents/reading-sets.edn`;
ADR-0081–0083 in full; `.agents/plans/roadmap.md` (Done holding four
pointers to rotate; the Deferred rows this arc split/added).

### Decision

Author rulings, recorded verbatim (design channel, 2026-08-08 —
originally ruled for the single-session close prompt; carried forward
unchanged into the resumption). `[A]` author-ruled, `[C]`
channel-inferred.

1. **AR-FC-0 `[A — tag law, case (ii); debt recorded in ADR-0083]`.**
   Verified `stable-20260808-fidelity-payoff` already present, annotated
   at `a13bc0b`, message "fidelity payoff landed, design-channel-verified
   2026-08-08 (ADR-0083)"; peeled ref resolves to `a13bc0b6a3ee78cf1e05
   93009cf062f77c4997cb` exactly, both locally and via `git ls-remote`.
   **Executed by the killed session's own Step 0; re-verified this
   session, not re-created.**

2. **AR-FC-1 `[C — both appends consolidate this arc's executed
   discipline; the author may strike either]`** (rulings appends). Under
   "From the fidelity arc (ADR-0081–0084)" in `.agents/rulings.md`: (a)
   semantics changes are predicted before they are made, standing — an
   interpreter/engine/emitter semantics change runs a blast-radius probe
   over every oracle root FIRST, lands a per-root identical-or-moves
   prediction, and any mover is STOP-AND-REPORT for an explicit license
   naming that mover alone (ADR-0082's own executed protocol, R3 as
   ruled and exercised — including the trace-then-license resolution the
   author ruled when the probe fired). (b) plausible-by-adjacency is not
   a diagnosis, standing — a defect attributed to a shared mechanism
   without a direct probe of the failing artifact is `[unverified]`
   until the probe exists; ADR-0072's colorectal diagnosis was inference
   from a shared submodule, overturned by the first trajectory scan
   (ADR-0082/0083's own erratum chain). **Executed by the killed
   session's own Step 1** — commit `e7961b9`; both appends re-verified
   by direct read this session, matching the ruling's own text exactly,
   no discrepancy.

3. **AR-FC-2 `[A — standing cadence]`.** `clojure -M:poly libs
   :outdated`; dated report below; no edit follows. **RE-RUN this
   session** (the killed session's own transcript-only output did not
   survive termination — no reconstruction from memory, a real run
   instead, per the resumption prompt's own explicit instruction).

4. **AR-FC-3 `[A — state.md's regeneration contract]`** (state
   regenerates). Every `[V]` claim probe-backed THIS session; skeleton
   preserved. **Executed** — commit `0227f2a`; regeneration table below.

5. **AR-FC-4 `[A — standing budget rule]`** (budgets). Re-derive every
   reading set whose member paths changed across `42cd1e0..HEAD`, AFTER
   the regeneration lands. **Executed** — commit `0227f2a`; only
   `:onboarding` moved (below).

6. **AR-FC-5 `[C — rotation mechanics per the five prior closes]`**
   (rotation). Done holds four pointers (0080–0083). **Executed** —
   commit `0227f2a`; ADR-0080's own pointer joined the attic's
   quality-review-arc section with a dated leftover note; ADR-0081–0083
   rotated under `## Fidelity arc — closed 2026-08-08 (ADR-0081–0084)`.
   ADR-0084's own pointer lands below (Step 3, sentinel-avoidance).

7. **AR-FC-6 `[A for cited items; C for composition]`** (the closing
   ADR). This entry — executed Step 3: rulings verbatim; the arc
   summary; the arc narrative; the intake list; the Externals; this
   close's own successor tag debt; the horizon note; PLUS the
   two-session deviation record and the interruption-pattern note
   (below).

### Step 0 — Preflight + tag (executed by the killed session; re-verified this session)

Tag `stable-20260808-fidelity-payoff` verified present at `a13bc0b`,
peeled ref resolving exactly, both locally and via `git ls-remote` —
confirmed independently by this session, not inherited from the killed
session's own transcript claim.

### Step 1 — Appends + cadence (AR-FC-1/2, executed by the killed session; cadence re-run this session)

Two rulings landed in `.agents/rulings.md` under "From the fidelity arc
(ADR-0081–0084)" (Decision, above), commit `e7961b9` ("docs: the
fidelity arc's law is appended — predictions precede semantics,
adjacency is not diagnosis (arc close, AR-FC-1/2)"), pushed, CI watched
green by the killed session before it died — re-verified this session
by direct read against the live tree, both appends matching the
ruling's own text exactly.

**AR-FC-2 — the `libs :outdated` report, 2026-08-08, re-run fresh this
session against tip `e7961b9`:**

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

**Unchanged from the quality-review arc's own AR-QC-2 report** — every
coordinate, version, and `latest` value identical: no new upstream
release surfaced across the entire fidelity arc (three sessions, none
of them touched `deps.edn`). The same three coordinates still show a
newer `latest` (`hapi-fhir-base`/`hapi-fhir-structures-r4`
8.2.0→8.10.1; `org.babashka/cli` 0.12.79→0.12.86, dev-tooling-only). No
listed upgrade reads as security-relevant — a NOTE for next-arc intake,
not an act. No `deps.edn` edit made or considered.

### Step 2 — State + budgets + rotation (AR-FC-3/4/5)

**Sequencing, disclosed:** the staleness tripwire
(`state_staleness_tripwire_test.clj`, ADR-0079's own gate) checks
COMMITTED state — this file (`notes/adr/0084-fidelity-arc-close.md`)
does not exist on disk until Step 3, so Step 2's own `state.md` commit
kept its header citation pointing at ADR-0080 (the newest `*-arc-
close.md` file on disk at that commit boundary) while regenerating
every other section fresh, then this Step 3 moves the citation to
ADR-0084 in the SAME commit that creates this file — the exact
ADR-0080 Step 2/Step 3 precedent, re-exercised. Verified live: the
tripwire test (and every other touched gate — `reading-set-budget-
test`, `roadmap-deferred-closure-lint-test`, `done-pointer-adr-test`)
ran green against the Step 2 commit before it landed.

**State.md regeneration table (claim → probe → disposition).** Held
claims are not re-listed exhaustively; this table records every claim
that changed since the prior regeneration (`9eb7da9`, ADR-0080) or that
this session's own fresh probe corrected:

| # | Claim | Probe | Disposition |
|---|---|---|---|
| 1 | Component graph — new brick/edge | `git log 42cd1e0..HEAD --name-only`; grep for `deps.edn`/`interface.clj` touches | **HELD — zero new bricks, zero new edges.** Every `src/` edit landed inside `gmf_interpreter.clj`, `census.clj` (`sim-trajectory`), and `digest.clj` (`oracle`, purely additive). |
| 2 | `open-encounter-index`/`:suppressed-encounter-ends`, new this arc | Direct read + fresh `grep` against `gmf_interpreter.clj`, `census.clj` | **NEW section, confirmed live.** The retired `index-of-last-open-encounter` has zero remaining call sites; the counter threads through all four ctx-folding loops. |
| 3 | Vendored module inventory | `ls components/sim/resources/sim/modules/*.json \| wc -l` | **UPDATED 23→24** — `anemia___unknown_etiology.json` joins (ADR-0083). |
| 4 | NOTICE row count | `grep -c '^\| \`' NOTICE` | **UPDATED 69→70** — one new row, the anemia entry. |
| 5 | Oracle root count | Fresh count of `digest.clj`'s own `roots` map; `bin/regression-oracle e7961b9 e7961b9` | **UPDATED 27→28** — `anemia-pair` joins, a FIRST BASELINE, purely additive; all 28 roots fresh-confirmed IDENTICAL at this session's own Step 0. |
| 6 | The two-module `EncounterEnd` blocker | Direct read of `roadmap.md`'s own Deferred section and ADR-0082/0083 | **CLOSED, its own diagnosis corrected.** Anemia vendors clean; colorectal was never actually blocked by this gap — deferred whole under its own true, still-undiagnosed name (`:clinical-content-only-when-admitted`). |
| 7 | The truncation-layer absorbed-error finding | Direct read of ADR-0082's own AR-EE-1a | **NEW section, documented as a mechanism, not chased as a defect** — `compile-trajectory`'s pre-existing `:pre-horizon` gate and `encounter-closed?` single-encounter scope both silently absorbed the dangling reference pre-fix, explaining why the fix's own oracle bracket came back byte-identical even for the licensed mover. |
| 8 | ADR file count | `ls notes/adr/*.md \| grep -v README \| wc -l` | **HELD at 81** at Step 2; this file makes it 82 once it lands (Step 3), the same staleness-at-count-instant pattern every prior regeneration has named. |
| 9 | `stable-*` tag count | `git tag -l 'stable-*'`, excluding the three frozen legacy tags | **UPDATED 32→36** — four new: `-quality-close`, `-fidelity-riders`, `-encounterend-fix`, `-fidelity-payoff`. |
| 10 | Full suite posture | `clojure -M:poly test :all skip:integration`, run twice | **UPDATED 273→275 namespaces**, 521 assertions, 0 failures/0 errors on the second, disclosed-clean run — one new namespace pair (`vendored_anemia_test.clj`, appearing once per lane). First run's own transient `mutate-stdout-stdin-loopback-test` flake disclosed and disambiguated (see Context, above; Live work, below) — never smoothed into an unqualified "green" claim. |
| 11 | Deferred/Next row counts | `awk` over `roadmap.md`'s own sections | **UPDATED 12→13 Deferred** (the EncounterEnd row's own closure split off a new colorectal-only row, net +1); **UPDATED 9→11 Next** (the two AR-FR-2 author backlog rows, fixture relocation and the ADR-footnote fork). |
| 12 | The sibling-flake SOAK | Fresh `gh run list --json` enumeration since `9cc3563` | **UPDATED 12→25 `test`-workflow push runs**, zero recurrence — two unrelated, already-disclosed failures in that span (`ac6ef5f2`/index-completeness, `deabbbdb`/roadmap-deferred-closure-lint), neither the named flake. |
| 13 | The engine `defspec` seed pin | `grep -n seed engine_test.clj` | **HELD, confirmed still live** — `{:num-tests 150 :seed -60645}`. |
| 14 | Census artifact set | `ls components/sim-trajectory/docs/census/*.edn` | **UPDATED, one new file** — `2026-08-08-synthea-7e08387-encounterend.edn` (ADR-0082's own labeled re-run, 85 modules, `{:ok-walked 84, :out-of-scope-by-ruling 1}`, exactly one row changed against the substance census). |
| 15 | Reading-set budgets | Diff every set's `:paths` against `git log 42cd1e0..HEAD --name-only` | **Only `:onboarding` re-derived** — four touched members (`roadmap.md`'s own churn, three READMEs); `:corpus`/`:sim`/`:judge`/`:docs` carry no touched path this arc (the fix/vendoring work touched `sim-trajectory`/`sim-emit-hl7`/`oracle`, none of them a `:paths` member of those four sets) — the first close since the quality-review arc's own "all five moved together" where only ONE set moves. |

Full regenerated content landed in `.agents/state.md` (this session's
own commit `0227f2a`); see that file directly for the complete text.

**Budget re-derivation (AR-FC-4).** `git log 42cd1e0..HEAD --name-only`
(`42cd1e0` = the quality-review arc's own closing tip, the base since
which `state.md`/`reading-sets.edn` were last touched) diffed against
every reading set's own `:paths`: only `:onboarding` carries a touched
member (`.agents/plans/roadmap.md`'s own Now/Done churn across three
fidelity-arc sessions plus this close's own rotation; `.agents/plans/
README.md`, `.agents/prompts/README.md`, `.agents/session-records/
README.md`'s own new entries). `AGENTS.md`, `.agents/skills/README.md`,
`.agents/memory/README.md`, and `.agents/skills/build-session/
SKILL.md` are all UNCHANGED in that diff — this arc touched no
ceremony/skill surface — so `:corpus`/`:sim`/`:judge`/`:docs` stay
untouched. Fresh actual (`wc -l` sum across all eight `:paths`,
measured AFTER the rotation landed): 1216. Re-applying the standing
formula (actual × 1.15, rounded up to the nearest 5): 1216×1.15 =
1398.4 → **1400** (1285 → 1400). Landed in `.agents/reading-sets.edn`
(commit `0227f2a`), a dated comment block matching the file's own
established convention.

**Done rotation (AR-FC-5).** ADR-0080's own pointer — the quality-review
arc's own closing ADR, left as the live roadmap's sole current entry at
that arc's own close — relocated into the attic's EXISTING `##
Quality-review arc — closed 2026-08-07 (ADR-0075–0080)` section, with a
dated append note, the disclosed-leftover class every prior close has
handled for its own predecessor. A new `## Fidelity arc — closed
2026-08-08 (ADR-0081–0084)` header holds ADR-0081–0083's own Done
pointers, relocated verbatim. The live roadmap's own Done section holds
an HTML-comment marker (not a pointer) recording that this ADR's own
pointer is deferred to Step 3 — the same dangling-reference
sentinel-avoidance ADR-0055/0064/0068/0074/0080 have each disclosed.
The Now section's own stale text refreshed to one line: nothing in
progress at this close.

Full suite green throughout (275 namespaces, 521 assertions, 0
failures/0 errors, confirmed by the second, disclosed-clean run —
matching the docs-only step's own expected shape). `clojure -M:poly
check`: OK. Committed `0227f2a` ("docs: the state regenerates, the
budgets re-derive, six arcs rest in the attic (arc close, AR-FC-3/4/5)"),
pushed, verified (one delta against the message file, the known
trailing-blank-line artifact). CI watched to conclusion: see
Verification, below.

### Step 3 (this entry) — ADR + record

**The oracle bracket spanning BOTH sessions' commits.**
`bin/regression-oracle a13bc0b <this session's own closing tip>`: all
TWENTY-EIGHT batches IDENTICAL — docs-only, confirmed across every
commit either session made in this close (see Verification, below for
the exact command and result).

### The arc narrative

The fidelity arc opened by executing the quality-review arc's own named
horizon — the EncounterEnd interpreter design pass, "the oldest
ruled-fix candidate" — and closed having done more than the brief
alone promised: not just a fix, but a correction. Fidelity riders
(ADR-0081, `c2bcb67`) re-verified the design brief field-for-field
against upstream `synthetichealth/synthea` `State.java` rather than
carrying it forward on trust, found no factual error, and recorded
three gating rulings (R1 openness-only wellness arms, R2 suppressed-end
visibility, R3 predict-then-confirm). Encounterend fix (ADR-0082,
`82d1753`) executed that license — but not on the first attempt at
landing code: the blast-radius probe R3 itself required, run BEFORE any
fix, found a real, already-shipped dangling reference in
`hypothyroidism`'s own oracle-seed walks, and the session stopped,
traced, and waited for the author's own ruling before writing a line of
the fix — the predict-then-confirm protocol doing its job twice in one
session, once catching a real defect before any code moved, once
catching that the licensed correction turned out invisible at the
oracle's own granularity for a reason the trace named precisely rather
than left mysterious. Fidelity payoff (ADR-0083, `a13bc0b`) closed the
loop the fix opened: anemia's own two-arc-old deferral came home,
pinned by a committed test; colorectal's own misdiagnosis — inference
from a shared submodule, never itself probe-verified — was overturned
by the same kind of evidence (a trajectory scan) that had closed the
interpreter gap in the first place, correcting rather than silently
re-deferring under a name the evidence no longer supported. This
session closes the arc, itself interrupted once and resumed — the
second such interruption in this repo's history, and the second to hit
an arc close specifically (see the deviation record, below).

### Intake, cited

* **Colorectal's own investigation, the arc's top handoff.**
  `colorectal_cancer.json`'s own `:clinical-content-only-when-admitted`
  gap (plus one early `:discharge-follows-admission`), one compile
  layer downstream of the interpreter (`compile-trajectory` or the
  engine, mechanism unknown), reconfirmed byte-identical pre/post-fix by
  ADR-0082, 2-of-3 seeds tried. Revisit trigger: a future session's own
  dedicated investigation — `.agents/plans/roadmap.md`'s own Deferred
  row names it explicitly.
* **The truncation-layer absorbed-error finding** (ADR-0082 AR-EE-1a):
  `compile-trajectory`'s own `:pre-horizon` gate and single-encounter-
  scope `encounter-closed?` rule both, pre-existing and unrelated in
  origin to the EncounterEnd gap, silently absorbed the dangling
  reference before this session's fix ever landed — a documented
  mechanism now known to carry a second, newly significant job, a
  candidate for a future fidelity pass.
* **The review-2 watch-list, restated and carried forward:** the
  sibling-flake SOAK (25 runs since `9cc3563`, zero recurrence, still
  not declared closed per its own stated bar); the census
  `:closure-file-count` undercount (still live in code, now a
  three-times-repeated real cost); wellness-encounters, restated once
  more so it does not slide out of the tracked horizon chain a second
  time; the `notice_verbatim_test` coverage gap (two file shapes still
  outside the gate's own recognized forms, hashes still manually
  verified correct).
* **Vendoring batch 4** (the veteran family) — unbuilt, carried forward
  unchanged from ADR-0080's own horizon.
* **Wave E's risk-attribute/vital-sign register** — unbuilt, unchanged.
* **The author's two backlog rows** (ADR-0081, AR-FR-2), both still
  unbuilt: fixture relocation (`components/corpus/test-fixtures/v2/
  simhospital` and its `v2-nist` sibling, to a top-level home); the
  ADR-footnote fork in user-facing docs (strip-to-dev-docs-only vs.
  footnotes, itself still unruled, its own prerequisite inventory not
  yet done).
* **The pairing-as-data design pass** — ruled IN at the quality-review
  arc's own close, still paused on its four unanswered shape questions
  (granularity, the storefront fixture, the taxonomy snapshot,
  mutation-adequacy as consumer). **Said plainly: this is its second
  close carried since the ruling** — named at ADR-0080's own close, not
  advanced this arc either, carried again rather than left to look
  forgotten.
* **The `mutate-stdout-stdin-loopback-test` flake, newly found this
  session** (see Context, above; `.agents/state.md`'s own Live work
  section): a load-sensitive failure in an `^:integration`-tagged test
  that runs under `skip:integration` regardless of its own tag (the
  flag skips the separate `integration` PROJECT, not individual tagged
  deftests inside `conformance`) — disambiguated as a flake, not a
  regression, by a clean second full-suite run; named for the next
  session that owns test-suite hygiene, not investigated or fixed here.

### Open Externals, restated unchanged

**NIST licensing inquiry** — narrowed, not resolved; still author
action. **IG pinning** — still open. **Clojars publish** — ruled,
deferred; F-5/F-6 remain open. **SETUP rewalk** — still owed. **`/mnt/c`
disposition** — closed (ADR-0047 AR-C-3), unchanged. **The GitHub
workflow-failure notification-email toggle** — still genuinely
unconfirmed, unchanged since the quality-review arc's own disclosure.
None of these six rows was touched by this arc's own work; restated
here, not re-decided.

### This close's own mechanical debt, recorded here

**The next arc's opening session tags `stable-20260808-fidelity-close`
at THIS session's own closing tip under standing ceremony.** No tag is
created by this session for its own closing tip — the tag law's own
case (ii) licenses a session to tag its PREDECESSOR's verified stable
point, not its own mid-flight tip.

### The two-session deviation record

**This close ran across two Code sessions — the second interruption
this repo has needed the resumption pattern for, and the second one
in a row to hit an arc close specifically** (the first: the vendoring
close, ADR-0074, resolved by the identical pattern this session just
re-exercised). An infrastructure-level API request block, unrelated to
the work's own content, terminated the first session immediately after
its own Step 1 landed and pushed. No work was lost: both of the first
session's own commits (the Step 0 tag-verification act, which created
no commit of its own — the tag already existed — and Step 1's `e7961b9`)
were already pushed and CI-green, and were independently re-verified by
the design channel via a fresh public clone before this session's own
resumption prompt was authored. The one casualty was AR-FC-2's own
`libs :outdated` output, captured only in the dead session's own
transcript and lost with it — re-run fresh this session rather than
recalled (Step 1, above).

**The interruption-pattern note, `[C]`, recorded per the resumption
prompt's own instruction, deciding nothing:** two of this repo's last
three arc closes (vendoring, ADR-0074; this one, ADR-0084) have now
been killed by the same infrastructure-level request-block class,
mid-ceremony, both times immediately after Step 1. Arc-close sessions
share a shape that may make them disproportionately exposed — they run
long, touch many files across several steps, and this repo's own
ceremony holds every step to a full preflight-to-push cycle rather than
batching. The design channel's own `[C]` intake suggestion, recorded
here for the author's own future ruling, decided nothing by this
session: **future arc-close prompts might pre-split into two smaller
sessions by design** (e.g., a first session scoped to Steps 0–1 only,
a second to Steps 2–3), reducing the exposure window and the cost of
an interruption when — not if — one lands mid-ceremony again, rather
than relying on the resumption pattern to absorb it after the fact each
time.

### The horizon note (verbatim, per this session's own prompt)

"The horizon, for the author's ruling: colorectal's investigation, the
pairing-as-data registry (four questions from a ruling), vendoring
batch 4, Wave E's register, the fixture-relocation and ADR-footnote
rows, publish-prep (F-5/F-6 + F-7), review 2 on cadence. `sim-emit-cda`
on its trigger."

### Verification

- `clojure -M:poly check`: OK, every step this session.
- Full suite (`clojure -M:poly test :all skip:integration`): 275
  namespaces, 521 assertions, 0 failures/0 errors on the disclosed-clean
  run (a first run surfaced the `mutate-stdout-stdin-loopback-test`
  flake, disambiguated — see Context, above).
- `gitleaks git --staged -v`: clean, every commit this session;
  `gitleaks` ran automatically on every push (pre-push hook), clean
  throughout.
- Post-push message verification, every commit this session: one delta
  each against the message file, the known harmless trailing-blank-line
  artifact.
- `bin/regression-oracle e7961b9 e7961b9` (Step 0, this session's own
  pre-digest): all twenty-eight vendored-root batches IDENTICAL,
  soundness "yes outside ns form."
- `bin/regression-oracle a13bc0b <this session's own closing tip>`
  (Step 3, spanning BOTH sessions' own commits): all twenty-eight roots
  IDENTICAL — confirmed docs-only across the entire two-session close,
  no STOP-AND-ESCALATE trigger.
- Tag verification: `stable-20260808-fidelity-payoff` peeled ref
  resolves to `a13bc0b` exactly, both locally and via `git ls-remote`.
- CI, watched to conclusion at every push this session (not assumed):
  `0227f2a` (Step 2) and this session's own closing commit — see the
  session record for both runs' own URLs and conclusions.
- The staleness tripwire's own sequencing: the Step 2 commit's citation
  (`0080` cited = `0080` newest-on-disk) confirmed by the actual gate
  (green) at that commit boundary before this Step 3 moved it.

### Fences

Docs-only: no `src/`, no `test/`, no config, no gates touched or edited
this session (every gate cited was read, not changed). No new design
work: the horizon note above RESTATES ruled directions, it decides
nothing; the interruption-pattern note above is intake for the author's
own ruling, not a decision. Frozen archives untouched except the
sanctioned acts: this ADR's own new file, and the live-attic appends to
`.agents/plans/roadmap-done-2026-08.md` (AR-FC-5). The
`mutate-stdout-stdin-loopback-test` flake found this session is named,
disambiguated, and left untouched — not fixed, not further
investigated, per this fence.

### Consequence

The fidelity arc — three sessions, opened by executing the quality-
review arc's own named horizon and closed by a two-session-interrupted
close that still landed clean — is complete. `.agents/state.md`
regenerates with fifteen corrected or newly-probed claims, including
two entirely new sections this file never carried before (the
EncounterEnd openness-tracking mechanism, the truncation-layer
absorbed-error finding). Only one reading-set budget moves this close,
the first time since the quality-review arc's own "all five together"
regeneration that a close touches just one. The arc's own three Done
pointers rotate to a new attic header, the quality-review arc's own
disclosed leftover (ADR-0080) joins its own arc's section, and the live
roadmap's Done section holds only this ADR's own pointer. The
interpreter's first semantics change since the GMF coverage waves is
proven, not merely claimed: predicted before it moved anything,
licensed where it moved, absorbed where the tree already hid it, and
honest about the diagnosis it overturned. The oldest deferred-vendoring
blocker in this repo's own history — named at vendoring batch 2,
2026-08-07 — closes; the module it wrongly kept company with gets its
own true, still-open name instead of a borrowed one. And the resumption
pattern this repo built for its own first interruption (the vendoring
close) proved itself a second time, unmodified — the same seam-
verification, the same READ-ONLY discipline for prior steps, the same
disclosed two-session shape — while also surfacing, in its own
preflight, a genuinely new and previously undisclosed test-suite
flake, run to ground rather than smoothed past on the way to a clean
commit.

### Index summary (moved verbatim from notes/ADRs.md by ADR-0143, 2026-08-16)

Fidelity arc close: the interpreter tells upstream's truth, and the record tells its own — closed across two Code sessions after an infrastructure block (the second such interruption, and the second to hit an arc close specifically); state and budgets regenerate (only `:onboarding` moves this time), four Done pointers rotate to the attic, `libs :outdated` re-run fresh, the oracle bracket spans both sessions at 28 roots identical, and an unrelated, newly-found subprocess-piping test flake is disambiguated live rather than smoothed past
