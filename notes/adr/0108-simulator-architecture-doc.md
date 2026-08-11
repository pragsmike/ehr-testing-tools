## ADR-0108 — Simulator architecture doc lands, made load-bearing by a co-landed purity lint

**Status:** Accepted (author-directed, autonomous session per R30), 2026-08-11.

### Context

Author charter, 2026-08-11, verbatim (the chartering ruling,
`.agents/rulings.md`, "From ADR-0108"): *"I want to document this
architecture in the tools repo, as that's where the implementation is.
This is more of an aid to understanding the design, as well as a guide
for agents to avoid departing too much from the established theory
when adding features. We might include a treatment in the guide as
well."* Ratified: *"Good sequence."* This session (2026-08-11, HEAD at
handoff `5a2832f`) executes that charter: `docs/dev/
simulator-architecture.md` (dev-docs, R34), a co-landed purity lint
proving its own state-isolation claim rather than merely asserting it,
and wiring into the agent reading path. The guide-side treatment named
in the charter's own last sentence is the author's own future
authorship, out of this session's own fence (Decision, Fences, below).
The tool-specific user guide (distinct from the generic EHR Testing
Guide) stays DEFERRED under its own named trigger, recorded verbatim
below and in `.agents/plans/roadmap.md`.

### Tag ceremony

Design channel verified the ADR-0107 landing at `5a2832f` by fresh
public clone. `stable-20260811-injuries-arc-close` tagged annotated at
`5a2832f`, message "injuries arc closed, design-channel-verified
2026-08-11 (ADR-0107)"; pushed; peeled ref confirmed
`5a2832ffe5e457e2584c24758828f4a5c1ff6800` — exact match; remote had
not moved (`git fetch` confirmed `origin/main` already at `5a2832f` at
session start).

### Decision

**[A] The chartering ruling and the ratification**, both verbatim
above, executed as: a new dev-docs page plus a co-landed docs-tooling
test, one commit, zero `src` changes anywhere in any sim-family brick
(the fence: describing accurately, not fixing — no discrepancy found
that would have forced a STOP-AND-REPORT).

**[A] The user-guide deferral, standing**, author verbatim: *"I've
been deferring creating the tool-specific user guide in tools repo
(distinct from EHR Testing Guide, which is more generic) until things
settled down and the tools were able to produce the realistic traffic
I need. That remains to be seen, but it's getting more likely to
verifiably happen soon."* Trigger (channel-proposed, un-vetoed): the
latency-realism arc landed PLUS one witnessed end-to-end demo of
latency-realistic traffic played into a downstream-receiver stand-in.
Recorded in `.agents/rulings.md` and `.agents/plans/roadmap.md`'s own
downstream-latency-realism Next row, alongside the full ratified
sequence: architecture doc landed (this ADR) → latency design pass
next (unscheduled) → guide treatment in the author's own queue →
user guide deferred under this trigger.

**[C] Dev-docs scope**: the architecture doc is dev-docs
(`docs/dev/simulator-architecture.md`), not user path — R34 governs,
never the footnote-citation discipline ADR-0101/ADR-0102 established
for `docs/` proper. Nothing guide-side or user-path landed this
session, per the driving prompt's own explicit fence.

### The doc

`docs/dev/simulator-architecture.md` (269 lines): a component
inventory table for the seven `sim-*` bricks plus `sim` itself, each
row citing its own `interface.clj` and the source functions behind it;
the decide/evolve doctrine restated from `engine.clj`'s own ns
docstring and `sim/ADR-0008`, never re-derived; the state-isolation
claim (section 3, below); the palgebra section using the diagrammatic
composition operator `⨟` (U+2A1F, never the infix ring-compose `∘`)
and `×` for resource-tensor RNG threading, matching `docs/dev/
notation.md`'s own existing convention (`gate = judge ⨟
route-by-verdict`); two honest wrinkles (`engine` is one fold over a
shared `World`, not parallel per-patient folds — cited to the actual
`world'` reduce in `run`'s own loop, `engine.clj:1534-1541`; the GMF
walk is an unfold meeting `evolve`'s fold, cited to `walk-module`/
`run-module`, `gmf_interpreter.clj:2061`/`:2161`); the naturality
witness cited by name (below); and the latency extension point named
in one sentence (section 5), building nothing. Every architectural
claim carries a file or ADR citation, re-read from the live tree while
writing the record, matching `docs/dev/source-sink-design.md`'s own
register style (the sibling doc it mirrors).

