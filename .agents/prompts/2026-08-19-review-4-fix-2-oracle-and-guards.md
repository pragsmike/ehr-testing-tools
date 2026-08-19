# Session prompt — review-4 fix 2/5: the oracle's coverage claim (plan Session E + R4-Q6 i/ii/iii) and guard coverage (plan Session C), with the rulings/skill items R4-Q1, R4-Q7, R4-Q8 — ADR-0156

Archived verbatim (R-A). Authored 2026-08-19; executed 2026-08-19.
Session record: [`2026-08-19-review-4-fix-2-oracle-and-guards.md`](../session-records/2026-08-19-review-4-fix-2-oracle-and-guards.md).

---

## Context

Claude Code under R30 in ehr-testing-tools, second fix session of the
repo-review-4 arc. HEAD at handoff: 1e20c63 (ADR-0155 addendum; tree
clean; CI green at 660b7bf and 1e20c63; last tag
`stable-20260819-review-4-fix-1-closure-and-harness` @660b7bf, no tag
owed). Author rulings on the plan 2026-08-18: "Q1 accept all
recommendations. Q2 that order ok. Q3 pair small ones." Every R4-Q is
the plan's RECOMMENDED option. This prompt is E+C, plus the three items
that are skill or register text and pair with C's surfaces (R4-Q1
`--amend` row; R4-Q7 `make ci-parity` named as the D3 probe; R4-Q8 the
zero-population rubric sentence). Rows by id are
`.agents/plans/2026-08-18-repo-review-findings.md`; sessions by letter
are the plan's Part 2. Quote the row.

Carried from fix 1/5 (ADR-0155, channel note, not a ruling): fix 1/5 did
NOT run `bin/regression-oracle` and argued its fence structurally; this
session brackets `7d998f0..HEAD` as well as its own change so the gap
closes on the record. Also carried (ADR-0155 out-of-fence note):
`post-push-verify` check 3 renders a not-yet-indexed run as `status=
conclusion=<pending>` with EMPTY fields, which the `"null"` guard does
not catch -- a fourth exit-rendering shape; fix it here if it is one
line, else row it.

## Channel anchors at 1e20c63 (re-derive every one)

* **E.** `components/oracle/src/ehrt/oracle/digest.clj` (593 lines):
  docstring :45-50 says "Six roots…" (there are 35; the docstring
  accounts for 11 per L1-5) and sits OUTSIDE the soundness body;
  `bin/regression-oracle:105` `digest_body_of` = `awk 'found{print}
  /^\(defn/{found=1; print}'` -- the `(ns …)` form with its 4 requires
  is outside the compared body (L1-4); `rulings.md:29-31`
  `R-oracle-script-contract` says the script "aborts on an undeclared
  digest-source diff" -- overstated. Review 4's pre-digest (ADR-0154
  artifact) found: rung1 48 / rung2 381 / rung3 13 / rung4 0; exactly
  one `:bed-ready` transfer (death-fixture → ED-H02 `:surge`); the
  vacuous set per L1-2 (9/22 decide dispatches, 8/21 evolve, 8 event
  kinds, `orm-message`/`oru-message`/`bed-swap-m…`, every site-profile
  bind, `sim-check` entirely, the second clock). ADR-0153's verdict
  reason ("a vacated LICENSED bed is handed over") is wrong -- L1-1 /
  R4-Q6(i). Oracle tests: none found under `components/oracle/test`
  (re-check: the directory may not exist; `find components -path
  '*oracle*test*'` returned nothing).
* **C.** D2-1: `R-audience-has-entry-path` has no gate; the audience
  surface with numbered segments is `docs/dev/AUDIENCES.md` `##
  Audience` (:18), six `N. **…**` segments; segment 5 (:113-119) "The
  Clojure library consumer, deferred stub" states a serving ("source
  docstrings only"), not a path -- the plan says RULE ON SEGMENT 5
  FIRST. `docs/what-is-this.md:82` has a bulleted (unnumbered) `##
  Audience` -- say whether the gate covers it. D2-2:
  `build-session/SKILL.md` cites `R-full-suite-before-push` (:100) and
  three others but NOT `R-session-verifies-ci-via-gh` or
  `R-stop-only-on-two-defensible-readings`. D2-4: four CI-only freshness
  paths (three `components/palgebra/examples/*-flow*.mermaid`,
  `event-examples.edn`) -- `docsgen_closure_test` (ADR-0155) now asserts
  they are ON the list, which is a local gate on the LIST, not on the
  CONTENT; re-read D2-4 and decide what is still owed (probably only the
  workflow-comment paragraph naming CI-sole-gated paths, and possibly
  nothing).
