# Roadmap — rolling plan and backlog

Updated by sessions in the same commit as work that changes a row. Successor to the
design channel's chat-resident ledger (retired 2026-08-01). Cite sources; one line
per item; done items move to the bottom of their section with a date and sha.

## Now (in progress)
- Nothing in progress at this close (fidelity arc close, ADR-0084,
  2026-08-08).

## Next (backlog, no session scheduled)
- The lookup-column `time` gap (named in the schema-invalid family
  backlog since ADR-0039, still untouched — Wave I's own six
  mechanisms didn't cover it). Bulk vendoring (batched by closure
  family) follows once the catalog fully walks. **Ratified as real**
  (design channel, 2026-08-06, `notes/adr/0066-player-fold.md` AR-BB1-R)
  — discharges the `[unverified]` intake note ADR-0064 carried for it;
  scheduled after the player-fold arc, still not built.
- **Wave G attachment deferral** (ADR-0037 AR-4, named trigger "multi-
  module assignment per patient"): upstream's own all-waiting-modules-
  attach-to-one-visit semantics only diverges from this project's
  per-module wait when one patient runs multiple modules concurrently —
  the engine's current one-module-per-patient assignment never
  exercises this, so it is deferred, not built. Revisit trigger: a
  future session that assigns more than one module to the same patient.
- Storefront demo fixture: minimal clean-gating FHIR fixture so the README's mutate
  demo shows a real accepted→rejected flip (2026-08-01 capture session finding).
  Also the named landing spot for the pairing registry's own FHIR rows and the
  tier-two coverage-to-gate promotion (2026-08-08, `notes/ADRs.md` ADR-0088,
  AR-PD-2/AR-PD-4) — this session deliberately lands v2-only.
- make quickstart → nightly integration workflow + single-```sh-fence guard in README
  (quickstart_fresh docstring corrected in same change)
- generator-source three-concerns split (ADR-0017 named-future)
- ehrt.corpus.display placement — presentation-leaning (ADR-0018 named-future)
- Markdown-table helper dedup (ADR-0018 named-future)
- **Corpus player: sim event-log input adapter** (`notes/adr/0014-
  corpus-player.md`) — an input adapter reading the sim's own event
  log directly, as opposed to the HL7 v2 file/directory input the
  player accepts today; named in ADR-0014's own Context as remaining
  work, not built.
- **Fixture relocation** (2026-08-08, fidelity riders, `notes/ADRs.md`
  ADR-0081, author backlog addition): move test fixtures out of
  components — the `components/corpus/test-fixtures/v2/simhospital`
  tree and its `components/corpus/test-fixtures/v2-nist` sibling,
  named explicitly — to a top-level home, so demos can use them.
  Flagged wrinkles: both trees are NOTICE/PROVENANCE-hashed and
  `-text` protected in `.gitattributes` (`v2/*.hl7`,
  `v2/simhospital/messages.out`, `v2/simhospital/LICENSE`,
  `v2-nist/covidELR/*.txt`, `v2-nist/COVID19_ELR-v2.3.1/**`), so the
  demos-front-door mechanic
  applies (ADR-0073: same-commit `.gitattributes` moves, byte-
  witnessing, pointer-README stubs) — and the live-path lint's
  blessed roots (`components/docs-tooling/test/ehrt/docs_tooling/
  test_source_live_path_lint_test.clj`'s `"test-fixtures"` allowlist
  entry) update with the move. Not built; a design/landing session of
  its own.
- **ADR references in user-facing documentation** (2026-08-08,
  fidelity riders, `notes/ADRs.md` ADR-0081, author backlog addition):
  remove bare `ADR-NNNN` citations from the user path (`docs/` proper,
  per ADR-0010's own audience fork — NOT `docs/dev/`; e.g. `docs/site-
  profiles.md`, `docs/judge-calibration.md`, `docs/glossary.md`,
  `docs/formats.md` all currently carry them) or convert them to
  clickable footnote links. Unruled fork: strip to dev-docs only, vs.
  footnotes that keep provenance. Prerequisite, not yet done by this
  row: a full inventory of every citation across the user path per
  ADR-0010's own three-way split (a fresh grep at design time will
  turn up more than the four named here, including `docs/use-cases/`
  siblings). Awaiting its own design moment, not built.

## Externals (author-only)
- Enable GitHub's workflow-failure notification email for this
  repository (one settings toggle) — closes the nobody-watching gap
  ADR-0075 named at zero session cost; named quality riders AR-QR-3,
  2026-08-07.
- NIST licensing inquiry: send the drafted gist (retires the confirmation-pending
  posture cited on the storefront Gate row)
- IG pinning: choose and commit the profile-tier conformance target (Gate row's
  other caveat)
- Clojars publish, when satisfied with the product (ruled 2026-07-31; ends the
  greenfield era — output formats freeze harder after first tag). **Dated note
  (D1a rider, 2026-08-02): this row IS the Clojars-vs-Maven-Central ruling —
  cross-referenced into `notes/ADRs.md` ADR-0001's own H5 entry today, closing
  that half of H5 as an open gate; the group/coordinates naming half and
  publication itself both stay open/parked, unchanged by this note.**
- SETUP rewalk by an unspoiled human reader (F3 superseded-pending-rewalk)
- Upstream the adapted repo-adaptation skill to pragsmike/skills (and cyberneutics
  if wanted) — AUTHOR ACTION named 2026-08-01
- Item 9 (ADR-0024, landed 2026-08-01 as mirror-with-gate, not symlinks): the
  fresh-session discovery probe is DONE — see Done section below. The
  "fast-forward /mnt/c" remainder is CLOSED (2026-08-05, scaffolding
  compaction C, `notes/ADRs.md` ADR-0047 AR-C-3): `/mnt/c` itself
  retired, so there is nothing left to fast-forward.
- **RESOLVED 2026-08-05** (scaffolding compaction C, `notes/ADRs.md`
  ADR-0047 AR-C-3): the standing-cost question this row posed — does
  `/mnt/c` still earn its keep — is answered: retire it. `bin/sync-
  mnt-c` deleted; the guarded-mirror doctrine retired from
  `.agents/skills/build-session/SKILL.md` (both copies) with a dated
  note. The physical directory's own deletion on the Windows side
  stays the author's own act, per this ruling.

## Deferred (explicitly, with revisit triggers)
Rows here are LIVE. Closed rows move to Done with their notes.
- **`EncounterEnd` no-op-when-nothing-open** (2026-08-07, vendoring
  batch 2, `notes/ADRs.md` ADR-0071, the `anemia___unknown_etiology.
  json` bail-out finding): upstream Synthea's own `EncounterEnd` idiom
  "close the encounter IF one is open, else no-op" (e.g. `anemia/
  anemia_sub.json`'s own `End Any Active Encounter Just In Case`)
  compiles here as an UNCONDITIONAL `:encounter-end` —
  `ehrt.sim-trajectory.gmf-interpreter/emit-and-advance`'s own
  `:encounter-end` case never checks whether `index-of-last-open-
  encounter` actually found one before emitting, producing a dangling
  `:discharge` that trips `ehrt.sim-check.check`'s own
  `:discharge-follows-admission` invariant at population scale (12,
  17, and 6 violations of 300 patients across three seeds tried).
  Blocks `anemia___unknown_etiology.json` (deferred whole, not
  vendored) and any future module whose own closure reaches this same
  idiom. Revisit trigger: a future session willing to extend
  `emit-and-advance`'s own `:encounter-end` case to no-op (open design
  question: silently drop the event, or attach a `:no-op true` marker)
  when no encounter is open.
  **Dated note (2026-08-07, vendoring batch 3, `notes/ADRs.md`
  ADR-0072): a SECOND blocked module, `colorectal_cancer.json` —
  unlike `hypothyroidism.json`'s own clean call path through the same
  shared `anemia/anemia_sub.json` submodule, `colorectal_cancer.json`'s
  own call sometimes lands outside an open encounter (2 of 3 seeds
  tried rejected at 300 patients, not universal every seed the way the
  first finding was, but a real, non-negligible population-scale rate)
  — same root cause, not a new gap. Revisit trigger unchanged.**
  **Dated note (2026-08-08, fidelity riders, `notes/ADRs.md` ADR-0081):**
  the revisit trigger fires — a design brief
  (`.agents/plans/2026-08-08-encounterend-design.md`) proposes real
  openness tracking in the walk state (an open-encounter index set on
  `:encounter`, cleared on the matched `:encounter-end`) and a compile
  rule that no-ops `:encounter-end` when nothing is open, gated by
  author rulings R1 (wellness arms), R2 (suppressed-end visibility),
  R3 (acceptance bar) — all three ruled in ADR-0081. The fix session
  itself is licensed but not yet run.
  **Dated note (2026-08-08, `notes/ADRs.md` ADR-0082, the EncounterEnd
  fix): the interpreter gap itself is CLOSED (see Done's own
  `- 2026-08-08 — encounterend-fix — ADR-0082` pointer for the fix
  landing; this row stays live, narrowed to colorectal's own remaining
  blocker below)** — `open-encounter-index` (a pure
  walk-level fold, retiring `index-of-last-open-encounter`) plus the
  A1/A5 compile-arm split land; `anemia___unknown_etiology.json` is
  confirmed CLEAN post-fix (0 violations at all three of ADR-0071's own
  seeds, in-session proof, ADR-0082) — ready for its own vendoring
  rider. `colorectal_cancer.json` is NOT: its own residual violations
  (`:clinical-content-only-when-admitted`, plus one early
  `:discharge-follows-admission`) persist BYTE-IDENTICAL pre- and
  post-fix at ADR-0072's own seeds — confirmed, via a raw-trajectory
  scan, to be UNRELATED to the dangling-`:encounter-end` gap this fix
  closes (the fixed interpreter's own raw walk is dangling-reference-
  free for every one of colorectal's 300 seed-42 patients) — a NEW,
  separate, still-open defect, one compile layer downstream
  (`compile-trajectory` or the engine, not yet localized), found as a
  byproduct of this session's own in-session proof and NOT fixed here
  (this session's own fence, AR-EE-6). Revisit trigger, narrowed:
  `colorectal_cancer.json`'s own clinical-content-outside-admission gap
  needs its own diagnosis before it can vendor; `anemia___unknown_
  etiology.json` needs none.
  **Dated note (2026-08-08, fidelity payoff, `notes/ADRs.md` ADR-0083):
  this row CLOSED — see Done, below — both modules it ever blocked are
  resolved, neither by extending this row's own revisit trigger.**
  `anemia___unknown_etiology.json` vendors clean (AR-FP-1, this
  session). `colorectal_cancer.json` — this row's ONLY erratum, dated
  and append-don't-erase — was NEVER actually blocked by this gap: the
  same in-session raw-trajectory scan that cleared `anemia___unknown_
  etiology.json` (ADR-0082, cited two notes above) found ZERO dangling
  `:encounter-end` references anywhere in `colorectal_cancer.json`'s
  own 300 seed-42 walks, and its own violations sit BYTE-IDENTICAL
  before and after the fix landed — a fix that had nothing to correct
  there. ADR-0072's own diagnosis ("same root cause, not a new gap",
  the dated note two above) was plausible BY ADJACENCY — the same
  shared `anemia/anemia_sub.json` submodule, the same violation
  invariant family — never itself probe-verified by a trajectory scan
  the way `anemia___unknown_etiology.json`'s own finding always was;
  this session's own probe is the first scan colorectal's blocker ever
  received, and it overturns the inference. Colorectal's real blocker
  moves to its own row, under its own true name, below.
- **`colorectal_cancer.json`'s own `:clinical-content-only-when-
  admitted` gap, true name, undiagnosed** (2026-08-08, fidelity payoff,
  `notes/ADRs.md` ADR-0083, corrected from the closed `EncounterEnd`
  row above): `colorectal_cancer.json` is deferred whole, NOT vendored
  — not blocked by the (now-closed) EncounterEnd gap, per the erratum
  above, but by a separate, still-undiagnosed defect one compile layer
  downstream of the interpreter (`compile-trajectory` or the engine,
  not yet localized): `ehrt.sim-check.check`'s own
  `:clinical-content-only-when-admitted` invariant (plus one early
  `:discharge-follows-admission`) rejects at 2 of 3 seeds tried
  (20260802, 42; 300 patients each, ADR-0072's own original counts,
  reconfirmed byte-identical post-fix by ADR-0082). Clinical content is
  compiling or replaying as though outside an open encounter — the
  mechanism is unknown. Revisit trigger: a future session's own
  dedicated investigation of this violation class against
  `colorectal_cancer.json`'s own closure — intake for the fidelity
  arc's own close (ADR-0084).
  **Dated note (2026-08-08, colorectal investigation, `notes/ADRs.md`
  ADR-0085): DIAGNOSED, not fixed — row stays LIVE.** The mechanism is
  now named: `ehrt.sim-trajectory.compile-trajectory/compile-
  trajectory`'s own legacy `:pre-horizon` drop gate tests only an
  event's own flag, with no back-reference check against the encounter
  it belongs to — an `:encounter` opened PRE-horizon (dropped) whose
  own `:encounter-end` and intervening clinical content fire
  POST-horizon (compiled normally) produces clinical-content and
  terminal-discharge steps with no matching compiled admission step,
  confirmed across 100% of the violating population (2 of 2 distinct
  patients, both seeds, three-layer probe evidence in ADR-0085). The
  truncation hypothesis ADR-0082 AR-EE-1a raised is CONFIRMED but
  narrower than stated: the `:pre-horizon` gate is the real mechanism,
  in a straddling-encounter shape that finding never exercised;
  `encounter-closed?`'s own single-encounter scope plays no defective
  role. Revisit trigger, narrowed to a fix session: two candidate fix
  shapes named in ADR-0085 (synthesize a compiled opening step for a
  straddling encounter, or generalize the Wave H `history-phase?`
  back-reference principle to the legacy path) — a genuine design
  choice for the design channel to rule on, not mechanical follow-
  through.
  **Dated note (2026-08-08, straddle fix, `notes/ADRs.md` ADR-0086):
  this row CLOSED — see Done, below.** The author ruled shape (b) —
  generalize `history-phase?`'s own back-reference principle to the
  legacy path — accepted now, shape (a) recorded (see the carry-across
  row, below). `colorectal_cancer.json` is clean (`:status :ok`, 0
  violations) at all three seeds (20260802, 1, 42), 300 patients each.
  The blast-radius probe's one predicted mover (`sleep-apnea`, a
  latent, already-shipped defect the oracle's own byte-digest checks
  could never catch) was licensed by name and confirmed exactly; all
  27 other oracle roots stayed byte-identical.
- **Corpus player `:mllp` transport sink** (`notes/adr/0014-corpus-
  player.md`, deferred whole per that session's own bail-out
  procedure): `:mllp` already exists as a *framing* (byte-level
  0x0B/0x1C 0x0D envelope, `ehrt.corpus-io.framing`) but there is
  no `:mllp` *sink kind* in `ehrt.corpus-io.source-sink`'s own
  `known-sink-kinds` (`#{:dir :file :stdout :blaze}`) (both namespace
  citations in this row corrected 2026-08-05 — the source-sink form at
  ADR-0049, the framing form at ADR-0050 register row A-6 — ADR-0014's
  text predates the tools→corpus rename and corpus-io split;
  transcribed faithfully by ADR-0048, corrected fix-forward here) — a
  real network socket write. Building
  one properly touches three namespaces at once (a new canonical
  schema and constructor in `source-sink.clj`, a new
  scheme in `source-sink-url.clj`'s grammar, and a new write function
  in `sink-write.clj`), not a single isolated extension point —
  assessed against the bail-out procedure and judged to balloon past
  "lands small." Deferred whole, not half-built: the player ships
  `--sink dir:`/`file:` only. Revisit trigger: a session needs wire
  transport and a lands-small shape is identified.
- **Carry-across emission** (2026-08-04, `notes/ADRs.md` ADR-0042
  AR-2): a straddling encounter (opens history, closes horizon) yields
  NO in-window wire traffic for that patient under Wave H's own pre-
  roll — real hospital censuses DO show patients mid-stay at window
  open, but building that emission is out of this session's own scope.
  Revisit trigger: a test scenario needs mid-stay-at-window-open
  realism.
  **Dated note (2026-08-08, straddle fix, `notes/ADRs.md` ADR-0086,
  AR-SF-5):** this row's own compile-layer half, recorded, not built —
  shape (a) from ADR-0085's own proposal (synthesize a compiled opening
  step at the horizon boundary for a straddling encounter), the arm the
  author did NOT rule for the legacy path this session (shape (b) was
  ruled instead — see the colorectal row's own closure, above). The
  straddle-detection machinery ADR-0086 lands (a fold-state tracking an
  open pre-horizon-opened span) is the shared prerequisite this row's
  own future emission work would build on. Row stays deferred, trigger
  unchanged.
- **Wellness cadence chronic-meds cap** (2026-08-03, `notes/ADRs.md`
  ADR-0037 AR-1): `EncounterModule.recommendedTimeBetweenWellnessVisits`'s
  own chronic-medications annual cap ("if hasChronicMeds && interval >
  1 year, interval = 1 year", lines 209-211 at the pin) is EXCLUDED from
  `next-wellness-tick` by ruling, not omitted by oversight —
  `active-chronic-medications` exists in this project's own persona/
  attribute model with no input cascade, so wiring the cap in is a
  register item, not a design question. Revisit trigger: a future
  session ranking calibration fidelity for the chronic cluster, or a
  finding that the cap's absence materially skews a census/corpus
  result.
- **Backload named future** (2026-08-03, `notes/ADRs.md` ADR-0031 AR-3):
  pre-roll stays emit-nothing, reaffirmed — no backloaded-history mode
  in the sim. The backload need (pre-window messages for systems that
  ingest historical loads) is a TOOLS-SIDE construction over sim
  output, fault-injection's own sibling, not a sim feature. Revisit
  trigger: a real consumer for pre-window messages appears.
- P2-5 intake staging-dir behavior (deferred 2026-07-31)
- Verdict-cache placement revisit (ADR-0011 note: second consumer, or never)
- `ImagingStudy` (R5, CHF trigger) and the stroke-risk data source (R7)
  — GMF coverage Wave D closed 2026-08-02 (D0-D3, see Done below)
  without owning either; H3's own attribute-weighted `distributed_
  transition` mechanism landed D3 but is only half of stroke's own
  revisit trigger (`stroke.json` stays deferred). **Dated
  cross-reference (2026-08-03, ADR-0031):** the stroke-risk DATA-SOURCE
  question is RULED — `.agents/plans/2026-08-02-gmf-parity-plan.md` §2
  (the risk-attribute register, curated calibration content rather than
  a ported calculation). This row's remaining substance is Wave E
  scheduling (stroke as the register's first consumer), not an open
  design question.
- **Census tool refinements** (ADR-0035/ADR-0036's own disclosed, not-
  fixed findings, `ehrt.sim-trajectory.census`): (b) no per-module
  census-seed override (every module shares the SAME global seed
  count) STANDS, untouched, its own trigger unfired: a future session
  needing a per-module seed-count override. (a) and (c) **CLOSED
  2026-08-07 (census substance, `notes/ADRs.md` ADR-0069 AR-VC-2/
  AR-VC-3): the substance qualifier (`:substance`/`:event-counts` on an
  `:ok-walked` row, `summarize`'s own `:ok-walked-by-substance` tally)
  and the labeled-filename fix (`artifact-filename`, `-main`'s optional
  third arg) both land — their own original text relocated verbatim
  into ADR-0069's own record, not restated here.**
  **Dated intake (2026-08-07, vendoring batch 2, `notes/ADRs.md`
  ADR-0071 AR-VB2-4, adjacent to (b), neither acted on): (i) the
  `:closure-file-count` metric counts JSON modules only, never
  lookup-table CSV data files (ADR-0070's own AR-VB1-2 lesson) — this
  batch had zero CSVs so the metric held, but a future batch could
  repeat the undercount; (ii) the three-seed sample can miss
  population-scale failures a real round-trip catches —
  `injuries.json` (batch 1) and `anemia___unknown_etiology.json`
  (batch 2) are now two independent findings the census's own narrow
  sample missed. Revisit trigger: a future session extending the
  census tool itself, not a vendoring session.**
  **Dated note (2026-08-05, standing-equipment promotion, `notes/ADRs.md`
  ADR-0044 AR-P-4): `ehrt.sim-trajectory.census` moved from
  `development/src` into `components/sim-trajectory` — relocation and
  test-exercise only, by ruling; the triggers above stood, untouched,
  none fired by the move.** A different, real finding surfaced
  INCIDENTALLY by the move (running the census's own 7 tests under
  `poly test` for the first time ever): two test fixtures had gone
  stale after GMF coverage Wave VS landed real `VitalSign`/`:vital-sign`
  support, fixed forward (ADR-0044's own Step 1) — not one of this row's
  own named refinements, disclosed separately there.
- UTI's own `ed_bundle.json` O2-saturation Observation states carry a
  `gmf_version 2` `:distribution` this loader has NEVER normalized
  (Observation is not one of ADR-0035's three ported contexts) — a
  stray, still-raw, string-keyed field `emit-and-advance`'s own
  `(= :procedure (:type state))` gate correctly ignores (ADR-0035's own
  execution note, Step 2's "real bug found and fixed mid-step"). The
  raw field itself stays unnormalized, disclosed, not built — revisit
  trigger: a future session that needs Observation's own v2 timing/
  value distributions for real (no vendored-corpus module currently
  reads the sampled value back).
- **Vital-sign channel** (ADR-0036 AR-7, GMF coverage Wave F's own
  explicit deferral): the `VitalSign` STATE type and the `:vital-sign`
  CONDITION type both require a vital-sign REGISTER with baseline
  values (State.java: Synthea's lifecycle engine sets these before any
  module runs) — engine-delegated content this project does not yet
  supply, authored calibration content pairing naturally with the
  re-scoped Wave E (risk-attribute register, above). Blocks
  `congestive_heart_failure`/`contraceptives`/`covid19` directly
  (census-confirmed). Revisit trigger: Wave E's own design session, or
  whichever session first needs a real vital-sign baseline.
  **Dated note (2026-08-07, vendoring batch 1, AR-VB1-5):** the
  substance census (ADR-0069's artifact,
  `components/sim-trajectory/docs/census/2026-08-07-synthea-7e08387-substance.edn`)
  shows this blockage is now partial, post-Wave-VS —
  `congestive-heart-failure` walks `[0 117 0]` and `contraceptives`
  walks `[0 89 0]`, both `:produces-content`; `covid19` alone walks
  `[0 0 0]`, `:zero-on-every-seed`, still fully blocked. The trigger
  above is unchanged (a real vital-sign baseline register, not yet
  built) — only the "blocks all three directly" citation updates to
  the current evidence.
- **Lookup-table column `time` — genuinely open, distinct from the
  Wave LC column-resolution mechanism** (compaction A, AR-A-5
  STALE-AUDIT disposition): Wave LC (ADR-0038 AR-1) DOES
  special-case a `time` lookup-table COLUMN (age/time date-range
  parsing) and the Wave LC census confirmed `hiv-diagnosis`
  (originally blocked on this column) moved `:load-failed` →
  `:ok-walked` — that evidence is real, recorded here rather than
  hidden. The Next section's own separate "lookup-column `time`
  gap" row (named since ADR-0039, schema-invalid family, still
  untouched per that row's own text) is a DIFFERENT concern this
  evidence does not resolve — author ruling (compaction A,
  AR-A-5): this row's `time` component stays explicitly LIVE
  regardless of the column-resolution evidence above, pending a
  future session that reconciles the two. The `race` half of the
  original combined row CLOSED this session — see Done, below.
  Revisit trigger: whichever session next touches the
  schema-invalid family's own `time` gap.

## Done (live — current arc only; full history in the attic files,
`.agents/plans/roadmap-done-2026-07.md` and `.agents/plans/roadmap-done-2026-08.md`,
scaffolding compaction B, `notes/ADRs.md` ADR-0046 — each closed arc's own
pointers rotate to a dated header in the attic at that arc's own close,
`notes/adr/0055-alignment-arc-close.md` AR-AC-5)
- 2026-08-08 — fidelity-arc-close — ADR-0084
- 2026-08-08 — straddle-fix — ADR-0086
- 2026-08-08 — colorectal-payoff — ADR-0087
- 2026-08-08 — pairing-registry — ADR-0088