### The mutable-state census

Re-run this session, cited verbatim in the doc's own section 3:

```
$ grep -rn '(atom \|(ref \|(agent \|volatile!\|set-validator!' \
    components/sim-model/src components/sim-trajectory/src \
    components/sim-engine/src components/sim-emit-hl7/src \
    components/sim-emit-fhir/src components/sim-check/src components/sim/src
components/sim-trajectory/src/ehrt/sim_trajectory/census.clj:407:    fetched (atom {id root-json-text})
```

Zero atoms/refs/agents/volatiles anywhere else across all seven
bricks' `src`. Two named exceptions: `census.clj`'s own `fetched`
probe-fetch memoization atom (a census-run's own bookkeeping, never
read by `decide`/`evolve`/`run`/`replay`) and `version.clj`'s own
`git-sha` (`components/sim/src/ehrt/sim/version.clj:19-37`, a `slurp`
of `.git/HEAD` wrapped in `try`/`catch` — not actually a mutable-state
primitive this lint's five forms would catch, listed for parity with
the doc's own two-exception statement). `java.util.Random`, seeded
once in `run` and explicitly threaded as `decide`'s own first
argument, is the one deliberate impurity inside the simulation path
itself — the RNG-path law (`.agents/rulings.md`, "Measurements sample
the claimed population, standing," AR-RL2-2, `notes/ADRs.md`
ADR-0092).

### The naturality witness

Cited by name, not merely gestured at: `fhir-patient-id-and-active-
mrn-resolve-to-the-same-hl7-identity` (`components/sim-emit-fhir/test/
ehrt/sim_emit_fhir/emit_fhir_test.clj:147`, a 150-trial `defspec`) —
`emitH`/`emitF`'s own claim that both renderings, drawn from one `GT`
object, resolve the same patient identity, proven rather than assumed.

### The purity lint, red proven

`ehrt.docs-tooling.sim-purity-lint-test`
(`components/docs-tooling/test/ehrt/docs_tooling/
sim_purity_lint_test.clj`): reader-based (never regex — the same
`ehrt.cli.cli-parse-guard-lint-test` discipline this test's own walker
mirrors), scanning every `.clj` file under the seven bricks' `src` for
a `(atom ...)`/`(ref ...)`/`(agent ...)`/`(volatile! ...)`/
`(set-validator! ...)` call anywhere in the read s-expression tree,
outside the two allowlisted namespaces (`ehrt.sim-trajectory.census`,
`ehrt.sim.version`).

**Non-vacuity, verified live this session.** A temporary, clearly
marked atom was planted in `components/sim-model/src/ehrt/sim_model/
config.clj` (an unallowlisted sim-family file):

```clojure
;; TEMPORARY -- ADR-0108 non-vacuity plant, removed before commit.
(def ^:private scratch-plant (atom {}))
```

Red, verbatim:

```
Testing ehrt.docs-tooling.sim-purity-lint-test

FAIL in (no-mutable-state-primitives-outside-the-two-named-exceptions-test) (sim_purity_lint_test.clj:121)
The following sim-family src files call atom/ref/agent/volatile!/set-validator! outside the two allowlisted namespaces (docs/dev/simulator-architecture.md section 3, ADR-0108): ("components/sim-model/src/ehrt/sim_model/config.clj")
expected: (empty? violators)
  actual: (not (empty? ("components/sim-model/src/ehrt/sim_model/config.clj")))

Ran 5 tests containing 14 assertions.
1 failures, 0 errors.
```

The plant was then removed; `git diff --stat` against
`config.clj` confirmed byte-identical to its pre-plant state; the lint
re-ran green (5 tests, 14 assertions, 0/0) against the real, unplanted
tree. The lint's own docstring points at the doc (`docs/dev/
simulator-architecture.md` section 3); the doc's own state-isolation
section points at the lint (`ehrt.docs-tooling.sim-purity-lint-test`)
— the pair is the guardrail the charter asked for.

### Wiring

`AGENTS.md` gains a pointer paragraph in its Structure section,
immediately after the seven `sim-*`/`sim` Components entries: *"Working
on any of the seven `sim`/`sim-*` bricks above: read
`docs/dev/simulator-architecture.md` first... Standing channel practice
from this ADR: any session prompt fencing sim-family `src` carries
this doc in its own Read-first list."* `.agents/reading-sets.edn`'s
`:sim` set gains `docs/dev/simulator-architecture.md` as a new
`:paths` member. Budget re-baseline (the file's own standing
discipline, ADR-0107's own re-baseline is the precedent): `:sim`'s
fresh actual (`wc -l` sum across all six `:paths`, measured after this
session's own `AGENTS.md` edit landed) — 294 (AGENTS.md) + 47
(sim/interface.clj) + 85 (engine-onboarding.md) + 240 (components.md)
+ 187 (build-session/SKILL.md) + 269 (simulator-architecture.md) =
1122. Re-applying the standing formula (actual × 1.15, rounded up to
the nearest 5): 1122 × 1.15 = 1290.3 → 1295. Budget moves 970 → 1295.
No other reading set carries `docs/dev/simulator-architecture.md`, so
no other set's budget changes here (`:onboarding`'s own `AGENTS.md`
growth is re-measured at this session's own close phase, below,
alongside that set's own roadmap/README churn).

### Commit

`62d1d5e` — "docs: simulator architecture doc, purity lint co-landed
(ADR-0108)." Four files: `docs/dev/simulator-architecture.md` (new),
`components/docs-tooling/test/ehrt/docs_tooling/
sim_purity_lint_test.clj` (new), `AGENTS.md`, `.agents/
reading-sets.edn`. Pushed; post-push verification: one delta against
the message file, the known harmless trailing-newline artifact; ASCII
check clean.

### Oracle bracket

**Pre-analysis**: this session's own footprint is a doc, a test file,
and register wiring — no `src` change anywhere in any brick, sim-family
or otherwise. Expectation: pure identity across every root.

**Bracket result.** `bin/regression-oracle 5a2832f 62d1d5e`: `IDENTICAL:
every root's digest matches between 5a2832f and 62d1d5e` — all 35
roots (the `injuries` root, first-baselined by ADR-0107, included)
byte-identical. Matches the pre-analysis exactly; no STOP-AND-REPORT
needed.

### Full gate

`clojure -M:poly check`: OK. Full local suite (`clojure -M:poly test
:all skip:integration`): 608 occurrences of "0 failures, 0 errors"
across the entire output, zero `FAIL`/`ERROR` report lines anywhere,
4 minutes 43 seconds. `ehrt.cli.cli-parse-guard-lint-test`: 4 tests, 22
assertions, 0/0. `bin/verify-nist-lock`: OK, 6 hit-nexus-sourced
coordinates matched. `gitleaks git --staged -v` (pre-commit) and
`gitleaks detect` (pre-push): no leaks found.

**The last five `main` CI runs** (`gh run list --limit 5 --branch
main`, checked at session start, before this session's own push): all
`completed`/`success` — `5a2832f` (CI-flake disclosure docs, 4m17s),
`1b66fb7` (injuries-arc-close session record, 4m42s), `29392cd`
(injuries batch feat, 4m16s), `7db2044` (nested-encounter fix, 4m16s),
`fdb3984` (injuries B2 assessment session record, 4m18s) — no red
among the five. This session's own push (`62d1d5e`) also confirmed
`completed`/`success` post-push.

### Fences

Touched exactly: `docs/dev/simulator-architecture.md` (new),
`components/docs-tooling/test/ehrt/docs_tooling/
sim_purity_lint_test.clj` (new), `AGENTS.md` (the pointer paragraph),
`.agents/reading-sets.edn` (the `:sim` set), plus the usual close-phase
register files (`notes/adr/0108-*.md` this file, `notes/ADRs.md`,
`notes/adr/README.md`, `.agents/plans/roadmap.md`, `.agents/
rulings.md`, `.agents/prompts/`, `.agents/session-records/`). The
temporary non-vacuity plant in `components/sim-model/src/ehrt/
sim_model/config.clj`: planted, red captured, removed, confirmed
byte-identical to its pre-plant state before this ADR's own commit —
never staged, never committed. ZERO other `src` changes in any brick.
Nothing guide-side, nothing user-path, no user-guide scaffolding.

### Deviations, dated 2026-08-11

None. Every step executed as the driving prompt specified; the
non-vacuity plant was licensed by the prompt itself (step 3) and fully
reverted before commit.

### Index line

```
- 2026-08-11 — simulator-architecture-doc — ADR-0108
```

(appended to `.agents/plans/roadmap.md`'s own Done section.)
