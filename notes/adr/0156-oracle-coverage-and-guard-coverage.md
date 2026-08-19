## ADR-0156 — review-4 fix 2/5: the oracle says what it cannot see, and three laws get the gates they never had

**Status:** Accepted (author-directed, autonomous session per R30),
2026-08-19.

### Context

Fix 2 of 5 in the repo-review-4 arc, the pair **E + C**, under the
author's standing ruling of 2026-08-18 — *"Q1 accept all
recommendations. Q2 that order ok. Q3 pair small ones."* — which makes
every R4-Q the plan's recommended option. Three ruling items that are
skill or register text ride along because they touch C's own surfaces:
**R4-Q1** (`--amend`), **R4-Q7** (`make ci-parity` as the D3 probe),
**R4-Q8** (the zero-population rubric sentence).

The two halves share the arc's cross-dimension pattern from a different
angle than fix 1 did. Fix 1 closed gates whose POPULATION was narrower
than the class they were read as enforcing. This session closes gates
whose CLAIM is wider than what they measure:

- **E** — `bin/regression-oracle` returns `IDENTICAL`, and that verdict
  is read as "nothing changed". It means "nothing changed *on the paths
  35 fixed-seed roots can reach*", and nothing anywhere said what those
  are. Separately, its own soundness check — the thing that keeps the
  comparison honest — could not see a `:require` change, while
  `rulings.md#R-oracle-script-contract` claimed it "aborts on an
  undeclared digest-source diff".
- **C** — three standing laws with no surface between them and the
  sessions they bind: an audience law with no gate at all, two process
  laws cited in no skill, and one act (`--amend`) with no law either
  way.

### Step 0 — the measurements everything below rests on

`bin/preflight` (the fail-closed one ADR-0155 landed): **exit 0, no
findings.** Five green CI runs on main, tree clean, HEAD == origin/main
at `1e20c63`, last tag `stable-20260819-review-4-fix-1-closure-and-harness`
@`660b7bf`, HEAD not tagged — disclosed, no tag owed.

Baseline `make test`, unpiped, `MAKE_EXIT` captured, wrapper ending
`exit "$MAKE_EXIT"`: **exit 0, 352 blocks / 3,990 tests / 17,876
assertions** — reconciles with ADR-0155 exactly. `poly check` OK.
Budgets: onboarding 1410/1530, corpus 1807/2045, sim 1253/1405, judge
901/1000, docs 714/785 — all under, none at its baseline.

**(a) The carried gap, closed.** ADR-0155 did not run the oracle and
argued its fence structurally. `bin/regression-oracle 7d998f0 HEAD`:
`IDENTICAL: every root's digest matches`, 35 rows, `declared-digest-change: no`,
**exit 0**. Predicted, and asserted rather than argued.

**(b) A fresh 35-root pre-digest at HEAD.** Exit 0, 35 `.edn`,
**114 seconds wall** — the number that put the fresh-digest gate in the
scheduled lane. 32 roots are engine pairs (`{:ground-truth :hl7}`), 3
are interpreter batches.

*A trap worth recording, because it is the one the event contract warns
about in its own schema.* A first pass grepped `:event ` across the EDN
and found 17 kinds. That is wrong: `:pre-horizon-facts` carry their own
`:event` key drawn from a DIFFERENT six-value vocabulary, and the
interpreter batches are a third vocabulary again. `event_schema`'s
`PreHorizonFact` docstring names this exactly — *"a consumer that walks
the EDN tree looking for `:event`, rather than iterating only the
top-level vector, will therefore find these and mistake them for log
events… the single most likely way a proprietary emitter gets this log
wrong."* Re-derived structurally, per engine root's top-level
ground-truth vector:

| | count |
|---|---|
| closed event kinds in the contract | 21 |
| **witnessed by some root** | **13** |
| vacuous | 8 |

The vacuous 8: `:bed-swap`, `:cancel-admit`, `:cancel-discharge`,
`:cancel-transfer`, `:merge`, `:order-placed`, `:result-available`,
`:step-rejected` — reconciling with register row L1-2's "8 event kinds"
exactly, by a different method.

