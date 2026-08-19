# Repo review 4 — the mitigation plan, for the author's ruling

Companion to
[`2026-08-18-repo-review-findings.md`](2026-08-18-repo-review-findings.md)
(the assessment register, ADR-0154). Produced under the `repo-review`
skill's step 5, whose own law this document obeys: **RULINGS ARE THE
AUTHOR'S — this skill proposes.**

Nothing here is executed. Per the author's ruling of 2026-08-18 ("Q2:
register and separate fix session"), review 4's assessment session
lands this plan and stops; the fix sessions are separate and follow the
author's rulings below. `roadmap.md#repo-review-4` stays OPEN.

**Scale.** The register carries **72 rows** across eight dimensions and
three sub-agent lines: **25 close-as-fine, 27 fix-session-candidate, 10
ruling-needed, 9 intake**, plus 1 cross-reference (mechanically
extracted; see the register's own summary). This plan batches the 27
candidates into **eight proposed sessions**, states the **10 rulings** as
lettered options with a recommendation each, names what is deliberately
fine, and lists the probes that did not run.

**If the author reads only one thing:** the register's cross-dimension
pattern is review 3's own thesis moved one level up — *a gate whose
population is narrower than the class it is read as enforcing*. Six
instances, and the sharpest (**L3-1**, ruling **R4-Q10**) is the rule
that is supposed to keep every other derived artifact honest.

---

## Part 1 — The rulings, as lettered options

Each carries a recommendation. None is acted on here.

### R4-Q1 — the `--amend` precedent (register D2-6)

ADR-0153 disclosed one message-only `git commit --amend` of an unpushed
commit. `.agents/rulings.md` has no row on `--amend`; `build-session`
has no rule. The act was defensible and disclosed, and the next session
has nothing to follow.

- **(a)** *Permit narrowly, with a row:* `--amend` is allowed **only**
  on a commit that is not yet pushed and **only** to change the message;
  any content amend is a new commit. Row in `rulings.md`, one line in
  `build-session`'s staging-hygiene section.
- **(b)** Forbid outright; a message error is fixed by a follow-up
  commit or a dated addendum.
- **(c)** Leave unruled; disclosure has sufficed twice.

**Recommendation: (a).** It matches what actually happened, it is
mechanically checkable by the author at review time (the range
`bin/post-push-verify` walks is unaffected), and it keeps the
never-rewrite-pushed-history law intact where it matters. (b) costs a
commit for a typo; (c) guarantees the next session re-reasons it.

### R4-Q2 — `bin/preflight`'s exit code (register L2-4, L2-3)

`bin/preflight:162` is an unconditional `exit 0`, reached after any
`FINDING:` or `FAIL:`. Every other ceremony script in `bin/` is
fail-closed. Separately, a failed `gh run list` falls through to
`OK: last five runs all green (or none found)` — the script prints
`FAIL:` first, so the output is self-contradictory rather than silently
false, but the summary line a session quotes says OK. **No behavioral
test covers `bin/preflight`.**

- **(a)** *Declare it advisory:* state in the script header AND in
  `build-session/SKILL.md` that preflight's exit code is not
  load-bearing and its OUTPUT is the artifact; separately fix the
  false-green so a failed query prints `UNKNOWN:` and never `OK:`.
- **(b)** Make it fail-closed: non-zero on any `FINDING:`/`FAIL:`.
- **(c)** Both: fail-closed **and** the `UNKNOWN:` branch.

**Recommendation: (c), with the `UNKNOWN:` branch as the
non-negotiable half.** The false green is the real defect — it
fabricates the one fact Step 0 exists to establish, in the script every
session is told to run. Whether the exit code is also a claim is a
smaller question, but (b) costs nothing and removes an asymmetry that
invites a caller to trust `$?`. If (c) is too much, take (a) and treat
the `UNKNOWN:` fix as mandatory regardless.

### R4-Q3 — `bin/post-push-verify` check 3 (register L2-6)

Check 3 reports the CI run at the pushed tip and the script exits 0
unconditionally — **deliberate and documented** per AR-CI-4 ("reported
once, never awaited"). The residue is that `gh`'s stderr is captured
with `2>&1` into the status field, so a broken `gh` renders as
`status=error: HTTP 401: Bad credentials conclusion=<pending>`, which
skims as "pending".

- **(a)** Keep advisory, fix the rendering: detect a non-zero `gh` and
  print `UNKNOWN:` rather than folding stderr into the status field.
- **(b)** Make a red conclusion at the pushed tip a non-zero exit
  (revisits AR-CI-4).
- **(c)** No change.

**Recommendation: (a).** AR-CI-4 is a considered ruling and (b) would
reopen it for a case the author already decided; the rendering bug is a
separate, smaller defect that (a) closes without touching the policy.

### R4-Q4 — the R-F8 fence rule, now with its number (register D8-2, D8-1)

ADR-0140 handed review 4 a proposed default and said only a ruling
could accept it: *"every fence a reader meets on the README / SETUP /
manual / use-case path is exercised … the census can gate
bare-fence-count-on-reader-path = 0."* **Measured at this tip: 38 bare
and 8 exercised on the reader path** (README 1, SETUP 3, manual 21,
use-cases 13). Repo-wide the figure moved the right way on its own:
50 bare of 76 command fences, from ADR-0140's 56 of 74.

- **(a)** *Tiered, gate what is cheap now:* gate
  `bare-on-README+SETUP = 0` immediately (**4 fences**), and register
  the manual's 21 and use-cases' 13 as a roadmap row with its own
  session.
- **(b)** Adopt the rule in full: all 38, one session.
- **(c)** Adopt as a non-gating census target; report the number at each
  review, gate nothing.
- **(d)** Reject; bare fences on the reader path are acceptable.

**Recommendation: (a).** The full rule is a real session — several
manual fences need a primed artifact cache, which is why D8-5 lapsed
twice — and gating the front door at zero captures most of the risk for
four fences. It also makes the ratchet real: once README and SETUP are
gated at 0, they cannot regress while the manual is worked through.

### R4-Q5 — the hand-regenerated derived surfaces (register D5-2)

Six surfaces carry a written regeneration trigger and are watched by
nothing: five `docs/manual/assets/*.svg` (each naming a specific live
source — `simulator-architecture.md` section 4, `pipeline.edn`'s
Mutate/Gate stages, cited witnessed values, the verdict ranking) and
`trajectory-computation.md:261-262`'s embedded mermaid block. They are
honestly labelled hand-authored; no generator exists, so they cannot
join `docsgen`.

- **(a)** *A register row each*, carrying the trigger verbatim, so the
  aging probe can see them.
- **(b)** *A gate*: a test asserting each banner's cited source file is
  unchanged since the asset's own last commit (a staleness tripwire, not
  a regeneration).
- **(c)** Both.
- **(d)** Accept as deliberately hand-owned and record that decision
  once, so the next review stops re-finding them.

**Recommendation: (b), scoped to the five SVGs.** (a) alone adds six
rows nobody will action; (d) is honest but loses the trigger. (b) is the
instrument this repo already reaches for — it is `sim-theory`'s lesson
("a freshness gate over a chain that excludes the chain's SOURCE proves
only that the middle agrees with itself") applied where no translator
can exist. The mermaid block in `trajectory-computation.md` is fairly
(d) — it describes pipeline shape at a level that rarely moves.

### R4-Q6 — the oracle's coverage claim (register L1-1, L1-2, L1-4)

Three questions, one subject. **This is the review's most consequential
finding.**

**(i) ADR-0153's stated reason for IDENTICAL is wrong.** It says the
verdict holds because "a vacated LICENSED bed is handed over exactly as
before". The oracle's *only* bed-ready transfer hands over a **surge**
bed (`ED-H02`, `:placement :surge`), so the guard's surge branch
evaluates true on it. IDENTICAL holds structurally instead: the
Emergency ward has `:beds 0` (`config.clj:41`), so `home-licensed-free?`
is identically false for every ED boarder.

- **(a)** A dated addendum on ADR-0153 replacing the sentence
  (`R-dated-addendum-not-silent-edit`).
- **(b)** Leave it; the verdict was right.

**Recommendation: (a).** ADR-0153 itself says "a right answer for the
wrong reason is worth catching once" — and then gave a second wrong
reason. An addendum is this repo's own standing instrument for exactly
this.

**(ii) Does the vacuous set warrant new oracle roots, or a coverage
statement?**

- **(a)** *A coverage statement in `digest.clj`* naming what no root can
  move, plus a gate asserting the witnessed event-kind set equals a
  committed list, so widening or narrowing coverage forces the claim to
  move.
- **(b)** Add roots that reach the capacity and order→result paths
  (a churn root, a pathway root).
- **(c)** Both.

**Recommendation: (a) now, (b) as a separate priced row.** (b) is
attractive and expensive: every new root is a permanent oracle cost on
every session's bracket, and adding one is itself a declared oracle
change. (a) closes the actual defect, which is that `IDENTICAL` is read
as meaning more than it does — and the gate in (a) is what would make
(b) visible when someone chooses to pay for it. **Worth stating
plainly: the capacity path's witness is one root deep** — lose
`death-fixture` and `:transfer`, `ADT^A02`, `:bed-ready` and rung 3 all
go dark together.

**(iii) The soundness check's blind spot.**
`bin/regression-oracle` diffs `digest.clj` outside its `(ns …)` form, so
a `:require` change — the exact class the standing-equipment promotion
made — passes silently. `rulings.md#R-oracle-script-contract` says the
script "aborts on an undeclared digest-source diff", which **overstates
what it does**.

- **(a)** Widen the diff to the whole file minus the docstring.
- **(b)** Amend the rule's text and the script header to say a
  `:require`/`:import` change is an undeclared digest-source change that
  must be asserted with `--declared-digest-change`.
- **(c)** Both.

**Recommendation: (c).** (a) is a two-line `awk` change and closes the
hole; (b) is owed regardless, because the rule as written is not true of
the script.

### R4-Q7 — the local cold-clone probe, D3-1's third answer (register D3-2)

Review 3's watch-list posed restore-or-retire. **The method was never
lost:** `make ci-parity` (`Makefile:288-297`) does fresh clone + cold
`EHR_TESTING_TOOLS_CACHE` + `poly check` + `poly test`, and has been in
the make graph the whole time. Two reviews recorded the probe as
"substituted" while the target existed.

- **(a)** Adopt `make ci-parity` as the standing D3 probe, name it in
  the rubric's D3 text, and state its one limit (it does not repoint
  `HOME`, so `~/.m2` is shared).
- **(b)** Restore the review-2 method by hand each time.
- **(c)** Retire the local probe; CI's cold runner is the evidence.

**Recommendation: (a).** It is the same method, already maintained, and
naming it in the rubric is what stops it going missing a third time.

### R4-Q8 — the rubric amendment (register D4-4)

This review's own D4 probe returned a perfect green from a pathspec that
matched **zero files**; a sanity check caught it. `R-empty-population-is-red`
(ADR-0148) already binds *tests*.

- **(a)** Amend the rubric: every probe that reports a zero must first
  assert its population is non-empty, and record the population size
  beside the result.
- **(b)** Leave it to probe-author discipline.

**Recommendation: (a).** It is one sentence, it generalises a rule this
repo already earned twice, and this review is its third instance —
found in the dimension whose subject is exactly this. **Note the fence:
an amendment is a plan item for ruling; this session did not make it.**

### R4-Q9 — two register rows owed (register D7-5)

- `roadmap.md`'s `P2-5 intake staging-dir` row reads
  `DEFERRED (trigger: none recorded -- ADR-0144 finding F-6)`. It
  satisfies the row grammar by declaring the absence of a trigger, so
  the row can never fire. ADR-0144 asked for the trigger on 2026-08-17
  and it is still open. **Options:** state a trigger; convert to
  `## Next`; or close it.
- The **corpus-player slices** (chartered ADR-0014) have no row in any
  register. **Options:** a roadmap row, or an explicit retirement.

**Recommendation:** state a trigger for the staging-dir row (or close
it — it has been deferred since 2026-07-31), and give the corpus-player
slices a row. Both are `R-unregistered-request-gets-a-row`'s own
remedy: visibility first, disposition later.

---

### R4-Q10 — the docsgen/diff-list closure rule (register L3-1, L3-2)

CI's workflow states the obligation in prose: *"if a new derived file
appears, it goes on a make target AND on the diff list below, same
commit."* **It has closure assertions for 2 of 12 docsgen leaves and 2
of 19 diff-list paths.** A sub-agent removed two leaves from `docsgen:`
and four paths from the diff list and the full per-push lane returned
**exit 0, 348/3,960/17,758, zero failures**. Separately,
`event-schema-baseline.edn`'s "deliberately NOT on `make docsgen`" —
the claim that keeps the repo's **only** schema-change gate
non-vacuous — is enforced by nothing (L3-2).

This is the class ADR-0136 opened for five artifacts and that ADR-0149
and ADR-0152 then closed for **one more artifact each**. The class has
never been closed.

- **(a)** *One closure gate, derived from a run:* enumerate docsgen's
  actual write set by running it and asserting set-equality with the
  diff list. Strongest, and slow — probably integration-tier.
- **(b)** *One closure gate, derived from the recipes:* every output
  path named in a `docsgen` leaf's recipe appears on the diff list.
  Weaker (it trusts the recipes to name their outputs) but per-push
  cheap, and it still closes the drop-a-leaf hole.
- **(c)** Keep adding per-artifact assertions as each new artifact
  lands — the status quo, which has produced 2 of 12 in three sessions.
- **(d)** (b) now, plus **L3-2's two-line freeze assertion regardless of
  which option is taken**.

**Recommendation: (d).** L3-2 is two lines beside an existing helper and
removes the possibility of the version gate silently voiding itself;
take it whatever else happens. (b) is the right per-push shape — it
closes the hole this review actually demonstrated, at a cost the
per-push lane can carry — and (a) can follow at integration tier if the
author wants the stronger property.

---

## Part 2 — The fix-session candidates, batched

**Eight** proposed sessions covering all **27** fix-session candidates
(A 5, B 2, C 3, D 1, E 3, F 3, G 4, H 6 = 27). Each is small, fenced,
and **names the gate it co-lands** — per the skill's step 6, "a fix
without a gate is half a fix". Ordering is by severity × cheapness; the
author's queue rules. Sessions G and H arrive from sub-agent line L-3
and are the largest block, which is itself the finding: the
generated-surface class had never been surveyed as a class.

### Session A — harness truthfulness (the watch item that fired)

**LANDED 2026-08-19, ADR-0155** (fix 1/5, paired with Session G), and it
took MORE than the rows listed: L2-4 and L2-6 are closed here too, because
author rulings R4-Q2 (c) and R4-Q3 (a) resolved them and both edits sit in
the same two scripts. Rows closed: L2-1, L2-2, L2-3, L2-4, L2-5, L2-6, L2-10.

**Rows:** L2-1, L2-2, L2-3 (the `UNKNOWN:` half), L2-5, L2-10.
**Why first:** this is review 3's H-2/H-3 watch item firing in a new
shape, and one of its rows is a tracked skill that *teaches* the
masking idiom. Everything here is small and independent of the rest.

- **L2-2** — `.agents/skills/extraction-stage/SKILL.md:95` and its
  mirror teach `> file 2>&1; echo EXITCODE:$?`, whose block exits 0.
  Replace with `EXITCODE=$?; … exit "$EXITCODE"`. **The single highest-value
  edit in this review.**
- **L2-1** — one clause on the law's four surfaces: a wrapper capturing
  `MAKE_EXIT` must END with `exit "$MAKE_EXIT"`.
- **L2-3** — `bin/preflight`'s failed-query path prints `UNKNOWN:`,
  never falls through to `OK:`.
- **L2-5** — `Makefile:90-92`'s for-loop: `|| exit 1` inside the loop,
  or `rm -rf target/use-cases` before the `mkdir -p`.
- **L2-10** — six comment sites in four `bin/` scripts say the wrapper
  "tees" when the code redirects. Reword; an editor "fixing" the code to
  match its comment would reintroduce ADR-0152's defect into five gate
  scripts.

**Co-landed gate:** a `docs-tooling` lint over `.agents/skills/**` and
`.claude/skills/**` rejecting a taught gate idiom that ends in
`echo …$?` without a following `exit`; **plus the first behavioral test
for `bin/preflight`**, which today has none. Red-first on the
extraction-stage line.

**Fence:** no change to what any gate command *does*, only to how its
status is propagated and described.

### Session B — environment residue (the three-hit class, at its root)

**Rows:** D3-1, D3-2.
**Why:** the executable-bit class has bitten three times in this window
alone and was gated at the symptom every time. The root cause is two
lines of local git config left over from the retired `/mnt/c` era.

- `git config --local core.fileMode true` and
  `git config --local --unset core.ignorecase` in the edit root.
  **Verified safe:** a fresh clone at this tip with `core.fileMode=true`
  has a completely clean tracked tree, so the flip introduces zero
  churn; and no tracked paths collide case-insensitively today.
- Adopt `make ci-parity` as the standing D3 probe (pending **R4-Q7**).

**Co-landed gate:** add a `core.fileMode` check to `bin/preflight`'s
environment section, beside the existing `/mnt/*` check — the same
shape, for the same class of residue. Keep `executable_bits_test`: it is
the gate that made the class visible and it protects clones this config
change cannot reach.

**Fence:** `.git/config` is not tracked, so this session's only tracked
edits are `bin/preflight` and its test.

### Session C — guard coverage (three laws, three gaps)

**Rows:** D2-1, D2-2, D2-4.

- **D2-1** — `R-audience-has-entry-path` has **no gate**. Add one:
  every numbered segment under `## Audience` carries at least one
  markdown link. **Rule on segment 5 first** ("the Clojure library
  consumer, deferred stub" states a serving, not a path) — the gate
  encodes whichever answer the author gives.
- **D2-2** — `R-session-verifies-ci-via-gh` and
  `R-stop-only-on-two-defensible-readings` are cited on **no** surface a
  session reads. Add both to `build-session/SKILL.md` where the other
  four already live; mirrors byte-copied.
- **D2-4** — four of CI's 20 freshness paths (the three
  `palgebra/examples/*.mermaid` and `event-examples.edn`) have **no
  local gate**; the workflow's comment block does not say which paths it
  is the sole gate for. Add that paragraph, and optionally close the gap
  for the three `.mermaid`.

**Co-landed gate:** the AUDIENCES lint (D2-1) is itself the gate; for
D2-2 the existing `skill_mirror_currency_test` covers propagation once
the text lands. Watch the `:docs` and `:onboarding` reading-set budgets
— `build-session/SKILL.md` is in **all five** sets, and ADR-0143 records
that this exact file has grown 162 → 309 lines by accretion.

### Session D — result-or-loud, widened to the class it names

**Row:** D4-1.
**Why:** `R-io-result-or-loud`'s lint forbids
`.listFiles`/`.list`/`.renameTo` and misses `.mkdirs`/`.delete`. There
are **13** `.mkdirs` sites in production `src` discarding the boolean —
including one inside `ehrt.kernel` itself — and 2 `.delete`.

**Not a mechanical sweep.** `.mkdirs` returning `false` is ambiguous
(already existed, or failed), so the fix is a kernel helper asserting
`(or (.mkdirs f) (.isDirectory f))` and failing loud otherwise, then
routing the 13 sites through it.

**Co-landed gate:** widen `io_vocabulary_lint_test`'s `forbidden-patterns`
by two, with the kernel namespace allowlisted as it already is for
`.listFiles`. Red-first on one site.

### Session E — the oracle's coverage claim

**Rows:** L1-2, L1-3, L1-5, plus **R4-Q6(i)**'s addendum and
**R4-Q6(iii)**'s soundness fix if ruled.

- A `COVERAGE` section in `digest.clj`'s docstring naming the vacuous
  set: the churn family, the whole order→result path (`:order-placed`,
  `:result-available`, `ORM^O01`, `obx-segment`), `:transfer`/`:bed-swap`/
  `:merge`, every site-profile override, the second clock, and
  `sim-check` in its entirety.
- Replace `Six roots` (`digest.clj:45`) with a current-state paragraph;
  the docstring accounts for 11 of 35 and sits **outside** the soundness
  body, so it can drift ungated.
- Generalise ADR-0150 (a)'s note: the oracle witnesses the
  **absent-profile identity** only, not merely "Z-segments are outside".

**Co-landed gate:** a test asserting the witnessed event-kind set equals
a committed list, so a root that widens coverage must update the claim
and a change that narrows it goes red. This is the gate that makes
R4-Q6(ii)(b) — adding roots — a visible, priced decision later.

### Session F — sampling adequacy, and the register rows owed

**Rows:** D6-1, D7-3, D1-1.
Grouped because each is small and none shares a surface with the others.

- **D6-1** — `every-m1-run-satisfies-the-invariant-catalog` runs 150
  trials against a **fixed** facility whose two wards are ED (0 beds/15
  surge) and Renal (1 bed/0 surge), with no churn. ADR-0153's defect
  needed one ward with **both** a licensed bed and a surge slot, plus
  churn — so the property was structurally incapable of finding it, at
  any trial count. Generate the facility (or add a second defspec) and
  put a churn profile on some fraction of trials.
- **D7-3** — D1-9 and D1-10 have been ruled fix-session candidates since
  2026-08-15 with **no roadmap row**, carried through one arc close and
  fourteen ADRs. One row (they were ruled together as R-B2/R-B3).
- **D1-1** — four ADRs cite a suite figure to an ADR that does not carry
  it (it lives in the session record). Either ADRs record their own
  **close** figure, or the reconciliation sentence cites the record.

**Co-landed gate:** D6-1's widened defspec is its own gate — and it
should be **red-first against the pre-ADR-0153 engine** to prove the
widened sample would have caught the original bug. That is the strongest
possible evidence for this row and it is cheap: the fix is one helper in
`engine.clj`.

---

### Session G — the closure gates (small tests, high leverage)

**LANDED 2026-08-19, ADR-0155** (fix 1/5, paired with Session A). L3-1
landed here too rather than waiting: author ruling R4-Q10 (d) took option
(b), and its closure gate is what subsumes L3-10's pairing check, exactly
as this session's own "Depends on" note anticipated. Rows closed: L3-1,
L3-2, L3-4, L3-9, L3-10.

**Rows:** L3-2, L3-4, L3-9, L3-10.
**Why grouped:** all four are *closure* assertions — each says "this
population is the whole population" — and each is a small test beside a
helper that already exists.

- **L3-2** — assert `event-schema-freeze` ∉ prerequisites of `docsgen`.
  Two lines beside `docsgen-depends-on-the-traces-target-test`.
- **L3-4** — `docs/dev/pipeline.md` is a generated artifact that is an
  *input* to `state-derived.md`, and the edge exists only as the
  left-to-right order of `docsgen:`'s prerequisites. Declare
  `state-derived: pipeline`, or assert the ordering the way
  `the-sim-theory-target-writes-the-equations-file-first-test` already
  does within a recipe.
- **L3-9** — `write-use-cases!` never prunes `pages-dir`, so a dropped
  case leaves a page that outlives its source, green forever. Assert
  `set(docs/use-cases/*.md) == set(case ids)`.
- **L3-10** — `palgebra-examples` hardcodes three converter calls
  against five `*-equations.txt`, with **zero** test references to the
  directory. Derive the population from the directory, or assert the
  pair set.

**Co-landed gate:** each row *is* a gate. Red-first is cheap for all
four (plant an orphan page; add a fourth `.mermaid`; put
`event-schema-freeze` on `docsgen`).

**Depends on:** **R4-Q10** if the author takes (a) or (b) — the closure
gate there may subsume part of L3-10.

### Session H — every artifact points back at its inputs

**Rows:** L3-3, L3-5, L3-6, L3-7, L3-8, L3-11.
**Why grouped:** one defect in six places — a generated artifact that
does not name what moves it, which is what made both `state-derived.md`
movers and the ADR-0135 converter blast radius discoveries rather than
predictions.

- **L3-3** — have `state-derived.md`'s renderer **emit its own input
  list** (26 reading-set paths, the directory roots, `digest.clj`, the
  modules `NOTICE`). A generated enumeration cannot go stale, and it
  turns "which of my edits moved this?" into a grep.
- **L3-5** — `docs/formats.md`'s banner names one of its two inputs;
  `event-examples.edn` supplies every example on the page. One line.
- **L3-6** — the python converter produces bytes in 28 artifacts and is
  named at 1. Name it in each banner.
- **L3-7** — 19 docsgen outputs carry no marker (4 `.mermaid`, 14
  traces, `formats.md`). Give the converter a `%%` banner (verify the
  ADR-0135/0152 arrow-renumbering hazard is unaffected) and the traces a
  per-directory note.
- **L3-8** — `AGENTS.md` tells a cold agent that **4** files are
  generated when **53** are. Replace the hand list with a pointer to a
  generated one.
- **L3-11** — `demos/traces/README.md`, the tree's own front door, never
  mentions `make traces`.

**Co-landed gate:** L3-3's emitted input list is self-gating (it is
generated). For the rest, the honest gate is L3-8's generated list plus
a marker assertion over the docsgen write set — **which is R4-Q10's gate
doing double duty**, so land this session after that ruling.

