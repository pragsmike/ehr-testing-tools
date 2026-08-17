## ADR-0136 — Register every string-diagram derivation: the make graph and CI freshness gate get the tree's population, not their own

**Status:** Accepted — 2026-08-15

### Context

Repo review 3's Step-0 rubric amendment (`dbbeb1f`, the
population-closure law) widened two probes from enumerating a registry
to enumerating the tree. Both immediately surfaced defects that had
been sitting outside the old population for weeks. D5's is the reason
this session exists.

Tree-first enumeration found **10** derived artifacts. The make graph
registered **5** — `docs/dev/pipeline.md`, `docs/use-cases.md`,
`docs/use-cases/*.md`, `docs/operators.md`, `docs/cli.md` — which are
exactly the five CI's generated-doc freshness step diffed. The other
five carried generation banners or came out of the same converter and
were named by no target and no diff step:

- `components/sim/docs/sim-theory-diagram.md` (and its embedded block)
- `components/sim/docs/sim-theory-diagram.mermaid`
- `components/palgebra/examples/ai-study-flow-v3.mermaid`
- `components/palgebra/examples/committee-flow.mermaid`
- `components/palgebra/examples/deliberated-choice-flow.mermaid`

Three of those five were **stale against their own converter**. They
are the string-diagram skill's own teaching material, and what they
demonstrated to any reader who opened them was precisely the defect
ADR-0135 had been chartered to fix one session earlier: every codomain
dead-ending at the operation box with nothing wired out. ADR-0135
changed the converter (`8c9f291`), regenerated every artifact the make
graph knew about, regenerated `sim-theory` under a mid-session
license — and could not see these, because nothing in the repo knew
they were derived.

The finding is the gate's, not the artifacts'. A freshness gate whose
population is the make graph cannot see a derivation that was never
added to the make graph. `sim-theory` had been maintained for its whole
life by a hand recipe pasted in its own two file headers, which is how
it stayed fresh (a careful human ran it) and also why nothing
mechanical would have noticed if that human had not.

The design channel confirmed the headline independently before this
session by regenerating all three examples itself: `ai-study-flow-v3` 0
committed `_out` nodes vs 3 regenerated, `committee-flow` 0 vs 6,
`deliberated-choice-flow` 0 vs 6.

### Author rulings, verbatim

- **"accept all."** (2026-08-15) — binding the channel's
  recommendations as put: **R-1** delete `bin/check-palgebra-drift`
  with the load-bearing zero-caller inventory recorded at deletion;
  **R-2** register BOTH unregistered standing requests as roadmap rows
  now — visibility first, disposition later; **R-3** D5's RED stands as
  scored (severity tracks the mechanism, not this instance set's blast
  radius).

Session batching and gate design follow the review's own plan
(`.agents/plans/2026-08-15-repo-review-3-plan.md`, Session A), which
that ruling adopted.

### Step 0 — Preflight

`bin/preflight` plain, all five checks reported. Last five CI runs on
`main` green. Edit root `/home/mg/src/ehr-testing-tools`, not under
`/mnt/`. Tree clean including untracked. HEAD matched `origin/main` at
`fca52ec`. HEAD not tagged `stable-*`, disclosed — correctly, since the
review arc is open and tags at its step-7 close; **no tag is owed by
this session**.

Tag substance verified directly rather than through a flag
`bin/preflight` does not have: `git rev-parse
stable-20260815-result-nodes^{}` = `b139de589083c6b4967c1a4769b2c6a8d17feac4`,
the expected commit.

### Step 1 — The gate, witnessed red

Two new `make` targets, both folded into `docsgen` so the population is
one target:

- **`sim-theory`** runs the converter on
  `components/sim/docs/sim-theory-equations.txt` to produce
  `sim-theory-diagram.mermaid`, then **splices** that file into
  `sim-theory-diagram.md`'s embedded ` ```mermaid ` block with a
  four-rule `awk` filter. Splicing rather than check-only comparison
  was chosen deliberately: the invariant the prompt named is byte
  agreement across three surfaces (equations → `.mermaid` → embedded
  block), and splicing makes one `git diff` the whole enforcement,
  uniform with the other five artifacts, instead of adding a second
  failure mechanism inside `make` itself. It also retires the
  paste-it-back-in-by-hand step rather than merely gating it.
- **`palgebra-examples`** regenerates the three
  `components/palgebra/examples/*-flow*.mermaid` from their sibling
  `*-equations.txt`.

CI's generated-doc freshness step now diffs all ten paths, and its
header comment records why the population changed and what obligation
comes with a new derived file (a make target AND a diff-list entry,
same commit).

**Red, witnessed before any regeneration landed**, running CI's step
verbatim against the tree at `fca52ec`:

```
 components/palgebra/examples/ai-study-flow-v3.mermaid       | 13 ++++++++++---
 components/palgebra/examples/committee-flow.mermaid         | 12 +++++++++++-
 .../palgebra/examples/deliberated-choice-flow.mermaid       | 12 +++++++++++-
 3 files changed, 32 insertions(+), 5 deletions(-)
DIFF_EXIT=1
=== failing path count ===
3
```

**Exactly three**, and exactly the three the register named. `sim-theory`
did not fail — it was fresh, as ADR-0135 left it, and the new target is
a byte-exact no-op against it (verified separately: `make sim-theory`
alone on the clean tree left `git status` showing only the `Makefile`).
Exactly-three was the stated proof that the gate had the right
population; more or fewer would have been a STOP.

### Step 2 — Green

`make docsgen` regenerated the three. The delta is ADR-0135's
result-node feature: the `%% --- Result types (terminal outputs) ---`
declarations, the `Op -- "name" --> name_out` wires, and the green
`style ..._out fill:#e8f5e9` block. Re-running the freshness check
against the staged fix exits 0.

