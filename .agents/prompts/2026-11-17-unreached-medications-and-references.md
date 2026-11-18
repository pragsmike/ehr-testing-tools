# Session: why listed modules never reach their medications, and a pre-horizon end that references the wrong order (diagnosis only)

Context. Two findings from `.agents/session-records/2026-11-17-dispense-census.md`, both unexplained there. (1) No shipped root holds a `chronic: true` medication order: dense-7500 walks `metabolic_syndrome_care` and emits only its wellness encounters; clinic-decade lists `asthma` at weight 10 and no event or pre-horizon fact cites it. (2) In the committed module-mix trace two pre-horizon `:medication-end` facts in one registration both carry `:references 17`. This session explains both. Measurement and reading only: no source, config, schema, test or consumer-doc change; the output is one record. RUN AFTER the medication-dispense-kind session has landed (one working tree). Base: the tip at start (verify HEAD == origin/main, clean tree, `bin/preflight`). HALT on any premise mismatch and report it.

Read first (ranges at `dc58500f`; re-find by symbol if the tip has moved them):
- .agents/session-records/2026-11-17-dispense-census.md ("Why the chronic states are never reached", "Findings", the script)
- components/sim/resources/sim/modules/asthma.json and metabolic_syndrome_care.json: `Initial` and every transition out of the first five states
- components/patient-simulator/src/ehrt/patient_simulator/gmf_interpreter.clj: how an Attribute condition reads an attribute nobody set; where `:references` is attached to an ending event
- components/patient-simulator/src/ehrt/patient_simulator/compile_trajectory.clj 245-260, 320-330, 420-470, 560-600
- components/patient-simulator/docs/limitations.md 28-48; demos/scenarios/clinic-decade/config.edn 10-20, 140-150; demos/scenarios/dense-7500/config.edn 1136-1140

Author ruling, verbatim, 2026-11-17: "Go" -- given to "why `asthma` and `metabolic_syndrome/medications` are never reached, plus the duplicate `:references` finding. It is measurement only."

Channel expectations (not rulings; correct from the tree and report every difference):
- asthma's guards read the attributes `atopic`, `asthma_type`, `smoker`; metabolic_syndrome_care's read `diabetes`, `prediabetes`, `hypertension`, `obesity` and six more. No vendored module sets `atopic` or `diabetes`: upstream sets them in modules this tree does not carry. If so, both walks stall at their first attribute guard for every patient, by construction, not by chance.
- The `:references` duplicate is either the interpreter resolving a MedicationEnd by state name to the FIRST matching order instead of the latest open one, or a correct reference that the census misread. Not known which.

Steps:
1. Unreached medications. For each of asthma and metabolic_syndrome_care: name the exact state where walks stop, and why, by reading the module and by tracing at least 200 walks at the scenario's own seed and config. List every attribute either module reads, and for each say which vendored module writes it, if any.
   Invariant: every "nobody sets it" is backed by a search of all 66 module files, shown. Gate: a table module -> stopping state -> cause -> attribute -> writer, in the record.
2. Reach. Say what it would take for a shipped root to hold in-log chronic orders, as options with their cost and without choosing: vendoring the upstream module that sets the attribute, a config-level attribute seed if the tree has such a mechanism, or a fixture module. Also report, for dense 7,500 and clinic-decade, how module-authored orders split between in-log and pre-horizon and what in the config sets that split.
   Invariant: no option is described as available unless the mechanism exists in the tree at a cited line. Gate: the options list with citations.
3. The reference. Reproduce the module-mix registration the census names. Decide, from the interpreter's code and the trajectory, whether the second end's `:references` is wrong. If wrong: name the function and line, state the rule it applies, say whether an in-log `:medication-end`'s `:order-event-id` can be wrong the same way, and count affected facts in module-mix, dense 750 and clinic-decade.
   Invariant: the verdict cites code, not only output. Gate: verdict, mechanism and counts in the record. Do NOT fix it.
4. Record and close. `.agents/session-records/2026-11-17-unreached-medications-and-references.md`; archive this prompt verbatim under the same slug in `.agents/prompts/`; `make docsgen`. No ADR, no roadmap row. Leak check over the diff. One commit, ASCII: `docs: record why listed modules never reach their medications, and the pre-horizon end reference (archives prompt)`. Docs-only push (AGENTS.md 22); `bin/post-push-verify <base> <tip>`; `gh run watch --exit-status`; CI id appended in the usual follow-up commit. Report: the shas, the CI run id, step 1's table, step 2's options, step 3's verdict, and every premise this prompt got wrong.