Emitter families, from MSH-9: **5 witnessed** — `ADT^A04` (2,289),
`ORU^R01` (1,768), `ADT^A01` (441), `ADT^A03` (441), `ADT^A02` (**1**).
`ORM^O01`: zero, corroborating L1-7 and ADR-0142.

Depth, which is the part that matters: `:transfer` is **one
occurrence, one root** (`death-fixture`) and so is `ADT^A02`.
`:medication-end` is one root deep too (`injuries`), which the register
had not noticed. L1-1's rung tallies reproduce: 48 / 381 / 13 / 0.

*Cross-check on the derivation itself:* the pre-digest's own
`sha256sum` manifest is **byte-identical** to the baseline manifest
`bin/regression-oracle` produced at HEAD in (a), and again to the target
manifest in the self-bracket below. The coverage numbers and the bracket
are looking at the same bytes.

**One correction to L1-2, re-derived rather than carried forward.** Its
summary lists `oru-message` among the never-invoked and describes "the
whole order→result path" as vacuous. `ORU^R01` **is** emitted, 1,768
times across 14 roots — by `observation-message` (`emit_hl7.clj:805`)
and `diagnostic-report-message` (`:843`). It is `oru-message` (`:709`),
the `:result-available` order-result emitter, that no root reaches. The
sub-agent's per-function count was right; the summary that generalized
it to a message family was not. The COVERAGE block states the precise
version.

**(c) `digest_body_of` on the live file.** 593-line file → **524-line
body**, 0 of 4 requires surviving, the whole `(ns …)` form outside, the
`roots` map inside. Reproduces L1-4 exactly — and then does not add up:
the first `^(defn` is at line 110, so the body should be 484 lines.

It is. The awk was `awk 'found{print} /^\(defn/{found=1; print}'`, and
**both rules match every `(defn` line**, so each of the 41 is printed —
40 of them twice. 484 + 40 = 524. Harmless to the diff (both sides
duplicate alike) and wrong in every line count ever taken from it,
including L1-4's own. Found by arithmetic that refused to close, which
is the only reason it was found at all.

**(d) The widening, measured before it was written.** Whole file minus
the leading docstring: **493 lines** (593 − the 100 docstring lines),
requires included, each line once. `digest.clj` is unchanged across
`7d998f0..HEAD`, so that bracket reads IDENTICAL under either awk —
confirmed.

*Whole-file-minus-docstring, not whole-file.* The plan offered both.
The docstring is dated historical narrative; taxing every added note
with `--declared-digest-change` would push the narrative out of the
file, which is a worse outcome than the drift it prevents. Everything
that must not drift silently goes **below** it instead — which is why
the COVERAGE block is where it is, and why a test asserts it is there.

**(e) The fourth exit-rendering shape, reproduced.** With `gh`
succeeding but no run indexed for the tip, jq's `.[0]` is null and
`[null,null,null] | join("\x1f")` is **two bare separators**, not the
empty string. Both guards missed it and check 3 printed
`status= conclusion=<pending>` with every field empty — which skims as a
pending run. One line fixes it, so it was fixed here rather than rowed.

