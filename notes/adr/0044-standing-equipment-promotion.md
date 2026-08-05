<!-- Attic file: notes/adr/0044-standing-equipment-promotion.md -->
<!-- Moved verbatim from notes/ADRs.md by scaffolding compaction B (2026-08-05, notes/ADRs.md ADR-0046). notes/ADRs.md remains the citation index -- see there. -->

## ADR-0044 — Standing-equipment promotion: census enters `sim-trajectory`, the oracle digest becomes a component, J2 closes structurally

**Status:** Accepted (author-ruled 2026-08-05, design channel, AR-P-1
through AR-P-5 below; recorded verbatim, attributed, per this
document's own ADR-0007 provenance-tag convention — every ruling below
is `[A]`). Executed same day.

### Context

Two prior arcs left standing equipment outside the tested tree: sim
split B (ADR-0043, complete) and the 2026-08-05 docs coherence pass
(ADR-0043's own tail) both named the census tool and the regression
oracle's digest as cleanup-arc seeds still sitting in `development/src`
and `bin/oracle-src` respectively. `ehrt.sim-trajectory.census`
(`development/src`, ADR-0034) is a `sim-trajectory` dev entry point
requiring that component's own INTERNAL namespaces (`gmf`,
`gmf-interpreter`) — legal only because `development/src` sits outside
Polylith's own foreign-component boundary, not because the reach is
sound; its 7 co-landing tests (`development/test`) were never wired
into any real project's own test alias, so `clojure -M:poly test` never
ran them (confirmed directly this session: a full-suite baseline run
before any edit produced 202 `Test results:` blocks with zero census
assertions among them). `bin/regression-oracle`'s own digest producer
(`bin/oracle-src/ehrt/oracle/digest.clj`) carries ADR-0030's own J2
design — always read from the CURRENT checkout, never from either
worktree under test — a deferred limitation (roadmap, "the oracle
harness's own standing limitation") that has already forced two live
workarounds when a producer function's own call shape changed mid-span
(ADR-0033 AR-4b's own hand-run six-root table; ADR-0043 M2's own
split-mode bracket, `978c54f` digested by its own worktree, this
stage's tip by its own). A fresh call-position census of every
`digest.clj` dependency, taken at this session's own Step 0
(tip `f9830ec`), found every var except `ehrt.sim-trajectory.gmf-
interpreter/dob-epoch-day` already on `ehrt.sim-trajectory.interface`
— the two apparent internals reaches this ADR's own driving prompt
flagged as needing checking (`urinary-tract-infections-engine`,
`emit-hl7/test`) turned out to be comment-text artifacts, not real
calls.

### Decision

Ruled 2026-08-05, design channel, recorded verbatim:

**AR-P-1 (census home).** Census moves INTO `components/sim-trajectory`
as `ehrt.sim_trajectory/census.clj` (ns `ehrt.sim-trajectory.census`);
its test moves into the component's test tree. Internals access becomes
intra-component (legal); the interface does NOT grow for census — it is
equipment, not API; invocation is via `-m ehrt.sim-trajectory.census`
or the existing launcher, repointed. Census's deps enter
`sim-trajectory`'s deps.edn as needed, evidenced.

**AR-P-2 (digest home).** Digest becomes `components/oracle`
(`ehrt.oracle.digest` + `ehrt.oracle.interface` exposing the entry
point(s) the script calls). Its requires repoint to interfaces:
`ehrt.sim-trajectory.interface` (adding `dob-epoch-day` to that
interface iff the fresh call-site census confirms the need — one var,
caller-evidenced, recorded), `ehrt.sim-engine.interface`,
`ehrt.sim-emit-hl7.interface`, `ehrt.sim-model.interface`. The
interface-repoint is this session's sanctioned improvement; digest
LOGIC moves verbatim (the byte-comparison in Step 5 is the proof).
`bin/oracle-src/` retires with a dated pointer in the script's header.

**AR-P-3 (script redesign — J2 closes).** `bin/regression-oracle`
changes: per-worktree classpath now includes that worktree's own
`components/oracle` (each side runs its own digest); a cross-side
digest-source equivalence check runs FIRST — identical or ns/require-
only diff proceeds; anything else requires an explicit
`--declared-digest-change` flag (recorded in the manifest header) or
aborts. The J2 deferred row closes with a dated note citing this
session; ADR-0030's own J2 precedent gets a dated amendment, fix-
forward style. Brackets spanning PRE-promotion refs: the script detects
a side lacking `components/oracle` and falls back to that side's own
`bin/oracle-src` if present (one transitional branch, commented with
its own retirement condition: remove when no bracket needs a
pre-promotion baseline).

**AR-P-4 (no census improvements).** The deferred census refinements —
substance qualifier, per-module seeds, the same-day filename overwrite
bug — stay Deferred with their triggers intact. Promotion is relocation
plus test-exercise, nothing else; if the overwrite bug's fix tempts
during the move, it is a FINDING.

**AR-P-5 (this session's own bracket — declared transitional split).**
The bracket spanning this session runs old-mechanism@pre
(`bin/oracle-src` at `f9830ec`) vs new-mechanism@post (the component),
manifests compared; soundness condition: digest's cross-side diff is
ns/require/interface-repoint lines only, asserted and recorded. All
ELEVEN batches byte-identical, expected-change set NONE. This is the
last split-mode bracket; this ADR says so.

### Execution note

**Step 0 (characterize).** Tip confirmed `f9830ec`. Fresh call-position
census of digest.clj's every dependency var found exactly one gap
(`dob-epoch-day`, evidenced, added per AR-P-2's iff-clause) — every
other var digest.clj calls was already on the relevant interface.
Census's own launcher: `clojure -M:dev -m ehrt.sim-trajectory.census
<checkout> <out-dir>`, unchanged by the promotion (the `:dev` alias
already wires `poly/sim-trajectory` as a `:local/root` dep).

**Step 1 (`a17fab1`, AR-P-1).** Census and its test moved verbatim into
`components/sim-trajectory`; `deps.edn`'s `:test` alias drops the now-
empty `development/test` entry (`components/sim-trajectory/test` was
already listed). **Real finding, disclosed, fixed forward:** running
these 7 tests under `poly test` for the FIRST TIME EVER surfaced two
stale fixtures — GMF coverage Wave VS (ADR-0039, 2026-08-04) had landed
real support for the `VitalSign` state type and the `:vital-sign`
condition type three sessions before this test file was ever actually
exercised, so `load-failed-json`/`walk-failed-json` had silently
stopped exercising their own `:load-failed`/`:walk-failed` verdicts.
Both fixtures swapped to deliberately FICTIONAL type names
(`NoSuchStateType` / `"No Such Condition Type"`) rather than another
real-but-currently-deferred type, since `gmf.clj`'s own closed
whitelists make a fictional name permanently unrecognized, immune to
the next coverage wave going stale the same way this one did. No
`census.clj` behavior change. **Citation gap, disclosed:** this ADR's
own driving prompt attributed the poly-test invisibility to "the
roadmap's own Wave I finding" — no such row was found in
`.agents/plans/roadmap.md` under that name (searched directly, this
session); the underlying claim itself was independently confirmed by a
live before/after `poly test` run (202 blocks before this promotion, 0
census assertions among them; 204 after), so the finding stands on its
own evidence, the citation is corrected here rather than repeated.

**Step 2 (`c065cdd`, AR-P-2).** `components/oracle` created;
`ehrt.oracle.digest` moved with the four interface repoints named
above; `ehrt.sim-trajectory.interface` gains `dob-epoch-day`.
`run-walk`'s own 6-arg interpreter call becomes an 8-arg interface call
with the same `{}`/`{}` defaults the impl's own 6-arg arity was already
filling in internally (verified against `gmf_interpreter.clj`'s own
arity chain) — spelled out because the interface does not carry that
shorthand, not a behavior change. `bin/oracle-src` deleted; a dated
pointer lands in `bin/regression-oracle`'s own header (the script's
mechanical redesign is Step 3, not this commit). Verified before
committing: the promoted digest, run standalone against the current
tip, is byte-identical (sha256, all 11 roots) to `bin/oracle-src`'s own
pre-promotion digest run in a disposable worktree at `f9830ec`.
`components/oracle` documented in `AGENTS.md` and
`docs/dev/architecture.md` (bricks table + mermaid diagram) — the
structure-currency gate caught its own absence, red then green, live.
`poly/oracle` added to the root `deps.edn`'s `:dev` alias (composed
into no shipped project — dev/build equipment only).

**Step 3 (`3da479e`, AR-P-3).** `bin/regression-oracle` redesigned: per-
worktree resolution (`oracle_wiring_for`), the ns-form-external
soundness check, the `--declared-digest-change` flag, and the
transitional fallback to a worktree's own `bin/oracle-src` when
`components/oracle` is absent there. Red→green proven live, four ways:
same-ref bracket at `f9830ec` via the fallback branch (IDENTICAL, all
11 roots); same-ref bracket at `c065cdd` via the `components/oracle`
branch (IDENTICAL, all 11 roots, SAME digests as the fallback run —
confirming both branches produce identical content); mixed-side bracket
(`f9830ec` vs `c065cdd`) WITHOUT the flag (aborts, exit 1, the
ns-form-external diff printed); the SAME mixed-side bracket WITH
`--declared-digest-change` (proceeds, IDENTICAL across all 11 roots) —
in substance AR-P-5's own bracket, re-run and recorded formally below.

**Step 5 (verification).** AR-P-5's own declared transitional split
bracket: `bin/regression-oracle f9830ec 3da479e --declared-digest-change`.
Soundness check reported DIFFERS outside the `(ns ...)` form (expected —
the interface-repoint touches every call site, not only the `ns` form);
the printed diff is exactly the AR-P-2 repoints (four require-target
renames, the `run-walk` 6→8-arg expansion, and the mechanical
`gmf/`→`sim-trajectory/` call-site renames across ten producer
functions) — no other content. All ELEVEN batches byte-identical,
expected-change set NONE, exactly as AR-P-5 ruled. `poly check` clean
and the full suite green (204 `Test results:` blocks, 0 failures/0
errors, both lanes) at every one of the three code commits above.

### Dated amendment to ADR-0030 J2

See `notes/ADRs.md` ADR-0030's own J2 entry, amended in place, dated
2026-08-05: J2's "digest.clj is always read from THIS checkout, never
from either worktree" design is CLOSED by this ADR's AR-P-2/AR-P-3 —
cross-referenced there rather than restated.

### Roadmap

The J2 deferred row (`.agents/plans/roadmap.md`) closes, dated, citing
this ADR. The "Census tool refinements" deferred row's own three
triggers (substance qualifier, per-module seed override, filename
collision) stand, untouched, re-cited not re-opened (AR-P-4).

### Fence

No census behavior changes (AR-P-4 — the filename-overwrite bug
survives this session on purpose). No digest logic changes — interface
repoints are the entire licensed diff; Step 5's soundness-check diff
(printed, inspected) is the proof, not merely an assertion. No new
tests beyond the 7 that moved (digest.clj carries none of its own; the
gap is disclosed here, not built — trigger: a future session that adds
real unit coverage for digest.clj's own pure helpers). No oracle-script
features beyond AR-P-3's own list. Deferred rows re-cited, not
re-opened. Frozen archives untouched.

---

