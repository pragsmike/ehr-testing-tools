# Session: P7 -- referential columns A, B2, C (2026-09-05)

Archived verbatim, as issued. Record:
[`../session-records/2026-09-05-p7-stop-derivation.md`](../session-records/2026-09-05-p7-stop-derivation.md).

Clone of record `~/src/ehr-testing-tools`, branch `main`, HEAD
`094791e` at issue and at close — this session committed docs only.

---

# Session: P7 -- referential columns A, B2, C populated and shipped (2026-09-05)

Roadmap row `referential-corpus-population` (PRIORITY 7): 14 cells the
2026-09-01 ledger recorded as convictable-but-unwitnessable because no
committed config emitted a cancel, a :medication-end, or a
:care-plan-end. demos/scenarios/dense-7500/config.edn now does (2026-
09-04 record: A 2,230 / B2 4,884 / C 3,486 sites at 7,500). This
session adds the three columns as data, witnesses every cell under
Q5(a) set-equality, amends the ledger, and closes the row. Rider: a
`:sim-check` stage in pipeline.edn (ruled (b) 2026-09-04). No sub-agents.

Read first: AGENTS.md; .agents/skills/build-session/SKILL.md; the row;
.agents/plans/2026-09-01-event-mutation-population-ledger.md sections
5-7; notes/adr/0176-*.md (Q5, Q6, Q9); components/corpus/src/ehrt/
corpus/operators.clj :380-612 (columns, shapes, the doseq, catalog-
gaps); components/corpus/test/ehrt/corpus/event_mutate_test.clj
(populations :76-99, the loop tests :190-310); the event schema's
typing of :cancels-event-id, :order-event-id, :start-event-id;
components/corpus/docs/pipeline.edn (:check at :66) and use-cases.edn's
:ground-truth-as-a-test-oracle :equations; .agents/reading-sets.edn.

Rulings, binding:
- Q5(a), Q6(a), Q9(a) standing: set-equality conviction per cell; an
  unwitnessable operator is refused, not shipped; a null shape on a
  schema-forbidden field is dropped and recorded, not tested.
- R-population: the third harness population is dense-7500/config.edn
  at the SMALLEST --patients at which every A/B2/C shape has a site,
  derived by measurement; generation must stay under 60 s in-test or
  STOP with the measured cost. No new config file.
- R-rider: pipeline.edn gains `:sim-check` (label "Sim check",
  contract: the invariant catalog over the ground-truth log; distinct
  from :check's expectation judge); the oracle use case's equation
  repoints to it. lint.clj untouched (no catalytic resource added).
- R-pins: any count pin that moves gets its own commit naming the
  count and why -- never folded into a payload commit.

Steps:
1. Derive, in the record: per column the carrier kinds, field, target
   kind, schema type (hence whether Q9 drops the null shape -- expect
   A yes, B2 and C no: 4+5+5 = 14); the smallest dense --patients per
   R-population with per-shape site counts. Gate: 14 cells named.
   No commit.
2. RED: add the population delay; conviction expectations for the 14
   cells in the loop tests' own idiom (declared finding = the ledger
   section 6 invariant per column). Gate: exactly the 14 red;
   every-population-checks-clean green on the new population.
   Commit: test: columns A, B2, C -- 14 conviction witnesses, RED
3. GREEN: three entries in referential-columns; nothing else in
   operators.clj. `make docsgen` (operators.md, cli.md if `sim mutate`
   enumerates operators via help.clj :doc -- edit :doc only).
   Gate: corpus brick green in all projects.
   Commit: feat(corpus): referential columns A, B2, C shipped (ADR-0176)
4. Rider: pipeline.edn :sim-check; use-cases.edn equation; `make
   use-cases` and the pipeline renderer. Gate: usecases + lint tests.
   Commit: docs: sim check is its own pipeline stage
5. Ledger section 6 rows -> measured counts and "shipped"; section 7
   note dated; ADR-0176's "14 population gaps" sentence gains a dated
   addendum (verbatim kept). Full make test; pins per R-pins.
   Gate: full suite green. Commit: docs: population ledger closed
6. Oracle + bracket vs 094791e: IDENTICAL expected (no payload
   change; mutate is post-run). Record; row -> CLOSED with citations;
   indexes; archive. Fences: no sim, sim-engine, sim-check src; the
   `event-mutation-catalog-gate` row stays BLOCKED, untouched.
   Commit: docs: P7 session record (archives prompt)
7. Push; verify CI yourself (gh run view); close-marker commit.

---

## Author ruling issued mid-session, 2026-09-05

Verbatim, in response to the session's STOP-AND-REPORT at the step-1
gate:

> STOP accepted on both counts: (b) will be a separate session before P7
> resumes, and 13-not-14 is assented with the 14th's cause rowed as a
> checker gap. Do no engineering. Write your step-1 derivation and
> findings 1-3 into .agents/session-records/2026-09-05-p7-stop-
> derivation.md -- the 14-cell table with schema types and site counts
> at 20 and 40, the two-line run.clj:146 / decide.clj:331 mechanism with
> the three blind nil? assertions named, the A/inverted-span
> observed-vs-declared sets, and the per-carrier target note -- archive
> the prompt, regenerate the indexes, commit, push, verify CI,
> close-marker. Tree otherwise untouched.

## Deviation record

Steps 2 through 7 of the prompt as issued were NOT executed, by the
ruling above. What this session did instead:

- **Step 1 executed in full**, and its derivation is the record's
  sections 1 and 2. Its gate ("14 cells named") is met.
- **Steps 2-5 not executed.** Step 2's population, step 3's three
  column entries, step 4's `:sim-check` rider and step 5's ledger
  amendment all carry forward to the resumed P7 session unchanged. No
  `components/` file, no test and no generated surface was touched, so
  there is nothing to unwind.
- **Step 6's fences honoured** even though its payload was not
  executed: no `sim` / `sim-engine` / `sim-check` src, and
  `roadmap.md#event-mutation-catalog-gate` untouched. Its record,
  prompt archive and index regeneration WERE done, which is this
  session's whole diff.
- **Step 6's row closure NOT done.** `roadmap.md#referential-
  corpus-population` stays OPEN and unedited; the resumed session
  amends it.
- **Step 7 executed**: push, `gh run view` CI verification, close
  marker.
- **R-pins observed.** The three counts in `.agents/state-derived.md`
  that this session's own files move -- roadmap rows, archived
  prompts, session records -- are a GENERATED surface CI diffs, not
  hand-written pins, and they are regenerated by `make state-derived`
  in this session's own commit rather than split out. No hand-written
  count pin moved.
- **One roadmap row ADDED**, licensed by the ruling above:
  `roadmap.md#cancel-invariant-has-no-time-clause`, PRIORITY 8.
- **R-rider not exercised.** The `:sim-check` pipeline stage is
  untouched and carries forward.
