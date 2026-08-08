## ADR-0081 — Fidelity riders: the arc opens

**Status:** Accepted (author-directed, autonomous session per R30), 2026-08-08.

### Context

Prior: `notes/adr/0080-quality-arc-close.md` closed the quality-review
arc and named its own horizon note verbatim — the EncounterEnd
interpreter design pass as "the oldest ruled-fix candidate," a
two-module blocker (`anemia___unknown_etiology.json`, `notes/ADRs.md`
ADR-0071; `colorectal_cancer.json`, ADR-0072) both deferred whole on
the same `anemia/anemia_sub.json` close-encounter-if-open idiom
compiling as an unconditional `:encounter-end` in this project's
interpreter.

This session OPENS the fidelity arc, ruled by the author (design
channel, 2026-08-08: "go" on the design brief). A design-channel-
authored brief, `.agents/plans/2026-08-08-encounterend-design.md`,
diagnoses the gap: probe-grounded against upstream
`synthetichealth/synthea` `State.java` at the pin
(`7e08387c68a7f0e21d13076609a159fd473fc902`) and this project's own
`gmf_interpreter.clj` at `42cd1e0`. Per this repo's standing evidence-
over-ruling discipline, this session re-verified both sides of the
brief's diagnosis rather than carrying it forward on trust:

- **Upstream**, fetched fresh this session from the pinned raw source
  (`EncounterEnd.process`, `org.mitre.synthea.engine.State`): has
  exactly the five arms the brief describes — own-encounter-open
  (really end it, discharge disposition attached, provider released,
  return `true`), wellness-encounter-open held by the shared
  `EncounterModule` (remove the active-wellness attribute, don't close
  the record encounter, return `true`), a stale active-wellness
  attribute with no current encounter (cleanup, return `true`),
  someone-else's-encounter-open (return `false` — the state blocks and
  retries), and nothing-open (return `true`, a no-op). Confirmed
  field-for-field against the fetched source.
- **In-tree**: `emit-and-advance`'s `:encounter-end` case (line 1697)
  compiles unconditionally; `index-of-last-open-encounter` (lines
  1207-1209) is openness-blind — it returns the last `:encounter`
  EVENT index whether or not that encounter is closed, `nil` when
  there was never one; the Wave H phase-inheritance fold (lines
  1936-1959) already pairs each `:encounter` with its matching
  `:encounter-end` and clears state on consumption ("encounters never
  nest in this project's own GMF subset") — proof that per-walk
  openness is trackable today, just not consulted at compile time.
  `anemia/anemia_sub.json`'s own "End Any Active Encounter Just In
  Case" state and its shared vendoring inside `hypothyroidism.json`'s
  closure both confirmed live.

No factual error was found in the brief; it lands unchanged, exactly
as authored.

Also disclosed here, one session late, by the first session actually
able to: a near-miss from the quality-close session (ADR-0080). That
session created `stable-20260807-quality-close` prematurely, caught
its own contradiction against the tag law (a `stable-*` tag records a
session's own design-channel-VERIFIED closing point, and ADR-0080's
own text had not yet been so verified at the moment the tag was cut),
and deleted it before it reached the record — disclosed in the design
channel 2026-08-07, repo-recorded here (AR-FR-0).

R30 ceremony. Read-first (this session): ADR-0080 in full (the horizon
note this executes); the brief itself in full; ADR-0071/0072's own
deferral sections (the incident record the brief cites);
`.agents/plans/roadmap.md`'s Next section and its Deferred two-module-
blocker row; `.gitattributes` (the fixture-relocation row's `-text`
citations); `components/docs-tooling/test/ehrt/docs_tooling/
test_source_live_path_lint_test.clj` (the blessed-roots allowlist the
fixture move will touch); a fresh grep of `ADR-[0-9]{4}` across
`docs/` (the footnote-links row's own scope); ADR-0010 (the audience
fork the footnote-links row's prerequisite inventory must respect).

### Decision

Author rulings, recorded verbatim (design channel, 2026-08-08). `[A]`
author-ruled, `[C]` channel-inferred.

1. **AR-FR-0** `[A — tag law, case (ii); debt recorded in ADR-0080]`.
   Annotated `stable-20260807-quality-close` at `42cd1e0`, message
   `quality arc closed, design-channel-verified 2026-08-08 (ADR-0080)`;
   pushed; peeled ref verified against
   `42cd1e02ed6c5b4138b95221d174a4126256f477`, both locally and on the
   remote (`git ls-remote origin refs/tags/stable-20260807-quality-close^{}`).
   **Executed Step 0.**

2. **AR-FR-1** `[A — the author's "go"; C for landing mechanics]`.
   `.agents/plans/2026-08-08-encounterend-design.md` lands EXACTLY as
   authored (verified this session, factually sound against both the
   upstream pin and the live tree — see Context; no correction
   needed). The roadmap's Deferred two-module-blocker row gains a
   dated line pointing at the brief. **Executed Step 1** — commit
   `6cb4627`.

   The brief's own three rulings, RULED by the author (design channel,
   2026-08-08, all three as recommended):

   - **R1 — the wellness arms.** Adopt the proposed openness-only
     treatment, divergence disclosed in the interpreter doc: this
     subset runs one module per walk and compiles wellness encounters
     as the module's own events (Wave G), so there is no cross-module
     wellness context to exit — a wellness encounter opened by this
     module's walk closes like any other, by openness alone (upstream
     A1/A5 by symmetry), not by upstream's own A2/A3 ownership
     distinction.
   - **R2 — suppressed-end visibility.** Count-and-surface: the walk
     context tallies `:suppressed-encounter-ends`, surfaced in census
     walk rows and round-trip metadata. Not an error — A5 is legal
     upstream semantics — but a zero-cost diagnostic, per the standing
     error-honesty lesson that an absorbed signal should at least be
     countable.
   - **R3 — the acceptance bar.** All 27 current oracle roots
     predicted-and-confirmed identical; any mover — predicted, before
     the fix lands, or actual, after — is STOP-AND-REPORT with
     evidence, never a silently-accepted movement.

   These three rulings LICENSE the fix session; none of R1-R3's own
   substance is executed by this session (see Fences).

