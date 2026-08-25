---
name: scenarios
description: >
  Run divergent scenario generation (the fan operation) using the roster
  defined in .agents/skills/scenarios/roster.md. Takes a situation description and
  produces a set of distinct narrative scenarios exploring different
  possible futures. Every run writes a scenario record to
  <situation-dir>/scenarios/ (00–03 files). Use when the user types
  '/scenarios [situation]' or asks for scenario generation.
---

# Scenario Generation Skill

Generate a set of divergent narrative scenarios from an ambiguous situation.
Each scenario is narrated independently by a character from the scenario roster,
exploring a distinct possible future under stated assumptions. The output is
a **scenario set** — not a forecast, not a probability distribution, but a
collection of internally consistent narratives that collectively span the
space of plausible futures.

This is the **fan** operation — the divergent half of the fan/funnel duality
formalized in `palgebra/duality-and-composition.md`. The convergent half is
the committee (`/committee`). The composition — fan then funnel — is the
**deliberated choice** pipeline.

## Situation resolution

The skill determines where to write output using this precedence:

1. **`--situation <path>`** — if the user provides this on invocation, use `<path>` as the situation directory directly.
2. **Config file** — read `situations_root` from `.agents/cyberneutics-config.yaml` (path relative to repo root). **History**: this repo unified the three skills' config on `.agents/cyberneutics-config.yaml` (2026-07-23); the upstream skills library adopted the same convention on 2026-08-21 (pragsmike/skills@88c5bf2), so this is no longer a divergence. Then append `<topic-slug>/`.
3. **Default** — use `~/situations/<topic-slug>/`.

Once the situation directory is resolved:
- Create the directory if it doesn't exist.
- If `situation.md` doesn't exist, create it with this template:
  ```yaml
  ---
  situation:
    created: YYYY-MM-DD
    topic: "short topic description"
    slug: "topic-slug"
  ---

  # [Topic]

  [Narrative description from the user's input]
  ```
- Write scenario output to `<situation-dir>/scenarios/`.

**Rosters** (`.agents/skills/scenarios/roster.md`) are always read from this repo — they are part of the methodology, not the situation output.

## When to use

- User types `/scenarios [situation description]`
- User asks to "explore possible futures for [X]" or "generate scenarios for [Y]"
- The problem's ambiguity is primarily about **what might happen** rather than
  **what to do given what we know**
- Before a `/committee` run, when the committee should deliberate across explored
  futures rather than a single framing
- When "what are we missing?" means "what futures haven't we imagined?" rather
  than "what perspectives haven't we heard?"

## When NOT to use

- The decision is well-framed and the question is which option to choose →
  use `/committee` directly
- The user wants adversarial stress-testing of a specific proposal → `/committee`
- The user wants to evaluate an existing plan → `/review` or `/committee`

## The Scenario Roster

**Read the roster from `.agents/skills/scenarios/roster.md`.** That file contains the
core character definitions (4 narrative lenses) and the extension mechanism
for domain-specific characters.

If `.agents/skills/scenarios/roster.md` does not exist or is unreadable, tell the user
and stop. Do not fall back to a hardcoded roster.

## What the skill does

When invoked, the skill:

1. **Reads the roster** from `.agents/skills/scenarios/roster.md` to get core characters
2. **Parses extensions** if the user provided a `with:` clause
3. **Frames the situation** (Phase 0) — structures the user's input into a
   situation with explicit ambiguity markers
4. **Creates the scenario record directory** and writes pre-generation files
5. **Generates scenarios** independently per character (Phase 1)
6. **Assesses coverage** across divergence axes (Phase 2–3)
7. **Flags gaps** if the scenario set has blind spots

### Resource equation

```
situation × scenario-roster × scenario-parameters → scenario-set  [Fan]
  {catalytic: scenario-roster, scenario-parameters}
```

The roster and parameters shape the output without being consumed.

## Situation framing guidance

**If the user provides a vague situation**: Prompt for specificity before
generating. A well-framed situation includes:

- **What's uncertain**: The ambiguities that scenarios should explore
- **What's at stake**: Why this situation matters, what depends on it
- **Time horizon**: How far into the future scenarios should project
- **Key actors**: Who or what has agency in this situation

**If the user provides a rich description**: Extract the above elements and
confirm before proceeding.

**Examples:**

Bad: "What happens with AI?"

