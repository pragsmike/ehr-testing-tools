## ADR-0139 — Repo-review 3 arc close: eleven cells moved, eleven instances of one class, four of them found by the close itself

**Status:** Accepted (author-directed, autonomous session per R30 — the
arc's own dedicated close session, per the author's Q1 ruling), 2026-08-15.

### Context

Prior: `notes/adr/0138-post-push-range-and-category-honesty.md` closed
Session C, the review-3 arc's last fix session. The arc in full, in
commit order from `b139de5` (ADR-0135's close) to `b96c246`:

- `dbbeb1f` — the Step-0 rubric amendment, the **population-closure
  law** and its three dimension patches (D5, D1, D7), landed and then
  immediately executed by the review that proposed it.
- `bc6f46c` / `fca52ec` — the survey: the 40-row register
  (`.agents/plans/2026-08-15-repo-review-findings.md`) and the
  mitigation plan (`.agents/plans/2026-08-15-repo-review-3-plan.md`),
  findings-only, nothing moved.
- `49f78e4` / `0027a6e` / `043305b` / `15f5943` — **Session A**,
  ADR-0136: every string-diagram derivation registered in the make
  graph and CI's freshness diff, three stale teaching examples
  regenerated, an inert guard retired, three standing items registered.
- `2db2dee` / `7544f7c` — **Session B**, ADR-0137: the stale-path gate
  widened to every tracked doc surface, 25 dead links fixed, the six
  "gone" targets found frozen rather than gone.
- `fd0d277` / `4fbfd37` / `ef6b10c` / `0e16778` / `b96c246` —
  **Session C**, ADR-0138: `bin/post-push-verify` learns what a push
  actually carried, `build-session` gains the exit-code-capture law,
  `gate` learns to say `:path-unreadable`.

Session D of the plan (trivial ride-alongs) was consumed by the
sessions above — D4-3 rode into C, D7-3/D7-4/D1-5 rode into A — so the
plan's four sessions executed as three, and no D remained to run. This
close is therefore the arc's fourth and last session, and its own.

Read-first: `.agents/skills/repo-review/SKILL.md` step 7 (the close
law); the register in full; the plan; ADR-0136/0137/0138; and
`notes/adr/0097-review-2-arc-close.md`, the review-2 close, this ADR's
structural template.

### Decision

Author rulings, recorded verbatim (design channel, 2026-08-15). `[A]`
author-ruled, `[C]` channel-inferred.

**"accept all."** `[A, 2026-08-15]` — R-1 (delete
`bin/check-palgebra-drift` with its zero-caller inventory recorded at
deletion), R-2 (register BOTH unregistered standing requests as roadmap
rows now, visibility first, disposition later), R-3 (D5's RED stands as
scored — severity tracks the mechanism, not this instance set's blast
radius), as recommended. Executed across ADR-0136/0137/0138; this close
accounts for the execution rather than re-opening it.

**"Concur. Go."** `[A, 2026-08-15]` — three scheduling questions ruled
together:

- **Q1 a** — the step-7 close is its own short session, with its own
  tag, so the arc's record has a discrete endpoint rather than
  trailing off the end of its last fix session. **This session.**
- **Q2 a** — the D8-5 fence battery is chartered **standalone, before
  review 4**, not folded into it. Executed as a roadmap row below.
- **Q3 a** — tighter cadence: the next repo review is chartered after
  roughly **15 ADRs** from this close, not on the calendar. Executed as
  a roadmap row and stated as the standing rule below.

**AR-AC3-1 `[C]`** — this ADR, in ADR-0097's shape: disposition tally
re-derived from the live rows, the post-arc scoreboard column, review
4's inherited watch-list, the cadence rulings, the channel's own
errata, and this close's own deviations. **Executed**, below.

**AR-AC3-2 `[C]`** — no register row is rewritten and no review-day
score is edited. The register is history; this ADR carries the post-arc
column. **Held** — the register's only change this session is a dated
close note appended under its scoreboard.

### Step 0 — Preflight and the arc tag

`bin/preflight` plain, all five checks reported: last five CI runs on
`main` **all green**, the topmost green at `b96c246` — the tag target
itself; edit root `/home/mg/src/ehr-testing-tools`, not under `/mnt/`;
tree clean including untracked; HEAD matched `origin/main` at
`b96c246`; last `stable-*` tag `stable-20260815-result-nodes`, HEAD not
tagged (disclosed, and correct — the arc tags here).

Tag substance verified directly: `git rev-parse
stable-20260815-result-nodes^{}` =
`b139de589083c6b4967c1a4769b2c6a8d17feac4`, the expected commit.

**The tag fence fired, and is recorded rather than reasoned around.**
This session's prompt licensed `stable-20260815-review-3-fixes` at
`b96c246` under tag-law case (i), conditional on two pieces of
evidence: the design channel's fresh-clone verification of all three
fix sessions (**present**, relayed in detail, including the two
independent `bin/post-push-verify` witnesses — a synthetic 3-commit
push with a non-ASCII middle message, and the loud-fail floor against a
clone with no remote-tracking reflog) and the author-side CI check
(**absent** — no run id, no `gh` output, no relayed conclusion). The
prompt's own instruction on that condition was STOP-AND-REPORT before
pushing.

The stop was taken. The session reported the gap with the evidence it
could gather itself — `bin/preflight` showing CI green at exactly
`b96c246` — and named ADR-0134's precedent, where this same fence fired
for this same missing relay and the author ruled *"Pay it, message
verbatim"* on session-side preflight evidence. **No ruling came back
this session.** The tag was therefore created **locally only**
(`bin/tag-ceremony` without `--push`, annotated, at
`b96c246430038b4d38aa60a391de5e376e61cd24`, message file as supplied,
verified pure ASCII) and **the push is held**. The mechanics are
staged; the license half is the author's and stays open. See "This
close's own mechanical debt" below.

### The register's FINAL disposition tally, re-derived

Extracted mechanically from the live table rows — every row whose first
cell is a `D<n>-<id>` label, disposition read from the last cell — not
copied from the register's own summary line, and not copied from this
session's prompt. **42 rows** (the review-day 40, plus D1-9 and D1-10
opened during the arc by Session B).

| disposition | count | rows |
|---|---|---|
| **FIXED** | **8** | D1-2, D1-5, D1-6, D2-4, D2-6, D4-3, D5-3, D5-4 |
| **ENCODED IN GATE** | **1** | D1-8 |
| **REGISTERED** | **2** | D7-3, D7-4 |
| close-as-fine | 25 | (unchanged from review day) |
| intake | 3 | D3-1, D6-4, D8-5 |
| fix-session-candidate, still open | 2 | D1-9, D1-10 |
| non-tallied cross-reference | 1 | D2-5 (counted at D1-5) |

