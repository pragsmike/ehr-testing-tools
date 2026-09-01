# 2026-09-01 — event-stream mutation, implementation 2: breadth

## 1. Scope

Asked for: **breadth over the live population, the gap ledger, and the
lineage sidecar**, under three new rulings — Q10(a) (ship only where a
population exists; record the rest as population gaps and row the
population work), Q11(a) (`sim-check.interface` stays un-widened), and
Q12(a) (`--lineage PATH`).

Did all of it, in five commits from `68ed148`, red before green.

1. `1fb26f9` — the population ledger and three ADR-0176 corrections.
2. `6ce4555` — RED. The closed loop, parameterized over the catalog.
3. `895a24e` — GREEN. Eleven more operators; the catalog at twelve.
4. `--lineage PATH`, its CLI tests, and the consumer docs.
5. this record, its paired prompt archive, and the state-derived
   regeneration, last.

**The headline is not the eleven operators.** It is that the session's
own measurement refuted ADR-0176 three times, and the corrections are
what made the breadth honest rather than merely large.

## 2. Red-green evidence

**The RED, shown rather than asserted.** At `6ce4555`, the acceptance
namespace ran 145 assertions with **90 failures and 4 errors**. Every
failure was one of exactly two things: `(operators/lookup <id> "1")`
returning nil for the eleven operators that did not exist, and the new
derivation gate demanding coverage for `:placeholder-event-id` and
`:order-event-id`. The four errors were `mutate/mutate`'s `case`
meeting `(:format nil)`. The spine's own operator passed every row it
appeared in and **both populations checked clean**, so the failure was
about the missing catalog and not about the scaffolding.

**The GREEN.** At `895a24e`, the same namespace: **426 assertions, 0
failures, 0 errors**, and `clojure -M:poly check` **OK** — the corpus →
sim / sim-engine test-scope edges stay legal and add no cycle.

**THE LOOP, EXERCISED FOR REAL through `bin/ehrt`**, over a 200-patient
clinic-decade log (702,763 bytes), one operator per row:

| operator | exit | observed finding set |
|---|---|---|
| `phantom-placeholder-event-id` | 1 | `identity-fill-…` |
| `null-placeholder-event-id` | 1 | `identity-fill-…` |
| `cross-patient-placeholder-event-id` | 1 | `identity-fill-…` |
| `wrong-kind-placeholder-event-id` | 1 | `identity-fill-…` |
| `inverted-span-placeholder-event-id` | 1 | `identity-fill-…`, `timestamps-monotone` |
| `clock-skew` | 1 | `timestamps-monotone` |
| `drop-registration` | 1 | `participant-ids-exist-in-run`, `registered-is-every-patients-first-event` |
| `orphan-participant` | 1 | `clinical-content-only-when-admitted`, `every-encounter-is-opened-and-closed-or-still-open`, `participant-ids-exist-in-run`, `registered-is-every-patients-first-event` |

Observed = declared, exactly, in every row — Q5(a)'s set equality
holding at the shell and not merely in a test. The unmutated log checks
`:ok`, and `ehrt sim mutate` with no operator is `cmp`-identical over
all 702,763 bytes.

**`--lineage`, exercised the same way.** Stdout with the flag is
`cmp`-identical to stdout without it; the sidecar lands at a path whose
parent directories did not exist; and `--lineage` without
`--operator-id` exits 2 with `:missing-required-opt` / `:opt
:operator-id` and **writes no file**.

## 3. What the measurement refuted

Three ADR-0176 claims, all corrected by dated addendum rather than
rewrite, all argued in
`.agents/plans/2026-09-01-event-mutation-population-ledger.md`.

**(a) Section 2(iv)'s declared population is empty** — the spine had
already measured this; this session established what the population
actually IS. Derived by grep rather than recalled: the three opt-in demo
configs are the only ones turning on the keys that mint referential
content, and `config-latency.edn` is not a third population (its ground
truth is `cmp`-identical to `ed-tuesday`'s over 428,889 bytes, which is
a small free confirmation of the arc-wide byte-identity claim).

**Two of five carrier columns carry any candidate site at all.**

**(b) The matrix arithmetic under-counts.** `:order-event-id` has TWO
carriers convicted by DIFFERENT invariants and typed differently, so the
matrix is 23 cells rather than 20, and the spine's forward price of "19
referential operators remaining" reads 22.

**(c) All three structural operators' single-invariant claims are
false.** Measured at ADR-worded scope, they produced between one and
eight DIFFERENT finding sets depending on which site the draw landed
on. This is the session's most consequential finding, because a varying
set cannot be declared and Q5(a)'s equality is not negotiable.

Two mechanisms behind it are properties of the LOG FORMAT rather than
of these operators, and any later structural operator meets them too:

1. **Dropping an event RENUMBERS the log** — every log-index reference
   past the drop point silently repoints one event earlier, so a drop
   injects referential faults it never declared unless the indices are
   repaired in the same edit. `drop-one-event` repairs them.
