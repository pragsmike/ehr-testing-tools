# Repo review 5 — mitigation plan, for the author's ruling

Companion to `.agents/plans/2026-08-25-repo-review-findings.md` (88 rows,
`repo-review` SKILL.md step 5). **Nothing here executes.** Rulings are
the author's; this document proposes and recommends.

Shape as ruled 2026-08-18 and carried: **the register and this plan are
the whole of the assessment session (ADR-0170); the fixes are separate,
ruled sessions.** No fix, no ruling, no skill amendment, no roadmap row
closed by the session that wrote this.

**Read the register's cross-dimension pattern first.** Every proposed
session below is an instance of it: *a claim that was true when written,
that nothing keeps true.* The repo's gates ask "is this true?" at
authoring time; almost none ask "is this still true?" — and the one part
of the tree that does (the generated surfaces, regenerated and diffed on
every push) is the one part with nothing wrong in it.

---

## Part 1 — Rulings needed, as lettered options with a recommendation

### Q-A. The tag ledger (Step 0's disclosure)

`stable-20260821-patient-simulator-charter` (ADR-0162, `6ce2160`) is the
newest tag. **ADR-0163 … ADR-0169 carry none** — seven ADRs. Enumerated
from `git log`, not from the ADR numbering, that is **three tagless arc
closes**, not the two the prompt's ledger implies:

| close commit | date | what it closed |
|---|---|---|
| `68af03b` | 2026-08-23 | the unpaired end-step / citation-scope arc (ADR-0163, ADR-0164) |
| `7c1dfa5` | 2026-08-23 | the generator-side coverage / care-plan-end invariant arc (ADR-0165, ADR-0166) |
| `4772e73` | 2026-08-25 | **arc 0** (ADR-0169) — with two follow-ups after it, `ddcd12a` and `f05f51a`, so the arc's pushed tip is `f05f51a` |

`rulings.md#R-arc-closes-in-own-session` says an arc closes "in its own
session **with its own tag**."

- **(a)** Pay one catch-up tag per tagless arc close, at the next Step 0
  of a session touching that arc's area — three tags, each at its own
  close commit.
- **(b) [RECOMMENDED]** Pay **three** tags now, at the author's
  convenience, at `68af03b`, `7c1dfa5` and arc 0's close — and decide
  there whether arc 0's tag goes at `4772e73` (the close commit the
  record's own table names) or at `f05f51a` (the arc as pushed); the
  record's commit table is itself a register row, D1-2. Then add a
  `bin/close-scaffold` line that prints the tag the close owes, so the
  next arc cannot close tagless silently. The tags are the author's to
  pay; the *reminder* is mechanizable and is what actually failed.
- **(c)** Record a dated waiver: the rule means "an arc close is not an
  appendix to the last fix session", and the tag was the marker rather
  than the point. Cheapest, and it weakens a rule that has held for
  fifteen arcs.

### Q-B. Q4 — the host-side sample (the ruling L-3 was chartered to price)

**Row it first.** `rulings.md#R-unregistered-request-gets-a-row` requires
a roadmap row *before* a disposition, and Q4 has none (D7-1). Then:

- **(a)** Host sample inside `bin/preflight`. ~15 lines, two
  `powershell.exe` calls, must render `UNKNOWN:` when interop is dead
  (`R-preflight-fail-closed`) — and interop *does* die; ADR-0167's own
  remedy severed it. **Rejected on the evidence**: `bin/preflight` is by
  its own header a *session-start* script, and arc 0's own three-sample
  table reads **23/16/29 at session start, 1/4/4 pre-suite, 21/30/25
  pre-cell-C**. Session start is the one moment least likely to be the
  moment of the figure.
- **(b)** A ruling clause only: a rider on `R-full-suite-before-push`
  plus `build-session`'s gate-run bullet — *a recorded timed figure
  carries a host-side sample taken at the moment of the figure, and names
  its kind (wall vs poly `Execution time`)*. Zero runtime cost; prose,
  not mechanism.
- **(c) [RECOMMENDED — C plus B]** `bin/host-sample`: ~30 lines,
  `LoadPercentage` ×3, top-6 by CPU, `wslhost` process/thread/parent
  census, powercfg + AC state, printing a one-block record for paste into
  a session record; `UNKNOWN:` when interop is down; **needs `git
  update-index --chmod=+x`** in this clone. Cited by the (b) rider, with
  `bin/preflight` calling it once at session start for the opening
  reading. This is the option that obeys the audit-evidence law — the
  mechanism recommended is the mechanism used — and it makes the health
  record a derivable artifact rather than a hand-transcribed one.
