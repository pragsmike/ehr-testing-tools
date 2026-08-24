# Rulings register -- standing rules only

**One row per standing rule, with the ADR that ruled it.** Verbatim from this file's
founding header (ADR-0047 AR-C-2): *"It is NOT a history: the ADR files themselves
(`notes/adr/`) are the narrative and execution record of record; this file exists so a
cold session can find 'what rule applies going forward' without re-reading forty-plus
ADRs to separate standing rules from executed-once decisions."* Between ADR-0048 and
ADR-0144 every block appended here was a history anyway; ADR-0145 moved all 56 of them,
verbatim, into the ADRs that own them, and made the shape enforceable.

Row contract, gated by `ehrt.docs-tooling.rulings-lint-test`:

    - **R-<slug>** -- <rule> -- ADR-NNNN [SUPERSEDED-BY R-<slug> (ADR-NNNN)]

Slugs are unique file-wide; a rule is cited `rulings.md#R-<slug>`. Three lines a row,
maximum. A superseded rule KEEPS its row and names its successor -- nothing here is
deleted. A rule earns a row only if a FUTURE session must still follow it; a decision
executed once belongs to its own ADR, which is also where every row's reasoning lives.
Appended by the session that takes the ruling, not held to arc close.

- **R-intake-front-door** -- a sim run enters `ehr corpus intake` as a foreign pipeline's
  output would, never as a privileged first-party producer -- ADR-0043
- **R-provenance-leaf** -- `ehrt.provenance.*` depends on `malli` and nothing else; an
  `ehrt.*` require inside it is itself the violation -- ADR-0043
- **R-sim-facade-frozen** -- `ehrt.sim.interface`'s vars, names and arities stay
  byte-identical; thinning the facade is a separate author-ruled decision -- ADR-0043
- **R-ledger-counting-definition** -- every parity or deftest ledger states which counting
  definition it uses -- `deftest`-only, or `deftest` plus `defspec` -- ADR-0043
- **R-oracle-script-contract** -- `bin/regression-oracle` resolves a classpath per worktree,
  equivalence-checks the whole digest source minus its leading docstring, and aborts on an
  undeclared diff; a `:require`/`:import` change IS one (widened ADR-0156) -- ADR-0044
- **R-move-not-improve** -- a relocation moves equipment and exercises it; a fix found
  mid-move is a FINDING, recorded, never taken. Prose included -- ADR-0044
- **R-deferred-rows-live** -- `## Deferred` rows are LIVE; one that closes moves with its
  notes intact, never a closure note left in place of the move -- ADR-0045
- **R-adr-citation-continuity** -- `notes/ADRs.md` stays the citation target forever; ADR
  numbers are never renumbered -- ADR-0046
- **R-session-narrative-hierarchy** -- the ADR is the sole session narrative, the session
  record the ceremony log, the roadmap a pointer, the prompt archive provenance -- ADR-0046
- **R-state-regeneration** -- `.agents/state.md` is regenerated at each arc close, every
  claim re-probed against the live tree -- ADR-0047
- **R-register-append-contract** -- standing rulings only, appended per arc close by the
  design channel, citing the closing ADR -- ADR-0047 SUPERSEDED-BY R-rulings-row-contract
  (ADR-0145)
- **R-mnt-c-retired** -- no session routes work through a Windows-mounted clone; a fresh
  instance is a NEW regression and a STOP-AND-REPORT -- ADR-0047
- **R-transcript-not-record** -- only repo artifacts are citable as fact; anything witnessed
  solely in a transcript is `[unverified]` until an artifact captures it -- ADR-0048
- **R-stable-tag-author-only** -- tagging remains the author's act alone -- ADR-0048
  SUPERSEDED-BY R-tag-law (ADR-0057)
- **R-dependency-review-cadence** -- `clojure -M:poly libs :outdated` is report-only, run at
  each arc close and before any publish; upgrades never ride along -- ADR-0055
