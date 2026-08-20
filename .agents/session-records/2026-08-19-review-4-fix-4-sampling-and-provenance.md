# Session record: review-4 fix 4/4 -- sampling adequacy and artifact provenance (2026-08-19)

**Prompt:** [`.agents/prompts/2026-08-19-review-4-fix-4-sampling-and-provenance.md`](../prompts/2026-08-19-review-4-fix-4-sampling-and-provenance.md)
**ADR:** [`notes/adr/0158-sampling-adequacy-and-artifact-provenance.md`](../../notes/adr/0158-sampling-adequacy-and-artifact-provenance.md)
**Mode:** R30, autonomous. **Base:** `bdc10ee`.

Plan Sessions **F** and **H** of review 4, paired under "Q3 pair small
ones". Author rulings taken: R4-Q4 (a), R4-Q5 (b) and (d), R4-Q9.
Thirteen register rows; four roadmap rows opened, two closed.

## Step 0

`bin/preflight` **exit 0**, all five checks disclosed: last five CI runs
on `main` green; edit root `/home/mg/src/ehr-testing-tools`, not under
`/mnt/`, `core.fileMode` **true**, `core.ignorecase` **unset**; tree
clean including untracked; HEAD `bdc10ee` == `origin/main`; last stable
tag `stable-20260819-review-4-fix-3-environment-and-result-or-loud` @
`ae396cf`; HEAD untagged, **no tag owed**.

Baseline `make test`, unpiped, wrapper ending `exit "$MAKE_EXIT"`:
**`MAKE_EXIT=0`, 358 zero-failure blocks / 4,040 tests / 18,110
assertions**, 0 failures, 0 errors. Reconciles exactly against
**ADR-0157's own Verification section** (which carries `358 / 4,040 /
18,110` as its CLOSE -- the first ADR in the 0150-0157 run to record its
own close figure, which is the practice D1-1's new rule codifies).
`clojure -M:poly check` **OK**.

Budgets at Step 0: `:corpus` 1821/2045, `:docs` 728/785, `:judge`
915/1000, `:onboarding` 1450/1530, `:sim` 1267/1405.

**A prompt premise corrected, in the safe direction.** The prompt says a
session "cannot probe penny's edit root and must not claim to". This
session RUNS in that edit root (`git reflog`: clone 2026-07-28, every
session since committing here), so the author's payment was verified
rather than taken on report -- 0 tracked `100644` files executable on
disk (the row's 360), 0 CR bytes in the three named `openai.yaml`
mirrors (the row's 3), tree clean, preflight exit 0 with both OK lines.
The row closes **author-paid AND session-verified**.

**A second prompt correction.** `4d6ff78` is named as "the commit before
the 0153 fix"; it is ADR-0153's ADDENDUM, after the fix. The pre-fix
engine is **`ceedcfd`**; the fix is `885b1c9`. All historical runs used
`ceedcfd`, where `check_test.clj` is byte-identical to HEAD.

Probes (b)-(e): 4 bare fences on README+SETUP as predicted, instrument
`bin/fence-census`; 4 of 5 SVG tripwires born RED; the `%% Arrow N`
numbering claim VERIFIED from two sides; the converter write set
enumerated at **28**.

## Commits

| sha | what |
|---|---|
| `ca02aa0` | test: red -- widened defspec, front-door fence gate, SVG tripwire |
| `3c4e346` | fix: sampling adequacy + owed rows (Session F) |
| `f1d4952` | test: red -- artifacts name their inputs (Session H) |
| `d02a085` | fix: every generated artifact points back at its inputs (Session H) |
| this | docs: ADR-0158, close |

Both red-first commits pushed with their green successors
(`rulings.md#R-red-pushed-with-green`), disclosed in each message.

## The historical red (D6-1)

Scratch worktree at `ceedcfd`, widened defspec cherry-picked in.

**First recorded failure: seed `1787179118735`, after 24 trials, 665
ms.** Shrunk counterexample: 21 patients, arrival-gap 44, ED 0/5, Renal
**1 bed / 2 surge**, Cardiology 2/1; weights Renal 2 / Cardiology 3 /
Emergency 1; **churn `nil`**.

Necessity measured against `ceedcfd`, 400 trials each:

| configuration | violations |
|---|---|
| mixed wards + hot churn, DEFAULT single-home pathway | **0** |
| mixed wards + multi-home pathways, no churn | 2 (0.5%) |
| mixed wards + multi-home pathways + `sample-profile` | 11 (2.8%) |

