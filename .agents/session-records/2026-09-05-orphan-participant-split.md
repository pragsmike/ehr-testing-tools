# 2026-09-05 — orphan-participant split by a log fact, and a Done-row cap

Roadmap row `roadmap.md#orphan-participant-shape-gap`, PRIORITY 6, now
CLOSED. Ceremony mode: R30 (commit and push at each checkpoint), taken
from the prompt. Prompt archived at
[`../prompts/2026-09-05-orphan-participant-split.md`](../prompts/2026-09-05-orphan-participant-split.md).
Reasoning-of-record: `notes/adr/0176-event-stream-mutation.md`, addendum
(c) in section 6 and a closing line in section 8.

Rulings in force: **R-cap**, **R-split**, **Q3(a)**, **Q5(a)**,
**Q6(a)**, **R-pins**.

## 0. Preflight — run LATE, and disclosed as such

`bin/preflight` was NOT run at session start. It was run at step 3, and
this is a deviation from the build-session skill's own opening, recorded
rather than papered over. What it reported then was consistent with a
clean start: last five CI runs on `main` all green, edit root
`/home/mg/src/ehr-testing-tools` (not `/mnt/`), `core.fileMode` true,
`core.ignorecase` unset, and the two findings it printed were this
session's own — an unclean tree (the step-3 doc edits) and a local HEAD
ahead of `origin/main` by this session's three commits. Session start
was `4cfa570c`, equal to `origin/main`, which the step-4 oracle bracket
uses as its baseline.

## 1. What landed

**bb4fcb3d — `docs: Done rows are pointers -- a gate, and 19 rows compacted to comply`.**
R-cap. The gate, then the compaction.

**cf82f095 — `test: orphan-participant split by log fact -- RED`.**
Two loop rows, the predicate law, and its dual.

**97d0c31e — `feat(corpus): orphan-participant narrowed by log fact; two closed-start operators (ADR-0176 addendum c)`.**
The site predicate, the two derived registrations, the register deleted.

**260e8c10 — `docs: catalog at twenty-eight; addendum (c) extended`.**
ADR-0176's addendum (c) gains its dated line; section 8's owed
disposition is closed where it was raised; the consumer-facing count
moves.

**fd5c5a26 — `test(cli): the operator catalog pins move, 36 -> 38 and 26 -> 28`.**
Six assertions in `bases/cli`, its own commit per R-pins.

Then **`docs: session record (archives prompt)`** -- this record, its
prompt archive and the row moved to Done -- and **`docs: state-derived
and the two indexes regenerated`**, the last being a pin move of its
own (session records 239 -> 240,
archived prompts 231 -> 232, roadmap Next 7 -> 6 and Done 44 -> 45).

## 2. R-cap: an instruction that had failed twice

ADR-0144's row contract has said `cap -- six lines, maximum` since it
was written. It said it in `roadmap-lint-test`'s own namespace
docstring, with **no assertion behind it** — the three siblings in that
same bullet list (token, slug, priority) each have a gate; the cap had
none. The result was measured before anything was changed: **19 of the
44 `## Done` rows were over 400 characters, the longest 6,269**, each of
them a second copy of a session record that can drift against it.

`done-rows-are-pointers-not-ledgers-test` caps a Done row at
`done-row-cap` = **480 characters**. Two things about the shape are
worth keeping:

* It is a **character** count, not the stated line count, and that is
  not a liberty. A roadmap row is written unwrapped, one physical line,
  so "six lines, maximum" was never measurable against a Done row at
  all — which is part of why it never became a gate.
* The gate does not also require a record path, though R-cap names one
  as something a compacted row keeps. Twenty-five pre-existing
  de-scaffold rows carry no path and are correct as they stand;
  requiring one would have turned compliant rows red. Path RESOLUTION is
  already gated, by `ehrt.docs-tooling.stale-path-test`, for which
  `.agents/plans/roadmap.md` is a scan root.

Witnessed RED first, naming all nineteen rows with their sizes, then
each compacted to token, slug, ADR/sha, at least one record path, and
one clause of outcome. **Two rows that cited no record at all now cite
one** (`ts-1-seventh-bed-arc` and `ts-2-outpatient-holds-a-bed`, both to
`2026-08-29-ts-defects-and-blocked-cells.md`), and **two that cited only
a brace glob** (`emission-add-ons`, `engine-fold-extensions`) now cite a
file that exists. The `engine-emit-namespace-extraction` row listed
seventeen record paths; it now brackets them, first and last.

Two mechanism-sanity cases land with the gate: that it catches an
over-long Done row and only it, and that it does not reach an open row,
whose budget stays ADR-0144's.

## 3. R-split: the fifth invariant was never ambiguous

ADR-0176 section 8 left three defensible readings and called the
disposition owed. Reading 1 — narrow again — is right, and the reason it
is right is that **the fifth invariant fires on a condition that can be
stated in advance**:

> the reattributed event is the START of a span that some END event
> CITES.

