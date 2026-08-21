## ADR-0162 — the patient simulator is named, and given a charter it can be held to

**Status:** Accepted (author-directed, autonomous session per R30),
2026-08-21.

### Context

Three rulings from the design conversation of 2026-08-20, landed
together because the first changes where the other two are written:

1. **The name.** "patient-simulator is a good-enough name." The brick
   was `sim-trajectory`, which named the OUTPUT of one of its three
   stages rather than the thing it is. "We aren't constrained by
   compatibility in names for packages or namespaces" — the public
   contract is the ground-truth event feed and the CLI's file-based
   operations, not any interior spelling.
2. **The charter.** The mission sentence at the front door:
   *realistic EHR message traffic is the priority; patient-lifetime
   simulation is relevant only inasmuch as it contributes to realistic
   traffic.* Plus the deliberate limitations consolidated in one table,
   each with its citation and its trigger-if-any.
3. **Care plans.** "Apart from naming the possibility, we shouldn't
   spend any effort on supporting richer care plan events." So the
   `#careplan-guard-resolution` row stops being a queued defect and
   becomes a declared limitation.

The ordering is the author's own argument: rename first, charter under
the new name, because charter-first would create more places the rename
must then touch.

The isolation this rename names ALREADY EXISTED and is unchanged here.
The brick's outward `:require` surface is `ehrt.kernel.interface` and
`ehrt.sim-model.interface`, confirmed by reading all five `ns` forms,
not by grep. `sim-engine` requires nothing from it; every mention in
engine and schema source is comment prose. The coupling is the
pathway-IR data contract, wired by `sim`. **The rename buys the name;
the structure was already done.**

### Step 0 — the census, recorded before anything moved

`bin/preflight` exit 0, no findings. Baseline `make test` exit 0 at
**366 blocks / 4,084 tests / 18,336 assertions**, reconciling exactly
against `.agents/session-records/2026-08-20-attic-rotation-law.md`.
`clojure -M:poly check` OK. Reading sets: onboarding 1400/1530, corpus
1836/2045, sim 1278/1405, judge 926/1000, docs 739/785.

Rename surface, censused before the first `git mv`:

| population | count |
|---|---|
| tracked brick files (all move) | 42 |
| live consumer files carrying the name | 77 |
| ...of which rewritten | 75 |
| history files carrying the name (untouched, R-RP) | 162 |

The two live files deliberately NOT rewritten:
`.agents/state-derived.md` (generated — `make docsgen` rewrites it), and
`.agents/plans/README.md:39`, whose line describes the archived
`2026-08-02-sim-split-plan.md` by the names that plan itself used;
changing it would misdescribe the document it indexes. It carries no
`components/sim-trajectory` path form, so no path gate sees it.

Marker census — the charter table's population and the drift lint's
baseline. Exactly three, all in one file:

| file:line | marker | subject |
|---|---|---|
| `gmf.clj:1245` | `not ported` | MedicationOrder `:reason`'s three-way resolution |
| `gmf.clj:1247` | `UNDECLARED` | the CarePlan `assign_to_attribute` / `referenced_by_attribute` pair |
| `gmf.clj:1326` | `DELIBERATELY UNDECLARED` | VitalSign `expression` |

**Two premise mismatches against the prompt's channel anchors**, both
fix-forward with disclosure per
`rulings.md#R-stop-only-on-two-defensible-readings` (mechanical
conflicts, one defensible reading each):

- `projects/ehrt-cli/deps.edn` carries FOUR references (`:57`, `:99`,
  `:121`, `:139`) the prompt's deps census omitted entirely;
  `projects/integration` is at `:74`, not `:72`.
- `.agents/reading-sets.edn` names NO path inside the brick. The `:sim`
  set reads `docs/dev/simulator-architecture.md` and
  `docs/dev/components.md`, not the brick's own docs. No reading-set
  path edit was owed — only a re-measure. (Line counts did not move
  either: every doc edit was in-place word replacement, 5/5, 11/11,
  10/10 insertions/deletions.)

A third item the prompt did not anticipate: `components/*/resources` is
gated to nest under exactly one directory NAMED FOR ITS OWN BRICK
(`ehrt.docs-tooling.resource-nesting-test`, ADR-0052 AR-F3-2). So
`resources/sim-trajectory/` had to move too, carrying the three
`io/resource` calls in `gmf_interpreter.clj`.

### The rename (c44d240)

`git mv` of the brick, both `ehrt/sim_trajectory` package directories,
and the resources directory; then a line-split-preserving pass and a
plain sweep. Five occurrences were split across a line break
(`components/sim-` + newline + `trajectory/...`); the break MOVES WITH
THE NAME (`patient-` + newline + `simulator`), so every line count and
wrap column is preserved exactly.

Landed: 118 files, 693 insertions, 661 deletions — 42 renamed (6
unmodified, 36 rewritten) + 75 consumers + `state-derived.md`. Matches
the Step-0 census exactly.