**One detail beyond the register's account, found by regenerating
rather than by reading the register.** The register characterized all
three deltas as "exactly ADR-0135's result-node feature."
`committee-flow` and `deliberated-choice-flow` are exactly that.
`ai-study-flow-v3` is **two** converter generations behind, not one: it
was additionally missing the gate/spider styling and its legend
(`SecurityTriageToShortList` renders purple now, and the
`%% --- Operations (boxes) ---` header gained its "spiders use distinct
shapes" clause). Recorded because it sharpens the finding rather than
softening it — the unregistered population had been drifting longer
than the headline suggested, which is exactly what an unregistered
population does.

### Step 2b — The hand recipes retired, line count preserved

Both headers now point at `make sim-theory` instead of carrying a
runnable converter incantation.

The `sim-theory-equations.txt` edit was the constrained one. The
converter numbers its `%% Arrow N` comments from **that file's own line
numbering**, so any header edit changing the line count silently
renumbers every arrow in the output — ADR-0135 diagnosed exactly this
(off by one, from a since-added header line). The header was therefore
rewritten **in place at exactly 17 comment lines in, 17 out**, total
file length preserved at 46. The fallback the prompt authorized
(absorb the renumbering, disclose the churn) was **not needed**.
Verified, not assumed:

```
equations line count: 46 (must be 46)
.mermaid UNCHANGED -- zero Arrow-N renumbering
```

The header now also states that its line count is load-bearing, so the
next editor is told rather than left to rediscover it. The Makefile
target carries the same caution.

ADR-0135's historical disclosure note in `sim-theory-diagram.md` is
**kept verbatim**, dead converter path and all — it is a record, not a
recipe. Because that note refers to "the recipe above" and the recipe
is now gone, an ADR-0136 note was added immediately before it stating
what stood there and that the note below refers to it, preserving the
referent without editing the record.

Gate and fix co-landed in one commit (`49f78e4`), red witnessed in this
document and the session record.

### Step 3 — Ruled riders (`0027a6e`)

**R-1 — `bin/check-palgebra-drift` deleted.** Its own header called it
a "Nightly drift check." The zero-caller inventory was **re-derived at
deletion**, not inherited from the register: `Makefile` 1 hit, which is
the comment listing it among pre-carve targets that "stay superseded",
not an invocation; `.github/workflows/test.yml` 0;
`.github/workflows/integration.yml` 0; all of `bin/` excluding the
script itself 0; all of `.agents/skills/` 0. Every other tracked hit is
prose. It could not have fired in any case: it diffs this repo's
palgebra files against copies vendored into a sibling
`../ehr-testing-sim` checkout, and that repo was consolidated **into**
this workspace at `a0534d0`, so the premise is gone and it clean-skips
by construction. Disposition row added to `notes/carve-loss-audit.md`
under a new "Later dispositions" section, with the accepted-warts
`bin/` row updated to match.

One live dependency was checked before deleting rather than after:
`bases/cli/test/ehrt/cli/executable_bits_test.clj` names the script.
Reading it showed the reference is in its docstring only (the script is
cited as the historical first instance of the index-mode-loss bug
class); the test itself enumerates tracked files dynamically via `git
ls-files -s`, so the deletion shrinks its population and needs no edit
there. **Zero `src/` changes**, as fenced.

The irony is worth stating plainly: review 3's D5-4 found three stale
`.mermaid` outputs sitting inside the very directory this script
nominally watched, and it was never going to see them — it pairs the
`.txt` sources and the `.py`, never the outputs, and ran nowhere
anyway. A guard that cannot fire is worse than no guard, because its
presence reads as coverage. That coverage is real now, one commit
earlier.

**R-2 + D7-4 — three roadmap rows, visibility first:**

- **Deferred** — the Synthea demographics extraction, standing
  unregistered in
  `components/sim-model/resources/sim-model/demographics/NOTICE:26`
  since `3f43a46` (2026-08-05), revisit trigger stated verbatim: *a
  session with a Synthea checkout available*. A pointer paragraph was
  added at the NOTICE itself. That file is safe to edit: it is this
  repo's own hand-authored provenance prose whose entire purpose is
  recording that the three tables are hand-curated originals and **not**
  vendored from Synthea, so no verbatim upstream bytes exist there to
  disturb; and it carries no
  `| Filename | Upstream URL | Commit SHA | SHA-256 | Retrieved |`
  table for `notice_verbatim_test.clj` to match against (checked by
  reading that test's eligibility rule, not assumed).
- **Next** — `docs/dev/source-sink-design.md:56` OPEN-4 (`--engine`),
  open in its own table since `499cad4` (2026-07-29) and in no register
  for the 17 days since. The row carries OPEN-4's own question as its
  question; disposition deliberately not taken, per the ruling. Next
  rather than Deferred because Deferred rows owe a revisit trigger and
  this one has none yet.
- **Deferred** — the `mutate-stdout-stdin-loopback-test` flake, carried
  in `.agents/state.md:668` alone for 18 days. `state.md` is
  regenerated at every arc close and was never a durable anchor — which
  was review 2's own finding, recurring here for the third time. The
  row states a closing bar (no recurrence by the next repo review →
  close it and D3-2 together against the accumulated green runs) so the
  soak can actually end rather than accumulate forever.

### Fences honored

Touched only: `Makefile`, `.github/workflows/test.yml`, the five
derived artifacts, the two `sim-theory` headers, `bin/check-palgebra-drift`
(deleted), `notes/carve-loss-audit.md`, `.agents/plans/roadmap.md`, the
demographics `NOTICE`, the register's disposition cells, and the close
artifacts. **Zero `src/`. Zero converter changes** — the converter was
correct; only its outputs were stale. No fence pressure arose and no
STOP condition fired.

### Verification at the final tree

- `make docsgen` idempotent; the ten-path freshness diff exits 0.
- `sim-theory-equations.txt` 46 lines; `sim-theory-diagram.mermaid`
  byte-identical to its pre-session self; 13 `%% Arrow N` comments,
  unrenumbered.
- Embedded block in `sim-theory-diagram.md` byte-identical to
  `sim-theory-diagram.mermaid`.
- Full `make test`: see the session record for the unpiped log,
  captured `MAKE_EXIT`, and the block-count reconciliation against the
  636 baseline.
- `bin/post-push-verify` run, and the full pushed range additionally
  verified by hand — that script's own range-derivation defect is
  register row D1-6 and Session C's subject, so its result alone is not
  yet trustworthy for a multi-commit push (same treatment ADR-0135's
  session gave it).

### Lesson

ADR-0135's lesson was that a converter change must reach every artifact
the converter produces. This one is the layer under it: **you cannot
sweep what nothing enumerates.** ADR-0135's manual sweep was careful and
still missed three files, because care scales with what the sweeper can
see, and the repo offered no way to see these. Two sessions in a row,
the same class — a registry standing in for a population — produced the
defect. The rubric amendment that caught it (`dbbeb1f`) generalizes:
enumerate from the tree, then diff against the registry, and treat the
gap as the finding.

The corollary is the practical one, and it is why the two headers now
point at a target instead of carrying a recipe: **a derivation
maintained by a documented hand procedure is an unregistered
derivation.** It stays correct exactly as long as a human keeps running
it, and it fails silently the first time one does not.

### Index summary (moved verbatim from notes/ADRs.md by ADR-0143, 2026-08-16)

Register every string-diagram derivation: the make graph and CI freshness gate get the tree's population — the first fix session under repo review 3's open arc (`.agents/plans/2026-08-15-repo-review-3-plan.md` Session A), on the author's ruling *"accept all."* Tree-first enumeration found 10 derived artifacts against 5 registered; the 5 unregistered included the three `components/palgebra/examples/*-flow*.mermaid` teaching diagrams, **stale against their own converter** and therefore demonstrating to any reader precisely the defect ADR-0135 was chartered to fix — channel-confirmed by independent regeneration before the session began, and the headline finding of the review. New `make` targets `sim-theory` (converter + an `awk` splice into `sim-theory-diagram.md`'s embedded block, so equations → `.mermaid` → embedded block agree byte for byte under one `git diff`) and `palgebra-examples`, both folded into `docsgen`; CI's freshness step now diffs all ten paths and its header records the obligation that comes with a new derived file. Red witnessed first, running CI's step verbatim against `fca52ec`: **exactly three** failures, exactly the three named, `sim-theory` correctly not among them — the stated proof the gate had the right population. Regeneration surfaced one detail beyond the register's account, recorded because it sharpens the finding: `ai-study-flow-v3.mermaid` was **two** converter generations behind, not one (also missing the gate/spider styling). Hand recipes retired from both `sim-theory` headers for `make sim-theory` pointers, with the `sim-theory-equations.txt` edit rewritten in place at exactly 17 comment lines in / 17 out (46-line file preserved) so no `%% Arrow N` renumbering occurred — the authorized disclosed-churn fallback was not needed — and the header now states that its line count is load-bearing; ADR-0135's historical disclosure note kept verbatim, dead path and all, with its "the recipe above" referent preserved by a new note rather than by editing the record. Riders, both ruled: `bin/check-palgebra-drift` deleted with its zero-caller inventory re-derived at deletion and a `notes/carve-loss-audit.md` disposition row (a guard whose sibling-checkout premise died at the merge, which nominally watched the very directory whose outputs went stale); three roadmap rows registered visibility-first — the Synthea demographics extraction (Deferred, trigger verbatim, pointer paragraph added at the `NOTICE` after confirming it holds no verbatim upstream bytes and no provenance table), `source-sink-design.md`'s OPEN-4 `--engine` (Next, carrying its own question, disposition deliberately not taken), and the loopback flake (Deferred, with a stated closing bar after 18 days in `state.md` alone). Zero `src/`, zero converter changes — the converter was correct, only its outputs were stale. Closes review-3 rows D5-3/D5-4/D2-4 (D5's RED) and registers D1-5/D7-3/D7-4

