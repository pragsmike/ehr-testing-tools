# Engine namespace extraction, 7 of N: the `config` and `assignment` clusters

Session record, 2026-08-30. HEAD at start `b177982`, confirmed equal to
`origin/main`; working clone clean, no fresh clone taken. Ceremony R30.
Program: `roadmap.md#engine-namespace-extraction-and-apply-unification`
(P5), author rulings C1(a) (`engine.clj` stays the facade, moved PUBLIC
vars get delegating defs, private movers widen with none, no test file
changes) and S1(a) (an equivalence proof replaces red-before-green).

This is the FIRST session of the program to land TWO namespaces. The
census's DAG (`.agents/plans/engine-extraction-census.md` section 3a)
puts `config` and `assignment` alongside `streams` and `state` in the
leaf rank, both of them ahead of `encounters`; six extractions have
already taken everything downstream, so both are free and neither
depends on the other. They are the last two leaves. `decide` and `run`
are what remain.

## 1. Step 1 -- the tip, the spans, the privacy markers, the re-exports

`b177982`. `bin/preflight` exit **0**, no findings. Two DISCLOSED
lines, both expected: "HEAD is not currently tagged stable-*" (tags
retired by the de-scaffold ruling of 2026-08-25) and "a run among the
last five is still in progress" -- that run is `b177982` itself, the
log-index close's own CI-record commit, whose predecessor `876fadf`
concluded green as run 33318257671. The four concluded runs on `main`
are all green.

Every span below was re-derived from THIS tree by paren balance, not
transcribed from the census, whose numbers are at `517a96d` -- **six**
extractions ago.

`engine.clj` is **3,571 lines / 99 top-level forms plus the `ns`**
here, against the census's 4,884 / 157. The partition closes: 99 +
`streams`' 16 + `state`'s 14 + `encounters`' 10 + `evolve`'s 32 +
`fold`'s 3 + `log-index`'s 10 = **184**, which is 181 plus the evolve,
fold and log-index moves' three delegating defs -- the arithmetic the
log-index record left standing, unchanged because this session has not
moved anything yet.

### The two regions

Both clusters are CONTIGUOUS, and they are adjacent to each other with
exactly one non-mover between them: `defn- pop-min` (`2096-2102`),
which the census assigns to `run`. That form stays. Neither cluster is
scattered, which makes this the easiest pair of regions of the seven
sessions -- the opposite end of the range from `log-index`'s four.

| cluster | region | forms | privacy HERE | census rendering |
|---|---|---:|---|---|
| `assignment` | `2018-2094` | 3 | -- | -- |
| | `2022-2040` `defn- weighted-pick` | | **private** | `defn-` -- correct |
| | `2042-2067` `defn assign-pathway` | | **public** | `defn` -- correct |
| | `2074-2094` `defn assign-module` | | **public** | `defn` -- correct |
| `config` | `2104-2246` | 5 | -- | -- |
| | `2104-2141` `def config-keys` | | **public** | `def` -- correct |
| | `2143-2180` `def Persons` | | **public** | `def` -- correct |
| | `2182-2221` `def Scheduling` | | **public** | `def` -- correct |
| | `2223-2238` `defn valid-scheduling?` | | **public** | `defn` -- correct |
| | `2240-2246` `defn valid-persons?` | | **public** | `defn` -- correct |

**The census's privacy renderings are 8 for 8 here**, and that is worth
recording precisely because the previous session's were 6 for 10. The
log-index cluster's four errors all had one cause -- the section-1
rendering drops `^:private` from a `def` -- and neither of these
clusters has a private `def` at all. Every marker above was still read
off the tree rather than trusted.

The two regions' interior comment blocks:

* `2018-2020`, the M3-adjacent banner introducing `weighted-pick` and
  `assign-pathway`. It introduces those forms and nothing else, so it
  travels with them.
* `2069-2073`, the M5b banner introducing `assign-module`. It carries a
  POSITIONAL claim -- "the SAME shape/law as `assign-pathway` just
  above" -- and that claim SURVIVES the move unchanged, because
  `assign-pathway` is still directly above `assign-module` in the new
  namespace. This is the first travelling positional claim of the seven
  sessions that needs no restatement; the log-index cluster's had to be
  rewritten because the form it pointed at was not coming along.
* `config` has NO interior comment block at all -- five forms,
  contiguous, nothing between them but blank lines. Only `fold` has had
  that property before.

The span convention differs from the census's and the difference is not
drift: the census partitions every line of the file, so a form's span
runs to the line before the next form begins and absorbs the blank and
comment lines after it. The spans above are paren-balance spans of the
forms themselves. Census `assignment` 74 lines / here 66 form lines
inside a 77-line region; census `config` 144 / here 139 inside 143.

### `interface.clj`: the channel's expectation, verified

The channel expected `config-keys` and `valid-persons?` to be on the
re-export list. **Both are** -- `interface.clj:46` `(def config-keys
engine/config-keys)` and `:82` `(def valid-persons? engine/valid-
persons?)`, and census constraint 4 names both. No other mover is on
that list: not `assign-pathway`, not `assign-module`, not `Persons`,
not `Scheduling`, not `valid-scheduling?`. `interface.clj` is not
edited by this session.

### Delegating defs owed, and what makes each load-bearing

Seven of the eight movers are public, so C1(a) owes **seven delegating
defs** -- more than any prior session of the program (`streams` 11 is
larger; `state` 13 larger still; but those were single-cluster
sessions, and this is the largest obligation per cluster since
`state`). `weighted-pick` is `defn-` and gets none under constraint 5.

