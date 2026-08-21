# Archived prompt: patient-simulator-rename-and-charter (2026-08-20)

Session prompt -- the patient simulator gets its name and its charter:
`sim-trajectory` -> `patient-simulator`, a front-door scope statement
with a gated limitations table, and the care-plan row re-scoped to a
declared limitation -- ADR-0162

## Context

Claude Code under R30 in ehr-testing-tools. HEAD at handoff: 2d937b9
(ADR-0161 addendum; tree clean; CI green at tip; last tag
`stable-20260820-attic-rotation-law` @171ccb6, no tag owed). This
session lands three author rulings from the design conversation of
2026-08-20 (quoted below): the brick rename, the scope charter, and the
`#careplan-guard-resolution` re-scope. The mission sentence the charter
encodes: realistic EHR message traffic is the priority; patient-lifetime
simulation is relevant only inasmuch as it contributes to realistic
traffic.

Channel anchors at 2d937b9 (re-derive every one):

* The isolation already EXISTS: `components/sim-trajectory` holds GMF
  load (`gmf.clj`), lifetime interpretation (`gmf_interpreter.clj`),
  pathway-IR compilation (`compile_trajectory.clj`), `census.clj`,
  `interface.clj`. Its outward `:require` surface is kernel (+ sim-model
  per its own interface docstring -- verify by reading the actual ns
  forms, not my grep). `sim-engine` requires NOTHING from it (every
  mention in engine/schema source is comment prose); the coupling is the
  pathway-IR data contract, wired by `sim`. The rename buys the name;
  the structure is done.
* Rename surface, censused: the brick dir + 5 src namespaces + test
  tree; consumer `:require`s in ~10 files (corpus `player.clj`,
  docs-tooling `strip_fresh.clj`, oracle `digest.clj`:120, sim-check
  `check.clj`, sim-emit-hl7 `emit_hl7.clj`, sim-engine
  `engine.clj`/`event_schema.clj`, sim-model
  `persona.clj`/`pathway.clj`/`interface.clj` -- re-grep, requires only,
  comments follow); `deps.edn` root (:42, :133, :196) and
  `projects/conformance`:68, `projects/integration`:72 aliases; the four
  docs files riding the brick; `docs/glossary.md`; one
  `state-derived.md` hit (regenerates). Comment/docstring mentions of
  the old name across the tree: update those in files the rename already
  touches; elsewhere they are HISTORY and stay (R-RP: prompts, session
  records, ADRs, attics are never edited).
* `digest.clj` requires the interface at :120, so the rename IS a
  digest-source change: `bin/regression-oracle` must abort undeclared,
  then run `--declared-digest-change` with ALL 35 digests IDENTICAL -- a
  pure rename moves no bytes of ground truth; any digest moving means
  the rename was not pure: STOP.
* Charter surfaces today are scattered: 3 in-source
  `UNDECLARED`/`DELIBERATELY` markers in `gmf.clj` (:1244-51, :1326, +
  re-grep), ~25 scope statements across the four docs, an interface
  docstring about split mechanics. No front door.
