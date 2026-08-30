# Engine namespace extraction, 9 of N: the `decide` cluster

Session record, 2026-08-30. HEAD at start `ac27ee9`, confirmed equal to
`origin/main`; working clone clean, no fresh clone taken. Ceremony R30.
Program: `roadmap.md#engine-namespace-extraction-and-apply-unification`
(P5), author rulings C1(a) (`engine.clj` stays the facade, moved PUBLIC
vars get delegating defs, private movers widen ONLY where a call site
stays behind, no test file changes) and S1(a) (an equivalence proof
replaces red-before-green).

This is the LARGEST move of the program and the last cluster with a
real choice in it. After it, `engine.clj`'s residue is `run` and its
pre-loop, plus forty-one delegating defs -- which is the material
section 6 offers the author for the pending run/residual ruling.

`bin/preflight` exit 1, one FINDING: the working tree carried this
session's own sweep edits when it ran. Section 1's checks all pass:
HEAD matches `origin/main`, repo root is not under `/mnt/`, the last
five CI runs are green. One DISCLOSED line, expected -- "HEAD is not
currently tagged stable-*" (tags retired by the de-scaffold ruling of
2026-08-25).

## 1. Step 1 -- the tip, the spans, the privacy markers, the edges

Everything below was re-derived from THIS tree with Clojure's own
reader, never transcribed from the census, whose numbers are at
`517a96d` -- **eight** extractions ago.

`engine.clj` is **3,469 lines / 98 top-level forms plus the `ns`** at
`ac27ee9`, which is the figure the extraction-8 record closed on,
unchanged.

### The cluster is 58 forms, not 59

| | census (`517a96d`) | here (`ac27ee9`) |
|---|---:|---:|
| forms | 59 | **58** |
| lines | 1,613 | **1,360** form-lines |
| `defmulti` | 1 | 1 |
| `defmethod decide` | 32 | 32 |
| helpers | 26 | **25** |

The missing form is `observation-value-fields`, which section 3a's own
cycle-breaker analysis sent to `state` and the second extraction moved
there. The census lists it under `decide` in section 1 and under the
breaker in section 3a; both are right at `517a96d`, and section 1 is
what is stale now.

The line figures are not comparable and the difference is not drift:
the census partitions every line of the file, so a form's span absorbs
the blank and comment lines after it. 1,360 is the sum of the 58
paren-balance spans; the two source regions they sit in span 1,594
lines including their twelve interior comment blocks.

### Two non-contiguous regions

| region | span at `ac27ee9` | forms |
|---|---|---:|
| A | `335-1871`, `defmulti decide` .. `decide :care-plan-end` | 56 |
| B | `1962-2018`, `decide :cancel-transfer` .. `:cancel-discharge` | 2 |

Between them sit the `evolve` and `replay` delegating defs and their
two banners (`1873-1960`), left by the fourth and fifth extractions.
The two cancels are down there because they were written to sit AFTER
`replay` when `replay` was a real `defn` in this file -- the channel's
"the two cancel decides sat apart at census sha" is confirmed, and the
reason is the fifth extraction, not the census.

### Privacy markers: 26 for 26 CORRECT

Every marker was read off the tree by the reader, not trusted. This is
the SECOND cluster with nothing to correct there, and the first of any
size: the log-index cluster's renderings were 6 for 10, and the cause
was that section 1's rendering drops `^:private` from a `def`. **This
cluster has no private `def` at all** -- all nineteen private movers
are `defn-` -- which is exactly why the rendering could not go wrong.

What section 1's PROSE gets wrong is different: "the decide
multimethod, its 32 methods, and 26 private helpers". Six of the
helpers are PUBLIC -- `compile-patient`, the three `*-stay-minutes`
tables, `documented-step-rejection-reasons` and `person-entry` -- and
section 1's own per-form list renders all six correctly as `def`/`defn`.
The prose and the list disagree; the list is right.

### The seven public movers, and what each delegating def resolves for

| mover | resolves for | class |
|---|---|---|
| `decide` | `engine_test.clj` **90** sites, `emit_hl7_test.clj` 14, `siu_test.clj` 1 | test-only, and the largest such obligation of the program |
| `compile-patient` | `interface.clj:62` (constraint 4) | interface |
| `documented-step-rejection-reasons` | `interface.clj:93` (constraint 4), AND four direct `engine/`-qualified readers: `event_schema.clj:985`, `check.clj:998`, `event_conformance_test.clj:81`, `check_test.clj:692` | **BOTH** |
| `person-entry` | `engine_test.clj`, six sites | test-only |
| `delivery-stay-minutes` | `prelude`, two sites | **RESIDUE-ONLY** |
| `injury-stay-minutes` | `prelude`, two sites | RESIDUE-ONLY |
| `unidentified-stay-minutes` | `prelude`, one site | RESIDUE-ONLY |

