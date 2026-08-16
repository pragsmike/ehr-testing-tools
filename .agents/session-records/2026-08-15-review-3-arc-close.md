# 2026-08-15 -- Repo-review 3 arc close: step-7 close ADR, post-arc scoreboard, review 4's watch-list

Records-only close session for the repo-review-3 arc, under R30
(commit and push at each checkpoint, unattended) — the ceremony mode
this session's prompt did not override. Landing record:
`notes/adr/0139-review-3-arc-close.md`. Archived prompt:
`.agents/prompts/2026-08-15-review-3-arc-close.md`.

## Step 0 -- preflight, every finding disclosed

`bin/preflight` plain, all five checks:

```
-- 1. Last five CI runs on main --
  green  b96c2464  2026-08-15T22:36:54Z  docs: post-push receipts for review-3 fix session C
  green  0e16778a  2026-08-15T22:35:37Z  docs: ADR-0138, session record, prompt archive, and register disposit...
  green  7544f7c7  2026-08-15T19:13:38Z  docs: post-push receipts for review-3 fix session B
  green  2db2dee9  2026-08-15T19:12:41Z  feat: stale-path gate covers every tracked doc surface; 25 dead
  green  15f59438  2026-08-15T15:51:38Z  docs: post-push receipts for review-3 fix session A
OK: last five runs all green (or none found)
OK: repo root '/home/mg/src/ehr-testing-tools' is not under /mnt/
OK: working tree clean, including untracked files
OK: local HEAD (b96c246430038b4d38aa60a391de5e376e61cd24) matches origin/main
Last stable-* tag: stable-20260815-result-nodes (b139de589083c6b4967c1a4769b2c6a8d17feac4)
DISCLOSED: HEAD is not currently tagged stable-*
```

The one DISCLOSED line is correct and expected: the arc tags at its
step-7 close, which is this session.

Tag substance verified directly rather than trusted:
`git rev-parse stable-20260815-result-nodes^{}` =
`b139de589083c6b4967c1a4769b2c6a8d17feac4`, exactly the commit the
prompt named. Baseline tip `b96c246`, matching `origin/main`.

Note for the record: the topmost green CI run is at **`b96c246`
itself** — the tag target. That matters for the fence below.

## Step 0.2 -- the tag: fence fired, STOP taken, push held

The prompt licensed `stable-20260815-review-3-fixes` at `b96c246`
under tag-law case (i), conditional on two pieces of evidence, with an
explicit instruction: *"if the CI relay is absent from this prompt's
context, STOP-AND-REPORT before pushing the tag."*

| condition | present? | evidence |
|---|---|---|
| Design-channel fresh-clone verification of all three fix sessions | **yes** | Relayed in the prompt in detail, including two independent `bin/post-push-verify` witnesses: a synthetic 3-commit push with a non-ASCII middle message (derived the recorded pre-push tip, checked all three, exit 1) and the loud-fail floor against a fresh clone with no remote-tracking reflog (exit 2 rather than a `tip^1` guess). |
| Author-side CI check | **no** | No run id, no `gh run list` output, no relayed conclusion anywhere in the prompt. Contrast the register's own Step-0 row for the previous tag, which cited *"run 31884986962 green on `b139de5`"* — that is what a relay looks like here, and nothing of that shape is present. |

**The stop was taken.** Reported to the author with the evidence this
session could gather itself — `bin/preflight` showing CI green at
exactly the tag target — and with ADR-0134's precedent named, where
this same fence fired for this same missing relay and the ruling came
back *"Pay it, message verbatim"* on session-side preflight evidence.

**No ruling came back in-session.** The tag was therefore created
**locally only**:

```
$ LC_ALL=C grep -n '[^ -~]' <message-file>   # ASCII check on the supplied message
message is pure ASCII

$ bin/tag-ceremony stable-20260815-review-3-fixes b96c246430038b4d38aa60a391de5e376e61cd24 <message-file>
OK: created annotated tag 'stable-20260815-review-3-fixes' at b96c246430038b4d38aa60a391de5e376e61cd24
OK: tag created locally only (no --push given)
TAG_EXIT=0
```

