# Session prompt -- arc 3b-2: the bed-status cycle, with ADT^A20 (ADR-0174 §2(c)+(d), sweep 2 of 3)

**Archived** by the session it drove,
[`.agents/session-records/2026-08-27-arc-3b-bed-cycle.md`](../session-records/2026-08-27-arc-3b-bed-cycle.md).
Verbatim, as issued.

---

Context. HEAD 9fc9df5. Sweep 1 lifted the encounter horizon (dark IDENTICAL
36 roots, on = one added root). This sweep is R-mix-6: bed status is STATE
-- vacated->dirty->cleaning->ready, `allocate` gated on `:ready`,
`bed-ready-location` consumes the READY event, durations per ward from the
`:facility` stream (D1) -- plus the author's addition at ruling C: ADT^A20 on
the wire so the bed board (`ehrt play --board`, ADR-0067) can see the cycle.
Dark-then-on behind a run-config key `:bed-cycle` (absent = today byte-for-
byte; the opt-in law). Read ADR-0174 §2(c) whole (:467-600) and §2(d)'s A20
paragraphs (:640-672), then sweep 1's record -- two traps it names WILL
recur here: `classify-change` compares nested `[:map-of]` values whole, so
a key added inside a nested map reads as breaking (bump if it says so); and
a top-level event field belongs to the event's FIRST participant only. §2(c)
widens `:participants` with a `{:bed-id :ward :role :subject}` shape --
that is exactly where the second trap bites. Re-derive every line.

Step 1. RED then GREEN, dark. Unit configs WITH `:bed-cycle`, `pos?`
witnesses: (i) world `:beds` index `bed-id -> {:status :since-t
:last-patient-id}`, every licensed bed born `:ready` (:470-475); (ii)
`facility.clj:56` `free` = "status is `:ready`" when the index is present,
"not a key in board" when absent -- the absent path must be the SAME
function of the same inputs, not a copy; (iii) one kind
`:bed-status-change` (`:bed :ward :from :to`, `:last-patient-id` on the
`:dirty` transition only, :517-520), a bed-subject participant, on the
wire as ADT^A20 `[MSH EVN NPU]` with NPU-1 the PL PV1-3 already renders
and NPU-2 the status -- the first message with no PID/PV1, so it cannot
go through `single-subject-message`; add the sibling, do not widen it;
(iv) the cycle: discharge/transfer-out -> `:dirty`@t -> `:cleaning`@t+d1
-> `:ready`@t+d1+d2, d1/d2 from `Ward :turnaround-minutes` (schema
`config.clj:19`; every existing facility file gains a default the
session states) drawn on `:facility`; `bed-ready-location` decided at
the READY event (:492-500); (v) cancels: `:cancel-discharge`/`:cancel-
transfer` restore the bed's status with the location, via
`:reinstate-index` (:573-579) -- the dirty->occupied arc is legal ONLY
there; (vi) invariants 1-3 per :558-600 -- `no-assignment-to-a-non-ready-
bed` (the four `allocate` callers; `:bed-swap` EXCLUDED, :98-104, with the
reason in the docstring), `every-ready-follows-a-cleaning`,
`bed-cycle-transitions-are-legal` -- each firing on a mutated corpus;
`occupancy-within-capacity` and `surge-only-when-earlier-rungs-exhausted`
asserted UNCHANGED (4-5); every existing patient-keyed invariant
`filter`s participants by `:patient-id` presence (:78-80) -- count the
filters you add; (vii) `classify-change` reported, bump if owed. PROOF:
`bin/regression-oracle 9fc9df5 HEAD` IDENTICAL, 37 roots, no declaration;
pinned fixtures, both v2 conformance baselines, `demos/traces` byte-
equal. Commit dark.

Step 2. ON. Opt in the six corpora (+ `config-latency.edn`, which must
stay ground-truth-identical to `config.edn` -- the exerciser asserts it)
and ONE new oracle root pair `bed-cycle` (37 existing untouched). Predicted
movers: every opted-in corpus -- every bed-ready transfer shifts by
d1+d2 (today 7 of 102 vacate->occupy transitions are at 0 s, :ADR §1(ii));
A20 messages appear in every emitted stream. Re-pin ONCE per sweep 1's
list; `bin/regression-oracle <dark sha> HEAD --declared-digest-change`:
37 IDENTICAL, one `+`. Wire witnesses across the opted-in set, `pos?`:
`:bed-status-change` per transition kind, A20 messages, bed-ready
transfers now strictly after their discharge (0-second transitions -> 0),
boarders whose wait grew. `ehrt play --board` on ed-tuesday renders
`:dirty`/`:cleaning` beds -- the consumer R-mix-6 names, now fed; add the
board assertion to `bin/demo-exerciser-ed-tuesday`. `make test` +
`make integration` (the 38th root's three integration-tier pins, as
sweep 1's `611a285` learned). Commit ON.

Step 3. Push; CI; no tag. Record one page: both oracle lines, the
witness table (0-second transitions before/after is the headline), the
board screenshot-as-text, filters added, bump owed or not, ADR premises
the tree contradicted. Roadmap: 3b sweep 2 of 3.

Fences. Step 1 moves nothing. Step 2 moves only opted-in corpora and the
new pair. No scheduling kind. `:bed-swap` stays out of the cycle. No
second churn lottery entry. MSH-12 stays `"2.3"` (A20 exists in 2.3; say
where you checked). No re-pin outside the list without naming it.
