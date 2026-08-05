# Demo: order/result (ORM^O01 / ORU^R01), through the CLI post-Task-0

Re-run of the M3 order/result cycle, but THROUGH the CLI this time —
the M4 Task 0 wiring fix's own proof: `:pathways` (an authored pathway
containing an `:order` step) now reaches the engine from
`ehrt.sim.run/run-command`, not just from a direct
`engine/run` API call the way `engine-test` already exercised it.
Before Task 0, this exact config would have silently run the DEFAULT
pathway instead — no error, just the wrong traffic, invisible to
anyone watching only the CLI.

## Command

```bash
clojure -M:cli run --seed 42 --patients 3 \
  --config docs/demos/order-result/config.edn --emit hl7
```

[`config.edn`](config.edn) supplies `:pathways` — the data-heavy key
with no flag of its own, per `run-command`'s own `--config`
passthrough (Milestone M4 Task 0).

## What to look for

- [`ground-truth.edn`](ground-truth.edn): every patient's log opens
  with `:registered` (Persona, M4), then `:admission`, `:order-placed`,
  `:result-available`, `:discharge` — the authored pathway exactly, for
  all 3 patients.
- [`messages.txt`](messages.txt): 12 messages (4 per patient — ADT^A01,
  ORM^O01, ORU^R01, ADT^A03). `:registered` renders no message of its
  own (ADR-0012's own precedent: truth about the run, not wire traffic
  a real ADT/ORM/ORU feed would carry).

## Excerpt: ORM^O01 then ORU^R01 for patient 1 (MRN000001), verbatim from `messages.txt`

```
MSH|^~\&|EHR-TESTING-SIM|SIM|||20240101000000+0000||ORM^O01|MRN000001-O01-0|P|2.3
PID|1||MRN000001||Gonzalez^Barbara||19350221|F|||22 Chestnut Ct^^Providence^RI^02903||607-335-0157
PV1|1|I|Renal^^RENAL-01^general-hospital||||0384055899^Chen^Amara
ORC|NW|MRN000001-O01-0
OBR|1|||58410-2^CBC panel - Blood by Automated count^LN

MSH|^~\&|EHR-TESTING-SIM|SIM|||20240101012100+0000||ORU^R01|MRN000001-R01-4860|P|2.3
PID|1||MRN000001||Gonzalez^Barbara||19350221|F|||22 Chestnut Ct^^Providence^RI^02903||607-335-0157
PV1|1|I|Renal^^RENAL-01^general-hospital||||0384055899^Chen^Amara
ORC|NW|MRN000001-R01-4860
OBR|1|||58410-2^CBC panel - Blood by Automated count^LN
OBX|1|NM|6690-2^Leukocytes [#/volume] in Blood by Automated count^LN||21.0|K/uL|4.5-11.0|H
OBX|2|NM|789-8^Erythrocytes [#/volume] in Blood by Automated count^LN||4.7|M/uL|4.2-5.9|N
OBX|3|NM|718-7^Hemoglobin [Mass/volume] in Blood^LN||19.6|g/dL|12.0-17.5|H
OBX|4|NM|4544-3^Hematocrit [Volume Fraction] of Blood by Automated count^LN||41.2|%|36.0-50.0|N
OBX|5|NM|777-3^Platelets [#/volume] in Blood by Automated count^LN||280.0|K/uL|150-450|N
```

(Segments are shown one per line here for readability; the real wire
format in `messages.txt` uses `\r`, HL7v2's actual segment delimiter.)
PID is enriched with the same patient's Persona-sampled demographics as
every other message type — order/result messages are not a special
case (`ehrt.sim-emit-hl7.emit-hl7/pid-segment` applies uniformly). Note
OBX-8 `H` (abnormal-high) on the leukocyte and hemoglobin results —
computed truth from `ehrt.sim-engine.order-profiles/abnormal-flag`,
never a re-derivation at emit time.
