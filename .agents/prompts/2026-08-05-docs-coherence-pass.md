2026-08-05 — docs coherence pass: gmf-interpreter consolidation, patient-state errata, arc riders
Session prompt (design channel, 2026-08-05). The sim split B arc is COMPLETE and verified (`f522db7..0986a86`, ADR-0043, M1–M4). This session is the cleanup arc's next front: the docs coherence pass the arc seeds named, plus three author-ruled riders from M4's verification. R30 ceremony throughout.
TWO DISCIPLINES, KEPT SEPARATE: consolidation (Steps 1–2) is restructure-by-move — content relocates VERBATIM, never rewritten; errata (Step 3) is the errata-sweep skill's territory — named stale claims fixed where mechanism changed under prose. Read `.agents/skills/errata-sweep/SKILL.md` before Step 3 and follow its citation-vs-instruction distinction; do NOT apply it to Steps 1–2, which its own scope statement excludes.
Context
`components/sim-trajectory/docs/gmf-interpreter.md` (3,668 lines) has two strata: sections 1–8 + appendix + ratification record (the design doc, through ~line 1404) and sections 9–16 — eight dated wave sections (B, C, D1a, D1b, D2, D3, census, VS) appended 2026-08-02 through 08-04. The doc is now archaeology-first: a reader wanting current state must fold eight waves by hand.
`components/sim/docs/patient-state-model.md` (462 lines) predates Wave H and Wave VS: its "history" content is all `Person.history` (visit-history-as-log, ADR-0008) — the history PHASE (opt-in DOB-start walks, phase-marked events, compile-drops-history, ADR-0042) and the vital-sign register (ADR-0039) are absent.
`.agents/reading-sets.edn` absorbed FOURTEEN bump commits across the arc — budgets are now accumulated increments, not derived numbers.
Three riders ruled by the author (2026-08-04, design channel, M4 verification): (1b) `explain-profiles` deletion — the M4 session kept it against AR-M4-5(a)'s verbatim retirement ruling at zero callers; the author enforces the original ruling. (2) The M4 session's annotate-over-delete treatment of the façade's fat-component disclosure is RATIFIED. (3) M4's parity ledger counted deftest-only where M1–M3 counted deftest+defspec; conservation holds under both definitions (design-channel verified: pre-M4 sim 95 → 32 + 62 + 1 under the both-forms definition); a dated note closes the comparability gap.
Read first

