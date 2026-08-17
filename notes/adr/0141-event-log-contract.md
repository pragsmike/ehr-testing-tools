## ADR-0141 — The ground-truth event log becomes a contract: census, Event schema, generated formats.md section, custom-emitter use case

**Status:** Accepted (author-directed, autonomous session per R30),
2026-08-16.

### Context

A consumer wants to generate simulated hospital traffic with
`ehr-testing-tools` and translate it, with their own code, into
proprietary formats we cannot know ahead of time. The traffic must
therefore be available in the richest semantic form we have — the
ground-truth event log — and the mechanism for that already existed and
was load-bearing: both built-in emitters take `ground-truth` as their
first argument, `ehrt sim run --format ground-truth` emits the bare EDN
vector, `corpus generate sim` persists it as `events.edn` (ADR-0100),
and `sim check` and `play` already consume it.

What was missing was the CONTRACT. `sim-engine` had malli schemas for
`PatientState` and the fold's record types — the fold RESULTS — but
none for the event itself, and `docs/formats.md` had no event-log
section at all (`event-sourcing.md` gives the why, not the shape). So a
consumer reverse-engineered the event shape by reading `emit_hl7.clj`:
reading our HL7 emitter in order to write a not-HL7 emitter. That made
our emitter's field choices the de facto contract, and made a schema
CHANGE indistinguishable from a schema BREAK.

The author's rulings, verbatim:

> "Ok, add it, and make EDN be primary. JSON can be derived later. This
> will be a priority after the immediate review is done."

> "Choose a." — this arc runs BEFORE latency realism.

Two design questions were deliberately left unruled for the session to
put evidence behind. After Step 1's census:

> "Q-A a. Q-B b→a (a). Promote the tabulator to bin/event-census,
> author-licensed fence widening. Nested-:event collision: describe in
> schema (separate fact schemas) and lead the formats.md prose with the
> warning; no rename this arc. S-1..S-5 and the Z-segment asymmetry
> stay register rows. Proceed to Step 2."

And, on accepting Step 2 from a fresh clone:

> "Two-artifact gate stands; the ObservationEntry export is accepted
> as-is. Proceed to Step 3 as ruled … Add one sentence the ruling
> didn't carry: the [:re …] pattern dialect is java.util.regex."

### Decision

**Q-A (a): the event log is a PUBLIC, VERSIONED contract.**
`ehrt.sim-engine.event-schema/schema-version` is `"1.0.0"`, stamped
into every `sim run` manifest as `:event-schema-version`, so a log
always carries the version of the contract that produced it. Additive
change (a new kind, a new OPTIONAL key) does not bump; anything else
does; a key or kind slated for removal is marked deprecated in
`docs/formats.md` for one minor release first.

**Q-B (a): malli source of truth, ALSO exported as committed EDN.**
Every referenced schema is inlined — no registry — so
`event-schema.edn` is self-contained and readable without running
Clojure. EDN is primary; JSON is a projection under stated rules.

**The nested-`:event` collision is described, not renamed**, as its own
`PreHorizonFact` schema, and it LEADS the formats.md section.

### The census (Step 1)

[`.agents/plans/2026-08-16-event-log-census.md`](../../.agents/plans/2026-08-16-event-log-census.md),
derived from the tree by the co-landed
[`bin/event-census`](../../bin/event-census). Two populations,
reconciled exactly:

- **source-derived**: every `{:event …}` construction site in
  `engine.clj` — **21 kinds**;
- **corpus-derived**: **4,997 events across eleven runs**, `out/`
  cleared first — **21 kinds**, with no residue in either direction.

The two demo scenarios alone reach only 17. Four further corpora exist
because a census that stopped there would have declared four kinds
unreachable and one live consumer read dead.

Two claims in the driving prompt were **corrected against the tree**:
`replay`'s `{:before :after :world-before :world-after}` are a derived
trace record WRAPPING an event, not event keys; and the universal key
set is FOUR (`:event :t :participants :warm-up`), not five —
`:active-mrn` is absent from `:bed-swap`, `:merge`, `:step-rejected`.