- **R-pairing-registry-home** -- `judge` is the accepted acyclic home for the
  mutate-to-judge conviction registry -- ADR-0055
- **R-law-surface-propagation** -- an amendment to standing law lands on every surface
  stating that law, in the session that rules it -- ADR-0055
- **R-tag-law** -- `stable-*` tags are SESSION acts, under a specific licence or for a
  verified predecessor point; deferring a licensed one is the deviation. `v*` tags stay
  AUTHOR ACTION -- ADR-0057
- **R-two-voices-two-homes** -- user surfaces speak operator language; maintainer content
  lives in source comments and dev docs, relocated never deleted -- ADR-0064
- **R-errors-name-artifact** -- every operational error names the thing it could not find or
  parse, with a next step; unknown input is rejected by name -- ADR-0064
- **R-audit-uses-mechanism** -- audit evidence uses the mechanism it recommends: resolve
  paths rather than parse grammar, walk the data the gate walks -- ADR-0064
- **R-tests-build-own-dirs** -- a test builds the directories it needs; depending on a live
  mutable repo directory is the violation, tracked fixtures are not -- ADR-0068
- **R-folds-strict-sinks-lenient** -- a coherence fold rejects what it cannot reconstruct; a
  display sink absorbing foreign traffic skips-with-cue and counts the skips -- ADR-0068
- **R-vendored-bytes-are-law** -- vendored content is byte-verbatim at its pin,
  `-text`-protected, NOTICE-hashed, gate-verified; an edit is STOP-AND-REPORT -- ADR-0074
- **R-population-scale-gate** -- a module joins on a witnessed content-producing round trip
  at population scale; a census verdict is never a vendoring licence -- ADR-0074
- **R-multi-seed-once-flagged** -- a round trip that flags a module re-runs at 2-3
  well-mixed seeds at population scale before any verdict -- ADR-0080
- **R-defspec-seed-policy** -- seeds stay unpinned repo-wide; a spec that has actually
  flaked pins or durably logs its seed -- ADR-0080
- **R-io-result-or-loud** -- a production I/O call that can fail routes through
  `ehrt.kernel.io` -- `list-files`/`rename!`, and from 2026-08-19 `mkdirs!`/`delete!` with
  `delete-quietly!` the declared cleanup exception (ADR-0157) -- or handles failure by name; failure never impersonates an empty result -- ADR-0080
- **R-ci-watched-not-awaited** -- preflight discloses the last five runs; watching to
  conclusion is for a session whose claim is about CI; no push carries a known-failing test
  -- ADR-0080
- **R-semantics-predicted-first** -- a semantics change probes every oracle root FIRST and
  lands a per-root prediction; a mover is STOP-AND-REPORT for a licence naming it --
  ADR-0084
- **R-no-adjacency-diagnosis** -- a defect attributed to a shared mechanism without a direct
  probe of the failing artifact is `[unverified]` -- ADR-0084
- **R-witnessed-rows-only** -- a registry row exists only where the loop ran against a real
  fixture; every pinned expectation is MEASURED first; promotions need a dated ruling --
  ADR-0089
- **R-license-granularity** -- a licensed oracle mover is licensed by NAME and at the
  granularity the licence states; any deviation, a surprise identical included, is a STOP --
  ADR-0089
- **R-measure-claimed-population** -- a sweep claiming to measure a population draws from
  that population's own generation path, never a synthetic one assumed equivalent --
  ADR-0093
- **R-horizon-anchors-roadmap** -- an item surviving past ONE arc close in horizon prose
  gains a roadmap row in that same close: notes narrate, the roadmap remembers -- ADR-0093
- **R-post-push-ascii** -- post-push ceremony includes the per-commit ASCII check over the
  pushed range, expected empty, disclosed in-session -- ADR-0093
- **R-permission-denied-category** -- a missing path and an unreadable one share ONE
  category, distinguished by a `:reason :permission-denied` key, never a second one --
  ADR-0098
