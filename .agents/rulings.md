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
  citing `notes/adr/0047-scaffolding-compaction-c.md` Step 0 as the
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
- **The user-guide deferral, standing** [A, ruled 2026-08-11, author
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
  pass next -> guide treatment in the author's own queue -> user guide
  deferred under this trigger).
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
  directed]: the user-guide deferral trigger's own second condition
  (`.agents/plans/roadmap.md`'s Next section) is executed this session
  -- one witnessed end-to-end demo of latency-realistic traffic played
  into a downstream-receiver stand-in. Trigger conditions MET, PENDING
  AUTHOR RATIFICATION: whether the board counts as the stand-in the
  trigger's own language anticipated, and whether to open the
  tool-specific user-guide work, are the author's own calls, flagged
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
user-guide opening; ruled 2026-08-11)

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
  delivery"); (b) featured prominently in the tool-specific user guide
  (opened this session, see the roadmap row); (c) a treatment in the
  general EHR Testing Guide (Ch 24 "completeness illusion" section --
  the author's own queue, the guide is permanently outside this
  workspace per `AGENTS.md`). The use case landed this session
  (ADR-0112) executes the "Should it be a use case?" half of the
  ruling in the affirmative, per the sequence the author accepted.
- **User-guide trigger read** [C, channel-read, recorded honestly for
  author correction at a glance]: the channel read the author's
  "featured prominently in the tool user guide" plus the subsequent
  "ok" (accepting the channel's proposed recording sequence) as
  RATIFYING the user-guide trigger -- this workspace's own `--board`
  accepted as the downstream-receiver stand-in the trigger's language
  anticipated (both trigger conditions met, ADR-0110) -- and as
  OPENING the tool-specific user-guide work. The author did not veto
  this reading when it was stated explicitly in the same exchange.
  Provenance is channel-read, not author-verbatim; the author may
  strike or correct it.
