# Session: disclose that medication orders have no dispense, fill or administration (docs-only)

Context. The downstream consumer reports that the ground-truth stream carries medication orders but no dispensing (fill) events. They are right, and no consumer-facing page says so: dispense/fill appears only as an "earlier queue" line in the handoff. This session states the gap where a consumer will read it. Docs-only: no source, config, schema or test changes; nothing generated moves except by `make docsgen`. Base `64b71c08` (verify HEAD == origin/main, clean tree, `bin/preflight`). HALT on any premise mismatch below and report it; do not resolve it yourself.

Read first (line ranges at `64b71c08`):
- AGENTS.md 14-31 (de-scaffold: no ADR, no roadmap row, no rulings row; a finding is one line in the record)
- docs/future-features.md 1-30 (what the menu is; entry voice), 205-241 (the "Fidelity evidence" section is the shape for a non-fault section; "What is not on this menu")
- docs/consuming-ground-truth.md 98-118 (the opt-in law), 444-513 ("What is not warranted"), 886-893
- components/sim-engine/src/ehrt/sim_engine/event_schema.clj 1062-1085 (`:medication-order`, `:medication-end`)
- components/patient-simulator/src/ehrt/patient_simulator/compile_trajectory.clj 300-330; gmf.clj 1232-1262 (what the loader declares for MedicationOrder)
- components/sim-emit-hl7/src/ehrt/sim_emit_hl7/v2_replay.clj 566-576 (both kinds are truth-only)
- components/patient-simulator/docs/limitations.md 28-48 (the register admits only gaps declined ON PURPOSE, each with a resolving citation)
- .agents/handoffs/handoff-2026-10-01.md 220-230; .agents/prompts/2026-09-30-docs-errata-third-party-and-future-features.md (the last session that edited the menu)

Author ruling, verbatim, 2026-11-17: "go" -- given to the channel's proposal of "the docs-only dispense disclosure: the `future-features.md` entry and the limitations row". Step 3 narrows the second half; that narrowing is the channel's, not a ruling.

Channel expectations (not rulings; correct from the tree and report any difference):
- `:medication-order` carries `:active-mrn`, `:codes` and an optional `:citation` and nothing else; its schema doc reads "A medication is prescribed."; `:medication-end` closes it by citation. No kind for dispense, fill or administration exists in the event schema.
- Neither kind has a message-type registry entry; no RDE, RDS or RAS is emitted.
- Under components/sim/resources/sim/modules (66 JSON files): 134 `MedicationOrder` states in 26 files; 83 author `prescription`, 33 `prescription.refills`, 32 `prescription.duration`, 13 `administration: true`, 37 `chronic: true`. `medication-order->step` keeps `:codes` and the citation only.
- No code or gate reads docs/future-features.md by content; link gates in docs-tooling do read it.
- GMF has no dispense state. This is from the channel's memory of upstream, NOT probed: do not assert it in a doc unless a file in this tree supports it.

Steps:
1. Measure. Count the MedicationOrder population above by PARSING the JSON (not grep), over components/sim/resources/sim/modules only. Confirm by reading the schema and the emitter registry that no dispense/fill/administration kind and no pharmacy message type exists.
   Invariant: every figure that reaches a doc is one this session counted. Gate: the command and its output are in the record; any difference from the expectations above is stated.
2. docs/future-features.md: add a section between "Fidelity evidence" and "What is not on this menu", in the Fidelity section's shape (a "Not every gap here is a fault class" lead, then one `###` entry on medication dispensing and administration). The entry says, consumer-voiced: what a consumer would get (fills or administrations that name the order they belong to); that "dispense" is three different things the log would have to choose among -- an inpatient pharmacy dispense to the unit, a community fill of a prescription a hospital feed may never see, and an administration -- and that which one a consumer joins against is the fact worth telling us; and a *Today:* line stating step 1's facts (an order is a prescription fact with codes and a citation, it closes with `:medication-end`, neither renders to any HL7 message, the modules' authored prescription detail is not carried).
   Invariant: no design is chosen and no promise made. A *Design stance:* line appears ONLY if it restates law the tree already holds (the opt-in law is the candidate); otherwise the entry says no design is chosen. Nothing identifies the downstream consumer; internal sizing stays out. Gate: every relative link and anchor in the new text resolves.
3. Do NOT add a row to components/patient-simulator/docs/limitations.md. That table holds gaps declined on purpose with a citation recording the decision; the dropped prescription fields have no recorded decision. Record it instead as ONE finding line in the session record, naming `medication-order->step` and step 1's counts, marked "undecided: needs an author ruling (declared limitation, or owed)".
   Invariant: `git diff -- components/` is empty before docsgen. Gate: that diff.
4. docs/consuming-ground-truth.md, "What is not warranted": one bold-led paragraph immediately before the "Two gated limitation registers" paragraph, saying that a `:medication-order` is a prescription fact and that the log holds no fill, dispense or administration for it and no pharmacy message on the wire, linking the new future-features anchor. No other paragraph of that section changes.
   Invariant: the paragraph claims nothing step 1 did not establish. Gate: `git diff --stat` shows exactly two hand-edited docs.
5. Ceremony: archive this prompt verbatim as `.agents/prompts/2026-11-17-dispense-disclosure.md`; a SHORT record `.agents/session-records/2026-11-17-dispense-disclosure.md` (step 1's command and counts, what was written where, step 3's finding line, every expectation the tree contradicted); `make docsgen`.
   Invariant: docsgen changes only generated index/state files. Gate: docs-tooling brick tests green; `git status` names exactly the two docs, the prompt, the record and the generated files.
6. Leak check, then close. Grep the full diff for anything identifying the downstream consumer (name, employer, product, host or JDK strings, receipt values); the tree's phrase is "the downstream consumer". One commit, ASCII message: `docs: medication orders carry no dispense, fill or administration -- say so (archives prompt)`. No local `make test` owed (AGENTS.md 22). Push; `bin/post-push-verify 64b71c08 <tip>`; `gh run watch --exit-status`; append the CI run id to the record's Close in the usual follow-up commit. Report: the shas, the CI run id, step 1's counts, and every premise in this prompt the tree contradicted.
