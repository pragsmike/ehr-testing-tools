# 2026-09-04 -- the prime audience: ground-truth QA teams, documented and routed

An author ruling of 2026-09-04 names one actor this workspace's PRIME
audience: teams that consume the ground-truth event log as a **semantic
oracle** for QA of their own downstream system. Documentation serves
them first; the features they need are the prominent, easy-to-discover
ones. The ruling is additive — nothing is removed or deprecated by it.
This session lands that ruling across the register, the three routing
surfaces, the use-case catalog, an exerciser, and the CLI's own help.

Base `e1be14d9`. Ceremony: commits at each checkpoint, a single push at
the end — taken from the prompt's own step sequencing (step 6 is the
full suite, step 7 is the push), not R30's per-checkpoint push. No
sub-agents.

## 0. `bin/preflight`

Ran before any git operation. **No findings, exit 0.** Last five CI runs
on `main` all green; edit root `/home/mg/src/ehr-testing-tools`, not
under `/mnt/`; `core.fileMode` true, `core.ignorecase` unset; tree clean
including untracked; local HEAD matched `origin/main` at `e1be14d9`. One
disclosure, and it is the correct state: HEAD is not tagged `stable-*`
— no tag is paid.

## 1. C1 — the register (`efbc878`)

`docs/dev/AUDIENCES.md` is the register `docs/README.md` routes off, and
it says so itself, so a ruling that does not land here lands nowhere.

- The preamble declares segment 7 prime **before** the count sentence,
  so the count sentence's own colon stays adjacent to the list it
  introduces, and states the additive fence in the same breath: no
  segment renumbered, folded, demoted or deprecated, no capability
  removed.
- The count sentence grows six → seven in the register's own grow-note
  convention — dated, attributed, and naming the record that carries
  the ruling. Per the ruling itself that record is a session record,
  not an ADR.
- Segment 7 states what the actor does, how it differs from segments 4
  and 6 in **both** directions, its entry path, and its in-tree witness.

Segment 7's own three-step entry path is the one the ruling names:
`consuming-ground-truth.md` → `formats.md`'s "Read the top-level vector
only" → `future-features.md`'s "Scale ergonomics".

**Gate.** `audience-entry-path-test`'s segment-count pin moved 6 → 7 in
the same commit, and its universal link law (every numbered segment
carries a markdown entry path) passes over the new segment.
`audience-entry-path`, `stale-path`, `link-footnote-gate`: 18 tests,
420 assertions, 0 failures.

## 2. C2 — routing (`345a1a1`)

The ruling's prominence rule applied literally, FIRST on each surface:

| Surface | What it gained | Position |
|---|---|---|
| `docs/README.md` | "I need a ground truth to check my own system against" | immediately after "I don't know what this is yet" — the first destination for a reader who already knows what this is |
| `docs/what-is-this.md` `## Audience` | one bullet | first of eight |
| `README.md` | one short paragraph, no fence | directly under the opening pitch, before "See it run" |

The `what-is-this.md` bullet is deliberately **distinct** from the
QA/test-automation bullet already there: that cohort wants reproducible
scenarios and defect-injected corpora; this one wants the log itself as
the answer key. Folding them would have lost exactly the distinction the
ruling is about.

The README paragraph carries no fence, so the front-door fence gate's
zero-bare-fence ratchet is untouched, and no register code (`ADR-NNNN`,
`EXP-xx`, `D9`) — the README register-code tripwire's own population.

**Gate.** `link-footnote-gate`, `stale-path`, `audience-entry-path`,
`front-door-fence-gate`, `invocation-lint`: 25 tests, 756 assertions,
0 failures.

## 3. C3 — the use case (`84057ee`)

`:ground-truth-as-a-test-oracle`, authored in
`components/corpus/docs/use-cases.edn` and regenerated with
`make use-cases`. FIRST in `:start-here` and FIRST in `:cases`, so it
leads the generated index too.

The table's own ordering comment is **rewritten rather than left
describing an order it no longer has**: row 1 is first by ruling, rows
2..7 keep the order they had (by how many readers each question serves),
and the emitter author's row is still last on purpose.

**The strip was run verbatim before the commit**, three commands, all
exit 0 — and the pipe's verdict and the retained asset's verdict came
back byte-identical, which is the determinism claim the whole "versioned
QA asset" framing rests on.

**Gate.** Case-count pin 22 → 23, plus a NEW named gate,
`committed-start-here-table-leads-with-the-prime-audiences-own-row-test`,
which asserts POSITION rather than membership — prominence was the whole
of the ruling, and a membership assertion would go green on an edit that
kept the row and demoted it. `usecases`, `link-footnote-gate`,
`stale-path`, `index-completeness`, `lint`: 64 tests, 728 assertions,
0 failures.

## 4. C4 — the exerciser (`cfa8465`)

`bin/usecase-ground-truth-oracle`, in `bin/usecase-custom-emitter`'s
shape: a BEGIN/END marker block the freshness check reads, per-command
`expect`/`expect_eval` assertions, and named invariants over real
captured output.

