<!-- Attic file: notes/adr/0038-wave-lc.md -->
<!-- Moved verbatim from notes/ADRs.md by scaffolding compaction B (2026-08-05, notes/ADRs.md ADR-0046). notes/ADRs.md remains the citation index -- see there. -->

## ADR-0038 — Wave LC: lookup-table columns generalize to attribute resolution — H2's own whitelist retires, 9 modules close

**Status:** Accepted (author-ruled 2026-08-03, design channel, AR-1
through AR-5 below, recorded verbatim, attributed, per ADR-0007's own
provenance-tag convention — every ruling below is `[A]`); executed
same day.

### Context

The post-G census (`2026-08-03-synthea-7e08387-wave-g.edn`, ADR-0037)
ranked lookup-table columns as the leading frontier family: 9 modules
(`acute-myeloid-leukemia`, `diabetic-retinopathy-treatment`,
`hiv-diagnosis`, `myocardial-infarction`, `stable-ischemic-heart-
disease`, `vhd-aortic`, `vhd-mitral`, `vhd-pulmonic`, `vhd-tricuspid`)
blocked on columns (`race`, `state`, `time`,
`diabetic_retinopathy_stage`, `operative_status`, `cardiac_surgery`,
`vhd_mr_risk`, `vhd_ps_risk`, `vhd_tr_risk`) `ehrt.sim-trajectory.gmf`'s
own loader rejected outside a hand-picked whitelist
(`recognized-lookup-table-columns`, `#{"gender"}`, D3a/H2, ADR-0029).
Read directly against the pin
(`7e08387c68a7f0e21d13076609a159fd473fc902`,
`src/main/java/org/mitre/synthea/engine/Transition.java`'s own
`LookupTableTransition` — `loadLookupTable`/`follow`, ~259–445 —
and `LookupTableKey`, ~445+): upstream's own mechanism has no closed
column vocabulary at all. `this.attributes` is every header column
that isn't `age`/`time`/a declared transition-state name, full stop —
H2's whitelist was this project's own invention, never a mirror of
anything upstream does, and it was blocking ordinary attribute columns
that resolve exactly like any module-set SetAttribute value already
does. No vendored root uses any of the new columns (all nine load
clean today), so the oracle claim is PURE IDENTITY, with the
persona-field draw-count hazard ADR-0036's own AR-5 first named
applying again (AR-4, below).

### Decision

**AR-1 (column resolution rule).** A lookup-table column resolves, in
order: (a) special `age` → patient age in years at current walk time,
matched by range containment against the row's "low-high" form
(unchanged from D3a); (b) special `time` → current walk time matched
by containment against the row's date range, format per
`Utilities.parseDateRange` at the pin (read directly, both accepted
forms transcribed into the loader: an ISO date-date range, inclusive of
the full calendar day at both ends, or a raw epoch-millisecond range
split at the FIRST hyphen only — real files this session found use
both, `covid19_prob.csv`/`hiv_stage.csv`/`hiv_care.csv`/
`hiv_diagnosis_early.csv` all the millis form, every pair a real
UTC start-of-day/end-of-day-minus-1ms boundary); (c) otherwise an
ATTRIBUTE column — resolved against the current module's namespaced
attributes first, then a persona-field mapping (`gender` → `:sex`,
F/M-translated, this project's own pre-existing special; `race` →
`:race`; `socioeconomic_category` → `:socioeconomic-category`, if ever
seen; `state` → `:state`, new below), else HONEST ABSENCE: a recorded
walk error (upstream throws here unconditionally,
`!person.attributes.containsKey(...)`; this project returns the error
as a value — ADR-0036 AR-4's own guard-layer precedent, reused
verbatim, `honest-absence` unchanged). The module-attribute-first order
matters: upstream has one flat attribute namespace; this project's own
module-namespacing means a module-set column (`operative_status`) and
a persona column (`race`) resolve from different stores, and this
divergence is disclosed at the resolver, not silently papered over.

**AR-2 (matching + defaults preserved).** Row matching is CASE-
SENSITIVE string equality on the attribute's rendered value —
`LookupTableKey.equals`'s own `this.attributes.equals(that.attributes)`
(`List<String>.equals`, plain `.equals()` per element, read at the pin,
not assumed) — deliberately NOT the case-insensitive match the `:race`
CONDITION type performs (`race-condition-holds?`, ADR-0036): a
lookup-table `race` COLUMN is an ordinary attribute cell to upstream,
sharing no code with the `Race` Logic class. No matching row → the
per-option `default_probability` distribution, exactly as today for
every column family. Row selection remains one distributed-transition
draw (existing law; no new draw behavior) — a possible honest-absence
throw during row lookup happens BEFORE that draw, so a walk-error never
leaves a partial rng-consumption count.