**The channel's expectation about `documented-step-rejection-reasons`
is VERIFIED, not assumed**: it is on `interface.clj`'s re-export list at
`:93`, and census constraint 4 names it. `compile-patient` is the other
one at `:62`. No other mover is on that list -- not `decide` itself.

**RESIDUE-ONLY is a class no earlier cluster had.** The three
`*-stay-minutes` tables have no reference anywhere in the tree outside
`engine.clj` -- a whole-repo scan returns only the census's own three
list rows. Their delegating defs are owed neither to `interface.clj`
nor to a test file, but to `prelude`, which stays behind and names all
three unqualified. `config`'s `Persons` and `Scheduling` had no caller
of ANY kind; these have exactly one, and it is in the file the def
lives in.

### Constraint 5 read the same way extraction 8 read it, and the answer differs

A whole-repo `with-redefs` census returns `engine/run`, `engine/stream`
and `engine/stream-seed` and nothing else, so constraint 1 does not
bite and no mover is redefined by any test.

Nineteen movers are private. The residue was scanned for every one of
them by whole-symbol match, and read in place:

* **`days->seconds` has a CODE caller that stays behind** -- `prelude`'s
  follow-up interval, `(when (< u f) (days->seconds days))`. One caller
  stays, so under the extraction-8 reading widening IS owed: it becomes
  `defn` in `decide` and `prelude` calls `decide/days->seconds`.
* `turnaround-seconds` appears once more in the residue and `vacate-bed`
  once, both in COMMENTS -- one in a banner that travels, one inside
  `run`. Bare names in mechanism claims, no position and no namespace.
  Safe, the class extraction 7 already dispositioned.
* The other seventeen appear in the residue not at all.

So exactly ONE of nineteen widens. The census's section 3a row for
`run -> decide` names `days->seconds` and is right; what it could not
say is that this is the only crossing that forces a widening.

### Outgoing edges, corrected from the tree

Method: the 98 top-level names `engine.clj` defines, each searched as a
whole symbol against the moved text, then every hit read in place to
separate code from prose.

**Nothing in the moved text resolves in `engine.clj`.** Ten names that
stay behind occur inside it -- `run` (40 times), `evolve` (8),
`stream` (9), `replay` (3), `prelude` (2), `initial-patient`,
`demographics-from-persona`, `placeholder-demographics`, `one-stream`,
`assign-pathway` -- and **every single occurrence is docstring or
comment prose.** There is no forward reference to `run` because `run`
is defined below; the rest are attributions. This is the
stop-and-report condition the prompt named, and it does not arise.

The real edges, all already qualified in `engine.clj` and carried
verbatim:

| callee ns | crossings | what |
|---|---:|---|
| `sim-model` | 26 | `allocate` ×6, `occupancy-board` ×4, `ward-by-name` ×3, `free` ×2, `reference-today-epoch-day` ×2, `persona`, `ward-census`, `turnaround-minutes`, `licensed-bed-ids`, `choose-attending`, `bed-placement`, `Persona` |
| `log-index` | 12 | `last-uncancelled-index` ×4, `subject-superseded?` ×2, `reinstated-state` ×2, `last-cited-index` ×2, `bed-reoccupied-by-someone-else?` ×2 |
| `streams` | 11 | `rand-int-in` ×5, `uniform-choice` ×3, `minted-encounter-id-field` ×2, `minted-appointment-id-field` |
| `encounters` | 4 | `gate-compiled-encounters` ×2, `encounter-openable?` ×2 |
| `order-profiles` | 2 | `sample-analyte-value`, `abnormal-flag` |
| `patient-simulator` | 2 | `run-module`, `compile-trajectory` |
| `state` | 1 | `observation-value-fields`, the cycle breaker |

Section 3a's headline row -- `decide -> log-index`, 11 edges, the
biggest in the census -- is confirmed as **12** by textual occurrence
and 11 by the census's own per-(form, callee) counting, which is the
same number differently counted, and the five named callees are exactly
right. Its `decide -> streams` 11 and `decide -> encounters` 3 are
confirmed at 11 and 4, the extra being a second `encounter-openable?`
occurrence in one form. The channel's "evolve possible" is CORRECTED:
there is no `decide -> evolve` edge in code; the eight `evolve`
occurrences are all prose.

