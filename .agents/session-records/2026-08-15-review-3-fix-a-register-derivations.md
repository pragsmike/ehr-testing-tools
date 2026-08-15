# 2026-08-15 -- Review-3 fix session A: register every string-diagram derivation (D5-3/D5-4/D2-4), with ruled riders

Reasoning of record: [`notes/adr/0136-register-every-derivation.md`](../../notes/adr/0136-register-every-derivation.md).
Driving prompt archived verbatim at
[`.agents/prompts/2026-08-15-review-3-fix-a-register-derivations.md`](../prompts/2026-08-15-review-3-fix-a-register-derivations.md).

The first step-6 fix session under repo review 3's OPEN arc, on the
author's ruling *"accept all."* Three commits: the co-landed gate+fix,
the ruled riders, and this close. No tag owed or paid -- the review arc
tags at its own step-7 close. Post-push verification receipts land
separately, per this repo's own pattern (`00bdad7`).

## Step 0 -- Preflight

`bin/preflight` plain, all five sections reported:

- Last five CI runs on `main`: green, green, green, green, green
  (`fca52ec5`, `bc6f46cc`, `dbbeb1f8`, `b139de58`, `00bdad77`).
- Edit root `/home/mg/src/ehr-testing-tools`, not under `/mnt/`.
- Working tree clean, untracked files included.
- Local HEAD `fca52ec50160086991869820b8e75b2c5d1cb1d2` == `origin/main`.
- Last `stable-*` tag `stable-20260815-result-nodes`
  (`b139de589083c6b4967c1a4769b2c6a8d17feac4`); **DISCLOSED: HEAD not
  tagged `stable-*`**.

That disclosure is correct and expected, not a finding: the review arc
is open and tags at step 7, so this session owes no tag.

`bin/preflight` has no `--expect-tag` flag, so tag substance was
verified directly instead:

```
$ git rev-parse stable-20260815-result-nodes^{}
b139de589083c6b4967c1a4769b2c6a8d17feac4
```

-- the expected commit.

## Step 1 -- The gate first, red witnessed

Two `make` targets added on the `docsgen` pattern, both folded into
`docsgen` so the population is one target:

- **`sim-theory`** -- converter on
  `components/sim/docs/sim-theory-equations.txt` ->
  `sim-theory-diagram.mermaid`, then an `awk` splice of that file into
  `sim-theory-diagram.md`'s embedded ` ```mermaid ` block.

  The prompt allowed a check-only comparison if splicing proved
  awkward. It did not, and splicing is the better of the two options
  it offered: it makes one `git diff` the entire enforcement across
  all ten paths rather than adding a second failure mechanism inside
  `make`, and it *retires* the paste-it-back-in-by-hand step instead
  of merely gating it -- which is the point of the session.

- **`palgebra-examples`** -- the three
  `components/palgebra/examples/*-flow*.mermaid` from their sibling
  `*-equations.txt`. Its comment records why the population is three
  and not five: `lemon-pie` and `decision-monad` ship as equation
  sources only, with no committed `.mermaid`.

CI's freshness step now diffs ten paths, one per line, with a header
comment recording why the population changed and the obligation a new
derived file carries.

**Red, before any regeneration landed** -- CI's step run verbatim
against the tree at `fca52ec`:

```
=== git diff --exit-code (the ten registered paths) ===
 components/palgebra/examples/ai-study-flow-v3.mermaid       | 13 ++++++++++---
 components/palgebra/examples/committee-flow.mermaid         | 12 +++++++++++-
 .../palgebra/examples/deliberated-choice-flow.mermaid       | 12 +++++++++++-
 3 files changed, 32 insertions(+), 5 deletions(-)
DIFF_EXIT=1
=== failing path count ===
3
```

**Exactly three, and exactly the three named** -- the STOP condition
(`red count != 3`) did not fire. `sim-theory` correctly did not fail.
Verified separately that the new `sim-theory` target is a byte-exact
no-op against the fresh tree, so its absence from the red set is the
target working, not the target missing:

```
$ make sim-theory && git status --porcelain
 M Makefile
```

Independent pre-session cross-check: the design channel had regenerated
all three examples in a separate environment and found 0 committed
`_out` nodes vs 3/6/6 regenerated. This session's own red reproduces
that from the opposite direction.

