# Session prompt -- strip executability: exercisers, citation gate,
# ADR-0127 erratum (ADR-0129)

You are Claude Code executing under R30 ceremony in
~/src/ehr-testing-tools. Autonomous; mg's rulings below are final.
Drafted by the design channel from a fresh public clone at HEAD
56613c7 (ADR-0128 close; all four commits CI-green, verified by
author gh list + channel API). Re-derive every claim. The tree wins.

## Read first

- .agents/plans/2026-08-13-manual-review-1.md -- dimension 1 IN FULL
  including the per-chapter coverage-class table; this session
  closes it
- .agents/skills/build-session/SKILL.md + session-prompt/SKILL.md --
  including the new anti-fabrication tripwire and Step-0 receipts
  sections; they bind this session
- components/docs-tooling/src+test: demo_exerciser_fresh.clj,
  quickstart_fresh.clj and both tests -- the pattern being extended
- bin/demo-exerciser-ed-tuesday + bin/quickstart-demo -- the
  expect/expect_eval wrapper shape the five new scripts reuse
- Makefile `integration:` target; docs/use-cases/ the four cited
  pages; README.md "What you get" (lines ~84-155, current (verify))
- notes/adr/0127-*.md Step 3 (the wrong 1170 figure) + its existing
  addendum; notes/adr/0121-*.md erratum form
- notes/adr/0120-*.md -- R3's exerciser mandate, the charter
  authority for extension

## Author rulings in effect (verbatim)

- Dim-1 fix design [A, 2026-08-13, "Q1 a. Q2 a. Q3 a. Q4 a."]:
  Q1(a) per-source scripts -- five new bin/ exercisers on the proven
  pattern, PLUS the citation gate; Q2(a) env-var placeholders are
  the sanctioned strip parameterization, exercisers bind fixtures;
  Q3(a) exercise exactly the five cited sources, the gate enforces
  cited-implies-exercised for the future; Q4(a) What-you-get
  extraction pairs command fences with adjacent expected-output
  fences and compares output.
- 1170 erratum [A, 2026-08-13, "Do b"]: dated erratum appended to
  ADR-0127 -- Step 3's `:sim` 1170/1295 was arithmetically wrong
  when recorded, true 1293/1295, see ADR-0128.
- Standing directive [A, 2026-08-13, verbatim]: "let's always look
  for opportunities to improve the agent-facing parts."
- Tag license: 56613c7 CI-verified green. Tag in Step 0.

## Standing practices (explicit text)

- Any generative/defspec failure at ANY seed: NEW finding, STOP.
- Full `make test` before EVERY push. Never fabricate output.
- Anti-fabrication tripwire (skill): drafting a skip-justification
  IS the stop.
- Step-0 receipts: paste tag-ceremony's full output into the
  session-record draft BEFORE Step 1.
- Exec bits on new scripts via `git update-index --chmod=+x`
  (core.fileMode=false); verify `git ls-files -s bin/` shows 100755
  before each commit containing scripts.
- Count-lock probe: bin/ census, README index locks, and -- new
  surface -- any lock on Makefile targets or docs/manual citation
  tables. Grep before fencing assumptions.
- Red-before-green on every new test: witness the red (or the
  pre-state failure, pasted from a real scratch run) before the
  green lands.
- Cross-commit ordering: extraction+register (commit 2) before
  scripts that depend on it (commit 3) before the gate that reads
  the register (commit 4).
- Verify-then-cite; ASCII; budget headroom pre-check for the ADR
  and roadmap additions (skills untouched this session -- no
  reading-set growth expected; verify).

## Step 0 -- Ceremony + tag

Fresh-clone parity. HEAD 56613c7 or STOP. bin/tag-ceremony:
ANNOTATED `stable-20260813-hardening` at 56613c7, --push, paste
full output (receipts). Oracle pre-digest, all 35 roots; predicted
end-state pure identity -- docs-tooling is not a pipeline root, and
no pipeline src is in this fence.

## Step 1 -- ADR-0127 erratum (commit 1)

Append-only dated erratum to notes/adr/0127-*.md, 0121 form:
Step 3's ":sim 1170/1295... none needing a bump" was arithmetically
wrong when recorded -- the five :sim paths at 21114e3 summed to
1293/1295, two lines of headroom -- discovered ADR-0128 when a
+5-line skill edit tripped the real headroom; budget since
re-derived to 1495, see ADR-0128. Register-line marker on 0127's
notes/ADRs.md entry per convention.
Commit: `docs: ADR-0127 erratum -- Step 3 sim reading-set figure
was wrong when recorded (ADR-0129)`

## Step 2 -- Extraction extension + exercised-sources register
## (commit 2)

