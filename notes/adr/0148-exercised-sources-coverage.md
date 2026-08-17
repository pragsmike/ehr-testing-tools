## ADR-0148 — Every exercised-sources row is gated by construction: one coverage test over the register in place of nine hand-written cases

**Status:** Accepted (author-directed, autonomous session per R30),
2026-08-17.

### Context

ADR-0146's U-15 is the whole argument, and it is worth restating in its
own terms because this session exists only to finish it.

That session added `bin/event-census` as a taught command on
`docs/use-cases/custom-emitter-from-the-event-log.md`'s fence and did
not add it to `bin/usecase-custom-emitter`. The entire docs-gate
battery — including `strip-fresh-test`, *whose whole job is proving
that a page and its exerciser teach the identical commands* — stayed
green. The commit landed a page teaching four commands and a script
running three.

The cause was two-layered. `bin/usecase-custom-emitter` was the one row
of the nine in `exercised-sources.edn` with **no live `check-entry`
case**; and it could not have passed if added, because the script wrapped
the taught redirect as `expect 0 bash -c '…'`, which `strip-fresh`'s
unwrapper cannot read. ADR-0146 fixed both halves *for that row*: it
added the missing case, rewrote the wrapper as `expect_eval`, and landed
`R-exercised-implies-gated` and `R-taught-shell-lines-use-expect-eval`.

What it did **not** do — and said so, opening
`roadmap.md#exercised-row-gate-closure` at PRIORITY 7 — is make the rule
structural. Nine hand-written per-row cases cover nine rows. The tenth
row added tomorrow is covered by nothing, and the silence that hid U-15
is available again, unchanged. `rulings.md#R-population-closure` names
the right shape: enumerate the population from the tree, diff it against
whatever claims to cover it, and treat the gap as the finding — not a
tenth hand-written case.

### The census, re-derived at tip `5c1d73e`

Everything below was probed live before anything was written, rather
than carried over from the prompt's own channel-probe summary.

#### 1. The register: **nine** rows, all fresh today

The session prompt states ten. It is nine — `exercised_sources_test.clj:27`
pins `(= 9 (count rows))`, and `roadmap.md#exercised-row-gate-closure`'s
own text says "as one of **nine** already had". Recorded as a premise
correction rather than adapted around silently
(`docs/dev/way-of-working.md` §2).

`(check-all (load-registry))` at `5c1d73e`, run live:

| # | source | script | extraction | live `check-entry` case | result |
|---|---|---|---|---|---|
| 1 | `README.md` (Quickstart) | `bin/quickstart-demo` | `:quickstart-fresh` | `strip_fresh_test.clj:164` `check-entry-delegates-live-to-quickstart-fresh-test` | fresh, 15/15 |
| 2 | `demos/scenarios/ed-tuesday/README.md` | `bin/demo-exerciser-ed-tuesday` | `:demo-exerciser-fresh` | `strip_fresh_test.clj:171` `…-to-demo-exerciser-fresh-test` | fresh, 21/21 |
| 3 | `docs/use-cases/judge-tier-calibration-studies.md` | `bin/usecase-judge-tier-calibration` | `:single-fence` | `strip_fresh_test.clj:206` | fresh, 9/9 |
| 4 | `docs/use-cases/profile-tier-hl7v2-conformance-gating.md` | `bin/usecase-profile-tier-v2` | `:single-fence` | `strip_fresh_test.clj:211` | fresh, 3/3 |
| 5 | `docs/use-cases/acceptance-qa-of-vendor-corpora.md` | `bin/usecase-acceptance-qa` | `:single-fence` | `strip_fresh_test.clj:216` | fresh, 6/6 |
| 6 | `docs/use-cases/regression-baselining.md` | `bin/usecase-regression-baselining` | `:single-fence` | `strip_fresh_test.clj:221` | fresh, 4/4 |
| 7 | `docs/use-cases/custom-emitter-from-the-event-log.md` | `bin/usecase-custom-emitter` | `:single-fence` | `strip_fresh_test.clj:301` (ADR-0146, U-15's own fix) | fresh, 5/5 |
| 8 | `README.md` ("What you get") | `bin/readme-what-you-get` | `:paired` | `strip_fresh_test.clj:226` | fresh, 6/6 |
| 9 | `demos/scenarios/clinic-decade/README.md` | `bin/demo-exerciser-clinic-decade` | `:demo-exerciser-fresh` | `strip_fresh_test.clj:185` | fresh, 5/5 |

**Nine of nine have a live case, and nine of nine are fresh.** No row is
diverged today, so the F-3 STOP condition the prompt names does not fire.
The gap this session closes is therefore not a present-tense hole — it is
that the covering set is hand-maintained, and hand-maintenance is exactly
what failed once already.

#### 2. `check-all` is reached by nothing

`strip_fresh.clj:218`. Zero callers in the whole tree — no test, no
`bin/` script, no `make` target. The register's own aggregate check has
existed since ADR-0129 and has never been run by the build. That is the
mechanism this session needed, sitting unused beside nine hand-written
calls to the thing it wraps.

Every `strip_fresh.clj` fn reachable from `check-all`, and its coverage:

| fn | line | reachable from `check-all` | exercised by |
|---|---|---|---|
| `check-all` | 218 | — (the root) | **nothing** |
| `check-entry` | 178 | yes | 10 direct calls, `strip_fresh_test.clj` |
| `script-command-lines` | 83 | yes | indirectly only (0 direct calls) |
| `single-fence-command-lines` | 141 | yes | 7 direct calls |
| `command-output-pairs` | 149 | yes | 5 direct calls |
| `fenced-blocks` (private) | 109 | yes | indirectly |
| `unwrap-script-line` (private) | 77 | yes | indirectly |
| `diverge-at` (private) | 100 | yes | indirectly |
| `absent-script-result` (private) | 171 | yes | `strip_fresh_test.clj:120` |
| `blank-line?` / `comment-or-blank?` | 68/70 | yes | indirectly |
| `parse-elided-edn` | 256 | no (runtime-output half) | 2 direct calls |
| `subset-match?` | 262 | no | 6 direct calls; also `bin/demo-exerciser-clinic-decade:137` |
| `paired-output-check!` | 286 | no | `bin/readme-what-you-get:74,79` (not a test — `System/exit`) |

#### 3. Two trivial-pass hazards, probed

The prompt asks whether a `bash -c` wrapper is reported honestly or
silently fresh. Probed directly:

    === bash -c wrapper ===
    {:ok? false, :readme-count 1, :script-count 1,
     :divergence {:index 0,
                  :readme "foo > out/x",
                  :script "bash -c 'foo > out/x'"}}

**Green on arrival, and said so.** The wrapper is not silently fresh and
never was — it produces a loud divergence carrying the wrapper text
verbatim, which is precisely how U-15's own red witness read. No
`:unreadable` classification is *required*; adding one would improve a
diagnostic, not close a gate, and this session does not widen a
mechanism that already holds (move-don't-improve).

But the probe found the hazard's real sibling, and this one is a genuine
silent pass:

    === no matching fence in doc + empty script marker block ===
    {:ok? true, :readme-count 0, :script-count 0, :divergence nil}

    === doc fence exists but is all comments; script block empty ===
    {:ok? true, :readme-count 0, :script-count 0, :divergence nil}

    === :paired with no pairs at all + empty script block ===
    {:ok? true, :readme-count 0, :script-count 0, :divergence nil}

Three routes to a source yielding **zero** taught command lines — no
fence of the row's own language, a fence that is entirely comments, a
`:paired` row whose source holds no genuine pair — and in all three the
old code compared `[]` to `[]`, found no divergence, and certified the
pair **fresh**. A freshness check reporting green over a page-script pair
that no command ever passed through is the same "proves nothing while
looking green" shape as U-15, arriving through an absent population
rather than an unreadable line. It is also, word for word, the shape the
session prompt anticipated — "an empty command list that trivially
matches" — attributed to the wrong trigger.

This is what Step 3's src change fixes, and it is the only src change
this session makes.

#### 4. The dual, probed: no ungated exerciser exists today

Enumerating exerciser-shaped scripts from the tree (`bin/usecase-*`, or
any name containing `exerciser`) and diffing against the register:

    tree (usecase-*/*exerciser*): 7 scripts
    in tree, NOT registered: ()
    registered, NOT in tree: ()

Seven exerciser-shaped scripts, all registered; nine registered scripts,
all present. `bin/quickstart-demo` and `bin/readme-what-you-get` match
neither naming pattern and are covered by the rows-have-scripts
direction instead.

**A disclosed narrowing of (d).** The prompt scopes the dual to scripts
"that a `docs/**` page cites as its exerciser". Probed, that population
is two cites of one script (`bin/demo-exerciser-ed-tuesday`, in
`docs/manual/04-time-on-the-wire.md` and `05-batch-delivery.md`); the
five `bin/usecase-*` scripts are cited by no reader-facing page at all,
so the cite-filtered gate over them would be **vacuous**. Landed as two
gates rather than one narrow one: the unconditional tree-population
closure (7 scripts, non-vacuous) *and* the doc-cite closure, each with an
explicit `(pos? (count …))` sanity assertion so a silently-empty
population fails by name rather than passing as "no violations".

`.agents/**` is excluded from the cite population on purpose: prompts,
session records and plans mention scripts that were deferred
(`bin/demo-exerciser-busy-tuesday`) or renamed, and a historical mention
there is the workspace talking to itself, not a page claiming to be
exercised.

### Decision

One test namespace, `ehrt.docs-tooling.exercised-sources-coverage-test`,
running `check-all` over `load-registry` itself — so a row added tomorrow
is gated the moment it is registered, with no test edit at all — plus the
dual, so a page cannot claim "exercised" by a script the register never
gates. And one src fix: a source yielding zero taught commands is never
reported fresh.

### Fences

- Src changes are confined to `components/docs-tooling`.
- The nine existing hand-written live cases are **not deleted** this
  session. They are now redundant, not wrong; each is marked and their
  retirement is a roadmap row (move-don't-improve).
- No register row is edited: (d) forced none.
- No new extraction kind.

### Red, then green

RED, witnessed before any src change, on the new namespace alone:

    Ran 8 tests containing 26 assertions.
    6 failures, 0 errors.

All six in `a-source-yielding-no-taught-commands-is-never-reported-fresh-test`,
two per route:

    FAIL in (…) (exercised_sources_coverage_test.clj:168)
    no fence of the row's own language
    expected: (false? (:ok? r))
      actual: (not (false? true))

    FAIL in (…) (exercised_sources_coverage_test.clj:170)
    no fence of the row's own language
    expected: (= :ehrt.docs-tooling.strip-fresh/no-taught-commands (:readme (:divergence r)))
      actual: (not (= :ehrt.docs-tooling.strip-fresh/no-taught-commands nil))

…and the same pair for "a fence that is entirely comments" and "a
`:paired` row whose source holds no genuine pair".

**Everything else was green on arrival, exactly as the census predicted**
— (a) coverage over all nine live rows, (b) the seeded two-row
instrument failing on precisely the diverged row, (c) the `bash -c`
pin, and all three (d) population gates. That is the census doing its
job: it is why this session's src change is one function.

GREEN after `reject-vacuous`:

| gate | baseline `5c1d73e` | close |
|---|---|---|
| `make test` (unpiped, `MAKE_EXIT`) | **0** | **0** |
| blocks | 342 | 344 |
| tests | 3,890 | 3,906 |
| assertions | 17,496 | 17,548 |

Baseline reconciles exactly with ADR-0147's own recorded figures. The
deltas are the new namespace and nothing else: +8 tests and +26
assertions, counted twice because the namespace runs under two projects.

`bin/regression-oracle 5c1d73e 3dd20ed`, its own output:

    --- declared-digest-change: no (soundness: yes outside ns form) ---
    IDENTICAL: every root's digest matches between 5c1d73e and 3dd20ed

35 of 35 roots. `clojure -M:poly check` OK at both ends.

### Findings

**F-1 — the register has nine rows, not ten.** The session prompt's
channel probe says ten. `exercised_sources_test.clj:27` pins nine, and
the roadmap row this session closes says "one of **nine**". Reported
rather than adapted around; nothing downstream depended on the number,
because the whole point of the landing is that no test names a count of
rows any more.

**F-2 — the `bash -c` hazard needed no fix, and its sibling did.** The
prompt anticipated an `:unreadable` classification for U-15's wrapper.
Probed first, per its own instruction: the wrapper already diverges
loudly. What is genuinely silent is the ABSENT population — a source
yielding zero taught commands, which compared `[]` to `[]` and reported
fresh. The prompt named the right shape ("an empty command list that
trivially matches") against the wrong trigger. Fixed the sibling, pinned
the wrapper.

**F-3 — (d) as scoped would have been vacuous, and was widened.** The
prompt scopes the dual to exercisers "a `docs/**` page cites". That
population is two cites of one script; the five `bin/usecase-*` scripts
are cited by no reader-facing page, so a cite-filtered gate over them
asserts nothing. Landed as the unconditional tree-population closure
(seven scripts) *and* the cite closure, each carrying an explicit
non-empty assertion. This is the session's own second instance of the
rule it is landing: `R-empty-population-is-red` was earned twice over,
once by the mechanism under test and once by the test.

**F-4 — no row is diverged, and no register row was edited.** All nine
rows are fresh at `5c1d73e` and at close. Neither STOP condition the
prompt names fired.

**F-5 — the ADR-0147 freshness gate fired on this session's own
footprint.** The first full green run went red on
`state-derived-md-matches-a-fresh-render-test`: this session added an
ADR file, a test namespace, a roadmap row and two rulings rows, all of
which `.agents/state-derived.md` counts. Fixed by `make docsgen`, never
by hand — which is the whole design of ADR-0147, working, one session
after it landed.

### Consequences

The nine hand-written per-row cases are now the *second* thing proving
each row, and the register itself is the first. A tenth row added
tomorrow is gated the moment it is registered. The path that produced
U-15 — register a row, write its script, forget its test, watch the
docs-gate battery stay green — is closed by construction rather than by
remembering.

Two rulings landed: `R-register-gated-by-its-own-loader` and
`R-empty-population-is-red`. One roadmap row closed
(`[exercised-row-gate-closure]`), one opened
(`[strip-fresh-hand-case-retirement]`, PRIORITY 16) — the hand cases are
kept this session because their pinned `:readme-count`s are NOT
subsumed and carry a real distinct signal, so retiring them is judgement
about where the pins belong, not a deletion (`rulings.md#R-move-not-improve`).
