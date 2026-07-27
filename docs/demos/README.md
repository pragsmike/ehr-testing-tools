# Demos

Small, CLI-produced traces (`clojure -M:cli run ...` — never engine
internals, per `AGENTS.md`'s M4 standing rule) captured as fixtures: the
exact command, the resulting ground-truth log, and the rendered HL7v2
messages, side by side. Each demo is ≤10 patients, small enough to read
in full.

**These double as future log-player fixtures.** A later milestone's
log player (`.agents/plans/roadmap.md`'s consumer plan) will want small,
known-good, CLI-produced traces to replay — these are seeded now so
that future work has real examples to start from rather than
generating throwaway ones.

## Contents

- [`order-result/`](order-result/) — the M3 order/result cycle
  (ORM^O01 + ORU^R01), re-run through the CLI after Milestone M4
  Task 0's wiring fix (`:pathways` now reaches the engine from
  `run-command`, not just from a direct API caller — the gap the tools
  consumer loop surfaced). Uses `--config` to supply the pathway
  (`ehr-testing-sim.run/run-command`'s data-heavy-key passthrough
  vehicle, since `:pathways` has no CLI flag of its own).
- [`persona-enriched/`](persona-enriched/) — Milestone M4's Persona
  landing: demographic PID enrichment (name, DOB, sex, address, phone)
  and a payer-carrying IN1 segment, on a plain default run (no
  `--config` needed — Persona samples for every patient unconditionally).
  Seed 41 was chosen because it happens to produce an `O'Brien` patient,
  so the excerpt also documents that ordinary apostrophes need no ER7
  escaping at all (Task 4's own finding is about literal delimiter
  characters, not everyday punctuation).

Each subdirectory holds:

- `README.md` — the exact command, what to look for, and an excerpt
- `config.edn` — present only for `order-result/` (the `--config` file)
- `ground-truth.edn` — the full ground-truth log, pretty-printed
- `messages.txt` — the full rendered HL7v2 message set, blank-line
  separated (each message's own segments are `\r`-delimited internally,
  per the real ER7 wire format — a text viewer that shows them running
  together per message is rendering that CR correctly, not truncating)
