## ADR-0080 — Quality arc close: the repo examined itself, ruled on what it saw, and fixed the worst of it

**Status:** Accepted (author-directed, autonomous session per R30), 2026-08-07/08.

### Context

Prior: `notes/adr/0079-lint-family.md` landed fix session 2, closing
eight small, mechanical register rows. This session CLOSES the
quality-review arc per the standing close pattern (ADR-0074/0068 the
models): rulings appends, the dependency-review cadence, `state.md`
regeneration, budget re-derivation, Done rotation, and the closing ADR.
Docs-only; anything new found is next-review intake, never an act.

The arc being closed — the `repo-review` skill's first full cycle,
seeded by an incident bridge: ci current (ADR-0075, `9acb79b` — the
bridge: 32 commits of unwatched red, the derived docs caught up,
preflight learned to look); quality riders (ADR-0076, `89c0d24` — the
skill landed, the sibling flake fixed atomically, preflight widened to
five runs); repo review 1 (ADR-0077, `93bd9a6` — 45 rows, eight lenses,
the first scoreboard: 4 green / 3 yellow / 1 red); result or loud
(ADR-0078, `758f3af` — `ehrt.kernel.io`, eleven sites converted, the
demonstrated silent count-of-zero dead, gated); lint family (ADR-0079,
`8eeafb2` — four gates, four protections, one pinned seed, one
sitting). The register's ruled dispositions are fully executed or
explicitly assigned; the scoreboard's RED dimension is structurally
closed pending re-probe at review 2.

R30 ceremony. Read-first: `notes/adr/0074-vendoring-arc-close.md` (the
pattern); `.agents/rulings.md` in full; `.agents/state.md` in full
(regenerated at the vendoring close, five landed sessions stale);
`.agents/reading-sets.edn`; ADR-0075 through 0079 in full (their
findings and disclosures feed this close); the findings register
(`.agents/plans/2026-08-07-repo-review-findings.md`); `.agents/plans/
roadmap.md` (Done holding six pointers to rotate).

### Decision

Author rulings, recorded verbatim (design channel, 2026-08-07). `[A]`
author-ruled, `[C]` channel-inferred.

1. **AR-QC-0 `[A — tag law, case (ii); debt recorded in ADR-0079]`.**
   Annotated `stable-20260807-lint-family` at `8eeafb2`, message "lint
   family landed, design-channel-verified 2026-08-07 (ADR-0079)";
   pushed; peeled ref verified (`git ls-remote --tags origin` resolves
   `stable-20260807-lint-family^{}` to `8eeafb2` exactly). **Executed
   Step 0.**

2. **AR-QC-1 `[A/C per append]`** (rulings appends). Under "From the
   quality-review arc (ADR-0075–0080)" in `.agents/rulings.md`: (a)
   `[A — ruled AR-RL-5(5)]` multi-seed-once-flagged, standing; (b)
   `[A — ruled AR-RL-5(3)]` the `defspec` seed policy, standing; (c)
   `[C — the arc's own executed discipline; the author may strike]`
   I/O speaks Result or fails loud, standing; (d) `[C — likewise]` CI
   is watched, never waited on, and commits land green, standing.
   **Executed** — commit `9eb7da9`, verified by direct re-read after
   landing, all four appends read exactly as ruled.

3. **AR-QC-2 `[A — standing cadence]`.** `clojure -M:poly libs
   :outdated`; dated report below; no edit follows. **Executed Step
   1.**

4. **AR-QC-3 `[A — state.md's regeneration contract]`** (state
   regenerates). Every `[V]` claim probe-backed THIS session; skeleton
   preserved. **Executed** — commit `c9c3b3f`; regeneration table
   below.

5. **AR-QC-4 `[A — standing budget rule]`** (budgets). Re-derive every
   reading set whose member paths changed across `cd6c56c..HEAD`,
   AFTER the regeneration lands. **Executed** — commit `c9c3b3f`; all
   FIVE sets moved (the shared `build-session/SKILL.md` growth alone
   touches every set), not just `:onboarding` — see Budget
   re-derivation, below.

