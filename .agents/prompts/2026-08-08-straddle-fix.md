# 2026-08-08 — ehr-testing-tools: the straddle fix (colorectal fix session)

## Context

Conventions read at HEAD `b81b847` (colorectal investigation, ADR-0085),
design channel, 2026-08-08, verified by fresh public clone (tip, tag
peel, changed-file set, ADR content all re-probed). This session
executes the ruled fix for ADR-0085's diagnosis — the straddling
encounter: `compile-trajectory`'s legacy pre-horizon drop clauses test
only an event's own `:pre-horizon` flag, so an encounter opened
pre-horizon (opening dropped) whose content and `:encounter-end` fire
post-horizon compiles orphaned clinical content and a matching-less
discharge, tripping `:clinical-content-only-when-admitted` (and, for
one patient, `:discharge-follows-admission` — same mechanism, ADR-0085).

The author ruled the arm 2026-08-08, design channel, verbatim: **"Accept
recommendation: (b) now, (a) recorded."** Shape (b): generalize the
antecedent-dropped principle to the legacy path — the straddling span is
treated as history in full. Shape (a) (synthesize a compiled opening at
the horizon boundary) is NOT built; it is recorded on the carry-across
row (AR-SF-5).

R30 ceremony, standing. Ext4 clone at its UNC path; fast-forward, record
HEAD (expect `b81b847`; later escalates unless the record chain explains
it). Commit-and-push at checkpoints; commits land green. Roadmap rows
land in the same commit as the work that changes them.

This is the first compile-layer semantics change since Wave H — the
blast-radius protocol (rulings register, fidelity arc) applies in FULL.

## Read first

1. `notes/adr/0085-colorectal-investigation.md` — the diagnosis this
   session executes: the mechanism, both patient traces, the proposed
   shapes, the tag debt.
2. `notes/adr/0082-encounterend-fix.md` — the blast-radius protocol as
   executed once before: per-root prediction, the STOP-AND-REPORT when a
   mover appeared, the trace-then-license resolution, the bracket
   matching the prediction. Also the seed-42 prose figure AR-SF-6's
   erratum corrects.
3. `components/sim-trajectory/src/ehrt/sim_trajectory/compile_trajectory.clj`
   — the legacy drop clauses (~426-435), `pre-horizon-dropped-types`
   (~271, note `:encounter-end` is already a member),
   `pre-horizon-fact-types` (~294), `history-phase?` (~336-362) and its
   docstring's "no-op for `:encounter-end` (already correctly phased by
   interpreter-level inheritance)" claim — AR-SF-4's probe target.
4. `components/oracle/src/ehrt/oracle/digest.clj` — the 28 roots; which
   are interpreter batches (raw-walk digests, compile-trajectory NOT in
   path) vs engine pairs (in path). Classify, don't assume.