**`bin/regression-oracle` had to change, and the bracket could not have
run otherwise.** `run_one` hard-coded `poly/sim-trajectory {:local/root
"$wt/components/sim-trajectory"}` and the matching `:oracle-run`
extra-path. A bracket runs TWO worktrees; after this rename the baseline
side carries `components/sim-trajectory` and the target side
`components/patient-simulator`, so one literal cannot serve both. Added
`sim_brick_dir_for`, resolving per worktree — the same shape, and the
same retirement condition, as the `oracle_wiring_for` fallback beside
it. `ehrt.integration.oracle-coverage-test` mirrors that deps block for
THIS checkout only, so it names the current directory outright; the
now-deliberate divergence is recorded in its own docstring.

**Proof, all four:**

| check | result |
|---|---|
| `bin/regression-oracle 2d937b9 HEAD` | ABORTED undeclared, **exit 1**, on digest.clj's require/alias change |
| same, `--declared-digest-change` | **IDENTICAL, exit 0, 35 roots each side** |
| full `make test` | 366 / 4,084 / 18,336 — **ZERO delta** |
| `make docsgen` | `demos/traces/**` regenerated **byte-identical** |

The traces result is worth keeping: it re-ran the demo exercisers end to
end and is an independent witness on a surface the oracle does not
cover. And every renamed brick test namespace migrated with IDENTICAL
test and assertion counts, name for name — `gmf-interpreter-test` 204 /
536 before and after, and so on for all fourteen.

### The charter (5a2ab20 red, b1c0965 green)

`components/patient-simulator/docs/limitations.md`: the mission
sentence, a dependency-direction paragraph (traffic CONSUMES patient
simulation as compiled pathway IR, never the reverse), and **eight
rows**, each with what / why declined / citation / trigger-if-any.
`interface.clj` gains a SCOPE section carrying the same sentence, so a
reader who never opens the docs still meets the scope statement at the
component's one public namespace.

