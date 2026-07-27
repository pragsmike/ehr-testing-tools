# Demo: Persona-enriched PID + IN1 (Milestone M4)

A plain default run — no `--config` needed, since every patient is
now unconditionally prepended with a `:registered` step
(`ehr-testing-sim.engine/run`'s own docstring) that samples a persona
(name, DOB, sex, address, phone, SSN-shaped id, payer). Seed 41 was
picked because it happens to produce a patient named `O'Brien`, giving
this demo a real, naturally-occurring apostrophe to show alongside
Task 4's escaping property (which is about literal ER7 delimiter
characters — `|^~\&` — not ordinary punctuation; an apostrophe needs
no escaping at all, and this excerpt shows exactly that: it round-trips
untouched).

## Command

```bash
clojure -M:cli run --seed 41 --patients 5 --emit hl7
```

## What to look for

- [`ground-truth.edn`](ground-truth.edn): every patient's log opens
  with `:registered`, carrying the full `ehr-testing-sim.persona/Persona`
  map — patient 4 (`PID-000003-...`) is `O'Brien, Jessica`.
- [`messages.txt`](messages.txt): PID is enriched on every message type
  (both ADT^A01 and ADT^A03 below), not admission-only; IN1 rides ONLY
  the admission message, the real HL7v2 convention.

## Excerpt: patient 4 (MRN000004), admission — PID + IN1, verbatim from `messages.txt`

```
MSH|^~\&|EHR-TESTING-SIM|SIM|||20240101015700+0000||ADT^A01|MRN000004-A01-7020|P|2.3
EVN|A01|20240101015700+0000
PID|1||MRN000004||O'Brien^Jessica||19811009|F|||56 Harborview Rd^^Boston^MA^02108||832-794-8280
PV1|1|I|Renal^^RENAL-01^general-hospital||||7769592800^Chen^Amara
IN1|1||medicaid|Medicaid
```

(Segments shown one per line for readability; the real wire format in
`messages.txt` uses `\r`, HL7v2's actual segment delimiter.)

**PID-5** (`O'Brien^Jessica`) — the apostrophe passes through byte-
faithfully; `ehr-testing-sim.emit-hl7/escape-er7` is the identity
function on any string containing none of ER7's five reserved
characters (`|^~\&`), which an apostrophe isn't one of
(`escape-er7-is-identity-for-strings-with-no-delimiter-characters`,
`test/ehr_testing_sim/emit_hl7_test.clj`). **PID-7** (`19811009`) is
the sampled DOB, HL7 date format. **PID-8** (`F`) is sex, Table 0001.
**PID-11** (`56 Harborview Rd^^Boston^MA^02108`) is the XAD address.
**PID-13** (`832-794-8280`) is the sampled phone. **IN1-3/IN1-4**
(`medicaid` / `Medicaid`) are the sampled payer pool entry's id/name —
SimHospital issue #3's own request
(`docs/research/SimHospital-Synthea-limitations-considered.md` §5.3),
answered.

Patient 4's own discharge (`ADT^A03`, later in `messages.txt`) carries
the identical enriched PID and no IN1 — proof PID enrichment is
uniform across message types while IN1 stays admission-only.