* **Skill items.** `make ci-parity` (`Makefile:324-332`): fresh clone,
  cold `EHR_TESTING_TOOLS_CACHE`, `poly check`, `poly test :all
  skip:integration`; limit: `HOME`/`~/.m2` shared (R4-Q7).
  `repo-review/SKILL.md` dimension list :28-60 / procedure :108-172 --
  find the D3 line to name the probe in and the step-3 text to add the
  R4-Q8 sentence to. `rulings.md` has no `--amend` row;
  `R-dated-addendum-not-silent-edit` (:168) is its neighbour in spirit.

## Read first

1. Register rows L1-1..L1-5, D2-1, D2-2, D2-4, D2-6, D3-2, D4-4; plan
   R4-Q1, Q6, Q7, Q8; Sessions C and E.
2. `digest.clj` whole (it is the subject); `bin/regression-oracle`
   whole; ADR-0044 (the script contract), ADR-0142 (declared-change
   form), ADR-0150 (a), ADR-0151 (d), ADR-0153 §oracle, ADR-0154 §L-1;
   the pre-digest artifact ADR-0154 recorded.
3. `docs/dev/AUDIENCES.md` whole; `docs/what-is-this.md` :80-100;
   `rulings.md#R-audience-has-entry-path` and its ADR; `usecases.clj`
   (the only docs-tooling file mentioning audience -- see whether an
   AUDIENCES renderer exists to hang a lint on); `build-session/SKILL.md`
   whole + HISTORY; `repo-review/SKILL.md` whole;
   `skill_mirror_currency_test`; `reading-sets.edn` (budgets:
   build-session is in all five sets -- ADR-0143).
4. `rulings.md#R-oracle-script-contract`, `#R-law-surface-propagation`,
   `#R-dated-addendum-not-silent-edit`, `#R-empty-population-is-red`,
   `#R-full-suite-before-push`, `#R-red-pushed-with-green`,
   `#R-session-verifies-ci-via-gh`; `:sim` and `:docs` reading sets.

## Author rulings, verbatim

* "Q1 accept all recommendations. Q2 that order ok. Q3 pair small ones."
  (2026-08-18) -- hence R4-Q6(i) (a) dated addendum on ADR-0153;
  R4-Q6(ii) (a) now -- coverage statement + gate -- and (b) new roots as
  a separate PRICED row, not this session; R4-Q6(iii) (c) widen the
  soundness diff AND amend the rule text; R4-Q1 (a) `--amend` permitted
  narrowly (unpushed, message only) as a row + one build-session line;
  R4-Q7 (a) `make ci-parity` is the standing D3 probe, named in the
  rubric with its `HOME` limit; R4-Q8 (a) rubric sentence: a probe
  reporting zero first asserts a non-empty population and records its
  size.
* C / D2-1 segment 5: RULING WANTED at Step 0 if the gate cannot encode
  both readings -- see Step 0(f).
* Tag: no tag owed at Step 0. This session's own close tag: pay
  in-session if its tip run concludes success while open, else next
  Step 0 -- say which.

## Step 0

Fresh clone, tip 1e20c63; `bin/preflight` (the NEW fail-closed one --
its exit is load-bearing now); baseline `make test` unpiped, MAKE_EXIT
captured, wrapper ENDS `exit "$MAKE_EXIT"`, reconcile vs ADR-0155's 352
blocks / 3,990 tests / 17,876 assertions; `poly check`; reading sets vs
baselines. Then:

(a) `bin/regression-oracle 7d998f0 HEAD` (closing fix 1/5's gap --
predict IDENTICAL: docs/tests/Makefile only; assert).
(b) Pre-digest over 35 roots at HEAD and, from it, the WITNESSED
EVENT-KIND SET (`:event` keywords present in any root's ground truth)
and the witnessed emitter-family set -- this is the committed list the
gate in Step 2 will assert against; reconcile your counts with L1-1/L1-2
(rung tallies, the one `:bed-ready`).
(c) Run `digest_body_of` on the live `digest.clj`: confirm the `(ns …)`
form and the :1-44 docstring are outside the body; count lines in/out.
(d) Measure the soundness-diff widening's effect: with the widened awk
(whole file minus the leading docstring, OR whole file -- decide; plan
says "whole file minus the docstring"), does `7d998f0..HEAD` still read
IDENTICAL? It should.
(e) Check 3 of `post-push-verify` with an un-indexed run id: reproduce
the empty-fields rendering.
(f) Segment 5: can the D2-1 gate be written so that "at least one
markdown link per numbered segment" holds for segment 5 AS WRITTEN? If
segment 5 has a link (e.g. to the go-public section) the gate passes and
no ruling is owed; if it has none, STOP-AND-REPORT with the two readings
-- (i) add a link to the deferred-stub's nearest path (the go-public
gate section / a docstring entry point) so the law holds universally, or
(ii) the gate exempts explicitly-deferred segments by a declared marker
-- and the channel's recommendation: (i), one link, the law stays
universal.

