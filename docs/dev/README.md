# docs/dev/

The maintainer path — for contributing to or extending this workspace,
not for using what it builds. If you're looking for how to run
`bin/ehrt`, generate a corpus, or read a report, you want
[`docs/`](../) instead; nothing here assumes you've read this
directory first.

Start with [`architecture.md`](architecture.md) — the workspace map:
what a Polylith brick is here, which components and bases exist, how
projects compose them, and where the theory docs live. Then:

- [`way-of-working.md`](way-of-working.md) — this workspace's own
  session conventions (checkpoints, fix-forward-with-disclosure, the
  commit/push ritual) and how they differ from sim's 40-session
  pre-merge history.
- [`positioning.md`](positioning.md) — how this workspace relates to
  [`ehr-testing-guide`](https://github.com/pragsmike/ehr-testing-guide)
  and to every audience that reads its docs, user and maintainer alike
  — the canonical audience register `docs/README.md` itself routes
  from.
- [`migration/polylith-brief.md`](migration/polylith-brief.md) — the
  Polylith architecture reference the sim+tools consolidation was
  planned against.
- [`notation.md`](notation.md) — the resource-equation notation
  `pipeline.md` and sim's own theory docs are written in.
- [`pipeline.md`](pipeline.md) — the generated pipeline diagram
  (`make pipeline` from `components/corpus/docs/pipeline.edn`).
- [`components.md`](components.md) — the external tools/artifacts this
  workspace builds on, at decision-informing depth.
- [`engine-onboarding.md`](engine-onboarding.md) — the checklist a new
  engine wrapper (generator, mutator, or gate) must satisfy before it's
  trusted enough to feed a committed manifest.
- [`source-sink-design.md`](source-sink-design.md) — the deeper design
  rationale for the intake/mutate/sink seam, cited throughout source as
  `Part I`–`Part IX`.

## Deprecation notices

- **`bases/sim-cli` / `projects/sim`** — DEPRECATED, not removed (R33,
  `notes/ADRs.md` ADR-0009). Kept working and tested; `bin/ehrt sim
  run` (the in-process mount, ADR-0005) is the presented surface
  everywhere else. Retirement trigger, dated not scheduled:
  `notes/facts-register.md` F2 — retire when a review finds no use
  outside its own tests.

## Component-adjacent docs

Not listed above because they live beside the component they document,
not in this directory — a maintainer working on that component's own
code reads them alongside it. The full disposition of every doc this
workspace carries, including why each of these stayed
component-adjacent instead of moving here, is `notes/docs-audit.md`.

- `components/sim/docs/` — sim's engine internals (event sourcing, the
  GMF interpreter, patient-state model, operational models, trajectory
  computation), its own theory docs (`sim-theory.*`), research
  (`research/`), and demo fixtures (`demos/`).
- `components/corpus/docs/` — `palgebra-design.md`, the experiments
  evidence trail (`experiments/`), research (`research/`), and the
  hand-authored docsgen sources (`pipeline.edn`, `use-cases.edn`,
  `signature.edn`).
- `components/palgebra/` — palgebra's own tooling and examples.
