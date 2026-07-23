---
name: committee
description: >
  Run an adversarial committee deliberation using the bundled roster to
  explore decision spaces, surface assumptions,
  and map trade-offs. Every run writes a deliberation record to
  the situation-directory deliberations folder (00-04 files). Use when the user types
  '/committee [topic]' or asks for a committee deliberation.
license: MIT
allowed-tools:
  - codex
  - claude-code
  - opencode
metadata:
  author: cyberneutics
  version: 0.1.0
  tags:
    - deliberation
  tested-tools:
    - claude-code
---

# Committee Deliberation Skill

Simulate a structured adversarial committee deliberation using the Cyberneutics
methodology. The committee explores problem spaces through genuine conflict
rather than convergent consensus, surfacing assumptions, trade-offs, and
blind spots.

## Situation resolution

The skill determines where to write output using this precedence:

1. **`--situation <path>`** ??" if the user provides this on invocation, use `<path>` as the situation directory directly.
2. **Config file** ??" read `situations_root` from `.agents/committee-config.yml` (path relative to repo root), then append `<topic-slug>/`.
3. **Default** ??" use `~/situations/<topic-slug>/`.

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
- Write deliberation output to `<situation-dir>/deliberations/`.
- **Auto-detect scenarios**: If `<situation-dir>/scenarios/` exists and contains `02-scenarios.md`, automatically operate in scenario-aware mode (see below). The user can still provide an explicit `scenario_context:` to override this.

**Rosters** (`roster.md`) are bundled with this skill ??" they are part of the methodology, not the situation output.

## When to use

- User types `/committee [topic/question]`
- User asks to "run a committee on [X]" or "deliberate on [Y]"
- User asks for **remediation** (e.g. "committee respond to evaluation for `<situation-dir>`" or "/committee remediation --situation `<path>`") when an evaluation scored below threshold and the committee should address the critique
- Complex sociotechnical problems where single perspectives miss important angles
- Decisions with competing values, unclear trade-offs, or political dimensions
- Situations where "what are we missing?" matters more than "what's the answer?"
- **After `/scenarios`**: When the same situation directory already contains scenarios, the committee auto-detects them and deliberates across previously generated scenarios (the deliberated choice workflow). Or provide `scenario_context:` explicitly.

## The Committee Roster

**Read the roster from `roster.md`** (bundled with this skill). That file contains the full character definitions: propensities, key questions, what each catches, failure modes, calibration examples (good/bad), interaction dynamics, and voice notes.

If `roster.md` does not exist or is unreadable, tell the user and stop. Do not fall back to a hardcoded roster.

## What the skill does

When invoked, the skill:

1. **Reads the roster** from `roster.md` to get character definitions, interaction dynamics, and voice notes.
2. **Resolves the situation directory** (see Situation resolution above) and **creates the deliberation record directory** `<situation-dir>/deliberations/`, writing 00-charter.md, 01-roster.md, 01-convening.md.
3. **Initializes the committee** with all characters and their propensities
4. **Presents the problem** as stated by the user (or prompts for clarification if vague)
5. **Generates initial perspectives** from each character (2-3 paragraphs each)
6. **Facilitates structured debate** with characters responding to each other
7. **Writes 02-deliberation.md** (full transcript) and **03-resolution.md** (decision, votes, summary).
8. **Surfaces key insights** (optionally inline): assumptions, trade-offs, evidence requirements, decision space map, recommended next steps.

