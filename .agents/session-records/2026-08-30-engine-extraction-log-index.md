# Engine namespace extraction, 6 of N: the `log-index` cluster

Session record, 2026-08-30. HEAD at start `c82436b`, confirmed equal to
`origin/main`; working clone clean, no fresh clone taken. Ceremony R30.
Program: `roadmap.md#engine-namespace-extraction-and-apply-unification`
(P5), author rulings C1(a) (`engine.clj` stays the facade, moved PUBLIC
vars get delegating defs, private movers widen with none, no test file
changes) and S1(a) (an equivalence proof replaces red-before-green).

The census's DAG (`.agents/plans/engine-extraction-census.md` section
3a) puts `log-index` after `fold`, extracted one session ago, and before
`decide`. This is the most SCATTERED cluster of the six -- four
non-contiguous regions -- and the first whose one delegating def is
owed to a TEST file rather than to `interface.clj`.

## 1. Step 1 -- the tip, the spans, the privacy markers, the re-export

`c82436b`. `bin/preflight` exit **0**, no findings; its one DISCLOSED
line is "HEAD is not currently tagged stable-*", expected under the
de-scaffold ruling of 2026-08-25 (tags retired). Last five CI runs on
`main` all green.

Every span below was re-derived from THIS tree by paren balance, not
transcribed from the census, whose own numbers are at `517a96d` --
**five** extractions ago.

`engine.clj` is **3,794 lines / 108 top-level forms plus the `ns`**
here, against the census's 4,884 / 157. The partition still closes:
108 + `streams`' 16 + `state`'s 14 + `encounters`' 10 + `evolve`'s 32 +
`fold`'s 3 = **183**, which is 181 plus the evolve and fold moves' two
delegating defs -- the arithmetic the fold record left standing,
carried one term further.

### The four regions, and what the census got wrong

| form | census (`517a96d`) | here (`c82436b`) | lines | privacy HERE | census rendering |
|---|---|---|---:|---|---|
| `defn events-for-patient` | `889-898` | **`324-332`** | 9 | **public** | `defn` — correct |
| `def ^:private reinstatable-event-types` | `2059-2074` | **`1496-1510`** | 15 | `^:private` | `def` — **drops the marker** |
| `defn- last-uncancelled-index` | `2075-2094` | **`1512-1530`** | 19 | `defn-` | correct |
| `def ^:private cited-opening-event-types` | `2409-2416` | **`1826-1832`** | 7 | `^:private` | `def` — **drops the marker** |
| `defn- last-cited-index` | `2417-2459` | **`1834-1875`** | 42 | `defn-` | correct |
| `defn- bed-reoccupied-by-someone-else?` | `3140-3154` | **`2050-2063`** | 14 | `defn-` | correct |
| `def ^:private status-a-cancel-target-leaves` | `3155-3170` | **`2065-2079`** | 15 | `^:private` | `def` — **drops the marker** |
| `def ^:private statuses-that-supersede-a-reinstatement` | `3171-3194` | **`2081-2103`** | 23 | `^:private` | `def` — **drops the marker** |
| `defn- subject-superseded?` | `3195-3228` | **`2105-2137`** | 33 | `defn-` | correct |
| `defn- reinstated-state` | `3229-3272` | **`2139-2181`** | 43 | `defn-` | correct |

**FOUR privacy renderings wrong in one cluster, the worst of the six.**
The census shows five of the ten as private; the tree says **nine of
ten**. The rendering drops `^:private` from every `def` systematically
-- the same convention the fold record diagnosed on
`bed-correction-event-types` -- and here it costs four rows at once.
The consequence is the whole shape of the move: constraint 5 governs
nine movers, and C1(a)'s delegating-def obligation arises **exactly
once**, on `events-for-patient`.

