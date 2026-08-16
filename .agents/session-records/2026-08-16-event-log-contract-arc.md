# 2026-08-16 — the event-log contract arc: census, Event schema, generated formats.md section, custom-emitter use case

Autonomous (R30). Baseline `24f351d`; ADR-0141. Five commits,
`0f647ea..3b979e9` plus this close. The ground-truth event log went from
an implicit shape a consumer reverse-engineered out of `emit_hl7.clj` to
a public, versioned, machine-readable contract.

## Step 0 — preflight, disclosed in full

`bin/preflight` (plain): last five CI runs on `main` **all green**; edit
root `/home/mg/src/ehr-testing-tools`, not under `/mnt/` — OK; tree
clean including untracked — OK; local HEAD `24f351d` matched
`origin/main`; last `stable-*` tag `stable-20260815-review-3-fixes`,
**DISCLOSED: HEAD not tagged**.

Both standing tags verified peeled AND verified present on the remote,
not merely locally:

```
b139de58…  stable-20260815-result-nodes^{}
b96c2464…  stable-20260815-review-3-fixes^{}
```

**The fence-battery micro-session's deferred close tag was NOT paid.**
The author-side CI relay was not in the prompt's context, so the
standing conditional license did not vest. Reported as pending;
proceeded, since the arc's own work did not depend on it. No tag owed
or taken by this session either; its own close tag defers to the next
Step 0 under the same standing license.

## Step 1 — the census, and a STOP for rulings

`.agents/plans/2026-08-16-event-log-census.md`, committed at `0f647ea`
before the rulings, on the fence-battery precedent (register first,
ruling after).

Two populations reconciled **exactly at 21 kinds**: every `{:event …}`
construction site in `engine.clj`, against **4,997 events across eleven
runs** with `out/` cleared first. The two demo scenarios reach only 17;
four further corpora exist because stopping there would have declared
four kinds unreachable and one live consumer read dead.

Two claims in the driving prompt were **corrected against the tree**:
`replay`'s `:before`/`:after`/`:world-*` are a derived trace record
wrapping an event, not event keys; and the universal key set is four,
not five (`:active-mrn` is absent from `:bed-swap`, `:merge`,
`:step-rejected`).

The consumer cross-check found no dead read — `check.clj`'s
`:disposition` read on `:discharge` looked dead until a purpose-built
death corpus proved it live, at 1 event in 4,997 and **0 in anything the
docs teach**.

## The rulings

Q-A **(a)** public/versioned; Q-B **(a)** malli source plus committed
EDN export; tabulator promoted to `bin/event-census` under licensed
fence widening; nested-`:event` collision described in the schema and
leading the prose, no rename; S-1..S-5 and the Z-segment asymmetry stay
register rows. Later: two-artifact gate accepted, `ObservationEntry`
export accepted, and one added sentence — the `[:re …]` dialect is
`java.util.regex`.

## Red, then green

The schema landed **one kind short** (`:care-plan-end` omitted) to prove
the gate bites, which only works because the coverage assertion compares
the fixture fleet's kinds to the declared vocabulary in BOTH directions:

```
RED:   produced but not declared in the Event schema: (:care-plan-end)
       expected: (= 21 (count declared))  actual: (not (= 21 20))
GREEN: Ran 18 tests containing 83 assertions. 0 failures, 0 errors.
```

## Five findings that came from running, not reading

1. **The EDN export was not readable EDN.** `#"…"` regex literals are a
   Clojure reader feature `clojure.edn/read-string` rejects — so the
   artifact was unreadable by exactly the non-Clojure consumer it exists
   for. Found by the parity test failing to read it.
2. **A false claim I wrote, then caught.** "byte-identical to what
   `--format ground-truth` prints" — 169,945 vs 169,944 bytes. ADR-0100
   is not wrong; I collapsed its internal `:bare-text` comparison with
   CLI stdout. Corrected precisely.
3. **`--format ground-truth --json` emits EDN, not JSON.**
4. **The R-F5 fence class, reintroduced by me and caught.** Step 4's
   taught strip redirected into an `out/` subdirectory it never created
   — the same class the battery fixed three commits earlier, in the
   first page written after that fix.
5. **The clinical fixture's lead-in delay is load-bearing.** The GMF
   walk starts at DOB; a newborn's DOB is still up to a year back, so an
   encounter at t=0 produces nothing at all. Three attempts returned
   `{:registered 2}` before this was understood.

