<!-- Attic file: notes/adr/0017-tools-split-stage-2.md -->
<!-- Moved verbatim from notes/ADRs.md by scaffolding compaction B (2026-08-05, notes/ADRs.md ADR-0046). notes/ADRs.md remains the citation index -- see there. -->

## ADR-0017 — `tools` split stage 2: `corpus-io` extracted, generator-source's own domain edge kept the seam split correctly, `:necessary` re-derived again

**Status:** Accepted (author-ruled 2026-07-31 on two escalations, session executed same day).

### Context

`notes/2026-07-30-refactoring-review.md` §5.1a proposed a staged split
of `components/tools`; stage 1 (ADR-0016) extracted `docs-tooling`,
the dev-time doc/lint tooling and the sole source of the former
`tools → palgebra` src edge. This record executes stage 2, the
transport/IO seam of the corpus cluster: sources, sinks, spooling,
framing, wire codecs. Method is stage 1's proven cycle
(characterize → extract → verify byte-identical → `poly check`
green). Session start: the WSL ext4 clone was already at
`origin/main` (`039805e`, "docs: archive split stage 1 prompt with
deviation record") — no fast-forward needed.

### Decision

**Landing shape**, per author ruling on names (`corpus-io`,
`ehrt.corpus-io.*`) and on the directional rule that governs the
whole stage (AR-2): `corpus-io` may never require `ehrt.tools.*`,
`ehrt.docs-tooling.*`, or any judge component — the domain implements
or consumes this component's constructors, never the reverse.

```
components/corpus-io/  -- src/ehrt/corpus_io/{framing,er7,spool,
                          spool_source,source_sink,source_sink_url,
                          sink_write,operation_manifest,canonicalizers,
                          interface}.clj, test/ehrt/corpus_io/{same}_test.clj,
                          deps.edn ({:deps {metosin/malli ...
                          org.clojure/data.json ...}})
components/tools/      -- loses nine namespaces; ehrt.tools.corpus.
                          generator-source (unmoved) gains two
                          relocated pieces (below)
```

Characterization (full require-edge map over the 17
`components/tools/src/ehrt/tools/corpus/` files, plus every external
consumer) surfaced two real edges from the seam back into the domain's
generator registry that AR-1's own filename-based default list didn't
anticipate — both escalated to the author in chat before any code
moved, both resolved by the author choosing the recommended option:

1. **`source-sink`'s own `generator-source` constructor** (validates
   +shapes a generator-kind Source, distinct from
   `generator-source.resolve!`, which additionally *executes* the
   engine) called `generators/resolve-params` — a real, tested,
   load-bearing edge into `ehrt.tools.corpus.generators` (domain,
   stays). Its only caller, `source-sink-url`'s `finish-source`
   (inside `parse-source-designator`), also moves by AR-1's default,
   so relocating just the constructor would only shift the forbidden
   edge one file over. **Ruled:** split `source-sink-url` along its
   own pre-existing internal seam — `parse-designator`, the shared
   parse skeleton, already took a `finish` callback as a parameter.
   `parse-designator`/`finish-sink`/`parse-sink-designator`/every
   pure URL-grammar helper moved to corpus-io, now exported
   (`parse-designator` was `defn-`, now `defn`, re-exported). The
   relocated `generator-source` constructor, `finish-source`, and
   `parse-source-designator` (plus the generator-query coercion
   helpers) all stayed behind in `ehrt.tools.corpus.generator-source`
   — already the domain-side home AR-2 anticipated for the *execute*
   half, now also home to the *validate+shape* half and the URL
   entry point, calling corpus-io's own re-exported `parse-designator`
   as the callback-supplying side (forward arrow, legal). The exported
   name `parse-source-designator` is unchanged everywhere it's
   consumed (`ehrt.tools.interface`, `bases/cli`) — only its source
   namespace changed.