Both span-end invariants read the patient off *both* ends
(`medication-end-references-existing-order-and-follows-it-in-time`,
`care-plan-end-references-existing-start-and-follows-it-in-time`), so
renaming the start breaks the same-patient clause. Whether a log closes
its spans is a property of **the log**, not of the event's kind — which
is exactly why addendum (c)'s kind-list narrowing could not reach it.
The kind list is derived from `clinical-content-only-when-admitted`, and
that list *contains* the span starts.

So the log fact goes in the site predicate:

* `:orphan-participant` sites only on therapeutic-intent events **no end
  cites**, and keeps its four-set.
* `:orphan-closed-medication-order` and `:orphan-closed-care-plan-start`
  take the sites it gives up — one per span column, **derived** from
  `referential-columns` by the same overlap that defines the problem, so
  a third span joining the referential family mints its third operator
  with no edit here. Same edit function, same phantom id, same
  one-draw-on-the-site discipline (Q3(a)).

Q5(a) equality now holds per operator on **every** population by
construction rather than by measurement. The `declared-shape-gaps`
register is therefore empty, and it comes out with its divergence test:
an empty register with a live test behind it is a scaffold, and a
register that CAN be empty is one a later session refills.

### The RED reproduced the ADR's own measurement from the other side

The gate over the split, `no-orphan-participant-site-is-a-start-some-
end-cites-test`, derives its cited-start set by **transcribing** the two
span-end invariants rather than reading `operators.clj`'s helper — a gate
derived from the code under test agrees with that code's mistakes. Run
against the unfixed operator it named **14 sites** over dense-7500,
indices `[48 55 60 68 74 83 92 100 122 129 156 183 185 278]`.

ADR-0176 section 8's exhaustive table says 6 sites add the
medication-end invariant and 8 add the care-plan-end one. **6 + 8 = 14.**
Two independent derivations, the same set — which is the corroboration
this split rests on, and it is worth more than either alone.

## 4. Gates

| gate | result |
|---|---|
| `bin/preflight` | run late (section 0); findings were this session's own |
| `clojure -M:poly check` | OK, as part of every `make test` |
| `clojure -M:poly test brick:docs-tooling` (step 0) | RED naming 19 rows, then green |
| `clojure -M:poly test brick:corpus` (step 1, RED) | 18 failures / 6 errors, all traced |
| `clojure -M:poly test brick:corpus` (step 2, GREEN) | **6687 passes, 0 failures, 0 errors**, exit 0, wall 586 s |
| `make test` | **27609 passes, 0 failures, 0 errors**, exit 0, 414 namespace runs / 4843 tests |
| `bin/regression-oracle 4cfa570 97d0c31` | **IDENTICAL**, every root; soundness yes outside the leading docstring |
| `bin/ground-truth-bracket 4cfa570 97d0c31` | **IDENTICAL**, 38 roots, the standing 3 keyless skips |
| `gitleaks git --staged -v` | no leaks, before every commit |

The catalog-wide gate reports **47 of 84 (operator, population) pairs
sited** where it reported 45 of 78 — 28 operators now — and prints **no
shape-gap disclosure line**, because there is no shape gap.

The `make test` figure moves against the Q11(c) session's own close
figure of 27,423 at the same 414 namespace runs: **+186**, and every one
of them is the two new operators reaching a `doseq` that already walked
the catalog. No wall claim is made — this repo's CI spread is 436-1302 s
and a single local run is not a performance measurement.

The `:onboarding` reading set fell 1473 -> 1463 lines as a side effect of
R-cap, since `roadmap.md` is one of its paths: headroom 57 -> 67. The
`:docs` set, which stands at headroom 0, was not touched.

### The oracle's reach, stated rather than implied

`bin/regression-oracle` digests the output of `sim run`. **No oracle
root applies a mutation operator**, so an IDENTICAL verdict here says
that this session did not perturb generation — which is what it is for —
and says *nothing* about the split itself. What proves the split is the
loop, over every sited pair, plus the transcribed predicate law in
section 3. This is the same disclosure ADR-0176's own apply-unification
sibling made about `engine/replay`: run the instrument, then say what it
can and cannot see.

## 5. Findings, one line each

1. **A stale `/tmp/green.log` from the Q11(c) session was read as this
   session's own result.** A `&&` chain short-circuited on a failing
   patch script, so the `clojure … > /tmp/green.log` never ran — and the
   grep that followed read a file left behind by a prior session, whose
   trailing `WALL_SECONDS=414.06` matches that session's own recorded
   figure exactly. Caught because `git status` disagreed with the tree
   the "result" implied. Scratch log names are now removed before use.
2. **The CLI base pins the operator catalog by name and by count, and
   `brick:corpus` cannot see it.** Six assertions across four tests in
   `bases/cli/test/ehrt/cli/core_test.clj` moved 36 → 38 and 26 → 28.
   A brick-scoped green is not evidence about a base that reads that
   brick's registry.
3. **Placing the prompt archive before a `make test` costs a full run.**
   `state-derived-test` compares each `INDEX.md` against a fresh
   directory listing, so a file added mid-session turns it red and
   `poly` aborts the remaining project. Regenerate first, or place last.

## 6. Fences honoured

No `components/sim-check` source was touched. The other 25 operators are
unchanged: `git diff` over `operators.clj` is the orphan section, the
banner comment above it, and nothing else.

## 7. Push and CI

PENDINGPUSH
