# 2026-09-01 — event-stream mutation, implementation 1: the spine

## 1. Scope

Asked for: **the spine, not the catalog.** ONE referential operator end
to end — registration, application, lineage, CLI, and the closed oracle
loop — proving the whole contract ADR-0176 designed, under all nine
questions ruled `(a)` on 2026-09-01. Breadth is session 2.

Did: all of it, in four commits from `7096394`, red before green.

1. `1c22a10` — RED. The closed-loop acceptance test, committed failing.
2. `4b4f992` — GREEN. `:format :event` joins the operator registry, the
   `mutate-event` stage, the operator, the Q6 refusal path, the lineage
   envelope.
3. `652ff40` — `ehrt sim mutate`, plus the two docs ADR-0176 named as
   going stale on the implementing commit.
4. this record, its paired prompt archive, one test-expectation catch-up
   and the state-derived regeneration, last.

**RED-BEFORE-GREEN was in force and was honoured.** This row is
behavior, not refactor, so S1(a) does not apply. Commit 1 is the
acceptance test failing; commit 2 is what makes it pass.

## 2. Red-green evidence

**The RED, shown rather than asserted.** At `1c22a10`,
`clojure -M:poly test :all skip:integration`: ten namespaces green (19,
88, 28, 10, 51, 23, 89, 73, 45, 41 passes; 0 failures, 0 errors), then

```
Syntax error compiling at (ehrt/corpus/event_mutate_test.clj:173:14).
java.lang.RuntimeException: No such var: mutate/event-content-hash
```

— the first of the three API elements the spine requires and that did
not exist (`mutate/event-content-hash`, the `:format :event` dispatch
branch, `operators/catalog-gaps`). `clojure -M:poly check` was **OK** at
that same commit, which is itself the evidence for ADR-0176 Q2(a)'s
dependency claim: the corpus → sim and corpus → sim-engine *test-scope*
edges the acceptance test adds are legal and add no cycle.

**The GREEN.** `ehrt.corpus.event-mutate-test`, 46 assertions, and the
full suite green at each subsequent commit.

**THE LOOP, EXERCISED FOR REAL through `bin/ehrt`** — the step-4 gate,
and the thing the row exists for:

| step | invocation | result |
|---|---|---|
| 1 | `ehrt sim check < clean.edn` | exit 0, `:status :ok` |
| 2 | `ehrt sim mutate < clean.edn` (no operator) | `cmp` byte-identical over 335,547 bytes; sha256 `b01ae5ca…2e05` both sides |
| 3 | `run --format ground-truth \| mutate --operator-id phantom-placeholder-event-id --seed 424242 \| check` | exit 1, and the ENTIRE violation list is one entry: `{:invariant :identity-fill-references-its-placeholder-registration :patient-id "PID-000099-86c0f8f4" :at 448531961}` |

Step 3 is Q5(a)'s set EQUALITY holding at the shell, not merely in a
test: observed = declared, one element, nothing else.

**Oracle.** `bin/ground-truth-bracket 7096394 652ff40` — **IDENTICAL**,
38 roots digested, 3 skipped by name (`appendicitis.edn`,
`ear-infections.edn`, `sore-throat.edn`, the interpreter-layer batch
roots carrying no `:ground-truth` key). `bin/regression-oracle
7096394 652ff40` — see section 6. `652ff40` is the last commit in this
session touching any `src` file; everything after it is test
expectations, docs and session records, so the claim covers the whole
arc's behavioural surface. Byte-identity here is expected BY
CONSTRUCTION rather than by proof: the stage is post-run and outside
`engine/run`, so there is no path by which a shipped corpus could move.

## 3. Judgment calls, and their ratification status

**(a) The operator is `:phantom-placeholder-event-id`, not the
`:start-event-id` one ADR-0176's own lineage example sketched. RATIFIED
BY MEASUREMENT, and it is this session's most consequential call.**
ADR-0176 section 2(iii) illustrates the lineage record with
`:dangling-start-event-id`. Probed at `7096394` before anything was
written, over both the oracle's engine-layer roots and full sim runs:

| reference field | carriers | resolving |
|---|---|---|
| `:cancels-event-id` | 0 | 0 |
| `:order-event-id` | 1 | 0 (a legitimate pre-horizon straddle, reference correctly `nil`) |
| `:start-event-id` | 0 | 0 |
| `:placeholder-event-id` | 29–31 | all |