2. **`sink-write`'s operation-manifest emission** required
   `ehrt.tools.corpus.operation-manifest`, which AR-1's naive
   per-filename default listed as staying. `operation-manifest.clj`
   itself has zero domain edges (only `malli.core`); its two
   consumers are `sink-write` (moving) and `intake` (staying, a
   trivial forward-arrow repoint). **Ruled:** moved
   `operation-manifest` to corpus-io despite AR-1's literal default,
   per AR-1's own license to "verify each against real require edges,
   not filenames."

**AR-3 (er7/canonicalizers) resolved without conflict, both by
default:** `er7` (wire codec) requires kernel only — moved.
`canonicalizers` requires kernel only (its own docstring's
"registered into `ehrt.tools.canonical`" citation was already stale
before this session — the registry it actually calls,
`kernel/register!`, moved to kernel at ADR-0008; corrected in the
same pass as the mechanical rename) — moved.

**Final seam assignment, as executed:**
- **Moved:** `framing`, `er7`, `spool`, `spool-source`,
  `source-sink` (minus `generator-source`), `source-sink-url`
  (minus `finish-source`/`parse-source-designator`/generator-query
  coercion), `sink-write`, `operation-manifest`, `canonicalizers`.
- **Stayed:** `intake`, `mutate`, `operators`, `generators`,
  `generate`, `golden-comparison`, `manifest`, plus
  `generator-source` (gained the two relocated pieces above).

**Consumers repointed forward (AR-4), no relay left in `tools`'s own
interface:** `ehrt.tools.interface` drops every re-export it used to
source from the nine moved namespaces (`framing-lookup`,
`spool-resolve!`, `default-framing`, `dir-sink`, `dir-source`,
`parse-sink-designator`, `path-designator->path`, `write-dir!`,
`write-stdout!`, `strip-run-timestamp-suffix`,
`strip-synthea-run-metadata`) with zero remainder — every real
consumer could be repointed to `ehrt.corpus-io.interface` directly
this stage: `bases/cli/src/ehrt/cli/core.clj` (four of its
`ehrt.tools.interface`-sourced aliases now source
`ehrt.corpus-io.interface` instead, same alias names, so every call
site is textually unchanged except the two `source-sink-url/` call
sites that resolve `parse-source-designator` — repointed to the
`generator-source` alias, which still sources `ehrt.tools.interface`);
`components/docs-tooling/src/ehrt/docs_tooling/lint.clj` (its
`:framing` target-4 lookup now calls `ehrt.corpus-io.interface/lookup`
directly, a new real `docs-tooling → corpus-io` edge, rather than
relaying through `ehrt.tools.interface/framing-lookup`, which no
longer exists); `projects/integration`'s own
`zero_flag_reproducibility_test.clj` (canonicalizers, repointed
directly). Domain namespaces that stayed in `tools` and genuinely
need the moved seam (`ehrt.tools.corpus.mutate`/`operators` for er7,
`intake` for source-sink/operation-manifest, `display`/`player` for
framing, `generator-source` for the pieces above) all require
`ehrt.corpus-io.interface` directly now — the permanent
`tools → corpus-io` arrow.