No `--push`, so no peeled-ref verify is owed or claimed. The mechanics
are staged and the license half stays the author's. Carried as this
close's own mechanical debt in ADR-0139; the paying command is
recorded there, and `bin/tag-ceremony` is idempotent, so the paying
session verifies the existing local tag rather than re-creating it.

## Re-derived disposition counts (the skill's arithmetic law)

Extracted mechanically from the register's live table rows — every row
whose first cell is a `D<n>-<id>` label, disposition read from the last
cell. Not copied from the register's summary line, and not copied from
this session's prompt.

| disposition | count |
|---|---|
| FIXED | **8** (D1-2, D1-5, D1-6, D2-4, D2-6, D4-3, D5-3, D5-4) |
| ENCODED IN GATE | **1** (D1-8) |
| REGISTERED | **2** (D7-3, D7-4) |
| close-as-fine | 25 |
| intake | 3 (D3-1, D6-4, D8-5) |
| fix-session-candidate, still open | 2 (D1-9, D1-10) |
| non-tallied cross-reference | 1 (D2-5) |

41 disposition-carrying rows + 1 cross-reference = **42 rows**, which
is the row count exactly (the review-day 40 plus D1-9 and D1-10, opened
during the arc by fix session B).

**Eleven cells moved this arc.** Every fix-session-candidate and
ruling-needed row the review itself opened is closed or registered.

**Two figures in the prompt did not re-derive, and both are corrected
rather than repeated:**

- **"17 FIXED cells across sessions A-C."** No reading produces 17. The
  arc-changed count is 11. A naive grep for bolded FIXED-family markers
  returns 18, of which 7 sit in *evidence* cells marking review-2
  findings this review confirmed fixed (D2-1, D2-2, D4-2, D6-1, D7-2,
  D8-1, D8-3).
- **"five recorded instances plus three fix-session sightings"** of the
  registry-as-population class. Re-derived: five recorded by the
  review, two opened during the arc (D1-9, D1-10), four opened by this
  close — three by re-scoring (C-1, C-2, C-3) and one by its own
  full-suite run (C-4) — **eleven instances**, plus the three live
  sightings of D1-6's under-coverage Session C quantified.

**No register cell disagrees with what ADR-0136/0137/0138 claim.** Each
moved cell's citation was checked by reading the cited ADR: 0136 carries
D5-3/D5-4/D2-4/D1-5/D7-3/D7-4 (by id in its index line where the body
uses ruling names), 0137 carries D1-2 and D1-8 (cited 3x and 5x in the
body), 0138 carries D1-6/D2-6/D4-3. The prompt's second STOP condition
never fired.

## The post-arc scoreboard, and the probes behind each cell

Scored against the **live tree**, not against the fix ADRs' accounts of
themselves. Full reasoning in ADR-0139.

| dimension | review 3 (finding day) | post-arc (close day) |
|---|---|---|
| D1 — Claim-reality coherence | YELLOW | **YELLOW (held)** |
| D2 — Guard coverage | YELLOW | **YELLOW (held)** |
| D3 — Environment independence | YELLOW | **YELLOW (untouched)** |
| D4 — Error honesty | GREEN | **GREEN** |
| D5 — Mirror and derivation drift | **RED** | **YELLOW (up two)** |
| D6 — Sampling adequacy | GREEN | **GREEN** |
| D7 — Continuity integrity | YELLOW | **YELLOW (held, not earned)** |
| D8 — Operator experience | YELLOW | **YELLOW (held, not earned)** |

Finding day 2 green / 5 yellow / 1 red -> close day **2 green / 6
yellow / 0 red**.

The probes this session actually ran, with their output:

- **`Makefile:151`** — `docsgen: pipeline use-cases operators-doc
  cli-doc sim-theory palgebra-examples`. Both new targets are in the
  graph.
