# Archived prompt: busy-tuesday-exerciser-deferred (2026-08-14)

Original driving prompt below, verbatim. Executed as ADR-0130. The
row it charters was NOT landed as originally scoped -- see
`notes/adr/0130-busy-tuesday-exerciser-deferred.md` and this prompt's
own sibling session record for the full account: a real, previously-
undisclosed defect (`ehrt.sim-trajectory.gmf/slug` doesn't sanitize
commas out of raw upstream state names, breaking `ehrt play`'s own EDN
read-back for `uti/abx_tx.json`) blocked the busy-tuesday scenario's
own third fenced command mid-session. Two in-session author rulings
changed the scope from what is written below: (a) widened the fence to
license a minimal `demo-exerciser-fresh` marker parameterization once
the register was found genuinely inexpressible as pure data; (b) after
the slug defect surfaced, reduced the close to land only the
parameterization mechanism, the citation-gate test fix it forced, and
the skill sentence -- reverting the busy-tuesday register row/script/
Makefile line and closing this ADR partial-with-open-rows (the
ADR-0125 precedent), with two new roadmap rows recording the deferred
work and its own new prerequisite.

---

# Session prompt — busy-tuesday exerciser (ADR-0130)

You are Claude Code executing under R30 ceremony in
~/src/ehr-testing-tools. Autonomous; mg's rulings below are final.
Drafted by the design channel from a fresh public clone at HEAD
3b30aba (ADR-0129 close). Re-derive every claim. The tree wins.

## Read first

- .agents/plans/roadmap.md — the "Demo exerciser (busy-tuesday)"
  row IN FULL (it is the charter; its "re-derived live, never
  hardcoded" clause binds the design)
- demos/scenarios/busy-tuesday/README.md — whole file; the three
  fenced commands and the "What to look for" witnessed figures are
  the assertion source
- bin/demo-exerciser-ed-tuesday + ehrt.docs-tooling.strip-fresh +
  exercised-sources.edn — the pattern and register this session
  extends BY DATA, not new plumbing (the row's own words: "one more
  register row... without any code change, only data" — if you find
  a code change genuinely necessary, STOP and report why)
- notes/adr/0103, 0104 — the boundary-cadence fix and the
  ed/busy contrast the README cites
- .agents/skills/build-session/SKILL.md — binds this session

## Author rulings in effect

- Row chartered ADR-0120; this session ruled front-of-queue [A,
  2026-08-13, "busy-tuesday exerciser"].
- R3 [A, ADR-0113, verbatim]: "The demos must be known to work,
  and exercised as documented to make sure they actually play out
  as written." All three fenced commands run — including both play
  variants.
- Tag license CONDITIONAL: the channel could not confirm CI on the
  nine ADR-0129 commits (API rate-limited). bin/preflight's
  last-five-runs check is your verification: all green → licensed,
  lay `stable-20260813-strip-executability` at 3b30aba; any red or
  missing → STOP, report, NO tag.

## Standing practices (explicit text)

- Generative/defspec failure at any seed: NEW finding, STOP. Full
  `make test` before every push. Never fabricate output;
  anti-fabrication tripwire per skill. Step-0 receipts pasted
  before Step 1. Exec bits via `git update-index --chmod=+x`,
  verify 100755 before commit. Count-lock probe (register row
  count, bin/ census, README indexes). Red-before-green: freshness
  case red before the script exists (or co-land, disclosed — the
  ADR-0129 precedent). Verify-then-cite. ASCII. Session-record
  checkpoint commits are sanctioned when `make integration`'s
  tree-clean postcondition requires them (ADR-0129's discovered
  practice — this session also WRITES that sentence into the
  skill, Step 1(iv)).

## Step 0 — Ceremony + conditional tag

Fresh-clone parity; HEAD 3b30aba or STOP. bin/preflight — paste
output; nine ADR-0129 runs green → bin/tag-ceremony ANNOTATED
`stable-20260813-strip-executability` at 3b30aba --push, receipts
pasted. Oracle pre-digest, all 35 roots; predicted pure identity
(register data + one script + one skill sentence; zero pipeline
src).

## Step 1 — Exerciser + register row + wiring (commit 1)

(i) Register: one new row for
demos/scenarios/busy-tuesday/README.md →
bin/demo-exerciser-busy-tuesday, existing shape
(:demo-exerciser-fresh or :multi-fence — whichever the extraction
genuinely is; verify against the README's actual fence structure,
don't assume ed-tuesday's). Freshness case red-witnessed, then:
(ii) bin/demo-exerciser-busy-tuesday: expect/expect_eval shape;
cleans out/scenarios/busy-tuesday before generate (the README's
own non-empty-out-dir rejection); runs all three commands
verbatim; asserts the README's named invariants RE-DERIVED LIVE —
extract the "What to look for" closing-summary EDN and the
zero-inpatient claim from the README at runtime and subset-match
against the real run's output, excluding genuinely
run-volatile keys (:wallclock-ms; any other exclusion must be
argued in the record, and an excluded key that is actually
deterministic is a finding). Seed 20260807 determinism is the
contract: :emitted 68, :snapshot-count 48, :skip-count 41,
inpatients 0 must reproduce or the run is a FINDING — STOP,
report; no figure edits, no README edits.
(iii) Makefile integration: add the script after the five
ADR-0129 lines. Execute end-to-end once in-session, real
artifacts, tail pasted; note the added wallclock in the record
(the README's own witnessed ~3m39s per play — expect ~8 min
total; materially more is itself worth a line).
(iv) build-session SKILL.md (+ mirror, identical): ONE sentence
in the checkpoint-isolation section sanctioning session-record
checkpoint commits when make integration's tree-clean
postcondition requires them. Budget check per the lock BEFORE
committing (:sim is the tight set — verify current headroom
against 1495; over → STOP).
Commit: `feat: busy-tuesday exerciser -- register row, script,
integration wiring; checkpoint-commit practice in skill (ADR-0130)`

## Step 2 — Records + close (commit 2)

ADR-0130 + register line; roadmap: busy-tuesday exerciser row
CLOSED (every shipped scenario README now register-exercised —
note it against R3); rulings "From ADR-0130"; state.md;
bin/close-scaffold --expect-tag
stable-20260813-strip-executability@3b30aba; prompt self-archive
+ index entries; final `make integration` green on a clean tree
+ oracle bracket, results appended (checkpoint commits as needed,
sanctioned above).
Commit: `docs: session record and prompt archive -- busy-tuesday
exerciser (ADR-0130)`

## Fence

ONLY: bin/demo-exerciser-busy-tuesday (new);
exercised-sources.edn; Makefile `integration:` lines ONLY;
.agents/skills/build-session/ + .claude mirror (the one sentence
+ any formula-derived budget line, disclosed); notes/ADRs.md +
notes/adr/0130-*.md; .agents/ tree. NOTHING ELSE: no
docs-tooling src/test unless the register genuinely can't express
the row (STOP first), no README edits anywhere, no config.edn,
no other bin/, no .github/. Full `make test` green before each
push; `make integration` green once at close, clean tree.
Oracle: pure identity, all 35 roots. ASCII. STOP-AND-REPORT on:
CI red at preflight, invariant non-reproduction, register
inexpressibility, budget overrun, HEAD moved, red, tag anomaly.

Self-archive this prompt to .agents/prompts/ per convention.
