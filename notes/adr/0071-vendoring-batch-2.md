## ADR-0071 — Vendoring batch 2: the chronic clinic tail — seven ailments join the mix, one deferred

**Status:** Accepted (author-directed, autonomous session per R30), 2026-08-07.

### Context

Prior: vendoring batch 1 landed and was design-channel-verified
(`notes/adr/0070-vendoring-batch-1.md`, tip `d41a278`) — five vendored,
`injuries.json` deferred whole on a real `gmf-interpreter` `max-steps`
gap. This session is the vendoring arc's third, executing batch 2 of
the curation plan ADR-0070's own Context recorded verbatim: "the
chronic clinic tail" — hypothyroidism, rheumatoid-arthritis,
osteoarthritis, osteoporosis, anemia-unknown-etiology,
attention-deficit-disorder, allergic-rhinitis, dermatitis.

Read-first: ADR-0070 in full (this session repeats its mechanics
verbatim); `components/sim/resources/sim/modules/NOTICE` (the row
format, the batch-1 section as the model); `vendored_dementia_test.clj`
and `vendored_asthma_test.clj` (the single-file and closure-bearing
test patterns); `components/oracle/src/ehrt/oracle/digest.clj`'s batch-1
dated note; `components/sim/docs/demos/README.md`.

Two lessons from ADR-0070 bound this session: (1) the census's
`:closure-file-count` counts JSON only, not lookup-table data files —
closures are enumerated FRESH from the pin checkout, never read off the
artifact's own metric; (2) the census's three-seed sample can miss
population-scale failures — the 300-patient round-trip gate is the real
filter, and every module keeps its own whole-module bail-out.

### Decision

Author rulings, recorded verbatim (this session's own driving prompt,
2026-08-07). `[A]` author-ruled, `[C]` channel-inferred.

**AR-VB2-0 `[A — tag law, case (ii); debt recorded in ADR-0070]`.**
Annotated `stable-20260807-vendoring-batch-1` at `d41a278`, message
"vendoring batch 1 landed, design-channel-verified 2026-08-07
(ADR-0070)"; pushed; peeled ref verified — resolves exactly to
`d41a278`.