| mover | resolves for | class |
|---|---|---|
| `config-keys` | `interface.clj:46` (constraint 4), and hence `identifiers.clj:148`'s `(select-keys opts engine/config-keys)` plus four `sim`-side test files that alias the INTERFACE; AND directly `engine_test.clj:1557`, `run_test.clj:99/:172/:284`, `emit_hl7_test.clj:1031`, `persons_test.clj:90` | **BOTH** -- the first mover in the program whose def is owed to `interface.clj` and to test files independently |
| `valid-persons?` | `interface.clj:82`, and through it `persons_run_test.clj:87` | interface |
| `valid-scheduling?` | `scheduling_test.clj:450` and `:452` ONLY | TEST-ONLY, the second such of the program after `events-for-patient` |
| `assign-pathway` | `engine_test.clj` -- seven calls across six lines (`:961`, `:966`, `:967`, `:971`, `:972`, `:980`) | test-only |
| `assign-module` | `engine_test.clj:1561`, `:1565`, `:1573` | test-only |
| `Persons` | **nothing** | see below |
| `Scheduling` | **nothing** | see below |

**`Persons` and `Scheduling` are the first movers of the program whose
delegating defs have no call site of any kind.** Verified rather than
assumed: a whole-repo scan for either name outside `engine.clj` returns
only a fixture string (`persons_test.clj:334`, `:name "Persons Death
Fixture"`) and five pieces of prose that use the English words
"Scheduling's four kinds" -- not one `engine/Persons` or
`engine/Scheduling`. Their defs are owed by C1(a)'s rule alone, not by
a resolver, and they are written anyway: the ruling says moved PUBLIC
vars get delegating defs, and narrowing that to "public vars someone
calls" would be this session inventing an exception the author did not
grant. Recorded here so the next reader knows the two defs are
deliberate and not cargo.

**Constraint 1 does not bite.** A whole-repo scan of every `with-redefs`
naming an `engine/`-qualified var returns `engine/run`, `engine/stream`
and `engine/stream-seed` and nothing else. No mover of either cluster
is redefined by any test, so no call site inside `engine.clj` needs to
stay unqualified for a test's sake.

### Outgoing edges: BOTH clusters are code-level leaves

Method: the 67 top-level names `engine.clj` defines, each searched as a
whole symbol against each cluster's whole text, then every hit read in
place to separate code from docstring.

* **`assignment` -> nothing.** Four hits: `weighted-pick`,
  `assign-pathway` and `assign-module` are intra-cluster, and `run`
  occurs twice, both in `assign-pathway`'s docstring ("see `run`'s own
  docstring"). The three bodies call `.nextDouble` on the `^Random`
  they are handed, `filter`/`filterv`, and each other. Nothing else.