**Watch:** L3-3 and L3-8 both add lines to `state-derived.md`, which is
counted by four reading sets. Re-measure budgets at close
(`R-register-hygiene-at-close`).


## Part 3 — Deliberately fine

Recorded so review 5 does not re-open them:

- **The skills mirror.** 60 files, byte-identical, and the gate checks
  content, executable bit, and orphans. The strongest guard in the repo.
- **The CLI's error surface.** Every failure names its artifact and
  carries a category; unknown-command carries a recovery route. Bare
  invocation exiting 0 with usage is the ruled status quo.
- **Help at 40/80/120 columns.** Genuinely re-wraps; zero overflow.
- **`set -uo pipefail` without `-e`** in 15 `bin/` scripts. Deliberate
  and correct — `gate fhir`'s taught rejection is exit 1 *by design* and
  `set -e` would abort on it. Worth one line in the `expect` header block
  stating the invariant the shape requires, but not a fix.
- **The two `catch` blocks returning `"unknown"`/`nil`** in
  `cli/core.clj`. Optional environment probes where absence is a real
  answer.
- **`.gitattributes`.** Six `-text` carve-outs, each traceable to the
  incident that earned it; ADR-0149's CRLF finding is resolved in both
  the edit root and a fresh clone.
- **The two `gmf-interpreter-findings.md` standing requests.**
  Conditional guidance sited where the triggering session would read it,
  not requests awaiting action.
