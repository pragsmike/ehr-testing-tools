## ADR-0063 — UX fixes 5: the help wraps like it means it — hanging indents, width gated

**Status:** Accepted (author-directed, autonomous session per R30), 2026-08-06.

### Context

Prior: ux fixes 4 landed and was design-channel-verified (`7837fac`,
`notes/adr/0062-ux-fixes-4.md`). This is the arc's last fix: register
row B-4(b) (`.agents/plans/2026-08-06-ux-audit-findings.md`) — the
render functions gain hanging-indent word wrap so a long string
degrades gracefully instead of running one unbroken line, now and for
every future string. Content is DONE (session 4's author-ruled words
are untouchable; the voice gate guards them); this session changes
only how those words are laid out on a terminal.

### Decision

Ruled 2026-08-06, recorded verbatim (author rulings, this session's
own prompt):

**AR-U5-0 (tag, standing ceremony).** Annotated
`stable-20260806-ux-fixes-4` at `7837fac`, message "ux fixes 4 landed,
design-channel-verified 2026-08-06 (ADR-0062)"; push; verify.

**AR-U5-1 (the wrap).** The render functions word-wrap every
description at a fixed width (default 80 columns) with a hanging
indent aligned to each layout's own description column -- flag rows
wrap under the description start, group/verb docs under their text
start. Wrap on spaces only; a single token longer than the remaining
width (a long path, a URL) lands on its own line unbroken rather than
being split mid-token. Rendered STRING CONTENT is byte-identical to
the spec's -- the wrap inserts newlines and indent spaces, nothing
else; the voice gate and a content-preservation assertion (below) both
prove it.

**AR-U5-2 (red-first, two ways).** (a) The width test: no rendered
line of `ehrt help` or any `ehrt help <group>` exceeds 80 columns --
witnessed RED against the current renderer first (the audit's
over-long strings still render as single long lines today;
transcript the offenders). (b) The content-preservation property: for
every group, stripping the wrap's inserted whitespace (newlines +
indent runs) from the rendered output yields exactly the same
character sequence as rendering unwrapped -- spec-derived, so future
strings are covered. Both tests land with the fix in ONE commit,
green.

**AR-U5-3 (the generated doc's disposition).** Determine by inspection
whether `docs/cli.md`'s generated region shares the terminal render
path. If it does, regenerate and disclose the diff (markdown consumers
reflow anyway -- a wrapped source is acceptable); if it renders
separately, leave it byte-identical and say so. Either way the
disposition is one paragraph in ADR-0063, not a judgment left
implicit.

**AR-U5-4 (scope).** No spec-data changes of any kind -- not one
string, not one comment (the voice gate plus AR-U5-2(b) enforce this
structurally). No `core.clj`. Terminal width stays a constant this
session -- a `--width`/COLUMNS affordance is a NOTE for the arc
close's intake if the session thinks it worthwhile, never an act. The
oracle bracket must show all ELEVEN batches identical; any change is
STOP-AND-ESCALATE.

### Execution

**Read-first.** Register rows B-4(a)/(b) and D-1's rendered-output
evidence (`.agents/plans/2026-08-06-ux-audit-findings.md`); `help.clj`
in full, its render functions in particular; ADR-0062's before/after
transcripts as the current-state reference.

**AR-U5-0, tag.** `stable-20260806-ux-fixes-4` did not yet exist;
created annotated at `7837fac`, pushed, verified: peeled ref resolves
to `7837fac` exactly.

**AR-U5-1, the wrap.** `bases/cli/src/ehrt/cli/help.clj` gained
`default-wrap-width` (80, a private-namespace constant, no CLI-facing
knob), `wrap-lines` (greedy word-wrap, spaces only, an over-width
single token lands alone unbroken), and `wrap-with-hanging-indent`
(renders a prefix + word-wrapped text, continuation lines indented to
the prefix's own length). Every render function that emits a
rendered-string-position value -- `render-flag`, `render-exit-codes`,
`render-verb`'s own doc and positional-doc, `render-group`'s own doc
and positional-doc, `render-top-level`'s own top-level-doc and its
per-group summary line -- now routes its text through
`wrap-with-hanging-indent` at the call site's own prefix (flag rows:
`"  " flag "  "`; exit-code rows: `"  " code "  "`; verb docs: `"  "`;
group/positional docs: the literal text before the doc starts, e.g.
`"ehrt " group-name " -- "` or `"Positional: " positional " -- "`).
Each of `render-flag`/`render-flags`/`render-exit-codes`/`render-verb`/
`render-group`/`render-top-level` gained an optional trailing `width`
arg (default `default-wrap-width`) purely so the content-preservation
test (AR-U5-2(b)) can compare an 80-column render against an
effectively-unwrapped one -- never a user-facing surface; `core.clj`
is untouched and no CLI flag reads it.

**AR-U5-2, red-first, two ways.**

(a) **The width test, witnessed RED against the pre-wrap renderer.**
`bases/cli/test/ehrt/cli/help_wrap_test.clj`'s
`top-level-lines-fit-width-test` and
`every-group-page-lines-fit-width-test` were run against `help.clj`
temporarily reverted to `7837fac`'s content (the content-preservation
section, which depends on the not-yet-existing wrap mechanism and
cannot compile against that tree, was held out via a temporary
`(comment ...)` block for this one run, then restored before
implementing): `Ran 2 tests containing 266 assertions. 83 failures, 0
errors.` The two worst offenders, transcripted: `bin/ehrt help`'s own
top-level doc line ran 482 columns unbroken; `bin/ehrt help corpus`'s
`corpus generate` verb doc ran 404. Full failure transcript in the
Appendix.

