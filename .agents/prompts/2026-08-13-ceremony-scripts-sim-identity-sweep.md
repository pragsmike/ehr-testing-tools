# Session prompt — ceremony scripts + build-session absorption --
# citation sweep and glossary linkage (ADR-0127)

You are Claude Code executing under R30 ceremony in
~/src/ehr-testing-tools. Autonomous; mg's rulings below are final.
Drafted by the design channel from a fresh public clone at HEAD
04ad5af (2026-08-13, ADR-0126 close). Every behavioral claim is
current (verify) — re-derive from the live tree. The tree wins.

## Read first

- .agents/rulings.md — "From ADR-0122" R13 (the charter, verbatim
  scope) and "From ADR-0126"
- .agents/plans/roadmap.md — ceremony-scripts row, sim-identity row
- notes/adr/0126-citation-sweep-glossary-linkage.md — the
  classification recipe, the near-miss (help.clj:471 blanket-sed),
  the index-completeness catch; this session reuses all three
  lessons
- .agents/skills/build-session/SKILL.md (187 lines, current
  (verify)) + .agents/skills/errata-sweep/SKILL.md — Step 1 runs
  under the errata-sweep skill's discipline
- components/docs-tooling/test/ehrt/docs_tooling/
  tag_law_test.clj, index_completeness_test.clj,
  skill_mirror_currency_test.clj — the three locks this session's
  artifacts must satisfy; read WHOLE before writing any script or
  skill edit
- .agents/skills/wsl-windows-git-hygiene/SKILL.md — edit-root
  confirmation and exec-bit doctrine for Step 2
- bin/regression-oracle and one other existing bin/ script — house
  style for the new scripts
- Evidence-base ADRs for what the scripts must prevent:
  notes/adr/0120-*.md (slug drift), 0124 (silently skipped tag,
  repaid in 0125), plus ADR-0126's session record (the
  index-completeness late catch)

## Author rulings in effect (verbatim)

- R13 charter [A, 2026-08-13, "Both a." part (b)]: ceremony —
  "tag ceremony, preflight (last-five-CI-runs check, edit-root
  confirmation), post-push message verification, and the
  close-phase scaffold (self-archive, session record, prompt
  archive, index bump) — moves from prose a session re-reads each
  time to scripts, with checkpoint isolation, red capture, and
  sweep census absorbed into the build-session skill alongside
  them."
- Sim-identity sweep folded into this session [A, 2026-08-13,
  "Fold it in."].
- Script granularity [A, 2026-08-13, "Q1 a."]: four one-purpose
  scripts — bin/preflight, bin/tag-ceremony, bin/post-push-verify,
  bin/close-scaffold — matching existing bin/ one-purpose style.
- Sweep scope [A, 2026-08-13, "Q2 b."]: ALL bare ADR-NNNN across
  the sim-doc file set, classified and qualified in one pass, not
  ADR-0010 alone.
- Tag license: the design channel verified the ADR-0126 landing by
  fresh clone (three commits, ASCII, lineage, CI green on all
  three — runs confirmed via API 2026-08-13). Tag instructed in
  Step 0.

## Standing practices (explicit text)

- Any generative/defspec failure at ANY seed is a NEW finding —
  STOP. No re-run license.
- Full `make test` before EVERY push.
- Never fabricate output; every result in the record is pasted
  from a real run.
- Count-lock probe before touching any cataloged collection:
  bin/ contents, skill file sets, README indexes, and — Step 1
  specifically — any test or tool that parses/hashes the sim-doc
  files (bin/check-palgebra-drift cites sim-theory.edn; grep the
  test tree for locks on all six files before editing).
- Gate-forced companions land in-fence and are NAMED: the
  .claude/skills mirror (skill_mirror_currency_test), README index
  entries for every new file (index_completeness_test — ADR-0126's
  own late catch, now anticipated, not discovered).
- Verify-then-cite every path:line this prompt names.
- Cross-commit ordering: scripts (commit 2) land BEFORE the skill
  that references them (commit 3).
- Sweep inventory discipline: the channel's site census below is
  expected to UNDERCOUNT — re-derive it yourself.
- Double-check your own tag ceremony in your transcript before
  closing.

## Step 0 — Ceremony + tag payment

Fresh-clone parity. Confirm HEAD 04ad5af; if moved, STOP and
report. Lay ANNOTATED tag `stable-20260813-citation-sweep` at
04ad5af, ASCII message referencing ADR-0126 and the channel's CI
verification; push; verify peeled ref against remote equals
04ad5af exactly. Oracle pre-digest: all 35 roots; expected
end-state this session is pure identity (no behavioral src edits
anywhere in the plan).

## Step 1 — Sim-identity citation sweep (commit 1)

Under the errata-sweep skill. Channel census, current (verify),
expected to undercount: 17 bare ADR-0010 sites across
components/sim/docs/{event-sourcing.md (5),
patient-state-model.md (7), sim-theory.md (1), sim-theory.edn (1,
inside the :contract string at ~line 78 — which ALSO carries a
bare ADR-0011 with identical drift)} and
components/sim-trajectory/docs/{gmf-interpreter.md (1),
trajectory-computation.md (2)}.

