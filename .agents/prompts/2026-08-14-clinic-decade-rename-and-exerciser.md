# Archived prompt: clinic-decade-rename-and-exerciser (2026-08-14)

# Session prompt — busy-tuesday -> clinic-decade rename + exerciser
# completion (ADR-0132)

You are Claude Code executing under R30 ceremony in
~/src/ehr-testing-tools. Autonomous; mg's rulings below are final.
Drafted by the design channel from a fresh public clone at HEAD
c27bdd3 (ADR-0131 close; all four commits CI-green, channel API
verification). Re-derive every claim. The tree wins.

## Read first

- .agents/plans/roadmap.md — the rename+exerciser row (UNBLOCKED
  2026-08-14) IN FULL
- notes/adr/0130-*.md — the deferred exerciser design + Appendix
  (the drafted script this session adapts) and the slug STOP
  narrative; notes/adr/0131-*.md — the fix that unblocked command 3
- demos/scenarios/busy-tuesday/ + ed-tuesday READMEs + demos/README
  + demos/scenarios/README — the rename surface
- bases/cli/src/ehrt/cli/help.clj + its core_test — help-string
  count-locks BEFORE fencing assumptions (the ADR-0126 lesson)
- components/docs-tooling: exercised-sources.edn, strip_fresh.clj,
  demo_exerciser_fresh.clj + all three test namespaces — marker
  fixtures and register mechanics
- components/corpus/docs/use-cases.edn + Makefile docsgen list —
  generated-page companions
- .agents/skills/{build-session,errata-sweep}/SKILL.md — bind

## Author rulings in effect (verbatim)

- Name [A, 2026-08-13, "clinic-decade it is."]: the scenario is
  renamed busy-tuesday -> clinic-decade. Frozen records (notes/adr/
  bodies, session records, prompt archives, register history lines)
  KEEP the old name; this session's ADR carries the mapping.
- Exerciser design [A, ADR-0130 ruling chain, unchanged]: all three
  README commands run verbatim; invariants re-derived live from the
  README at runtime, never hardcoded; :wallclock-ms the only
  presumed-volatile key (any other exclusion argued in the record);
  out-dir cleaned before generate.
- R3 [A, ADR-0113]: demos exercised as documented.
- Tag license: ADR-0131's four commits CI-verified green (channel
  API + preflight re-check). Lay `stable-20260814-slug-fix` at
  c27bdd3 in Step 0.

## Channel pre-probe (at ef15885 — RE-DERIVE at HEAD; ADR-0131 may
## have added reference sites)

22 live-reference files incl.: 4 READMEs, both scenario config.edn,
use-cases.edn (+ generated play-a-generated-corpus page),
bases/cli/help.clj (EMITTED text -> docs/cli.md regen + help-string
locks), docs-tooling src comments + test marker fixtures (the
honestly-named busy-tuesday markers from ADR-0130's parameterization
tests), exercised-sources.edn mechanism comment, roadmap/rulings/
state. Frozen records excluded by rule.

## Standing practices (explicit text)

Generative failure at any seed: NEW finding, STOP. Full `make test`
before every push. Never fabricate; tripwire per skill. Step-0
receipts. Exec bits via `git update-index --chmod=+x`, verify
100755. Count-lock probe: register row count, bin/ census, help
strings, README indexes, marker fixtures. Red-before-green on the
freshness case. Sweep inventory discipline: full rename inventory
re-derived, one session, no residue (ADR-0099 form); any site
beyond the pre-probe widens in, disclosed. Verify-then-cite. ASCII.
Checkpoint commits sanctioned per skill. Move-don't-improve: the
rename changes NAMES, not content — the one sanctioned improvement
is the rename itself.

## Step 0 — Ceremony + tag

Fresh-clone parity; HEAD c27bdd3 or STOP. preflight; tag-ceremony
ANNOTATED `stable-20260814-slug-fix` at c27bdd3 --push, receipts.
Oracle pre-digest all 35 roots; predicted end-state: pure identity
(rename touches no engine behavior; out/ paths are ungenerated at
oracle time — VERIFY no digested artifact embeds the scenario path
string before relying on this; if any does, that is predicted
movement to declare, Step 1).

## Step 1 — Rename sweep (commit 1)

git mv demos/scenarios/busy-tuesday demos/scenarios/clinic-decade.
Full inventory sweep of live references: READMEs (incl. both
cross-refs and any "busy Tuesday" prose framing — retitle the
scenario's own README so the name and its teaching text agree —
content edits beyond naming are OUT), config path strings,
use-cases.edn -> make docsgen regen companions in SAME commit,
help.clj emitted text -> cli.md regen + any help-string lock
companion named, docs-tooling comments + marker fixture strings in
tests (marker text becomes "BEGIN clinic-decade commands..." —
tests must still prove parameterization against a non-default
marker), exercised-sources comment, roadmap/rulings/state live
mentions. Frozen records untouched. Residue check: repo-wide grep
for busy-tuesday outside frozen paths must return ZERO before
commit.
Commit: `refactor: rename busy-tuesday scenario to clinic-decade
-- full live-reference sweep (ADR-0132)`

## Step 2 — Exerciser completion (commit 2)

Adapt ADR-0130's Appendix script as bin/demo-exerciser-clinic-decade
(new name throughout, marker text to match). Register row
(:demo-exerciser-fresh with explicit marker-open/marker-close —
the ADR-0131-era parameterization now carries it as data);
freshness case red-witnessed (script absent) then green; register
count-lock bump named. Makefile integration line. Execute
end-to-end in-session: all three commands, README-witnessed
figures (68/48/41, inpatients 0) must reproduce — drift is a STOP,
no figure/README edits. Note added lane wallclock.
Commit: `feat: clinic-decade exerciser -- register row, script,
integration wiring; all three commands witnessed (ADR-0132)`

## Step 3 — Records + close (commit 3)

ADR-0132 (rename mapping table old->new for every renamed path;
the name ruling verbatim; exerciser evidence); roadmap: rename+
exerciser row CLOSED — note R3 fully discharged: every shipped
scenario README register-exercised; rulings "From ADR-0132" (the
clinic-decade ruling verbatim); state.md; close-scaffold
--expect-tag stable-20260814-slug-fix@c27bdd3; prompt self-archive
+ indexes; final make test + make integration green, clean tree;
oracle bracket appended (pure identity expected per Step 0's
verified prediction).
Commit: `docs: session record and prompt archive -- clinic-decade
rename and exerciser (ADR-0132)`

## Fence

ONLY: demos/scenarios/** (the mv + README/config name edits);
bin/demo-exerciser-clinic-decade (new); exercised-sources.edn;
Makefile integration lines; bases/cli/help.clj (name strings only)
+ regenerated docsgen pages as named companions; use-cases.edn;
docs-tooling src comment strings + test marker fixtures (fixture
strings only — no assertion-logic changes beyond the string);
count-lock companions, each named; notes/ADRs.md + 0132;
.agents/ tree. NOTHING ELSE: no gmf.clj, no module JSONs, no
frozen records, no skills, no .github/. Full make test before
every push; make integration green at close. Oracle: per Step 0's
verified prediction. ASCII. STOP-AND-REPORT on: figure drift,
residue grep nonzero, unpredicted oracle movement, lock surprises
beyond named classes, HEAD moved, red, tag anomaly.

Self-archive this prompt to .agents/prompts/ per convention.