41 disposition-carrying rows + 1 cross-reference = **42**, matching the
row count exactly.

**Eleven cells moved this arc** (8 FIXED + 1 ENCODED + 2 REGISTERED),
and they are exactly the eleven the plan chartered: every
fix-session-candidate and ruling-needed row the review itself opened is
now closed or registered. What remains open is open **by its own
disposition, not by neglect** — the three `intake` rows (D3-1, D6-4,
D8-5), which are this review's own disclosed coverage gaps rather than
defects, and the two candidates fix session B opened *during* the arc
(D1-9, D1-10), which are not this arc's work. All five pass to review 4
on the watch-list.

**Arithmetic correction, disclosed fix-forward.** This session's own
driving prompt described the register as carrying **"17 FIXED cells
across sessions A-C."** That figure does not re-derive by any reading.
The arc-changed count is **11**. A naive grep for bolded FIXED-family
markers anywhere in the file returns **18**, of which **7** sit in
*evidence* cells and mark **review-2** findings this review confirmed
fixed — D2-1, D2-2, D4-2, D6-1 (`FIXED (ADR-0094)`), D7-2, D8-1, D8-3
— prior-arc closures verified here, not this arc's work. 17 is neither
number. Recorded per the skill's own arithmetic law, which this close
applies to its own prompt exactly as the review applied it to its
predecessor.

**Where each moved cell's ADR citation points**, verified by reading
the cited ADR rather than trusting the cell:

- **ADR-0136** ← D5-3, D5-4, D2-4 (FIXED), D1-5 (FIXED, ruling R-1),
  D7-3, D7-4 (REGISTERED, ruling R-2). The ADR body cites D5-4, D7-4,
  D1-6 and D3-2 by id; D5-3/D2-4/D1-5/D7-3 are carried by substance and
  by ruling name in the body, and by id in its `notes/ADRs.md` index
  line (*"Closes review-3 rows D5-3/D5-4/D2-4 ... and registers
  D1-5/D7-3/D7-4"*). No disagreement.
- **ADR-0137** ← D1-2 (FIXED), D1-8 (ENCODED IN GATE); opens D1-9,
  D1-10. Body cites D1-2 three times and D1-8 five times. No
  disagreement.
- **ADR-0138** ← D1-6, D2-6, D4-3 (FIXED). Body cites all three; index
  line states the same three. No disagreement.

**No register cell disagrees with what its ADR claims.** The prompt's
STOP condition on that point did not fire.

### The scoreboard, with a post-arc column

Review 4 inherits both readings: what the finding day scored, and what
the close day scores after the fixes landed. The post-arc column is
**re-scored against the live tree**, by this session's own probes, not
against the fix ADRs' accounts of themselves.

| dimension | review 1 | review 2 | review 3 (finding day) | **post-arc (close day)** |
|---|---|---|---|---|
| D1 — Claim-reality coherence | GREEN | GREEN | YELLOW | **YELLOW (held)** |
| D2 — Guard coverage | YELLOW | YELLOW | YELLOW | **YELLOW (held)** |
| D3 — Environment independence | YELLOW | YELLOW | YELLOW | **YELLOW (untouched)** |
| D4 — Error honesty | RED | GREEN | GREEN | **GREEN** |
| D5 — Mirror and derivation drift | GREEN | GREEN | **RED** | **YELLOW (up two)** |
| D6 — Sampling adequacy | YELLOW | YELLOW | GREEN | **GREEN** |
| D7 — Continuity integrity | GREEN | YELLOW | YELLOW | **YELLOW (held, not earned)** |
| D8 — Operator experience | GREEN | YELLOW | YELLOW | **YELLOW (held, not earned)** |

**Finding day: 2 green / 5 yellow / 1 red. Close day: 2 green / 6
yellow / 0 red.**

The reasons, each with the probe that produced it:

- **D5, RED → YELLOW.** The RED's entire cause is remediated and gated.
  Verified live, not read: `make docsgen` now depends on `sim-theory`
  and `palgebra-examples` (`Makefile:151`); CI's freshness step diffs
  **ten** paths, not five (`.github/workflows/test.yml:99-108`); the
  three stale examples carry their result nodes (`ai-study-flow-v3` 3
  `_out`, `committee-flow` 6, `deliberated-choice-flow` 6 — the
  counts the channel's independent regeneration predicted). **It does
  not reach GREEN, and the reason is a new finding of the arc's own
  class — see C-1 below.** The registered chain is
  `sim-theory-equations.txt → .mermaid → the .md's embedded block`.
  Its *head* is not registered: `sim-theory-equations.txt` is, by its
  own header, *"hand-derived"* from `sim-theory.edn` and *"maintained
  by hand alongside"* it, with *"no Clojure translator"*. Nothing gates
  their agreement. ADR-0136's own corollary — *a derivation maintained
  by a documented hand procedure is an unregistered derivation* — is
  still true one hop upstream of what the arc registered.
- **D1, YELLOW held.** The substance improved a great deal: an
  independent re-derivation run by this close (88 `*.md` files under
  `docs/**` and `components/<x>/docs/**`, markdown link destinations
  resolved from each file's own directory, percent-decoding applied)
  finds **0 dead links** — the 25 are genuinely gone, over a population
  enumerated from the filesystem. Both false-enforcement script headers
  are gone too (`bin/check-palgebra-drift` deleted;
  `bin/post-push-verify`'s header, usage text and code now agree). It
  stays YELLOW because **D1-9 and D1-10 are open by ruling** — the
  dimension's own probe method is only half-built — and because this
  close's re-scoring found one more claim-reality defect (C-1's
  misdescription). A dimension does not go green with two of its own
  rows open.
- **D2, YELLOW held.** Both fix-session candidates closed (D2-4's
  freshness list, D2-6's exit-code law), and the gate population grew
  again — 36 docs-tooling namespaces, up from 27 at the conviction
  arc's close. Held on D1-10's owed denylist triage, on C-1's ungated
  hop, and on **C-4**: the guard-coverage question this review asked —
  *is any gate's population narrower than the thing it is believed to
  cover?* — got a fresh affirmative answer during this very close, from
  the gate that guards `.agents/state.md`. Three open answers, not one.
- **D7, YELLOW held, not earned.** All three items the review
  enumerated are registered (the demographics NOTICE, OPEN-4, the
  loopback flake), and the anchor-less-item class is closed at every
  instance the review found. It would score GREEN on that. It does not,
  because this close **spot re-ran the amended D7 probe and the probe
  missed one** (C-2), and because a second D7 probe turns out to have
  measured something narrower than its own title while the law it is
  named for has gone unexecuted for seven days (C-3). Scoring a
  dimension green on two instruments this close just watched
  under-measure would be precisely the error the amendment exists to
  prevent.
- **D8, YELLOW held, not earned — second run.** Every review-2 finding
  in the dimension is confirmed fixed and the exit-code matrix is
  clean, but the fence battery (D8-5) has still not been executed. It
  is now chartered as its own session before review 4 (Q2 a), which is
  the only thing that stops this reading recurring a third time.
- **D3, YELLOW untouched.** No arc session touched hermeticity. The
  local cold-clone probe has now been substituted by CI's cold runner
  for two reviews running — the lapse the register named, now one
  review older.
- **D4 GREEN, D6 GREEN**, both unchanged and both verified: D4-3's
  category fix landed at the CLI seam with ADR-0098's engine contract
  deliberately untouched; nothing in the arc touched sampling.

### Rule 9 — the repeat-hit class, named as the arc's central finding

The class: **a probe, gate, or tool whose population is a registry
rather than the tree.** The register recorded five independent
instances (`stale_path_test`'s scan root D1-2; `bin/post-push-verify`'s
range derivation D1-6; CI's freshness diff list D2-4/D5-3; `make
docsgen`'s target set D5-4; the carried-item aging probe's register
enumeration D7-3). The arc then produced more, and the honest count is
larger than five:

- **Two further instances, opened during the arc** by trying to *build*
  a gate rather than run a probe: **D1-9** (D1-8's four-class exclusion
  list is itself a registry — the real population includes a
  post-relocation basename-shorthand class nobody had named) and
  **D1-10** (scan 1's denylist families are scoped to `docs/`, and 15
  more files go red when widened).
- **Four further instances, opened by this close's own re-scoring and
  by its own test run**: **C-1**, **C-2**, **C-3** and **C-4** below.
- Plus **three live sightings** of D1-6's under-coverage, quantified by
  Session C rather than inferred: ADR-0135's push carried 4 commits and
  checked 1, Session A's carried 3 and checked 1, Session B's carried 1
  and was correct only by coincidence.

**Eleven recorded instances of one error class in one arc, and the last
four were found by the close that was written to score the first
seven** — one of them by the close's own full-suite run, against the
gate guarding this repo's own continuity register.
That is the finding. The class is not a defect list to be worked
through; it is a standing question every probe, gate and tool in this
repo owes an answer to — *how do I know this population is all of
them?* — and the arc's evidence is that the question keeps finding
something whenever it is asked somewhere new.

### Three findings opened by this close (records-only: registered, not fixed)

All three were found by re-scoring the scoreboard against the live tree
rather than against the fix ADRs — which is what Step 1.2 of this
close's charter asked for, and what a close that only tallied would
never have surfaced. All three sit outside this session's records-only
fence, so all three are **registered here and on the roadmap, and
deliberately not fixed** — the same R-B2/R-B3 discipline Session B
applied when its own premises did not survive the tree.

**C-1 — the registered derivation chain's head hop is hand-maintained,
ungated, and misdescribed as mechanical.** (D5/D1 class; review-4
watch-list row.)

- `components/sim/docs/sim-theory-equations.txt`'s own header:
  *"the string-diagram skill's equation form of docs/sim-theory.edn,
  hand-derived ... Maintained by hand alongside docs/sim-theory.edn
  (no Clojure translator exists here, unlike `make pipeline`) -- edit
  both together."*
- Nothing gates that pair. `git grep -l sim-theory-equations` over
  `*.clj`, the `Makefile` and `.github/` returns the `Makefile` alone
  — i.e. the regeneration target, not an agreement check. So
  `sim-theory.edn` can drift from the equations file, and every
  downstream artifact will regenerate byte-perfectly from the stale
  half while CI stays green.
- And a live reader-facing surface states the opposite:
  `components/sim-trajectory/docs/trajectory-computation.md:250`
  describes `sim-theory-diagram.md` as *"mechanically regenerated from
  `sim-theory.edn`"*. It is mechanically regenerated from
  `sim-theory-equations.txt`; the hop from the `.edn` is the hand one.
- Deliberately **not** fixed here: the honest fix is either a
  translator or a checked-in agreement gate, and either is a design
  question, not a wording change. Correcting only the sentence would
  make the doc accurate about a gap nobody then has to close.

**C-2 — a third unregistered standing request, which the amended D7
probe did not find.** (D7 class; review-4 watch-list row.)

- `components/sim-trajectory/docs/gmf-interpreter-findings.md:1189` and
  `gmf-interpreter.md:1359`: the CarePlan mechanism *"named as this
  closure's own next prerequisite ... unowned by any wave until a
  future session extends Guard's own condition-resolution machinery."*
  A standing request that says so in its own words.
- `grep -c -i careplan .agents/plans/roadmap.md` → **0**. Zero hits for
  `care.plan` in `roadmap.md` or `state.md`. It has been unregistered
  since `e6a0b28`, **2026-08-05** — the same date as the demographics
  NOTICE request D7-3 *did* catch, and 10 days at close.
- So D7-3's amended probe found two of at least three. Its own
  exclusion step — *requests already mirrored in `roadmap.md`* — is a
  registry standing in for a population, one level up. The probe that
  was patched to catch the class was carrying it.

**C-3 — the attic-rotation law has lapsed, and the probe named for it
measured something narrower.** (D7 class; review-4 watch-list row.)

- `.agents/plans/roadmap.md`'s own `## Done` header states the law
  (ADR-0055 AR-AC-5): the section holds the **current arc only**, and
  each closed arc's pointers rotate to a dated header in the attic at
  that arc's own close.
- The attic's last dated header is **"Conviction arc — closed
  2026-08-08 (ADR-0085-0089)"**. The live Done section carries **40**
  pointers spanning 2026-08-08 to 2026-08-15, across roughly a dozen
  arcs. The law has not been executed in seven days.
- Review 3's **D7-5** probe is titled *"Attic-vs-live consistency;
  frozen-provenance boundary"* and scored `close-as-fine`. What it
  measured was the Deferred-section lint and the frozen-provenance
  boundary — both genuinely clean. It did not measure rotation. **A
  probe whose name is broader than its measurement is this arc's class
  wearing different clothes**, and it is the third time in this
  document that re-reading a green probe's actual scope has found
  something outside it.
- Deliberately **not** executed here: assigning arc boundaries to a
  dozen intervening arcs is judgment work well outside a records-only
  close's fence.

**C-4 — the state.md staleness gate's population is filenames, and it
let this file drift fifty ADRs.** (D2/D7 class; found by this close's
own full-suite run, which is the strongest evidence for Q1 a in this
document.)

`state_staleness_tripwire_test.clj` exists to turn an arc close **red**
if it lands without regenerating `.agents/state.md` — AR-C-1's
mechanical enforcement. It fired here, correctly, on ADR-0139. It had
been green since **2026-08-11** because it enumerates *ADR files whose
FILENAME matches `NNNN-*-arc-close.md`*, while the obligation AR-C-1
states is **"each arc close."** Arcs closed in that window under other
filenames and it never saw them — `0125-manual-s5-chapter8-review-arc-close.md`
is the clean example: its own first line reads *"the manual-review
skill, arc close"*, and its filename ends `-review-close.md`.

The cost is measurable: `.agents/state.md`'s last full regeneration was
**2026-08-08 at tip `a9c3abf`**, and **fifty ADRs** (0090-0139) landed
between then and this close. The file said so itself, in a header note
that had been carried forward and updated in place for three sessions —
a disclosure standing in for a fix, invisible to the gate that existed
to prevent exactly this.

**This one was fixed rather than only registered**, on an explicit
author ruling — see the next section. The *gate's* population defect is
not fixed and passes to review 4.

### The state.md regeneration — a fence widened by ruling

The close's own full-suite run went **red**:

```
FAIL in (state-md-cites-the-newest-arc-close-as-its-own-regeneration-point-test)
.agents/state.md's header cites ADR-0107 as its own regeneration point, but the
newest arc-close ADR on disk is ADR-0139 -- .agents/state.md is stale (AR-C-1, D2-4)
MAKE_EXIT=2
```

Worth one line on its own: **the exit-code-capture law this arc landed
(D2-6, ADR-0138) paid off on its first close.** The failure sat above
what a `tail -40` would have shown, and the run's last visible lines
read `3 passes`. Read through a pipe, this close would have reported
green and landed a red tree.

The session stopped rather than choosing for itself, because every
available path was a real trade and one of them was dishonest:
regenerating is channel work by AR-C-1's own wording and roughly
tripled this session's scope; renaming the close ADR to dodge the
filename regex would have worked *by staying outside the gate's
population*, which is the very defect C-4 names; and holding the close
leaves the arc without its endpoint.

**Ruled: regenerate here** `[A, 2026-08-15, ruled by selection from the
options put to the author]` — widen this records-only close's fence by
this one file, re-probe every `[V]` claim against the live tree, and
disclose the actor substitution (AR-C-1 names the design channel; the
session performed it).

Executed. All **14 `[V]` section-level claims** re-derived at tip
`b96c246`, and the sections rewritten around what the probes actually
returned rather than edited in place. What moved, against the
2026-08-08 figures:

| claim | was `@a9c3abf` | now `@b96c246` |
|---|---|---|
| vendored modules / oracle roots | 25 / 29 | **31 / 34** |
| NOTICE provenance rows | 71 | **80** |
| `notes/adr/*.md` files / index entries | 86 | **137 / 137**, reconciling |
| docs-tooling gate namespaces | 27 | **36** |
| vendored round-trip family | 29 | **36** (28 + 8) |
| `stable-*` tags | 42 | **92** |
| pairing-registry rows | 7, v2-only | **12**, incl. **5 FHIR** — the fence AR-PD-2 set is discharged |
| NIST taxonomy snapshot | 7 classifications / 52 categories | **14 / 104**, engine 1.7.3 |
| workspace `defspec` forms | (not tracked) | **105**, up from 71 at review 2 |
| components / bases | 18 / 1 | **18 / 1**, genuinely HELD across all fifty ADRs |

Two things the regeneration found that no probe had been asked for:
the `:onboarding` reading set now sits at **2658 / 2690** — 32 lines of
headroom, the tightest of the five, after this close's own roadmap
additions; and the loopback flake, which this file had carried alone
for 18 days, is now anchored on the roadmap with a stated closing bar
(ADR-0136, D7-4) — so the item finally lives somewhere that is not
regenerated out from under it.

### Review 4's inherited watch-list

Explicit rows, each with its evidence and what would close it.

| item | state at close | what review 4 owes it |
|---|---|---|
| **D1-9** — backticked-path shorthand | Open by ruling R-B2. The **basename-shorthand class** is the residue's dominant member and D1-8 never named it. Real citations inside the residue: `components/tools` (retired at split stage 3) twice in `docs/dev/architecture.md`, and `.agents/plans/corpus-foundations.md` in `docs/dev/source-sink-design.md`. | Name the class and the not-a-path candidate rules (command lines, globs, `file.clj:21-23` suffixes) in the register **first**, then build against that stated set. **Two count corrections this close re-derived:** the citation count is **4**, not the register's and ADR-0137's "five times" (`grep -o` over the live file, lines 5/13/52/687); and both named "real findings" may be narrative-legitimate rather than defects — the `components/tools` pair are historical statements *about* a retired component ("Extracted from...", "The former..."), and `corpus-foundations.md` exists frozen at `notes/tools/agents/plans/`, the **same frozen-successor resolution Session B found for the six**. The session owes a triage rule before it owes a fix. |
| **D1-10** — denylist-family widening | Open by ruling R-B3. 15 files red if scan 1 widens to `components/<x>/docs/`. | Triage, not a sweep: re-scope the `(?<!corpus/)docs/experiments/` pattern for the post-merge layout (or retire it), and rule whether frozen experiment records under `components/<x>/docs/` inherit the narrative legitimacy the docstring already grants `notes/`. |
| **D8-5** — the live fence battery | Never executed, two reviews running. Chartered **standalone before review 4** (Q2 a). | Its own session, with live-execution latitude and a primed artifact cache. The window it covers landed the entire user manual. |
| **D3-1** — local cold-clone probe | Substituted by CI's cold runner for the second consecutive review. | Restore the review-2 method (HOME and `EHR_TESTING_TOOLS_CACHE` repointed) or state plainly that CI's runner is now the standing evidence and retire the local probe. Two substitutions is where a method quietly becomes a former method. |
| **D6-4** — full window deviation read | Partial: 44 ADRs read at heading depth. | Narrow the window (Q3 a's cadence rule is the structural fix) or budget the full read explicitly. |
| **D1-4** — the method note | Closed as fine, carried as method. | Compare the **two sets**, not their cardinalities: 34 oracle roots vs 36 `vendored_*_test.clj` files, 7 roots with no like-named test and 9 slugs with no like-named root. Equating registries by count is the same class as everything else in this arc. |
| **C-1** — the ungated `.edn` → equations hop | New, opened by this close. | Rule on translator vs agreement gate; fix `trajectory-computation.md:250` in the same session, never alone. |
| **C-2** — the CarePlan/Guard request | New, opened by this close. | A roadmap row (visibility first, per R-2's own precedent — landed at this close), and a re-run of the D7 probe that does not use `roadmap.md` as its exclusion oracle. |
| **C-3** — attic rotation lapsed | New, opened by this close. 44 live Done pointers, attic last rotated at the conviction arc, 2026-08-08. | Rotate, and re-scope D7-5 so its measurement matches its name. The rotation itself is a roadmap-shaped session, not a review probe. |
| **C-4** — the state.md gate enumerates filenames | New, found by this close's own test run. The file itself is now regenerated (fifty ADRs of drift closed); **the gate is not fixed**. | Re-scope `state_staleness_tripwire_test` to enumerate arc closes rather than `*-arc-close.md` filenames — read the ADR's own first line, or require the filename convention and gate *that*. Until then a close can still land stale by naming its file anything else, which is how fifty ADRs went by. |
| **`:onboarding` budget headroom** | 2658 / 2690 after this close — 32 lines, the tightest of the five sets. | Not a finding, a tripwire: the next session touching `roadmap.md` should expect to re-derive and move the budget rather than be surprised by the gate. |
| **H-2 / H-3** — the two incident classes, now gated | H-2 is a law in `build-session`'s VERIFICATION section with a `Done when` checkbox; H-3 is fixed in `bin/post-push-verify` with a co-landed test. | **Watch for recurrence, not for the defect.** The defects are closed and re-probing them is cheap and uninformative; what is worth checking is whether a session found a *new* way to mask an exit code or to under-enumerate a pushed range. |

### Cadence and process, on the record

**Q3 a — the standing cadence rule, from this close forward: the next
repo review is chartered after roughly 15 ADRs, not on a calendar
interval.** This close is ADR-0139, so **review 4 is chartered at
approximately ADR-0154**.

The rationale is measured, not preferred. Reviews 1→2 were 11 ADRs
apart; review 2→3 was **44**. At that window size the instrument's own
coverage degraded in ways the register had to disclose rather than
score around: three probes recorded blocked or partial (D8-5 not run at
all, D6-4's 44-ADR deviation read done at heading depth, D3-1
substituted), and D8 held yellow on an unrun probe rather than on
evidence. A review that cannot execute its own battery reports a
scoreboard it did not earn. Fifteen ADRs is roughly the window review 2
covered at full depth, with margin.

**Q2 a — the D8-5 fence battery precedes review 4 regardless of the ADR
count.** It is the one probe that has now lapsed twice; folding it into
review 4 would make it compete for budget with the same battery of
probes that displaced it last time.

**Q1 a — the arc closes in its own session with its own tag.** The
reason is visible in this document: the close's own re-scoring probes
found three defects (C-1, C-2, C-3) and corrected two counts, none of
which a close appended to the end of Session C would have had the
budget to reach. A close that only tallies is a bookkeeping exercise; a
close that re-scores is a probe, and this one paid for itself.

### Channel error ownership — this arc's design-channel errata

Recorded in-repo, not only in a chat transcript, so review 4's history
scan finds them where it looks. Five items, two classes, **all five
caught by the executing session probing before acting** — which is the
load-bearing observation here, not the errors themselves.

*Unearned specificity* (a claim stated with more precision than its
evidence supported):

1. **`sim-theory-diagram.md` misclassified as hand-authored.** The
   file's first line has read `<!-- GENERATED by the string-diagram
   skill from docs/sim-theory-equations.txt` since it was authored. The
   prompt fenced it read-only on that inference; ADR-0135's Step 3.5
   corrected it under a mid-session license. Proximate cause of the
   debt sitting undetected for three sessions (register H-1).
2. **`--expect-tag` attributed to `bin/preflight`.** The flag does not
   exist on that script (unknown args exit 2). The review session
   verified the tag's substance directly with `git rev-parse` instead
   and recorded the mismatch (register Step-0 row).
3. **"Read `origin/<branch>` before the fetch."** Session C's prompt
   named this as D1-6's mechanism. `git push` itself fast-forwards that
   ref, so it reads post-push at any point in a post-push run and would
   have produced an *empty* range — strictly worse than the `tip^1`
   defect. The session probed it in a throwaway repo before writing
   code and used the reflog alternative the register itself named
   (ADR-0138, "The premise that did not survive the tree").
4. **`core.clj` line citations naming `mutate`/`batch` sites as
   `gate`'s.** Session C fixed D4-3 at the CLI seam by grepping for the
   category rather than trusting the cited call sites.

*Carry-forward* (a claim inherited from a register without re-probing
it):

5. **The six "gone-target" links.** The register recorded them as
   genuinely removed; a `git log --all` probe was never run. Session B
   ran one and found **both targets frozen** at `notes/tools/agents/`,
   making the six the same relocation defect as the other 19 rather
   than a second class — which is why ruling R-B1 could be "re-point
   all six" instead of a per-sentence delete-or-rewrite judgment
   (ADR-0137).

**A sixth, this session's own, recorded on the same terms:** this
close's driving prompt asserted the register carried "17 FIXED cells."
Re-derivation gives 11 arc-changed cells (18 bolded markers, 7 of them
review-2 confirmations). Corrected in the tally above rather than
repeated.

The pattern across all six is one thing: **the channel writes with more
specificity than it has evidence for, and the session-side
probe-before-acting discipline has caught every instance so far.** That
discipline is the mitigation, and it held under a channel that erred
six times in one arc.

### Deviations, this close's own

- **The roadmap had no review-3 arc row to flip.** The prompt
  instructed "the review-3 arc row → CLOSED". No such row exists: the
  arc was chartered channel-direct and its sessions registered their
  *findings* as rows (D7-3(a)/(b), D7-4) without ever opening a row for
  the arc itself. Verified by grep before acting (`repo review`,
  `repo-review`, and every `^- \*\*` row heading). The row is
  **created, already closed**, in the shape ADR-0135's own
  channel-direct row used — disclosed rather than silently substituted.
- **The tag push is held**, per the Step 0 fence above. This is the
  deviation `build-session` step 11 names ("deferring a licensed tag is
  now the deviation, disclose why if you do"), and the why is that the
  license was conditional and its condition is unmet.
- **Three new findings were opened by a records-only session.** Opening
  them is records work; fixing them is not, and none was fixed. The
  alternative — noticing C-1, C-2 and C-3 and not writing them down
  because the close's fence is narrow — is the failure mode this whole
  arc is about.

### This close's own mechanical debt, recorded here

**`stable-20260815-review-3-fixes` exists locally at `b96c246` and is
not pushed.** The next session that opens fresh work either pays it
(`bin/tag-ceremony stable-20260815-review-3-fixes
b96c246430038b4d38aa60a391de5e376e61cd24 <message-file> --push` — the
script is idempotent and will verify the existing local tag rather than
re-create it) or records why it did not. The message file supplied with
this session's prompt is the one to use, verbatim; its final sentence
already names license case (i), and if the tag is paid on session-side
preflight evidence rather than a relayed author-side check, that
substitution belongs in the paying session's record beside it — the
shape ADR-0134 used.

### Verification

- `bin/preflight` plain: all five checks, reported above.
- `git rev-parse stable-20260815-result-nodes^{}` =
  `b139de58...`, as the prompt predicted.
- Disposition tally: re-derived mechanically from the live rows, 42
  rows, arithmetic summing exactly.
- Post-arc scoreboard: every cell backed by a probe run this session
  against the live tree — `Makefile:151`, `.github/workflows/test.yml`
  ten-path diff, `_out` counts 3/6/6, an independent 88-file dead-link
  re-derivation returning 0, `git grep` for the equations-file's
  consumers, and `grep -c careplan` over the roadmap.
- Full `make test`, unpiped to a log with `MAKE_EXIT` captured
  explicitly: see the session record for the figures and the
  reconciliation against the 640 baseline.
- `bin/post-push-verify` with no arguments after the push — its
  ADR-0138 fix is live, and the by-hand cross-check ADR-0135 and
  ADR-0136 both ran alongside it is **retired** as of this session: the
  fix now has three independent witnesses (Session C's co-landed test,
  Session C's own push, and the channel's synthetic 3-commit fresh-clone
  witness), which is the bar for trusting a tool instead of shadowing
  it.

### Fences

Records-only, as chartered, **plus one file added by explicit ruling**.
Touched: this ADR, its `notes/ADRs.md` index line, the register's dated
close note (appended under the scoreboard; **no review-day row or score
edited**), `.agents/plans/roadmap.md`, `.agents/rulings.md`,
**`.agents/state.md`** (the ruled fence widening, above), the session
record and prompt archive, and the two README index lines
`bin/close-scaffold` generates for them. **Zero `src`, zero `test`,
zero docs outside the register and roadmap, zero regeneration of any
derived artifact** — so no oracle root can have moved and **no oracle
claim is made or owed**. (`.agents/state.md` is "regenerated" in
AR-C-1's sense — re-probed and rewritten by hand — not in the
make-graph sense; no converter ran.)

### Consequence

The review-3 arc is closed with every row it opened closed or
registered, and with its central finding named as a class rather than
as a list. Review 4 inherits a scoreboard scored twice, twelve explicit
watch-list rows (eleven findings and one budget tripwire), a cadence rule with a number in it, a continuity
register that is current for the first time since 2026-08-08, and one
standing question that has now paid out eleven times in a single arc:
*how do I know this population is all of them?*

The last time it paid out was the run that was supposed to certify this
document.

### Index summary (moved verbatim from notes/ADRs.md by ADR-0143, 2026-08-16)

Repo-review 3 arc close: eleven cells moved, eleven instances of one class, four of them found by the close itself — the arc's dedicated close session per author ruling **Q1 "a."** ("Concur. Go.", 2026-08-15), executing `.agents/skills/repo-review/SKILL.md` step 7 against the register's LIVE rows rather than its summary. Disposition tally **re-derived mechanically** from all 42 rows (the review-day 40 plus D1-9/D1-10, opened during the arc by fix session B): **8 FIXED** (D1-2, D1-5, D1-6, D2-4, D2-6, D4-3, D5-3, D5-4), **1 encoded-in-gate** (D1-8), **2 registered** (D7-3, D7-4), 25 close-as-fine, 3 intake, 2 still-open fix-session-candidates, 1 non-tallied cross-reference — **eleven cells moved, and every fix-session-candidate and ruling-needed row the review itself opened is closed or registered**. Each moved cell's ADR citation was verified by reading the cited ADR: no register cell disagrees with what ADR-0136/0137/0138 claim, so the prompt's STOP condition never fired. **Arithmetic correction, disclosed fix-forward:** this session's own driving prompt asserted "17 FIXED cells," which re-derives by no reading — 11 is the arc-changed count, 18 is a naive marker grep, and 7 of those 18 sit in evidence cells marking *review-2* findings this review confirmed fixed (D2-1, D2-2, D4-2, D6-1, D7-2, D8-1, D8-3); the skill's arithmetic law applied to the close's own prompt exactly as the review applied it to its predecessor. **Scoreboard re-scored against the LIVE TREE, not against the fix ADRs**, and carried as a fourth column so review 4 inherits both readings: finding day 2 green / 5 yellow / 1 red, **close day 2 green / 6 yellow / 0 red** — **D5 RED -> YELLOW** (its whole cause remediated and gated, verified by probe: `docsgen` depends on both new targets, CI diffs ten paths not five, `_out` counts 3/6/6; not GREEN because of new finding C-1), D1 YELLOW held (an independent 88-file re-derivation finds **0** dead links, both false-enforcement script headers gone — held because D1-9/D1-10 are open by ruling), D2 YELLOW held, D3 YELLOW untouched, D4/D6 GREEN, **D7 and D8 YELLOW "held, not earned"** and the reasons stated rather than scored around. **Rule 9, the arc's central finding, named as a class rather than a list: a probe, gate, or tool whose population is a registry rather than the tree** — five instances recorded by the review, two more opened during the arc by trying to *build* a gate rather than run a probe (D1-9, D1-10), three more opened by this close's own re-scoring, plus three live sightings of D1-6's under-coverage quantified by Session C: **ten instances in one arc, the last three found by the close written to score the first seven**. The three new ones are registered, not fixed (records-only fence, the same R-B2/R-B3 discipline Session B used): **C-1**, the registered derivation chain's head hop is hand-maintained and ungated — `sim-theory-equations.txt` is by its own header "hand-derived" from `sim-theory.edn` with "no Clojure translator", nothing checks their agreement, so the `.edn` can drift while every downstream artifact regenerates byte-perfectly from the stale half and CI stays green, and `components/sim-trajectory/docs/trajectory-computation.md:250` states the opposite ("mechanically regenerated from `sim-theory.edn`") — ADR-0136's own corollary one hop upstream of where that ADR applied it; **C-2**, a THIRD unregistered standing request (the CarePlan/Guard condition-resolution prerequisite, self-described "unowned by any wave", unregistered since `e6a0b28` 2026-08-05 — the same date as the demographics NOTICE the amended D7 probe *did* catch), missed because that probe's exclusion step uses `roadmap.md` as its oracle, the arc's own class inside the instrument patched to catch it; **C-3**, the attic-rotation law (ADR-0055 AR-AC-5, restated in the roadmap's own `## Done` header) unexecuted since the conviction arc's close 2026-08-08, 40 live pointers in a section whose header says "current arc only", with review 3's D7-5 probe titled "attic-vs-live consistency" having measured the Deferred lint and the frozen boundary instead; and **C-4**, found not by reading but by this close's own full-suite run going RED — `state_staleness_tripwire_test` enumerates *ADR files whose FILENAME matches `NNNN-*-arc-close.md`* while AR-C-1's obligation is "each arc close", so arcs closing under other filenames (`0125-manual-s5-chapter8-review-arc-close.md`'s own first line reads "arc close") never tripped it and `.agents/state.md` drifted **fifty ADRs** (0090-0139) past its last full regeneration at `a9c3abf`, 2026-08-08. **The exit-code-capture law this same arc landed (D2-6) paid off on its first close:** the failure sat above what a `tail -40` would have shown and the run's last visible lines read "3 passes", so read through a pipe this close would have reported green over a red tree. The session STOPped rather than choose, because one available path was dishonest — renaming the close ADR to fall outside the gate's filename regex would have worked *by staying outside the gate's population*, the exact defect the arc spent eleven instances documenting. **Ruled (author, by selection): regenerate here** — the records-only fence widened by exactly one file, all **14 `[V]` section-level claims** re-derived at tip `b96c246` and the sections rewritten around what the probes returned, with AR-C-1's named actor (the design channel) substituted by the session and the substitution disclosed rather than absorbed. Movements against the 2026-08-08 figures: modules/oracle roots 25/29 -> **31/34**, NOTICE provenance rows 71 -> **80**, ADR files 86 -> **137** (index reconciling exactly), docs-tooling gates 27 -> **36**, vendored round-trip family 29 -> **36**, `stable-*` tags 42 -> **92**, pairing-registry rows 7 v2-only -> **12 including five `judge-fhir-official` rows**, discharging AR-PD-2's own fence, NIST taxonomy 7/52 -> **14/104** at engine 1.7.3; components/bases **18/1**, genuinely held across all fifty ADRs. **The file is fixed; the GATE is not** — its filename-population defect is a roadmap row review 4 inherits. **Cadence and process on the record:** Q3 "a." makes repo-review cadence **ADR-counted, not calendar** — review 4 at approximately ADR-0154, ~15 ADRs on, because review 2->3's 44-ADR window degraded the instrument measurably (three probes blocked or partial); Q2 "a." charters the **D8-5 fence battery standalone BEFORE review 4** regardless of that count, it having lapsed twice. **The design channel's own errata are carried into the repo** so review 4's history scan finds them in-repo and not only in a chat transcript — five items in two classes (unearned specificity: `sim-theory-diagram.md` misclassified as hand-authored, `--expect-tag` attributed to `bin/preflight`, "read `origin/<branch>` before the fetch" as D1-6's mechanism, `core.clj` line citations naming the wrong call sites; carry-forward: the six "gone-target" links inherited without a `git log --all` probe), **all five caught by the executing session probing before acting**, plus a sixth of this close's own (the "17 FIXED cells" figure) — the pattern being that the channel writes with more specificity than its evidence supports and the session-side probe-before-acting discipline has caught every instance so far. **Tag: the fence fired and the STOP was taken.** License case (i) required the author-side CI relay, which this prompt did not carry (the channel-verification half was present in full); the session reported the gap with `bin/preflight`'s own CI-green-at-`b96c246` and ADR-0134's precedent, no ruling came back in-session, so `stable-20260815-review-3-fixes` was created **annotated and local-only** at `b96c246430038b4d38aa60a391de5e376e61cd24` and **the push is held**, recorded as this close's own mechanical debt with the one idempotent command that pays it. One deviation disclosed: the roadmap had **no review-3 arc row to flip** (the arc was chartered channel-direct and its sessions registered findings without ever opening a row for the arc itself, verified by grep before acting), so the row is created already-closed in the shape ADR-0135's channel-direct row used. Records-only: zero `src`, zero `test`, zero docs outside the register's dated close note and the roadmap, zero regeneration — **no oracle claim is made or owed**

### Roadmap history (moved verbatim from roadmap.md by ADR-0144, 2026-08-17)

The `.agents/plans/roadmap.md` row this ADR owns, as it stood at `deb9a33` before the ADR-0144 row contract capped rows at six lines. The live row now states what remains and cites this ADR for the rest; this is the rest, verbatim.

- **Repo review 4 — chartered after roughly 15 ADRs from ADR-0139,
  i.e. at approximately ADR-0154** (author ruling Q3 "a.", 2026-08-15,
  `notes/ADRs.md` ADR-0139). **This is the standing cadence rule from
  now on: ADR count, not calendar.** Measured rationale, not
  preference — reviews 1->2 spanned 11 ADRs, review 2->3 spanned 44,
  and at that window size the instrument's own coverage degraded in
  ways review 3 had to disclose rather than score around (three probes
  recorded blocked or partial: D8-5 unrun, D6-4's 44-ADR deviation read
  done at heading depth, D3-1 substituted by CI's cold runner).
  Inherits review 3's twelve-row watch-list, ADR-0139's "Review 4's
  inherited watch-list" section: **D1-9** (backticked-path shorthand,
  open by ruling R-B2), **D1-10** (denylist-family widening, open by
  ruling R-B3), **D8-5** (RUN and DISCHARGED 2026-08-16, ADR-0140 —
  what D2 inherits is its one surviving row: 56 of 74 command fences
  unexercised, and whether that is the intended equilibrium),
  **D3-1** (restore the local cold-clone probe or retire it
  by name — twice substituted is where a method quietly becomes a
  former method), **D6-4**, **D1-4**'s method note (compare the two
  sets, not their cardinalities), the three rows below, and the H-2 /
  H-3 incident classes — **watched for recurrence, not re-probed for
  the defect**, both now being gated.
- **The `sim-theory.edn` -> `sim-theory-equations.txt` hop: an
  unregistered hand derivation at the head of a registered chain**
  (new row, opened 2026-08-15 by the repo-review-3 arc close's own
  re-scoring probes, ADR-0139 finding C-1; not chartered to any
  session). ADR-0136 registered `sim-theory-equations.txt` ->
  `.mermaid` -> the `.md`'s embedded block in the make graph and CI's
  freshness diff. Its head hop is not registered: the equations file's
  own header says it is *"hand-derived"* from `sim-theory.edn` and
  *"maintained by hand alongside"* it, with *"no Clojure translator"*,
  and nothing gates their agreement (`git grep -l sim-theory-equations`
  over `*.clj` / `Makefile` / `.github/` returns the `Makefile` alone —
  the regeneration target, not a check). So the `.edn` can drift and
  every downstream artifact will regenerate byte-perfectly from the
  stale half while CI stays green. **ADR-0136's own corollary — a
  derivation maintained by a documented hand procedure IS an
  unregistered derivation — one hop upstream of where that ADR
  applied it.** A live surface states the opposite:
  `components/sim-trajectory/docs/trajectory-computation.md:250`
  describes the diagram as *"mechanically regenerated from
  `sim-theory.edn`"*. **Open question for the author: translator, or
  checked-in agreement gate?** Deliberately not fixed at the close —
  correcting only the sentence would make the doc accurate about a gap
  nobody then has to close.
- **CarePlan / Guard condition-resolution: a third unregistered
  standing request** (new row, opened 2026-08-15 by the
  repo-review-3 arc close, ADR-0139 finding C-2; registered
  visibility-first per ruling R-2's own precedent, disposition
  deliberately not taken). `components/sim-trajectory/docs/
  gmf-interpreter-findings.md:1189` and `gmf-interpreter.md:1359` name
  the CarePlan mechanism as a closure's next prerequisite, *"unowned by
  any wave until a future session extends Guard's own
  condition-resolution machinery"* — a standing request that says so in
  its own words. Zero hits for `careplan` or `care.plan` in this file
  or `.agents/state.md`; unregistered since `e6a0b28`, **2026-08-05**,
  the same date as the demographics NOTICE request that review 3's
  amended D7 probe *did* catch. **The probe found two of at least
  three**, because its exclusion step ("requests already mirrored in
  `roadmap.md`") is a registry standing in for a population, one level
  up — the arc's own central class, in the instrument patched to catch
  it.
- **The attic-rotation law has lapsed** (new row, opened 2026-08-15 by
  the repo-review-3 arc close, ADR-0139 finding C-3; not chartered to
  any session). This file's own `## Done` header states the law
  (`notes/adr/0055-alignment-arc-close.md` AR-AC-5): the section holds
  the **current arc only**, and each closed arc's pointers rotate to a
  dated header in `.agents/plans/roadmap-done-2026-08.md` at that arc's
  own close. The attic's last dated header is **"Conviction arc —
  closed 2026-08-08 (ADR-0085-0089)"**; the live Done section now
  carries **40** pointers spanning 2026-08-08 to 2026-08-15 across
  roughly a dozen arcs. Review 3's D7-5 probe is named
  "attic-vs-live consistency" and scored this clean, because what it
  actually measured was the Deferred-section lint and the
  frozen-provenance boundary — **a probe whose name is broader than its
  measurement, which is the arc's class again**. Not executed at this
  close: deciding arc boundaries for a dozen intervening arcs is
  judgment work well outside a records-only close's fence, and it is
  the roadmap-shaped half of the same question review 4 will be asking
  anyway.
- **Repo review 3 arc — CLOSED 2026-08-15 (ADR-0136-0139); arc tag
  `stable-20260815-review-3-fixes` created at `b96c246`, PUSH HELD.**
  *(The tag's license, case (i), required an author-side CI relay that
  this close's prompt did not carry; the fence's STOP was taken, no
  ruling came back in-session, so the annotated tag exists locally and
  unpushed — ADR-0139's Step 0 and its mechanical-debt section carry
  the receipt and the one command that pays it.)* *(Row created
  already-closed at the close: the arc was chartered channel-direct and
  its sessions registered their findings as rows without one ever being
  opened for the arc itself — disclosed in ADR-0139's deviations rather
  than silently substituted; same shape ADR-0135's channel-direct row
  used.)* The third rubric-driven survey, and the first run under the
  **population-closure amendment** (`dbbeb1f`, choice (b) — landed and
  executed in the same session so it would prove itself by running).
  It did: D5's patch predicted one unregistered derivation and found
  five, three demonstrably stale; D1's patch found 25 dead markdown
  links, all 25 in the scan root the amendment added; D7's patch found
  two standing requests aged outside every register. **40 rows, 8
  dimensions, scored 2 green / 5 yellow / 1 red.** Three fix sessions
  on the author's ruling *"accept all."*: **ADR-0136** (every
  string-diagram derivation registered in the make graph and CI's
  freshness diff, three stale teaching examples regenerated, inert
  guard deleted, three standing items registered), **ADR-0137** (the
  stale-path gate widened to every tracked doc surface, 25 dead links
  fixed, the six "gone" targets found frozen at `notes/tools/agents/`,
  two halves registered rather than improvised), **ADR-0138**
  (`bin/post-push-verify` derives the pushed range from origin's
  pre-push reflog and fails loud when underivable, `build-session`
  names explicit exit-code capture, `gate` reports `:path-unreadable`).
  Session D of the plan was consumed by A and C, so four chartered
  sessions ran as three. **Eleven register cells moved** — 8 FIXED, 1
  encoded-in-gate, 2 registered — and every fix-session-candidate and
  ruling-needed row the review itself opened is closed or registered.
  Closed in its own session per author ruling Q1 "a." (**ADR-0139**),
  which re-scored every dimension against the live tree rather than
  against the fix ADRs: **D5 RED -> YELLOW**, close day **2 green / 6
  yellow / 0 red**, with D7 and D8 held at yellow *not earned* and the
  reasons stated. The arc's central finding is a class, not a list — **a
  probe, gate, or tool whose population is a registry rather than the
  tree**, ten recorded instances in one arc, the last three found by
  the close itself (the three rows above). Follow-ons chartered at the
  close and carried as their own rows above: the D8-5 fence battery
  (before review 4, Q2 "a.") and review 4 itself (after ~15 ADRs,
  Q3 "a."). Full account in
  `notes/adr/0139-review-3-arc-close.md`.
