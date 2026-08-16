# Rulings register — standing decisions only

**Seed, not a completed register (ADR-0047 AR-C-2).** This file holds
only the STANDING `[A]` rulings — ongoing rules a future session must
still follow, not one-off execution choices — stated by this arc's own
five ADRs (0043–0047), extracted by reading those attic files
directly. It is NOT a history: the ADR files themselves (`notes/adr/`)
are the narrative and execution record of record; this file exists so
a cold session can find "what rule applies going forward" without
re-reading forty-plus ADRs to separate standing rules from executed-
once decisions.

**Back-fill of ADR-0001 through ADR-0042 is a named future, not
attempted here.** Extracting standing rulings from all forty-two prior
ADRs is judgment work — deciding which of each ADR's rulings are still
"standing" versus superseded or one-off — that exceeds what one
session can responsibly do alongside its own primary work. Trigger:
the next design-channel onboarding session that misses a rule this
register would have surfaced.

**Contract:** appended at each arc close, by the design channel,
citing the closing ADR. A ruling that gets superseded stays in this
file with a dated superseding note (never silently deleted) — same
discipline `notes/ADRs.md` itself uses for ADRs.

---

## From ADR-0043 (sim split B, M1–M4)

- **The intake-front-door doctrine** (AR-M1-4): a sim run enters `ehr
  corpus intake` as if it were a foreign pipeline's own output, never
  a privileged first-party producer. Deliberate, not a gap to
  eventually special-case — the discipline has caught real defects
  before precisely because nothing about the intake path assumes the
  producer is trustworthy.
- **`provenance` is a leaf schema component, forbidden forever from
  depending on anything but `malli`** (AR-2) — not `kernel`, not
  `corpus`, not `sim`, not any other brick. A future `ehrt.*` require
  inside `ehrt.provenance.*` is itself the violation, not something to
  accommodate.