- **(d)** Do nothing. The record-writers have been excellent without it —
  and the two sessions with no such prompt clause produced two figures
  asserted quiet on Linux-side evidence alone, later proven taken under
  an orphaned `wslhost` holding half the machine.

**The rider's second clause is the load-bearing one.** This review's own
baseline settles it: comparing like with like, the post-arc-0 suite is
**+3 s** on both clocks against the ADR-0167 baseline; the arc-0 record's
**+27 s** is a wall minus a poly execution time, and wall-against-wall the
same record's figures give **−12 s**. **Naming the kind, not sampling the
host, is what prevented the only sign error in the window** (L3-1, L3-7).

### Q-C. `R-witness-population-is-counted` (L1-9)

`R-empty-population-is-red` (ADR-0148) gates the **corpus**; it is
satisfied by a gate whose candidate subset is zero — demonstrated, live,
at L1-1. ADR-0169's stronger pattern (pin the count of the subset that
can exhibit the failure) is what separates the gates that survived this
review from the ones that did not.

- **(a) [RECOMMENDED]** Land it as its own row, three lines, contract-shaped:
  *"**R-witness-population-is-counted** — a gate over a generated corpus
  asserts the SIZE of the subset that can EXHIBIT the failure it claims
  to catch, pinned as a count, not merely that the corpus is non-empty; a
  drift of that subset to zero is red, not green — ADR-NNNN"*, citing
  ADR-0169 as origin.
- **(b)** Widen `R-empty-population-is-red` in place. Cheaper, and it
  loses the distinction that is the whole finding.
- **(c)** Leave it as a per-ADR habit. Rejected: the habit is cited in ten
  docs-tooling test files and **zero** simulation-side ones, which is
  exactly the half of the tree where it failed.

### Q-D. The born-red / born-green bifurcation (W-1, D2-4)

This window established two disciplines and wrote neither down: a
behaviour change owes **red-first**; an output-identical refactor owes
**born-green on the pre-change tree** (a *stronger* obligation — the gate
is witnessed passing before it has anything to catch).

- **(a) [RECOMMENDED]** One row carrying both halves, with ADR-0166
  (`:70-90`, the two-directional red witness) and ADR-0169 (`:62-66`) as
  the worked examples.
- **(b)** Two rows. More faithful, more surface.
- **(c)** Leave as precedent. W-1 has now fired at two consecutive
  reviews on the same trigger.

### Q-E. The second suite over the close commit (D2-6)

`R-full-suite-before-push` binds a **push**; arc 0 pushed once and ran the
suite twice, for a reason it stated. Nothing writes the reason down, so
this session had to re-derive it to run its own close.

- **(a) [RECOMMENDED]** A clause: the close commit gets its own full run
  when it touches any docsgen-gated or `.agents/` surface — which is every
  close. States what is already practice and stops the re-derivation.
- **(b)** Leave it. The rule as written is satisfied by one run and the
  second is a session's own judgement.

### Q-F. Correcting a defect that has rotated into the append-only attic (D7-2)

ADR-0161's rotation law moves rows **verbatim**, which faithfully
preserved ADR-0159's F-1 defect into `roadmap-done-2026-08.md:2675-2680`
— an append-only file. The live `#register-gate-row-ownership` row still
says the defect is in `roadmap.md`.

- **(a) [RECOMMENDED]** Correct the live row's location claim (one line,
  errata) and rule that the attic is **read-only history**: a rotated
  defect is described, never edited. The register-gate work then targets
  the *gate*, which is where the row's real subject was all along.
- **(b)** Permit a dated, disclosed correction in the attic. Weakens
  append-only for a cosmetic gain.
- **(c)** Close `#register-gate-row-ownership`'s roadmap half as moot.
  Rejected: the `rulings.md` half (F-2) is untouched and still live.

### Q-G. `R-premise-correction-is-a-finding` (L2-4)

`grep -c -i premise .agents/rulings.md` → **0**. The most-exercised
discipline of the window — 40 corrections in six days — has no row a
prompt can cite, which is why five prompts invented five wordings and six
shipped without one.

- **(a) [RECOMMENDED]** Mint the row, citing `docs/dev/way-of-working.md`
  §2 and this window's forty instances; cite it from `build-session` step
  12 (which already states the doctrine) and from `session-prompt`'s
  fence step (which does not).