* **`config` -> nothing.** Ten hits: five intra-cluster, and
  `bed-status-change`, `compile-patient`, `decide`, `run` and `stream`
  all occur ONLY inside docstrings and comments (`config-keys`'
  `:bed-cycle` comment, `Persons`' `:personas` line, `Scheduling`'s
  `:follow-up` line, and `run`'s name throughout). The five bodies are a
  keyword vector, two malli schemas, `(m/validate Scheduling ...)` plus
  an arithmetic band-sum, and `(m/validate Persons ...)`.

**The channel's prediction is corrected on the assignment half.** It
expected `assignment -> streams` was possible. It is not: neither
`assign-pathway` nor `assign-module` goes through `uniform-choice` or
`rand-int-in`; each takes `.nextDouble` off the `java.util.Random` its
caller hands it, which is exactly what makes the fixed-consumption law
readable in one function. The `config -> none` half is confirmed.

Outgoing edges OUTSIDE `engine.clj`: `assignment` needs
`(:import [java.util Random])` for its two `^Random` hints and nothing
else; `config` needs `[ehrt.sim-model.interface :as sim-model]` (for
`sim-model/Persona` inside `Persons`) and `[malli.core :as m]`. Both are
already `engine.clj`'s own requires, and `state.clj` already carries
exactly that pair, so **no `deps.edn` and no `workspace.edn` change**.

## 2. Step 2 -- ONE constraint-6 citation sweep, covering both clusters

Census section 5 item 6: a snippet pinned BY PATH from another brick's
doc is invisible to a call-graph census, and cost the streams
extraction a red. The sweep therefore runs before either move and its
hit list lands before either move commit, every hit dispositioned.

### 2a. Method

**Level 0 -- bare names.** The eight mover names grepped across the
whole repo outside `engine.clj`, in `clj`/`cljc` and separately in
`md`/`edn`/`txt`/`yml`/`yaml`/`sh`/`html`/`svg`.

**Level 1 -- docstring and comment phrases.** Every string literal and
`;;` comment in the moved text, whitespace-normalised, cut into every
distinct six-word window of 25+ characters: **334 phrases** for
`assignment` and **705** for `config`. Each searched against the
whitespace-normalised text of **1,440** files (`target/`, `.git/`,
`.clj-kondo/`, `out/`, `node_modules/`, `.cpcache/` excluded).

**Level 2 -- attribution and positional citations**, swept in BOTH
directions: out of the clusters (prose elsewhere that names a mover)
and into them (prose inside the moved text that makes a positional
claim), plus every prior-extraction banner in `engine.clj`.

**Level 3 -- the gated registries**, each read directly rather than
inferred: `hand-owned-assets.edn`, both `docs/limitations.md` charter
registers, `exercised-sources.edn`, `.agents/reading-sets.edn`.

### 2b. What seven of eight movers being PUBLIC does to this sweep

It collapses most of it, and saying so up front is honest rather than
lazy. `engine/assign-pathway`, `engine/assign-module`,
`engine/config-keys`, `engine/Persons`, `engine/Scheduling`,
`engine/valid-scheduling?` and `engine/valid-persons?` all keep
resolving through their delegating defs, so a citation that names a
mover BY NAME, or even namespace-qualified as
`ehrt.sim-engine.engine/<mover>`, is still true after the move. The
level-0 sweep returns well over a hundred such hits -- ADRs 0027, 0036,
0043, 0104, 0109, 0110, 0142, 0171, 0173, 0174, 0175; the census; a
dozen session records and prompts; `demos/scenarios/ed-tuesday/`'s two
configs and its README; `sim-theory.edn`; `sim-model/config.clj`;
`site_profile.clj`; `emit_hl7.clj`; `hazards.clj`; `persona.clj`;
`gmf.clj`; `gmf_interpreter.clj`; `order_profiles.clj`;
`process.clj`; `run.clj` -- and **not one of them is falsified by a
namespace-qualified reference**, because every one of those references
names a var that still resolves.

What survives that filter is: prose that names the FILE or namespace a
mover LIVES IN as a placement claim, and any reference at all to
`weighted-pick`, the one private mover, which constraint 5 forbids
covering with a def.

### 2c. `weighted-pick` -- the one private mover, swept exhaustively

The name is common: `sim-model/persona.clj` has its own private
`weighted-pick`, `person-simulator` has `pp/weighted-pick`, and
`patient-simulator` has `weighted-pick-transition`. Every hit was read
in place to decide which function it names.

| hit | text | disposition |
|---|---|---|
| `sim_model/persona.clj:150-152` | "the same shape **`ehrt.sim-engine.engine`'s own private weighted-pick** uses, kept as an independent small copy here rather than a shared dependency" | **REPOINT** -- this sweep's sharpest finding |
| `engine_test.clj:977` | "a statistical sanity check on `weighted-pick`'s math" | bare name in a test docstring; a mechanism claim, no namespace and no position -- and C1(a) forbids touching test files. **Safe** |
| `notes/adr/0171:202` | "A separate private `weighted-pick`" | bare name, frozen ADR. **Safe** |
| `notes/adr/0104:76` | "`assign-module`'s own `(seq pool) (weighted-pick pool`" | bare name, frozen ADR. **Safe** |
| `notes/adr/0036:159,163` | the divide-by-zero guard and the draw-count law | bare names, frozen ADR. **Safe** |
| `.agents/plans/engine-extraction-census.md:214` | `* 3335-3354 defn- weighted-pick` | the document this session corrects, in section 6. **Safe** |
| `.agents/session-records/2026-08-25-arc-1-...md:38` | a line-number delta list | frozen record. **Safe** |
| `sim-model/resources/.../given-names.edn:9` | "ehrt.sim.persona's weighted-pick divides" | names PERSONA's copy, under a namespace that has not existed since the sim split. Already stale, not ours. **Safe, left alone** |
| `patient-simulator` and `person-simulator` hits | `weighted-pick-transition`, `pp/weighted-pick` | different functions entirely. **Safe** |

`persona.clj:150-152` is exactly the class the log-index sweep met in
`churn.clj` and the fold sweep met in `check.clj`: prose naming the
namespace a **private** var lives in, which constraint 5 forbids
covering with a delegating def. Left alone it would be ADR-0170's
pattern -- a claim true when written that nothing keeps true. It is a
docstring in a `src` file, changes no behaviour, and `persona.clj` is
not a test file, so C1(a)'s fence does not reach it.

Worth naming for its own sake: that docstring exists to explain why
`persona.clj` does NOT depend on the engine (the engine calls into
persona at patient-init, so the reverse edge would be circular). The
repoint makes the sentence name `ehrt.sim-engine.assignment` and
changes nothing about that argument -- and creates no require, because
the sentence is prose about a dependency the file deliberately does not
have.

### 2d. Placement claims about public movers -- ONE repoint owed

| hit | text | disposition |
|---|---|---|
| `docs/dev/simulator-architecture.md:395-396` | "the same law `assign-pathway`/`assign-module` already establish **in `engine.clj`**" | **REPOINT** to `assignment.clj` |
| `docs/dev/simulator-architecture.md:184-185` | "`assign-pathway` and `assign-module` (`defn assign-pathway`, `defn assign-module`) are the load-bearing worked examples" | **SAFE, unedited** -- see below |
| `patient_simulator/gmf.clj:1989-1990` | "`ehrt.sim-engine.engine/assign-module` is this schema's own resolver -- kept there, not here, mirroring `assign-pathway`'s own placement" | **SAFE** -- a namespace-qualified reference to a PUBLIC mover, which the delegating def forwards; the "there, not here" contrast is with `gmf.clj` and stays true |
| `sim/docs/sim-theory.edn:49`, `:109` | `ehrt.sim-engine.engine/assign-module`, `ehrt.sim-engine.engine/config-keys` | same class. **Safe** |
| `person_simulator/hazards.clj:18`, `consumption_test.clj:8`, `sim_model/persona.clj:33`, `:243`, `sim_engine/order_profiles.clj:89`, `emit_hl7.clj:1826`, `gmf_interpreter.clj:938`, `sim/run.clj:270` | `ehrt.sim-engine.engine/assign-pathway` / `assign-module` as the named precedent for a fixed-consumption law | same class, eight files. **Safe** |
| `notes/adr/0174:397` | "`config-keys` list (`engine.clj:2158`)" | a PATH-AND-LINE attribution in a frozen ADR. Already stale at this tip (`config-keys` is at `2104-2141`), and stale before this session touched anything. The repo does not repoint ADRs. **Left alone, recorded** |
| `projects/conformance/test-fixtures/sim-configs/full-capability.edn:47` | "`ehr-testing-sim.engine/assign-module`" | a namespace retired at the sim split. Already stale. **Left alone** |
| `notes/sim/agents/plans/roadmap.md:250` | "`ehr-testing-sim.engine/config-keys` is now the canonical, documented list" | frozen historical roadmap under `notes/sim/`, same retired namespace. **Left alone** |
| `demos/scenarios/ed-tuesday/config.edn`, `config-latency.edn`, `README.md`, `clinic-decade/config.edn` | seven `ehrt.sim-engine.engine/config-keys` non-membership claims about `:latency`, `:site-profile`, `:charges` | LIVE shipped surfaces, and every one names a public mover the def forwards. **Safe** |

**Why `:184-185` is safe and `:395` is not.** Both are in the same live
onboarding doc. `:184-185` names the movers by DEFINING FORM (`defn
assign-pathway`) and no file -- which is precisely the shape repo-review
row L2-17's repair converted twelve stale `engine.clj:NNN` citations
INTO, and it survives this move untouched because a `defn` keeps its
name wherever it lives. That repair paying off in a later session is
worth recording as evidence the class was actually closed and not just
patched. `:395` names the file, and after the move the law is
established in `assignment.clj`, so the sentence goes stale. Same doc,
same paragraph subject, opposite dispositions, for the reason L2-17
identified.

### 2e. Constraint 2's class -- a doc citing a mover's BODY, not its name

Census constraint 2 records the streams extraction's own near-miss:
`docs/consuming-ground-truth.md` names "`ehrt.sim-engine.engine/stream-
scheme`'s own docstring" as an authority, and a bare delegating def
would have made that citation resolve to nothing. The remedy there was
to carry the docstring onto the delegating def. **The same page cites
`config-keys` twice, and it cites something a delegating def CANNOT
carry.**

| hit | text | disposition |
|---|---|---|
| `docs/consuming-ground-truth.md:149` | "The canonical list is `ehrt.sim-engine.engine/config-keys` -- nineteen keys ... **and it carries a per-key comment for each**" | **REPOINT** |
| `docs/consuming-ground-truth.md:596` | "`ehrt.sim-engine.engine/config-keys` -- the canonical engine-key list, **with a comment per key**" | **REPOINT** |

`config-keys`' per-key comments -- the `:encounters`, `:bed-cycle` and
`:scheduling` opt-in paragraphs -- are `;;` lines INSIDE the vector
literal. They are not the docstring, so constraint 2's remedy does not
apply: they travel with the form and cannot be carried onto a
delegating def by any means. The VAR still resolves and its value is
still the same nineteen keys; what stops being true is the sentence's
second half. Both are repointed to `ehrt.sim-engine.config/config-keys`,
which is where the list and its comments will live.

Costs no gate: `docs/consuming-ground-truth.md` is hand-authored (not a
`docsgen` leaf), is not a `.agents/reading-sets.edn` member, is not a
`hand-owned-assets.edn` source, and has no `exercised-sources.edn` row.
Checked, not assumed.

The four other live-doc `config-keys` citations -- `docs/site-
profiles.md:253`, `docs/manual/04-time-on-the-wire.md:99`,
`docs/dev/architecture.md:109`, `components/sim/docs/sim-theory.md:134`
-- are all NON-MEMBERSHIP claims ("`:latency` never reaches
`engine/config-keys`", "`:site-profile` is not a member") about a var
that still resolves and still holds the same set. **All safe.**

### 2f. Into the clusters, and the prior-extraction banners

* `engine.clj:2069-2073`, `assign-module`'s own banner, makes the
  positional claim "the SAME shape/law as `assign-pathway` just above".
  It travels, and `assign-pathway` is still just above. **True after
  the move, unedited** -- the first travelling positional claim of the
  seven sessions that needs no restatement.
* `engine.clj:2018-2020`, the M3-adjacent banner, makes no positional
  claim. Travels verbatim.
* Every prior-extraction banner in `engine.clj` was read for a mover
  name: the `state` banner (`98-120`), `streams` (`211-227`),
  `encounters` (`265-281`), `evolve` (`1871-1897`) and `fold`
  (`1907-1950`). **Not one names a mover of either cluster.** The
  `log-index` extraction left no banner at all -- its one delegating def
  carries its own docstring pointer instead (`engine.clj:325-331`) --
  so there is nothing there to go stale either.
* Everything in `engine.clj` that names a moving name and STAYS behind
  was read line by line: `839` ("`valid-scheduling?` is what guarantees
  the remainder is not negative"), `1253` ("the fixed-consumption law
  `assign-pathway` and ..."), `2495`, `2506`, `2988`, `2992`,
  `3072-3074`, `3128-3129`. Every one is a bare name in a mechanism
  claim -- no position, no namespace, no file. **All safe.** The four
  live call sites that stay (`2497`, `2521`, `3190`, `3196`) resolve
  through the delegating defs unqualified, exactly as they do today.

### 2g. Phrase hits -- shared prose, not citations

Level 1 returned hits in 5 files for `assignment` and 27 for `config`,
after excluding `.lsp/.cache/db.transit.json` (an editor index, a
derived artifact of the very source being swept, and by far the largest
hit count in both runs -- 161 and 285). Every remaining file was read.

The dominant pattern in the `config` run is a SHARED BANNER, not a
citation: `;; ARC 3B SWEEP <n> (ADR-0174 ...)` is the header convention
sweep-1/2/3 used in every file they touched, so it recurs in
`run_test.clj`, `run.clj`, `check.clj`, `state.clj`, `encounters.clj`,
`emit_hl7.clj`, `site_profile.clj`, `event_schema.clj`, `board.clj`,
`sim_model/config.clj`, `facility.clj`, `sim_model/interface.clj`,
`oracle_coverage_test.clj`, three demo configs and one session record.
It names an ADR and an arc, never `engine.clj` and never a mover.
**All safe.**

The rest, each read in place:

| file | disposition |
|---|---|
| `sim/docs/operational-models.md:277` | "one draw cannot land in two bands" restates `Scheduling`'s band law in the doc's own words; no path, no attribution. **Safe** |
| `docs/consuming-ground-truth.md:269` | "Every engine key in the table above is a fact generator, so it draws" -- generic prose about engine keys, not a quote of `config-keys`. **Safe** |
| `notes/sim/agents/plans/roadmap.md:251` | "the canonical, documented list of every key" -- frozen historical roadmap, retired namespace (2d). **Safe** |
| `person_simulator/process.clj:675` | `:population [{:person-id .. :id-tag ..} ...]` -- that component's OWN front door documenting its own config in the same notation. Coincidence of shape, not a citation. **Safe** |
| `sim_engine/person_fold.clj` (2) | "person-id -> that person's own death/t0" -- the sibling half of the same contract, its own prose. **Safe** |
| `sim_engine/interface.clj` (3) | "the two-layer treatment `:modules` already has" -- the seam's own restatement of `Persons`' rationale; fenced from edits anyway. **Safe** |
| `engine_test.clj` (5+1), `scheduling_test.clj` (6), `run_test.clj` (12), `persons_test.clj` (1) | restated docstring prose in test docstrings; bare names, mechanism claims. **Safe**, and C1(a) forbids touching them |
| `sim_model/persona.clj` (3) | the `weighted-pick` docstring -- **already dispositioned as the one REPOINT in 2c** |
| `patient_simulator/gmf.clj` (1) | the M5b banner -- dispositioned in 2d as **safe** |
| `sim/docs/sim-theory.edn` (4) | the M5b contract sentence -- dispositioned in 2d as **safe** |
| `notes/adr/0173` (7), `notes/adr/0174` (2) | frozen ADRs; the phrases are the opt-in law and the fact-generator sentence, both of which the ADRs authored and `config-keys` quotes, not the reverse. **Safe** |
| `.agents/session-records/` (3), `.agents/plans/` | frozen by construction. **Safe** |

### 2h. Level 3 -- the gated registries

* **`hand-owned-assets.edn`.** All five rows' sources read in full:
  `docs/dev/simulator-architecture.md` (twice), `components/corpus/
  docs/pipeline.edn`, `demos/scenarios/ed-tuesday/README.md`,
  `components/corpus/docs/palgebra-design.md`. **The tripwire WILL
  fire**, on the `gt-emitters.svg` row, because 2d's one repoint is in
  `simulator-architecture.md`. It fires for the THIRD time in four
  extractions and for the same structural reason the roadmap's P5 row
  already states: that doc names engine forms by defining form and by
  file. `two-clocks.svg` cites the same source but is `:verdict :stale`
  with a live `:stale-row`, and the tripwire (claim (d)) skips
  non-`:fresh` rows, so only the one row goes red. The extraction is
  therefore a **RED-FIRST commit** under
  `rulings.md#R-red-pushed-with-green` -- the test reads `git log -1` on
  the SOURCE, so no commit can carry the sha that names itself -- and it
  is pushed with the successor that bumps `:reviewed-at`, never alone.
  PREDICTED here, not discovered by CI.
* **`.agents/reading-sets.edn`.** `docs/dev/simulator-architecture.md`
  is a `:sim` set member with `:budget-lines 1405`, so the repoint moves
  a set member and `.agents/state-derived.md` must be regenerated in the
  same commit. This is the evolve session's own first-run red, predicted
  in advance here instead of met at the gate.
* **Both charter registers.** `components/person-simulator/docs/
  limitations.md` and `components/patient-simulator/docs/
  limitations.md` were grepped for all eight mover names and for
  `engine.clj`: **not one hit**. Constraint 6's original class -- a
  verbatim docstring phrase pinned by path from another brick's
  register -- does not arise for either cluster. Constraint 7's bare
  token scan is over `person-simulator`'s own `src`, sees `#{"stream"
  "newborn-id-tag"}`, and is untouched.
* **`exercised-sources.edn`.** No `sim-engine` row, no `engine.clj`
  path, no mover name. **Untouched.**

### 2i. Coverage honesty, said in advance

`config`'s five forms are validated at `run`'s entry guards and read by
`identifiers.clj`'s config projection; `assignment`'s three are called
once per patient in the pre-loop, on every run that carries `:pathways`
or `:module-assignment`. So unlike the log-index cluster -- which the
oracle's roots could not reach at all -- **both brackets do exercise
this pair**, `assign-pathway` and `assign-module` through the pathway
and module roots, and `config-keys`/`valid-persons?`/`valid-scheduling?`
through every root's own config parse. An IDENTICAL bracket here is
therefore real evidence rather than a vacuous pass. That said, `Persons`
and `Scheduling` are schemas consulted only on the guard path, and no
oracle root supplies a MALFORMED config, so the two `m/validate` calls
are exercised only in their true branch; the suite's own
`malformed-scheduling-is-a-result-not-a-throw` and
`outcome-rates-summing-past-one-are-refused` carry the false branch.

### 2j. The hit list, in one place

FOUR repoints owed, each stating something true today and false the
moment its forms leave. They land WITH their move commits, not before.

| # | hit | with which move | why no def can forward it |
|---|---|---|---|
| 1 | `sim_model/persona.clj:150-152` | `assignment` | names the namespace of a PRIVATE mover; constraint 5 forbids a def |
| 2 | `docs/dev/simulator-architecture.md:395-396` | `assignment` | a FILE placement claim, not a var reference |
| 3 | `docs/consuming-ground-truth.md:149` | `config` | cites the form's BODY COMMENTS, which no def carries |
| 4 | `docs/consuming-ground-truth.md:596` | `config` | same |

TWO consequential gates, both PREDICTED here rather than met at the
gate, and both attaching to repoint 2 alone:

* `hand-owned-asset-freshness-test` will go RED on `gt-emitters.svg` at
  the assignment commit. Its successor bumps `:reviewed-at`; the pair is
  pushed together (`rulings.md#R-red-pushed-with-green`).
