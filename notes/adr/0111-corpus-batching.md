## ADR-0111 — Corpus batching: the transport gets one notch real

**Status:** Accepted (author-directed, autonomous session per R30), 2026-08-11.

### Context

Two author rulings, 2026-08-11 (`.agents/rulings.md`, this ADR's own
entry): the batcher is a CORPUS-LEVEL tool, separate from the sim,
working on any directory of valid v2 message files including foreign
corpora (Q1 a, verbatim: *"It should work on any corpus, even an
existing directory of foreign (but valid) message files."*); the HL7
v2 batch protocol's BHS/BTS wrappers land as a `:batch` framing codec
in v1 (Q2 a, verbatim: *"Go."*). One session: mechanism + demo,
composing with the latency arc (ADR-0109/ADR-0110) this session's own
driving prompt named as chartering context, not a dependency --
batching and delayed-transmission are two independent, composable
transport realisms.

### Tag ceremony

The design channel verified the ADR-0110 landing at `b5b9b9e` by fresh
public clone (this session's own preflight: `git fetch` confirmed
`origin/main` already at `b5b9b9e`; the last five CI runs on `main`
were all `completed`/`success`). `stable-20260811-latency-demo` tagged
annotated at `b5b9b9e`; pushed; peeled ref confirmed
`b5b9b9e2e3664624c0126de44ae88f2980712c17` -- exact match.

### Decision

**[A] The partition fn** (`ehrt.corpus-io.batch/partition-messages`,
corpus-io, pure): tagged messages (`{:message :source}`, `:source` any
caller label -- ordinarily a filename) sort GLOBALLY by MSH-7 transmit
time (never per-file), then partition into epoch-aligned, half-open
buckets: bucket `k` spans `[k*interval-ms, (k+1)*interval-ms)` against
the Unix epoch, so hourly batches align to the clock hour and daily
batches to UTC midnight -- matching real-world delivery schedules
without any caller-supplied anchor. Empty buckets are never present in
the result (skipped entirely, v1). A message whose own MSH-7 doesn't
parse is `kernel/error :unparseable-transmit-time`, naming the
offending `:source`, fail-fast before any sorting or bucketing --
the author's own premise is a foreign-but-VALID corpus, so an
unparseable transmit time is a defect to surface, never a silent skip.

**[A] The `:batch` codec** (`ehrt.corpus-io.framing`, D2's codec-only
doctrine, pure bytes, same encode/decode call shape as its siblings):
`encode` wraps `items` (raw ER7 message byte arrays, the same shape
`:er7-multi`'s own items) as `BHS|^~\&` + a message separator +
`:er7-multi`-encoded messages + `BTS|<true count>` + a trailing
separator. `decode` strips both wrappers, VERIFIES `BTS-1` against the
actual decoded message count (`kernel/rejected :batch-count-mismatch`
on a mismatch -- a free transport-integrity check, exercised by the
CLI's own self-check on every write, not merely offered) and yields
the messages. Byte-exact like `:er7-multi`/`:mllp`/`:ndjson`: BHS/BTS
are read/written as fixed ASCII structural bytes only (`"BHS"`,
`"BTS"`, `"|"`, ASCII digits for `BTS-1`), no `java.lang.String`
conversion anywhere in `encode-batch`/`decode-batch`, the same charset
law this namespace's other byte-exact codecs already state.

**Field choices, minimal and deterministic.** BHS carries only its own
field-separator/encoding-characters pair (`BHS|^~\&`, the same
2-populated-field shape a bare MSH head would have) -- no creation-time
field is populated at all, so the determinism law (no wall clock
anywhere) is satisfied trivially rather than by threading a batch
window's own boundary time through a call shape (`encode`/`decode`)
that stays uniform with every sibling codec. BTS carries exactly
`BTS-1`, computed from `items` itself by `encode`, never a
caller-supplied number that could drift from reality. FHS/FTS
(file-level wrappers) are a NAMED DEFERRAL -- not built.

**`:batch` is NOT one of `ehrt.corpus-io.source-sink/Framing`'s closed
five kinds**, deliberately: that schema, and `docs/dev/
source-sink-design.md` Part II's own "the five framing kinds" prose,
are both out of this session's own fence (not touched). `:batch` lives
purely in `framing.clj`'s own `known-framings`/`decode`/`encode`
dispatch and is called directly (`corpus-io/encode :batch items`),
the same way `ehrt.corpus.player/frame-event` already calls `:er7-multi`
directly without ever constructing a Source/Sink map -- the batcher
never constructs one either.

**Move-don't-improve micro-relocation.** The partition fn needs MSH-7
extraction; the one existing implementation
(`message-timestamp-ms`/`parse-dtm-lenient`/`message-type-trigger`/
`message-patient-id` and their private helpers) lived in
`ehrt.corpus.player`, a `corpus` namespace -- and `corpus-io` may never
require `ehrt.corpus.*` (the corpus-io interface's own AR-2 directional
rule; `corpus` already requires `corpus-io`, the opposite direction).
The entire "lenient segment/field reads" block moved, byte-identical,
to a new `ehrt.corpus-io.er7-fields`; `ehrt.corpus.player` re-exports
the same four public names as plain `def`s pointing at the moved
implementation. No behavior change: `ehrt.corpus.player-test`'s own 24
pre-existing deftests over these functions ran unmodified, green,
proving the move byte-identical, rather than duplicated tests being
written at the new location for functions that didn't change.

**[A] The CLI leg**: `ehrt corpus batch DIR --interval MINUTES
--out-dir OUT` (`bases/cli`, alongside `mutate`/`intake`/`operators`
in the `corpus` group). Reads every `*.hl7` candidate file directly
under `DIR` (`files-with-extension-in`, the same helper `corpus
mutate` already uses), splits multi-message files via
`ehrt.corpus.player/split-er7-multi` (`ehrt play`'s own directory-input
machinery, reused, never re-split a second time), tags each message
with its own filename, and hands the full cross-file seq to
`partition-messages`. For each occupied bucket, writes
`batch-NNN.hl7` (sequential, 0-based, over OCCUPIED buckets only --
never the bucket's own absolute epoch index, a deliberately simpler v1
choice than encoding gaps into the numbering) via `:batch` framing, raw
bytes (`io/output-stream`, no charset conversion at the file-write
boundary either), then immediately decodes what it just wrote back
in-memory and confirms the message count before ever reporting success
-- the codec's own free transport-integrity check, exercised, not
merely trusted.

`--interval` is REQUIRED (a plain integer count of minutes -- see
"Interval spec," below) and must be positive; no sensible universal
default schedule exists to assume (D8's determinism law governs
defaults, not requiredness -- the same "determinism is a feature, not
a default" reasoning `sim run --seed` already uses). `--out-dir`
defaults to `<DIR>-batches/` (D12's derived-out-dir pattern) and is
rejected `:out-dir-exists` when it already exists and is non-empty
(the same guard `generate-sim-command`'s own zero-flag determinism
story uses) -- never silently overwritten. Categorized reads, by name:
missing `DIR` (`:file-not-found`), no `*.hl7` candidates
(`:batch-input-empty`), an unreadable or malformed candidate file
(`:batch-input-unreadable`/`:batch-input-malformed`, both naming the
file), an unparseable MSH-7 (`partition-messages`'s own
`:unparseable-transmit-time`, propagated, naming the file) -- fail
fast throughout, per the author's own foreign-but-valid premise.
Unknown-flag rejection is entirely spec-derived (`help/cli-spec`'s new
`batch` verb entry): no dedicated validation code, the same mechanism
every other verb's own flag surface already gets.

**Interval spec, decided and disclosed.** No existing CLI flag in this
repo uses a unit-suffixed duration grammar (`15m`/`1h`/`24h`) anywhere
-- every duration-shaped flag (`--rate`, `--idle-cap`,
`--arrival-gap`, `--warm-up-seconds`) is a bare integer in one fixed,
named unit. `--interval` follows that established convention: a plain
integer count of minutes, matching `--arrival-gap`'s own closest
sibling shape, rather than inventing an unprecedented grammar this one
flag alone would carry.

**[A] The demo**: `demos/scenarios/ed-tuesday/README.md` gains
"Batched delivery" -- the batcher run over the SAME
`out/scenarios/ed-tuesday-latency` out-dir the "second clock" section
already generates, `--interval 60` (hourly). Witnessed: 283 messages,
34 occupied hourly buckets spanning `2026-08-11T00:00Z` through
`2026-08-12T13:00Z`, every one of the 34 written files self-verified
(`:verified true`); an interior gap (two empty hours,
`08:00Z`-`10:00Z` on 2026-08-12) is visibly absent from the batch
sequence, illustrating the interior-empty-batch deferral live rather
than only in prose. A straddling encounter: Smith, James (MRN000002,
bed ED-H05) -- admitted (A01, MSH-7 `2026-08-11T00:30:26Z`) lands in
`batch-000.hl7` (3 messages); discharged (A03, MSH-7
`2026-08-11T01:34:19Z`) lands in `batch-001.hl7` (4 messages), one
clock-hour later. Both files individually self-verify clean
(`BTS-1` = 3 and 4 respectively, matching their own real message
counts exactly) -- the lesson (the author's own charter, ADR-0107/
ADR-0109, restated for batching): transport-level completeness (every
`BTS-1` count checks out) says nothing about clinical-level
completeness (the encounter's own full record set spans batches).
Determinism witnessed directly: two independent `ehrt corpus batch`
runs over the same out-dir at the same `--interval` produced
byte-identical output directories (`diff -rq`, empty).

**[C] The taxonomy note.** Transport realism (delayed individual
transmission, ADR-0109; schedule batching, this ADR) simulates CORRECT
transport behaviors, deterministically; mutation (`ehrt corpus mutate`)
injects INCORRECT content with an expected finding. Message loss and
duplication sit on the boundary (a real transport does both) -- a named
future taxonomy question, not resolved here; recorded origin: the
author's own "mutation as imperfect transport" framing from this
session's driving conversation (paraphrased here -- the literal words
were not carried into this session's own written context, only this
paraphrase; not presented as a verbatim quote).

### Named deferrals (v1)

- **`--anchor`**: bucket alignment is always against the Unix epoch;
  an alternative caller-supplied anchor is not built. Revisit trigger:
  a future session with a concrete need for a non-epoch-aligned
  schedule (e.g. a facility's own local-midnight cutover at a
  non-UTC-round offset).
- **Interior empty-batch realism**: an empty bucket between two
  occupied ones is skipped entirely (no file, no placeholder, no gap
  marker) -- a receiver never sees "batch N+1 is missing," only that
  the next file it receives is numbered one higher than the last with
  no visible sequence break (v1's sequential-over-occupied-only
  numbering, disclosed above). Revisit trigger: a future session
  wanting to simulate a receiver noticing a missing scheduled delivery.
- **FHS/FTS** (file-level wrappers, the batch protocol's own next tier
  up from BHS/BTS): not built. Revisit trigger: a future need to
  simulate multiple batches bundled into one file-level transfer.

### Oracle bracket

**Pre-analysis:** pure identity on all 35 roots was the prediction --
this session's own footprint is corpus-io + cli + demo docs only; no
`sim`/`emit`/engine `src` change anywhere (the micro-relocation moves
`ehrt.corpus.player`'s own lenient-read block within `corpus`/
`corpus-io`, neither of which is a sim-family brick).

**Bracket result.** `bin/regression-oracle b5b9b9e d1f8fa1`
(`d1f8fa1`: this session's own demo-commit checkpoint, run before the
close-phase commit, per the driving prompt's own step ordering):
`IDENTICAL: every root's digest matches between b5b9b9e and d1f8fa1` --
all 35 roots, matching the pre-analysis; no STOP-AND-REPORT needed.

### Full gate

`clojure -M:poly check`: OK. Component-scoped runs during the build
(`component:corpus-io`, `component:corpus`, `component:cli`, each
composing the `conformance`/`ehrt-cli` projects): zero `FAIL`/`ERROR`
across 616 test-namespace "0 failures, 0 errors" occurrences per run.
New coverage: `ehrt.corpus-io.batch-test` (7 tests, 20 assertions),
`ehrt.corpus-io.framing-test`'s own 8 new `:batch` cases (round-trip
property, empty-items, a byte-exact concrete example, the charset law,
a tampered-count mismatch, three malformed-frame legs), `ehrt.cli.
core-test`'s own `batch-command`/dispatch coverage (missing path,
required/positive `--interval`, empty/unreadable/malformed candidates,
unparseable MSH-7 naming the file, `--out-dir` collision, unknown-flag
routing, a two-file cross-file-ordering happy path, interior-gap
sequential numbering, derived out-dir, single-file input, multi-message
split) -- `ehrt.cli.core-test` overall: 289 tests, 853 assertions, all
green. Two pre-existing coverage tests updated in place, not newly
authored: `dispatch-unknown-corpus-action-names-its-verbs-test` and
`spec-command-pairs-match-dispatchs-known-routes-test`/`stub-key`
(`help_test.clj`) now include `["corpus" "batch"]`, the same shape
every prior verb addition to this spec required. `make docsgen`:
`docs/cli.md` regenerated, the new `ehrt corpus batch` section the only
delta (pipeline.md/use-cases.md/operators.md byte-unchanged). Full
local suite (`make test`: `clojure -M:poly check` + `clojure -M:poly
test :all skip:integration` + `bin/verify-nist-lock`): OK, zero
`FAIL`/`ERROR` anywhere. `ehrt.cli.cli-parse-guard-lint-test` and
`ehrt.docs-tooling.sim-purity-lint-test`: both confirmed green within
that same run. `gitleaks git --staged -v` (pre-commit, each checkpoint)
and `gitleaks detect` (pre-push): no leaks found.

**The last five `main` CI runs** (`gh run list --limit 5 --branch
main`, checked at session start): all `completed`/`success` --
`b5b9b9e` (ADR-0110 fix-forward session-record, 3m31s), `2faa5ba`
(ADR-0109 session-record close, 4m45s), `d6ed674` (ADR-0108 doc
landing, 4m30s), `62d1d5e` (ADR-0108 architecture doc, 4m41s),
`5a2832f` (ADR-0107 CI-flake disclosure, 4m17s) -- no red among the
five.

### Fences

Touched: `components/corpus-io/{src,test}` (new `batch.clj`/
`er7-fields.clj`/`batch_test.clj`; `framing.clj`/`framing_test.clj`/
`interface.clj` extended), `components/corpus/src/ehrt/corpus/
player.clj` (the micro-relocation only -- re-exports, no new
capability), `bases/cli/{src,test}` (`core.clj`/`help.clj`/
`core_test.clj`/`help_test.clj`), `docs/cli.md` (generated),
`demos/scenarios/ed-tuesday/README.md` (the new subsection), `demos/
scenarios/README.md` (one owed line), `notes/adr/0111-*.md`, `notes/
ADRs.md`, `notes/adr/README.md`, `.agents/*` close-phase files. ZERO
`sim`/`emit`/engine `src` change anywhere; the scenario `config.edn`/
`config-latency.edn` untouched -- the demo reuses the existing
out-dir's own already-documented generate command, never regenerating
it. No RNG anywhere in the new code (deterministic by design): two
independent `ehrt corpus batch` runs over the same input produced
byte-identical output, witnessed by `diff -rq`, not merely asserted.

### Deviations, dated 2026-08-11

- **`:batch` deliberately NOT added to `ehrt.corpus-io.source-sink/
  Framing`'s closed enum**, disclosed above under "Decision" -- a
  design-doc-adjacent choice made because `docs/dev/
  source-sink-design.md` itself is out of this session's own fence
  (not a file this session's prompt licensed touching), and nothing
  in the batcher's own design requires constructing a Source/Sink map
  at all. Not a deviation from any ruling; a scope-respecting
  implementation choice, recorded so a future session extending
  Source/Sink to `:batch` finds this note first.
- **The taxonomy note's own "mutation as imperfect transport" framing**
  is recorded above as a paraphrase, not a verbatim quote -- the
  literal words were named in this session's own driving prompt as
  "the author's own... framing from the driving conversation" but the
  literal text itself was not carried into this session's own written
  context. Disclosed rather than fabricating quotation marks around
  invented text.

### Index line

```
- 2026-08-11 — corpus-batching — ADR-0111
```

(appended to `.agents/plans/roadmap.md`'s own Done section; a new
"Transport realism — batching" row lands in the Deferred section
naming the three named deferrals above, each with its own revisit
trigger.)
