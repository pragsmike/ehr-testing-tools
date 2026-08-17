## ADR-0089 — The conviction arc closes: two loops convicted on evidence, and the close itself splits by design

**Status:** Accepted (author-directed, autonomous session per R30), 2026-08-08.

### Context

Prior: the pairing registry (`notes/adr/0088-pairing-registry.md`, tip
`948f5e5`) landed the mutate↔judge conviction registry as data. This
session CLOSES the conviction arc (ADR-0085–0089) per the standing
close pattern (ADR-0080/0084 the models), itself run across two Code
sessions by the author's own explicit ruling — the first time this
repo has pre-split an arc close by design rather than absorbing an
interruption after the fact. Session A (`notes/adr/` — recorded in
`.agents/session-records/2026-08-08-conviction-close-a.md`, commits
`ed90706`/`a9c3abf`) executed Step 0 (tag verification) and Step 1 (the
rulings appends plus the dependency-review cadence). This session
(session B) executes state regeneration, budget re-derivation, Done
rotation, and this closing ADR.

R30 ceremony. Read-first: `notes/adr/0084-fidelity-arc-close.md` (the
regeneration-table format, the budget mechanics, the tripwire
sequencing disclosure, the Done-rotation precedent); `.agents/
state.md` in full (the regeneration target); `.agents/session-records/
2026-08-08-conviction-close-a.md` (the cadence report, the tag
fix-forward disclosure); `.agents/plans/roadmap.md` +
`.agents/plans/roadmap-done-2026-08.md` (the rotation source and
target); `notes/adr/0085–0088` (the arc narrative's sources).

### Decision

Author rulings, recorded verbatim. `[A]` author-ruled, `[C]`
channel-inferred.

1. **AR-CB-0 `[A]`** (close A's own AR-CA-4 debt, channel-verified).
   Tag `stable-20260808-conviction-appends` at `a9c3abf`, annotated,
   standing ceremony. Did not already exist locally or on the remote;
   created fresh. **Executed Step 0.**

2. **AR-CB-1 `[A — standing regeneration rule]`.** `.agents/state.md`
   regenerated with every `[V]` claim probe-backed THIS session,
   citation `[V @a9c3abf]`. **Executed Step 1** — commit `0d7140d`;
   regeneration table below. Tripwire sequencing (the ADR-0080/0084
   precedent, re-exercised): the Step 1 commit kept its header citation
   at ADR-0084 (the newest arc-close ADR on disk at that boundary);
   this Step 2 commit that creates this file moves the citation to
   ADR-0089 in the SAME commit.

3. **AR-CB-2 `[A — standing budget rule]`.** Reading-set budgets
   re-derived from `git log 45eb2f4..HEAD --name-only` against
   `.agents/reading-sets.edn`'s own `:paths`; only sets with a touched
   member moved. **Executed Step 1** — commit `0d7140d`; TWO sets
   moved (`:onboarding`, `:judge`), the first close since the
   quality-review arc's own "all five together" regeneration where
   more than one set moves; disposition table below.

4. **AR-CB-3 `[A — standing rotation rule]`.** The closed arc's Done
   pointers rotated to a dated header in `roadmap-done-2026-08.md`,
   following the attic's own live pattern: the prior close's own
   leftover pointer (ADR-0084) rotates WITH this arc, joining its own
   `## Fidelity arc` section; this arc's own three Done pointers
   (ADR-0086/0087/0088 — ADR-0085 carried none, diagnosis-only) open a
   new `## Conviction arc — closed 2026-08-08 (ADR-0085–0089)` section.
   ADR-0089's own pointer lands in Done and stays for the next arc.
   **Executed Step 1** — commit `0d7140d`.

5. **AR-CB-4 `[A]`** (this ADR's own contents): the arc narrative; the
   pre-split adoption record; the intake sweep; the horizon note
   verbatim; this close's own mechanical debt. **Executed Step 2** —
   this entry.

6. **AR-CB-5 `[C]`.** No law appends this session (session A landed
   them); no roadmap row content changes beyond rotation and the
   ADR-0089 pointer. **Held** — no finding this session invalidated
   prior work; nothing STOP-AND-REPORTed.

### Step 0 — Preflight + tag (executed, no commit)

Working directory confirmed the ext4 clone, HEAD `a9c3abf` exactly
(conviction close A's own closing tip), branch up to date with
`origin/main`, working tree clean, no untracked files
(`git status --porcelain=v1 --untracked-files=all`, empty).
`clojure -M:poly check`: OK. Oracle self-bracket (`bin/regression-oracle
a9c3abf a9c3abf`): all 29 roots IDENTICAL, byte-for-byte, soundness
"yes outside ns form". Last five CI runs on `main` disclosed, all
`success`: `31286768535` (`a9c3abf`), `31286289031`, `31282587609`,
`31282341319`, `31282107053` — no red window. Tag
`stable-20260808-conviction-appends` did not already exist locally or
on the remote; created annotated at `a9c3abf`, pushed, verified —
peeled ref resolves exactly, both locally and via `git ls-remote`.

### Step 1 — State + budgets + rotation (AR-CB-1/2/3), commit `0d7140d`

**State.md regeneration table (claim → probe → disposition).** Held
claims are not re-listed exhaustively; this table records every claim
that changed since the prior regeneration (`e7961b9`, ADR-0084) or that
this session's own fresh probe corrected:

| # | Claim | Probe | Disposition |
|---|---|---|---|
| 1 | Component graph — new brick/edge | `git log 45eb2f4..HEAD --name-only` | **HELD — zero new bricks, zero new edges.** `judge-v2-nist/deps.edn` gained a `"resources"` entry on its own existing `:paths` vector (its first resources directory), not a new dependency. |
| 2 | `straddle-open?` / `:suppressed-straddle-spans` | Direct read + fresh grep, `compile_trajectory.clj` | **NEW section, confirmed live.** Compile-time mirror of `mark-phase`'s own `open-phase`, generalized to the legacy path; the second suppression counter alongside `:suppressed-encounter-ends`. |
| 3 | `ehrt.judge.pairing` | Direct read, `components/judge/src/ehrt/judge/pairing.clj`; `git log` for `judge/interface.clj` | **NEW section, confirmed live.** Schema/loader/coverage, five new interface re-exports, no collision. |
| 4 | Vendored module inventory | `ls components/sim/resources/sim/modules/*.json \| wc -l` | **UPDATED 24→25** — `colorectal_cancer.json` joins (ADR-0087). |
| 5 | NOTICE row count | `grep -c '^\| \`' NOTICE` (correct pattern — a naive shell-escaped variant over-matches prose) | **UPDATED 70→71** — one new row, the colorectal entry. |
| 6 | Oracle root count | `bin/regression-oracle a9c3abf a9c3abf` | **UPDATED 28→29** — `colorectal-pair` joins, additive; ONE pre-existing root moved (`sleep-apnea`, the straddle fix's licensed mover, ADR-0086) — the first oracle-root MOVE, not just addition, since the vendoring arc's own Wave G. |
| 7 | The two-module `EncounterEnd` blocker | Direct read of `roadmap.md`'s own Deferred section and ADR-0083/0085/0086/0087 | **FULLY CLOSED.** Both modules ADR-0071/0072 ever blocked (anemia, colorectal) are now vendored — two genuinely distinct defects that happened to share a submodule, not one gap wearing two names. |
| 8 | The truncation-layer absorbed-error finding | Direct read of ADR-0082 AR-EE-1a and ADR-0085 AR-CI-3 | **CONFIRMED, NARROWED.** ADR-0085's own bisection found the `:pre-horizon` gate is the real mechanism, in a straddling-encounter shape ADR-0082's own hypothyroidism trace never exercised; `encounter-closed?`'s own truncation scope plays no defective role — supersedes ADR-0082's own broader framing. |
| 9 | ADR file count | `ls notes/adr/*.md \| grep -v README \| wc -l` | **HELD at 86** at Step 1; this file makes it 87 once it lands (Step 2), the same staleness-at-count-instant pattern every prior regeneration has named. |
| 10 | `stable-*` tag count | `git tag -l 'stable-*'`, excluding the three frozen legacy tags | **UPDATED 36→42** — six new: `-fidelity-close`, `-colorectal-investigation`, `-straddle-fix`, `-colorectal-payoff`, `-pairing-registry`, `-conviction-appends`. |
| 11 | Full suite posture | `clojure -M:poly test :all skip:integration` | **UPDATED 275→283 namespaces**, 566 "0 failures, 0 errors" project-block confirmations (unchanged count — project-block granularity, not assertion-count-sensitive), 0 failures/0 errors throughout. The disclosed `mutate-stdout-stdin-loopback-test` flake did NOT fire. |
| 12 | Deferred/Next row counts | `awk` over `roadmap.md`'s own sections | **Deferred HELD at 13** — both the EncounterEnd and colorectal rows closed IN PLACE with disclosed relocation (the `roadmap-deferred-closure-lint-test`'s own compliant shape), not by removal, so the bullet count is unchanged despite two closures. **Next DOWN 11→10** — the first DECREASE any close has recorded for this count; ADR-0086 added a row, ADR-0087/0088 each removed one on execution, net −1. |
| 13 | The sibling-flake SOAK | Fresh `gh run list --json` enumeration, filtered to after the fix commit's own timestamp | **UPDATED 25→40 `test`-workflow push runs since `9cc3563`**, zero recurrence of the named test, still the same two already-disclosed unrelated failures (`ac6ef5f2`, `deabbbdb`) — no new failure in the fifteen runs landed since the fidelity close. |
| 14 | The engine `defspec` seed pin | `grep -n seed engine_test.clj` | **HELD, confirmed still live** — `{:num-tests 150 :seed -60645}`. |
| 15 | Reading-set budgets | Diff every set's `:paths` against `git log 45eb2f4..HEAD --name-only` | **TWO sets moved** — `:onboarding` (1400→1470) and `:judge` (1040→1055), the first close since the quality-review arc's own "all five together" regeneration where more than one set moves; `:corpus`/`:sim`/`:docs` HELD. |

Full regenerated content landed in `.agents/state.md` (this session's
own commit `0d7140d`); see that file directly for the complete text.

**Budget re-derivation (AR-CB-2).** `git log 45eb2f4..HEAD --name-only`
(`45eb2f4` = the fidelity arc's own closing tip, the base since which
`state.md`/`reading-sets.edn` were last touched) diffed against every
reading set's own `:paths`: `:onboarding` (`.agents/plans/roadmap.md`,
`.agents/prompts/README.md`, `.agents/session-records/README.md` all
touched — five conviction-arc sessions' worth of Now/Done/index churn
plus this close's own rotation) and `:judge`
(`components/judge/src/ehrt/judge/interface.clj`, grown by the pairing
registry's own five new re-exports) both moved; `:corpus`/`:sim`/
`:docs` carry no touched member (this arc's own src/test edits touched
`sim-trajectory`, `sim-emit-hl7`, `oracle`, `judge`, `judge-v2-nist`,
none of them a `:paths` member of those three sets). Fresh actuals
(`wc -l` sum across each set's own `:paths`, measured AFTER the
rotation landed): `:onboarding` 1274 (was 1216); `:judge` 914 (was
901). Re-applying the standing formula (actual × 1.15, rounded up to
the nearest 5): `:onboarding` 1274×1.15 = 1465.1 → **1470** (1400 →
1470); `:judge` 914×1.15 = 1051.1 → **1055** (1040 → 1055). Landed in
`.agents/reading-sets.edn` (commit `0d7140d`), a dated comment block
matching the file's own established convention.

**Done rotation (AR-CB-3).** ADR-0084's own pointer — left in place as
the live roadmap's sole current entry from the fidelity arc's own close
until now, the disclosed-leftover class every prior close has handled
for its own predecessor — relocated into the attic's EXISTING `##
Fidelity arc — closed 2026-08-08 (ADR-0081–0084)` section with a dated
append note. A new `## Conviction arc — closed 2026-08-08 (ADR-0085–
0089)` header holds ADR-0086/0087/0088's own three Done pointers,
relocated verbatim — ADR-0085 (diagnosis-only) carried no Done pointer
of its own, named in the section's own prose instead. The live
roadmap's own Done section holds an HTML-comment marker (not a
pointer) recording that this ADR's own pointer is deferred to this
Step — the same dangling-reference sentinel-avoidance ADR-0055/0064/
0068/0074/0080/0084 have each disclosed.

Full suite green throughout (283 namespaces, 566 "0 failures, 0
errors" confirmations, 0 failures/0 errors, confirmed by grepping the
entire run output). `clojure -M:poly check`: OK. Committed `0d7140d`
("docs: the conviction arc's state is regenerated — every claim
re-probed at the close (arc close B, AR-CB-1/2/3)"), pushed, verified
(one delta against the message file, the known trailing-blank-line
artifact). CI watched to conclusion: run `31287834460`, `success`,
3m20s.

### Step 2 (this entry) — ADR

### The pre-split adoption, recorded

The author ruled 2026-08-08, design channel, verbatim: **"Close. adopt,
two close sessions."** — adopting ADR-0084's own intake suggestion (a
first session scoped to Steps 0–1 only, a second to Steps 2–3) as
standing practice for arc closes going forward, first executed by this
close. Session A ran Steps 0–1 (the tag, the rulings appends, the
dependency-review cadence); this session ran the rest (state
regeneration, budgets, rotation, this ADR). **The pattern's first
observed benefit landed inside its own first execution:** session A's
own Step 0 lightweight-tag slip (a plain `git tag` omitting `-a`,
landing a tag without an annotation) was caught by that session's own
verification step before any downstream reliance and corrected in
place (delete, recreate annotated, re-verify) — a small, contained
blast radius exactly because the ceremony detail that went wrong lived
inside a single, narrowly-scoped session rather than deep inside a
longer, four-step close where a slip has more surface to compound
before anyone re-checks it. The adoption's own reasoning is now
evidenced, not just argued: splitting the close did what it was
proposed to do the very first time it ran.

### The arc narrative

The conviction arc opened by executing the fidelity arc's own top
handoff — colorectal's investigation — and closed having convicted two
separate loops on evidence rather than inference. Colorectal
investigation (ADR-0085, diagnosis-only per its own ruled fence)
localized `colorectal_cancer.json`'s own violations to `compile-
trajectory`'s legacy pre-horizon drop gate: no back-reference check
against the encounter a dropped event belongs to, so a straddling
encounter (opened pre-horizon, closed and/or containing clinical
content post-horizon) compiles its post-horizon tail with no matching
admission — confirmed across 100% of the violating population by a
three-layer probe, and, in passing, catching and disclosing its own
predecessor's `19`-figure transcription slip rather than silently
inheriting it (see Intake, below). The straddle fix (ADR-0086) closed
the gap structurally: a compile-time mirror of `mark-phase`'s own
straddle-inheritance mechanism, generalized to the legacy path; the
blast-radius protocol — this repo's second exercise of the full
predict-then-license discipline ADR-0082 first proved — found ONE real
mover, `sleep-apnea` (a latent, already-shipped malformed compiled
shape invisible to byte-identity checks until this arc's own first-ever
straddle-freedom sweep), STOP-AND-REPORTed it, and was licensed by name
at the exact predicted granularity. Colorectal payoff (ADR-0087)
vendored the module as the 29th oracle root, pinning the straddle
counter against the same real patients the investigation traced by
name — and disclosing, not hiding, a first measurement attempt that
undercounted a rare branch before a second, adapted methodology
succeeded (see Intake, below). Pairing registry (ADR-0088) — a
different kind of conviction — landed the mutate↔judge registry as
data: seven witnessed rows, a names-only NIST taxonomy snapshot, and
three pairs honestly named as un-witnessable rather than forced. This
close, split by design across two sessions, is the sixth landed session
of the arc and the second loop it closes: the colorectal thread
(diagnose → fix → vendor) and the pairing-as-data design pass (ruled at
the quality-review arc's own close, carried across two subsequent
closes, finally executed here).

### Intake, cited

* **The oracle blind-spot, evidenced (ADR-0086).** Byte-identity
  digest comparison — this repo's own standing regression harness —
  cannot see a malformed COMPILED shape if that shape has never
  changed: `sleep-apnea.json`'s own dangling `:outpatient-visit-end`
  shipped since vendoring batch 1 (ADR-0070) and passed every oracle
  bracket run against it since, because nothing had ever compared it.
  This arc's own straddle-freedom sweep — checking all 28 pre-existing
  roots for straddling spans BEFORE the fix, not just after — is the
  first time any of the 28 roots received this specific audit; 27 came
  back clean, one did not. Named for review 2 and the pairing-as-data
  adequacy conversation (ADR-0086's own intake, restated): byte-
  identity oracle checks and clinical-invariant checks (`check/
  check-all`) catch structurally different defect classes, and a
  module can pass one while silently failing the other for arcs at a
  time.
* **The three skipped pairing cells (ADR-0088), future-witnessing
  candidates for the storefront session.** `:corrupt-encoding-
  characters`/`:corrupt-segment-name` against `judge-v2-nist` (the
  parser throws before any `Report` entry exists — no finding-level
  class to witness at this tier, on this input); `:malformed-datetime-
  value` against `judge-v2-nist` (masked by the profile bundle's own
  473-finding pre-existing noise floor). None is a catalog
  contradiction. The roadmap's own storefront-demo-fixture Next row
  already names the pairing registry's own FHIR rows and the
  tier-two-to-gate promotion as its landing spot (ADR-0088's own
  cross-reference); these three skipped cells are named here again as
  candidates that same session — or a future NIST-profile-bundle
  improvement — could turn witnessed, not a gap this arc leaves
  unnamed.
* **The dependency-review cadence, restated (session A's own AR-CA-2,
  2026-08-08, against tip `f8df2cc`):** unchanged from the fidelity
  arc's own report — no new upstream release surfaced across the
  entire span from the quality-review arc's own AR-QC-2 report through
  this arc, **now FOUR sessions/arcs standing** with `deps.edn`
  untouched. `hapi-fhir-base`/`hapi-fhir-structures-r4` (8.2.0→8.10.1)
  and `org.babashka/cli` (0.12.79→0.12.86, dev-tooling-only) remain the
  only coordinates showing a newer `latest`; neither reads as
  security-relevant. A NOTE for the next arc's own intake, not an act.
* **The channel's own two disclosed errors this arc, cited where
  they're already recorded.** (1) The ADR-0082 `19` propagation: that
  ADR's own seed-42 prose figure (`{:clinical-content-only-when-
  admitted 19, :discharge-follows-admission 1}`) contradicted its own
  summary table three lines earlier and both of ADR-0072's and
  ADR-0085's own independent measurements (all three `4/0/4`) — a
  transcription slip, most likely made while authoring ADR-0082, that
  then propagated into the archived driving prompt for the colorectal-
  investigation session (`.agents/prompts/2026-08-08-colorectal-
  investigation.md`, itself frozen provenance per this repo's own
  standing law, corrected via a dated erratum on ADR-0082 rather than
  edited); recorded in full at `notes/adr/0082-encounterend-fix.md`'s
  own Erratum section (AR-SF-6) and `notes/adr/0085-colorectal-
  investigation.md`'s own Reproduction section, which disclosed the
  discrepancy first. (2) **A disclosed correction to this session's
  own driving prompt, made here rather than silently absorbed:** the
  prompt named this second error as "AR-A-5 over-literal prompt
  wording" — no citation matching that exact tag exists anywhere in
  this arc's committed record (a direct search across every
  conviction-arc ADR, session record, and archived prompt found `AR-
  A-5` used only as a citation of the STANDING relocation-with-notes
  law from scaffolding compaction A, never as an error label). The
  closest genuine, already-disclosed match is ADR-0087's own AR-CP-2
  finding: the colorectal-payoff driving prompt licensed "a well-mixed
  seed sweep" without naming the exact mechanism, and that session's
  own first, literal reading of it (mirroring the anemia test's
  interpreter-layer idiom verbatim) silently undercounted a rare,
  real branch (the straddle counter, ~2-of-900) across three tries
  before an adapted, engine-population-matched methodology succeeded
  — disclosed in full in `notes/adr/0087-colorectal-payoff.md`'s own
  Measurement section, not silently discarded. This is cited here as
  the best-evidenced instance matching the prompt's own description
  (under-specified prompt wording read too literally, producing a
  wrong first result); the mismatch between the prompt's own citation
  tag and the record is itself disclosed rather than resolved by
  inventing a matching tag.
* **This close's own two-session pre-split, its first-execution
  evidence** — see The pre-split adoption, above.

### Open Externals, restated unchanged

**NIST licensing inquiry** — narrowed, not resolved (the pairing
registry's own taxonomy snapshot names but does not settle it, ADR-0088
AR-PD-3); still author action. **IG pinning** — still open. **Clojars
publish** — ruled, deferred; F-5/F-6 remain open. **SETUP rewalk** —
still owed. **`/mnt/c` disposition** — closed (ADR-0047 AR-C-3),
unchanged. **The GitHub workflow-failure notification-email toggle** —
still genuinely unconfirmed, unchanged since the quality-review arc's
own disclosure. None of these six rows was touched by this arc's own
work; restated here, not re-decided.

### This close's own mechanical debt, recorded here

**The next arc's opening session tags `stable-20260808-conviction-close`
at THIS session's own closing tip under standing ceremony.** No tag is
created by this session for its own closing tip — the tag law's own
case (ii) licenses a session to tag its PREDECESSOR's verified stable
point, not its own mid-flight tip.

### The horizon note (verbatim, per this session's own prompt)

"The horizon, for the author's ruling: vendoring batch 4 (the veteran
family); the storefront fixture session (FHIR pairing rows — the
roadmap's named landing spot — plus the three skipped v2 cells as
witnessing candidates); Wave E's risk-attribute/vital-sign register;
review 2 on the author's cadence (inherited watch-list); publish-prep
(F-5/F-6 + F-7); the author's two backlog rows (fixture relocation;
ADR-references-in-user-docs, unruled). Externals awaiting the author
alone: the NIST licensing gist, IG pinning, Clojars F-5/F-6, the SETUP
rewalk, the GitHub failure-notification toggle."

### Verification

- `clojure -M:poly check`: OK, every step this session.
- Full suite (`clojure -M:poly test :all skip:integration`): 283
  namespaces, 566 "0 failures, 0 errors" project-block confirmations
  (grepped across the full run output, not sampled), 0 failures/0
  errors throughout. The disclosed `mutate-stdout-stdin-loopback-test`
  flake did NOT fire.
- `gitleaks git --staged -v`: clean, every commit this session;
  `gitleaks` ran automatically on every push (pre-push hook), clean
  throughout.
- Post-push message verification, every commit this session: one delta
  each against the message file, the known harmless trailing-blank-line
  artifact.
- `bin/regression-oracle a9c3abf a9c3abf` (Step 0, this session's own
  pre-digest): all twenty-nine vendored-root batches IDENTICAL,
  soundness "yes outside ns form."
- Tag verification: `stable-20260808-conviction-appends` peeled ref
  resolves to `a9c3abf` exactly, both locally and via `git ls-remote`.
- CI, watched to conclusion at every push this session (not assumed):
  `0d7140d` (Step 1) — run `31287834460`, `success`, 3m20s — and this
  session's own closing commit — see the session record for its own
  URL and conclusion.
- The staleness tripwire's own sequencing: the Step 1 commit's citation
  (ADR-0084 cited = ADR-0084 newest-on-disk) confirmed by the actual
  gate (green) at that commit boundary before this Step 2 moved it.

### Fences

Docs-only: no `src/`, no `test/`, no config, no gates touched or
edited this session (every gate cited was read, not changed). No law
appends (session A landed the arc's own rulings appends, Step 1). No
roadmap row content changes beyond the rotation and this ADR's own
Done pointer. No new design work: the horizon note above RESTATES
ruled directions, it decides nothing. Frozen archives untouched except
the sanctioned acts: this ADR's own new file, and the live-attic
appends to `.agents/plans/roadmap-done-2026-08.md` (AR-CB-3,
executed Step 1). The prompt-citation mismatch named under Intake,
above, is disclosed, not silently resolved by editing any archived
prompt (archived prompts are frozen provenance, per this repo's own
standing law).

### Consequence

The conviction arc — six landed sessions across five ADRs, closing two
genuinely different kinds of loop (a diagnose-fix-vendor thread and a
design-pass-to-data landing) — is complete, and the close itself
demonstrates the pattern the fidelity arc's own close proposed as
intake: splitting an arc close into two smaller sessions caught a real
mechanical slip inside a narrow blast radius on its very first run,
evidence for the practice rather than an argument for it. `.agents/
state.md` regenerates with fifteen corrected or newly-probed claims,
including two entirely new sections (the straddle-detection mechanism,
the pairing registry) and the first oracle-root MOVE (not merely an
addition) since the vendoring arc's own Wave G. Two reading-set budgets
move together for the first time since the quality-review arc's own
"all five together" regeneration. The arc's own Done pointers rotate
to a new attic header, the fidelity arc's own disclosed leftover
(ADR-0084) joins its own section, and the live roadmap's Done section
holds only this ADR's own pointer. Colorectal's own four-ADR,
three-session deferral — this repo's second-oldest live vendoring
blocker — closes; the oracle's own blind spot to malformed compiled
shapes is named, evidenced, and carried forward rather than left
implicit; and the pairing-as-data registry, ruled in at the
quality-review arc's own close and carried across two subsequent
closes without action, finally lands as sixty-nine lines of committed,
execution-checked data.

### Index summary (moved verbatim from notes/ADRs.md by ADR-0143, 2026-08-16)

The conviction arc closes: two loops convicted on evidence, and the close itself splits by design — the author's own "adopt, two close sessions" ruling gets its first execution (session A: tag + rulings appends; session B: state/budgets/rotation/this ADR), catching a mechanical tag slip inside a narrow blast radius as the pattern's own first observed benefit; `state.md` regenerates (two new sections, the first oracle-root MOVE since Wave G); two reading-set budgets move together for the first time since the quality-review arc's own close; the oracle's own blind spot to malformed compiled shapes (byte-identity can't see what it's never compared) is named and evidenced; a prompt-citation mismatch is disclosed rather than silently resolved