- **R-user-path-adr-footnoted** -- an ADR citation in the user path (`docs/` proper) becomes
  a footnote marker linking the index, never stripped -- ADR-0101
- **R-footnote-append-in-place** -- the visible `ADR-NNNN` token stays in the prose,
  followed by its marker -- ADR-0101 SUPERSEDED-BY R-footnote-marker-only (ADR-0102)
- **R-mllp-abandoned** -- the `:mllp` sink is abandoned, not deferred; no transport work
  follows from it without a fresh ruling -- ADR-0102
- **R-footnote-marker-only** -- a footnoted citation drops the visible token and keeps the
  marker alone -- ADR-0102
- **R-footnote-scope-origin-qualified** -- an origin-qualified citation (`sim/ADR-NNNN`)
  footnotes like a bare one, under a distinct marker targeting the index it names --
  ADR-0102
- **R-user-manual-deferral** -- the user manual waits on latency-realistic traffic plus a
  witnessed downstream-receiver demo -- ADR-0108 SUPERSEDED-BY R-user-manual-opened
  (ADR-0112)
- **R-dev-docs-not-user-path** -- `docs/dev/` is dev-docs under R34, never under the
  user-path footnote-citation discipline -- ADR-0108
- **R-corpus-tools-foreign-corpora** -- a corpus-level tool works on any directory of valid
  messages, foreign corpora included, never through sim-specific machinery -- ADR-0111
- **R-transport-realism-vs-mutation** -- transport realism simulates CORRECT transport
  behaviour deterministically; mutation injects INCORRECT content with an expected finding
  -- ADR-0111
- **R-user-manual-opened** -- the deferral's trigger is met -- `--board` accepted as the
  downstream stand-in -- and the manual work is open -- ADR-0112
- **R-user-manual-name** -- this workspace's user docs are the "user manual"; prior verbatim
  quotes saying "user guide" stay as spoken -- ADR-0113
- **R-manual-shape** -- `docs/manual/` is a chaptered narrative layer over existing
  references, never duplicating them, with `ed-tuesday` as its one running scenario --
  ADR-0113
- **R-demos-exercised** -- demos are exercised as documented: each scenario README's fenced
  commands run in order, exit codes and named invariants asserted -- ADR-0113
- **R-diagrams-derive-from-data** -- a manual diagram derives from data wherever derivable,
  committed as SVG with its source, so it cannot drift -- ADR-0113
- **R-engine-flake-seed-licence** -- a session may pin seed `7844068501`, classify the
  shrunk counterexample and fix or file, without a fresh ruling -- ADR-0114 SUPERSEDED-BY
  R-generative-failure-is-new-finding (ADR-0122)
- **R-out-dir-protected-artifact** -- `--out-dir` means one thing repo-wide: a protected
  artifact directory that refuses a collision -- ADR-0115
- **R-front-door-vs-engine-tier** -- `corpus generate` is the front door and defaults
  `--seed`; `sim run`/`sim identifiers` are the engine tier and require it -- ADR-0115
- **R-provenance-wall-clock-exemption** -- provenance recording a real-world act may default
  to wall clock; generating or transforming CONTENT stays inside the determinism law --
  ADR-0115
- **R-seed-contract-non-negative** -- the seed contract is non-negative longs, validated at
  engine entry; generative tests draw from documented contracts, not type ranges -- ADR-0116
- **R-kernel-result-for-new-guards** -- a component with no invalid-option convention adopts
  the kernel's result-not-throw doctrine rather than inventing one -- ADR-0116
- **R-audit-callers-on-result-branch** -- a function whose return contract gains a
  Result-typed branch has every caller audited, not just the function -- ADR-0116
- **R-no-derivation-through-nondeterminism** -- a flag whose only sensible derivation routes
  through a non-deterministic input is required, not derived -- ADR-0117
- **R-scoped-flag-mismatch-rejected** -- a source- or mode-scoped flag supplied in the wrong
  mode is rejected, not warned about -- ADR-0117
