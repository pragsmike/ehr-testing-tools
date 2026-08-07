# Demos

Small, CLI-produced traces (`bin/ehrt sim run ...` — never engine
internals, per `AGENTS.md`'s M4 standing rule) captured as fixtures: the
exact command, the resulting ground-truth log, and the rendered HL7v2
messages, side by side. Each demo is ≤10 patients, small enough to read
in full.

**These double as future log-player fixtures.** A later milestone's
log player (`.agents/plans/roadmap.md`'s consumer plan) will want small,
known-good, CLI-produced traces to replay — these are seeded now so
that future work has real examples to start from rather than
generating throwaway ones.

**For a population-scale, run-it-yourself scenario instead of a small
captured trace, see the sibling
[`../scenarios/`](../scenarios/README.md)** (vendoring batch 2's own
rider, `notes/ADRs.md` ADR-0071, AR-VB2-R) — a scenario is a runnable
`config.edn` + `README.md`, not a fixture.

## Contents

- [`boarding-transfer/`](boarding-transfer/) — the top-level README's own
  headline claim, captured as a real trace: ED hallway boarding and a
  bed-ready transfer, both emergent from census pressure against the
  default facility's configured capacity, never scripted. Source of
  the top-level README's own excerpt (go-public session, Task 3 —
  every prior version of that excerpt had drifted from any real
  output; this demo is the fix).
- [`order-result/`](order-result/) — the M3 order/result cycle
  (ORM^O01 + ORU^R01), re-run through the CLI after Milestone M4
  Task 0's wiring fix (`:pathways` now reaches the engine from
  `run-command`, not just from a direct API caller — the gap the tools
  consumer loop surfaced). Uses `--config` to supply the pathway
  (`ehrt.sim.run/run-command`'s data-heavy-key passthrough
  vehicle, since `:pathways` has no CLI flag of its own).
- [`persona-enriched/`](persona-enriched/) — Milestone M4's Persona
  landing: demographic PID enrichment (name, DOB, sex, address, phone)
  and a payer-carrying IN1 segment, on a plain default run (no
  `--config` needed — Persona samples for every patient unconditionally).
  Seed 41 was chosen because it happens to produce an `O'Brien` patient,
  so the excerpt also documents that ordinary apostrophes need no ER7
  escaping at all (Task 4's own finding is about literal delimiter
  characters, not everyday punctuation).
- **`site-profiles/`** — relocated (D1a rider, 2026-08-02, ADR-0029) to
  `components/sim-emit-hl7/docs/demos/site-profiles/` — its sole
  subject, the site-profile invariance property, is emitter-owned, not
  residual-sim-owned. See that component's own `docs/demos/README.md`.
- [`emit-state/`](emit-state/) — Milestone M6's own demo pair: the SAME
  seed as `order-result/`, rendered once as HL7v2 (`--emit hl7`) and
  once as FHIR R4 (`--emit fhir`) — two renderings, one truth, with
  `Patient.id`/`Patient.identifier` resolving to the same patient-id/MRN
  `ehrt.sim-emit-hl7.emit-hl7` uses, and the same computed LOINC/abnormal-
  flag truth visible in both an OBX segment and an `Observation`
  resource.

Each subdirectory holds:

- `README.md` — the exact command, what to look for, and an excerpt
- `config.edn` — present only for `order-result/` (the `--config` file)
- `ground-truth.edn` — the full ground-truth log, pretty-printed
- `messages.txt` — the full rendered HL7v2 message set, blank-line
  separated (each message's own segments are `\r`-delimited internally,
  per the real ER7 wire format — a text viewer that shows them running
  together per message is rendering that CR correctly, not truncating)

(`site-profiles/`'s own exception to this shape — one `ground-truth.edn`
for two site profiles, `messages-default.txt`/`messages-aldric.txt` in
place of a single `messages.txt` — is documented at its new home,
`components/sim-emit-hl7/docs/demos/README.md`, not restated here.)
