## ADR-0097 — Review-2 arc close: the plan executes in full, two findings anchor, one session closes it

**Status:** Accepted (author-directed, autonomous session per R30 — ONE
session, a deliberate author-ruled deviation from the two-session-close
standing default, ADR-0084's own intake), 2026-08-09.

### Context

Prior: `notes/adr/0096-cluster-b-parse-guards.md` closed cluster B, the
review-2 arc's own last fix session. The arc in full: survey
(`0092-repo-review-2.md`, 76 rows across eight dimensions), rulings
landing (`0093-review-2-rulings-landing.md`, the author's six-part
verbatim ruling executed), the census fix (`0094-census-closure-file-
count.md`, ruling 6), cluster A (`0095-cluster-a-gate-wiring.md`, D2-4/
D2-18), cluster B (`0096-cluster-b-parse-guards.md`, D4-5/D4-6/D4-7/
D8-3). The plan is fully executed: every ruling ruled and landed, every
fix cluster consumed. The author ruled 2026-08-09, verbatim: **"Emit
the prompt for one close session."** — the whole arc closes in ONE
session, not the two-session pattern ADR-0084's own intake made
standing (a second, deliberate infra-block-independent instance of
that same default being set aside; the conviction arc's own two-
session close, ADR-0089, remains the standing default's own positive
demonstration — this is the exception, named as one, not a silent
reversion). This close moves no scoreboard; re-scoring belongs to
review 3, on the author's own cadence call.

R30 ceremony. Read-first: `notes/adr/0080-quality-arc-close.md` (the
review-1 close, this ADR's own structural template); `notes/adr/0092-
repo-review-2.md`, especially its "Carried to review 3" section;
`notes/adr/0093` through `0096` in full, `0096`'s own "Two new
findings" section as the anchors' verbatim source; `.agents/plans/
2026-08-09-repo-review-findings.md`, the 76-row register this close's
own tally re-accounts for; `.agents/rulings.md`, the anchor law
(AR-RL2-3) this close executes and the two other review-2 laws whose
first enforcement this window witnessed.

### Decision

Author rulings, recorded verbatim (design channel, 2026-08-09). `[A]`
author-ruled, `[C]` channel-inferred.

**AR-AC-0 `[A — ADR-0096's own successor tag debt]`.** Annotated
`stable-20260809-cluster-b-parse-guards` tagged at `05b8624`, message
"cluster B parse guards landed, design-channel-verified 2026-08-09
(ADR-0096)"; pushed; peeled ref verified (`git ls-remote --tags
origin` resolves `stable-20260809-cluster-b-parse-guards^{}` to
`05b8624df5fc4609299ddc4cd9033763ed8dfe4f` exactly). **Executed Step
0.**

**AR-AC-1 `[A — the anchor law, AR-RL2-3, its own first execution AT
AN ARC CLOSE]`.** Two `roadmap.md` rows, wording sourced from
ADR-0096's own findings section:

1. **`ehrt gate fhir PATH`'s own permission-denied leg** → `Next`: a
   small fix session. **Executed** — see `.agents/plans/roadmap.md`,
   Next section.
2. **`ehrt play`'s own bare reads** → `Deferred`, with a named revisit
   trigger. **Executed** — see `.agents/plans/roadmap.md`, Deferred
   section.

Both cite ADR-0096's own Finding 1 and Finding 2 respectively — the
first time AR-RL2-3 fires at an actual arc close (its two prior
instances, wellness-encounters and the `notice_verbatim_test` gap,
landed inside ADR-0093, a rulings-landing session, not a close).

**AR-AC-2 `[A — D8-4, ruled 2026-08-09, verbatim: "I choose a."]`.**
Bare/`help`-level unknown flags — currently silently swallowed (help
printed, exit 0) while subcommands report `:unknown-flag` — will be
ROUTED through the same `:unknown-flag` category. Src work, so it does
NOT land in this close: anchored as a one-line RIDER on the gate-fhir
`Next` row (AR-AC-1 item 1), same file family, same future fix
session, its own red→green evidence owed there. `docs/cli.md` is NOT
touched by this close; option (b) of D8-4's own register text is
struck. **Executed** — see the rider paragraph on the roadmap's own
gate-fhir row.

**AR-AC-3 `[C]` (this ADR, in ADR-0080's own shape).** Arc narrative;
final disposition tally; deviations sweep, both sides, fix-forward;
review 3's inheritance; open Externals restated; this close's own
mechanical debt; the horizon note. **Executed**, below.

**AR-AC-4 `[C]`.** No register edits, no re-scoring, no new laws.
**Held** — verified by `git diff --cached --stat` at commit time
(below).

### The arc narrative

The review-2 arc opened on the author's own cadence ruling, verbatim
"Review 2" (ADR-0092): eight dimensions re-surveyed, 76 rows recorded,
review 1's own prior arithmetic independently re-derived and confirmed
exact. Its own headline result was genuinely mixed — the repo's single
most severe finding across both review cycles (D4's repo-wide,
demonstrated-live silent-success I/O pattern) closed, independently
re-verified by running the gate and re-deriving the grep rather than
trusting the fix session's own account, while two dimensions clean at
review 1 (continuity integrity, operator experience) each regressed to
yellow on the identical underlying shape: a fix or gate that closed
the literal trigger a prior probe happened to hit, leaving a
structurally adjacent, more general trigger of the same class open.
Nothing moved beyond the register and that ADR's own plan draft — the
author's rulings on six items followed next session, verbatim "1 a. 2
a. 3 a. 4 b. 5 (a)-lite. 6 a." (ADR-0093): three standing rulings
appended to `.agents/rulings.md` (the RNG-path law, the roadmap-anchor
law, the ASCII-check law), two aged horizon-note-only items
(wellness-encounters, the `notice_verbatim_test` gap) gained their
first roadmap anchor under the new law's own first execution, Wave E
parked with a named trigger, the census fix scheduled, and the front
door's own honesty gap closed by disclosure alone — no byte of the
demo touched. The census fix landed next (ADR-0094): both
`:closure-file-count` branches converge on one definition, red-to-green
gated, closing a 3x-undercount repeat-cost record open since ADR-0074
and unactioned across six window ADRs. Cluster A followed (ADR-0095):
a new reader-based static gate closes the `2088763` classpath-break
class structurally, its witness pair reproducing the exact historical
incident and nothing else; `bin/verify-nist-lock` joined the push
lane it had silently been absent from for three arcs, proven to trip
and to pass. Cluster B closed the arc's last fix session (ADR-0096):
four register rows close on one root cause and one precedented fix
shape, six-fold red-to-green evidence, a new function-granular static
lint closing the recurrence loop the same way its sibling already does
for a different bug class — and two new findings surfaced honestly
rather than silently fixed or silently buried, both anchored by this
close (AR-AC-1, above). The oracle held pure identity across all 34
roots at every step this arc touched code — every fix session was
CLI-shell/tooling/CI-wiring work, never sim/engine-path work.

### The register's FINAL disposition tally

Re-derived directly from `.agents/plans/2026-08-09-repo-review-
findings.md`'s own 76 rows (D1:9, D2:18, D3:5, D4:12, D5:3, D6:6, D7:14,
D8:9 — summing to 76 exactly, the same discipline AR-RR2-2 and ADR-0080's
own Step-3 tally both applied to their own predecessors, applied here a
third time):

* **57 close-as-fine** — stand unchanged, no action owed (D1: 9, D2: 16,
  D3: 4, D4: 9, D5: 3, D6: 3, D7: 9, D8: 4 = 57).
* **8 fix-session-candidate — 8 of 8 landed or explicitly anchored, none
  silently dropped.** D2-18 (the classpath static gate) and D2-4
  (`verify-nist-lock` wiring) — both ADR-0095. D4-5 (`read-base-data`'s
  `:fhir` branch), D4-6 (`gate --baseline`), D4-7 (`check
  --assertions`) — all three ADR-0096. D8-3 (permission-denied,
  `corpus mutate`/bare `gate`/`show`) — ADR-0096 for its two in-fence
  legs (bare `gate` dispatch, `show`); its own THIRD command, `gate
  fhir`, diagnosed live as living outside the register's own cited fix
  location (ADR-0096 Finding 1) and anchored by THIS close (AR-AC-1
  item 1), not double-counted as a second landing. D8-7 (the dangling
  README link) — ADR-0093, cluster C rider (AR-RL2-7). D8-4 (bare-flag
  routing) — ruled `[A]` "I choose a" (ADR-0093), execution anchored
  by THIS close as the gate-fhir row's own rider (AR-AC-2), riding that
  future fix session rather than a second, separate one.
* **5 ruling-needed — 5 of 5 ruled**, quoting the author's own six-part
  verbatim ruling (ADR-0093), mapped: D8-6 (ruling 1, "a" — the front
  door's honesty disclosure, landed in README.md). D6-2 (ruling 2, "a"
  — "measurements sample the claimed population," landed in
  `.agents/rulings.md`). D7-7 and D7-8 (ruling 3, "a" — "horizon items
  anchor in the roadmap," landed in `.agents/rulings.md` plus both
  items' own first roadmap rows). D7-13 (ruling 4, "b" — Wave E parked,
  landed as a Deferred row). The author's own ruling also covered two
  items OUTSIDE the register's 5 ruling-needed rows in the same
  verbatim sentence — H-6 (ruling 5, "(a)-lite," a history-scan row,
  landed in `.agents/rulings.md`) and D6-1 (ruling 6, "a," a register
  row dispositioned `intake`, not `ruling-needed` — scheduled ADR-0093,
  executed ADR-0094) — both accounted for under their own dispositions
  here, not double-counted against the 5.
* **5 intake — 3 genuinely carried to review 3, 2 resolved within this
  same arc.** Carried: D3-2 (both flakes' SOAK, a larger, still-clean
  sample), D7-14 (the ADR-footnote-fork backlog row), D8-8 (`make
  quickstart`'s own untimed full run). Resolved in-arc, not carried:
  D6-1 (fixed, ADR-0094) and D7-9 (its own cross-referenced,
  self-healed census-anchor row, tied to D6-1's own resolution, "not
  double-tallied" per the register's own text).
* **1 non-tallied cross-reference** — D6-4 (the three skipped NIST
  pairing cells; folded into D2's own count per the register's own
  disposition, not a D6 finding).

**Total: 57 + 8 + 5 + 5 + 1 = 76**, matching the register's own row
count exactly — independently re-summed here, not copied from the
register's own summary line.

### Deviations sweep, both sides, fix-forward

Design-channel side:

- **The "no Wave-E row" claim.** ADR-0093's own AR-RL2-4 verified the
  design channel's own claim (that no Wave-E roadmap row existed)
  against the live tree BEFORE acting on it — evidence outranks the
  claim, standing practice (ADR-0048). The grep found a related but
  non-duplicate "Vital-sign channel" row; the new Wave-E row
  cross-references it rather than duplicating it, disclosed as a
  judgment call in that session's own record.
- **The Done-notes fence wording.** The census session's own driving
  prompt (`.agents/prompts/2026-08-09-census-closure-file-count.md`,
  AR-CF-5) named the Next-row closure as "moves to Done WITH its notes
  intact (the standing Deferred/Next contract)" — AR-A-5's own
  Deferred-row full-relocation language, written for a row shape this
  Next row did not have. ADR-0094's own landed text corrects this in
  the open, citing the actually-applicable contract by name (AR-B-4,
  "the roadmap Done entry is a pointer, the narrative above is this
  ADR's own") and executing the simpler one-line-pointer shape — never
  flagged as a deviation in that session's own record, restated here
  as this close's own required accounting.
- **Cluster B's charter premise mismatch (`gate fhir`).** AR-CB-2's own
  red-evidence pass found `ehrt gate fhir PATH`'s permission-denied
  crash bottoms out in `judge-fhir-official`/`kernel.digest`, outside
  cluster B's own stated fence — surfaced mid-session via
  AskUserQuestion, ruled in the open (fix the two in-fence legs,
  disclose the third), never silently absorbed or silently widened.
- **The `play` regression, caught in-flight.** `sniff-path-format`'s
  own signature change (bare value → Result) broke every existing
  caller; the full local suite caught it live (36 failures, 3 errors)
  before landing, not after — the callers were fixed the same session,
  disclosed in ADR-0096's own Verification section.
- **The prompt-archive transport reflow.** `.agents/prompts/2026-08-09-
  repo-review-2.md`'s own archived prompt is hard-wrapped at a ~80-char
  line width; its four siblings in this same window
  (`review-2-rulings-landing`, `census-closure-file-count`,
  `cluster-a-gate-wiring`, `cluster-b-parse-guards`) each preserve
  their own original long, unwrapped paragraph lines (max line lengths
  666–987 characters, against repo-review-2's own 86) — confirmed by
  direct `awk '{print length}' | sort -rn` measurement, this close's
  own probe, not carried from any prior session's claim. Content-
  verbatim (the words match), not byte-verbatim (the line breaks
  don't) — the em-dash incident's (H-6) own benign sibling: a transport
  difference that changes bytes without changing meaning, unlike H-6's
  own character-level substitution. No action taken; disclosed per this
  close's own fence (no register edit, no law append).
- **The design channel's CI-API verification gap, this window.** Every
  "CI watched to conclusion" claim this arc's own sessions made rests
  on that session's own `gh run list`/`gh run view` output plus clone
  consistency — the design channel that authored each session's own
  driving prompt never itself independently re-queried the GitHub API
  to confirm a prior session's own reported conclusion before citing
  it forward. Not a defect (session-reported CI status, watched to
  conclusion in-session, is this repo's own standing practice,
  ADR-0075/0076/0078's "CI is watched, never waited on" ruling) — named
  here as an epistemic gap the design channel itself carries, the
  transcript-witnessed-is-not-repo-recorded principle (ADR-0048)
  applied one level up, from session claims to the channel's own
  cross-session trust.

A third instance, caught in THIS close's own preflight and corrected
before landing rather than repeated:

- **This close's own driving prompt named a stale horizon claim.** "The
  veteran family arc (Batch 4) opens next" does not hold against the
  live tree: batch 4 landed in full 2026-08-08 (`notes/ADRs.md`
  ADR-0090, five of nine candidates vendored, two zero-substance, two
  deferred whole under their own true diagnosed names) — a full arc
  BEFORE review 2's own survey even opened, not a future one. Corrected
  in the horizon note below rather than repeated; the third
  design-channel unearned-specificity instance this arc's own record
  has now caught (after AR-RL2-4's own verify-then-add and the census
  session's own Done-notes wording correction) — the first two were
  caught inside their own executing session, this one is caught by the
  close that inherited it, the same catch-before-landing discipline
  applied one session later than usual, never past this ADR's own
  commit.

Repo/session side (already disclosed in their own sessions, restated
here per this close's own accounting duty, not re-litigated):

- Repo review 2's own D8 sub-agent paused a `make quickstart` run
  rather than let it block the review indefinitely (disclosed
  ADR-0092's own session record).
- Cluster A's `sniff-files` helper and cluster B's own oracle-bracket
  technique (a real commit + `git reset --soft`, vs. cluster A's own
  `git stash create`) — both mechanical, both disclosed in their own
  prompt archives' deviation records, neither requiring a fresh
  ruling.

### Review 3's inheritance

Restated from ADR-0092's own "Carried to review 3" section, extended:

- **D3-2** — both flakes' SOAK, on a larger, still-clean sample; re-run
  once the sample roughly doubles again.
- **H-3** — the oracle's own blind-spot to malformed compiled shapes
  that never changed; a structural instrument limitation, worth the
  author's own explicit acknowledgment that byte-identity oracles are a
  floor, not a ceiling, on semantic correctness.
- **D7-14** — the ADR-footnote-fork backlog row; the next session
  touching the Next section should re-cite it, breaking a two-session
  drop streak before it becomes three.
- **D8-8** — `make quickstart`'s own full timing; re-run in isolation
  with a >15-minute budget for a clean reading.

**Plus, new to this close:**

- **The two anchored findings** (AR-AC-1, above): `ehrt gate fhir
  PATH`'s own permission-denied leg (with D8-4's own routing decision
  riding as a rider, `Next`); `ehrt play`'s own bare reads (`Deferred`,
  named trigger).
- **The lint allowlist as a named watch item.**
  `cli_parse_guard_lint_test.clj`'s own allowlist
  (`play-events-from-file`, `play-events-from-dir`) is itself the
  `ehrt play` row's own tripwire — removing those two entries and
  re-running the lint is the fix session's own ready-made, already-
  confirmed-non-vacuous gate (ADR-0096: reports exactly those two names
  with the allowlist stripped).
- **D8-4, ruled.** "I choose a" — no longer an open call; it rides the
  gate-fhir fix session as a rider, not a review-3 carry.
- **`state.md`'s own content staleness, newly found by this close
  itself.** This close's own landing forced a citation-only fix-forward
  (the Dated append, above) to keep the staleness tripwire green;
  `state.md`'s own CONTENT still reflects its last full regeneration
  (2026-08-08, ADR-0089) and does not yet account for ADR-0090 through
  ADR-0097. A future session's own ruling should schedule the next full
  regeneration, the same standing act AR-QC-3/AR-CB-1 each executed for
  their own arc.

### Open Externals, restated unchanged

None of these six rows was touched by this arc's own work; restated
here, not re-decided, per the ADR-0080 pattern:

**GitHub's workflow-failure notification-email toggle** — still
genuinely unconfirmed (ADR-0080's own probe found no reachable API
surface for a personal Settings→Notifications→Actions preference);
zero session cost either way. **NIST licensing inquiry** — narrowed,
not resolved; still author action. **IG pinning** — still open.
**Clojars publish** — ruled, deferred; the group/coordinates-naming
half and publication itself both stay open. **SETUP rewalk** by an
unspoiled human reader — still owed. **Upstream the repo-adaptation
skill** to pragsmike/skills — still author action, named 2026-08-01.

### This close's own mechanical debt, recorded here

**The next session that opens fresh work tags
`stable-20260809-review-2-arc-close` at THIS session's own closing
tip, under standing ceremony** — the tag-law case (ii) pattern every
prior close in this repo has used for its own predecessor; no tag is
created by this session for its own closing tip. **The one-session-
close deviation, recorded per AR-AC-3's own "no ceremony beyond that"
instruction:** the review-2 arc closed in one session rather than
this repo's own two-session-close standing default (ADR-0084 intake),
by the author's own explicit ruling ("Emit the prompt for one close
session," 2026-08-09) — a deliberate exception, not a silent lapse.

### The horizon, per this session's own prompt

Untouched, carried forward from ADR-0096: the oracle's own blind-spot
intake (H-3); the two remaining `defspec` flake watch items (D3-2);
the ADR-footnote-fork backlog row (D7-14); `make quickstart`'s own
untimed full run (D8-8); the two deferred veteran modules under their
own true names (`veteran_hyperlipidemia.json`'s stale-reference
`MedicationEnd` idempotency question, `veteran_mdd.json`'s max-steps
backstop-vs-legitimate-long-loop question — each carries its own named
revisit trigger, neither a scheduled session); publish-prep Externals.

What's new: this close's own successor tag debt (above); review 3
itself, unscheduled, on the author's own cadence call, the same
posture ADR-0092's own opening carried for review 2. A design-channel
horizon claim this close's own driving prompt carried forward — "the
veteran family arc (Batch 4) opens next" — does not hold against the
live tree (see the Deviations sweep, above) and is corrected here
rather than repeated: batch 4 is closed history, not a queued next
arc; the two deferred veteran modules' own named revisit triggers are
what remains open from it, already carried above. The injuries-
family/busy-board idea remains exactly what the driving prompt named
it: an un-committed author aside, noted here without a roadmap row —
no commitment was ruled, and none is invented by this close.

### Dated append, 2026-08-09 — state.md's own citation, fix-forward

This ADR's own file is named `*-arc-close.md` and is therefore the
file `state_staleness_tripwire_test.clj`'s own regex tracks as the
newest arc-close ADR on disk — landing it (Step 3's own commit)
tripped that gate red at this session's own full-suite run, since
`.agents/state.md`'s header still cited ADR-0089 (its last full
regeneration, conviction arc close, 2026-08-08). This close's own
driving prompt scoped no `state.md` ruling (docs-only, "nothing else
moves" beyond the two named roadmap rows, the ADR itself, and
ceremony) — a genuine, disclosed gap against this file's own standing
regeneration contract (AR-C-1: "every `[V]` claim re-probed... at
each arc close"), not a silent evasion of it. Resolved to the
NARROWEST fix the gate itself demands: the tripwire's own docstring
states it "checks CURRENCY... not CONTENT," so only `state.md`'s
header citation sentence was updated to ADR-0097, fix-forward, landing
in this close's own Step 4 commit (never amending Step 3's). Every
section below `state.md`'s own updated header still reflects its LAST
full regeneration (2026-08-08, tip `a9c3abf`) — six ADRs' worth of
landings since (0090 through 0097) are NOT reflected there. Named here
as a newly-found watch item for review 3's own inheritance and for
whichever future session next rules a full `state.md` regeneration,
the same way AR-QC-3 (ADR-0080) and AR-CB-1 (ADR-0089) each did for
their own arc.

### Verification

- `clojure -M:poly check`: OK, Step 0.
- Oracle pre-digest (`bin/regression-oracle 05b8624 05b8624`): all
  THIRTY-FOUR roots confirmed IDENTICAL, soundness "yes outside ns
  form" — the expected trivial tip-against-itself result.
- Untracked files at Step 0: none (`git status --porcelain
  --untracked-files=all`, empty).
- CI lanes, all disclosed at Step 0: `test`-lane last five runs on
  `main` all green (`31340857607`, `31340341691`, `31330881843`,
  `31330580554`, `31328812231`). `Integration`-lane last three runs:
  the two most recent (`31312458033` 2026-08-09T12:06:30Z,
  `31308023126` 2026-08-09T10:18:45Z, both `workflow_dispatch`) green;
  the third-most-recent (`31301880957`, `schedule` trigger,
  2026-08-09T07:45:13Z) shows `failure` — investigated before
  disclosure, not merely reported: its own `headSha` (`b7a1dc88`)
  predates `2088763` (the classpath fix restoring `judge-v2-nist` to
  the `integration` project's own composition, landed
  2026-08-09T10:14:39Z UTC), and its failure log is the exact,
  already-disclosed H-4/`2088763` signature — a stale scheduled-run
  checkout replaying already-fixed history, not a live regression. Full
  detail in this close's own prompt archive deviation record.
- Full local suite (`clojure -M:poly test :all skip:integration`), run
  at Step 3 after every doc/ceremony edit: [recorded below at commit
  time].
- `gitleaks git --staged -v`: clean at every commit this session.
- Post-push message verification and the ASCII check (`git log
  --format=%B -1 | LC_ALL=C grep -n '[^ -~]'`, AR-RL2-5), every commit
  this session: [recorded below at commit time].
- Tag verification: `stable-20260809-cluster-b-parse-guards` peeled ref
  resolves to `05b8624df5fc4609299ddc4cd9033763ed8dfe4f` exactly.
- `git status --porcelain`: clean before this session's first tool
  call, clean at each commit boundary.

### Fences

Docs-only: no `src/`, no `test/`, no config, no `.gitattributes`, no
gates touched or edited this session. No fixes of any kind, including
the two anchored findings — anchoring is a roadmap row, not a fix. No
register edits (`.agents/plans/2026-08-09-repo-review-findings.md`
untouched). No law appends (`.agents/rulings.md` untouched this
session). No scoreboard movement. No `docs/cli.md` touch. Roadmap: the
two named rows (the gate-fhir `Next` row carrying the D8-4 rider, the
`ehrt play` `Deferred` row) plus the Done pointer, nothing else moved
— verified by `git diff --cached --stat` at commit time, below. **One
disclosed exception, mechanically forced, not discretionary:**
`.agents/state.md`'s own header citation sentence, updated to name
this ADR (the Dated-append section, above) — the narrowest fix the
staleness tripwire itself demands (currency, not content), landed
because this ADR's own filename tripped that standing gate red at this
session's own full-suite run; no other line in `state.md` touched.

### Index line

```
- 2026-08-09 — review-2-arc-close — ADR-0097
```

(appended to `.agents/plans/roadmap.md`'s own Done section.)

`notes/adr/README.md`'s own file count corrects 94→95, verified by
`ls notes/adr/*.md | grep -v README | wc -l`, not arithmetic.

### Consequence

The review-2 arc — five sessions, opened by the author's own periodic-
review ruling and closed in one session by the author's own explicit
one-session-close ruling — is complete. Every one of its 76 register
rows is accounted for: 57 close-as-fine stand, 8 fix-session-candidates
landed or explicitly anchored (none silently dropped), 5 ruling-needed
rows all ruled in the author's own verbatim words, 5 intake rows
resolved (2) or genuinely carried (3), and the 1 non-tallied
cross-reference noted rather than miscounted. Two findings cluster B
disclosed but could not fix in-fence — `gate fhir`'s own permission-
denied leg, `ehrt play`'s own bare reads — get their first roadmap
anchor under this arc's own new law (AR-RL2-3), the law's own first
execution at an actual arc close; D8-4's own routing call rides along
as that row's own rider rather than staying an open question. Three
design-channel unearned-specificity instances surface across this
close's own accounting — two caught inside their own executing
session, one caught by this close itself before landing — none
escaping into the permanent record uncorrected, the same discipline
ADR-0048's "transcript-witnessed is not repo-recorded" ruling and this
arc's own H-7 precedent both established. The oracle held pure
identity across every commit this arc touched code; this close touches
none. Review 3 opens next, unscheduled, on the author's own cadence
call, inheriting four carried items, two new anchors, a named lint
watch item, and one ruled-but-not-yet-executed routing decision — nothing
smoothed over, nothing taken beyond what was ruled.
