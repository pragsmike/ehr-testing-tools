# Alignment & Cleanup Audit — ehr-testing-tools workspace

A working brief for the design channel and the Code sessions it spawns. Adapted
2026-08-05 from the generic "Consolidating Tangled Clojure Repos into a Polylith
Monorepo" brief, against tip `89e327f`. Every structural claim below was probed
live at that tip; claims marked [audit] could not be verified in the design
channel's sandbox (no Maven/Clojars network) and are the audit session's job.

**Why the original doesn't fit:** it is a migration playbook for N tangled
repos becoming one workspace. This workspace is post-migration (18 components,
1 base, 3 projects + development, top-ns `ehrt`, poly 0.3.32 pinned), its
lib-skew validation is already `:error`-enforced, and its change discipline
(move-don't-improve, oracle identity, R30) is stricter than the brief's. What
survives adaptation: the audit posture of §§6–7 and §11, `poly ws` as the
machine-readable API, and a handful of pitfalls — one of which is live in our
tree (seeded below).

---

## 1. Purpose

One audit arc, three payoffs:

1. **Reduce cognitive load** — shrink what a cold session must read and hold to
   work safely; retire narrative from load-bearing config; promote prose
   invariants into gates so vigilance becomes mechanism.
2. **Smooth the planned evolution** — the named next fronts (corpus-player
   slices per ADR-0014, `sim-emit-cda` third sibling per `emit_fhir.clj`'s
   contract note, pairing-as-data registry, the ADR-0017/0018 named-futures)
   should land without backtracking. Audit the seams they will land on *now*,
   while changing them is cheap.
3. **Publication readiness** — Clojars is ruled (registry question settled — do
   not re-raise) and "output formats freeze harder after first tag." Pre-tag is
   the last cheap moment for surface/naming/artifact-shape fixes.

## 2. Standing law that governs this arc (cited, not restated)

The audit proposes; the author rules; sessions execute under:

- Move-don't-improve, one sanctioned improvement per stage — AR-P-4
  (`.agents/rulings.md`, from ADR-0044). A tempting fix found mid-audit is a
  FINDING, recorded, never taken in the same motion.
- Red→green per step; co-landed invariants (gate lands in the same commit as
  the work it protects).
- Oracle bracket: all-eleven-batches byte identity is the standing expectation
  for every structural session (ADR-0044); any digest change requires
  `--declared-digest-change` recorded in the manifest header.
- Frozen surfaces the audit may NOT touch, only report against:
  `ehrt.sim.interface` façade (AR-M4-3, permanent); `provenance`'s
  malli-only dependency law (AR-2); `sim-emit-hl7` requires nothing beyond
  `sim-model` + its own namespaces (AGENTS.md constraint, probe-confirmed);
  frozen archives (`notes/`, sealed prompts, session records).
- Annotate-not-rewrite for plan/roadmap artifacts; fix-forward with dated
  disclosure everywhere else.
- Evidence over ruling: every audit row cites a probe, or carries
  `[unverified]`.

## 3. Seeded findings (probed 2026-08-05 at `89e327f`)

Rows the audit session inherits rather than rediscovers:

- **S1 — Resource-nesting hazard, live.** `components/sim-model/resources/sim/`
  and `components/sim/resources/sim/` both nest under `sim`. No filename
  overlap today (`demographics/*` vs `modules/*`), so no current collision —
  but every project composing both bricks merges the two trees on classpath,
  and `sim-model`'s resources don't carry its brick name. ADR-0025 disclosed
  this tolerance at split time; it was never ruled permanent. Candidate fix:
  rename to `resources/sim-model/` + update the loaders in the same commit;
  oracle must stay identity (resource *content* unchanged). Author ruling
  needed: fix, or rule the tolerance permanent with a dated note.
- **S2 — Stable-tag discipline is vestigial. RULED 2026-08-05: adopted.**
  Exactly three `stable-*` tags exist, all pre-dating the current architecture
  (`stable-bootstrap`, `stable-ehrt-1`, `stable-pre-monorepo`), so `poly
  test`'s incremental since-stable computation has been meaningless. The
  author rules: adopt live `stable-*` tagging. Mechanics: the author tags
  (tags are the author's alone — R30) after each design-channel-verified
  landing, format `stable-YYYYMMDD-<session-slug>` (matches the existing
  `^stable-.*` pattern in `workspace.edn`; no config change needed). The
  three legacy tags stay — frozen history, harmless once superseded by a
  newer stable point. First tag under the new discipline: the rider session's
  own verified landing. Recorded as a standing ruling in the rider session's
  ADR and appended to `.agents/rulings.md` citing it.
- **S3 — `workspace.edn` carries ~50 lines of `:necessary` re-derivation
  narrative.** Five dated re-derivations live as comments inside load-bearing
  config. Cognitive-load candidate: move the narrative to the ADR trail (most
  of it already cites ADRs), keep a two-line pointer + the current invariant
  ("re-derive via `poly deps`/`check` with entries cleared whenever project
  composition changes"). Needs ruling — config comments are not plan artifacts,
  so annotate-not-rewrite does not bind them, but the history is real and must
  land somewhere citable before it leaves the file.
- **S4 — Corpus-player backlog is roadmap-invisible.** The player's remaining
  work (bed board, census sink, MLLP sink, accumulator wiring, sim event-log
  adapter) lives only in ADR-0014's deferral text; the live roadmap has zero
  rows for it (probed this session). Restore rows citing ADR-0014 — this is
  the same rider already named for the next session's Step 0, listed here so
  the audit and the rider don't race.
- **S5 — Prose invariants without gates.** At minimum: the `sim-emit-hl7`
  dependency law and the `provenance` leaf law are enforced today by poly's
  general interface rules plus vigilance, not by a test that names the law.
  Each is a ~10-line deftest in `docs-tooling` (grep the ns forms, assert the
  allowed require set). Candidate promotions; enumerate others during audit.
- **S6 — Riders already owed** (carried from the 2026-08-05 onboarding, ride
  on this arc's first session): rulings-register append recording
  transcript-witnessed ≠ repo-recorded, citing ADR-0047 Step 0;
  `myocardial_infarction.json` Deferred-row relocation per AR-A-5.

## 4. Audit checklist, by area

Each row: probe → finding → recommendation with reasoning → author ruling →
(if ruled) a fenced fix session. Findings-only is a legitimate outcome for any
row.

### 4.1 Dependency graph & composition

- `clojure -M:poly deps` / `poly check :dev` — orphan warnings, unexpected
  edges. [audit — sandbox has no Maven access; run in-workspace]
- `poly libs :outdated` — never part of any ceremony to date [audit]. Decide:
  scheduled dependency review (per-arc? pre-publication?) or explicit
  non-goal, recorded either way.
- The `nist-hit` Nexus dependency: hit-nexus has no SLA and changed operators
  (NIST → Prometheus, Aug 2026). The wiring notes already prescribe the
  end-state — mirror resolved jars into a `file://` repo, sha256s in
  `artifacts.lock.edn`. Audit whether that end-state landed; if not, it is a
  supply-chain row with an existing design, not a new question.
- Root `:dev` (19 entries) and `:test` (20 paths) counted complete at tip;
  verify 1:1 brick↔path mapping mechanically, and consider a docs-tooling gate
  asserting it so the next brick addition can't silently miss the root aliases
  (the brief's §11 pitfall, promoted to mechanism).

### 4.2 Interface & surface hygiene (publication-facing)

- Per component: interface is thin delegation, exposes only what callers use,
  docstrings state *what*, implementation docstrings state *how*. The frozen
  façade is exempt from change but not from description.
- `poly ws get:components:*:interface` as the enumeration source; diff each
  surface against actual external callers (grep) to find never-called vars —
  report only; surface-thinning is per-component author rulings, and for
  `ehrt.sim.interface` it is ruled out entirely.
- Reflection warnings sweep (`*warn-on-reflection*`) — cheap now, embarrassing
  after a public tag.

### 4.3 Naming & layout coherence

- `sim-*` family is coherent (probe-confirmed); audit the non-sim components
  (`corpus`, `corpus-io`, `judge*`, `kernel`, `palgebra`, `oracle`,
  `docs-tooling`, `provenance`) against the same noun/action conventions —
  report drift, rename only by ruling (renames touch commit-immutable
  citations; AR-B-2's no-renumbering logic applies in spirit).
- Resource nesting: S1 above, plus a one-line gate candidate — every
  `components/X/resources/` contains exactly one subdir named `X` (with a
  ruled allowlist for any tolerated exception).

### 4.4 Evolution-seam readiness (the backtrack-prevention core)

For each named next front, answer: *what would this front wish we had changed
first?*

- **Corpus-player slices (ADR-0014):** does the source-sink protocol
  accommodate the `:mllp` sink kind and a census/bed-board sink without
  reshaping? ADR-0014 recorded the three-namespace shape a future `:mllp` sink
  needs — verify the seam still matches post-split reality.
- **`sim-emit-cda`:** the contract note prescribes a sibling emitter. Audit
  what `sim-emit-fhir` and `sim-emit-hl7` share by copy today (site-profile
  patterns, event-log/replay access idioms); if a third sibling would copy it
  a third time, that shared core is a pre-extraction candidate — *before* the
  sibling lands, not during.
- **Pairing-as-data registry:** vocabulary is load-bearing and the design pass
  is design-channel-first (roadmap row). The audit's only job here: confirm
  where the vocabulary would live (`judge`? `kernel`? a new leaf like
  `provenance`?) has an acyclic home, so the design pass starts from a known
  landing spot.
- **ADR-0017/0018 named-futures** (generator-source split, `corpus.display`
  placement, table-helper dedup): re-read each against the post-split tree —
  confirm still-live, sharpen or close with a dated note.

### 4.5 Docs & cognitive-load surface

- Staleness sweep of counts/paths in `AGENTS.md`, `AUTHORS-GUIDE.md`,
  component `docs/` — the compaction-C staleness class, hunted deliberately
  once instead of caught incidentally forever. The stale-path tripwire learns
  per-stage; audit its learned set for completeness against every retired ns.
- Reading-set budgets: re-derive after any doc changes this arc lands (AR-D-3
  formula, actual×1.15 round-to-5).
- Known out-of-scope `/mnt/c` residue (AUTHORS-GUIDE §1 generic routing advice,
  facts-register F7 historical timing) — already disclosed in compaction C's
  record as outside AR-C-3's scope; the audit may propose dated notes but a
  ruling extends the scope, not a session's own judgment.

### 4.6 Publication readiness (Clojars — preparation only, no publishing)

- One-artifact-per-workspace: decide what Clojars gets (the `ehrt-cli` uberjar
  is a project; a *library* artifact is a different composition and the
  brief's §11 warns against publishing two libraries sharing components from
  one workspace). This is the open half of H5 (coordinates/naming) — an author
  decision to schedule, distinct from the settled registry ruling.
- Pre-tag checklist to draft: LICENSE/NOTICE currency (vendored Synthea
  modules, SimHospital corpus, demographics tables — NOTICE files exist,
  audit coverage), version surface (`ehrt.sim.version` et al.), README
  quickstart truth (the `make quickstart`→nightly row already in Next).

## 5. What the audit does NOT do

- No refactoring during auditing. The audit session's output is a findings
  register + recommendations; fixes are separate, fenced, ruled sessions.
- No façade changes, no `provenance` dependency changes, no archive edits.
- No re-raising settled rulings (Clojars-vs-Central; `/mnt/c`; GMF scope
  exclusions).
- No dependency upgrades as a side effect of `poly libs :outdated` — that
  probe produces a report row, nothing else.

## 6. Session shape

This brief itself lives at `.agents/plans/2026-08-05-alignment-audit-brief.md`,
landed by the rider session below (with its index entry) so the audit prompt's
Read-first list can cite it.

0. **Rider session (Code, R30, small, docs-only):** land this brief + index
   entry; restore the corpus-player rows to the roadmap citing ADR-0014 (S4);
   relocate `myocardial_infarction.json` to the Done attic with notes intact
   per AR-A-5 (S6); append the rulings register (transcript-witnessed ≠
   repo-recorded citing ADR-0047 Step 0; stable-tagging adoption citing this
   session's own ADR); author its ADR + record. Oracle identity mandatory —
   no `src/` touched. The author tags the verified landing:
   `stable-YYYYMMDD-alignment-riders`, the new discipline's first tag.
1. **Audit session (Code, R30):** run §4's probes in-workspace, land
   `.agents/plans/2026-08-XX-alignment-audit-findings.md` (rows: probe,
   evidence, recommendation, proposed disposition), execute S6's riders and
   S4's roadmap restoration (small, pre-ruled, fence-listed in the prompt),
   touch nothing else. Oracle bracket expected identity.
2. **Design channel:** verify the landing by fresh probe, then present the
   findings register with per-row recommendations for author rulings.
3. **Fix sessions:** one fenced session per ruled cluster (e.g., S1+resource
   gate; S3 narrative relocation; S5 gate promotions), each with its own
   oracle bracket, red→green steps, and co-landed gates. Sequenced so
   evolution-seam fixes (§4.4) land before their consuming fronts open.

## 7. Retained from the generic brief

- `poly ws get:...` is the machine API — prefer it over parsing pretty
  reports for every enumeration probe above.
- `poly check` non-zero exit as a gate (already in ceremony via hooks).
- The naming-convention table (§6 of the original) as the reference standard
  for the §4.3 coherence audit.
- The §11 pitfall list as a checklist seed — resource nesting (S1, live),
  root-alias completeness (§4.1), one-library-per-workspace (§4.6). The rest
  of its pitfalls (profiles, interface reserved word, brick-to-brick refs)
  were probed and do not apply.

Everything else in the original — the four-remedy argument, phases 0–4, the
migration bibliography, editor setup — is context for a repo that no longer
exists. Superseded by this brief for this workspace; the original stays
citable as the source of the audit posture.