* `state-derived-test` needs `.agents/state-derived.md` regenerated in
  the SAME commit, because `simulator-architecture.md` is a `:sim`
  reading-set member with a line budget.

The `config` move fires NEITHER: neither `consuming-ground-truth.md`
nor any file it touches is a registry source or a set member.
**`config` is therefore not a red-first commit and `assignment` is** --
which is why they are landed in that order, config first, so the
red-first pair is the last thing on the branch and the pushed tip is
green whatever else happens.

## 3. Step 3 -- `ehrt.sim-engine.config` (`bc595a5`)

Five forms, one contiguous region, `2104-2246`. **`config.clj`'s body
diffs against `engine.clj`'s own text at `b177982` as ZERO differing
lines** -- and not one widening line was needed, which no earlier
extraction could say: all five vars were already public, so constraint
5 never arose.

Five delegating defs, and what makes each load-bearing differs enough
to be worth writing down rather than summarising:

* `config-keys` -- `interface.clj:46`, which census constraint 4 pins,
  and through it `identifiers.clj:148`'s `(select-keys opts
  engine/config-keys)` plus `charges_run_test`, `siu_run_test`,
  `ladders_run_test` and `chatter_run_test`, all four of which alias the
  INTERFACE. AND, independently, six direct call sites against
  `ehrt.sim-engine.engine`: `engine_test.clj:1557`,
  `run_test.clj:99/:172/:284`, `emit_hl7_test.clj:1031`,
  `persons_test.clj:90`. **The first mover of the program whose def is
  owed to `interface.clj` and to test files independently** -- `fold`'s
  was interface-only, `log-index`'s test-only.
* `valid-persons?` -- `interface.clj:82`, and through it
  `persons_run_test.clj:87`.
* `valid-scheduling?` -- `scheduling_test.clj:450` and `:452`, and
  nothing else. The program's SECOND test-only def.
* `Persons`, `Scheduling` -- nothing at all, as section 1 records.

### Gates

* **`make test` MAKE_EXIT=0**, 16 min 22 s wall on an UNSAMPLED host --
  this run's own line, not a timing claim.

  **Namespaces 408 and tests 4,751 are IDENTICAL** to the log-index
  extraction's own recorded run. Assertions are **24,123**, +2 against
  its 24,121, explained to the assertion:
  `ehrt.docs-tooling.io-vocabulary-lint-test` reported **127** in each
  of the two projects it runs in there and **128** in each here, and the
  tree gained one production file. One new file is +1 twice -- the
  benign class the six prior extractions each recorded and this
  session's prompt named in advance.
* **`clojure -M:poly check`** OK.
* **`bin/regression-oracle 6a93136 bc595a5`**: `IDENTICAL: every root's
  digest matches`, `declared-digest-change: no`. That script's own
  output, per `rulings.md#R-oracle-script-contract`.