- **`state.md` at 119/120 lines** and **`:onboarding` at 1398/1530.**
  Tripwires, not findings — the next session touching either should
  expect to compact rather than bump.
- **The oracle's module breadth.** All 31 vendored module JSONs are
  reached by at least one root; determinism verified across independent
  runs. Every gap this review found is in engine/emitter path coverage,
  never module coverage.

---

## Part 4 — Probes that did not run, named rather than dropped

The skill requires this list; the driving prompt requires it per
dimension. The probe budget was 12 per dimension (96 cap) and **no
dimension exhausted it** — the constraint on this review was the
window's own shape, not the budget.

| dimension | probes run / budget | probes NOT run, named |
|---|---|---|
| D1 | 5 / 12 | Re-hashing every stated hash against on-disk bytes (no new hash claims landed in this window). Full citation re-resolution across every tracked doc surface — delegated to the standing `stale_path_test`, whose fourth scan root closed review 3's D1-2, rather than re-run by hand. |
| D2 | 6 / 12 | **The full 113-row rulings→gate map.** This review mapped the **nine rows added in this window** plus the multi-surface family, not all 113. A complete map is a session's work and is the single most valuable un-run probe here — review 3 did not run it either, and no artifact in the repo holds one (`2026-08-17-rulings-census.md` is a migration census, not a gate map). |
| D3 | 4 / 12 | **`make ci-parity` itself was not executed** — identified as the standing method (D3-2) but not run, because a second full suite under this session's contention would have produced a number worse than useless. Also not run: test helpers' tmpdir/parallelism/file-descriptor assumptions. |
| D4 | 4 / 12 | Silent caps and truncations; parses that default on malformed input. The nil-returning-I/O and catch-block probes were run; these two were not. |
| D5 | 4 / 12 | The exhaustive three-way population enumeration (banner scan × make graph × freshness list) was **delegated to L-3** — see the L-3 section for what it did and did not establish. |
| D6 | 3 / 12 | Per-sample power analysis for the census seeds and the round-trip populations. This review probed the one verdict this window's own defect bore on (D6-1) rather than the whole sampled-verdict population. |
| D7 | 5 / 12 | Attic-vs-live consistency beyond the Done-pointer count; the pairing/index/done-pointer gates were taken as green from the Step-0 suite rather than re-run individually. |
| D8 | 4 / 12 | **Executing the live command fences** — the D8-5 battery ran standalone on 2026-08-16 (ADR-0140) and this review re-measured its *census* rather than re-executing 76 fences. The README's own two-commands-to-demo path was not run for real this time (`make quickstart` exists and is CI-adjacent). |