**Eight rows, not the prompt's seven, and the naming differs — this is a
finding, not a liberty.** The prompt's "attributes" is two genuinely
distinct declines: the CarePlan attribute pair, and
MedicationOrder/CarePlanStart's `:reason`. And the prompt's "three-way
ConditionEnd" does not exist in the tree under that description: the
THREE-WAY thing is `:reason`'s resolution (attribute / PriorState /
ConditionOnset), which is `gmf.clj:1245`'s own `not ported` marker.
`ConditionEnd` has a real and separate limitation — `gmf.clj:1152`
declares `:condition-onset` and nothing else, so the attribute-reference
form loads without schema failure (the state maps are open) and simply
does not resolve — and it gets its own row on its own evidence
(`gmf-interpreter.md`, "a reference shape v1's interpreter does not
resolve", witnessed at `sinusitis.json`'s `Sinusitis_Ends`).

The care-plan row carries the author's two-part trigger: fix owed when
any emitter surface renders care-plan state — (a) a FHIR CarePlan
resource, or (b) a render-time patient-context feature reachable by
site-profile Z bindings, realistically a facility Z segment fed by
care-plan state for ADT-driven care coordination. With the order noted:
**without the fix, such a surface renders every plan ever started as
active — a plausible-looking lie, worse than absence.** The priced
option is preserved verbatim in substance (port the attribute pair at
the loader, resolve at the interpreter; predicted contract-neutral;
declared oracle change on every root drawing `bronchitis` or
`injuries`), so no future session re-derives it.

The two standing-request sentences
(`gmf-interpreter-findings.md`, `gmf-interpreter.md`) gained a dated
note pointing at the charter entry — APPENDED, nothing above rewritten.

### THE GATE WAS WRONG, AND ITS OWN RED WITNESS CAUGHT IT

This is the part of the session worth reading.

`ehrt.docs-tooling.patient-simulator-charter-test` gates three things:
the charter exists and carries the mission sentence; every citation
RESOLVES (quoted text occurs verbatim in the named file, anchored by
stable TEXT and never by line number); and every in-source limitation
marker is COVERED by a citation landing inside that marker's own `;;`
comment block. Coverage is per BLOCK, not per line, because `gmf.clj`'s
CarePlan block carries two markers about two different limitations and
each earns its own row.

Red before the charter: **7 failures, 0 errors, 11 passes**, every
obligation named legibly.

Then the prompt's instructed plant — `;; :some-invented-field is
DELIBERATELY UNDECLARED here.` into the real `gmf.clj`. **The lint
stayed GREEN.** The charter's VitalSign citation was the bare string
`"DELIBERATELY UNDECLARED"`, and the planted line CONTAINED it, so the
new marker cited itself covered. A snippet that matches wherever it is
pasted anchors nothing, and the drift lint would have blessed every
future marker that happened to quote its own citation.

Two fixes: a new `every-charter-citation-anchors-exactly-one-place-test`
(a citation occurring twice in its own file is red), and the offending
citation sharpened to a unique single-line form. The new test then
immediately found a SECOND weak anchor already sitting in the table —
`"is deliberately NOT included"` occurs twice in `check.clj` (`:434` for
`:medication-end`, `:448` for `:care-plan-end`) — which was re-pointed
at unique text.

Re-planted after the fix: **caught, 1 failure, 0 errors**, naming
`gmf.clj:1342`. Plant withdrawn; `gmf.clj` is byte-identical to
c44d240.

The lesson generalises past this gate: **a red witness that confirms
what you expected has told you less than one that fails.** Had the plant
gone red the first time, the weak anchor would have shipped.

### CI red at c44d240, and why a local run could not have caught it

Run 32475479703 failed on `hand-owned-asset-freshness-test`:
`docs/dev/simulator-architecture.md` changed, so
`docs/manual/assets/gt-emitters.svg` needed re-review.

Verdict: **not stale.** Section 4's equation block is byte-identical
across the diff — `walk : RNG x Persona x Closure -> Trajectory` and its
four siblings untouched. The one section-4 line the rename touches is
the prose sentence naming WHICH BRICK supplies `walk`, a name and not a
signature. And the asset carries no brick name at all: its complete text
is `GT`, `ground-truth log`, `ER7*`, `HL7v2 messages`, `FHIR*`, `FHIR
bundles`, `emitH`, `emitF`. `:reviewed-at` bumped to `c44d240d` with
that reasoning; the prior ADR-0158 review is kept beside it, not
overwritten. The second asset citing the same source, `two-clocks.svg`,
is already `:verdict :stale` with a `:stale-row`, so the tripwire
correctly exempted it — which is why exactly one fired.

**Why local `make test` was green.** That gate reads GIT HISTORY: when
did the source file last change. At Step 1 the suite ran on an
UNCOMMITTED tree, so `git log` still returned the OLD sha and the gate
was blind BY CONSTRUCTION to the very change it exists to catch. A
pre-commit run cannot exercise it, ever.

This is a near-sibling of watch-list row **W-1** (born-red gates landing
unexecuted) but a distinct class, and review 5 should hold them apart:
W-1 is a TIER being skipped, so running the tier fixes it. This gate
runs in the right tier, on the right machine, and still cannot see.
The remedy is ORDERING — run history-reading gates AFTER the commit —
and this session adopted it: the Step-3 gates were re-run post-commit,
green, before the push.

### The row re-scope

`#careplan-guard-resolution`: PRIORITY 4 -> the bottom of `## Next`,
re-bodied as a DECLARED LIMITATION whose authority is the charter, with
the two-part trigger and the priced option, and ADR-0139 C-2's Guard
half named as absorbed. Before: a queued defect with census S-2 folded
in. After: six lines, pointing at the table.

**The slug is KEPT**, deliberately. It is cited from eight surfaces —
`notes/adr/0147`, `0150`, `0159` (twice), `0144`,
`.agents/plans/2026-08-16-event-log-census.md`,
`.agents/plans/2026-08-18-repo-review-findings.md`, the 2026-08 attic,
two prompts and a session record — all of which R-RP forbids editing,
and `roadmap-lint`'s `every-cited-slug-resolves-test` would break on
them. Re-slugging trades a cosmetic gain for real orphaning.

**One row was ADDED, disclosed**:
`#stale-path-retired-namespace-addendum`. Every prior namespace
retirement in `stale_path_test`'s family joined its denylist in the same
commit (the S2/S3/M2/M3/M4 addenda, each recorded in that file's own
docstring). `ehrt.sim-trajectory.` did not, because a new gate owes its
own red and this session's fence allowed none. No live surface carries
the old form today; the row buys the gate that stops it coming BACK.
Registered rather than built — which is precisely the discipline
ADR-0139 C-2, this session's own subject, exists to teach. No other row
moved.

### Suite reconciliation

| step | blocks | tests | assertions | delta |
|---|---|---|---|---|
| Step 0 baseline | 366 | 4,084 | 18,336 | — |
| Step 1, the rename | 366 | 4,084 | 18,336 | **zero** |
| Steps 2-3 | 368 | 4,100 | 18,378 | +2 / +16 / +42 |

The +42 needed reconciling, because the predicted figure was +38:

- **+2 blocks, +16 tests, +38 assertions** — the new charter namespace,
  which runs TWICE (development and `projects/conformance` both compose
  `docs-tooling`): 8 x 2 and 19 x 2.
- **+4 assertions** — `invocation-lint-test`, 269 -> 271 per pass. It
  scans every `.md` under `components/*/docs/**` and exactly two of its
  `deftest`s iterate that set, so one new doc is +2 per pass. Welcome
  rather than incidental: it means the charter sits inside the EXISTING
  docs-gate population by construction, not only under its own test.

### Consequences

The component is named for what it is. The scope statement has one home
and a gate that keeps the table honest against the source. A future
session adding a deliberate limitation to this brick's `src` either
tables it or goes red, and a future session tempted to re-derive the
care-plan fix finds it priced.

What this deliberately did NOT do, per the author's ruling: no attribute
port, no Guard work, no emitter change, no care-plan test, no
event-schema change. The possibility is named; nothing was spent on it.
