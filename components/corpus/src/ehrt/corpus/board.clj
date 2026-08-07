(ns ehrt.corpus.board
  "The bed board (ADR-0014's own deferred surface, player board session
  2, `notes/ADRs.md` ADR-0067): folds a paced HL7 v2 stream into the
  SAME accumulator `ehrt.sim-emit-hl7.v2-replay/fold-message` already
  builds for the emitter-coherence property, and renders a state
  snapshot from it. `corpus` is this interface's first real external
  caller (AR-BB2-1) -- `fold-event` below is the call site the AR-6
  grep discipline now names.

  `fold-event` turns a genuinely foreign trigger (outside the
  emitter's own handled set -- a real feed's A08, A05, ...) into a
  counted, cued skip rather than a crash: the fold itself stays STRICT
  (`ehrt.sim-emit-hl7.v2-replay`'s own scope boundary, unchanged here),
  it is this caller that decides a foreign message is a display-layer
  event, not a fatal one (the pacer's own cue rule, ADR-0014).

  `render-snapshot` is pure: acc x instant-ms -> string, no clock, no
  IO -- the executor (bases/cli's board sink) supplies both."
  (:require [ehrt.sim-emit-hl7.interface :as sim-emit-hl7]))

(defn fold-event
  "acc x message -> {:acc acc' :unfolded? bool}. Wraps
  ehrt.sim-emit-hl7.interface/fold-message: a message whose own trigger
  is outside the emitter's handled set throws there (documented scope
  boundary) -- caught here and reported as an unfolded skip (acc
  returned UNCHANGED) rather than propagated. Any other exception
  (a genuinely malformed message) is not this fn's concern and
  propagates unchanged."
  [_acc _message]
  (throw (ex-info "ehrt.corpus.board/fold-event: not yet implemented (player board, AR-BB2-3)" {})))

(defn render-snapshot
  "acc (the fold-event accumulator) x instant-ms (the snapshot's own
  absolute epoch millis) -> a rendered whiteboard string: a header
  naming the snapshot instant (ISO-8601, UTC), occupied beds grouped
  by ward (wards sorted, beds sorted within), one line per patient,
  then a one-line tally (inpatients / active outpatients / discharged
  / merged). Operator voice throughout -- no citation, milestone, or
  internal-namespace token anywhere in the rendered text."
  [_acc _instant-ms]
  (throw (ex-info "ehrt.corpus.board/render-snapshot: not yet implemented (player board, AR-BB2-4)" {})))
