<!-- Attic file: notes/adr/0010-documentation-doctrine.md -->
<!-- Moved verbatim from notes/ADRs.md by scaffolding compaction B (2026-08-05, notes/ADRs.md ADR-0046). notes/ADRs.md remains the citation index -- see there. -->

## ADR-0010 — Documentation doctrine: audience-forked, user path complete at root

**Status:** Accepted (author-directed, autonomous session per R30), 2026-07-29.

### Context

`notes/docs-audit.md` (this same session, Phase 3) dispositioned all 76
files under root `docs/`, `components/sim/docs/`, and
`components/tools/docs/`: which move to a complete, history-free user
path at root `docs/`; which move to a maintainer path at `docs/dev/`;
which stay component-adjacent (needed only by someone working on that
specific component's own code); which merge (two glossaries, two
problem statements); which retire to `notes/` as frozen provenance
(stale copies, pre-positioning scratch material, and the two
now-superseded per-directory NAV `README.md` pages). This record is
the doctrine that audit's dispositions implement, and the standing
instruction for every doc landed after it.

### Decision

**Three classes of doc, one rule each.**

1. **User path (`docs/`).** Complete at this level — a domain expert or
   informaticist never needs to descend into `components/`, never meets
   Polylith, and never learns sim and tools were separate repos before
   this workspace existed (R34). Every doc here either teaches a task
   (`use-cases.md`, `simulate-your-facility.md`, `site-profiles.md`),
   is a reference a task points at (`cli.md`, `operators.md`,
   `locators.md`, `formats.md`, `judge-calibration.md`, `glossary.md`),
   or orients a reader who doesn't have a task yet (`what-is-this.md`).
2. **Dev path (`docs/dev/`).** For maintaining or extending this
   workspace. `architecture.md` (new this session) is its own map:
   bricks, projects, where the theory docs live. `positioning.md`
   (moved and revised), `way-of-working.md`, the Polylith migration
   brief, `notation.md`, `pipeline.md`, `components.md`,
   `engine-onboarding.md`, `source-sink-design.md`, and deprecation
   notices (`bases/sim-cli`, R33) all live here.
3. **Component-adjacent (`components/*/docs/`).** Stays exactly where
   it is — not a residual bucket, each doc earns this on its own terms
   (`notes/docs-audit.md`'s own reason column): sim's engine internals
   and theory docs, tools' `palgebra-design.md` and evidence-trail
   experiments, both components' `research/`, and the hand-authored
   docsgen sources (`pipeline.edn`, `use-cases.edn`, `signature.edn`) —
   material a contributor to that specific component's own code needs,
   that a user or general workspace maintainer never does.

**R34's history rule, applied literally.** The user path names no
Polylith term, no `components/` path, no pre-merge repo name as
architecture (citations of `ehr-testing-sim`/`ehr-testing-tools` as
*origin provenance* in ADRs and `notes/` are unaffected — this rule
governs the user path's own voice, not this workspace's historical
record). `docs/what-is-this.md` states the problem and what this
workspace does in the present tense; the two pre-merge problem
statements it merges are themselves the historical record now, retired
to `notes/sim/docs/`/`notes/tools/docs/`.

**Generated docs moved with their write-paths, in the same commit,
never hand-edited afterward.** `cli.md`, `operators.md`, `use-cases.md`
(user path) and `pipeline.md` (dev path, since it names catalytic/stage
jargon the equation notation defines, not a task doc) are `make
docsgen` output; their hand-authored sources
(`components/tools/docs/{pipeline,use-cases}.edn`) stay component-
adjacent. `docsgen.clj`/`pipeline.clj`/`usecases.clj`'s own `:out`
write-paths (via the `Makefile`'s targets) point at the new locations;
their own banner text and internal cross-references were audited and
fixed in the same pass — a banner claiming to be "generated from
docs/pipeline.edn" when the real source is
`components/tools/docs/pipeline.edn` is exactly the kind of stale
self-reference a doc move must not leave behind.

**Every internal link, script-verified, not eyeballed.** A link-
resolution script (Python, this session's own scratch tool, not
committed) walked every `[text](target)` in every file under `docs/`
plus the root `README.md`, resolved `target` relative to its own
file's directory, and flagged anything that didn't exist on disk.
First pass: 34 broken links (files that moved out from under a same-
directory link that used to work; source `.edn` cross-references whose
depth changed; two doc-move-independent, genuinely pre-existing dead
links to `.agents/memory/patterns.md`, never live-populated after the
merge, fixed to point at their real content's frozen-provenance
location, `notes/tools/agents/memory/patterns.md`). Second pass: 0.
This is the cold-reader check the session's own prompt asked for, run
as a mechanical walk rather than a manual click-through — stronger,
not weaker: a script doesn't get tired of checking link 40.

**Standing instruction: a new doc declares its row before it's
written.** User path, dev path, or component-adjacent — stated in the
PR or commit that adds it, not inferred later. A doc that doesn't
obviously fit one of the three rows is a signal to ask, not to guess
into `docs/` by default.

### Deviation record

**A CLI-name rename gap, found and closed while moving files, not
originally scoped to this phase.** ADR-0009's own rename swept
generated docs and code but not every hand-authored doc under
`components/tools/docs/` and `components/sim/docs/` — `locators.md`,
`formats.md`, `judge-calibration.md`, `notation.md`,
`source-sink-design.md` still said `ehr corpus mutate`/`` `ehr` `` in
prose, untouched because ADR-0009's own sweep scope was code and
generated output, not every hand-authored prose doc in the tree. Found
only because this phase's own file-by-file link audit required reading
each moved doc anyway; fixed in the same commit as the move, not
deferred to a third rename pass.

**`docs/locators.md`'s own source citations were stale by more than
this session's rename** — `src/ehr_testing_tools/locator.clj` and
`test/ehr_testing_tools/locators_doc_test.clj`, pre-Polylith paths
that never existed in this workspace at all (the pre-carve tools repo's
own tree, `src/ehr_testing_tools/` not `src/ehrt/tools/`). Fixed to
the real current paths (`components/kernel/src/ehrt/kernel/locator.clj`
post-ADR-0008, `components/tools/test/ehrt/tools/locators_doc_test.clj`)
while this page was already open for the CLI-name and path-depth
fixes above -- not a new sweep, a repair made cheap by already being in
the file for other reasons.

**Prose (non-link) staleness named and left, matching this workspace's
own established posture (ADR-0005, ADR-0008).** A handful of plain-text
`docs/source-sink-design.md`/`docs/components.md` citations inside
`use-cases.edn` and elsewhere reference old paths in running prose,
not inside a markdown link the cold-reader script could catch —
lower-value to chase than the linked and generated surfaces this
record's own verification actually covers, disclosed rather than
silently incomplete.

---