The prime audience raises what the script owes the page. That audience
retains corpora as versioned QA assets, so the load-bearing claim is not
"these commands run" but "the retained log carries the same verdict the
throwaway pipe produced". **Invariant 1 diffs the two verdicts
byte-for-byte.** The other six: both verdicts green; the catalog counted
at 45 out of `:invariants-checked` rather than out of prose; the four
config-needing invariants present by name (they are the reason the strip
passes `--config` to `sim check` at all); the retained log opening as a
bare vector whose first event is a registration; the four entity classes
the page tells a reader to derive invariants over really present; and
the run's own top-level event count taken from the checker's `:events`
rather than from a tree-walking grep, which would count the nested
`:pre-horizon-facts` the contract excludes.

**Run once against the live tree, after the commit, exit 0:**

    == usecase-ground-truth-oracle: OK (4 steps, 7 invariants) ==

It had to run after rather than before, because the script's own
tree-clean postcondition sees its own untracked file as dirty. C4's
message was amended to carry that output — an unpushed, message-only
amend, which is the only amend `rulings.md#R-amend-unpushed-message-only`
allows.

**Gate.** Registered in `exercised-sources.edn`, so ADR-0148's
population closure gates it the moment it is registered — no per-row
test case. Row pin 17 → 18. `exercised-sources`,
`exercised-sources-coverage` (`check-all` over the live register, this
row included), `citation-gate`, `strip-fresh`: 46 tests, 112 assertions,
0 failures.

## 5. C5 — CLI discovery (`3b87a9b`)

`sim run --format`'s `:doc` described `ground-truth` for one audience
only — the emitter author. The prime audience's job was invisible at the
very flag that produces the thing they consume.

The `:doc` now names the stream as the SEMANTIC layer underneath every
message, then states its two jobs as **both first-class**: as a test
oracle for a system of your own (pointing at
`docs/consuming-ground-truth.md` for the run contract and at the new
use-case page for the strip), and as the contract both built emitters
read (pointing where it always did). **The emitter half is reworded, not
reduced** — both its links survive verbatim, which is the ruling's own
no-removal fence applied to help text.

`docs/cli.md` regenerated with `make cli-doc`. The `:flags` vectors —
the flag whitelist — are untouched: this is a `:doc` string and nothing
else.

**Gate.** `make docsgen` run in full afterwards, **exit 0**, moving
exactly two files: `docs/cli.md`, and `.agents/state-derived.md`'s
`docs/use-cases/` count 22 → 23 — a derived index catching up with the
page added two commits earlier, which is what the step's gate permits.
`invocation-lint`, `link-footnote-gate`, `stale-path`, `state-derived`,
`cli-tombstone`: 33 tests, 771 assertions, 0 failures.

## 6. THE FINDING: a sixth pin nobody listed (``05e58e7``)

**Found by the full suite, not by any checkpoint gate.** The prompt
named five gates for the use-case step. `artifact-provenance-test` holds
a sixth: the converter-rendered population — every generated artifact
carrying mermaid, enumerated from the tree — pinned at 28. A 23rd
use-case page is a 23rd rendered diagram.

    FAIL in (every-converter-rendered-artifact-names-the-converter-test)
    expected: (= 28 (count population))
      actual: (not (= 28 29))
    make: *** [Makefile:49: test] Error 1

The failure was the good kind: it printed all 29 paths and told the
reader what to do with a legitimate addition. The pin belonged in C3 and
was not there; it moves in its own commit rather than by amending C3,
because an amend on an unpushed commit is message-only and a content
change is a new commit. The pin's own message is rewritten to say where
the 29th came from, so the next reader is not left reconciling against
ADR-0158's 28.

## 7. What is now FIRST, and where

| List | First entry now | Was |
|---|---|---|
| `docs/README.md`'s destinations | ground truth as an oracle | task-first practitioner (still second) |
| `docs/what-is-this.md` `## Audience` | ground-truth QA teams | integration engineers (still second) |
| `README.md`'s opening | "Who this serves first" paragraph | (new; "See it run" unchanged below it) |
| `use-cases.md` "Start here" | "I need a semantic ground truth…" | "I need realistic FHIR test data…" (still second) |
| `docs/use-cases.md` catalog | Ground truth as a test oracle | Generate conforming synthetic data (still second) |

And what is deliberately **not** first: `docs/dev/AUDIENCES.md`'s
numbered list, where the prime audience is segment **7**. R-segment
required exactly that — declared prime in the preamble, numbered last —
because renumbering is what the additive fence forbids. The preamble is
what carries the prominence there.

## 8. Judgment calls, and their ratification status

**(a) A forward reference removed rather than left dangling.**
Segment 7's entry path first cited the use-case page that step 3 creates.
That link would have been dead for two commits, so it was dropped from
C1 and the pointer added in C3's own commit, in `docs/README.md`, once
the page existed. Every commit stands green on its own. **Unratified.**

**(b) "FIRST in every list" scoped to lists a reader chooses from.**
`make integration`'s recipe gained the exerciser in place, beside its
siblings, rather than at the head. A make recipe is an execution order,
not a chooser's list; reordering it would change what runs when, for no
reader's benefit. **Unratified.**

