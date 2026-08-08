## ADR-0077 — Repo review 1: the first assessment — every lens, nothing moved

**Status:** Accepted (author-directed, autonomous session per R30), 2026-08-07.

### Context

Prior: `notes/adr/0076-quality-riders.md` opened the quality-review
arc — the `repo-review` skill landed, the `merge-config-file` flake got
a mechanism fix, preflight widened to five CI runs, and the assessment
session's own prompt was named as the arc's next act. This session
IS that assessment: the skill's own steps 1–4 (history scan, probe
battery, dated register, scoreboard) — steps 5–7 (the mitigation plan,
the ruled fix sessions, the arc close) follow AFTER the author rules
on the register this session lands, per the skill's own survey
discipline (ADR-0049/0058's "examined, registered, NOTHING MOVED").

R30 ceremony. Read-first (this session): `.agents/skills/repo-review/
SKILL.md` in full; the audit-register format exemplars
(`.agents/plans/2026-08-05-alignment-audit-findings.md`,
`2026-08-06-ux-audit-findings.md`); every arc-close ADR (0047, 0055,
0064, 0068, 0074) and every session record's own "Deviations,
disclosed" section landed since the repo's start, read through the
closes-and-deviations lens rather than the full 76-file per-ADR
corpus (the wrong unit for this session's own scope); ADR-0075/0076
in full (the freshest incident cluster and the seeded watch-list).

### Decision

Author rulings, recorded verbatim (this session's own driving prompt,
2026-08-07). `[A]` author-ruled, `[C]` channel-inferred.

**AR-RR-0 `[A — tag law, case (ii); debt recorded in ADR-0076]`.**
Annotated `stable-20260807-quality-riders` at `89c0d24`, message
"quality riders landed, design-channel-verified 2026-08-07
(ADR-0076)"; pushed; peeled ref verified (`git ls-remote --tags
origin` resolves `stable-20260807-quality-riders^{}` to `89c0d24`
exactly). **Executed Step 0, this session.**

**AR-RR-1 `[A — the author's review mandate, design channel
2026-08-07; C for scoping]` (the survey).** Run the skill's steps 1–4
in full. First-run specifics: the history window is the repo's whole
life, read through the closes-and-deviations lens; the scoreboard
carries NO prior column — this run IS the baseline the second review
carries forward. The register lands at `.agents/plans/2026-08-07-
repo-review-findings.md` in the established audit-register format:
one row per finding — dimension, evidence (gathered by the mechanism
it recommends — re-derive, re-hash, re-run, never re-read), severity,
PROPOSED disposition. **Executed** — 44 rows across 8 dimensions,
26 close-as-fine, 9 fix-session-candidate, 6 ruling-needed, 3 intake;
full detail and the scoreboard live in the register itself, restated
below.

**AR-RR-2 `[C — the seeded watch-list; the probe battery starts here
but does not end here]`.** Every named item probed: SOAK status (D3
row 1 — 3 CI runs since the fix, all green, sample still short of the
5-7-push target, carried as a watch item, not closed); the `defspec`
seed generalization (D3 row 2 — GENERALIZED beyond the one instance
ADR-0076 found: all 71 `defspec` forms repo-wide pin no seed, not a
special case); the EncounterEnd gap (D6 row 3 — re-confirmed still
deferred whole, unchanged); the census's JSON-only count and 3-seed
power (D6 rows 1-2 — the undercount confirmed STILL LIVE in code,
unfixed since ADR-0074 disclosed it, now a 3x-repeat cost); carried-
item aging (D7 row 4 — all five named items aged explicitly in a
table; pairing-as-data's "four closes old" claim independently
re-derived and confirmed EXACT); the laws-to-gates map (D2 rows 1-2 —
~24 standing rulings mapped, 6 gated, ~10 correctly gateless-by-nature,
4 real gaps); the CI-only/author-machine-only inventory (D2 row 8 —
fully enumerated, all already self-disclosed, no undisclosed
environment-restricted surface); the operators-registry shared-
mutable-state pattern (folded into D2's guard sweep and D4's catch-
block sweep — no second polluter class found beyond the one ADR-0075
already fixed, confirmed clean by the full-suite green run). **Executed
in full, Step 1.**

**AR-RR-3 `[C — probe mechanics]`.** Dimension 8's fence-execution
probe ran every LIVE doc command fence (~28 groups across README,
AUTHORS-GUIDE, all of `docs/**`, all `components/*/docs/**`, all of
`demos/**`) and the CLI error matrix against the built `bin/ehrt` from
workspace root, scratch output confined to a fresh `out/` directory,
fully cleaned after (confirmed by `git status --porcelain` empty
before and after — no tracked file touched). Dimension 1's re-
derivations covered `state.md`'s own claims, both NOTICEs (the
modules tree's 69-row provenance table, confirmed by direct count),
`reading-sets.edn` (fresh `wc -l` sums against all five budgets), and
the ADR/README counts (74, matching `notes/adr/README.md`'s own
self-disclosed figure). Dimension 5 regenerated every derived doc via
`make docsgen` and diffed — zero bytes changed anywhere. Six of the
eight dimensions' probe batteries ran as independent, parallel,
read-only sub-agents under the same re-derive-never-re-read
discipline; dimensions 1 and 5 ran directly in this session (both
single-command-shaped, no benefit to delegating). **Executed, Step 1.**

**AR-RR-4 `[C — scope]` (fences).** SURVEY ONLY, held: no src, no
test, no doc-content fix, no gate change, no flake-hardening, no
tidying-in-passing landed this session. The only tree changes: the
register file itself (`.agents/plans/2026-08-07-repo-review-
findings.md`), this ADR plus its indexes, the roadmap Done pointer,
and this session's own record + prompt archive. The oracle bracket
(below) shows all twenty-seven batches identical. Standing untracked
files (`config/busy-weekday.md`) untouched. **Held** — the one
temptation this session's own probes surfaced repeatedly (D2-3/D2-5/
D2-6's missing gates, D3-3's `-text` extension, D3-4/D4-1/D8-2/D8-3's
shared I/O-honesty root cause) was recorded as register rows with a
proposed disposition each, never fixed in passing, exactly the
discipline AR-RR-4 names.

### The probe-battery summary, per-dimension row counts

| dimension | rows | verdict | close-as-fine | fix-session-candidate | ruling-needed | intake |
|---|---|---|---|---|---|---|
| D1 — Claim–reality coherence | 9 | GREEN | 9 | 0 | 0 | 0 |
| D2 — Guard coverage | 8 | YELLOW | 3 | 3 | 2 | 0 |
| D3 — Environment independence | 5 | YELLOW | 1 | 2 | 1 | 1 |
| D4 — Error honesty | 4 | RED | 3 | 1 | 0 | 0 |
| D5 — Mirror and derivation drift | 3 | GREEN | 3 | 0 | 0 | 0 |
| D6 — Sampling adequacy | 5 | YELLOW | 3 | 0 | 1 | 1 |
| D7 — Continuity integrity | 7 | GREEN | 4 | 1 | 1 | 1 |
| D8 — Operator experience | 5 | GREEN | 4 | 2 | 0 | 0 |
| **Total** | **44 (some rows recur across the fix-session-candidate row count where a cluster spans dimensions — 9 distinct fix-session-candidate rows, not double-counted)** | — | **26** | **9** | **6** | **3** |

**The single RED dimension is severity-driven, not volume-driven.** D4
(error honesty) has only one real finding among four probes, but that
finding (D4-1: a repo-wide, currently-live nil-`.listFiles` conflation
with a DEMONSTRATED silent-success path in `bases/cli/core.clj`'s
`mutate-command`) is the register's single highest-severity row —
a real I/O failure can produce a clean, successful, wrong `{:count 0}`
answer today, with zero error surfaced, directly contradicting the UX
arc's own standing rule ("errors name their artifact"). The register
itself names the cross-dimension pattern: D3-4 (`kernel/artifact.clj`'s
unchecked `.renameTo`), D4-1, and D8-2/D8-3 (`corpus mutate`'s
unwrapped file-read) share ONE root cause — an I/O call outside this
repo's own `Result`-vocabulary convention — surfacing independently in
three different dimensions' probes, meaning a single fix session
scoped to "every I/O call in `src/` that can fail silently instead of
through `Result`" would plausibly close four rows across three
dimensions at once.

### Scoreboard — restated (first assessment, no prior column)

4 green (D1, D5, D7, D8), 3 yellow (D2, D3, D6), 1 red (D4). Zero
STOP-AND-REPORT-worthy findings — every row is a recommendation
against a live, otherwise-healthy tree, not a broken build. Full row-
level detail lives in the register; this ADR restates only the
scoreboard and the counts, per this arc's own narrative-hierarchy
convention (`.agents/rulings.md` AR-B-4 — the ADR execution record is
the sole narrative, the register is the survey artifact itself, not
duplicated here).

### Execution record

**Step 0 — preflight + tag.** Working directory confirmed the ext4
clone; tip `89c0d24` exactly, working tree clean. `clojure -M:poly
check`: OK. Full suite baseline: green, 261 `Test results:` lines
(matching the vendoring-arc-close baseline count exactly), 511
assertions, 0 failures/0 errors. Last-five CI conclusions on main
(the AR-QR-3-widened check): `89c0d24` success, `9a34409` success,
`9cc3563` success, `d0129b9` success, `74ebc6b` failure (the already-
disclosed, already-fixed-forward off-ceremony landing ADR-0076
recorded — not a fresh finding). Oracle pre-digest
(`bin/regression-oracle 89c0d24 89c0d24`): all twenty-seven roots
IDENTICAL, soundness "yes outside ns form" — the expected trivial
result of a tip-against-itself bracket, confirming the oracle tool
itself is live and the 27-root set matches `state.md`'s own claim
(independently re-confirmed, D1-4). AR-RR-0 executed: tag created,
pushed, peeled ref verified.

**Step 1 — the survey (AR-RR-1/2/3).** History scan against the
closes-and-deviations lens (Context, above); full eight-dimension
probe battery run (six dimensions via parallel read-only sub-agents,
two — D1, D5 — directly); working notes gathered in scratch, no
commit at this step (the register lands whole in Step 2, the
ADR-0049 survey-artifact-lands-once pattern).

**Step 2 — the register (AR-RR-1).** `.agents/plans/2026-08-07-
repo-review-findings.md` landed: 44 rows, the first-assessment
scoreboard, the register summary naming the cross-dimension I/O
pattern. Committed `ac6ef5f` ("docs: the first assessment lands --
eight lenses, every probe recorded, nothing moved (repo review 1,
AR-RR-1)"), pushed. **This commit left CI red for roughly ten
minutes** (run `31230302344`, one failure: `index-completeness-test`,
`.agents/plans/README.md` missing the register file's own index
entry) — this session did not run the full suite again after Step 2's
own commit before proceeding to Step 3, so the gap wasn't caught
locally until Step 3's own full-suite re-run surfaced the identical
class of gap for its OWN three new files and the fix (adding the
missing `.agents/plans/README.md` entry, among the other two) closed
both gaps in the same pass. Disclosed plainly rather than folded
silently into Step 3's own account — see Verification, below, for the
watched conclusion of Step 3's own closing run.

**Step 3 (this entry) — ADR-0077 + record.** This file lands;
`notes/ADRs.md` gains its index line; `notes/adr/README.md`'s own
file count corrects 74→75, verified by `ls`, not arithmetic. Roadmap
Done pointer appended:

```
- 2026-08-07 — repo-review-1 — ADR-0077
```

**Oracle bracket** (`bin/regression-oracle 89c0d24 <this session's own
closing tip>`): this session's own touches were docs/plan-file only —
the register, this ADR, the index/README/roadmap edits, and the
session record + prompt archive — no `src/`, `test/`, `deps.edn`, or
`workspace.edn` in any component the oracle digests. All twenty-seven
roots confirmed byte-identical; see Verification, below.

### This session's own mechanical debt, recorded here

**The next session that opens fresh work tags `stable-20260807-repo-
review-1` at THIS session's own closing tip, under standing
ceremony.** No tag is created by this session for its own closing tip
— the tag law's own case (ii) licenses a session to tag its
PREDECESSOR's verified stable point, not its own mid-flight tip; this
session inherits `stable-20260807-quality-riders` (AR-RR-0, above) and
passes its own tag forward exactly the same way.

### The horizon, restated unchanged

This session was the arc's own survey, not a fix session — it did not
touch the horizon `notes/adr/0074-vendoring-arc-close.md` named and
`notes/adr/0075-ci-current.md`/`0076-quality-riders.md` restated
unchanged (the EncounterEnd design pass, Wave E's own register,
vendoring batch 4, pairing-as-data, publish-prep). None of those were
in scope here and none were touched — though pairing-as-data's own
aging (D7-4/D7-5, now confirmed exactly four closes old) and the
census's own repeat-cost undercount (D6-1) are both re-surfaced by
this review with sharper evidence than the horizon notes alone
carried, for whichever session next picks either up.

**What DOES change:** the design channel's own next act is now named
— after fresh-probe verification of this session's own landing (the
register's row-level evidence sampled and re-derived independently,
this ADR's own scoreboard cross-checked against the register), the
design channel distills this register into the mitigation plan for
the author's rulings (the skill's own step 5), and the ruled fix
sessions follow. The register itself already names the one
efficiency worth carrying into that plan: D3-4/D4-1/D8-2/D8-3's shared
root cause is plausibly one fix session, not four.

### Verification

- `clojure -M:poly check`: OK, Step 0 and again at Step 3.
- Full suite (`clojure -M:poly test :all skip:integration`): green at
  Step 0 baseline (261 namespaces, 511 assertions, 0 failures/0
  errors). **Re-run at Step 3 after landing this ADR's own index/
  README/roadmap edits — and it caught a real gap**: this session's
  own new files (the register, this ADR, the session record, and the
  prompt archive) had not yet been added to their own directory's
  index README (`.agents/plans/README.md`, `.agents/prompts/
  README.md`, `.agents/session-records/README.md`) — `index-
  completeness-test` failed with exactly the three missing entries
  named, 3 failures / 0 errors. Fixed forward in the same step (the
  three README entries added); full suite green again after (511
  passes, 0 failures, 0 errors). Disclosed here rather than silently
  corrected — the gate did exactly the job it exists to do, catching
  this session's own ceremony gap before it landed.
- `gitleaks git --staged -v`: clean, both commits this session
  (`ac6ef5f`, and this ADR's own closing commit); `gitleaks` also ran
  automatically on every push (pre-push hook), clean throughout.
- Post-push message verification: this session's commits checked
  against their message files after push.
- `bin/regression-oracle 89c0d24 <this ADR's own closing tip>`: all
  twenty-seven roots confirmed IDENTICAL, soundness "yes outside ns
  form" — this session's own touches never reached any oracle-digested
  path.
- Tag verification: `stable-20260807-quality-riders` peeled ref
  resolves to `89c0d24` exactly (`git ls-remote --tags origin`).
- CI, this session's own two pushes, watched directly (not assumed):
  `ac6ef5f` **failure** (run `31230302344`, the index-completeness gap
  disclosed above), `075db9b` **success** (run `31230768905`, watched
  to conclusion) — the fix in the same push that landed this ADR
  closed the gap `ac6ef5f` opened, confirmed green by the mechanism
  the claim is about (a watched CI run), not merely a local re-run.

### Fences

Everything AR-RR-4 names, held: no src/test/gate/doc-content change,
no flake-hardening, no tidying-in-passing — every fix-shaped
temptation this session's own probes surfaced became a register row
with a proposed disposition instead. The three index-README additions
(Verification, above) are inside AR-RR-4's own licensed scope ("the
register file itself, ADR-0077 + indexes") — they are the indexes a
`README.md`-per-directory index-completeness gate requires for the
very files AR-RR-4 already named as this session's tree changes, not
a new class of edit. The register's dispositions are PROPOSALS; the
author rules on them in the design channel after this lands.
`notes/ADRs.md` ADR-0055/0064/0068/0074's own horizon items stay
untouched. `config/busy-weekday.md` untouched.

### Consequence

The quality-review arc's own first survey is landed: eight dimensions,
every probe recorded (including the clean ones — 26 of 44 rows), the
seeded watch-list fully covered and in several cases sharpened past
what the horizon notes alone carried (pairing-as-data's exact age,
the census undercount's repeat-cost, the `defspec` seed gap's true
repo-wide scope). One real, demonstrated, currently-live defect
surfaced (D4-1, a silent-success path in `corpus mutate`'s file-
listing sweep) alongside two siblings sharing its root cause in other
dimensions' probes — the kind of cross-lens catch this skill's own
rotating rubric exists to make possible, the same way ADR-0075 caught
a stale doc a green local suite couldn't see and ADR-0072 caught a
byte-corruption a correct hash couldn't see. Nothing moved: the
register and this ADR are a survey, complete and dated, awaiting the
author's own rulings before any fix session runs.
