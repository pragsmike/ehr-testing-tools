# Demo: ED hallway boarding, then a bed-ready transfer (emergent, not scripted)

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
clojure -M:cli run --seed 1 --patients 25 --arrival-gap 20 --emit hl7 --format er7
```

(`--format er7`, go-public session Task 1, is what produced
[`messages.txt`](messages.txt) directly — bare wire bytes, nothing
else.)

## What to look for

- [`ground-truth.edn`](ground-truth.edn): patient `PID-000010-8a582fca`
  (MRN000011) is admitted at `:t 5760` directly into an Emergency
  surge slot (`{:ward "Emergency", :bed "ED-H04", :placement :surge}`)
  — boarding from the very first event, home ward Renal. At `:t 11220`,
  patient `PID-000008-ef953636` (MRN000009) discharges from
  `RENAL-04` in the SAME tick that MRN000011's own `:transfer` event
  fires, moving them from `ED-H04` into the just-freed `RENAL-04` —
  `:bed-ready true` on that transfer event names exactly this
  causation. The same coupling repeats later in the log (`:t 19200`,
  a different boarder reusing `ED-H04`), so this is not a one-off
  artifact of this particular seed.
- [`messages.txt`](messages.txt): the three messages this produces,
  found by grepping their own control ids (`MRN000011-A01`,
  `MRN000009-A03`, `MRN000011-A02`) — see the excerpt below.

## Excerpt: admission (boarding), the other patient's discharge, and the triggered transfer

```
MSH|^~\&|EHR-TESTING-SIM|SIM|||20240101013600+0000||ADT^A01|MRN000011-A01-5760|P|2.3
PID|1||MRN000011||Rodriguez^Michael||19740420|M|||605 Lakeview Way^^Minneapolis^MN^55401||220-451-5853
PV1|1|I|Emergency^^ED-H04^general-hospital||||6962094986^Reyes^Priya

MSH|^~\&|EHR-TESTING-SIM|SIM|||20240101030700+0000||ADT^A03|MRN000009-A03-11220|P|2.3
PID|1||MRN000009||Kim^Joshua||19921021|M|||482 Ridgeway Ln^^Springfield^IL^62704||666-875-6750
PV1|1|I|Renal^^RENAL-04^general-hospital||||6962094986^Reyes^Priya|||||||||||||||||||||||||||||01

MSH|^~\&|EHR-TESTING-SIM|SIM|||20240101030700+0000||ADT^A02|MRN000011-A02-11220|P|2.3
PID|1||MRN000011||Rodriguez^Michael||19740420|M|||605 Lakeview Way^^Minneapolis^MN^55401||220-451-5853
PV1|1|I|Renal^^RENAL-04^general-hospital|||Emergency^^ED-H04^general-hospital|6962094986^Reyes^Priya
```

(Segments are shown one per line here for readability; the real wire
format in `messages.txt` uses `\r`, HL7v2's actual segment delimiter.)

PV1-3 on the admission (`Emergency^^ED-H04^general-hospital`) is the
boarding placement itself — administratively assigned to Renal
(`:home-ward "Renal"` in the ground truth) but physically in an ED
surge slot. The transfer's PV1-3 (`Renal^^RENAL-04^general-hospital`)
and PV1-6 (`Emergency^^ED-H04^general-hospital`, the prior location) —
firing at the exact same `:t` as the other patient's own discharge —
are the wire-level trace of the causation `ground-truth.edn`'s
`:bed-ready true` names directly.