(b) **The content-preservation property.** This property is a
statement about the wrap mechanism's own correctness (word content and
order surviving wrapping) -- it has no meaningful red state against a
renderer that does no wrapping at all (there is nothing to strip), and
depends on private functions (`wrap-lines`, `wrap-with-hanging-indent`)
and a `width` arg on the render functions that do not exist before
AR-U5-1 lands; against the pre-wrap tree this section fails to
compile, not fails an assertion -- a stronger, self-evident form of
"doesn't exist yet," disclosed here rather than forced into an
artificial runtime-red shape. Proven two ways once the mechanism
exists: `wrap-lines-round-trips-every-rendered-spec-string-test`
walks every rendered-string-position value in `cli-spec` (the same
walk `help-voice-test` already trusts) and asserts joining
`wrap-lines`' own output with single spaces reconstructs the input
exactly -- true because wrapping only ever replaces an existing
inter-word space with a line break, never touches word content.
`wrapped-and-unwrapped-pages-carry-identical-content-test` renders
`render-top-level` and every `render-group` at width 80 and at an
effectively-unlimited width (100000), whitespace-normalizes both (any
run of whitespace collapsed to one space), and asserts equality --
this is the operational form of "stripping the wrap's inserted
whitespace yields the same character sequence as rendering unwrapped":
normalizing whitespace erases both the wrap-inserted breaks and any
pre-existing structural spacing consistently on both sides, so the
equality can only hold if wrapping added, dropped, or reordered no
content. A `wrap-lines-mechanism-sanity-test` proves the primitive
directly on synthetic input independent of `cli-spec`'s own current
strings (wraps at the given width; an over-width single token lands
alone unbroken; hanging-indent alignment is exact).

Both properties, plus the existing `help-test`/`help-voice-test`
suites, land green in the fix commit: `Ran 16 tests containing 827
assertions. 0 failures, 0 errors.` (help-test 65, help-wrap-test 598,
help-voice-test 164).

**AR-U5-3, the generated doc's disposition.** `docs/cli.md` is
rendered by `components/docs-tooling/src/ehrt/docs_tooling/docsgen.clj`'s
own `render-cli-md`/`flags-table`/`verb-section`/`group-section` --
entirely separate pure functions, markdown-table-shaped, that never
call any of `help.clj`'s `render-*` functions (the two files share
only the `cli-spec` data, per that namespace's own docstring on why
the two renderers can't be one: components can't require bases).
`docs/cli.md` does **not** share the terminal render path this session
touched. Confirmed by running `make cli-doc` after the wrap landed and
diffing the result against the pre-session file: no diff, byte
identical. Disposition: left untouched, correctly -- nothing to
disclose beyond this paragraph.

**AR-U5-4, scope -- confirmed clean.** No `cli-spec` string, comment,
or default value changed (the voice gate, `help-test`'s own coverage
suite, and AR-U5-2(b)'s own content-preservation property all still
pass unmodified). `core.clj` untouched. `default-wrap-width` is a
`def`, not a flag; no `--width`/`COLUMNS` reads anywhere. Oracle
bracket below: all eleven batches identical.

### Verification

- `clojure -M:poly check`: OK (both before and after the wrap).
- Red→green: the width test witnessed red (83 failing assertions, two
  worst-offender lines transcripted) against `help.clj` reverted to
  `7837fac`; green (0 failures) after AR-U5-1 landed. The
  content-preservation section's own "red" is the pre-wrap tree's
  inability to compile it at all (disclosed under AR-U5-2(b) above,
  not a runtime assertion failure).