- **The façade (`ehrt.sim.interface`) stays permanently frozen in
  surface** (AR-M4-3, honoring the 08-02 plan's own AR-3): var list,
  names, and arities byte-identical across every extraction stage —
  `corpus` depends on this interface's own stability in-process
  (ADR-0012). Any future thinning of the façade itself is a SEPARATE,
  explicit author-ruled decision — never a side effect of some other
  session's own work.
- **Every future parity/deftest ledger in this project states which
  counting definition it uses** (`deftest`-only, or `deftest`+
  `defspec`), explicitly, rather than leaving the reader to infer it
  from context (AR-D-6, the lesson this arc's own four stages learned
  the hard way).

## From ADR-0044 (standing-equipment promotion)

- **`bin/regression-oracle`'s standing script contract** (AR-P-3):
  per-worktree classpath resolution (each side's own `components/
  oracle`); a cross-side digest-source equivalence check runs FIRST;
  anything beyond an ns/require-only diff requires an explicit
  `--declared-digest-change` flag (recorded in the manifest header) or
  the script aborts. This closed ADR-0030's own J2 limitation
  structurally, not as a one-off fix.
- **Promotion moves equipment, it does not improve it** (AR-P-4,
  generalized): a relocation session's job is relocation plus
  test-exercise; a tempting fix found mid-move is a FINDING, recorded,
  never taken — the same fence this arc's own compaction sessions
  (0045–0047) all re-apply to their own scope.

## From ADR-0045 (scaffolding compaction A)

- **The Deferred section's own standing contract** (AR-A-5): rows
  there are LIVE. A row that closes moves to Done WITH its notes
  intact (relocation, not rewrite) — never left in place with a
  closure note substituted for actually moving it. (See `.agents/
  state.md`'s own disclosed finding: `myocardial_infarction.json`
  is a known violation of this exact rule, predating this ruling,
  not yet swept.)

## From ADR-0046 (scaffolding compaction B)

- **Citation continuity, standing** (AR-B-2): `notes/ADRs.md` REMAINS
  the citation target forever — every "notes/ADRs.md ADR-NNNN"
  citation repo-wide stays resolvable (index → per-ADR file). No
  renumbering, ever — ADR numbers are load-bearing in immutable places
  this workspace cannot edit (commit messages, archived prompts).
- **The canonical session-narrative hierarchy** (AR-B-4, explicitly
  recorded as standing rule): the ADR execution record is the SOLE
  narrative of a session; the session record is the ceremony/
  verification log; the roadmap Done entry is a one-line pointer
  (date, slug, ADR number) gated by `ehrt.docs-tooling.done-pointer-
  adr-test`; the prompt archive is provenance. New execution-record
  appends go directly to the per-ADR file, never back through
  `notes/ADRs.md` itself.

## From ADR-0047 (scaffolding compaction C — this arc's close)

- **`.agents/state.md`'s own regeneration contract** (AR-C-1): the
  design channel regenerates this file at each arc close, every `[V]`
  claim re-probed against the live tree at landing time before it
  lands — never carried forward stale.
- **This register's own append contract** (AR-C-2, restated): standing
  rulings only, appended per arc close, citing the closing ADR — see
  this file's own header.
- **`/mnt/c` is retired, permanently** (AR-C-3): no future session
  routes work through a second, Windows-mounted clone of this repo.
  The ext4 clone, reached by its UNC path, is the only clone of
  record. A future session that finds itself defaulting to a `/mnt/c`
  working directory again is encountering a NEW regression, not a
  known, guarded hazard — treat it as a fresh STOP-AND-REPORT, not
  routine vigilance.

## From ADR-0048 (alignment riders)

**Mid-arc append, author-licensed (see ADR-0048's own deviation
note):** this register's stated contract (AR-C-2, above) is "appended
at each arc close, by the design channel." Both rulings below land
mid-arc instead, from a build session, because the author licensed it
explicitly this session — a deviation-with-license, not a silent
change to the contract itself.

- **Transcript-witnessed is not repo-recorded** (from ADR-0048,
  citing `notes/adr/0047-scaffolding-compaction-c-arc-close.md` Step 0 as the
  evidencing event): only repo artifacts are citable as established
  fact. An event witnessed only in a chat transcript — a design-
  channel claim, an author statement, a prior session's own summary —
  is `[unverified]` until a repo artifact (a probe, a test, a file)
  actually captures it. ADR-0047's own Step 0 is the standing proof
  this discipline is load-bearing, not theoretical: re-probing a
  design-channel-authored draft against the live tree caught it wrong
  twice in one pass (a component-dependency claim — `sim-emit-hl7`
  does not depend on `sim-engine` — and a "four-incident ledger"
  naming session labels with zero supporting evidence in the named
  records), both corrected in place rather than carried forward.
- **Stable-tag discipline, adopted** (AR-R-2, STANDING, from
  ADR-0048): live `stable-*` tagging is adopted. The author tags after
  each design-channel-verified landing, format
  `stable-YYYYMMDD-<session-slug>`, matching the existing `^stable-.*`
  pattern in `workspace.edn` (no config change). The three legacy tags
  (`stable-bootstrap`, `stable-ehrt-1`, `stable-pre-monorepo`) stay —
  frozen history, superseded by the first new stable point. Tagging
  remains the author's act alone (R30). **Superseded 2026-08-06 — this
  ruling's own final sentence is out of date; see "From the tag-law
  session (ADR-0057)," below, for the standing replacement.** The
  format/legacy-tag substance above still holds, unedited.

## From the alignment arc (ADR-0048–0055)

- **Dependency-review cadence, standing** (A-3, from ADR-0050 AR-F1-6;
  corrected 2026-08-07, lint family AR-LF-6, D7-1 -- this ruling had
  cited invented sub-letters "AR-F1-6a"/"AR-F1-6b"; ADR-0050's own
  source labels these two rulings `A-3`/`D-3` under one shared AR-F1-6
  heading, the letter-suffix scheme never existed there):
  `clojure -M:poly libs :outdated` is report-only, run at each arc close
  plus mandatory before any publish; upgrades are never taken as a side
  effect of running it.
- **Pairing-as-data registry landing spot, accepted** (D-3, from
  ADR-0050 AR-F1-6): `judge` is the accepted acyclic home for the
  mutate↔judge conviction registry; the design pass starts from there.
- **Law-surface propagation lesson, standing** (from ADR-0051 AR-F2-0
  and ADR-0053 AR-F4-4): an amendment to standing law lands on every
  surface that states the law, in the same session that rules it —
  never left to a later session to notice the drift, and never resolved
  ad hoc by a session that only notices the conflict in passing. Two
  instances this arc: `AGENTS.md`'s tag rule lagged ADR-0049's AR-AU-0
  by three sessions before ADR-0051 closed the gap; `components/
  judge-v2-nist/deps.edn`'s own comment block (and its source, the
  archived spike-notes document) kept prescribing in-repo NIST-jar
  vendoring for eleven days after ADR-0005's 2026-07-24 amendment made
  that prescription unlawful, until ADR-0053 closed it.

  **Third instance, 2026-08-06 (tag law, `notes/adr/0057-tag-law.md`
  AR-T-1/AR-T-2):** this file's own AR-R-2, above, stated tagging as
  "the author's act alone" nine sessions after ADR-0049's AR-AU-0 had
  already amended the mechanic, and was never itself corrected when
  ADR-0051's AR-F2-0 reconciled `AGENTS.md` alone — leaving
  `AUTHORS-GUIDE.md`, both `build-session` `SKILL.md` copies,
  `.agents/state.md`, and this file all stating or implying the retired
  law until the tag-law session swept every surface at once and gated
  the retired phrasing against recurrence
  (`ehrt.docs-tooling.tag-law-test`).

## From the tag-law session (ADR-0057)

- **Stable-tag discipline, AMENDED 2026-08-06** (AR-T-1, STANDING,
  superseding AR-R-2's final sentence above): `stable-*` continuity
  tags are SESSION ACTS. A session creates and pushes one (i) when its
  own prompt licenses a SPECIFIC tag at a SPECIFIC commit, a license
  the design channel issues only after verifying the landing it names,
  or (ii) for its own predecessor's design-channel-verified stable
  point, as standing ceremony, without bouncing back to the author.
  **Deferring a licensed tag is now the deviation** and needs a
  disclosed reason — the inverse of AR-R-2's own default. The author
  may always tag directly, licensed or not; a tag already present at
  the exact commit and message a session would otherwise have created
  is verified and disclosed, never re-created. Release `v*` tags stay
  AUTHOR ACTION, unchanged — publication itself is author-gated, so its
  tags are too. `notes/ADRs.md` ADR-0003's original author-only
  trust-boundary reasoning is superseded in scope for this one class of
  tag, not erased: the design channel's own landing verification is now
  that boundary, and the tag is its mechanical consequence.

## From the UX arc (ADR-0056–0064)

- **Two voices, two homes, standing** (brief §3, executed by ADR-0062):
  user-facing surfaces (help, errors, command-bearing docs) speak
  operator language; maintainer content (citations, milestone history,
  internal names) lives in source comments and dev docs, relocated
  never deleted.
- **Errors name their artifact, standing** (ADR-0060, ADR-0061): every
  operational error names the concrete thing it could not find or
  parse, with a next step where one exists; unknown input is rejected
  by name, never silently accepted.
- **Audit evidence uses the mechanism it recommends, standing**: fence
  verification resolves paths rather than parsing grammar, and string
  inventories walk the data the gate will walk rather than grepping
  source. Two same-arc instances of the cheaper method being wrong:
  AR-U2-R's non-resolving fences (ADR-0060) and the 38-vs-36 token
  count (ADR-0062).

## From the player arc (ADR-0066–0068)

- **Tests build their own directories, standing** [A] (from
  ADR-0067 AR-BB2-R, its own append to this register explicitly
  deferred to this arc's own close per this register's own contract):
  *"Tests should build their own directories as needed."* Verbatim,
  the author, 2026-08-07. A test that reads a live mutable repo
  directory — depending on an untracked, author-held file's mere
  presence or absence — is the violation; tracked test-fixtures
  directories are fine, out of this rule's own scope (the same
  carve-out ADR-0067's own enumeration drew for `config/synthea/` and
  every lint test's own literal walk of the tracked tree).
- **Folds stay strict, sinks stay lenient** [C] (channel-inferred,
  generalized from ADR-0066's fold and ADR-0067's board-sink design —
  the author may strike it): a coherence-law fold rejects what it
  cannot faithfully reconstruct (`:unsupported-trigger` — total over a
  documented trigger set, never silently partial); a display sink
  absorbing foreign traffic skips-with-cue and counts what it skipped,
  never crashes and never silently mis-folds. The two live side by
  side deliberately: the fold is the coherence property's own spec,
  the sink is what a stranger's real feed hits.

## From the vendoring arc (ADR-0069–0074)

- **Vendored bytes are law** [C] (channel-inferred consolidation of
  this arc's own executed discipline — the author may strike it):
  upstream content vendors byte-verbatim at its named pin, `-text`-
  protected from git normalization, NOTICE-hashed per file, and
  gate-verified on every test run (`notice_verbatim_test`). An
  edit-tempting vendored file is STOP-AND-REPORT, never a fix
  (ADR-0070's own mechanics; AR-VB3-R1's teeth; the `uti_recurrence`
  lesson — a hash and the bytes it names are only honest together when
  something actually re-checks them, which nothing did until batch 3).
- **The population-scale gate outranks the census sample** [C]
  (channel-inferred consolidation — the author may strike it): a
  module joins the tree only with a witnessed content-producing
  engine-layer round trip at population scale; zero-substance modules
  are not vendorable; a census verdict is evidence for curation, never
  a vendoring license (the `injuries`, `anemia___unknown_etiology`, and
  `colorectal_cancer` precedents, ADR-0070–0072).

## From the quality-review arc (ADR-0075–0080)

- **Multi-seed-once-flagged, standing** [A — ruled AR-RL-5(5)]: a
  vendoring round-trip that flags a module re-runs at 2–3 well-mixed
  seeds at population scale before any verdict — codifies ADR-0071/
  ADR-0072's own followed practice (findings register D6-4), previously
  precedent-only.
- **The `defspec` seed policy, standing** [A — ruled AR-RL-5(3)]: seeds
  stay unpinned repo-wide, for generator diversity; a spec that has
  actually flaked pins or durably logs its seed (the engine spec,
  ADR-0079); the printed-seed-plus-CI-retention default is sufficient
  otherwise; revisited on the next flake.
- **I/O speaks Result or fails loud, standing** [C — the arc's own
  executed discipline; the author may strike it]: a production I/O
  call that can fail routes through `ehrt.kernel.io` or handles its
  failure mode by name; an I/O failure never impersonates an empty
  result (ADR-0078, gated by `io_vocabulary_lint_test`).
- **CI is watched, never waited on, and commits land green, standing**
  [C — likewise]: preflight discloses the last five runs' conclusions;
  watch-to-conclusion is reserved for sessions whose own claim is about
  CI; no push carries a knowingly-failing test (ADR-0075/0076/0078's
  own pattern shift, superseding the older red-checkpoint-commit
  pattern).

## From the fidelity arc (ADR-0081–0084)

- **Semantics changes are predicted before they are made, standing**
  [C — the arc's own executed discipline; the author may strike it]: an
  interpreter/engine/emitter semantics change runs a blast-radius probe
  over every oracle root FIRST, lands a per-root identical-or-moves
  prediction, and any mover is STOP-AND-REPORT for an explicit license
  naming that mover alone; the post-change bracket must match the
  prediction exactly (ADR-0082's own executed protocol, R3 as ruled and
  exercised — including the trace-then-license resolution the author
  ruled when the probe fired).
- **Plausible-by-adjacency is not a diagnosis, standing** [C —
  likewise]: a defect attributed to a shared mechanism without a direct
  probe of the failing artifact is `[unverified]` until the probe
  exists; ADR-0072's colorectal diagnosis was inference from a shared
  submodule, overturned by the first trajectory scan (ADR-0082/0083's
  own erratum chain).

## From the conviction arc (ADR-0085–0089)

- **Witnessed rows only, standing** [A]: the pairing registry
  (ADR-0088) holds per-operator rows that exist ONLY when the
  mutate→judge loop was actually executed against a real fixture;
  unwitnessed cells do not appear; every pinned expectation is
  MEASURED before it is written (a wrong first measurement is
  disclosed, never silently discarded — ADR-0087/0088's own
  precedent); tier promotions (report-only → gating) happen only by
  dated author ruling.
- **Licenses bind at their own granularity, standing** [A]: a licensed
  oracle mover is licensed by NAME and at the EVIDENCE GRANULARITY the
  license states (ADR-0086: `sleep-apnea`, walks #17/#58/#269); the
  post-change bracket must match at that granularity, and any
  deviation — a different root, a different walk set, a surprise
  identical — is a fresh STOP-AND-REPORT, never absorbed by the
  existing license.

## From review 2's rulings (ADR-0092/0093)

- **Measurements sample the claimed population, standing** [A, ruled
  AR-RL2-2, 2026-08-09, citing ADR-0087 / ADR-0092 D6-2]: a sweep or
  sample claiming to measure a population must draw from that
  population's own RNG path/generation mechanism, never an independent
  synthetic path assumed equivalent; a zero measured against a
  known-nonzero branch is the tripwire (ADR-0087's own self-caught
  miss — a synthetic sweep for `:suppressed-straddle-spans` drew from
  an independent RNG path and measured zero against a real, disclosed
  2-of-900 branch — is the precedent).
- **Horizon items anchor in the roadmap, standing** [A, ruled AR-RL2-3,
  2026-08-09, citing ADR-0092 D7-7/D7-8]: any item surviving past ONE
  arc close purely in horizon-note prose gains a `roadmap.md` Deferred
  or Next row in the SAME close that first restates it; horizon notes
  narrate, the roadmap remembers (ADR-0092's own A/B evidence — the one
  aged item with a roadmap anchor, the census undercount, self-healed
  after a single missed restatement; the two without one, wellness-
  encounters and the `notice_verbatim_test` coverage gap, did not
  recover across three — is the precedent).
- **Post-push verification includes the ASCII check, standing** [A,
  ruled AR-RL2-5, 2026-08-09, citing ADR-0091 AR-SD-6 / ADR-0092 H-6]:
  the standing post-push ceremony adds one mechanical line, `git log
  --format=%B -1 | LC_ALL=C grep -n '[^ -~]'`, expected EMPTY; any hit
  is disclosed in-session, not discovered by channel report later. This
  is ceremony boilerplate for session prompts and session practice —
  NO new repo test, NO workflow change, NO gate file.

## From ADR-0098 (mid-arc append, author-licensed via this session's own
driving prompt — the same license class ADR-0048's own mid-arc append
used: not an arc close, but the author's own ruling explicitly directed
into this register the same session it was made)

- **Judge-family entry-guard charter width, ruled** (2026-08-09, author
  verbatim "Q1 a.", citing ADR-0096 Finding 1 / ADR-0097 AR-AC-1 item
  1): a permission-denied (or missing-path) entry-guard fix scoped by
  its own driving prompt to one judge engine widens to every judge
  engine sharing the identical defect shape (an `.isFile`-only, or
  no, entry check that a chmod-000 path passes) IN THE SAME SESSION,
  when the author rules it — not deferred to a future session per
  engine. `judge-fhir-official`'s own Finding-1 fix widened to
  `judge-v2-hapi`/`judge-v2-nist` on this ruling; not standing beyond
  its own session unless a future author ruling re-confirms it for a
  different defect class.
- **Permission-denied category shape, standing** [A, ruled 2026-08-09,
  author verbatim "Q2 a.", citing family parity ruled 2026-07-31]: a
  missing-path and an exists-but-unreadable path share ONE category
  (`:file-not-found` in this judge family; the category a component's
  own existing missing-path convention already uses elsewhere) — never
  a second, unreadable-specific category — distinguished by a
  `:reason :permission-denied` payload key on the unreadable leg only.
  Applies to any future component gaining this same missing-vs-
  unreadable distinction: extend the existing category with a `:reason`
  key, don't mint a new one.

## From ADR-0099 (fixture relocation, backlog row licensed by ADR-0081
AR-FR-2(a); rulings taken this session's own driving prompt, author-
ruled 2026-08-09 the day before this session ran)

- **Fixture relocation target home** [A, ruled 2026-08-09, author
  verbatim "Q1 a."]: the new home for `components/corpus/test-
  fixtures/` is a root-level `test-fixtures/` directory, sibling to
  `demos/` — not nested under any component.
- **Fixture relocation scope** [A, ruled 2026-08-09, author verbatim
  "Q2 a."]: the ENTIRE tree moves as one subtree-whole `git mv`, all
  four subtrees together (`v2/`, `v2-nist/`, `fhir/`, and
  `reports/` — the last unnamed in ADR-0081's own row, riding along by
  this ruling, disclosed in ADR-0099) — never split across sessions or
  left partially relocated.

## From ADR-0100 (corpus player: sim event-log adapter, roadmap Next
row named since ADR-0014; rulings taken this session's own driving
prompt, author-ruled 2026-08-10, the session's own day)

- **Sim event-log adapter semantics** [A, ruled 2026-08-10, author
  verbatim "Q1 a."]: native event playback — events paced by `:t`
  directly via an injectable timestamp-extraction seam on `plan`
  (continuing the `:tty?-fn`/`:sleep-fn` injection lineage, not a
  second pacer); a compact event-line ticker; `--board` under event
  input REJECTED with a named-deferral hint (the board's fold is
  wire-side; feeding it would need emission parameters the log does
  not carry).
- **Producer-side event log** [A, ruled 2026-08-10, author verbatim
  "Q2 a."]: `corpus generate sim` also spools the ground-truth vector
  as `events.edn` into out-dir, same `pr-str` bytes as `--format
  ground-truth`'s bare text — disclosed against D7 ruling 4's
  "provenance is the generator's word" (that ruling governed
  manifests; `events.edn` is data, not provenance, so it does not
  reopen that ruling's scope).
- **Demo touch, scoped** [A, ruled 2026-08-10, author verbatim "Q3
  a."]: busy-tuesday's README gains ONE "play the sim's own story"
  example line once the adapter lands. Nothing else attaches — no
  rows invented, no other demo asides committed.

## From ADR-0101 (user-path ADR citations become footnotes; rulings
taken this session's own driving prompt, author-ruled 2026-08-10, the
session's own day)

- **The footnote fork, resolved** [A, ruled 2026-08-10, author verbatim
  "as clickable footnotes" then "a."]: every bare `ADR-NNNN` citation in
  the user path (`docs/` proper, not `docs/dev/`) becomes a footnote
  marker whose definition links the citation index (`notes/ADRs.md`),
  never stripped. Superseded ADR-0081's own unruled fork
  ("strip to dev-docs only, vs. footnotes that keep provenance").
- **`--sink` ratification** [A, ruled 2026-08-10, author verbatim
  "--sink call ok for now."]: ADR-0100's own disclosed judgment call —
  rejecting `--sink` on event input, beyond Q1 a.'s own literal scope
  which named only `--board`, categorized `:play-sink-unsupported-for-
  events` — is RATIFIED as standing. No code change; this entry is the
  record.

## From ADR-0102 (user-path citations go marker-only, full user path,
gate hardened; `:mllp` sink abandoned; rulings taken this session's own
driving prompt, author-ruled 2026-08-10, the session's own day)

- **`:mllp` sink, abandoned for now** [A, ruled 2026-08-10, author
  verbatim: "Let's abandon `:mllp` for now."]: no transport work, no
  amendment to `docs/dev/source-sink-design.md` (its D2/D3 stand as
  written and governing). `notes/adr/0014-corpus-player.md`'s own
  "future `:mllp` sink" deferral language (repeated in its 2026-07-30
  fulfillment note's own list of what "remain[s] exactly as deferred
  here") is RULED SUPERSEDED IN PART by this entry — the sink is
  abandoned, not merely still-deferred — without editing that frozen
  record itself (frozen-archives discipline). `bases/cli/src/ehrt/cli/
  help.clj`'s `play --sink` doc line, the one place actually claiming
  `mllp:` was "recognized but deferred," is corrected (ADR-0102) to
  name only `dir:`/`blaze:`.
- **Footnote form, marker-only** [A, ruled 2026-08-10, author verbatim:
  "For footnotes, do b, marker-only form. I don't want ADRs cluttering
  user-facing prose."]: supersedes ADR-0101's own append-in-place
  convention (visible `ADR-NNNN` token immediately followed by its
  marker) — every footnoted citation site in the user path now drops
  the visible token; the marker alone remains, standing law for any
  future citation this workspace footnotes.
- **Footnote scope, full user path** [A, ruled 2026-08-10, author
  verbatim "a," in answer to whether the origin-qualified citations
  footnote too]: ADR-0101's own bare-only scope widens permanently —
  an origin-qualified citation (`sim/ADR-NNNN`, `tools/ADR-NNNN`) in
  the user path footnotes exactly like a bare one, targeting the
  frozen pre-merge index (`notes/sim/ADRs.md`, `notes/tools/ADRs.md`)
  it actually names, under a distinctly-named marker
  (`[^sim-adr-NNNN]`/`[^tools-adr-NNNN]`) so it never collides with a
  same-numbered current-workspace record's own marker.

## From ADR-0103 (board boundary catch-up; rulings taken this
session's own driving prompt, author-ruled 2026-08-10, executed
2026-08-11)

- **Bugfix now, redesign later** [A, ruled 2026-08-10, author verbatim
  "c."] — choosing both: this session executes the `--board` cadence
  bugfix only; the busy-tuesday/ED scenario redesign is chartered as
  its own, separate, not-yet-opened arc (see the ED-direction ruling,
  below, and `.agents/plans/roadmap.md`'s own Next row).
- **ED-weighted redesign direction, chartering context only, NOT
  executed** [A, ruled 2026-08-10, author verbatim]: *"Maybe weight
  the patient population toward immediate, emergent conditions like
  trauma/injuries? This would simulate an actual ED, which is where a
  lot of the activity and churn would happen."* Recorded here as the
  redesign arc's own charter — no scenario, config, module, or content
  change accompanies this entry; the design pass that turns this
  direction into a plan is a future session's own work.

## From ADR-0104 (ed-tuesday scenario; rulings taken this session's own
driving prompt, author-ruled 2026-08-10, executed 2026-08-11)

- **"C-with-A-first," standing until B lands** [A, ruled 2026-08-10,
  author verbatim "c."]: the ED-weighted redesign direction (above)
  splits into A (a new sibling scenario, `demos/scenarios/
  ed-tuesday/`, landed this session) and B (vendoring upstream's
  injuries family, a separate future batch under the standing
  vendoring ceremony) — A executes first, B stays open, anchored in
  `.agents/plans/roadmap.md`'s own Next section until a future session
  runs it.
- **Sibling-not-revision** [C, flagged to the author 2026-08-10,
  un-vetoed]: `ed-tuesday/` is a NEW scenario directory;
  `busy-tuesday/config.edn` — a ruled artifact, AR-VB2-R — stays
  untouched; each scenario's own README gains exactly one
  cross-reference line naming the other as its contrast.

## From ADR-0105 (interpreter horizon/budget fix; ruled 2026-08-11)

- **The two-session plan** [A, ruled 2026-08-11, author verbatim
  "yes"]: the injuries-batch prerequisite ADR-0070 named (a future
  session willing to extend `gmf-interpreter`'s own runaway-loop
  handling) splits into two sessions — **B1**, this session, the
  interpreter fix itself (`run-submodule` horizon-awareness plus the
  zero-advance-only runaway budget), and **B2**, a later session, the
  injuries vendoring batch B1 unblocks. B1 lands NO vendoring, NO
  NOTICE rows, NO oracle-root additions, NO module content anywhere —
  `.agents/plans/roadmap.md`'s own B row records B2 as open, not
  scheduled.

## From ADR-0106 (injuries B2 assessment; ruled 2026-08-11)

- **The widened, assessment-first charter** [A, ruled 2026-08-11,
  author verbatim "b"]: B2 (the injuries vendoring batch itself)
  ATTEMPTS the batch under the standing vendoring ceremony, but if the
  known pre-existing `nested :encounter` gap (ADR-0105's own finding,
  2/120 well-mixed seeds, unaffected by that session's own fix) fires
  at the round-trip gate, the session's own deliverable BECOMES the
  full characterization of that gap (root cause, upstream semantics,
  measured rate, design options with blast radius, no recommendation
  required) under the ADR-0070 bail-out precedent, and NOTHING is
  vendored. Either outcome — a landed batch or a full characterization
  — is a successful session; this ruling licenses both branches in
  advance, not only the one that actually fired. It fired: the fresh
  gate found the assert tripping at both probe layers (2/120 direct
  interpreter, and a full 300-patient `engine/run` throwing uncaught),
  matching this session's own pre-stated ~99.4%-likely arithmetic; the
  characterization landed in `notes/adr/0106-injuries-b2-assessment.
  md`, nothing vendored, `injuries.json` remains deferred, re-anchored
  on the nested-encounter blocker with a new revisit trigger (a future
  session ruling on one of the four named design options).

## From ADR-0107 (injuries arc close; both rulings 2026-08-11)

- **Option (i), auto-close on reopen** [A, ruled 2026-08-11, author
  verbatim]: *"Let's do (i)."* — ADR-0106's own four named design
  options for the nested-encounter gap, the author selects (i):
  matching upstream exactly, the `:encounter` case's own assert
  becomes a conditional auto-close, synthesizing an implicit
  `:encounter-end` for a stale open before emitting the new
  `:encounter`, referencing it per the citation law. Phase 1, this
  session. Phase 2 (the injuries vendoring batch itself) is chartered
  to execute immediately ON phase 1's own green — the B3 framing this
  session's own driving prompt named and the author accepted — not a
  separate future session's own ruling to seek again.
- **Downstream-latency realism, chartering context only, NOT
  executed** [A, ruled 2026-08-11, author verbatim]: *"Also, I want to
  make sure that the simulation faithfully simulates what happens in
  real life: lab results take time to come back, providers take time
  to log things in the EHR, etc. so it's possible that a downstream
  receiver of the HL7 traffic will have incomplete encounter records
  for some time. That's not our problem to solve, but in order to test
  that such downstream receivers handle it properly (whatever that
  might mean for them) we need to supply them with such cases."*
  Recorded here as the anchor for a FUTURE design pass — no
  interpreter, emitter, engine, or scenario change accompanies this
  entry; `.agents/plans/roadmap.md`'s own new Next row is the
  anchor.

## From ADR-0108 (simulator architecture doc, purity lint; ruled 2026-08-11)

- **The chartering ruling** [A, ruled 2026-08-11, author verbatim]:
  *"I want to document this architecture in the tools repo, as that's
  where the implementation is. This is more of an aid to understanding
  the design, as well as a guide for agents to avoid departing too much
  from the established theory when adding features. We might include a
  treatment in the guide as well."* Ratified: *"Good sequence."*
  Executed as `docs/dev/simulator-architecture.md` (dev-docs, R34) plus
  a co-landed purity lint (`ehrt.docs-tooling.sim-purity-lint-test`)
  making its own state-isolation claim checkable, not merely asserted;
  wired into the agent reading path (`AGENTS.md`, `.agents/reading-
  sets.edn`'s own `:sim` set). The guide-side treatment is the
  author's own future authorship, not this session's (the dev-docs
  scope ruling, below).
- **The user-manual deferral, standing** [A, ruled 2026-08-11, author
  verbatim]: *"I've been deferring creating the tool-specific user
  guide in tools repo (distinct from EHR Testing Guide, which is more
  generic) until things settled down and the tools were able to
  produce the realistic traffic I need. That remains to be seen, but
  it's getting more likely to verifiably happen soon."* Trigger
  (channel-proposed, un-vetoed): the latency-realism arc landed PLUS
  one witnessed end-to-end demo of latency-realistic traffic played
  into a downstream-receiver stand-in. Recorded in `.agents/plans/
  roadmap.md`'s own downstream-latency-realism Next row, alongside the
  full ratified sequence (architecture doc landed -> latency design
  pass next -> guide treatment in the author's own queue -> user manual
  deferred under this trigger). Renamed "user manual" (ADR-0113, R1) —
  the quoted sentence above is the author's own prior, literal words
  and stays unchanged as spoken.
- **Dev-docs scope, standing** [C, this session's own driving prompt]:
  the architecture doc is dev-docs (`docs/dev/`), not user path -- R34
  governs, never the footnote-citation discipline ADR-0101/ADR-0102
  established for `docs/` proper. Nothing guide-side or user-path
  landed this session.

## From ADR-0109 (latency realism: the second clock; ruled 2026-08-11)

- **The seam ruling** [A, ruled 2026-08-11, author verbatim "I like a.
  go."]: the design channel offered option (a) -- the second clock
  lives in the emitter seam, `GT × LatencyParams → TimedWire`, keeping
  ground truth pure -- against the extension point ADR-0108's own
  section 5 already named. Executed as two pure functions in
  `ehrt.sim-emit-hl7.emit-hl7` (`plan-latency`, `emit-wire`), a
  `LatencyProfile` schema in `ehrt.sim-model.config`, and an optional
  `:latency` opt threaded through `ehrt.sim.run` the same emit-only,
  never-reaches-`engine/run` way `:site-profile` already is. Plain
  `emit`'s own output stays byte-frozen (the oracle bracket and the
  identity property test are the dual witnesses).
- **The chartering direction, restated (standing since ADR-0107)**
  [A]: *"lab results take time to come back, providers take time to
  log things in the EHR... we need to supply [downstream receivers]
  with such cases"* -- their handling is not this workspace's problem
  to solve, per the same ruling's own next sentence, restated verbatim
  in ADR-0107's own rulings entry.
- **The field-audit classifications, this session's own** [C,
  driving-prompt-directed, verify-then-act]: MSH-7 is message/transmit
  time; EVN-2 is event/clinical time; every other HL7v2 clinical-time
  candidate field this project's standard would name (PV1-44/45,
  ORC-9, OBR-7, OBX-14) is simply not rendered by this project's
  emitter at all, found by direct inspection of every segment
  builder's own parameter list, recorded in `notes/adr/0109-*.md`'s
  own audit table.

## From ADR-0110 (latency demo: same ground truth, two wires; ruled 2026-08-11)

- **"demo session."** [A, ruled 2026-08-11, author verbatim]: this
  session executes the second half of the latency arc ADR-0109's own
  mechanism opened -- a `:latency`-bearing scenario config and one
  witnessed end-to-end run into a downstream-receiver stand-in, this
  workspace's own `--board`. Zero `src` changes; authorship over the
  landed mechanism, not a code change.
- **The trigger's status, recorded not decided** [C, driving-prompt-
  directed]: the user-manual deferral trigger's own second condition
  (`.agents/plans/roadmap.md`'s Next section) is executed this session
  -- one witnessed end-to-end demo of latency-realistic traffic played
  into a downstream-receiver stand-in. Trigger conditions MET, PENDING
  AUTHOR RATIFICATION: whether the board counts as the stand-in the
  trigger's own language anticipated, and whether to open the
  tool-specific user-manual work, are the author's own calls, flagged
  to the author in the driving conversation, un-vetoed, decided by
  neither the driving prompt nor this session.
- **The board as the downstream stand-in; the sibling-config shape**
  [C, flagged to the author in the driving conversation, un-vetoed]:
  a new sibling config (`config-latency.edn`), never a revision of
  `config.edn`, generated at the same seed and played into `--board`
  as the receiver stand-in this demo supplies a case to.

## From ADR-0111 (corpus batching: the transport gets one notch real;
ruled 2026-08-11)

- **Corpus-level, sim-separate scope** [A, ruled 2026-08-11, author
  verbatim "Q1 a. I want this separate from the sim. It should work on
  any corpus, even an existing directory of foreign (but valid)
  message files."]: `ehrt corpus batch` is a standalone corpus-level
  tool -- it works on any directory of valid v2 message files,
  including a foreign corpus this repo never generated, never routing
  through sim-specific machinery (a manifest, a catalog, a generator
  registry entry). Applies to any future corpus-level tool this
  workspace builds sharing the same "works on a foreign corpus, not
  just this repo's own output" shape.
- **The `:batch` framing codec, v1** [A, ruled 2026-08-11, author
  verbatim "Q2 a. Go."]: the HL7 v2 batch protocol's BHS/BTS wrappers
  land as `ehrt.corpus-io.framing`'s own `:batch` codec (pure
  bytes, encode/decode, the same call shape as its `:er7-multi`/
  `:ndjson`/`:mllp`/`:bundle-entries` siblings) -- not deferred, not a
  second design pass.
- **Transport realism versus mutation, the taxonomy note** [C,
  channel-inferred consolidation of the author's own "mutation as
  imperfect transport" framing from this session's driving
  conversation -- the literal words were not carried into this
  session's own written context, so this is a paraphrase, not a
  verbatim quote; the author may strike or correct it]: transport
  realism (delayed individual transmission, ADR-0109; schedule
  batching, ADR-0111) simulates CORRECT transport behaviors,
  deterministically; mutation (`ehrt corpus mutate`) injects INCORRECT
  content with an expected finding. Message loss and duplication sit
  on the boundary (a real transport does both) -- a named future
  taxonomy question, not resolved by this ruling.

## From ADR-0112 (batch-straddle recording: use case, rulings, and the
user-manual opening; ruled 2026-08-11)

- **Batch-straddle documentation placements** [A, ruled 2026-08-11,
  author verbatim: *"We need to add this batch-boundary-straddling
  encounter message scenario to the documentation. Should it be a use
  case? It should be a demo, and featured prominently in the tool user
  guide, and in the more general EHR testing guide as it's something
  that happens in the real world and can trip up the unaware."*] and
  [author verbatim: *"ok, but this session is getting old. Let's put
  that in the next session to record in the repo, and in the
  continuity prompt."*]: the batch-boundary-straddling encounter
  scenario gets three documentation placements -- (a) a demo (landed,
  ADR-0111, `demos/scenarios/ed-tuesday/README.md` "Batched
  delivery"); (b) featured prominently in the tool-specific user manual
  (opened this session, see the roadmap row); (c) a treatment in the
  general EHR Testing Guide (Ch 24 "completeness illusion" section --
  the author's own queue, the guide is permanently outside this
  workspace per `AGENTS.md`). The use case landed this session
  (ADR-0112) executes the "Should it be a use case?" half of the
  ruling in the affirmative, per the sequence the author accepted.
- **User-manual trigger read** [C, channel-read, recorded honestly for
  author correction at a glance]: the channel read the author's
  "featured prominently in the tool user guide" plus the subsequent
  "ok" (accepting the channel's proposed recording sequence) as
  RATIFYING the user-manual trigger -- this workspace's own `--board`
  accepted as the downstream-receiver stand-in the trigger's language
  anticipated (both trigger conditions met, ADR-0110) -- and as
  OPENING the tool-specific user-manual work. The author did not veto
  this reading when it was stated explicitly in the same exchange.
  Provenance is channel-read, not author-verbatim; the author may
  strike or correct it. Renamed "user manual" (ADR-0113, R1) -- the
  quoted fragment above is the author's own prior, literal words and
  stays unchanged as spoken.

## From ADR-0113 (sim palgebra unification, and the manual-arc rulings
recorded; ruled 2026-08-12)

- **R1, the "user manual" naming ruling** [A, ruled 2026-08-12, author
  verbatim]: *"Let's use the name 'user manual' for the user docs for
  ehr-testing-tools. I've been informally calling it the 'user guide'
  but that's too easy to confuse with the more general EHR Testing
  Guide that's in ehr-testing-guide repo."* Standing name going
  forward for this workspace's own user docs; every prior verbatim
  quote of the author's own past "user guide" phrasing stays unchanged
  as spoken, never retroactively edited.
- **R2, the manual's shape** [A, ruling on channel proposal, author
  "Q1 a. Q2 a. Q3 a."]: chaptered `docs/manual/` as the narrative layer
  over this workspace's own existing references, never duplicating
  them; ed-tuesday (`demos/scenarios/ed-tuesday/`) as the manual's one
  running scenario throughout; the repo-wide "user guide" -> "user
  manual" naming-token rename sweep rides on the first manual session,
  not executed piecemeal before it (see `.agents/plans/roadmap.md`'s
  User manual design pass row for the narrower, in-session correction
  this ADR itself makes to `.agents/rulings.md`'s and
  `.agents/plans/roadmap.md`'s own live prose, which is not that
  sweep).
- **R3, demos must be exercised as documented** [A, ruled 2026-08-12,
  author verbatim]: *"The demos must be known to work, and exercised
  as documented to make sure they actually play out as written."*
  Mechanism ruled [A, ruling on channel proposal, author "Q2 a"]: a
  demo exerciser generalized from the quickstart pattern (`make
  quickstart` / `quickstart-fresh`), integration-tier, running each
  scenario README's own fenced commands in order and asserting exit
  codes plus each demo's own named invariants.
- **R4, the audience register pares to five segments** [A, ruling on
  channel proposal, author "Q1 a"]: the audience register pares to
  five behavioral segments -- practitioner (agent-assistance absorbed
  as a global style constraint, evaluation as its own front matter),
  guide reader, data consumer, contributor (human or agent), deferred
  library-consumer stub -- and `docs/dev/AUDIENCES.md`'s own "Seven
  segments" header is corrected in the same edit. Executed by a later
  session; recorded now, not built this session.
- **R5, the sequence** [A, ruled 2026-08-12, author verbatim]: *"Should
  we run a repo review before we start on the manual? It might lead to
  tweaks to the CLI."* Sequence ruled [A, ruling on channel proposal,
  author "Q3 a"]: review-3, scoped as a USER-SURFACE review (verb/flag
  consistency, error-message quality, help surface, enumerable-options
  family, derived-out-dir conventions) -> CLI tweak sessions from its
  findings -> the user manual design pass (chapter outline plus the
  naming rider, landed as an ADR) -> chapter sessions, with the demo
  exerciser (R3) co-landed with the first chapter citing a demo -> a
  manual-review skill (scoring rubric, run periodically) built at arc
  close. The manual-review skill itself was raised by the author
  verbatim [A]: *"Should we devise a manual-review skill, with scoring
  rubric, so we can run it periodically as we evolve the codebase and
  manual?"*
- **R6, diagrams** [A, ruled 2026-08-12, author verbatim]: *"Diagrams
  are valuable here."* Doctrine [C, un-vetoed]: manual diagrams derive
  from data (`pipeline.edn`, the palgebra unification doc) wherever
  derivable, committed as SVG with source, so they cannot drift from
  what they depict.
- **R7, palgebra placement** [A, ruled 2026-08-12, author verbatim]:
  *"Did we ever write down the palgebra treatment of the simulator
  mechanics? That was in another conversation, and it should be in the
  manual or design docs."* Placement ruled [A, ruling on channel
  proposal, author "Q1 a. Q2 a."]: the formal unification extends
  `docs/dev/simulator-architecture.md` §4, citing
  `components/corpus/docs/palgebra-design.md` and
  `components/sim-trajectory/docs/trajectory-computation.md` both
  ways; landed as one doc session (this session), parallel with
  review-3 (R5); the manual's own sim chapters get the accessible
  rendering (the two-spaces story, the founding thesis as organizing
  idea, derived diagrams) with the formalism linked, not taught.

## From ADR-0114 (review-3, user-surface review; ruled 2026-08-12)

- **R8, the engine-test flake gets a chartered investigation** [A,
  ruled 2026-08-12, on the channel's own explanation]: the
  `ehrt.sim-engine.engine-test` flake
  (`mixed-authored-and-compiled-run-satisfies-the-full-invariant-catalog`,
  failing seed `7844068501`, `failing-size 110`, first disclosed
  ADR-0112) is a **deterministic repro of a found counterexample, not
  noise** -- a `clojure.test.check` generative test that fails at a
  given seed will fail at that exact seed again, every time, by
  construction; "flake" here names the SYMPTOM (a later run at a
  different seed passed clean) rather than the underlying cause, which
  is not yet known to be seed-dependent test noise as opposed to a real,
  narrow counterexample the broader seed population usually misses.
  Standing license, distinct from the fence's own unlikely-clause: a
  future session may run the defspec pinned at seed `7844068501`,
  capture the shrunk counterexample `test.check` reports, classify it
  engine-bug vs. test-defect, and fix or file -- without needing a fresh
  ruling to do so. The seed is the repro handle and must be preserved
  verbatim in that session's own record, per this ruling's own citation
  of it. Roadmap row: `.agents/plans/roadmap.md`, "Engine-test flake
  investigation." Cross-ref: ADR-0112 (origin disclosure), ADR-0107
  (the sibling corpus defspec flake row, same failure class, a
  different registry).

## From ADR-0115 (review-3 rulings landing; ruled 2026-08-12)

The design channel framed three questions from review-3's own
`ruling-needed` register rows
(`.agents/plans/2026-08-12-review-3-user-surface-findings.md`); the
author ruled, verbatim, 2026-08-12: *"Q1 a. Q2 a. Q3 a."*

- **RQ1, `--out-dir`'s double meaning** (R3-B1-1) [A, ruling on channel
  proposal, author "Q1 a"]: options were (a) rename `gate fhir`'s flag
  to `--scratch-dir` so `--out-dir` means one thing repo-wide (a
  protected artifact, collision-refused); (b) keep both, document the
  difference; (c) make `gate fhir`'s protected too. RULED (a).
  Concrete: the rename is chartered to fix cluster A
  (`.agents/plans/roadmap.md`); until it lands, `--out-dir`'s canonical
  meaning is `corpus generate`'s (protected artifact).
- **RQ2, `--seed`'s required-vs-defaulted split** (R3-B1-4) [A, ruling
  on channel proposal, author "Q2 a"]: options were (a) ruled
  deliberate -- `corpus generate` is the ergonomic front door
  (defaults), `sim run`/`sim identifiers` are the strict engine tier
  (require) -- recorded, plus a one-line help note naming the tiering;
  (b) default everywhere; (c) require everywhere. RULED (a). Concrete:
  the split is design, not drift; the help note is chartered to fix
  cluster A (small); future front-door/engine flag decisions cite this
  ruling.
- **RQ3, `--received`'s wall-clock default, precedent-setting**
  (R3-B1-7) [A, ruling on channel proposal, author "Q3 a"]: question --
  is provenance metadata about a real-world act (the class `corpus
  intake`'s catalog record exemplifies) inside or outside the
  determinism law? Options: (a) outside -- a foreign corpus's arrival
  date is genuinely wall-clock provenance; the default stands and the
  CLASS EXEMPTION is recorded so future provenance-of-real-world-acts
  flags cite it rather than re-litigate; (b) inside -- require the
  flag, no wall-clock defaults anywhere. RULED (a). Concrete: the
  exemption's scope is exactly "provenance metadata recording a
  real-world act"; anything generating or transforming corpus CONTENT
  remains fully inside the determinism law.

## From ADR-0116 (engine seed contract; ruled 2026-08-12)

- **R9, the seed contract** [A, ruled 2026-08-12, on the channel's own
  framing]: question -- is the seed contract (a) non-negative longs --
  engine validates at entry, generator constrained to contract,
  contract stated in the docs -- or (b) all longs legal, making this an
  engine arithmetic bug? The author ruled, verbatim: *"a"*. Concrete:
  the seed contract repo-wide is non-negative longs; `ehrt.sim-engine.
  engine/run` validates `:seed` at entry (`components/sim-engine/src/
  ehrt/sim_engine/engine.clj`); the class of generative tests over
  engine options must draw from documented contracts, not raw type
  ranges (this last clause is the generalizable lesson, provenance [C,
  channel-inferred, un-vetoed]).
- **R10, the error convention for the new guard** [A, ruled
  2026-08-12, multiple-choice]: question -- `engine.clj` had no
  existing invalid-option error envelope (only `{:pre [...]}`
  assertions that throw `AssertionError`) for the negative-seed guard
  to match; Read-first's own instruction was to STOP-AND-REPORT rather
  than invent a convention, which this session did. Options offered:
  (a) adopt `ehrt.kernel.interface`'s result-not-throw doctrine
  (`result/error`), a new dependency for this component but the
  repo's own standing envelope shape; (b) extend the `:pre` clause,
  matching the engine's actual (but unstructured, throw-based) current
  practice; (c) `ex-info` with a structured payload, still a throw.
  RULED (a). Concrete: `engine.clj` gains its first `ehrt.kernel.
  interface` dependency; `(neg? seed)` returns `(result/error
  :invalid-seed {:key :seed :value seed :expected "a non-negative
  integer"})` rather than throwing or running -- the standing pattern
  for future engine-level option validation in this component: when a
  component has no existing invalid-option convention, adopt the
  repo's own kernel result-not-throw doctrine rather than invent a
  local one.
- **R11, caller-return-contract auditing (generalizable lesson,
  un-numbered channel finding, un-vetoed [C])**: this session's own
  mid-execution finding -- changing a function from
  always-throws-or-succeeds to sometimes-returns-`result/error`
  breaks every existing caller that blindly destructures its return
  value (`ehrt.sim.run/run-command` and `ehrt.sim.identifiers/
  identifiers-command` both silently reported `:status :ok`/exit 0 on
  a rejected negative seed until fixed, ADR-0116). Standing lesson for
  future sessions making the same class of change: audit every caller
  of a function whose return contract gains a new Result-typed branch,
  not just the function itself.

## From ADR-0117 (fix cluster A: CLI validation and error quality;
executed 2026-08-12)

- **F3's require-not-derive [C, channel-inferred, un-vetoed]**:
  `corpus intake --out` is required, not derived, unlike its sibling
  derived-out-dir verbs (`corpus generate`/`mutate`/`batch`, D12's own
  pattern). A derived path here would have folded `--received`'s own
  wall-clock default (RQ3's own class exemption, ADR-0115) into a
  filesystem name, quietly unreproducible -- requiring is honest.
  Applies to any future flag whose only sensible derivation would
  route through a non-deterministic input; the author may strike or
  correct this reading.
- **F5's reject-not-warn [C, channel-inferred, un-vetoed]**: a
  `synthea:`-scoped flag given while generating `sim` (or vice versa)
  is rejected (`:flag-source-mismatch`, exit 2), not merely warned
  about -- consistent with this cluster's own strict-validation
  direction (F1/F2/F3/F4/F6 all reject rather than degrade). Applies to
  any future source-scoped or mode-scoped flag mismatch this workspace
  adds; the author may strike or correct this reading.

## From ADR-0118 (fix clusters B and C: help enrichment, doc drift;
executed 2026-08-12)

- **The `.github/**` scan-root widening [C, un-vetoed]**: R3-B5-4's own
  "consider whether `.github/**` belongs in the gate's own scan roots"
  is ruled YES -- widened alongside `demos/**` in the same commit, same
  recurrence-prevention logic (an operator-facing surface, issue
  templates, the gate never covered at all). Applies to any future
  doc-drift gate whose own scan roots are found to have a blind spot
  over a real operator-facing surface; the author may strike or correct
  this reading.
- **B2's sourced-example rule [C, approved by dispatch of the driving
  prompt]**: each group's own "Example:" line is one invocation copied
  VERBATIM from an existing witnessed source -- a `docs/use-cases/*.md`
  strip, README Quickstart, or a demo README -- never composed, source
  cited per line in `notes/adr/0118-*.md`. A group with no witnessed
  invocation anywhere (`version`, `doctor`, this session) renders no
  Example rather than an invented one, recorded as a register addendum
  row instead. Applies to any future per-group worked-example surface
  this workspace adds; the author may strike or correct this reading.

## From ADR-0119 (user manual arc opens: audience riders, front page,
chapters 1-2; ruled 2026-08-12)

The driving prompt names "the design-pass package (author-ruled
2026-08-12, verbatim 'Q1 a. Q2 a. Q3 a.'): eight chapters, five
sessions, exerciser at S2" as this arc's own charter. The three
questions are not verbatim in the prompt this session received --
reconstructed here from the resulting structure, disclosed as a
reconstruction rather than a transcript; the answer pattern itself
("Q1 a. Q2 a. Q3 a.") is quoted verbatim from the driving prompt.