- **(b)** Leave it in `build-session` only. That is where it already is,
  and it is a *session rescue*, not a *channel pre-check* — the
  distinction L2-9 makes concrete: way-of-working §2's own textbook
  example (JDK 21 / Temurin 17) **recurred verbatim inside the window**,
  in a prompt authored by the same channel.

### Q-H. Amend-and-quote as the default landing for a correction (L2-15)

~29 of 40 corrections live only in a session record while the erring
prompt or plan sits unamended. The good shape exists and is used exactly
twice: the traffic-scale plan's `:53-60`, which amends in place and
**quotes the original verbatim**.

- **(a) [RECOMMENDED]** Rule it the default for a correction against a
  **live** artifact (plan, roadmap, doc, skill); record-only stays correct
  for a correction against a **dated** one (an ADR, a session record).
  Pair with an `## Errata` stub in `bin/close-scaffold`'s archive template
  (L2-8) so the archived prompt carries what its session found.
- **(b)** Errata stub only. Half the value; the live plans keep drifting.

### Q-I. Verification's home in an ADR (D1-7, L3-5)

**Zero** of ADR-0160..0169 carries a `Verification` heading; the window's
verification substance is spread across ten different heading names, and
**ADR-0169 — the largest src change of the window — carries none of them,
no figures, and no pointer to the record that holds them.**

- **(a) [RECOMMENDED]** Widen `R-session-narrative-hierarchy`: an ADR that
  lands executable change carries either a verification block or a
  one-line pointer to the record that does. Cheap, and it is the same edit
  that fixes L3-5's broken roadmap → ADR → record chain.
- **(b)** Restore a mandatory `## Verification` heading. Heavier, and the
  substance is already being written under better-named headings.
- **(c)** Leave it. Then `roadmap.md`'s 9.58× continues to resolve to an
  ADR that does not contain it.

### Q-J. The early charter (L2-14) — the cadence rule itself

`R-review-cadence-in-adrs` says ~15 ADRs past the prior **close**;
ADR-0159 computed **~0174**, corrected the channel's own ~0169 figure, and
recorded the correction *"so the next session does not average them"*.
This review ran at **0170**, over a ten-ADR window.

- **(a) [RECOMMENDED]** Add a clause: *the cadence may be pulled forward
  by author ruling; the ruling is recorded as an override in the review's
  own ADR, and the NEXT due point is computed from the actual close.* That
  is exactly what ADR-0170 does, and it converts a silent re-override of a
  landed correction into a declared decision.
- **(b)** Leave the rule as written and record this run as a deviation.
  Honest, and it leaves the next pull-forward to re-decide from scratch.
- **(c)** Change the interval. **Rejected on this review's own evidence**:
  a ten-ADR window produced 88 rows and two red dimensions — but the
  window was unusually dense (one large refactor, one incident arc, one
  program), and nothing here argues the interval is wrong in general.

---

## Part 2 — Fix-session candidates, batched

**38 fix-session-candidate rows** batch into **six** proposed sessions.
Each is small, fenced, and names the gate it co-lands —
`repo-review` step 6's standing requirement: *a fix without a gate is
half a fix.*

### Session A — the citation clock (the review's headline)

**Rows:** L2-17, L2-10, L2-6, L2-7, L2-12, D1-1, D1-5, D7-2 (the location
half), L2-18.
**Why first:** it is the cross-dimension pattern's largest and cheapest
instance, and it sits on an `:onboarding` reading-set member, i.e. what a
cold session reads to learn the engine.

- Fix the ≥9 stale `engine.clj:NNN` cites in
  `docs/dev/simulator-architecture.md`, converting them to
  **symbol-anchored** form rather than to new line numbers.
