# Session prompt — ORU MSH-10 injectivity (roadmap row 3)

Repo `ehr-testing-tools`, ext4 clone of record
`/home/mg/src/ehr-testing-tools`. HEAD at session start `e81f7026`,
matching `origin/main`. Ceremony R30 (the prompt named no prepare-only
mode). Archived under R-A alongside
[`2026-09-23-oru-control-id.md`](../session-records/2026-09-23-oru-control-id.md).

The prompt is verbatim below. It carried two unfilled placeholders — a
literal `[DATE]` and an unruled `[A | B]` ADR form — and its central
premise did not survive measurement; both were resolved in-session by
the author and are recorded in the deviation record at the end.

## The prompt, verbatim

> # ORU MSH-10 is injective: the result's control id carries its order's index (roadmap row 3)
>
> Context: `control-id-for` (segments.clj) keys every single-subject event `mrn-trigger-t`; two
> results for one patient in one second collide. Measured live: seed-424242-clinic-decade 6 duplicate
> MSH-10s in 2 groups (MRN000189-R01-119086260 x6, ...-125739060 x2), clinic-decade demo 1. The result
> event already carries `:order-event-id`, an :int log index proven to resolve to a real `:order-placed`
> for the same patient (through merges, ADR-0179 R-inv). EMISSION-ONLY: no ground-truth byte moves.
> Downstream (32k-subject retained corpus at e81f7026) is pinned on this landing and will be told the
> commit and the new key shape.
>
> Read first:
> - components/sim-emit-hl7/src/ehrt/sim_emit_hl7/segments.clj:61-97, registry.clj:58-59
> - components/sim-emit-hl7/src/ehrt/sim_emit_hl7/planners.clj:250-270 (the ordinal precedent)
> - components/sim-engine/src/ehrt/sim_engine/decide.clj:1564-1576; event_schema.clj:857
> - components/sim-check/src/ehrt/sim_check/check.clj:1291-1300
> - components/sim-emit-hl7/test/.../emit_hl7_test.clj:265-275, chatter_test.clj:238-250, ladders_test.clj:325-335
> - components/sim/src/ehrt/sim/identifiers.clj:100-112
> - notes/adr/0175-arc-4-emission-add-ons.md:264-272; .agents/plans/roadmap.md:37-41
> - .agents/session-records/2026-08-28-arc-4-sweep-3-status-ladders.md:288-300
> - bin/regression-oracle:26-32,96 (usage, declared-change line); bin/ground-truth-bracket (usage line)
>
> Author rulings [DATE]: ADR form: [A: new ADR-0181 | B: dated amendment to ADR-0175:266-272].
> Key shape: `mrn-<order-event-id>-R01-t` -- the order's log index, no ordinal, no new draw.
>
> Steps:
> 1. Red test in emit_hl7_test.clj: `control-id-for` is injective over `:result-available` in a
>    fixture with two orders on one patient whose results land in one second (fixed turnaround if the
>    shipped profiles draw it); AND a corpus gate: `sim identifiers` `:control-ids` are distinct on the
>    seed-424242-clinic-decade root.
>    Invariant: red at e81f7026 naming exactly the 6 measured duplicates.  Gate: brick:sim-emit-hl7
>    shows exactly these new failures and no other.
> 2. The arm: a `:result-available` case in `control-id-for` -> `(str active-mrn "-" order-event-id
>    "-" trigger "-" t)`, `order-event-id` destructured; docstring gains ORU as the second four-part
>    family with its discriminator named. No other arm changes.
>    Invariant: `git diff -- components/sim-engine components/sim-check` is empty.
>    Gate: step 1 green; brick:sim-emit-hl7 and brick:sim green; clojure -M:poly check OK.
> 3. Readers: grep components/ bases/ for anything splitting a control id on "-" or assuming three
>    parts (corpus-io, judge, ladders basis, bidirectional-derivability).
>    Invariant: every reader keys on the whole id or on a tuple the message carries; none on part count.
>    Gate: brick tests of every namespace the grep touches, green.
> 4. `bin/ground-truth-bracket e81f7026 HEAD` with no flag; `bin/regression-oracle e81f7026 HEAD
>    --declared-digest-change`.
>    Invariant: bracket IDENTICAL on every digested root; oracle DIFFERS on exactly the roots whose
>    corpora carry `:result-available` and IDENTICAL on every other; list both sets in the record.
>    Gate: the two banners.
> 5. ADR per ruling; roadmap row 3 -> CLOSED with the commit; figures.edn untouched (a field moved, no
>    count did -- say so in the record). `make docsgen && git diff --exit-code` clean.
>    Invariant: `grep -rn "mrn-trigger-t" docs/ notes/` names ORU's exception wherever it states the
>    base shape.  Gate: that command.
> 6. Not docs-only: full `make test` unpiped, MAKE_EXIT recorded, wrapper ends `exit "$MAKE_EXIT"`.
>    Record .agents/session-records/[DATE]-oru-control-id.md + prompt archive, same slug. Push; gh run
>    view the tip; CI green is the close marker. Record carries the downstream notice line: commit sha,
>    key shape, "ground-truth unchanged -- your retained corpus stands."

## Deviation record

**The session executed step 1's measurement and then halted. Steps 2, 3,
4 and the closing half of 5 were NOT executed, by author ruling.**

1. **Premise mismatch, found at step 1 and escalated rather than worked
   around.** The prompt's context paragraph attributes
   `seed-424242-clinic-decade`'s six duplicate MSH-10s to
   `:result-available`. Measured: that corpus contains **zero**
   `:result-available` events, and the six duplicates are
   `:observation`/`:diagnostic-report`, which carry no `:order-event-id`
   and cannot take the ruled key shape. Applying the ruled arm leaves the
   corpus unchanged, so step 1's corpus gate would have been red at
   baseline and red after — unsatisfiable as written.
2. **Author ruling, 2026-09-23, replacing the unfilled `[DATE]` line.**
   Scope: **halt, characterize only** — land nothing behavioural, return
   the key shape to the design channel. ADR form: **A, a new ADR-0181**.
3. **Steps not executed, and why.** Step 2 (the arm) and step 3 (reader
   grep for part-count assumptions) are behavioural and fall under the
   halt. Step 4's bracket and oracle were not run: with no executable
   form changed, the oracle's verdict is vacuous, and the record names
   the committed digest gates that did run instead rather than claiming
   an oracle it did not invoke (ADR-0030 J2). Step 5's roadmap closure
   became a roadmap **correction** — the row stays OPEN.
4. **Step 5's `mrn-trigger-t` grep invariant, reinterpreted.** The prompt
   wanted every doc stating the base shape to name ORU's exception. No
   ORU exception exists, so the grep's two hits in
   `notes/adr/0175-arc-4-emission-add-ons.md` are left as they are: both
   describe chatter's ordinal accurately and neither makes the
   `:result-available` attribution ADR-0181 corrects.
5. **`figures.edn` untouched**, as the prompt required — and for a
   stronger reason than it anticipated: no field moved either.
6. **Scope the session added**, unruled and disclosed: seven readers'
   prose corrected in the same commit, since they asserted the population
   this session measured false. Prose only. Reasoning and reversibility
   in the record's "Judgment calls" section, item 2.
7. **`bin/preflight` ran mid-session, not first.** Disclosed in the
   record; its only finding was this session's own uncommitted work.
