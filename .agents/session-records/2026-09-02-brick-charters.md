# 2026-09-02 — brick charters: every brick gets its one-page contract

**Ceremony:** R30 standing default (commit and push at each checkpoint).
**Fence:** docs only — no src, no interface edits, no renames. Held.
**Driving prompt:** [`.agents/prompts/2026-09-02-brick-charters.md`](../prompts/2026-09-02-brick-charters.md).

## What landed

Twenty charters, one page per brick, at
`components/<brick>/docs/charter.md` and `bases/cli/docs/charter.md`,
in the author-agreed six-part shape (mission / interface contract /
data shapes owned / invariants guaranteed / non-goals / forbidden
edges). Plus [`docs/charters.md`](../../docs/charters.md), the index,
and `bin/charter-completeness`, the gate.

| commit | contents |
|---|---|
| `f25657a` | the gate, deliberately **red** over all 20 bricks |
| `13d3db2` | batch 1 — engine family (kernel, sim-model, sim-engine, patient-simulator, person-simulator) |
| `f39a689` | batch 2 — emitters and the sim façade |
| `9267411` | batch 3 — corpus, transport, judge family |
| `4b5438f` | batch 4 — docs-tooling, oracle, cli base; **set complete** |
| `60dcfb9` | the index, and the gate extended to cover it |

