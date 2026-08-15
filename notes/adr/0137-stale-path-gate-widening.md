## ADR-0137 — The stale-path gate gets every tracked doc surface: 25 dead links fixed, and two halves deliberately registered instead of improvised

**Status:** Accepted — 2026-08-15

### Context

Repo review 3's D1-2 found 25 dead markdown links on live
reader-facing surfaces, **all 25 under `components/<x>/docs/` and zero
under `docs/`** — and named the reason precisely: the gate that should
have caught them, `stale_path_test.clj`, was green, because its own
docstring read *"Deliberately scoped: this scan covers docs/ (plus the
use-cases.edn source above) only."* That gate's origin (P1-1,
2026-07-31, finding 4) was **this exact link family**. It fixed the
instances inside `docs/`, froze its population at `docs/`, and stayed
green ever after over a population that excluded where the rest lived.

D1-2 is the third recorded hit of the scan-root class and the first
found inside a *gate's* population rather than a probe's. The
highest-severity single instance was
`components/sim/docs/third-party-sources.md`: a licensing and
provenance document whose six dead links all pointed at
`notes/facts-register.md`, the register its own third-party claims
rest on.

D1-8 recorded the false-positive control that makes those 25
defensible, with the standing warning that a widened gate must encode
all four classes **or it lands noisy and gets weakened later**.

### Decision

Widen the gate with a fourth scan — dead markdown-link resolution over
every `*.md` under `docs/**` **and** `components/<x>/docs/**`, the
component doc roots enumerated from the filesystem rather than a
hand-maintained list — and fix all 25 links in the same commit, red
witnessed first.

Retire the "Deliberately scoped" sentence in favour of a **per-scan
population statement**: this namespace runs four independent scans over
three populations, and the docstring now names each population and how
each is enumerated. That is the population-closure law applied to the
gate that the law's own amendment caught.

All four D1-8 exclusion classes are encoded, three of them
structurally:

- **(a)** the shorthand backticked citation convention (`sim/run.clj`)
  — excluded by construction: this scan resolves markdown link
  destinations, and a backticked citation is never one.
- **(b)** generator template sources (`use-cases.edn`), whose links are
  authored to resolve at the generated output's location — excluded by
  construction again: the scan reads `*.md`, and the template's
  *rendered* output is in the population and resolves.
- **(c)** `docs/dev/migration/polylith-brief.md`'s external tutorial
  examples — the one class that cannot be structural, because those
  paths are shaped exactly like real ones; excluded by name, with a
  test that the same shape still trips from any other file.
- **(d)** percent-encoding is **decoded**, not excluded — an encoding
  step, so the research file whose name carries literal spaces resolves
  here as it does in any renderer.

### The 6 that were not a second class (ruling R-B1)

The register recorded 19 un-re-depthed `../` prefixes plus **6 whose
target is genuinely gone**, and the session prompt carried a
mid-session STOP so the author could rule per sentence on re-point vs.
rewrite vs. delete.

The stop found the premise wrong in the fixable direction: **both
targets exist, frozen.** These docs came from the pre-merge `tools`
repo, where `.agents/memory/patterns.md` and
`.agents/plans/archive/judge-gate-refactor.md` were live sibling paths;
the merge froze that whole tree into `notes/tools/agents/`. So the 6
are the **same** un-re-depthed relocation defect as the other 19,
differing only in that the destination moved too — not a second class
at all.

Evidence, verified before the ruling was sought: `.agents/memory/` has
only ever held `README.md` (no delete commit — `patterns.md` was never
created at the post-merge path, and that README says as much itself);
the frozen file's numbered **pattern 15, "Provenance is measured at the
point of execution,"** is verbatim what all five citing sentences
invoke (`unshare -r` remapping uid, "the JVM that actually runs the");
and the frozen `judge-gate-refactor.md` opens by describing itself as
the spent, repo-inventoried execution plan retained as record — a
verbatim match for `palgebra-design.md`'s Companion sentence.

Ruled **"Re-point all six"**, including `palgebra-design.md`'s visible
backticked label, so no rendered text names a path that does not exist.

### What this session deliberately did NOT build (rulings R-B2, R-B3)

Two of the prompt's design premises did not survive contact with the
tree. Both were reported at the stop and **registered rather than
improvised**, because loosening a gate quietly is the failure mode
D1-8 exists to prevent.

**R-B2 — the root-anchored *backticked* path half.** The prompt asked
for it, on the premise that class (a) excludes the shorthand
convention structurally. It does not: `sim/run.clj` is excluded, but
the convention is far broader than that shape. A
first-segment-is-a-repo-root-entry reading leaves **216** dead; adding
component-root resolution still leaves **95**, and many of those sit
inside `docs/`, which D1-2 measured as clean. The residue is dominated
by a class D1-8 never named — post-relocation **basename shorthand**
(`docs/notation.md` cited from anywhere, meaning that doc wherever it
now lives, e.g. `components/corpus/docs/notation.md`) — alongside
backticked command lines (`bin/ehrt play … --board 60`), globs
(`components/*/docs/*.md`), and `file.clj:21-23` line suffixes. The
alternative reading, "root-anchored" = leading slash, yields 8
candidates, all OS absolute paths (`/tmp/exp-d3/`,
`/root/.fhir/packages`) — 8 false positives and a vacuous check.
Neither reading reaches green today under D1-8's four exclusions, so
the half is a register row, not a silent new exclusion list. It has
real findings inside it: `components/tools` (a retired component) cited
twice in `docs/dev/architecture.md`, and
`.agents/plans/corpus-foundations.md` cited five times in
`source-sink-design.md`.

**R-B3 — widening the *denylist* families.** The retired sentence's
actual subject was scan 1, not the link scan. Widening scan 1 to
`components/<x>/docs/` turns **15** more files red — `ehr_testing_tools`
(1), `ehrt.tools.` (1), and `(?<!corpus/)docs/experiments/` (14, mostly
the same basename-shorthand false positive, since inside
`components/corpus/docs/` the sibling form *is* correct). Incompatible
with the exactly-25 red witness and outside this session's fence, so
scan 1 keeps its `docs/` population — stated as fact in the new
population statement rather than hidden behind "deliberately scoped."

### Consequences

- The gate's population is now derived from the tree: a component
  gaining a `docs/` directory joins the link scan by existing, with a
  test asserting exactly that.
- 25 dead links fixed, red witnessed at exactly 25 before any fix, and
  the exactly-19 / exactly-6 midpoint recorded as the fence it was.
- Every rewritten target was verified per-link against the resolved
  file rather than applied as a blind rewrite.
- Two register rows carry forward with real findings inside them; the
  next session inherits the analysis rather than rediscovering it.
- Zero `src/` outside the one test namespace; zero converter or
  generator changes; vendored bytes untouched.