**AR-3 (persona `:state`).** Optional field, ADR-0036's own race/SES
pattern verbatim — draw 16, sampled ONLY when persona config supplies
`:state-weights` (zero draws otherwise, the identity hazard, AR-4).
Deliberately NOT the same field as the pre-existing `:address :state`
(a USPS two-letter abbreviation, `places.edn`'s own vocabulary): the
blocked modules' CSVs key on full US state NAMES
(`ace_arb_amlodipine_benazepril_product_distribution.csv`'s own first
data row, `26-35,M,Alabama,...`, confirmed by direct read, reached via
`myocardial_infarction.json`'s own closure) — a genuinely different
vocabulary, two `:state`-shaped fields, never unified. The census's own
persona-config gains a fixed, single-option `:state-weights` pool
(`"Alabama"`, the same transcribed value), disclosed in the artifact
header.

**AR-4 (oracle bracket — pure identity).** No vendored root uses any
new column (all nine load clean today) and persona `:state` draws only
on config. Every oracle batch byte-identical; any change is a
STOP-AND-ESCALATE, suspecting the persona-draw conditionality first
(ADR-0036 AR-5's own note, applying again).

**AR-5 (census re-run).** Same params, plus persona `:state` default
disclosed in the header; disambiguated filename (the census tool's own
same-day overwrite bug stays open — workaround, not fixed, ADR-0035's
own disclosed gap). Expected: the 9 column-blocked modules move
(resolve or unmask their next blocker); vendored roots unmoved.

### Execution note (filled same day, 2026-08-03)

**Step 1 (loader generalization, `26f280a`).** `recognized-lookup-
table-columns` retired. `parse-lookup-table` (`gmf.clj`): any
non-weight column other than `age`/`time` is now a generic attribute,
loaded unconditionally — the only load-time rejection left is a
structurally malformed `age`/`time` cell
(`:malformed-lookup-table-range`, replacing
`:unrecognized-lookup-table-column`), the same class of gap upstream
also rejects at load. `parse-time-range`/`iso-date-range-pattern` add
the `time` special, converted to this project's own epoch-day unit
(`Math/floorDiv` by one day's millisecond count recovers the same
`[low-day high-day]` pair the ISO form produces directly — proven by a
dedicated test cross-checking both forms against the same calendar
day). `census.clj`'s own gap-category tracking renamed to match
(`:unrecognized-lookup-table-columns` → `:malformed-lookup-table-
ranges`). 8 new tests (`gmf-test`): any-attribute-column-name loads,
malformed age/time reject, both time forms agree.

**Step 2 (walk-time resolution, `6af4dc0`).** `lookup-column-value`
(new, `gmf-interpreter.clj`): AR-1's resolution order, module-
namespaced attributes first (the SAME root-namespaced key
`attribute-condition-holds?`/`resolve-distribution-value` already
read), then the persona-field mapping, else `honest-absence`.
`lookup-table-row-matches?` generalizes from a hardcoded `gender`
check plus `age-range` to `:time-range` containment (mirroring
`:age-range`) and an `every?` fold over `lookup-column-value` for
every remaining attribute column. `resolve-lookup-table-transition`
gains `module-id` (the namespaced lookup needs it, thread from
`resolve-transition` the same way `resolve-distribution-value` already
receives it). 8 new tests (`gmf-interpreter-test`): module-attribute
column, persona `:race` column, module-attribute-wins-over-persona
precedence, honest-absence walk-error (`:condition-type
:lookup-table-column`), `:time-range` containment (in/out of range),
one-draw consumption.

**Step 3 (persona `:state`, `50f7efd`).** `persona.clj`: `:state-
weights` config key, draw 16, the same `(seq pool)` presence-and-
empty-pool guard 14/15 already establish. 6 new tests
(`persona-test`): absent with no config, sampled when configured,
distinct from `:address :state` (assertion on both value and length),
conditional draw count (13/14/16 across no-config/state-only/all-three
configs), schema-valid property test.

**Step 4 (oracle bracket).** `bin/regression-oracle 4d868df 50f7efd`
(the tip before Step 1 → the Step 3 tip) — all 9 vendored root
batches IDENTICAL: `appendicitis`, `death-fixture`, `ear-infections`,
`ear-infections-engine`, `sepsis`, `sinusitis`, `sore-throat`,
`total-joint-replacement-engine`, `urinary-tract-infections-engine`.
AR-4's pure-identity claim holds, byte-verified.

**Step 5 (census re-run, `a12c911`).** `:ok-walked` 64→73,
`:load-failed` 17→8, `:walk-failed` 3→3 (unchanged),
`:out-of-scope-by-ruling` 1→1 (unchanged), total 85→85. Movement traced
module-by-module (verdict-set diff against the post-G artifact): all 9
predicted modules (`acute-myeloid-leukemia`,
`diabetic-retinopathy-treatment`, `hiv-diagnosis`,
`myocardial-infarction`, `stable-ischemic-heart-disease`, `vhd-aortic`,
`vhd-mitral`, `vhd-pulmonic`, `vhd-tricuspid`) move `:load-failed` →
`:ok-walked`, cleanly — zero surfaced a next blocker, zero regressed.
The 8 modules remaining `:load-failed` (`allergies`,
`congestive-heart-failure`, `covid19`, `hiv-care`,
`home-health-treatment`, `home-hospice-snf`, `hospice-treatment`,
`injuries`) are blocked on wholly unrelated gaps (`VitalSign`/
`AllergyOnset`/`Vaccine` unrecognized state types, `injuries`' own
pre-existing `:schema-invalid` NamedDistribution gap, ADR-0037's own
account) — this wave's own frontier closed exactly and only the
lookup-column family, no incidental movement elsewhere.

`clojure -M:dev:test` per namespace: `gmf-test` 53/53, `gmf-interpreter-
test` 153/153, `persona-test` 19/19, `census-test` 7/7 — all 0
failures/0 errors. `gitleaks git --staged -v`: clean, every commit.

### Fence

No schema-invalid family work (the `injuries`/hospice complex-
transition boundary, `Transition.java`'s own `NamedDistribution` gap on
nested `complex_transition` distributions); no vital-sign work
(ADR-0036 AR-7's own named deferral, unmoved by this wave); no Wave H.
Next-frontier read (the schema-invalid family, the vital-sign channel,
the tail) awaits the design channel's own post-LC pass over this
artifact.

---

