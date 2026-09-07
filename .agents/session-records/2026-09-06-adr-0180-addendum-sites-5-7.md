# 2026-09-06 — ADR-0180 addendum: sites 5, 6 and 7 chartered

Prompt: [`../prompts/2026-09-06-adr-0180-addendum-sites-5-7.md`](../prompts/2026-09-06-adr-0180-addendum-sites-5-7.md).

**A CHARTER SESSION. No engine code changed, no site enacted.** Its whole
output is a dated addendum to `notes/adr/0180-indexes-ride-the-fold.md`
and two roadmap lines. What it adds beyond the rulings it was handed is
four measurements: the hash-order argument and its hole, both run rather
than reasoned; a second hole the ruling does not name; and the actual
`engine/replay` invocation count per `check-all`, which is not the
fourteen the charter priced site 7 on.

Author rulings enacted as written: R-order-2, R-hash-order,
R-already-merged, R-check-once, over R-fold-carrier / R-equivalence /
R-raw-read / R-no-second-path / R-membership-from-post-state.

## 0. Preflight

`bin/preflight` exit 0: last five CI runs on `main` green, edit root
`/home/mg/src/ehr-testing-tools` (not under `/mnt/`), `core.fileMode`
true, `core.ignorecase` unset, working tree clean including untracked,
local HEAD `13bbad53` equal to `origin/main`. One DISCLOSED line, the
standing one: HEAD is not tagged `stable-*` (the tag law is retired —
AGENTS.md's de-scaffold ruling — and no tag was paid here either).

## 1. What landed

**`88a6d6e8` — `docs: ADR-0180 addendum -- sites 5-7 chartered`.**
408 lines appended to ADR-0180 as `### Addendum, 2026-09-06 — sites 5,
6 and 7`, plus a pointer paragraph in the record's `**Status:**` block.
Sites 5, 6 and 7 in the record's own "The four sites" format — scan,
index, draw/allocation status, equivalence argument, obligation — the
`:eligible-index` concern as ONE sub-map with two views, R-hash-order's
argument and hole, R-already-merged's three-step proof, site 7's design
and its one real divergence, and a `Sequencing (R-order-2)` section.
Gate: `make state-derived` and `make adr-index` both regenerate to no
diff.

**`ac081b7c` — `docs: roadmap -- sites 5-7 pointer; determinism
hash-order hazard row`.** The P1 row gains a thirteen-line pointer naming all three
sites and both of the profile's unrowed `decide` methods; a new
`**[determinism-hash-order-dependence]**` row lands at PRIORITY 2.
Gate: `make test`.

**`docs: session record (archives prompt)`.** This file, the prompt
archive, and the two generated `INDEX.md`s. A close-marker commit
follows it once CI is verified.

## 2. The measurements this charter makes

All four are probes against the live tree at `13bbad53`, run through
`clojure -M:dev` on scratch scripts; none of them changed a tracked file.

**(a) The hash-order argument, verified rather than asserted.** Over
2,000 minted-shape patient-ids, a `PersistentHashMap` built by
inserting them forward and one built by inserting them in reverse
produce the IDENTICAL key seq; and a 697-key sub-map built by inserting
a `shuffle` of its own members reproduces the parent map's filtered key
order exactly. That is R-hash-order's argument as a measurement.

**(b) The collision hole, and it is REAL at this program's scale.**
`streams/patient-id-for` over real seeds, counting distinct 32-bit
`hasheq` values:

| seed | patient-ids | colliding |
|---|---|---|
| 424242 | 2,500 / 7,500 / 22,500 | **0** |
| 424242 | 45,000 | **1** |
| 424242 | 67,500 | **1** |
| 2 | 45,000 | **1** |
| 1, 7, 99, 123456, 987654321 | 45,000 | 0 |

The pair at seed 424242 is `PID-032071-30c64e95` / `PID-038357-4bf55dc9`
(`hasheq` 1559044961), and reversing their insertion order in a sub-map
demonstrably reverses them relative to `:patients`. **Every committed
cell of the measured decade is collision-free**, which is exactly why
the bracket and the oracle cannot see this.

**(c) A SECOND hole, of a different mechanism.** `{}` and `(hash-map)`
are `PersistentArrayMap` and iterate in INSERTION order below nine
entries. Five minted ids give array-map order `[0 1 2 3 4]` against
hash-map order `[2 1 4 0 3]`. A sub-map grown from `{}` therefore
disagrees with a `:patients` that has been a hash map since t=0, for its
first eight members — the first merge and the first bed-swap of every
run, and every hand-built scripted test world.

**(d) `engine/replay` invocations per `check-all`, counted live** by
rebinding the var around one `check-all` on `sim-engine`'s own `run` at
60 patients with the active churn profile:

| log | events | invocations | events replayed |
|---|---|---|---|
| churn, no `:bed-cycle` | 278 | **17** | 4,726 |
| churn with `:bed-cycle` | 171 | **20** | 3,420 |

## 3. Judgment calls

**(a) The addendum is APPENDED; no section above it was edited.** That
is the record's own convention, stated in its `Dated corrections`
section ("The sections above are left as they stood, and this section is
where they are read against"), and it collides with something real here:
R-order-2 numbers the check-all work **7** where the charter's
`Sequencing (R-order)` section numbers it **5**. The addendum states the
renumbering and says which is current rather than rewriting the earlier
section. One defensible reading, so not a stop.

**(b) The `**Status:**` block gained a pointer paragraph, and the
generated index did not move.** `docsgen/parse-adr` reads the status
line's leading STATUS WORD only — everything up to the first `,` `(` `.`
`;` `:` or dash — so "Accepted" is unchanged. Verified by regenerating:
`make adr-index` produced no diff.

**(c) The two redundant-scan findings got the P1 POINTER LINE, not a row
of their own.** The prompt licensed either. They are not findings beside
sites 5 and 6; they ARE sites 5 and 6 — the `:patients` scans in both
methods and `already-merged?`'s log scan — so a separate row would have
been a second register of one fact, which `R-cap` and
`rulings.md#R-register-hygiene-at-close` both push against. The P1 line
names both explicitly and says so.

**(d) The determinism row is PRIORITY 2, ahead of the two gate gaps.**
Roadmap priorities must ascend in file order, and the row is a
precondition of sites 5 and 6 rather than independent backlog: a session
that does not read it will replace the order it is required to preserve.

**(e) The addendum records an OPTION that would close R-hash-order's
hole, explicitly NOT as a licence.** A carrier keyed on each patient's
POSITION in `:patients`' own fixed seq order preserves hash order (so it
is not the sorted-order oracle change the ruling defers) and has no
collision node to diverge at. It rests on `:patients` gaining no key
mid-run, which is true of `run` and is an assumption rather than a
guarantee. Written down because the fact belongs to the author, and
marked as needing a ruling because the session that meets it will
otherwise take it as licensed.

**(f) `:onboarding` headroom is now 8**, 34 -> 8 against an unchanged
1,530-line budget, all of it spent by the roadmap. Within budget, ratchet
untouched, and the next session should know the number is **8** before it
plans a roadmap edit.

## 4. Findings

**THE RULING'S OWN HOLE IS REACHED, and only outside the instrumented
range.** R-hash-order calls full 32-bit collisions "plausible"; at seed
424242 there is exactly one at 45,000 patient-ids and one at 67,500, and
none at any of 2,500 / 7,500 / 22,500. So the hole is real AND the
bracket, the oracle and both bracketed cells are structurally blind to
it — the same shape as ADR-0179's R-queue blindness, named before the
session rather than after it. The law test at every replay entry is the
only detector, which is what makes R-fold-carrier's law point 2 load-
bearing here rather than ceremonial.

**A SECOND HOLE THE RULING DOES NOT NAME, and the session will meet it
first.** Measurement (c) above. It is not a rare event: it fires on the
first merge or bed-swap of every run and in every hand-built test world.
The consequence is a one-line requirement stated in the addendum —
the carrier must be a `PersistentHashMap` from empty
(`clojure.lang.PersistentHashMap/EMPTY`), not a map literal — and it is
the site-1 `fold-events` lesson in a new place: a structure that looks
like the one it mirrors is not the one it mirrors.

**FOURTEEN CALL SITES ARE NOT FOURTEEN INVOCATIONS.** 17 without a bed
cycle, 20 with one, measured. `check.clj:399` sits inside the private
helper `fold-records`, which five invariants call; `check.clj:731` sits
inside `bed-fold`, reached from `bed-transitions` (three rows) and from
`log-derived-bed-fold`. Site 7 is therefore underpriced by a further
20%-40% on top of the two payoffs the profile already added.

**`check.clj:929`'s `or` IS REACHED, contrary to how the charter reads.**
ADR-0180's site-5 sentence says "`check.clj:929` already accepts
pre-folded entries", which a reader takes as the carrier always being
supplied. It is not: `log-derived-bed-fold` is `bed-fold` **or nil** —
nil on a log with no bed cycle — so on the shipped default that site
replays like any other. Recorded rather than corrected in place, in the
addendum's own site-7 section.

**THE RUN ALREADY BUILDS THE ENTRIES IT WOULD HAND OVER, and that cuts
both ways.** `:replay-entries` is in `full-algebra`, the accumulator is
seeded at `run.clj:1577`, and `fold.clj:572-579` records that nothing
reads it — so the in-run handoff R-check-once rules costs no NEW
retention. But run-built entries are not replay-built entries:
`init-world` seeds `:patients` with every patient at t=0 while `replay`
starts `{:patients {}}` and bootstraps on first sight, so `:world-before`
differs in population. Ten of the reading sites index it by a
participant id and are indifferent; `check.clj:929` iterates it whole
through `occupancy-board`, where the VALUE still agrees and the COST
becomes O(all patients) per surge event — site 3's quadratic
reintroduced in check. Named as site 7's central obligation, with three
remedies listed and none chosen.

**AGENTS.md STATES A LAW THE SHIPPED CODE DOES NOT KEEP.** Its
Constraints section inherits from `sim/ADR-0002` that determinism means
"no wall-clock, no hash-order dependence". `decide :merge` and `decide
:bed-swap` resolve a draw positionally over a vector whose order is
`(:patients world)`'s hash-map iteration order, which is a hash-order
dependence in the plain sense. The runs stay reproducible on a fixed
Clojure and JDK; what is not established is stability across a `hasheq`
or `PersistentHashMap` change. **Rowed, not fixed** —
`roadmap.md#determinism-hash-order-dependence` — and deliberately not
resolved by editing AGENTS.md, because which of the two sentences gives
way is a ruling and not a session's call.

## 5. Suite figures

| point | test-result lines | passes | failures | errors |
|---|---|---|---|---|
| `ac081b7c`'s tree (step 2's gate) | 422 | 27,935 | 0 | 0 |