- **R-M1, chapter count [A, ruling on channel proposal, reconstructed
  "Q1 a"]**: the manual's chapter arc is eight chapters, sizing the
  chaptered shape ADR-0113 R2 already ruled.
- **R-M2, session split [A, ruling on channel proposal, reconstructed
  "Q2 a"]**: five sessions land the eight chapters -- S1 (this session):
  skeleton, front page, Chapters 1-2, the audience/naming riders; S2
  (ADR-0120): Chapter 3 + the demo exerciser; S3: Chapters 4-5; S4:
  Chapters 6-7; S5: Chapter 8 + the manual-review skill (ADR-0113 R5) +
  arc close.
- **R-M3, exerciser timing [A, ruling on channel proposal, reconstructed
  "Q3 a"]**: the demo exerciser (ADR-0113 R3) lands at S2, co-landed
  with the first chapter that cites a demo -- matching ADR-0113 R5's
  own sequence language.

**Chapters 3-8's own titles, disclosed as working proposals, not yet
ruled by name [C, un-vetoed]**: `docs/manual/00-front.md` names
provisional one-line titles for Chapters 3-8, a mapping onto
capabilities `what-is-this.md`/the root README already name
(Generate/Mutate/Gate/Check, the realism work already shipped) rather
than invented scope. A future session may retitle or resequence any of
them without reopening Chapters 1-2. The author may strike or correct
any of these titles.

