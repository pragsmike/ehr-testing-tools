# Session prompt — repo review 5: the assessment (register + plan),
# hybrid shape, no fixes (ADR-0170)

## Errata, recorded by the session this prompt drove

The prompt's own fence — *"Premise corrections are findings — L-2 is the
ledger, and this prompt is inside its window: if a path, count, or line
above is not what it is described as, that is an L-2 row with this prompt
as its source"* — is honoured here rather than only in the register.
**Eight** premises did not hold; every one is a register row or a
disclosed deviation, and none was adapted around silently.

1. **`.agents/handoffs/` does not exist.** The prompt attributes
   `engine.clj:1490` to "the handoff"; there is no such directory in this
   repo. The citation lives at `notes/adr/0163-…:166` and in the 08-23
   session record. (Register L2-10.)
2. **The `Random` is at `engine.clj:1605` at `f05f51a`, not `:1504`** —
   and `:1490` was **correct when written**. The line walked
   `1490 → 1504 → 1564 → 1605` across four commits in four days, moved by
   arc 0's own work. Not a channel accuracy failure; a citation-rot one,
   and the register's flagship example. (L2-10.)
3. **`run_test.clj:386-440` is wrong at both ends.** `gated-runs` is
   `:396-430`; `:386` sits inside the preceding deftest; and `:440` stops
   one line short of `(use-fixtures :once generate-corpora-once)` at
   `:441` — the line that makes it a fixture at all. True span `:388-441`.
   (L2-12.)
4. **"Arc 0's F-3: 'read the element in hand' was not the mechanism"** —
   arc-0's F-3 is *"`last-uncancelled-index` cannot ride either carrier"*,
   a scope correction. Nothing anywhere retracts "read the element in
   hand"; family (ii) implements exactly that. (L2-11.)
5. **"The three new defspecs PLUS the arc-0 naive-vs-fast defspec"**
   implies four. The tree has **three** new `defspec` forms since
   ADR-0159 (80 → 83), and the naive-vs-fast one **is** one of the three.
   (Register D6-1.)
6. **"Every stated count in the 0160-0169 Verification sections"** — none
   of the ten ADRs has a `Verification` section. The population is empty,
   which is exactly the R4-Q8(a) silent-green hazard the rubric warns
   about; the substance lives under ten differently-named headings, and
   ADR-0169 has none of them. (Register D1-7.)

7. **The tag ledger names two arc closes; the tree has three.** Step 0
   glosses ADR-0163..0169 as "the fix arc close (0166), the traffic-scale
   program (0168), and arc 0 (0169)". Enumerated from `git log` rather
   than from the ADR numbering, the tagless **arc closes** are
   `68af03b` (ADR-0163/0164), `7c1dfa5` (ADR-0165/0166) and `4772e73`
   (arc 0, ADR-0169; pushed tip `f05f51a`) — **three**, and ADR-0168 is a
   design capture rather than an arc close. Corrected in the plan's Q-A.
8. **The expected-files list omits `.agents/plans/README.md`.** The
   READ-BACK fence expects "two plans, one ADR, one prompt archive, one
   session record, roadmap one line, generated files regenerated".
   `ehrt.docs-tooling.index-completeness-test` requires a star-bullet in
   that README for every real file in `.agents/plans`, so landing two
   plan files without touching it leaves the tree red. Fix-forward under
   `R-stop-only-on-two-defensible-readings`, disclosed in the session
   record's deviation 2 and named in the ADR's files-touched list.

One further prompt premise is a correction the register endorses rather
than contradicts: the cadence claim (~ADR-0174, not ~0169) is **true and
doubly landed** — the window's best-executed premise correction — and
this run's ADR-0170 charter is recorded as an author OVERRIDE of it
rather than as compliance with it (register row L2-14).

Everything below is the prompt as issued.

---

## Context

Claude Code in `ehr-testing-tools`, running the `repo-review` skill
(`.agents/skills/repo-review/SKILL.md`, 186 lines — read it whole; this
prompt binds it, it does not replace it). HEAD at handoff: `f05f51a`
(arc-0 close, eight commits pushed 2026-08-25; tree clean). Roadmap row
`roadmap.md#repo-review-5` (OPEN PRIORITY 3) charters this review at
"approximately ADR-0174"; ADR-0159 :466-492 worked the arithmetic
(fifteen past the CLOSE, ADR-0159). This run lands at ADR-0170 — FIVE
EARLY, by author ruling 2026-08-25, because the arc-0 record is fresh and
the review's named theme (unearned specificity, ADR-0159 errata (c)) has
a dense new sample. Record the early charter as an author override of
`R-review-cadence-in-adrs`, not as compliance with it; the next review's
due point is computed from THIS close, per the rule. Sequencing gate per
the skill: no arc is mid-flight — ADR-0169 closed in its own session
(arc 0), arc 1 not yet chartered as a session — confirm at Step 0.