## Step 2 -- Green

`make docsgen` regenerated the three; the freshness check against the
staged fix exits 0. The delta is ADR-0135's result-node feature: the
`%% --- Result types (terminal outputs) ---` declarations, the
`Op -- "name" --> name_out` wires, the green `style ..._out
fill:#e8f5e9` block.

**One finding beyond the register's account**, surfaced by regenerating
rather than by reading. The register (D5-4) characterized all three
deltas as "exactly ADR-0135's result-node feature." That holds for
`committee-flow` and `deliberated-choice-flow`. `ai-study-flow-v3` is
**two** converter generations behind: besides the result nodes it was
missing the gate/spider styling that *predates* ADR-0135 --
`SecurityTriageToShortList` was rendering as an ordinary dark box and
now renders purple, and the operations-section header gained its
"spiders use distinct shapes" clause. Recorded in the commit message,
the ADR, and the register row itself, because it sharpens the finding
rather than softening it: the unregistered population had been drifting
longer than the headline said.

### The line-count constraint -- preserved, no fallback needed

Both headers now point at `make sim-theory`. The
`sim-theory-equations.txt` edit is the constrained one: the converter
numbers its `%% Arrow N` comments from that file's own line numbering,
so any line added or removed silently renumbers every arrow (ADR-0135
diagnosed exactly this, off by one).

The header was rewritten **in place at exactly 17 comment lines in, 17
out**. The prompt's fallback -- absorb the renumbering, disclose the
churn -- was **not needed**. Verified rather than assumed:

```
equations line count: 46 (must be 46)
=== does the .mermaid change? ===
 components/sim/docs/sim-theory-diagram.md | 19 +++++++++++++------
 1 file changed, 13 insertions(+), 6 deletions(-)
=== arrow numbers before/after ===
13
.mermaid UNCHANGED -- zero Arrow-N renumbering
```

The only changed file is the `.md`, and only its header. The header now
also *states* that its line count is load-bearing, and the `sim-theory`
target carries the same caution -- the next editor is told rather than
left to rediscover it.

ADR-0135's historical disclosure note is kept **verbatim**, dead
converter path and all. Because it says "the recipe above" and the
recipe is now gone, an ADR-0136 note was placed immediately before it
naming what stood there -- the referent is preserved without editing
the record.

Committed `49f78e4`, gate and fix co-landed, seven files.

## Step 3 -- Ruled riders (`0027a6e`)

**R-1 -- `bin/check-palgebra-drift` deleted.** Zero-caller inventory
re-derived at deletion, not inherited from register row D1-5:

| surface | hits |
|---|---|
| `Makefile` | 1 -- and it is the comment listing the script among pre-carve targets that "stay superseded", not an invocation |
| `.github/workflows/test.yml` | 0 |
| `.github/workflows/integration.yml` | 0 |
| all of `bin/`, excluding the script itself | 0 |
| all of `.agents/skills/` | 0 |

Every other tracked hit is prose (this audit, the three review
registers, ADR-0002/0004/0127, archived prompts). It could not have
fired regardless: it diffs against a sibling `../ehr-testing-sim`
checkout, absent here, and that repo was consolidated **into** this
workspace at `a0534d0` -- premise gone, clean-skip by construction.

One live dependency was checked **before** deleting:
`bases/cli/test/ehrt/cli/executable_bits_test.clj` names the script.
Reading it showed the reference is docstring-only (cited as the
historical first instance of the index-mode-loss bug class) and that
the test enumerates tracked files dynamically via `git ls-files -s` --
so the deletion shrinks its population and needs no edit there. Zero
`src`/test changes, as fenced.

Disposition row added to `notes/carve-loss-audit.md` under a new "Later
dispositions" section; the accepted-warts `bin/` row updated to match.

**R-2 + D7-4 -- three roadmap rows, visibility first, disposition
later:**

