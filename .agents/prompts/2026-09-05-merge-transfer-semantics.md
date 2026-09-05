# 2026-09-05 — merge transfer semantics (ADR-0179)

Repo `pragsmike/ehr-testing-tools`, clone `~/src/ehr-testing-tools`
(ext4, WSL — the sole clone of record), session start `007deea6`, equal
to `origin/main`. Ceremony mode R30 (commit and push at each
checkpoint), taken from the prompt below. Paired record:
[`../session-records/2026-09-05-merge-transfer-semantics.md`](../session-records/2026-09-05-merge-transfer-semantics.md).

## The prompt, verbatim

```text
Session: merge transfer semantics (ADR-0179) — 2026-09-05

Context: a merge today sets :status :merged on the absorbed record and nothing else
(evolve.clj:290-295); the run loop then drops that patient-id's remaining queue
(run.clj:1289-1300, M2b). So the absorbed record keeps its bed (census ghost) and a
pending :result-followup vanishes rather than re-associating to the survivor. Ruled
2026-09-05: release the bed; re-queue result followups on the survivor; widen the
order-reference invariant to resolve through the merge. Payload behavior → ADR.
Fresh clone of pragsmike/ehr-testing-tools at 007deea6 or later. WSL only. No sub-agents.

Read first: AGENTS.md; .agents/skills/build-session/SKILL.md (:87-88, :142 close ceremony);
evolve.clj:290-310; run.clj:1282-1300, 1380-1396; decide.clj:1434-1500;
check.clj:1147-1188; components/sim/docs/patient-state-model.md:540-556;
notes/adr/0178-*.md (shape); bin/ground-truth-bracket; .agents/rulings.md.

Author rulings, verbatim and binding:
 R-bed:   "the :merged arm clears :location and :home-ward" — bed released at merge.
 R-queue: ":result-followup steps of the absorbed patient-id re-queue on the survivor at
          the same :t, :active-mrn rewritten to surviving-mrn, subject rewritten to the
          survivor's patient-id; all other queued steps stay dropped."
 R-inv:   "result-references-existing-order-and-follows-it-in-time accepts an order whose
          subject was merged into the result's subject by a :merge at :t <= result :t."
 R-loc:   result :location/:attending stay order-time. Not touched; recorded in the ADR
          as open pending downstream reply.
 R-pins (standing): a moved count pin is its own commit. R-edit (standing): backticked
          prose through a script file. Declared oracle change: roots reaching :merge move.

Steps (one gate each; commit message given):
1. bin/preflight. Enumerate oracle roots whose ground truth contains a :merge, and the
   subset where the absorbed patient-id had a :result-followup queued at merge time.
   Invariant: counts recorded before any engine edit. Gate: none (derivation).
   Commit: "docs: ADR-0179 derivation -- oracle roots reaching :merge enumerated".
2. Write notes/adr/0179-merge-transfer-semantics.md (R-bed, R-queue, R-inv, R-loc-open,
   the :merged-into field below, step-1 counts); index row in notes/ADRs.md.
   Gate: make state-derived diff clean. Commit: "docs: ADR-0179 merge transfer semantics".
3. RED. Tests: (a) evolve :merge :merged arm → :location nil, :home-ward nil, and a new
   :merged-into survivor-patient-id (channel expectation: the survivor id comes from the
   event's participants :role :survivor; correct from the tree). (b) run loop: a result
   followup queued on the absorbed id lands as :result-available on the survivor with the
   survivor's :active-mrn, no event on the absorbed id after its terminal. (c) invariant:
   merged-through order passes; an unrelated patient's order still convicts; the
   :order-event-id mutation operators (operators.clj:512, 539) still convict.
   Invariant: all three fail. Gate: make test shows exactly those failures.
   Commit: "test: ADR-0179 red -- bed release, followup re-queue, invariant widening".
4. GREEN engine. evolve.clj :merged arm per R-bed + :merged-into. run.clj M2b branch: before
   recur, filter the popped entry's steps for :type :result-followup, rewrite per R-queue
   using :merged-into, and enqueue via the same reduce at :1380-1387 (same seq-no
   discipline). Invariant: bracket-proven; movement confined to step-1's roots, every
   moved root reclassified in the record. Gate: bin/ground-truth-bracket.
   Commit: "engine: merge releases the bed and re-queues result followups (ADR-0179)".
5. GREEN checker. Widen check.clj:1174-1188 per R-inv: resolve the order's subject through
   any :merge in the same log. Invariant: 46 catalog vars unchanged (if a NEW invariant is
   needed, its pin move is a separate commit per R-pins). Gate: make test.
   Commit: "check: order reference resolves through a merge (ADR-0179)".
6. Downstream fixture. Regenerate test-fixtures/downstream-calibration at 500/1000; record
   whether shas move and, if so, the exact event-level diff (as 753d320's record did).
   Gate: make test. Commit: "test: downstream calibration re-measured under ADR-0179".
7. Docs (script file, R-edit): patient-state-model.md:555 :merge row planned→landed with the
   new transfer semantics; consuming-ground-truth.md merge sentence if one exists.
   make state-derived. Gate: make test. Commit: "docs: merge semantics documented".
8. Session record (asks-to-disposition; step-1/4/6 counts as recorded evidence; judgment
   calls with ratification status); archive this prompt; close ceremony per SKILL.md
   (enumerate/terminate background processes). Push. Verify CI. Close-marker commit
   recording CI success sha. Commit: "docs: record CI success at <sha> -- ADR-0179 close".
```

