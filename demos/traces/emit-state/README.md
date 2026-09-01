# Demo: EmitState — one seed, two renderings, one truth

> **Generated, byte-exact.** The captured artifacts in this directory --
> fhir-bundle-patient1.json, ground-truth.edn, messages.txt -- are written by `bin/regen-traces`, run
> via `make traces` (a `make docsgen` leaf). They are byte-for-byte
> capture of the command below, not prose about it, so hand-editing one
> makes it a fiction: change the command or the engine and regenerate.
> CI freshness-diffs `demos/traces/` whole. This README itself is
> hand-owned. (ADR-0158, review-4 register row L3-7.)

Milestone M6's own demo pair: the SAME `--seed 42 --patients 3` run as
[`../order-result/`](../order-result/) (identical `config.edn`, reused
here — the ground truth these two demos share is byte-identical by
construction, the emitter-coherence law made visible), rendered once as
HL7v2 (`--emit hl7`) and once as FHIR R4 (`--emit fhir`). The pairing is
the point: two renderings of one immutable ground-truth log, with the
SAME identifiers resolving across both.

## Commands

```bash
bin/ehrt sim run --seed 42 --patients 3 \
  --config demos/traces/order-result/config.edn --emit hl7

bin/ehrt sim run --seed 42 --patients 3 \
  --config demos/traces/order-result/config.edn --emit fhir
```

## What to look for

- [`ground-truth.edn`](ground-truth.edn) — the shared truth both
  renderings derive from (identical to `order-result/ground-truth.edn`).
- [`messages.txt`](messages.txt) — the HL7v2 stream (`--emit hl7`), same
  as `order-result/messages.txt`. **Dated note, 2026-08-16 (ADR-0142):
  "same as" is now literally true — both files were regenerated this
  session and are byte-identical at 5,822 bytes. They were NOT before:
  the sibling copy had drifted, having been captured before PV1 gained
  its trailing positional fields, because nothing in the build
  regenerates or freshness-checks `demos/traces/**`. **Errata,
  2026-08-17 (ADR-0149): both halves of that sentence are now false.
  `make traces` regenerates this file and CI diffs it on every push, and
  the shared byte count is 5,823, not 5,822 — the capture ADR-0142 made
  by hand was missing the single trailing newline the CLI's own
  `--format er7` output carries.** This copy was
  current, so its own regeneration shows ADR-0142's change and nothing
  else: 18 changed lines, 3 `OBR-7` and 15 `OBX-14`, on the three ORU
  messages. The three ORM^O01 messages are byte-identical across the
  regeneration — the ORM freeze witnessed on a committed artifact
  rather than only asserted.**
- [`fhir-bundle-patient1.json`](fhir-bundle-patient1.json) — patient
  1's own end-of-run FHIR Bundle (`--emit fhir`, `ehrt.sim-emit-fhir.interface/bundle-run`):
  `Patient`, `Encounter`, five `Observation`s (the CBC panel), and
  `Coverage` — no `Condition`/`MedicationRequest` here, since this
  patient's pathway is a plain admission + order + discharge (no module,
  no medication). Post-M6 (ADR-0014): every resource here also carries
  `meta.security` (the standard HTEST "test health data" label) and
  `meta.tag` (`{"system": "urn:ehrt.sim", "code": "42"}`, this
  run's own seed) — regenerated to show the labels, no other content
  changed. The v2 side (`ground-truth.edn`, `messages.txt`) is
  byte-identical to before: the labels are a FHIR-rendering-only
  addition, never a ground-truth fact.

## The ids resolving across both — patient 1 (MRN000001)

Patient 1's opening ADT^A01, verbatim from `messages.txt` (segments
shown one per line for readability; the real wire format uses `\r`):

```
MSH|^~\&|EHR-TESTING-SIM|SIM|||20240101000000+0000||ADT^A01|MRN000001-A01-0|P|2.4
EVN|A01|20240101000000+0000
PID|1||MRN000001||Garcia^Sandra||19520726|F|||914 Fairview Blvd^^Salt Lake City^UT^84101||(349)906-1132
PV1|1|I|Renal^^RENAL-04^general-hospital||||4255631598^Chen^Amara
IN1|1||medicare-65|Medicare
```

The same patient's `Patient` and one `Observation` resource, from
`fhir-bundle-patient1.json`:

```json
{
  "resourceType": "Patient",
  "id": "PID-000000-918175ce",
  "identifier": [{"system": "urn:ehrt.sim:mrn", "value": "MRN000001"}],
  "name": [{"family": "Garcia", "given": ["Sandra"]}],
  "gender": "female",
  "birthDate": "1952-07-26",
  "address": [{"line": ["914 Fairview Blvd"], "city": "Salt Lake City", "state": "UT", "postalCode": "84101"}],
  "telecom": [{"system": "phone", "value": "349-906-1132"}]
}
```

```json
{
  "resourceType": "Observation",
  "id": "PID-000000-918175ce-obs-2",
  "status": "final",
  "code": {"coding": [{"system": "http://loinc.org", "code": "718-7",
                        "display": "Hemoglobin [Mass/volume] in Blood"}]},
  "valueQuantity": {"value": 8.2, "unit": "g/dL"},
  "referenceRange": [{"low": {"value": 12.0}, "high": {"value": 17.5}}],
  "interpretation": [{"coding": [{"code": "L"}]}]
}
```

Resolving across both:

- **`Patient.id` ("PID-000000-918175ce") is `ehrt.sim-engine.streams/patient-id-for`'s
  own internal id for patient ordinal 0** — never rendered on the wire
  itself (HL7 has no field for it), but the SAME id
  `ehrt.sim-emit-hl7.emit-hl7` uses internally to know which patient a
  message is about; the
  cross-emitter id property test
  (`ehrt.sim-emit-fhir.emit-fhir-test/fhir-patient-id-and-active-mrn-resolve-to-the-same-hl7-identity`)
  checks exactly this correspondence.
- **`Patient.identifier[0].value` ("MRN000001") is PID-3, verbatim** —
  the one identifier both formats actually put on the wire/document.
- **The Observation's LOINC code (`718-7`, Hemoglobin) and abnormal
  interpretation (`L`, low) are the SAME computed truth** rendered
  twice: OBX-8 `L` in the ORU^R01 message (`messages.txt`, not shown
  above — see `../order-result/README.md`'s own excerpt for the full
  ORU), and `interpretation[0].coding[0].code "L"` here — both derived
  from the identical `ehrt.sim-engine.order-profiles/abnormal-flag`
  computation over the identical sampled value, never re-derived
  independently by either emitter.
