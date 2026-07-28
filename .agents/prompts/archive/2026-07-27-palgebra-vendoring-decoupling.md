# 2026-07-27 — Vendor palgebra converter into the string-diagram skill; sever live `../ehr-testing-tools` path references

## Context

`ehr-testing-sim` claims independence from `ehr-testing-tools` (ADR-0001: tools may depend on sim, never the reverse), but four live path references break that claim in practice: the string-diagram skill and the two `docs/sim-theory-*` regeneration headers invoke `../ehr-testing-tools/palgebra/tools/resource_equations_to_mermaid.py` (requiring a sibling checkout), and `docs/sim-theory.md` hyperlinks `../../ehr-testing-tools/docs/notation.md`, which escapes the repo root and is a dead link on GitHub's web UI. ADR-0005 copied the skill's teaching material but deliberately did not vendor the converter; this session reverses that clause. The converter is 442 lines of stdlib-only Python and three example equation files; the author has ruled that copying them under the skill directory is the right cost now, with a future palgebra-repo extraction recorded as a consideration, not undertaken. A companion prompt (Part B, separate session, tools repo) adds a nightly drift check on the tools side — the side allowed to know about sim.

Copy source is pinned: `ehr-testing-tools` @ `7ecce38ca2f388759c4cb9a934a8c6f2fee7a5c8` (2026-07-27).

## Read first

- `notes/ADRs.md` — ADR-0001 (dependency direction), ADR-0005 (skill adoption, the non-vendoring clause this session supersedes)
- `.agents/skills/string-diagram/SKILL.md` — Step 2 and the "Files" section (the live references)
- `docs/sim-theory-diagram.md` header and `docs/sim-theory-equations.txt` header (regeneration commands)
- `docs/sim-theory.md` line ~6 (the escaping hyperlink)
- `AUTHORS-GUIDE.md` §"Git operations: WSL only" and the ceremony section

## Author rulings

1. Vendor under the skill directory, not repo root. The converter and examples live at `.agents/skills/string-diagram/tools/` and `.agents/skills/string-diagram/examples/`. The skill becomes self-contained; nothing outside `.agents/skills/string-diagram/` and the two docs headers may reference the vendored files.
2. Provenance is pinned, not vague. Every vendored file gets a comment header naming source repo, source path, commit SHA `7ecce38ca2f388759c4cb9a934a8c6f2fee7a5c8`, vendoring date 2026-07-27, and "vendored per ADR-0016." Equation files take `#` comment lines; the Python file takes a `#` block after the shebang/docstring.
3. ADR-0005 is not edited. A new ADR-0016 supersedes its non-vendoring clause. Append-only discipline holds.
4. `docs/sim-theory.md` links notation by URL, not by copy. `notation.md` is conceptual reference, not tooling the skill executes; the absolute GitHub URL (the pattern `AUTHORS-GUIDE.md` already uses) is the fix. Do not vendor it.
5. The committed mermaid diagram is ground truth. Step 4's regeneration check must reproduce it byte-identically. A nonempty diff is a STOP-AND-REPORT, not a silent regeneration — distinguish "reality disagrees with a sound check" from "check misencodes its invariant" (AUTHORS-GUIDE §7) before touching anything.
6. Historical references stay. `notes/ADRs.md`, session records, facts register, and prose mentions of ehr-testing-tools in docstrings/comments are provenance and description, not live paths. The sweep in Step 7 must classify, not delete.
7. Palgebra-repo extraction is recorded, not performed. ADR-0016's alternatives section and a roadmap entry capture it (see Steps 5–6). No third repo is created this session.

## Steps

### Step 0 — Preconditions

Confirm a sibling checkout exists at `../ehr-testing-tools` and is at commit `7ecce38ca2f388759c4cb9a934a8c6f2fee7a5c8` (`git -C ../ehr-testing-tools rev-parse HEAD`). If absent or at a different commit, STOP and report — do not copy from an unpinned source. Confirm `python3 --version` succeeds; if Python is unavailable, the verification in Steps 1 and 4 degrades to skip-when-absent: record the skipped probe in the deviation appendix and say so in the final report.

### Step 1 — Vendor the four files

Copy from the sibling checkout into the skill directory:

```
../ehr-testing-tools/palgebra/tools/resource_equations_to_mermaid.py
    → .agents/skills/string-diagram/tools/resource_equations_to_mermaid.py
../ehr-testing-tools/palgebra/examples/ai-study-equations.txt
    → .agents/skills/string-diagram/examples/ai-study-equations.txt
../ehr-testing-tools/palgebra/examples/lemon-pie-equations.txt
    → .agents/skills/string-diagram/examples/lemon-pie-equations.txt
../ehr-testing-tools/palgebra/examples/decision-monad-equations.txt
    → .agents/skills/string-diagram/examples/decision-monad-equations.txt
```