5. `.agents/plans/roadmap.md` — the colorectal Deferred row (closes this
   session if acceptance passes, AR-A-5 relocation-with-notes) and the
   Carry-across emission Deferred row (~line 236, gains AR-SF-5's note).
6. `notes/adr/0071-vendoring-batch-2.md` §the multi-seed law and
   `.agents/rulings.md` — blast-radius, co-landed invariants, AR-A-5.

## Author rulings

- **AR-SF-0 [A]** (ADR-0085, "Successor tag debt"): tag
  `stable-20260808-colorectal-investigation` at `b81b847`, Step 0,
  standing ceremony — the predecessor's design-channel-verified stable
  point (verified 2026-08-08 by fresh public clone). Verify-and-disclose
  if already present; deferral is the deviation.
- **AR-SF-1 [A]** (ruled 2026-08-08, verbatim above): implement shape
  (b) only. **[C] default mechanism within the arm** (refinable by the
  session, disclosed in ADR-0086; escalate only if a choice EXCEEDS the
  arm): treat the whole straddling span as pre-horizon — when a
  `:pre-horizon` `:encounter` opening is dropped, every subsequent event
  belonging to that encounter's span, up to and including its
  `:encounter-end`, receives the EXISTING pre-horizon disposition:
  `pre-horizon-dropped-types` members drop, `pre-horizon-fact-types`
  members become registration facts. No new third disposition, no new
  event kinds. In-span membership attribution (the `:references`
  back-edge where present, the open-straddle interval otherwise) is the
  session's own design work within the arm — state the chosen rule and
  its evidence in ADR-0086.
- **AR-SF-2 [A — standing rule, fidelity arc]** (blast-radius protocol,
  in full): BEFORE any src edit, probe every oracle root's own
  seed/population for straddling spans (read-only instrumentation,
  `with-redefs`/scratch, zero working-tree disturbance), classify each
  root in-path or not-in-path, and land a per-root identical-or-moves
  prediction table. **Any predicted mover is STOP-AND-REPORT** — echo
  the table and await a license naming that mover alone. Zero predicted
  movers: disclose the table and proceed. The post-change bracket
  (Step 2) must match the prediction EXACTLY; any surprise mover or
  surprise-identical is itself a STOP-AND-REPORT, not a shrug.
- **AR-SF-3 [C]** (acceptance bar): post-fix,
  `colorectal_cancer.json` at 300 patients shows **0 violations at all
  three seeds** (20260802, 1, 42 — the multi-seed-once-flagged law's
  own set, pin verified first as before). Co-landed invariants: the fix
  commit carries its own tests — at minimum a unit test on a minimal
  synthetic straddle trajectory (pre-horizon `:encounter`, post-horizon
  in-span content + `:encounter-end`) asserting the compiled steps
  contain none of them and any fact-type in-span events landed as
  registration facts, written RED first against the unfixed tree
  (red→green evidence in the record).
- **AR-SF-4 [C]** (history-mode scope probe): before implementing,
  verify whether `history? true` mode already handles the straddle via
  interpreter-level phase inheritance (ADR-0042 AR-2), as
  `history-phase?`'s docstring claims for `:encounter-end`. If history
  mode is ALSO gapped, STOP-AND-REPORT — widening scope to a second
  mode is not this ruling's license. If it is sound, say so in ADR-0086
  with the probe evidence, and the fix stays legacy-path-only.
- **AR-SF-5 [A]** (ruled 2026-08-08: "(a) recorded"): append a dated
  note to the Carry-across emission Deferred row recording shape (a) —
  synthesize a compiled opening at the horizon boundary — as that row's
  own compile-layer half when its trigger fires, citing ADR-0085's
  proposal and this session's ADR-0086; note the straddle-detection
  machinery this fix lands is the shared prerequisite. Row stays
  deferred, trigger unchanged.
- **AR-SF-6 [C]** (erratum rider, fix-forward law): append a dated
  erratum to ADR-0082 correcting its seed-42 prose figure
  (`{:clinical-content-only-when-admitted 19, …}` — contradicted by its
  own summary table, ADR-0072, and ADR-0085's fresh measurement; two
  consistent measurements against one self-contradicting record).
  Append-don't-erase, citing ADR-0085's disclosure. The erratum also
  notes the archived colorectal-investigation prompt propagated the
  prose figure (archived prompts are frozen provenance — noted, never
  edited).
- **AR-SF-7 [C]** (suppression visibility, the R2 precedent): if it
  lands as a purely ADDITIVE key on `compile-trajectory`'s existing
  return map (`{:steps … :registration-facts …}`) with every caller
  confirmed tolerant, a `:suppressed-straddle-spans` count (spans, not
  events) is licensed as a zero-cost diagnostic, surfaced additively
  the way `:suppressed-encounter-ends` already is. Any friction —
  a caller that destructures exhaustively, a schema that closes the
  map — makes this a FINDING, recorded and not taken.

## Steps

**Step 0 — Preflight + tag (AR-SF-0).** Ext4 clone confirmed,
fast-forward, HEAD recorded (expect `b81b847`), clean tree, untracked
disclosure. `clojure -M:poly check` OK. Oracle pre-digest
`bin/regression-oracle b81b847 b81b847` — 28 roots IDENTICAL expected.
Last-five CI conclusions disclosed. Tag per AR-SF-0. No commit.

**Step 1 — Blast radius + scope probes (AR-SF-2, AR-SF-4).** Classify
the 28 roots in-path/not-in-path by reading `digest.clj`. Instrument
straddle detection read-only; count straddling spans per in-path root at
its own seed/population; land the per-root prediction table; run the
AR-SF-4 history-mode probe. Movers predicted → STOP-AND-REPORT and end
the turn awaiting license. Zero movers → disclose and continue. No
commit.

**Step 2 — The fix (AR-SF-1/3/7).** Red test first, then the fix, then
green; colorectal acceptance runs at all three seeds; full suite
(`clojure -M:poly test :all skip:integration`; the known loopback flake,
if it fires once, disambiguates by an independent second run, disclosed,
untouched); oracle bracket `bin/regression-oracle b81b847 <staged-tip>`
matching the Step 1 prediction exactly (`--declared-digest-change` only
if a licensed mover exists). `gitleaks git --staged -v` clean. Commit:

    fix: straddling encounters drop whole — the legacy gate learns the span (straddle fix, AR-SF-1/2/3)

Push; verify the pushed message; watch CI to conclusion.

**Step 3 — The record.** Author `notes/adr/0086-straddle-fix.md` (the
prediction table and its bracket confirmation, the acceptance runs, the
history-mode verdict, the attribution rule chosen, the AR-SF-7
disposition); append the AR-SF-6 erratum to ADR-0082; index line in
`notes/ADRs.md`; `notes/adr/README.md` count 83→84. Roadmap, same
commit: the colorectal Deferred row MOVES to Done with its notes intact
(AR-A-5 — relocation, never a substituted closure note) IF acceptance
passed; a Next-section intake row for the colorectal vendoring payoff
session (population-scale gate law — vendoring is NOT this session's
act); the AR-SF-5 dated note on the carry-across row. Commit:

    docs: the straddle fix recorded — colorectal's row closes, carry-across gains its compile-layer half (ADR-0086)

Push; verify; watch CI.

**Step 4 — Ceremony.** Session record + this prompt archived verbatim
(`2026-08-08-straddle-fix.md`, both homes indexed, same commit). Record
this session's own successor tag debt in ADR-0086 (the standing
pattern). Commit:

    docs: session record and prompt archive — straddle fix

## Fences (what this session does NOT do)

No vendoring — colorectal's vendoring is its own payoff session even if
acceptance is spotless. No emitter edits (`sim-emit-hl7`/`sim-emit-fhir`
untouched). No shape (a) construction beyond AR-SF-5's note. No
`gmf_interpreter.clj` edit — this is a compile-layer fix. No history-mode
change absent an explicit license (AR-SF-4). No census-tool, loopback-
flake, or pairing-as-data work. No state.md regeneration — arc-close
work.

## Close-out

Session record carries: HEAD start/end, the tag act, the full prediction
table and bracket result side by side, acceptance counts per seed,
red→green evidence for the co-landed tests, suite shape, shas, post-push
verification, CI conclusions. Echo to chat: the prediction-vs-bracket
table, acceptance counts, the history-mode verdict, the AR-SF-7
disposition, shas, CI status.