Good: "Our 50-person B2B SaaS company has been approached by a larger
competitor about acquisition. The offer is 3x revenue. We have 18 months
of runway. The founding team is split — CTO wants to sell, CEO wants to
raise Series B. Key uncertainty: will the market consolidate (favoring
acquisition) or fragment (favoring independence)?"

## Scenario parameters

Parameters constrain the fan's divergence. They can be user-specified or
defaulted:

| Parameter | Default | Description |
|-----------|---------|-------------|
| **Divergence axes** | Derived from situation framing | The dimensions along which scenarios should differ (e.g., "market consolidation vs. fragmentation", "regulatory tightening vs. loosening") |
| **Time horizon** | 2–3 years | How far into the future each scenario projects |
| **External shocks** | None (characters generate their own) | Specific events to include (e.g., "key person departure", "regulatory change") |
| **Scenario count** | One per character (4 core + extensions) | Can be increased for finer coverage |

## Scenario record directory

Every `/scenarios` run writes a record to the situation's scenarios directory.

**Location:** `<situation-dir>/scenarios/` (see **Situation resolution** above for how `<situation-dir>` is determined).

**Topic-slug:** Derive from the situation: lowercase, replace spaces with `-`,
remove special characters. Example: "acquisition offer from competitor" →
`acquisition-offer`. The slug is used to name the situation directory when one
isn't specified via `--situation`.

**Files, in order:**

### 00-situation.md

Markdown with YAML front matter. The framed situation.

```yaml
---
situation:
  date: YYYY-MM-DD
  topic: "short description"
  uncertainties:
    - "uncertainty 1"
    - "uncertainty 2"
  stakes: "what depends on this"
  time_horizon: "2-3 years"
  key_actors:
    - "actor 1"
    - "actor 2"
  raw_input: "the user's original text"
---
```

Body: narrative framing of the situation (2–3 paragraphs).

### 01-roster.md

Copy the YAML front matter from `.agents/skills/scenarios/roster.md`, plus any
user-specified extensions. Records which roster was used for this run.

### 01-parameters.md

Markdown with YAML front matter recording the parameters used.

```yaml
---
parameters:
  divergence_axes:
    - axis: "axis name"
      poles: ["pole A", "pole B"]
    - axis: "axis name"
      poles: ["pole A", "pole B"]
  time_horizon: "2-3 years"
  external_shocks: []
  scenario_count: 4
---
```

