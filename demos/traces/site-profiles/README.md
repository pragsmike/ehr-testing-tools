# Demo: two site profiles, one seed — the invariance property (Milestone site-profiles)

> **Generated, byte-exact.** The captured artifacts in this directory --
> ground-truth.edn, messages-aldric.txt, messages-default.txt -- are written by `bin/regen-traces`, run
> via `make traces` (a `make docsgen` leaf). They are byte-for-byte
> capture of the command below, not prose about it, so hand-editing one
> makes it a fiction: change the command or the engine and regenerate.
> CI freshness-diffs `demos/traces/` whole. This README itself is
> hand-owned. (ADR-0158, review-4 register row L3-7.)
> The `config*.edn` beside them is the opposite: a hand-authored INPUT
> `bin/regen-traces` reads with `--config`, never writes.

`docs/site-profiles.md`'s own thesis, made concrete: the SAME seed and
patient count, rendered under two different site profiles, produce
byte-identical ground truth and messages that differ only on the
declared dialect surfaces (MSH-3/4/5/6/12, PV1-2/PV1-36, and the
presence of a Z-segment).

## Commands

```bash
bin/ehrt sim run --seed 42 --patients 2 --emit hl7
bin/ehrt sim run --seed 42 --patients 2 --emit hl7 --config demos/traces/site-profiles/config-aldric.edn
```

The first run uses no site profile at all (the absent/default profile);
the second uses [`config-aldric.edn`](config-aldric.edn) — `:site-profile`
has no CLI flag of its own, so it rides `run-command`'s `--config`
passthrough (`ehrt.sim.run/run-command`'s docstring), the same
vehicle `:pathway`/`:order-profiles` use.

## The gaudy profile

`config-aldric.edn` is deliberately different-looking on every dialect
surface this milestone defines: HL7 version 2.5.1 (not 2.3), a renamed
sending facility (`ALDRIC-EHR`/`ALDRIC`, not `EHR-TESTING-SIM`/`SIM`)
plus receiving app/facility (blank by default), custom patient-class
and discharge-disposition codes (`IN^99ALDRIC`, `HOME^99ALDRIC`), and a
`ZPI` payer Z-segment bound to the patient's own sampled persona/payer
state.

## What to look for

- [`ground-truth.edn`](ground-truth.edn): the SAME log for both runs —
  verified programmatically when this demo was generated (`(= default-
  ground-truth aldric-ground-truth)` => `true`), and it is ONE file
  here, not two, precisely because there's only one ground truth to
  show.
- [`messages-default.txt`](messages-default.txt) /
  [`messages-aldric.txt`](messages-aldric.txt): the same four messages
  (two admissions, two discharges), rendered twice.

## Excerpt: patient 1's admission (`MRN000001`), both profiles

Default profile (`messages-default.txt`):

```
MSH|^~\&|EHR-TESTING-SIM|SIM|||20240101000000+0000||ADT^A01|MRN000001-A01-0|P|2.3
EVN|A01|20240101000000+0000
PID|1||MRN000001||D'Angelo^Joshua||19810203|M|||78 Magnolia St^^Atlanta^GA^30303||735-633-0549
PV1|1|I|Renal^^RENAL-02^general-hospital||||4255631598^Chen^Amara
IN1|1||commercial-ppo|Commercial PPO
```

St. Aldric's Memorial profile (`messages-aldric.txt`):

```
MSH|^~\&|ALDRIC-EHR|ALDRIC|DOWNSTREAM|DOWNSTREAM-FAC|20240101000000+0000||ADT^A01|MRN000001-A01-0|P|2.5.1
EVN|A01|20240101000000+0000
PID|1||MRN000001||D'Angelo^Joshua||19810203|M|||78 Magnolia St^^Atlanta^GA^30303||735-633-0549
PV1|1|IN^99ALDRIC|Renal^^RENAL-02^general-hospital||||4255631598^Chen^Amara
IN1|1||commercial-ppo|Commercial PPO
ZPI|commercial-ppo|commercial|ALDRIC-PAYER-V1
```

(Segments shown one per line for readability, trailing empty PV1 fields
elided; the real wire format uses `\r`, HL7v2's actual segment
delimiter, and PV1 carries its full 36 fields — see `mask-pv1-fields`,
`test/ehrt/sim/emit_hl7_test.clj`, for exactly which two are
masked when comparing.)

**MSH-3/4/5/6/12** (sending/receiving app+facility, version id) are the
only MSH fields that differ. **PV1-2** (patient class) differs
(`I` → `IN^99ALDRIC`) — the same underlying `:inpatient` state value,
rendered through St. Aldric's own code-table override
(`ehrt.sim-emit-hl7.site-profile/code-for`). **ZPI** is new: a Z-segment
this profile alone declares, bound to `[:persona :payer :id]` and
`[:persona :payer :type]` (both resolve against the SAME `:registered`
event's persona that produced the IN1 segment above — `commercial-ppo`
appears in both), plus a literal fallback field. Patient 1's own
discharge (later in each `messages-*.txt`) shows the same pattern on
**PV1-36** (discharge disposition): `01` (today's standard default)
vs. `HOME^99ALDRIC`.

Everything else — PID's demographic fields, the ward/bed/attending in
PV1-3/6/7, IN1's payer id/name, the timestamps themselves — is
byte-identical across both renderings, because none of it is a
declared dialect surface.
