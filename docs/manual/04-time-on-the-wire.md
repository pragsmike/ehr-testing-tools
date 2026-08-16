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
bin/ehrt play out/scenarios/ed-tuesday-latency --board 60 --rate 100000
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
That section's own field audit found exactly two timestamp-bearing
fields this workspace's emitter renders: **MSH-7**, the message's own
transmit time, shiftable by a sampled delay; and **EVN-2**, an ADT
message's own clinical event time, never shifted. One message,
two clocks — the figure below draws exactly one of them, field values
and all.

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
b4e776f773502cf78795a83bb52836ea208c831935330cb0480a731525e637f1  out/scenarios/ed-tuesday-base/events.edn
b4e776f773502cf78795a83bb52836ea208c831935330cb0480a731525e637f1  out/scenarios/ed-tuesday-latency/events.edn
```

`diff` prints nothing; the digests match. The same 383 ground-truth
events, either way — only `msg-%03d.hl7`'s own MSH-7 values, and the
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
bin/ehrt play out/scenarios/ed-tuesday-latency --board 60 --rate 100000
```

## What one message's own two clocks look like

Walker, William (MRN000013), [pathway](../glossary.md) `ed-fast-track`: admitted, EVN-2
clinical time `2026-08-11T03:36:00Z`; discharged 37 minutes later,
`04:13:00Z` — ordinary, unremarkable, log-order-correct clinical
history, per the demo README's own narration. On the wire, the two
messages' own sampled transmit delays don't preserve that order: the
discharge's own delay (20m54s) is shorter than the admission's own
(1h00m46s), so the discharge (A03) transmits first, MSH-7
`04:33:54Z`, and the admission (A01) transmits second, MSH-7
`04:36:46Z` — reordered on the wire, never in ground truth. This is
the figure below: one message (Walker's own admission, A01) with its
EVN-2 (clinical) and MSH-7 (transmit) fields both shown, an hour apart.

<img src="assets/two-clocks.svg" alt="One HL7v2 message carrying two clocks: EVN-2 clinical time and MSH-7 transmit time, offset by a sampled latency delay" width="640" />

A board that folds strictly in arrival order has no way to know the
admission it just received is already stale — it puts Walker right
back on the board, in the same bed the board already shows occupied
by someone else. Chapter 1 already showed you that exact board
snapshot; this chapter's own job was the mechanism producing it, not
the symptom itself. `ed-tuesday`'s own README states plainly what
this workspace does and doesn't do about it: "a receiver that buffered
incoming messages briefly and reconciled by clinical time (EVN-2, when
present) rather than folding strictly in arrival order would not have
produced Walker's own phantom re-admission" — a receiver's own design
question, not this workspace's to answer. Supplying the case is the
job; Chapter 5 supplies a second, complementary one.

**Strip source citations, per strip:**

| strip | source |
|---|---|
| `bin/ehrt play out/scenarios/ed-tuesday-latency --board 60 --rate 100000` | `demos/scenarios/ed-tuesday/README.md`, "The second clock" |
| `bin/ehrt corpus generate sim ...` (base + latency, two commands) | `demos/scenarios/ed-tuesday/README.md`, "The second clock" |
| `diff`/`sha256sum` ground-truth-invariance transcript | `demos/scenarios/ed-tuesday/README.md`, "The second clock" |
| Walker EVN-2/MSH-7 values (`03:36:00Z`, `04:13:00Z`, `04:33:54Z`, `04:36:46Z`) | `demos/scenarios/ed-tuesday/README.md`, "What the board actually shows" |
