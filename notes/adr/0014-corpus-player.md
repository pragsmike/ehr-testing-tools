<!-- Attic file: notes/adr/0014-corpus-player.md -->
<!-- Moved verbatim from notes/ADRs.md by scaffolding compaction B (2026-08-05, notes/ADRs.md ADR-0046). notes/ADRs.md remains the citation index -- see there. -->

## ADR-0014 — Corpus player: pacer semantics, plan/execute time seam, cue rule extends artifact-vs-display; bed board and accumulator wiring deferred

**Status:** Accepted (author-directed, autonomous session per R30), 2026-07-30.

**Note (2026-07-30, added by this same session, per ADR-0012/ADR-0013's
own precedent):** this record shares its number with the frozen
`notes/tools/ADRs.md` ADR-0014 ("Intake learns optional manifest
sidecars, directory-scoped and generator-agnostic"). Per this file's
own preamble citation rule: a bare `ADR-0014` anywhere in this
workspace's live documents means *this* record; the tools-era one is
always cited as `notes/tools/ADRs.md` ADR-0014 or `tools/ADR-0014`.

### Context

The corpus player was named in the founding design chat (2026-07-27)
and given a four-part decomposition in ADR-0013's own player paragraph:
an **input adapter** (something → a time-ordered event stream), a
**pacer** (stream-time → wallclock at rate R, with idle-skip), an
optional **accumulator** (the M6 v2-replay state fold), and **sinks** —
a ticker (one line/block per event, the primary visual sink) and paced
file/MLLP emission (the non-visual sleeper that makes the player a
load/soak instrument). Three of the four parts already existed as
tested code by the time this session started: the message boundary is
`ehrt.tools.corpus.framing`'s own `:er7-multi` codec (the one splitter
in the codebase, ADR-0013); the ticker's rendering is literally
`ehrt.tools.display/render-er7-message`, built per-message for exactly
this reuse; and an accumulator already lives in the sim arc. This
session builds the one genuinely new mechanism — the pacer — plus the
two thin sinks that compose from already-landed parts: the ticker and
paced file emission. The bed board (a state-snapshot-at-intervals
surface), the accumulator's own wiring into the player, a sim
event-log input adapter, and where any of this should live relative to
a separate `ehr-testing-viz` repo are all explicitly deferred, named
here as the player's remaining work rather than built.

A useful identity anchors the design and is worth stating plainly:
**`ehrt play` at rate ∞, ticker sink, is `ehrt show`.** The player is
`show` plus time — nothing about `show`'s own rendering or dispatch
needed to change for this to be true; the pacer is purely additive.

### Decision

**Scope, ruled in chat, 2026-07-30: pacer + ticker sink + paced file
emission, this session.** Bed board, accumulator wiring, a sim
event-log input adapter, and the `ehr-testing-viz` placement question
are deferred, recorded, not built.

**Placement: `components/tools`** — the founding chat's own ruling
("start with the player living in tools"), matching `ehrt.tools.display`'s
own placement (ADR-0013) and this component's existing corpus/judge
machinery.

**Plan/execute split — the `:tty?-fn` seam pattern applied to time.** A
pure planning function (`ehrt.tools.player/plan`) takes a
time-ordered event sequence plus `{:rate :idle-cap-ms}` and returns an
emission plan — a seq of `[wait-ms event]` pairs, plus clamp/
unparseable/skip counts — with **no clock, no sleep, no IO**. A small
executor (`bases/cli`) folds the plan through an injected `:sleep-fn`
and a sink function. Every pacing computation is property-testable
without a wallclock; ambient time exists only in the production
executor's own default `:sleep-fn` (`Thread/sleep`). This is exactly
the injection discipline ADR-0013's `:tty?-fn` already established,
applied to a second ambient concern.

**Pacing semantics.**