- **R-gate-scan-roots-cover-operator-surfaces** -- a doc-drift gate's scan roots cover every
  operator-facing surface, `.github/**` and `demos/**` included -- ADR-0118
- **R-examples-are-sourced-verbatim** -- a worked example is copied VERBATIM from a
  witnessed source and cited; with no witnessed source it renders none -- ADR-0118
- **R-generative-failure-is-new-finding** -- any generative failure in a `test.check`
  defspec is a new finding to STOP on and record, never a re-run under a prior seed charter
  -- ADR-0122
- **R-bin-scripts-one-purpose** -- ceremony mechanics land as one-purpose `bin/` scripts,
  never one combined script -- ADR-0127
- **R-improve-agent-facing** -- *'let's always look for opportunities to improve the
  agent-facing parts'* -- skills, scripts and prompts are a standing target -- ADR-0128
- **R-dated-addendum-not-silent-edit** -- a correction to a landed ADR is a dated
  fix-forward addendum, never a silent edit to its existing text -- ADR-0128
- **R-collision-guard-warn** -- a state-name collision warns at module load and the load
  proceeds -- ADR-0131 SUPERSEDED-BY R-exact-name-resolution (ADR-0133)
- **R-rename-frozen-records-keep-old-name** -- a rename sweeps every live reference; frozen
  records keep the old name and the renaming ADR carries the mapping -- ADR-0132
- **R-exact-name-resolution** -- name-valued references resolve by EXACT raw string through
  a load-time table, never through `slug`; a missing name is a load rejection -- ADR-0133
- **R-review-actor-split** -- the reviewer/actor split may be satisfied across channel and
  session: the report commit precedes every fix, each fix cites its row -- ADR-0134
- **R-unrelayed-tag-condition-stops** -- a tag licence conditioned on a relay the prompt
  does not carry STOPs and reports the run id; never substitute your own check -- ADR-0134
  SUPERSEDED-BY R-session-verifies-ci-via-gh (ADR-0148)
- **R-unregistered-request-gets-a-row** -- an unregistered standing request gets a roadmap
  row before it gets a disposition -- visibility first -- ADR-0139
- **R-severity-tracks-mechanism** -- a review dimension is scored on the gap its mechanism
  leaves open, not on what fell through it this time -- ADR-0139
- **R-arc-closes-in-own-session** -- an arc closes in its own session with its own tag; the
  close is a probe, not an appendix to the last fix session -- ADR-0139
- **R-lapsed-probe-standalone** -- a lapsed probe is chartered standalone, ahead of the
  review that would otherwise absorb it -- ADR-0139
- **R-review-cadence-in-adrs** -- repo-review cadence is measured in ADRs, not days: the
  next review is chartered roughly 15 ADRs past the prior close -- ADR-0139
- **R-never-dodge-a-gate-by-population** -- a gate that fires is telling you something true
  about scope: widen the fence under a ruling, or stop -- never move outside it -- ADR-0139
- **R-population-closure** -- enumerate the population from the tree, diff it against
  whatever claims to cover it, and treat the gap as the finding -- ADR-0139
- **R-disclosed-convention-is-a-disposition** -- a documented convention a reader can act on
  is a real disposition; the disclosure is what makes it one -- ADR-0139
- **R-out-is-the-only-output-root** -- generated output lands under `out/`, the single
  tool-owned output root -- ADR-0139
- **R-event-log-edn-primary** -- the event log's primary artifact is EDN; JSON is a later
  projection of it -- ADR-0141
- **R-event-schema-versioned** -- the event schema is public and versioned: the manifest
  carries `:event-schema-version`, and a non-additive diff must carry a bump -- ADR-0141
- **R-schema-source-plus-export-parity** -- the Malli schema is the source of truth and a
  committed EDN export is gated at parity with it -- ADR-0141
- **R-regex-dialect-stated** -- a pattern handed to a consumer names its dialect; this
  project's is `java.util.regex` -- ADR-0141
