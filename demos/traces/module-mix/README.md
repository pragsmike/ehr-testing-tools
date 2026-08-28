# Demo: the full vendored GMF module set, weighted, with churn

> **Generated, byte-exact.** The captured artifacts in this directory --
> ground-truth.edn, messages.txt -- are written by `bin/regen-traces`, run
> via `make traces` (a `make docsgen` leaf). They are byte-for-byte
> capture of the command below, not prose about it, so hand-editing one
> makes it a fiction: change the command or the engine and regenerate.
> CI freshness-diffs `demos/traces/` whole. This README itself is
> hand-owned. (ADR-0158, review-4 register row L3-7.)
> The `config*.edn` beside them is the opposite: a hand-authored INPUT
> `bin/regen-traces` reads with `--config`, never writes.

M7's own module-curation session vendored a second real Synthea module
(`appendicitis.json`, alongside the existing `sinusitis.json`) —
`components/patient-simulator/docs/gmf-interpreter.md`'s M7 section has the full survey. This demo
is the population-scale proof: both vendored modules assigned across a
mixed patient population by weight, churn active, rendered to real
ER7 wire bytes.

## Command

```bash
bin/ehrt sim run --seed 71 --patients 100 --config demos/traces/module-mix/config.edn --churn --emit hl7 --format er7
```

[`config.edn`](config.edn) is this session's own documented default
`:modules`/`:module-assignment` — see its own header comment for why
`:pathway` is an explicit empty pathway (module-only patients, the
documented shape `ehrt.sim.run`'s own `:modules` docstring
names) and why `:module-horizon-days` is 3650 (ten years, not the
engine's own 90-day default): `appendicitis.json`'s real onset is
gated behind an age-bracket `Delay` that can run for decades, and a
90-day horizon gives it almost no room to land inside the window
before being dropped as a pre-horizon fact.

## What this demo does NOT show, honestly

`components/patient-simulator/docs/gmf-interpreter.md`'s M7 survey found no vendorable
`Observation`-bearing module this session (every real candidate —
`sore_throat.json`, `sepsis.json`, `osteoporosis.json`,
`hypothyroidism.json`, and others — is blocked by a mandatory-path
condition-vocabulary gap, a `wellness: true` schema-encoding gap, or a
`CallSubmodule`/`Death` load-time gate). **This demo cannot show a real
module-sourced OBX** — that goal is recorded here as genuinely unmet,
not quietly dropped, per this session's own seam checkpoint. Neither
vendored module carries an `Observation` state.

This demo also cannot show `appendicitis.json`'s own inpatient/surgical
half — `components/patient-simulator/docs/gmf-interpreter.md`'s M7 section documents why
(`compile-trajectory`'s multi-encounter-per-episode truncation): the
excerpt below shows the real emergency admission and the discharge
that follows it, and stops there, exactly as the real engine output
does. This is the concrete demo-level evidence for that finding, not
merely a description of it.

## What to look for

- [`ground-truth.edn`](ground-truth.edn), line 9701: patient
  `PID-000021-4740fba3` (MRN000022) is admitted to the Emergency ward
  (`ED-H05`, a surge slot) at `:t 251210100` — the `:citation
  {:module "appendicitis", :state :appendicitis-encounter}` on the
  event itself is the glass-box trace back to the exact vendored
  module state that produced it. The admission's own `:conditions`
  vector carries a `History of appendectomy` annotation, its own
  citation naming `appendicitis`/`history-of-appendectomy`. The very
  next event, same `:t`, is a `:discharge` citing
  `appendicitis`/`transfer-to-inpatient` — the module's own state name
  for what SHOULD have been a transfer into the inpatient surgical
  encounter, and instead is where this patient's compiled trajectory
  stops (the truncation finding, above).
- [`ground-truth.edn`](ground-truth.edn), at `:t 5809740`: patient
  `PID-000056-e2258744` (MRN000057)'s `:outpatient-visit`/
  `:outpatient-visit-end` pair citing `sinusitis`/`doctor-visit` and
  `sinusitis`/`end-encounter` — one of 68 patients (of 100) whose own
  event stream carries at least one `sinusitis`-cited fact this run.
- **Mix summary** (patients whose own event stream carries at least
  one fact citing each module — computed from the ground-truth log's
  own citations, since module ASSIGNMENT itself isn't separately
  stamped onto the `:registered` event; see the note below):
  **68 patients manifest `sinusitis` content, 1 manifests `appendicitis`
  content, 31 manifest neither** (of 100 registered; re-counted from the
  regenerated log after ADR-0171's stream partition reshuffled which
  ordinal draws which module and which walk — it was 87/3/10 before).
  The 90/10 weight
  (`config.edn`) is the ASSIGNMENT ratio; the manifested ratio is lower
  for `appendicitis` because — realistically — only a fraction of
  patients ever assigned the module actually onset appendicitis within
  their own lifetime and this run's horizon window (`appendicitis.json`'s
  own real lifetime incidence, ~7–8%, `components/patient-simulator/docs/gmf-interpreter.md`'s
  appendix). The 31 "neither" patients are registrations too young, or
  otherwise unlucky, for their assigned module to have produced a
  fact-bearing event yet within this run's own window — a real,
  honest artifact of how module content actually manifests, not a bug.

## Excerpt: a real vendored-module emergency admission, glass-box cited

```
MSH|^~\&|EHR-TESTING-SIM|SIM|||20311217123500+0000||ADT^A01|MRN000022-A01-251210100|P|2.3
EVN|A01|20311217123500+0000
PID|1||MRN000022||Davis^Michael||20051216|M|||35 Aspen Way^^Albuquerque^NM^87102||(967)291-8344
PV1|1|I|Emergency^^ED-H05^general-hospital||||7919726852^Reyes^Priya|||||||||||||||||||||||||||||
IN1|1||commercial-hmo|Commercial HMO

MSH|^~\&|EHR-TESTING-SIM|SIM|||20311217123500+0000||ADT^A03|MRN000022-A03-251210100|P|2.3
EVN|A03|20311217123500+0000
PID|1||MRN000022||Davis^Michael||20051216|M|||35 Aspen Way^^Albuquerque^NM^87102||(967)291-8344
PV1|1|I|Emergency^^ED-H05^general-hospital||||7919726852^Reyes^Priya|||||||||||||||||||||||||||||01
```

(Segments are shown one per line here for readability; the real wire
format in [`messages.txt`](messages.txt) uses `\r`, HL7v2's actual
segment delimiter — found by grepping `MRN000022`.)

The A01 is the real emergency admission `appendicitis.json`'s own
`Appendicitis_Encounter` state produced; the A03 immediately following
(same `:t`, `PV1-3` unchanged — the ward never actually changes wire-
side) is what `compile-trajectory` renders for the module's own
`Transfer_To_Inpatient` state, since this project has no `:transfer`
wire content wired to a same-episode encounter-class change yet (the
gap documented above). No FHIR/state excerpt is included here — this
demo did not additionally run `--emit fhir`; `demos/traces/emit-state/`
already demonstrates that surface.

## Determinism

Re-run twice (`--format ground-truth`, piped to a byte comparison):
identical output both times, same seed and config, per this project's
own determinism law (ADR-0002/ADR-0008).