**The regions merged rather than drifted apart.** The census says four
non-contiguous regions and four is still right, but two of them are now
CONTIGUOUS PAIRS and one is a contiguous block of five:

  * **`324-332`** — `events-for-patient`, alone, between the `one-stream`
    delegating def and `defmulti decide`.
  * **`1496-1530`** — `reinstatable-event-types` + `last-uncancelled-index`,
    under the M2b churn-family banner at `1493-1494`. That banner
    introduces `documented-step-rejection-reasons` at `1532` as well,
    which is NOT a mover, so the banner **stays**.
  * **`1826-1875`** — `cited-opening-event-types` + `last-cited-index`.
  * **`2050-2181`** — `bed-reoccupied-by-someone-else?`,
    `status-a-cancel-target-leaves`,
    `statuses-that-supersede-a-reinstatement`, `subject-superseded?`,
    `reinstated-state`: five forms, blank-line separated, with ONE
    interior comment block above them at **`2044-2048`** that
    introduces exactly this group and nothing else, so it **travels**.

**Third census correction: the line total is wrong twice, and the two
figures disagree with each other.** Section 1's table row says 202
lines; section 1's own form listing says 230. The tree says **220**.
Neither census figure was right at `517a96d` either, since they cannot
both be.

**Fourth census correction: section 4d's apply-site-3 citation.** It
gives `reinstated-state`'s replay fallback as `engine.clj:3229-3271`;
here it is `2139-2181`, and the fallback expression is at `:2181`.

### The re-export check, and the first non-load-bearing delegating def

`components/sim-engine/src/ehrt/sim_engine/interface.clj` was read in
full. **NOT ONE of the ten movers is on its re-export list** -- the
list is `run`, `config-keys`, `compile-patient`, `person-plan`,
`person-deaths`, `valid-persons?`, `default-churn-profile`,
`sample-profile`, `replay`, `documented-step-rejection-reasons`, the
five event-contract vars, `default-profiles`, `abnormal-flag`, and the
five stream-partition vars. Census constraint 4's list does not name a
mover either.

So this is the **first extraction whose delegating def is owed to
something other than `interface.clj`**. `events-for-patient` is public
and C1(a) gives every public mover a def; what makes THIS one
load-bearing is `components/sim-engine/test/ehrt/sim_engine/
engine_test.clj`, which calls `engine/events-for-patient` at **ten**
sites (`:111`, `:113`, `:114`, `:1013`, `:1014`, `:1025`, `:1026`,
`:1156`, `:2466`, `:2565`, `:2566`) and which C1(a) forbids this
session to touch. Without the def those ten sites do not resolve.

### Edges, derived by whole-symbol scan

The 220 lines were extracted to a scratch file, string literals and
line comments stripped, and every token matched against the set of
every top-level name `engine.clj` defines.

**Outgoing: exactly TWO, and neither is stop-and-report.**

