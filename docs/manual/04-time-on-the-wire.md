# Chapter 4 — Time on the wire

Chapter 1 already showed you the symptom: on `ed-tuesday`'s own latency
wire, a patient's admission message can arrive on the board *after*
his discharge message, because two independently sampled transmit
delays landed in the "wrong" order. This chapter is the mechanism
underneath that symptom — the two clocks a message actually carries,
and how to pace one yourself.

## Pacing: `ehrt play`, and the board as a downstream stand-in

[`docs/cli.md`](../cli.md#ehrt-play) names `ehrt play` plainly: "`ehrt
show` plus time." A corpus's own messages are paced against their own
timestamps and rendered as they'd actually arrive — `--rate` sets
stream-seconds per wallclock-second, and `--board` swaps the
message-by-message ticker for a bed-state snapshot, rendered every N
stream-minutes. There is no real downstream receiver inside this
workspace to hand a paced stream to; `--board` stands in for one —
exactly the role Chapter 3 already used it in, folding whatever
arrives, in the order it arrives, the way any receiver's own intake
would. `ed-tuesday`'s own README plays its latency wire this way:

(This plays the latency wire that the two `corpus generate sim`
invocations below create; run those first, or run
`bin/demo-exerciser-ed-tuesday` once.)

```bash
bin/ehrt play out/scenarios/ed-tuesday-latency --board 60 --rate 10000000
```

[`docs/use-cases/play-a-generated-corpus-back-over-time.md`](../use-cases/play-a-generated-corpus-back-over-time.md)
is the reference walkthrough for `ehrt play` on its own, real time
versus a chosen rate, `--sink` versus a rendered ticker — linked, not
restated here.

**A reader-side identity anchor: huge rate *is* `show`.** "Paced" and
"instant" sound like they should differ, but they're related by a
proven identity, not merely an intuition: play a corpus at an
arbitrarily large `--rate` (or write it to a file sink) and you get
back exactly the bytes `ehrt show` (or the unpaced file) would have
given you directly —
`play-command-at-huge-rate-matches-show-identity-test` and
`play-command-file-sink-writes-byte-identical-to-unpaced-content-test`
(`bases/cli/test/ehrt/cli/core_test.clj`) both prove it. Pacing
changes *when* you see a message, never *what* it says. Nothing here
asks you to take that on faith — the property is a passing test you
can point to, not a claim about the code's intentions.

## The second clock

Real transport doesn't deliver instantly. `ed-tuesday`'s own [second
clock](../../demos/scenarios/ed-tuesday/README.md#the-second-clock)
section plays the *same* [ground truth](../glossary.md) onto two wires: one instant, one
where each message's own transmission lags its own clinical event by a
sampled delay — a real EHR's own downstream feed, modeled rather than
idealized. The mechanism is
[`docs/dev/simulator-architecture.md`](../dev/simulator-architecture.md#5-extension-point-downstream-latency-realism)
section 5's own extension point, built: a second, independently seeded
RNG samples a per-event-type transmit delay at the [emitter](../glossary.md) seam
(`GT × LatencyParams → TimedWire`) — `engine`'s own ground truth is
never touched, never re-entered, never aware latency exists at all.
That section's own field audit found, in 2026-08-11, exactly two
timestamp-bearing fields this workspace's emitter rendered: **MSH-7**,
the message's own transmit time, shiftable by a sampled delay; and
**EVN-2**, an ADT message's own clinical event time, never shifted.
One message, two clocks — the figure below draws exactly one of them,
field values and all.

**Dated update, 2026-08-16: there are now four, and the two new ones
are on results.** That audit also recorded which fields HL7v2 *would*
put clinical time in if this emitter ever rendered them — and named
`OBR-7` and `OBX-14` among them. Until this update it didn't, so a
result message carried MSH-7 alone and a receiver handed a late result
had nothing to back-date it with. Both now render, on all three of the
shapes this workspace emits as `ORU^R01`, carrying the result event's
own clinical instant and never shifting under latency. "One message,
two clocks" is no longer an ADT-only story; ["When the result is
late"](#when-the-result-is-late) below is the same mechanism on the
result wire. (Order messages are deliberately unchanged — see that
section's closing note.)

**Generate both wires, same seed, separate out-dirs** — copied
verbatim from the demo README:

```bash
bin/ehrt corpus generate sim --seed 20260811 --patients 100 \
  --reference-date 2026-08-11 --churn \
  --config demos/scenarios/ed-tuesday/config.edn \
  --out-dir out/scenarios/ed-tuesday-base

bin/ehrt corpus generate sim --seed 20260811 --patients 100 \
  --reference-date 2026-08-11 --churn \
  --config demos/scenarios/ed-tuesday/config-latency.edn \
  --out-dir out/scenarios/ed-tuesday-latency
```

**A second reader-side identity anchor: ground truth is invariant
under latency.** `:latency` rides `:config` as an emit-only
passthrough — it never reaches `engine/config-keys`, so the *facts*
(who was admitted, when, to which bed) can't change depending on
whether a downstream wire happens to be laggy. Only the *rendering*
differs. Witnessed directly, not merely asserted:

```
$ diff out/scenarios/ed-tuesday-base/events.edn out/scenarios/ed-tuesday-latency/events.edn
$ sha256sum out/scenarios/ed-tuesday-base/events.edn out/scenarios/ed-tuesday-latency/events.edn
fe13a7ba59939e548be8d98589b005ff7c14e33ef8e82d4d54d47ad388bbb8d8  out/scenarios/ed-tuesday-base/events.edn
fe13a7ba59939e548be8d98589b005ff7c14e33ef8e82d4d54d47ad388bbb8d8  out/scenarios/ed-tuesday-latency/events.edn
```

`diff` prints nothing; the digests match. The same 1,269 ground-truth
events, either way — only `msg-NNNN.hl7`'s own MSH-7 values, and the
file order `emit-wire` sorts them into (by transmit time, never log
order), differ between the two out-dirs. This is the general case of
a narrower identity `docs/dev/simulator-architecture.md` section 4
proves directly: at the identity element of `LatencyParams` — absent,
`nil`, or empty offsets —
`emit-wire-with-absent-nil-or-empty-offsets-is-byte-identical-to-emit`
(`components/sim-emit-hl7/test/ehrt/sim_emit_hl7/latency_test.clj`)
shows the lowering collapses to plain `emit`'s own output exactly.
**Zero offsets *is* plain emit; huge rate *is* `show`.** Latency and
pacing each add one dimension to how a corpus is delivered, and each
one proves, rather than assumes, that it adds nothing else.

**Play the latency wire into the board:**

```bash
bin/ehrt play out/scenarios/ed-tuesday-latency --board 60 --rate 10000000
```

## What one message's own two clocks look like

Gonzalez, Olivia (MRN000095), bed `ED-H13`: admitted, EVN-2 clinical
time `2026-08-11T23:11:00Z`; discharged 31 minutes later, `23:42:00Z` —
ordinary, unremarkable, log-order-correct clinical history, per the demo
README's own narration. On the wire, her messages' own sampled transmit
delays don't preserve that order: the transfer's delay is 48m29s, the
discharge's 39m08s, and the admission's 1h14m22s, so they transmit
transfer-first (MSH-7 `23:59:29Z`), discharge-second
(`2026-08-12T00:21:08Z`) and admission-*last* (`00:25:22Z`) — reordered
on the wire, never in ground truth. This is the figure below: one
message (her own admission, A01) with its EVN-2 (clinical) and MSH-7
(transmit) fields both shown, an hour and a quarter apart.

<img src="assets/two-clocks.svg" alt="One HL7v2 message carrying two clocks: EVN-2 clinical time and MSH-7 transmit time, offset by a sampled latency delay" width="640" />

A board that folds strictly in arrival order has no way to know the
admission it just received is already stale — it puts Gonzalez right
back on the board, in a bed she vacated an hour and a half of clinical
time earlier. Chapter 1 already showed you that exact board
snapshot; this chapter's own job was the mechanism producing it, not
the symptom itself. `ed-tuesday`'s own README states plainly what
this workspace does and doesn't do about it: "a receiver that buffered
incoming messages briefly and reconciled by clinical time (EVN-2, when
present) rather than folding strictly in arrival order would not have
produced her own phantom re-admission" — a receiver's own design
question, not this workspace's to answer. Supplying the case is the
job; Chapter 5 supplies a second, complementary one.

## When the result is late

The story above is an admission — an ADT message, whose clinical
time rides EVN-2. A *result* has no EVN segment at all (EVN is
ADT-specific, by HL7v2 convention), so until 2026-08-16 a lagged
`ORU^R01` on this wire carried exactly one timestamp and a receiver
could not tell a stale result from a fresh one. It now carries its own
clinical instant twice over: **OBR-7** on the order-context segment,
and **OBX-14** on every observation.

Gonzalez, Joshua (MRN000010) had a CBC panel resulted at
**04:52:00Z**. The `:result-available` band in
[`config-latency.edn`](../../demos/scenarios/ed-tuesday/config-latency.edn)
is 20–120 minutes, and this run's own sample came out at 43m13s, so
the message transmitted at **05:35:13Z**. Both wires, same seed, same
patient, same message — the whole difference is which clock moved:

```
--- out/scenarios/ed-tuesday-base/msg-0072.hl7 (the instant wire)
MSH|^~\&|EHR-TESTING-SIM|SIM|||20260811045200+0000||ORU^R01|MRN000010-R01-17520|P|2.4
OBR|1|||58410-2^CBC panel - Blood by Automated count^LN|||20260811045200+0000||||||||||||||||||F
OBX|1|NM|6690-2^Leukocytes [#/volume] in Blood by Automated count^LN||5.7|K/uL|4.5-11.0|N|||F|||20260811045200+0000

--- out/scenarios/ed-tuesday-latency/msg-0084.hl7 (the latency wire)
MSH|^~\&|EHR-TESTING-SIM|SIM|||20260811053513+0000||ORU^R01|MRN000010-R01-17520|P|2.4
OBR|1|||58410-2^CBC panel - Blood by Automated count^LN|||20260811045200+0000||||||||||||||||||F
OBX|1|NM|6690-2^Leukocytes [#/volume] in Blood by Automated count^LN||5.7|K/uL|4.5-11.0|N|||F|||20260811045200+0000
```

(One line of each message, of five OBX segments; the other four move
the same way, which is to say not at all. The trailing `F`s in OBR-25
and OBX-11 are the status **ladder**'s doing, not latency's — a
terminal result says "final" because its ladder said so.)

`MSH-7` moved by 43m13s. `OBR-7` and `OBX-14` did not move at all —
they are the *same bytes* on both wires. So is everything else: the
values, the flags, the reference ranges, the status codes. Only the
transmit clock, and the message's position in the stream (`msg-0072` on
the instant wire, `msg-0084` on the latency wire, since `emit-wire`
sorts by transmit time) reflect the delay.

That is the whole payoff. A receiver folding this stream in arrival
order sees the result at 05:35:13Z, but the message itself says the
observation happened at 04:52:00Z — enough to reconcile by clinical
time rather than by arrival, which is exactly the defence Gonzalez,
Olivia's phantom re-admission had no equivalent of on the ADT side. This
workspace still does not fold that way itself; supplying the case is
the job.

**The order message is deliberately not symmetric.** The `ORM^O01` that
preceded this result still ends its OBR at OBR-4, with no OBR-7. OBR-7
means *observation* time, and an order's observation has not happened
yet; the field an order would actually owe is ORC-9, transaction time.
Rendering OBR-7 there would have put a plausible-looking timestamp in a
field whose meaning does not fit — so it stays a named revisit rather
than a silent ride-along. Author ruling, 2026-08-16: "Results only; ORM
byte-frozen."

**Strip source citations, per strip:**

| strip | source |
|---|---|
| `bin/ehrt play out/scenarios/ed-tuesday-latency --board 60 --rate 10000000` | `demos/scenarios/ed-tuesday/README.md`, "The second clock" |
| `bin/ehrt corpus generate sim ...` (base + latency, two commands) | `demos/scenarios/ed-tuesday/README.md`, "The second clock" |
| `diff`/`sha256sum` ground-truth-invariance transcript | `demos/scenarios/ed-tuesday/README.md`, "The second clock"; re-witnessed 2026-08-29 against this chapter's own two commands. The digest has MOVED twice, `b4e776f7…` → `d00bf49c…` → `fe13a7ba…`, and the property it witnesses has not: `diff` is still silent and the two out-dirs still agree. The first move was the event contract's 1.0.0 → 1.1.0 rename of a result entry's `:units` to `:unit`[^adr-0150-mv]; the second is the four arc-3 opt-in keys `config.edn` took between 2026-08-26 and 2026-08-27, which is also why the event count on this page went 383 → 1,269 — the run models a twenty-year population now, not one shift |
| Gonzalez EVN-2/MSH-7 values (`23:11:00Z`, `23:42:00Z`, `23:59:29Z`, `00:21:08Z`, `00:25:22Z`) | `demos/scenarios/ed-tuesday/README.md`, "What the board actually shows"; re-witnessed 2026-08-29 by fresh regeneration. This chapter named `Walker, William (MRN000013)` until then, a cast the demo README replaced on 2026-08-28 |
| Gonzalez ORU pair (`msg-0072.hl7` / `msg-0084.hl7`, MSH-7 `04:52:00Z` vs `05:35:13Z`, OBR-7/OBX-14 `04:52:00Z` both) | this chapter's own two `corpus generate sim` commands above, re-run 2026-08-29 at seed 20260811 with `out/` cleared first[^result-clock]. Named `Rodriguez, Jacob (MRN000005)` at `msg-020`/`msg-023` until then — a pair witnessed 2026-08-16, before the stream partition, and superseded by it |

[^result-clock]: `notes/adr/0142-result-clinical-time.md` — the field
    audit, the author rulings behind OBR-7's value and OBX-14's
    positional pad, and the declared oracle change the two fields
    caused (14 of 35 roots moved, exactly as predicted).

[^adr-0150-mv]: `notes/adr/0150-event-log-shape-defects.md` — census
    row S-6, the contract's first non-additive change, and why the
    order-profile `--config` key stayed `:units` while the event key
    became `:unit`.