**Tests (AR-5).** All nine moved src namespaces' tests moved 1:1 to
`components/corpus-io/test`, renamed. `source-sink-test`/
`source-sink-url-test` split the same way their source files did:
the generator-source constructor's own three tests and every test
that calls `parse-source-designator` (nine `deftest`s, including the
non-generator `:dir`/`:file`/`:stdin`/`:blaze` cases — the function
itself relocated wholesale, so its whole test suite follows, not just
the generator-flavored cases) moved to
`ehrt.tools.corpus.generator-source-test`, which also gained
`print-source-designator` as a new corpus-io/interface export purely
to keep its own round-trip property test working (no domain edge of
its own — :dir/:file only are printable). Two mechanical-rename misses
caught only by running the suite (per stage 1's own "Step 4 is a real
command, not a checklist item" lesson): `er7_test.clj`/
`framing_test.clj` both use `ehrt.tools.corpus.simhospital-corpus`
(a test fixture helper, never one of the 17 moved src namespaces) —
the mechanical `ehrt.tools.corpus. → ehrt.corpus-io.` sed swept this
require too, since it matched the same prefix; both test-classpath
FileNotFoundExceptions, fixed by pointing the require back at
`ehrt.tools.corpus.simhospital-corpus` (both projects that compose
these tests, `conformance`, already declare `poly/tools` too, so its
test dir is on the classpath alongside corpus-io's). A third miss,
`mutate_test.clj`'s own `er7` require, was caught the same way.
Default test placement matches AR-3's own default (docs-tooling
precedent): `projects/conformance` and `projects/ehrt-cli` both
gained a real `poly/corpus-io` dependency; `projects/integration`
gained it too, but for a different reason than test-hosting —
`poly/tools`'s own src now requires it transitively, and its own
`zero_flag_reproducibility_test.clj` requires it directly.

**`:necessary` re-derivation (AR-6, same method as ADR-0016).**
Every entry cleared to `[]`, `poly check` run once to see exactly
what's unreachable without an override:

| Project | Before this stage | After | Why |
|---|---|---|---|
| `ehrt-cli` | *(no key)* | *(no key)* | `bases/cli` directly requires `ehrt.tools.interface`, `ehrt.corpus-io.interface`, and `ehrt.docs-tooling.interface` now — every other declared brick, including `corpus-io`, is real-edge-reachable transitively through those three. |
| `conformance` | `["docs-tooling"]` | `["docs-tooling"]` (unchanged) | `corpus-io` needs no separate entry: reachable transitively the moment `docs-tooling` is force-included (`docs-tooling → tools → corpus-io`, and `docs-tooling → corpus-io` directly, both real now). |
| `integration` | `["tools"]` | *(no key)* | Empirical finding, not carried forward from ADR-0016's table: with `:necessary []`, `poly check` reports zero warnings for this project. `corpus-io`'s own presence (real `poly/tools → poly/corpus-io` src edge) changed the graph's connectivity in a way this stage did not fully trace by hand — recorded as observed, not re-derived from first principles, per the deviation record below. |

Confirmed: `clojure -M:poly check` — `OK`, zero warnings, with the
table above as the final `workspace.edn` state.

### Verification

Characterization baseline (before any Step 2 edit, HEAD `039805e`):
per-push lane (`skip:integration`) exit 0, **193** `Testing ehrt.*`
occurrences; sha256 pins of the four generated docs; byte captures of
`corpus generate sim`, `corpus intake` (dir: source), `corpus
operators --format v2`, `gate v2` (fixture), `corpus mutate`
(blank-required-field on the generated corpus's own `msg-000.hl7`).

Post-move verification, same commands: `clojure -M:poly check` —
`OK` (after the structure-currency red/green cycle below).
Per-push lane: exit 0, **193** occurrences — unchanged count, and the
full sorted namespace-occurrence list diffed byte-for-byte against the
baseline shows exactly the nine moved namespaces' two occurrences
each renamed `ehrt.tools.corpus.* → ehrt.corpus-io.*`, nothing added,
nothing dropped, nothing else touched. Generated docs: all four
sha256-identical to the pre-move baseline (docsgen/docs-tooling
untouched by this stage, as expected). CLI baseline commands re-run
into a fresh scratch dir: every output file byte-identical to the
baseline except the two expected differences — the embedded scratch
directory path itself (different invocation, different `--out-dir`),
and `operation-manifest.edn`'s `:git` field gaining a `-dirty` suffix
(this session's own uncommitted tree, correctly reported by `git
describe`, not a regression). Integration lane: run once, see below.

**The structure-currency red moment (AR-6 evidence, same mechanism
stage 1 exercised).** Before `AGENTS.md`/`docs/dev/architecture.md`
were updated, the per-push lane ran with `components/corpus-io`
already on disk — `ehrt.docs-tooling.structure-currency-test`
correctly FAILED ("corpus-io ... is missing from
docs/dev/architecture.md's bricks table"), confirming the 2026-07-31
gate still works before either doc was touched. Both docs updated
(mermaid diagram, bricks table, projects table, closing "kept
current" pointer in `architecture.md`; "Landed so far" in
`AGENTS.md`); the lane's next run is green.

### Deviation record

**Two escalations, both resolved by author ruling in chat rather than
silently, both above (Decision).** Unlike stage 1's escalations
(which fired mid-Step-2, after code had already started moving), both
of this stage's escalations were caught during Step 1's own
characterization, before any file moved — the AskUserQuestion round
happened with the full edge map already in hand, so both rulings
landed as the FIRST attempt, not a reversed first-then-second pass
the way stage 1's circular-dependency finding was.

**`integration`'s `:necessary` re-derivation is reported as an
empirical observation, not a fully-traced graph proof.** ADR-0016
found `tools` needed an override in this project ("nothing else
integration declares has a real edge to it"); clearing `:necessary`
to `[]` this session shows zero warnings for the same project with
`corpus-io` now also declared. `corpus-io` itself has no edge into
`tools` (the whole point of AR-2's directional rule), so the
mechanism by which `tools` became reachable without an override was
not traced to a specific new edge — recorded as a genuine, verified
tool result (re-run three times, `project:integration`/
`project:conformance`/`project:ehrt-cli` filters all show the same
single conformance/docs-tooling warning) rather than silently
asserted as understood.

**Named-future list (for stage 3):** `components/tools`'s own
interface width (down nine defs from the nine moved namespaces, but
still wide — the domain surface: `corpus.*`, `check`, `sim`) is
unchanged in kind by this stage, per its own fence — stage 3's job.
`generator-source.clj` now carries three distinct concerns
(execute-and-wrap `resolve!`, validate-and-shape `generator-source`,
and URL-parsing `parse-source-designator`) that happen to share the
same domain edge, not a designed cohesion — worth a look if stage 3
finds a cleaner split, not attempted here (move, don't improve).
`docs/formats.md` and `docs/locators.md` (user-path docs, ADR-0010)
cite the pre-move `ehrt.tools.corpus.operation-manifest`/
`ehrt.tools.corpus.er7` paths, including a now-broken relative link
in `locators.md` to `er7.clj`'s old location — found, not fixed, per
ADR-0011's own precedent of recording rather than silently touching
user-path docs outside a stage's declared fence; an author call for
the P1-1 errata sweep, same as the citations ADR-0011 itself deferred.
`components/tools/docs/experiments/EXP-A4-results.md` cites
pre-Polylith `src/ehr_testing_tools/corpus/canonicalizers.clj` —
historical experiment-results record, left untouched (same class as
stage 1's `notes/docs-audit.md`, not a live document).

**A genuine characterization miss, caught only by running the
integration lane, not by Step 1's own caller map.** Step 1's
require-edge map searched for direct `ehrt.tools.corpus.X` requires to
find external consumers of the moving namespaces — it did not think
to also search `projects/*/test` for files reaching the same
functions indirectly through an `ehrt.tools.interface`-sourced alias
(the same pattern `bases/cli/src/ehrt/cli/core.clj` itself uses).
`projects/integration/test/ehrt/tools/intake_source_golden_test.clj`
aliases `ehrt.tools.interface` as `source-sink` and calls
`source-sink/dir-source` — invisible to a grep for
`ehrt.tools.corpus.source-sink`, only surfaced when the integration
lane's own compile step hit `No such var: source-sink/dir-source`.
Fixed by repointing that one alias to `ehrt.corpus-io.interface`
(same pattern as `zero_flag_reproducibility_test.clj`'s own
`canonicalizers` repoint). A repo-wide grep for every moved function
name across all test trees, run after this fix, found no further
instances — recorded here as the check that should have been part of
Step 1's own caller map from the start, not merely as a fix.

---

