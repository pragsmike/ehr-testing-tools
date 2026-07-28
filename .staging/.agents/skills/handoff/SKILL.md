---
name: handoff
description: >
  Generate a session handoff document for successor agents. Use when the user
  types '/handoff' or asks to create a handoff. Reads the previous handoff (if any)
  for continuity, generates a new handoff with today's date, saves it to
  .agents/handoffs/, and archives the previous one to .agents/handoffs/archive/.
license: MIT
allowed-tools:
  - codex
  - claude-code
  - opencode
metadata:
  author: cyberneutics
  version: 0.1.0
  tags:
    - automation
  tested-tools:
    - claude-code
---

# Handoff Skill

Generate effective session handoff documents that enable successor agents to
continue work on this repository, avoiding pitfalls encountered and building
on lessons learned that aren't captured in formal documentation.

## When to use

- User types `/handoff`
- User asks to "create a handoff" or "write a handoff document"
- End of a significant work session when continuity matters
- After completing major milestones or before known breaks

## What the skill does

1. **Reads previous handoff** from `.agents/handoffs/handoff-[YYYY-MM-DD].md` (if exists) to understand trajectory and maintain continuity
2. **Analyzes current session** to extract:
   - What was accomplished
   - What mistakes were made and lessons learned
   - What dead ends were explored
   - Current state of work (completed, in-progress, blocked)
   - Immediate next steps for successor
3. **Generates new handoff** following the structured template
4. **Saves** as `.agents/handoffs/handoff-[TODAY].md`
5. **Archives previous** handoff to `.agents/handoffs/archive/handoff-[OLD-DATE].md`

All steps happen automatically without confirmation prompts.

## Handoff document structure

The generated handoff follows this structure:

### Session Summary
- Duration and main accomplishments
- Original intent vs. actual outcome
- Key deliverables

### Mistakes and Lessons
- Specific mistakes made during session
- Root causes and how to avoid them
- Concrete examples

### Dead Ends Explored
- Approaches that didn't work and why
- What was learned from failures
- Conditions under which they might be worth revisiting

### Current State
- Completed this session
- In progress (with blockers)
- Not yet started but planned

### Immediate Next Steps
- Prioritized, actionable tasks
- Context for why each is ready
- Dependencies and blockers

### Open Questions and Decisions Needed
- Questions requiring human input
- Topics needing research/exploration

### Technical Notes
- Repository structure quirks
- Tools and workflow notes
- File management approach

### Watch-Outs for Successor
- Tasks that seem simple but aren't
- Hidden dependencies
- Quality standards

### Theoretical/Conceptual Notes
- Key insights from session
- Unresolved tensions
- How understanding evolved

### What Worked Well / What To Do Differently
- Collaboration patterns that were productive
- Process improvements needed
- Communication adjustments

### Context for Specific Files
- File-by-file notes on status, quirks, dependencies

### Session Metadata
- Agent, date, goals
- Continuation priority

## Key principles for generation

1. **Be specific**: "File Y has hidden dependency on Z because of reason R" not "Watch out for X"

2. **Capture the invisible**: What seemed obvious in context but won't be obvious to successor?

3. **Save time**: What would you want to know picking this up fresh? What did you learn the hard way?

4. **Be honest**: Mistakes and dead ends are valuable intelligence

5. **Think forward**: What enables successor to maintain momentum vs. starting from scratch?

6. **Prioritize ruthlessly**: Focus on what materially affects successor's effectiveness

7. **Maintain narrative continuity**: Reference previous handoff observations—did predicted issues materialize? Did recommended approaches work? What trajectory is the work following?

## Example bad vs. good entries

**Bad**: "Be careful with the evaluation rubrics"
- Not specific, not actionable

**Good**: "The evaluation-rubrics-reference.md has examples that reference other artifacts. If you change artifact names/structure, those examples need updating too. Caught this when we moved files and examples broke."
- Specific, explains why it matters, gives concrete scenario

**Bad**: "The user prefers good documentation"
- Everyone prefers good documentation, this says nothing

**Good**: "The user prefers examples over abstract explanation. When explaining theoretical concepts, noticed engagement increased significantly when we included concrete transcripts/examples rather than just describing patterns."
- Specific observation, actionable pattern

**Bad**: "Some tasks still need to be done"
- Obvious from the task list already

**Good**: "The config migration task is listed as 'pending' but the scope hasn't been defined beyond the title. Before starting, confirm: does this cover only the YAML schema change, or also updating all downstream consumers? Those are different tasks with very different scope."
- Identifies real ambiguity, suggests clarifying question, prevents wasted effort

## Implementation notes

**File naming**: `handoff-YYYY-MM-DD.md` using today's date

**Archive location**: `.agents/handoffs/archive/` — ensure directory exists before moving files

**Reading previous handoff**: If multiple handoffs exist in `.agents/handoffs/`, read the most recent (by date in filename). Don't read from `archive/` — those are historical.

**Continuity narrative**: The handoff should note:
- What the previous handoff predicted/recommended
- Whether those predictions were accurate
- What trajectory the work is following
- Evolution of understanding about user preferences
- Patterns that are becoming clearer over multiple sessions

**No redundancy**: Don't repeat what's already in repository documentation. The handoff is for meta-context, session-specific intelligence, and invisible knowledge.

**Tone**: Direct, practical, specific. Written for another AI agent picking up the work. Be frank about mistakes and unknowns.

## Usage

When user types `/handoff`, simply execute the skill:
1. Read `.agents/handoffs/handoff-*.md` (most recent if multiple)
2. Analyze the current session transcript
3. Generate comprehensive handoff document
4. Save as `.agents/handoffs/handoff-[TODAY].md`
5. Move previous handoff to `.agents/handoffs/archive/`
6. Confirm completion: "Handoff document created: .agents/handoffs/handoff-[TODAY].md (previous handoff archived)"

No intermediate confirmations or reviews—generate and save directly.
