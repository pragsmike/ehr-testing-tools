# Roadmap — rolling plan and backlog

Updated by sessions in the same commit as work that changes a row. Successor to the
design channel's chat-resident ledger (retired 2026-08-01). Cite sources; one line
per item; done items move to the bottom of their section with a date and sha.

## Now (in progress)
- Nothing in progress at this close (vendoring batch 1, ADR-0070,
  2026-08-07 — the vendoring arc's second session; five everyday
  ambulatory modules landed — asthma, bronchitis, sleep-apnea,
  fibromyalgia, dementia; a sixth, `injuries.json`, assessed and
  deferred whole on a real `gmf-interpreter` gap. Successor tag debt:
  `stable-20260807-vendoring-batch-1` at this session's own closing
  tip, owed to the next session's own Step 0. Next: batch 2 — "the
  chronic clinic tail" — awaits the author's own go and its own
  prompt, per ADR-0070's own curation-plan record; wellness-encounters
  waits its own design pass, never routine vendoring).

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
- Pairing-as-data (review P3-3): mutate↔judge conviction registry — design pass in
  the design channel first; vocabulary is load-bearing
- Storefront demo fixture: minimal clean-gating FHIR fixture so the README's mutate
  demo shows a real accepted→rejected flip (2026-08-01 capture session finding)
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

## Externals (author-only)
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
- 2026-08-07 — player-arc-close — ADR-0068
- 2026-08-07 — census-substance — ADR-0069
- 2026-08-07 — vendoring-batch-1 — ADR-0070