`:placeholder-event-id` is the only reference field with a real
population. It is also the only one of the four whose invariant carries
**no pre-horizon escape hatch** — `medication-end-…` and
`care-plan-end-…` both excuse a nil reference when the patient's own
`:registered` carries a matching `:pre-horizon-facts` citation, and
`identity-fill-references-its-placeholder-registration` has no such
branch. A resolving reference made dangling therefore convicts
unconditionally, with no excuse logic for the operator to have to
mirror. That is what makes it the spine's operator rather than a
hand-wave: the contract is proved where the proof is unambiguous.

**(b) `:site` was added to the lineage record; ADR-0176's example did
not show it. RATIFIED — the prompt required it** ("parent identity,
operator id, seed, site recorded"). It also earns its place: with one
draw over a candidate set, the seed alone identifies the site only if
you also have the parent log, and `:site` makes the injection legible
from the record alone.

**(c) `corpus.lineage`'s `:transformation` became a `:multi` over two
STRICT shapes, not one loosened map. Deliberate.** The obvious move —
make `:locator` optional and add optional `:seed`/`:site` — would have
quietly stopped saying that a *file* lineage record without a locator is
malformed. Two dispatched shapes keep both guarantees.

**(d) Q6's refusal is implemented as "declares no finding", not
"declares a finding `check` does not have". DISCLOSED, and the weaker of
the two.** ADR-0176 section 2(i) claims `:expected-findings` is
"MACHINE-CHECKABLE … because `check`'s vocabulary is this repository's
own". It is not machine-checkable *at registration* today:
`ehrt.sim-check.interface` exports `check-all` and nothing else, and its
own docstring says its contents are "exactly the union of what residual
sim's own src-scope callers reach today, found by fresh call-position
grep, not by interface-design judgment". Enumerating the invariant names
means widening a façade whose width is a standing decision, which is not
a spine session's call to make unilaterally. So registration refuses an
EMPTY finding set (`:unconvictable-operator`, recorded as a catalog gap)
and the closed loop proves the declared set exactly right for the one
registered operator. **The vocabulary cross-check is session-2 scope and
needs a ruling on widening that seam.**

**(e) `ehrt sim mutate`'s lineage is reachable programmatically but not
at the shell. DISCLOSED, not resolved.** `main!` gives `:bare-text`
metadata precedence over `--json`/`--edn` — the precedent
`ehrt sim run --format ground-truth` set — so a filter's stdout is the
mutant and the envelope carrying the lineage is shadowed. Inventing a
different precedence rule for the second bare-format verb would be
exactly the drift this repo's discipline exists to prevent, so the
precedent was followed and the gap is named instead. A consumer who
pipes a mutant onward currently has no shell-level provenance for it.
**Session-2 want; a `--lineage PATH` sidecar is the obvious shape and
was deliberately not invented here.**

**(f) The judge-side conviction gate was scoped rather than satisfied.
Deliberate, and argued.** `every-catalog-operator-has-at-least-one-
witnessed-row-test` requires every catalog operator to have a witnessed
pairing row against a judge. Event operators have no judge and cannot
acquire one: their substrate is the in-memory log, never a file. They
are not unwitnessed — their conviction is proved *harder*, by the closed
loop asserting observed = declared exactly, where a pairing row asserts
only that *some* expected class appears among the observed. The
exclusion is a statement about which instrument witnesses which layer.

## 4. Findings

**(F1) ADR-0176 section 2(iv)'s declared population is EMPTY. The one
reading of the ADR the tree refuted.** That section names
`bin/ground-truth-bracket`'s gated corpora as "the natural population"
for the catalog-wide gate. Measured at `7096394`, every engine-layer
oracle root runs a `module-only` pathway and carries **zero** carriers
of all four log-index reference fields (sinusitis 0/0, sepsis 0/0,
appendicitis 0/0, sore-throat 0/0). The reference fields are minted by
the full sim path — scheduling, identification, medication spans — which
those roots do not exercise. **Session 2 cannot use the gated corpora as
its population and must either bring its own scenario runs or add a
root that exercises the full path.** This is the single most important
thing this session hands forward.

**(F2) `:cancels-event-id` and `:start-event-id` have no population
anywhere, not just in the oracle roots.** In a 200-patient
`clinic-decade` run: 7 `:appointment-cancel` events, all carrying
`:appointment-id` and none carrying `:cancels-event-id`; 5
`:care-plan-start` events and 0 `:care-plan-end`. Two of the derived
matrix's four columns are therefore **unexercisable on anything this
repository currently generates**, and session 2 owes a decision: author
a scenario that produces them, or record those columns as declared gaps
the way the v2 catalog records its dropped candidates.

**(F3) The link-footnote gate caught a real defect at exactly the right
moment.** The operator's `:contract/:target` sentence carried a visible
`ADR-0173` token — and that sentence is rendered *verbatim* into
`docs/operators.md`, which is consumer-facing prose. Fixed at the
source (provenance moved into the catalog's own comments), not by
exempting the file. **Anyone adding an operator should know the
`:target` sentence is public prose, not an internal comment**; nothing
in `operators.clj` said so before, and now its own comment does.

**(F4) EDN round-trip over a ground-truth log is byte-identical**,
measured over 335,546 bytes and confirmed at the shell with `cmp` and
`sha256sum`. This is what lets `ehrt sim mutate`'s pass-through be a
genuine byte-identical path through the same codec both branches use,
rather than a special-cased copy of stdin. It also justifies
`event-content-hash` hashing `pr-str` output: the hash identifies the
bytes a consumer actually holds.

**(F5) `poly test` aborts the whole run on the first project's failure**,
so a single red namespace hides every later project's state. Four
separate iterations this session were needed to walk out to the
`ehrt-cli` project's own tests. Worth knowing when estimating a
"suite green" claim's cost.

## 5. What this session deliberately did NOT do

* **No second operator.** The derived referential family (4 reference
  fields × 5 defect shapes, minus the cells the event schema forbids)
  and the three structural operators (`drop-event`, `clock-skew`,
  `orphan-participant`) are session 2, as the prompt scoped.
* **No catalog-wide gate.** ADR-0176 section 2(iv)'s "run the whole
  catalog against a fixed set of clean logs" needs a population that
  exists (F1) and more than one operator. Not stubbed, not half-built.
* **No `engine/run`, emitter, or `fold/apply-events` edit**, and no
  `engine/config-keys` entry — the stage is outside `run` entirely.
* **No locator override** for event operators, no `--rate`/`--count`
  multi-site parameter, no schema-invalid mutants — Q3(b), Q9(b) and
  the locator question are named growth paths in ADR-0176, all additive.
* **RNG family tag 6 (`:mutation`) was NOT reserved in
  `streams.clj`.** ADR-0176 section 2(iii) says to reserve it unused.
  It is in `components/sim-engine`, the operator draws from its own
  seed and touches no run stream, and reserving it is a one-line
  sim-engine edit this session's fences put out of reach. **Carried
  forward as an explicit debt** — it is cheap, and its whole purpose is
  to be there before someone needs to re-key the table.

## 6. Close

## 6. Close

**Full suite, unpiped, at the tip** (`rulings.md#R-full-suite-before-push`):
`clojure -M:poly check` **OK**; `clojure -M:poly test :all
skip:integration` exit **0** — 414 namespaces, **24,542 passing
assertions, 0 failures, 0 errors** across both projects.

**Oracle, both halves, `7096394` → `652ff40`:**

* `bin/regression-oracle` — **IDENTICAL**: every root's digest matches
  between the two disposable worktrees. This is the whole-pair claim
  `rulings.md#R-oracle-script-contract` reserves the phrase for.
* `bin/ground-truth-bracket` — **IDENTICAL**, 38 roots digested, 3
  skipped by name (`appendicitis.edn`, `ear-infections.edn`,
  `sore-throat.edn`).

Expected by construction, and stated that way rather than as a lucky
result: the stage is post-run and outside `engine/run`, so there is no
path by which a shipped corpus could move. `652ff40` is the last commit
touching any `src` file this session.

**Fences, each checked:** no `engine/run` edit; no emitter edit; no
`fold/apply-events` edit; no `engine/config-keys` entry; mutation absent
is byte-identical everywhere (proved twice — by the oracle for shipped
corpora, and by `cmp` for `ehrt sim mutate`'s own pass-through).

**AUTHOR ACTION: none.** No tag, no repo-level `gh` mutation, no git
surgery.

**What session 2 is owed, priced.** The remaining catalog is 19
referential operators (4 reference fields × 5 defect shapes, minus the
cells the event schema's `:int` versus `[:maybe :int]` split forbids)
plus 3 structural ones. The contract is now fixed, so each is a
`:candidate-sites` predicate, a one-site `:fn`, a declared finding set,
and a row in the loop — mechanical, once the population question is
answered. **It is the population, not the operators, that is the real
work**: finding F1 says the declared population is empty, and findings
F1–F2 together say two of the four reference-field columns are
unexercisable on anything this repository currently generates. That
needs a scenario or a new oracle root before the breadth is worth
writing. Two smaller carries: the `:mutation` RNG family tag is still
unreserved (section 5), and `:expected-findings` is not yet checked
against `check`'s own vocabulary at registration (section 3(d)), which
needs a ruling on widening `ehrt.sim-check.interface`.

**CI: green at `87bce30`** — run 33553136845, the `test` workflow on
`main`, 12m04s, conclusion success. The close marker for this session.
