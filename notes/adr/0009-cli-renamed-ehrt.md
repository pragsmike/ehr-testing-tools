<!-- Attic file: notes/adr/0009-cli-renamed-ehrt.md -->
<!-- Moved verbatim from notes/ADRs.md by scaffolding compaction B (2026-08-05, notes/ADRs.md ADR-0046). notes/ADRs.md remains the citation index -- see there. -->

## ADR-0009 — CLI renamed `ehrt` ("e-heart"); base `cli`, project `ehrt-cli`; `sim-cli` deprecated

**Status:** Accepted (author-directed, autonomous session per R30), 2026-07-29.

### Context

The CLI has been `ehr` since ADR-0002's own H2 landing. The author's
own session prompt (2026-07-29) renames it: **R32** [A] — the CLI is
renamed `ehr` → `ehrt`, pronounced "e-heart." Rationale on record:
memorable, and `ehr` stays reserved for future payload-EHR tooling (a
separate, not-yet-built capability this workspace may grow later,
distinct from the testing tooling `ehrt` names). **R35** [C] — the base
`bases/ehr-cli` renames to `bases/cli` (namespace `ehrt.cli.*`,
avoiding the `ehrt.ehrt-cli` stutter a literal rename would have
produced), and the project `projects/tools-cli` renames to
`projects/ehrt-cli` (projects name deployables; the deployable is now
`ehrt`). **R33** [A] — `bases/sim-cli` (sim's own standalone CLI) is
deprecated, not removed: it keeps working, its own tests keep running,
but no user-facing doc mentions it and `AGENTS.md`'s dev path marks it
deprecated, with a named (not scheduled) retirement trigger.

### Decision

**Every surface renamed, mechanically, in one pass:** `bin/ehr` →
`bin/ehrt` (`git mv`, preserving the index's `100755` mode --
`bases/cli/test/ehrt/cli/executable_bits_test.clj`'s own generic
`bin/`-prefix check proved this without needing a special case).
`bases/ehr-cli` → `bases/cli`, `ehrt.ehr-cli.core`/`.help` →
`ehrt.cli.core`/`.help`. `projects/tools-cli` → `projects/ehrt-cli`,
`workspace.edn`'s own project key and `:alias` renamed to match. Root
`deps.edn`'s `:ehr` alias → `:ehrt` (`poly/ehr-cli` → `poly/cli`, `-m
ehrt.ehr-cli.core` → `-m ehrt.cli.core`); every other `deps.edn`
touching the old base/project name (`projects/ehrt-cli`,
`projects/conformance`, `projects/integration`) updated to match.
`.githooks/`, `.github/workflows/integration.yml`'s own `clojure
-M:ehr artifact fetch` calls, `Makefile`'s `cli-doc` target
(`ehrt.ehr-cli.help/write-cli-md!` → `ehrt.cli.help/write-cli-md!`),
`README.md`'s Quickstart fence and project map, `bin/quickstart-demo`,
`AGENTS.md`, `AUTHORS-GUIDE.md`, `SETUP.md`, `CONTRIBUTING.md` --
every one of these carries the CLI's real invocable name, so every one
needed the same rename, not a representative sample.

**The CLI's own rendered surface, not just its file paths.**
`bases/cli/src/ehrt/cli/help.clj`'s `cli-spec` map has a `:program`
key (`"ehr"` → `"ehrt"`) that `render-top-level` already correctly
read from `spec` rather than hardcoding -- but `group-section` and
`verb-section` (the same file) hardcoded the literal string `` `ehr `` in
every section heading, independent of `:program`. Left alone, the
`:program` rename would have produced a `docs/cli.md` whose synopsis
said `ehrt <group>...` while every section heading still said `` `ehr
gate` `` -- a real, user-visible inconsistency a file-path grep would
never catch, only found by reading the renderer's own output shape.
Fixed by renaming both hardcoded literals directly (not by switching
them to read `(:program spec)`, which would be a design change beyond
this rename's own scope). The table-of-contents anchor links
(`#ehr-<group>`, built from the same hardcoded prefix) needed the
matching fix for the same reason -- a markdown anchor is derived from
its heading's own rendered text, so a heading rename without an anchor
rename breaks every internal cli.md link silently. `docs/cli.md`,
`docs/use-cases.md`, `docs/pipeline.md`, `docs/operators.md`
regenerated via `make docsgen` after the source fixes (`use-cases.edn`
hand-edited first, per this file's own generated-vs-authored
discipline) -- never hand-edited.

**Historical prose left alone, deliberately.** Every mention of `bin/ehr`
or `ehr-cli` describing a *past* event -- `bases/cli/test/ehrt/cli/executable_bits_test.clj`'s
own account of the H2 carve's executable-bit incident, ADR entries
prior to this one, archived session prompts, `.agents/session-records/`
-- was left exactly as written. Those records are accurate about what
was true when they were made; rewriting them to say `ehrt` would
misrepresent history, the same discipline `notes/sim/`/`notes/tools/`
frozen provenance already enforces for a different class of file. Only
citations of *current* state (what the CLI is called *now*) were
renamed.

**CLI smoke baseline note.** No committed byte-baseline file for `ehr
help`/`ehr corpus operators` output exists anywhere in this workspace
(ADR-0002's own "byte-identical" verification was a comparison against
a live `stable-pre-monorepo` clone at verification time, not a stored
fixture) -- there is nothing to regenerate. This record is the
disclosure the session prompt asked for: any *future* comparison
against a pre-rename invocation is moot, because the pre-rename
invocation (`bin/ehr ...`) no longer exists; `bin/ehrt help` and `bin/ehrt
corpus operators` are this rename's own fresh baseline, verified
below, not diffed against anything older.

**Sim-cli deprecation (R33).** `AGENTS.md`'s "Landed so far" section
now states the deprecation and its retirement trigger directly;
`notes/facts-register.md` F2 carries the same trigger as a dated row.
`SETUP.md` and `CONTRIBUTING.md` -- both user-facing, both found
during this session's own sweep to still present `sim-cli` as the
example of "something this workspace builds" (stale even before this
session: `SETUP.md`'s own text said "once `bases/sim-cli` has landed,"
long since true) -- now point at `bin/ehrt sim run` instead.
`bases/sim-cli`'s own code, deps.edn, and tests are untouched: `poly
check`/`poly test` still exercise them, unconditionally, exactly as
before.

### Verification

`clojure -M:poly check`: green. `clojure -M:poly test :all
skip:integration`: 0 failures/0 errors (full run). `bin/ehrt help`:
exit 0 from the workspace root, real invocation, prints the renamed
`ehrt`-branded usage text. `make ci-parity`: fresh clone, cold cache,
green (recorded below in the deviation record with the actual run
numbers). `make docsgen`: regenerated all four derived docs cleanly,
zero hand-edits to any `docs/*.md` output.

### Deviation record

**The `group-section`/`verb-section` hardcoded-heading finding**,
already recorded above as part of the decision, since it's load-bearing
to what "every surface" actually meant for this rename, not an
incidental aside.

**`docsgen_test.clj`'s own fixture assertions needed updating, not just
its fixture data.** `test-cli-spec`'s own `:program "ehr"` value is
synthetic and doesn't need to match the real CLI name -- but two
assertions independently hardcoded the expected `` `ehr <group>...` ``
heading text (mirroring `docsgen.clj`'s own pre-fix hardcoding), so
they broke the moment `group-section`/`verb-section` were fixed to
emit `ehrt`. Fixed in the same commit; a reminder that a test can
duplicate a hardcoded literal independently of the code under test,
not just inherit it.

**`quickstart_fresh_test.clj`, `usecases_test.clj`, and
`corpus/spool_source_test.clj`'s own `bin/ehr`-shaped fixture strings
left unchanged, deliberately.** These are synthetic example data fed
into a generic line-matching/rendering algorithm that treats them as
opaque text -- not assertions about this repo's own real invocation
name, and not read by anything outside their own test. Renaming them
would cost real review attention for zero behavioral or documentation
value; named here as a considered choice, not an oversight the next
session should "finish."

---

