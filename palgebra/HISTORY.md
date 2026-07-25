# History

Lineage record for the `palgebra` namespace root, written at the moment
the language was first claimed into its own tree (`ehr-testing-tools` R2,
2026-07-25, `.agents/plans/judge-gate-refactor.md` Phase 2) — surfaced now,
while it is a note, not repo-history archaeology later.

## Origin: cyberneutics

The notation and its rendering machinery originate in the author's
[cyberneutics](https://github.com/pragsmike/cyberneutics) methodology
project. The `string-diagram` skill this repo copied and adapted
(`notes/ADRs.md` ADR-0003) is verified upstream: its `SKILL.md` (name and
description match the copy that lived at
`.agents/skills/string-diagram/SKILL.md` in this repo before this session)
is at
[`.claude/skills/string-diagram/SKILL.md`](https://github.com/pragsmike/cyberneutics/blob/main/.claude/skills/string-diagram/SKILL.md)
in that repo, retrieved HTTP 200 2026-07-24 (`docs/notation.md`'s citation,
the seed for this note). The fan/funnel spider duality the skill's
annotations render is theory documented upstream at
`palgebra/duality-and-composition.md` in the cyberneutics repo — cited
directly by `SKILL.md` and by two of the claimed example equation sets
(`decision-monad-equations.txt`, `deliberated-choice-equations.txt`).

Primitive palgebra, as it existed upstream: equation syntax (`×`, `+`,
`→`), named operations, catalytic annotations, fan/funnel spider
topology, feedback loops, and the Mermaid string-diagram rendering —
`resource_equations_to_mermaid.py` and the base equation grammar in
`SKILL.md`.

## This repo's embellishments

Everything below was added inside `ehr-testing-tools`, not upstream, over
P4–P6 and R1 of `.agents/plans/corpus-foundations.md`:

- **Union resources** (`docs/notation.md`, P6) — a named resource declared
  as the union (coproduct) of others, rendered via the existing
  fan/funnel machinery reused for a merge node (`{spider: funnel}`), not
  new diagram machinery.
- **External stages** (`docs/notation.md`, P6) — a black-box operation
  this repo doesn't implement, rendered with a dashed border
  (`{external: true}`).
- **Catalytic-resource targets** — the four-target resolution rule
  (`docs/notation.md`) and its mechanical verification
  (`ehr-testing-tools.lint`, tier-1 pipeline lint, P6).
- **The judge/gate factorization and the D-register**
  (`docs/palgebra-design.md`, ADR-0009, R1) — the observe/judge/act
  layering (D1), judge vs. validator/checker as species (D2), the
  judgment type carrying its subject (D3), two registries by role and
  lifetime (D4), the two modeling layers abstract/lowered (D5), and the
  decision register (D1–D13, O1–O5) this repo's palgebra design record
  keeps as its reasoning-of-record.

## The ruling on backporting

**Lineage is acknowledged here explicitly; backporting these
embellishments upstream to cyberneutics is deliberately deferred — no
sync machinery is built.** This repo's `palgebra/` tree is the living
line for this repo's own purposes; keeping it synchronized with
upstream, or upstreaming its embellishments, is a decision for whoever
maintains cyberneutics, not an obligation this tree takes on. (D9,
`docs/palgebra-design.md` §I.7: develop in-repo under the palgebra
namespace root, extract to its own repo when a second instance or an
external user appears — this note travels with the language at that
point.)