**Census (step 1's gate):** 20 bricks — 19 components + the `cli` base
— carrying **273** public interface vars. Final gate state:

```
bricks checked: 20   interface vars checked: 273
charter completeness: OK
```

## The gate, and the reading it encodes

The prompt said every interface var must appear in "exactly one
charter". Taken **globally** that is wrong and the tree says so:
fourteen var *names* are carried by more than one brick (`ok`, `ok?`,
`rejected`, `rejected?`, `error`, `valid?`, `lookup`, `check-all`,
`gate-dir`, `gate-file`, `ManifestV1_1`, `message-patient-id`,
`message-timestamp-ms`, `message-type-trigger`). So the gate is **per
brick**: every var on brick B's interface heads exactly one bullet in
B's own charter, and nothing else.

**Bidirectional**, the reverse half being the "no invented promises"
fence mechanised: no charter bullet may name a var its brick does not
export. **Anchored** to `- \`name\`` at line start — ADR-0162's own
lesson paid forward, since a bare substring matches wherever it is
pasted. Matching uses `awk index()` rather than regex because var
names carry `?`, `!`, `*`, `+`, `.`, `<`, `>` and `-`.

The gate earned its keep three times:

1. **INVENTED**, batch 2 — `sim-emit-fhir`'s charter headed a bullet
   with `snapshot-at`, an internal deliberately unexported. The
   forward half could never have seen it.
2. **DUPLICATED ×2**, batch 3 — `OperationManifestV1` and
   `UnionResource` named at bullet head in both §2 and §3.
3. **A bug in the gate itself**, batch 4. `ehrt.oracle.interface`'s
   whole surface is `-main`; the reverse check ran `grep -qxF "$c"`,
   so grep read `-main` as options, died *"invalid max count"*, and
   reported a **real var as INVENTED**. Fixed with `--`, and the
   reason is now a comment in the script. The last brick of the last
   batch was the only one that could have found it.

## Findings — the author's review queue

### Stale docstring (found, not caused)

**ADR-0172 ruling F1, on `ehrt.person-simulator.interface`**, still
says: *"The component lands ALONE: nothing in this workspace calls it,
and nothing may until arc 3's fold."* The call sites say otherwise —
`components/sim/src/ehrt/sim/run.clj` requires it at `:36` and calls
it three times: `initial-persona` at `:206`, `persons` at `:212` and
`:216` (the two-pass deaths fold). Arc 3's fold happened; the sentence
outlived its condition. **Not fixed** — `interface.clj` is source and
the fence is docs-only. Recorded in that brick's charter.

### Docstring count drift

`ehrt.corpus.interface` says the former `ehrt.tools.interface`'s 64
defs *"became 38 here"*. The file now carries **44**. The sentence is a
dated, historically true statement about what stage 3 landed, and every
addition since is individually annotated (`board-*`/ADR-0067, the three
P3-6 parity mounts, `mutate`) — but its tense reads as present.

### The sharpest structural question, raised on three charters as one

`judge`'s stated mission is the verdict vocabulary **every** engine
reports in. But only `judge-fhir-official` requires
`ehrt.judge.interface` in `src` (`fhir.clj:31`). `judge-v2-hapi`
(`v2.clj:42`) and `judge-v2-nist` (`v2.clj:52`) require **only
`kernel`**. Either those two return a plainer shape that something
above lifts into a `Report` — making "the vocabulary every engine
reports in" an aspiration rather than the wiring — or they build
Reports from plain data with **no gate holding them to the schema**.
Verified by reading all three `ns` forms. (UNCLEAR-VH3 / VN3.)

### Every UNCLEAR raised, verbatim by id

Twenty-six, across seventeen charters. Listed by brick so the author
can review by cluster; each is stated in full on its own charter page.

- **kernel** — K1 `lookup` has no docstring and returns a bare value,
  not a result envelope. K2 the bare `valid?` is `result/valid?`,
  though `locator` and `invocation` also define one.
- **sim-model** — M1 `valid-facility?`/`explain-facility` are
  referenced **only by their own test** and are not on the seam, yet a
  facility *is* user-supplied config and the five ARC-4 profile
  families were seamed expressly for fail-fast. M2 the bare `valid?`.
- **sim-engine** — E1 `compile-patient` is documented twice,
  differently, and neither docstring is the whole promise. E2
  `newborn-id-tag` is exported with no caller, so nothing fails if its
  contract drifts.
- **patient-simulator** — P1 the scope sentence now lives in three
  places and the gate checks `limitations.md`, not the charter. P2
  `dob-epoch-day` is on the seam for a *tool's* convenience.
- **person-simulator** — N1 who owns `PersonEvent`'s schema, given
  that the validator for this brick's output lives in its consumer.
- **sim-emit-hl7** — H1 the two emitters take different edges to the
  same contract. H2 `emit` and `emit-wire` overlap and neither is
  marked the successor.
- **sim-emit-fhir** — F1 why this emitter requires `sim-engine` in
  `src` and its sibling does not. F2 is `bundle-run` the whole
  capability, given no second clock on the FHIR side.
- **sim** — S1 the façade forwards five of `kernel`'s seven result
  vars; note AR-M4-3 freezes it against *added* vars, so resolving is
  not a free edit. S2 where the operator-facing config vocabulary is
  canonically defined, given `config-keys` is the engine's.
- **corpus** — C1 the def-count drift above. C2 `mutate` is the one
  unannotated capability. C3 eight capability families under one
  interface (see the seam note below).
- **corpus-io** — IO1 three names reachable at two seams; **not** a
  drift hazard (the player's are true delegations,
  `player.clj:44-47`), a navigational one. IO2 the bare `lookup` is
  the *framing* registry's.
- **judge** — J1 no result-envelope vocabulary on a seam whose whole
  subject is verdicts. J2 `sampling-header`'s name.
- **judge-v2-hapi** — VH1 no `gate-batch`. VH2 no `make-validator`.
  VH3 (above).
- **judge-v2-nist** — VN1 what `make-validator` returns on a bad
  bundle — the one judge constructor that can fail on *configuration*.
  VN2, VN3 (above).
- **judge-fhir-official** — FO1 `gate-batch` exists only here. FO2 the
  verdict cache is reachable by one engine.
- **provenance** — PV1 the bare `valid?` names the **frozen** v0 while
  the live v1.1 carries the longest name. PV2 growth was left room but
  no rule for what belongs here rather than with its producer.
- **palgebra** — PA1 the bare `valid?` again. PA2 this brick's design
  document lives in `components/corpus/docs/`.
- **sim-check** — CK1 `order-profiles-config` is on the seam but the
  CLI threads facility and warm-up only, so the fourth argument is
  reachable from a library caller and not from a command line. CK2 the
  catalog is not enumerable from the seam — no counterpart to
  `documented-step-rejection-reasons`.
- **docs-tooling** — DT1 the interface is one var while the real
  contract is thirteen namespaces of gates living in the **test**
  tree. DT2 where a charter gate would live if one is wanted.
- **oracle** — O1 a dev tool's needs shaped a production seam. O2 the
  golden-root set is code and nothing states its membership rule.
- **cli** — CLI1 the simulator is reached through the corpus domain
  (below). CLI2 `check` and `gate` are separate groups.

**A recurring one worth a single ruling:** the bare `valid?` appears
at four seams — `kernel` (= `result/valid?`), `sim-model` (=
`pathway/valid?`), `provenance` (= v0's), `palgebra` (= the
pipeline-level one) — each time the unqualified name among qualified
siblings, and each time a reader at the seam cannot tell which. Four
charters raise it separately; it is one question.

## Two draft claims that were wrong, and were caught by verifying them

Recorded because the near-miss is the useful part, and because both
would have shipped as confident prose.

1. `corpus-io`'s draft UNCLEAR asserted a **drift hazard**: four
   field-reader names duplicated between this brick and
   `ehrt.corpus.player` with nothing keeping them in sync. They are
   **true delegations** (`player.clj:44-47`), so they cannot disagree
   by construction. Rewritten as the navigational question it is.
2. `judge-v2-nist`'s draft said the engine is absent from `bases/cli`'s
   require list. **It is present.** Struck.

## The corpus-brick seam — described, not designed

The charter format made a seam visible that a reader of
`interface.clj` would have to assemble by hand. `corpus` carries
**eight capability families** under one interface, each with its own
ADR and largely disjoint consumers: generate (+ the generator
registry), intake, mutate/operators, check, golden comparison, display
(`ehrt show`), player + board (`ehrt play`), and the sim adapter.

**Priced, as an observation only.** The natural cut is between the
**domain** families (generate/intake/mutate/check/compare — which own
registries and reach `judge` and `provenance`) and the
**presentation** families (display, player, board — which are pure,
have no registry, and whose one named caller is `bases/cli`). The sim
adapter belongs with neither: it is a *mounting* seam, and it is the
reason `corpus` requires `sim` at all.

Three things make this more than a tidiness argument, and all three
are reasons to be careful rather than reasons to act:

- `bases/cli` does **not** require `ehrt.sim.interface`. Every mention
  in `core.clj`/`help.clj` is docstring prose (`core.clj:2375`,
  `:2381`, `:2437`, `:2589`, `:2605`; `help.clj:206`). `ehrt sim …`
  runs **through the corpus sim adapter**. So the adapter is not an
  incidental convenience — it is the live path for a headline command
  group.
- That edge is why `corpus`'s forbidden-edge list cannot include
  `sim`, and transitively **why `provenance` had to be carved at all**
  (`corpus → sim` exists, so `sim → corpus` for the manifest schema
  would be a cycle).
- ADR-0012 makes `corpus` depend on **`ehrt.sim.interface`'s
  stability**, which is the stated reason that façade's width survived
  the entire sim split unchanged.

A split would have to decide where the adapter lands, and that
decision moves the cycle argument. **Not proposed here.**

## Reading-set placement — proposed, not taken

The prompt reserves budget re-triage to the author, so this session
took none. The proposal, for the record:

- Add `docs/charters.md` (**74 lines**) to `:onboarding`, replacing
  nothing. It is the one page that routes to all twenty.
- Add each domain's own charter to its task-class set — e.g.
  `components/corpus/docs/charter.md` to `:corpus`,
  `components/sim/docs/charter.md` to `:sim` — **in place of** that
  brick's `interface.clj`, which the charter now summarises and cites.
  For `:corpus` that trades 151 + 134 = 285 lines of interface source
  for 227 + 215 = 442 lines of charter, so it is **not** free; for
  `:sim` it trades 54 for 160.

Measured actuals at this commit, for whoever does the triage:

| set | actual | budget | baseline | headroom |
|---|---|---|---|---|
| `:onboarding` | 1486 | 1530 | 1530 | 44 |
| `:corpus` | 1889 | 2045 | 2045 | 156 |
| `:sim` | 1347 | 1405 | 1405 | 58 |
| `:judge` | 975 | 1000 | 1000 | 25 |
| `:docs` | 785 | 785 | 785 | **0** |

## The `:docs` budget — a pre-existing breach, cleared

Step 3 asks AGENTS.md for one pointer line. AGENTS.md is a member of
**all five** reading sets, and `:docs` was **already over budget at the
tip**: 787 actual against a 785 budget and a 785 ratchet baseline,
headroom **−2**, matching `.agents/state-derived.md`'s own table as
regenerated at `eff7a0f`. So the previous session shipped the breach.

`.agents/rulings.md#R-budget-stop` says a session over budget compacts
or stops, never bumps. **Author ruling, in session: add the pointer and
compact to pay for it.** Done in one commit — the "Reading this repo"
paragraph went from 10 lines to 8, dropping the sentence about the
deleted `reading-set-budget-test` (which `.agents/reading-sets.edn`'s
own header states at length, and which the compacted text now points
at) and the migration-session-4 provenance, and repairing a 103-column
line that broke the file's own ~72-column wrap. Net **−2**, so `:docs`
lands at **785 — at budget, headroom 0**, and the pre-existing breach
is gone.

Flagged for the author: **headroom 0 means the next line added to any
of those five paths breaches again.** That is a real constraint on the
reading-set proposal above, and the reason it is a proposal.

## Gates and verification

- `bin/charter-completeness` — red witness at `f25657a` (20 MISSING
  CHARTER, 0 vars checked, exit 1); green at the tip (20 bricks, 273
  vars, OK). Red witness again for the index gate before
  `docs/charters.md` landed ("MISSING INDEX", exit 1).
- **`make test` caught a real failure**, and the first run's exit code
  was misread — the background wrapper's `; echo EXIT=$?` reported the
  *wrapper's* success, not `make`'s. Actual: **EXIT=2, 1 failure**,
  `ehrt.docs-tooling.link-footnote-gate-test/no-visible-adr-token-in-prose-test`
  — `docs/charters.md` carried visible `ADR-0162`/`ADR-0172` tokens,
  which ADR-0102's ruling forbids in `docs/` prose. Fixed to the
  sanctioned form: footnote markers with `Design record [ADR-NNNN]`
  definitions. The gate is scoped to `docs/` proper, so the twenty
  component charters — which cite ADRs heavily — are out of scope.

**Suite at the close:** `make test` **exit 0**, 414 blocks / 4,801
tests / 25,607 assertions, 0 failures, 0 errors. `make docsgen`
regenerated `demos/traces/**` **byte-identical** — an independent
witness on a surface the charter work never touched.
- This session made **no regression-oracle claim.** None was owed: the
  fence is docs-only and no `src` byte moved.

## What this deliberately did not do

No `src` edit, no interface edit, no rename, no gate re-pointing. The
two ADR-era charters (`patient-simulator`, `person-simulator`) got the
new format with their ADR cited as ancestry, and their gated
`docs/limitations.md` remains the authority for what those bricks
decline — this session changed neither gate. `bin/charter-completeness`
is a **script, not a test**, deliberately: promoting it into `make
test` would add a namespace that runs twice (development and
`projects/conformance`) plus a `state-derived` regeneration, which is
code, not docs. Registered as `docs-tooling`'s UNCLEAR-DT2.
