# Charter — `sim`

> **Draft for the author's edit.** Derived from
> `src/ehrt/sim/interface.clj` and the namespaces it delegates to
> (`run`, `identifiers`, `version`, and `manifest`), their own
> docstrings, and the ADRs those docstrings cite. **UNCLEAR** marks a
> contract the shipped surface does not settle.

## 1. Mission

Orchestrate a simulation run: take a config, drive the engine, plan
and render emission, self-check the result, and stamp a manifest — and
be the one **stable façade** everything above the simulator depends on.

Residual `components/sim` is **pure orchestration** (`run`,
`identifiers`, `version`, `manifest`). Every concern that could be
named as its own domain — `sim-model`, `patient-simulator`,
`sim-engine`, `sim-emit-hl7`, `sim-emit-fhir`, `sim-check`,
`provenance` — now lives in its own component behind this **same**
façade, unchanged in width or shape across the split (ADR-0043, M4;
`.agents/plans/2026-08-02-sim-split-plan.md` AR-3).

## 2. Interface contract

**This façade is deliberately wide** (migration ruling R5, ADR-0001):
it re-exports exactly what `bases/sim-cli`'s own src called, determined
by grep against the pre-merge `ehr-testing-sim` repo, **not** by
interface-design judgment. Its width is not evidence about how
`components/sim` should be decomposed, and narrowing it is a separate,
author-ruled decision — not a consequence of the split completing.
`corpus` depends on this interface's stability (ADR-0012).

### Result vocabulary, forwarded from `kernel`

- `ok` — `(ok payload)`, delegating to `kernel`'s `result/ok`.
- `rejected` — `(rejected category payload)`.
- `error` — `(error category payload)`.
- `ok?` — `(ok? r)`.
- `rejected?` — `(rejected? r)`.

Five of `kernel`'s seven result vars; see UNCLEAR-S1.

### Capabilities

- `run-command` — the `sim run` capability. `opts` takes `:seed`
  (**required**, long), `:patients`, `:reference-date` (ISO date
  string, the pinned input for HL7 timestamp anchoring),
  `:utc-offset` (pinned rendering and manifest input),
  `:warm-up-seconds`, `:emit` (`"hl7"` to render messages into the
  payload), plus engine options. Runs the simulation, **self-checks
  the invariant catalog over its own output**, and returns
  `{:ground-truth [...] :manifest {...} :summary {...} :messages
  [...]}` — `:messages` present only when `:emit` is `"hl7"`.
- `check-command` — the `sim check` capability:
  `(check-command ground-truth opts)` → the invariant catalog's
  verdict, given **this run's own config**. Q14(a), 2026-09-01: it
  threads facility and warm-up but **not** order profiles, and a
  flagless call is **byte-identical** to the 1-arity `check-all`.
  Lives beside `run-command`'s own self-check call deliberately —
  it is the same orchestration step (config → facility → catalog).
- `check-all` — the invariant catalog directly, four arities:
  `(check-all ground-truth)`,
  `+ facility-config`, `+ warm-up-seconds`, `+ order-profiles-config`.
  Delegates to `sim-check`.
- `identifiers-command` — the `sim identifiers` capability.
- `version` — this simulator's version.
- `git-sha` — `(git-sha)` → the build's git sha, stamped alongside
  `version`.

## 3. Data shapes owned

| shape | where | what it fixes |
|---|---|---|
| the **run manifest** | `manifest.clj` | every field stamped into a run, including `:event-schema-version`, `:stream-scheme`, seed, reference date and UTC offset |
| the **run-command payload** | `run.clj` | `{:ground-truth :manifest :summary :messages?}` |
| the **run config** | `run.clj` | the operator-facing option vocabulary, as distinct from the engine's own `config-keys` |
| the **identifiers** report | `identifiers.clj` | |

`ehrt.sim.manifest`'s own **mirror was retired** rather than kept: a
copy validates against itself and agrees with its own mistake. The
`ManifestV*` records live in `provenance`; this brick stamps them.

