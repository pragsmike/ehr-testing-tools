# Demos

Small, CLI-produced traces (`bin/ehrt sim run ...` — never engine
internals, per `AGENTS.md`'s M4 standing rule) captured as fixtures: the
exact command, the resulting ground-truth log, and the rendered HL7v2
messages, side by side.

**Relocated here (D1a rider, 2026-08-02, ADR-0029): moved from
`components/sim/docs/demos/site-profiles/` where it was left behind,
disclosed, at the sim-emit-hl7 extraction (Wave D stage D0, this same
ADR's own execution note, `.agents/session-records/2026-08-02-sim-split-
s3-wave-d-d0.md`) — its sole subject, the site-profile invariance
property, is emitter-owned, the same "subject is the emitter" test D0
applied to the three moved namespaces themselves. Pure file move, no
content change; verified oracle-free before moving (no code path reads
under this directory, grep-confirmed).**

## Contents

- [`site-profiles/`](site-profiles/) — the site-profiles milestone's own
  invariance property: the SAME seed, rendered under no profile and
  under a deliberately gaudy second profile (a different HL7 version,
  renamed sending facility, custom patient-class/disposition codes, a
  `ZPI` payer Z-segment) — one `ground-truth.edn` (byte-identical either
  way), two `messages-*.txt` files that differ only on the declared
  dialect surfaces.

Holds:

- `README.md` — the exact command, what to look for, and an excerpt
- `config-aldric.edn` — the second profile's `--config` file
- `ground-truth.edn` — ONE file — the two runs' logs are byte-identical,
  verified when generating this demo
- `messages-default.txt` / `messages-aldric.txt` in place of a single
  `messages.txt`