## Gates I broke, and what that cost

`0f647ea` went **RED on CI** — `every-real-item-is-indexed-test`, because
I pushed a docs-only plan file without running the suite, assuming a
plan file could not break anything. `f51715c` then needed mode 755 on
new `bin/` files. Both fixed forward in `c6d55e2`, never amended; CI
green again from `c6d55e2` on. By Step 4 the executable-bit check ran
BEFORE committing rather than after pushing.

Three pinned-count tripwires fired across the arc and all three were
right: use-case count 21→22, exercised-sources rows 8→9, and the
executable-bit gate. Each bump carries its reason in the test's own
history, per that test's convention.

## The worst thing that happened, and how it was caught

Closing the arc, `reading-set-budget-test` went red (`:onboarding` 2724
against 2690). The right response was not to bump the number: measuring
the red showed the register block had been written as ~50 lines of
per-defect detail inside `roadmap.md`, whose own header says *"Cite
sources; one line per item"*, and which every cold session reads. So it
was compressed first, budget moved second.

**The compression itself then destroyed content.** It was done with a
Python slice between two anchors, `s[:start] + terse + s[end:]`, on a
file made of INDEPENDENT backlog rows — which deleted everything
between: the D8-5 closure row, the repo-review-4 charter (the standing
ADR-count cadence rule plus its twelve-row watch-list), and the
`sim-theory.edn` unregistered-derivation row.

Caught by reading the diffstat before committing — **209 changed lines
where ~35 were intended**. Restored from HEAD, redone as anchored
insertion, diff verified to contain exactly one deletion (a reflowed
line). Nothing was lost.

Two things worth keeping. The near-miss came from the FIX, not the
original problem — the compression was correct, the mechanism was
reckless. And every number computed while the file was damaged was
wrong: the budget was re-derived from the restored tree (2699 → 3105),
not from the 2536 measured against a file missing three rows. A
slice-between-anchors edit on a file of independent rows is a data-loss
shape, and `reading-sets.edn`'s own note now says so.

## Full-suite reconciliation, predicted before each run

| run | blocks | predicted | passes | reconciled |
|---|---|---|---|---|
| Step 2 (`c6d55e2`) | 328 / 656 | **exact** | 16,882 | residue **0**, last 10 chased in a disposable worktree |
| Step 3 (`4ba51f7`) | 332 / 664 | **exact** | 17,024 | predicted 17,028 — **wrong by 4**, explained below |

Step 2's delta: new namespaces 464 + `test-source-live-path-lint` 8 +
`io-vocabulary-lint` 2 = 474 = observed.

Step 3's miss is the instructive one. I predicted +4 from the live-path
lint by assuming Step 2's rule carried. Reading it instead: it scans
only `_test.clj` files and asserts only for namespaces outside
`ehrt.docs-tooling.`. `event_fleet.clj` is not a `_test.clj`;
`event_log_doc_test.clj` is docs-tooling and therefore allowlisted. Both
new files were invisible to it. Step 2's own +4 attribution was correct
for its four `sim-*` test files — the rule simply does not reach either
file Step 3 added.

`make docsgen` verified **idempotent** by snapshot-and-compare. The
first check compared against HEAD, which proves nothing, and was redone.

## Commits

| sha | what |
|---|---|
| `0f647ea` | the census — 21 kinds, source and corpora reconciled, four consumers cross-checked |
| `f51715c` | `bin/event-census`, promoted under licensed fence widening |
| `c6d55e2` | the `Event` schema, red-first, plus consumer conformance and the version gate |
| `4ba51f7` | `docs/formats.md` gains "The event log", generated |
| `3b979e9` | the custom-emitter use case, exercised from birth |

`bin/post-push-verify` ran after every push; all three checks recorded
each time. `gitleaks` clean on every commit.

## Fences honoured

The event log's SHAPE did not change: zero `decide`/`evolve` changes,
zero emitter production changes, vendored bytes verbatim,
`docs/notation.md` untouched. Two widenings, both disclosed and both
author-accepted: `bin/event-census` (licensed) and a one-line
`ObservationEntry` export on `sim-model`'s interface.

**No tag owed or taken.** This session's own close tag defers to the
next session's Step 0 under the standing conditional license.
