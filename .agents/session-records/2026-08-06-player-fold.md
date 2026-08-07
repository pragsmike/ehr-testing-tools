# 2026-08-06 — player fold

## Scope

Opens the player arc (bed-board slice). Asked to make
`ehrt.sim-emit-hl7.v2-replay/fold-message` total over the emitter's
real trigger set — A17 (bed-swap) and A40 (merge), genuinely
two-participant messages the fold previously rejected with
`:unsupported-trigger` by documented scope boundary — and to make its
own time reading self-anchoring (an absolute epoch instant read from
the wire, honoring an explicit offset when present, no reference-date
parameter), so the fold no longer crashes on default `--churn` traffic
and could serve a downstream consumer with no knowledge of the run's
own reference date. Did exactly that: A17 folds each PID/PV1 pair
independently (the A02 treatment, per pair); A40's surviving entry
absorbs (a wire-side no-op) and the merged-away entry becomes a
`:status :merged` tombstone mirroring the engine's own `evolve :merge`
`:merged` arm exactly; `hl7-instant->millis` replaces
`hl7-instant->seconds`. No player, corpus, CLI, or interface changes —
`v2-replay` stays unexported (session 2's own scope). No emitter
changes. Full detail: `notes/adr/0066-player-fold.md`.

## Red→green evidence highlights

Two new unit tests (a real rendered A17 folded from empty into two
independently-updated participant entries; a real rendered A40 folded
onto a seeded two-participant accumulator, producing a survivor and a
tombstone) plus the emitter-coherence defspec's own churn profile
(widened from an exclusion set to the full
`ehrt.sim-engine.churn/sample-profile`) all errored against the pre-fix
fold with the exact `:unsupported-trigger` `ExceptionInfo` the old
scope boundary threw (`Ran 11 tests containing 49 assertions. 0
failures, 3 errors.`). Green after the fold extension landed — four
consecutive full-namespace runs (fresh `test.check` seed each time),
`0 failures, 0 errors` throughout. Full workspace suite
(`clojure -M:poly test :all skip:integration`) held green at every
checkpoint modulo the one pre-existing, unrelated failure named below;
`clojure -M:poly check`: OK throughout. Oracle bracket
(`bin/regression-oracle 03a8698 d1bf847`): all eleven vendored-root
digests identical — the fold is read-side only, confirming AR-BB1-5's
own expectation.

## Judgment calls and their ratification status

- **The tombstone shape for a merged-away entry.** The prompt named
  this a possible STOP-AND-REPORT point if the engine's own merge
  semantics left it ambiguous. It did not: `evolve :merge`'s own
  `:merged` arm touches exactly one field (`:status`), so the wire-side
  mirror is unambiguous — `:status :merged`, everything else held over.
  Recorded, not a judgment call requiring escalation. Ratified in
  `notes/adr/0066-player-fold.md` AR-BB1-2.
- **`pid-pv1-pairs`'s own walk semantics.** The prompt's own language
  ("stop pairing at any non-PID/PV1 segment") was read as license to
  abort the whole segment walk on the first non-match — which breaks
  immediately on MSH, the very first segment of every real message. A
  probe against a real rendered A17 (`clojure -M:dev`, ad hoc) caught
  this before it reached a committed red test: the walk instead SKIPS
  non-matching segments and continues, pairing PID+PV1 wherever it
  finds them, never aborting. Judgment call, not author-specified
  verbatim; the property (600+ trials across four runs, bed-swap
  included) is the ratification the prompt itself named as arbiter
  (AR-BB1-2's own "that property is the arbiter, this prompt's prose is
  not," extended in spirit to this parsing choice too).
- **A separate `segment-component` primitive, and re-scoping
  `parse-persona`/`parse-location`/`parse-attending`/`parse-class` from
  whole-message to segment-scoped.** Not named in the prompt at all —
  discovered necessary once the library's own `get-field-component`
  API was confirmed (by reading `com.nervestaple.hl7-parser.message`'s
  own source, extracted from the vendored jar) to flatten across EVERY
  same-id segment before indexing, which would silently mis-index a
  two-PID A17 message. Judgment call under the prompt's own read-first
  instruction ("confirmed against the library directly, not assumed"
  is this file's own established convention, `component`'s prior
  docstring); unratified beyond this record and the code's own
  docstrings.
- **The `absolutize` test-side time adapter's exact placement** (a
  preprocessing step on the true-state before
  `project-to-wire-visible-fields`, rather than a parameter to that
  function). Matches AR-BB1-4's own explicit instruction
  ("`project-to-wire-visible-fields` itself does not change shape")
  directly — not really a discretionary call, named here for
  completeness.

## Findings and HEAD landed

- **Premise mismatch at Step 0** (disclosed before any code changed,
  per this repo's own fix-forward-with-disclosure discipline): the
  prompt's own preflight named `config/busy-weekday.md` as the standing
  untracked fixture; the live tree instead carried two DIFFERENT
  untracked scratch files (`config/busy-tuesday.edn`,
  `config/busy-weekday.edn`), breaking
  `merge-config-file-suggests-a-same-stem-sibling-file`. Flagged to the
  author, who relocated both files themselves. `config/busy-weekday.md`
  itself does not exist on disk — a separate, pre-existing, unrelated
  gap (its own `:did-you-mean` assertion is the ONE failure that held
  constant, unrelated to this session's fenced scope, through every
  checkpoint) — disclosed in `notes/adr/0066-player-fold.md` AR-BB1-5,
  left unfixed.
- **`components/sim/docs/sim-theory.md`'s own emitter-coherence-law
  paragraph** stated the old exclusion live; reconciled in the Step 2
  commit (the propagation grep AR-BB1-3 required). A second hit,
  `notes/sim/agents/plans/roadmap.md`, is the frozen pre-split roadmap
  (historical record) and was correctly left untouched.
- **Module vendoring's `[unverified]` intake status (ADR-0064) is
  discharged** — ratified per AR-BB1-R, recorded in
  `notes/adr/0066-player-fold.md`, one line added to
  `.agents/plans/roadmap.md`'s own Next section; still not built,
  scheduled after this arc.
- HEAD landed: this session's own closing commit (docs-only: this
  record, the prompt archive, `notes/ADRs.md`'s index line,
  `notes/adr/0066-player-fold.md`, both READMEs, and the roadmap's own
  Done pointer + ratification note), on top of `d1bf847` (Step 2, the
  fold extension) and `f0f8148` (Step 1, the red tests).
