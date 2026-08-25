# Demo: ED hallway boarding, then a bed-ready transfer (emergent, not scripted)

> **Generated, byte-exact.** The captured artifacts in this directory --
> ground-truth.edn, messages.txt -- are written by `bin/regen-traces`, run
> via `make traces` (a `make docsgen` leaf). They are byte-for-byte
> capture of the command below, not prose about it, so hand-editing one
> makes it a fiction: change the command or the engine and regenerate.
> CI freshness-diffs `demos/traces/` whole. This README itself is
> hand-owned. (ADR-0158, review-4 register row L3-7.)

The top-level README's own headline claim — boarding and bed-ready
transfers *emerge* from census pressure against configured capacity,
never scripted — captured as one real, reproducible trace. The
default facility (`ehrt.sim.config/default-facility`) gives
Renal and Cardiology 4 beds + 2 surge slots each; a high enough
patient count against a tight enough arrival gap fills every rung of
the allocation ladder (home-ward bed, home-ward surge, other-ward
outlier bed) before the engine ever boards anyone in the Emergency
ward's own surge slots — this run does exactly that, then later
relocates a boarder the moment a real bed frees.

## Command

```bash
bin/ehrt sim run --seed 1 --patients 25 --arrival-gap 22 --emit hl7 --format er7
```

(`--arrival-gap` was **20** until ADR-0171's stream partition, which
reshuffled every draw in this run and pushed it over the cliff: at gap
20 the same seed now exits `:capacity-exhausted` on Renal, the fifth
rung this ladder deliberately does not have. Widening the gap by two
minutes is the smallest change that keeps the demo's own subject
intact, and it holds the demo's own subject matter almost exactly
level: 14 bed-ready transfers and 9 Emergency-surge boardings in 89
events, against the pre-partition capture's 15 and 9 in 90.)

(`--format er7`, go-public session Task 1, is what produced
[`messages.txt`](messages.txt) directly — bare wire bytes, nothing
else.)

## What to look for

- [`ground-truth.edn`](ground-truth.edn): patient `PID-000011-2380309d`
  (MRN000012) is admitted at `:t 6960` directly into an Emergency
  surge slot (`{:ward "Emergency", :bed "ED-H06", :placement :surge}`)
  — boarding from the very first event, home ward Renal. At `:t 11820`,
  patient `PID-000002-1c9756ce` (MRN000003) discharges from
  `RENAL-03` in the SAME tick that MRN000012's own `:transfer` event
  fires, moving them from `ED-H06` into the just-freed `RENAL-03` —
  `:bed-ready true` on that transfer event names exactly this
  causation. The coupling is not a one-off artifact of this seed: the
  log carries **fourteen** `:bed-ready true` transfers, the next one at
  `:t 13980` (MRN000013, boarding in `ED-H03`, pulled into `RENAL-02`
  by MRN000006's discharge).
- [`messages.txt`](messages.txt): the three messages this produces,
  found by grepping their own control ids (`MRN000012-A01`,
  `MRN000003-A03`, `MRN000012-A02`) — see the excerpt below.

## Excerpt: admission (boarding), the other patient's discharge, and the triggered transfer

```
MSH|^~\&|EHR-TESTING-SIM|SIM|||20240101015600+0000||ADT^A01|MRN000012-A01-6960|P|2.3
PID|1||MRN000012||Davis^Mason||20140106|M|||312 Cedar Ln^^Nashville^TN^37203||618-233-1406
PV1|1|I|Emergency^^ED-H06^general-hospital||||6283041245^Reyes^Priya

MSH|^~\&|EHR-TESTING-SIM|SIM|||20240101031700+0000||ADT^A03|MRN000003-A03-11820|P|2.3
PID|1||MRN000003||Garcia^Michael||19731220|M|||312 Cedar Ln^^Nashville^TN^37203||340-265-9096
PV1|1|I|Renal^^RENAL-03^general-hospital||||2403984257^Chen^Amara|||||||||||||||||||||||||||||01

MSH|^~\&|EHR-TESTING-SIM|SIM|||20240101031700+0000||ADT^A02|MRN000012-A02-11820|P|2.3
PID|1||MRN000012||Davis^Mason||20140106|M|||312 Cedar Ln^^Nashville^TN^37203||618-233-1406
PV1|1|I|Renal^^RENAL-03^general-hospital|||Emergency^^ED-H06^general-hospital|6283041245^Reyes^Priya
```

(Segments are shown one per line here for readability; the real wire
format in `messages.txt` uses `\r`, HL7v2's actual segment delimiter.)

PV1-3 on the admission (`Emergency^^ED-H06^general-hospital`) is the
boarding placement itself — administratively assigned to Renal
(`:home-ward "Renal"` in the ground truth) but physically in an ED
surge slot. The transfer's PV1-3 (`Renal^^RENAL-03^general-hospital`)
and PV1-6 (`Emergency^^ED-H06^general-hospital`, the prior location) —
firing at the exact same `:t` as the other patient's own discharge —
are the wire-level trace of the causation `ground-truth.edn`'s
`:bed-ready true` names directly.