`:t` is monotone within each run, and that is stated as a RUN-level
property (`run-t-monotone?`), never a per-event constraint: it is
meaningless across a concatenation, and nothing marks a run boundary.

### Red, then green (Step 2)

The schema was landed **one kind short** — `:care-plan-end` omitted —
to prove the gate bites. What makes that possible is the coverage
assertion: `every-declared-kind-is-actually-produced` compares the
fixture fleet's kinds against the declared vocabulary in BOTH
directions, so a validity test cannot pass vacuously for a kind nothing
produces.

```
RED:   produced but not declared in the Event schema: (:care-plan-end)
       expected: (= 21 (count declared))  actual: (not (= 21 20))
GREEN: Ran 18 tests containing 83 assertions. 0 failures, 0 errors.
```

The fleet (`ehrt.sim-engine.event-fleet`, test path) reaches all 21
kinds deterministically where the census needed 400-patient ten-year
runs and five churn seeds: the churn family is authored as explicit IR
steps, and one GMF fixture module walks the whole clinical vocabulary
in a single encounter.

**The version gate needed TWO artifacts.** `event-schema.edn` is the
current contract, regenerated by `make docsgen` and diffed by CI, so
the published EDN can never lag the source. `event-schema-baseline.edn`
is the FROZEN last-versioned contract, re-frozen only on a bump. One
file cannot do both jobs: a gate whose baseline is regenerated in the
same commit has an empty diff by construction, and could only ever
confirm the schema agrees with itself — the failure mode
`ehrt.sim.manifest`'s own retired mirror is this repo's standing lesson
for. `classify-change` defines "additive" mechanically and is itself
tested against seven hand-built diffs.

**Consumers became first-class.** `sim-emit-hl7`, `sim-emit-fhir` and
`sim-check` validate their own INPUT against the contract, in tests
only — zero production validation, zero runtime cost. `sim-emit-fhir`
turned out to be a different kind of consumer entirely: it reads
exactly one raw key (`:t`) and takes everything else from
`engine/replay`, so its real contract surface is `evolve`'s reads.

### Findings that changed the work, found by running

1. **The EDN export was not EDN.** `m/form` renders `Persona`'s
   `:dob`/`:phone`/`:ssn` as `#"…"` regex literals — a Clojure reader
   feature `clojure.edn/read-string` rejects outright. An artifact
   carrying one is unreadable by exactly the non-Clojure consumer it
   exists for. Normalized to `[:re "<pattern>"]`; the parity test could
   not have worked either way, since two `Pattern` objects with the
   same source are never `=`. Author-added: the dialect is
   `java.util.regex`, and `docs/formats.md` says so.

2. **`--format ground-truth --json` emits EDN, not JSON.** The flags
   read as if they compose; `--format` wins.

3. **A false claim, written and then caught.** The first draft of
   formats.md said `events.edn` is "byte-identical to what `--format
   ground-truth` prints". It is not — 169,945 vs 169,944 bytes, the
   printed form carrying a trailing newline. ADR-0100 is not wrong; its
   byte-equality test compares `events.edn` against `sim-run-command`'s
   internal `:bare-text`, never CLI stdout, and this session collapsed
   the two. Corrected precisely, because "byte-identical" is exactly
   the phrase someone wires a digest comparison to.

4. **The R-F5 fence class was reintroduced and caught.** Step 4's
   taught strip redirected into an `out/` subdirectory it never created
   (`FENCE_EXIT=1`) — the same class the D8-5 battery had fixed three
   commits earlier, in the first page written after that fix. A taught
   `mkdir -p` now leads the fence, proven by re-running from a removed
   directory.

5. **There are no instants in an event log.** Zero `#inst`; `:t` is an
   integer. The ISO-8601-vs-epoch-ms question the prompt raised is moot
   here, and the page says so rather than inventing a rule.

### Generated documentation (Step 3)

`docs/formats.md` gains "The event log", generated by
`ehrt.docs-tooling.event-log-doc` from the PUBLISHED ARTIFACTS as
files — never from the `sim-engine` namespace. `docs-tooling` gains no
dependency on the domain, and the page is rendered from the same bytes
a consumer receives, so it cannot describe something the artifact does
not say. The nested-`:event` warning is derived from `PreHorizonFact`'s
own enum, with tests on the derivation AND on its position. Examples
come from the same fleet the gate runs against, so the documented
example and the gated contract cannot drift.