* `.agents/skills/errata-sweep/SKILL.md` — Step 3's governing skill, including its worked example and scope limits.
* `components/sim-trajectory/docs/gmf-interpreter.md` — full section-header pass (grep `^## \|^### `) before touching anything.
* `components/sim/docs/patient-state-model.md`.
* `notes/ADRs.md` ADR-0042 (history phase), ADR-0039 (vital register), ADR-0043 (the riders' landing zone).
* `.agents/reading-sets.edn` + the budget test in docs-tooling.
* `components/sim-trajectory/docs/census/` — the census EDN artifacts section 15 describes.
* `components/sim-engine/src/ehrt/sim_engine/order_profiles.clj:66` — rider (1b)'s target.

Author rulings (record verbatim in the session's ADR entry)

1. AR-D-1 (consolidation architecture: split by move). `gmf-interpreter.md` keeps sections 1–8 + appendix + ratification record as the living reference. Sections 9–16 move VERBATIM to `components/sim-trajectory/docs/gmf-interpreter-findings.md` (the dated findings trail — a header paragraph explains its nature and points back). In the main doc, the moved sections are replaced by one pointer section: a per-wave one-line index (wave, date, what it established, ADR) linking to the findings file. Sections 1–8 are updated IN PLACE only where a wave superseded them — each such edit cites the superseding wave/ADR inline; anything not contradicted stays untouched. No wave content is synthesized into new prose. Before the move: fresh grep for inbound links/anchors to the moved sections anywhere in the repo; repoint what the move breaks; record the inbound-link census.
2. AR-D-2 (patient-state-model). Two additive sections: the history phase (cite ADR-0042: opt-in `:history`, DOB-start, phase-marked events, compile drop, encounter-anchored inheritance + the ratified one-hop `:references` extension) and the vital-sign register (cite ADR-0039, authored-knobs ruling AR-3). Plus errata where existing prose now states something the mechanism no longer does (Step 3's skill governs these fixes; additive sections are ordinary writing, cited to the ADRs, no invention beyond them).
3. AR-D-3 (budget re-baseline). Recompute every reading set's actual current line count; set each budget to actual + 15% headroom (rounded up to the nearest 5); replace the accumulated bump comments with ONE dated note recording the re-baseline, its formula, and that it supersedes the arc's fourteen increments. reading-sets.edn is config, not a plan artifact — rewrite with a dated note is the correct discipline here, not annotation stacking. The budget test must pass immediately after with zero per-set slack anomalies (no set over its fresh budget).
4. AR-D-4 (rider 1b: explain-profiles). Delete the def at `order_profiles.clj:66` with a dated disclosure comment at the site of its former neighbors OR in the commit message + ADR line (author enforces AR-M4-5(a) as written: zero callers = retire; the M4 session's conservative deviation is overruled, its disclosure honored by citation). Re-verify zero callers by fresh grep first — if one exists now, STOP-AND-ESCALATE (the ruling's evidence condition would no longer hold).
5. AR-D-5 (rider 2: façade ratification). Dated line in ADR-0043's tail: the author RATIFIES M4's annotate-over-delete treatment of the façade docstring's fat-component disclosure — annotate-not-rewrite applied to a docstring is the house discipline; the prompt's word "retires" was the design channel's imprecision, recorded as such.
6. AR-D-6 (rider 3: ledger definitions). Dated note in ADR-0043's tail: M1–M3 parity ledgers counted `^(deftest `+ `^(defspec `; M4's counted deftest only; conservation verified under both definitions (numbers above); future ledgers state their counting definition explicitly.

Steps
Step 0 — Characterize (evidence, no edits). Verify tip = `0986a86` (STOP-AND-ESCALATE on mismatch). Section-header census of gmf-interpreter.md; inbound-link grep for its section anchors and filename across the repo; fresh zero-caller grep for `explain-profiles`; per-set actual line counts for every reading set; patient-state-model claims that contradict current mechanism (candidate list, each with the contradicting ADR/source). Record all in the session record.
Step 1 — The split (AR-D-1). Create `gmf-interpreter-findings.md` (header + sections 9–16 verbatim); replace them in the main doc with the pointer index; repoint inbound links per Step 0's census. Diff discipline: the findings file's content must be byte-identical to the moved sections (verify by extraction diff, record the command). Full suite green (budget test will fail on the changed sets — fix in Step 4, or take the licensed temporary bump if the test must pass per-commit; record which). Commit: `docs(gmf): the interpreter doc splits — living reference and dated findings trail (docs pass, AR-D-1)`
Step 2 — Sections 1–8 currency pass (AR-D-1's in-place half). Each superseded claim in sections 1–8 updated with an inline citation to its superseding wave/ADR; a per-edit list in the session record (claim, what superseded it, where). Nothing uncontradicted changes. Commit: `docs(gmf): design sections catch up to the waves — cited in-place updates only (docs pass, AR-D-1)`
Step 3 — Errata sweep (AR-D-2 + skill). Run the errata-sweep skill's procedure over patient-state-model.md's contradicted claims (Step 0's candidate list, re-verified per the skill); write the two additive sections. Any other stale claims Step 0 surfaced elsewhere ride here with the same discipline. Commit: `docs(patient-state): history phase and vital register arrive; stale claims fixed per errata-sweep (docs pass, AR-D-2)`
Step 4 — Budget re-baseline (AR-D-3). Recompute, rewrite, one dated note; budget test green with fresh budgets. Commit: `chore(reading-sets): budgets re-baselined — one derivation replaces fourteen increments (docs pass, AR-D-3)`
Step 5 — Riders (AR-D-4/5/6). The deletion (fresh grep first), the two ADR-0043 dated notes. Commit: `chore: M4 verification riders — explain-profiles retires as ruled, façade annotation ratified, ledger definitions noted (docs pass, AR-D-4/5/6)`
Step 6 — Verification + record. Oracle bracket (`bin/regression-oracle 0986a86 <step-5-tip>`): all ELEVEN batches byte-identical — Step 5's deletion is the only src edit and it must be invisible; any digest change is STOP-AND-ESCALATE. Deftest parity: unchanged everywhere (state the counting definition per AR-D-6). Façade seam byte-identical. `clojure -M:poly check` clean; both lanes green; tripwire, pairing, index-completeness, budget gates all green. Session record `.agents/session-records/2026-08-05-docs-coherence-pass.md`; prompt self-archives. Final commit: `docs: coherence-pass session record — the interpreter doc is readable again`
Fences
No wave content rewritten — moved verbatim or left alone; the pointer index and sections 1–8's cited updates are the only new gmf-interpreter prose. No code edits except AR-D-4's one deletion. No reading-set membership changes — budgets re-derive, sets' file lists stand (membership changes are a separate ruling). Frozen archives untouched (`notes/`, session records, sealed prompts — gmf-interpreter.md is a component doc and is NOT frozen; that distinction is load-bearing for this session). No re-opening of deferred items. If a Step 2 update would require judgment beyond citing a wave's explicit finding, it is a FINDING for the record, not an edit.

## Deviation record

- **Step 1's own commit message closing line** used the plan's own
  `docs: the interpreter doc splits...` wording verbatim; this
  session's Step 6 commit message differs slightly from the prompt's
  own suggested closing message ("coherence-pass session record — the
  interpreter doc is readable again") — recorded here, both wordings
  convey the same close-out, no substance change.
- **§9-§16 internal self-citation repoint** (sections 1-8's own bare
  `§9` through `§16` references, 25 occurrences) was discovered only
  while drafting Step 2, after Step 1's own commit had already landed
  and pushed. AR-D-1's own "repoint what the move breaks" clause
  covers it, but Step 0's inbound-link census (a repo-wide grep) could
  not have caught it, since these are internal to the document itself,
  not inbound links from elsewhere. Folded into Step 2's commit rather
  than amending the pushed Step 1 commit — disclosed in the session
  record's own Judgment calls section.
- **`.agents/plans/roadmap.md` updated in Step 6** (Done section, the
  Deferred row's EXECUTED annotation, the "Now" section's stale
  deferred-item citation) — not a named AR in this prompt, done for
  consistency with every prior session's own established convention in
  that file (visible throughout its Done-section history), self-caught
  by the reading-set budget gate one final time (still comfortably
  under the fresh Step 4 budget).
- **Step 4's "temporary bump" option was not needed.** No reading set
  actually cites `gmf-interpreter.md` or `patient-state-model.md` in
  its own `:paths` (verified by grep before Step 1), so Steps 1–3's
  edits never affected any set's budget — only this record's own
  `roadmap.md` growth in Step 6 touched a budgeted file, and it landed
  comfortably inside Step 4's fresh headroom. The prompt's own licensed
  fallback was read, understood, and found unnecessary.
