# 2026-08-25 — arc 2a: the person-simulator charter ADR

## Scope

Traffic-scale arc 2, design half (`roadmap.md#person-simulator`,
PRIORITY 3), unblocked by ADR-0171. A charter ADR derived from the
tree, on ADR-0162's sibling discipline. Payload session under the
de-scaffold moratorium: **no `components/*/src` or `components/*/test`
change, no draw anywhere, no component code**. Arc 2b implements what
section 5 rules.

Delivered:

1. `notes/adr/0172-person-simulator-charter.md` (**Proposed**) — the
   31-row demographics census, the front door (mission sentence,
   dependency direction, interface, a 14-kind event vocabulary, hazard
   rates, streams, clinical hooks, the mortality seam), the referential
   invariant shape every person event must satisfy, **11 gated
   limitations**, and **seven rulings A–G** as lettered options with a
   recommendation each.
2. Roadmap row `#person-simulator` — two lines added, the charter
   pointer. No other row moved.
3. `notes/ADRs.md` and `.agents/state-derived.md` regenerated
   (`make adr-index state-derived`). ADR count 171 → 172.

## What the census found

Nineteen of thirty-one live read sites are time-varying — but the
count overstates arc 3's work, which is the number the session was
asked to produce. `sim-emit-hl7`'s six collapse to **one lookup
shape** (five of the six read their persona out of the single
`patient-id`-keyed map at `emit_hl7.clj:302`), and
`patient-simulator`'s twelve collapse to **two** (`:756`, and `:state`
at `:1005`/`:1047`), because the other ten read `:dob` or `:sex`,
which no life-arc process moves.

## Findings, disclosed rather than absorbed

Two premise mismatches against the session prompt, both mechanical with
one defensible reading each, resolved in the tree's favour per
`rulings.md#R-stop-only-on-two-defensible-readings` and recorded in the
ADR's section 1:

* **There is no NK1 rendering to census.** The prompt asked for
  "emit-hl7's PID/IN1/NK1". `NK1` appears in **no `src` file anywhere
  in the tree** — only a test's unresolvable-locator list
  (`locators_doc_test.clj:168`) and a vendored research reference. So
  the household work R-mix-2 charters has no wire surface today at all,
  which became limitations row 8 rather than a gap to fill in passing.
* **Provenance records no demographics.** The prompt's census asked for
  "corpus/provenance". `:persona-config` is a run config key
  (`engine.clj:1858`) and is never stamped into a manifest;
  `components/sim/src` contains no occurrence of "persona". A corpus's
  demographic configuration is therefore not distinguishable on its
  artifact's face, unlike `:event-schema-version` and ADR-0171's
  `:stream-scheme`. Named, not fixed — the fence forbids the src change,
  and it is a provenance question, not this component's.

A third item the prompt did not anticipate, and the ADR's most
substantive departure: **R-mix-4's three identification events cannot
be person events** under the same session's "engine→person: none in v1"
constraint, because a placeholder registration is conditioned on an
arrival the person process cannot see. Emitting one at a drawn time
would make the person process *drive* engine control flow — strictly
stronger than the feedback edge v1 forbids. Resolution: the person
stream carries the **disposition** (`:identity-unavailable` window,
`:identity-resolution` branch) and the engine mints the wire-visible
placeholder/fill/merge. Both R-mix-4 branches stay in scope, as ruled;
only the seam moved. Because the prompt's shape is defensible, this is
carried as **ruling G** rather than decided here.

No stop condition fired: R-mix-1 through R-mix-4 and the census are all
honoured together. R-mix-3 in particular is honoured literally — a
residence move draws a whole new row from the same 24-row
`places.edn` pool (`persona.clj:64`), and limitations row 7 gates it.

## The seam the whole arc turns on

`engine.clj:182`, in its own words: a Persona is *"never resampled
after (the attribute-pool contract)"*. Sampled at `engine.clj:493`
from the `:patient` stream (`engine.clj:484`'s own `{rng :patient}`),
carried on the `:registered` event (`:513`), folded by
`evolve :registered` (`:1179`). Arc 2 supplies the alternative; arc 3
replaces that line.

## Red→green evidence

A docs-only session; the proof is the generated surface staying fresh
and the docs gates staying green.

* Every `file:line` in ADR-0172 was verified by reading the cited line
  back from this clone at `41081dd`. **Six citations drafted from a
  first pass were wrong and were corrected before commit**:
  `engine.clj` decide-dispatch 488→484, `check.clj`
  `medication-end-references-…` 662→682, its care-plan twin 714→740,
  the pre-horizon-escape quote 669→690, `compile_trajectory.clj`
  `death->step` 328→326, `pre-horizon-dropped-types` 363→344.
* One claim was **wrong as first written and corrected in place**: the
  ADR asserted `NK1` "occurs exactly once in the whole tree". A
  tree-wide grep found five occurrences — the test line plus four in
  `components/corpus/docs/research/HL7v2_ER7_MLLP_Reference.md`. The
  claim that survives is the one that is both true and load-bearing:
  no `src` file anywhere carries it.
* `make adr-index state-derived` run before commit, so CI's freshness
  diff has nothing to catch.
* Docs-tooling gates run locally before push (see the commit's own
  message for counts).

## What this deliberately did NOT do

No component. No `components/person-simulator` directory, no interface,
no `docs/limitations.md`, no charter gate — every one of those is arc
2b's, and arc 2b may not start until section 5 is ruled. No rulings-row
was added: `rulings.md` is FROZEN under the de-scaffold ruling, so
section 4's eleven laws land as **gates** in arc 2b's own commit or not
at all, the same disposition ADR-0171 took. No tag.

## Close marker

CI run **32893144168** @ `92e37c9` concluded **success**, verified by
this session via `gh` (`rulings.md#R-session-verifies-ci-via-gh`, kept
as the close marker after the de-scaffold ruling retired it as a tag
condition). One commit, one push, no tag.

`bin/preflight` before the commit: exit 1 on the tree-clean check
alone, reporting this session's own six files. Every other check OK --
last five CI runs green, edit root not under `/mnt/`, HEAD matching
`origin/main` at `41081dd`. `bin/post-push-verify 41081dd 92e37c9`:
remote tip matched, every message in range pure ASCII.

The history-reading gates were run AFTER the commit, not before --
ADR-0162's own ordering remedy, since `hand-owned-asset-freshness-test`
reads `git log` and is blind by construction on an uncommitted tree.
`hand-owned-asset-freshness`, `process-law-citation`,
`front-door-fence-gate`, `lint`: **15 tests, 84 assertions, 0
failures**. Nothing fired: this session changed no source any
hand-owned asset cites.