`churn` occurs twice, both in comments, so `decide.clj` needs no
`churn` require despite the census's cluster prose. Requires:
`sim-model`, `patient-simulator`, `encounters`, `log-index`,
`order-profiles`, `state`, `streams`, plus `(:import [java.util
Random])` for two `^Random` hints. **All seven were already
`engine.clj`'s own requires, so no `deps.edn` and no `workspace.edn`
change** -- the REQUIRE SET the prompt asks to be confirmed.

## 2. Step 2 -- the constraint-6 sweep, and its own commit

The prompt changes the program's pattern here: dispositions land BEFORE
the move, in `76b0e56`, rather than with it. The consequence is worth
naming, because it is new: for one commit the five repointed citations
name `decide.clj`, which does not yet exist. Nothing gates on that --
`stale-path-test` is a forbidden-PATTERN scan, not an existence check,
and its live patterns are all retired `ehrt.sim.*` forms -- but a reader
bisecting the branch will meet a forward reference and should know it
was deliberate.

### 2a. Method

**Level 0.** The 26 mover names grepped across the whole repo outside
`engine.clj`, with a look-behind that excludes namespace-qualified
prefixes so `engine/decide` and a bare `decide` are counted separately.

**Level 1.** Every string literal and `;;` comment in the moved text,
whitespace-normalised, cut into every distinct six-word window of 25+
characters: **4,342 phrases**, searched against the
whitespace-normalised text of **1,521** files (`.git/`, `target/`,
`out/`, `.cpcache/`, `.clj-kondo/`, `.lsp/`, `node_modules/` excluded).

**Level 2.** Positional and attribution claims in BOTH directions: out
of the cluster (prose elsewhere naming a mover by file or namespace)
and into it (prose inside the moved text making a positional claim),
plus every prior-extraction banner in `engine.clj` read for a mover
name.

**Level 3.** The gated registries, each read directly:
`hand-owned-assets.edn`, both `docs/limitations.md` charter registers,
`exercised-sources.edn`, `.agents/reading-sets.edn`.

### 2b. What SEVEN of twenty-six movers being public does to level 0

The same collapse extraction 7 recorded, and worth restating because
this cluster's names are the most-cited in the tree. `engine/decide`
alone occurs at **105 call sites** across three test files (plus one
docstring mention in `sim_model/facility.clj`),
`engine/documented-step-rejection-reasons` across five files,
`engine/compile-patient` across two and `engine/person-entry` in one --
and **not one of them is falsified by the move**, because every one
names a var that still resolves through its delegating def.

What survives that filter is: prose naming the FILE or NAMESPACE a
mover LIVES IN, and any reference at all to one of the nineteen private
movers, which constraint 5 forbids covering with a def.

### 2c. Five repoints, all landed in `76b0e56`

| # | hit | text | why no def forwards it |
|---|---|---|---|
| 1 | `docs/dev/simulator-architecture.md:81` | "(`engine.clj`'s `defmulti decide`, dispatching on `(:type step)`)" | names the DEFINING FORM; `engine.clj` keeps a `def`, not the `defmulti` |
| 2 | `sim_engine/event_schema.clj:520` | "(`engine.clj`'s own `rejected-outcome` docstring)" | a PRIVATE mover |
| 3 | `sim_model/pathway.clj:179` | "(engine.clj's own :order decide method)" | FILE placement claim about a `defmethod` |
| 4 | `sim_check/check.clj:1018` | "engine.clj's :order docstring" | same |
| 5 | `sim_engine/order_profiles.clj:22` | "the order/result step types (engine.clj) just call it" | same |

Repoint 1 is the fourth fire of the same class in five extractions, and
its shape was copied from the sibling bullet the fourth extraction had
already rewritten for `defmulti evolve`. Repoints 3-5 are the class
register row L2-17 did NOT close: L2-17 converted `engine.clj:NNN` line
citations into `defn` names, which survives a move; these name the file
in prose, which does not.

### 2d. What level 0 found and left alone