`EXIT=0`, 21 minutes 42 seconds, `bin/verify-nist-lock` OK on all six
coordinates. **Identical to `5ac15dce`'s figures** (site 4's record,
section 6), which is what a docs-only diff should look like from here.

Docs-only diffs are exempt from a local suite run under AGENTS.md's
de-scaffold ruling; it was run anyway because the prompt's step 2 named
it as that step's gate. Every figure above is taken from the `EXIT=`
sentinel the command wrote itself and NOT from the harness completion
notification — site 4's own finding applied: the notification did
arrive, and it was checked against the sentinel rather than believed.

## 6. Background processes

One: the `make test` of step 2, started with `run_in_background` and
read back through its own `EXIT=` sentinel. No `sleep` waiter was used
and none was written. At close: `ps` shows zero `sleep` and zero `java`
processes belonging to this session.

## 7. HEAD landed

`ac081b7c` is the last payload commit of this session; this record and
its prompt archive land on top of it, and a close-marker commit follows
once CI is verified green with `gh run view`. Baseline was `13bbad53`.

The two payload commits went out in **two separate pushes**, one per
step, because each carried its own gate — so GitHub Actions ran twice
rather than once. `bin/post-push-verify` ran immediately after each: in
both cases check 1 (remote tip equal to HEAD) and check 2 (every commit
message in range pure ASCII) passed, and check 3 DISCLOSED that no CI
run was indexed yet, which is the expected reading and the reason
section 8 exists.