The canonical output is the **deliberation record directory** (00??"04). The substance is not consensus??"it's a **map of the decision space** showing what's at stake, what's uncertain, and what different framings reveal or obscure.

## Scenario-aware mode (deliberated choice)

When the situation directory contains scenarios, or the user provides an explicit `scenario_context:`, the committee operates in **scenario-aware mode** ??" the convergent half of the fan??'funnel composition.

**How to detect**: Either (a) the resolved `<situation-dir>/scenarios/` contains `02-scenarios.md` (auto-detected), or (b) the user includes `scenario_context: <path>` in their invocation, or (c) the user says "deliberate across the scenarios in [path]."

**What changes**:

1. **Read the scenario set**: Read `02-scenarios.md` and `00-situation.md` from the specified directory. Extract scenario titles, narrators, assumptions, and key implications.
2. **Charter includes scenario context**: The `00-charter.md` gets additional fields:
   ```yaml
   scenario_context: "<situation-dir>/scenarios/"
   scenarios_summary:
     - id: 1
       title: "scenario title"
       narrator: "Continuity"
       key_assumption: "the distinguishing assumption"
     - id: 2
       title: "scenario title"
       narrator: "Disruption"
       key_assumption: "the distinguishing assumption"
   ```
3. **Opening statements engage with scenarios**: Each character's opening must reference at least 2 scenarios from their propensity. Maya asks which scenario hides the worst political dynamics; Vic demands evidence for or against each scenario's assumptions; etc.
4. **Debate ranges across futures**: The structured debate considers which futures to optimize for, which to survive, and which to dismiss ??" not just which perspective is right.
5. **Resolution is scenario-aware**: The resolution distinguishes:
   - **Robust actions**: commitments that make sense across most scenarios
   - **Scenario-dependent actions**: contingent commitments with trigger conditions
   - **Monitoring plan**: early warning signs to watch (from the scenarios)
   - **Dismissed futures**: which scenarios the committee judged implausible, with justification

**What stays the same**: Roster, deliberation requirements, intervention patterns, record directory structure, evaluation/remediation flow ??" all unchanged. Scenario-aware mode enriches the input; it does not change the committee's process.

**Backward compatibility**: If no `scenario_context` is provided, the committee operates exactly as before. The scenario-aware fields in the charter are optional.

For the full workflow, see `references/deliberated-choice-workflow.md`.

## Deliberation requirements

The skill enforces these constraints:

- **Genuine conflict**: Characters must disagree from their propensities, not converge to diplomatic consensus
- **Character consistency**: Each stays true to their epistemic stance throughout
- **Evidence proportionality**: Claims require evidence proportional to stakes
- **Explicit reasoning**: No skipped steps in reasoning chains
- **Named trade-offs**: Specific costs/benefits, not vague "pros and cons"

## Standard deliberation format

### Phase 1: Initial Perspectives

Each character provides their take on the problem (2-3 paragraphs):
- What concerns them most
- What assumptions they see
- What questions need answering
- What evidence would change their view

### Phase 2: Structured Debate

Characters respond to each other:
- Challenge claims lacking evidence
- Question hidden assumptions
- Identify missed perspectives
- Force explicit trade-off reasoning

### Phase 3: Synthesis

Not consensus, but clarity on:
- What perspectives are in tension and why
- What trade-offs are unavoidable
- What evidence would resolve key uncertainties
- What the decision actually optimizes for

## Problem framing guidance

**If user provides vague problem**: Prompt for specificity before running deliberation

Good problem framing includes:
- **Decision to be made**: Specific, not abstract
- **Current situation**: Context needed to understand stakes
- **Constraints**: Time, budget, resources, dependencies
- **Why it's hard**: Competing values, past failures, uncertainty

**Examples:**

Bad: "Should we hire more people?"

Good: "Should we hire two junior engineers or one senior engineer by end of month, given $X budget, tight Q3 deadlines, and limited mentoring bandwidth?"

## Intervention patterns

The skill should watch for and correct these failure modes during generation. When a failure mode is detected, apply the corresponding intervention internally before continuing:

**Too polite** ??" Characters are deferential ("Maya raises good points, but..."), hedging, or converging to comfortable agreement. This is the most common failure mode.

Intervention: Force each character to argue AGAINST the emerging consensus from their propensity. For each character in the roster, formulate a challenge based on their documented propensity and key question.

**Evidence-free claims** ??" A character asserts something without support.

Intervention: The evidence-focused character objects. The claiming character must either:
- Provide specific evidence (patterns, data, instances ??" not speculation)
- Specify what evidence would confirm or refute the claim
- Withdraw or soften the claim to "hypothesis worth testing"

If the evidence-focused character is the one making an unsupported claim, another character should challenge.

**Vague trade-offs**: Demand specific costs/benefits, not abstract "advantages"

**Premature consensus**: If everyone agrees too easily, something is being swept under the rug??"dig harder

**Circular debate**: If repeating arguments, summarize what's been established and identify what's still at stake

## Usage patterns

### Basic invocation
```
/committee Should we adopt microservices architecture?
```

The skill should then:
1. Prompt for any missing context if needed
2. Run deliberation with standard format
3. Deliver mapped decision space

### With context provided
```
/committee [detailed problem description with constraints, history, etc.]
```

The skill recognizes sufficient context and proceeds directly to deliberation.

### With situation directory (deliberated choice)
```
/committee --situation ../situations/acquisition-offer What should we do about the acquisition offer?
```

If the situation directory contains scenarios, auto-detects them and runs in scenario-aware mode. Each character engages with the scenarios from their propensity. The resolution distinguishes robust actions from scenario-dependent ones.

### With explicit scenario context
```
/committee [decision question] scenario_context: <path-to-scenarios-dir>/
```

Reads the scenario set from the specified directory. Use when the scenarios are in a different location than the situation directory.

### Scoped invocation
```
/committee quick [topic]
```
Run abbreviated version (1 paragraph per character, focus on key tensions)

```
/committee rigorous [topic]
```
Run extended version with explicit Robert's Rules structure, multiple rounds

## Output format

After deliberation, provide:

**KEY TENSIONS IDENTIFIED:**
- [Tension 1]: [Why these perspectives conflict]
- [Tension 2]: [What's being traded off]

**ASSUMPTIONS SURFACED:**
- [Assumption 1]: [Which character(s) identified this]
- [Assumption 2]: [Why it matters]

**EVIDENCE REQUIREMENTS:**
- [What data/evidence would resolve key uncertainties]
- [What predictions are testable]

**DECISION SPACE MAP:**
Not "do this" but "if you optimize for X, you sacrifice Y, and here's what each character thinks matters most"

**RECOMMENDED NEXT STEPS:**
- [Information gathering needs]
- [Clarifying questions for stakeholders]
- [Small experiments to test key assumptions]

## Integration with other artifacts

The committee skill can reference:

- **Previous deliberations**: Other situation directories under the situations root; if a similar problem was deliberated before, note lessons learned from those records
- **Character propensity reference**: `references/character-propensity-reference.md` for detailed character calibration
- **Setup templates**: `references/committee-setup-template.md` for advanced customization
- **Main technique doc**: `references/adversarial-committees.md`

## Deliberation record directory (always)

Every committee run writes a deliberation record to a dedicated directory. There is no single-file or inline-only mode??"the directory is the canonical output.

**Location:** `<situation-dir>/deliberations/` (see **Situation resolution** above for how `<situation-dir>` is determined).

**Topic-slug:** Derive from the topic: lowercase, replace spaces with `-`, remove or replace special characters. Examples: "Should we adopt microservices?" ??' `microservices-adoption`; "Is the author a crackpot?" ??' `is-author-crackpot`. The slug is used to name the situation directory when one isn't specified via `--situation`.

**Phased file production:** Create the directory and write files in order.

1. **Before deliberation** (from topic + any user context):
   - **00-charter.md** ??" Markdown with YAML front matter. `charter:` with `goal`, `context` (2??"4 sentences), `success_criteria` (list), `exit_conditions` (list), `deliverable_format: "Resolution Artifact + Decision Space Map"`.
   - **01-roster.md** ??" Copy the YAML front matter from `roster.md`. This records which roster was used for this deliberation.
   - **01-convening.md** ??" Markdown: Date, Selection strategy (e.g. "Standard roster from roster.md"), Rationale (why this roster: diversity, tensions, coverage), Composition notes (key productive tensions and alliances from the roster's interaction dynamics), Outcome ("Committee convened. See 01-roster.md."). **Optional (evaluation feedback loop):** A short "Remediation parameters" section with **remediation_threshold** (default 13; pass if sum of five rubric scores ??? this) and **max_remediation_rounds** (default 2), if this deliberation should use non-default values.

2. **During/after deliberation:**
   - **02-deliberation.md** ??" Full transcript in this structure:
     - Header: "Phase 2: Deliberation" with topic and protocol (Robert's Rules).
     - "Opening Statements" ??" one subsection per roster member, 2??"3 paragraphs each.
     - "Initial Positions Summary" ??" table: Member | Stance | Confidence | Key Concern.
     - "Key Tensions Identified" ??" numbered list.
     - "Round 1", "Round 2", ??? ??" Chair + member exchanges; after each round, "Round N Analysis" (emerging consensus, new tension, status, next).
     - "Final Consensus" ??" bullet list; status: DELIBERATION COMPLETE.
     - Then the standard output blocks: **KEY TENSIONS IDENTIFIED**, **ASSUMPTIONS SURFACED**, **EVIDENCE REQUIREMENTS**, **DECISION SPACE MAP**, **RECOMMENDED NEXT STEPS**, and if applicable **VERDICT** or **CONCLUSION**.

3. **After synthesis:**
   - **03-resolution.md** ??" Markdown with YAML front matter. From Final Consensus and DECISION SPACE MAP / VERDICT. Structure: `resolution:` with `date` (YYYY-MM-DD), `topic`, `outcome` (PASSED | DEFERRED | NO_CONSENSUS), `decision` (one line), `summary` (paragraph), optional `details`, optional `implementation_plan` (list of action/description), `votes` (one entry per roster member: YES | NO | ABSTAIN or conditional text), `signatures` (chair: "Committee (Cyberneutics)", ratified_by: "User").

After writing the record, you may summarize the decision space map (KEY TENSIONS, RECOMMENDED NEXT STEPS) inline for the user's convenience; the authoritative output remains the directory.

**Reference:** The deliberation record schema is documented inline above.

## Remediation mode (Committee respond to evaluation)

When the user or workflow invokes the committee for **remediation** (e.g. "committee respond to evaluation for `<situation-dir>`" or "/committee remediation --situation `<path>`" or "run a remediation round for this deliberation"):

1. **Resolve the situation directory** (via `--situation`, config, or the path the user provides) and locate `<situation-dir>/deliberations/`.
2. **Read:** 00-charter.md, 02-deliberation.md, and the **latest evaluation file** (04-evaluation-1.md, or 06-evaluation-2.md if the first remediation already ran, or 08-evaluation-3.md if two remediations exist). From the evaluation file use `transcript_review`: rubric scores, biggest_gaps, recommendations.
3. **Check:** If 05-remediation-1.md and 07-remediation-2.md already exist, do not run again (max 2 remediation rounds). Tell the user the deliberation has already had two remediation rounds.
4. **Produce:**
   - **Remediation file:** 05-remediation-1.md (or 07-remediation-2.md if 05 already exists). Content: frame the evaluation as a motion to recommit; for each recommendation in the evaluation, state accept / reject with reason / amend, and what the committee will add or change; then summarize the new round you will add.
   - **Append to 02-deliberation.md:** A new section "## Response to evaluation (motion to recommit)" and "## Round 2: [topic]" (or "Round 3" if 07-remediation-2.md). Include the committee's point-by-point response and the new round of debate addressing the evaluator's recommendations.
   - **Update 03-resolution.md** if the resolution or consensus changed.
5. **After writing:** Suggest running `/review` again on the directory; the review skill will write to 06-evaluation-2.md (or 08-evaluation-3.md).

## Customization options

**Standard roster is default**, but user can request modifications:

```
/committee [topic] with: [modified character focus]
```

Example:
```
/committee architecture decision with:
Maya as security focus, Vic as performance focus
```

**Scaling:**
- Default: All roster members
- `/committee quick [topic]`: 3 characters (the evidence and systems characters, plus one contextual)
- `/committee extended [topic]`: Full roster + domain expert

## Common scenarios

**Strategic decisions**: Full roster, emphasis on the political-awareness and values-guardian characters

**Technical decisions**: Focus on the institutional-memory, evidence, and systems characters

**Hiring decisions**: Full roster, with the institutional-memory character emphasizing past hiring outcomes, the systems character on team dynamics

**Partnership decisions**: Emphasize the political-awareness character (due diligence), values-guardian (alignment), evidence character (contract terms)

**Process changes**: Emphasize the systems character (second-order effects) and the institutional-memory character (past process failures)

## What success looks like

After running the committee, the user should be able to articulate:

- What each perspective was worried about
- Why those worries are in tension
- What evidence would favor each interpretation
- What they're choosing to optimize for and what downside they're accepting
- What early warning signs would indicate they chose wrong

**The committee doesn't tell you what to do**??"it shows you what you're actually deciding.

## Implementation notes

**Character voice**: Maintain consistency with the voice notes in `roster.md`. Each character should sound like a competent professional operating from their propensity ??" never a caricature.

**Debate dynamics**: Force genuine conflict
- Characters should interrupt/challenge each other
- "I see your point" ??' too polite, push harder
- Evidence-free assertion ??' the evidence-focused character objects
- Missing system dynamics ??' the systems-focused character points out

**Length calibration**:
- Quick: ~500 words total (1 para per character)
- Standard: ~1500 words (2-3 paras per character + debate)
- Rigorous: ~3000+ words (full structured debate with multiple rounds)

## Files reference

- **Operational roster**: `roster.md`
- **Extended character reference**: `references/character-propensity-reference.md`
- **Setup templates**: `references/committee-setup-template.md`
- **Main technique doc**: `references/adversarial-committees.md`
- **Robert's Rules protocol**: `references/roberts-rules-forcing-function.md`
- **Deliberated choice workflow**: `references/deliberated-choice-workflow.md` ??" composing fan??'funnel
- **Lesson extraction**: `references/lesson-extraction-template.md`

## Example invocation flow

**User**: `/committee Should we migrate to microservices?`

**Skill response**:
```
I'll run a committee deliberation on microservices migration. To ensure quality 
deliberation, can you provide:

- Current architecture context (monolith size, team size)
- Timeline/urgency
- Past attempts or discussions on this
- Why you're considering this now

Or I can proceed with general deliberation if you'd like to keep it abstract.
```

**User provides context**

**Skill**: Reads roster from `roster.md`, resolves the situation directory, runs full deliberation with all roster members. With the standard roster, this surfaces things like:
- The institutional-memory character notes a prior failed attempt and specific reasons
- The political-awareness character questions if the tech lead is empire-building
- The values-guardian asks if this serves actual mission or is resume-driven
- The evidence character demands specific performance/scaling data justifying change
- The systems character maps how this affects team structure and future hiring

**Result**: User realizes the question isn't "should we use microservices" but "have the conditions that caused 2019 failure actually changed, and are we solving the right problem?"

## Final notes

- **This is not consensus generation**??"it's decision space exploration
- **Characters are tools**, not realistic people??"use them instrumentally
- **Conflict is the feature**, not a bug
- **The map is not the territory**??"deliberation informs but doesn't make the decision

When the user finally decides, they do so with eyes open to trade-offs, not because the AI told them what to do, but because they understand what they're choosing and why.

