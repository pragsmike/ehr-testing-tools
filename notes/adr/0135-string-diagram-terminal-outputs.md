## ADR-0135 — String-diagram terminal outputs: every codomain now wires somewhere

**Status:** Accepted (author-directed, autonomous session per R30), 2026-08-15.

### Context

Chartered directly from the design channel, 2026-08-14, with no prior
open roadmap row — the roadmap row this record's own close adds is
written into the Done section as a closed row, and says so.

The roadmap row text, verbatim:

> String-diagram terminal outputs — palgebra diagrams showed inputs,
> not outputs — CLOSED (ADR-0135; chartered channel-direct 2026-08-14,
> no prior open row). Every single-equation use-case diagram dead-ended
> at the operation box: `resource_equations_to_mermaid.py` emitted
> output wires only for discard sinks and feedback edges, and
> classify_types had no terminal-output class. Fixed per author rulings
> "Q1 a. Q2 b.": one green result node per coproduct summand, `_out`
> suffixed, wired from the operation; discard/feedback/intermediate
> semantics untouched; skill doc extended in the same commit; all 21
> pages plus pipeline.md regenerated mechanically.

The defect was channel-witnessed on the live tree at `00bdad7`
(`docs/use-cases/profile-tier-hl7v2-conformance-gating.md`) and
mechanism-pinned before this session began: the signature line reads
`datum × profile-artifact → pass + rejected + no-verdict [Gate]`, the
diagram drew both input wires, and nothing wired out. The mechanism is
`components/palgebra/tools/resource_equations_to_mermaid.py` — its
per-equation output pass emitted a wire only for `{discard:}` outputs
(red sinks), its feedback loop only for `{feedback:}` mappings, and
`classify_types` had no terminal-output class at all, so a codomain
that was neither discarded nor consumed downstream got no node and no
wire. Multi-stage diagrams (`docs/dev/pipeline.md`) masked the gap
because each stage's outputs are the next stage's inputs — but not
entirely, as Step 3 showed: even there, four genuinely terminal types
were invisible.

The string-diagram skill's own SKILL.md wire table carried the same
gap (discard and feedback each mapped to a wire; a plain output mapped
to nothing), so this was a shared design gap in script and skill doc
together, not script-vs-spec divergence — hence the co-landing in
Step 2 rather than a script fix alone.

This is a docs/tooling micro-arc: zero corpus `src`, zero `demos`,
zero module JSON. **No oracle claim is made or owed**, and none is
made here.

### Author rulings, verbatim

Charter, rendering, style, and scope, in one line: **"Q1 a. Q2 b.
Micro-arc."**

- **Q1 a** — one result node per coproduct summand, wired
  `Op -- name --> node`; a discarded summand still goes to its red
  sink; a summand consumed by any other equation in the file stays an
  inter-op edge (the existing producer map arbitrates); only
  truly-terminal outputs get result nodes.
- **Q2 b** — result nodes get a visually distinct tint so domain and
  codomain are tellable at a glance (never the source grey).

**The fence widening, ruled mid-session.** Step 3.4's glance was
reported to the author as chartered (follow-row candidates, no edits).
The channel proposed acting on them instead; the author licensed it,
verbatim:

> b. Widen the fence by one step before close: regenerate
> components/sim/docs/sim-theory-diagram.md with the updated converter
> and fix its header's regeneration recipe path (dead
> .agents/skills/string-diagram/tools/… → live
> components/palgebra/tools/resource_equations_to_mermaid.py) in the
> same commit. First locate the diagram's true equations source; if it
> is ambiguous or missing, STOP-AND-REPORT instead of improvising.
> Witness two-run byte-determinism as in Step 3.3. Quote this ruling
> verbatim in the ADR and rulings rows; record the widening as
> channel-proposed, author-licensed. Then proceed to the close as
> chartered.

Recorded as what it is: **channel-proposed, author-licensed**. The
original prompt's read-only fence on hand-authored diagrams stood
until this ruling and was not improvised past — the candidates were
reported first and acted on only after the license. Step 3.5 below is
that step; the close then proceeded as chartered.

### Step 0 — Ceremony