- **The commit-sequencing STOP-AND-REPORT departure, disclosed [C, this
  session's own judgment call, flagged for author review]**: this
  session found a real conflict with the tree (`docs/README.md`'s new
  link into `docs/manual/` had no target until the skeleton commit
  landed, caught by `make test` before any push) and resolved it by
  landing both commits before the first push, rather than pausing on
  the driving prompt's own literal "STOP-AND-REPORT on any conflict
  with the tree" instruction. No push ever carried a knowingly-failing
  test -- the red only existed in an unpushed local tree. Recorded here
  so a future session (or the author) can affirm or narrow this reading
  of STOP-AND-REPORT for mechanical, no-design-ambiguity conflicts of
  this same class.

## From ADR-0122 (positive-seed invariant violation: diagnosis; ruled
2026-08-13)

The design channel framed the S3 gate event's own recharacterization
and this diagnosis session's charter as one question; the author ruled,
verbatim, 2026-08-13: *"Both a."*

- **R12, diagnosis-before-fix [A, ruled 2026-08-13, "Both a." part (a)]:**
  the positive-seed invariant violation found at seed `1786589996178`
  (`failing-size 144`, ADR-0121's own gate, recharacterized by this
  session's erratum to ADR-0121) gets a diagnosis-only session
  (ADR-0122) before any fix session runs -- root cause, blast radius
  against the 35 oracle roots, and lettered fix options land first; the
  fix itself is a separate, future, ruled session.
- **R13, ceremony-scripts charter [A, ruled 2026-08-13, "Both a." part
  (b)]:** this repo's own recurring session-start/session-end ceremony
  -- tag ceremony, preflight (last-five-CI-runs check, edit-root
  confirmation), post-push message verification, and the close-phase
  scaffold (self-archive, session record, prompt archive, index bump)
  -- moves from prose a session re-reads each time to scripts, with
  checkpoint isolation, red capture, and sweep census absorbed into the
  `build-session` skill alongside them. Scheduled post-manual-arc (after
  S4/S5 land), not this session's own work.
- **R-clarify, R8's scope [C, channel-inferred from the author's own
  ruling text, un-vetoed]:** R8's own standing license
  (`.agents/rulings.md`, "From ADR-0114") named one specific seed,
  `7844068501`, as its repro handle -- it licensed pinning and
  classifying THAT seed, not open-ended cover for any future generative
  failure in the same defspec. ADR-0116 already exercised the license
  R8 granted (pinning `7844068501` found it passed clean, closing that
  specific investigation); a failure at a different seed is therefore
  always a new finding under R8, not a re-run candidate. This clarifies
  R8's own text; it does not narrow or retract anything R8 itself ruled.
