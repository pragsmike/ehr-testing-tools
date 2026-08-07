## ADR-0067 — Player board: the whiteboard exists — the accumulator meets the pacer

**Status:** Accepted (author-directed, autonomous session per R30), 2026-08-07.

### Context

Prior: the player fold landed and was design-channel-verified
(`01d9459`, `notes/adr/0066-player-fold.md`) — the fold is total over
the emitter's real trigger set (A17/A40 included), self-anchored in
absolute epoch millis, and the coherence property over the full churn
family is its own spec. This session builds what that enabled: the bed
board — a state-snapshot-at-intervals display (`notes/adr/0014-corpus-
player.md`'s own deferred surface) wired into `ehrt play`.

The component-graph change the wiring needs — exporting `v2-replay`
through `ehrt.sim-emit-hl7.interface` and giving `corpus` its first
real external edge into `sim-emit-hl7` — was ruled approved for this
session in ADR-0066 AR-BB1-R. One rider was ruled by the author on
2026-08-07: "Tests should build their own directories as needed" — the
standing principle resolving the fresh-clone-red finding ADR-0066
disclosed (`merge-config-file-suggests-a-same-stem-sibling-file` read
the LIVE `config/` directory and depended on the untracked
`config/busy-weekday.md`; the suite has never been green on a fresh
clone since that test landed, ADR-0060).

### Decision

Author rulings, recorded verbatim (this session's own driving prompt,
2026-08-07):

**AR-BB2-0 (tag, standing ceremony).** Annotated
`stable-20260806-player-fold` at `01d9459`, message "player fold
landed, design-channel-verified 2026-08-06 (ADR-0066)"; pushed; peeled
ref verified — resolves exactly to `01d9459`.

**AR-BB2-R (rider — tests build their own directories, RULED —
STANDING LAW).** *"Tests should build their own directories as
needed."* Verbatim, the author, 2026-08-07. Executed:
`merge-config-file-suggests-a-same-stem-sibling-file` was rewritten to
build its own fixture in a temp directory (`temp-dir-path*`, the same
helper its own neighboring tests already used) — creates
`<tmp>/busy-weekday.md`, requests `<tmp>/busy-weekday.edn`, asserts
`:did-you-mean` names the temp `.md` path — and its docstring corrected
(it had claimed the untracked repo file was "a pre-existing fixture in
this repo," which was false on a fresh clone). This makes the suite
green on a fresh clone for the first time since ADR-0060, and makes
`config/` safe for the author's own real configs. `config/busy-
weekday.md`'s disposition is now ceremonial: still untracked, still
untouched, no longer load-bearing for any test.

Enumeration (fresh grep, every test tree, for any OTHER test reading a
live mutable repo directory — the `config/` class; tracked
test-fixtures directories are explicitly out of scope):

| Match | Disposition |
|---|---|
| `config/synthea/synthea.properties` (integration tests, CLI parse test, two manifest-fixture literals) | Tracked (`git ls-files config/` confirms), out of scope by the ruling's own carve-out |
| `docs-tooling` lint tests walking `docs/`, `components/`, `.agents/skills`, `notes/prompts`, `.` (license-text, structure-currency, resource-nesting, stale-path, prompt-record-pairing, index-completeness, root-alias-completeness, skill-mirror-currency, invocation-lint, provenance-leaf-law) | Not the `config/` hazard class — these walk the TRACKED repo tree as their own literal test subject (that walk is the point of a lint test), not an ad hoc author-held scratch file whose presence/absence is incidental |
| Every `.listFiles`/`file-seq` call in `corpus`, `corpus-io`, `projects/conformance`, `projects/integration` test trees | Already scoped to a test-built temp dir or a tracked test-fixtures directory |

No additional offender found landing as small as the rider itself; none
fixed beyond the rider's own named test. The ruling sentence above is
STANDING LAW — its own `.agents/rulings.md` append is deferred to this
arc's own close, per that register's own contract (not appended
mid-arc without license).

**AR-BB2-1 (the export and the edge).** `ehrt.sim-emit-hl7.interface`
now exports exactly `fold-message` (2-arg) — nothing else the board
sink needs — with the header's own grep-discipline comment updated to
name `corpus`'s new board sink as `v2-replay`'s first real external
caller (the sentence claiming "NO real external caller at all" is
retired for `v2-replay`; it still holds for `site-profile`). Fresh
caller-grep for `sim-emit-hl7.interface` (recorded below) confirms
`components/corpus/src/ehrt/corpus/board.clj` as the new site.

**Finding — `components/corpus/deps.edn` needed no edit.** The
ruling's own literal text named `components/corpus/deps.edn` gaining a
`poly/sim-emit-hl7 {:local/root ...}` entry; `clojure -M:poly check`,
the ruling's own nominated arbiter, was run after `board.clj` was
written to genuinely `:require [ehrt.sim-emit-hl7.interface ...]` —
green with zero `deps.edn` changes anywhere. Fresh grep of every
`components/*/deps.edn` in this repo confirms NONE ever declares a
`poly/*` local-root edge (that wiring lives only in `deps.edn`'s own
`:dev`/`:test`/`:ehrt` aliases and each `projects/*/deps.edn`) — and
every one of those that already carries `poly/corpus` already carried
`poly/sim-emit-hl7` too, transitively, since `sim`'s own src has
required it since the sim split. The literal instruction didn't match
this repo's own established convention; the arbiter it named settled
it. `poly check` was run again after the full Step 3 implementation
landed — still OK, zero `deps.edn` edits.

**AR-BB2-2 (the `--board` flag).** `ehrt play PATH --board N`: N is a
positive integer (stream-minutes per snapshot), coerced and validated
the same way `--rate`/`--idle-cap` already are (`:invalid-board`,
reject by name, exit 2 via the standard Result rejection path). Board
is display-only: ignored when `--sink` is given (same precedence
wording `--ticker`'s own doc already states); wins over `--ticker`
when both are given. Both flags' own `:doc` strings say so. The result
envelope gains `:snapshot-count`/`:unfolded-count` when board mode ran,
and omits both keys otherwise (confirmed live, below).

**AR-BB2-3 (the board sink).** `board-sink` (`bases/cli/src/ehrt/cli/
core.clj`) closes over a fold accumulator and the board's own snapshot
cadence. Each event folds via `ehrt.corpus.interface/board-fold-event`
(the exported `fold-message`, wrapped); a message whose own trigger is
outside the emitter's handled set is SKIPPED — accumulator held
unchanged — with a cue printed through the same `println-fn` the
ticker itself would use (board IS the display, so the cue prints
inline, never routed to stderr) and counted (`:unfolded-count`).
Snapshot cadence is measured in stream time: a snapshot renders the
first time a message's own timestamp crosses each successive
board-minutes boundary from the first parseable timestamp; a message
with no parseable timestamp neither advances the anchor nor crosses a
boundary (the pacer's own lenient posture, unchanged). `finalize-fn`
renders one more snapshot, unconditionally, at the last timestamp seen,
after `run-plan!` completes — regardless of whether that instant
already coincided with a boundary-crossing snapshot.

**AR-BB2-4 (the renderer).** `ehrt.corpus.board/render-snapshot` is
pure (accumulator x instant-ms -> string), living in a new `corpus`
namespace (`ehrt.corpus.board`) rather than `player.clj` or `display.clj`
— fresh-read judgment: it needed its own `sim-emit-hl7` require
(`player.clj` is deliberately clock/IO-free and has no such need) and
deliberately does not entangle `display.clj`'s own still-open
placement question (`.agents/plans/roadmap.md`'s own named-future
row). Renders a header naming the snapshot instant (ISO-8601, UTC, via
`java.time.Instant/ofEpochMilli`), occupied beds grouped by ward
(sorted) with beds sorted within, one line per patient (bed, patient
name, MRN, class, attending when present — a `?` when a bootstrapped-
from-transfer entry never got a class at all, the ticker's own
never-throw convention extended here), then a one-line tally
(inpatients / active outpatients / discharged / merged — tombstones
counted, never listed as occupying a bed). Operator voice throughout —
the CLI's own `help-voice-test` gate was re-run after the `--board`
flag doc landed; still green.

**Live-probe-caught bug, fixed same session.** The first live-probe run
(below) surfaced a merge tombstone (`:status :merged`) still rendering
as occupying a bed: `fold-merge` absorbs into the survivor but never
clears the merged-away entry's OTHER fields, so a merged-away mrn that
had a real `:location` before the merge keeps it, stale, in the
accumulator. `render-snapshot`'s own `occupied?` originally checked
only `(:location entry)`; fixed to also exclude `:status :merged`.
The renderer's own multi-ward unit test was strengthened to build its
tombstone fixture with exactly this shape (a merged entry carrying a
stale `:location`) so the regression is caught by the unit suite next
time, not only by a live probe.

**AR-BB2-5 (scope + oracle).** `v2_replay.clj` src untouched this
session (confirmed: the only `sim-emit-hl7` change is `interface.clj`'s
export). Emitter untouched. No pacer-algorithm change (`player.clj`'s
own `plan` is untouched; the board is a sink, not a scheduler). Oracle
bracket `bin/regression-oracle 01d9459 6ee8f79`: soundness check
IDENTICAL outside `digest.clj`'s own `(ns ...)` form; all ELEVEN roots'
SHA-256 digests IDENTICAL between baseline and tip (appendicitis,
death-fixture, ear-infections, ear-infections-engine, ear-infections-
history-engine, sepsis, sinusitis, sore-throat, total-joint-
replacement-engine, urinary-tract-infections-engine, urinary-tract-
infections-history-engine) — matching this ruling's own expectation:
the board is read-side only, never touching the emitter or the engine.

### Fresh caller-grep (AR-6 mechanism, exercised not asserted)

`grep -rn "sim-emit-hl7.interface" --include="*.clj" .` after AR-BB2-1
landed: `components/corpus/src/ehrt/corpus/board.clj` is the new site
(`(:require [ehrt.sim-emit-hl7.interface :as sim-emit-hl7])`, calling
`sim-emit-hl7/fold-message`). Every other hit is a pre-existing caller
unchanged by this session: `components/oracle/src/ehrt/oracle/digest.clj`,
`components/sim/src/ehrt/sim/{run,identifiers}.clj` and their tests,
`components/sim-emit-fhir/test/.../emit_fhir_test.clj`,
`components/sim-engine/test/.../churn_scenarios_test.clj`,
`components/sim-emit-hl7/test/` (its own vendored-fixture and
emitter-order tests), and `components/docs-tooling/test/.../
stale_path_test.clj` (a string-literal check, not a real call).

### Red→green evidence

Step 2 (red, witnessed): with the interface export and the (edit-free)
edge landed, `ehrt.corpus.board-test` (renderer + `fold-event`, against
stub bodies that threw "not yet implemented") ran `5 tests, 0 failures,
5 errors` — every test errored on the stub's own exception, not a
compile failure, confirming the export/edge compiled cleanly. The new
`play-command-board-*` tests in `ehrt.cli.core-test` (run directly,
since `--board` didn't exist in `play-command` at all yet) ran
`251 tests, 7 failures, 0 errors` — the four cadence/precedence/cue
tests failed cleanly on assertion mismatches (missing `:snapshot-count`/
`:unfolded-count` keys, an empty `re-seq` count, the ticker's own
compact line still present); the sink-precedence test passed trivially
(it only asserts the ABSENCE of board keys, true before implementation
too — a real regression guard once green, not red evidence itself).
Both transcripts quoted in full in `.agents/session-records/
2026-08-07-player-board.md`.

Step 3 (green): `render-snapshot`/`fold-event` implemented for real;
one nil-`:class` NPE (a bootstrapped-from-A02-alone entry, occupying a
bed with no class ever set) found and fixed with the ticker's own `?`
leniency convention before any test file needed changing beyond the
DOB field an unrelated `hl7-date->iso` NPE demanded in the synthetic
test fixtures (both `board_test.clj` and `core_test.clj`'s own `msh`/
`board-message` helpers were missing PID-7, tripping a nil `subs` in
frozen `v2_replay.clj` code — fixed in the fixtures, not the frozen
src). `--board` flag, `board-sink`, and the renderer landed together;
`ehrt.corpus.board-test` and `ehrt.cli.core-test` (256 tests, 749
assertions) both `0 failures, 0 errors`; `help-voice-test`/
`help-wrap-test` re-run clean. Full workspace suite (`clojure -M:poly
test :all skip:integration`): 227 namespaces, `0 failures, 0 errors`,
exit 0. `clojure -M:poly check`: OK.

### Live probe (AR-BB2-5)

`bin/ehrt corpus generate sim --seed 42 --patients 8 --churn` produced
no A17/A40 traffic; seeds 1/2/3/5/7/11/13/17/19/23 were swept (`grep`
for `ADT^A17`/`ADT^A40` per seed) — seed 5 produced both (one A17, one
A40) alongside 8 A01/2 A02/7 A03/2 A12, spanning about 7h19m of stream
time. `bin/ehrt play <seed-5 corpus> --board 60 --rate 100000`: 7
snapshots (six boundary crossings across the ~7.3-hour span plus the
unconditional final one, which coincided exactly with the last
boundary crossing and rendered a second time as the ruling specifies),
correctly tallying the A40 merge (`merged: 1` from the second snapshot
onward) and — post-fix — never listing the merged-away mrn as
occupying a bed. `bin/ehrt play <seed-5 corpus> --board 60 --rate
100000 --sink file:...` vs. a plain `--sink file:...` run at the same
rate: byte-identical output, and the board-mode result envelope
carries neither `:snapshot-count` nor `:unfolded-count` — board
correctly ignored. Full transcript quoted in
`.agents/session-records/2026-08-07-player-board.md`.

### Fences honored

Src edits stayed exactly where scoped: `ehrt.sim-emit-hl7.interface`
(export + header comment only — `v2_replay.clj` itself untouched);
`components/corpus/src/ehrt/corpus/{board,interface}.clj` (new
namespace + its re-exports); `bases/cli/src/ehrt/cli/{core,help}.clj`
(the `--board` flag, `board-sink`, `play-command`'s wiring and
docstring, the `cli-spec` entry); their own test trees; and
`components/sim/test/ehrt/sim/run_test.clj` (the rider's one test — no
additional offender landed, per the enumeration above). No
`components/corpus/deps.edn` or `projects/*/deps.edn` edit was needed
(the finding above). Emitter untouched. No pacer-algorithm change. No
gate weakened. Frozen archives untouched except this ADR's own new
file, the `notes/ADRs.md` index line, `notes/adr/README.md`'s file
count, and `.agents/plans/roadmap.md`'s own Done pointer.

### Consequence

`ehrt play PATH --board N` renders a real bed board: the wire-side
accumulator ADR-0066 made total now drives a display, not just a
property test. A stranger running this tool against a paced HL7 v2
stream can watch the hospital breathe — beds fill, patients transfer,
discharge, and (when a feed carries it) merge — without reading a line
of this project's own source. The two remaining player-arc rows in
`.agents/plans/roadmap.md`'s own Next section (bed board, accumulator
wiring) are DONE; the sim event-log input adapter stays Next,
unbuilt. Arc close (rulings appends including this session's own
standing-law rider, state regeneration, budgets, rotation, tags)
follows a fresh design-channel probe against this landing; the
vendoring arc's design pass opens after, per the standing ratification
ADR-0066 AR-BB1-R already gave it.