Prepend the provenance header (ruling 2) to each copy. Verify the converter still runs after the header edit:

```bash
python3 .agents/skills/string-diagram/tools/resource_equations_to_mermaid.py \
  .agents/skills/string-diagram/examples/lemon-pie-equations.txt
```

Expect Mermaid text on stdout, exit 0.

Commit: `skill(string-diagram): vendor palgebra converter and examples, pinned to tools@7ecce38 (ADR-0016)`

### Step 2 — Make SKILL.md self-contained

In `.agents/skills/string-diagram/SKILL.md`:

- Step 2's command becomes `python .agents/skills/string-diagram/tools/resource_equations_to_mermaid.py equations.txt -o flow.mermaid` (repo-root-relative, matching how agents invoke it).
- Delete the "requires a sibling checkout" paragraph in Step 2.
- Rewrite the "Files" section: the four entries become skill-relative paths; the prose records that the converter and examples are vendored copies of ehr-testing-tools' palgebra material, pinned by the header SHAs, per ADR-0016 (superseding ADR-0005's non-vendoring clause). Keep the sentence that they speak only in wires/boxes/composition.

Commit: `skill(string-diagram): self-contained — skill-relative paths, sibling-checkout requirement removed`

### Step 3 — Repoint the two regeneration headers

In `docs/sim-theory-diagram.md` and `docs/sim-theory-equations.txt`, change the regeneration command's script path to `.agents/skills/string-diagram/tools/resource_equations_to_mermaid.py`. Touch nothing else in either header; the MILESTONE notes and hand-edit warnings stay verbatim.

Commit: `docs(sim-theory): regeneration commands use the vendored converter path`

### Step 4 — Prove the vendored converter reproduces the committed diagram

Run the (path-updated) regeneration command from `docs/sim-theory-diagram.md`'s header against `docs/sim-theory-equations.txt`, writing to a scratch file, and diff the output against the mermaid block currently committed in `docs/sim-theory-diagram.md`. Byte-identical → record the probe in the final report and proceed. Any difference → STOP-AND-REPORT per ruling 5. Do not commit scratch output.

