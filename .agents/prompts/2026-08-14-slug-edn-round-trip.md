# Archived prompt: slug-edn-round-trip (2026-08-14)

Original driving prompt below, verbatim. Executed as ADR-0131, all
five steps landed as scoped -- no STOP-AND-REPORT this session. The
declared-oracle-change prediction (Step 1) matched the official `bin/
regression-oracle` bracket (Step 4) exactly: 3 roots moved, 1
structurally-eligible root correctly predicted NOT to move (grep-
confirmed unreached at its own seed/population), 5 roots warned with
zero byte movement, 27 roots untouched. Acceptance held: busy-tuesday's
own README-witnessed figures (68/48/41, `inpatients: 0`) reproduced
byte-for-byte, and the README's own third command -- the one ADR-0130
found broken -- completed for the first time ever. One found-and-
disclosed discrepancy against the channel's own pre-probe: the
collision census's "8 modules" figure was wrong (actual 5, pair count
and per-module breakdown otherwise exact) -- see `notes/adr/
0131-slug-edn-round-trip.md` for the full account.

---

# Session prompt — slug EDN round-trip fix + injectivity guard
# (ADR-0131)

You are Claude Code executing under R30 ceremony in
~/src/ehr-testing-tools. Autonomous; mg's rulings below are final.
Drafted by the design channel from a fresh public clone at HEAD
ef15885 (ADR-0130 close). Re-derive every claim. The tree wins.
THIS SESSION CHANGES ENGINE BEHAVIOR — the declared-oracle-change
ceremony binds; pure identity is NOT the predicted end-state.

## Read first

- .agents/plans/roadmap.md — the slug row IN FULL (the charter; this
  session also widens it with defect 2, dated) and the rename+
  exerciser row (unblocked by this session; do not touch its work)
- components/sim-trajectory/src/ehrt/sim_trajectory/gmf.clj —
  slug (~:45-55), kebab-key, and the module-load path that will
  carry the guard
- notes/adr/0130-*.md — the defect discovery narrative
- The declared-oracle-change standing law and the oracle mechanism
  docs/scripts (bin/regression-oracle + wherever baselines and
  declarations are recorded — read before predicting)
- notes/ADRs.md ADR-0071 (AR-VB2-R vendoring rider precedent —
  cited by the new rider row, NOT executed this session)
- .agents/skills/build-session/SKILL.md — binds throughout

## Author rulings in effect (verbatim)

- Q1 [A, 2026-08-13, "Q1 a."]: sanitization = fold exactly the
  non-EDN-keyword-legal characters to `-`, collapse runs, trim
  edge hyphens. EDN legality defines the fold set; nothing more.
- Q2 [A, 2026-08-13, "Q2 b."]: injectivity guard lands WARN-mode —
  loud per-collision warning at module load, load proceeds;
  escalation to hard-error is chartered into the new rider row,
  triggered by that row's per-pair module corrections landing.
  Module JSONs are NOT edited this session (vendored verbatim,
  ADR-0071).
- Slug row chartered ADR-0130; front-of-queue by sequencing.
- Tag license CONDITIONAL: bin/preflight green on the three
  ADR-0130 runs (b3483dc, 06aec01, ef15885) → lay
  `stable-20260813-busy-tuesday-deferral` at ef15885; any red →
  STOP, no tag.

## Channel pre-probe (2026-08-13, public tree at ef15885 — expected
## substantially right, MUST be re-derived, discrepancies disclosed)

- Defect 1 breakers (10 specimens, 3 modules): uti/abx_tx.json x5
  (commas), injuries/broken_jaw.json x1 (comma),
  veteran_lung_cancer.json x4 (parens). `?` `'` `&` are legal
  keyword constituents — out of scope under Q1(a).
- Defect 2 collisions under CURRENT slug (10 pairs, 8 modules):
  hypothyroidism, veteran_ptsd x2, colorectal_cancer, sleep_apnea
  x4, injuries x2. Verified specimen: sleep_apnea's
  'Home CPAP Unit' / 'Home_CPAP_Unit' — distinct Device states,
  different transition structures, each referenced twice; current
  loader silently drops one.
- Known breaker/collision-module consumers:
  demos/scenarios/busy-tuesday/config.edn,
  projects/conformance/test-fixtures/sim-configs/
  full-capability.edn. The full configs→roots map is Step 1's job.

## Standing practices (explicit text)

Generative failure at any seed pre-existing this session's changes:
NEW finding, STOP (your own new property test going red against
OLD code is the point, not a finding). Full `make test` before
every push. Never fabricate; tripwire per skill. Step-0 receipts.
Count-lock probe (test-tree locks on gmf state counts, module
counts, warning text). Verify-then-cite. ASCII. Checkpoint
commits sanctioned per skill. Red-before-green MANDATORY here:
the property test and guard test are witnessed red against
pre-fix code (stash-isolation per the ADR-0130 worked example)
before any fix lands.

