---
name: probe
description: >
  Run the composed fan→funnel pipeline N times on the same situation to
  map the decision landscape. Produces a variance report (what's stable
  vs. variable across runs) and a decision landscape map (basins, ridges,
  load-bearing assumptions). Writes to <situation-dir>/probes/ with
  self-contained run-0N/ subdirectories. Use when the user types
  '/probe [situation]' or asks to probe a decision space.
---

# Probe Skill

Run the composed scenarios→committee pipeline (the deliberated choice)
multiple times on the same situation. Compare the resolutions. Identify
what persists across runs (**eigenforms** — robust decisions) and what
varies (**residues** — decisions sensitive to framing, parameters, or
stochasticity). Produce a **variance report** and **decision landscape map**.

This is the **iteration** operation from `palgebra/duality-and-composition.md`:
the Deleuzian walk through the decision space. Each run is an architectural
walk (in residuality theory terms). The walks are never identical — each
actualizes a different trajectory — and the differences reveal structure.

## Situation resolution

The skill determines where to write output using this precedence:

1. **`--situation <path>`** — if the user provides this on invocation, use `<path>` as the situation directory directly.
2. **Config file** — read `situations_root` from `.claude/cyberneutics-config.yaml` (path relative to cyberneutics repo root), then append `<topic-slug>/`.
3. **Default** — use `~/situations/<topic-slug>/`.

Once the situation directory is resolved:
- Create the directory if it doesn't exist.
- If `situation.md` doesn't exist, create it (see scenarios or committee skill for template).
- Write probe output to `<situation-dir>/probes/`.
- Each run's self-contained artifacts go in `<situation-dir>/probes/run-0N/`.

**Rosters** (`agent/roster.md`, `agent/scenario-roster.md`) are always read from the cyberneutics repo — they are part of the methodology, not the situation output.

## When to use

- User types `/probe [situation]`
- User asks to "map the decision landscape for [X]" or "test stability of [Y]"
- High-stakes decisions where understanding the *topology* of the decision
  space matters more than any single resolution
- When a single deliberated choice produced a result the user suspects
  is sensitive to framing or assumptions
- When the user wants to know: "Would we reach the same conclusion if
  we ran this again?"

## When NOT to use

- The decision is low-stakes or time-sensitive → single `/committee` or
  `/scenarios` + `/committee` is sufficient
- The user wants to explore possible futures → `/scenarios`
- The user wants one deliberation → `/committee`
- Token budget is constrained — Probe runs N full pipelines

## Resource equation

```
(situation → resolution)^N → variance-report  [Probe]
variance-report → decision-landscape-map  [Map]
```

The Probe runs the composed pipeline N times, collects the resolutions,
and synthesizes a variance report. The Map operation converts the report
into an actionable decision landscape.

## What the skill does

When invoked, the skill:

1. **Resolves the situation directory** (see Situation resolution above) and **frames the situation** (or reuses an existing framing if the situation already contains scenarios)
2. **Runs N independent deliberated choice cycles** (default N=3):
   - Each run generates scenarios independently (fresh fan)
   - Each run deliberates independently (fresh funnel)
   - Runs do not share context — no cross-contamination
3. **Compares the N resolutions** to produce a variance report
4. **Synthesizes the variance report** into a decision landscape map
5. **Presents findings** to the user: what's stable, what's variable,
   what's load-bearing

### Run independence

Each run must be genuinely independent:
- Fresh scenario generation (different stochastic trajectories)
- Fresh committee deliberation (different debate dynamics)
- No reference to other runs during generation
- The situation framing is the only shared input

This ensures that agreement across runs reflects the decision landscape's
structure, not cross-contamination.

## Parameters

| Parameter | Default | Description |
|-----------|---------|-------------|
| **N** (run count) | 3 | Number of independent runs. Minimum 2, recommended 3, maximum 5 unless user overrides. |
| **Scenario parameters** | Per-run defaults | Optionally fix scenario parameters across runs (same axes, same horizon) to isolate committee-level variance, or let them vary to capture full pipeline variance. |
| **scenario_context** | None | Optionally point to an existing scenario set (e.g. `<situation-dir>/scenarios/`) to reuse across runs (only the committee varies). This isolates funnel-level instability. |