## Step 1 -- E red

(i) Oracle coverage gate: a test (home: `components/oracle/test/…` --
create the test dir if absent, register it in the root `deps.edn`
`:test` paths, `poly check`) asserting the witnessed event-kind set
derived from a FRESH 35-root digest equals a committed list
(`oracle/resources/oracle/witnessed-event-kinds.edn` or a `def` beside
`roots` -- say which; the latter keeps it inside the soundness body,
which is a feature). Mark it integration-tier iff a 35-root digest
exceeds your integration threshold (record the wall time; the pre-digest
is ~? s -- measure at Step 0(b)); if integration, ALSO add a per-push
test that the committed list is non-empty and each kind is a known
`Event` kind (`event_schema` has the kind enum).
`R-empty-population-is-red`.
(ii) Soundness: a shell-level test (shape: `exit_truthfulness_test` runs
`bin/` scripts) that a `:require`-only change to a scratch copy of
`digest.clj` is reported DIFFERS by the widened `digest_body_of` -- red
against the current awk.

Commit: "test: red -- oracle coverage claim gated against a fresh
digest; soundness check must see the ns form (ADR-0156, review-4 E)"

## Step 2 -- E green

`digest.clj`: replace :45-50 "Six roots…" with a current-state paragraph
(35 roots by family, counts from Step 0(b)); add a `COVERAGE` section
INSIDE the soundness body (a `def` or a comment block after the first
`defn` -- it must be in the compared region) naming the vacuous set
verbatim from L1-2 and the one-root-deep capacity witness
(death-fixture; lose it and `:transfer`/`ADT^A02`/`:bed-ready`/rung 3 go
dark together); generalise ADR-0150 (a): the oracle witnesses the
ABSENT-profile identity only (all four bind points nil), not merely
"Z-segments outside". `bin/regression-oracle`: widen `digest_body_of` to
the whole file minus the leading docstring (or whole file -- Step 0(d)'s
decision), update the soundness echo text, and the header comment.
`rulings.md#R-oracle-script-contract`: dated append -- the script
"equivalence-checks the whole digest source minus its docstring and
aborts on an undeclared diff; a `:require`/`:import` change is a
digest-source change -- ADR-0156" (keep the ADR-0044 citation).

Then the oracle on ITSELF: `bin/regression-oracle 1e20c63 HEAD` must
report DIFFERS-in-digest-source (the docstring/COVERAGE edits are inside
the body now) and require `--declared-digest-change` -- this is the gate
working; assert it, declare, and record that the 35 digests are
IDENTICAL under the declaration (predict so: no root or emitter
changed). Full `make test`; push red+green together.

Commit: "feat: oracle coverage stated inside the soundness body and
gated against a fresh digest; soundness diff widened to the ns form;
R-oracle-script-contract text made true (ADR-0156, R4-Q6 ii a, iii c)"

## Step 3 -- R4-Q6(i) and the new priced row (docs)

Dated addendum on ADR-0153 replacing the "vacated LICENSED bed" sentence
with the structural reason (ED `:beds 0`, `config.clj:41`; the one
bed-ready transfer is INTO a surge slot) --
`R-dated-addendum-not-silent-edit`, quote L1-1. Roadmap: NEW row
`[oracle-coverage-roots]` (six-line cap): "R4-Q6(ii)(b) -- add roots
reaching the capacity and order→result paths (a churn root, a pathway
root); each is a declared oracle change and a permanent per-session
cost; priced before taken" -- `## Next`, priority after the review arc.

Commit: "docs: ADR-0153 addendum corrects the oracle-reason sentence;
oracle-coverage-roots rowed and priced (ADR-0156, R4-Q6 i a)"

## Step 4 -- C red