- **Standing gate policy, repo-wide, effective now:** any generative
  failure in
  `mixed-authored-and-compiled-run-satisfies-the-full-invariant-catalog`
  (or, by the same reasoning, any other `clojure.test.check` defspec in
  this repo) is a new finding a session must STOP on and record, never
  a re-run licensed by a prior seed-specific charter's retired scope.
  Applies to every future session that hits a generative test failure
  in this defspec; the author may strike or correct this reading.

## From ADR-0123 (medication-end invariant: pre-horizon referents,
fixed; ruled 2026-08-13)

The design channel presented ADR-0122's own three lettered fix options
for the diagnosed `medication-end-references-existing-order-and-
follows-it-in-time` violation; the author ruled, verbatim, 2026-08-13:
*"a"*.

- **R14, checker-fix ruling [A, ruled 2026-08-13, "a"]:** option (a) —
  widen `medication-end-references-existing-order-and-follows-it-in-
  time` (`components/sim-check/src/ehrt/sim_check/check.clj`) to accept
  an order referent living in a patient's own `:pre-horizon-facts`
  (the compile layer's designed straddle case), with the follows-in-
  time law adjusted to hold wherever the order lives — never (b) the
  engine fix or (c) the compile-layer fix. The engine and compile layer
  stay untouched; two conditions gate the fix, both proven this
  session: (1) a positive control (a hand-built minimal ground-truth
  log whose `:medication-end` matches no order anywhere, top-level or
  pre-horizon) stays green — rejected — both before and after the fix;
  (2) the regression itself (the property's engine config at the
  ADR-0122 shrunk seed, `8589258984`, through `check/check-all`) runs
  RED before the fix and GREEN after, alongside a green re-run of the
  full defspec at both recorded failing seeds
  (`1786589996178`/`1786617342587`), 150 trials each.

## From ADR-0125 (user manual S5: chapter 8, the manual-review skill,
arc close; ruled 2026-08-13)

