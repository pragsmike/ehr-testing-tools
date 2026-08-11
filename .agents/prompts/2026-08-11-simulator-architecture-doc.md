# 2026-08-11 — ehr-testing-tools: simulator architecture doc, purity lint (ADR-0108)

## Context

Archived 2026-08-11. Repo: `~/src/ehr-testing-tools` (ext4 clone).
Session opened at HEAD `5a2832f` (ADR-0107's own close) and closed at
this record's own close-phase commit. Original prompt follows
verbatim; a deviation record follows that.

## Original prompt (verbatim)

# Session prompt -- simulator architecture doc, purity lint (ADR-0108)

## Context

You are Claude Code executing under R30 ceremony in the ehr-testing-tools
workspace. This session executes the author's 2026-08-11 ruling ("Good
sequence", ratifying the design channel's proposal): a dev-docs
architecture document for the simulator, made load-bearing by a
co-landed purity lint, wired into the agent reading path -- an aid to
understanding AND a guardrail against feature work drifting from the
established theory. The guide-side treatment is the AUTHOR's own
future authorship (derives from this doc; not this session); the
tool-specific user guide stays DEFERRED under a named trigger this
session records. HEAD at handoff: 5a2832f. This session's ADR is
ADR-0108.

Channel-probed architecture facts the doc documents (verify each
against the tree; the doc cites file/ADR, never this prompt):
- Seven sim-family bricks: sim-model (pure schemas), sim-trajectory
  (load-closure / compile-trajectory / run-module -- the GMF walk),
  sim-engine (run, the decide/evolve pair per sim/ADR-0008, replay,
  churn, order-profiles), sim-emit-hl7 and sim-emit-fhir (emit,
  fold-message), sim-check (invariants), sim (mount: result
  envelopes, commands, manifest).
- The doctrine, from engine.clj's own ns docstring: decide
  (rng, t, world, patient-id, step) -> {:events ...} reads the world
  and never returns new state (cross-patient coupling lives here);
  evolve (patient-state, event) -> patient-state' is pure and total;
  patient state EXISTS ONLY as the fold of evolve over events;
  replay is that fold re-run -- the basis of sim-check and the
  oracle regime.
- Mutable-state census (re-run the grep, cite the result): zero
  atoms/refs/agents/volatiles in the simulation path; the known
  exceptions are census.clj's probe-fetch memoization atoms and
  version.clj's git read. java.util.Random is the one impurity:
  seeded, explicitly threaded, fixed-consumption per the RNG-path
  law (.agents/rulings.md).
- The palgebra section uses the diagrammatic-composition operator
  (U+2A1F) -- NEVER the infix ring compose -- and resource-tensor
  notation for RNG threading, per the author's own convention:
    walk    : RNG (x) Persona (x) Closure -> Trajectory
    engine  : RNG (x) Config -> GT     (per-step decide then
                                        evolve-fold over World)
    emitH   : GT (x) Params -> ER7*     emitF : GT (x) Params -> FHIR*
    replay  : GT -> (State x State)*    check = replay ; invariants
    board   = split ; fold-message*
  (render with the real Unicode operators in the doc). Two honest
  wrinkles stated, not hidden: run is ONE fold over a shared World
  (draw the merge node as the fold it is, not parallel wires); the
  interpreter walk is an unfold meeting evolve's fold. The
  emitH/emitF naturality out of one GT object cites the passing
  named property test as its witness (find and cite it by name).
- The latency design pass (the roadmap's own next row) will add a
  second clock: an arrow GT -> TimedWire between engine and the
  emitters. The doc names that extension point in one sentence,
  building nothing.

## Read first

- components/sim-engine/src/ehrt/sim_engine/engine.clj -- the ns
  docstring and decide/evolve, verbatim
- docs/patient-state-model.md (or wherever engine.clj's own docstring
  points -- follow its citation)
- notes/adr/ -- sim/ADR-0008 via notes/sim/ADRs.md, ADR-0105/0107
  (the interpreter's recent contracts the doc must not contradict)
- docs/dev/source-sink-design.md -- the sibling doc whose register
  style this one mirrors
- components/docs-tooling/test/ehrt/docs_tooling/ -- lint homes and
  the stale-path/link-gate patterns the new lint mirrors
- .agents/reading-sets.edn and AGENTS.md -- the wiring targets and
  the budget mechanism (a re-baseline may be needed; the ADR-0107
  close's own re-baseline is the precedent)
- .agents/rulings.md -- the RNG-path law's exact wording, tag law,
  ASCII verification

## Author rulings, verbatim

- [A] 2026-08-11, author verbatim (the chartering ruling): "I want to
  document this architecture in the tools repo, as that's where the
  implementation is. This is more of an aid to understanding the
  design, as well as a guide for agents to avoid departing too much
  from the established theory when adding features. We might include
  a treatment in the guide as well." And ratified: "Good sequence."
- [A] The user-guide deferral, author verbatim, recorded in the
  roadmap with the channel's named trigger: "I've been deferring
  creating the tool-specific user guide in tools repo (distinct from
  EHR Testing Guide, which is more generic) until things settled
  down and the tools were able to produce the realistic traffic I
  need. That remains to be seen, but it's getting more likely to
  verifiably happen soon." Trigger (channel-proposed, un-vetoed):
  the latency-realism arc landed PLUS one witnessed end-to-end demo
  of latency-realistic traffic played into a downstream-receiver
  stand-in.
- [C] The doc is dev-docs (docs/dev/simulator-architecture.md), not
  user path -- R34 governs. The guide treatment derives from this
  doc later, in the author's own queue; nothing guide-side this
  session.

## Steps

1. **Tag ceremony (tag law case i -- licensed here).** The design
   channel has verified the ADR-0107 landing at `5a2832f` by fresh
   public clone. Tag `stable-20260811-injuries-arc-close` at
   `5a2832f`, push, verify the peeled ref. Remote moved =
   STOP-AND-REPORT.

2. **The doc.** docs/dev/simulator-architecture.md per Context:
   component inventory with interface citations, the decide/evolve
   doctrine with its ADR anchors, the state-isolation claim with the
   census result and the named exceptions, the palgebra section with
   the real operators, the two wrinkles, the naturality witness
   cited by test name, the latency extension point in one sentence.
   Every architectural claim carries a file or ADR citation --
   claims the lint can't check must still be checkable by a reader.

3. **The purity lint, red proven.** A docs-tooling test asserting
   zero mutable-state primitives (atom/ref/agent/volatile!/
   set-validator, as forms -- not in comments or strings; reuse the
   form-scanning pattern the parse-guard lint established if it
   fits) across the seven sim-family bricks' src, with an inline
   allowlist naming census.clj and version.clj AND the reason each
   is allowed, mirroring how the doc states them. Non-vacuity: plant
   a temporary atom in an unallowlisted sim src file, paste the red
   verbatim, remove it. The lint's docstring points at the doc; the
   doc's state section points at the lint -- the pair is the
   guardrail.

4. **Wiring.** AGENTS.md gains the doc pointer in its
   sim-work-relevant section; .agents/reading-sets.edn adds it where
   the set's purpose fits, re-baselining the budget if tripped
   (disclosed, ADR-0107 precedent). The design channel adopts (and
   the ADR records as standing channel practice): any session prompt
   fencing sim-family src carries this doc in Read-first.

5. **One commit** for doc + lint + wiring.
   Commit message (ASCII only):
   `docs: simulator architecture doc, purity lint co-landed (ADR-0108)`

6. **Oracle bracket.** Pure identity on all 35 roots expected -- the
   footprint is a doc, a test file, and register wiring; no src
   change anywhere. Movement = STOP-AND-REPORT.

7. **Full gate.** poly check, full local suite, CLI parse-guard
   lint, bin/verify-nist-lock.

8. **Close phase.** FIRST: self-archive this prompt. Then: ADR-0108
   (the census evidence, the lint's red witness, the wiring,
   deviations dated); roadmap: the ratified sequence recorded
   (architecture doc landed -> latency design pass next -> guide
   treatment in the author's queue -> user guide deferred under the
   named trigger, trigger text verbatim); .agents/rulings.md records
   the 2026-08-11 chartering ruling verbatim; notes/ADRs.md index
   row; notes/adr/README.md count 105 -> 106; session record.
   Commit message (ASCII only):
   `docs: session record and prompt archive -- architecture doc (ADR-0108)`

9. **Push and verify.** Push per R30 checkpoints. Post-push, ASCII
   check FIRST on every commit message, then CI confirmation.

## Fences

- Touch ONLY: docs/dev/simulator-architecture.md (new),
  components/docs-tooling/test/ (the lint), AGENTS.md (pointer),
  .agents/reading-sets.edn, notes/adr/0108-*.md, notes/ADRs.md,
  notes/adr/README.md, .agents/* close-phase files. The sweep RULE
  governs over this list (ADR-0099 precedent).
- ZERO src changes in any sim-family brick -- the doc DESCRIBES; if
  describing accurately would require fixing something first,
  STOP-AND-REPORT the discrepancy instead.
- Nothing guide-side, nothing user-path, no user-guide scaffolding.
- No history rewrites; deviations dated; STOP-AND-REPORT over
  improvisation.
- Channel claims (the census, the doctrine wording, the equations)
  are verify-then-act -- the doc's citations come from your own
  reads.

## Deviations, disclosed (recorded at this session's own close)

None substantive — every step executed as the driving prompt
specified, and the tag/doc/lint/wiring content was verified
against the live tree at every claim (the RNG-path law's exact
citation, the `⨟`/`×` operators' precedent in `docs/dev/notation.md`,
the naturality witness's real test name and trial count) rather than
taken from the prompt's own paraphrase. Two disclosed judgment calls,
neither a departure from the prompt's own instructions:

- **The purity lint scans whole files, not per-`defn` like the
  parse-guard lint it mirrors.** The prompt's own Context asks for
  "zero mutable-state primitives... across the seven sim-family
  bricks' src," a file-level claim, not a function-level one (unlike
  the parse-guard lint's own function-granular try-ancestry
  tracking, which exists to distinguish guarded from unguarded reads
  — purity has no such guard: any occurrence anywhere in a file is
  the violation). The reader-based walker itself (never regex) is
  reused verbatim from the parse-guard lint's own discipline, per
  the prompt's own "if it fits" instruction.
- **`ehrt.sim.version` is allowlisted even though it triggers no
  violation today** (its only impurity, `git-sha`'s `.git/HEAD`
  read, is not one of the five forms this lint polices). Listed
  anyway, per the doc's own two-exception statement and so a future
  atom/ref/agent/volatile added to that namespace does not need a
  third allowlist entry invented on the spot — the exception was
  already named, for this file, for this reason.