- **R-register-hygiene-at-close** -- the close commit moves this session's own closed rows
  to `Done` and re-measures every reading set, recording the actuals -- ADR-0143
- **R-budget-stop** -- exceed a reading-set budget and you compact or you stop, never bump;
  budget and baseline move only inside a compaction ADR -- ADR-0143
- **R-red-pushed-with-green** -- a red-first commit is pushed together with its green
  successor, never alone -- ADR-0143
- **R-anchored-register-edits** -- a register of independent rows is edited by anchored
  insertion or replacement, never by slicing between two anchors -- ADR-0143
- **R-adr-index-generated** -- `notes/ADRs.md` is generated from the ADR files' own headings
  and Status lines, CI freshness-gated -- ADR-0143
- **R-narratives-move-to-owning-adr** -- a register row's narrative moves VERBATIM into the
  ADR that owns it, under a dated heading -- ADR-0143
- **R-roadmap-status-tokens** -- every roadmap row opens with `OPEN`, `CLOSED <date>
  <ADR|sha>`, `DEFERRED (trigger: ...)` or `EXTERNAL`; `CLOSED` only under `## Done` --
  ADR-0144
- **R-roadmap-slug-anchors** -- every roadmap row carries a unique `**[slug]**` and is cited
  `roadmap.md#<slug>`; a line-number cite from a live surface is red -- ADR-0144
- **R-roadmap-six-line-cap** -- six lines a roadmap row, maximum -- ADR-0144
- **R-roadmap-row-destinations** -- a closed roadmap row moves verbatim to the attic leaving
  one `## Done` line; a live row's overflow moves verbatim to its owning ADR -- ADR-0144
- **R-roadmap-priority** -- `## Next` rows carry `PRIORITY n`, unique and ascending in file
  order -- ADR-0144
- **R-remainder-tokens-the-row** -- a closure leaving a live remainder keeps ONE row,
  retitled and retokened by what survives; splitting it is re-triage -- ADR-0144
- **R-section-retriage-is-author-judgement** -- moving a row between `## Next`, `##
  Deferred` and `## Externals` is author judgement, never mechanical -- ADR-0144
- **R-rulings-row-contract** -- this register holds standing rules only, one row per rule
  with its ADR; a superseded rule keeps its row and names its successor -- ADR-0145
- **R-skill-rules-not-history** -- a skill states the imperative and one cite; its narrative
  history lives in a sibling `HISTORY.md` no reading set carries -- ADR-0145
- **R-reading-sets-header-cap** -- `.agents/reading-sets.edn` carries a 20-line header;
  budget derivations live in `.agents/plans/reading-sets-history.md` -- ADR-0145
- **R-audience-has-entry-path** -- every segment in `docs/dev/AUDIENCES.md` states its
  own entry path; a registered audience without one is a routing gap everywhere that
  register is keyed off -- ADR-0146
- **R-exercised-implies-gated** -- a row in `exercised-sources.edn` needs a live
  `check-entry` case: a script that exists is not a script PROVEN to teach its page's
  own commands -- ADR-0146
- **R-taught-shell-lines-use-expect-eval** -- a taught line needing a shell is exercised
  via `expect_eval`, never `expect 0 bash -c`, which the freshness unwrapper cannot
  read -- ADR-0146
- **R-count-by-presence-not-truthiness** -- code reporting which fields it dropped counts
  by PRESENCE; truthiness reports a present-but-false field as absent -- ADR-0146
- **R-register-three-ways** -- a register is GENERATED where its content is derivable
  from the tree, capped and LINTED where it is hand-owned judgement, and ATTIC'D
  verbatim where it is history. No register is more than one of the three -- ADR-0147
- **R-register-in-a-set-is-linted** -- a register carried by any reading set owes a
  lint on its own growth; without one the budget only measures the growth, and a
  measurement is not a limit -- ADR-0147