## Deviation record

Everything the session did differently from the prompt above, and why.
The full reasoning is in the paired record.

1. **Step 1 needed a committed artifact and the prompt names none.**
   The commit message is `docs:`, so the derivation was written to
   `.agents/plans/2026-09-05-adr-0179-merge-census.md` (the shape
   `2026-09-01-event-mutation-population-ledger.md` already sets) plus
   its `README.md` index row.

2. **Step 3's gate is a PARTIAL witness, and this is disclosed rather
   than reported as met.** `make test` halts at the first failing brick,
   so the red run reports test (c)'s two failures inside the
   `conformance` project and never reaches `sim-engine`, where (a) and
   (b) live. All three were captured red by targeted runs instead; both
   outputs are in the record.

3. **R-inv is implemented TRANSITIVELY, a disclosed generalization.**
   The ruling's wording is one hop; merge chains are reachable under
   R-queue, and the literal reading would convict a log the engine
   itself now writes. Taken as fix-forward-with-disclosure
   (`rulings.md#R-stop-only-on-two-defensible-readings`) and flagged for
   ratification in ADR-0179's own R-inv section.

4. **Step 3(c)'s third clause needed no new test.** The
   `:order-event-id` mutation operators at `operators.clj:512` and
   `:539` are already gated by `event_mutate_test.clj`'s catalog loop,
   which asserts the exact finding set each operator declares. Their
   requirement here is non-regression, and the loop is what proves it;
   two hand-built conviction tests were added beside the widening
   anyway, because the loop cannot show that the widening is *narrow*.

5. **Two items were found that no ruling covers, and neither was
   fixed.** The bed a merge now frees never returns to housekeeping
   (`:merge` is not in `bed-correction-event-types`), and the wire-side
   `v2-replay` tombstone now diverges from the engine's merged record.
   Both are recorded — the first in ADR-0179's own open items beside
   R-loc, the second in the record and in the test docstring that used
   to claim the two mirrored each other.

6. **One adjacent erratum was fixed rather than left.**
   `patient-state-model.md`'s accumulator table typed `:status` as
   `[:enum :new :admitted :discharged :expired]`, omitting `:merged`,
   which the code has carried since M2b. Fixed because the same commit
   adds a `:merged-into` row whose own text says "absent unless
   `:status = :merged`", and leaving the two adjacent would have been an
   introduced contradiction.
