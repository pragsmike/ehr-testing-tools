# Session prompt — arc 4 sweep 5: fan-out and the MLLP sink

Archived verbatim. ADR-0175 designs (f)+(g), ruling B1; author ruling
2026-08-28: collision option (b). Paired record:
[`../session-records/2026-08-28-arc-4-sweep-5-fan-out-mllp.md`](../session-records/2026-08-28-arc-4-sweep-5-fan-out-mllp.md).

---

Session prompt -- arc 4 sweep 5: fan-out and the MLLP sink (ADR-0175 designs (f)+(g), ruling B1; author ruling 2026-08-28: collision option (b))

Context. HEAD ba82f9f. Ruling (b): fan-out derives per-subscriber
identity from LOG INDICES, never MSH-10; `[oru-control-id-collision]`
stays open, cosmetic, priced. Read ADR-0175 (f) and (g) whole -- they
are unusually concrete: `:fan-out [{:name :filter {:message-types ...
:patient-classes ...} :msh {...}}]` riding `:config`; one `dir:` spool
per subscriber under the corpus root; `effective-msh` fills MSH-3/4/5/6
per subscriber; filters name concrete `TYPE^TRIGGER`s (state the
allow-list law for unknown ones); the SUBSEQUENCE LAW; `:mllp` sink kind
IMPLIES `:framing :mllp` (any other framing on it is a config error);
ACK pairing per the jar's `ACK [MSH MSA ERR]`; `--sink mllp://host:port`
extends the existing designator, no new flag. Also read sweep 3's
record :285-305 (the 7 live duplicate MSH-10s) and `corpus-io`
`source_sink.clj:62/:86` (`known-sink-kinds` vs `implemented-sink-kinds`)
and `framing.clj:164-200` (the codec the sink must reuse, not rewrite).

Expected proof shape -- DIFFERENT from sweeps 1-4, say so up front:
fan-out and MLLP change NO base message byte and NO ground truth, so
BOTH brackets should read IDENTICAL on the dark commit AND the on
commit. The evidence layer is the spool/exerciser tier: subscriber-spool
digests, the subsequence law, ACK pairing. If either bracket DIFFERS at
any commit, a base byte moved -- STOP.

Step 0, ride-along, own commit (the author action sweep 4 queued): the
four scheduling kinds' `:doc` strings drop "deliberately unrendered in
1.7.0" for the true sentence; `make event-schema-export
formats-event-log`; `classify-change` printed (expect zero -- if a bump
is owed, STOP, that is not a `:doc` edit). Full suite; commit.

Step 1. Fan-out, RED then GREEN, dark then on (its opt-in is the
`:fan-out` key itself; no existing config carries one until step 3):
(i) `spool-sim-output!` (or the seam the tree names) writes each
subscriber's spool by FILTERING the base message vector by log-index
order -- the identity of a message is its position in the emitted
vector, (b)'s ruling made structural; (ii) the SUBSEQUENCE LAW as a
property over generated subscriber tables: with no `:msh` override, a
subscriber's spool is a byte-exact subsequence of the base spool; with
one, byte-exact after masking exactly the overridden MSH fields --
state the mask, and RED-prove the law by a transform that reorders;
(iii) unknown `TYPE^TRIGGER` in a filter is a config REJECT before the
engine runs (the `:invalid-siu` precedent); (iv) per-subscriber `INDEX`
/ digest files so the exerciser can assert them; (v) the accumulator
row: correct `roadmap.md#corpus-player-slices` to what (f) measured as
already built, one line.

Step 2. MLLP, RED then GREEN: `:mllp` joins `implemented-sink-kinds`
with the implication rule and its config-error test; the sink reuses
`framing.clj`'s codec (a no-drift gate naming the fns, like
`oracle-lib`'s); a loopback in-test server proves: framing round-trip
on a real corpus, ACK pairing POSITIONAL (socket order) with MSA-2
equality asserted per pair, MSA-1 codes handled (AA continue; AE/AR
behaviour stated and tested), a missing ACK times out visibly; the 7
known duplicate MSH-10s: run the pairing over seed-424242's spool and
assert the positional law survives them (MSA-2 equality is per-pair,
not a global bijection -- the collision row's reason quoted in the
test). `ehrt play --sink mllp://...` through the designator, its
round-trip parse/print law extended, help text updated.

Step 3. Opt-in: `:fan-out` with two subscribers (an ADT feed and a
lab feed, per (f)'s example) in `ed-tuesday` + `clinic-decade` configs
(+ `config-latency.edn`); exerciser assertions: subscriber spool
digests, subsequence spot-checks, an MLLP round-trip leg in the
ed-tuesday exerciser (loopback). Both brackets at every commit:
IDENTICAL, both halves, all 41 roots -- the sweep's whole claim.
`make test` + `make integration`; push; CI; no tag. Record one page:
bracket lines (all IDENTICAL), the subsequence/mask law verbatim, the
ACK pairing law and the duplicate test's outcome, spool digests, ADR
premises contradicted. Roadmap: sweep 5 of 6; the spike rerun next.

Fences. No base message byte, no ground truth, no schema diff, no new
message family (an ACK is RECEIVED, not emitted by the generator --
the player's test server emits it; if any seam requires the generator
to emit one, STOP). No MSH-10-keyed identity anywhere in fan-out. No
new flag. Codec never duplicated. One sweep.