**AR-VB2-R `[A for the need, design channel 2026-08-07; C for the
shape]` (the scenarios home).** `components/sim/docs/scenarios/`
landed, sibling of `demos/`: a scenario is a RUNNABLE configuration
(`config.edn` + `README.md`, fence-gated, no captured trace — scenario
output is population-scale). First scenario, `busy-tuesday/`, landed
verbatim per this session's own prompt appendix, then live-probed: the
README's own generate command (`bin/ehrt corpus generate sim --seed
20260807 --patients 200 --config .../busy-tuesday/config.edn --out-dir
out/scenarios/busy-tuesday`) ran to completion (68 messages), and
`bin/ehrt play out/scenarios/busy-tuesday --board 60 --rate 100000`
rendered 68 bed-state snapshots with real content from the module mix
(outpatient-only traffic; `inpatients: 0` throughout, `active
outpatients` climbing to 48 across the ten-year horizon) — witnessed,
full output in the session record. `demos/README.md` gained one
cross-reference line.

**AR-VB2-1 `[C — batch-1 mechanics, repeated]` (the vendoring).**
Executed for seven of eight candidates; the eighth
(`anemia___unknown_etiology.json`) was assessed and DEFERRED WHOLE
(below). Full closure enumeration, byte-verbatim copy, SHA-256, and
NOTICE rows — see Execution record.

**AR-VB2-2 `[C — the round-trip gate per ADR-0070 pattern]` (the
tests).** One `vendored_<module>_test.clj` per landed module, red
(missing classpath resource) then green (real compiled clinical
content AND real rendered HL7). See Execution record.

**AR-VB2-3 `[C — oracle mechanics per ADR-0070 precedent]` (the
roots).** Seven new engine-layer roots joined `digest.clj`, each a
FIRST BASELINE. The existing sixteen roots verified byte-identical
across this session's entire span (both a manual pre/post digest
comparison and the official `bin/regression-oracle` bracket). See
Verification.

**AR-VB2-4 `[C — intake, not acts]` (the census-refinement intake).**
Recorded below, next-close intake only, neither acted on this session.

**AR-VB2-5 `[C — scope]` (fences).** Held exactly: no module-content
edits; no loader/interpreter/engine/emitter changes (the bail-out
precedent fired for `anemia___unknown_etiology.json`, below); no
batch-3/4 modules attempted; the scenario config landed verbatim,
never tuned; standing untracked files untouched.

### Expected-count disclosure (AR-VB2-1)

The census artifact's own `:closure-file-count`, summed naively across
all eight batch-2 candidates (2+1+3+1+2+1+2+7), is **19**. The real
composition landed is **16 distinct new files** across the seven landed
modules — a SMALLER total than the naive sum, for two disclosed
reasons, neither a repeat of batch-1's JSON-vs-CSV undercount (fresh
enumeration found ZERO lookup-table CSVs anywhere in this batch's eight
closures — the metric happened to be JSON-complete here):

| Module | Closure-file-count | Actual NEW files landed |
|---|---|---|
| hypothyroidism | 2 | **2** (root + `anemia/anemia_sub.json`) |
| rheumatoid-arthritis | 1 | 1 |
| osteoarthritis | 3 | **2** (root + `dme/wheelchair.json`; `dme/wheelchair_end.json` already vendored at this pin from batch 1's own `injuries.json` assessment, reused not re-landed) |
| osteoporosis | 1 | 1 |
| anemia-unknown-etiology | 2 | **0** (deferred whole; `anemia/anemia_sub.json` already landed via `hypothyroidism`'s own closure, no new closure member) |
| attention-deficit-disorder | 1 | 1 |
| allergic-rhinitis | 2 | **2** (root + `medications/otc_antihistamine.json`) |
| dermatitis | 7 | 7 |
| **Total** | **19** | **16** |

The gap (19 → 16) is fully accounted for: `anemia/anemia_sub.json` is a
SHARED closure member (counted once in each of two modules' own
per-module counts, landed once), `dme/wheelchair_end.json` was already
vendored, and the deferred module's own unique closure member
(`anemia___unknown_etiology.json` itself) was never landed at all.

### Execution record

**Step 0 (preflight + tag).** Cwd confirmed the ext4 clone, tip
`d41a278`, working tree clean. `clojure -M:poly check` OK; full suite
green (`clojure -M:poly test`, every project 0 failures/0 errors);
oracle pre-digest (manual, direct `ehrt.oracle.digest/-main`
invocation, all sixteen roots) recorded to a scratch manifest.
AR-VB2-0 executed directly: `stable-20260807-vendoring-batch-1` created
annotated at `d41a278`, pushed, verified — peeled ref resolves exactly.

**Step 1 (`812cc84`, AR-VB2-R).** `components/sim/docs/scenarios/`
(README + `busy-tuesday/config.edn` + `busy-tuesday/README.md`) landed;
`demos/README.md` cross-reference added; `ehrt.docs-tooling.
invocation-lint-test` (4 tests, 229 assertions) green; the generate/play
commands live-probed (above). Post-push verification: one delta, the
known trailing-newline artifact.

**Step 2 (`f1af027`, AR-VB2-1/2).** Module by module, red (missing
classpath resource) then green:

| Module | Closure | Seed/population/horizon | Result |
|---|---|---|---|
| hypothyroidism | root + anemia_sub | 20260802 / 300 / 36500d | green, first try |
| rheumatoid-arthritis | 1 file | same | green, first try |
| osteoarthritis | root + wheelchair(+reused wheelchair_end) | same | green, first try |
| osteoporosis | 1 file | same | green, first try |
| attention-deficit-disorder | 1 file | same + `:history true` | green after adding `:history true` (a straddling-encounter case, ADR-0042) |
| allergic-rhinitis | root + otc_antihistamine | 20260802 / **3000** / 36500d | green after raising population (low onset odds land in early childhood, always pre-registration at 300 patients) |
| dermatitis | root + 6 Observation submodules | 20260802 / 300 / 36500d | green, first try |
| anemia-unknown-etiology | root + anemia_sub | 20260802 / 300 / 36500d | DEFERRED WHOLE (below) |

All seven landed modules verified together (`clojure -M:dev:test`, 7
tests, 29 assertions, 0/0), then via `clojure -M:poly test` with the
new files staged (poly's own change-detection gap on untracked-only
test additions, named in ADR-0070's own Deviations, repeated here) —
every project green, 0 failures/0 errors throughout. `clojure -M:poly
check` OK. NOTICE gained sixteen new rows plus a dated section (the
seven-landed/one-deferred narrative and the full
`anemia___unknown_etiology.json` finding); every hash cross-checked by
fresh `sha256sum` against the table before commit, and again authoring
this record (56 rows total, zero problems). Post-push verification: one
delta, the known trailing-newline artifact.

**Step 3 (`dfdbdf0`, AR-VB2-3).** `digest.clj` gained seven new producer
functions (`hypothyroidism-pair`/`rheumatoid-arthritis-pair`/
`osteoarthritis-pair`/`osteoporosis-pair`/`attention-deficit-disorder-
pair`/`allergic-rhinitis-pair`/`dermatitis-pair`) and seven new `roots`
map entries — purely additive, every existing producer function and
root entry byte-unchanged (confirmed by diff before staging). Manual
pre/post comparison (Step 0's own scratch manifest vs. a fresh run
against the edited tree) showed the sixteen pre-existing roots' own
`.edn` output byte-identical. The official standing harness,
`bin/regression-oracle f1af027 dfdbdf0 --declared-digest-change`,
reported `DIFFERS` — EXPECTED, per the Wave H pre-roll precedent: the
diff shows exactly seven ADDED lines (`allergic-rhinitis.edn`/
`attention-deficit-disorder.edn`/`dermatitis.edn`/`hypothyroidism.edn`/
`osteoarthritis.edn`/`osteoporosis.edn`/`rheumatoid-arthritis.edn`) and
ZERO removed or changed lines — every one of the sixteen pre-existing
root hashes identical between baseline and target. `clojure -M:poly
check` OK. Post-push verification: one delta, the known
trailing-newline artifact.

**Step 4 (this record).** `notes/adr/0071-vendoring-batch-2.md`
authored directly; index line appended to `notes/ADRs.md`;
`notes/adr/README.md`'s own stale file count corrected (68→69, verified
by `ls`); roadmap's "Now" section updated (this session's own close,
successor-tag debt) and a dated note added to the "Census tool
refinements" Deferred row (AR-VB2-4's own intake, below) and a NEW
Deferred row added for the `:encounter-end` no-op gap
(`anemia___unknown_etiology.json`'s own finding); Done pointer
(`- 2026-08-07 — vendoring-batch-2 — ADR-0071`) added in the same
commit as the index line; session record and prompt archive land in the
same commit.

This session's own successor tag debt: `stable-20260807-vendoring-
batch-2` at this session's own closing tip is owed to the next
session's own Step 0, per tag law (ADR-0057 AR-T-1) — not created here
(no ruling licensed it at this session's own closing commit).

### `anemia___unknown_etiology.json`: assessed, DEFERRED WHOLE

Full finding recorded in `components/sim/resources/sim/modules/
NOTICE`'s own dated section, summarized here. Two SEPARATE issues, one
fixed, one a genuine bail-out:

1. **Fixed (test-configuration, not a defect):** this module's own
   `Initial` state is the first Race-gated branch this vendoring arc
   has landed. `race-condition-holds?` throws `honest-absence` (caught
   as `:walk-error`) when the persona carries no `:race` at all, and
   `sim-model/persona` only assocs `:race` when its config names
   `:race-weights` — no prior vendored-module test in this repo has
   ever needed to. Supplying `:persona-config {:race-weights [...]}`
   (the same shape `ehrt.sim-trajectory.census/default-persona-config`
   already uses) resolves it cleanly.
2. **A real, standing `gmf-interpreter` gap (the bail-out trigger):**
   with the race fix in place, the shared `anemia/anemia_sub.json`
   submodule's own `End Any Active Encounter Just In Case` state — an
   upstream Synthea idiom meaning "close the encounter IF one is open,
   else no-op" — compiles here as an UNCONDITIONAL `:encounter-end`,
   `emit-and-advance`'s own `:encounter-end` case never checking
   whether `index-of-last-open-encounter` actually found one before
   emitting. Confirmed at 300 patients across three seeds (20260802,
   1, 42): 12, 17, and 6 violations respectively of `ehrt.sim-check.
   check`'s own `:discharge-follows-admission` invariant — every seed
   tried rejected, not a seed-tunable fluke. At population size, per
   the same reasoning `injuries.json`'s own batch-1 finding
   established, hitting this is a near-certainty.

Per the AR-VB2-5 fence (no loader/interpreter/engine changes this
session), `anemia___unknown_etiology.json` is NOT vendored — no NOTICE
row, no test, no oracle root. The shared `anemia/anemia_sub.json`
submodule stays vendored (landed via `hypothyroidism`'s own closure,
confirmed clean there at 3000 patients — its own call path never
reaches the hazardous state). Revisit trigger: a future session willing
to extend `emit-and-advance`'s own `:encounter-end` case to no-op when
no encounter is open.

### Census-refinement intake (AR-VB2-4, next-close only, not acted on)

Two findings this session, adjacent to the "Census tool refinements"
Deferred row's own standing item (b) (no per-module census-seed
override):

(i) The census's own `:closure-file-count` metric counts JSON modules
only, never lookup-table CSV data files (ADR-0070's own AR-VB1-2
lesson, batch-1's `asthma.json`). This batch happened to have zero
CSVs across all eight closures, so the metric was JSON-complete here —
but nothing about the metric itself changed, and a future batch could
easily repeat batch-1's undercount. A census refinement that also
counts data files would remove the need for every future batch's own
fresh-enumeration step to double as an accuracy check.

(ii) The census's three-seed sample can miss population-scale failures
a 300-patient (or larger) round-trip run catches — `injuries.json`
(batch 1, a `max-steps` gap) and `anemia___unknown_etiology.json`
(this batch, a dangling-`:encounter-end` gap) are now TWO real,
independent findings the census's own narrow sample missed both times.
A population-scale substance/walk check (even a single larger-seed
walk per module, not full 300-patient engine round-trips) would likely
surface both classes of gap at census time, before a vendoring session
ever reaches them.

Neither acted on this session (out of AR-VB2-5's own scope — no census
tooling change licensed); recorded here as intake for whichever future
session next revisits `ehrt.sim-trajectory.census`.

### Verification

- `bin/regression-oracle f1af027 dfdbdf0 --declared-digest-change`:
  `DIFFERS`, EXPECTED — seven added roots, zero changed/removed among
  the sixteen pre-existing ones (the diff output itself is the
  evidence, not a count comparison).
- Manual pre/post digest comparison (Step 0 baseline vs. post-Step-3
  tree, direct `ehrt.oracle.digest/-main` invocation, no worktree): the
  sixteen pre-existing roots' `.edn` manifests byte-identical.
- Full suite (`clojure -M:poly test`): green at the Step 0 baseline and
  again after Step 2 (new tests staged), every project, 0/0 throughout.
- `clojure -M:poly check`: OK, every step.
- `gitleaks git --staged -v`: clean, every commit this session.
- Post-push message verification, every step: one delta each against
  the message file, the known harmless trailing-newline artifact.
- Tag verification: `stable-20260807-vendoring-batch-1` peeled ref
  resolves to `d41a278` exactly.
- NOTICE hash cross-check: all sixteen new SHA-256 values re-derived by
  fresh `sha256sum` against the vendored bytes and matched against the
  table, twice (before commit, and again authoring this record) — 56
  total rows, zero problems.
- Scenario live probe: `busy-tuesday`'s own generate command produced
  68 messages from 200 patients; `bin/ehrt play ... --board 60 --rate
  100000` rendered 68 snapshots with real module-mix content, exit
  `{:status :ok, :snapshot-count 68, :emitted 68}`.

### Deviations, disclosed

- **`anemia___unknown_etiology.json` deferred whole** — see the
  dedicated section above; the batch commit message and NOTICE both
  name it.
- **`allergic_rhinitis.json` run at 3000 patients, not this batch's own
  300-patient convention** — see Execution record and NOTICE's own
  dated entry; disclosed, not silently absorbed.
- **`attention_deficit_disorder.json` needed `:history true`** — the
  first batch-1/batch-2 module whose own content genuinely straddles
  the fixed registration boundary (ADR-0042's own mechanism, unused by
  every prior vendored root because no prior closure's content ever
  needed it).
- **Expected-file-count divergence** — see the dedicated section above;
  the naive per-module sum (19) overcounts the real distinct-new-file
  total (16) for two disclosed, fully-accounted-for reasons (a shared
  submodule, an already-vendored reuse), not a repeat of batch-1's
  JSON-vs-CSV undercount (this batch had zero CSVs).
- **`poly test`'s own change-detection gap on untracked-only test
  additions** — the same operational finding ADR-0070 already named,
  repeated here (staged before every `poly test` invocation, not
  itself fixed, out of this session's own scope).
