# Dispense disclosure: medication orders carry no dispense, fill or administration (docs-only)

2026-11-17. Base `64b71c08`. Ceremony: R30, taken from the session
prompt (`.agents/prompts/2026-11-17-dispense-disclosure.md`), which
stated no prepare-only mode. `bin/preflight` ran first, exit 0, no
findings (HEAD matched origin/main, tree clean, last five CI runs
green; HEAD not tagged stable-*, disclosed). Docs-only: no source,
config, schema or test file changed; no local `make test` owed
(AGENTS.md 22). Author ruling, 2026-11-17: "go", to the channel's
proposal as the prompt quotes it.

## Step 1: the measurement

A scratch Python script, run `python3 -I medcount.py
components/sim/resources/sim/modules`: `rglob("*.json")`, `json.loads`
each file, walk `states`, keep every state whose `type` is
`MedicationOrder`, count keys. No grep. Output:

```
json files: 66
files with MedicationOrder: 26
{'states': 134, 'prescription': 83, 'refills': 33, 'duration': 32, 'admin_true': 13, 'admin_key': 13, 'chronic_true': 37, 'chronic_key': 37}
state keys: {'administration': 13, 'assign_to_attribute': 102, 'chronic': 37, 'codes': 134, 'complex_transition': 1, 'conditional_transition': 2, 'direct_transition': 127, 'distributed_transition': 4, 'prescription': 83, 'reason': 98, 'remarks': 26, 'type': 134}
prescription keys: {'as_needed': 51, 'dosage': 32, 'duration': 32, 'instructions': 9, 'refills': 33}
```

Every figure the prompt expected holds: 134 states in 26 of 66 files;
83 `prescription`, 33 `prescription.refills`, 32
`prescription.duration`, 13 `administration: true`, 37 `chronic:
true`. Not in the prompt: 51 `as_needed`, 32 `dosage`, 9
`instructions`.

Read, not counted:

- `event_schema.clj` 1062-1082: `:medication-order` is `:active-mrn`,
  `:codes`, optional `:citation`, doc "A medication is prescribed.";
  `:medication-end` closes it by citation. The schema's 29 kinds hold
  none for a dispense, a fill or a medication administration.
- `sim_emit_hl7/registry.clj` 135-140: neither kind has a message-type
  entry. The message types the emitter names are ADT, ORM, ORU, SIU and
  DFT; no RDE, RDS or RAS.
- `compile_trajectory.clj` 305: `medication-order->step` keeps `:type`,
  `:codes` and the citation.
- No `.clj`, `.edn`, Makefile or shell file names
  `future-features`; no gate reads it by content.

## What was written

- `docs/future-features.md`: a `## Clinical depth` section between
  "Fidelity evidence" and "What is not on this menu", one entry,
  `### Medication dispensing and administration`. It says no design is
  chosen and carries no *Design stance:* line: the opt-in law governs
  config keys, and saying a dispense would arrive as one is a design
  choice the tree does not hold. No module count reaches the doc.
- `docs/consuming-ground-truth.md`, "What is not warranted": one
  bold-led paragraph before "Two gated limitation registers", linking
  `future-features.md#medication-dispensing-and-administration`.
- No row in `components/patient-simulator/docs/limitations.md` (step 3).
  `git diff -- components/` was empty before docsgen.

## Finding

- `medication-order->step` (`compile_trajectory.clj:305`) drops every
  authored prescription field: of 134 `MedicationOrder` states, 83
  author `prescription` (51 `as_needed`, 33 `refills`, 32 `duration`,
  32 `dosage`, 9 `instructions`), 13 `administration: true`, 37
  `chronic: true`, and the loader schema (`gmf.clj:1244`) declares none
  of them. **Undecided: needs an author ruling (declared limitation, or
  owed).**

## Where the tree differed from the prompt

Every listed expectation held. Three things the prompt did not say:

1. **The FHIR emitter renders medication orders.**
   `sim_emit_fhir/emit_fhir.clj:224` writes one `MedicationRequest` per
   order (codes, status, encounter, authored time). So the two kinds
   are truth-only on the HL7 v2 side only, and "no pharmacy message on
   the wire" is true of v2 alone. Both docs say "HL7 v2" and the menu
   entry names the `MedicationRequest`; no `MedicationDispense` or
   `MedicationAdministration` string exists under `components/`,
   `bases/` or `docs/`. Fixed forward with this disclosure: one
   defensible reading, since the unqualified sentence would be false.
2. **"No kind for administration exists" needs the word medication.**
   `:immunization`'s schema doc reads "A vaccine is administered"; it
   is a vaccine administration. Both docs say "medication
   administration" or scope the sentence to the order.
3. **"GMF has no dispense state" stayed out of the docs**, as the
   prompt required: nothing in this tree supports or refutes it.

## Close

