# 2026-08-11 — ehr-testing-tools: corpus batching, the transport gets one notch real (ADR-0111)

## Context

Archived 2026-08-11. Repo: `~/src/ehr-testing-tools` (ext4 clone).
Session opened at HEAD `b5b9b9e` (ADR-0110's own close) and closed at
this record's own close-phase commit. Original prompt follows
verbatim.

## Original prompt (verbatim)

# Session prompt -- corpus batching: the transport gets one notch real (ADR-0111)

## Context

You are Claude Code executing under R30 ceremony in the ehr-testing-tools
workspace. This session lands batched delivery under two author rulings
(2026-08-11): the batcher is a CORPUS-LEVEL tool, separate from the
sim, working on any directory of valid v2 message files including
foreign corpora (Q1 a, author emphasis verbatim: "It should work on
any corpus, even an existing directory of foreign (but valid) message
files"); and the HL7 v2 batch protocol's BHS/BTS wrappers land as a
`:batch` framing codec in v1 (Q2 a). One session: mechanism + demo.
HEAD at handoff: b5b9b9e. This session's ADR is ADR-0111.

THE DESIGN (channel-specified under the rulings; verify-then-act):

1. **The partition fn** (pure, corpus-io): messages -> sorted by
   MSH-7 transmit time -> partitioned into epoch-aligned buckets
   (bucket k = [k*interval, (k+1)*interval) against the epoch, so
   daily batches align to UTC midnight, hourly to the hour --
   matching real schedules; an --anchor option is a NAMED DEFERRAL).
   Empty buckets are skipped v1 (interior empty-batch realism: named
   deferral note). MSH-7 extraction REUSES the player's existing
   extraction fn (one source of truth -- find it where ADR-0100's
   timestamp seam left it; if reuse requires moving it to a shared
   home, that is a move-don't-improve micro-relocation, disclosed).
   A message whose MSH-7 cannot be parsed is a CATEGORIZED error
   naming the file (the author's premise is foreign-but-VALID;
   an unparseable transmit time fails fast, never a silent skip).

2. **The `:batch` codec** (corpus-io/framing.clj, D2's codec-only
   doctrine -- pure bytes, encode/decode pair like its siblings):
   encode = BHS segment + the messages (er7-multi form) + BTS with
   BTS-1 = the TRUE message count; decode strips the wrappers,
   VERIFIES BTS-1 against the actual count (mismatch -> categorized
   error -- a free transport-integrity check), and yields the
   messages. BHS fields: minimal, deterministic -- NO wall clock
   anywhere (the determinism law); if a creation-time field is
   populated at all, it is the batch window's own boundary time.
   Read the v2 batch-protocol segment definitions before authoring;
   record the field choices in the ADR. FHS/FTS file-level wrappers:
   NAMED DEFERRAL.

3. **The CLI leg**: `ehrt corpus batch DIR --interval <spec>
   --out-dir OUT` -- reads every candidate message file in DIR
   (multi-message files split via the existing machinery), sorts ALL
   messages by MSH-7 across files (never file order -- state it in
   the help text: the wire's own transmit order is the batch
   order), partitions, writes `batch-NNN.hl7` per occupied bucket
   in `:batch` framing, prints a summary (batches written, messages
   per batch, span). Interval spec: a small unit grammar (e.g. 15m,
   1h, 24h) if the CLI's flag conventions accommodate it cleanly,
   else plain minutes -- read help.clj's conventions and decide,
   disclosed. Every landed CLI discipline applies: categorized
   reads (the ADR-0096/0098 family), unknown-flag routing,
   out-dir conventions matching the corpus group's precedents.

4. **The demo**: ed-tuesday's "second clock" README section gains a
   batching subsection: run the batcher over the SAME latency
   out-dir the section already generates (hourly interval), witness
   a straddling encounter -- ideally the section's own named patient
   -- with its admission/discharge records split across consecutive
   batch files, EACH batch's BTS-1 verifying clean. The lesson,
   stated in one or two sentences with the author's charter framing:
   transport-level completeness (every BTS count checks out) says
   nothing about clinical-level completeness (the encounter's record
   set spans batches); downstream receivers deciding "do I have all
   of this encounter?" get exactly the case they need. Witnessed
   block from a real run.

5. **The taxonomy note** (ADR prose, no code): transport realism
   (delay -- ADR-0109; batching -- this ADR) simulates CORRECT
   transport behaviors deterministically; mutation injects INCORRECT
   content with expected findings; loss/duplication sit on the
   boundary (real transports do both) -- a named future taxonomy
   question, the author's "mutation as imperfect transport" framing
   recorded as its origin.

ORACLE BRACKET: pure identity on all 35 roots -- no sim/emit/engine
src changes anywhere; the footprint is corpus-io + cli + demo docs.
Movement = STOP-AND-REPORT.

## Read first

- docs/dev/source-sink-design.md -- D2/D3 (the codec doctrine this
  session extends exactly as designed)
- components/corpus-io/src/ehrt/corpus_io/framing.clj -- the sibling
  codecs' encode/decode shapes and doctrine header
- components/corpus/src/ehrt/corpus/player.clj -- the MSH-7
  extraction fn (ADR-0100's seam) and split machinery
- bases/cli/src/ehrt/cli/core.clj -- the corpus group's conventions,
  the categorized-read family shapes, unknown-flag machinery;
  help.clj flag conventions
- demos/scenarios/ed-tuesday/README.md -- the second-clock section
  this extends (and its witnessed patient)
- notes/adr/0109-*.md and 0110-*.md -- the latency arc this composes
  with
- .agents/rulings.md -- RNG-path law (n/a here -- the batcher is
  DETERMINISTIC, no RNG anywhere; say so in the ADR), tag law,
  ASCII verification

## Author rulings, verbatim

- [A] 2026-08-11, author verbatim "Q1 a. I want this separate from
  the sim. It should work on any corpus, even an existing directory
  of foreign (but valid) message files."
- [A] 2026-08-11, author verbatim "Q2 a. Go." -- the BHS/BTS `:batch`
  framing codec in v1.
- [A] The chartering direction, recorded verbatim at the close (the
  author's batched-downstream framing, including the bundle/batch
  terminology finding: FHIR's Bundle is the formal term of art; HL7
  v2's is the batch protocol, BHS/BTS; practitioners' "bundles"
  gestures at both).
- [C] Everything under THE DESIGN: epoch-aligned buckets, skip-empty
  v1, fail-fast on unparseable MSH-7, the named deferrals
  (--anchor, interior empties, FHS/FTS), the taxonomy note.

## Steps

1. **Tag ceremony (tag law case i -- licensed here).** The design
   channel has verified the ADR-0110 landing at `b5b9b9e` by fresh
   public clone. Tag `stable-20260811-latency-demo` at `b5b9b9e`,
   push, verify the peeled ref. Remote moved = STOP-AND-REPORT.

2. **Mechanism commit**: partition fn + `:batch` codec + CLI leg,
   co-landed tests: partition boundary semantics (epoch alignment,
   half-open intervals, cross-file MSH-7 ordering on a hand-built
   multi-file foreign-shaped fixture); codec round-trip (encode then
   decode = identity, BTS-1 verified, mismatch categorized); the CLI
   family legs (missing dir, unreadable file, unparseable MSH-7,
   unknown flag) red->green per the landed shapes; `make docsgen`
   (cli.md gains the subcommand).
   Commit message (ASCII only):
   `feat: corpus batch -- schedule-partitioned delivery, BHS/BTS framing (ADR-0111)`

3. **Demo commit** (may fold into commit 2 if small -- your call,
   disclosed): the README subsection with the real witnessed run.
   Commit message (ASCII only):
   `docs: latency demo batched -- encounter straddles the schedule (ADR-0111)`

4. **Oracle bracket.** All 35 identical. Movement = STOP-AND-REPORT.

5. **Full gate.** poly check, full local suite, CLI parse-guard
   lint, sim purity lint, bin/verify-nist-lock.

6. **Close phase.** FIRST: self-archive this prompt. Then: ADR-0111
   (the codec's field choices, the boundary semantics, the witnessed
   straddle, the taxonomy note, the named deferrals, deviations
   dated); roadmap: the transport row (create it if the intake never
   landed one) dispositioned -- batching landed, deferrals anchored;
   .agents/rulings.md records both 2026-08-11 rulings verbatim;
   notes/ADRs.md index row; notes/adr/README.md count 108 -> 109;
   session record.
   Commit message (ASCII only):
   `docs: session record and prompt archive -- corpus batching (ADR-0111)`

7. **Push and verify.** Push per R30 checkpoints. Post-push, ASCII
   check FIRST on every commit message, then CI confirmation.

## Fences

- Touch ONLY: components/corpus-io/{src,test},
  components/corpus/{src,test} (only if the extraction-fn
  micro-relocation lands there), bases/cli/{src,test}, docs/cli.md
  (generated), demos/scenarios/ed-tuesday/README.md (the
  subsection), demos/scenarios/README.md (only if a line is owed),
  notes/adr/0111-*.md, notes/ADRs.md, notes/adr/README.md, .agents/*
  close-phase files. The sweep RULE governs (ADR-0099 precedent).
- ZERO sim-family src changes; emit/emit-wire untouched; the
  scenario CONFIGS untouched (the demo reuses existing out-dirs'
  generation commands).
- No RNG anywhere in the new code (deterministic by design -- the
  purity claim is testable: same input, same batches, byte-stable).
- No history rewrites; deviations dated; STOP-AND-REPORT over
  improvisation.
- Channel claims (the extraction fn's home, the batch-protocol
  segment shapes, help conventions) are verify-then-act.
