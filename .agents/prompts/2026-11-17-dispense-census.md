# Session: census of medication orders by class, before any dispense kind is built (measurement only)

Context. The author has ruled the cardinality source for a future opt-in `:medication-dispense` kind: orders the module marks `chronic: true` refill at a fixed period until `:medication-end` or the horizon; every other order fills once. Nothing is built here. This session measures whether that rule yields any multi-fill orders in the roots that matter, because the channel's own probe says it may not. No source, config, schema, test or consumer-doc change; the output is one session record. Base `24e19cfd` (verify HEAD == origin/main, clean tree, `bin/preflight`). HALT on any premise mismatch and report it; do not resolve it yourself.

Read first (ranges at `68d972d2`; none of these files changed through `24e19cfd`):
- AGENTS.md 14-31; .agents/session-records/2026-11-17-dispense-disclosure.md (step 1's counts and the open finding)
- components/sim-engine/src/ehrt/sim_engine/event_schema.clj 500-535 (pre-horizon facts and the nested `:event` hazard), 1062-1082
- components/patient-simulator/src/ehrt/patient_simulator/gmf.clj 1232-1262 and the loader's key normalization (how a state name becomes a citation's `:state`)
- components/patient-simulator/src/ehrt/patient_simulator/compile_trajectory.clj 300-330, 395-405
- demos/scenarios/dense-7500/config.edn 1136-1140, 2080; demos/scenarios/dense-7500/figures.edn (the 750 and 7,500 cell invocations); demos/scenarios/clinic-decade/config.edn 16, 145; demos/scenarios/ed-tuesday/config.edn 125, 260

Author ruling, verbatim, 2026-11-17: "b" -- chosen from: (A) one fill per order at order time; (B) `chronic: true` orders refill at a fixed period until `:medication-end` or the horizon, no draw, one authored-provisional constant; (C) a draw on a new stream family. Measuring before building is the channel's sequencing, not a ruling.

Channel expectations (not rulings; correct from the tree and report every difference):
- 37 `chronic: true` MedicationOrder states live in four files: medications/maintenance_inhaler (21), medications/emergency_inhaler (11), metabolic_syndrome/medications (4), osteoporosis (1). They are reached from asthma, metabolic_syndrome_care and osteoporosis.
- dense-7500 lists metabolic_syndrome_care and no asthma or osteoporosis; clinic-decade lists asthma; ed-tuesday lists none of the three.
- In the committed demos/traces/module-mix log (249 events): 11 in-log `:medication-order` events, all non-chronic; 83 pre-horizon `:medication-order` facts, all non-chronic; no in-log `:medication-end`. So most orders there sit before the horizon, where a first slice was going to skip them.
- Every in-log `:medication-order` carries a `:citation` whose `{:module :state}` names exactly one MedicationOrder state.

Steps:
1. Class map. From the vendored module JSON under components/sim/resources/sim/modules, build `{[module state] -> #{:chronic :administration :has-prescription}}`, keyed the way a citation is keyed. Use the loader's own normalization; do not re-derive the slug by hand.
   Invariant: the map has one entry per MedicationOrder state (134 expected). Gate: the count, in the record.
2. Generate four logs with `--format ground-truth` at the tree's own gated invocations: the dense-7500 750 cell, the dense-7500 7,500 cell, clinic-decade, ed-tuesday. Record each invocation, event count and sha-256.
   Invariant: each log matches the tree's recorded digest or figure for that cell where one exists. Gate: the match, or the mismatch reported and the session halted.
3. Census, per log, iterating the TOP-LEVEL vector only for in-log counts and `:registered` -> `:pre-horizon-facts` only for pre-horizon counts:
   (a) in-log `:medication-order` by class (chronic / administration / other), joined by citation; unmatched citations listed;
   (b) pre-horizon `:medication-order` facts by the same classes;
   (c) for chronic orders, in-log and pre-horizon separately: how many are closed by an in-log `:medication-end`, how many by a pre-horizon end, how many are still open at the log's last `:t`; and for in-log ones the order-to-end (or order-to-log-end) span in days: min, median, max;
   (d) distinct subjects holding at least one chronic order, in-log and pre-horizon.
   Invariant: (a)'s classes sum to the log's `:medication-order` kind count; unmatched is zero or each one is explained. Gate: the tables, in the record.
4. Projection, arithmetic only, nothing built. For period P in {30, 90} days: fills per chronic order = 1 + floor(span / P), every other non-administration order = 1, administration orders = 0. Per log, report total dispense events and the percentage they add to the event count, twice: in-log orders only, and with pre-horizon chronic orders included from the registration instant.
   Invariant: the projection uses only step 3's numbers. Gate: the four-by-four table, and one sentence per log saying whether rule B produces any order with more than one fill.
5. Record and close. `.agents/session-records/2026-11-17-dispense-census.md` with the tables, the scripts used (inline or under the record's own directory by this repo's practice), and every expectation the tree contradicted; archive this prompt verbatim as `.agents/prompts/2026-11-17-dispense-census.md`; `make docsgen`. No ADR, no roadmap row, no figures.edn edit. Leak check over the diff: nothing identifying the downstream consumer. One commit, ASCII message: `docs: record the medication-order census by class -- what a chronic-refill rule would yield (archives prompt)`. Docs-only push (AGENTS.md 22); `bin/post-push-verify <base> <tip>`; `gh run watch --exit-status`; CI id appended in the usual follow-up commit. Report: the shas, the CI run id, step 3(a)-(d) and step 4's table for each log, and every premise this prompt got wrong.