(i) D2-1 gate: a docs-tooling test over `docs/dev/AUDIENCES.md`'s `##
Audience` numbered segments: population = the `N. **…**` items (assert 6
today, non-empty), each carries ≥1 markdown link (per Step 0(f)'s
outcome). Say whether `what-is-this.md`'s bulleted list is in scope
(recommend: same assertion, separate population, since it is the
public-facing one).
(ii) D2-2: `skill_mirror_currency_test` is the propagation gate; the red
here is textual -- a test that `build-session/SKILL.md` cites every
PROCESS row in `rulings.md` tagged as process (if rows carry no such
tag, assert the explicit list of six: the four already cited + the two
missing).
(iii) R4-Q1: a test that `rulings.md` has a row slug
`R-amend-unpushed-message-only` (or the name you choose) -- trivial, but
it keeps the law and its citation co-landed.

Commit: "test: red -- audience segments carry an entry path;
build-session cites every process law; amend row exists (ADR-0156,
review-4 C, R4-Q1)"

## Step 5 -- C green + skill items

AUDIENCES segment 5 per Step 0(f); `build-session/SKILL.md`: cite
`R-session-verifies-ci-via-gh` in the tag/CI step and
`R-stop-only-on-two-defensible-readings` in the STOP-AND-REPORT step,
one sentence each, plus the R4-Q1 line in staging hygiene ("`--amend`
only on an unpushed commit and only for the message; content = new
commit"); mirror byte-copied. `rulings.md`:
`R-amend-unpushed-message-only` row (three lines, cites ADR-0153's
instance and ADR-0156). `repo-review/SKILL.md`: D3 names `make
ci-parity` as the standing local cold-clone probe with its
`HOME`/`~/.m2` limit (R4-Q7); step 3 gains the sentence "a probe
reporting zero first asserts its population is non-empty and records the
size beside the result" (R4-Q8); mirror. D2-4: the workflow comment
paragraph naming which freshness paths are CI-sole-gated, iff Step 0
found any still are after ADR-0155's closure gate (say).
`post-push-verify` check 3 empty-fields rendering → one more `UNKNOWN:`
branch iff one line (test it in `exit_truthfulness_test`), else a
roadmap line. Reading-set budgets: build-session is in ALL FIVE sets --
measure before and after; if any set breaches, compact the skill
(`R-budget-stop`), do not raise. Full `make test`; push red+green
together.

Commit: "fix: audience entry-path gate, process laws cited where
sessions read, amend row, ci-parity named as D3 probe, zero-population
rubric sentence (ADR-0156, R4-Q1 a, Q7 a, Q8 a)"

## Step 6 -- register hygiene

Rows L1-1..L1-5, D2-1, D2-2, D2-4, D2-6, D3-2, D4-4 → `FIXED ADR-0156`
by dated APPEND (keep original tokens); plan Sessions C, E marked
landed; R4-Q1/Q6/Q7/Q8 marked ruled+landed. Roadmap `#repo-review-4`
line → "fix 2/5 (E+C) ADR-0156" (compact, at cap). ADR-0153 addendum as
above.

## Close (self-archive FIRST)

Archive to
`.agents/prompts/2026-08-19-review-4-fix-2-oracle-and-guards.md`; open
the session record; then ADR-0156 (Step 0 a-f with measurements; the
witnessed-kind list as committed; the soundness widening's line counts;
the self-oracle DIFFERS-then-declared result; segment-5 disposition;
budgets before/after), registers, session record with `gh run view`
id/conclusion, full `make test` reconciled per namespace vs Step 0,
`bin/post-push-verify`, tag per ruling.

Commit: "docs: ADR-0156 -- review-4 fix 2/5: oracle coverage and guard
coverage, close"

## Fences

Files: `digest.clj` (docstring + COVERAGE block ONLY -- no `roots`, no
emitter call, no digest logic), `bin/regression-oracle`
(`digest_body_of` + messages + header), oracle tests (+ `deps.edn` test
path), `rulings.md` (two rows: the append and the new one), ADR-0153
addendum, `AUDIENCES.md` segment 5 (one link, per ruling), docs-tooling
tests, `build-session/SKILL.md` + HISTORY + mirror,
`repo-review/SKILL.md` + mirror, `test.yml` COMMENT only,
`post-push-verify` iff one line, roadmap; NO new oracle root (rowed, not
taken); NO change to any digest, root, or emitter path -- the 35 digests
under the declared source change are IDENTICAL (assert and record); NO
engine/emitter/check src; every planted red withdrawn; exit codes
unpiped, wrappers end `exit "$VAR"`; anchored register edits, dated
appends; R-RP.

READ-BACK: files touched vs this list; the oracle's two results
(7d998f0..HEAD IDENTICAL; 1e20c63..HEAD DIFFERS-declared, digests
IDENTICAL); the witnessed-kind list count; budgets.