* **Live `-M:dev` resolution check.** All five vars resolve and are
  public in the new namespace; each delegating def is `identical?` to
  the var it delegates to; `interface/config-keys` is `identical?` to
  `engine/config-keys` at 19 keys; and both guard predicates were driven
  down BOTH branches -- a well-formed value, a malformed `:persons`, and
  a `:scheduling` whose bands sum past 1.

### One thing found and deliberately not fixed

`malli.util` was ALREADY an unused `:require` in `engine.clj` at
`b177982` -- `grep -c 'mu/'` returns 0 there. This move neither caused
it nor removes it. Named because a reader of the extraction diff will
notice `mu` surviving a commit that took the last malli-heavy forms out,
and might reasonably suspect the move dropped a use.

## 4. Step 4 -- `ehrt.sim-engine.assignment` (`b8b9acb`)

Three forms, one contiguous region, `2018-2094` at `b177982` (`2019-
2095` after the config commit's one added `:require` line -- the region
was re-located by CONTENT, never by the remembered number, and
re-diffed before the cut). **`assignment.clj`'s body diffs against
`engine.clj`'s own text as ZERO differing lines.**

**The travelling positional claim survives unedited**, the first of the
eight extractions for which that is true. `assign-module`'s M5b banner
says it has "the SAME shape/law as `assign-pathway` just above", and
`assign-pathway` is still directly above it. The `state` extraction had
to move a header block to keep such a claim true; `log-index` had to
RESTATE one, because the form it pointed at was not coming along.