Body: brief rationale for the divergence axes chosen (why these axes capture
the situation's key uncertainties).

### 02-scenarios.md

The scenario set. YAML front matter with index metadata, then one section
per scenario.

```yaml
---
scenario_set:
  situation: "<topic-slug>"
  count: 4
  scenarios:
    - id: 1
      narrator: "Continuity"
      title: "short title"
      assumption_set: ["assumption 1", "assumption 2"]
    - id: 2
      narrator: "Disruption"
      title: "short title"
      assumption_set: ["assumption 1", "assumption 2"]
    # ...
---
```

**Each scenario section** contains:

- **Title**: a memorable name for this future (not "Scenario 1")
- **Narrator**: which character generated it
- **Assumptions**: the specific assumptions this scenario makes (stated explicitly)
- **Narrative** (3–5 paragraphs): a coherent story of how this future unfolds,
  told from the narrator's worldview. Should be vivid and specific — names,
  numbers, dates, concrete events — not abstract hand-waving.
- **Key implications**: what this future means for the decision at hand
  (2–3 bullets)
- **Early warning signs**: observable signals that would indicate this future
  is materializing (2–3 bullets)

**Scenario generation rules:**

1. Each character narrates **independently** — no awareness of other scenarios
2. Each scenario must be **internally consistent** — the narrative follows
   from its stated assumptions
3. Scenarios should be **distinct** — they should differ on at least one
   divergence axis
4. Scenarios should be **specific** — concrete events, not vague trends
5. Scenarios should be **plausible** — not necessarily probable, but not
   absurd. The value is in coverage, not likelihood.

### 03-assessment.md

Coverage assessment. Markdown with YAML front matter.

```yaml
---
assessment:
  date: YYYY-MM-DD
  scenario_count: 4
  coverage:
    axes_covered:
      - axis: "axis name"
        covered_by: [1, 3]
    axes_gaps: []
  sufficiency: ADEQUATE | GAPS_IDENTIFIED
  recommendations: []
---
```

Body: narrative assessment of the scenario set's coverage.

**Coverage checks:**

1. **Axis coverage**: Does at least one scenario explore each divergence axis?
2. **Quadrant coverage**: Are all four quadrants of the 2×2 (change × valence)
   represented?
3. **Blind spots**: Are there plausible futures that no scenario explores?
4. **Distinctness**: Are the scenarios genuinely different, or do they
   converge on similar narratives from different narrators?

**If GAPS_IDENTIFIED**: List specific gaps and recommend either:
- Generating additional scenarios (with guidance on which character or
  extension to use)
- Adjusting parameters (wider divergence axes, different time horizon)
- Proceeding with noted limitations

## Usage patterns

### Basic invocation

```
/scenarios Our main product line faces potential disruption from open-source alternatives
```

The skill frames the situation, generates scenarios with the core roster,
and assesses coverage. Output goes to the situation directory resolved via
config or default (`~/situations/<topic-slug>/scenarios/`).

### With explicit situation directory

```
/scenarios --situation ../situations/product-disruption Our main product line faces potential disruption from open-source alternatives
```

Writes output to `../situations/product-disruption/scenarios/`.

### With extensions

```
/scenarios [situation] with: Regulator (lens: regulatory-pressure, explores: how data privacy laws reshape the competitive landscape)
```

Generates core scenarios plus one additional from the Regulator lens.

### With explicit parameters

```
/scenarios [situation]
  axes: market-consolidation, regulatory-environment
  horizon: 5 years
  shocks: key-competitor-acquisition, new-EU-regulation
```

### Feeding into committee (the deliberated choice)

After a `/scenarios` run, the scenario set can serve as input to `/committee`.
If both skills target the same situation directory (via `--situation` or config),
the committee automatically detects existing scenarios:

```
/committee --situation ../situations/acquisition-offer What should we do about the acquisition offer?
```

Or explicitly specify the scenario context:

```
/committee [decision question] scenario_context: <situation-dir>/scenarios/
```

The committee then deliberates across the generated scenarios — which futures
to optimize for, which to survive, which to dismiss. See
`artifacts/deliberated-choice-workflow.md` for the full workflow.

## Intervention patterns

Watch for and correct these failure modes during generation:

**Convergent scenarios** — Different narrators producing essentially the same
future with different labels.

Intervention: Check whether scenarios differ on stated divergence axes. If
two scenarios agree on all axes, regenerate one with explicit instructions
to explore the opposite pole of at least one axis.

**Abstract hand-waving** — Scenarios that describe trends rather than events.
"The market evolves" instead of "Company X launches product Y in Q3."

Intervention: Demand specifics. Each scenario needs at least: one named
event, one concrete consequence, one measurable outcome.

**Implausible extremes** — Scenarios so extreme they're not useful for
decision-making. (See Disruption's failure mode.)

Intervention: Trace the causal chain. Each step in the narrative should
follow plausibly from the previous step. Disconnect = implausibility.

**Missing assumptions** — Scenarios that don't state what they're assuming.

Intervention: Extract assumptions and state them explicitly before the
narrative. If the narrator can't state assumptions, the scenario is
underdetermined.

## What success looks like

After running `/scenarios`, the user should be able to:

- Name 3–4 genuinely different futures for their situation
- Articulate the assumptions that distinguish each future
- Identify which uncertainties matter most (the divergence axes)
- See early warning signs for each future
- Decide whether to proceed to `/committee` for deliberation or iterate
  with different parameters

**The scenarios don't predict the future** — they map the space of plausible
futures so the user can prepare for multiple possibilities and recognize
which one is materializing.

## Integration with other skills

| Skill | Relationship |
|-------|-------------|
| `/committee` | Downstream — scenario set feeds into committee deliberation (auto-detected when both target the same situation, or via explicit `scenario_context:`) |
| `/review` | Does not apply directly to scenarios (review evaluates committee transcripts) |
| `/string-diagram` | The fan operation can be visualized as a one-to-many spider |

## Files reference

- **Operational roster**: `.agents/skills/scenarios/roster.md`
- **Theoretical grounding**: `palgebra/duality-and-composition.md`
- **Composition workflow**: `artifacts/deliberated-choice-workflow.md`
- **Practitioner guide**: `artifacts/scenario-generation.md`
- **Committee skill** (the funnel): `.agents/skills/committee/SKILL.md`