**(c) The equations.** `[EngineExecute]` for the sim producing a log —
the catalog's established name, spelled identically by four sibling
cases, though not itself a `pipeline.edn` label — and `[Check]` for the
judge over it, which IS one. The shared stage name could read as a claim
that `ehrt sim check` and `ehrt corpus check` are one implementation, so
the page says in bold that it is not. No new catalytic resource was
introduced, which is what keeps `docs-tooling`'s `lint.clj` — src,
outside this session's fence — untouched. **Unratified.**

**(d) The register row's witness citation.** `{:adr "ADR-0146" :date
"2026-09-04"}` — the standing law the row satisfies
(`R-exercised-implies-gated`), not an ADR of its own, because the ruling
writes none. This is the dense-7500 row's own precedent, four rows
below, and the row's comment says so and names the session record.
**Unratified.**

**(e) `--patients 8` over the `ed-tuesday` scenario** for "small". The
scenario config is what makes appointments and bed-status changes appear
at all — a bare `ehrt sim run` produces the thinnest stream this
simulator has — so a strip on the defaults would have taught the prime
audience to derive invariants over entities their log would not contain.
**Unratified.**

**(f) One stale sentence refreshed outside the payload.**
`audience-entry-path-test`'s out-of-scope note described
`what-is-this.md`'s bullet list as "seven, zero of them carrying a
link". C2 made it eight, one of them linking. Refreshed in the same
commit rather than left stale; the note's argument — that list is not
the routing register, so it stays ungated — is unchanged. **Unratified.**

## 9. Fences, and that they held

| Fence | Held? |
|---|---|
| No src beyond `help.clj`'s `:doc` | Yes — one `:doc` string; `:flags` untouched; `lint.clj`, `usecases.clj`, `strip_fresh.clj` all untouched |
| No renumbering | Yes — segments 1..6 are byte-identical; the prime audience is segment 7 |
| No removal or deprecation language | Yes — the emitter-author half of the `--format` doc keeps both links verbatim; no page, flag or section was deleted |
| No reading-set path gains a line without measured headroom | Yes — `:docs` measured at **785 of 785 lines, HEADROOM 0**, and none of its five paths was touched. `AGENTS.md`, `docs/dev/architecture.md`, `docs/dev/README.md`, `docs-tooling/interface.clj` and `build-session/SKILL.md` are all unedited |

## 10. Gate: full `make test`

**MAKE_EXIT=0.** 4,817 tests, 25,797 assertions, 0 failures, 0 errors,
over both projects (`conformance`, `ehrt-cli`). The run went to a log
with `$?` captured explicitly and the wrapper ended in `exit
"$MAKE_EXIT"` — ADR-0152's own mask, not repeated here.

Against the 2026-09-04 dense-7500 (b) close's 4,815 / 25,700: **+2 tests,
+97 assertions.** The +2 is the one new deftest,
`committed-start-here-table-leads-with-the-prime-audiences-own-row-test`
— `docs-tooling`'s tests run under BOTH projects, so a single new
deftest reads as two. The +97 is that deftest's own four assertions plus
per-case and per-file loop growth: a 23rd use-case case in the catalog's
conservation loops, a 23rd page in the link, dead-link, footnote and
converter-population scans, and a 7th "Start here" row. No test file was
added, which is why the growth is loop-shaped rather than block-shaped.

**Run twice.** The figures above are the run at `05e58e7`. It is run
again with this record, the prompt archive and the regenerated indexes
in the tree, because tree-scanning gates live in bricks none of those
belong to (`rulings.md#R-full-suite-before-push`); that second run's
result, and the CI conclusion at the pushed tip, are recorded in the
close-marker commit.

## 11. Close: the second suite run, the push, and CI

**Second `make test`**, with this record, the prompt archive and the
three regenerated indexes in the tree: **MAKE_EXIT=0**, the identical
**4,817 tests, 25,797 assertions, 0 failures, 0 errors**. The added
documents cost no assertion, which is the expected shape for a change
that adds no test file.

**Pushed** `e1be14d9..fc4dca33` to `origin/main`. `bin/post-push-verify`,
all three checks: remote tip matches HEAD; every commit message in the
range is pure ASCII; CI reported once, in progress at report time
(AR-CI-4).

**CI at the pushed tip, verified by this session:** run
[`33892045227`](https://github.com/pragsmike/ehr-testing-tools/actions/runs/33892045227),
`headSha` `fc4dca33`, **status `completed`, conclusion `success`**. No
tag is paid (`rulings.md#R-tag-law`, retired) — the green run at the tip
is the close marker.

## 12. HEAD landed

    05e58e7 docs: the converter-rendered population pin, 28 -> 29
    3b87a9b docs: sim run --format names the ground-truth contract
    cfa8465 docs: the prime audience's use case is exercised
    84057ee docs: use case -- ground truth as a test oracle
    345a1a1 docs: the prime audience routed first
    efbc878 docs: audience register -- segment 7, the prime audience