`bin/preflight` at session start: last five CI runs on `main` all
green (through `00bdad77`, ADR-0134's own post-push receipts);
edit-root ext4, not under `/mnt/`; working tree clean including
untracked; local HEAD matched `origin/main` at `00bdad77`; last
`stable-*` tag `stable-20260814-exact-name` at `46b82ba`, HEAD not
tagged.

No tag is paid this session — the prompt states the tag is NOT
self-paid and defers it to the next session's Step 0 under the
standing conditional license (channel fresh-clone verification plus
author-side CI check), ADR-0133's pattern.

### Step 1 — Red, witnessed

No Python test harness exists in this repo and this micro-arc does not
introduce one; the red is a Clojure test that shells out to the
script, `components/docs-tooling/test/ehrt/docs_tooling/
mermaid_render_test.clj`. It writes a temp equations file holding
exactly one equation —

```
datum × ctx → pass + rejected  [Op]  {catalytic: ctx}
```

— runs `python3 components/palgebra/tools/
resource_equations_to_mermaid.py <tmp> -o <tmp-out>` via
`clojure.java.shell/sh`, and asserts exit 0, a result node per summand
(`pass_out(["pass"])`, `rejected_out(["rejected"])`), a wire per
summand (`Op -- "pass" --> pass_out`, likewise `rejected`), and — the
no-regression assertion — that the catalytic input wire is still
dashed.

RED witnessed before any script edit: **6 assertions, 4 failures, 0
errors.** The four failures were exactly the two result-node and two
result-wire assertions; exit 0 and the dashed catalytic wire passed,
confirming the red isolates the missing behavior rather than a broken
invocation. The renderer's full output at red, quoted from the failure
report, ended at:

```
    %% --- Wires (typed connections) ---
    %% Arrow 1: Op
    datum -- datum --> Op
    ctx -. ctx .-> Op
```

— the dead-end, reproduced hermetically.

The test's docstring discloses the `python3` runtime dependency. That
dependency is not new: `make pipeline` and `make use-cases` both shell
out to this script, and `.github/workflows/test.yml` pins
`actions/setup-python` for exactly that reason. The STOP-AND-REPORT
condition attached to this step (python3 absent from the `poly test`
environment) did not fire — `python3` resolves to `/usr/bin/python3`,
3.8.10, and the script's `from __future__ import annotations` keeps
its `list[str]` annotations legal there.

This is the only test in the suite that runs the renderer for real.
`pipeline_test.clj` and `usecases_test.clj` deliberately work over
already-rendered text, and their own docstrings say so; nothing about
that split changes.

### Step 2 — Green: script and skill doc, co-landed (`8c9f291`)

In `resource_equations_to_mermaid.py`:

1. `classify_types` grows a fifth class and returns it alongside the
   existing four: `terminal = all_outputs - all_inputs - all_discard -
   all_feedback_sources`. Feedback sources are newly accumulated
   (`eq.feedback.keys()`) for exactly this subtraction.
2. A new node section after the sink block, `%% --- Result types
   (terminal outputs) ---`, emitting `{slugify(t)}_out(["{t}"])` for
   `t` in `sorted(terminal)`. The `_out` suffix is per the ruling and
   keeps the result node's ID clear of the source node for the same
   type name — a type can be consumed in one equation and terminally
   produced in another (enrichment pass-through, `catalog → catalog
   [Enrich]`).
3. The per-equation output pass extends the discard branch:
   `elif out in eq.feedback: continue` (the traced wire below is that
   output's own wire, unchanged) and `elif out in terminal:
   {op_slug} -- "{out}" --> {slugify(out)}_out`. Intermediates
   continue to render as inter-op edges via the producer map,
   untouched.
4. Styling per Q2 b: `%% Result types (terminal outputs): green
   rounded`, `style {slug}_out fill:#e8f5e9,stroke:#2e7d32,
   color:#1b5e20`. The channel-recommended colors were used
   unadjusted — no collision with the source grey `#f5f5f5` or the red
   sink `#fee`, which is the whole point of the ruling.
5. Determinism: result nodes and styles are emitted in sorted order.

**One placement deviation, disclosed in the commit message.** The
result NODE section follows the sink block as chartered, but the
result STYLE block sits *before* the sink style block rather than
after it. The sink style block is the last thing `generate_mermaid`
emits and deliberately carries no trailing blank line; appending after
it would have required adding one, changing the bytes of every
already-generated diagram that has sinks. Inserting before it keeps
"no existing emission reordered" literally true — the sink style block
stays last, byte-identical.

In `.agents/skills/string-diagram/SKILL.md`, same commit: the wire
table gains a plain-output row (produced, not consumed downstream →
green result node on the right, one per coproduct summand); a new
node-styling paragraph documents the three node classes by color and
the `_out` suffix rule; the Validation section is corrected (a
produced-but-unconsumed type is no longer an invisible "dangling
output the user should review" — it is classified terminal and drawn,
and the guidance is now "if a result node is really waste, annotate it
`{discard: X}`"); and the lemon-meringue worked example, whose
expected output this change affects, gains its one terminal output —
`AddMeringue -- "unbaked-pie" --> unbaked_pie_out`, verified by
actually running the converter over
`components/palgebra/examples/lemon-pie-equations.txt`, not asserted.

`.claude/skills/string-diagram/SKILL.md` was re-synced with `cp -p` in
the same commit — a mechanical consequence of the canonical `.agents/`
edit, gated by `ehrt.docs-tooling.skill-mirror-currency-test`
(ADR-0024), named in the commit message.

Re-run at green: **6 assertions, 0 failures, 0 errors.**

### Step 3 — Regeneration, mechanical (`4089322`)

`make use-cases && make pipeline`, zero hand edits.

`git status --short` afterwards: 21 files under `docs/use-cases/` plus
`docs/dev/pipeline.md`, and nothing else. `docs/use-cases.md` — inside
the fence, but byte-unchanged — did not move, because the generated
index links to each page rather than inlining its diagram
(`usecases_test.clj` asserts exactly that property). The
STOP-AND-REPORT on a diff exceeding the named set did not fire.

**The witnessed instance, fixed.** `docs/use-cases/
profile-tier-hl7v2-conformance-gating.md` now carries, quoted from the
regeneration diff:

```
    %% --- Result types (terminal outputs) ---
    no_verdict_out(["no-verdict"])
    pass_out(["pass"])
    rejected_out(["rejected"])
```

```
    Gate -- "pass" --> pass_out
    Gate -- "rejected" --> rejected_out
    Gate -- "no-verdict" --> no_verdict_out
```

```
    %% Result types (terminal outputs): green rounded
    style no_verdict_out fill:#e8f5e9,stroke:#2e7d32,color:#1b5e20
    style pass_out fill:#e8f5e9,stroke:#2e7d32,color:#1b5e20
    style rejected_out fill:#e8f5e9,stroke:#2e7d32,color:#1b5e20
```

The three-way verdict vocabulary the page's own prose is about is now
visible in the page's own diagram.

**Multi-stage masking was partial, not total.** `docs/dev/pipeline.md`
gained four result nodes — `report`, `catalog-entry`, `intake-record`,
`lineage-record` — with `Mutate -- "lineage-record" -->
lineage_record_out`, `Intake -- "catalog-entry"/"intake-record" -->`
their nodes, and `Report -- "report" --> report_out`. Every one is a
genuine yield of the pipeline that the diagram previously did not
draw. Stage-to-stage intermediates are untouched, as the ruling
requires.

**Determinism, verified rather than assumed.** Both targets were run a
second time over the already-regenerated tree and the resulting `git
diff` was byte-identical to the first. After committing, CI's own
freshness step was run locally — `make docsgen` followed by `git diff
--exit-code docs/dev/pipeline.md docs/use-cases.md docs/use-cases/
docs/operators.md docs/cli.md` — and exited clean. That
STOP-AND-REPORT did not fire either.

### Step 3.4 — Consistency glance, read-only

Hand-authored diagrams outside the regeneration path were read, not
edited (move-don't-improve; this arc's one sanctioned improvement is
the skill doc). Two of the three are not string diagrams at all and
raise no follow-row:

- `README.md`'s overview diagram uses a different visual vocabulary
  entirely (subgraphs, `{{...}}` shapes, unlabeled wires, no
  source/sink/result styling). It is an audience overview, not a
  palgebra rendering; the new convention does not reach it.
- `docs/dev/architecture.md`'s diagram is a brick dependency graph.
  Not applicable.

`components/sim/docs/sim-theory-diagram.md` IS a string diagram,
produced by this very converter, and yielded **two follow-row
candidates**. As this step was chartered, they were recorded and
reported, NOT acted on — the account below is what was reported to the
author. That report is what produced the mid-session ruling above;
Step 3.5 is the acting.

1. **Stale relative to the new convention.** Its embedded block — and
   the standalone `components/sim/docs/sim-theory-diagram.mermaid`
   beside it — predate this change, so `Check`, `EmitState`,
   `Package`, `SystemUnderTest`, and `ToolsCorpusIntake` still
   dead-end. Its own header records that three prior sessions could
   not run the converter (no Python, or a Windows Store shim) and left
   a standing request for "a future session with Python (or WSL)
   available" to regenerate. This session HAS python3 and a WSL shell,
   and deliberately did not use them here: the file is fenced
   read-only this arc, and regenerating it is a `components/sim` doc
   change with its own equations-file provenance to check, not a
   ride-along.
2. **A stale path in that file's own regeneration recipe.** Its header
   says to run `python .agents/skills/string-diagram/tools/
   resource_equations_to_mermaid.py`. No such path exists — the
   converter lives at `components/palgebra/tools/` in this workspace,
   which ADR-0005 corrected in SKILL.md at the carve-loss recovery
   without reaching this file's header. A session that picks up
   candidate 1 should fix the recipe in the same commit.

A third observation, noted so it is not mistaken for damage this
change caused: that file's committed block already contains `Calibrate
-- churn-profile --> churn_profile`, a feedback wire pointing at a node
ID nothing declares. Pre-existing and unrelated to terminal outputs.

### Step 3.5 — The licensed widening: both candidates acted on (`a8a5e65`)

**Equations source, located first as the ruling required.** Unambiguous:
`components/sim/docs/sim-theory-equations.txt`, named as the input by
the diagram's own `GENERATED by` header and by that file's own header.
The STOP-AND-REPORT on an ambiguous or missing source did not fire.

**Candidate 2 (the dead path) is fixed in both copies of the recipe.**
The `.md` header and `sim-theory-equations.txt`'s own header each
carried `python .agents/skills/string-diagram/tools/
resource_equations_to_mermaid.py` — same defect, same mechanism (ADR-0005
moved the converter to `components/palgebra/` at the carve-loss
recovery, corrected the pin in SKILL.md, and reached neither file), so
both were fixed in the same commit rather than leaving one live copy of
a dead command behind. Paths are now repo-root-relative and the header
says so. The equations-file header keeps its exact line count, which
matters: `%% Arrow N` numbering derives from the equations file's own
line numbering, so a header edit that changed the line count would have
shifted every arrow number as a side effect.

**Candidate 1 (the stale diagram) regenerated.** Beyond the terminal
outputs themselves, the regeneration surfaced exactly one other
difference, and it is a finding rather than a side effect: **`%% Arrow
N` renumbering — the M6 session's own unregenerated residue.** That
session removed three `# planned:` comment lines from the equations
file without being able to run the converter (its environment's
`python`/`python3` resolved to the Windows Store install-shim), and
arrow numbers derive from line numbering. RunModules 22→21,
CompileTrajectory 25→23, and so on down. Cosmetic, now reconciled.

Nothing else moved: no node, no wire, no catalytic input. **That
DISCHARGES the standing request the M5b and M6 notes each left** — "a
future session with Python (or WSL) available should actually run the
regeneration once to confirm this argument rather than continue
trusting it by inspection alone." This session had both, ran it, and
their argument holds. The discharge is recorded in the diagram's own
header, where the request was made.

The six codomains that gained result nodes: `pass` and `rejected` from
`Check`, `state-document` from `EmitState`, `run-manifest` from
`Package`, `sut-behavior` from `SystemUnderTest`, `catalog-entry` from
`ToolsCorpusIntake`. `Check`'s verdict coproduct is the starkest case —
the invariant catalog's answer was drawn going in and never coming
out. A bullet in the file's own "Verified in this render" list now
states the change, so the prose and the block agree.

**Two-run byte-determinism witnessed, as the ruling required:** the
converter was re-run over the final equations file and the output was
byte-identical to the first run. The standalone
`sim-theory-diagram.mermaid` and the `.md`'s embedded block were
verified equal after both runs — the exact drift the M5-prep session
had to fix once already, and the reason the standalone file was
regenerated alongside the embedded block rather than left behind.

The dangling `Calibrate -- churn-profile --> churn_profile` wire was
**not** fixed: unrelated to terminal outputs, out of this arc's scope,
and now recorded in the diagram's own header so the next session that
opens the file finds it stated rather than rediscovers it.

### Fences honored

Touched: the converter, `.agents/skills/string-diagram/SKILL.md` and
its `.claude/` mirror, the new `mermaid_render_test.clj`, the two
regeneration targets that actually moved, the three
`components/sim/docs/sim-theory-*` files the author's mid-session
ruling licensed, and this close's own artifacts. Zero corpus `src`,
zero `demos`, zero module JSON, zero `docs/notation.md` — the grammar
is unchanged; only rendering.

The original prompt's "hand-authored diagrams are read-only this arc"
fence held until the author lifted it explicitly for one named file.
`README.md` and `docs/dev/architecture.md` stayed read-only throughout;
neither is a string diagram and neither was touched.

### Verification at the final tree

`make test` green at the final tree, verified from a full captured log
rather than a tail: `clojure -M:poly check` OK; **636 `0 failures, 0
errors` occurrences**, zero lines carrying a non-zero failure or error
count, zero `FAIL in`/`ERROR in` lines, `bin/verify-nist-lock` OK on
all six NIST coordinates, and `make`'s own exit code 0 captured
explicitly.

**636 against ADR-0133/0134's own 632 baseline, reconciled exactly**:
this session adds one test namespace, `ehrt.docs-tooling.
mermaid-render-test`, which `poly test :all skip:integration` runs in
two project contexts (`conformance` and `ehrt-cli`), each run emitting
two matching lines — `+4`. Nothing else moved.

One process note worth recording, because it nearly shipped a false
green. The first two attempts ran `make test 2>&1 | tail -N`. Without
`pipefail` that pipeline reports `tail`'s exit code, not `make`'s, and
the tail also discards the body the block count is computed from. The
second attempt was killed mid-run and its log ends `make: *** [Makefile:40:
test] Error 143` — while the harness reported the command as exit 0.
The gate that closed this session is the third run, logged in full,
with `MAKE_EXIT=$?` captured after an unpiped `make test`.

### Lesson

The wire table in SKILL.md and the emitter agreed with each other and
were both wrong in the same place, which is why the gap survived every
regeneration and every reading of either surface alone. A table that
enumerates the annotated cases (`{discard:}`, `{feedback:}`,
`{catalytic:}`) and never states what happens to the unannotated
default is not a complete specification — the default is the case
nobody writes down. The co-landed-invariants rule caught it here only
because the prompt named both surfaces; a script-only fix would have
left the skill doc still teaching the gap.

A second lesson from the licensed widening: `components/sim/docs/
sim-theory-diagram.md` had accumulated three sessions' worth of "a
future session with Python should confirm this by actually running
it," and the reason no one did is now visible — the command they were
handed had not existed since ADR-0005. A stale recipe does not just
fail; it silently converts a mechanical check into a standing argument
from inspection. When a path move corrects one citation of a command,
grep for the others.

### Index summary (moved verbatim from notes/ADRs.md by ADR-0143, 2026-08-16)

String-diagram terminal outputs: every codomain now wires somewhere — chartered channel-direct 2026-08-14 with no prior open roadmap row (the row lands closed, and says so), against the defect the channel witnessed live at `00bdad7`: every single-equation palgebra diagram in the 21 generated use-case pages drew its inputs and dead-ended at the operation box, `docs/use-cases/profile-tier-hl7v2-conformance-gating.md` promising `datum × profile-artifact → pass + rejected + no-verdict [Gate]` and wiring nothing out. Mechanism, pinned before the session began: `components/palgebra/tools/resource_equations_to_mermaid.py` emitted output wires only for `{discard:}` sinks and `{feedback:}` mappings, and `classify_types` had no terminal-output class at all, so a codomain neither discarded nor consumed downstream got no node and no wire. **A shared design gap in script and skill doc together, not script-vs-spec divergence** — `.agents/skills/string-diagram/SKILL.md`'s own wire table mapped discard and feedback to wires and the unannotated default to nothing, which is why the gap survived every regeneration and every reading of either surface alone (the record's own lesson: the default is the case nobody writes down). Fixed per author rulings verbatim, **"Q1 a. Q2 b. Micro-arc."** — one green result node per coproduct summand (`fill:#e8f5e9,stroke:#2e7d32,color:#1b5e20`, never the source grey, so domain and codomain are tellable at a glance), `_out`-suffixed so a result node cannot collide with the source node for the same type name on enrichment pass-through, wired `Op -- "name" --> name_out`; discarded summands still go to their red sink, fed-back summands keep the traced wire, and a summand any other equation consumes stays an inter-op edge arbitrated by the existing producer map. Red witnessed first, in the only test in this suite that runs the renderer for real (`components/docs-tooling/test/ehrt/docs_tooling/mermaid_render_test.clj`, shelling out to `python3` — a dependency `make pipeline`/`make use-cases` and CI's own `actions/setup-python` already carried): 4 of 6 assertions failing, the two passes confirming exit 0 and an undisturbed dashed catalytic wire. Skill doc co-landed in the same commit (wire-table row, node-styling paragraph, corrected Validation guidance, and the lemon-meringue example's own `AddMeringue -- "unbaked-pie" --> unbaked_pie_out`, verified by running the converter rather than asserted), with `.claude/skills/`'s mirror re-synced as the mechanical consequence ADR-0024's gate requires. One placement deviation disclosed in its commit message: the result STYLE block sits before the sink style block rather than after, so the sink block stays last and no existing emission changes bytes. Regeneration was mechanical and byte-deterministic across two runs, touching 21 use-case pages plus `docs/dev/pipeline.md` and nothing else (`docs/use-cases.md` is inside the fence but byte-unchanged — the generated index links to pages rather than inlining diagrams); CI's own `make docsgen && git diff --exit-code` step was run locally on the regenerated bytes and exited clean. **Multi-stage masking proved partial, not total**: `pipeline.md` itself gained four genuine yields it had never drawn — `report`, `catalog-entry`, `intake-record`, `lineage-record`. Step 3.4's read-only glance clears `README.md` and `docs/dev/architecture.md` (neither is a string diagram) and raised two follow-row candidates against `components/sim/docs/sim-theory-diagram.md`, which IS one: stale against the new convention, and carrying a regeneration recipe that names a path gone since ADR-0005 moved the converter to `components/palgebra/tools/`. Both were reported first under the prompt's read-only fence and then **acted on under a mid-session author license, quoted verbatim in the record — "b. Widen the fence by one step before close..." — recorded as channel-proposed, author-licensed** (Step 3.5, `a8a5e65`). The equations source was located before anything was written and is unambiguous (`components/sim/docs/sim-theory-equations.txt`, named by both headers), so that ruling's own STOP-AND-REPORT never fired; two runs byte-identical, with the standalone `.mermaid` and the `.md`'s embedded block verified equal after each (the drift the M5-prep session already had to fix once); the dead path fixed in BOTH copies of the recipe, the equations-file header keeping its exact line count because `%% Arrow N` numbering derives from that file's own line numbering. Six terminal codomains now render there — `pass`/`rejected` from `Check` (the invariant catalog's verdict had been drawn going in and never coming out), `state-document`, `run-manifest`, `sut-behavior`, `catalog-entry`. **The regeneration DISCHARGES a standing request three sessions old**: the M5b and M6 notes each asked a future Python-having session to confirm by running rather than by inspection, and the reason none had is now visible — the command they were handed had not existed since ADR-0005. Their argument held; the only non-ADR-0135 difference was `%% Arrow N` renumbering, M6's own unregenerated residue from removing three `# planned:` comment lines. A pre-existing dangling wire there (`Calibrate -- churn-profile --> churn_profile`, targeting an undeclared node ID) is recorded in that file's own header, deliberately not fixed. `README.md` and `docs/dev/architecture.md` stayed read-only throughout. Zero corpus `src`, zero `demos`, zero module JSON, zero `docs/notation.md` — the grammar is unchanged, only its rendering — so no oracle claim is made or owed; `make test` green at the final tree