- **Deferred** -- Synthea demographics extraction, unregistered in
  that directory's `NOTICE` since 2026-08-05, revisit trigger stated
  verbatim ("a session with a Synthea checkout available"). A pointer
  paragraph was added at the `NOTICE` itself. The prompt made that
  conditional on the file not being inside a vendored-verbatim fence;
  it is not, and the check was substantive, not assumed: the whole
  purpose of that notice is recording that the three tables are
  hand-curated **originals, not** copied from Synthea, so there are no
  upstream bytes there to disturb. Separately confirmed it carries no
  `| Filename | Upstream URL | Commit SHA | SHA-256 | Retrieved |`
  table, by reading `notice_verbatim_test.clj`'s own eligibility rule
  -- that test skips non-matching table shapes and my addition is
  plain prose with no pipes.
- **Next** -- `source-sink-design.md` OPEN-4 (`--engine`), carrying its
  own question as the row's question, deliberately unanswered. Next
  rather than Deferred because Deferred rows owe a revisit trigger and
  this one has none yet; the row says so.
- **Deferred** -- the loopback flake, 18 days in `state.md` alone,
  with the durable-anchor diagnosis and a stated closing bar so the
  soak can end rather than accumulate.

### A ceremony slip, disclosed

The rider commit initially landed with **only** the script deletion.
`git add` was called with four pathspecs including
`bin/check-palgebra-drift`, which `git rm` had already staged and
removed from the working tree; `git add` aborts on the whole invocation
when any pathspec matches no file, so the other three files were never
staged and the commit went through with one. Caught immediately by
reading the `git commit` output against the `git diff --cached --stat`
printed a line earlier, which disagreed. Fixed by staging the three and
`git commit --amend` with the same message file; nothing had been
pushed. Recorded because "the stat output and the commit output
disagree" is a cheap check that caught this and would have caught it
just as well if it had been noticed later.

## Step 4 -- Verification

Full `make test` from the final tree, **unpiped, exit code captured
explicitly** (the pipe-masking incident class ADR-0135 caught):

```
MAKE_EXIT=0
0-failures-0-errors occurrences: 636
FAIL in / ERROR in lines: 0
total passes: 16315
log lines: 2472
```

**636 reconciles exactly with the review-3 baseline**, by the same
metric that record names (`0 failures, 0 errors` occurrences), and
16,315 passes matches it too. This session added no test namespace, so
zero delta was the expectation and zero delta is what the tree gave --
nothing to explain or absorb.

Run **twice**, deliberately. The first run started before
`bin/close-scaffold` created this record and its prompt archive, so it
could not have exercised the two gates those files answer to
(`index-completeness-test`, `prompt-record-pairing-test`) against their
final contents. The second run, from the tree as pushed, is the one the
figures above are quoted from; the first produced identical figures
(`MAKE_EXIT=0`, 636, 16,315, zero `FAIL in`/`ERROR in`, 2,472 log
lines each). A full suite that ran before the close artifacts existed
is not a full suite over what gets pushed -- the same
population-vs-registry confusion this whole session is about, in
miniature.

The loopback flake did not recur here either, which is one more green
run toward the closing bar the new roadmap row states.

Final-tree spot checks beyond the suite:

- `make docsgen` idempotent; ten-path freshness diff exits 0.
- `sim-theory-equations.txt` 46 lines; `sim-theory-diagram.mermaid`
  byte-identical to its pre-session self; 13 `%% Arrow N` comments,
  unrenumbered.
- `sim-theory-diagram.md`'s embedded block byte-identical to
  `sim-theory-diagram.mermaid`.
- `gitleaks git --staged -v` clean before each commit.

## Fences

Touched only: `Makefile`, `.github/workflows/test.yml`, the five
derived artifacts, the two `sim-theory` headers,
`bin/check-palgebra-drift` (deleted), `notes/carve-loss-audit.md`,
`.agents/plans/roadmap.md`, the demographics `NOTICE`, the register's
disposition cells, and the close artifacts. **Zero `src/`. Zero
converter changes.** No STOP condition fired and no fence pressure
arose -- at no point did any step look like it needed the converter
touched.

## Register updated in place

D5-3, D5-4, D2-4 -> **FIXED (ADR-0136)**. D1-5, D7-3, D7-4 ->
**REGISTERED / FIXED (ADR-0136)**, each citing this session's ADR and
what actually landed. The register is the review arc's working document
and stays truthful as fixes land; the arc itself remains OPEN, with
plan Sessions B, C and D still to run.
