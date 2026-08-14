# State of the project — continuity register

**CITATION-ONLY update, 2026-08-14, exact-name state resolution:
collision fix, restoration cascade (`notes/adr/0133-exact-name-
resolution.md`) — this citation moves here from ADR-0132, CONTENT NOT
RE-PROBED.** Not an arc close (this session's own naming convention),
so `state_staleness_tripwire_test.clj`'s own regex is untouched by
this update. Recorded here anyway, in the same append-only citation-
only spirit as the entries below. Landed: the vendoring-rider row
ADR-0131 chartered (5 modules, 10 slug-collision pairs) closed via a
new author ruling superseding its own original per-module-JSON-edit
framing — loader-side exact-name resolution instead (a raw-name -> key
table, every one of twelve name-valued reference categories resolved
by EXACT raw string, module JSONs verbatim, ADR-0071 preserved); the
WARN -> hard-error escalation ADR-0131 chartered is DISCHARGED, not
executed (both members now load as real states; the guard becomes a
disambiguation disclosure; a new strictness, `:unresolved-state-
reference`, lands instead). Two mid-session STOP-AND-REPORTs, both
ruled ("the restoration cascade," one restoration pulling on two more
threads): `gmf-interpreter.clj`'s own `max-steps` backstop switched to
reset-on-any-advance semantics (a real, legal recurring-care loop in
`veteran_ptsd.json` was false-firing the OTHER ADR-0105-licensed
semantics, unmasked by the restoration); `compile-trajectory.clj`'s
own `encounter->step`/`encounter-end->step` gained a `:virtual` clause
at both dispatch sites (resolving a decision ADR-0029 D3f's own
`gmf.clj` docstring had explicitly deferred to "whichever future
session first exercises a closure through the full compile-trajectory
pipeline" — this one). Declared-oracle-change prediction (5 roots
MOVE) matched the official `bin/regression-oracle` bracket on 4 of 5;
`hypothyroidism` predicted MOVE but stayed byte-identical, investigated
and explained (both its own collision-pair members are `:exact`-
severity Symptom states whose only effect is never read downstream in
this module — restored, real, but structurally unobservable), not a
bug. Full local suite green throughout (632 "0 failures, 0 errors"
blocks, matching this session's own pre-fix baseline exactly). Zero
module JSONs edited (vendored verbatim, ADR-0071 precedent). Every
section below still reflects its LAST full regeneration, 2026-08-08
against tip `a9c3abf` — twenty-six ADRs' worth of landings since (0090
through 0133, excluding citation-only entries) are NOT reflected below
and every `[V]` tag below should be read accordingly; a full
regeneration is still owed at a session that rules it.

**CITATION-ONLY update, 2026-08-14, scenario rename (busy-tuesday ->
clinic-decade) + exerciser completion (`notes/adr/0132-clinic-decade-
rename-and-exerciser.md`) — this citation moves here from ADR-0131,
CONTENT NOT RE-PROBED.** Not an arc close (this session's own naming
convention), so `state_staleness_tripwire_test.clj`'s own regex is
untouched by this update. Recorded here anyway, in the same
append-only citation-only spirit as the entries below. Landed: the
author's own name ruling ("clinic-decade it is.") executed as a full
live-reference sweep, zero residue outside frozen records; `bin/demo-
exerciser-clinic-decade` completes the exerciser row ADR-0130 closed
partial-with-open-rows, a new `exercised-sources.edn` row (count-lock
7 -> 8), all three README-taught commands witnessed end-to-end with
every named invariant re-derived live from the README and matched
(`68/48/41`/`inpatients: 0`, and the third command's own `367`/`49`
first-witnessed figures, both byte-for-byte the ADR-0130/ADR-0131
figures); R3 now fully discharged across every shipped scenario
README. Oracle held pure identity across all 35 roots, matching Step
0's own verified prediction. `make test`/`make integration` both
green, tree clean. Every section below still reflects its LAST full
regeneration, 2026-08-08 against tip `a9c3abf` — twenty-five ADRs'
worth of landings since (0090 through 0132, excluding citation-only
entries) are NOT reflected below and every `[V]` tag below should be
read accordingly; a full regeneration is still owed at a session that
rules it.

**CITATION-ONLY update, 2026-08-14, slug EDN round-trip fix + module-
load injectivity guard (`notes/adr/0131-slug-edn-round-trip.md`) —
this citation moves here from ADR-0130, CONTENT NOT RE-PROBED.** Not
an arc close (this session's own naming convention), so
`state_staleness_tripwire_test.clj`'s own regex is untouched by this
update. Recorded here anyway, in the same append-only citation-only
spirit as the entries below. Landed: `ehrt.sim-trajectory.gmf/slug`
(Q1(a)) now folds comma plus the reader's own thirteen terminating-
macro characters, alongside the pre-existing `_`/whitespace fold —
empirically derived against `clojure.edn/read-string` itself, not
hand-recalled from the reader grammar — restoring the emit-composed-
with-read identity law ADR-0130 found violated
(`uti/abx_tx.json`'s own comma-bearing state names); a module-load
injectivity guard (Q2(b), WARN-mode) warns to `*err*` per collision
group, naming the module, folded key, and every raw name, load
proceeding — escalation to hard-error chartered to a future rider
session as a mode switch, not a rewrite. Both defect censuses
re-derived across all 66 module JSONs (recursive): defect 1 (10
breaker keys/3 modules) matched the channel's own pre-probe exactly;
defect 2 (10 collision pairs) matched the pair count but found the
pre-probe's own "8 modules" figure wrong (actual 5, disclosed).
Declared-oracle-change prediction recorded BEFORE the fix, then
matched EXACTLY by the official `bin/regression-oracle` bracket: 3
roots moved (`urinary-tract-infections-engine`/`-history-engine`,
`injuries`), `veteran-lung-cancer` structurally contained a breaker
module but its own breaker states were grep-confirmed unreached at
that root's seed/population and correctly predicted NOT to move, 4
more roots plus `injuries` warned with zero byte movement, the
remaining 27 roots untouched. Acceptance: busy-tuesday regenerated
(seed 20260807, 200 patients), the README's own second command
(`--board`) reproduced ADR-0130's exact witnessed figures (`68/48/41`,
`inpatients: 0` throughout) byte-for-byte, and the README's own THIRD
command — the one that failed in ADR-0130 with `:play-input-
unreadable` — now completes for the first time ever. Full `make test`:
green (632 "0 failures, 0 errors" blocks, matching this session's own
pre-fix baseline, no other test moved). Zero module JSONs edited
(vendored verbatim, ADR-0071 precedent); zero README/figure edits.
Every section below still reflects its LAST full regeneration,
2026-08-08 against tip `a9c3abf` — twenty-four ADRs' worth of landings
since (0090 through 0131, excluding citation-only entries) are NOT
reflected below and every `[V]` tag below should be read accordingly;
a full regeneration is still owed at a session that rules it.

**CITATION-ONLY update, 2026-08-14, busy-tuesday exerciser: marker
widening landed, row deferred on a real slug EDN round-trip defect
(`notes/adr/0130-busy-tuesday-exerciser-deferred.md`) — this citation
moves here from ADR-0129, CONTENT NOT RE-PROBED.** Not an arc close
(this session's own naming convention; the row it chartered stays
OPEN, not closed), so `state_staleness_tripwire_test.clj`'s own regex
is untouched by this update. Recorded here anyway, in the same
append-only citation-only spirit as the entries below. **Reduced
close, TWO in-session STOP-AND-REPORTs, both ruled** — landed:
`ehrt.docs-tooling.demo-exerciser-fresh`'s own `script-command-lines`/
`check` widened to an explicit `marker-open`/`marker-close` pair
(ed-tuesday's own literal markers as default, every prior call site
byte-identical in behavior), `ehrt.docs-tooling.strip-fresh`'s own
`:demo-exerciser-fresh` case passing a register row's own markers
through, red-before-green proven via disposable-stash isolation; a
forced one-line `citation-gate-test` fix (a pre-session-register
simulation's own extraction-kind filter retargeted to script name,
after a legitimately-added third `:demo-exerciser-fresh` row broke its
own two-row sanity assumption); one sentence in `build-session/
SKILL.md` (+ mirror) sanctioning session-record checkpoint commits
when `make integration`'s tree-clean postcondition requires them
(ADR-0129's own discovered practice, now written down). **NOT
landed, reverted to byte-identity:** the busy-tuesday register row,
its own drafted `bin/demo-exerciser-busy-tuesday` script, and the
`Makefile` integration line — the script's own real end-to-end run
(seed 20260807, 200 patients) reproduced the seed-determinism contract
exactly on commands 1-2, then found a genuine, previously-undisclosed
defect on command 3: `ehrt.sim-trajectory.gmf/slug` never sanitizes
commas out of raw upstream Synthea state names before constructing a
keyword (`uti/abx_tx.json`'s own `"Cipro 500, 5 day"` state ->
`:cipro-500,-5-day`, which `pr-str`s cleanly but is not re-readable
EDN — `ehrt play events.edn`'s own read-back breaks on it). Full
disclosure, the drafted script's own verbatim text, and two new
`.agents/plans/roadmap.md` Next-section rows (the slug fix itself,
chartered `:sim`-family with a mandatory declared-oracle-change
assessment; scenario rename + exerciser completion, sequenced after
it) in `notes/adr/0130-*.md`. Zero `demos/` README edits, zero
`sim-trajectory`/module-content edits — the oracle holds pure identity
across all 35 roots. Every section below still reflects its LAST full
regeneration, 2026-08-08 against tip `a9c3abf` — twenty-three ADRs'
worth of landings since (0090 through 0130, excluding citation-only
entries) are NOT reflected below and every `[V]` tag below should be
read accordingly; a full regeneration is still owed at a session that
rules it.

**CITATION-ONLY update, 2026-08-13, strip executability: exercisers,
citation gate, ADR-0127 erratum (`notes/adr/0129-strip-
executability.md`) — this citation moves here from ADR-0128, CONTENT
NOT RE-PROBED.** Not an arc close (this session's own naming
convention), so `state_staleness_tripwire_test.clj`'s own regex is
untouched by this update. Recorded here anyway, in the same
append-only citation-only spirit as the entries below. Landed: five
new `bin/` strip exercisers (`usecase-judge-tier-calibration`,
`usecase-profile-tier-v2`, `usecase-acceptance-qa`, `usecase-
regression-baselining`, `readme-what-you-get`), each executed
end-to-end this session against real artifacts, wired into `make
integration`; a new `ehrt.docs-tooling.exercised-sources` registry and
`ehrt.docs-tooling.strip-fresh`'s two new extraction shapes; a new
`ehrt.docs-tooling.citation-gate` enforcing cited-implies-exercised
for every `docs/manual/0*.md` "Strip source citations" table entry
going forward; a dated erratum to `notes/adr/0127-*.md` (the wrong
`:sim` 1170/1295 figure, true 1293/1295); closes manual-review
dimension 1 (strip executability, FAIL -> PASS), the manual arc's
first all-dimensions-addressed state. Zero `docs/manual` prose/
`README.md`/`demos/` touched; `test-fixtures/` untouched (`acceptance-
qa` binds the already-committed `test-fixtures/v2`). Every section
below still reflects its LAST full regeneration, 2026-08-08 against
tip `a9c3abf` — twenty-two ADRs' worth of landings since (0090 through
0129, excluding citation-only entries) are NOT reflected below and
every `[V]` tag below should be read accordingly; a full regeneration
is still owed at a session that rules it.

**CITATION-ONLY update, 2026-08-13, agent-facing hardening: ADR-0127
addendum, anti-fabrication tripwire, Step-0 receipts (`notes/adr/
0128-agent-facing-hardening-2.md`) — this citation moves here from
ADR-0127, CONTENT NOT RE-PROBED.** Not an arc close (this session's
own naming convention), so `state_staleness_tripwire_test.clj`'s own
regex is untouched by this update — that gate tracks only
`*-arc-close.md` files, and ADR-0128 isn't one. Recorded here anyway,
in the same append-only citation-only spirit as the entries below, so
a future full regeneration finds this session named rather than
silently skipped. Landed: a dated addendum to `notes/adr/0127-*.md`
recording a transcript-witnessed fabricated-draft near-miss (a
deviation-justification drafted for skipping the Step 0 tag payment,
self-caught, deleted, never committed); an anti-fabrication tripwire
rule in `build-session/SKILL.md` (+ mirror); Step-0 receipts guidance
in `session-prompt/SKILL.md` (+ mirror) and `bin/close-scaffold
--expect-tag NAME@SHA`, a mechanical local+remote tag-payment
verification, smoke-tested three ways. Along the way found and fixed
a real `:sim` reading-set budget-lock error inherited from ADR-0127's
own Step 3 (measured 1170/1295 when the true actual was already 1293;
re-derived to 1495 per the standing formula). Zero `src` touched;
`bin/close-scaffold` is the only pre-existing script edited, mode
unchanged. Every section below still reflects its LAST full
regeneration, 2026-08-08 against tip `a9c3abf` — twenty-one ADRs'
worth of landings since (0090 through 0128, excluding citation-only
entries) are NOT reflected below and every `[V]` tag below should be
read accordingly; a full regeneration is still owed at a session that
rules it.

**CITATION-ONLY update, 2026-08-13, ceremony scripts, build-session
skill absorption, sim-identity citation sweep (`notes/adr/
0127-ceremony-scripts-sim-identity-sweep.md`) — this citation moves
here from ADR-0126, CONTENT NOT RE-PROBED.** Not an arc close (this
session's own naming convention), so `state_staleness_tripwire_test.
clj`'s own regex is untouched by this update — that gate tracks only
`*-arc-close.md` files, and ADR-0127 isn't one. Recorded here anyway,
in the same append-only citation-only spirit as the entries below, so
a future full regeneration finds this session named rather than
silently skipped. Landed: four `bin/` ceremony scripts (`preflight`,
`tag-ceremony`, `post-push-verify`, `close-scaffold`), R13's own
charter; `checkpoint isolation`/`red capture`/`sweep census` absorbed
into `build-session/SKILL.md` and its `.claude/` mirror; the
sim-identity citation sweep ADR-0126 disclosed but did not fix, now
CLOSED (238 raw hits re-derived, 106 sim-era sites origin-qualified,
132 workspace-current sites correctly left bare). Every section below
still reflects its LAST full regeneration, 2026-08-08 against tip
`a9c3abf` — twenty ADRs' worth of landings since (0090 through 0127,
excluding citation-only entries) are NOT reflected below and every
`[V]` tag below should be read accordingly; a full regeneration is
still owed at a session that rules it.

**CITATION-ONLY update, 2026-08-13, the citation errata sweep and
glossary linkage close (`notes/adr/0126-citation-sweep-glossary-
linkage.md`) — this citation moves here from ADR-0107, CONTENT NOT
RE-PROBED.** Not an arc close (this session's own naming convention),
so `state_staleness_tripwire_test.clj`'s own regex is untouched by
this update — that gate tracks only `*-arc-close.md` files, and
ADR-0126 isn't one. Recorded here anyway, in the same append-only
citation-only spirit as the entries below, so a future full
regeneration finds this session named rather than silently skipped.
Landed: glossary links across manual Chapters 1, 3–7 (manual-review
dimension 4, re-scored PASS); the citation errata sweep origin-
qualifying every in-fence bare `ADR-0010` verdict-family site to
`tools/ADR-0010`, plus a fourth, previously-unnamed drift family found
and disclosed (bare `ADR-0010` in `components/sim/docs/`/
`components/sim-trajectory/docs/` meaning `sim/ADR-0010`) — not fixed,
out of this session's own touch fence. Every section below still
reflects its LAST full regeneration, 2026-08-08 against tip `a9c3abf`
— nineteen ADRs' worth of landings since (0090 through 0126, excluding
citation-only entries) are NOT reflected below and every `[V]` tag
below should be read accordingly; a full regeneration is still owed at
a session that rules it.

**CITATION-ONLY update, 2026-08-11, the injuries arc's own close
(`notes/adr/0107-injuries-arc-close.md`) — this citation moves here
from ADR-0097, CONTENT NOT RE-PROBED.** `notes/adr/0107-injuries-arc-
close.md` is named `*-arc-close.md` (the file's own name follows this
repo's own vendoring-arc-close/player-arc-close/etc. naming
convention for a multi-ADR thread reaching its own conclusion —
ADR-0070 → ADR-0105 → ADR-0106 → ADR-0107 — not a claim that this is
one of the repo-wide arcs the sections below track) and is therefore
the newest file the staleness tripwire's own regex tracks, tripping
`state_staleness_tripwire_test.clj` red at this close's own full-suite
run against this file's PRIOR citation (ADR-0097). This session's own
driving prompt scoped it to the injuries closure alone (an interpreter
fix plus a vendoring batch, `notes/adr/0107-injuries-arc-close.md`'s
own Fences section) and named no state.md ruling — the SAME class of
gap the ADR-0097 citation-only update below already named and fixed
forward, not a silent evasion of it. The tripwire's own docstring
states it "checks CURRENCY... not CONTENT" — this update satisfies
exactly that narrow contract, the citation sentence only, landed in
this close's own close-phase commit. Every section below still
reflects its LAST full regeneration, 2026-08-08 against tip `a9c3abf`
— TEN ADRs' worth of landings since (0090 through 0107, excluding
this citation-only chain itself) are NOT reflected below and every
`[V]` tag below should be read accordingly; a full regeneration is
still owed at a session that rules it, the same way AR-QC-3/AR-CB-1
each did for their own arc.

**CITATION-ONLY update, 2026-08-09, the review-2 arc's own close
(`notes/adr/0097-review-2-arc-close.md`) — this citation moves here
from ADR-0089, CONTENT NOT RE-PROBED.** `notes/adr/0097-review-2-arc-
close.md` is named `*-arc-close.md` and is therefore the newest file
the staleness tripwire's own regex tracks, tripping
`state_staleness_tripwire_test.clj` red at that close's own full-suite
run against this file's PRIOR citation (ADR-0089). That close's own
driving prompt scoped it narrowly (docs-only, "nothing else moves"
beyond two named roadmap rows and the ADR itself) and named no
state.md ruling — a genuine, disclosed gap against this file's own
standing regeneration contract (AR-C-1: "every `[V]` claim
re-probed... at each arc close"), not a silent evasion of it. The
tripwire's own docstring states it "checks CURRENCY... not CONTENT" —
this update satisfies exactly that narrow contract, the citation
sentence only, fix-forward, landed in that close's own Step 4 commit
(never amending its Step 3 commit, this repo's own standing practice).
Every section below still reflects its LAST full regeneration,
2026-08-08 against tip `a9c3abf` — six ADRs' worth of landings since
(0090 through 0097) are NOT reflected below and every `[V]` tag below
should be read accordingly; a full regeneration is now owed at a
session that rules it, the same way AR-QC-3/AR-CB-1 each did for their
own arc. Named in `notes/adr/0097-review-2-arc-close.md`'s own dated
append as this close's own newly-found watch item.

Content last regenerated by the design channel 2026-08-08 at the
conviction arc's own close (see `notes/adr/0089-conviction-arc-close.md`,
AR-CB-1) — **this citation moved there from ADR-0084 in that exact
commit**, Step 2
(commit `0d7140d` landed every section below already-fresh while
deliberately holding this sentence at its PRIOR value, ADR-0084, the
newest `*-arc-close.md` file on disk at that commit boundary, because
the staleness tripwire (`state_staleness_tripwire_test.clj`, ADR-0079's
own gate) checks COMMITTED state and this arc's own closing ADR file
did not exist on disk until now; this Step 2 creates that file and
updates this citation in the same commit, closing the sequencing rather
than evading it — the exact ADR-0080/0084 Step 2/Step 3 precedent,
re-exercised across this close's own two-session split). Landed against
tip `a9c3abf`
(session A's own closing ceremony commit — the most recent real commit
on disk when this session's own probes ran) — every `[V]` claim below
was re-probed against the live tree THAT session before landing;
nothing carried forward on the prior version's own authority (this
file's own regeneration contract, ADR-0047 AR-C-1, restated
`.agents/rulings.md`).
(session A's own closing ceremony commit — the most recent real commit
on disk when this session's own probes ran) — every `[V]` claim below
was re-probed against the live tree THIS session before landing;
nothing carried forward on the prior version's own authority (this
file's own regeneration contract, ADR-0047 AR-C-1, restated
`.agents/rulings.md`). The prior version was authored 2026-08-08
against tip `e7961b9` (fidelity arc close, ADR-0084) — everything below
supersedes it. Regenerated at each arc close (standing rule,
unchanged). Provenance tags: `[A]` author-ruled, `[C]` channel-inferred,
`[V]` probe-verified at landing.

## What this repo is

Clojure/Polylith workspace generating deterministic synthetic hospital
traffic (Synthea GMF at pinned commit `7e08387c68a7f0e21d13076609a159fd473fc902`),
injecting defects, judging conformance (HL7v2/FHIR). Formats are
emitters of the patient state machine `[A]` (`notes/adr/` index:
ADR-0043's own "sibling emitters over one state machine" doctrine).
Book repo `ehr-testing-guide` is separate, permanently out of scope
(ADR-0001 R2). A stranger can watch that traffic breathe as a bed board
(`ehrt play PATH --board N`, player arc, ADR-0066–0067), or generate
and replay a demo from a top-level operator front door (`demos/`,
demos front door, ADR-0073), without reading a line of this project's
own source. **Twenty-five** Synthea GMF ailments are now vendored
in-tree at the project's own pinned commit, byte-verbatim,
NOTICE-hashed, and gate-verified on every test run (vendoring arc,
ADR-0069–0074, joined this arc by `colorectal_cancer.json`, colorectal
payoff, ADR-0087). **New this arc (conviction, ADR-0085–0089):** the
compile layer's own first semantics change since Wave H — a
straddling encounter (opened pre-horizon, closed and/or containing
clinical content post-horizon) now drops whole instead of leaking
post-horizon content with no matching admission, `compile-trajectory`'s
legacy pre-horizon gate generalizing the same `history-phase?`
back-reference principle the history-mode path already had — closing
this repo's second-oldest live vendoring deferral and, alongside it,
the mutate↔judge pairing registry lands as data (`ehrt.judge.pairing`):
seven witnessed rows, a names-only NIST taxonomy snapshot, two new
gated tiers.

## Component graph `[V @a9c3abf]`

18 `components/*` directories, 1 `bases/*` directory (`ls -d
components/*/ | wc -l` → 18, `ls -d bases/*/ | wc -l` → 1) — HELD
unchanged since the vendoring arc's own close. **The conviction arc
added no brick and no new require edge** — fresh `git log
45eb2f4..HEAD --name-only` shows zero new component/base directories
and zero new `deps.edn`/`interface.clj` COLLISION (`judge-v2-nist/
deps.edn` gained a `"resources"` entry on its own existing `:paths`
vector, its first resources directory, not a new dependency edge).

**New this arc: `straddle-open?`**
(`components/sim-trajectory/src/ehrt/sim_trajectory/compile_trajectory.clj`,
straddle fix, ADR-0086) — a compile-time fold state, the SAME
"one in-flight span, encounters never nest in this project's own GMF
subset" invariant `gmf-interpreter/mark-phase`'s own `open-phase`
already uses at interpreter time, generalized to the legacy
(`history?` false) compile path: the moment a raw-pre-horizon
`:encounter` is dropped, every subsequent event — regardless of its
OWN raw `:pre-horizon` — inherits the SAME pre-horizon disposition
until the matching `:encounter-end` closes the span. `:suppressed-
straddle-spans` joins `compile-trajectory`'s return map as a purely
additive key (spans counted, not events — a synthetic two-observation
span counts `1`, not `2`) — the second suppression counter this
repo carries, alongside `:suppressed-encounter-ends` (interpreter
layer, EncounterEnd fix, ADR-0082); `engine.clj`'s own `:registered`
decide method calls `compile-trajectory` directly, so unlike the first
counter this one is reachable through the same `engine/run` population
a round-trip test already exercises. History-mode soundness confirmed
empirically (zero unsound walks across all `history? true` oracle
roots) — the fix stays legacy-path-only by design, not by omission.

**Also new this arc: `ehrt.judge.pairing`**
(`components/judge/src/ehrt/judge/pairing.clj`, pairing registry,
ADR-0088) — schema (`PairingRow`/`Registry`), loader (`load-registry`),
and a pure coverage helper (`coverage`), backed by a committed EDN
resource (`components/judge/resources/judge/pairing-registry.edn`, 7
rows) and a names-only NIST taxonomy snapshot
(`components/judge-v2-nist/resources/judge-v2-nist/taxonomy.edn`, 7
classifications/52 categories). `ehrt.judge.interface` gained five new
re-exports (`PairingRow`, `PairingRegistry`, `PairingJudgeId`,
`load-pairing-registry`, `pairing-coverage`) — no collision with any
existing export; `open-encounter-index` and the EncounterEnd fix
(fidelity arc) carry over unchanged, both HELD.

`sim-engine` (engine/churn/order-profiles) ← {`sim-check`,
`sim-emit-fhir`, residual `sim` (run/identifiers/version/manifest-
build/façade), `oracle`} — HELD, still exactly five external callers.
`corpus` → `sim-emit-hl7` (player board's own edge) — HELD, unchanged.
`sim-emit-hl7/emit_hl7.clj`'s own four-require `ns` form — HELD,
unchanged. `provenance` ← {`corpus`, `sim`}, forbidden forever from any
`ehrt.*` require beyond itself — HELD, still gated
(`ehrt.docs-tooling.provenance-leaf-law-test`). Façade
`ehrt.sim.interface`'s surface — still `[A]`-frozen (AR-M4-3),
mechanically enforced (`interface_surface_test.clj`) — HELD, no
`sim`-façade edit this arc.

**Resource nesting, closed and gated, HELD unchanged.** The seven
`components/*/resources` directories workspace-wide grows to EIGHT this
arc (`judge-v2-nist` gains its first), each still nesting under its
own brick name. Gated: `ehrt.docs-tooling.resource-nesting-test`, no
allowlist.

## Vendored module inventory `[V @a9c3abf]`

**Twenty-five modules, twenty-nine oracle roots** (fresh `ls
components/sim/resources/sim/modules/*.json | wc -l` → 25; fresh count
via `bin/regression-oracle`'s own self-bracket → 29), up from
twenty-four/twenty-eight at the fidelity arc's own close — **one module
joined this arc**: `colorectal_cancer.json` (colorectal payoff,
ADR-0087), vendored verbatim from the pin-verified checkout, its own
oracle root (`colorectal-pair`) a FIRST BASELINE, purely additive
(`bin/regression-oracle eb4b339 34305d9 --declared-digest-change`: one
added line, zero changed/removed among the 28 pre-existing roots).
`components/sim/resources/sim/modules/NOTICE` holds **71 provenance
rows** (fresh `grep -c '^| \`'` — the correct pattern; a naive
`'^\| \`'` shell-escaping variant over-matches prose elsewhere in the
file and must not be reused — up from 70, one new row, the colorectal
entry).

**The colorectal thread, CLOSED this arc — four ADRs, three sessions
after its first deferral.** Deferred whole at vendoring batch 3
(ADR-0072) on a diagnosis-by-adjacency (the shared `anemia/anemia_sub.
json` submodule); that diagnosis overturned by the fidelity payoff's
own trajectory scan (ADR-0083), which found colorectal's own
violations byte-identical pre/post the EncounterEnd fix and named its
own true, still-undiagnosed blocker: `:clinical-content-only-when-
admitted` (plus one early `:discharge-follows-admission`). The
colorectal investigation (ADR-0085, diagnosis-only per its own ruled
fence) localized the real mechanism: `compile-trajectory`'s own LEGACY
`:pre-horizon` drop gate tests only an event's own raw flag, with no
back-reference check against the encounter it belongs to — a straddling
encounter (opened pre-horizon, dropped; closed and/or containing
clinical content post-horizon, compiled normally) produces compiled
clinical-content and terminal-discharge steps with no matching compiled
admission step, confirmed across 100% of the violating population (2 of
2 distinct patients, both seeds). The truncation hypothesis ADR-0082's
own AR-EE-1a raised is CONFIRMED but NARROWER than first stated: the
`:pre-horizon` gate is the real mechanism, in a straddling-encounter
shape that finding never itself exercised; `encounter-closed?`'s own
single-encounter scope plays no defective role (this narrower framing
supersedes ADR-0082's own broader one). The straddle fix (ADR-0086)
generalized the Wave H `history-phase?` back-reference principle to the
legacy path — the blast-radius probe (all 28 pre-existing oracle roots,
22 in the legacy compile path) found ONE licensed mover, `sleep-apnea`
(3 of 300 walks, a latent malformed compiled shape — a dangling
`:outpatient-visit-end` with no matching admission — already shipped
since vendoring batch 1, invisible to byte-identity oracle checks until
this session's own straddle-freedom sweep, the first any of the 28
roots ever received), confirmed exactly at the predicted 3-of-300
granularity, licensed as the new, more-correct standing baseline; the
other 27 roots stayed byte-identical. `colorectal_cancer.json` clean
(`:status :ok`, 0 violations) at all three seeds post-fix. The
colorectal payoff (ADR-0087) vendored it as the 29th oracle root,
pinned by a committed test (`vendored_colorectal_test.clj`) asserting
both the clean round trip AND the `:suppressed-straddle-spans` counter
(1/0/1 across seeds 20260802/1/42, measured via `with-redefs`
interception against the SAME real straddling patients the
investigation traced by name, not a re-derived population — a first,
undercounting synthetic-sweep attempt disclosed rather than discarded,
see `notes/adr/0087-colorectal-payoff.md`'s own Measurement section).

**The two-module `EncounterEnd` blocker — fully closed, both modules now
home.** `anemia___unknown_etiology.json` vendored clean at the fidelity
arc's own close (ADR-0083); `colorectal_cancer.json` vendored clean this
arc (above) — the interpreter gap ADR-0071/0072 first found and the
compile-layer gap this arc diagnosed and fixed were, in the end, two
genuinely distinct defects that happened to share a submodule, not one
gap wearing two names.

## Where history lives `[V @a9c3abf]`

* `notes/ADRs.md` = INDEX; entries at `notes/adr/NNNN-slug.md`. **File
  count: 86**, confirmed (`ls notes/adr/*.md | grep -v README | wc -l`)
  — this ADR-0089's own file will make it 87 once it lands in Step 2,
  the same staleness-at-count-instant pattern every prior regeneration
  has named.
* Roadmap = live map only; Done history at `.agents/plans/
  roadmap-done-2026-07.md` (0 entries, unchanged) and `roadmap-
  done-2026-08.md`. **This session's own rotation (AR-CB-3), landing in
  the same commit as this file:** ADR-0084's own pointer — left in
  place as the live roadmap's sole current entry from the fidelity
  arc's own close until now, the disclosed-leftover class every prior
  close has handled for its own predecessor — relocates into the
  attic's EXISTING `## Fidelity arc — closed 2026-08-08 (ADR-0081–0084)`
  section with a dated append note. A new `## Conviction arc — closed
  2026-08-08 (ADR-0085–0089)` header holds ADR-0086/0087/0088's own
  three Done pointers, relocated verbatim — ADR-0085 (diagnosis-only)
  never carried a Done pointer of its own, named in the section's own
  prose instead. `.agents/plans/roadmap.md`'s own Done section holds an
  HTML-comment sentinel (not a pointer) recording that ADR-0089's own
  pointer is deferred to Step 2 — the same dangling-reference
  sentinel-avoidance every prior close (ADR-0055/0064/0068/0074/0080/
  0084) has disclosed. Deferred rows: **13, HELD unchanged** — both the
  EncounterEnd row and the colorectal row this arc closed did so IN
  PLACE, with disclosed relocation notes (the `roadmap-deferred-
  closure-lint-test`'s own compliant shape, the `myocardial_
  infarction.json` precedent), not by removal, so the live bullet count
  is unchanged even though both are now substantively closed. Next
  rows: **10, DOWN from 11** — ADR-0086 added a "Colorectal vendoring
  payoff" row mid-arc, ADR-0087 removed it on execution, and ADR-0088
  removed the "Pairing-as-data" row on execution — net −1, the first
  DECREASE any close has recorded for this count (every prior arc grew
  it).
* Session narrative hierarchy `[A, AR-B-4]`: unchanged — ADR execution
  record = sole narrative; session record = ceremony log; roadmap Done
  = one-line pointer; prompt archive = provenance.

## Standing gates `[V @a9c3abf]`

`components/docs-tooling/test/ehrt/docs_tooling/` holds **27 test
files**, confirmed live (`ls ... | wc -l`) — HELD unchanged since the
quality-review arc's own close; the two new gates this arc land OUTSIDE
docs-tooling (below), the same "interpreter/registry behavior, not a
repo-hygiene lint" class the EncounterEnd fix's own tests were.

**Two new gates this arc, both witnessed RED before GREEN in-session:**
`taxonomy_currency_test.clj` (`components/judge-v2-nist/test/`,
pairing registry, ADR-0088) — re-derives the NIST engine's own
classification/category vocabulary from the resolved jar's own
`reference.conf` on every run and fails on drift; also cross-checks the
registry's own NIST-judge rows against the snapshot. `pairing_
conviction_test.clj` (`components/judge/test/`, tier-one, ADR-0088) —
one executable test PER registry row: load the fixture, apply the
operator, run the judge, assert at least one `:expected` class among
the observed findings — a future engine upgrade or catalog edit that
silently changes what a mutation actually convicts now breaks a
committed test, not a stale sentence in an operator's own `:contract`
prose.

**The façade surface-identity gate** (`interface_surface_test.clj`,
`components/sim/test/`, AR-LF-2) — HELD, unchanged.

**Local docsgen-currency gates** (`cli-md-is-current-test`,
`operators-md-is-current-test`) — HELD, unchanged; `docs/dev/
pipeline.md`/`docs/use-cases.md` stay CI-only, unchanged.

**Oracle: 29 roots, fresh-confirmed IDENTICAL** (`bin/regression-oracle
a9c3abf a9c3abf`, this session's own Step 0 pre-digest — soundness "yes
outside ns form"), up from 28 at the fidelity arc's own close — the
colorectal payoff's own new `colorectal-pair` root (ADR-0087), purely
additive; every pre-existing root stayed byte-identical across the
entire arc EXCEPT `sleep-apnea` (the straddle fix's own ONE licensed
mover, ADR-0086 — confirmed at the exact 3-of-300-walk granularity the
license bound it to, the new digest recorded as the more-correct
standing baseline, not merely a different one).

**Vendored round-trip family: 29 files** — up from 28, the new
`vendored_colorectal_test.clj` (ADR-0087). Full family, both bricks:
22 in `components/sim-emit-hl7/test/` (engine-layer round trips), 7 in
`components/sim-trajectory/test/` (interpreter-batch walks) — fresh
`find . -iname "vendored_*test.clj" | wc -l` confirms 29 workspace-wide.

`bases/cli/test/ehrt/cli/` holds 7 files — HELD, unchanged; this arc
touched no CLI surface. **Reading-set budgets: TWO sets move this
close (AR-CB-2) — the first time more than one set has moved since the
quality-review arc's own "all five together" regeneration.**
`:onboarding` (roadmap.md's own Now/Done churn across five
conviction-arc sessions plus this close's own rotation, plus the
prompts/session-records READMEs' own new entries): actual 1274 (up from
1216), budget 1400 → **1470**. `:judge` (`components/judge/src/ehrt/
judge/interface.clj` grown by the pairing registry's own five new
re-exports, ADR-0088 AR-PD-4 — the first time `:judge` has moved since
the quality-review arc's own close): actual 914 (up from 901), budget
1040 → **1055**. `:corpus`/`:sim`/`:docs` carry no touched member this
arc (the arc's own src/test edits touched `sim-trajectory`,
`sim-emit-hl7`, `oracle`, `judge`, `judge-v2-nist` — none of them a
`:paths` member of those three sets) — all three budgets HELD.
Structure-currency; index-completeness; stale-path tripwire;
prompt/record pairing; done-pointer→ADR; readme-presence;
skill-mirror-currency; docsgen/pipeline/usecases/quickstart-fresh/lint;
notes-prompts-frozen; tag-law; invocation-lint; io-vocabulary-lint;
state-staleness-tripwire; roadmap-deferred-closure-lint;
test-source-live-path-lint; resource-nesting; provenance-leaf-law; the
five alignment-arc gates — all HELD, unchanged, none touched this arc
beyond what's named above.

## The quality-review arc's own instrument `[V, HELD since @9eb7da9]`

The `repo-review` skill and its first survey's own dated register
(45 disposition-carrying rows, 28 close-as-fine / 9 fix-session-
candidate EXECUTED / 5 ruling-needed RULED / 3 intake — 1 executed, 2
still open) — unchanged this arc, no review 2 has run yet. See
`notes/adr/0080-quality-arc-close.md` for the full account; restated
here only as still-live context, not re-derived (this arc's own review
re-probe would be review 2's job, not a docs-only close's).

## The pairing registry `[V @a9c3abf]`

`ehrt.judge.pairing` (ADR-0088) — PER-OPERATOR WITNESSED ROWS, never a
matrix: a row lands only when the mutate→judge loop was actually
exercised in-session; unwitnessed cells do not appear. **Seven witnessed
rows this arc, v2 only (AR-PD-2's own fence — no FHIR rows yet):** all
five v2 operators against `judge-v2-hapi` (the ADT trio, one row
disclosed for the trio-wide-identical result), two of five against
`judge-v2-nist` (`:blank-required-field`, `:truncate-segment-fields`,
both landing a NEW `structure/Usage` category against the profile's own
473-finding noise floor). **Three NIST pairs SKIPPED, honestly named,
not forced:** `:corrupt-encoding-characters`/`:corrupt-segment-name`
(the NIST parser throws before any `Report` entry exists — the verdict
is right, there is no finding-level class to witness); `:malformed-
datetime-value` (the mutation's own effect is masked by the profile
bundle's own pre-existing 473-finding noise floor). None is a catalog
contradiction — AR-PD-5's STOP-AND-REPORT trigger never fired. Taxonomy
snapshot: **7 classifications, 52 categories**, committed at
`components/judge-v2-nist/resources/judge-v2-nist/taxonomy.edn`,
gated by its own currency test (above). Tier two (report-only,
does NOT gate): a computed coverage table over the live v2 operator
catalog — three cells "not witnessed" (the three skips above), the
five FHIR operators out of scope entirely this arc. Promoting tier two
to a gate is explicitly deferred to a future dated ruling, once FHIR
rows exist. Landing spot for FHIR rows: the storefront-fixture session
(roadmap's own existing Next row, already updated to name this).

## Live work `[V @a9c3abf]`

* Deferred = LIVE rows only (**13**, HELD — see Where history lives,
  above, for why the count doesn't drop despite two closures).
* Next (roadmap, **10** rows, DOWN from 11 — see Where history lives,
  above).
* **The sibling-flake SOAK** (`merge-config-file-suggests-a-same-stem-
  sibling-file`, ADR-0076's own fix, landed `9cc3563`), fresh count
  this close: **40 `test`-workflow push runs on `main` since `9cc3563`
  landed** (fresh `gh run list --json` enumeration, filtered to
  `createdAt` after the fix commit's own timestamp, `9cc3563` through
  this session's own `a9c3abf`), **zero recurrence of the named test**
  — the SAME two unrelated failures already disclosed at the fidelity
  arc's own close (`ac6ef5f2`, index-completeness-test, ADR-0077-related;
  `deabbbdb`, roadmap-deferred-closure-lint-test, ADR-0082-related),
  no new failure in the fifteen runs landed since. Forty runs, still
  not declared closed (the fix's own target was never "N clean runs,
  then stop counting") — carried to review 2 as the next re-probe
  point.
* **The engine `defspec` flake — pinned, not soaking:** `every-churned-
  run-satisfies-the-invariant-catalog` runs at a fixed `{:num-tests 150
  :seed -60645}` (confirmed live, fresh `grep`) — HELD, unchanged.
* **The `mutate-stdout-stdin-loopback-test` flake, named at the
  fidelity arc's own close, HELD unchanged, not re-investigated this
  arc (out of this docs-adjacent close's own fence):**
  `ehrt.conformance.mutate-stdout-stdin-loopback-test`'s own
  `^:integration`-tagged deftest, a load-sensitive subprocess-piping
  flake under heavy concurrent JVM load — did NOT fire in this session's
  own full-suite run (confirmed clean, fresh `grep`), consistent with
  its own load-dependent nature, not evidence it's gone. Named again
  for the next session that owns test-suite hygiene.
* **CI posture, in full, this close:** preflight discloses the last
  FIVE runs' conclusions — all green at this session's own Step 0 (see
  the session record). Commits land green — no push this arc carried a
  knowingly-failing test.
* **D2-7's own deferred scaffold** (the generalized multi-surface-law-
  drift registry): DEFERRED with a named trigger, unfired — HELD,
  unchanged.
* **D6-1's census `:closure-file-count` undercount, escalated, still
  open:** `sim-trajectory/census.clj` still counts JSON modules only,
  never CSV lookup tables — HELD, unchanged, no session touched it this
  arc.
* **Wellness-encounters** — HELD, restated unchanged; no session
  touched it this arc.
* **Vital-sign channel row, HELD unchanged** — `congestive-heart-
  failure`/`contraceptives` both `:produces-content` post-Wave-VS;
  `covid19` alone `:zero-on-every-seed`, still fully blocked.
* **`notice_verbatim_test`'s own coverage gap, HELD, still open** — the
  v2-nist `NOTICE.md` table (2-column) and the simhospital
  `PROVENANCE.md` hash (prose) both sit outside the gate's own
  recognized shapes; both hashes still manually verified correct.
* **Census artifact set, HELD unchanged:** ten `.edn` files under
  `components/sim-trajectory/docs/census/` (fresh `ls`) — no census
  re-run this arc (diagnosis/fix/vendoring sessions, not a census
  session).
* Externals (author-only), unchanged this arc, all six rows restated:
  the GitHub workflow-failure notification-email toggle (still
  unconfirmed); NIST licensing inquiry (narrowed, not resolved — the
  pairing registry's own taxonomy snapshot names but does not settle
  it, ADR-0088 AR-PD-3); IG pinning (still open); Clojars publish
  (ruled, deferred; F-5/F-6 remain open); SETUP rewalk (still owed);
  `/mnt/c` disposition (closed, unchanged).
* **Both audit registers' pointers, restated unchanged, neither touched
  this arc.** Alignment audit register (54 rows, FINAL per ADR-0055).
  UX audit register (21 rows, FINAL per ADR-0064).

## Environment `[A, 2026-08-08]`

Claude Code project root = UNC path to the ext4 clone, unchanged, no
new incident this arc (working directory confirmed `~/src/ehr-testing-
tools` at this session's own Step 0).

**Tag mechanic, HELD unchanged (tag law, ADR-0057 AR-T-1/AR-T-2).**
**`stable-*` tag census, fresh (`git tag -l 'stable-*'`, excluding the
three frozen legacy tags): 42 live**, up from the fidelity arc's own
36 — six new this arc, each one session's own tag-law case-(ii)
execution for its own predecessor's verified stable point: `-fidelity-
close` (ADR-0085's own AR-CI-0), `-colorectal-investigation` (ADR-0086's
own AR-SF-0), `-straddle-fix` (ADR-0087's own AR-CP-0), `-colorectal-
payoff` (ADR-0088's own AR-PD-0), `-pairing-registry` (conviction close
A's own AR-CA-0), `-conviction-appends` (this session's own AR-CB-0,
executed Step 0).

**The founding incident, still SIX guarded failures — HELD, unchanged.**
This arc touched no CLI/help-flag surface; all six mechanisms stand
unre-probed-for-regression by this session's own full-suite green run,
not independently re-walked live.

**CLI surface, HELD unchanged.** No new flag landed this arc.

**Full suite posture, fresh this session (Step 1 run against this
session's own working tree, roadmap/reading-sets edits only, no
src/test touch):** `clojure -M:poly test :all skip:integration` —
**283** `Testing ...` namespace announcements (up from 275 at the
fidelity arc's own close), **566** project-block "0 failures, 0
errors" confirmations (grepped across the full run output, not
sampled), zero other fail/error lines, exit code 0 — matching
ADR-0088's own precedent count exactly (project-block granularity is
insensitive to how many assertions a namespace gained this arc). The
disclosed `mutate-stdout-stdin-loopback-test` flake did NOT fire this
run.

**NIST supply-chain posture, `workspace.edn`, HELD unchanged** — both
untouched this arc.

**This arc's own pointers.** `notes/adr/0085-colorectal-investigation.md`
(the diagnosis, no fix), `0086-straddle-fix.md` (the fix, the
STOP-AND-REPORT, the license, the erratum on ADR-0082), `0087-
colorectal-payoff.md` (the 29th root), `0088-pairing-registry.md` (the
registry, the taxonomy snapshot, the three honest skips). This arc's
own close record is `notes/adr/0089-conviction-arc-close.md` (landing
Step 2).

## Design-channel contract (how sessions run)

Two channels: design (chat) plans/verifies vs public clone; Code
executes pasted prompts, self-archives, R30 ceremony. Evidence over
ruling — live probes have overturned survey rows, channel claims, and
the author's own summaries repeatedly across every arc, and this arc
added two more instances: ADR-0072's colorectal diagnosis-by-adjacency
(inherited from the fidelity arc, finally overturned by this arc's own
localization); `sleep-apnea`'s own latent malformed compiled shape,
shipped since vendoring batch 1 and invisible to byte-identity checks
until this arc's own first-ever straddle-freedom sweep. Continuity: new
design sessions read THIS file + roadmap + latest ADR execution record
— structural claims in any continuity prompt carry citations or an
explicit `[unverified]` tag `[A, AR-B-5]`.