## 4. Invariants guaranteed

- **A run self-checks.** `run-command` runs the invariant catalog over
  its own output, and **a run that violates its own invariants is an
  `:error` — a bug in us, not a legitimate rejection.** That is the
  result-doctrine's `:rejected`/`:error` distinction used precisely.
- **`:seed` is required.** Determinism is not optional, and a missing
  seed fails before anything is drawn.
- **Config is validated before the RNG starts.** The five ARC-4
  profiles are checked here, at entry, which is why their schemas are
  on `sim-model`'s seam at all.
- **Pinned rendering inputs.** `:reference-date` and `:utc-offset` are
  config, defaulted from the emitter's own constants, and stamped into
  the manifest — so a rendering is reproducible from the manifest.
- **One pool set per run.** `person-walk-config` defaults to
  `sim-model`'s exported payer pools, so a run's people and its
  patients draw coverage from one pool set by construction.
- **Façade stability.** The width and shape of this interface did not
  change across the entire split. `corpus` depends on that.
- **A flagless `check-command` is byte-identical to `check-all`'s
  1-arity.**

## 5. Non-goals

- **Owns no simulation logic.** The loop is `sim-engine`'s, the nouns
  are `sim-model`'s, the module walk is `patient-simulator`'s, the
  person process is `person-simulator`'s, the catalog is
  `sim-check`'s, and rendering belongs to the two emitters. This brick
  wires them.
- **Not a CLI.** `bases/cli` is the operator surface; this is a
  library capability that the CLI mounts in-process.
- **Does not define the manifest records** — `provenance` does.
- **Not evidence about decomposition.** Stated on the seam itself:
  do not treat this file's width as an argument about how
  `components/sim` should be split.

## 6. Forbidden edges

Requires, in `src`: `kernel`, `patient-simulator`, `person-simulator`,
`sim-check`, `sim-emit-fhir`, `sim-emit-hl7`, `sim-engine`,
`sim-model` — eight of the workspace's nineteen components. It is the
simulator's top of stack. (`provenance` is a **test-scope** edge here,
not a src one.)

Must never require:

- **`corpus`** or **`corpus-io`** — `corpus` depends on *this*
  interface's stability (ADR-0012); the reverse is a cycle and would
  break the guarantee corpus is built on.
- **`bases/cli`** — bases depend on components, never the reverse.
- **`judge`** and the three judge engines — judging is a separate
  domain that reads artifacts, not a simulator dependency.
- **`docs-tooling`**, **`oracle`**, **`palgebra`**.

## UNCLEAR — the author's review queue

- **UNCLEAR-S1 — the result vocabulary is forwarded, but only five
  sevenths of it.** This façade re-exports `ok`, `rejected`, `error`,
  `ok?` and `rejected?`, but not `error?` or `valid?`, which `kernel`
  also exports. Two readings: *(a)* correct and load-bearing — the
  seam carries exactly what `bases/sim-cli` called, so the two absent
  vars simply had no caller, and adding them would be
  interface-design judgment of the sort R5 forbids; *(b)* an
  incompleteness a caller will trip over, since a consumer that can
  construct an `error` cannot then test for one without reaching
  `kernel` directly. Note this façade is frozen by an equality gate
  (AR-M4-3) that fires on **added** vars, so resolving this is not a
  free edit.
- **UNCLEAR-S2 — where the operator-facing config vocabulary is
  defined.** `sim-engine` exports `config-keys` as "the canonical,
  documented list of every key `run`'s config map accepts", gated by a
  plumbing-completeness test. But `run-command`'s own docstring lists
  a *different* vocabulary (`:seed`, `:patients`, `:reference-date`,
  `:utc-offset`, `:warm-up-seconds`, `:emit`, "plus engine options").
  Whether the orchestration-level keys have their own canonical list
  and their own gate, or are documented only in that docstring, is not
  settled by the shipped surface. If it is only the docstring, the
  operator-facing vocabulary is the less-gated of the two.