### Variance isolation modes

| Mode | Scenarios | Committee | What it measures |
|------|-----------|-----------|-----------------|
| **Full variance** (default) | Fresh per run | Fresh per run | Total pipeline instability |
| **Funnel-only** | Fixed (reuse one scenario set) | Fresh per run | Committee-level instability given fixed futures |
| **Fan-only** | Fresh per run | Fixed parameters | Scenario-level instability (how different the futures are across runs) |

Full variance is the default because it maps the complete decision landscape.
Funnel-only and fan-only modes are diagnostic — use them when full variance
reveals instability and you want to locate its source.

## Probe record directory

**Location:** `<situation-dir>/probes/` (see **Situation resolution** above for how `<situation-dir>` is determined).

**Files:**

### 00-probe-charter.md

```yaml
---
probe:
  date: YYYY-MM-DD
  situation: "short description"
  N: 3
  mode: full_variance | funnel_only | fan_only
  scenario_parameters: {} | {fixed parameters}
  situation_source: "user input" | "<situation-dir>/scenarios/"
---
```

Body: the framed situation (or reference to existing framing).

### run-01/ through run-0N/

Each subdirectory contains the full artifacts from one deliberated choice cycle:

```
run-01/
  scenarios/
    00-situation.md
    01-roster.md
    01-parameters.md
    02-scenarios.md
    03-assessment.md
  deliberation/
    00-charter.md
    01-roster.md
    01-convening.md
    02-deliberation.md
    03-resolution.md
```

These are the standard `/scenarios` and `/committee` outputs, nested per run.

### 04-variance-report.md

The core analytical output. Compares the N resolutions.

```yaml
---
variance_report:
  date: YYYY-MM-DD
  situation: "short description"
  runs: 3
  mode: full_variance

  eigenforms:
    - finding: "description of what persisted across all runs"
      present_in: [1, 2, 3]
      confidence: high
    - finding: "description of another persistent finding"
      present_in: [1, 2, 3]
      confidence: high

  residues:
    - finding: "description of what varied"
      run_1: "what run 1 said"
      run_2: "what run 2 said"
      run_3: "what run 3 said"
      likely_source: situation_framing | parameters | operational_noise
    - finding: "another variable finding"
      run_1: "..."
      run_2: "..."
      run_3: "..."
      likely_source: parameters

  instability_sources:
    situation_framing:
      - "description of framing ambiguity that drove divergence"
    parameters:
      - assumption: "the load-bearing assumption"
        effect: "when X, resolution says A; when not-X, resolution says B"
    operational_noise:
      - "description of stochastic variation not attributable to substance"

  summary: "2-3 sentence synthesis"
---
```

Body: narrative analysis structured as:

1. **Eigenforms** — what every run found (the stable territory):
   For each eigenform, cite which runs found it and why it's robust.

2. **Residues** — what varied across runs (the interesting instability):
   For each residue, describe the variation, diagnose the source, and
   explain what the variation reveals about the decision landscape.

3. **Instability diagnosis** — which of the three sources drove the variance:
   - **Situation framing**: premises underdetermine the possibility space
   - **Parameters**: load-bearing assumptions that flip outcomes
   - **Operational noise**: LLM stochasticity not washing out

4. **Implications** — what the user should do with this information:
   - Eigenforms → commit with confidence
   - Parameter-driven residues → investigate the load-bearing assumptions
   - Framing-driven residues → tighten the situation description
   - Noise-driven residues → ignore (or increase N)

### 05-landscape-map.md

The topological synthesis. Converts the variance report into actionable structure.

