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

Pushed `64b71c08..68d972d2` as one commit (both docs, this record, the
prompt archive, docsgen's three generated files). Before it: the
docs-tooling brick tests green (exit 0); the leak grep over the staged
diff found nothing identifying the downstream consumer; gitleaks clean.
`bin/post-push-verify 64b71c08 68d972d2`: remote tip matches; the
commit message is ASCII; CI not yet indexed at that moment (disclosed
by the script). No local `make test` (docs-only, AGENTS.md 22). No tag
paid.

**CI run 44062772634 completed `failure`** at
`68d972d296038c648dcc30621e9cc85869e2448a`, attempt 1. **This session
has no close marker.** The failing step is `poly test :all
skip:integration`: `ClassNotFoundException:
hl7.v2.validation.ValidationContextBuilder` loading
`ehrt.judge.pairing-conviction-test` in project `conformance`, after
six "Download corrupted: Checksum validation failed, expected
'<!DOCTYPE'" lines for the three `gov.nist` 1.7.3 jars. Cause, probed
from this host at 10:16Z: `hit-nexus.nist.gov/repository/releases/...`
answers `302` to a `www.nist.gov` web page, so the jars arrive as HTML.
Not this diff: no file under `components/`, `projects/` or `.github/`
changed. The nightly Integration run 43909539260 (2026-11-16 16:37Z)
was still green, so the repository changed after it. `verify-nist-lock`
and the generated-doc freshness step were SKIPPED behind the failure,
so CI has not checked docsgen freshness at this tip. The fix (a mirror
CI can reach, or another source for the three coordinates) is an author
decision and was not attempted. The commit recording this paragraph
will fail CI the same way until then.

Background processes this session started: one `gh run watch`, exited
with the run; none left running.

Added 2026-11-17, later the same day: CI run 44108231313 completed
`success` at `e5cd16f8` (the NIST-channel fix,
`2026-11-17-nist-channel-cdc-pin.md`) and is the first green run to
check this session's docsgen freshness. That record also corrects the
inference above from the green nightly: the Integration lane restores
`~/.m2` from a cache, so run 43909539260 does not date the change.
