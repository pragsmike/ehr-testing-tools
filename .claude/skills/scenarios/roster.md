<!-- Copied verbatim from https://github.com/pragsmike/cyberneutics/blob/main/agent/scenario-roster.md (commit d2a475c), fetched 2026-07-23. Ruling: copy now, adapt at first real use — see notes/tools/prompts/2026-07-24-exp-sbom.md. Moved from agent/scenario-roster.md to here 2026-08-01 (migration session 1, item 6/7). -->
---
roster:
  roster_name: "Cyberneutics Scenario Roster"
  purpose: divergent_exploration
  core_size: 4
  extensible: true
  members:
    - name: Continuity
      role: baseline_extrapolator
      lens: continuity
    - name: Disruption
      role: break_finder
      lens: discontinuity
    - name: Opportunity
      role: upside_explorer
      lens: latent_potential
    - name: Constraint
      role: tightening_tracer
      lens: resource_limits
---

# Scenario Roster

This file is the operational roster for the `/scenarios` skill. The skill reads
it at invocation time to get the fixed core of narrative lenses. To change the
core composition, edit this file. For problem-specific extensions, see the
Extension Mechanism section below.

**This roster is distinct from the committee roster** (`.agents/skills/committee/roster.md`).
The committee roster creates convergent friction (characters argue).
The scenario roster creates divergent exploration (characters narrate
independently). Conflating them collapses the fan/funnel duality.

For the theoretical grounding, see `palgebra/duality-and-composition.md`
(Two Distinct Rosters).

---

## Design Principle

Each core member is a **narrative lens** — a worldview that generates a
distinct, internally consistent future from the same situation. They do not
argue with each other; they narrate independently. The divergence comes from
their different assumptions about what matters, what changes, and what persists.

The lenses are chosen to span the space of plausible futures along two axes:

- **Change axis**: Continuity ↔ Disruption (what stays the same vs. what breaks)
- **Valence axis**: Opportunity ↔ Constraint (what opens up vs. what closes down)

This 2×2 structure ensures the scenario set covers optimistic continuity,
pessimistic continuity, optimistic disruption, and pessimistic disruption —
the four quadrants that scenario planning literature (Schwartz, van der Heijden)
identifies as the minimum viable divergence.

---

## Core Character Definitions

### Continuity (Baseline Extrapolation)

- **Lens**: The future is the present, continued. Trends already in motion
  play out. Institutions adapt incrementally. No discontinuities.
- **Explores**: What happens if nothing surprising occurs? What does the
  default trajectory look like? Where does gradual change accumulate into
  significant consequences?
- **Asks**: "What if the current trajectory just... continues? What does
  year 3 look like if year 1 trends persist?"
- **Catches**: The non-obvious implications of business-as-usual; the slow
  boil that everyone ignores because no single moment is dramatic.
- **Failure mode**: Boring extrapolation that misses the point — describing
  "more of the same" without surfacing what accumulates or compounds.
- **Calibration**:
  - Bad Continuity: "Revenue grows 5% per year, team grows 10%, market stays stable. Everything is fine."
  - Good Continuity: "Revenue grows 5% per year but team grows 10% — by year 3 the burn rate exceeds growth. Nobody notices because each quarter looks fine. The crisis arrives as a cash crunch, not a strategic failure, and the org is unprepared because the narrative was always 'we're growing.'"

### Disruption (What Breaks)

- **Lens**: Something discontinuous happens. A key assumption fails. An
  external shock arrives. The rules change.
- **Explores**: What single point of failure, if it breaks, cascades into
  systemic change? What's the weakest load-bearing assumption? What external
  event would invalidate the current strategy?
- **Asks**: "What breaks first? What happens when it does?"
- **Catches**: Fragility hidden behind apparent stability; dependencies nobody
  tracks; cascading failures that simple risk analysis misses.
- **Failure mode**: Catastrophism — inventing implausible Black Swans for
  dramatic effect rather than tracing realistic failure cascades from
  identifiable vulnerabilities.
- **Calibration**:
  - Bad Disruption: "An asteroid destroys the data center. Alien technology makes our product obsolete. A global pandemic shuts down all operations."
  - Good Disruption: "Our largest customer (38% of revenue) is acquired by a competitor. Their new parent company has an in-house solution. We lose the contract with 90 days notice. The engineering team we hired specifically for that account has no other project. Meanwhile, the two smaller customers who were waiting for features we deprioritized for the big account have already started evaluating alternatives."

### Opportunity (What Opens Up)

- **Lens**: Latent potential actualizes. An unnoticed asset becomes valuable.
  A constraint lifts. Adjacent possibilities become accessible.
- **Explores**: What undervalued resources, relationships, or capabilities
  could become decisive advantages? What doors open if a key constraint
  changes? What adjacent territory is closer than it appears?
- **Asks**: "What's the best realistic outcome, and what would have to be
  true for it to happen?"
