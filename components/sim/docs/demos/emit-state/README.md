# Demo: EmitState — one seed, two renderings, one truth

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
  --config components/sim/docs/demos/order-result/config.edn --emit hl7

bin/ehrt sim run --seed 42 --patients 3 \
  --config components/sim/docs/demos/order-result/config.edn --emit fhir
```

## What to look for

- [`ground-truth.edn`](ground-truth.edn) — the shared truth both
  renderings derive from (identical to `order-result/ground-truth.edn`).
- [`messages.txt`](messages.txt) — the HL7v2 stream (`--emit hl7`), same
  as `order-result/messages.txt`.
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
MSH|^~\&|EHR-TESTING-SIM|SIM|||20240101000000+0000||ADT^A01|MRN000001-A01-0|P|2.3
EVN|A01|20240101000000+0000
PID|1||MRN000001||Gonzalez^Barbara||19350221|F|||22 Chestnut Ct^^Providence^RI^02903||607-335-0157
PV1|1|I|Renal^^RENAL-01^general-hospital||||0384055899^Chen^Amara
IN1|1||medicare-65|Medicare
```

The same patient's `Patient` and one `Observation` resource, from
`fhir-bundle-patient1.json`:

```json
{
  "resourceType": "Patient",
  "id": "PID-000000-918175ce",
  "identifier": [{"system": "urn:ehrt.sim:mrn", "value": "MRN000001"}],
  "name": [{"family": "Gonzalez", "given": ["Barbara"]}],
  "gender": "female",
  "birthDate": "1935-02-21",
  "address": [{"line": ["22 Chestnut Ct"], "city": "Providence", "state": "RI", "postalCode": "02903"}],
  "telecom": [{"system": "phone", "value": "607-335-0157"}]
}
```

```json
{
  "resourceType": "Observation",
  "id": "PID-000000-918175ce-obs-0",
  "status": "final",
  "code": {"coding": [{"system": "http://loinc.org", "code": "6690-2",
                        "display": "Leukocytes [#/volume] in Blood by Automated count"}]},
  "valueQuantity": {"value": 21.0, "unit": "K/uL"},
  "referenceRange": [{"low": {"value": 4.5}, "high": {"value": 11.0}}],
  "interpretation": [{"coding": [{"code": "H"}]}]
}
```

Resolving across both:

- **`Patient.id` ("PID-000000-918175ce") is `ehrt.sim-engine.engine/patient-id-for`'s
  own internal id for patient ordinal 0** — never rendered on the wire
  itself (HL7 has no field for it), but the SAME id
  `ehrt.sim-emit-hl7.emit-hl7` uses internally to know which patient a
  message is about; the
  cross-emitter id property test
  (`ehrt.sim-emit-fhir.emit-fhir-test/fhir-patient-id-and-active-mrn-resolve-to-the-same-hl7-identity`)
  checks exactly this correspondence.
- **`Patient.identifier[0].value` ("MRN000001") is PID-3, verbatim** —
  the one identifier both formats actually put on the wire/document.
- **The Observation's LOINC code (`6690-2`, Leukocytes) and abnormal
  interpretation (`H`, high) are the SAME computed truth** rendered
  twice: OBX-8 `H` in the ORU^R01 message (`messages.txt`, not shown
  above — see `../order-result/README.md`'s own excerpt for the full
  ORU), and `interpretation[0].coding[0].code "H"` here — both derived
  from the identical `ehrt.sim-engine.order-profiles/abnormal-flag`
  computation over the identical sampled value, never re-derived
  independently by either emitter.