3. **AR-FR-2** `[A — the author's backlog additions, design channel
   2026-08-08, recorded verbatim]`. Two rows added to
   `.agents/plans/roadmap.md`'s Next section. **Executed Step 1** —
   commit `6cb4627`.

   (a) **Fixture relocation.** Move test fixtures out of components —
   `components/corpus/test-fixtures/v2/simhospital` and its
   `components/corpus/test-fixtures/v2-nist` sibling, named
   explicitly — to a top-level home, so demos can use them. Flagged
   wrinkles: both trees are NOTICE/PROVENANCE-hashed and `-text`
   protected (`.gitattributes`, confirmed live this session: `v2/
   *.hl7`, `v2/simhospital/messages.out`, `v2/simhospital/LICENSE`,
   `v2-nist/covidELR/*.txt`, `v2-nist/COVID19_ELR-v2.3.1/**`), so the
   demos-front-door mechanic applies (ADR-0073: same-commit
   `.gitattributes` moves, byte-witnessing, pointer-README stubs); the
   live-path lint's blessed roots
   (`test_source_live_path_lint_test.clj`'s `"test-fixtures"`
   allowlist entry, confirmed live) update with the move.

   (b) **ADR references in user-facing documentation.** Remove bare
   `ADR-NNNN` citations from the user path, or convert them to
   clickable footnote links. Recorded fork: strip-to-dev-docs-only vs.
   footnotes (footnotes keep provenance) — **unruled, awaiting its own
   design moment.** Prerequisite named, not executed: a full inventory
   of the user path per ADR-0010's own three-way audience fork (root
   `docs/` only — NOT `docs/dev/`, which is the maintainer path). This
   session's own grep confirmed at least four live instances
   (`docs/site-profiles.md`, `docs/judge-calibration.md`,
   `docs/glossary.md`, `docs/formats.md`) but deliberately did not
   attempt the exhaustive inventory — that inventory is the row's own
   prerequisite, not this session's act.

4. **AR-FR-3** `[C — scope]` (fences). Held — see Fences, below.

### Pending rulings — not decided by this session

R1, R2, and R3 above ARE ruled — they gate the fix session; nothing
about them is open. What remains genuinely open, named so a future
reader doesn't conflate "ruled" with "resolved":

- **AR-FR-2(b)'s own fork** (strip-to-dev-docs vs. footnote links) is
  UNRULED — a design-channel item for its own future moment, not
  decided here.
- **The fix session itself has not run.** This ADR licenses it (R1-R3
  recorded verbatim, the incident record confirmed, the blast-radius
  protocol named); it does not execute any of it.

### Fences

Docs-only: no `src/`, no `test/`, no config, no gates touched or
edited this session (every gate cited above was read, not changed).
NO interpreter work: the brief DESCRIBES the fix; nothing in
`gmf_interpreter.clj` moved. NO fixture moves: AR-FR-2(a) is a Next
row, not an act. NO footnote work: AR-FR-2(b) is a Next row with an
explicitly unruled fork, not an act. Standing untracked files: none
existed at session start beyond the design brief itself, which this
session's own AR-FR-1 tracks into git.