**Three limits on this review's evidence, stated plainly:**

1. **Timings — partly answered at the close, and the remainder named.**
   The Step-0 suite (1,273 s) ran under three-sub-agent contention and
   is not comparable to anything. **The close-phase suite ran on a quiet
   machine: 944 s (15m44s), against ADR-0149's recorded 14m52s (892 s)**
   — about +6% across five ADRs that added test namespaces, which is
   growth, not regression. **So the suite half of the prompt's timing
   probe IS answered: nothing got materially slower.** What remains
   unanswered is the `docsgen` per-push tier question ADR-0149 left
   open: `make test` does not run `docsgen` (CI's freshness step does),
   and the only measurement this review obtained is L-3's **2:48** in
   its own clone **under contention**, against ADR-0149's 119.40 s. That
   is suggestive of real growth and is not evidence of it. **One quiet
   `make docsgen` at this tip would settle it** — the cheapest open
   question in this document.
2. **Sub-agent provenance is labelled per row**, never merged into the
   coordinator's own evidence. Rows marked "re-derived in part" carry
   the coordinator's confirmation of the *structural* claim with the
   sub-agent's instrumentation named as the source of the per-surface
   counts.
3. **Each sub-agent's own disclosed limits are carried into the register
   verbatim**, not summarised away: L-1 could not run a real
   `bin/regression-oracle` bracket across a mutated tree (the charter
   forbids committing a mutation), so its vacuous set rests on a
   reachability battery proven byte-identical to the clean digest rather
   than on the shell script's own verdict; L-2 did not observe real CI
   behaviour of the `test.yml` multi-line `run:` block; L-3 did not run a
   full parallel `make -j docsgen` and proved only one misordering edge.
   None of the three affects a row's disposition, and all three are
   stated where the rows live.