6. **AR-QC-5 `[C — rotation mechanics per the four prior closes]`**
   (rotation). Done holds six pointers (0074–0079, design-channel
   probed at `8eeafb2`). **Executed** — commit `c9c3b3f`; ADR-0074's
   own pointer joined the attic's vendoring-arc section with a dated
   leftover note; ADR-0075–0079 rotated under `## Quality-review arc —
   closed 2026-08-07 (ADR-0075–0080)`, with ADR-0075's own bridge role
   noted (the incident that seeded the arc rides with the arc it
   seeded, not the arc it followed). ADR-0080's own pointer lands
   below (Step 3, sentinel-avoidance). Now refreshed to one line.

7. **AR-QC-6 `[A for cited items; C for composition]`** (the closing
   ADR). This entry — executed Step 3: rulings verbatim; the arc
   summary; the register's FINAL disposition tally, re-derived from
   the register's own rows a second time, on principle; the arc
   narrative; review 2's inherited watch-list; the open Externals
   restated; this close's own successor tag debt; the horizon note.

### Step 0 — Preflight + tag

Cwd confirmed `~/src/ehr-testing-tools` (ext4, `uname -a` shows
Linux/WSL2); tip `8eeafb2` exactly; working tree clean. `clojure -M:poly
check`: OK. Full suite baseline green (`clojure -M:poly test :all
skip:integration`, exit 0, 273 namespaces, 511 assertions, 0 failures/0
errors). Last-five CI conclusions on `main` at Step 0 preflight: `8eeafb2`
success, `13cc046` success, `9b5c2e1` success, `758f3af` success,
`3684a30` success — all green, no red window to disclose. Oracle
pre-digest (`bin/regression-oracle 8eeafb2 8eeafb2`): all twenty-seven
roots IDENTICAL, soundness "yes outside ns form." AR-QC-0 executed: tag
created, pushed, peeled ref verified against `8eeafb28c7521ebc4d
77913452cd99d4e1e5aa07`.

### Step 1 — Appends + cadence (AR-QC-1/2)

Four rulings landed in `.agents/rulings.md` under "From the
quality-review arc (ADR-0075–0080)" (Decision, above). Committed
`9eb7da9` ("docs: the quality arc's law is appended -- seeds, loudness,
and a watched green (arc close, AR-QC-1/2)"), pushed, verified (one
delta against the message file, the known harmless trailing-blank-line
artifact).

**AR-QC-2 — the `libs :outdated` report, 2026-08-07/08, against tip
`8eeafb2`:**

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

**Unchanged from the vendoring arc's own AR-VAC-2 report** — every
coordinate, version, and `latest` value identical: no new upstream
release surfaced across the entire quality-review arc (five sessions,
none of them touched `deps.edn`). The same three coordinates still show
a newer `latest` (`hapi-fhir-base`/`hapi-fhir-structures-r4`
8.2.0→8.10.1; `org.babashka/cli` 0.12.79→0.12.86, dev-tooling-only). No
listed upgrade reads as security-relevant — a NOTE for next-arc intake,
not an act. No `deps.edn` edit made or considered.

### Step 2 — State + budgets + rotation (AR-QC-3/4/5)