| callee | resolves to | treatment in the new namespace |
|---|---|---|
| `replay` (in `reinstated-state`'s fallback, `:2181`) | `engine.clj:2036`, itself the fold extraction's delegating `(def replay ... fold/replay)` | `fold/replay` — the SAME function object, taken directly rather than through the facade |
| `sim-model/occupancy-board` (in `bed-reoccupied-by-someone-else?`, `:2062`) | `ehrt.sim-model.interface`, already an `engine.clj` require | `sim-model/occupancy-board`, unchanged |

The prompt predicted `fold` and/or `evolve` and `state`, with `streams`
possible. **`evolve`, `state` and `streams` are all absent**: no mover
touches them. The require set for `ehrt.sim-engine.log-index` is
therefore exactly two namespaces, both already required by
`engine.clj`, both inside dependency the brick already declares -- so
`deps.edn` and `workspace.edn` do not move.

Nothing in the moved text resolves in `engine.clj` after the move. The
one candidate, `replay`, is a delegating def whose VALUE is
`fold/replay`, so calling `fold/replay` from the new namespace is not a
behaviour change -- it is the same var value reached one hop shorter,
which is the treatment the fold extraction itself gave `evolve/evolve`
and `state/initial-patient`.

**Incoming: THIRTEEN call sites, all staying in `engine.clj`, all
qualifying.** Census section 3a's edge COUNTS are confirmed exactly --
`decide → log-index` 11, `run → log-index` 2 -- once the count is read
as (form, callee) pairs rather than textual occurrences.

| staying caller | callee | site |
|---|---|---|
| `defmethod decide :cancel-admit` | `last-uncancelled-index` | `:1570` |
| `defmethod decide :medication-end` | `last-cited-index` | `:1915` |
| `defmethod decide :care-plan-end` | `last-cited-index` | `:1950` |
| `defmethod decide :cancel-transfer` | `last-uncancelled-index` | `:2186` |
| `defmethod decide :cancel-transfer` | `reinstated-state` | `:2190` |
| `defmethod decide :cancel-transfer` | `subject-superseded?` | `:2201` |
| `defmethod decide :cancel-transfer` | `bed-reoccupied-by-someone-else?` | `:2205` |
| `defmethod decide :cancel-discharge` | `last-uncancelled-index` | `:2217` |
| `defmethod decide :cancel-discharge` | `reinstated-state` | `:2221` |
| `defmethod decide :cancel-discharge` | `subject-superseded?` | `:2228` |
| `defmethod decide :cancel-discharge` | `bed-reoccupied-by-someone-else?` | `:2232` |
| `defn run` (the in-loop fold) | `reinstatable-event-types` | `:3700` |
| `defn run` (the in-loop fold) | `cited-opening-event-types` | `:3703` |

All thirteen name a var that is `defn-`/`^:private` today and becomes
public THERE under constraint 5, so all thirteen become
`log-index/`-qualified and none gains a def here.
`events-for-patient` has **zero** callers inside `engine.clj`, which is
what section 3a's table already implied by carrying no edge for it.

### This move moves an APPLY SITE, for the second session running

Census section 4d: `reinstated-state`'s `(:before (nth (replay
ground-truth) idx))` fallback IS apply site 3. It moves VERBATIM.
Its divergence from `run`'s in-loop fold (section 4b) and from
`replay`'s own (4c) is the same divergence the fold session recorded,
and it is RULED to be paid at application-path unification, not here.
Nothing this fallback folds is added, removed or reordered; the
`(contains? world :reinstate-index)` guard and the index read are the
same two expressions in the same order.

## 2. Step 2 -- the constraint-6 citation sweep, run BEFORE the move

Census section 5 item 6: a snippet pinned BY PATH from another brick's
doc is invisible to a call-graph census, and cost the streams
extraction a red. So the sweep runs before the move and its hit list
lands before the move commit, every hit dispositioned.

### 2a. Method

**Level 1 — docstring and comment phrases.** Every string literal in
the 220 moved lines, whitespace-normalised, cut into every distinct
six-word window of 25+ characters: **1,393 phrases**. Each searched
against the whitespace-normalised text of **1,360** files (`clj`,
`cljc`, `cljs`, `md`, `edn`, `txt`, `svg`, `yml`, `yaml`, `sh`, `html`;
`target/`, `.git/`, `.clj-kondo/`, `out/` excluded).

**Level 2 — attribution and positional citations**, swept in BOTH
directions: out of the cluster (prose elsewhere that names a mover) and
into it (prose inside the moved text that makes a positional claim).

**Level 3 — the gated registries**, each read directly rather than
inferred: `hand-owned-assets.edn`, both `docs/limitations.md` charter
registers, `exercised-sources.edn`, `.agents/reading-sets.edn`.

### 2b. Bare-name hits — recipe level 0

`grep` for the ten names across the repo outside `engine.clj` and the
test tree returns hits in ADRs (`0164`, `0169`, `0171`, `0174`), the
census itself, six session records, four plans/prompts, `formats.md`,
`patient-state-model.md`, `event_schema.clj`, and the two
`event-schema*.edn` resources. Every one of the resource and
`formats.md` hits is a `:reason` KEYWORD
(`:illegal-cancel-transfer-subject-superseded`), not the var --
**safe**, and the keyword is untouched by this move.

Session records, plans and prompts are frozen by construction. The
census is the document this session corrects, in its own section 6.

**One frozen-document hit is worth naming because it is NOT the fold
session's class.** `notes/adr/0174-...md:577` reads "(`engine.clj`'s
`reinstatable-event-types`, whose set is exactly ...)" -- a PATH
attribution of a private mover, where ADR-0174's two mentions in the
fold sweep were bare names. It goes stale with this move and is left
alone anyway: an ADR is a frozen ruling, and the repo does not repoint
them. Recorded here so a later reader finds the correction rather than
rediscovering the staleness.

`.agents/plans/2026-08-25-repo-review-findings.md` row D4-1 quotes
`reinstated-state`'s docstring verbatim AND pins it by path and line,
`engine.clj:1283-1293`. That citation is **already stale** at this tip
(the form is at `2139-2181`) and was stale before this session touched
anything -- ADR-0170's own pattern, in a frozen register. Left alone.

### 2c. Attribution and positional citations — FIVE repoints owed

They land WITH the move commit, not before it: each states something
that is true today and false the moment the forms leave.

| hit | text | disposition |
|---|---|---|
| `churn.clj:135` | "It is caught where the state exists, by `engine/subject-superseded?` at decide time" | **REPOINT** — a private mover; nothing forwards this |
| `churn.clj:159` | "(ehrt.sim-engine.engine's bed-reoccupied-by-someone-else? guard)" | **REPOINT** — same class |
| `fold.clj:35` | "`engine.clj`'s `reinstated-state` also still calls `replay` unqualified through it." | **REPOINT** — `reinstated-state` leaves, and its call becomes `fold/replay` |
| `engine.clj:2020` | the fold extraction's own banner: "`reinstated-state` below still calls `replay` unqualified through it too." | **REPOINT** — same fact, stated from the other side |
| `engine.clj:1912` | "The participant predicate is the one `last-uncancelled-index` (**above**) already uses" | **REPOINT** — a positional claim in a comment that STAYS, about a form that leaves |

The two `churn.clj` hits are this sweep's sharpest finding and are
exactly the class the fold session met in `check.clj`: prose naming the
namespace a **private** var lives in, which constraint 5 forbids
covering with a delegating def. Left alone they would be ADR-0170's
pattern -- a claim true when written that nothing keeps true. Both
edits are comment/docstring text in a `src` file, changing no
behaviour; C1(a)'s fence is on TEST files and `churn.clj` is not one.

**Into the cluster: ONE comment block travels, and its positional claim
is repointed rather than moved false.** `engine.clj:2044-2048` reads
"M2b cancel-transfer/cancel-discharge: defined here, AFTER `replay`,
because their decide methods query it directly". It introduces exactly
the five forms of region 4 and nothing else, so it travels with them --
but `replay` is not in `ehrt.sim-engine.log-index` at all, so "AFTER
`replay`" cannot survive the move in any reading. It is restated in the
new namespace to say the same mechanism (the reinstated prior state is
QUERIED FROM THE LOG at decide-time, `docs/patient-state-model.md`'s
shadow-field dissolution) without the positional half. Named here
rather than left for a reader to notice, and it is the only prose in
the moved text that changes at all.

**Everything else in `engine.clj` that names a moving name and stays
behind was read line by line and is safe:**

| line | text | why safe |
|---|---|---|
| `500-501` | "Same fallback rule as `reinstated-state` and `last-cited-index`, and for the same reason: on the KEY" | bare names, a MECHANISM claim, no position and no namespace |
| `1122` | "`:transfer` (`reinstatable-event-types`' own set, and for the same reason: they are what vacates)" | bare name, no position |
| `1858` | "Same fallback rule as `reinstated-state`" | inside a MOVING form (`last-cited-index`); travels with it and still true |
| `1887` | "the same rule `reinstated-state` and `last-cited-index` already follow" | bare names in `person-entry`, which stays; mechanism claim |
| `1997-2001` | the fold banner: "before `log-index`, whose `reinstated-state` calls `replay`" | a census-ORDER claim, and this move is what makes the namespace it names real; still true |
| `3506`, `3512` | "What `reinstated-state` reads instead of replaying the whole log per cancel"; "The KEY's presence is what tells `reinstated-state`" | bare names; "Written below" at `:3506` refers to the index WRITE, not the mover |
| `3521` | "Read by `last-cited-index`." | bare name, no position |
| `1444` | the bed-cycle in-flight comment that `check.clj:561` attributes to "`ehrt.sim-engine.engine`'s own comment" | at `1444`, ABOVE region 2; not a mover, so that attribution stays true |

`fold.clj:9` ("before `log-index`, whose `reinstated-state` calls
`replay`") is the same class as `engine.clj:1997-2001` -- an ordering
claim that this move VALIDATES rather than breaks. **Safe, unedited.**

`check.clj`'s own namespace attributions were each checked against the
region map: `:23` and `:69` name `replay` and `encounter-id-for`,
`:505` names the `:disposition` field, `:561` names the comment at
`1444`. **None names a mover.** The independent judge is untouched by
this extraction -- the first of the six for which that is true.

### 2d. Phrase hits that are shared prose, not citations

The 1,393-phrase sweep returned hits in **12 files**. Every one was
inspected:

| file | phrases | disposition |
|---|---:|---|
| `.agents/plans/2026-08-25-repo-review-findings.md` | 53 | frozen register; row D4-1 quotes `reinstated-state` verbatim with an already-stale line pin (2b) — **safe** |
| `.agents/session-records/2026-08-25-arc-0-performance-under-equivalence.md` | 32 | frozen record, ADR-0169's own session — **safe** |
| `components/sim-engine/test/ehrt/sim_engine/engine_test.clj` | 28 | a test file C1(a) forbids touching; its `engine/` calls resolve through the one delegating def — **safe** |
| `.agents/session-records/2026-08-29-ts-5-superseded-cancel.md` | 23 | frozen record, the session that WROTE `subject-superseded?` — **safe** |
| `components/sim-check/src/ehrt/sim_check/check.clj` | 12 | the event-validity table's cancel-* row, restated by the independent judge; no phrase is paired with a path — **safe** |
| `components/sim/docs/patient-state-model.md` | 9 | the same doctrine, in the document that OWNS it — **safe** |
| `components/sim/test/ehrt/sim/run_test.clj` | 4 | `reinstated-state`'s docstring cites this test BY NAME; the name does not change and the citation travels — **safe** |
| `.agents/plans/2026-08-24-traffic-scale-program.md` | 4 | frozen plan — **safe** |
| `notes/adr/0169-...md` | 3 | the frozen ruling the docstring restates — **safe** |
| `.agents/session-records/2026-08-24-throughput-spike.md` | 3 | frozen record — **safe** |
| `components/sim-model/src/ehrt/sim_model/config.clj` | 1 | "and for the same reason: on" — shared idiom, not a citation — **safe** |
| `demos/scenarios/clinic-decade/README.md`, `.agents/session-records/2026-08-29-ts-defects-and-blocked-cells.md` | 1 each | "which is the whole of why" — ordinary English — **safe** |

None pairs a phrase with a path; none is read by a gate.
**Disposition: safe, as a class** -- the finding the streams, state,
encounters, evolve and fold sessions each recorded, met a sixth time.

### 2e. Gates and tripwires checked, and NONE fires

* **The hand-owned-asset tripwire.** `hand-owned-assets.edn`'s four
  live sources are `docs/dev/simulator-architecture.md` (twice),
  `components/corpus/docs/pipeline.edn`,
  `demos/scenarios/ed-tuesday/README.md` and
  `components/corpus/docs/palgebra-design.md`. All four were read for
  every one of the ten mover names and for the phrase set. **Not one
  occurrence.** `simulator-architecture.md` names `defmulti decide`,
  `defmulti evolve`, `defn replay`, `defn run`, `defn assign-pathway`
  and `defn assign-module` by defining form -- and no mover of this
  cluster. So this extraction does NOT fire the tripwire, breaking a
  two-session run of fires, and the move is **not** a RED-FIRST commit.
  Checked in advance, not assumed from the absence of a red.
* **The charter registers.** `person-simulator/docs/limitations.md`
  rows 1 and 10 pin phrases against
  `components/sim-engine/src/ehrt/sim_engine/streams.clj`;
  `patient-simulator/docs/limitations.md` pins against that component's
  own `compile_trajectory.clj`. **No row pins `engine.clj`**, and no
  pinned phrase occurs in the moved text. Row 10's reverse-edge scan is
  over `person-simulator`'s own `src` (census constraint 7), so a new
  `sim-engine` namespace is invisible to it.
* **`exercised-sources.edn`.** 21 rows, **none** naming any
  `sim-engine` source. Untouched.
* **`.agents/reading-sets.edn` / `state-derived`.** The `:sim` set's
  members are `AGENTS.md`, `sim/interface.clj`, `engine-onboarding.md`,
  `components.md`, `simulator-architecture.md` and the build-session
  skill. This move edits none of them -- unlike the evolve and fold
  extractions, whose `simulator-architecture.md` repoints moved a
  reading-set member and (for evolve) reddened `state-derived-test`.
  Regenerated anyway before running the suite, for this record's own
  addition.
* **No namespace-enumerating gate exists.** Grepped for a test or
  registry pinning the set of `ehrt.sim-engine.*` namespaces; the only
  hits naming `ehrt.sim-engine.fold`/`.evolve` outside the brick are
  `check.clj`'s three repointed comments and `hand-owned-assets.edn`'s
  two prose notes, neither of which is a population check.

### 2f. Coverage honesty — recorded, not fixed

**Both oracle brackets are blind to what this cluster does.** The
oracle's 41 roots reach **no cancel decide at all**, which is the
standing finding of `.agents/memory`'s
`project_oracle_blind_to_cancel_replay_simcheck` and which TS-5's own
close measured directly: the only cancel decide in the whole gated
population is one legal `:cancel-discharge` in `seed-202`, and the
other seven reinstating cancels there carry `:in-error true` and come
from `decide :transfer-in-error`, which never routes through
`decide :cancel-transfer`. `last-cited-index` fares little better --
`project_gated_corpora_thin_on_churn_and_citations` records ZERO
successful citation resolutions across the gated corpora.

So `bin/regression-oracle` and `bin/ground-truth-bracket` reporting
IDENTICAL will prove that this move breaks nothing the corpora
EXERCISE -- which for six of the ten movers is nothing at all. What
carries the load instead:

  * the **suite**, and specifically `engine_test.clj`'s cancel family
    (`:643`, `:702`, `:723`, `:730`, `:740`) plus
    `cancel-reinstatement-survives-the-fold-carried-index`, which drive
    `decide` directly against hand-built worlds -- exactly the path the
    corpora do not reach;
  * `ehrt.sim.run-test/cancel-decides-reinstate-exactly-what-replay-
    would-hand-back` and `citation-resolution-matches-the-whole-log-
    scan`, the two post-hoc equivalence proofs ADR-0169 left standing;
  * a **live resolution check** under `-M:dev`, loading the new
    namespace and resolving every moved var plus the one delegating def.

Said here in advance, as the fold session said it, so that an IDENTICAL
bracket is not read as evidence it cannot be.

## 3. Step 3 -- the extraction

`ehrt.sim-engine.log-index`, 301 lines, `ns` plus the ten forms in
`engine.clj`'s own source order. Commit `25d926e`, parent `2cca99d`.

**Verbatim, and measured rather than asserted.** The 220 moved lines
were cut to a scratch file BEFORE any edit, and the new file's form
region diffed against it. The diff is **eleven lines**: the ten
widenings, and `reinstated-state`'s `(replay ground-truth)` becoming
`(fold/replay ground-truth)`. Nothing else in 220 lines differs by a
character.

| widening | from | to |
|---|---|---|
| `reinstatable-event-types` | `def ^:private` | `def` |
| `last-uncancelled-index` | `defn-` | `defn` |
| `cited-opening-event-types` | `def ^:private` | `def` |
| `last-cited-index` | `defn-` | `defn` |
| `bed-reoccupied-by-someone-else?` | `defn-` | `defn` |
| `status-a-cancel-target-leaves` | `def ^:private` | `def` |
| `statuses-that-supersede-a-reinstatement` | `def ^:private` | `def` |
| `subject-superseded?` | `defn-` | `defn` |
| `reinstated-state` | `defn-` | `defn` |
| `events-for-patient` | `defn` (public) | unchanged; **delegating def in `engine.clj`** |

`engine.clj` keeps `(def events-for-patient ... log-index/events-for-
patient)` in the place the `defn` stood, gains one require, and
`log-index/`-qualifies all thirteen call sites. Two continuation lines
of the two `last-cited-index` calls were re-indented by ten columns to
follow their own opening form; the `(and ...)` continuation at `:3477`
was NOT, because `(and` itself did not move.

**The partition closes, and the arithmetic is now three terms long.**
`engine.clj` is 3,571 lines / **99** top-level forms plus the `ns`.
99 + `streams`' 16 + `state`'s 14 + `encounters`' 10 + `evolve`'s 32 +
`fold`'s 3 + `log-index`'s 10 = **184**, which is 181 plus the evolve,
fold and log-index moves' three delegating defs.

### Gates

* **`make test` MAKE_EXIT=0**, 16 min 46 s wall / 22 min 21 s user on an
  UNSAMPLED host -- reported as this run's own line, not offered as a
  timing claim.

  **Namespaces 408 and tests 4,751 are IDENTICAL** to the fold
  extraction's own recorded run. Assertions are **+2** against its
  24,119, and the +2 is explained to the assertion rather than waved at:
  `ehrt.docs-tooling.io-vocabulary-lint-test` is a `doseq` over every
  production source file with one `is` per file plus a population guard;
  it reported **126** in each of the two projects it runs in at the fold
  session and reports **127** in each here, and the tree gained one
  file. One new file is +1 twice. The same benign class the streams,
  state, encounters, evolve and fold sessions each recorded, and the
  class this session's own prompt named in advance.

  **The first run was green, and that is the disclosure.** The evolve
  session's first run was RED on `state-derived-test` because its
  `simulator-architecture.md` repoint moved a `:sim` reading-set member.
  This session's sweep (2e) established in advance that NO reading-set
  member is touched -- the five repoints land in `churn.clj`, `fold.clj`
  and `engine.clj`, none of them a set member -- so the gate had nothing
  to fire on. `.agents/state-derived.md` was regenerated at step 2
  anyway, for this record's own addition. A green gate proves nothing
  about whether it was going to fire; this one was not.

* **`clojure -M:poly check`** OK. Because `poly check` does not compile
  -- a standing finding of the arc-4 sweeps, not new here -- the real
  check is the `-M:dev` load below.

* **`bin/regression-oracle 2cca99d 25d926e`** -- the script's own
  output: `IDENTICAL: every root's digest matches between 2cca99d and
  25d926e`, over **41 roots**, `declared-digest-change: no (soundness:
  yes outside the leading docstring)`. No declaration was owed and none
  was made.

* **The live resolution check**, and it is what actually covers this
  cluster. Under `-M:dev`:

  * all ten vars resolve in `ehrt.sim-engine.log-index` and all ten are
    public -- `ns-publics` returns exactly the ten, no more;
  * `engine/events-for-patient` is `identical?` to
    `log-index/events-for-patient` -- the delegating def holds the same
    object, not a copy of the code;
  * `engine/replay` is `identical?` to `fold/replay`, which is what
    makes `reinstated-state`'s new direct call the same function reached
    one hop shorter rather than a different one;
  * every mover was driven on a three-event log: `events-for-patient`
    returns `[:registered :admission :discharge]`;
    `last-uncancelled-index` returns `1`; `reinstatable-event-types` and
    `cited-opening-event-types` answer their memberships;
    `last-cited-index` returns nil on a nil citation (its own guard);
    `subject-superseded?` returns **true** for `:discharged`/
    `:cancel-transfer` and **false** for `:discharged`/
    `:cancel-discharge`, which is TS-5's asymmetry reproduced exactly;
  * **apply site 3 was driven down BOTH branches** -- `reinstated-state`
    with a carried `:reinstate-index` returns the carried entry, and
    with no such key it takes the `fold/replay` fallback and returns the
    same pre-discharge `:admitted` status. The moved apply site works,
    and it works the same way on both paths.

## 4. Step 4 -- the ground-truth bracket

**`bin/ground-truth-bracket 2cca99d 25d926e`** -- the script's own
output: `IDENTICAL: every digested root's :ground-truth matches between
2cca99d and 25d926e (38 roots)`, with `coverage: 38 roots carry
:ground-truth and are digested; 3 skipped (no such key):
appendicitis.edn, ear-infections.edn, sore-throat.edn` and
`declared-digest-change: no`.

**BOTH brackets IDENTICAL at the move commit, with no declaration owed
-- the DARK bracket, and the strongest proof a pure refactor can
offer.** It is also, for this cluster and this cluster alone among the
six, the WEAKEST evidence, for the reason section 2f recorded in
advance: the oracle's roots reach no cancel decide and resolve no
citation, so six of the ten movers are simply not exercised by either
bracket. The brackets prove the four that are -- `events-for-patient`,
and the three log indexes `run`'s own loop maintains through
`reinstatable-event-types` and `cited-opening-event-types` -- and the
suite plus the live check prove the rest. Recorded this way rather than
reported as a clean sweep.

## 5. Census corrections

Five, each one sentence:

1. **Section 1 drops `^:private` from four of this cluster's `def`
   forms** -- `reinstatable-event-types`, `cited-opening-event-types`,
   `status-a-cancel-target-leaves`,
   `statuses-that-supersede-a-reinstatement` -- so it shows five private
   movers where the tree has nine.
2. **Section 1's two line totals for this cluster disagree with each
   other** (202 in the table, 230 in the form listing) and neither
   matches the tree's 220.
3. **Section 1's spans are five extractions stale**, and the four
   regions have MERGED into two contiguous pairs, one contiguous block
   of five, and one lone form -- not drifted apart.
4. **Section 4d's apply-site-3 citation `engine.clj:3229-3271` is
   stale**; the form was at `2139-2181` at `c82436b` and now lives in
   `log-index.clj`.
5. **Section 3a's edge counts are CORRECT** -- `decide → log-index` 11
   and `run → log-index` 2, confirmed exactly against the tree when read
   as (form, callee) pairs -- and this is recorded as a correction only
   in the sense that it is the first cluster for which the census's
   numbers were checked and found right.

## 6. P5 after this session

**SIX landings**, in the census's own dependency order:

| # | cluster | namespace | delegating defs |
|---:|---|---|---:|
| 1 | `streams` | `ehrt.sim-engine.streams` | 5 |
| 2 | `state` | `ehrt.sim-engine.state` | 0 |
| 3 | `encounters` | `ehrt.sim-engine.encounters` | 0 |
| 4 | `evolve` | `ehrt.sim-engine.evolve` | 1 |
| 5 | `fold` | `ehrt.sim-engine.fold` | 1 |
| 6 | `log-index` | `ehrt.sim-engine.log-index` | 1 |

`engine.clj` is down from 4,884 lines / 157 forms at the census sha to
**3,571 / 99**. What remains under the census's map is `decide` (59
forms, the largest cluster by a wide margin), `assignment` (3), `config`
(5) and `run` (6) -- and `decide` is the next one the DAG allows, since
every incoming edge this session left behind points at it.

**Two apply sites of the three now live outside `engine.clj`** (`replay`
in `fold`, `reinstated-state`'s fallback in `log-index`); `run`'s
in-loop fold, apply site 1, is the one still inside it. The ruled
unification pass therefore now spans three namespaces rather than one,
which is a fact for that session's scoping and not a cost this one
incurred.

### The require set, re-confirmed against step 1

`ehrt.sim-engine.log-index` requires exactly `[ehrt.sim-engine.fold :as
fold]` and `[ehrt.sim-model.interface :as sim-model]` -- the two
outgoing edges step 1 derived by whole-symbol scan, and nothing more.
No third require was needed at compile time, which is the confirmation
step 1's derivation was owed.

## 7. Step 5 -- the push and the close marker

Three commits pushed together, `c82436b..876fadf`:

| sha | commit |
|---|---|
| `2cca99d` | the pre-move citation sweep (steps 1-2) |
| `25d926e` | the extraction (step 3) |
| `876fadf` | record, prompt archive, P5 row (steps 3-5 close-out) |

`bin/post-push-verify`: `origin/main` matches tip, every commit message
in range pure ASCII. **CI run 33318257671 at `876fadf` -- SUCCESS.** CI
green at tip is this session's close marker, and no commit in the range
was RED-FIRST: the hand-owned-asset tripwire did not fire, which section
2e established in advance by reading all four of its sources rather than
by watching a gate stay quiet.
