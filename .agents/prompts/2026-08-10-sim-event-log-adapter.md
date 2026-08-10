# 2026-08-10 — ehr-testing-tools: corpus player, sim event-log input adapter (build session)

## Context

Archived 2026-08-10. Repo: `~/src/ehr-testing-tools` (ext4 clone).
Session opened at HEAD `3e3c167` (fixture relocation, ADR-0099) and
closed at the third feature commit plus this record's own close-phase
commit. Original prompt follows verbatim; a deviation record follows
that.

## Original prompt (verbatim)

Session prompt -- corpus player: sim event-log input adapter (ADR-0100)

## Context

You are Claude Code executing under R30 ceremony in the ehr-testing-tools
workspace. This session executes the roadmap Next row "Corpus player:
sim event-log input adapter" (notes/adr/0014-corpus-player.md, named
there as remaining work), under three author rulings recorded below,
PLUS the Deferred row that fires on this session by its own trigger:
"`ehrt play`'s own bare reads, true name" (ADR-0096 Finding 2 /
ADR-0097; revisit trigger verbatim: "the next session touching `ehrt
play` or the corpus-player slices"). HEAD at handoff: 3e3c167. This
session's ADR is ADR-0100.

What exists today (channel-probed, verify-then-act):
- The sim's event log has exactly one serialized form: `ehrt sim run
  --format ground-truth` prints `(pr-str ground-truth-vector)` to
  stdout (sim-ground-truth-bare-text, bases/cli). NO event log lands
  on disk: `spool-sim-output!`
  (components/corpus/src/ehrt/corpus/sim_adapter.clj... NOTE: the
  spooler actually lives in generators.clj per the probe -- confirm
  its true home by grep before editing) writes msg-%03d.hl7 + the
  sim's own manifest.edn verbatim, nothing else.
- The run payload carries :ground-truth (components/sim run.clj ~391)
  and the :execute-fn hands the WHOLE payload to the spooler -- the
  events are already in the spooler's hands.
- Ground-truth events are maps with :type
  (:admission/:discharge/:transfer/:outpatient-visit/...), :t on the
  engine's seconds-from-run-start clock (sim/ADR-0011), :location,
  :citation. Read the compile-trajectory source for the full shape
  before writing the ticker line.
- The player is message-native: corpus/player.clj `plan` takes raw
  ER7 strings and extracts MSH-7 internally; the ticker renders ER7;
  the board folds MESSAGES (wire-side, sim-emit-hl7/fold-message).
- play-events-from-file/play-events-from-dir (bases/cli/core.clj
  ~1442/1452) carry the unguarded slurp/sniff shape, allowlisted BY
  NAME in cli_parse_guard_lint_test.clj -- the allowlist entries are
  the fix's ready-made co-landed gate.

Oracle bracket, with its reasoning (re-earned for this session's
actual footprint): pure identity on all 34 roots is EXPECTED --
`ehrt.oracle.digest` requires only sim-trajectory/sim-model/
sim-engine/emit-hl7 interfaces and runs its OWN golden runs; it never
digests generate out-dirs and never touches components/corpus or
bases/cli, which is this session's entire footprint. The plan seam
(Step 4) must default to byte-identical behavior; the existing player
tests are the witness. Any digest movement is STOP-AND-REPORT.

## Read first

- `.agents/plans/roadmap.md` -- the Next row and the fired Deferred
  row ("ehrt play's own bare reads"), both verbatim
- `notes/adr/0014-corpus-player.md` -- the player's four-part
  decomposition, the plan/execute time seam, the cue rule, the
  bail-out procedure this session inherits
- `notes/adr/0067-player-board.md` -- the board's fold contract (why
  event input rejects --board)
- `notes/adr/0096-*.md` -- the categorized-read shapes the bare-reads
  fix mirrors, and Finding 2 itself
- `components/corpus/src/ehrt/corpus/player.clj` -- plan's full body:
  confirm timestamp extraction is the ONLY message-specific step
  before cutting the seam
- `components/corpus/src/ehrt/corpus/generators.clj` --
  spool-sim-output! and the :sim entry's :execute-fn
- `components/sim-trajectory/src/ehrt/sim_trajectory/
  compile_trajectory.clj` -- the event shape
- `bases/cli/src/ehrt/cli/core.clj` -- play-command,
  play-events-from-file/-from-dir, sim-ground-truth-bare-text
- `bases/cli/test/ehrt/cli/cli_parse_guard_lint_test.clj` -- the
  allowlist entries to remove
- `demos/scenarios/busy-tuesday/README.md`
- `.agents/rulings.md` -- tag law AR-T-1, ASCII-first verification

## Author rulings, verbatim

- [A] 2026-08-10, adapter semantics, author verbatim "Q1 a.": native
  event playback -- events paced by :t directly via an injectable
  timestamp-extraction seam on plan (continuing the
  :tty?-fn/:sleep-fn injection lineage, NOT a second pacer); a
  compact event-line ticker; `--board` under event input REJECTED
  with a named-deferral hint (the board's fold is wire-side; feeding
  it would need emission parameters the log does not carry).
- [A] 2026-08-10, producer side, author verbatim "Q2 a.":
  `corpus generate sim` also spools the ground-truth vector as
  `events.edn` into out-dir, same pr-str bytes as `--format
  ground-truth`'s bare text. The ADR notes the spooler's output-set
  change out loud against D7 ruling 4's "provenance is the
  generator's word" (that ruling governed manifests; events.edn is
  data, not provenance).
- [A] 2026-08-10, demo touch, author verbatim "Q3 a.": busy-tuesday's
  README gains ONE "play the sim's own story" example line once the
  adapter lands. Nothing else attaches: the injuries-family and
  busier-demo asides remain un-committed (ADR-0097's own wording),
  no rows invented.
- [A] The fired Deferred row, verbatim from the roadmap: the
  bare-reads fix with the allowlist entries' removal as its own
  co-landed gate.
- [C] Channel-inferred, verify before acting: the :sim-events
  recognition lives in play's OWN file dispatch (extension .edn ->
  guarded EDN parse -> shape check on the first element), NOT in the
  shared sniff-path-format -- extending the shared sniff would change
  gate/show behavior on .edn files, which no ruling covers. Grep
  sniff-path-format's caller set to confirm the blast radius claim
  before choosing.
- [C] The no-messages guard (:sim-produced-no-messages) is UNCHANGED:
  events.edn spools alongside messages on the normal hl7 path only.
  A fhir/none-emit run through generate still errors exactly as
  today -- widening the spooler to fhir output is NOT chartered. The
  playable-no-v2-corpus use case is served by redirecting
  `ehrt sim run --format ground-truth > events.edn` and playing that
  file explicitly.

## Steps

1. **Tag ceremony (tag law case i -- licensed here).** The design
   channel has verified the ADR-0099 landing at `3e3c167` by fresh
   public clone. Tag `stable-20260810-fixture-relocation` at
   `3e3c167`, push the tag, verify the peeled ref. If the remote has
   moved past 3e3c167, STOP-AND-REPORT.

2. **Commit 1 -- the fired Deferred row: play's bare reads.** Red
   first, pasted verbatim: the lint with the two allowlist entries
   removed (the ready-made red), plus a live `ehrt play` on a
   chmod-000 file and on a missing file (whoami check first --
   non-root required, ADR-0098 precedent). Fix:
   play-events-from-file/-from-dir get the landed categorized-read
   family shape (mirror ADR-0096/0098's own shapes and category
   names -- read them, do not invent). Co-landed: the allowlist
   entries REMOVED from cli_parse_guard_lint_test.clj in the same
   commit (the gate), plus red->green tests per leg. Green: lint
   passes with the entries gone; live re-runs categorized.
   Commit message (ASCII only):
   `fix: ehrt play bare reads categorized, lint allowlist entries retired (ADR-0100)`

3. **Commit 2 -- producer: events.edn.** spool-sim-output! also
   writes `events.edn` = `(pr-str (:ground-truth payload))` when
   ground-truth is present, alongside msg files, hl7 path only per
   the [C] fence above. Update the spooler docstring (the output-set
   contract). Co-landed tests: out-dir contains events.edn; its bytes
   equal sim-ground-truth-bare-text's output for the same run
   (byte-equality IS the test -- this is what makes
   `cat events.edn | ehrt sim check` work with zero check-side code,
   state that in the docstring); no-messages guard behavior
   unchanged. Verify intake/catalog and the out-dir-exists guard are
   indifferent to the new file (probe, and add a test only if a real
   interaction exists).
   Commit message (ASCII only):
   `feat: generate sim spools events.edn -- the ground-truth log lands on disk (ADR-0100)`

4. **Commit 3 -- the adapter.** In corpus/player.clj: cut the
   timestamp seam -- plan gains an injectable timestamp-extraction
   option whose DEFAULT is the existing MSH-7 path, byte-identical
   (existing tests unchanged and green are the witness; if plan turns
   out to do anything message-specific beyond timestamps,
   STOP-AND-REPORT before reshaping). Event playback: an explicit
   .edn file path in play's dispatch -> guarded EDN read (categorized,
   same family as commit 1) -> shape check (vector of maps carrying
   :type and :t; anything else is :play-input-unsupported with a
   hint) -> plan with a :t-based extraction (seconds -> ms) -> the
   event-line ticker: one line per event, :t rendered as day+hh:mm:ss
   offset from run start, :type, :location when present, citation
   when present -- exact fields from the REAL event shape you read
   in compile-trajectory, not this sketch. Both --ticker modes
   produce the event line for event input (documented in help text).
   --board with event input -> a categorized rejection with a
   named-deferral hint, ADR-0014's own bail-out style. Directory
   input semantics UNCHANGED even when events.edn is present in the
   dir (a test proves it: dir with both msg files and events.edn
   plays messages). Summary envelope unchanged in shape;
   :unparseable-count counts events with missing/invalid :t. Help
   text updated (docs/cli.md regenerates via make docsgen).
   Co-landed tests: hermetic plan-level pacing on synthetic events;
   CLI-level playback of a real generated events.edn (generate sim
   from commit 2, then play it); the rejection legs.
   Commit message (ASCII only):
   `feat: ehrt play reads the sim event log -- native event playback (ADR-0100)`

5. **Demo touch (Q3 a., may ride commit 3 or the close commit --
   your call, disclose which).** busy-tuesday README: one example
   line playing the scenario's own events.edn after generation.

6. **Oracle bracket.** bin/regression-oracle from this session's
   opening tag to the last feature commit. Expected pure identity on
   all 34 roots per Context. Movement = STOP-AND-REPORT.

7. **Full gate.** poly check, full local suite, the CLI parse-guard
   lint (now WITHOUT the play allowlist entries), bin/verify-nist-lock.

8. **Close phase.** FIRST: self-archive this prompt to
   .agents/prompts/. Then: ADR-0100 with red/green evidence verbatim,
   the D7-ruling-4 note, the sniff-blast-radius reasoning, and any
   deviations dated; roadmap: Next row to Done AND the fired Deferred
   row retired (its tripwire -- the allowlist entries -- is gone;
   say so); .agents/rulings.md records the three 2026-08-10 rulings
   ("Q1 a." / "Q2 a." / "Q3 a."); notes/ADRs.md index row;
   notes/adr/README.md count 97 -> 98; session record.
   Commit message (ASCII only):
   `docs: session record and prompt archive -- sim event-log adapter (ADR-0100)`

9. **Push and verify.** Push at each checkpoint per R30. Post-push,
   ASCII check FIRST on every commit message
   (`git log --format=%B -1 <sha> | LC_ALL=C grep -n '[^ -~]'`,
   expected empty), then CI confirmation.

## Fences

- Touch ONLY: components/corpus/{src,test} (player, the spooler's
  home file, and their tests), bases/cli/{src,test}, docs/cli.md
  (generated), demos/scenarios/busy-tuesday/README.md (one line),
  notes/adr/0100-*.md, notes/ADRs.md, notes/adr/README.md,
  .agents/* close-phase files. The sweep RULE governs over this
  list: if executing the chartered work correctly requires a
  path-adjacent edit this list missed, make it minimal, keep it
  within the chartered semantics, and disclose it in the ADR's
  deviations -- ADR-0099's precedent.
- Nothing in kernel, sim, sim-engine, sim-trajectory, sim-emit-hl7,
  oracle, judge src. The sim's own run/check surfaces are UNCHANGED.
- The board's fold and the shared sniff-path-format are UNCHANGED.
- plan's default path is byte-identical: existing player tests pass
  unmodified.
- No history rewrites; deviations in the ADR's dated appendix;
  STOP-AND-REPORT over improvisation; widening (fhir spooling, MLLP,
  stdin event input, sim check file flags) is the author's call, not
  yours -- name wants as findings.
- Channel claims (line numbers, the spooler's home file, payload
  shapes, caller sets) are verify-then-act: re-derive before relying.

## Deviations, dated 2026-08-10

**A channel-inferred claim in this prompt's own Context was wrong,
corrected before any code was built on it.** The Context's own event
shape sketch ("maps with `:type`... `:location`, `:citation`") does
not match the real payload: a live probe (both a bare churn run and a
real module-driven run through `demos/scenarios/busy-tuesday/
config.edn`) found the kind key is `:event`, not `:type` — every
`ehrt.sim-engine.engine/decide` `defmethod` returns an event keyed
`:event`, even though the multimethod itself dispatches on a compiled
step's own `:type` one layer upstream (`ehrt.sim-trajectory.
compile-trajectory`'s output, never returned to a caller directly) —
and `:location` is a map (`{:ward :bed :placement}`), not a bare
string. Step 4's own literal instruction ("shape check... carrying
:type and :t") is corrected to the real key throughout the
implementation, per this prompt's own verify-then-act instruction for
"payload shapes." Full detail, including the exact probe commands and
outputs, in ADR-0100's own Context and Deviations sections.

**`--sink` rejected on event input, beyond Q1 a.'s own literal
scope** (which named only `--board`). A structural necessity, not an
optional widening: `file-sink-fn`/`frame-event` assume ER7 text, and
a compiled event map has no wire framing at all — an unguarded
`--sink` on event input would crash with a raw `ClassCastException`.
Disclosed, mirroring the SAME bail-out style Q1 a. specified for
`--board`, applied to the one other flag sharing the identical
"requires wire-format message input" precondition. Full reasoning in
ADR-0100's own Commit 3 and Deviations sections.

**The live chmod-000/missing-file probes for the fired Deferred row
found the pre-fix tree already clean end to end**, not raw-crashing —
`sniff-path-format`'s own upstream guard masks the second bare read
for any input whose permission state is stable across both reads.
Disclosed as a real, honest finding rather than reported as a live
crash that didn't occur; the lint's own structural red (non-vacuous,
pasted in ADR-0100) and a new witness-pair predicate test are what
prove the fix.

**Demo touch landed on commit 3**, not the close commit — disclosed
per Step 5's own "your call, disclose which."

No other deviations from this prompt's own steps, fences, or rulings.
Every channel-inferred claim not named above (the spooler's true home
in `generators.clj`, `gate-candidate-extensions` excluding `.edn`,
`sniff-path-format`'s own caller set) was verified against the live
tree before being built on and held exactly as stated.