### Verification

- `clojure -M:poly check`: OK, every step this session.
- Full suite (`clojure -M:poly test :all skip:integration`) at true
  Step 0 baseline: **one known, already-explained failure** —
  `ehrt.docs-tooling.index-completeness-test` flagged the untracked
  design brief as missing from `.agents/plans/README.md`'s own index
  (42 passes, 1 failure, 0 errors in that namespace) — disclosed
  rather than smoothed over; resolved by Step 1's own index addition,
  landed in the same commit as the brief. Full suite after Step 1's
  edits: 511 assertions, 0 failures, 0 errors — matching ADR-0080's
  own closing shape exactly (docs-only, no `src/` touched).
- `gitleaks git --staged -v`: clean, before both commits this session;
  `gitleaks` ran again automatically on both pushes (pre-push hook),
  clean throughout.
- Post-push message verification, Step 1's commit (`6cb4627`): one
  delta against the message file, the known harmless trailing-blank-
  line artifact.
- `bin/regression-oracle 42cd1e0 42cd1e0` (Step 0 pre-digest): all
  twenty-seven vendored-root batches IDENTICAL, soundness "yes outside
  ns form."
- `bin/regression-oracle 42cd1e0 <this session's own closing tip>`
  (spanning every commit this session made): all twenty-seven roots
  IDENTICAL — expected and confirmed; nothing in `src/` changed this
  session.
- Tag verification: `stable-20260807-quality-close` peeled ref
  resolves to `42cd1e02ed6c5b4138b95221d174a4126256f477` exactly, both
  locally and via `git ls-remote`.
- CI: last-five runs on `main` at Step 0 preflight all green
  (`42cd1e0` success, `9eb7da9` success, `c9c3b3f` success, the
  scheduled Integration run success, `8eeafb2` success — no red
  window to disclose). Step 1's own push (`6cb4627`) watched to
  conclusion, not assumed — see the session record for its run URL
  and conclusion.

### Successor tag debt, recorded here

**The next session that opens fresh work tags
`stable-20260808-fidelity-riders` at THIS session's own closing tip,
under standing ceremony** — the same tag-law case (ii) pattern every
prior close in this repo has used for its own predecessor.

### Index line

```
- 2026-08-08 — fidelity-riders — ADR-0081
```

(appended to `.agents/plans/roadmap.md`'s own Done section.)

`notes/adr/README.md`'s own file count corrects 78→79, verified by
`ls notes/adr/*.md | grep -v README | wc -l`, not arithmetic.

### The horizon, restated unchanged

This session opened the fidelity arc and licensed its own fix session;
it did not run one. Untouched, carried forward unchanged from ADR-0080's
own horizon note: the pairing-as-data registry session, Wave E's
risk-attribute/vital-sign register, vendoring batch 4 (the veteran
family), the census closure-count refinement, publish-prep (F-5/F-6 +
F-7), review 2 on the author's cadence call, `sim-emit-cda` on its
trigger. Two new items join the backlog this session (AR-FR-2), both
explicitly not built: the fixture relocation, and the ADR-footnote
fork (itself still unruled).

**What DOES change:** after design-channel verification of this
session's own landing (a fresh probe against the pushed tip), the FIX
SESSION follows — already licensed by R1-R3 above, not gated on any
further author ruling: real openness tracking in the walk state, the
compile-arm dispatch (A1 tracked-index / A5 no-op), the suppressed-end
counter, the blast-radius protocol (predict-then-confirm across all 27
oracle roots), anemia and colorectal's red-first round-trips turning
green as the fix's own proof, and — with the fix green — the payoff
rider: anemia and colorectal vendor as a mini-batch under the standing
vendoring mechanics, closing the vendoring arc's two oldest deferrals.

### Consequence

The fidelity arc opens with its design already on the table, re-
verified rather than merely carried forward, and its three gating
rulings already recorded — the fix session that follows is licensed to
run without a further design pass. The roadmap's Deferred section now
names the fix it is waiting on by file, not just by description; the
Next section carries two new, honestly-scoped rows (one with a named
prerequisite not yet done, one with a fork not yet ruled) rather than
letting either idea live only in a chat transcript. A near-miss from
the arc that just closed — a tag cut and caught before it reached the
record — is written down rather than left to be rediscovered as an
unexplained gap between what the design channel disclosed and what the
repo shows.