## 8. CI

Two payload commits went out in two pushes and the record in a third, so
GitHub Actions ran three times rather than once. Verified with
`gh run view`'s own `status`/`conclusion` pair — polled through the
harness monitor, never a hand-rolled `until` waiter and never
`gh run watch`, whose premature-completion report site 4 recorded:

| commit | run | conclusion |
|---|---|---|
| `88a6d6e8` (the addendum) | 34078011071 | **success** |
| `ac081b7c` (the rows) | 34079493326 | **success** |
| `6508a9c8` (this record, tip) | 34079635129 | **success** |

`bin/post-push-verify` ran immediately after each push: remote tip equal
to HEAD, every commit message in each range pure ASCII, and the CI run
DISCLOSED as not yet indexed — which this section is where it was
awaited. `gitleaks` scanned ~44 MB at each push hook and found no leaks.
Each of the three pushed messages was diffed against the file that
produced it; every diff was exactly one trailing blank line, which is
`git log --format=%B`'s own formatting artefact and not a mismatch.

At close: `ps` shows zero `java` and zero `sleep` processes, the working
tree is clean, and the one background process this session started (step
2's `make test`) ended on its own `EXIT=0` sentinel.

CI green at the tip is the marker this charter landed; no tag was paid
(the de-scaffold ruling, 2026-08-25). Nothing was enacted: sites 5, 6
and 7 are three sessions that have not run, sequenced by R-order-2, and
`roadmap.md#determinism-hash-order-dependence` is the precondition the
first of them must read before it touches `eligible`.
