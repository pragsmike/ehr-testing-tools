## ADR-0050 — Alignment fixes 1: the past stops leaking — staleness swept, tripwire hardened, conventions named

**Status:** Accepted (author-directed, autonomous session per R30), 2026-08-05.

### Context

Prior: the alignment audit landed and was design-channel-verified
(`989d6cf`, `notes/adr/0049-alignment-audit.md`); its 47-row register
(`.agents/plans/2026-08-05-alignment-audit-findings.md`) came back with
author rulings — yes to the full menu. This session executes the first
ruled cluster: the staleness sweep, the tripwire's widened scope, and
the small documentation-of-convention notes. Everything below is
pre-ruled; nothing was discretionary beyond per-file sweep judgment
(AR-F1-2). R30 ceremony.

### Decision

Ruled 2026-08-05, recorded verbatim (author rulings, this session's own
prompt):

**AR-F1-0 (tag).** Per AR-AU-0's standing mechanic, this session was
licensed to create annotated tag `stable-20260805-alignment-audit` at
`989d6cf`, push, and verify on origin. **Not executed by this
session** — tag creation is AUTHOR ACTION in every ceremony mode
(`AGENTS.md`'s own "Session mode and ceremony" section: "Two classes of
action stay the author's alone under either mode: tags... and
repo-level `gh` mutations"), and this session's own prompt did not
override that standing rule. The exact commands are recorded below for
the author to run directly.

**AR-F1-1 (staleness sweep — register rows A-6, E-3, E-5, E-9).**
Fix-forward with dated notes where a note is warranted. Executed in
full — see "The sweep" below.

**AR-F1-2 (sweep judgment).** Within AR-F1-1's file set: a
CURRENT-TENSE reference to a retired ns updates to the live form; an
EXPLICITLY HISTORICAL sentence keeps the old form. Disposition table
below.

**AR-F1-3 (tripwire hardening — rows S7, E-7).** `stale_path_test.clj`
gains an explicit include-list scope extension and the `ehrt.sim-cli.`
forbidden-prefix addition. Red→green evidence below.

**AR-F1-4 (workspace.edn — rows S3, A-1).** The ~40-line `:necessary`
narrative relocates verbatim into this ADR (below); `workspace.edn`
keeps a two-line pointer + the standing invariant. `"development"`
gains `:necessary ["oracle"]`. Before/after `poly check :dev` evidence
below.

**AR-F1-5 (conventions documented — rows B-1, C-5, D-2, F-5).**
Docstring/annotation-only, no logic changes. Landed in
`430bb5c`..`a9d3bb6` — see the Step 3 account below.

**AR-F1-6 (standing rulings recorded, appended at arc close).**
(a) A-3 — dependency review is report-only `clojure -M:poly libs
:outdated` at each arc close plus mandatory before any publish;
upgrades are never taken as a side effect. (b) D-3 — `judge` is the
accepted landing spot for the pairing-as-data registry; the design pass
starts from there. **Both go to `.agents/rulings.md` at this ARC'S
CLOSE, not this session** — noted here so the close session can't miss
it (per AR-C-2's contract, `.agents/rulings.md`'s own header).

### Step 0 — preflight

Working directory confirmed `~/src/ehr-testing-tools` (ext4, not
`/mnt/c`); tip `989d6cf` exactly. Baseline full suite green (511
assertions across the run's final project block, 0 failures/0 errors,
`clojure -M:poly test :all skip:integration`).

### Step 1 — red evidence, then sweep + tripwire hardening (AR-F1-1/2/3)

**Ordering note, disclosed:** the sweep edits (A-6, E-3, E-5, E-9) were
drafted before the tripwire's own red-evidence capture, which would
have silently lost the "roadmap's framing ns must trip it" proof the
ruling asked for. Recovered by `git stash push -u` (moving all sweep
edits out of the working tree), applying the tripwire widening alone
against the now-unswept tree, capturing red, then `git stash pop` to
restore the sweep edits on top before re-running to green. No commit
was made in the wrong order — this is a working-tree-only correction,
disclosed per fix-forward-with-disclosure rather than left silent.

**Red (widened scope + `ehrt.sim-cli.` addition, unswept tree,
`989d6cf`):**

```
Testing ehrt.docs-tooling.stale-path-test

FAIL in (no-stale-path-family-anywhere-in-docs-or-use-cases-edn-test) (stale_path_test.clj:248)
.agents/plans/roadmap.md carries stale-path residue: [:retired-ehrt-tools-namespace]
expected: (empty? found)
  actual: (not (empty? [:retired-ehrt-tools-namespace]))

Ran 9 tests containing 166 assertions.
1 failures, 0 errors.
```

Exactly the predicted trip: widening `scan-sources` to include
`.agents/plans/roadmap.md` surfaces the pre-sweep `ehrt.tools.corpus.framing`
reference via the ALREADY-existing `ehrt.tools.` forbidden pattern —
proof the gap S7 named was real, not hypothetical, and that the fix
(the sweep, landing in the same commit) actually closes it.

**Tripwire hardening, `components/docs-tooling/test/ehrt/docs_tooling/stale_path_test.clj`:**

- Scope extension: a new `live-agents-plan-files` explicit include-list
  — `.agents/plans/roadmap.md`, `.agents/plans/README.md`,
  `.agents/prompts/README.md`, `.agents/session-records/README.md` —
  joins `scan-sources`. An include-list, not a directory glob or
  exclude-pattern, per the ruling's own instruction: attic files
  (`roadmap-done-*.md`) and dated one-shot files
  (`.agents/plans/2026-*-*.md` and kin, including this arc's own brief
  and findings register) stay out of scope, on purpose — they freeze
  prose at authoring time. Rationale recorded in the test's own
  docstring (2026-08-05 addendum), citing this ADR and register row S7.
- Forbidden-list addition: `ehrt.sim-cli.` joins `ehrt.tools.` (row
  E-7, ADR-0021's retirement, 2026-08-01, never folded in before).

**Green (widened scope + hardened forbidden list, swept tree):**

```
Testing ehrt.docs-tooling.stale-path-test

Ran 9 tests containing 166 assertions.
0 failures, 0 errors.
```

**The sweep — disposition table (AR-F1-2).** Fresh grep re-derived the
file list rather than trusting the register's own estimate (E-5's own
"12 file-hits" turned out to underspeak it): **25 hits across 8 files**
for the five S2/S3-vintage forms
(`ehrt.sim.{gmf,compile-trajectory,emit-hl7,v2-replay,site-profile}`).
Every hit in every file was judged CURRENT-TENSE (a present-tense
description of live architecture, a live test-name citation, or a live
demo-output citation using the retired ns as a name) — **zero
kept-historical** this sweep, consistent with the precedent this test's
own M2–M4 addenda already set for these exact files (their own
docstring: "those were swept forward anyway, live, as part of this same
session's own current-tense-surface discipline, just not gated by this
test").

| File | Hits | Disposition |
|---|---|---|
| `components/sim-emit-hl7/docs/demos/site-profiles/README.md` | 1 | updated (current-tense) |
| `components/sim/docs/sim-theory.md` | 7 | updated (current-tense) |
| `components/sim/docs/third-party-sources.md` | 4 | updated (current-tense) |
| `components/sim/docs/sim-theory.edn` | 6 | updated (current-tense) |
| `components/sim/docs/event-sourcing.md` | 4 | updated (current-tense) |
| `components/sim/docs/demos/README.md` | 1 | updated (current-tense) |
| `components/sim/docs/demos/persona-enriched/README.md` | 1 | updated (current-tense) |
| `components/sim/docs/demos/order-result/README.md` | 1 | updated (current-tense) |

Re-grep after the sweep: zero remaining hits of any of the five forms
anywhere under `components/*/docs/`.

**A-6 (`.agents/plans/roadmap.md`).** The `:mllp` Deferred row's second
stale reference (`ehrt.tools.corpus.framing`, left untouched by
AR-AU-1's own single-edit fence) corrected to `ehrt.corpus-io.framing`.
Merged into the row's existing dated note (rather than stacking a
second adjacent note, which would have read badly) — the note now
covers both namespace corrections in the row, citing ADR-0049 for the
first and this ADR for the second.

**E-3 (`components/corpus/docs/palgebra-design.md`).** The two
`test-integration/` citations repointed to the real
`projects/integration/test/ehrt/integration/{contract_pairing,baseline_gating}_test.clj`
paths. No dated note — the ruling asked for a plain repoint, unlike
A-6's namespace corrections.

**E-9 (`docs/formats.md`, `docs/glossary.md`).** Three
`ehrt.corpus.manifest/ManifestV1_1` citations (two in `formats.md`, one
in `glossary.md`) repointed to the canonical
`ehrt.provenance.manifest/ManifestV1_1` (ADR-0043's "the only acyclic
single home"). The re-export itself
(`components/corpus/src/ehrt/corpus/manifest.clj:60`,
`(def ManifestV1_1 provenance/ManifestV1_1)`) is untouched, per the
ruling.

Landed `430bb5c` ("docs: the past stops leaking — stale namespaces
swept, the tripwire learns new ground (alignment fixes 1,
AR-F1-1/2/3)"). Full suite green (0 failures/0 errors across all
projects, re-verified no `FAIL`/error markers anywhere in the captured
log beyond the expected `0 failures, 0 errors` lines). `clojure -M:poly
check`: OK.

### Step 2 — workspace.edn (AR-F1-4)

**The relocated `:necessary` narrative, verbatim** (from
`workspace.edn` as it stood at `989d6cf`, lines 15–54 — S3's own
finding, 40 lines, five dated re-derivation events):

> `:necessary` overrides warning 207 (poly's own brick-graph
> reachability check: is a declared brick reached by a real `:require`
> edge from the project's base, or from another brick the same project
> declares — it does not see a project's own ad hoc test-tree
> requires). Re-derived 2026-07-31 (docs-tooling split, finding 14),
> AGAIN the same day (corpus-io split stage 2, ADR-0017), a THIRD time
> the same day (split stage 3, ADR-0018: tools renamed corpus and
> retired), and a FIFTH time 2026-08-01 (sim-cli retirement, P3-6, F2
> fired — the "sim" project entry above this comment is gone with it:
> it existed solely to compose sim + sim-cli into one deployable
> artifact, and had no `:necessary` entry of its own to carry forward),
> each time via `clojure -M:poly deps`/`check` with every entry
> temporarily cleared. "ehrt-cli" needs no override: bases/cli now
> requires every interface it consumes directly (kernel, judge, all
> three engines, corpus, corpus-io, docs-tooling) — palgebra is
> reachable transitively through docs-tooling. "conformance" keeps the
> docs-tooling entry it has carried since stage 1 (the brick is there
> solely to host its own moved tests) and gains judge-fhir-official +
> judge-v2-nist: only this project's own test tree (parity/gate-loop
> suites) consumes them now that the retired facade's every-engine src
> relay is gone. NOT judge-v2-hapi, empirically: the corpus brick's own
> test tree (v2_contract_pairing_test) requires that engine's
> interface, a test-context brick edge poly's reachability DOES count,
> unlike a project's own ad hoc test-tree requires — which is exactly
> why "integration" needs judge-fhir-official listed (consumed only by
> its project test tree) while conformance's v2-hapi needs nothing (see
> ADR-0018's table). provenance (sim split B, M1, 2026-08-04): Step 1
> carried a temporary "provenance" `:necessary` override on all three
> entries below (landed ahead of the real `:require` edge, R-9's "three
> manifests during M1" transient); Step 2 repoints corpus' own src to
> it for real (ehrt.corpus.manifest, ehrt.corpus.interface), so every
> project including corpus reaches it transitively now — re-derived
> here, override dropped again.

`workspace.edn` now carries a two-line pointer plus the standing
invariant in its place; the full narrative above is this ADR's own
citable home going forward.

**A-1: `oracle` unreachable from `development`.** `"development"`
gains `:necessary ["oracle"]` with a one-line REPL-uniformity-rationale
comment (oracle carries no real `:require` edge from `:dev`'s own
composition, but the dev REPL is meant to reach every brick, oracle no
exception).

**Before/after evidence:**

```
BEFORE (clojure -M:poly check :dev):
  Warning 207: Unnecessary components were found in the development
  project and may be removed: oracle.

AFTER (clojure -M:poly check :dev):
  OK
```

Landed `6e27e78` ("chore: workspace.edn sheds its memoir; oracle's seat
is documented (alignment fixes 1, AR-F1-4)"). `clojure -M:poly check`
(all projects): OK. Full suite green, unchanged shape (config-only, no
`src/` touched).

### Step 3 — conventions (AR-F1-5)

All four notes/annotations/amendments, docstring/comment-only — `git
diff` on the two touched `src/` files (`components/corpus-io/src/ehrt/corpus_io/source_sink.clj`,
`bases/cli/src/ehrt/cli/core.clj`) confirmed showing only docstring
hunks before commit, no logic diffs.

- **B-1** — `implemented-sink-kinds`'s docstring
  (`components/corpus-io/src/ehrt/corpus_io/source_sink.clj`) gains an
  export-for-symmetry note: no external caller today (corpus's own
  sink-designator path doesn't exist yet), kept exported anyway
  mirroring `implemented-source-kinds`, named trigger: the player's
  sink slice consumes it once that path lands.
- **C-5** — `notes/adr/0011-per-engine-judge-split.md` gains a dated
  amendment recording the judge trio
  (`judge-fhir-official`/`judge-v2-hapi`/`judge-v2-nist`) as
  intentional ROLE-siblings: the surface asymmetry (`gate-batch`,
  `make-validator`) reflects real per-engine capability differences, no
  backfill intended.
- **D-2** — the audit brief
  (`.agents/plans/2026-08-05-alignment-audit-brief.md`) gains a dated
  annotation at its §4.4 sim-emit-cda paragraph (annotate-not-rewrite):
  a CDA sibling's nearest kin is `sim-emit-fhir`'s document-snapshot
  pattern, not `sim-emit-hl7`'s wire-stream idioms; the id-coherence
  law is a convention to reimplement, not code to extract.
- **F-5** — `repo-identity`'s docstring
  (`bases/cli/src/ehrt/cli/core.clj`) gains a clarifying line:
  `stable-*` tags are continuity/verification points (ADR-0003), not
  semver release tags; "no version tag has been cut" refers to the
  latter, unaffected by the four `stable-*` tags that now exist.

Landed `a9d3bb6` ("docs: conventions named where they live — sibling
roles, symmetry exports, tag kinds (alignment fixes 1, AR-F1-5)"). Full
suite green, `clojure -M:poly check`: OK.

### AR-F1-0 — the tag (AUTHOR ACTION, not executed)

Licensed but not run by this session (tags stay author-only in every
ceremony mode). Exact commands for the author, once this landing is
design-channel-verified:

```sh
git tag -a stable-20260805-alignment-audit 989d6cf \
  -m "alignment audit landed, design-channel-verified 2026-08-05 (ADR-0049)"
git push origin stable-20260805-alignment-audit
git ls-remote --tags origin | grep alignment-audit
```

If push auth fails, the `gh api repos/{owner}/{repo}/git/refs` fallback
applies (AR-AU-0's own standing mechanic), with disclosure.

### AR-F1-6 — standing rulings, pending append

Recorded here per this ruling's own instruction so the arc-close
session cannot miss it: at this arc's close, `.agents/rulings.md`
gains two entries (per AR-C-2's contract, appended by the design
channel at arc close) —

- **A-3, dependency review cadence:** `clojure -M:poly libs :outdated`
  is report-only, run at each arc close plus mandatory before any
  publish; upgrades are never taken as a side effect.
- **D-3, pairing-as-data registry landing spot:** `judge` is the
  accepted acyclic home; the design pass starts from there.

Not appended this session — this is the note, not the append.

### Verification

- `bin/regression-oracle 989d6cf a9d3bb6` (baseline: this session's own
  pre-session tip; target: this session's own tip immediately before
  this record's own closing commit — no `src/` logic touched at any
  point this session, only docstrings/comments/docs/config comments):
  **all ELEVEN vendored-root batches byte-identical** (`appendicitis`,
  `death-fixture`, `ear-infections`, `ear-infections-engine`,
  `ear-infections-history-engine`, `sepsis`, `sinusitis`,
  `sore-throat`, `total-joint-replacement-engine`,
  `urinary-tract-infections-engine`,
  `urinary-tract-infections-history-engine`). No
  `--declared-digest-change` licensed or needed.
- Full suite (`clojure -M:poly test :all skip:integration`): green at
  Step 0 baseline and after every subsequent step. Shape vs. Step 0 is
  unchanged except the tripwire's own new assertions:
  `ehrt.docs-tooling.stale-path-test` went from 8 deftests/153
  assertions (`989d6cf`) to 9 deftests/166 assertions (this session's
  tip) — +1 deftest
  (`scan-sources-includes-exactly-the-ruled-live-agents-plan-files-test`)
  and +13 assertions (6 in that new deftest, 7 added inside
  `each-forbidden-pattern-is-actually-caught-test` for the
  `ehrt.sim-cli.` pattern and its non-tripping siblings), verified by
  running the namespace directly against both the pre-session
  (`989d6cf`) and post-session file content — disclosed, not silent.
- `clojure -M:poly check`: OK, every step this session.
- `gitleaks git --staged -v`: clean, every commit this session.
- Post-push message verification, every checkpoint: each showed
  exactly one delta against its own message file — the known, harmless
  trailing-newline artifact prior sessions already name.

### Consequence

The first ruled cluster from the alignment-audit register lands: 8
files swept clean of five retired sim-family namespace forms (25
hits), the roadmap's own remaining stale reference and two other
live-doc staleness spots (E-3, E-9) fixed forward, the staleness
tripwire's scope widens by an explicit four-file include-list and
gains its missing `ehrt.sim-cli.` forbidden entry (red→green proven,
not merely asserted), `workspace.edn` sheds 40 lines of inline history
for a two-line pointer into this ADR, `poly check :dev`'s
previously-silent `oracle` warning is now silent for a documented
reason, and four small conventions (B-1, C-5, D-2, F-5) are named
where future readers will actually look for them. Two standing rulings
(AR-F1-6: A-3, D-3) are queued for the arc-close append, not lost.
AR-F1-0's tag stays licensed and ready for the author. This session
touched 11 of the register's 47 rows (S3, S7, A-1, A-6, B-1, C-5, D-2,
E-3, E-5, E-7, E-9); the remaining 36 stay open for future ruled
clusters (gate
promotions, S1 rename, NIST mirroring, LICENSE work, and whatever the
still-`ruling-needed` rows this session didn't touch — F-1/F-6/F-7's
publication-readiness cluster, A-4's supply-chain row, D-4's
named-future closures — draw next).