### Roadmap history (moved verbatim from roadmap.md by ADR-0144, 2026-08-17)

The `.agents/plans/roadmap.md` row this ADR owns, as it stood at `deb9a33` before the ADR-0144 row contract capped rows at six lines. The live row now states what remains and cites this ADR for the rest; this is the rest, verbatim.

- **`corpus generate --engine` — an OPEN question, registered for
  visibility, disposition deliberately NOT taken here.** Registered
  2026-08-15 by repo review 3 (finding D7-3(b), author ruling R-2
  "accept all.", ADR-0136); the question itself has been marked
  **Open** in `docs/dev/source-sink-design.md:56`'s own table since
  `499cad4`, 2026-07-29, and appeared in no register for the 17 days
  between. The row carries that table's own question rather than
  answering it: *should `corpus generate` grow an `--engine` flag now
  that the generator registry (SS-2) names more than one engine kind
  (`synthea`, `sim`), so a caller could pick which engine `corpus
  generate` drives instead of only ever driving Synthea?* SS-2's own
  ruling 6 put `corpus generate`'s verb, flags and defaults out of
  scope, leaving two live futures: a session adds `--engine`, or
  `corpus generate` stays Synthea-only forever with `intake
  GENERATOR-URL` as the one multi-engine door. It sits in Next, not
  Deferred, because Deferred rows owe a revisit trigger and this one
  has none yet — being in a register at all is the whole point of the
  row. Resolving it updates BOTH this row and OPEN-4 in place.
