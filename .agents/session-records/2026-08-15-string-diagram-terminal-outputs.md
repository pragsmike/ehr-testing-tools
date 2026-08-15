# 2026-08-15 -- String-diagram terminal-output result nodes (ADR-0135)

**Mode:** autonomous, R30 (commit and push at each checkpoint).
**Prompt:** `.agents/prompts/2026-08-15-string-diagram-terminal-outputs.md`
(self-archived at close, per convention).
**Record of decision:** `notes/adr/0135-string-diagram-terminal-outputs.md`.

A docs/tooling micro-arc, chartered channel-direct 2026-08-14 with no
prior open roadmap row. Author rulings, verbatim: **"Q1 a. Q2 b.
Micro-arc."** Zero corpus `src`, zero `demos`, zero module JSON — **no
oracle claim is made or owed**, and none is made.

## Step 0 — Ceremony

`bin/preflight` at start: last five CI runs on `main` green (through
`00bdad77`); edit-root ext4, not under `/mnt/`; tree clean including
untracked; local HEAD matched `origin/main` at `00bdad77`; last
`stable-*` tag `stable-20260814-exact-name` at `46b82ba`, HEAD not
tagged. No tag paid — the prompt states the tag is NOT self-paid and
defers it to the next session's Step 0 under the standing conditional
license (ADR-0133's pattern).

## Step 1 — Red, witnessed

New test, the only one in this suite that runs the renderer for real:
`components/docs-tooling/test/ehrt/docs_tooling/mermaid_render_test.clj`.
It writes a one-equation temp file —

```
datum × ctx → pass + rejected  [Op]  {catalytic: ctx}
```

— shells out via `clojure.java.shell/sh` to `python3
components/palgebra/tools/resource_equations_to_mermaid.py <tmp> -o
<tmp-out>`, and asserts exit 0, a result node and an output wire per
summand, and an undisturbed dashed catalytic input wire.

**RED: 6 assertions, 4 failures, 0 errors.** The four failures were
exactly the two result-node and two result-wire assertions; the two
passes (exit 0, dashed catalytic wire) confirm the red isolates the
missing behavior rather than a broken invocation. The renderer's whole
output at red ended at the dead-end the channel witnessed:

```
    %% --- Wires (typed connections) ---
    %% Arrow 1: Op
    datum -- datum --> Op
    ctx -. ctx .-> Op
```

The step's STOP-AND-REPORT condition (python3 absent from the test
environment) did **not** fire: `/usr/bin/python3`, 3.8.10, and the
script's `from __future__ import annotations` keeps its `list[str]`
annotations legal there. The docstring discloses the dependency; it is
not new (`make pipeline`/`make use-cases` shell out to this script and
CI pins `actions/setup-python` for exactly that reason).

## Step 2 — Green, script + skill doc co-landed (`8c9f291`)

`classify_types` grows a terminal class (`all_outputs - all_inputs -
all_discard - all_feedback_sources`, feedback sources newly
accumulated for that subtraction); a `%% --- Result types (terminal
outputs) ---` node section follows the sink block; the per-equation
output pass extends the discard branch with a feedback `continue` and
a terminal wire; a green style block lands per Q2 b, colors unadjusted
from the channel's recommendation. Sorted emission throughout.

**GREEN: 6 assertions, 0 failures, 0 errors.**