2. **Renaming a participant MOVES the event into a phantom patient's
   timeline**, where patient-scoped invariants convict the phantom for
   having no `:registered` first event.

## 4. Judgment calls, and their ratification status

**(a) All three structural operators ship NARROWED rather than as
worded, or not at all. RATIFIED BY MEASUREMENT, and it is this
session's most consequential call.** What gives under a cascade is the
breadth of `:candidate-sites`, not the equality gate — which is exactly
what a candidate-site predicate is for. Each narrowing was measured to
produce ONE finding set across every sampled site of both logs, and
each is a statement about what the operator means:

* `:clock-skew` excludes events whose `:t` is load-bearing for anything
  other than monotonicity. Without the third clause two ed-tuesday
  sites also tripped `scheduled-encounter-follows-its-appointment`.
* `:drop-registration` **replaces** the ADR's `drop-event`. Its
  load-bearing clause is that the patient must have at least one OTHER
  event: 5 of 33 sampled drops of a lone `:registered` produced a log
  that checks CLEAN — a fault injector reporting success while injecting
  nothing, which is ADR-0165's silence one layer up.
* `:orphan-participant` is scoped to therapeutic-intent clinical
  content, and the kind list is DERIVED from `check`'s own
  `clinical-content-only-when-admitted` scoping rather than hand-picked.

**(b) The referential family is a CROSS PRODUCT in the source, not
eleven hand-written entries. DELIBERATE, and it is what ADR-0176 Q8(a)
asks for.** A hand-listed catalog reproduces exactly the asymmetry
ADR-0166 spent a session closing. A new column now inherits all five
shapes for free, and the co-landed derivation gate walks the LIVE
`engine/Event` schema for every int-typed `*-event-id` field and
requires each to be either covered or recorded as a declared population
gap. `:person-event-id` excludes itself on the right ground — the schema
types it `:string`, so filtering on int-ness makes the exclusion a
property of the schema rather than a special case in the test.

**(c) A THIRD gap kind was named: the SHAPE GAP. DISCLOSED as an
addition to the ruled vocabulary, not a departure from it.** Q10(a)
distinguishes catalog gaps (unconvictable) from population gaps
(unwitnessable). Finding (c) above is neither: sites are plentiful and
`check` convicts enthusiastically, but ambiguously. Naming it was
necessary to record the structural three honestly; the alternative was
to call a narrowed operator a full one, or to declare the modal set and
let the other sites fail.

**(d) `--lineage` without `--operator-id` is a REJECTION, not an empty
file or a silent no-op. Judgment call, argued.** A pass-through has no
provenance, and a file saying so would be a provenance record for a
mutation that never happened. The loud path follows the `--seed`
precedent the spine set in the same verb.

**(e) The gate's populations run at SMALLER patient counts than the
ledger measures at. DISCLOSED.** The ledger measures each config at its
own documented invocation (`rulings.md#R-measure-claimed-population`);
the gate runs the cheapest counts measured to still carry every
operator's population (clinic-decade 60, ed-tuesday 40). Both are real
seeded runs through the threaded path; neither is a fixture. The site
counts therefore differ between the ledger's tables and the gate, and
that is stated rather than reconciled by quietly changing one.

**(f) `:orphan-participant` has no ed-tuesday population** — that
corpus emits no therapeutic-intent clinical content — so its gate runs
on clinic-decade alone. Recorded rather than papered over.

## 5. Findings

**(F1) `ehrt sim check` takes no facility config, so a scenario that
overrides `:facility` cannot be checked clean at the shell.** Found
live while establishing clean baselines: `check-all`'s 1-arity defaults
to `sim-model/default-facility` (6 ED surge slots) and ed-tuesday's
config bumps that ward to 16, so ed-tuesday's own clean log reads as
violating `:occupancy-within-capacity`. At the 2-arity with its own
`:facility` it is `#{}`. **A shipped demo teaches
`run --format ground-truth | sim check` as its oracle pipe, so this
makes that pipe report a false positive.** The corpus is sound; the
checker is config-starved. Rowed on `roadmap.md`, not fixed — the fix
widens the CLI/sim-check surface, which this session's fences forbid.

**(F2) A sixth defect shape was probed and dropped for want of a
population.** A pure-referential alternative to `inverted-span` —
repoint at a same-patient target occurring AFTER the citer, tripping
the time clause with no `:t` edit and therefore no `timestamps-monotone`
companion — measured 0 sites on column D in both configs and 1 on
column B1. Recorded rather than silently not built; the ADR's own
`:t`-moving shape shipped instead.

**(F3) An `orphan/add` variant CRASHES the replay machinery.**
Appending a phantom participant rather than replacing an existing one
throws `No matching clause: :subject` at
`ehrt.sim-engine.evolve:293`. A mutant no consumer can fold is worse
than one that convicts ambiguously, so only the replace variant was
considered. Not filed as a defect against `evolve` — the input is a log
no engine would produce — but recorded, because a later structural
operator that adds participants will meet it.

