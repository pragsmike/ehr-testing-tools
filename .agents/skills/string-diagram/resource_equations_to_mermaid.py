#!/usr/bin/env python3
"""
resource_equations_to_mermaid.py

Parse resource equations (typed monoidal category morphisms) and emit
a Mermaid flowchart in string-diagram style:
  - Operations are boxes (nodes)
  - Types are labeled wires (edges)
  - Cross products (×) become multiple input wires to one box
  - Catalytic inputs (dashed wires) are not consumed
  - Waste/discard outputs (red sinks) are rejected byproducts
  - Feedback loops connect outputs back to inputs
  - Spider annotations render fan/funnel topology with distinct shapes
  - Enrichment morphisms (metadata-only changes) render as outlined boxes
  - Gate/collapse operations (coproduct + discard) render as purple nodes

Input format (resource equations file):
  Lines of the form:
    A × B × C → D                    (anonymous operation)
    A × B → C  [OperationName]       (named operation)
    A × B → C  [Op] {catalytic: B}   (B is catalytic / not consumed)
    A → B + C  [Op] {discard: C}     (C is waste/rejected output)
    A → B      [Op] {feedback: B→X}  (B feeds back to input X)
    A → B + C  [Op] {spider: fan}    (one-to-many spider: trapezoid, blue)
    A × B → C  [Op] {spider: funnel} (many-to-one spider: inv. trapezoid, green)
    A × R → A  [Op] {enriches: ns}   (enrichment: payload unchanged, metadata updated)

  - Type names: hyphenated lowercase identifiers (e.g., experience-reports)
  - Cross product: × (unicode) or * (ascii fallback)
  - Arrow: → (unicode) or -> (ascii fallback)
  - Coproduct output: + separates multiple outputs
  - Parentheses group sub-products: (A × B) × C
  - Lines starting with # are comments
  - Blank lines are ignored

Usage:
  python resource_equations_to_mermaid.py equations.txt > flow.mermaid
  python resource_equations_to_mermaid.py equations.txt --direction TD > flow.mermaid
"""

from __future__ import annotations

import re
import sys
import argparse
from dataclasses import dataclass, field
from typing import Optional


# ---------------------------------------------------------------------------
# Data model
# ---------------------------------------------------------------------------

@dataclass
class Equation:
    """One resource equation: inputs → outputs, with metadata."""
    number: int
    inputs: list[str]           # flattened list of input type names
    outputs: list[str]          # primary outputs (left of +)
    operation: Optional[str]    # named operation, or None
    catalytic: set[str] = field(default_factory=set)   # input types that are not consumed
    discard: set[str] = field(default_factory=set)      # waste/rejected outputs
    feedback: dict[str, str] = field(default_factory=dict)  # output→input feedback mapping
    spider: Optional[str] = None  # 'fan' (one-to-many) or 'funnel' (many-to-one)
    enriches: Optional[str] = None  # namespace written by enrichment morphism


# ---------------------------------------------------------------------------
# Parser
# ---------------------------------------------------------------------------

def normalize(text: str) -> str:
    """Normalize unicode operators to ascii for parsing."""
    text = text.replace('×', '*').replace('→', '->').replace('→', '->')
    # Handle UTF-8 mangled versions (from pack files)
    text = text.replace('\u00d7', '*').replace('\u2192', '->')
    # Mojibake: UTF-8 bytes of × (C3 97) and → (E2 86 92) read as latin-1
    text = text.replace('\u00c3\u0097', '*')
    text = text.replace('\u00c3\u2014', '*')
    text = text.replace('\u00e2\u0080\u0093', '->')
    text = text.replace('\u00e2\u0086\u2019', '->')
    return text


def extract_types(expr: str) -> list[str]:
    """
    Extract type names from a product expression.
    Strips parentheses (they're grouping only — monoidal product is associative)
    and splits on *.
    """
    expr = expr.replace('(', '').replace(')', '')
    parts = [t.strip() for t in expr.split('*')]
    return [p for p in parts if p]


def parse_annotations(ann_str: str) -> tuple[set[str], set[str], dict[str, str], Optional[str], Optional[str]]:
    """Parse {catalytic: X, Y; discard: Z; feedback: W→V; spider: fan|funnel; enriches: ns}."""
    catalytic = set()
    discard = set()
    feedback = {}
    spider = None
    enriches = None

    ann_str = ann_str.strip().strip('{}')
    if not ann_str:
        return catalytic, discard, feedback, spider, enriches

    for clause in ann_str.split(';'):
        clause = clause.strip()
        if not clause:
            continue
        if ':' not in clause:
            continue
        key, val = clause.split(':', 1)
        key = key.strip().lower()
        vals = [v.strip() for v in val.split(',')]

        if key == 'catalytic':
            catalytic.update(vals)
        elif key == 'discard':
            discard.update(vals)
        elif key == 'feedback':
            for v in vals:
                arrow = v.replace('->', '→').replace('→', '→')
                if '→' in arrow:
                    src, dst = arrow.split('→', 1)
                    feedback[src.strip()] = dst.strip()
        elif key == 'spider':
            val_str = vals[0].lower() if vals else ''
            if val_str in ('fan', 'funnel'):
                spider = val_str
        elif key == 'enriches':
            enriches = vals[0] if vals else None

    return catalytic, discard, feedback, spider, enriches