### `weighted-pick` stays `defn-` -- constraint 5 read carefully

**This is the session's sharpest reading, and it went the other way
from the six extractions before it.** Census constraint 5 reads:

> **A private var that moves becomes public in its new namespace.**
> `rand-int-in`, `uniform-choice`, ... are all `defn-`/`^:private`
> today. They must NOT gain a delegating def in `engine.clj` -- that
> would widen the engine's public surface, which C1(a) does not ask for
> and `poly check` would not catch.

The PROHIBITION is the obligation, and it is honoured: `engine/
weighted-pick` does not resolve, asserted live under `-M:dev` rather
than inferred from the diff. The first sentence is a DESCRIPTION of
what happened in the streams cluster, and in every cluster since it was
FORCED by call sites left behind -- `streams`' four private movers,
`state`'s cycle breaker, `encounters`' ten, `log-index`'s nine all had
to stay reachable FROM `engine.clj`, so widening was the only way to
move them at all. `weighted-pick`'s only two callers are
`assign-pathway` and `assign-module`, which travel with it.

Two things then decide it. Widening would enlarge the new namespace's
public surface for no caller. And it would have **falsified this
commit's own repoint inside the commit that made it**: `sim_model/
persona.clj`'s docstring cites this function's PRIVACY -- "the same
shape `ehrt.sim-engine.assignment`'s own private `weighted-pick` uses,
kept as an independent small copy here" -- to explain why persona must
not depend on the engine. A public `weighted-pick` makes that sentence
wrong. Recorded at length because a later session reading only the
first line of constraint 5 would widen it and never notice.

### Gates

* **`make test` MAKE_EXIT=0**, 22 min 45 s wall, unsampled host.
  **Namespaces 408 and tests 4,751 are IDENTICAL** to both prior runs.
  Assertions **24,125**: +2 on the config commit's 24,123, +4 on
  log-index's 24,121, explained to the assertion each time --
  io-vocabulary-lint reports **129** per project here against 128 and
  127, and this session adds two production files at +1 twice each.
* **`clojure -M:poly check`** OK.
* **`bin/regression-oracle bc595a5 b8b9acb`**: `IDENTICAL: every root's
  digest matches`, no declaration.
* **`bin/ground-truth-bracket 6a93136 b8b9acb`**, named as spanning
  BOTH moves in one bracket, from the sweep sha to this one:
  `IDENTICAL: every digested root's :ground-truth matches ... (38
  roots)`, 3 skipped for carrying no `:ground-truth` key
  (`appendicitis`, `ear-infections`, `sore-throat`), no declaration.
* **Live `-M:dev` check.** All three vars resolve, `weighted-pick`
  private and the other two public; both delegating defs `identical?`;
  and **the fixed-consumption law driven on EVERY branch** --
  `assign-pathway`'s override and weighted branches leave the RNG at
  the same position as each other, and `assign-module`'s override,
  weighted and no-cover branches all three do. That law is what the
  namespace exists for, so it is tested at the seam rather than assumed
  across it.

### A self-inflicted red, and what did NOT catch it

The first run of this gate failed at **48 seconds**, `MAKE_EXIT=2`:

```
Syntax error macroexpanding clojure.core/ns at (ehrt/sim_engine/assignment.clj:1:1).
Call to clojure.core/ns did not conform to spec.
```

A scripted edit to the namespace's own ns DOCSTRING wrote unescaped
double quotes, closing the string early. No moved text was involved --
the body re-diffed verbatim after the repair, and the whole defect was
in prose this session wrote.

**`clojure -M:poly check` returned `OK` over that same tree.** That is
the THIRD consecutive session to catch `poly check` missing a broken
file in this brick (arc 4 sweeps 4 and 5 each caught it missing an
uncompilable docstring), and the first to catch it missing one that
does not even READ. The standing finding is therefore stronger than
"`poly check` does not compile": it does not parse either, and the only
thing that caught a namespace with an unbalanced string was the test
run itself.

## 5. The tripwire -- predicted, then held against a green suite
(`7ca549f`)

`hand-owned-asset-freshness-test` went red on `gt-emitters.svg` at
`b8b9acb`, exactly as section 2h predicted, and `7ca549f` is the green
successor that bumps `:reviewed-at` to `b8b9acb`. Pushed together, never
alone (`rulings.md#R-red-pushed-with-green`).