1a. INVENTORY: all bare `ADR-NNNN` (any number) across those six
files, plus any sibling file in the same two docs/ trees the grep
surfaces. Classify each: sim-era referent → `sim/ADR-NNNN`
(link target notes/sim/ADRs.md where a link exists); workspace
referent → correctly bare, untouched; tools-era → `tools/ADR-NNNN`.
Trace every referent to the actual record before rewriting —
ADR-0126's recipe. No blanket seds (the help.clj:471 near-miss).
Ambiguous → STOP.

1b. Before editing sim-theory.edn: prove the :contract string is
prose-consumed only (grep tests and bin/check-palgebra-drift for
content hashes or string locks on it). Run
bin/check-palgebra-drift after the edit if it covers these files.
Any runtime/lock consumption of the edited string → STOP.

Commit: `docs: sim-identity citation sweep -- origin-qualify
sim-era ADR citations in sim docs (ADR-0127)`

## Step 2 — Four ceremony scripts (commit 2)

bin/preflight, bin/tag-ceremony, bin/post-push-verify,
bin/close-scaffold. Bash, house style per existing bin/, tracked
100755 (verify mode with `git ls-files -s bin/` before commit —
the exec-bit evidence class). Each: `--help`, deterministic
output, no network beyond git/gh, and NEVER auto-push without an
explicit flag. Required behaviors, from the evidence base:

- preflight: last-five-CI-runs check (green/red per run);
  edit-root confirmation (the WSL2 root, per
  wsl-windows-git-hygiene); tree-clean check that counts
  UNTRACKED files too (the tree-clean false-positive class);
  HEAD-vs-remote tip match; last stable-* tag and whether HEAD is
  tagged.
- tag-ceremony: takes tag name + target sha + message; validates
  slug against the stable-YYYYMMDD-<slug> pattern and ASCII
  (ADR-0120 slug drift); creates ANNOTATED tag; pushes only with
  --push; ALWAYS finishes by verifying the peeled ref against the
  remote (the ADR-0124 skipped-tag class — the verify half is not
  optional).
- post-push-verify: confirms remote tip == local HEAD, per-commit
  ASCII check over the pushed range, and reports the CI run
  triggered for the tip (poll once, print status, don't wait).
- close-scaffold: scaffolds session record + prompt archive
  filenames per convention AND the README index entries for both
  (index_completeness_test's requirement, generated not
  hand-remembered); idempotent; touches nothing outside .agents/.

Read tag_law_test and index_completeness_test FIRST and align the
scripts' checks with the tests' actual rules — encode by reference
to the same convention, don't fork a second definition. Smoke
each script in-session (--help + one real or dry-run invocation)
and paste output into the record. If a census/count lock on bin/
exists, the companion edit lands in this commit, named.

Commit: `feat: ceremony scripts -- preflight, tag-ceremony,
post-push-verify, close-scaffold (ADR-0127)`

## Step 3 — build-session skill absorption (commit 3)

.agents/skills/build-session/SKILL.md AND the .claude/skills
mirror, identical (skill_mirror_currency_test): absorb checkpoint
isolation, red capture, and sweep census as skill sections;
rewrite the ceremony prose sections to invoke the four scripts by
name instead of restating their steps (one definition, in the
script; the skill points at it). Keep the skill's own length
budget in mind — check reading_set_budget_test for a lock.

Commit: `docs: build-session skill absorbs checkpoint isolation,
red capture, sweep census; ceremony via bin/ scripts (ADR-0127)`

## Step 4 — Records + close (commit 4)

ADR-0127 + register line (disclose: full classified sim-sweep
inventory with per-class counts including the ADR-0011 member and
any widening; script smoke evidence; any count-lock companions);
rulings register "From ADR-0127" (verbatim rulings above);
roadmap: ceremony-scripts row CLOSED, sim-identity row CLOSED
(dim-1 strip-executability row stays open, untouched);
.agents/state.md; session record + prompt archive WITH their
README index entries (use your own new bin/close-scaffold for
this — its first real use is its own smoke test); self-archive
this prompt.

Commit: `docs: session record and prompt archive -- ceremony
scripts and sim-identity sweep (ADR-0127)`

## Fence

ONLY: bin/ (the four new scripts); the six sim-doc files + any
inventory-widened sibling in the same two docs/ trees, disclosed;
.agents/skills/build-session/ + .claude/skills/build-session/;
notes/ADRs.md + notes/adr/0127-*.md; .agents/ tree. NOTHING ELSE.
Zero edits to src/, test/, demos/, docs/ outside the named files,
frozen registers, any existing bin/ script, Makefile, .github/.
Exception path: a count-lock-forced test companion is allowed
ONLY if it is a census/index lock (never behavioral), named in
the record — anything else STOPs. Full `make test` green before
each of the four pushes. Oracle: pure identity, all 35 roots.
ASCII commit messages. STOP-AND-REPORT on: classification
ambiguity, sim-theory.edn lock/runtime consumption, HEAD moved,
any test red, any generative-seed failure, tag anomaly.

Self-archive this prompt to .agents/prompts/ per convention.