**(f) Segment 5 — STOP-AND-REPORT, and what it found.** The prompt
expected one linkless segment. There are **two**: segment 5 ("The
Clojure library consumer, deferred stub", 0 links) and **segment 1**
("Guide readers, arriving method-first", 0 links). Segments 2/3/4/6
carry 1/1/3/6.

That changed the question. The exemption reading — a declared
`deferred stub` marker the gate skips — would have left segment 1 red
anyway, while adding a marker vocabulary the rule's own text does not
carry. **Author ruling, 2026-08-19: (i), the universal law.** Two
one-token edits, no carve-out.

Also asked, also answered from the tree: `docs/what-is-this.md`'s
bulleted `## Audience` is **out of scope**, and the prompt's suggested
"same assertion, separate population" would have gone red on all seven
of its bullets — none carries a link. The law's own text is scoped to
`docs/dev/AUDIENCES.md`, and the two lists do different jobs: one is the
routing register `docs/README.md` keys off, the other describes who the
software is for. Gating the public list is a new rule for whoever rules
it.

### Decision — E

**The coverage claim lives inside the compared region.** `digest.clj`
gains a COVERAGE block naming the vacuous set, the structural cause (all
35 roots pass `:pathway {:name "module-only" :steps []}`; 11 of 18
components plus `bases/cli` are off the oracle classpath), the
one-root-deep capacity witness, and two committed sets —
`witnessed-event-kinds` (13) and `witnessed-message-types` (5). It sits
beside `roots`, below the docstring, **inside** what the soundness check
diffs. That placement is the mechanism, not decoration: widening or
narrowing coverage now forces `--declared-digest-change`, and the
session that pays it has to say which way coverage moved.

**ADR-0150 (a) generalized.** It said Z-segments are outside the oracle.
True, and a quarter of the surface. The sole emitter call is the
five-arg arity, so `site-profile` is nil at **all four** bind points —
MSH dialect, `:patient-class` table, `:discharge-disposition` table,
Z-segments. `effective-msh` and `code-for` are invoked, on their
nil-profile branch only. The oracle witnesses the **absent-profile
identity** and nothing else; any site-profile milestone must nominate a
different witness up front.

**The docstring states its own population.** `Six roots, matching this
session's own J1 ruling verbatim` → a current-state paragraph, 35 roots
in two families, gated against `(count roots)`. The superseded opening
is quoted as the history of what it replaced, not deleted
(`R-dated-addendum-not-silent-edit`); the gate strips backtick-quoted
spans before checking, so a bare restatement stays red and history stays
legible.

**The soundness check sees the ns form** (R4-Q6 (iii) (c)), and
`R-oracle-script-contract`'s text is made true of the script rather than
of the intention: *"equivalence-checks the whole digest source minus its
leading docstring, and aborts on an undeclared diff; a
`:require`/`:import` change IS one (widened ADR-0156)"*. ADR-0044 stays
the citation, per the row contract's own last-`-- ADR-NNNN` rule.

**R4-Q6 (ii) (b) is rowed, not taken.** `roadmap.md#oracle-coverage-roots`,
PRIORITY 3, with its price attached: each new root is a declared oracle
change *and* a permanent per-session cost on every bracket, and today's
35 cost 114 seconds a side.

### Decision — C, and the three ride-alongs

- **D2-1.** `ehrt.docs-tooling.audience-entry-path-test`: every numbered
  segment carries ≥1 markdown link, population enumerated from the file
  (not from its header's count, which has drifted before), asserted
  non-empty and asserted to be six. Segment 5 links the go-public gate
  section, named as its entry path until there is a released artifact;
  segment 1 links the guide it names and `docs/README.md`.
- **D2-2 / R4-Q1.** `ehrt.docs-tooling.process-law-citation-test`: nine
  process laws, each asserted to be a real row AND cited in both copies
  of `build-session/SKILL.md`. Nine, not the plan's six — the plan
  counted only the rows added in review 4's window, and the skill
  already cited `R-tag-law`, `R-anchored-register-edits` and
  `R-oracle-script-contract`. Counted from the skill, not from the
  plan's memory of it. `R-session-verifies-ci-via-gh` is cited at step
  11, where the tag licence's CI condition is actually decided;
  `R-stop-only-on-two-defensible-readings` at step 12;
  `R-amend-unpushed-message-only` at step 4. HISTORY.md carries the
  reasoning for both.
- **D2-4.** Re-read after ADR-0155's closure gate, which gates the
  diff *list*, not the *content*. Four paths still have no local content
  gate — the three `palgebra/examples/*.mermaid` and
  `event-examples.edn` — and the workflow comment now names them as the
  four this step is the sole gate for, with the list-vs-content
  distinction stated. Closing the three `.mermaid` locally is left as
  its own change; D2-4's disposition asked for the paragraph.
- **R4-Q7.** `repo-review/SKILL.md` D3 names `make ci-parity`
  (`Makefile:324`) as the standing probe, with its one limit stated: no
  `HOME` repoint, so `~/.m2` is shared — cold-cache parity, not a cold
  machine.
- **R4-Q8.** Step 3 of the procedure gains *"a probe reporting zero
  first asserts its population is non-empty and records the size beside
  the result"*, generalizing `R-empty-population-is-red` from tests to
  audit probes.

### The oracle on itself

The point of the widening is that this session's own change is now
visible to it. Both directions run and recorded:

| invocation | soundness | verdict | exit |
|---|---|---|---|
| `bin/regression-oracle 1e20c63 HEAD` | `DIFFERS outside the leading docstring -- STOP` | none, aborted | **1** |
| `… --declared-digest-change` | `DIFFERS … asserted, proceeding` | **`IDENTICAL: every root's digest matches`**, 35 rows | **0** |

The gate refuses an undeclared digest-source change and passes a
declared one — and under the declaration, **all 35 digests are
identical**, confirming no root, emitter or digest-logic path moved.
Predicted before running; asserted after. The target manifest is
byte-identical to Step 0 (b)'s pre-digest.

### Deviations and disclosures

**The gate's home moved, and the prompt's home would never have run.**
Step 1 named `components/oracle/test/…`. The oracle brick belongs to no
testable project — `poly info` shows `---` under conformance, ehrt-cli
and integration, `s--` under dev — and poly's own `help test` says brick
tests run from every project *"except for the development project"*. A
test there would never have run in `make test` or in CI: a gate that
cannot fail, which is the exact class this arc exists to close. One
defensible reading, so fix-forward with disclosure
(`R-stop-only-on-two-defensible-readings`): the per-push half is in
`docs-tooling`, the 114-second fresh-digest half in
`projects/integration`. No `deps.edn` and no project composition
changed. The integration half shells the digest out through the same
synthetic classpath `run_one` builds — the mechanism under audit, which
is the form this repo's own rubric asks evidence to take.

**Test extractors refined between red and green.** Two: `^:private`
defs, and allowing the superseded `Six roots` opening to be quoted as
history while a bare restatement stays red. Both were still red at the
red commit `079fe80` — the defs did not exist and the docstring stated
the claim unquoted.

**The roadmap row was compacted, not grown.** `#repo-review-4` was at
its six-line cap; adding fix 2's line made it seven and
`roadmap-lint-test` said so. Compacted to six rather than the cap being
touched.

**`.agents/state-derived.md` regenerated once, at the close.** Four new
test namespaces move its generated counts (189 → 193 test namespaces, 46
→ 49 docs-tooling gates), which took `make test` red mid-session exactly
as ADR-0143's contract says it should. Regenerated at the end rather
than three times along the way, and disclosed in the commit that omitted
it.

**Pushes batched, for a gate reason.** `rulings-lint-test`'s
every-cited-ADR-resolves check goes red on a row citing ADR-0156 until
this file exists, so the six commits are pushed together after the close
rather than per step. `R-red-pushed-with-green` holds — no red-first
commit is pushed alone — and `R-full-suite-before-push` holds: the full
suite runs on the final tree, before the push.

**Not done, deliberately:** no new oracle root (rowed and priced); no
local gate for the three `.mermaid`; no change to any digest, root,
emitter or engine path; `docs/what-is-this.md` untouched.

### Consequences

`IDENTICAL` now has a written scope, kept honest by the same mechanism
that keeps the digests honest — put the claim inside the compared
region and drift costs a declaration. The oracle's blind spots are
enumerated rather than rediscovered each time someone reasons about them
from memory, which is what produced ADR-0153's wrong reason and, before
it, the reason that ADR corrected.

Three laws that bound sessions from a register they were not required to
read now sit on the surface a session is routed through, with a test
that says so. And the review's own instrument is amended twice: a probe
that reports zero must show its population first, and the cold-clone
probe that two reviews recorded as lost is named where it cannot go
missing a third time.