## Step 0 — Ceremony + conditional tag

Fresh-clone parity; HEAD ef15885 or STOP. preflight → conditional
tag per license above, receipts pasted. Oracle PRE-digest all 35
roots recorded — this is the baseline the declaration measures
against.

## Step 1 — Census + oracle prediction (commit 1, docs-only)

Re-derive both defect censuses across all 66 module JSONs
(recursive; the channel's numbers above are the cross-check).
Map every oracle root's config → module set → predicted movement:
a root moves iff its modules include a defect-1 breaker (fold
changes compiled keywords → events.edn/ground-truth bytes).
Defect-2 warn-mode moves NOTHING (warning is stderr, not
artifact bytes — verify this claim against how the oracle
captures output before relying on it; if warnings land in
digested artifacts, that IS predicted movement, declare it).
Record the prediction as the declared-oracle-change declaration
in the ADR draft and roadmap row BEFORE any src edit.
Commit: `docs: slug defect census and declared-oracle-change
prediction (ADR-0131)`

## Step 2 — Red (no commit; stash-isolated witness)

(i) Generative property test: for arbitrary raw GMF name strings
(generator must produce commas, parens, brackets, mixed
whitespace/underscore runs, edge punctuation), the round-trip law
holds: (= k (edn/read-string (pr-str k))) for the slug-derived
keyword, AND fold idempotence (slug(slug-output) = slug-output).
(ii) Guard test: loading each pre-probed collision module emits
the per-collision warning naming module, folded key, and both raw
names; a collision-free module emits none.
Witness both RED against pre-fix code, pasted.

## Step 3 — The fix (commit 2)

slug per Q1(a) — fold set = complement of EDN keyword-legal
constituents, collapse `-` runs, trim edges; docstring updated
with the law and the fold-set rationale. Collision guard
WARN-mode in the module-load path per Q2(b) — detection at
post-slug key assembly, warning per pair, load proceeds; guard
code structured so the rider session's escalation is a mode
switch, not a rewrite. Both tests green. Full `make test`; any
OTHER test red is a finding — STOP (count-locks on state names
may legitimately trip: those are census-class companions, update
only with the specific lock named in the record).
Commit: `fix: slug folds non-EDN-legal chars; module-load
collision guard (warn) -- emit-read identity restored (ADR-0131)`

## Step 4 — Oracle verdict + acceptance (commit 3)

Oracle POST-digest all 35 roots. Movement must match Step 1's
prediction EXACTLY — any unpredicted root moving, or predicted
root not moving, is a STOP (either the census or the fix is
wrong). Record prediction vs. actual side by side. Re-baseline
per the declared-change ceremony's own mechanism.
ACCEPTANCE: regenerate busy-tuesday (seed 20260807, 200
patients) and run the README's third command —
`bin/ehrt play .../events.edn --rate 100000` — to completion;
paste the tail. If any README-witnessed figure (68/48/41,
inpatients 0) changes, STOP — no README edits, no figure edits.
Commit: `test: oracle re-baseline per declaration; busy-tuesday
events.edn playback witnessed green (ADR-0131)`

## Step 5 — Records + close (commit 4)

ADR-0131 (census tables, prediction-vs-actual, the ruling
verbatim, the warn→error escalation trigger); roadmap: slug row
CLOSED, NEW vendoring-rider row (per-pair corrections across the
8 collision modules, AR-VB2-R form, guard escalation to error
rides it), rename+exerciser row marked UNBLOCKED; rulings "From
ADR-0131"; state.md; close-scaffold --expect-tag
stable-20260813-busy-tuesday-deferral@ef15885; prompt
self-archive + indexes; final make test + make integration
green, clean tree.
Commit: `docs: session record and prompt archive -- slug fix and
injectivity guard (ADR-0131)`

## Fence

ONLY: components/sim-trajectory/{src,test} (gmf.clj + the
module-load path + new/extended tests); census-class count-lock
companions, each named; notes/ADRs.md + notes/adr/0131-*.md;
.agents/ tree; oracle baseline files per the declared-change
ceremony ONLY as that ceremony's own mechanism directs. NOTHING
ELSE: no module JSONs, no README/docs edits, no docs-tooling, no
bin/ (except nothing), no Makefile, no skills. STOP-AND-REPORT
on: prediction/actual oracle mismatch, README figure drift,
pre-existing generative failure, unexpected red, guard
inexpressibility without wider refactor, HEAD moved, tag anomaly.

Self-archive this prompt to .agents/prompts/ per convention.
