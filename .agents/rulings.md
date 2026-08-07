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

- **Dependency-review cadence, standing** (A-3, from ADR-0050 AR-F1-6a):
  `clojure -M:poly libs :outdated` is report-only, run at each arc close
  plus mandatory before any publish; upgrades are never taken as a side
  effect of running it.
- **Pairing-as-data registry landing spot, accepted** (D-3, from
  ADR-0050 AR-F1-6b): `judge` is the accepted acyclic home for the
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