`docs/formats.md` and both EDN resources join CI's freshness diff.

### Exercised from birth (Step 4)

`docs/use-cases/custom-emitter-from-the-event-log.md`, with
`bin/example-custom-emitter` (~40 lines, depending on NOTHING in this
repo — if a worked example needed our schema namespace, the log would
not be a consumable contract) and `bin/usecase-custom-emitter`,
registered in `exercised-sources.edn`. Page, example and exerciser all
land in one commit: the D8-5 battery's own proposed reader-path rule
(R-F8) satisfied by construction rather than retrofitted.

### Fences honoured

The event log's SHAPE did not change. Zero `decide`/`evolve` changes,
zero emitter production changes, vendored bytes verbatim,
`docs/notation.md` untouched. Every shape defect the census found is a
REGISTER ROW, not a fix — describing the current truth first, then
changing it under the versioned contract, is the whole point of the
tier this contract is published at.

Two disclosed widenings, both author-accepted: `bin/event-census`
(licensed), and a one-line `ObservationEntry` export on `sim-model`'s
interface — restating that shape in `sim-engine` would have built
exactly the mirror this repo already learned not to build.

### Consequences

A proprietary consumer can now build against a published, versioned,
machine-readable contract, with a worked example proving the seam, and
learn from the page itself that the log's sharpest edge is the nested
`:event` collision. A schema change is now distinguishable from a
schema break — mechanically, not by convention.

Register rows carried out of this arc (S-1, S-2, S-4, S-5, S-6 and the
Z-segment asymmetry) are recorded in the roadmap; S-3 was withdrawn as
correct behaviour on evidence.

### Index summary (moved verbatim from notes/ADRs.md by ADR-0143, 2026-08-16)

The ground-truth event log becomes a contract: the event vocabulary and per-kind key population derived FROM THE TREE by the co-landed `bin/event-census` (21 `{:event ...}` construction sites reconciled exactly against **4,997 events across eleven corpora**; the two demo scenarios alone reach only 17, so four further corpora exist because a census stopping there would have declared four kinds unreachable and one live consumer read dead), landing `ehrt.sim-engine.event-schema/Event` as a closed 21-branch multi-schema; author rulings **Q-A (a)** (public, VERSIONED — `:event-schema-version` in every manifest, additive change non-breaking, non-additive change bump-enforced by a test against a FROZEN baseline, since one artifact regenerated in the same commit could only confirm the schema agrees with itself) and **Q-B (a)** (malli source of truth ALSO exported as self-contained EDN, every reference inlined, with a parity test); landed RED-FIRST — the schema shipped one kind short to witness the coverage assertion bite (`produced but not declared: (:care-plan-end)`) before completing to green; both emitters and `sim-check` now validate their own INPUT against the contract in TESTS ONLY (zero runtime cost), and `sim-emit-fhir` turned out to read exactly one raw key, making `evolve` its real contract surface; `docs/formats.md` gains a GENERATED event-log section rendered from the published artifacts rather than the namespace, led — per ruling — by the nested-`:event` collision warning, itself derived from `PreHorizonFact`'s own enum; `docs/use-cases/custom-emitter-from-the-event-log.md` ships with `bin/example-custom-emitter` (depending on nothing in this repo, which is the demonstration) and an exerciser registered from birth (R-F8 by construction); four findings came from RUNNING rather than reading — the EDN export was not readable EDN (`#"..."` regex literals, now `[:re "<pattern>"]`, dialect `java.util.regex` stated per author instruction), `--format ground-truth --json` emits EDN not JSON, a "byte-identical" claim this session wrote was false by one trailing newline and is corrected precisely, and the R-F5 fence class was reintroduced and caught by executing the new page's own strip; the event log's SHAPE is unchanged (zero `decide`/`evolve`, zero emitter production, vendored bytes verbatim), every shape defect being a register row per ruling