- **`.github/workflows/test.yml:99-108`** — the freshness step diffs
  **ten** paths, not five.
- **Result-node counts in the three regenerated examples** —
  `ai-study-flow-v3` **3** `_out`, `committee-flow` **6**,
  `deliberated-choice-flow` **6**. Exactly the counts the channel's
  independent pre-session regeneration predicted (0 vs 3, 0 vs 6, 0 vs
  6).
- **An independent dead-link re-derivation**, written for this close
  rather than borrowed from the gate: 88 `*.md` files under `docs/**`
  and `components/<x>/docs/**`, markdown link destinations resolved
  from each file's own directory, percent-decoding applied,
  http/https/mailto skipped -> **0 dead links**. The 25 are genuinely
  gone, over a population enumerated from the filesystem.
- **`git grep -l sim-theory-equations`** over `*.clj`, `Makefile`,
  `.github/` -> the `Makefile` alone. Nothing checks the `.edn` against
  the equations file (finding C-1).
- **`grep -c -i careplan .agents/plans/roadmap.md`** -> **0**; zero
  hits for `care.plan` in `roadmap.md` or `state.md` (finding C-2).
- **Attic state** — last dated header in
  `.agents/plans/roadmap-done-2026-08.md` is *"Conviction arc — closed
  2026-08-08 (ADR-0085-0089)"*; the live `## Done` section carries
  **40** pointers (finding C-3).

## The red run, the ruling, and the state.md regeneration

The first full-suite run at the final tree went **red**:

```
FAIL in (state-md-cites-the-newest-arc-close-as-its-own-regeneration-point-test)
  (state_staleness_tripwire_test.clj:52)
a close landing without regeneration turns this red at that session's own full-suite run
.agents/state.md's header cites ADR-0107 as its own regeneration point, but the newest
arc-close ADR on disk is ADR-0139 -- .agents/state.md is stale (AR-C-1, D2-4).
expected: (= newest cited)
  actual: (not (= "0139" "0107"))

MAKE_EXIT=2
```

**The exit-code-capture law paid off on its first close.** This arc
landed that law (D2-6, ADR-0138); this session is the first to run
under it. The failure line sat well above the end of the log, and the
run's last visible lines read `Test results: 3 passes, 1 failures`.
Through a `tail -40`, `MAKE_EXIT` would have been `tail`'s zero and
this close would have reported green over a red tree. Recorded because
a law's first live catch is worth more than its rationale.

The session stopped rather than choosing, because one of the available
paths was dishonest: regenerating is channel work by AR-C-1's own
wording and roughly tripled this session's scope; **renaming the close
ADR to fall outside the gate's filename regex would have worked by
staying outside the gate's population** — the exact defect this arc
spent eleven instances documenting; and holding the close leaves the
arc without an endpoint.

**Ruled: regenerate here** (author, 2026-08-15, by selection from the
options put) — the records-only fence widened by this one file, every
`[V]` claim re-probed, and the actor substitution disclosed (AR-C-1
names the design channel; this session performed it).

Executed: all **14 `[V]` section-level claims** re-derived at tip
`b96c246` and the sections rewritten around what the probes returned.
The probe outputs are in the batch above and in the ADR's own table.
Headline movements against the 2026-08-08 figures: modules/oracle roots
25/29 -> **31/34**; NOTICE rows 71 -> **80**; ADR files 86 -> **137**
(index reconciling exactly); docs-tooling gates 27 -> **36**; vendored
round-trip family 29 -> **36**; `stable-*` tags 42 -> **92**;
pairing-registry rows 7 (v2-only) -> **12 including five FHIR rows**,
discharging AR-PD-2's own fence; NIST taxonomy 7/52 -> **14/104** at
engine 1.7.3. Components/bases **18/1**, genuinely held across all
fifty ADRs.

