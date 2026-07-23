---
name: string-diagram
description: >
  Convert resource equations into Mermaid string diagrams. Use when the user has
  typed resource equations (A × B → C style) and wants a visual diagram, or when
  the user describes a pipeline of operations on typed artifacts and wants both
  the equations and the diagram. Also use when editing or extending an existing
  set of resource equations.
---

# String Diagram Skill

Generate Mermaid string diagrams from resource equations written in a
monoidal-category style. Operations become boxes, types become labeled
wires, cross products become multiple input wires, and the diagram is
isomorphic to the equations — given either representation, the other
can be derived mechanically.

## When to use

- User provides resource equations and wants a Mermaid diagram
- User describes a pipeline in prose and wants it formalized as equations + diagram
- User wants to edit, extend, or compose existing equation sets
- User asks for a "string diagram," "process flow," "resource flow," or "pipeline diagram"

## Core concept

A resource equation has the form:

```
inputs → outputs  [OperationName]  {annotations}
```

This is a morphism in a symmetric monoidal category:
- **Types** (objects) are named text artifact kinds
- **Operations** (morphisms) are named transformations
- **×** is the monoidal product (multiple inputs)
- **+** is the coproduct (multiple outputs, e.g., accepted + rejected)
- **Catalytic inputs** participate but aren't consumed (comonoid copy)
- **Discard outputs** are waste/byproducts (the "egg shells")
- **Feedback** connects an output back to an earlier input (traced monoidal structure)

## Equation syntax

```
# Comment lines start with #

# Simple: one input, one output, named operation
A → B  [Transform]

# Cross product: multiple inputs
A × B × C → D  [Combine]

# Parentheses group (associativity only, no semantic effect)
(A × B) × C → D  [Combine]

# Coproduct output: multiple outputs
A → B + C  [Split]

# Annotations: catalytic, discard, feedback
A × B → C  [Op]  {catalytic: B}
A → B + C  [Op]  {discard: C}
A → B      [Op]  {feedback: B→X}

# Combined annotations (semicolon-separated)
A × B → C + D  [Op]  {catalytic: B; discard: D}

# Spider topology (fan/funnel duality)
A → B + C  [Diverge]  {spider: fan}
A × B → C  [Converge]  {spider: funnel}
```

**Spider annotations**: Mark operations as `fan` (one-to-many, divergent) or
`funnel` (many-to-one, convergent) to render them with distinct shapes and
colors. Fan operations get a trapezoid shape (blue); funnel operations get an
inverted trapezoid (green). Use for the fan/funnel duality from
`palgebra/duality-and-composition.md`. Annotations on continuation lines
(indented, containing `{`) are joined to the previous equation automatically.

**Type names:** lowercase hyphenated identifiers (e.g., `experience-reports`, `candidates-long-list`)

**ASCII fallbacks:** `*` for `×`, `->` for `→`

## Diagram conventions

The generated Mermaid diagram follows string-diagram style:

| Concept | Equation | Diagram |
|---------|----------|---------|
| Operation | `[Name]` | Dark box node |
| Source type (raw input) | Appears only as input | Light rounded node on left |
| Intermediate type | Produced and consumed | Labeled wire between operation boxes |
| Catalytic input | `{catalytic: X}` | Dashed wire (not consumed) |
| Waste/discard | `{discard: X}` | Red sink node |
| Feedback loop | `{feedback: X→Y}` | Wire from output back to input node |
| Cross product | `A × B` | Multiple wires entering one box |
| Coproduct | `A + B` | Multiple wires leaving one box |
| Fan spider | `{spider: fan}` | Trapezoid node (blue) — one-to-many topology |
| Funnel spider | `{spider: funnel}` | Inverted trapezoid node (green) — many-to-one topology |

## How to use

### Step 1: Write or receive equations

If the user provides equations, save them to a `.txt` file. If the user
describes a pipeline in prose, formalize it as equations first, confirm
with the user, then proceed.

### Step 2: Run the converter

```bash
python /path/to/resource_equations_to_mermaid.py equations.txt -o flow.mermaid
```

Options:
- `--direction LR` (default) — left to right flow
- `--direction TD` — top to bottom flow
- `-o file.mermaid` — write to file (default: stdout)

### Step 3: Deliver both representations

Always provide the user with **both** the equations file and the Mermaid
file. They are isomorphic — this is the point. The user should be able
to edit either one and regenerate the other.

## Extending and composing

To add an equation to an existing set:
1. Append the new equation line to the `.txt` file
2. Re-run the converter
3. The diagram updates mechanically

To compose two pipelines (e.g., pipeline A feeds into pipeline B):
1. Concatenate the equation files
2. Ensure the output types of A match the input types of B (same names)
3. The converter will automatically wire them together

## Validation

The converter classifies every type as source, intermediate, sink, or
catalytic by analyzing the equation set. If a type appears as input but
is never produced and isn't a declared source, that's a gap. If a type
is produced but never consumed and isn't a declared sink, that's a
dangling output. The user should review these.

## Example: Lemon Meringue Pie

```
egg → yolk + white + egg-shells  [SeparateEgg]  {discard: egg-shells}
lemon × butter × sugar × yolk → lemon-filling + lemon-peel + butter-wrapper  [MakeLemonFilling]  {discard: lemon-peel, butter-wrapper}
white × sugar → meringue  [MakeMeringue]
crust × lemon-filling → unbaked-lemon-pie  [FillCrust]
unbaked-lemon-pie × meringue → unbaked-pie  [AddMeringue]
```

This produces a diagram isomorphic to a standard process flow diagram
for making lemon meringue pie, with ingredients as source nodes,
operations as boxes, and waste (egg shells, lemon peel, butter wrapper)
as red sink nodes.

## Files

- `SKILL.md` — this file
- `resource_equations_to_mermaid.py` — the converter (Python 3.7+, no dependencies)
- `ai-study-equations.txt` — the AI study pipeline equations
- `lemon-pie-equations.txt` — the lemon meringue pie equations
- `decision-monad-equations.txt` — the fan→funnel deliberated choice pipeline (demonstrates spider annotations)