- **Synthea-extracted demographics tables — hand-curated placeholders
  today, replaceable wholesale when a Synthea checkout is at hand.**
  Registered 2026-08-15 by repo review 3 (finding D7-3(a), author
  ruling R-2 "accept all.", ADR-0136); the request itself has stood
  unregistered in
  `components/sim-model/resources/sim-model/demographics/NOTICE:26`
  since `3f43a46`, 2026-08-05, with zero hits for `demographics` in
  either this file or `state.md` across that whole window — invisible
  to the carried-item aging probe by construction, because that probe
  enumerates the registers. The NOTICE's own words, verbatim: *"A
  future session WITH a Synthea checkout available can replace the
  content of these three files wholesale with a real extraction,
  keeping ehrt.sim.persona's readers unchanged, since the schema is
  already shaped to match."* **Revisit trigger, verbatim: a session
  with a Synthea checkout available.** Until then `given-names.edn`,
  `surnames.edn` and `places.edn` are hand-curated originals, not
  copied or derived from any Synthea file, and the NOTICE is the
  standing record of that distinction — nothing here is a correctness
  defect, only an unregistered intention that is now registered.
- **`ehrt.conformance.mutate-stdout-stdin-loopback-test`'s own flake**
  (first recorded `dc52a25`, 2026-07-28). Registered here 2026-08-15
  by repo review 3 (finding D7-4, ADR-0136) after 18 days carried in
  `.agents/state.md:668` alone — and `state.md` is regenerated at
  every arc close, so it was never a durable anchor. That is review
  2's own D7-7/D7-8 finding recurring a third time; this row is the
  anchor those fixes did not reach. Evidence keeps strengthening and
  never gets acted on: zero recurrence in review 3's own full-suite
  baseline (636/636, zero `FAIL in`/`ERROR in`), and none in this
  session's suite either. **Revisit trigger: the next session that
  owns test-suite hygiene, or any recurrence.** Closing bar, stated
  so the soak can actually end rather than accumulating forever: if
  no recurrence appears by the next repo review, close this row and
  D3-2 together against the accumulated green runs, citing them.