`.agents/skills/string-diagram/SKILL.md` in the same commit: wire-table
row for the unannotated default, a node-styling paragraph (three
classes by color, plus the `_out` suffix rule), corrected Validation
guidance (a produced-but-unconsumed type is no longer an invisible
"dangling output" — it is drawn, and real waste should be annotated
`{discard: X}`), and the lemon-meringue worked example's own terminal
output, `AddMeringue -- "unbaked-pie" --> unbaked_pie_out` — verified
by running the converter over
`components/palgebra/examples/lemon-pie-equations.txt`, not asserted.
`.claude/skills/string-diagram/SKILL.md` re-synced with `cp -p`
(ADR-0024's mirror gate), disclosed in the commit message.

**One placement deviation, disclosed in the commit message:** the
result NODE section follows the sink block as chartered, but the
result STYLE block sits *before* the sink style block. The sink style
block is the last thing `generate_mermaid` emits and carries no
trailing blank line; appending after it would have added one, changing
the bytes of every already-generated diagram that has sinks. Inserting
before keeps "no existing emission reordered" literally true.

## Step 3 — Regeneration diff scope (`4089322`)

`make use-cases && make pipeline`, zero hand edits. `git status
--short` afterwards: **21 files under `docs/use-cases/` plus
`docs/dev/pipeline.md`, and nothing else.** `docs/use-cases.md` is
inside the fence but byte-unchanged — the generated index links to
pages rather than inlining diagrams. The diff-scope STOP-AND-REPORT
did not fire.

**Witnessed fix on the named instance**, quoted from
`docs/use-cases/profile-tier-hl7v2-conformance-gating.md`'s own
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

**Multi-stage masking was partial, not total.** `docs/dev/pipeline.md`
gained four genuine yields it had never drawn — `report`,
`catalog-entry`, `intake-record`, `lineage-record`. Intermediates
untouched.

**Determinism, verified not assumed:** both targets re-run over the
already-regenerated tree produced a byte-identical `git diff`. After
committing, CI's own freshness step was run locally — `make docsgen`
then `git diff --exit-code docs/dev/pipeline.md docs/use-cases.md
docs/use-cases/ docs/operators.md docs/cli.md` — and exited clean.
That STOP-AND-REPORT did not fire.

## Step 3.4 — Consistency glance (read-only, nothing edited)

- `README.md` — a different visual vocabulary entirely (subgraphs,
  `{{...}}` shapes, unlabeled wires, no styling classes). An audience
  overview, not a palgebra rendering. No follow-row.
- `docs/dev/architecture.md` — a brick dependency graph. Not
  applicable.
- `components/sim/docs/sim-theory-diagram.md` — **is** a string
  diagram from this converter, and yielded two follow-row candidates:
  (1) it and its standalone `.mermaid` sibling are stale against the
  new convention, and its own header already carries three prior
  sessions' standing request for a Python-having session to regenerate
  it; (2) that header's regeneration recipe cites
  `.agents/skills/string-diagram/tools/...`, a path that has not
  existed since ADR-0005 moved the converter to
  `components/palgebra/tools/`.

Both were **reported to the author under the prompt's read-only fence,
not acted on** — which is what produced the mid-session ruling below.

## Step 3.5 — Licensed fence widening (`a8a5e65`)

Channel-proposed, author-licensed. Ruling verbatim, quoted in full in
ADR-0135 and `.agents/rulings.md`: *"b. Widen the fence by one step
before close: regenerate components/sim/docs/sim-theory-diagram.md
with the updated converter and fix its header's regeneration recipe
path … First locate the diagram's true equations source; if it is
ambiguous or missing, STOP-AND-REPORT instead of improvising. Witness
two-run byte-determinism as in Step 3.3. … Then proceed to the close
as chartered."*

**Source located first, as required:** `components/sim/docs/
sim-theory-equations.txt`, named as the input by the diagram's own
`GENERATED by` header and by that file's own header. Unambiguous — the
STOP-AND-REPORT did not fire.

**Dead path fixed in both copies** (the diagram's header and the
equations file's own — same defect, same mechanism, same commit).
The equations-file header keeps its exact line count on purpose:
`%% Arrow N` numbering derives from that file's line numbering, so a
header edit that changed the count would silently renumber every
arrow. Paths are now repo-root-relative and the header says so.

**Regeneration result.** Beyond ADR-0135's own six new result nodes —
`pass`/`rejected` from `Check`, `state-document` from `EmitState`,
`run-manifest` from `Package`, `sut-behavior` from `SystemUnderTest`,
`catalog-entry` from `ToolsCorpusIntake` — exactly one other
difference: `%% Arrow N` renumbering (RunModules 22→21,
CompileTrajectory 25→23, …), the M6 session's own unregenerated
residue from removing three `# planned:` comment lines it could not
re-run the converter after. No node, no wire, no catalytic input moved.
**That discharges the standing request the M5b and M6 notes each
left** for a Python-having session to confirm by running rather than
by inspection; the discharge is recorded in the diagram's own header,
where the request was made. The reason nobody had discharged it is now
visible: the command those sessions were handed had not existed since
ADR-0005.

**Two-run byte-determinism witnessed**, and the standalone `.mermaid`
verified equal to the `.md`'s embedded block after each run — the
exact drift the M5-prep session already had to fix once, which is why
the standalone file was regenerated alongside the block rather than
left behind. A "Verified in this render" bullet now states the change
so the file's prose and its block agree.

The dangling `Calibrate -- churn-profile --> churn_profile` wire
(target node ID undeclared) was **not** fixed — unrelated, out of
scope, and now stated in the file's own header so the next session
finds it recorded rather than rediscovers it.

## Close

ADR-0135 plus its `notes/ADRs.md` index line, the roadmap Done row
(disclosing the channel-direct charter, no prior open row, and the
licensed widening) and its dated pointer, four rulings rows, this
record, and the prompt archive. `bin/close-scaffold` generated the
record/prompt pair and both README index lines.

**`make test` at the final tree, verified from a full captured log:**
`clojure -M:poly check` OK; 636 `0 failures, 0 errors` occurrences;
zero non-zero failure/error counts; zero `FAIL in`/`ERROR in` lines;
`bin/verify-nist-lock` OK on all six NIST coordinates; `MAKE_EXIT=0`.
636 vs ADR-0133/0134's 632 baseline reconciles exactly as `+4`: this
session's one new namespace (`ehrt.docs-tooling.mermaid-render-test`)
runs in two project contexts, `conformance` and `ehrt-cli`, two
matching lines each.

Process note, recorded because it nearly shipped a false green: the
first two attempts ran `make test 2>&1 | tail -N`. Without `pipefail`
that reports `tail`'s exit code rather than `make`'s, and the tail
discards the body a block count needs. The second attempt was killed
mid-run — its log ends `make: *** [Makefile:40: test] Error 143` while
the harness reported the command as exit 0. The gate that closed this
session is the third run: unpiped, logged in full, `MAKE_EXIT=$?`
captured. `bin/post-push-verify` after push.

No tag paid — NOT self-paid per the prompt, deferred to the next
session's Step 0 under the standing conditional license (ADR-0133's
pattern).
