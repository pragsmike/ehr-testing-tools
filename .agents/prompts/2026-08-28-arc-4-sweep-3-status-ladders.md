# Arc 4 sweep 3 -- status ladders (ADR-0175 design (b), ruling B1)

Session prompt, archived verbatim. Drove
`.agents/session-records/2026-08-28-arc-4-sweep-3-status-ladders.md`.

---

Session prompt -- arc 4 sweep 3: status ladders (ADR-0175 design (b), ruling B1)

Context. HEAD d0e7feb. Sweep 2 landed chatter/charges and the sampler.
This sweep is design (b): order/result status ladders -- ORM^O01
restatements carrying ORC-5, ORU^R01 restatements carrying OBR-25/OBX-11
in-progress codes, rungs at fixed fractions of the `:order-placed` ->
`:result-available` interval (a pure function of the log: `:result-
available` carries `:order-event-id`, ADR :?(b) -- NO draw, state whether
that holds all the way or one ratio draw rides `:emission` under the
fixed-consumption law). Behind `:ladders`, absent = today byte-for-byte.
The vocabulary (tables 0038/0123/0085) appears in no jar and no resource
in this tree -- it ships as site-profile code tables (`:order-status`,
`:result-status`, `:observation-result-status`) beside `:bed-status`,
overridable, never asserted as citation. Read ADR-0175 (b) whole, sweep
2's record (control-id ordinals `mrn-trigger-t-<ordinal>`; the
non-injective derivability triple -- rung messages must not worsen it:
state the rung's identity tuple and test its injectivity on a corpus).

Step 0 -- the root the ADR requires BEFORE the sweep (its own coverage
note: no oracle root places an order; `witnessed-event-kinds` holds
neither `:order-placed` nor `:result-available`, no ORM^O01 anywhere).
Add ONE root `order-pathway` (dark for this sweep -- NO `:ladders` key)
whose config demonstrably places orders and yields results; its coverage
note claims the two kinds and both message families. Existing 40 roots
untouched; `bin/ground-truth-bracket d0e7feb HEAD` IDENTICAL 36+;
oracle: one `+` line, declared as root-addition per sweep 2's precedent
(or undeclared if the script treats additions as clean -- read
`bin/regression-oracle`'s contract and say which). Commit.

Step 1. RED then GREEN, dark. Unit configs WITH `:ladders {:rungs
[0.25 0.5] ...}` (shape per the ADR; defaults are YOUR clinical choice,
disclosed as sweep 3b-3 did): (i) each rung an ORM^O01 (ORC-5) or
ORU^R01 (OBR-25/OBX-11) restating the SAME ORC/OBR the final message
carries, at `t = placed + f*(result - placed)`, riding `emit-wire`'s
latency plan untouched for non-ladder messages (byte-equal, assert);
the final `:result-available` message gains the FINAL codes in the same
fields -- that edits an existing message's bytes, so it is part of THIS
sweep's declaration, say so; (ii) codes from the three new site-profile
tables, override proven (the aldric-style profile precedent); (iii)
MSH-10 unique across ladder + chatter + base at a t collision; the rung
identity tuple injective on a real corpus (the sweep-2 finding, not
repeated); (iv) unsolicited `:observation` ORUs gain NO ladder (they
have no order -- assert zero); (v) the sampler's strata absorb the new
volume with no code change (assert the ORM/ORU strata report n/gated);
(vi) `classify-change`: zero contract diff or STOP. PROOF: ground-truth
bracket IDENTICAL; oracle IDENTICAL over all 41 (no root opts in yet).
Commit dark.

Step 2. ON. Opt in the six corpora (+ `config-latency.edn`) AND the
`order-pathway` root (its purpose); the other 40 roots stay. Both
brackets: ground truth IDENTICAL; oracle declared -- the mover set is
exactly the opted-in roots (name non-movers incl. the two zero-message
roots and any corpus with zero orders: a corpus that places no order
gains no ladder, `pos?` only where the population exists -- count
`:order-placed` per corpus FIRST and print it in the witness table).
Re-pin the message-side list; tripwire pre-push. Witness table: rungs
per corpus by family, final-message code changes, sampled strata,
orders per corpus. `make test` + `make integration`. Commit ON.

Step 3. Push; CI; no tag. Record one page: bracket lines per commit,
witness + sampling tables, the rung identity tuple, defaults disclosed,
ADR premises contradicted. Roadmap: sweep 3 of 6; SIU next.

Fences. No ground-truth change (bracket-enforced); no draw unless
stated under the fixed-consumption law; no new event kind or schema
diff (STOP); no SIU/NK1/fan-out/MLLP; tables never asserted as HL7
citation. One declared message sweep.