- Errata: `roadmap.md:12` ("folds per `check-all`", not "calls in
  `check.clj`"); `roadmap.md:16-17`'s `ADR-0169 F-n` citations re-pointed
  at the session record; `#register-gate-row-ownership`'s location claim.
- `session-prompt/SKILL.md:146` → `roadmap.md#<slug>` (its own procedure
  at `:111-114` already forbids what its checklist instructs).
- **Co-landed gate:** a lint that resolves every `<file>.clj:<n>` cite on
  a **live** surface against the symbol the prose names, red when it does
  not. Population enumerated from the tree, not from a list. The two
  existing shapes to copy are `citation_gate` and
  `roadmap_lint_test`'s `line-cite-pattern` — the latter's own
  narrowness (it needs a digit, so a `roadmap.md:LINE` placeholder passes)
  is L2-7 and is fixed in the same commit.
- **Fence:** doc and register text only; no `src`, no behaviour.

### Session B — the vacuous gates (D6's red)

**Rows:** L1-1, L1-12, L1-10, L1-7, L1-4, D6-2.
**Why:** two gates in the tree right now are green over a candidate
population of **zero**, and one of them has a docstring claiming the
opposite.

- Pin the candidate count in both ADR-0163 gates and either re-point them
  at a corpus that carries the events or amend the seed-5 docstring.
- `(is (seq targets))` in `reinstate-index-covers-…`, and either expose
  the index or rename the test to what it checks.
- Drop `:churn true` from the two clinic-decade `gated-runs` entries (it
  splices into pathway IR and clinic-decade has zero steps), or give them
  a pathway churn can act on — and record the mechanism where the next
  gate author will read it.
- Give the citation non-vacuity companion the reinstatement companion's
  multi-seed floor.
- Move `the-mutations-actually-…-fire`'s per-invariant check **inside**
  the property, or make each mutator's applicability a generated
  precondition.
- **Co-landed gate:** this session's fixes ARE gates. The ruling at Q-C
  (`R-witness-population-is-counted`) is what makes them a class rather
  than five repairs.
- **Fence:** test-only; no `src`; **no re-pinning of any arc-0 baseline**
  (a changed corpus here would silently retire arc 0's byte-identity
  proof).

### Session C — the measurement ledger

**Rows:** L3-1, L3-2, L3-3, L3-4/D1-2, L3-5, L3-6, L3-7, L3-8, L3-9.
**Why:** nine figures across four surfaces, one of them sign-flipped, all
mechanical.

- Arc-0 record: three commit counts → 8; the "no push, no tag" clause;
  the `wall` column's `13m59s` → the kind-matched comparison (this
  register's own baseline gives the answer: **+3 s on both clocks**).
- `notes/adr/0167:9` → the residual probe's own poly band; drop `26m39s`,
  which no record supports. `:163` and `roadmap.md:330` gain "poly
  `Execution time`".
- `9.58×` → `9.55×` on all five surfaces, or a stated rounding basis.
- The "3.1× a GitHub runner" claim: re-measure unpiped on a quiet penny
  (one `poly test`, ~5 min) or downgrade to "~2×, from a pty-taxed
  pre-reboot run, never re-measured". **Do not leave "uncontended"
  standing.**
- One line in ADR-0169's Consequences citing the arc-0 record and its
  host-load disclosure (this is Q-I(a)'s first application).
- Narrow the plan's umbrella run-parameter block to its own date.
- **Co-landed gate:** none is available for prose figures, which is Q-B's
  whole point. Land this session **after** Q-B is ruled, so the rider and
  `bin/host-sample` land with it.
- **Fence:** docs only; the one permitted measurement is the re-measure
  above, and it carries a full health record.

### Session D — the freshness-gate generalization

**Rows:** L1-8, D3-2, L1-5.
**Why:** in each case **the remedy already exists in the tree** and was
not generalized to its sibling.

- Generalize `attic_rotation_test`'s `git diff --numstat HEAD` working-tree
  step (ADR-0161) to every history-reading freshness gate — starting with
  `hand_owned_asset_freshness_test:81`, which is ADR-0162's own carried
  class.
- Make `bin/preflight` state the HEAD and the moment it ran, so a Step-0
  preflight taken after the session's own edits is visible as such.
- Seed `engine_test`'s `world-of` with empty carrier maps so scripted
  cancel/citation tests take the **shipped** branch, and cross-cite the
  arc-0 defspecs from ADR-0164's two regression gates.
- **Co-landed gate:** a test asserting that every `shell/sh "git" "log"`
  freshness gate also consults the working tree.
- **Fence:** test and `bin/` only.

### Session E — coverage depth

**Rows:** L1-2, L1-3, D7-3 (W-3's gate), D1-4, D1-8.
**Why:** the suite's broadest correctness property is blind to more than
half the event vocabulary, including an invariant added inside this
window.

- Assert which event kinds `every-m1-run-satisfies-the-invariant-catalog`
  actually produces; widen the pathway generator or row the 11 uncovered
  kinds as declared waivers.
- A population-scale gate that witnesses either referential END invariant
  **rejecting** (ADR-0166's own Red-2B mutation shows the shape).
- The W-3 gate: assert a `CLOSED … ADR-NNNN **[slug]**` row's ADR file
  contains the slug. Closes W-3 as a class instead of re-measuring it
  every review.
- Render the written-file count in `state-derived.md` so `AGENTS.md` can
  cite rather than restate it; state `## Done`'s counting definition in
  its own heading.
- **Co-landed gate:** each bullet is one.
- **Fence:** test and generated-register only; **no vendored module, no
  schema, no event-shape change** (`R-event-contract`'s bump obligation).

### Session F — the aged carries

**Rows:** D7-1 (Q4's row), D7-4 (W-4), D8-1 (W-5), D1-6 (W-13), L2-3,
L2-5, L2-8, L2-9.
**Why:** four watch rows fired on aging alone, and the prompt-structure
half is what stops the next forty premise corrections.

- Row Q4 before it is ruled (`R-unregistered-request-gets-a-row`).
- `#two-clocks-asset-field-audit`: state the blocker on the row or fix the
  SVG's audit sentence. It has outlived three arcs.
- `:onboarding` compaction chartered as work, not paid as a per-session
  tax by whoever closes next.
- `session-prompt/SKILL.md` gains the premise fence as a required line and
  both skills gain a premise box in "Done when";
  `bin/close-scaffold` gains the `## Errata` archive stub.
- **Co-landed gate:** `reading_set_budget_test` already ratchets
  `:onboarding`; the skill edits are covered by `skill_mirror_currency_test`
  plus the lint at Q-G.
- **Fence:** registers and skills only. **This session amends skills, so
  it cannot be the session that also rules on them.**

---

## Part 3 — Deliberately fine

Recorded so the author can distinguish "nothing is wrong here" from
"nobody looked". **30 close-as-fine rows**, of which these are the ones
worth stating positively:

- **The generated surface is spotless.** `make docsgen` (all twelve
  leaves, 2m12s) and `make state-derived` both leave the tree
  byte-identical; the skills mirror is `diff -r`-identical at 59 files
  each side; both of review 4's landed banner fixes are intact ten ADRs
  later. This is the one part of the tree with a clock on it, and it is
  the one part with nothing wrong in it.
- **Error honesty.** Arc 0's two new index reads fall back on the KEY,
  never on a missing entry, with the reason written in-source — a
  would-be silent nil deliberately routed into a loud gate failure.
- **The operator surface.** Every CLI probe green against the built
  binary; the README's own demo path run for real, never-overwrite
  contract and all, matching its prose word for word; front-door bare
  fences still zero without the exempt count rising.
- **`R-preflight-fail-closed` in the wild.** Exercised twice for real,
  both times disclosed with the finding named. It did what ADR-0155
  designed it to do.
- **`rulings.md` hygiene.** The window's diff is append-only, twelve rows,
  every one attributed. W-11 could not fire.
- **Arc 0's four gated-corpus/defspec pairs** are the best-guarded gates
  in the repo — the only ones whose thinness is measured *and pinned*, so
  they degrade loudly. They are the reference implementation for Q-C.
- **Review 4's register arithmetic re-derives in every figure** — 72 rows,
  25/27/10/9/1, provenance 20/17/0 — the first time this standing
  sub-step has come back clean.

---

## Part 4 — Sequencing, and what this plan does NOT propose

**One overlap, named so it is not discovered mid-session.** Session A's `session-prompt/SKILL.md:146` fix and Session F's premise-fence additions touch the same file. Either merge them into F (which is already the skills session) or land A's one line first and rebase F — but they may not run concurrently: `skill_mirror_currency_test` holds `.claude/skills` byte-equal, so two sessions editing the same SKILL.md is a guaranteed conflict rather than a merge.

**Order:** Q-B and Q-C are the two rulings that unblock the most work
(Sessions C and B respectively). Q-G unblocks F. Sessions A, D and E are
independent of every ruling and could run first if the author prefers
motion to ceremony.

**Not proposed, deliberately:**

- **No skill amendment is executed here.** The `repo-review` rubric would
  benefit from a ninth probe class — *a claim with no clock* — but the
  assessment session may not amend its own skill, and this plan says so
  rather than doing it. It is Session A's natural close.
- **No tag is paid.** Q-A is disclosed and recommended; tags are the
  author's.
- **No roadmap row is closed.** `#repo-review-5` stays OPEN and gains one
  line pointing here — which, at 6 lines, requires compacting the row
  first (D7-7).
- **No fix, however trivial.** Nine of the rows above are one-line
  errata. They are plan items, not acts.
