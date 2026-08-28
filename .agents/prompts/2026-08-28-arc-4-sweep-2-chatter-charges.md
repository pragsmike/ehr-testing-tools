# Arc 4 sweep 2 -- chatter and DFT^P03 (ADR-0175 designs (a)+(c), ruling B1)

Session prompt, archived verbatim. Drove
`.agents/session-records/2026-08-28-arc-4-sweep-2-chatter-charges.md`.

---

Session prompt -- arc 4 sweep 2: chatter and DFT^P03 (ADR-0175 designs (a)+(c), ruling B1)

Context. HEAD 1a71b36. Sweep 1 flipped to 2.4 and landed
`bin/ground-truth-bracket` -- the proof instrument this sweep inherits.
This sweep is B1's first tranche: re-statement chatter (A08/A31/A28/IN1,
design (a)) and DFT^P03 charges (design (c)), each behind its OWN opt-in
key (E1): `:chatter` and `:charges`, absent = today byte-for-byte on the
wire. Ground truth NEVER moves -- chatter is derivable restatement, the
price table is emission config; a new ground-truth event kind is a STOP.
Read ADR-0175 designs (a), (c), (h) whole -- (a) carries the message
table (:245-268), the fixed-consumption law (:255-257: one draw per
ground-truth event in log order, drawn and discarded for uncovered
events, so rule X never shifts kind Y), the control-id change (:266-270:
`mrn-trigger-t-<ordinal>`, ordinal within `(mrn, trigger, t)`), and the
A08-vs-A31 rule (encounter open at t). Sweep 1's record: the two
zero-message roots (`dermatitis`, `veteran-self-harm`) -- name them in
every witness table; IDENTICAL there is an empty population.

Step 1. RED then GREEN, dark (`:chatter`/`:charges` in no existing
config). Unit configs WITH each key, `pos?` witnesses: (i) the (a) table
verbatim -- `:demographic-update` -> A08 iff an encounter is open at t
else A31; `:coverage-change` -> same rule, IN1-only payload, PID
unchanged; `:registered` -> A28; periodic re-statement at
`{:restatement {:rate-per-patient-day r}}` by the same A08/A31 rule --
counted SEPARATELY (the program's A08 volume is the periodic half; an
event-driven-only reading of the witness is the miss the ADR names);
(ii) determinism -- chatter draws ride the `:emission` stream under the
fixed-consumption law; two configs differing in one chatter rule
produce identical draws for everything else (assert it); MSH-10 unique
across the whole stream (the control-id ordinal, tested at a t
collision); (iii) `emit-wire` interleaves chatter at its `:at` with the
latency plan unchanged for non-chatter messages (byte-equal, assert
it); (iv) DFT^P03 per encounter close, lines from the log's procedures/
orders/bed-days priced by a `:charges {:price-table ...}` config table
-- the table is emission config, its absence for a code is a counted
skip, never a ground-truth read-back; FT1 fields from the v2.4
structures jar; (v) the (h) gate: `gate v2` full on skeleton-kind
messages, MSH-10-sorted-prefix sample per MSH-9 stratum on add-on
messages, per-stratum `n`/gated printed, and the born-red determinism
gate (same corpus twice -> same sample); (vi) `classify-change`: no
contract change is expected -- if one is owed, STOP (a chatter sweep
that touches the event schema has crossed R-skeleton-or-emission).
PROOF: `bin/ground-truth-bracket 1a71b36 HEAD` IDENTICAL 36;
`bin/regression-oracle <step-0 sha> HEAD` IDENTICAL, no declaration
(no root opts in). Commit dark, one per design if that keeps causes
single.

Step 2. ON. Opt in the six corpora (+ `config-latency.edn`) --
`:chatter` everywhere, `:charges` where encounters close (say where
that isn't); ONE new oracle root `chatter-charges` (39 untouched)
carrying both keys, its coverage note naming what it newly witnesses
(A08 A31 A28 DFT; IN1-only updates). Predicted movers: message digests
only, opted-in roots only -- run BOTH brackets: ground truth IDENTICAL
36; oracle declared, the mover set = exactly the opted-in engine-layer
roots (name the non-movers incl. the two zero-message roots), one `+`.
Re-pin the message-side list (baselines, traces, README wire excerpts
-- the tripwire BEFORE the push). Witness table per corpus, `pos?`
where the population exists: A08 event-driven, A08 periodic
(SEPARATELY), A31, A28, IN1-only, DFT messages, charge lines, sampled
strata with n/gated. `make test` + `make integration`. Commit ON.

Step 3. Push; CI; no tag. Record one page: bracket lines per commit,
the witness table, the sampling table, draw-consumption proof, ADR
premises contradicted. Roadmap: sweep 2 of 6; ladders next need an
order-placing oracle root -- row it as sweep 3's step 0.

Fences. No ground-truth change of any kind (bracket-enforced). No new
event kind, no schema bump (STOP if `classify-change` disagrees). No
SIU, no NK1, no fan-out, no MLLP. Price table never read by the
engine. One declared message sweep; the tripwire handled pre-push.