| hit | disposition |
|---|---|
| `docs/dev/simulator-architecture.md:87` -- "`defmethod decide :discharge` and its `bed-ready-location` helper", "`defmethod decide :merge`" | names by DEFINING FORM, no file. **Safe** -- L2-17's repair paying off again, in the same paragraph as repoint 1 |
| `docs/dev/simulator-architecture.md:76`, `:153`, `:156` | all cite `engine.clj`'s **ns docstring** or ns form, which stays. **Safe** |
| `sim_engine/fold.clj:84` -- "`decide :transfer-in-error` does not call `vacate-bed`" | two bare names, a mechanism claim. **Safe** |
| `sim_check/check.clj:987` -- "see ehrt.sim-engine.engine/documented-step-rejection-reasons" | namespace-qualified reference to a PUBLIC mover; the def forwards it. **Safe** |
| `docs/dev/architecture.md:109`, `sim_engine/interface.clj:32` | interface-width prose naming the var. **Safe** |
| `demos/scenarios/ed-tuesday/config.edn:25`, `:115`, `config-latency.edn:94` | cite "engine.clj's own `run` docstring" and its module-only-patient pattern; `run` stays and the phrase occurs nowhere else in the file. **Safe** |
| `oracle/digest.clj:112`, `:291` | dated notes; `:291`'s `:history` default is in `run`'s own docstring, which stays. **Safe** |
| `patient-simulator/docs/gmf-interpreter.md:235`, `:240`, `:244`, `:254`, `:399` | cite `decide`'s signature (already stale since ADR-0171 made it `streams`, pre-existing), `events-for-patient` (a log-index def that still resolves), and the ns docstring twice. **Safe, none ours** |
| `patient-simulator/docs/gmf-interpreter-findings.md:491`, `:1103`, `:1131` | `:491` names `citation-fields` -- a private mover -- "elsewhere in `engine.clj`", which WOULD be a repoint but for the file's own header: "Content here is historical record ... not the current state". **Frozen by construction, left alone** |
| `notes/adr/0165:107`, `0174:165`/`:715`, `0171:136`/`:217`/`:254`, `0153:206`, `0104:76`, `0151`, `0150` | frozen ADRs, several with `engine.clj:NNN` citations already stale. The repo does not repoint ADRs. **Left alone** |
| `sim_engine/churn.clj:136`, `state.clj:414-424` | prior extractions' own prose, naming movers by defining form. **Safe** |

### 2e. Two repoints that could not land in the sweep commit

Both are INSIDE the moved text and both are TRUE at `76b0e56`, so
restating them there would have made them false for one commit. They
land with the move, and they are the only prose in the moved text that
changes at all:

| line at `ac27ee9` | text | restated to |
|---|---|---|
| `486` | "`replay` (below) bootstraps" | "`ehrt.sim-engine.fold/replay` bootstraps" |
| `1084-1085` | "`replay` and the run loop's own two folds, below)" | "`ehrt.sim-engine.fold/replay` and `run`'s own two in-loop folds)" |

This is the log-index extraction's precedent -- a travelling positional
claim is restated to keep the mechanism and drop the position -- paid
twice rather than once.

### 2f. THREE positional claims that travel and stay TRUE

`delivery-stay-minutes`' "and the same bounded-encounter one" naming
the table above it, `unidentified-stay-minutes`' "the same
bounded-encounter reasoning as the two above", and the Wave D D2
banner's "the SAME decide/evolve shape `:medication-order`/
`:medication-end` establish, two defmethod-pairs up". All three name
forms that travel WITH them in the same order, so all three survive
unedited -- the `assign-module` precedent from extraction 8, three
times over.

`bed-status`' "every branch below asks through it" is the fourth: its
readers are all `decide` methods and every one travels, verified by the
residue scan returning zero `bed-status` occurrences.

### 2g. TWO claims in the moved text were ALREADY FALSE at this tip