def parse_equation(line: str, number: int) -> Optional[Equation]:
    """Parse one equation line into an Equation object."""
    line = normalize(line.strip())
    
    # Skip comments and blanks
    if not line or line.startswith('#'):
        return None
    
    # Extract annotation block {…} if present
    ann_str = ''
    ann_match = re.search(r'\{([^}]*)\}', line)
    if ann_match:
        ann_str = ann_match.group(1)
        line = line[:ann_match.start()].strip()
    
    # Extract operation name [Name] if present
    operation = None
    op_match = re.search(r'\[([^\]]+)\]', line)
    if op_match:
        operation = op_match.group(1).strip()
        line = line[:op_match.start()].strip()
    
    # Split on arrow
    if '->' not in line:
        return None
    
    lhs, rhs = line.split('->', 1)
    
    # Parse outputs (may have + for coproduct)
    output_parts = [t.strip() for t in rhs.split('+')]
    outputs = [p for p in output_parts if p]
    
    # Parse inputs
    inputs = extract_types(lhs)
    
    catalytic, discard, feedback, spider, enriches = parse_annotations(ann_str)

    return Equation(
        number=number,
        inputs=inputs,
        outputs=outputs,
        operation=operation,
        catalytic=catalytic,
        discard=discard,
        feedback=feedback,
        spider=spider,
        enriches=enriches,
    )


def parse_file(filepath: str) -> list[Equation]:
    """Parse a file of resource equations.

    Handles continuation lines: an indented line containing '{' is joined
    to the previous non-blank, non-comment line before parsing.
    """
    equations = []
    with open(filepath, 'r', encoding='utf-8') as f:
        raw_lines = f.readlines()

    joined = []
    for line in raw_lines:
        stripped = line.rstrip('\n')
        if stripped and stripped[0] in (' ', '\t') and '{' in stripped:
            if joined:
                joined[-1] += ' ' + stripped.strip()
                continue
        joined.append(stripped)

    for num, line in enumerate(joined, start=1):
        eq = parse_equation(line, num)
        if eq:
            equations.append(eq)
    return equations


# ---------------------------------------------------------------------------
# Analysis: classify types
# ---------------------------------------------------------------------------

def classify_types(equations: list[Equation]) -> tuple[set[str], set[str], set[str], set[str]]:
    """
    Classify every type as:
      - source: appears only as input (never produced by an operation)
      - sink: appears only as output discard
      - intermediate: produced and consumed
      - catalytic: explicitly marked as not consumed
    """
    all_inputs = set()
    all_outputs = set()
    all_catalytic = set()
    all_discard = set()
    
    for eq in equations:
        all_inputs.update(eq.inputs)
        all_outputs.update(eq.outputs)
        all_catalytic.update(eq.catalytic)
        all_discard.update(eq.discard)
    
    sources = all_inputs - all_outputs  # never produced → raw input
    sinks = all_discard
    intermediate = all_outputs & all_inputs  # produced and consumed
    
    return sources, sinks, intermediate, all_catalytic


# ---------------------------------------------------------------------------
# Mermaid generation
# ---------------------------------------------------------------------------

def slugify(name: str) -> str:
    """Turn a type/operation name into a valid Mermaid node ID."""
    return name.replace('-', '_').replace(' ', '_').replace('/', '_')