```yaml
---
landscape_map:
  date: YYYY-MM-DD
  situation: "short description"

  basins:
    - basin_id: A
      description: "description of this cluster of resolutions"
      runs: [1, 3]
      key_commitment: "the decision this basin represents"
      assumptions_required: ["assumption 1", "assumption 2"]
    - basin_id: B
      description: "description of alternative cluster"
      runs: [2]
      key_commitment: "the alternative decision"
      assumptions_required: ["assumption 3"]

  ridges:
    - between: [A, B]
      pivots_on: "the assumption that separates the basins"
      sensitivity: "how small a change flips the outcome"

  load_bearing_assumptions:
    - assumption: "the critical assumption"
      if_true: "basin A"
      if_false: "basin B"
      currently_testable: true | false
      test: "how to test it"

  robust_actions:
    - action: "something that appears across multiple basins"
      present_in_basins: [A, B]
      rationale: "why it's robust"

  monitoring_plan:
    - signal: "early warning sign"
      indicates: "which basin is materializing"
      source_scenario: "which scenario identified this signal"
---
```

Body: narrative landscape summary structured as:

1. **Terrain overview** — how many distinct basins? How fragmented is the
   decision landscape?

2. **Basin descriptions** — what each cluster of resolutions looks like,
   what assumptions it requires, what it optimizes for.

3. **Ridge analysis** — where do outcomes flip? Which assumptions are
   load-bearing? How sensitive are the transitions?

4. **Robust commitments** — actions that survive across basins. These are
   safe to take regardless of which future materializes.

5. **Contingent commitments** — actions that depend on which basin you're in,
   with trigger conditions for switching.

6. **Monitoring plan** — early warning signs that indicate which basin
   is materializing, sourced from the scenarios.

## Execution approach

Given token and time costs, the probe should be transparent about its process:

1. **Before starting**: Tell the user how many runs will be executed and the
   expected cost/time. Ask for confirmation if N > 3.
2. **During each run**: Briefly note which run is in progress ("Run 2 of 3").
3. **Between runs**: Do not carry forward any specific content from previous
   runs. The situation framing is the only shared input.
4. **After all runs**: Generate the variance report and landscape map, then
   present the key findings inline.

### Abbreviated runs

For probing, individual runs can be more compact than standalone deliberated
choices:
- Scenarios: 2–3 paragraphs per scenario (not 3–5)
- Committee: standard format (not rigorous/extended)
- Resolution: full format (this is what gets compared)

The resolution is the unit of comparison. Scenario and deliberation detail
matters for the individual run but the probe's analytical value comes from
comparing resolutions.

## What success looks like

After a probe, the user should be able to:

- **Name the eigenforms**: "Every run agreed that [X]"
- **Name the ridges**: "The decision hinges on whether [Y]"
- **Distinguish robust from fragile commitments**: "We can safely do [A]
  regardless; [B] depends on [Y]"
- **Know what to monitor**: "If we see [signal], basin [Z] is materializing"
- **Decide whether to commit**: The landscape map either gives confidence
  (broad basin, clear eigenforms) or identifies what needs more investigation
  (narrow ridges, load-bearing assumptions that are untested)

## Integration with other skills

| Skill | Relationship |
|-------|-------------|
| `/scenarios` | Probe runs this internally per run (fan half) |
| `/committee` | Probe runs this internally per run (funnel half) |
| `/review` | Can be run on individual probe runs' deliberations if quality is in question |
| `/string-diagram` | The probe pipeline can be visualized; see `palgebra/duality-and-composition.md` |

## Theoretical grounding

- **Residuality theory** (`wild/residuality-theory/`): architectural walks,
  residues vs. eigenforms, process over substance
- **Deleuzian repetition** (`essays/06-deleuze-difference-repetition.md`):
  repetition produces difference, not redundancy
- **Decision monad** (`palgebra/duality-and-composition.md`): the Probe tests
  the monad's stability — does M(x) ≈ M(x) across runs?
- **Cybernetic observation** (`essays/04-cybernetics-and-observation.md`):
  eigenforms are what the observer converges on through repeated observation

## Files reference

- **Formal treatment**: `palgebra/duality-and-composition.md` (Probe and Map operations)
- **Philosophical bridge**: `essays/10-decisions-under-uncertainty.md`
- **Scenario skill**: `.agents/skills/scenarios/SKILL.md`
- **Committee skill**: `.agents/skills/committee/SKILL.md`
- **Workflow**: `artifacts/deliberated-choice-workflow.md`
- **Residuality theory**: `wild/residuality-theory/README.md`