- **`--rate R`**: stream-seconds per wallclock-second. Default `60` —
  one stream-hour of corpus time passes per wallclock-minute; `--rate
  1` is real time. Implemented as ordinary division
  (`wait-ms = delta-ms / R`), so `R` at (or near) infinity naturally
  yields all-zero waits with no special-cased sentinel — this is
  *why* the show identity above holds by construction, not by a
  separate code path.
- **Timestamps come from MSH-7**, parsed leniently by field-splitting
  the raw MSH segment text on the character MSH-1 itself declares (no
  HAPI dependency for reading one field) and accepting any
  `YYYY[MM[DD[HH[MM[SS]]]]]` prefix with an optional trailing
  fraction/zone ignored; missing trailing components default to the
  start of their unit (month 1, day 1, hour/min/sec 0).
- **Input order is preserved, never sorted.** Emission order is
  semantically load-bearing (an ADT message before the ORU it
  triggers) and the corpus's own order is the corpus's own statement —
  `plan` walks events in the order given, full stop.
- **Negative inter-event deltas are clamped to zero and counted**
  (`:clamped-count`) — an out-of-order or duplicate timestamp doesn't
  produce a negative wait, it produces an immediate emission, tallied
  so a caller can tell a well-ordered corpus from one that wasn't.
- **A message with a missing or unparseable MSH-7 paces at zero
  delta** (emitted immediately after its predecessor) **and is
  counted** (`:unparseable-count`); its own timestamp is treated as
  identical to its predecessor's for every *later* delta computation
  too, so one bad timestamp doesn't corrupt every subsequent gap in
  the run.
- **Idle-skip is a wallclock cap, not a stream-time threshold:**
  `--idle-cap SECONDS` (default `5`) caps any single computed wait —
  applied *after* dividing by rate, since "wait" means wallclock wait.
  When a wait is actually capped, a skip cue is emitted (see below) and
  tallied (`:skip-count`), distinct from `:clamped-count` (a capped
  wait is never also a clamped one — clamping is specifically the
  negative-delta case).

**The cue rule extends ADR-0013's artifact-vs-display boundary.** A
skip cue (showing that stream-time jumped) is emitted to the ticker's
own stream or to stderr — **never into a data sink.** Paced emission to
a file (or, later, a socket) writes bytes byte-identical to what
unpaced emission through the same sink designator would write; pacing
changes *when* bytes move, never *which* bytes. ADR-0013 drew its
artifact-vs-display line around content (files, `--report`, redirected
bytes are deterministic; a live terminal's rendering is not in scope of
that doctrine). This record extends that line explicitly: **artifact
content stays deterministic under the player too — timing is the
instrument's own concern, entirely outside the doctrine's scope.**
Stated here so a future session doesn't read paced-emission timing
variance as a doctrine violation; it isn't one, because content is
unaffected.

**Sinks.**

- **Ticker (the default):** full mode renders each message as a
  complete block via `render-er7-message` — the exact call shape
  ADR-0013's own display test already pinned — separated by blank
  lines, pretty-always (no TTY consultation, matching `show`'s own
  discipline). A `--ticker line` mode emits one compact line per event
  (MSH-7 timestamp, MSH-9 message type^trigger, first PID-3 patient
  identifier when the message carries a PID segment) via the same
  lenient field-splitting `plan` already does for MSH-7 — reusing the
  segment-splitting idea, not a second HL7 parser.
- **Data sinks reuse the existing source-sink designator vocabulary**
  (`ehrt.tools.corpus.source-sink-url`) rather than inventing a
  parallel flag scheme — `--sink dir:.../file:...` designators land
  exactly like `--out-dir`/`--out` do everywhere else in this CLI.