- `clojure -M:poly test :all skip:integration` (workspace root): every
  project's own summary green, 0 failures, 0 errors throughout; `grep
  -c "FAIL\|ERROR"` on the full transcript: 0.
- `bases/cli` (`help-wrap-test` + `help-test` + `help-voice-test`): 16
  tests, 827 assertions, 0 failures, 0 errors.
- `bin/verify-nist-lock`: OK, 6/6 coordinates match.
- `gitleaks`: clean, staged scan and push hook.
- Post-push message verification: one delta, the known harmless
  trailing-newline artifact of `git log --format=%B`.
- Tag verification: `stable-20260806-ux-fixes-4` peeled ref resolves
  to `7837fac` exactly.
- **Oracle bracket** (`bin/regression-oracle 7837fac f617b22`):
  **IDENTICAL: every root's digest matches** -- all eleven
  vendored-root batches (`appendicitis`, `death-fixture`,
  `ear-infections`, `ear-infections-engine`,
  `ear-infections-history-engine`, `sepsis`, `sinusitis`,
  `sore-throat`, `total-joint-replacement-engine`,
  `urinary-tract-infections-engine`,
  `urinary-tract-infections-history-engine`) byte-identical, exactly
  as AR-U5-4 required -- a help-render layout change touches no
  emitted byte. Soundness check: digest source identical outside its
  own `(ns ...)` form; no `--declared-digest-change` needed. Bracket
  ends at `f617b22` (the fix commit, the last behavior-bearing
  commit), not this record's own closing commit, following
  ADR-0059/ADR-0060/ADR-0061/ADR-0062's own precedent.

### Fences (standing law applies unchanged, this session's own prompt)

`help.clj` edits landed ONLY in its render functions (new private
`wrap-lines`/`wrap-with-hanging-indent`/`default-wrap-width`, and the
five existing `render-*` functions threaded through them) -- no
`cli-spec` string, comment, or default changed; no `core.clj`; no new
CLI flag. One new file: `bases/cli/test/ehrt/cli/help_wrap_test.clj`.
`docs/cli.md` untouched, correctly (AR-U5-3). No gate weakening.
Frozen archives untouched apart from this ADR + index + Done pointer +
session-record/prompt archival, all sanctioned.

### Consequence

`bin/ehrt help` and every `bin/ehrt help <group>` page now wrap at 80
columns with a hanging indent that keeps a long description visually
attached to the flag or group it belongs to, instead of running one
unbroken line past the edge of a real terminal -- the last piece of
the UX arc's own B-4 finding (content in session 4, mechanism here).
The content-preservation tests mean this holds for every future string
too, not just today's. After landing: the design channel verifies by
fresh probe, then drafts the ARC CLOSE -- the pending appends, state
regeneration, budgets, rotation, register tally, and final tags, per
the alignment close's standing pattern. This landing's own tag rides
the close session's Step 0.

### Step 2 (this entry) — record

This ADR lands; `notes/ADRs.md` gains its own index line;
`notes/adr/README.md`'s own file count corrected 60→61 ("as of
ADR-0063"). Done pointer added in the same commit as the index line:

```
- 2026-08-06 — ux-fixes-5 — ADR-0063
```

Session record (`.agents/session-records/2026-08-06-ux-fixes-5.md`)
and this session's own driving prompt archived
(`.agents/prompts/2026-08-06-ux-fixes-5.md`), both indexed in
`.agents/session-records/README.md` and `.agents/prompts/README.md`
in the same commit.

### Appendix — red transcript (width test, against the pre-wrap tree)

`top-level-lines-fit-width-test` / `every-group-page-lines-fit-width-test`,
`help.clj` temporarily reverted to `7837fac`'s content (the
content-preservation section held out via a temporary `(comment ...)`
block for this one run, restored immediately after):

```
Ran 2 tests containing 266 assertions.
83 failures, 0 errors.
{:test 2, :pass 183, :fail 83, :error 0, :type :summary}
```

Two representative failures (the worst offenders the audit's own B-4
row named):

```
FAIL in (top-level-lines-fit-width-test)
render-top-level: line exceeds 80 columns and is not a single unbreakable token:
"Every command accepts --json (EDN is canonical, --json a projection); `ehrt show FILE` renders a v2/FHIR file for a human. See docs/formats.md." (146 columns)

FAIL in (every-group-page-lines-fit-width-test)
render-group corpus: line exceeds 80 columns and is not a single unbreakable token:
"  Generate a deterministic synthetic corpus. Takes a source subcommand: `corpus generate sim` (this workspace's own engine; the flags marked sim:) or `corpus generate synthea` (the flags marked synthea:). Bare `corpus generate` means `generate sim`. Both bare commands are byte-reproducible as-is; re-running into an existing non-empty --out-dir is rejected (:out-dir-exists), never silently overwritten." (404 columns)
```

Live-captured, not simulated: `bin/ehrt help` (pre-wrap) ran its
top-level doc line at 482 columns; `bin/ehrt help corpus` ran its
`corpus generate` verb doc at 404 columns -- both match the test
failures above exactly.

After AR-U5-1 landed, the same two tests: `0 failures` (part of the
full 827-assertion green run above).

### Appendix — before/after transcripts (AR-U5-1's byte-identical-content claim)

**`bin/ehrt help`, unified diff (before = `7837fac`, after = `f617b22`):**
newlines and indent spaces inserted at word boundaries only; every
word, in the same order, survives. Whitespace-normalizing both sides
(`tr '\n' ' ' | tr -s ' '`) and diffing: no difference.

```diff
--- before
+++ after
@@ -1,29 +1,65 @@
 Usage: ehrt <group> [<verb>] [flags]
 