**The prediction had to survive its own gate saying otherwise.** `make
test` was GREEN over the very tree carrying the repoint, because the
test reads `git log -1` on the SOURCE and cannot see an uncommitted
edit. After the commit, `git log -1 -- docs/dev/simulator-architecture.md`
returns `b8b9acb` against `:reviewed-at "ff26bb6c"`. A green pre-push
suite is not evidence this row was not going to fire, and a session that
took the green as a refutation would have pushed a red tip.

**The trigger did not fire in SUBSTANCE, which is new.** This row's
trigger names "section 4's own equations". The three previous extraction
fires each landed inside the doc's engine prose -- section 1's brick
table, section 2's doctrine -- because those name engine forms by
DEFINING FORM. This one is one line in the ADR-0109 addendum under `## 5.
Extension point`, at `396`, where section 4 spans `193-372`. Section 4's
equation block was verified byte-identical by diffing `193-372` as a
BLOCK, not inferred from the hunk header; the whole file diff since
`ff26bb6c` is one hunk of one line.

**And one predicted gate did NOT fire.** Section 2j predicted
`state-derived-test` would need a regeneration in the same commit,
because `simulator-architecture.md` is a `:sim` reading-set member.
Measured: the repoint is LINE-NEUTRAL -- `engine.clj` becomes
`assignment.clj` inside one line -- so the set's actual **1341** does
not move and `make state-derived` produced no diff at all. The evolve
extraction's own repoint added lines and reddened that gate; this one
does not. **The prediction was refuted by its own measurement**, and is
recorded rather than quietly dropped: a sweep that predicts two fires
and gets one is more useful to the next session than one that reports
only the hit.

## 6. Closing arithmetic, census corrections, and what is next

### The partition still closes