def generate_mermaid(equations: list[Equation], direction: str = 'LR') -> str:
    """Generate a Mermaid flowchart from parsed equations."""
    
    sources, sinks, intermediate, catalytic_types = classify_types(equations)
    
    lines = []
    lines.append(f'flowchart {direction}')
    lines.append('')
    
    # Collect all types and operations
    all_types = set()
    operations = []
    for eq in equations:
        all_types.update(eq.inputs)
        all_types.update(eq.outputs)
        if eq.operation:
            operations.append(eq.operation)
    
    # --- Source nodes (raw inputs) ---
    lines.append('    %% --- Source types (raw inputs, not produced by any operation) ---')
    for t in sorted(sources):
        slug = slugify(t)
        if t in catalytic_types:
            lines.append(f'    {slug}(["{t}"])')
        else:
            lines.append(f'    {slug}(["{t}"])')
    lines.append('')
    
    # --- Operation nodes ---
    lines.append('    %% --- Operations (boxes; spiders use distinct shapes) ---')
    for eq in equations:
        if eq.operation:
            slug = slugify(eq.operation)
            if eq.spider == 'fan':
                lines.append(f'    {slug}[/"{eq.operation}"\\]')
            elif eq.spider == 'funnel':
                lines.append(f'    {slug}[\\"{eq.operation}"/]')
            else:
                lines.append(f'    {slug}["{eq.operation}"]')
    lines.append('')
    
    # --- Sink nodes (waste/discard) ---
    if sinks:
        lines.append('    %% --- Waste / discard sinks ---')
        for t in sorted(sinks):
            slug = slugify(t) + '_sink'
            lines.append(f'    {slug}(["{t}"])')
        lines.append('')
    
    # --- Wires ---
    lines.append('    %% --- Wires (typed connections) ---')
    
    # Track which types are produced by which operation (for wiring intermediates)
    producer = {}  # type_name → operation_slug
    for eq in equations:
        if eq.operation:
            for out in eq.outputs:
                if out not in eq.discard:
                    # For enrichment morphisms, don't overwrite the upstream producer
                    # when the output type matches an input type (the artifact passes
                    # through unchanged — the upstream producer still "owns" it).
                    if eq.enriches and out in eq.inputs:
                        continue
                    producer[out] = slugify(eq.operation)
    
    for eq in equations:
        if not eq.operation:
            continue
        
        op_slug = slugify(eq.operation)
        
        lines.append(f'    %% Arrow {eq.number}: {eq.operation}')
        
        # Input wires
        for inp in eq.inputs:
            inp_slug = slugify(inp)
            is_catalytic = inp in eq.catalytic or inp in catalytic_types
            
            # If this type is produced by another operation, wire from that op
            if inp in producer:
                src = producer[inp]
            else:
                src = inp_slug
            
            if is_catalytic:
                lines.append(f'    {src} -. {inp} .-> {op_slug}')
            else:
                lines.append(f'    {src} -- {inp} --> {op_slug}')
        
        # Output wires — discard goes to sink
        for out in eq.outputs:
            if out in eq.discard:
                sink_slug = slugify(out) + '_sink'
                lines.append(f'    {op_slug} -- "{out}" --> {sink_slug}')
        
        # Feedback wires
        for src_type, dst_type in eq.feedback.items():
            dst_slug = slugify(dst_type)
            lines.append(f'    {op_slug} -- {src_type} --> {dst_slug}')
        
        lines.append('')
    
    # --- Styling ---
    lines.append('    %% --- Styling ---')
    lines.append('')
    
    # Operations: dark boxes; spiders, enrichments, gates get distinct colors
    lines.append('    %% Operations: dark boxes (fan=blue, funnel=green, enrichment=outlined, gate=purple)')
    for eq in equations:
        if eq.operation:
            slug = slugify(eq.operation)
            if eq.spider == 'fan':
                lines.append(f'    style {slug} fill:#1a3a5c,stroke:#0d47a1,color:#bbdefb,stroke-width:2px')
            elif eq.spider == 'funnel':
                lines.append(f'    style {slug} fill:#1b5e20,stroke:#2e7d32,color:#c8e6c9,stroke-width:2px')
            elif eq.enriches:
                # Enrichment morphisms: outlined box (payload unchanged, metadata updated)
                lines.append(f'    style {slug} fill:#e8eaf6,stroke:#3949ab,color:#1a237e,stroke-width:2px')
            elif eq.discard and len(eq.outputs) >= 2:
                # Gate / collapse operators: coproduct with discard (purple)
                lines.append(f'    style {slug} fill:#4a148c,stroke:#6a1b9a,color:#e1bee7,stroke-width:2px')
            else:
                lines.append(f'    style {slug} fill:#2d2d2d,stroke:#000,color:#fff,stroke-width:2px')
    lines.append('')
    
    # Sources: light rounded
    lines.append('    %% Source types: light rounded')
    for t in sorted(sources):
        slug = slugify(t)
        lines.append(f'    style {slug} fill:#f5f5f5,stroke:#999,color:#333')
    lines.append('')
    
    # Sinks: red
    if sinks:
        lines.append('    %% Waste sinks: red')
        for t in sorted(sinks):
            slug = slugify(t) + '_sink'
            lines.append(f'    style {slug} fill:#fee,stroke:#c00,color:#c00,stroke-width:2px')
    
    return '\n'.join(lines)


# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------

def main():
    parser = argparse.ArgumentParser(
        description='Convert resource equations to a Mermaid string diagram.'
    )
    parser.add_argument('input', help='Path to resource equations file (.txt)')
    parser.add_argument('--direction', default='LR',
                        choices=['LR', 'RL', 'TD', 'BT'],
                        help='Flowchart direction (default: LR)')
    parser.add_argument('-o', '--output', help='Output file (default: stdout)')
    
    args = parser.parse_args()
    
    equations = parse_file(args.input)
    
    if not equations:
        print(f"No equations found in {args.input}", file=sys.stderr)
        sys.exit(1)
    
    mermaid = generate_mermaid(equations, direction=args.direction)
    
    if args.output:
        with open(args.output, 'w') as f:
            f.write(mermaid)
        print(f"Wrote {args.output} ({len(equations)} equations)", file=sys.stderr)
    else:
        print(mermaid)


if __name__ == '__main__':
    main()