- **Catches**: Upside blindness — teams so focused on risk mitigation that
  they miss opportunities already within reach; assets nobody is leveraging.
- **Failure mode**: Wishful thinking — describing desirable outcomes without
  specifying the preconditions or plausible paths that would produce them.
- **Calibration**:
  - Bad Opportunity: "AI solves all our problems. The market doubles. A visionary leader transforms the culture."
  - Good Opportunity: "Three engineers built an internal tool for the support team that cuts ticket resolution time by 40%. Nobody outside support knows it exists. If productized, it addresses the same pain point our top 50 prospects cite in sales calls. The tool exists, the data exists, the market signal exists — what's missing is the decision to redirect 2 engineers for 3 months."

### Constraint (What Tightens)

- **Lens**: Resources contract. Regulations increase. Optionality narrows.
  The room to maneuver shrinks.
- **Explores**: What happens when budgets are cut, regulations tighten,
  talent leaves, or competitive pressure increases? Which current freedoms
  are we taking for granted? What does decision-making look like with
  fewer options?
- **Asks**: "What if we had half the budget, half the time, or half the
  team? What would we stop doing first — and what does that reveal about
  our actual priorities?"
- **Catches**: Hidden dependencies on favorable conditions; strategies that
  only work in abundance; the gap between stated priorities and revealed
  priorities under pressure.
- **Failure mode**: Generic pessimism — listing bad things without tracing
  how specific constraints reshape specific decisions and reveal actual
  priorities.
- **Calibration**:
  - Bad Constraint: "Budget cuts are bad. We'd have to fire people and cancel projects. Morale would suffer."
  - Good Constraint: "A 30% budget cut forces a choice: keep the platform team (long-term architecture) or the feature team (short-term revenue). The CEO says both matter equally, but the cut reveals which one actually gets protected. If the platform team is cut, we're betting that technical debt won't compound before the next funding round. If the feature team is cut, we're betting that existing customers won't churn before the platform investment pays off. The constraint doesn't create the tension — it makes a latent tension undeferrable."

---

## Extension Mechanism

The core roster spans general divergence. For domain-specific problems,
the user can add 1–2 **extension characters** when invoking `/scenarios`.

### How to specify extensions

In the `/scenarios` invocation, add a `with:` clause:

```
/scenarios [situation] with: Regulator (lens: regulatory-pressure, explores: how policy changes reshape the landscape)
```

Or multiple:

```
/scenarios [situation] with:
  Incumbent (lens: market-defense, explores: how established players respond to disruption)
  Regulator (lens: regulatory-pressure, explores: how new rules constrain or enable action)
```

### Extension character format

Each extension needs at minimum:
- **Name**: a label for the character
- **Lens**: the worldview it narrates from (one phrase)
- **Explores**: what it explores (one sentence)

The skill will flesh out the character's asks, catches, and failure mode
from the lens and exploration focus. The user does not need to provide
full character definitions.

### When to extend

Extend when the core roster's divergence axes miss a dimension critical
to the problem:

- **Policy/regulatory problems**: add Regulator
- **Market/competitive problems**: add Incumbent, Entrant, or Customer
- **Technical architecture problems**: add Legacy (existing system's inertia) or Scale (what happens at 10x)
- **Organizational problems**: add Culture (informal norms and resistance)

Do **not** extend to add epistemic stances (skepticism, optimism, caution) —
those belong in the committee roster. Scenario extensions should generate
*futures*, not *attitudes toward futures*.

---

## Interaction Mode

Unlike the committee roster, scenario characters do **not** interact.

| Property | Committee Roster | Scenario Roster |
|----------|-----------------|-----------------|
| Interaction | Adversarial debate | Independent narration |
| Characters | Argue with each other | Narrate alone |
| Divergence source | Conflicting epistemic stances | Different worldview assumptions |
| Convergence | Resolution through structured debate | None — convergence is the funnel's job |
| Output | Transcript (single document, multi-voice) | Scenario set (multiple documents, one voice each) |

Each scenario character produces a self-contained narrative. The narratives
are collected into a scenario set. The scenario set is then available as
input to the committee (the funnel) for adversarial deliberation — which
futures to optimize for, which to survive, which to dismiss.

---

## Relationship to Scenario Planning Literature

The core roster's design draws on:

- **Peter Schwartz** (*The Art of the Long View*): scenarios as narratives,
  not forecasts; value is in coverage, not probability.
- **Kees van der Heijden** (*Scenarios: The Art of Strategic Conversation*):
  the 2×2 matrix of driving forces as minimum viable divergence.
- **Shell scenario planning tradition**: scenarios as "memories of the future"
  that stretch mental models and surface hidden assumptions.

The extension mechanism draws on Cultural Theory's insight that worldviews
(individualist, egalitarian, hierarchist, fatalist) generate genuinely
different narratives about the same facts — but uses domain-specific lenses
rather than abstract cultural types, for actionability.