- **MLLP transport sink: deferred, per this session's own bail-out
  procedure.** `:mllp` already exists as a *framing* (byte-level
  0x0B/0x1C 0x0D envelope, `ehrt.tools.corpus.framing`) but there is no
  `:mllp` *sink kind* in `ehrt.tools.corpus.source-sink`'s own
  `known-sink-kinds` (`#{:dir :file :stdout :blaze}`) — a real network
  socket write. Building one properly touches three namespaces at
  once (a new canonical schema and constructor in `source-sink.clj`, a
  new scheme in `source-sink-url.clj`'s grammar, and a new write
  function in `sink-write.clj`), not a single isolated extension
  point — assessed against this session's own bail-out procedure and
  judged to balloon past "lands small." Deferred whole, not
  half-built: this session ships `--sink dir:`/`file:` only, and names
  the shape a future `:mllp` sink would need (the three-namespace
  surface above, plus connection lifecycle and ACK/retry policy,
  explicitly out of scope even when it does land).

**End-of-run summary.** The player emits a standard Result envelope —
events emitted, stream-time span, wallclock elapsed, rate, clamp
count, unparseable-timestamp count, skip count, sink designator —
through the existing TTY/`--pretty`/`--edn`/`--json` machinery
(ADR-0013): during a run the ticker (or the data sink) owns stdout: the
summary is the machine surface, printed once, at the end, the same way
every other command's result already is. Ctrl-C mid-run producing a
partial, graceful summary is out of scope this session — recorded as
deferred, not silently unhandled.

**One combined capture-and-build session, unattended (R30), matching
the 2026-07-30 output-UX session's own shape.**

### Alternatives rejected

*Sorting events by timestamp before pacing* — the corpus's own order is
part of what it says; an ADT-before-its-own-ORU corpus sorted by a
slightly-later ORU timestamp would silently reorder a causally
meaningful sequence. *A stream-time idle-skip threshold (skip whenever
the corpus itself has a large gap) instead of a wallclock cap* — this
would make skip behavior depend on the corpus's own timestamps
independent of the chosen rate, so the same corpus would skip
differently at `--rate 1` vs `--rate 3600` for no reason a user chose;
capping the actual wallclock wait is what "idle" means from the
sitting-at-a-terminal, watching-it-play perspective this sink serves.
*Treating a capped wait as also "clamped"* — conflating two different
findings (a corpus with out-of-order timestamps vs. a corpus with a
long real gap) into one counter would make the summary's own clamp
count lie about ordering when the real story was pacing, or vice
versa. *Building the MLLP sink today, minimally* — assessed directly
against the bail-out procedure (see Decision, above) and found to
cross three namespace boundaries rather than one; a half-built network
sink with no ACK handling and untested lifecycle is a worse outcome
than a clearly named deferral.

### Consequence

`ehrt.tools.player` is new, pure surface with no IO; `bases/cli` gains
`ehrt play` beside `ehrt show`, `:sleep-fn`/a clock seam in the
injection map, and `--rate`/`--idle-cap`/`--ticker`/`--sink` flags.
Nothing about any existing verb's behavior, exit code, or output
contract changes. A future session building the bed board, wiring the
sim accumulator into the player, or adding the `:mllp` sink inherits a
tested pacer and two working sinks to build around, and this record's
own three-namespace assessment of what an `:mllp` sink actually needs.

**Fulfillment note (2026-07-30, added by the CLI trial-UX session,
ADR-0015).** The directory half of this record's own `play-command`
input-scope deferral ("A single HL7 v2 (ER7) file is this session's
own input scope; a directory... is `:play-input-unsupported`") is
retired: `ehrt play` now accepts a directory of files sharing the
sniffed v2 format, concatenated in lexical filename order before
planning — fix-forward, not a revert of this record's own original
scoping, which was correct for what this session actually built. The
FHIR half of the same deferral, and every other item this record names
as future work (bed board, accumulator wiring, a sim event-log input
adapter, the `:mllp` sink), remain exactly as deferred here. See
ADR-0015 for the fulfillment's own design record.

**Status.** Accepted (author-directed, autonomous session per R30), 2026-07-30.

---