* Care-plan facts for the charter entry (all tree-verified 2026-08-20):
  care-plan events reach NO wire -- `emit_hl7.clj:86-90` deliberately
  renders nothing ("no real CarePlan-equivalent segment... natural
  rendering is a FHIR CarePlan"); the FHIR emitter has zero care-plan
  hits; `check.clj:444-453` includes `:care-plan-start` in linkage,
  deliberately excludes `:care-plan-end`. S-2 (census :571-625): 4 of 12
  vendored CarePlanEnd states cite by `referenced_by_attribute`; the
  attribute pair is unported (`gmf.clj:1244-51`); 7/7 observed
  `:care-plan-end` events resolve neither field; plans stay `:active`
  forever in ground truth. Site-profile Z bindings resolve against
  the-event-being-rendered + persona ONLY (`context-for-event`,
  ADR-0150) -- cross-event patient state like "active care plans at
  render time" is unreachable by construction.

## Author rulings, verbatim (2026-08-20)

* "patient-simulator is a good-enough name." Rename to
  `patient-simulator` / `ehrt.patient-simulator.*`. "We aren't
  constrained by compatibility in names for packages or namespaces" --
  the public contract is the ground-truth event feed and the CLI
  file-based operations; interior names are free to move. Rename FIRST,
  charter under the new name (the author's own ordering argument:
  (b)-first would create more places (a) must change).
* The charter: the mission sentence above at the front door; the
  deliberate limitations consolidated, each with citation and
  trigger-if-any.
* Care plans: "apart from naming the possibility, we shouldn't spend any
  effort on supporting richer care plan events." So: NO attribute port,
  NO Guard extension, NO emitter work, NO new tests of care-plan
  behavior. The charter entry names the possibility -- the realistic
  consumer is a facility Z-segment fed by care-plan state (ADT-driven
  care coordination: case-management enrollment, pathway membership on
  admission/discharge feeds) -- and states the two-part trigger: fix
  owed when any emitter surface renders care-plan state (a FHIR CarePlan
  resource, OR a render-time patient-context feature reachable by
  site-profile Z bindings), noting that without S-2's fix such a surface
  would render every plan ever started as active -- a plausible-looking
  lie, worse than absence. `#careplan-guard-resolution`: re-scoped,
  demoted from P4 to the bottom of `## Next`, its remedy re-pointed at
  the charter entry, the priced option (attribute-pair port,
  contract-neutral by prediction, declared oracle change on roots
  drawing bronchitis/injuries) preserved in the row text so no future
  session re-derives it.
* Tag: no tag owed at Step 0. Close tag: pay in-session if the tip run
  concludes success while open, else next Step 0 -- say which.

## Read first

1. `components/sim-trajectory/**` whole (it moves); the interface
   docstring; the four docs; the 3+ in-source limitation markers.
2. Every consumer file above; `deps.edn` x3; `Makefile` and `test.yml`
   (grep for the name -- my grep found none, verify); `docs/glossary.md`;
   `bin/regression-oracle` + `digest.clj` :15-25,:120 and
   `rulings.md#R-oracle-script-contract`.
3. Census S-2 (:571-625); ADR-0139 C-2 (:341-349);
   `gmf-interpreter-findings.md:1189`, `gmf-interpreter.md:1359` (the
   standing-request sentences the charter absorbs); the roadmap row;
   ADR-0150 (`context-for-event`), ADR-0156 (declared-digest-change
   mechanics).
4. `rulings.md#R-full-suite-before-push`, `#R-red-pushed-with-green`,
   `#R-session-verifies-ci-via-gh`, `#R-done-attic-rotation` (the
   close's own rotation), `#R-law-surface-propagation`,
   `#R-empty-population-is-red`; build-session skill; budgets (`:sim`
   reading set names the docs paths that move -- check
   `reading-sets.edn` and update paths in the same commit).

## Step 0

Fresh clone, tip 2d937b9; `bin/preflight`; baseline `make test`
unpiped, MAKE_EXIT captured, wrapper ends `exit "$MAKE_EXIT"`,
reconcile vs ADR-0161's 366 blocks / 4,084 tests / 18,336 assertions;
`poly check`; budgets. Census the rename surface yourself (requires,
aliases, docs links, reading-set paths, exercised-sources rows,
Makefile/workflow mentions) and record the file list in the ADR BEFORE
moving anything -- the read-back compares against it. Census the
limitation markers (`grep -n "UNDECLARED\|DELIBERATELY\|not ported"
...` across the brick's src and docs) -- this list is the charter
table's population and the drift lint's baseline.

## Step 1 -- the rename (one commit, mechanical, no content change)

`git mv components/sim-trajectory components/patient-simulator`; ns
forms `ehrt.sim-trajectory.*` -> `ehrt.patient-simulator.*`; consumer
requires; `deps.edn` x3 aliases (`poly/patient-simulator`); docs links;
reading-set paths; glossary entry. Comment mentions updated in touched
files only. `poly check` is the arbiter (zero unresolved). Full `make
test` (counts UNCHANGED vs baseline -- a rename adds nothing; any delta
is a finding). Oracle: `bin/regression-oracle 2d937b9 HEAD` aborts
undeclared (assert exit 1), then `--declared-digest-change` -> 35/35
IDENTICAL or STOP. `make docsgen` (state-derived regenerates). Push.
Commit: "refactor: sim-trajectory renamed patient-simulator -- the
patient-simulation half gets its name; pure rename, declared
digest-source change, 35/35 digests identical (ADR-0162)"

## Step 2 -- red (charter + lint)

(i) A docs-tooling test:
`components/patient-simulator/docs/limitations.md` exists, carries the
mission sentence, and its table's citation column resolves (every cited
file:line-ish anchor exists -- anchor by stable text, not line number,
the roadmap-lint precedent). (ii) The drift lint: every in-source
limitation marker (the Step 0 census's grep, encoded as the test's own
scan) appears in the charter table -- population asserted non-empty
(`R-empty-population-is-red`); plant one marker in a scratch copy,
witness red, withdraw. (iii) The interface docstring's SCOPE section
exists (assert the heading + the mission sentence's presence). Commit:
"test: red -- patient-simulator carries its charter: front-door scope,
limitations table gated against the in-source markers (ADR-0162)"

## Step 3 -- green (the charter)

`limitations.md`: the mission sentence; the dependency-direction
paragraph (traffic consumes patient simulation as compiled input, never
the reverse); the table -- one row per deliberate limitation: attributes
(assign/referenced pair; S-2's evidence; the Z-segment possibility NAMED
as the anticipated consumer; the two-part trigger; the preserved fix
price), expressions (rejected at load), physiology, lookup-table
demographics, three-way ConditionEnd, death-as-event (states parse, no
event kind renders it), the biographical-> pre-horizon-facts compression
-- each: what / why declined / citation / trigger-if-any (most: none).
Interface docstring gains the SCOPE section (three sentences + pointer
to limitations.md). The two docs standing-request sentences
(`gmf-interpreter-findings.md:1189`, `gmf-interpreter.md:1359`) gain a
dated line pointing at the charter entry (append, not rewrite). Full
`make test`; push red+green. Commit: "docs: patient-simulator charter --
mission at the front door, deliberate limitations tabled with citations
and triggers, drift-gated (ADR-0162)"

## Step 4 -- the row re-scope

`#careplan-guard-resolution`: re-titled/re-slugged if you judge the slug
misleading (say; anchors elsewhere must follow -- grep), demoted to the
bottom of `## Next`, body rewritten (six-line cap): declared limitation,
charter entry is the authority, two-part trigger verbatim-short, the
priced option preserved in one line (attribute pair, contract-neutral
predicted, declared oracle change on bronchitis/injuries roots), C-2's
Guard half named as absorbed. NO other row moves. Commit rides Step 3 or
stands alone -- say which.

## Close (self-archive FIRST)

Archive to
`.agents/prompts/2026-08-20-patient-simulator-rename-and-charter.md`;
session record; ADR-0162 (the rename file list vs Step 0's census; the
oracle's two results; the charter table as landed; the marker census;
the row re-scope); roadmap (+ rotation if the cap fires); session record
with `gh run view` id/conclusion; full `make test` reconciled vs Step 0
(Step 1 zero-delta, Steps 2-3 the one new test ns -- predict its
double-count); `bin/post-push-verify`; tag per ruling. Commit: "docs:
ADR-0162 -- patient-simulator named and chartered, close"

## Fences

NO behavior change anywhere: the rename is byte-pure outside
ns/require/alias/link lines (the oracle's 35/35 proves the half that can
be proven; the suite's zero-delta at Step 1 proves the rest); NO
attribute port, NO Guard work, NO emitter change, NO care-plan test, NO
event-schema change (author: name the possibility, spend nothing);
charter files + docstring + the two dated append lines + roadmap are the
only content-bearing edits; R-RP on all history surfaces (old name
survives in prompts/records/ADRs/attics untouched); every planted red
withdrawn; exit codes unpiped; ASCII; anchored register edits; rotation
law honored at close. READ-BACK: the rename file list vs census
(exact); oracle abort-then-identical; suite deltas per step; the charter
table row count vs the marker census; the care-plan row before/after.
