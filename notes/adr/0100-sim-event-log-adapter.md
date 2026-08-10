## ADR-0100 — Corpus player: the sim's own event log, native playback — and `ehrt play`'s own bare reads, closed

**Status:** Accepted (author-directed, autonomous session per R30), 2026-08-10.

### Context

The roadmap Next row (`.agents/plans/roadmap.md`, "Corpus player: sim
event-log input adapter") anchors this session, named in
`notes/adr/0014-corpus-player.md`'s own Context as remaining work since
2026-07-30: the player is message-native (`plan` takes raw ER7
strings), but the sim's own event log — a richer, pre-emission record
of what actually happened — has never been playable directly. The
fired Deferred row (`ehrt play`'s own bare reads, `notes/ADRs.md`
ADR-0096 Finding 2 / ADR-0097) rides the same session: its own revisit
trigger ("the next session touching `ehrt play` or the corpus-player
slices") fires exactly here.

**What existed before this session, verified:** the sim's event log
had exactly one serialized form — `ehrt sim run --format
ground-truth` prints `(pr-str ground-truth-vector)` to stdout
(`sim-ground-truth-bare-text`, `bases/cli/core.clj`). No event log
landed on disk: `spool-sim-output!` (confirmed live at
`components/corpus/src/ehrt/corpus/generators.clj`, exactly where the
driving prompt's own probe suspected it might be, not
`sim_adapter.clj`) wrote `msg-%03d.hl7` files plus sim's own
`manifest.edn` verbatim, nothing else. `play-events-from-file`/
`play-events-from-dir` (`bases/cli/core.clj`) carried the unguarded
`slurp` shape ADR-0096 Finding 2 named, allowlisted by name in
`cli_parse_guard_lint_test.clj`.

**A channel-inferred claim in this session's own driving prompt was
wrong, corrected in the open before any code was built on it.** The
prompt's Context asserted ground-truth events "are maps with `:type`
(`:admission`/`:discharge`/`:transfer`/`:outpatient-visit`/...), `:t`
... `:location`, `:citation`." A live probe (`ehrt.sim.interface/
run-command`, both a bare churn run and a real module-driven run
through `demos/scenarios/busy-tuesday/config.edn`) found the real
shape different in two ways: the kind key is **`:event`, not
`:type`** — `ehrt.sim-engine.engine/decide`'s own multimethod
dispatches on a compiled step's `:type` (`ehrt.sim-trajectory.
compile-trajectory`'s own output), but every `defmethod decide`
returns an *event* keyed `:event` (e.g. `{:event :admission :t 0
...}`) — and `:location` is a map (`{:ward :bed :placement}`), not a
bare string. Confirmed across every event kind a live probe reached
(`:registered`, `:admission`, `:discharge`, `:transfer` shape read
from `engine.clj` source, `:outpatient-visit`/`:outpatient-visit-end`/
`:observation` from the busy-tuesday probe). Every shape check, the
timestamp seam, and the ticker render below use `:event`, the real
key — the prompt's own verify-then-act instruction for "payload
shapes" governed exactly this correction.

### Decision

**Q1 a. (adapter semantics), author verbatim, 2026-08-10:** native
event playback — events paced by `:t` directly via an injectable
timestamp-extraction seam on `plan` (continuing the `:tty?-fn`/
`:sleep-fn` injection lineage, not a second pacer); a compact
event-line ticker; `--board` under event input REJECTED with a
named-deferral hint (the board's fold is wire-side; feeding it would
need emission parameters the log does not carry).

**Q2 a. (producer side), author verbatim, 2026-08-10:** `corpus
generate sim` also spools the ground-truth vector as `events.edn` into
out-dir, same `pr-str` bytes as `--format ground-truth`'s bare text.
Disclosed against D7 ruling 4's "provenance is the generator's word"
(that ruling governed manifests; `events.edn` is data, not
provenance — it doesn't reopen that ruling's scope).

**Q3 a. (demo touch), author verbatim, 2026-08-10:** busy-tuesday's
README gains ONE "play the sim's own story" example line once the
adapter lands. Nothing else attaches.

**The fired Deferred row (ADR-0096 Finding 2 / ADR-0097):** the
bare-reads fix, with the allowlist entries' removal as its own
co-landed gate.

**[C] The `:sim-events` recognition lives in play's OWN file dispatch**
(extension `.edn` → guarded EDN parse → shape check on the first
element), never the shared `sniff-path-format`. Verified before
acting: fresh grep of `sniff-path-format`'s own caller set
(`sniff-files` — used by both `sniff-gate-command` and
`play-events-from-dir`; `sniff-gate-command` itself, the bare `gate`
dispatch; `show-file`; `play-events-from-file`) confirms it is shared
by `gate`/`show`, neither of which any ruling licenses to change
behavior on a `.edn` path. Widening the shared sniff to recognize
`.edn` would make a `.edn` file newly classifiable by `gate`/`show`
too — out of scope. Extension-based recognition, entirely inside
`play-command`'s own dispatch, touches neither.

**[C] The no-messages guard is unchanged:** `events.edn` spools
alongside messages on the normal hl7 path only. A fhir/none-emit run
through `generate` still errors exactly as today — widening the
spooler to fhir output was not chartered. The playable-no-v2-corpus
use case is served by redirecting `ehrt sim run --format ground-truth
> events.edn` and playing that file explicitly.

### Commit 1 — the fired Deferred row: `ehrt play`'s own bare reads

**Red, live, before any fix (`whoami`: `mg`, `id -u`: 1000, non-root):**
the lint with the two allowlist entries emptied reported exactly
`play-events-from-file`/`play-events-from-dir`, non-vacuous, matching
ADR-0096's own original finding:

```
FAIL in (no-unguarded-cli-parse-read-outside-a-try-test)
expected: (empty? violators)
  actual: (not (empty? ("play-events-from-file" "play-events-from-dir")))
```

**A genuine, disclosed finding from the live probe pass:** `ehrt play`
against a chmod-000 single file, a missing file, and a directory
containing one chmod-000 file among readable ones, ALL already
returned a clean categorized error (`:path-unreadable`/
`:gate-path-not-found`) on the pre-fix tree — not a raw exception.
The reason: both `play-events-from-file` and `play-events-from-dir`
call the already-guarded `sniff-path-format` FIRST (to sniff the v2
format), which fully reads the file and fails cleanly on a permission
or missing-path error before either function's own SECOND, unguarded
`slurp` is ever reached. The bare-read gap ADR-0096 Finding 2 named is
real and exactly matches the lint's own structural check (a `try` in a
different top-level defn — `sniff-path-format`'s own — guards nothing
inside `play-events-from-file`'s own form), but it is not, on this
session's own live probes, independently reproducible via a simple
permission-denied trigger: the upstream guard masks it for any
input whose permission state is stable across both reads. This is
disclosed rather than assumed reproducible; the fix (wrap the second
read too) closes the structural gap regardless of live reachability
today — the same defense-in-depth reasoning a lint exists for.

**Fix:** `slurp-play-input` (`bases/cli/core.clj`) wraps `(slurp f)`
in `try`/`catch`, `result/error :play-input-unreadable {:path
:message}` — the same shape `read-base-data`/`sniff-path-format`
already use. Both call sites (`play-events-from-file`'s own second
read, `play-events-from-dir`'s own per-file map) route through it.

**Co-landed gate:** the two allowlist entries removed from
`cli_parse_guard_lint_test.clj`; the mechanism itself (the now-empty
`allowlisted-fn-names` var and its filter step) retired along with
them, since nothing remains allowlisted. A new witness-pair test,
`violation-predicate-reproduces-the-play-charter-sites-test`, proves
both former sites' own reduced structural shape trips pre-fix and
does not post-fix — the same git-history-independent regression proof
the four original charter sites already have, extended to these two
(the real live trigger being masked upstream, per the finding above,
this predicate-level proof is the actual regression gate, not a live
behavioral test).

**Green:** lint passes with the entries gone (`ehrt.cli.cli-parse-
guard-lint-test`: 4 tests, 22 assertions, 0 failures, 0 errors). Live
re-runs post-fix: identical output to pre-fix (both already clean,
confirming the fix changes nothing observable end-to-end today, only
the structural guarantee). `ehrt.cli.core-test`: 259 tests (unchanged
count — no new behavioral test needed for the reasons above), 762
assertions, 0 failures, 0 errors.

**Commit:** `fix: ehrt play bare reads categorized, lint allowlist entries retired (ADR-0100)` (`e005702`).

### Commit 2 — producer: `events.edn`

`spool-sim-output!` (`components/corpus/src/ehrt/corpus/
generators.clj`) also writes `events.edn` = `(pr-str ground-truth)`
when the run payload carries `:ground-truth`, alongside the message
files, on the same hl7-only branch the no-messages guard already
gates — a fhir/none-emit run still returns `:sim-produced-no-messages`
exactly as before, `events.edn` never attempted.

**Red, before the fix:** the two new co-landed tests
(`ehrt.corpus.generators-test/sim-execute-fn-events-edn-is-pr-str-of-
ground-truth-test`, `ehrt.cli.core-test/generate-sim-command-
events-edn-matches-format-ground-truth-bare-text-test`) both failed on
the pre-fix `generators.clj` (a `git stash push --keep-index` isolated
the one `src` fix from its own new tests): `ehrt.corpus.generators-
test` — 2 failures, 2 errors (a raw `FileNotFoundException` slurping
the never-written `events.edn`); `ehrt.cli.core-test` — 1 failure, 1
error, same mechanism.

**Green, after `git stash pop` restored the fix:** `ehrt.corpus.
generators-test` — 18 tests, 44 assertions, 0 failures, 0 errors.
`ehrt.cli.core-test` — 260 tests, 766 assertions, 0 failures, 0
errors. The CLI-level test is the real byte-equality claim end to end:
a real `generate-sim-command` run's own `events.edn` compared,
byte-for-byte, against `sim-run-command {:format "ground-truth"}`'s
own `:bare-text` metadata for the SAME seed/patients — not just the
unit-level `pr-str` assertion.

**Intake/catalog indifference, probed live, not assumed:** a real
`generate sim` out-dir (hl7 messages + `manifest.edn` + `events.edn`)
run through `ehrt.corpus.intake/intake!` catalogs `events.edn` exactly
the way it already catalogs `manifest.edn` — `:format :unknown`,
`:file-count 4`, no crash, no special interaction. The shared
`out-dir-exists?` guard is a pre-check against the out-dir's existence
before generation ever runs, structurally indifferent to what
`spool-sim-output!` later writes. No new test added for either, per
the driving prompt's own "add a test only if a real interaction
exists" instruction — none does.

**Commit:** `feat: generate sim spools events.edn -- the ground-truth log lands on disk (ADR-0100)` (`56e75da`).

### Commit 3 — the adapter

**`plan`'s own injectable timestamp seam** (`components/corpus/src/
ehrt/corpus/player.clj`): a new `:timestamp-fn` option, defaulting to
`message-timestamp-ms` (the original MSH-7 path) — every existing
`player-test` deftest, unmodified, stays green (31 tests, 51
assertions including six new ones for the seam itself, 0 failures, 0
errors), the byte-identical-default witness the driving prompt
required. `plan` itself does nothing message-specific beyond calling
this one function — confirmed by reading the whole function body
before editing; no STOP-AND-REPORT triggered.

**`event-timestamp-ms`** (same namespace): a ground-truth event's own
`:t` (seconds from the sim run's own epoch, sim/ADR-0011), scaled to
ms; `nil` for a missing or non-numeric `:t` — the event-input
counterpart to `message-timestamp-ms`, exported through
`ehrt.corpus.interface`.

**Event recognition, `bases/cli/core.clj`:** `play-command`'s own
dispatch gains a third branch — a non-directory path whose name ends
in `.edn` routes to `play-events-from-edn-file` (guarded EDN read via
`slurp-play-input`, one `try` wrapping both the slurp and the
`edn/read-string`, `:play-input-unreadable` on either failure) → a
shape check (a non-empty vector of maps, the first carrying both
`:event` and `:t`; anything else is `:play-input-unsupported` with a
hint). Directory input is untouched by construction:
`gate-candidate-extensions` (`#{"json" "hl7"}`) already excludes
`.edn`, so a directory scan never sees `events.edn` sitting beside
message files — proven by a co-landed test (a directory with both a
real `.hl7` message and an `events.edn` plays exactly the one
message).

**The event-line ticker:** one line per event — a day+HH:MM:SS offset
computed directly from `:t` (already an offset from the run's own
epoch; no subtraction needed), the event kind (`:event`), `:location`
when present (`ward/bed`), `:citation` when present
(`[module/state]`). Both `--ticker full` and `--ticker line` render
this SAME line for event input (co-landed test), since there is no
wire-format "full" rendering for a compiled event — the mode
distinction that matters for message input collapses to one rendering
here, documented in help text.

**`--board`/`--sink` rejections:** `--board` on event input rejects
`:play-board-unsupported-for-events` per Q1 a., exactly as ruled.
**A judgment call, disclosed, not separately ruled:** `--sink` on
event input ALSO rejects, `:play-sink-unsupported-for-events` — not
explicitly named in Q1 a. (which named only `--board`), but a
structural necessity: `file-sink-fn`/`frame-event` assume ER7 text
(`.getBytes ^String event`), and a compiled event map has no wire
framing at all — an unguarded `--sink` on event input would crash
with a raw `ClassCastException`, the exact bare-exception shape this
whole session's own Deferred-row half exists to close. The rejection
uses the identical bail-out style Q1 a. specified for `--board`
(a named category, a hint explaining the deferral), applied to the
one other flag that shares the same "requires wire-format message
input" precondition. Minimal, within chartered semantics, disclosed
per the sweep rule rather than left to crash.

**Summary envelope:** `first-ts`/`last-ts` (feeding `:stream-span-ms`)
now use the SAME per-input timestamp-fn `plan` was given, not a
hardcoded `message-timestamp-ms` — the envelope's own key set is
unchanged for event input (a co-landed test asserts the identical key
set message input already carries). `:unparseable-count` counts
events with a missing/invalid `:t` automatically, via the same `plan`
mechanism that already counts an unparseable MSH-7 — no special-case
needed.

**Help text (`bases/cli/help.clj`):** `--ticker`/`--board`/`--sink`'s
own `:doc` strings, and `PATH`'s own `positional-doc`, updated to name
both input shapes and the event-input-only rejections, voice-gate
clean (no `ADR-NNNN`/`D9`-style citation — `help-voice-test`,
`help-wrap-test` both re-run green: 2/167, 13/2486 assertions).
`docs/cli.md` regenerated via `make docsgen`; `git diff --stat`
confirms ONLY `docs/cli.md` actually changed bytes (the other three
`make docsgen` targets — `pipeline.md`, `use-cases.md`,
`operators.md` — reproduced byte-identical, unrelated to this
session's own diff, confirmed via `git status` before staging).

**Red, before the fix:** with only the test files present (`git
stash push --keep-index` isolating every `src` change — `core.clj`,
`help.clj`, `interface.clj`, `player.clj`, `docs/cli.md`), `ehrt.cli.
core-test` reported 20 failures/2 errors across every new event-log
test; `ehrt.corpus.player-test` failed to COMPILE at all (`No such
var: player/event-timestamp-ms`) — both confirm the tests exercise
real, new surface, not vacuous assertions.

**Green, after `git stash pop`:** `ehrt.corpus.player-test` — 31
tests, 51 assertions, 0 failures, 0 errors. `ehrt.cli.core-test` — 272
tests, 800 assertions, 0 failures, 0 errors (twelve new deftests:
pacing-by-`:t`, the event-line render, both ticker modes identical,
unparseable-`:t` counting, malformed EDN, empty-vector, wrong-shape,
`--board` rejection, `--sink` rejection, the summary-envelope key set,
directory-input-unaffected, and a real end-to-end
`generate-sim-command`-then-`play-command` round trip on a live sim
run). `ehrt.cli.help-test`/`help-voice-test`/`help-wrap-test`/
`cli-parse-guard-lint-test` all re-confirmed green at this commit too.

**Live-probed, not just tested (`bin/ehrt`, the real executable):** a
real `busy-tuesday` config generated a small `events.edn`
(3 patients, 6 events); playing it end to end via `bin/ehrt play`
rendered real event lines including a real `:citation`
(`[sore_throat/:doctor-visit]`, confirming `:state` values are
sometimes keywords, handled by `str` either way) and a genuine
multi-year `:t` offset (`1287d 00:05:00`); `--board`/`--sink` both
rejected live with the categorized hint; a directory containing both
the real message files and `events.edn` played only the messages,
confirmed by the rendered ER7 content.

**Demo touch (Q3 a.), riding this commit, disclosed here rather than
the close commit:** `demos/scenarios/busy-tuesday/README.md` gains one
example playing `out/scenarios/busy-tuesday/events.edn` directly,
right after the existing `--board` example.

**Commit:** `feat: ehrt play reads the sim event log -- native event playback (ADR-0100)` (`3e49932`).

### Oracle bracket

`bin/regression-oracle stable-20260810-fixture-relocation 3e49932`
(this session's own opening tag to the last feature commit): soundness
check IDENTICAL outside `digest.clj`'s own `(ns ...)` form; all
THIRTY-FOUR roots' SHA-256 digests IDENTICAL between baseline and
target — matching the driving prompt's own expectation exactly:
`ehrt.oracle.digest` requires only sim-trajectory/sim-model/
sim-engine/emit-hl7 interfaces and runs its own golden runs, never
touching `components/corpus` or `bases/cli` (this session's entire
footprint), and generate-out-dirs are never digested.

### Verification

- `clojure -M:poly check`: OK, at every commit and at this close.
- Full local suite (`clojure -M:poly test :all skip:integration`),
  full unredirected capture: 0 failures, 0 errors anywhere in the log
  (grepped in full — 592 occurrences of "0 failures, 0 errors", zero
  of any other count), 3m59s.
- `ehrt.cli.cli-parse-guard-lint-test`: 4 tests, 22 assertions, 0
  failures, 0 errors — the allowlist gone, every function in
  `core.clj` covered with no exemption.
- `bin/verify-nist-lock`: OK, 6/6 hit-nexus-sourced coordinates match
  `artifacts.lock.edn` exactly.
- Oracle bracket: pure identity, all 34 roots (above).
- `gitleaks git --staged -v`: clean at every commit.
- Tag verification: `stable-20260810-fixture-relocation` (Step 1, this
  session) tagged at `3e3c167` (confirmed identical to `origin/main`
  and this session's own working-tree HEAD before any tool call),
  pushed, peeled ref resolves to `3e3c167` exactly.
- Post-push message verification and the ASCII check (`git log
  --format=%B -1 <sha> | LC_ALL=C grep -n '[^ -~]'`), all three fix
  commits: empty (clean) — no wrapper mangling.
- Last five `test`-lane runs on `main` (`gh run list --limit 5
  --branch main`), checked at Step 0 (before this session's first
  commit): all `completed`/`success` (`31385394301`, `31384612590`,
  `31369714669` [an `Integration` schedule run, not a `test`-lane
  push], `31351608040`, `31351267585`).
- `git status --porcelain`: clean before this session's first tool
  call.

### Fences

Touched exactly the list the driving prompt named: `components/corpus/
{src,test}` (`player.clj`, `interface.clj`, `generators.clj`, their
tests), `bases/cli/{src,test}` (`core.clj`, `help.clj`, `core_test.clj`,
`cli_parse_guard_lint_test.clj`), `docs/cli.md` (generated),
`demos/scenarios/busy-tuesday/README.md` (one example), plus this ADR
and the close-phase files below. Nothing in `kernel`, `sim`,
`sim-engine`, `sim-trajectory`, `sim-emit-hl7`, `oracle`, `judge` src —
the live probes that established the real ground-truth event shape
(`:event`, `:location` as a map) READ `ehrt.sim-engine.engine`/
`ehrt.sim-trajectory.compile-trajectory` source and ran real sim
invocations; they never edited either. The board's fold and the
shared `sniff-path-format` are unchanged (verified structurally,
above). `plan`'s default path is byte-identical: every existing
`player-test` deftest passed unmodified, at every commit.

### Deviations, dated 2026-08-10

1. **The Context's own channel-inferred event shape (`:type`) was
   wrong; corrected before any code was built on it.** See Context,
   above — the real key is `:event`; `:location` is a map. Every
   shape check, the timestamp seam, and the ticker use the real,
   live-probed shape. The driving prompt's own verify-then-act
   instruction for "payload shapes" is what licensed re-deriving this
   rather than building on the stated sketch.
2. **`--sink` rejected on event input, a judgment call beyond Q1 a.'s
   own literal scope** (which named only `--board`). Disclosed in
   Commit 3, above, with the structural reasoning (a raw
   `ClassCastException` otherwise) and the precedent it mirrors (the
   SAME bail-out style Q1 a. specified for `--board`). Within
   chartered semantics — no widening (no `:sink` implementation for
   events, only its categorized rejection) — per the sweep rule.
3. **The live chmod-000/missing-file probes for the fired Deferred row
   found the pre-fix tree already clean end to end**, not raw-crashing
   as a literal reading of "the ready-made red" might suggest — the
   upstream `sniff-path-format` guard masks the second bare read for
   any stable-permission input. Disclosed rather than reported as a
   live crash that didn't occur; the lint's own structural red (the
   actual, non-vacuous tripwire) and a new witness-pair predicate test
   are what prove the fix, per Commit 1 above.

No other deviations from the driving prompt's own steps, fences, or
rulings.

### Consequence

`ehrt play` reads the sim's own event log natively: `ehrt sim run
--format ground-truth`'s bare stdout, or `ehrt corpus generate sim`'s
own `events.edn` (now spooled on disk, byte-identical to that same
bare text), plays with the same pacer, the same injection discipline,
and a purpose-built compact ticker — no second implementation of time.
`ehrt play`'s own two-session-old bare-read gap (ADR-0096 Finding 2)
closes on the same categorized-read shape the rest of this codebase
already uses, its lint allowlist retired. `ehrt.corpus.player.md`'s
own ADR-0014 Context — "a sim event-log input adapter... future
work" — is fulfilled; the sim event-log adapter row leaves the
roadmap's own Next section. `--mllp`, accumulator-into-`--board`
wiring for event input, and stdin event input all remain named,
disclosed, out of this session's own scope. The oracle holds pure
identity across all 34 roots — this session touched CLI-shell code,
the pure player core, and CLI tests only, never sim/engine-path work.

### Index line

```
- 2026-08-10 — sim-event-log-adapter — ADR-0100
```

(appended to `.agents/plans/roadmap.md`'s own Done section, alongside
the Next-row removal and the Deferred-row retirement this same commit
makes.)

`notes/adr/README.md`'s own file count corrects 97→98, verified by
`ls notes/adr/*.md | grep -v README | wc -l`, not arithmetic.