So the row's own remedy is necessary but NOT sufficient: **multiple home
wards** is the ingredient D6-1 does not name, and churn is amplifying
rather than required. An earlier probe in this session concluded churn
was necessary; the defspec's own first failing trial refuted it, and the
conclusion was corrected in the landed comment rather than left.

Trial count 150 -> **300** on evidence: 5-of-6 historical reds at 150,
**8-of-8 at 300**, 3.3 s against 2.0 s. HEAD: **6 of 6 green at 300**
plus 900 further trials at 150 -- 2,700 trials, no flake.

## Fence census, before and after

| | exercised | exempt | bare | command total |
|---|---|---|---|---|
| before | 26 | 0 | 50 | 76 |
| after | 28 | 3 | 46 | 77 |

Front door 4 bare -> **0** (5 exercised, 3 exempt). Population closed at
228 then 229 (the SETUP ladder split adds one fence). Remainder of
R-F8's rule -- 34 fences on the manual and use-case path -- is
`roadmap.md#reader-path-fence-battery`.

## Tripwire dispositions

4 of 5 born red; 3 reviewed to **fresh** with the diff that moved them
named (`gt-emitters` an addendum to a different section, `straddle-
timeline` a scenario rename, `verdict-ranking` a dead-link fix), and
**1 true stale asset**: `two-clocks.svg`, whose "exactly two
timestamp-bearing fields" audit ADR-0142 falsified. Rowed as
`roadmap.md#two-clocks-asset-field-audit`; the gate asserts that anchor
exists, so retiring the finding turns the suite red.

## The 28-artifact diff class

**Banner-only.** Every non-`%%` changed line in the whole regeneration
is in the converter's own source (4 deletions, all there). Predicted by
Step 0(d) and checked before committing.

## Rows

**Opened (4):** `#two-clocks-asset-field-audit`,
`#reader-path-fence-battery`, `#backtick-shorthand-and-denylist-
widening`, `#corpus-player-slices`.
**Closed (2):** `#edit-root-worktree-residue`, `#intake-staging-dir`.
(The prompt's read-back line says "opened(3)/closed(4)"; the actual
counts are 4 and 2, named above.)

**Register:** 13 rows marked, D8-2 partly (its remainder named).

## Departures, disclosed

1. The two roadmap closures moved from Step 2 to the close commit:
   `done-pointer-adr-test` requires a Done row to cite an ADR present in
   `notes/ADRs.md`, and ADR-0158 did not exist at Step 2.
2. `done-pointer-adr-test`'s non-vacuity check corrected from DISTINCT
   ADRs vs bullet count to bullets-carrying-a-pointer -- immune to two
   bullets sharing one ADR (which this session produces) and strictly
   stronger. Fix-forward with disclosure, one defensible reading.
3. `strip_fresh.clj` / `exercised_sources.clj` gained an optional
   `:fence-index`, beyond the prompt's fence list, because the front-door
   gate could not otherwise reach SETUP.md's second ```sh fence without
   re-tagging a fence's language to game first-match.

## Verification

Full `make test`, unpiped, `MAKE_EXIT=0` at each gate:

| point | blocks | tests | assertions |
|---|---|---|---|
| Step 0 baseline (`bdc10ee`) | 358 | 4,040 | 18,110 |
| Step 2 close (`3c4e346`) | 362 | 4,060 | 18,200 |
| Step 4 close (`d02a085`) | 364 | 4,070 | 18,304 |
| this close | 364 | 4,070 | 18,304 |

`clojure -M:poly check` **OK** at every gate. Oracle: the converter and
the `state-derived` renderer are off the digest path -- predicted
IDENTICAL, no root or `digest.clj` change, oracle coverage green
throughout.

**Budgets re-measured at close** (`R-register-hygiene-at-close`), all
five green, none raised:

| set | Step 0 | close | budget | headroom |
|---|---|---|---|---|
| `:corpus` | 1821 | 1832 | 2045 | 213 |
| `:docs` | 728 | 735 | 785 | 50 |
| `:judge` | 915 | 922 | 1000 | 78 |
| `:onboarding` | 1450 | 1482 | 1530 | 48 |
| `:sim` | 1267 | 1274 | 1405 | 131 |

First measurement after the `AGENTS.md` and `SKILL.md` edits left
`:onboarding` at 44 headroom and `:docs` at 46; both of this session's
own additions were then compacted rather than left tight, recovering 5
lines across 5 sets. No baseline moved.

**CI:** `3c4e346` run `32312928469` **completed success**; `d02a085` run
`32315304870` **completed success**. The close commit's own run is
recorded in ADR-0158's addendum.

**`bin/post-push-verify`** run after each push, its three checks
recorded in the ADR's own Receipts.