Prior assessment (the baseline this run scores against): review 4,
`.agents/plans/2026-08-18-repo-review-findings.md` +
`2026-08-18-repo-review-4-plan.md`, ADR-0154..0159, arc close ADR-0159
whose "Review 5's inherited watch-list" (:374-402) is THIRTEEN rows
W-1..W-13. The window under review is ADR-0160..0169 (ten ADRs,
2026-08-20 → 08-25): oracle-coverage integration half (0160), attic
rotation law (0161), patient-simulator charter (0162), the
consumer-reported seed-424242 defect arc (0163-0166: compile-time drop,
patient-scoped citation, generator-side coverage meter, care-plan-end
invariant), the suite-time doubling diagnosed as machine-not-tree (0167 +
amendment), the traffic-scale program (0168), arc 0 under equivalence
proof (0169). Eleven session records 2026-08-20..08-25, including the
throughput spike (2026-08-24) which has no ADR of its own — it is
plan/record only, by design; review it as a record.

Shape — carried from review 4's ruling "Q1 c, Q2 register and separate
fix session" (2026-08-18); the author has not re-ruled, so this run
assumes the same shape. Confirm at Step 0 by reading the row; if the row
or a later ruling says otherwise, STOP.

* **(c) HYBRID.** The coordinating session runs the eight-dimension
  battery ITSELF under a probe budget: at most 12 probes per dimension
  (96 cap), each recorded as dimension / method / expected / observed /
  verdict per skill step 3; un-run probes are LISTED, never silently
  skipped. Sub-agents are dispatched for exactly THREE lines this window
  opened, one each, each in its own fresh clone of `f05f51a`, no probe
  cap, findings returned as rows in the register's format:
  * **L-1 GATE VACUITY.** This window found, three times, a green gate
    that saw nothing: ADR-0165 (the ADR-0163 fix had blinded the suite to
    both end types — the coverage meter's first execution found it);
    ADR-0169 F-1/F-2 (the gated corpora carry ten reinstating cancels in
    ONE run and zero successful citation resolutions, so two of arc 0's
    gated-corpus gates would have passed vacuously — the session had to
    add population-scale companions and "actually fires/resolves" count
    assertions); and `digest.clj` :575-588 stating what its 35 roots
    cannot witness (the whole cancel family, `engine/replay`, `sim-check`
    entirely), which arc 0 had to route around. Review 4's L-1 built the
    root × surface matrix for the oracle; do the same for EVERY gate
    added ADR-0160..0169 (`grep -l 'ADR-016' components/*/test` is the
    seed, not the population — enumerate from the ADRs' Verification
    sections): what population does each gate actually see, is that
    population asserted non-empty / non-trivial in the gate itself, and
    which gates are green over a population that cannot exhibit the
    failure they claim to catch. Deliverable: the gate × witnessed-
    population matrix, the vacuous-or-nearly set, and whether ADR-0169's
    "assert the count so a drift to zero goes red" pattern should be a
    `rulings.md` row.
  * **L-2 THE PREMISE-CORRECTION LEDGER.** The design channel's one
    recurring error class is unearned specificity — paths, rows, figures,
    causes, premises asserted without a probe — and this window's records
    carry its in-tree trace as F-numbered premise corrections: F5-1/F5-2/
    F5-3 (throughput spike), F3-1 and F-3 (arc 0: "no 10^5 PROJECTED
    figure exists"; "read the element in hand" was not the mechanism),
    the ADR-0163 diagnosis that the executing session's trace gate
    overturned (a plausible mechanism asserted as cause), the handoff's
    `engine.clj:1490` for a `Random` that lives at :1504, and this
    review's own charter (the channel told the author review 5 was "due
    at ~ADR-0169"; the tree says ~0174). From the archived prompts
    (`.agents/prompts/2026-08-2*.md`) and the session records, enumerate
    EVERY prompt premise a session corrected in this window, classify
    each (path / figure / mechanism / population / cadence / premise-of-
    fact), state which prompt fence caught it (F5-style "premise
    corrections are findings" or none), and whether the correction landed
    in-tree or only in transcript. Deliverable: the ledger as rows, the
    class frequencies, and the one or two prompt-structure mechanisms
    (already in `build-session` or missing) that would have converted
    each class from a session finding into a channel pre-check.
  * **L-3 MEASUREMENT DISCIPLINE.** ADR-0167's lesson (Linux-side signals
    are blind to host load; the Windows-side sample is now part of a
    timed-run health record), the spike's F2/F3 fences (quiet machine,
    MEASURED vs PROJECTED), and arc 0's cell C run at host load 21/30/25
    disclosed as biasing AGAINST its own claim. Enumerate every timed or
    counted figure recorded in the window (suite walls, scaling
    exponents, cell timings, memory figures, speedups): does it carry a
    health record incl. the host-side sample; is it labelled
    MEASURED/PROJECTED where the plan requires; was it taken unpiped;
    does any later surface (roadmap, plan, memory summary, ADR) quote it
    with a precision or a status the record does not support (a single
    cell quoted as a speedup; a projection quoted as a figure). Q4
    (host-side sample into `bin/preflight` or a convention doc — unruled,
    low priority) is the ruling this line prices. Deliverable: the
    timed-figure register with provenance, and the Q4 options with a
    recommendation.

  Sub-agent findings are TRANSCRIPT-WITNESSED until the coordinator
  re-derives at least one cited artifact per finding in ITS OWN clone.
  Rows that survive enter the register as ordinary rows; rows that fail
  are recorded as "sub-agent claim, coordinator could not reproduce",
  never dropped and never promoted. The register states which, per row.
* **Q2:** this session lands the REGISTER and the PLAN only (skill steps
  1-5). NO fix, NO disposition beyond PROPOSED, NO skill amendment (an
  amendment is a plan item for ruling), NO roadmap row closed.

## Read first

1. `.agents/skills/repo-review/SKILL.md` whole; review 4's register,
   plan, prompt (`.agents/prompts/2026-08-18-repo-review-4.md` — its
   Deviation record is five items, read them), and ADR-0159 :348-402
   (review 3's watch-list dispositioned, review 5's inherited thirteen)
   and :403-465 (the four findings the close opened) and :634-end (the
   second addendum: the nightly caught what four green sessions could
   not — a D4/D6 seed).
2. Every ADR 0160-0169 at heading depth PLUS each one's Deviations /
   findings / "worth your attention" sections in full (skill step 2 — ten
   ADRs, read them); ADR-0167's amendment; every session record
   2026-08-20..08-25 (eleven files); `.agents/plans/2026-08-24-traffic-
   scale-program.md` whole (it carries the spike's disposition and arc
   0's MEASURED row at :195).
3. `.agents/rulings.md` whole — D2 maps every standing row to its gate,
   including the rows added this window (`R-mix-1..7`,
   `R-skeleton-or-emission`,
   `R-per-person-streams-before-generator-fixes`,
   `R-output-identical-exempt-from-reshuffle-era`, the attic rotation law
   from ADR-0161); `.agents/state.md`, `state-derived.md`; `roadmap.md`
   OPEN rows (P1..P31, 1 and 2 newly filled by arc 0);
   `reading-sets.edn`.
4. `digest.clj` :575-588; `bin/regression-oracle`; `bin/preflight`;
   `components/sim/test/ehrt/sim/run_test.clj` :386-440 (`gated-runs`,
   the ADR-0165 fixture) and its arc-0 gates;
   `components/sim-check/test/ehrt/sim_check/check_test.clj` (the
   naive-reference oracles and the "actually fire" test) — L-1 seeds the
   coordinator must be able to re-derive.

## Author rulings, verbatim

* Charter: "review 5" (2026-08-25) — this is the run, five ADRs early.
* Shape: carried, "Q1 c, Q2 register and separate fix session"
  (2026-08-18).
* Push: "Have the prompts do the push, too." (2026-08-25) — this session
  pushes its own close (after `R-full-suite-before-push`) and verifies CI
  via `gh` itself (`R-session-verifies-ci-via-gh`). Tags remain the
  author's: NO tag is paid by this session. Step 0 discloses the tag
  ledger instead (below).

## Step 0 (skill step 1)

Fresh clone, tip `f05f51a`; `bin/preflight` (last five CI runs disclosed
via `gh`); baseline `make test` unpiped with the health record incl. the
Windows-side sample (ADR-0167 convention), MAKE_EXIT captured, reconcile
vs ADR-0169's 370 blocks / 4,166 tests / 18,690 assertions at 14m17s;
`poly check`; reading sets vs baselines (`:onboarding` headroom is 34
lines at `f05f51a` per `state-derived.md` :82 — W-13 is live, expect to
compact, `R-budget-stop` applies); confirm no arc mid-flight. **Tag
ledger:** the newest tag is `stable-20260821-patient-simulator-charter`
(ADR-0162); ADR-0163..0169 — the fix arc close (0166), the traffic-scale
program (0168), and arc 0 (0169) — carry no tag.
`R-arc-closes-in-own-session` says an arc closes "with its own tag".
Disclose which closes are tagless; propose the disposition in the plan
(pay at next Step 0 per arc, one catch-up tag, or record a waiver); do
not pay. Then re-derive review 4's own summary arithmetic from its
per-dimension disposition counts (skill step 4's standing sub-step) and
record it before drafting. Dispatch L-1/L-2/L-3 NOW, in parallel with
your own battery, each with: the tip, its charter above verbatim, the row
format, and the instruction to return rows + the commands that produced
each.

## Step 1 — history scan (skill step 2)

From the ten ADRs and eleven records: every incident, deviation,
disclosed self-inflicted red, prediction miss, and channel erratum,
classified to a rubric dimension. Seeds this window itself named (verify
each, do not carry): the born-red / blinded-gate class hit again
(ADR-0165 found the 0163 fix blinded the suite; arc 0 found two gates
vacuous before landing them — W-1 and L-1); the machine-not-tree class
(ADR-0167: a doubling attributed to the tree was an orphaned
`wslhost.exe` — was any tree-side hypothesis acted on before the host was
sampled?); the diagnosis-before-trace class (ADR-0163's first proposed
mechanism overturned by the session's step-2 trace gate — what prompt
structure caught it, and is it in `build-session`); the
second-suite-covers-the-close pattern (arc 0 ran `make test` twice,
14m35s then 14m17s — is a close-commit-only delta required to re-run the
suite, or is that a `R-full-suite-before-push` over-read; a ruling either
way); the memory-summary-supersedes-record pattern (arc 0's close marked
the spike's ranking "superseded" in the plan while leaving the record
untouched — correct, and is it written anywhere that records are
immutable and plans carry supersession); the `## Done` count the session
reported as 30 when the tree held 29 (a figure asserted from memory, not
from `wc`). Repeat-hit classes raise their dimension's severity, per the
skill.

## Step 2 — probe battery (skill step 3), eight dimensions, budget 12 each

Beyond the skill's own probes, this window's specific probes. **D1:**
every stated count in the 0160-0169 Verification sections re-derived
against `git show <sha>` (ten ADRs — all of them); `state-derived.md`
`make state-derived` + diff; W-12 (live plan/register population claims —
the traffic-scale plan's "19 of 31 vendored modules" and "six of 29
invariants" are two to re-derive, they are load-bearing); W-13. **D2:**
every `rulings.md` row → its gate, incl. the window's rows; W-1 (gates
born red since ADR-0159 — ADR-0165's coverage gate WAS born red by
design, its disposition is the ADR itself; is that the precedent or the
rule); W-2 (`fence-exemptions.edn` holds 3 rows at `f05f51a`, unchanged
from ADR-0158 — confirm, and confirm the reason-quality of each); W-11
(diff `rulings.md` `d49f1c6..f05f51a` and ADR-0159..`f05f51a` for
unattributed clauses). **D3:** cold-clone per the skill; W-9
(`R-preflight-fail-closed` — did any record in the window reason around a
`FINDING:`/`UNKNOWN:`); the new push responsibility (the session pushes —
what does `bin/post-push-verify` assume about who pushed). **D4:** suite
wall across the window's records (14m ADR-0167 amendment → 14m35s /
14m17s arc 0, +24 tests) — measure at Step 0, don't carry; the docsgen
tier; L-3 feeds this. **D5:** W-8 (which generated surfaces still
hand-list inputs); the arc-0 plan appendix's MEASURED row — is the plan
on any freshness or lint surface that would catch a stale figure. **D6:**
full-window deviation read (ten ADRs, eleven records — arc 0 alone
disclosed ten deviations by number); W-6 (the historical-red technique:
arc 0 used a two-worktree bracket for byte-identity, which is its
sibling — used a second time? if so W-6 fires and `build-session` owes a
step, plan item); W-7 (83 `defspec` forms at `f05f51a` vs ADR-0159's 80 —
compare the SETS; sample the three new ones plus the arc-0 naive-vs-fast
defspec for the fixed-shape blind spot: arc 0's own
"the-mutations-actually-make-all-six-invariants-fire" test exists BECAUSE
the first hand-picked run did not induce all six — that is W-7's failure
mode, caught in-session; record it as an instance). **D7:** carried-item
aging incl. the thirteen watch-list rows (re-derive each row's CURRENT
state, W-4 `#two-clocks-asset-field-audit` in particular — still open? it
has now outlived three arcs); W-3 (re-run audit (a)'s script over
ADR-0160..0169's row closures); W-10 (roadmap continuation lines under
the right row — the arc-0 close trimmed two rows to the six-line cap;
check the trim did not orphan a line); rows outside any register (Q2/Q3/Q4
from the channel handoff — Q3 is now ADR-0168's stream migration, Q2
closed by ADR-0167, Q4 unruled and unrowed:
`R-unregistered-request-gets-a-row` says it gets a row before a
disposition — plan item); the corpus-player slices (chartered ADR-0014,
now named as arc 4's vehicle in `#emission-add-ons` — rowed at last, or
still only cited?). **D8:** W-5 (`bin/fence-census` re-measured vs
ADR-0158's 28/3/46 of 77 — the number moved or it did not; if it fell, by
exercise or by exemption).

## Step 3 — the register (skill step 4)

`.agents/plans/2026-08-25-repo-review-findings.md` (or your own date),
review 4's row format `id | probe | evidence | finding | recommendation |
disposition` (disposition in {ruling-needed, fix-session-candidate,
close-as-fine, intake} — PROPOSED); scoreboard with review 4's scores
beside; probes-not-run per dimension; the three sub-agent sections with
per-row provenance; review 4's re-derived arithmetic; the thirteen
watch-list rows each with fired / not-fired / cannot-tell and the
evidence. Nothing moves.

## Step 4 — the plan (skill step 5)

`.agents/plans/<date>-repo-review-5-plan.md`: fix-session candidates
batched into proposed sessions (small, fenced, each naming its co-landed
gate); rulings needed as lettered options with a recommendation each (at
minimum: the tag ledger disposition; Q4 host-side sample as gate or
convention; whether L-1's "assert the population count" pattern and L-2's
best prevention mechanism become `rulings.md` rows / `build-session`
steps; the second-suite-after-close question; W-6 if it fired; any skill
amendment; the early-charter override — does the cadence rule get a "may
be pulled forward by ruling" clause or stay as written); the
deliberately-fine list; probes-not-run. The plan goes to the author.
Nothing executes.

## Close (self-archive FIRST)

Archive to `.agents/prompts/<date>-repo-review-5.md`; open the session
record; then ADR-0170 (the assessment ADR: shape as ruled, the early
charter and its override, budgets used per dimension, sub-agent
provenance tallies, the scoreboard delta, the plan's location — NOT an
arc close); roadmap: `#repo-review-5` STAYS OPEN, gains one line pointing
at the register and plan (six-line cap; `## Done` is at 29 of 30, so a
CLOSED row is NOT owed here and no rotation should trigger — if it does,
that is a finding); regenerate INDEX files and `state-derived`; session
record; full `make test` reconciled per namespace vs Step 0 (expected
delta ZERO — no test added; if nonzero, explain); commit "docs: ADR-0170
— repo review 5 assessment: register and plan landed, nothing fixed";
PUSH; `gh run` id/conclusion into the record via a follow-up docs commit
if the run concludes while open, else recorded as owed at next Step 0;
`bin/post-push-verify`. No tag.

## Fences

NO src change; NO test change; NO fix of any finding, however small
("trivial ride-along" is a plan item, not an act); NO skill or rulings
amendment; NO roadmap row closed; NO register other than the two new
files + the ADR + records touched (state-derived and INDEX regenerate by
`make`, that is not a breach); sub-agent rows enter ONLY through
coordinator re-derivation or with the could-not-reproduce label; probe
budget 12/dimension enforced and reported; exit codes unpiped; every
timed figure carries a health record incl. the host-side sample; `out/`
cleared before runs; you date your own artifacts. **Premise corrections
are findings** — L-2 is the ledger, and this prompt is inside its window:
if a path, count, or line above is not what it is described as, that is
an L-2 row with this prompt as its source. READ-BACK names the fence: the
ADR states files touched (expect: two plans, one ADR, one prompt archive,
one session record, roadmap one line, generated files regenerated) and
the close-phase suite delta vs Step 0.
