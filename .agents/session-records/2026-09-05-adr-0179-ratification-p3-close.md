# 2026-09-05 — ADR-0179 ratified transitive, and the person-simulator row closed

Docs-only session, two steps and a close. Ceremony mode: **R30** (commit
and push at each checkpoint), taken from the prompt. Prompt archived at
[`../prompts/2026-09-05-adr-0179-ratification-p3-close.md`](../prompts/2026-09-05-adr-0179-ratification-p3-close.md).
Reasoning-of-record: `notes/adr/0179-merge-transfer-semantics.md`, which
gains two dated addenda and nothing else.

Rulings in force: **R-inv-ratified**, **R-merge-bed-cycle-open**,
**R-edit** (standing), **R-cap** (standing). `.agents/rulings.md` is
FROZEN and was not opened.

## 0. Preflight

`bin/preflight` ran FIRST, before any git operation, and exited **0 with
no findings**: the last five CI runs on `main` all green; edit root
`/home/mg/src/ehr-testing-tools`, not under `/mnt/`; `core.fileMode`
true; `core.ignorecase` unset; working tree clean including untracked;
local HEAD `6e3271de` equal to `origin/main`. One DISCLOSED line, not a
finding: HEAD is not tagged `stable-*` (last such tag is
`stable-20260821-patient-simulator-charter`). No tag was paid —
`rulings.md#R-tag-law` is retired, and CI green at the tip is the close
marker.

## 1. Asks to disposition

| ask | disposition |
|---|---|
| Step 1: dated addendum, `R-inv-ratified` verbatim, under the transitive section | **DONE** — `2eab4482` |
| Step 1: `R-merge-bed-cycle-open` verbatim, under "the open-items section", heading corrected from the file | **DONE, WITH THE CORRECTION THE PROMPT ASKED FOR** — there is no open-items section; filed under `### Not changed`, section 2 below |
| Step 1: state that a downstream answer to the A40/census question is the revisit trigger | **DONE** — in the addendum's own second sentence |
| Step 1 invariant: `git diff --stat` touches only that file | **HELD** — 1 file, 25 insertions, **0 deletions**; nothing above either insertion point changed |
| Step 1 gate: `make state-derived` diff clean, then `make test` | **BOTH GREEN** — section 5 |
| Step 2: the A-G check, before deleting anything | **ALL SEVEN RULED, zero open** — section 3 |
| Step 2: delete the OPEN `[person-simulator]` PRIORITY 3 row | **DONE** — `c0e4d4cb`, 23 lines |
| Step 2 invariant: no other row moves | **HELD** — priorities 1, 4, 9, 10, 11, unique and ascending |
| Step 2 gate: `make test` (row-cap and index gates) | **GREEN** — section 5 |
| Step 3: record, prompt archive, close ceremony, push, CI, close marker | this record |

## 2. The heading the prompt asked to be corrected

The prompt named "the open-items section (channel expectation: the
section that lists R-loc and the bed-cycle item -- correct the heading
from the file)". **ADR-0179 has no open-items section**, and its two
OPEN items are not co-located:

- **R-loc** is raised under `### Decision`, as the fourth ruling, marked
  "Recorded here as **OPEN pending downstream reply**".
- **The bed's housekeeping** is the last bullet of `### Not changed`,
  whose own first line reads "OPEN, beside R-loc".

That bullet is therefore the only place in the file that names both, and
it is where the second addendum is filed. The correction is stated in
the addendum itself as well as here, so a reader who arrives at the ADR
without this record still learns why the addendum sits in a section
titled "Not changed".

## 3. The A-G check, stated either way

The prompt's stop condition was: *if any of the row's "seven rulings A-G
open" (ADR-0172) remain unruled in the tree, STOP.* They do not.

`notes/adr/0172-person-simulator-charter.md:4` carries **"RULED
2026-08-25: A1 B1 C1 D1 E1 F1 G1"**, and its section 5 quotes each one
verbatim at the option it selected — A1 (a newborn is a full person),
B1 (the head of household draws once), C1 (the GMF death is
authoritative), D1 (`:identification {:merge-fraction 0.35}`), E1
(authored-provisional rates with in-source markers), F1 (the component
lands alone), G1 (person-side disposition, engine-side minting).

**Zero letters open.** The row was deleted rather than left standing,
and no skip to step 3 was taken. Recorded here in the affirmative
because the prompt asked for the result either way, not only on the STOP
branch.

What actually closed the row, beyond the charter: ADR-0173 (arc 3a, the
engine's fold across four parts), ADR-0174 (arc 3b, encounter-horizon
scheduling and bed status), and the 2026-09-02 F1 addendum at
`components/person-simulator/src/ehrt/person_simulator/interface.clj:45`
— "arc 3's fold has landed -- ehrt.sim.run requires this interface and
calls initial-persona and persons". That addendum is what spends ruling
F1's "the component lands ALONE" clause, which was the row's own last
live sentence.

