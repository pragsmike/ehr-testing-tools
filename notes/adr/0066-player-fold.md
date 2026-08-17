## ADR-0066 — Player fold: the accumulator learns two-participant messages and absolute time

**Status:** Accepted (author-directed, autonomous session per R30), 2026-08-06.

### Context

Prior: the ux epilogue landed and was design-channel-verified
(`03a8698`, `notes/adr/0065-ux-epilogue.md`). This session opens the
player arc (bed-board slice, `.agents/plans/roadmap.md`'s first
corpus-player named future, ADR-0014's own deferral).

`ehrt.sim-emit-hl7.v2-replay/fold-message` threw `:unsupported-trigger`
on A17 (bed-swap) and A40 (merge) by documented scope boundary, while
bare `--churn`'s `sample-profile` (`ehrt.sim-engine.churn/sample-profile`)
emits both (`:bed-swap 0.03`, `:merge 0.01`) — a bed board fed default
churn traffic would crash on the first bed-swap or merge, and a board is
precisely the surface that cares about bed swaps. This session makes
the fold total over the emitter's real trigger set and self-anchoring
in time; a follow-on session wires it into the player and builds the
board sink.

### Decision

Author rulings, recorded verbatim (this session's own driving prompt,
2026-08-06, transcript-witnessed until this record):

**AR-BB1-0 (tag, standing ceremony).** Annotated `stable-20260806-ux-epilogue`
at `03a8698`, message "ux epilogue landed, design-channel-verified
2026-08-06 (ADR-0065)"; pushed; peeled ref verified — resolves exactly
to `03a8698`.

**AR-BB1-R (rider — the arc's rulings become repo fact).** The author
ruled the arc's shape today (design channel, 2026-08-06): (1) the fold
extension comes FIRST; (2) the component-graph change the wiring needs
(exporting `v2-replay` through `ehrt.sim-emit-hl7.interface`, a new
corpus→sim-emit-hl7 edge) is approved for session 2, not taken here;
(3) timing goes GENERAL — the bed board must serve foreign traffic, so
the fold self-anchors from the wire, no reference-date parameter. Also
ruled: module vendoring (ADR-0064's own "module vendoring widening the
ailment mix" intake note, recorded `[unverified]` there) is RATIFIED as
real and scheduled after this arc — the author ratification ADR-0064's
own close record said was owed is hereby given. Recorded here; the
roadmap's own Next row (`.agents/plans/roadmap.md`, the "Bulk
vendoring... follows once the catalog fully walks" bullet) gained a
one-line dated ratification note citing this ADR, same commit as this
record — nothing else moved, vendoring stays next-arc scope.

**AR-BB1-1 (A17 — the fold learns two participants).** `fold-message`
handles trigger "A17": walk the parsed message's segments in order,
pair each PID with its own immediately-following PV1 (two pairs; stop
pairing at any non-PID/PV1 segment — Z-segments may trail), and fold
each pair as a location/attending update onto that PID-3's own entry
(the A02 treatment, per pair) — never-seen MRNs self-initialize
(bootstrap-from-empty holds, foreign traffic may open mid-stream).
Red-first unit test against a real rendered A17, built via the
emitter's own `bed-swap-message` path (`churn_scenarios_test.clj`'s own
deterministic two-patient `:with` construction, reused), not a
hand-typed string.

**AR-BB1-2 (A40 — the fold learns identity reassignment).**
`fold-message` handles trigger "A40": PID-3 is the surviving MRN,
MRG-1 the merged-away one. The surviving entry absorbs per the
engine's own merge semantics (a no-op on the wire, below); the
merged-away MRN's entry becomes a tombstone — `:status :merged`, every
other field held over unchanged, the wire-side mirror of
`ehrt.sim-engine.engine`'s own `evolve :merge` `:merged` arm, which
touches `:status` alone:

```clojure
(defmethod evolve :merge
  [patient {:keys [participants surviving-mrn merged-mrns]}]
  (let [role (:role (first (filter #(= (:patient-id patient) (:patient-id %)) participants)))]
    (case role
      :survivor (-> patient (update :mrns into merged-mrns) (assoc :active-mrn surviving-mrn))
      :merged (assoc patient :status :merged))))
```

No ambiguity surfaced — the engine's own shape is unambiguous (one
field, nothing else), so the tombstone shape was not a two-way design
choice; `evolve-entry`'s `:survivor` arm is a wire-side no-op too:
`surviving-mrn` is always `(:active-mrn survivor)` itself (unchanged),
`:mrns` is truth-only and never wire-visible (`project-to-wire-visible-fields`),
and A40's own PV1 rides entirely blank
(`ehrt.sim-emit-hl7.emit-hl7/merge-message`), so there is no
location/attending update to apply to the survivor at all. Red-first
unit test against a real rendered A40, same mechanism rule.

**AR-BB1-3 (the property becomes the spec).** The emitter-coherence
defspec's churn profile extends to the FULL `sample-profile` set — the
`no-bed-swap-no-merge-churn` exclusion dies. Red-first, witnessed:
before the fold changes landed, the extended property ran against the
pre-fix fold and errored — `clojure.lang.ExceptionInfo:
v2-replay: unsupported message trigger (documented scope boundary)`
with `{:trigger "A40"}` on one run (shrunk to `[seed=0 patients=8
use-churn=true use-order=true]`) and `{:trigger "A17"}` on another
(same shrunk params) — both from `ehrt.sim-emit-hl7.churn/sample-profile`
churn at `patients` in `[1,10]`, well within the property's own default
150-trial budget; no seed pinning was needed. Green after the fold
extension landed (four consecutive full runs, 150 trials each, `0
failures 0 errors`). The `v2-replay` ns docstring's own scope-boundary
paragraph rewrote from "documented not silent" exclusion to
supported-with-citation; `v2_replay_test.clj`'s own header followed.
Grep for any other live surface stating the A17/A40 exclusion found one:
`components/sim/docs/sim-theory.md`'s own emitter-coherence-law
paragraph ("Documented scope boundary, not silent: the property
excludes bed-swap...") — reconciled in the same commit.
`notes/sim/agents/plans/roadmap.md`'s own mention is the FROZEN
pre-split roadmap (historical record of the original M2b scope
decision, mirroring `notes/tools/ADRs.md`'s own frozen-archive class,
distinct from the LIVE `.agents/plans/roadmap.md`) — left untouched,
per the same "frozen archives stay, live docs/docstrings reconcile"
rule ADR-0007's own citation-space audit already established.

**AR-BB1-4 (general time — the fold self-anchors from the wire).**
`hl7-instant->seconds` and `fold-message`'s `reference-date` parameter
are RETIRED: MSH-7 (and, via the same `t`, every observation's own
`:t`) parses to an ABSOLUTE epoch instant read from the wire alone —
`hl7-instant->millis` honors an explicit trailing offset (`±ZZZZ`/`Z`)
when present, treats a naive timestamp as UTC otherwise, lenient on
truncated precision via the SAME `YYYY[MM[DD[HH[MI[SS]]]]]` prefix
contract `ehrt.corpus.player/parse-dtm-lenient` already implements
(read there, aligned here — not extracted across the component
boundary this session; a shared helper, if ever wanted, is disclosed
as a finding not taken, not built). Unit: epoch MILLIS, matching the
player's own representation. The coherence property compares via a
test-side adapter (`absolutize`, in `v2_replay_test.clj`) converting
the engine's own run-relative seconds to absolute millis using the
run's own `ref-date` — `project-to-wire-visible-fields` itself does not
change shape and never sees a reference-date. `v2-replay`'s callers are
zero outside its own component — fresh grep (`grep -rn
"v2-replay/fold-message\|v2-replay/replay-messages\|sim-emit-hl7\.v2-replay"
--include="*.clj"`, excluding `components/sim-emit-hl7/`) hit only
`ehrt.docs-tooling.stale-path-test`, which checks for the RETIRED
`ehrt.sim.v2-replay` namespace NAME as a string literal, never calls
either function — the signature change was free, confirmed.

**AR-BB1-5 (scope + oracle).** No player, corpus, CLI, or interface
changes — `v2-replay` stayed unexported this session. No emitter
changes — `bed-swap-message`/`merge-message` untouched. The oracle
bracket (below) shows all ELEVEN batches identical — the fold is
read-side only. `config/busy-tuesday.edn`/`config/busy-weekday.edn`
(untracked scratch fixtures the author was already holding at session
start, unrelated to `config/busy-weekday.md`'s own standing disposition
since ADR-0060) were moved by the author out of `config/` at this
session's own Step 0, after their mere presence broke
`merge-config-file-suggests-a-same-stem-sibling-file`'s own sibling-file
assumption — a premise mismatch disclosed and resolved before any fold
code changed, not silently worked around. One PRE-EXISTING, unrelated
failure remained at every checkpoint throughout this session, baseline
through close: `merge-config-file-suggests-a-same-stem-sibling-file`'s
own `:did-you-mean` assertion, because `config/busy-weekday.md` itself
(the fixture the test's own comment calls "a real, deliberately
untouched pre-existing fixture in this repo") does not currently exist
on disk — out of this session's own fenced scope
(`components/sim-emit-hl7/src/ehrt/sim_emit_hl7/v2_replay.clj` and its
own test tree only), disclosed here rather than fixed silently or left
unmentioned.

### Red→green evidence

Step 1 (red, witnessed): `fold-message-folds-a-real-rendered-a17-into-two-participant-location-updates`
and `fold-message-folds-a-real-rendered-a40-into-a-survivor-and-a-tombstone`
both asserted the NEW post-fix behavior and errored against the
pre-fix fold with the exact `:unsupported-trigger` `ExceptionInfo`
(`{:trigger "A17"}` / `{:trigger "A40"}`). The extended
`emitter-coherence-reconstructed-state-matches-the-log-fold-at-every-boundary`
defspec errored the same way. `Ran 11 tests containing 49 assertions.
0 failures, 3 errors.` Committed `f0f8148`, pushed.

Step 2 (green): all three reds green; four consecutive full
`v2-replay-test` namespace runs (different `test.check` seeds each
time) all `0 failures, 0 errors`; full workspace suite
(`clojure -M:poly test :all skip:integration`) green throughout, one
pre-existing unrelated failure held constant (AR-BB1-5, above);
`clojure -M:poly check`: OK at every checkpoint. Committed `d1bf847`,
pushed.

### Oracle bracket

`bin/regression-oracle 03a8698 d1bf847`: soundness check IDENTICAL
outside the `digest.clj` `(ns ...)` form; all ELEVEN roots' SHA-256
digests IDENTICAL between baseline and target (appendicitis,
death-fixture, ear-infections, ear-infections-engine,
ear-infections-history-engine, sepsis, sinusitis, sore-throat,
total-joint-replacement-engine, urinary-tract-infections-engine,
urinary-tract-infections-history-engine) — matching AR-BB1-5's own
expectation exactly: the fold is read-side only, never touching the
emitter or the engine. This session's own closing commit (this ADR,
the index line, the session record, and the roadmap ratification note)
is docs-only — no re-run needed, since nothing after `d1bf847` touches
`src/` or `test/` (the same disclosed convention ADR-0065's own oracle
bracket used).

### Fences honored

Src edits stayed exactly where scoped:
`components/sim-emit-hl7/src/ehrt/sim_emit_hl7/v2_replay.clj` and its
own test tree (`v2_replay_test.clj`). The interface stayed untouched —
`v2-replay` is not exported. The emitter stayed untouched —
`bed-swap-message`/`merge-message` render exactly as before, oracle-
confirmed. The player stayed untouched — `ehrt.corpus.player`'s own
`parse-dtm-lenient` was read and aligned with, never modified. No gate
weakened — the emitter-coherence property only WIDENED what must hold
(the full churn set, not a subset); both existing unit-test-style
assertions and the property's own trial count are unchanged or grown,
never shrunk. Frozen archives untouched except this ADR's own new
file, the `notes/ADRs.md` index line, `notes/adr/README.md`'s file
count, and the roadmap's own one-line ratification note (AR-BB1-R).
`config/busy-weekday.md` stays untracked and untouched (standing
disposition since ADR-0060) — confirmed still untracked/nonexistent at
this close, unrelated to the two scratch files the author relocated at
Step 0.

### Consequence

`ehrt.sim-emit-hl7.v2-replay` is now total over the emitter's real
trigger set: a bed board fed default `--churn` traffic (bed-swap 0.03,
merge 0.01 per gap) no longer crashes on the first genuinely
two-participant message. Time is read from the wire alone, matching
what a real downstream consumer — the bed board that must serve
foreign traffic, not only this project's own emitted stream — would
actually have available. The emitter-coherence property is now the
real spec its own docstring always claimed: it holds over the FULL
churn family, not a hand-picked subset. Module vendoring's own
`[unverified]` intake status (ADR-0064) is discharged — ratified,
scheduled, still not built. The next session (accumulator wiring: the
interface export and corpus→sim-emit-hl7 edge under AR-BB1-R ruling
(2), the `--board` cadence flag, the board renderer and its gates) gets
its own prompt; this landing's own tag rides that session's Step 0
under standing ceremony.

### Index summary (moved verbatim from notes/ADRs.md by ADR-0143, 2026-08-16)

Player fold: the accumulator learns two-participant messages and absolute time — A17/A40 fold into the wire-side accumulator, MSH-7 reads an absolute epoch instant, the emitter-coherence property runs over the full churn family
