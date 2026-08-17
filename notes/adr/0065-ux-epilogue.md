## ADR-0065 — UX epilogue: muscle memory gets an answer, help gets a width

**Status:** Accepted (author-directed, autonomous session per R30), 2026-08-06.

### Context

Prior: the UX arc closed and was design-channel-verified (`2e77096`,
`notes/adr/0064-ux-arc-close.md`). This session exists because the
founding incident's own command shape resurfaced live the same day,
from the author's own shell: `clojure -M:cli run --seed 42 ...
--config ./config/busy-weekday.edn ...` → `FileNotFoundException ...
run (No such file or directory)`.

Diagnosis, probe-backed against the live tree before any code changed
(per this repo's own verification discipline): the file not found is
`run`, NOT the config. Fresh grep of the root `deps.edn` found no
`:cli` alias at all; `git log -S':cli' -- deps.edn` returned zero
commits — the alias never existed in the monorepo root. `clojure
-M:cli` therefore drops the undeclared alias silently and
`clojure.main` treats the bare positional `run` as an init-script
path, throwing `FileNotFoundException` instead of anything
diagnostic. The alias DID exist pre-monorepo: `git show
906a954:.staging/deps.edn` carries `:cli {:main-opts ["-m"
"ehr-testing-sim.cli"]}`, its own comment literally teaching the
`clojure -M:cli run --seed 42 ...` form. It died silently at monorepo
consolidation (2026-07-28) — never carried over, never tombstoned.
ADR-0059 (ux fixes 1, AR-U1-2) swept the taught form from every live
doc surface and gated the sweep (`invocation-lint-test`); that closed
the DOCS half of this gap. The RUNTIME half — the alias itself — was
never addressed: founding failure #1, at a layer the UX arc's own
audit register never named. The session prompt's own `[unverified-by-
live-run]` link in this diagnosis chain — `clojure.main`'s own
script-load behavior for an unresolved alias's bare positional
argument — is CLOSED by this session's own Step 2 live probe, below:
the sandbox that authored the diagnosis could not resolve Maven
Central to reproduce it directly; the author's own pasted transcript
was the outcome evidence, and the fixed tree's own live probe (same
section) now closes the loop the sandbox couldn't.

Author rulings, recorded verbatim (this session's own driving
prompt):

### Decision

**AR-EP-0 (tag, standing ceremony).** Annotated `stable-20260806-ux-close`
at `2e77096`, message "ux arc closed, design-channel-verified
2026-08-06 (ADR-0064)"; pushed; peeled ref verified — resolves exactly
to `2e77096`. This discharges the mechanical debt the archived close
prompt recorded — the debt was named in
`.agents/prompts/2026-08-06-ux-arc-close.md` ("the next arc's opening
session tags `stable-20260806-ux-close` at this tip under standing
ceremony") ONLY, never in ADR-0064 itself (fresh grep of ADR-0064: zero
hits for the tag name) — a citation-precision miss this session
corrects rather than repeats (AR-EP-5, below).

**AR-EP-1 (the tombstone).** Ruled: reject the retired `clojure
-M:cli` shape by name, exit 2 — explicitly NOT a synonym for `:ehrt`,
which would resurrect the cwd-relative-path hazard `bin/ehrt`'s own
header exists to prevent. Root `deps.edn` gains a `:cli` alias
(`:extra-deps` Clojure 1.12.5 only, `:extra-paths ["bases/cli/src"]`,
`:main-opts ["-m" "ehrt.cli.retired"]`). New namespace
`bases/cli/src/ehrt/cli/retired.clj`: requires nothing beyond
`clojure.core`; a pure `retired-message` fn plus a `-main` that prints
it to `*err*` and `System/exit 2` (the operational-error contract).
The message names what was retired and what replaces it, and shows
one worked example mirroring the founding shape (`bin/ehrt sim run
--seed 42 --patients 200 --config config/busy-weekday.edn`), ending
"run bin/ehrt help for commands". No citations, milestones, or
internal names in the message — provenance lives in the namespace's
own source comment (two voices, two homes, `.agents/rulings.md`
"From the UX arc"). Two gates, red-first: `bases/cli/test/ehrt/cli/
retired_test.clj` (the message clears `help-voice-test`'s own
agent-speak pattern, names `bin/ehrt`, and never shows `clojure
-M:cli run` as a worked example again — it may name the retired form,
never re-teach it) and `components/docs-tooling/test/ehrt/
docs_tooling/cli_tombstone_test.clj` (reads root `deps.edn` as EDN,
asserts the `:cli` alias exists with exactly the given `:main-opts`
and `:extra-paths` — the tombstone is not silently removable).

**Disclosed finding, not a gate weakening:** `ehrt.docs-tooling.
invocation-lint-test`'s own scan surface (`README.md`,
`AUTHORS-GUIDE.md`, `docs/**`, `components/*/docs/**`) does not cover
`bases/cli/src` or `bases/cli/test` at all — same "out of scope by
construction" class as `.agents/prompts/`/`.agents/session-records/`.
The retired namespace's own source comment names `clojure -M:cli`
literally (for provenance); this needed no gate exemption because the
gate never reaches that file.

**AR-EP-2 (the sibling muscle-memory path).** Probed first: `bin/ehrt
run --seed 1 --patients 1` (the OLD top-level verb through the NEW
entry point) returned `:unknown-command` with hint `"run: ehrt help"`
— generic, not pointing at `sim run`, confirming the near-miss crosses
a GROUP boundary (`run` is a verb name, sim's own, not a group name)
that the existing group-name check in `unknown-command-error` could
never catch. Fix: a new private `verb-name-groups` helper in
`ehrt.cli.core` returns every group whose own verbs include a given
token; `unknown-command-error` extended so a token matching a verb in
EXACTLY ONE group gets `:did-you-mean "<group> <verb>"` and a hint
naming that group (`run: ehrt help sim`); a token matching no verb, or
matching more than one group's verb, keeps the prior generic
behavior. Red-first (`dispatch-unknown-top-level-verb-matching-one-
groups-verb-suggests-it-test`), confirmed failing before the fix,
green after.

**AR-EP-3 (`--width`/COLUMNS).** Author ruled the affordance IN,
2026-08-06, retiring ADR-0064's own "Intake for the next arc" row
naming it. Scope: help rendering only (the ADR-0063 wrap mechanism);
error payloads are data, untouched. Resolution order: explicit
`--width N` beats the `COLUMNS` environment variable beats
`ehrt.cli.help/default-wrap-width` (80). Validation asymmetry,
deliberate and disclosed: `--width` is user input — a value that does
not parse to an integer >= `ehrt.cli.help/min-wrap-width` (40) is
rejected by name (`:invalid-width`, naming the flag, the given value,
and what was expected; exit 2; red-first); `COLUMNS` is ambient — any
value that doesn't parse to a valid width falls back SILENTLY to 80,
never an error, since a broken terminal variable must not break help.
`--width` joins `ehrt.cli.help/global-flags` (fresh read: no
dedicated "help flags" list exists separate from global-flags in
`cli-spec`; global-flags is the join point every other command-wide
flag already uses, so this is the established location, not a new
one) — disclosed scope note in its own `:doc` string, since it is the
one global flag that does NOT apply to every command's own rendering
the way `--json`/`--pretty`/`--edn` do. `help_voice_test` passes
unmodified (the new flag's doc carries no agent-speak token).
`help_wrap_test` extended: every-line-fits and content-preservation
both re-proven at 40/60/120, not just the 80-column default;
resolution-order cases (flag beats env beats default; env beats
default when no flag; a non-numeric or sub-floor `COLUMNS` falls back
silently); the rejection case. Red-first throughout, including the
pure `ehrt.cli.help/resolve-width`/`parse-width-flag` unit tests —
disclosed deviation: those two pure functions were authored alongside
their own unit tests rather than strictly before them (the dispatch-
layer integration in `ehrt.cli.core`, the actually-observable behavior
change, WAS red-first, confirmed failing pre-fix).

**Live-caught defect, fixed in scope:** extending `help_wrap_test` to
40/60/120 surfaced a real pre-existing bug the 80-column-only test
never could: `render-top-level`'s own "Run `ehrt help <group>` for a
group's verbs and flags." line was a bare string literal, never
routed through `wrap-with-hanging-indent` like every other rendered
line — invisible at 80 columns (56 chars, under the old ceiling),
real at 40. Fixed by wrapping it the same way its neighbors already
are — not a wrap-ALGORITHM change (the fence's own boundary), applying
the existing mechanism to a line that had never been run through it.

**AR-EP-4 (rider — AR-A-5 relocation class).** The two FIXED-marked
Deferred rows (Procedure-duration / ADR-0032; closure full-pipeline /
ADR-0033) relocated from the live roadmap's Deferred section into
`.agents/plans/roadmap-done-2026-08.md`, notes intact, each with a
dated relocation note — their own "see Done, below" pointers had
dangled since the Done rotation (scaffolding compaction B, ADR-0046)
moved the arcs they pointed at out from under them. Same
sanctioned-append class as compaction A's own AR-A-5 and ADR-0064's
own AR-UC-5. `myocardial_infarction.json`'s known violation is
untouched, disclosed, out of this rider's scope. Deferred row count
after: 11 (fresh count), all live.

**AR-EP-5 (corrections, recorded).** Two transcript-witnessed findings
from the arc-close verification, repo-recorded here per the standing
rule that a transcript-witnessed claim needs a repo home:

1. `.agents/state.md`'s `emit_hl7.clj` claim ("the `:require` form is
   still exactly `[ehrt.sim-model.interface :as sim-model]` and
   `[ehrt.sim-emit-hl7.site-profile :as site-profile]`") is imprecise.
   Fresh read of `components/sim-emit-hl7/src/ehrt/sim_emit_hl7/
   emit_hl7.clj`'s own `:require` form: it holds FOUR requires
   (`com.nervestaple.hl7-parser.parser`, `clojure.string`, and the two
   named `ehrt.*` ones) — the two named are the only `ehrt.*`
   requires, which is what the GATED invariant
   (`ehrt.docs-tooling.sim-emit-hl7-dependency-test`) actually checks
   and what HELD, but the claim's own wording read as if those two
   were the entire form. "Audit evidence uses the mechanism it
   recommends" (the UX arc's own third standing law) applies to claim
   PHRASING too, not only to verification method. `state.md` itself is
   NOT edited this session — it regenerates at the next arc's own
   close, per its own contract; this record is the correction's home
   until then.
2. The `stable-20260806-ux-close` tag debt (AR-EP-0, above): recorded
   in the archived close prompt only, never in ADR-0064 — a citation-
   precision miss, now discharged and disclosed rather than silently
   repeated.

**AR-EP-6 (scope + oracle).** `config/busy-weekday.md` stays untracked
and untouched (standing disposition since ADR-0060) — confirmed still
untracked at both Step 0 and this close. Nothing from the feature
horizon (corpus-player, pairing-as-data, module vendoring) lands here.
The oracle bracket (below) shows all ELEVEN batches identical — the
tombstone alias, the help-width plumbing, and the near-miss hint touch
no emitted byte, exactly as scoped.

### Live probe transcripts

**Step 2, the founding shape itself, against the fixed tree**
(workspace root, real `clojure -M:cli`, not a test double):

```
$ clojure -M:cli run --seed 42 --patients 200 --config ./config/busy-weekday.edn
clojure -M:cli is retired.

The CLI moved: use bin/ehrt instead. For example:

  bin/ehrt sim run --seed 42 --patients 200 --config config/busy-weekday.edn

run bin/ehrt help for commands
$ echo $?
2
```

Confirmed on stderr only (stdout empty under `2>/dev/null`; the full
message present under `1>/dev/null`) — closes this session's own
`[unverified-by-live-run]` link: the undeclared-alias/bare-positional-
as-script-path failure mode the diagnosis named is real, and the fix
answers exactly that shape.

**Step 3, the sibling muscle-memory path, against the fixed tree:**

```
$ bin/ehrt run --seed 1 --patients 1
{:status :error, :category :unknown-command, :payload {:args ["run"], :valid-options [...], :hint "run: ehrt help sim", :did-you-mean "sim run"}}
$ echo $?
2
```

**Step 3, `bin/ehrt help` at three widths, against the fixed tree:**
`--width 40` and `--width 60` both render (content-identical to the
unwrapped page, whitespace-normalized); `COLUMNS=120 bin/ehrt help`
(no `--width`) renders at 120, confirming the COLUMNS arm resolves
against the real environment, not only the injectable test seam;
`bin/ehrt help --width abc` returns `{:status :error, :category
:invalid-width, :payload {:flag "--width", :value "abc", :expected
"an integer >= 40"}}`, exit 2.

### Red→green evidence

`bases/cli/test/ehrt/cli/retired_test.clj` and `components/docs-
tooling/test/ehrt/docs_tooling/cli_tombstone_test.clj`: both created
red (namespace-load failure / three failing assertions against the
pre-fix tree), green after `ehrt.cli.retired` and the `:cli` alias
landed. `dispatch-unknown-top-level-verb-matching-one-groups-verb-
suggests-it-test`: red (generic hint, no `:did-you-mean`) before
`verb-name-groups`/the `unknown-command-error` extension, green after.
The nine new `--width`/COLUMNS dispatch-layer tests in `core_test.clj`:
red before `resolved-help-width` and the dispatch-branch threading
landed, green after. `help_wrap_test.clj`'s new 40/60/120 invariants:
red on first run (the unwrapped top-level line, above), green after
that fix. Full non-integration suite (`clojure -M:poly test :all
skip:integration`) green throughout every checkpoint: 511 assertions
in the tail component both at Step 0's baseline and at this close, 0
failures/0 errors in the full log at every checkpoint (grepped, not
asserted from a tail). `clojure -M:poly check`: OK at every checkpoint.

### Oracle bracket

`bin/regression-oracle 2e77096 bccd46a` (Step 0 tip to Step 3's own
tip — every `src`/`test` change this session lands by `bccd46a`; Step
4, this ADR and its own archival trail, is docs-only): soundness check
IDENTICAL outside the `digest.clj` `(ns ...)` form; all ELEVEN roots'
SHA-256 digests IDENTICAL between baseline and target (appendicitis,
death-fixture, ear-infections, ear-infections-engine,
ear-infections-history-engine, sepsis, sinusitis, sore-throat,
total-joint-replacement-engine, urinary-tract-infections-engine,
urinary-tract-infections-history-engine) — matching AR-EP-6's own
expectation exactly: a tombstone alias, help-width plumbing, and a
hint touch no emitted byte. Re-run against `<this session's own
closing commit>` for completeness (session record); expected and
confirmed unchanged, since nothing after `bccd46a` touches `src/` or
`test/`.

### Fences honored

Src edits stayed exactly where scoped: the new `retired.clj`; root
`deps.edn`'s new `:cli` alias; `help.clj`'s width resolution,
`cli-spec`'s `--width` entry, and the one pre-existing unwrapped line
it caught; `core.clj`'s `unknown-command-error` (AR-EP-2's probe
failed, so the fix landed) and the dispatch-layer width threading. No
wrap-ALGORITHM change. No error-payload reformatting beyond the new
`:invalid-width` category itself. No gate weakened — `retired_test.clj`
and `cli_tombstone_test.clj` are new, additive gates; `help_wrap_test.clj`
and `core_test.clj` grew, nothing in either lost an assertion. Frozen
archives untouched except this ADR's own new file, the `notes/ADRs.md`
index line, `notes/adr/README.md`'s file count (62→63, same discipline
ADR-0064's own Step 3 applied), and the AR-EP-4 attic append to
`.agents/plans/roadmap-done-2026-08.md`.

### Consequence

The founding incident's own command shape now answers in words: a
stranger typing the pre-monorepo invocation from memory gets a
redirect naming `bin/ehrt` and a worked example, not a stack trace.
The sibling near-miss (a bare top-level verb, crossing a group
boundary) gets a real hint. `ehrt help` degrades gracefully at
whatever terminal width the caller actually has, live, not only in a
test's own comparison arm. Two long-dangling Deferred rows join the
attic. Two transcript-witnessed corrections from the arc-close
verification are now repo-recorded. The oracle bracket confirms none
of this touched a single emitted byte of the sim engine's own output.
The feature horizon (corpus-player, pairing-as-data, module vendoring)
is untouched, unruled, waiting for its own session.

### Index summary (moved verbatim from notes/ADRs.md by ADR-0143, 2026-08-16)

UX epilogue: muscle memory gets an answer, help gets a width — the retired `:cli` alias redirects, a sibling near-miss gets a real hint, `--width`/COLUMNS lands