docs-tooling src+test, co-landed, red witnessed first:
(i) Command/output fence pairing: extend the extraction layer so a
```bash (or ```sh) command fence immediately followed (blank lines
allowed between) by a non-command fence (```clojure etc.) yields
(command, expected-output) pairs; sources without paired output
keep today's command-only shape. Do not change existing extractor
behavior for ed-tuesday/quickstart -- their tests must pass
UNMODIFIED (if any existing test needs editing, STOP: that is a
behavior change, not an extension).
(ii) exercised-sources register: a docs-tooling-owned EDN resource
listing each exercised source: {source path, script path, marker
pair, env bindings}. Seed with the two existing pairs (README
Quickstart / bin/quickstart-demo; ed-tuesday README /
bin/demo-exerciser-ed-tuesday) plus the five new entries (scripts
land next commit -- the register may lead the scripts WITHIN this
session only because commit 3 lands before any gate reads it;
disclose the one-commit window in the record).
(iii) A generalized strip-fresh check parameterized by register
entry; five new freshness test cases (red until commit 3's scripts
exist -- witness one red, pasted, then land scripts in commit 3;
if you judge red-spanning-commits unacceptable, reorder so scripts
and freshness tests co-land -- your discretion, disclosed).
Commit: `feat: strip extraction pairs output fences; exercised-
sources register and generalized freshness check (ADR-0129)`

## Step 3 -- Five exercisers + integration wiring (commit 3)

bin/usecase-judge-tier-calibration, bin/usecase-profile-tier-v2,
bin/usecase-acceptance-qa, bin/usecase-regression-baselining,
bin/readme-what-you-get -- names your discretion within bin/ house
style, register updated to match. Each: expect/expect_eval wrapper
shape, BEGIN/END markers per register, commands verbatim from
source. acceptance-qa binds VENDOR_CORPUS to a committed fixture
(test-fixtures/v2 or better -- verify intake accepts it).
what-you-get compares paired expected output (Q4 a) -- normalize
only what quickstart-demo already normalizes; no looser.
Wire all five into Makefile `integration:` after the ed-tuesday
line. Exec bits per practice. EXECUTE each end-to-end once,
in-session, real artifacts (bin/ehrt artifact fetch as needed --
Synthea, JDK, FHIR validator for the fhir gate; NIST jars are
lock-verified already); paste each run's tail. Any strip that
fails to execute verbatim is a FINDING about the strip -- STOP,
report, no silent doc edits.
Commit: `feat: five strip exercisers wired into integration --
use-case pages and README what-you-get covered (ADR-0129)`

## Step 4 -- Citation gate (commit 4)

docs-tooling test: every "Strip source citations" table entry in
docs/manual/0*.md resolves to a register entry (or is
Quickstart/ed-tuesday-covered via the same register). Red
witnessed against the pre-session register state (scratch eval,
pasted), green against the live one. Gate failure message must
name the offending chapter, the cited source, and the register
path -- actionable, not cryptic.
Commit: `feat: citation gate -- manual strip sources must be
register-exercised (ADR-0129)`

## Step 5 -- Re-score + records + close (commit 5)

Targeted manual-review re-run, DIMENSION 1 ONLY, scored with
file:line evidence -- expect PASS (all three "neither" rows now
covered); record in the review-run file per the dim-4 precedent.
ADR-0129 + register line; roadmap: dim-1 row CLOSED, manual arc
fully green (note it -- first all-dimensions-addressed state),
busy-tuesday exerciser row now references the register mechanism;
rulings "From ADR-0129" verbatim; state.md; close via
bin/close-scaffold --expect-tag stable-20260813-hardening@56613c7;
prompt self-archive + index entries.
Commit: `docs: session record and prompt archive -- strip
executability closed (ADR-0129)`

## Fence

ONLY: components/docs-tooling/{src,test,resources}; the five new
bin/ scripts; Makefile `integration:` target lines ONLY;
notes/adr/0127-*.md (APPEND-ONLY), 0129, notes/ADRs.md;
.agents/ tree; .agents/plans/2026-08-13-manual-review-1.md
(dim-1 re-score append). NOTHING ELSE: no docs/manual chapter
edits, no use-cases.edn, no README.md, no demos/, no
test-fixtures/ additions without STOP (if acceptance-qa needs a
new fixture, STOP and propose), no .github/, no other bin/, no
skills. Full `make test` green before every push; `make
integration` green once after commit 3 and once at close. Oracle:
pure identity, all 35 roots. ASCII. STOP-AND-REPORT on: any strip
failing verbatim execution, extractor behavior change breaking an
existing test, fixture gaps, budget overrun, HEAD moved, red, tag
anomaly.

Self-archive this prompt to .agents/prompts/ per convention.