Recorded, not repaired (`rulings.md#R-move-not-improve`, whose "Prose
included" half is explicit). Neither is caused by this move and neither
is made worse by it:

* **`defmulti decide`'s docstring** says each method "draws from the
  family its census row names (`stream-family-tag` above)".
  `stream-family-tag` was a PRIVATE streams mover; it left `engine.clj`
  at the FIRST extraction and got no delegating def, so "above" has
  been false since. The claim would be true again if it named
  `ehrt.sim-engine.streams/stream-family-tag`.
* **The bed-status banner** says the `:beds` index is nil unless opted
  in "exactly like `:encounter-minting` above it". `:encounter-minting`'s
  only other occurrence in `engine.clj` is inside `run`, BELOW it. The
  encounters extraction (third) took the forms that were above.

Both are the ADR-0170 species -- a claim true when written that nothing
keeps true -- and both are now findings a later session can take.

### 2h. Level 1 -- 4,342 phrases, and no repoint

Hits in 65 files. Every non-frozen one was read; not one is a citation.

* The dominant pattern is the SHARED BANNER `;; ARC 3B SWEEP <n>
  (ADR-0174 ...)`, the header convention those sweeps used in every file
  they touched. It recurs in `check.clj`, `sim/run.clj`, `config.clj`,
  `encounters.clj`, `state.clj`, `emit_hl7.clj`, `site_profile.clj`,
  `board.clj`, `sim_model/interface.clj`, `sim_model/config.clj`,
  `facility.clj`, `bin/demo-exerciser-ed-tuesday` and three demo
  configs. It names an ADR and an arc, never `engine.clj` and never a
  mover.
* The second pattern is SHARED PROSE between siblings:
  `components/sim/docs/operational-models.md` (22 phrases, five
  sentences) restates the appointment banding law and the boarding
  coupling in the same words the methods use; `log_index.clj` (22)
  shares the "Same fallback rule" and "hand-built world" sentences with
  the cancel methods it was extracted alongside; `person_fold.clj`,
  `evolve.clj`, `encounters.clj`, `streams.clj`, `churn.clj`,
  `event_schema.clj` and `sim-theory.edn` each share one or two. None
  is paired with a path.
* Everything else is `.agents/session-records/`, `.agents/plans/`,
  `.agents/prompts/` and `notes/adr/` -- frozen by construction.

### 2i. Level 3 -- the registries

* **`hand-owned-assets.edn`.** All five rows' sources read in full. The
  tripwire WILL fire on `gt-emitters.svg`, because repoint 1 is in
  `docs/dev/simulator-architecture.md`. **Fourth fire in five
  extractions, for the reason the P5 row already states.** PREDICTED
  here, and the prediction had to survive its own gate saying otherwise:
  `make test` over the tree carrying the repoint was GREEN at
  `MAKE_EXIT=0`, because the test reads `git log -1` on the SOURCE and
  cannot see an uncommitted edit. `two-clocks.svg` cites the same source
  but is `:verdict :stale` with a live `:stale-row`, and the tripwire
  skips non-`:fresh` rows, so only the one row goes red.
* **`.agents/reading-sets.edn`.** `simulator-architecture.md` is a
  `:sim` member with `:budget-lines 1405`. Repoint 1 is NOT
  line-neutral -- it replaces two lines with three -- so
  `.agents/state-derived.md` was regenerated in the same commit:
  `:sim` **1341 -> 1342**, headroom 64 -> 63. Extraction 7 predicted
  this gate and was refuted by its own line-neutral edit; this one pays
  it. Neither `engine.clj` nor `decide.clj` is a set member, so the
  move itself moves no budget.
* **Both charter registers.** `components/person-simulator/docs/
  limitations.md` and `components/patient-simulator/docs/limitations.md`
  grepped for all 26 mover names and for `engine.clj`: **not one hit.**
  Every `sim-engine` path either register pins is `streams.clj`, which
  the FIRST extraction repointed. Constraint 6's original class does
  not arise for this cluster. Constraint 7's bare token scan is over
  `person-simulator`'s own `src` and is untouched.
* **`exercised-sources.edn`.** No `sim-engine` row, no `engine.clj`
  path, no mover name. Untouched.

### 2j. Twelve test-file citations, in ten files, go stale and are LEFT ALONE

C1(a) forbids touching test files, so these are disclosed rather than
fixed, and they belong to the ruled repoint pass:

| file | text | class |
|---|---|---|
| `engine_test.clj:200` | "The specification copy of `engine`'s own `waiting-boarder` predicate" | **a NAMESPACE claim about a PRIVATE mover** -- the class extraction 8 met in `persona.clj` and could repoint because that file was `src`. Here it is fenced. NEW to the program |
| `engine_test.clj:1067` | "see engine.clj's :order docstring" | file placement |
| `engine_test.clj:1993` | "engine.clj's own :registered anchors registration-t" | file placement |
| `vendored_colorectal_test.clj:96` | "`engine.clj`'s own `:registered` decide method calls `compile-trajectory` directly" | file placement |
| `vendored_injuries_test.clj:78` | "(`engine.clj`'s own `:registered` decide method calls it directly)" | file placement |
| `vendored_ear_infections_test.clj:14` | "(`engine.clj`'s own `:registered` defmethod)" | file placement |
| `vendored_veteran_prostate_cancer_test.clj:59` | "`engine.clj`'s own `:registered`" | file placement |
| `vendored_sepsis_test.clj:12` | "engine.clj's own :registered event" | file placement |
| `vendored_dementia_test.clj:7` | "engine.clj anchors registration-t" | file placement |
| `vendored_allergic_rhinitis_test.clj:12` | "since `engine.clj` anchors `registration-t`" | file placement |
| `vendored_uti_test.clj:14`, `:22` | "`engine.clj`'s bare 5-arity `run-module` call", "`engine.clj`'s own fixed registration-t anchor" | file placement |

`persons_test.clj:420` ("because `hook-ward`") and `engine_test.clj`'s
`:257`, `:258`, `:331` are bare-name mechanism claims and stay true.
`engine_test.clj:2440`/`:2446`/`:2543` cite `engine.clj:480`, already
stale for many sessions. `latency_test.clj:56` cites
"(assign-pathway/assign-module, engine.clj)", stale since extraction 8.
**The fenced-citation backlog is now the largest single artefact the
repoint pass will have to clear, and it is growing one cluster at a
time.**

## 3. Step 3 -- `ehrt.sim-engine.decide` (`4a9296b`)

58 forms, two regions, 1,701 lines. **`decide.clj`'s body diffs against
`engine.clj`'s own text at `76b0e56` as THREE differing lines**, each
named in section 2 before the move and each verified by diffing the
body as a block, not inferred from a hunk header:

```
152c152,153
<   ;; is now every patient's FIRST event, and `replay` (below) bootstraps
---
>   ;; is now every patient's FIRST event, and `ehrt.sim-engine.fold/replay`
>   ;; bootstraps
514c515
< (defn- days->seconds [d] (* 86400 (long d)))
---
> (defn days->seconds [d] (* 86400 (long d)))
750,751c751,752
<   `participants-of` and `participant-ids-exist-in-run`; `replay` and the
<   run loop's own two folds, below).
---
>   `participants-of` and `participant-ids-exist-in-run`;
>   `ehrt.sim-engine.fold/replay` and `run`'s own two in-loop folds).
```

**No code line differs**, and the one that changes shape is the
widening the residue forces.

### The MultiFn, verified live rather than assumed

The prompt asked for this specifically, and it is the delegation the
whole cluster hangs on:

* `engine/decide` is `identical?` to `decide/decide`.
* Both report **32** methods, and `(methods engine/decide)` is
  `identical?` to `(methods decide/decide)` -- ONE method table, not a
  copy.
* A `:registered` step dispatched through each returns the same
  `[:registered]` and the same `:advance 0`.
* A `:cancel-transfer` was driven through `engine/decide` against a
  hand-built world and returned `[:cancel-transfer]` -- the branch NO
  bracket reaches, exercised at the seam rather than assumed across it.

### The rest of the live `-M:dev` check

* All seven public movers resolve, are public in `decide`, and each
  delegating def is `identical?` to the var it delegates to.
* `interface/compile-patient` and
  `interface/documented-step-rejection-reasons` are `identical?` to
  `engine`'s, the latter at nine reasons.
* `decide/days->seconds` is public and returns 259200 for 3.
  **`engine/days->seconds` does not resolve** -- the widening did not
  leak a delegating def.
* All eighteen remaining private movers are `:private` in `decide` and
  resolve in `engine` not at all.

### Gates

* **`clojure -M:poly check`** OK -- and, per the standing finding, that
  is not evidence the file compiles. The evidence is the `-M:dev` load
  above and the suite.
* **`make test` at this commit halts at `MAKE_EXIT=2`** on the
  `hand-owned-asset-freshness-test` red predicted in section 2i, which
  is the FIRST project `make` runs, so the suite is never reached. That
  is not a finding: it is what a red-first commit looks like, and the
  full-suite figures are the successor's.

## 4. Step 4 -- the brackets

Both are the scripts' own output, per `rulings.md#R-oracle-script-contract`.

* **`bin/regression-oracle 76b0e56 4a9296b`** -- across the move alone:

  ```
  IDENTICAL outside the leading docstring -- proceeding
  --- declared-digest-change: no (soundness: yes outside the leading docstring) ---
  IDENTICAL: every root's digest matches between 76b0e56 and 4a9296b
  ```

  41 distinct roots, exit 0, no declaration.

* **`bin/ground-truth-bracket ac27ee9 4a9296b`** -- named as spanning
  BOTH commits in one bracket, from the session's own starting tip
  through the move:

  ```
  --- coverage: 38 roots carry :ground-truth and are digested; 3 skipped
      (no such key): appendicitis.edn, ear-infections.edn, sore-throat.edn ---
  IDENTICAL: every digested root's :ground-truth matches between ac27ee9
      and 4a9296b (38 roots)
  ```

  Exit 0, no declaration.

**What those two IDENTICALs do NOT prove, restated because this is the
cluster where it matters most.** They exercise the producer of every
event in every root, so they are the strongest evidence any move in this
program has had for the bulk of its forms. They are blind to six:
`rejected-outcome`, `documented-step-rejection-reasons`, and the four
cancel-family decide methods (`:cancel-admit`, `:cancel-transfer`,
`:cancel-discharge`, `:transfer-in-error`). No gated corpus emits a
cancel and none resolves a citation. What covers those instead:

| form | covered by |
|---|---|
| the four cancel methods | `engine_test.clj`'s cancel family -- roughly forty `engine/decide` calls against hand-built worlds -- plus `ehrt.sim.run-test`'s reinstatement tests |
| `rejected-outcome` | the same, through every illegal-cancel branch, plus `ehrt.sim-check`'s conformance walk |
| `documented-step-rejection-reasons` | `event_conformance_test.clj:81` and `check_test.clj:692`, which iterate the set, and `event_schema.clj:985`, which builds an `:enum` from it |

and, in this session specifically, a `:cancel-transfer` driven by hand
through `engine/decide` at the seam under `-M:dev`, returning
`[:cancel-transfer]`. That is the log-index record's own §2f model,
applied to a cluster where 52 of 58 forms ARE bracket-covered -- the
honest statement is not "the bracket proves nothing" but "the bracket
proves 52 and is silent on 6".

## 5. The tripwire successor (`e385c74`)

`hand-owned-asset-freshness-test` went red on `gt-emitters.svg` at
`76b0e56`, exactly as section 2i predicted, and this commit is the green
successor that bumps `:reviewed-at` to `76b0e56`. All three are pushed
together, never alone (`rulings.md#R-red-pushed-with-green`).

**The prediction survived its own gate saying otherwise, again.** `make
test` was GREEN at `MAKE_EXIT=0` over the tree carrying the repoint,
because the test reads `git log -1` on the SOURCE. After the commit,
`git log -1 -- docs/dev/simulator-architecture.md` returns `76b0e56`
against `:reviewed-at "b8b9acb"`, and the assertion
`(str/starts-with? sha reviewed-at)` is false.

**What is new is WHICH commit is red.** The prompt asked the sweep's
dispositions to land BEFORE the move rather than with it, so for the
first time in this program the red-first commit is a docs commit and the
extraction itself is downstream of it. The consequence: `make test` at
the move commit halts at `MAKE_EXIT=2` in `docs-tooling`, the first
project `make` runs, and never reaches the suite. The move's suite
figures are therefore this commit's, and that is a property of the
ordering, not of the move.

### The suite, taken here because the move commit cannot reach it

**`make test` MAKE_EXIT=0** over the close tree. **Namespaces 408 and
tests 4,751 are IDENTICAL** to the extraction-8 close and to this
session's own sweep gate -- no test was added, removed or renamed by
any of the four commits. Assertions are **24,127**, +2 against 24,125,
**explained to the assertion**: `ehrt.docs-tooling.io-vocabulary-lint-
test` reported 129 in each of the two projects it runs in at the
extraction-8 close and reports **130** in each here, and the tree
gained exactly one production file. One new file is +1 twice -- the
benign class every extraction of this program has recorded and this
session's prompt named in advance.

`clojure -M:poly check` OK at the same tree. Per the standing finding
(three consecutive sessions have caught it), that is not evidence any
file compiles, let alone parses; the evidence is the `-M:dev` load in
section 3 and this suite run.

**The trigger did not fire in SUBSTANCE, for the second extraction
running.** Section 4's equation block was verified byte-identical by
diffing `193-372` at `b8b9acb` against `194-373` here as a BLOCK -- the
one-line offset being exactly what the section-2 edit introduces --
rather than inferred from the hunk header. The repoint is in section 2's
`decide` bullet, and its new wording is verbatim the shape extraction 4
gave the `evolve` bullet a few lines below it.

## 6. Closing arithmetic, census corrections, and the residue

### The partition still closes

Counted with Clojure's own reader.

| namespace | lines | forms (+ ns) |
|---|---:|---:|
| `engine` | 1,968 | 47 |
| `decide` | 1,701 | 58 |
| `streams` | 331 | 16 |
| `state` | 441 | 14 |
| `encounters` | 243 | 10 |
| `evolve` | 469 | 32 |
| `fold` | 155 | 3 |
| `log-index` | 301 | 10 |
| `config` | 187 | 5 |
| `assignment` | 144 | 3 |

47 + 58 + 16 + 14 + 10 + 32 + 3 + 10 + 5 + 3 = **198**, which is 181
plus the evolve, fold, log-index, config, assignment and decide moves'
**seventeen** delegating defs (1 + 1 + 1 + 5 + 2 + 7). `engine.clj` went
98 forms to 47 by losing 58 and gaining 7, and 3,469 lines to 1,968.

### Census corrections, one sentence each

* Section 1's `decide` list is 59 forms at `517a96d` and **58** here:
  `observation-value-fields` left with `state` as section 3a's own
  cycle breaker, so the two sections were consistent then and section 1
  is stale now.
* Section 1's prose "26 private helpers" is wrong twice: there are 25
  helpers, and SIX of them are public; its own per-form list renders all
  six correctly, so the list is right and the sentence is not.
* Section 1's privacy renderings for this cluster are **26 for 26
  correct** -- the cluster has no private `def`, which is the exact
  condition under which the rendering's known defect cannot fire.
* Section 1's spans are stale by eight extractions: the cluster is at
  `335-1871` and `1962-2018`, not `899-2058` and `3273-3334`.
* Section 3a's `decide -> log-index` 11, `decide -> streams` 11 and
  `decide -> encounters` 3 are confirmed (12/11/4 by textual
  occurrence, the same edges differently counted), and its five named
  log-index callees are exactly right.
* Section 3a's `run -> decide` row names `days->seconds`, and that
  single crossing is the only one in the program's nine clusters that
  forced a private mover to widen.
* Section 4a is confirmed exactly: `run`'s `(decide ...)` is the sole
  event producer, and this move relocated it while leaving the call
  site and its unqualified name where they were.

### What remains in `engine.clj`, for the pending run/residual ruling

**47 forms plus the `ns`, of which 41 are delegating defs.** The
public/private split is 43 public / 4 private, and the four private
ones are the whole of the real code apart from `person-plan` and `run`:

| form | lines | what |
|---|---:|---|
| `pop-min` | 7 | private, the queue primitive |
| `placeholder-registration` | 31 | private |
| `select-person` | 28 | private |
| `prelude` | 611 | private, the pre-loop |
| `person-plan` | 56 | public |
| `run` | 594 | public |

That is **1,327 lines of real code in a 1,968-line file**: the
remaining ~640 lines are the `ns` docstring, the 41 delegating defs and
the nine "moved to" banners. The residue is now two-thirds code by line
and one-eighth code by form.

Three observations the author asked for, offered as evidence rather
than as a recommendation, and unchanged in substance from extraction
7's -- what this session adds is that they are now measured against the
final shape rather than predicted:

1. **The facade is already mostly defs.** 41 of 48 top-level forms are
   delegating defs. Extracting `run` would leave a file of defs and
   banners and nothing else -- a legitimate facade shape, but a
   different decision from the nine moves so far, every one of which
   left real code behind.
2. **`run` is a caller of everything and a callee of nothing internal.**
   Confirmed again at this tip: the residue's six real forms reach
   `assignment`, `churn`, `config`, `decide`, `encounters`, `evolve`,
   `fold`, `log-index`, `order-profiles`, `person-fold`, `state` and
   `streams` -- all twelve siblings -- and no sibling reaches back.
3. **Constraint 1 binds `run` and nothing else.** `engine_test.clj:2505`
   perturbs the partition by `with-redefs` on
   `ehrt.sim-engine.engine/stream`, and `run`'s call sites must keep
   resolving through that var. A `run` extraction would have to keep
   `run` callable through `engine/run` AND keep `stream` unqualified
   inside the moved body -- two constraints no previous move had to
   satisfy at once.

A fourth, new with this session: the residue's `prelude` is the ONLY
reason three of this cluster's seven delegating defs exist, and the
only reason `days->seconds` widened. **A `run` extraction would make
all four of those obligations disappear** -- the three `*-stay-minutes`
defs would have no caller anywhere and `days->seconds` could go back to
`defn-`. That is a real, cheap simplification available on the other
side of the ruling, and it is named here so the ruling can price it.

### Budget

`.agents/plans/roadmap.md` is an `:onboarding` member at **1518 of
1530**, twelve lines of headroom, which extraction 7 compacted the row
to leave. This session's P5 update was written to fit inside it:
it REPLACES 89 lines with 83, so `:onboarding` goes
**1518 -> 1512 of 1530, eighteen lines of headroom** -- six
more than it started with, while the row gains a whole cluster.
The compaction is in the per-session narrative, which belongs in
these records and is here in sections 1-5; what the row keeps is
the standing shape of each landing and the three lessons that
generalise (the tripwire recipe, the census-correction habit, and
constraint 5 read as a prohibition).

One mechanical lesson, since it cost this session a red: `make
state-derived` must be the LAST edit before the gate. It was run, then a
four-line roadmap correction was applied on top, and `state-derived-test`
failed on a one-line `:onboarding` difference the renderer had already
been asked about. Regenerate after the last hand edit, not before it.