**C-4, the finding underneath the failure.** The gate enumerates *ADR
files whose filename matches `NNNN-*-arc-close.md`*; AR-C-1's
obligation is "each arc close". `0125-manual-s5-chapter8-review-close.md`'s
own first line reads "the manual-review skill, arc close" and the gate
never saw it. Measured cost: **fifty ADRs** (0090-0139) between
state.md's last full regeneration and this one. **The file is fixed;
the gate is not** — roadmap row landed, review 4 inherits it.

## Three findings opened by this close, registered and not fixed

Found by re-scoring against the live tree, as Step 1.2 instructed.
Registering them is records work and in fence; fixing them is not, and
none was fixed.

- **C-1** — the registered derivation chain's head hop
  (`sim-theory.edn` -> `sim-theory-equations.txt`) is hand-maintained
  by the equations file's own admission, ungated, and described as
  *"mechanically regenerated"* by
  `components/sim-trajectory/docs/trajectory-computation.md:250`.
  ADR-0136's own corollary, one hop upstream of where that ADR applied
  it.
- **C-2** — a third unregistered standing request (CarePlan / Guard
  condition-resolution, *"unowned by any wave"*, unregistered since
  `e6a0b28`, 2026-08-05). The amended D7 probe missed it because its
  exclusion step uses `roadmap.md` as its oracle.