-Every command accepts --json (EDN is canonical, --json a projection); `ehrt show FILE` renders a v2/FHIR file for a human. See docs/formats.md.
+Every command accepts --json (EDN is canonical, --json a projection); `ehrt show
+FILE` renders a v2/FHIR file for a human. See docs/formats.md.
 
 Groups:
   artifact  Fetch and resolve locked external engine/tool artifacts.
-  corpus  Generate, mutate, intake, and inspect synthetic corpora. Any PATH, --out-dir, or --out also accepts a dir:/file: URL designator in place of a bare path; bare paths are the common spelling. `corpus generate` is the front door for new corpora; `corpus intake` catalogs existing ones -- and can generate-then-catalog, or read piped bytes, in one command (see intake).
+  corpus  Generate, mutate, intake, and inspect synthetic corpora. Any PATH,
+          --out-dir, or --out also accepts a dir:/file: URL designator in place
+          of a bare path; bare paths are the common spelling. `corpus generate`
+          is the front door for new corpora; `corpus intake` catalogs existing
+          ones -- and can generate-then-catalog, or read piped bytes, in one
+          command (see intake).
 [... gate/check/version/doctor/sim/show/play group lines wrap the same way ...]
 
 Global flags:
   --json  project the EDN result to JSON (EDN remains canonical)
-  --pretty  force a human-readable summary, even when stdout is piped -- already the default at a real terminal
-  --edn  force the raw EDN envelope, even at a terminal -- already the default when stdout is piped or redirected
+  --pretty  force a human-readable summary, even when stdout is piped -- already
+            the default at a real terminal
+  --edn  force the raw EDN envelope, even at a terminal -- already the default
+         when stdout is piped or redirected
   --help  print this command's usage and exit 0 without running it
 
 Exit codes:
   0  ran and passed
   0  bare invocation, help, and --help all exit 0 too
   1  ran and legitimately rejected
-  2  operational error (bad invocation, missing artifact, subprocess failure, etc.)
-  3  a gate found :no-verdict outcomes and the default --treat-no-verdict-as policy is in effect -- see that flag to fold them into pass or rejected
+  2  operational error (bad invocation, missing artifact, subprocess failure,
+     etc.)
+  3  a gate found :no-verdict outcomes and the default --treat-no-verdict-as
+     policy is in effect -- see that flag to fold them into pass or rejected
```

**`bin/ehrt help corpus`, after (excerpt -- the historically worst page,
per the audit's own B-4 finding):**

```
ehrt corpus -- Generate, mutate, intake, and inspect synthetic corpora. Any
               PATH, --out-dir, or --out also accepts a dir:/file: URL
               designator in place of a bare path; bare paths are the common
               spelling. `corpus generate` is the front door for new corpora;
               `corpus intake` catalogs existing ones -- and can
               generate-then-catalog, or read piped bytes, in one command (see
               intake).

ehrt corpus generate
  Generate a deterministic synthetic corpus. Takes a source subcommand: `corpus
  generate sim` (this workspace's own engine; the flags marked sim:) or `corpus
  generate synthea` (the flags marked synthea:). Bare `corpus generate` means
  `generate sim`. Both bare commands are byte-reproducible as-is; re-running
  into an existing non-empty --out-dir is rejected (:out-dir-exists), never
  silently overwritten.

Flags:
  --config-path  synthea: Synthea properties file (default:
                 resources/synthea-default.properties)
  --seed  patient/master-generation seed (integer), shared by both sources
          (default: 1)
  ...
  --config  sim: path to an EDN file carrying the data-heavy engine keys
            (:pathway/:pathways/:order-profiles/:churn-profile/:site-profile/:modules/...)
            (default: none)
```

The last `--config` line (90 columns) is AR-U5-1's own named exception:
the `(:pathway/:pathways/...)` parenthetical is one 78-character token
that, even alone on its own continuation line under a 12-column
indent, cannot fit within 80 -- it lands unbroken rather than being
split mid-token, exactly as ruled. No other line in either transcript
exceeds 80 columns.

Line-count check on `bin/ehrt help corpus`: 56 lines before, 98 after
-- content unchanged (whitespace-normalized diff: none), laid out
across more, narrower lines.