**No `## Done` line was added.** Closed work is the ADR record, as
`roadmap.md`'s own `## Done` section states, and R-cap makes rows
pointers. The one thing worth checking before deleting a slug was
whether anything still cites it: the only
`roadmap.md#person-simulator` anywhere in the tree is in the dated
`2026-08-25-arc-2a-person-simulator-charter.md` session record, which
`roadmap-lint`'s scan roots exclude by design (dated one-shot files
narrate history at authoring time). No live pointer rots.

## 4. Why the P3 commit is two files

`.agents/state-derived.md` is GENERATED and it **counts roadmap rows**
and line-counts `roadmap.md` for the reading-set table, so deleting a
23-line row moves it. Held back, it would fail CI's own freshness diff.
Its deltas are exactly that arithmetic and nothing else:

| | before | after |
|---|---:|---:|
| roadmap rows (all sections) | 79 | 78 |
| `## Next` rows | 6 | 5 |
| `:onboarding` reading set, lines | 1,464 | 1,441 |
| `:onboarding` headroom | 66 | 89 |

Headroom GREW, so `rulings.md#R-budget-stop` was never in play. Step 1's
own `make state-derived` produced **no diff at all**, which is the right
reading: `state-derived` counts ADR **files** under `notes/adr/`, and an
addendum adds none.

## 5. Gates

Every run through a wrapper that ends in `exit "$MAKE_EXIT"`, logged
whole, never piped.

| checkpoint | gate | result |
|---|---|---|
| 1 | `make state-derived`, then `git status` | exit 0, **only the ADR modified** |
| 1 | `make test` | **MAKE_EXIT=0**, 4,855 tests, 27,667 assertions, 0 failures, 0 errors, 25m44s |
| 2 | `make state-derived`, then `git status` | exit 0, roadmap + state-derived only |
| 2 | `make test` | **MAKE_EXIT=0**, 4,855 tests, 27,667 assertions, 0 failures, 0 errors, 21m44s |

Both runs are **identical in count** to the figures the ADR-0179 close
recorded at `3e1d8346` (its own session record carries them). That is
the expected reading for a docs-only diff, and it is stated as a
reconciliation rather than as evidence of anything: no test was added,
so no count could move.

**No regression-oracle claim and no bracket run.** Neither was owed —
this session changes zero payload bytes — and saying so explicitly is
cheaper than a reader wondering why an ADR-0179 amendment carries no
digest.

## 6. Judgment calls, disclosed

1. **Two anchored insertions rather than one appended block.** "A dated
   addendum under the transitive section ... and under the open-items
   section" was read as *placement*, not as one block at the end
   mentioning two sections. The competing reading is defensible, but it
   loses what placement buys in this repo: a reader who arrives at the
   transitive section is told it is ratified, there, rather than having
   to reach the end of the file. "Nothing above the addendum changes"
   holds under either reading and holds here — 25 insertions, 0
   deletions.
2. **Em dashes in the new prose, `--` inside the quoted rulings.** The
   file's own body uses `—` throughout; the author's rulings as issued
   use `--`. Quotes are verbatim, surrounding prose matches the file.
   The first anchor attempt failed on exactly this and was caught by the
   script's own count assertion rather than by silently patching the
   wrong place.
3. **The R-edit script asserts before it writes.** Each anchor must
   occur exactly once or the script exits non-zero having written
   nothing. Anchor 2 did fail that way (an `--` where the file has `—`),
   and the tree was verifiably untouched afterwards.
4. **A bogus ASCII check, caught and redone.** `grep -n '[^\x00-\x7F]'`
   through the WSL wrapper is not a byte-range class in BRE — it matched
   nearly every line and reported "clean" from an inverted reading of
   the exit code. Redone as `LC_ALL=C grep -nP`, which gave exit 1 (no
   match) on both messages. The `commit-msg` hook would have caught a
   real violation regardless; the point is that the check as first
   written could not have.

## 7. Fences — what this session deliberately did not do

- **No engine change, and no `:bed-status-change` minted from a merge.**
  R-merge-bed-cycle-open holds the item OPEN; it does not license the
  decide-layer change that would close it.
- **R-loc is not resolved.** It stays OPEN under `### Decision`, waiting
  on the same downstream reply.
- **`.agents/rulings.md` was not opened.** FROZEN since 2026-08-25; the
  ADR is the record, which is why two rulings landed as addenda and not
  as rows.
- **No `## Done` row for `person-simulator`,** per section 3.
- **Nothing else in ADR-0179 was touched** — not its Status line, not
  its count pins, not the `statuses-entitled-to-a-location` paragraph.

## 8. Background processes

Two, both `make test` runs (`bxrk0xhol`, `b1i6zdyas`), both reported
complete by the harness with exit 0. Nothing this session started is
still running.