- **C-3** — the attic-rotation law (ADR-0055 AR-AC-5, restated in the
  roadmap's own `## Done` header) unexecuted since 2026-08-08, with
  review 3's D7-5 probe titled *"attic-vs-live consistency"* having
  measured the Deferred lint and the frozen boundary instead.

All three are roadmap rows as of this close, plus watch-list rows in
ADR-0139.

## What landed

| file | change |
|---|---|
| `notes/adr/0139-review-3-arc-close.md` | new — the close ADR |
| `notes/ADRs.md` | one index line (137 index entries = 137 `notes/adr/*.md` files, reconciled) |
| `.agents/plans/2026-08-15-repo-review-findings.md` | a dated close note appended **under the scoreboard**; **no review-day row or score edited** |
| `.agents/plans/roadmap.md` | the arc row (created already-closed, see deviations); two chartered follow-on rows (D8-5 battery, review 4); three new-finding rows (C-1, C-2, C-3); four Done pointers (ADR-0136 through 0139) |
| `.agents/rulings.md` | one section carrying both verbatim rulings with their glosses, the state.md fence-widening ruling, and the population-closure law as the arc's standing finding |
| `.agents/state.md` | **full regeneration under the ruling above** — first since 2026-08-08 (`a9c3abf`), fifty ADRs of drift, all 14 `[V]` sections re-probed at `b96c246` |
| `.agents/session-records/2026-08-15-review-3-arc-close.md` | this file |
| `.agents/prompts/2026-08-15-review-3-arc-close.md` | the archived prompt + its deviation record |
| both `README.md` index lines | mechanically required by `index-completeness-test` / `prompt-record-pairing-test`; generated by `bin/close-scaffold`, disclosed in the commit message |

Zero `src`, zero `test`, zero docs outside the register and roadmap,
zero regeneration of any derived artifact — `.agents/state.md` is
"regenerated" in AR-C-1's hand-re-probed sense, not the make-graph
sense; no converter ran. **No oracle claim is made or owed.**

## Deviations

1. **The tag push is held** — Step 0.2 above. This is the deviation
   `build-session` step 11 names ("deferring a licensed tag is now the
   deviation, disclose why if you do"), and the why is that the license
   was conditional and its condition is unmet.
2. **The roadmap had no review-3 arc row to flip.** Step 2.2's premise
   does not hold against the tree — verified by grep (`repo review`,
   `repo-review`, every `^- \*\*` row heading) before acting. The arc
   was chartered channel-direct and its sessions registered their
   findings as rows without one ever being opened for the arc itself.
   Row created already-closed, in the shape ADR-0135's own
   channel-direct row used.
3. **Two prompt figures corrected** rather than repeated (the "17 FIXED
   cells" and the instance count), per the skill's arithmetic law.
4. **Four findings opened by a records-only session** (C-1, C-2, C-3
   registered and not fixed; C-4's *file* fixed by ruling, its *gate*
   registered and not fixed).
4b. **The fence was widened by one file** (`.agents/state.md`) under an
   explicit author ruling, with AR-C-1's named actor (the design
   channel) substituted by this session and the substitution
   disclosed — not absorbed.
5. **Done pointers for ADR-0136 through 0138 were added by this close**,
   not by their own sessions. The gate is one-directional (a pointer's
   ADR must resolve; not every ADR needs a pointer), so nothing was
   failing — but the live `## Done` section is where a reader looks for
   what landed, and the arc's own three fix sessions were missing from
   it. **Rotation was deliberately NOT performed** — see C-3; assigning
   arc boundaries to a dozen intervening arcs is judgment work outside
   a records-only close's fence.

## Verification

- `bin/preflight`: above, every finding disclosed.
- Disposition tally: re-derived mechanically, 42 rows, summing exactly.
- Post-arc scoreboard: every cell backed by a probe run this session,
  outputs above.
- ADR index reconciliation: `grep -c '^- \*\*ADR-' notes/ADRs.md` =
  **137**; `ls notes/adr/*.md | grep -v README | wc -l` = **137**.
- Full `make test`, unpiped, exit code captured explicitly (never
  through a pipe — the ADR-0138 law's first close-session application):
  figures below.
- Push and `bin/post-push-verify`: receipts below.

### Full suite

Third run of the session, at the true final tree. Run 1 went red
(state.md staleness, above); run 2 was stopped mid-flight once further
record edits landed, rather than reported against a tree it no longer
matched. Only the figures below were inserted after this run, and no
gate reads them.

```
MAKE_EXIT=0
blocks '0 failures, 0 errors': 640
'Test results:' lines:         320
Testing ns announcements:      320
total assertions:              16382
FAIL in:                       0
ERROR in:                      0
nonzero-failure signatures:    0
bin/verify-nist-lock:          green (nist-hl7-v2-validation, nist-xml-util,
                               nist-hl7-v2-schemas, nist-validation-report)
```

### Push receipts

```
$ git push
   b96c246..f91ca13  main -> main
(pre-push hook: gitleaks clean, poly check OK)

$ bin/post-push-verify            # no arguments -- the ADR-0138 fix is live
== bin/post-push-verify (main, range b96c2464..f91ca13f) ==
-- 1. Remote tip vs HEAD --
OK: origin/main (f91ca13f...) matches tip (f91ca13f...)
-- 2. Per-commit ASCII check, b96c2464..f91ca13f --
OK: every commit message in range is pure ASCII
-- 3. CI run at tip --
CI run for f91ca13f...: status=queued conclusion=<pending>
  https://github.com/pragsmike/ehr-testing-tools/actions/runs/31921825657
DISCLOSED: reported once, not awaited to conclusion (AR-CI-4)
PPV_EXIT=0
```

**The derived range is one commit, and that is correct, not a
regression.** `bin/post-push-verify` with no arguments now derives its
base from `origin/main`'s own pre-push value rather than from
`tip^1`; this records-only close pushed exactly one commit, so the
derived range `b96c2464..f91ca13f` covers exactly what was pushed. The
old code would have produced the same range here **by coincidence** —
which is precisely the failure mode D1-6 described (Session B's push
was "correct only by coincidence"), and precisely why the fix was
witnessed against a synthetic three-commit push instead of a one-commit
one.

**The by-hand cross-check is retired as of this session.** ADR-0135 and
ADR-0136 both shadowed this script by re-checking the pushed range by
hand, correctly, because the script was known-wrong. It now has three
independent witnesses — Session C's co-landed test (red at a one-commit
range over a three-commit push, green after), Session C's own push, and
the design channel's synthetic 3-commit fresh-clone witness with a
non-ASCII middle message. Three witnesses is the bar for trusting a
tool instead of shadowing it, and this session trusted it.