**(F4) The two-element declared sets are the ADR being right.** Section
2(iv) chose a SET over a singleton on the argument that "inverting a
span's `:t` trips both `timestamps-monotone` and the span's own
referential invariant". Measured true, in both columns.

## 6. What this session deliberately did NOT do

* **No population-generation work.** Q10(a) and the fences both stop at
  measurement. The 14 population-gapped cells are rowed as
  `roadmap.md#referential-corpus-population`, priced as corpus
  authoring rather than operator work.
* **No `sim-check.interface` widening**, per Q11(a), so
  `:expected-findings` is still not cross-checked against `check`'s own
  vocabulary at registration. Still carried from the spine.
* **No catalog-wide gate.** ADR-0176 section 2(iv)'s "whole catalog
  against a fixed set of clean logs" is now BUILDABLE, because the
  ledger gives it a population — but the per-operator loops are what
  this session was asked for, and a catalog-wide gate over two real runs
  is its own cost question. Rowed.
* **No `engine/run`, emitter, or `fold/apply-events` edit**, and no
  `engine/config-keys` entry.
* **RNG family tag 6 (`:mutation`) is STILL unreserved** in
  `streams.clj` — a one-line sim-engine edit, out of reach of this
  session's fences as it was of the spine's. Carried forward for the
  third time; worth an author decision rather than another carry.

## 7. Close

**Full suite, unpiped, at `a7afe6b`** (`rulings.md#R-full-suite-before-push`):
`clojure -M:poly check` **OK**; `clojure -M:poly test :all
skip:integration` exit **0** — 414 namespaces, **25,420 passing
assertions, 0 failures, 0 errors**, 19m25s across both projects. That is
+878 assertions on the spine's 24,542, which is the twelve-row
acceptance loop, the derivation gate, and the three `--lineage` tests.

**Both oracles, `68ed148` → `a7afe6b`:**

* `bin/regression-oracle` — **IDENTICAL**: every root's digest matches
  between the two disposable worktrees. This is the whole-pair claim
  `rulings.md#R-oracle-script-contract` reserves the phrase for.
* `bin/ground-truth-bracket` — **IDENTICAL**, 38 roots digested, 3
  skipped by name (`appendicitis.edn`, `ear-infections.edn`,
  `sore-throat.edn`, the interpreter-layer batch roots carrying no
  `:ground-truth` key).

Expected by construction rather than stated as a lucky result: the
stage is post-run and outside `engine/run`, so there is no path by
which a shipped corpus could move. `a7afe6b` is the last commit
touching any `src` file this session; everything after it is session
records and the state-derived regeneration.

**Fences, each checked:** no `engine/run` edit; no emitter edit; no
`fold/apply-events` edit; no `engine/config-keys` entry; no
`sim-check.interface` widening (Q11(a) held — `:expected-findings` is
still only checked for non-emptiness at registration); no
population-generation work beyond measurement; mutation absent is
byte-identical everywhere, proved twice — by both oracles for shipped
corpora, and by `cmp` at the shell for `ehrt sim mutate`'s own
pass-through over 702,763 bytes.

**Final catalog census.**

| | count | where recorded |
|---|---|---|
| SHIPPED | **12** operators (9 referential across 2 carrier columns, 3 structural) | the registry; `docs/operators.md` renders each |
| POPULATION-GAPPED | **14** cells across 3 carrier columns | the ledger, section 6; each names the invariant that would convict it |
| SHAPE-GAPPED | **3** shapes as ADR-0176 words them, all shipped NARROWED instead | the ledger, section 5; ADR-0176 addendum (c) |
| CATALOG-GAPPED (Q6) | **0** | nothing this session considered was unconvictable |

23 matrix cells + 3 structural shapes = 26 candidates considered; 12
shipped, 14 recorded as unwitnessable, 0 invented to fill a hole.

**AUTHOR ACTION: one, and it is small.** RNG family tag 6
(`:mutation`) has now been carried unreserved through two
implementation sessions, because it is a `components/sim-engine` edit
and both sessions' fences put it out of reach. It is one line, its
whole purpose is to exist before someone needs to re-key the table, and
a third carry would be a habit rather than a decision. Either license a
sim-engine edit for it or rule it dropped.

**What P6 still owes**, and it is now three named things rather than a
question: the catalog-wide gate (buildable at last, since the ledger
gives it a population); the `:expected-findings` vocabulary cross-check
(needs the ruling Q11(a) deferred, on widening
`ehrt.sim-check.interface`); and the `:mutation` tag above. The 14
population gaps are NOT on that row — they are
`roadmap.md#referential-corpus-population`, priced as corpus authoring,
because it is the population and not the operators that is the real
work.

**CI: green at ** -- run 33572397123, the  workflow on , 10m53s, conclusion success. The close marker for this session.
