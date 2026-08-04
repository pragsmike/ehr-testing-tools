# 2026-08-03 — GMF coverage Wave LC session prompt

Repo: `ehr-testing-tools`. Ran as a Claude Code session against the
native-Windows working directory
(`C:\Users\prags\Documents\ehr-testing-tools`), with all git/build
operations routed through `wsl -e bash -lc "cd ~/src/ehr-testing-tools && ..."`
against the WSL ext4 clone, per this repo's own WSL-only-git
convention; file edits routed through the UNC path
(`\\wsl.localhost\Ubuntu\home\mg\src\ehr-testing-tools\...`) so they
land on the same clone the git/build commands target. Preflight clean:
ext4 clone at `origin/main`'s own HEAD (`d9545c9`), no uncommitted
changes; ADR-0037 confirmed the latest ADR, next ADR 0038. A
pre-existing Synthea checkout was found at `~/synthea-checkout`,
confirmed via `git rev-parse HEAD` to already equal the pin
(`7e08387c68a7f0e21d13076609a159fd473fc902`) exactly — no fresh clone
needed.

## Prompt, verbatim

> 2026-08-03 — Build session: Wave LC — lookup-table column generalization
> Context
> The post-G census (`2026-08-03-synthea-7e08387-wave-g.edn`, ADR-0037) ranks lookup-table columns as the leading frontier family: 9 modules including `myocardial-infarction`, blocked on columns (`race`, `state`, `time`, `diabetic_retinopathy_stage`, `operative_status`, `cardiac_surgery`, `vhd_mr_risk`, `vhd_ps_risk`, `vhd_tr_risk`) the sim's loader rejects as unrecognized. The design channel pinned upstream semantics (`7e08387c68a7f0e21d13076609a159fd473fc902`, `src/main/java/org/mitre/synthea/engine/Transition.java`, `LookupTableTransition` ~259–445): columns are GENERIC person attributes with exactly two specials, so the fix is removing a whitelist upstream never had, not adding nine features. The author ratified this as the next wave (2026-08-03). No vendored root uses any of the new columns (they all load today), so the oracle claim is PURE IDENTITY, with the persona-field hazard from Wave F applying again (AR-4).
> Read first
> 1. `AGENTS.md`; `.agents/skills/build-session/SKILL.md`
> 2. The sim's lookup-table loader and column validation (wherever `unrecognized-lookup-table-columns` rejections originate — `gmf.clj` and/or the closure table loader) and the walk-time lookup-table transition implementation in `gmf_interpreter.clj`
> 3. `components/sim-model/src/ehrt/sim_model/persona.clj` — the Wave F race/SES optional-field + conditional-sampling pattern (`:state` follows it exactly)
> 4. Synthea at the pin: `Transition.java` `LookupTableTransition` (~259–445) AND `LookupTableKey` (~445+, for equality/case semantics — READ IT, the design channel did not) AND `Utilities.parseDateRange` (for the `time` column's range format — READ IT, same)
> 5. `notes/ADRs.md` — ADR-0036 (honest-absence + persona-field precedents), ADR-0037; next ADR expected 0038
> 6. The post-G census artifact header (re-run params)
> Author rulings (design channel, 2026-08-03; record in ADR-0038)
> * AR-1 (column resolution rule). A lookup-table column resolves, in order: (a) special `age` → patient age in years at current walk time, matched by range containment against the row's "low-high" form; (b) special `time` → current walk time matched by containment against the row's date range (format per `Utilities.parseDateRange` at the pin — transcribe its accepted forms into the table loader's doc and tests); (c) otherwise an ATTRIBUTE column — resolved against the current module's namespaced attributes first, then a persona-field mapping (`race` → `:race`, `socioeconomic_category` → `:socioeconomic-category` if ever seen, `state` → `:state`), else HONEST ABSENCE: a recorded walk error (upstream throws here; we return the error as a value — ADR-0036's guard-layer precedent verbatim). The module-attribute-first order matters: upstream has one flat attribute namespace; the sim's namespacing means a module-set column (`operative_status`) and a persona column (`race`) resolve from different stores, and the ADR discloses this divergence.
> * AR-2 (matching + defaults preserved). Row matching is string equality on the attribute's rendered value with whatever case semantics `LookupTableKey`'s equality actually implements at the pin (read, transcribe, test — do not assume case-insensitive). No matching row → the per-option `default_probability` distribution, exactly as today for recognized columns; a missing DEFAULT is whatever upstream does (read and match). Row selection remains one distributed-transition draw (existing law; no new draw behavior).
> * AR-3 (persona `:state`). Optional field, Wave F's pattern verbatim: sampled ONLY when persona config supplies weights (zero draws otherwise — the identity hazard), census persona-config gains a fixed value recorded in the artifact header. Value vocabulary is whatever the blocked modules' CSVs actually key on (read `myocardial_infarction`'s table at the pin; likely US state names — transcribe the census default from a real row, cited).
> * AR-4 (oracle bracket — pure identity). No vendored root uses any new column (they load clean today) and persona `:state` draws only on config. Every oracle batch byte-identical; any change STOP-AND-ESCALATE, suspecting the persona-draw conditionality first (Wave F's AR-5 note).
> * AR-5 (census re-run). Same params + persona `:state` default (disclosed in header), disambiguated filename (overwrite bug still open — workaround, don't fix). Expected: the 9 column-blocked modules move (resolve or unmask next blockers — classify); vendored roots unmoved (escalate). MI resolving would close the Wave F payoff arc two waves late — note it in the classification if it does.
> Steps
> Step 0 — Preflight. Standard; ADR-0037 at origin; next ADR 0038; Synthea checkout at pin (source reads + census).
> Step 1 — Loader generalization. Column validation becomes AR-1's resolution taxonomy (specials + attribute columns; unknown column is no longer a load rejection — reserve load-time rejection for structurally invalid tables only, e.g. malformed age ranges, which upstream also rejects at load). Tests: each column family loads; malformed ranges still reject; the `time` range formats from `parseDateRange` round-trip. Commit: `feat(sim-trajectory): lookup-table columns generalize to attribute resolution -- whitelist retired (ADR-0038 AR-1)`
> Step 2 — Walk-time resolution + matching. AR-1 resolution order, AR-2 matching/case/defaults (transcribed from `LookupTableKey`), honest-absence walk error. Tests: module-attribute column, persona column, age/time containment, no-match→defaults, absent-attribute error, one-draw consumption. Commit: `feat(sim-trajectory): lookup transitions resolve module attributes + persona fields at walk time (ADR-0038 AR-1/AR-2)`
> Step 3 — Persona `:state`. Field + conditional sampling + draw -count test (Wave F pattern) + census persona-config default. Commit: `feat(sim-model): optional persona :state, config-sampled only (ADR-0038 AR-3)`
> Step 4 — Oracle bracket. Pure identity per AR-4; record table; escalate on any change.
> Step 5 — Census re-run. Per AR-5; commit artifact + movement classification. Commit: `docs(sim-trajectory): census after Wave LC -- lookup-column family closed (ADR-0038)`
> Step 6 — Records. ADR-0038 (rulings verbatim, attributed; execution note: oracle table + classification). Roadmap: LC → Done; next-frontier note (schema-invalid family, vital-sign channel, tail) awaiting the design channel's post-LC read. Session record + prompt self-archive + budget check. Commit: `docs: wave LC records -- column whitelist retired (archives prompt)`
> Fences
> * No schema-invalid family work (the injuries/hospice complex-transition boundary), no vital-sign work, no Wave H — all awaiting their own reads/rulings.
> * Red→green per step required.
> * AR-2's case semantics and AR-1's `time` formats come from READING the pinned source, not assumption — two places the design channel explicitly did not pre-verify are named as session reads.
> * Deviations: dated deviation-record appendix on the archived prompt.

## Deviation record

- **AR-1(c)'s "module's namespaced attributes"** resolved to the SAME
  `root-id`-namespaced key `attribute-condition-holds?`/
  `resolve-distribution-value` already use (root-scoped, not literally
  the calling module's own id) — the prompt said "current module's
  namespaced attributes," which this session read as "the mechanism
  every other attribute read in this file already uses," not a new,
  narrower scoping rule. Not escalated; consistent with every existing
  attribute-read call site.
- **The census-category rename** (`:unrecognized-lookup-table-column` →
  `:malformed-lookup-table-range`, `census.clj`/`census_test.clj`) was
  folded into Step 1's own commit rather than given its own checkpoint
  — a direct, necessary consequence of the loader's own rejection-
  reason rename (without it, `census-test` goes red). Not named
  separately in the prompt's own Steps list; treated as part of Step
  1's own scope.
- **AR-3's "a fixed value"** was read literally (singular) as a
  SINGLE-option `:state-weights` pool (`{:state "Alabama" :weight
  1.0}`), not a multi-option pool covering the CSVs' own ~50-state
  vocabulary the way Wave F's race/SES pools cover their OWN closed
  vocabularies in full — `:state` has no closed vocabulary to cover.
  Disclosed in ADR-0038 AR-3's own execution note, not escalated.
- **`docs/gmf-interpreter.md`'s three historical passages** describing
  the now-retired whitelist were dated-annotated in place rather than
  rewritten, matching this repo's own established treatment of prose
  describing a past decision that has since changed — not named in
  the prompt, added as part of Step 6's own "Records" scope.