No commit (verification only; note the result in Step 5's ADR text).

### Step 5 — ADR-0016

Append to `notes/ADRs.md`:

ADR-0016 — String-diagram skill vendors its palgebra tooling; ADR-0005's non-vendoring clause superseded. Record: (a) the decision and the four vendored files with their pin SHA; (b) motivation — the sibling-checkout requirement contradicted the repo's independence claim and the go-public posture; (c) Step 4's reproduction probe result as evidence the copy is faithful; (d) drift handling — a nightly diff in ehr-testing-tools' consumer-loop workflow (the dependency-direction-correct side), referencing this ADR; (e) alternatives considered: keeping the sibling requirement (rejected — breaks independence), fetching the script by URL at use time (rejected — network dependence, unpinned), and extracting palgebra into its own repo that both sim and tools depend on — deferred, not rejected: palgebra is itself a tool for expressing and working with abstract designs and does not belong to either consumer; vendoring now stages that extraction (the skill directory becomes a `git mv` source) rather than foreclosing it.

Commit: `adr: ADR-0016 vendored palgebra tooling, supersedes ADR-0005 non-vendoring clause`

### Step 6 — Roadmap note: the palgebra repo

In `.agents/plans/roadmap.md`, in whatever section holds unscheduled / horizon items (create a short "Considered, unscheduled" subsection at the end if none exists), add an entry: Extract palgebra (converter, notation, examples, string-diagram skill) into a standalone repo both ehr-testing-sim and ehr-testing-tools depend on. Trigger to revisit: the nightly drift check firing more than rarely, or a third consumer appearing. See ADR-0016 alternatives.

Commit: `plans: record palgebra-repo extraction as a considered, unscheduled direction`

### Step 7 — Sweep and classify

Run `grep -rn '\.\./ehr-testing-tools' --exclude-dir=.git .` and `grep -rn 'ehr-testing-tools' docs/ .agents/skills/`. Expected end state: zero live path references anywhere; remaining name mentions only in historical/provenance contexts (ADRs, session records, facts register, docstrings, README/AGENTS prose, the ADR-0016 text itself, and the vendored files' own provenance headers). Also verify `docs/sim-theory.md`'s notation link is now the absolute GitHub URL (this edit lands here if not already folded into Step 3's commit — author preference: fold it into Step 3). Classify every hit in the final report; anything live that survives is a finding.

No commit unless the sweep finds a stray to fix; if so: `docs: sever remaining live ehr-testing-tools path reference (sweep finding)`

### Step 8 — Test gate

`make test` (or the deps.edn test alias if make is unavailable). All green expected — this session touched no `src/` or `test/` code. Any failure is a STOP-AND-REPORT.

### Step 9 — Archive this prompt

Copy this prompt into `.agents/prompts/archive/` under its dated filename. Append a deviation-record appendix if any ruling was deviated from or any probe was skipped (Step 0's Python skip-when-absent included).

Commit: `prompts: archive 2026-07-27 palgebra vendoring decoupling session`

## Final report

State: files vendored (with SHAs), Step 4 probe result, sweep classification table, test-gate result, deviations if any.

---

## Part B (separate session, `ehr-testing-tools` repo) — Nightly drift check on the vendored palgebra copies

### Context

`ehr-testing-sim` now vendors four palgebra files pinned to tools@7ecce38 (sim's ADR-0016). Tools is the repo allowed to know about sim (sim ADR-0001), so the drift guard lives here: the existing nightly integration workflow already exercises sim as a consumer; this adds a byte-diff between tools' authoritative palgebra copies and sim's vendored ones.

### Read first

- The nightly integration workflow under `.github/workflows/` (the one running the cross-repo consumer loop)
- `palgebra/tools/resource_equations_to_mermaid.py` and `palgebra/examples/{ai-study,lemon-pie,decision-monad}-equations.txt`
- sim's `notes/ADRs.md` ADR-0016 (in the workflow's checkout of sim)

### Author rulings

1. Diff ignores provenance headers only. Sim's copies carry a prepended provenance comment block; the comparison strips exactly that block (or compares from the first non-provenance line) — no fuzzier normalization. Everything else must be byte-identical.
2. Failure names the remedy. The failing step's message says the files have drifted, names sim ADR-0016, and states the fix is a re-vendor session in sim (or a deliberate divergence ADR), never a silent edit on either side.
3. This is a check in the existing nightly workflow, not a new workflow.

### Steps

**Step 1 — Add the drift-check step.** In the nightly workflow, after the sim checkout step, add a step that for each of the four file pairs strips sim's provenance header and diffs against tools' copy; nonzero diff fails the job with ruling 2's message. Keep it as a small shell step or a script under `bin/`/`scripts/` per this repo's existing convention — follow whichever the workflow already uses.

Commit: `ci(nightly): drift check — palgebra copies vendored in ehr-testing-sim vs. authoritative copies here (sim ADR-0016)`

**Step 2 — Prove it fires.** Run the check locally (or via workflow_dispatch) twice: once as-is (expect pass), once with a one-character local mutation to a tools-side example file (expect fail with the ruling-2 message), then revert the mutation. Record both probe results in the final report.

**Step 3 — Archive this prompt.** Per this repo's prompt-archiving convention, with deviation appendix if any.

Commit: `prompts: archive 2026-07-27 palgebra drift-check session`

### Final report

Workflow step location, both probe results (pass and induced fail), deviations if any.

---

## Deviation-record appendix (Part A, this repo, as actually run 2026-07-27)

- **Step 0.** No deviation. Sibling checkout was present at exactly the pinned commit; `python3 --version` succeeded (3.8.10) — the skip-when-absent branch was not needed.
- **Step 4.** Ruling 5's STOP-AND-REPORT fired for real: the regeneration diff was nonempty. Analysis found two non-structural causes — (1) twelve `%% Arrow N` comment-number shifts, a pre-existing artifact of the equations file's line-position-derived numbering (already documented as non-structural by this file's own M4 MILESTONE note), and (2) one trailing blank line, traced via direct introspection of `generate_mermaid()` to an unconditional blank separator line emitted before the (here-empty) waste-sinks section — a converter quirk unrelated to vendoring. No node, wire, or style line differed. Per the ruling's own instruction to distinguish "reality disagrees with a sound check" from "check misencodes its invariant" before touching anything, this was surfaced to the author rather than silently resolved either way; the author chose "treat as reproduced, continue" (not to edit the committed diagram, and not to halt the session). Recorded here, not silently absorbed into a bare "byte-identical" claim.
- **Step 7.** No stray live references survived the sweep; no fix commit was needed.
- **This repo's own prompt-archive convention was empty before this session** (`.agents/prompts/archive/` did not exist — `docs/way-of-working.md` §3 already named this as an honest, standing gap). This session is the first to actually populate it, closing that gap rather than working around it.
- **Part B was not run in this session.** It is explicitly scoped to a separate session in the `ehr-testing-tools` repo; this session only touched `ehr-testing-sim`.
