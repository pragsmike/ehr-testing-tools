# 2026-09-01 — event-stream mutation: the design ADR, and the future-features rider

## 1. Scope

Asked for: probe the mutation surface as it stands; write the design ADR
`roadmap.md#event-stream-mutation` calls for, with every open choice
lettered for an author ruling; write the `docs/future-features.md` rider
(author-ruled 2026-08-30) as a consumer-voiced torture-kit menu across
three fault layers; link it from README and from
`docs/consuming-ground-truth.md`'s exclusions; push and verify CI.

Did: all of it. **Nothing was implemented.** No `components/*/src` file
was opened for edit, no operator was written, no `engine/config-keys`
entry was proposed. The ADR proposes; the author disposes.

Landed in three commits from `f402868`:

1. `47c6cbb` — `notes/adr/0176-event-stream-mutation.md` plus the
   regenerated `notes/ADRs.md` row. Gate: `adr-index-test`, 10 tests /
   16 assertions, green.
2. the rider — `docs/future-features.md`, one line in `README.md`
   between the maturity table and Scope, one paragraph in
   `docs/consuming-ground-truth.md`'s fault-injection exclusion.
3. this record, its paired prompt archive, and the state-derived
   regeneration, last.

## 2. Red-green evidence

**A docs-only session's proof is the suite staying green and untouched**
(this README's own words), and it did. Full `make test` at the tip:
see section 5 for the figure, run unpiped per
`rulings.md#R-full-suite-before-push`.

**The doc gates were run individually before the suite**, because a new
file under `docs/` proper meets four of them at once and a failure there
is cheaper to find alone than inside a full run:
`link-footnote-gate-test`, `stale-path-test`, `invocation-lint-test`,
`readme-presence-test`, `index-completeness-test` — **28 tests / 729
assertions, 0 failures, 0 errors.**

**One gate shaped the rider's own text, and it is worth naming.**
`link-footnote-gate-test`'s third check forbids a visible `ADR-NNNN`
token anywhere in `docs/` prose, and its
`strip-footnote-definition-lines` is `(?m)^\[\^id\]:.*$` — it strips a
definition's OWN LINE and not its continuation lines. The rider's
footnotes were drafted wrapped, which would have left `ADR-0111` on a
continuation line and tripped the check. Rewritten as four single-line
definitions before the file was ever copied into the clone, so the gate
never went red — recorded because the next author to write a
docs/-proper footnote will hit exactly this and the gate's own docstring
does not say it.

**No oracle claim is made and none is owed.** No `components/*/src`
file changed, so no corpus, digest or baseline can have moved;
`bin/ground-truth-bracket` and `bin/regression-oracle` were not run,
and running them would have been ceremony rather than evidence.

## 3. Judgment calls, and their ratification status

**(a) The ADR's Q1 recommendation departs from the channel's stated
expectation. UNRATIFIED — this is the ruling the session exists to
ask for.** The prompt anticipated *"a post-decide, pre-apply transform
on the ground-truth log, so ALL emitters and all three apply sites see
one mutated truth"*, and instructed the session to correct that from the
tree if the pipeline disagreed. It disagrees, on four independent
readings at `f402868`:

1. `fold/apply-events` receives ONE decide's batch, never the log
   (`fold.clj:439`, called at `run.clj:1350`). Four of ADR-0166's five
   defect shapes — cross-patient, phantom index, wrong kind, inverted
   span — need the whole log and are inexpressible at that seam.
2. A mutation there is folded into `:world`, and `:log-mirror` publishes
   that world's `:ground-truth` back for a mid-run `decide` to read
   (`fold.clj:455-457`). There is no seam there that mutates the log
   without mutating what `decide` sees, so the fault propagates and
   `check` reports a cascade — "class X and nothing else" cannot close.
3. The engine REPAIRS some injections silently: a `:rejected` decide
   outcome means *"THIS one step doesn't happen"* (`run.clj:1303-1317`),
   so deleting an `:admission` there makes the later `:discharge`
   decline to fire and the injected defect vanish.
4. `rulings.md#R-transport-realism-vs-mutation` already assigns
   wrong-WORLD to `:churn-profile`. A content fault means the record is
   wrong and the world was right, so the world must not be re-derived
   from the mutant.

The recommendation is instead a post-run, whole-log stage outside
`engine/run`. **The row's own requirement is still met, and more
cleanly**: every emitter takes a log as input, so an emitter handed the
mutant inherits the mutation with no emitter change at all, and there is
exactly one mutated log because the stage produces exactly one.
Byte-identity becomes free by construction — the `:latency` precedent —
rather than something a sweep has to prove.

**(b) Nine lettered questions rather than a smaller, tidier set.
UNRATIFIED, by design.** Q1 injection contract, Q2 catalog home, Q3
sites per application, Q4 seed and RNG family, Q5 what "nothing else"
means, Q6 an unconvictable operator, Q7 CLI surface, Q8 v1 membership,
Q9 schema validity. Each is a place where two defensible readings exist
and the choice changes what gets built; none is a preference poll. Q6
and Q9 in particular were not in the prompt and surfaced from the tree:
Q6 because `operators.clj:24-39`'s "recorded as dropped, not shipped
unconvictable" precedent points one way while ADR-0166's own error
ledger points the other, and Q9 because the schema's `:int` versus
`[:maybe :int]` split (`event_schema.clj:716` against `:972`/`:1037`)
decides which cells of the operator matrix can exist at all.

**(c) The rider names the layer boundary as a design stance, not as a
ruling on ADR-0111's open taxonomy question. Deliberate.** ADR-0111
recorded message loss and duplication as *"a named future taxonomy
question, not resolved here"*. The rider states the semantic that makes
the kit coherent — a duplicate EVENT means the world had two
occurrences and a receiver must keep both; a duplicate MESSAGE means the
world had one and a receiver must dedupe — and its own footnote says
this is the shape an answer would take rather than the answer. A
future-features menu is not the register where a standing taxonomy
question gets closed.

**(d) The transport entry honours `R-mllp-abandoned` explicitly.**
ADR-0102 abandoned the `:mllp` sink and forbade transport work following
from it without a fresh ruling. The rider's transport entry therefore
says, in the consumer's own language, that this waits on a decision to
do transport work at all rather than on somebody writing it — and its
footnote points at that record. The framing codec's existence
(`corpus_io/framing.clj:164`, byte-exact) is stated so a reader does not
conclude more is missing than is.

**(e) `docs/README.md` was NOT edited.** It is a routing page keyed to
audiences, not an exhaustive index of `docs/`, and
`index-completeness-test` does not walk `docs/`. The prompt asked for
exactly two links and got exactly two. Named so a later reader does not
read the absence as an omission.

## 4. Findings

**F1 — the ADR-0166 promotion is smaller than it looks, and the useful
part is the DERIVATION.** The five test-side shapes
(`check_test.clj:452-483`) are hand-scripted logs for ONE invariant, and
promoting them literally would produce five operators over
`:start-event-id`. The schema carries four log-index reference fields —
`:cancels-event-id`, `:order-event-id`, `:start-event-id`,
`:placeholder-event-id` — each with a named invariant behind it, so the
v1 family is a 4x5 matrix. ADR-0166's own error ledger is the argument
for deriving rather than listing: `:medication-end` got a referential
invariant and its structural twin `:care-plan-end` did not, and the
asymmetry sat unnoticed from 2026-08-02 to 2026-08-23. A hand-listed
operator catalog reproduces that failure one layer up.

**F2 — `:person-event-id` is a stamp, not a reference, and the catalog
already says so.** `person-scoped-provenance-is-a-stamp-not-a-reference`
(`check.clj:1860`). It is the seventh field that looks referential and
the one that must NOT get a referential operator. Recorded in the ADR at
the place a reader would otherwise read its absence as an oversight.

**F3 — census §3e's `:log-mirror` reversal will be met FIRST by a
mutation consumer.** `.agents/plans/apply-unification-census.md:311-318`:
the concern is `(into (:ground-truth world) events)`, and a world
starting from `{:patients {}}` makes that `(into nil events)` — a LIST,
in REVERSE order. Inert at sites 2 and 3 today because both discard the
world. A caller folding a MUTANT through `replay` or `reinstated-state`
is exactly the new consumer that meets it, and must seed
`:ground-truth []`. Carried into the ADR rather than left in the census.

**F4 — `docs/consuming-ground-truth.md:561-588` becomes stale on the
implementing commit, in both halves.** The three-row fault-injection
table gains a fourth row and the paragraph beginning *"There is no
event-level mutation operator catalog today"* is retired outright. Named
in the ADR's Consequences so that session does not have to find it.

## 5. HEAD landed

Three commits from `f402868`:

* `47c6cbb` — `notes/adr/0176-event-stream-mutation.md` and the
  regenerated `notes/ADRs.md` index row.
* `48a84bd` — `docs/future-features.md`, the README line, the
  `consuming-ground-truth.md` paragraph.
* this commit — the record, its paired prompt archive, and
  `.agents/state-derived.md` plus both INDEXes, regenerated LAST.

**Full `make test`, unpiped, at the tip before the push
(`rulings.md#R-full-suite-before-push`): 4,765 tests / 24,255
assertions, 0 failures, 0 errors, `EXIT=0`.** `clojure -M:poly check`
`OK`; `bin/verify-nist-lock` `OK` on all six coordinates. Execution
time 23m03s for the `poly test` half — the one figure worth naming,
since it is roughly double CI's own recent 12-13 minute runs and is the
`wslhost` shape ADR-0167 characterized rather than anything this
session's diff did (a docs-only diff cannot move suite time).

**Doc gates run individually first**, before the full suite:
`link-footnote-gate-test`, `stale-path-test`, `invocation-lint-test`,
`readme-presence-test`, `index-completeness-test` — 28 tests / 729
assertions, 0 failures, 0 errors. `adr-index-test` at commit 1 — 10
tests / 16 assertions, green.