- **R-git-from-wsl** -- every git operation runs from WSL, never native Windows, enforced
  by `.githooks/pre-commit` once `git config core.hooksPath .githooks` is set per clone --
  ADR-0003
- **R-staging-hygiene** -- `git diff --cached --stat` is read before every commit and
  anything outside the checkpoint in flight is unstaged first -- ADR-0007
- **R-register-gated-by-its-own-loader** -- a register that gates a population is covered
  by ONE test over its own loader, never per-row hand cases: a row must be gated the
  moment it is registered, with no test edit -- ADR-0148
- **R-empty-population-is-red** -- a population gate asserts its population is non-empty;
  "no violations" over zero items is a pass that proves nothing, whether the zero is an
  empty scan or an empty extraction -- ADR-0148
- **R-session-verifies-ci-via-gh** -- a tag licence's CI condition is met by the executing
  session's own `gh run view <id>` concluding success, id and conclusion recorded; author
  relay sufficient, never required; pay in session if it concludes while open -- ADR-0148
- **R-full-suite-before-push** -- a push is preceded by full `make test` unpiped with
  MAKE_EXIT recorded, and a wrapper capturing it ENDS with `exit "$MAKE_EXIT"`; `poly test
  brick:`/`project:` are aids, never the gate: tree-scanning gates live elsewhere -- ADR-0150
- **R-preflight-fail-closed** -- `bin/preflight` exits non-zero on any FINDING/FAIL/UNKNOWN,
  and a check that could not MEASURE reports UNKNOWN, never OK: its output is the artifact
  a session discloses and its exit code is a claim as well -- ADR-0155
- **R-stop-only-on-two-defensible-readings** -- STOP-AND-REPORT binds where two readings
  are both defensible; a mechanical conflict with one defensible reading is fix-forward
  with disclosure -- ADR-0146
- **R-amend-unpushed-message-only** -- `git commit --amend` only on a commit not yet
  pushed and only to change its message; a content change is a new commit -- ADR-0156
- **R-done-attic-rotation** -- `## Done` holds at most 30 LINES; a close that exceeds it
  rotates oldest WHOLE rows verbatim into `.agents/plans/roadmap-done-<yyyy-mm>.md`,
  which is append-only. Mechanical -- no arc boundaries to judge -- ADR-0161
- **R-mix-1** -- life-arc dynamics (residence, employment, coverage, households,
  mortality) are bespoke hazard-rate processes, never GMF modules -- ADR-0168
- **R-mix-2** -- family/household structure is in scope for the person process,
  pregnancy->delivery explicitly -- ADR-0168
- **R-mix-3** -- geography stays small and file-drawn; the table grows modestly, and
  a wholesale extraction is a separate ruling -- ADR-0168
- **R-mix-4** -- unidentified/unresponsive ED arrivals and delayed-insurance flows are
  in scope, both branches (fill-in-place and merge-with-existing-MRN) -- ADR-0168
- **R-mix-5** -- scheduling is STATE: appointment new/reschedule/cancel/no-show are
  skeleton events carrying invariants, not rendered chatter -- ADR-0168
- **R-mix-6** -- bed-status is STATE: the vacated/dirty/cleaning/ready cycle is
  world-level ground truth that assignment is gated on -- ADR-0168
- **R-mix-7** -- chatter and fan-out are emission add-ons DOWNSTREAM of the fact
  generators; mix ratios are emission config and reshuffle nothing -- ADR-0168
- **R-skeleton-or-emission** -- every traffic family is classified by one test: if
  downstream invariants or later messages' content must respect it, it is skeleton
  (generated, judged); if it is derivable restatement, it is emission -- ADR-0168
- **R-per-person-streams-before-generator-fixes** -- Q3(b) is CALLED FOR, not deferred:
  per-patient/per-person RNG streams plus the from==to delay-draw skip precede the
  traffic-scale generator arcs; a generator fix landing first owes an author ruling -- ADR-0168