Counted with **Clojure's own reader**, not by `grep -c '^('` -- which
overcounts `evolve.clj` by one, and not by the paren-balance scanner
used for the spans, which undercounts the same file by one. Two
independent instruments disagreeing on one file is why the authoritative
count is the reader's.

| namespace | lines | forms (+ ns) |
|---|---:|---:|
| `engine` | 3,469 | 98 |
| `streams` | 331 | 16 |
| `state` | 441 | 14 |
| `encounters` | 243 | 10 |
| `evolve` | 469 | 32 |
| `fold` | 155 | 3 |
| `log-index` | 301 | 10 |
| `config` | 187 | 5 |
| `assignment` | 144 | 3 |

98 + 16 + 14 + 10 + 32 + 3 + 10 + 5 + 3 = **191**, which is 181 plus
the evolve, fold, log-index, config and assignment moves' **ten**
delegating defs (1 + 1 + 1 + 5 + 2). `engine.clj` went 99 forms to 98
by losing eight and gaining seven, and 3,571 lines to 3,469.

### Census corrections, one sentence each

* Section 1's spans for both clusters are stale by six extractions:
  `assignment` is at `2018-2094` and `config` at `2104-2246` at
  `b177982`, not `3335-3408` and `3417-3560`.
* Section 1's privacy renderings for these two clusters are **correct,
  8 for 8** -- the first pair of clusters for which this record has
  nothing to correct there.
* Section 3a's edge table is right that `run -> assignment` is 2 edges
  and `run -> config` is 2, and right that neither cluster appears as a
  CALLER anywhere: both are code-level leaves, with no outgoing edge to
  any `engine.clj` form.
* Section 5 constraint 5's first sentence is a DESCRIPTION of the
  streams case, not an independent obligation; its prohibition is the
  obligation. See section 4.
* Section 5 constraint 2's remedy -- carry the docstring onto the
  delegating def -- does not reach a citation of a form's BODY comments.
  `config-keys` is the first mover to have one.

### For the author, before session 8

The prompt asks for anything bearing on whether `run` should extract or
remain as `engine.clj`'s residue. Three observations, offered as
evidence rather than as a recommendation:

1. **`decide` is the last cluster with a real choice in it, and `run`
   may not be a choice at all.** After `decide` moves, `engine.clj`'s
   residue is `pop-min`, `placeholder-registration`, `select-person`,
   `prelude`, `person-plan`, `run` -- six forms, of which `prelude`
   (611 lines) and `run` (594) are the two largest in the file -- plus
   whatever delegating defs the program has accumulated, which is ten
   today and will be substantially more after `decide`. The residue is
   therefore already mostly DEFS, not code.
2. **`run` is the only cluster that is a caller of everything and a
   callee of nothing internal.** Census section 3a gives it outgoing
   edges to seven clusters and no incoming edge at all. Extracting it
   would leave `engine.clj` as a file of delegating defs and nothing
   else -- which is a legitimate shape for a facade, but is a different
   decision from the seven moves so far, every one of which left real
   code behind.
3. **Constraint 1 binds `run` specifically and nothing else.**
   `engine_test.clj:2505` perturbs the partition by `with-redefs` on
   `ehrt.sim-engine.engine/stream`, and `run`'s four call sites must
   keep resolving through that var. This session re-derived the full
   `with-redefs` census: `engine/run`, `engine/stream`,
   `engine/stream-seed`, and nothing else in the tree. So a `run`
   extraction would have to keep `run` itself callable through
   `engine/run` AND keep `stream` unqualified inside the moved body --
   two constraints that no previous move had to satisfy at once.

### A budget this close nearly consumed, disclosed

`.agents/plans/roadmap.md` is an `:onboarding` reading-set member, and
the P5 row's growth is metered against that set. The first draft of this
session's P5 update was **+25 lines against 29 of headroom**, leaving
**4**. Not over budget, so `rulings.md#R-budget-stop` was not triggered
and no bump was available or wanted -- but four lines is not headroom, it
is a trap for session 8, whose cluster (`decide`, 59 forms) is the
largest in the census and will owe the longest row update of the
program.

The addition was therefore COMPACTED in this session rather than left
for the next one to hit: `:onboarding` now reads **1518 of 1530, twelve
lines of headroom**. The detail that came out of the roadmap is not
lost -- it is sections 4 and 5 above, which is where a register's
supporting narrative belongs. Recorded because the measurement only
exists if someone runs `make state-derived` and reads the table, and a
session that adds to a long row without doing that will silently spend
what is left.

### The close gate

`make test` at the close tree (record, prompt archive, compacted P5
row, regenerated `state-derived`/both `INDEX.md`): **MAKE_EXIT=0**, 24
min 44 s wall, unsampled host -- the third figure in this session's own
spread of 16 min 22 s / 22 min 45 s / 24 min 44 s over work of the same
size, which is why every wall figure here is this run's own line and not
a timing claim.

**Namespaces 408, tests 4,751, assertions 24,125 -- IDENTICAL to the
assignment gate**, as they must be: this commit adds no production
source file, so `io-vocabulary-lint` has nothing new to count.

The four gates this session put at risk were read individually rather
than inferred from `MAKE_EXIT`:

| gate | result |
|---|---|
| `hand-owned-asset-freshness-test` | 28 assertions, 0 failures -- **green again**, the bump commit closing the red-first pair |
| `state-derived-test` | 23 assertions, 0 failures |
| `index-completeness-test` | 47 assertions, 0 failures -- both new INDEX rows present |
| `roadmap-lint-test` | 32 assertions, 0 failures -- the compacted P5 row passes both its guards |

### CI at the pushed tip -- the close marker

`gh run view 33324437155`: **status=completed, conclusion=success**, at
`83633c5827f64533445d896a024539ebcf90e266`, the pushed tip
(https://github.com/pragsmike/ehr-testing-tools/actions/runs/33324437155).
That is the close marker under `rulings.md#R-session-verifies-ci-via-gh`,
which the de-scaffold ruling of 2026-08-25 retired as a TAG condition and
kept as this. No tag was paid.

It also settles the red-first pair from the outside: `b8b9acb` and
`7ca549f` were pushed together, and CI -- which sees committed state and
therefore CAN see what the local suite could not -- is green over the
range containing both.