**Sequencing, disclosed:** the staleness tripwire
(`state_staleness_tripwire_test.clj`, ADR-0079's own gate) checks
COMMITTED state — `notes/adr/0080-quality-arc-close.md` (this file)
does not exist on disk until Step 3, so this step's own state.md commit
kept its header citation pointing at ADR-0074 (the newest `*-arc-
close.md` file on disk at that commit boundary) while regenerating
every other section fresh, then Step 3 moves the citation to ADR-0080
in the SAME commit that creates this file. Verified live: the tripwire
test's own regex-extraction logic was simulated against the Step 2
commit before landing it (`0074` cited = `0074` newest-on-disk), then
the full suite confirmed the gate itself green (4/4 assertions) at that
commit. A wrong-order landing is exactly what this gate exists to
catch; this session let it govern the order rather than evading it.

**State.md regeneration table (claim → probe → disposition).** Held
claims are not re-listed exhaustively; this table records every claim
that changed since the prior regeneration (`2f474b8`, ADR-0074) or that
this session's own fresh probe corrected:

| # | Claim | Probe | Disposition |
|---|---|---|---|
| 1 | Component graph — new brick/edge | `git log cd6c56c..HEAD --name-only`; grep for `deps.edn`/`interface.clj` touches | **HELD — zero new bricks, zero new edges.** One `interface.clj` touch (`kernel`'s own, re-exporting the new `io` helper to existing dependents — internal growth, not a new external edge). |
| 2 | `ehrt.kernel.io`, new this arc | Direct read of `components/kernel/src/ehrt/kernel/io.clj` and `io_test.clj` | **NEW section, confirmed live.** `list-files`/`existing-dir-nonempty?`/`rename!`, nine-plus call sites converted (ADR-0078), gated by `io_vocabulary_lint_test.clj` (allowlist `{ehrt.kernel.io, ehrt.sim.run}`). |
| 3 | `docs-tooling` gate-family file count | `ls components/docs-tooling/test/ehrt/docs_tooling/ \| wc -l` | **UPDATED 23→27** — four new: `io_vocabulary_lint_test.clj`, `state_staleness_tripwire_test.clj`, `roadmap_deferred_closure_lint_test.clj`, `test_source_live_path_lint_test.clj`. |
| 4 | `sim`'s own façade surface-identity gate, new this arc | `find . -iname interface_surface_test.clj` | **NEW claim, confirmed live** — `components/sim/test/ehrt/sim/interface_surface_test.clj` (AR-LF-2), witnessed red against a temporary extra var before landing (ADR-0079). |
| 5 | Local docsgen-currency gates, new since ADR-0075 | Direct read of `bases/cli/test/ehrt/cli/help_test.clj` and `components/corpus/test/ehrt/corpus/operators_doc_test.clj` | **NEW claim, confirmed live** — `cli-md-is-current-test`/`operators-md-is-current-test`, in-process byte comparisons alongside CI's own. |
| 6 | ADR file count | `ls notes/adr/*.md \| grep -v README \| wc -l` | **UPDATED 74→77** at this session's own Step 2 (five landed this arc, 0075–0079); this file makes it 78 once it lands (Step 3), the same staleness-at-count-instant pattern every prior regeneration has named. |
| 7 | `stable-*` tag count | `git tag -l 'stable-*'`, excluding the three frozen legacy tags | **UPDATED 26→32** — six new: `-vendoring-close`, `-ci-current`, `-quality-riders`, `-repo-review-1`, `-result-or-loud`, `-lint-family` — each one session's own tag-law case-(ii) execution for its predecessor. |
| 8 | Full suite posture | `clojure -M:poly test :all skip:integration` | **UPDATED 261→273 namespaces**, 511 assertions, 0 failures/0 errors throughout — the twelve new namespaces are the four new docs-tooling gates, the façade gate, the kernel `io` helper's own unit tests, and the register-listed call sites' own expanded coverage (ADR-0078). |
| 9 | Deferred/Next row counts | `awk` over `roadmap.md`'s own sections | **HELD at 12/9** — this arc's own fix sessions touched zero Deferred/Next rows. |
| 10 | Oracle root count | `bin/regression-oracle 8eeafb2 8eeafb2` | **HELD at 27** — no vendoring session ran this arc. |
| 11 | The sibling-flake SOAK | Fresh `gh run list` enumeration since `9cc3563` | **UPDATED 3→12 runs**, zero recurrence (one unrelated, already-disclosed failure, `ac6ef5f`/index-completeness) — see Live work. |
| 12 | The engine `defspec` seed pin | `grep -n seed engine_test.clj` | **HELD, confirmed still live** — `{:num-tests 150 :seed -60645}`. |
| 13 | The review instrument (skill, register, scoreboard) | Direct read + byte-diff of both skill mirrors; register row re-count | **NEW section, confirmed live** — see State.md's own "The quality-review arc's own instrument" section, and the FINAL disposition tally, below. |
| 14 | Reading-set budgets | Diff every set's `:paths` against `git log cd6c56c..HEAD --name-only` | **ALL FIVE sets re-derived** (AR-QC-4, below) — the first close where every set moved in the same regeneration. |

Full regenerated content landed in `.agents/state.md` (this session's
own commit `c9c3b3f`); see that file directly for the complete text.

**Budget re-derivation (AR-QC-4).** `git log cd6c56c..HEAD --name-only`
(`cd6c56c` = the vendoring arc's own closing tip, the base since which
`state.md`/`reading-sets.edn` were last touched) diffed against every
reading set's own `:paths`: `.agents/skills/build-session/SKILL.md` — a
member of EVERY set — grew twice this arc (ADR-0075 AR-CI-3's CI-check
line, ADR-0076 AR-QR-3's five-run widening), so all five sets carry a
touched member; `:onboarding` additionally carries five touched
`.agents/*/README.md` files and `roadmap.md` itself (Now/Done churn,
shrunk again by this session's own AR-QC-5 rotation). Fresh actuals
(`wc -l` sums, measured AFTER the rotation landed): `:onboarding` 1115,
`:corpus` 1788, `:sim` 843, `:judge` 901, `:docs` 727. Re-applying the
standing formula (actual × 1.15, rounded up to the nearest 5) to each:
onboarding 1115×1.15=1282.25→**1285** (1240→1285); corpus
1788×1.15=2056.2→**2060** (2040→2060); sim 843×1.15=969.45→**970**
(915→970); judge 901×1.15=1036.15→**1040** (980→1040); docs
727×1.15=836.05→**840** (775→840). Every budget increases — the shared
skill-file growth alone accounts for `:sim`/`:judge`/`:docs`'s own
moves, each of which carries no other touched path. Landed in
`.agents/reading-sets.edn` (commit `c9c3b3f`), a dated comment block
matching the file's own established convention.

**Done rotation (AR-QC-5).** ADR-0074's own pointer — the vendoring
arc's own closing ADR, left as the live roadmap's sole current entry at
that arc's own close — relocated into the attic's EXISTING `##
Vendoring arc — closed 2026-08-07 (ADR-0069–0074)` section, with a
dated append note, the disclosed-leftover class every prior close has
handled for its own predecessor. A new `## Quality-review arc — closed
2026-08-07 (ADR-0075–0080)` header holds ADR-0075–0079's own Done
pointers, relocated verbatim, with one line noting ADR-0075's own
bridge role — the incident that seeded this arc rides with the arc it
seeded, not the vendoring arc it followed (the ux-epilogue precedent,
inverted). The live roadmap's own Done section holds an HTML-comment
marker (not a pointer) recording that this ADR's own pointer is
deferred to Step 3 — the same dangling-reference sentinel-avoidance
ADR-0055/0064/0068/0074 have each disclosed. The Now section's own
stale text refreshed to one line: nothing in progress at this close.

Full suite green throughout (273 namespaces, 511 assertions, 0
failures/0 errors, matching Step 0's own baseline shape exactly — a
docs-only step). `clojure -M:poly check`: OK. Committed `c9c3b3f`
("docs: the state regenerates, the budgets re-derive, five arcs rest in
the attic (arc close, AR-QC-3/4/5)"), pushed, verified (one delta
against the message file, the known trailing-blank-line artifact). CI
watched to conclusion: run `31251728653`, **success**.

### Step 3 (this entry) — record, and the FINAL disposition tally

**The register's FINAL disposition tally, re-derived from the
register's own rows directly, not its corrected summary — the AR-RL-R
discipline, applied a second time, on principle.** Independent row-by-
row count against `.agents/plans/2026-08-07-repo-review-findings.md`
(45 disposition-carrying rows, matching AR-RL-R's own corrected 45/28/5
exactly, re-confirmed rather than trusted):

* **28 close-as-fine** — untouched, correctly disposed at survey time,
  no action owed.
* **9 fix-session-candidate — 9 of 9 EXECUTED.** D2-3 (façade
  surface-identity gate), D2-5 (Deferred in-place-closure lint), D2-6
  (test-source live-path lint) — all three, ADR-0079. D3-3 (`-text`
  protection extension) — ADR-0079. D3-4 (`artifact.clj`'s unchecked
  `.renameTo`), D4-1 (the 9+-site nil-`.listFiles` sweep), D8-2/D8-3
  (`corpus mutate`'s unwrapped file-read) — the shared root-cause
  cluster, ADR-0078. D7-1 (the `rulings.md` citation correction) —
  ADR-0079.
* **5 ruling-needed — 5 of 5 RULED** (AR-RL-5's own five items,
  ADR-0078). D2-4 (the `state.md` staleness tripwire): ruled ADOPTED
  and mechanically BUILT (ADR-0079). D3-2 (the `defspec` seed policy):
  ruled the middle path — seeds stay unpinned repo-wide; the one
  flaked spec's seed PINNED (ADR-0079). D6-4 (multi-seed-once-flagged):
  ruled ADOPTED, codified in `.agents/rulings.md` THIS session
  (AR-QC-1). D7-5 (pairing-as-data): ruled IN, its own design pass
  opened in the design channel in parallel with the result-or-loud
  session — four shape questions still open, carried to the horizon,
  below. D2-7 (the generalized multi-surface-law-drift scaffold): ruled
  DEFERRED with a named trigger — a THIRD law drifting the hard way
  builds the registry; none has yet (see Live work watch-list, below).
* **3 intake — 1 EXECUTED this session, 2 still open, carried to
  review 2.** D7-6 (wellness-encounters): RE-SURFACED — see the
  horizon note, below, the act this row's own disposition asked for.
  D3-1 (the sibling-flake SOAK) and D6-1 (the census `:closure-file-
  count` undercount, escalated): both stay open — fresh counts in the
  watch-list, below.

**Total: 28 + 9 + 5 + 3 = 45**, matching AR-RL-R's own corrected count
exactly — this session's own independent re-derivation confirms it, not
merely repeats it.

### The arc narrative

The quality-review arc opened with an incident, not a plan: CI had been
red on `main` since ADR-0065 landed, thirty-two commits deep, and
nobody — not a build session, not the design channel's own
verification loop — had been watching (ADR-0075). The fix was
mechanical (two staleness gates, local this time, not CI-only) but the
lesson was procedural: preflight itself had never looked. That lesson
became the arc's own opening act — a rotating, eight-dimension review
instrument (`repo-review`), landed the very next session alongside a
fix for the flake the incident had also surfaced (ADR-0076), immediately
run for the first time (ADR-0077): forty-five rows, every probe
gathered by re-derivation, re-hash, or re-run, never by re-reading a
prior claim as its own verification, closing with a scoreboard the
register's own arithmetic got wrong on the first pass — caught by the
exact discipline the register itself recommends, before this session's
own close ran that discipline a second time to confirm it. The author's
five rulings on the register drove two fix sessions: the highest-
severity cluster first — one root cause, three dimensions, a real,
demonstrated silent-success path where a failed directory listing had
been reading as an empty one (ADR-0078) — then eight small, mechanical
gates that turn four more prose laws into mechanically-checked ones,
each witnessed red before it was made to pass (ADR-0079). Every
fix-session-candidate row closed. Every ruling-needed row ruled. The
register's own scoreboard stays red in name only — the finding is
fixed, the color updates at the next survey, by design, not by
oversight.

### Review 2's inherited watch-list

* **The sibling-flake SOAK** (`merge-config-file`, ADR-0076's own fix,
  landed `9cc3563`): **12 CI runs since, zero recurrence** (one
  unrelated, already-disclosed failure in that span) — comfortably past
  the fix's own stated "roughly one push in five to seven" bar without
  a single recurrence, still not declared closed (the target was never
  "N clean runs, then stop counting"). Re-probe at review 2.
* **The `notice_verbatim_test` coverage gap** (ADR-0079): the v2-nist
  `NOTICE.md` table (2-column, not the gate's 5-column shape) and the
  simhospital `PROVENANCE.md` hash (prose, not a table, not named
  NOTICE) both sit outside the gate's own recognized shapes. Both
  files' hashes are still manually verified correct — a coverage gap,
  not an active drift — extending the parser to a second table shape
  and a differently-named file judged to balloon past "lands small."
* **D2-7's own deferred scaffold**, the generalized multi-surface-law-
  drift registry: DEFERRED with a named trigger, unfired — a THIRD law
  drifting the hard way (after the tag law and the state.md tripwire's
  own now-gated instance) builds it; none has yet.
* **D6-1's census `:closure-file-count` undercount, escalated:** still
  live in code, unfixed since ADR-0074/ADR-0071's own disclosure, now a
  THREE-TIMES-repeated real cost (asthma, vhd-pulmonic, vhd-tricuspid).
  Ruled-deferred by the vendoring arc; this arc's own review
  re-confirmed it live and recommends the author schedule it
  explicitly rather than let it age further as ambient backlog debt.
* **Wellness-encounters, RE-SURFACED here** (D7-6's own executed
  disposition, discharged by this very note): named once (ADR-0070),
  already at risk of sliding out of the tracked horizon chain the same
  day it was named — restated explicitly here, and in the horizon note
  below, so it stops sliding.
* **The register summary re-derivation as standing probe.** This
  session's own FINAL tally (above) is the second time this arc has
  re-derived the register's own disposition counts directly from its
  rows rather than trusting a prior summary — the skill itself now
  names this as a standing probe review 2 should run again, not a
  one-off correction.

### Open Externals, restated unchanged

**NIST licensing inquiry** — narrowed, not resolved; still author
action. **IG pinning** — still open. **Clojars publish** — ruled,
deferred; F-5/F-6 remain open. **SETUP rewalk** — still owed. **`/mnt/c`
disposition** — closed (ADR-0047 AR-C-3), unchanged. **The GitHub
workflow-failure notification-email toggle** (named ADR-0076 AR-QR-3,
zero session cost): this session attempted to probe whether the author
has toggled it (`gh api /repos/.../subscription` → 404, no explicit
override on record; `gh api /user`'s own `notification_email` field is
a different, unrelated setting) and found no API surface this session's
token can reach for a personal Settings → Notifications → Actions
preference — **genuinely unconfirmed, disclosed rather than assumed
either way; the row stays open.** None of these six rows was touched by
this arc's own work; restated here, not re-decided.

### This close's own mechanical debt, recorded here

**The next arc's opening session tags `stable-20260807-quality-close`
at THIS session's own closing tip under standing ceremony.** No tag is
created by this session for its own closing tip — the tag law's own
case (ii) licenses a session to tag its PREDECESSOR's verified stable
point, not its own mid-flight tip; this session inherits
`stable-20260807-lint-family` (AR-QC-0, above) and passes its own tag
forward exactly the same way.

### The horizon note (verbatim, per this session's own prompt)

"The horizon, for the author's ruling: the EncounterEnd interpreter
design pass (two vendored-ready modules blocked, the oldest ruled-fix
candidate), the pairing-as-data registry session (design pass RUNNING
in the design channel, four shape questions pending the author), Wave
E's risk-attribute/vital-sign register, vendoring batch 4 (the veteran
family), the census closure-count refinement, publish-prep (F-5/F-6 +
F-7). Review 2 on the author's cadence call. `sim-emit-cda` on its
trigger."

### Verification

- `clojure -M:poly check`: OK, every step this session.
- Full suite (`clojure -M:poly test :all skip:integration`): green at
  Step 0 baseline (273 namespaces, 511 assertions, 0 failures/0
  errors) and again after Step 2's own edits (273/511/0/0, identical
  shape) — matching the docs-only step's own expected shape exactly.
- `gitleaks git --staged -v`: clean, every commit this session;
  `gitleaks` ran automatically on every push (pre-push hook), clean
  throughout.
- Post-push message verification, every commit this session: one delta
  each against the message file, the known harmless trailing-blank-line
  artifact.
- `bin/regression-oracle 8eeafb2 8eeafb2` (Step 0): all twenty-seven
  vendored-root batches IDENTICAL, soundness "yes outside ns form."
- `bin/regression-oracle 8eeafb2 <this session's own closing tip>`
  (Step 3, spanning every commit this session made): all twenty-seven
  roots IDENTICAL — confirmed docs-only, no STOP-AND-ESCALATE trigger.
- Tag verification: `stable-20260807-lint-family` peeled ref resolves
  to `8eeafb2` exactly.
- CI, watched to conclusion at every push this session (not assumed):
  `9eb7da9` success, `c9c3b3f` success (run `31251728653`), and this
  session's own closing commit — see the session record for its own
  run URL and conclusion.
- The staleness tripwire's own sequencing: simulated against the Step 2
  commit before landing (`0074` cited = `0074` newest-on-disk), then
  confirmed green by the actual gate (4/4 assertions) at that commit
  boundary — the tripwire's own first real exercise, inspected, not
  merely trusted.

### Fences

Docs-only: no `src/`, no `test/`, no config, no `.gitattributes`, no
gates touched or edited this session (every gate cited was read, not
changed). No new design work: the horizon note above RESTATES ruled
directions, it does not extend them; the review-2 watch-list NAMES open
items, it decides nothing. Frozen archives untouched except the
sanctioned acts: this ADR's own new file, and the live-attic appends to
`.agents/plans/roadmap-done-2026-08.md` (AR-QC-5, the same act every
prior arc close has exercised for its own predecessor's pointer).

### Consequence

The quality-review arc — five sessions, opened by an unwatched-CI
incident and closed by turning the review that incident inspired into
standing equipment — is complete. `.agents/state.md` regenerates with
fourteen corrected or newly-probed claims, including two entirely new
sections this file never carried before (the `io` vocabulary law, the
review instrument itself as standing equipment). All FIVE reading-set
budgets re-derive together for the first time, a single shared skill
file's own growth touching every task class at once. The arc's own five
Done pointers rotate to a new attic header, the vendoring arc's own
disclosed leftover (ADR-0074) joins its own arc's section, and the live
roadmap's Done section holds only this ADR's own pointer. A production
I/O call that can fail now speaks through one shared, gated vocabulary
instead of nine-plus different silent-failure shapes; a Deferred row
cannot close in place without disclosure; a test cannot reach for a
live repo path outside its own tracked fixtures; `state.md` cannot go
stale without the very next full-suite run saying so, out loud, at the
session that let it happen — and this session is the tripwire's own
first live exercise, sequenced around deliberately rather than tripped
by accident. The register's forty-five rows are no longer a survey
awaiting rulings; every ruling-needed row is ruled, every fix-candidate
row is fixed, and the two rows that stay genuinely open (the flake soak,
the census undercount) are named for review 2 with fresh counts, not
left to age as ambient debt. The next arc opens with six named
Externals, a five-item ruled horizon, and one piece of mechanical debt
(the `stable-20260807-quality-close` tag) — all recorded here rather
than left to be rediscovered.

### Index summary (moved verbatim from notes/ADRs.md by ADR-0143, 2026-08-16)

Quality arc close: the repo examined itself, ruled on what it saw, and fixed the worst of it — the register's forty-five rows close out (9 fix-session-candidate executed, 5 ruling-needed ruled, the FINAL tally independently re-derived a second time on principle); `state.md` regenerates around its own staleness tripwire's first live sequencing test; all five reading-set budgets move together for the first time; five Done pointers rotate to the attic; the sibling-flake soak (12 clean runs), the census undercount, and wellness-encounters are named for review 2

### Rulings-register history (moved verbatim from `.agents/rulings.md` by ADR-0145, 2026-08-17)

## From the quality-review arc (ADR-0075–0080)

- **Multi-seed-once-flagged, standing** [A — ruled AR-RL-5(5)]: a
  vendoring round-trip that flags a module re-runs at 2–3 well-mixed
  seeds at population scale before any verdict — codifies ADR-0071/
  ADR-0072's own followed practice (findings register D6-4), previously
  precedent-only.
- **The `defspec` seed policy, standing** [A — ruled AR-RL-5(3)]: seeds
  stay unpinned repo-wide, for generator diversity; a spec that has
  actually flaked pins or durably logs its seed (the engine spec,
  ADR-0079); the printed-seed-plus-CI-retention default is sufficient
  otherwise; revisited on the next flake.
- **I/O speaks Result or fails loud, standing** [C — the arc's own
  executed discipline; the author may strike it]: a production I/O
  call that can fail routes through `ehrt.kernel.io` or handles its
  failure mode by name; an I/O failure never impersonates an empty
  result (ADR-0078, gated by `io_vocabulary_lint_test`).
- **CI is watched, never waited on, and commits land green, standing**
  [C — likewise]: preflight discloses the last five runs' conclusions;
  watch-to-conclusion is reserved for sessions whose own claim is about
  CI; no push carries a knowingly-failing test (ADR-0075/0076/0078's
  own pattern shift, superseding the older red-checkpoint-commit
  pattern).