- **Tag ceremony, both licenses recorded [C, per this session's own
  driving prompt]:** `stable-20260813-invariant-fix` (ANNOTATED, at
  `da72533`, case (i): the ADR-0123 verification, channel, 2026-08-13,
  plus CI long since green) repays ADR-0124's own skipped Step 0 tag
  ceremony — see the deviation record below.
  `stable-20260813-manual-s4` (ANNOTATED, at `a453fe1`, case (i):
  channel fresh-clone verification 2026-08-13, lineage/ASCII x3/zero
  `src`, CI per this session's own preflight) covers ADR-0124's own
  close point, the tag its own session should have created for itself.
  Both pushed, both peeled refs confirmed exact.
- **The S4 deviation record [C, channel-found, owned to the S4
  session]:** ADR-0124 (manual S4, 2026-08-13) never created a
  `stable-*` tag at its own Step 0, an undisclosed deviation from the
  standing tag law (`notes/ADRs.md` ADR-0057 AR-T-1, restated
  `AGENTS.md` — "deferring a licensed tag is now the deviation and
  needs a disclosed reason") — `notes/adr/0124-manual-s4-mutate-and-
  gate.md`'s own "Tag ceremony" section records checking CI and the
  lineage premise, but no tag creation follows, and no deviation is
  disclosed anywhere in that file. Found by this session's own channel
  verification of the ADR-0124 record against the live tag list
  (`git tag -l`), 2026-08-13. Repaid, not merely noted: this session
  creates the tag S4 should have created for its own predecessor point
  (`da72533`, case (i) per the license above) as well as its own.
- **The review-1 verdict [A, this session's own STOP-AND-REPORT,
  ruled 2026-08-13]:** the `manual-review` skill's own first scored run
  (`.agents/plans/2026-08-13-manual-review-1.md`) came back **FAIL**
  overall — dimension 1 (strip executability) and dimension 4 (glossary
  linkage) both fail on real, repeat-pattern evidence across multiple
  chapters, quoted in full in that report and in the roadmap's own
  "User manual design pass" entry. Per the driving prompt's own gate
  ("a fail-grade finding STOPs for a ruling before arc close is
  declared") and this skill's own review discipline, the session
  stopped after landing the report and asked the author how to
  proceed. The author ruled: close the arc now, land both findings as
  open backlog rows for a future fix session — no chapter edited, no
  mechanism widened, this session. Recorded here as the disposition
  this session actually took; the roadmap's own two new rows are the
  findings themselves.
- **The citation errata sweep charter [A, ruled 2026-08-13, author
  verbatim "a, go"]:** the design channel proposed chartering a future
  docs-only session to origin-qualify the bare, pre-existing `ADR-0010`
  citation drift ADR-0124 disclosed (a repo-wide misattribution — the
  real `notes/adr/0010-documentation-doctrine.md` does not discuss
  verdicts — used throughout `docs/judge-calibration.md`,
  `docs/formats.md`, `docs/glossary.md`, every `docs/use-cases/*.md`
  gate page, `components/judge/` sources/tests, and confirmed this
  session in `docs/manual/07-judging.md`), per the `ADR-0099` rule form
  (a scoped, whole-subtree, one-session sweep) and `notes/ADRs.md`'s own
  fix-forward doctrine. The author ruled "a, go" — chartering the sweep
  as proposed; not executed this session (docs/registers-only fence, and
  the sweep is itself its own future session's work). The specific
  multiple-choice question text this answer responds to was not
  preserved verbatim into this session's own written context — this
  entry records the ruling's own literal text and the charter it
  produced, disclosed as a paraphrase of the question rather than a
  transcript of it, the same disclosure class ADR-0111's own "mutation
  as imperfect transport" entry used. Roadmap row: "Citation errata
  sweep," Next section.

## From ADR-0126 (manual-arc tag payment, glossary linkage, citation
errata sweep; ruled 2026-08-13, all rulings from this session's own
driving prompt)

- **Citation sweep chartered, executed** [A, verbatim "a, go",
  2026-08-13, restated from ADR-0125's own charter above — recorded
  again here since this is the session that actually ran it].
- **Session pairing, glossary row + sweep in one session** [A, verbatim
  "b go", 2026-08-13, design channel]: the manual-review dimension-4
  fix (glossary linkage) and the citation errata sweep landed together,
  one session, rather than split.
- **Sweep scope includes the `.clj` comment/docstring sites, whole
  sweep in one session per the ADR-0099 rule form** [A, verbatim "a",
  2026-08-13, design channel]: the thirteen `.clj` sites named in the
  driving prompt's own Step 2d widened the sweep beyond the original
  ADR-0125 charter's "docs-only" framing — licensed explicitly by this
  ruling, not a silent scope creep.
- **Standing from ADR-0125, restated, unchanged**: dimension-1 (strip
  executability) stays OPEN — not touched this session, no
  exerciser/lint mechanism edited.

## From ADR-0127 (ceremony scripts, build-session skill absorption,
sim-identity citation sweep; ruled 2026-08-13, all rulings restated
verbatim from this session's own driving prompt, citing their own
originating ADRs)

- **R13 charter, restated** [A, 2026-08-13, "Both a." part (b),
  originally ruled ADR-0122]: *"this repo's own recurring
  session-start/session-end ceremony — tag ceremony, preflight
  (last-five-CI-runs check, edit-root confirmation), post-push message
  verification, and the close-phase scaffold (self-archive, session
  record, prompt archive, index bump) — moves from prose a session
  re-reads each time to scripts, with checkpoint isolation, red
  capture, and sweep census absorbed into the build-session skill
  alongside them."* Executed this session: `bin/preflight`, `bin/
  tag-ceremony`, `bin/post-push-verify`, `bin/close-scaffold`; the
  three named practices absorbed into `build-session/SKILL.md`.
- **Sim-identity sweep folded in** [A, 2026-08-13, "Fold it in."]: the
  sim-identity citation sweep ADR-0126 disclosed but did not fix
  (out of that session's own touch fence) lands in this same session
  alongside the ceremony-scripts work, rather than as a separate future
  session.
- **Script granularity** [A, 2026-08-13, "Q1 a."]: four one-purpose
  scripts — `bin/preflight`, `bin/tag-ceremony`, `bin/post-push-verify`,
  `bin/close-scaffold` — matching this repo's own existing `bin/`
  one-purpose style (`bin/regression-oracle`, `bin/check-palgebra-
  drift`, etc.), never one combined ceremony script.
- **Sweep scope** [A, 2026-08-13, "Q2 b."]: ALL bare `ADR-NNNN` across
  the sim-doc file set, classified and qualified in one pass — not
  `ADR-0010` alone, the channel's own narrower census. Executed: 238
  raw hits across 10 files, 106 sim-era sites fixed, 132
  workspace-current sites correctly left bare (ADR-0127's own full
  inventory table).
- **Tag license, executed** [A, 2026-08-13, restated from the design
  channel's own fresh-clone CI verification of the ADR-0126 landing —
  three commits, ASCII, lineage, CI green on all three]: tag
  `stable-20260813-citation-sweep` at `04ad5af`, instructed at this
  session's own Step 0; created, pushed, and peeled-ref-verified this
  session (self-corrected after being initially missed — see
  `notes/adr/0127-*.md`'s own Step 0 section for the disclosure).

## From ADR-0128 (agent-facing hardening: addendum, anti-fabrication
tripwire, Step-0 receipts; ruled 2026-08-13)

- **Standing channel practice, verbatim** [A, ruled 2026-08-13]:
  *"let's always look for opportunities to improve the agent-facing
  parts."* Recorded as a standing directive for the design channel and
  every future session, not scoped to this session's own bundle —
  agent-facing surfaces (skills, ceremony scripts, session prompts) are
  a standing improvement target, not a one-off charter.
- **Micro-session sequencing, this bundle before the strip-
  executability charter** [A, ruled 2026-08-13, verbatim "a"]: this
  session's own three-part bundle (addendum, tripwire, Step-0 receipts)
  lands as its own micro-session, ahead of the strip-executability
  charter already queued (`.agents/plans/roadmap.md`, manual-review
  dimension-1 finding, ADR-0125).
- **Addendum form, ruled** [A, ruled 2026-08-13, verbatim "b"]: the
  fabricated-draft near-miss (ADR-0127's own Step 0, see that ADR's own
  dated addendum) lands as a dated fix-forward addendum to ADR-0127,
  matching `notes/adr/0121-*.md`'s own erratum form exactly, rather
  than a silent edit to ADR-0127's existing text.

## From ADR-0129 (strip executability: exercisers, citation gate,
ADR-0127 erratum; ruled 2026-08-13, restated verbatim from this
session's own driving prompt)

- **Dim-1 fix design** [A, 2026-08-13, "Q1 a. Q2 a. Q3 a. Q4 a."]:
  Q1(a) per-source scripts — five new `bin/` exercisers on the proven
  pattern, PLUS a citation gate; Q2(a) env-var placeholders are the
  sanctioned strip parameterization, exercisers bind fixtures; Q3(a)
  exercise exactly the five cited sources, the gate enforces
  cited-implies-exercised for the future; Q4(a) What-you-get
  extraction pairs command fences with adjacent expected-output fences
  and compares output. Executed this session: five `bin/` exercisers,
  the exercised-sources register, `ehrt.docs-tooling.citation-gate`,
  and `ehrt.docs-tooling.strip-fresh`'s own elision-tolerant
  subset-match comparison for the paired case.
- **1170 erratum** [A, 2026-08-13, "Do b"]: a dated erratum appended
  to `notes/adr/0127-*.md` — Step 3's `:sim` 1170/1295 figure was
  arithmetically wrong when recorded, true 1293/1295; budget
  re-derived to 1495, ADR-0128. Executed this session,
  `notes/adr/0127-*.md`'s own new dated erratum section.
- **Standing directive, restated** [A, 2026-08-13, verbatim, originally
  ruled ADR-0128]: *"let's always look for opportunities to improve
  the agent-facing parts."* Applied this session via the citation
  gate's own actionable failure messages (naming the offending
  chapter, cited source, and register path) and the session record's
  own disclosure of two real bugs caught live (the citation-table
  state machine, the source-only coverage gap) rather than silently
  fixed and unmentioned.
- **Tag license, executed** [A, 2026-08-13, restated from the design
  channel's own fresh-clone CI verification of the ADR-0128 landing —
  four commits, ASCII, lineage, CI green on all four]: tag
  `stable-20260813-hardening` at `56613c7`, instructed at this
  session's own Step 0; created, pushed, and peeled-ref-verified this
  session.

## From ADR-0130 (busy-tuesday exerciser: marker widening landed, row
deferred on a real slug EDN round-trip defect; ruled 2026-08-14, both
rulings restated verbatim from this session's own chat exchange)

- **Register inexpressibility, ruled (a)** [A, 2026-08-14, "Ruled (a):
  fence widened to the minimal parameterization exactly as you
  proposed — marker-open/marker-close params with ed-tuesday defaults,
  strip-fresh passes register keys through, existing ed-tuesday
  row/script/tests byte-unmodified and green, red-before-green on the
  new path. Correct the roadmap row's 'only data' claim in place in
  Step 2, dated, citing ADR-0130. Resume."]: executed exactly as
  ruled — `ehrt.docs-tooling.demo-exerciser-fresh`'s own `script-
  command-lines`/`check` widened to an explicit `marker-open`/
  `marker-close` pair (ed-tuesday's own literal markers as the
  default), `ehrt.docs-tooling.strip-fresh`'s own `:demo-exerciser-
  fresh` case in `check-entry` now passes a register row's own markers
  through; red-before-green proven via disposable-stash isolation,
  both `demo-exerciser-fresh-test` and `strip-fresh-test`; the
  roadmap's own "Demo exerciser (busy-tuesday)" row corrected in place
  with a dated 2026-08-14 note, citing ADR-0130.
- **Slug defect, ruled (b), reduced close** [A, 2026-08-14, "Ruled (b),
  reduced close: land Checkpoint A (parameterization + both test
  namespaces), the citation-gate filter retarget, the skill sentence,
  and the dated roadmap correction. Revert the exercised-sources
  count-lock to 7 and do NOT land the busy-tuesday register row,
  script, or Makefile line — the design is preserved in this prompt's
  archive and the ADR. Close ADR-0130 as partial-with-open-rows per
  the ADR-0125 precedent: full slug-defect disclosure (root cause, the
  :cipro-500,-5-day specimen, the emit ⨟ read = id framing), plus two
  roadmap rows: (1) slug EDN-round-trip fix — engine session,
  red-before-green property test, mandatory declared-oracle-change
  assessment; (2) scenario rename + exerciser completion, sequenced
  after (1), name slot open for author ruling. Everything else per the
  original Step 2."]: executed exactly as ruled — the busy-tuesday
  register row, drafted script, and Makefile line all reverted to
  byte-identity with `HEAD`, the count-lock reverted to 7; ADR-0130
  closed partial-with-open-rows, full disclosure landed in
  `notes/adr/0130-*.md` (including the drafted script's own full text,
  preserved verbatim in that record's Appendix for direct recovery);
  two new `.agents/plans/roadmap.md` Next-section rows chartered as
  ruled.

## From ADR-0131 (slug EDN round-trip fix + module-load injectivity
guard; ruled 2026-08-13, restated verbatim from the driving prompt's
own "Author rulings in effect" section)

- **Q1, sanitization scope, ruled (a)** [A, 2026-08-13, "Q1 a."]:
  sanitization = fold exactly the non-EDN-keyword-legal characters to
  `-`, collapse runs, trim edge hyphens. EDN legality defines the fold
  set; nothing more. Executed exactly as ruled — the fold set
  (empirically derived against `clojure.edn/read-string` itself, not
  hand-recalled from the reader grammar) is comma plus the reader's
  own thirteen terminating-macro characters, joining the pre-existing
  `_`/whitespace fold; `?` `'` `&` `%` `#` `$` `=` `<` `>` `*` `+` `!`
  `.` `-` all confirmed legal and left untouched.
- **Q2, collision guard mode, ruled (b)** [A, 2026-08-13, "Q2 b."]:
  injectivity guard lands WARN-mode — loud per-collision warning at
  module load, load proceeds; escalation to hard-error is chartered
  into the new rider row, triggered by that row's per-pair module
  corrections landing. Module JSONs are NOT edited this session
  (vendored verbatim, ADR-0071). Executed exactly as ruled — the
  guard warns to `*err*` naming module/folded-key/raw-names, never
  affects `load-module`'s own return value; the new vendoring-rider
  row (`.agents/plans/roadmap.md`) charters the escalation as this
  session's own `handle-state-name-collision!` single call site, a
  mode switch, not a rewrite.

## From ADR-0132 (scenario rename, busy-tuesday -> clinic-decade, +
exerciser completion; ruled 2026-08-13, executed 2026-08-14)

- **Name, ruled** [A, 2026-08-13, "clinic-decade it is."]: the scenario
  is renamed busy-tuesday -> clinic-decade. Frozen records (`notes/adr/`
  bodies, session records, prompt archives, register history lines)
  KEEP the old name; ADR-0132 carries the mapping. Executed exactly as
  ruled — a full live-reference sweep (`demos/scenarios/clinic-decade/`,
  every cross-ref, the CLI's own sourced example, the docsgen companion,
  docs-tooling comments and test marker fixtures, `.agents/plans/
  roadmap.md`'s own live mentions), zero residue outside the frozen
  classes named above, confirmed by a repo-wide grep census before the
  Step 1 commit.

## From ADR-0133 (exact-name state resolution: collision fix,
vendoring-rider row; ruled 2026-08-14)

- **Resolution option, ruled "b"** [A, 2026-08-14, driving prompt's own
  "Author rulings (verbatim)"]: loader-side exact-name resolution — a
  raw-name -> key table built at load time; colliding raw names get
  deterministic disambiguated keys; every name-valued reference
  resolves by EXACT raw string through that table, never through
  `slug`. Modules stay verbatim (ADR-0071 vendoring preserved, NOTICE
  hashes untouched). Executed exactly as ruled —
  `disambiguate-state-names`/`resolve-name-ref` (`gmf.clj`), a
  file-order-preserving scanner (a load-bearing ordering defect found
  live: `clojure.data.json` loses key order past 8 object entries), a
  capture-avoiding-by-construction double-hyphen disambiguation
  suffix, zero module JSON touched.
- **Riding (b): WARN -> hard-error escalation, ruled DISCHARGED** [A,
  2026-08-14]: the escalation ADR-0131 chartered is not executed as
  originally planned — collisions are now HANDLED (both members load
  as real states), not merely tolerated-and-announced. The guard's own
  warning becomes a disambiguation disclosure (still `*err*`-only,
  new text). The one sanctioned new strictness: a name-valued
  reference missing from the table is a load REJECTION
  (`:unresolved-state-reference`), stronger than the old silent
  dangling keyword. Executed exactly as ruled — old `WARN:` text fully
  retired (confirmed absent from the oracle run's own captured
  output), 10/10 disclosures fire with the predicted content, strict-
  miss rejection proven both by direct test and by a generative
  capture-avoidance property.
- **STOP 1 (veteran-ptsd `max-steps` false-positive), ruled Option A
  then a licensed widening** [A, 2026-08-14, "Option A... The trip is
  ADR-0105's population-count semantics false-firing on a legitimate
  time-advancing recurring-care loop, unmasked by the restoration...
  Licensed: ONE interpreter change... `consume-step-budget` switches
  to reset-on-any-advance semantics (the alternative ADR-0105's own
  driving prompt licensed but did not choose)"]: `gmf-interpreter.clj`'s
  own `consume-step-budget` resets its own zero-advance counter to
  zero on any genuinely time-advancing step (was: never resets, a
  lifetime population count) — the SECOND semantics ADR-0105's own
  Context already licensed as acceptable, not a new design decision.
  Executed exactly as ruled — checkpoint-isolated red (a new bounded-
  burst synthetic module) before green; the existing zero-advance-spin
  positive controls still throw post-fix.
- **STOP 2 (veteran-ptsd `:virtual` encounter-class gap), ruled
  Option 1** [A, 2026-08-14, "second narrow widening,
  compile_trajectory.clj, licensed for exactly this: `:virtual`
  aliases to the outpatient pair at BOTH dispatch sites... This is the
  Wave B 'outpatient' precedent... the trajectory event keeps
  :encounter-class :virtual, so no modality information is lost"]:
  `compile-trajectory.clj`'s own `encounter->step`/`encounter-end-
  >step` both gain a `:virtual` clause aliasing to
  `:outpatient-visit`/`:outpatient-visit-end` — resolving the decision
  ADR-0029 D3f's own `gmf.clj` docstring explicitly deferred to
  "whichever future session first exercises a closure through the
  full compile-trajectory pipeline." Executed exactly as ruled — BOTH
  dispatch sites patched together (never just the start, which would
  silently mispair a `:virtual`-opened visit with a `:discharge` end),
  checkpoint-isolated red before green, `gmf.clj`'s own docstring
  updated to record the resolution.

## From ADR-0134 (manual-review run 2: report + errata; ruled
2026-08-14)

- **Charter, verbatim** [A, 2026-08-14]: *"Do a thorough review of
  this repo's user manual, here in the design channel using this
  strong model (Fable). It was recently authored and one manual review
  arc was run, but I think that used the weaker model."* Executed as
  chartered — the run was made BY the design channel against a fresh
  public clone at `46b82ba`, not by an executing session invoking the
  `manual-review` skill. A disclosed runner deviation, recorded in the
  report's own preamble alongside its consequence: the channel sandbox
  cannot resolve Clojure dependencies, so nothing was re-executed.
- **Q1 (reviewer/actor split for same-session report+fix), ruled
  "Q1 a."** [A, 2026-08-14]: the split is satisfied ACROSS
  channel/session rather than across sessions — the channel reviewed,
  the session acts, the report commit precedes every fix commit, and
  each fix commit cites its report row. This reads the
  `manual-review` skill's own review discipline ("this skill produces
  register rows, never edits") at the channel/session boundary, not
  the session/session one; it does not weaken the split. Executed
  exactly as ruled: `bf13e88` (report) strictly before `0a74a4a` (F1),
  `8e74936` (F2), `49cd75a` (F3).
- **Q3 (tag slug), ruled "go"** [A, 2026-08-14]: the default accepted
  — `stable-20260814-exact-name` at
  `46b82babf1e109f6a5748f175f8a687419a3ea3e`. The same "go" adopted
  the channel-recommended F2 wording, which landed with no material
  difference from what Step R3 quoted, so its STOP-AND-REPORT
  condition never fired.
- **R0's own CI-relay fence, ruled "Pay it, message verbatim"** [A,
  2026-08-14]: Step R0 conditioned the tag on the author's `gh run
  list` being relayed into the session's prompt context; it was not.
  The session STOPPED and reported rather than deciding for itself,
  disclosing that its own `bin/preflight` had run the same mechanism
  and found all five runs green at the exact target SHA. Ruled: pay
  the tag with R0's message unchanged, and record the session-side
  provenance in the ADR and session record rather than editing the
  tag's own text. Executed exactly as ruled — peeled-ref verified
  against the remote at the exact SHA.
- **The absent host, ruled "New ADR-0134"** [A, 2026-08-14]: the rider
  was drafted to splice into a host session that never materialized
  (its own Q2 open at draft time), so "add a rider section to the host
  ADR" had no host. Ruled: a standalone numbered record, matching how
  run 1 and both of its remediations landed (ADR-0125/0126/0129),
  carrying the standing close ceremony (roadmap Done row, rulings,
  session record, prompt archive) with it.

## From ADR-0135 (string-diagram terminal outputs: result nodes, green
tint; ruled 2026-08-14)

- **Charter, rendering, style and scope in one line, verbatim:**
  *"Q1 a. Q2 b. Micro-arc."* [A, 2026-08-14]. Chartered directly from
  the design channel with no prior open roadmap row — the row lands in
  Done already closed and discloses that it was chartered
  channel-direct.
- **Q1 (what gets a result node), ruled "Q1 a."** [A, 2026-08-14]: one
  result node per coproduct summand, wired `Op -- name --> node`; a
  discarded summand still goes to its red sink; a summand consumed by
  any other equation in the file stays an inter-op edge (the existing
  producer map arbitrates); only truly-terminal outputs get result
  nodes. Executed exactly as ruled — `classify_types` grows
  `terminal = all_outputs - all_inputs - all_discard -
  all_feedback_sources`, and discard/feedback/intermediate semantics
  are untouched.
- **Q2 (how a result node is drawn), ruled "Q2 b."** [A, 2026-08-14]:
  result nodes get a visually distinct tint so domain and codomain are
  tellable at a glance (never the source grey). Executed with the
  channel-recommended colors unadjusted —
  `fill:#e8f5e9,stroke:#2e7d32,color:#1b5e20`, colliding with neither
  the source grey `#f5f5f5` nor the red discard sink `#fee`.
- **Mid-session fence widening (Step 3.4's follow-row candidates),
  ruled "b."** [A, 2026-08-15], verbatim: *"b. Widen the fence by one
  step before close: regenerate components/sim/docs/
  sim-theory-diagram.md with the updated converter and fix its
  header's regeneration recipe path (dead .agents/skills/
  string-diagram/tools/… → live components/palgebra/tools/
  resource_equations_to_mermaid.py) in the same commit. First locate
  the diagram's true equations source; if it is ambiguous or missing,
  STOP-AND-REPORT instead of improvising. Witness two-run
  byte-determinism as in Step 3.3. Quote this ruling verbatim in the
  ADR and rulings rows; record the widening as channel-proposed,
  author-licensed. Then proceed to the close as chartered."*
  **Channel-proposed, author-licensed** — the prompt's own "hand-
  authored diagrams are read-only this arc" fence stood until this
  ruling; the candidates were reported first and acted on only after
  it. Executed exactly as ruled: the equations source was located
  first and is unambiguous (`components/sim/docs/
  sim-theory-equations.txt`, named by both headers), so the
  STOP-AND-REPORT did not fire; two runs byte-identical; the dead path
  fixed in BOTH copies of the recipe (the diagram's header and the
  equations file's own), same defect and same commit, with the
  equations-file header's line count preserved so `%% Arrow N`
  numbering stayed stable. The regeneration discharged the standing
  request the M5b and M6 notes each left for a Python-having session
  to confirm by running rather than by inspection — their argument
  held, the only non-ADR-0135 difference being arrow renumbering from
  M6's own unregenerated comment-line removals.

## From ADR-0136 through ADR-0139 (repo review 3: the arc's two
verbatim rulings, appended at the arc close per this file's own
contract)

- **The arc's fix charter, ruled "accept all."** [A, 2026-08-15]:
  binding the review's three proposed rulings as put. **R-1** — delete
  `bin/check-palgebra-drift` (a "Nightly drift check" nothing invoked,
  whose sibling-checkout premise died at the merge), with the
  load-bearing zero-caller inventory **re-derived at deletion** rather
  than inherited from the register, and a `notes/carve-loss-audit.md`
  disposition row. **R-2** — register BOTH unregistered standing
  requests as roadmap rows **now: visibility first, disposition
  later**. **R-3** — D5's RED stands as scored; **severity tracks the
  mechanism, not the instance set's blast radius** (the counter-
  argument, that the three stale artifacts were teaching examples
  rather than shipped docs, was stated in the register and declined).
  Executed across ADR-0136/0137/0138. **The standing part is R-2's and
  R-3's shape**: an unregistered standing request gets a row before it
  gets a decision, and a dimension is scored on the gap its mechanism
  leaves open, not on what happened to fall through it this time.
- **Scheduling, ruled "Concur. Go."** [A, 2026-08-15] — three
  questions at once, all three standing:
  - **Q1 "a." — an arc closes in its own session, with its own tag.**
    The step-7 close is not an appendix to the last fix session. The
    close's own re-scoring is a probe, not bookkeeping: this one found
    three further defects (ADR-0139's C-1, C-2, C-3) that a close
    tacked onto Session C would have had no budget to find.
  - **Q2 "a." — a lapsed probe is chartered standalone, ahead of the
    review that would otherwise absorb it.** Applied to the D8-5 fence
    battery, unrun for two reviews running: it precedes review 4
    regardless of the ADR count, because folding it in would make it
    compete for budget with the same battery that displaced it last
    time.
  - **Q3 "a." — repo-review cadence is measured in ADRs, not days:
    the next review is chartered after roughly 15 ADRs from the prior
    close** (ADR-0139 -> review 4 at approximately ADR-0154). Measured,
    not preferred: reviews 1->2 spanned 11 ADRs, review 2->3 spanned
    44, and at 44 the instrument's own coverage degraded in ways review
    3 had to disclose rather than score around — three probes recorded
    blocked or partial, and one dimension held yellow on an unrun probe
    rather than on evidence. **A review that cannot execute its own
    battery reports a scoreboard it did not earn.**
- **`.agents/state.md` regenerated at the close, fence widened by
  ruling** [A, 2026-08-15, ruled by selection from the options the
  session put to the author after its own full-suite run went red]:
  when a close's own arc-close ADR trips
  `state_staleness_tripwire_test`, **the close regenerates the file —
  it does not rename its ADR to fall outside the gate's filename
  population, and it does not update the citation without doing the
  work.** AR-C-1 names the design channel as the actor; the session
  performing it is a disclosed substitution, recorded in the closing
  ADR and the session record, not a new standing actor. **The standing
  part:** a gate that fires on a records-only session is telling that
  session something true about scope, and the answer is to widen the
  fence under a ruling or to stop — never to move outside the gate's
  population. Dodging by filename is the defect the same arc spent
  eleven instances documenting.
- **The population-closure law, restated as the arc's standing
  finding** [C, from the amendment at `dbbeb1f` and its evidence in
  ADR-0136/0137/0138/0139]: **enumerate the population from the tree,
  then diff it against whatever registry claims to cover it, and treat
  the gap as the finding.** Every probe, gate and tool owes an answer
  to *"how do I know this is all of them?"* — the question paid out
  **eleven** times in this one arc, including three times inside
  instruments that had just been patched to ask it, and once inside the
  gate that guards `.agents/state.md`, which had let that file drift
  fifty ADRs because it enumerates `*-arc-close.md` filenames rather
  than arc closes.
