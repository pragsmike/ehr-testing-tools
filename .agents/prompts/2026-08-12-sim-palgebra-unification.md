# 2026-08-12 — ehr-testing-tools: sim palgebra unification, and the manual-arc rulings recorded (ADR-0113)

## Context

Archived 2026-08-12. Repo: `~/src/ehr-testing-tools` (ext4 clone).
Session opened at HEAD `3545026` (ADR-0112's own close) and closed at
this record's own close-phase commit. Original prompt follows
verbatim. No mid-session author communication occurred; no fence
conflict arose. Two exploratory test-invocation commands misfired and
were self-corrected without touching the tree or requiring author
input — recorded in this session's own record
(`.agents/session-records/2026-08-12-sim-palgebra-unification.md`),
not reproduced here as they are not part of the driving prompt.

## Original prompt (verbatim)

Session prompt — sim palgebra unification, and the manual-arc rulings recorded (ADR-0113)
You are Claude Code executing under R30 ceremony for ehr-testing-tools (github.com/pragsmike/ehr-testing-tools), working for mg, the sole author. This is a DOCS-AND-REGISTERS-ONLY session: zero `src` change, zero test-code change, zero generated-doc change. Two jobs: (1) record the batch of author rulings from the 2026-08-12 design exchanges (manual arc, review-3 charter, palgebra placement) — transcript-witnessed rulings become repo-recorded now, per standing doctrine; (2) extend `docs/dev/simulator-architecture.md` §4 into the full palgebra unification (author ruling Q1 a), every claim citing a witnessing test this prompt has pre-verified. STOP-AND-REPORT on any conflict between this prompt and the tree.
Read first

1. `docs/dev/simulator-architecture.md` — whole doc; §4 is the seam you extend.
2. `components/corpus/docs/palgebra-design.md` — the machinery the extension instantiates (esp. §I.4 two layers, §I.5 lower/erase, D5/D6).
3. `components/sim-trajectory/docs/trajectory-computation.md` — the mechanics synthesis; gains a cross-pointer.
4. `components/sim/docs/sim-theory.md` — the resource-theory reading; cited, not modified.
5. `docs/dev/notation.md` — the notation contract §4 already follows.
6. The four witness tests at their cited lines (verify each before citing — see the verify-then-cite fence):
   * `components/sim-emit-hl7/test/ehrt/sim_emit_hl7/latency_test.clj:29`
   * `components/corpus-io/test/ehrt/corpus_io/framing_test.clj:266` (and the sibling round-trip deftests at :29, :121, :142, :224)
   * `bases/cli/test/ehrt/cli/core_test.clj:2800` and `:2827`
   * `components/sim-emit-fhir/test/ehrt/sim_emit_fhir/emit_fhir_test.clj:147`
7. `.agents/rulings.md` tail; `.agents/plans/roadmap.md` (the user-guide design-pass row and Externals section — both change).

Author rulings, verbatim (the recording batch — Step 1 lands all
of these; quotes are the author's literal words from the
2026-08-12 design exchanges)
R1 (naming): "Let's use the name 'user manual' for the user docs for ehr-testing-tools. I've been informally calling it the 'user guide' but that's too easy to confuse with the more general EHR Testing Guide that's in ehr-testing-guide repo."
R2 (manual shape; channel questions, author "Q1 a. Q2 a. Q3 a."): chaptered `docs/manual/` as the narrative layer over the existing references, never duplicating them; ed-tuesday as the manual's one running scenario throughout; the naming rename sweep rides on the first manual session.
R3 (demos exercised; author verbatim): "The demos must be known to work, and exercised as documented to make sure they actually play out as written." Ruled mechanism (channel-proposed, author "Q2 a"): a demo exerciser generalized from the quickstart pattern (`make quickstart` / `quickstart-fresh`), integration-tier, running each scenario README's fenced commands in order and asserting exit codes plus each demo's own named invariants.
R4 (audience paring; author "Q1 a"): the audience register pares to five behavioral segments — practitioner (agent-assistance absorbed as a global style constraint, evaluation as its front matter), guide reader, data consumer, contributor (human or agent), deferred library-consumer stub — and the "Seven segments" header is corrected in the same edit. Executed by a later session, recorded now.
R5 (sequence; author verbatim: "Should we run a repo review before we start on the manual? It might lead to tweaks to the CLI." then "Q3 a" on the channel's proposal): review-3, scoped as a USER-SURFACE review (verb/flag consistency, error-message quality, help surface, enumerable-options family, derived-out-dir conventions) → CLI tweak sessions from its findings → manual design pass (chapter outline + naming rider as an ADR) → chapter sessions with the demo exerciser co-landed with the first chapter citing a demo → manual-review skill (scoring rubric, run periodically, author raised it verbatim: "Should we devise a manual-review skill, with scoring rubric, so we can run it periodically as we evolve the codebase and manual?") built at arc close.
R6 (diagrams; author verbatim): "Diagrams are valuable here." Channel doctrine, un-vetoed: manual diagrams derive from data (pipeline.edn, the unification doc) wherever derivable, committed as SVG with source, so they cannot drift from what they depict.
R7 (palgebra placement; author verbatim: "Did we ever write down the palgebra treatment of the simulator mechanics? That was in another conversation, and it should be in the manual or design docs." then "Q1 a. Q2 a."): the formal unification extends `docs/dev/simulator-architecture.md` §4, citing `palgebra-design.md` and `trajectory-computation.md` both ways; one doc session (this one), parallel with review-3; the manual's sim chapters get the accessible rendering (two-spaces story, the founding thesis as organizing idea, derived diagrams) with formalism linked, not taught.
Step 0 — Preflight and tag ceremony

* `git fetch`; confirm `origin/main` at `3545026` (`354502609d1ca825726fc1a8475cfa733555e563`, ADR-0112 close). Else STOP-AND-REPORT.
* Confirm CI green for `3545026` (`gh run list --limit 5 --branch main`). This is the ONE unverified dimension of the ADR-0112 tag license: the design channel verified the landing by fresh clone on 2026-08-12 (lineage ed5f51d→abed772→9bdc346→3545026; ASCII clean on all three messages; footprint exactly the amended fence, 13 files, zero src; the licensed test-edit's diff exact; zero-src diff independently re-deriving the oracle identity's basis; registers, ADR deviations, and index count all content-verified) but the sandbox GitHub API rate-limited the CI check twice — the known structural gap. If CI on `3545026` is green: tag `stable-20260811-batch-straddle-recording`, ANNOTATED, at `3545026`; push; confirm the peeled ref exact. License: case (i), channel-verified on every dimension except CI, CI session-confirmed at this preflight — disclose that split in the ADR's tag-ceremony section. If CI is NOT green: STOP-AND-REPORT.

Step 1 — Record the rulings batch
`.agents/rulings.md` — append a "From ADR-0113" section carrying R1–R7 above in the established entry format, author quotes verbatim, provenance tags: R1/R3/R5's quoted sentences and R6/R7's quoted questions are [A]; the channel-proposed mechanisms the author ruled "a" on are [A, ruling on channel proposal]; R6's derive-from-data doctrine is [C, un-vetoed].
`.agents/plans/roadmap.md`:

1. The existing "Tool-specific user-guide design pass" row: rename to "User manual design pass" (R1 — and fix any other "user guide" token in this file and in `.agents/rulings.md`'s live entries; frozen archives and session records untouched); fold in R2's shape rulings and R5's position (awaiting review-3 findings); note the naming-sweep rider (R2) and the manual-review-skill-at-arc-close (R5).
2. New Next row: review-3, user-surface scope (R5) — chartered, awaiting the channel's prompt; scope list verbatim from R5.
3. New Next row: demo exerciser (R3) — integration-tier, quickstart-pattern generalization, co-lands with the manual's first demo-citing chapter (R5's sequence).
4. New row (register maintenance): audience register paring (R4) — five segments + header fix; a small future docs session.
5. This session's own row: sim-palgebra unification → moves to Done at close.

Commit 1 (verbatim, ASCII):

```
docs: record manual-arc and palgebra rulings; charter review-3 (ADR-0113)

```

Step 2 — The unification (extend §4)
Append a new subsection to `docs/dev/simulator-architecture.md` §4, after "The naturality witness", titled "### The two layers, instantiated". Write it in the doc's existing voice (typed claims, honest wrinkles, every witness cited by test name and file:line). Content, pre-decided — prose is yours, claims and witnesses are not:

1. GT is the sim's abstract-layer object (`palgebra-design.md` §I.4: content on wires, no infrastructure). The sim purity lint (`sim_purity_lint_test.clj`, ADR-0108) is the abstract layer's no-infrastructure rule, mechanically enforced — the lint and the layer are the same discipline seen from two sides.
2. The founding thesis as algebra. "Formats are just emitters of the patient state machine" (glossary, sim's founding thesis) means: `emitH` and `emitF` are two lowerings of one abstract object, and the naturality witness (`fhir-patient-id-and-active-mrn-resolve-to-the-same-hl7-identity`, `emit_fhir_test.clj:147`) is the coherence law between them — already cited above in this doc; here it gains its layer-reading.
3. Honest wrinkle #3: no erase exists from wire back to GT. The emitter arrows are one-way; `lower ⨟ erase = id` does NOT apply at the GT→ER7 tier because no total `erase` is implemented or intended. That is precisely why the regression oracle freezes bytes: where erasure doesn't exist, byte-identity of the lowered image is the checkable surrogate for abstract-object equality. State this plainly — the machinery is used where it holds and named absent where it doesn't.
4. Where `lower ⨟ erase = id` genuinely holds, witnessed:
   * the framing codecs are lower/erase pairs at the transport tier — `batch-round-trip-property-test` (`framing_test.clj:266`) and its four sibling round-trips (`:29`, `:121`, `:142`, `:224`);
   * pacing is movement within a fiber, erasing to the same unpaced bytes — `play-command-at-huge-rate-matches-show-identity-test` (`core_test.clj:2800`) and `play-command-file-sink-writes-byte-identical-to-unpaced-content-test` (`:2827`);
   * the latency second clock's zero point is the identity — `emit-wire-with-absent-nil-or-empty-offsets-is-byte-identical-to-emit` (`latency_test.clj:29`, 100-trial defspec); GT invariance under a latency config is architectural (latency params enter at the emitter seam only, §5/ADR-0109) and was witnessed live (ADR-0110's `diff`/`sha256sum` of ground truth under `config-latency.edn`).
5. Transport realism versus mutation, as algebra. The ADR-0111 taxonomy note restated in layer terms: transport realisms (latency, batching) move within the erasure fiber — the abstract object is unchanged; mutation deliberately produces a DIFFERENT abstract object with an expected finding. Message loss and duplication remain the note's named open boundary — do not resolve it here.
6. Cross-pointers, both ways: this subsection cites `palgebra-design.md` (the machinery) and `trajectory-computation.md` (the mechanics); add one pointer LINE each to `palgebra-design.md` (near §I.4: "the simulator instantiation: docs/dev/simulator-architecture.md §4") and to `trajectory-computation.md`'s intro (same target). Additions only; not a rewrite of either doc.

Verify-then-cite fence: before landing, re-verify every test citation above at its path:line in the live tree (deftest/defspec name exact). A missing or renamed witness is STOP-AND-REPORT, never a silent substitution.
Budget check: `ehrt.docs-tooling.reading-set-budget-test` may trip on the doc's growth (the `:sim` set carries this doc, re-baselined 970→1295 at ADR-0108). If it fails on this session's growth alone, re-baseline with a one-line disclosure in the ADR (the ADR-0108 precedent); any other budget failure is STOP-AND-REPORT.
Commit 2 (verbatim, ASCII):

```
docs: sim palgebra unification -- the two layers instantiated (ADR-0113)

```

Step 3 — ADR and close

* Self-archive this prompt at close-phase START.
* `notes/adr/0113-sim-palgebra-unification.md`: context (R7 verbatim; the three previously-disjoint bodies named — mechanics docs, §4, palgebra-design — and the gap this closes), decision (the subsection's claim list with witnesses; the rulings batch landed), tag ceremony (Step 0's split-license disclosure), oracle bracket, full gate, fences, index line. `notes/ADRs.md` + `notes/adr/README.md` (110 → 111, as-of line).
* Roadmap Done line: `- <run date> — sim-palgebra-unification — ADR-0113`
* Session record.

Oracle bracket. Pre-analysis: pure identity on all 35 roots — docs and registers only; the three touched design docs are not oracle roots' `src`. Run `bin/regression-oracle 3545026 <final-commit>`; any non-identity is STOP-AND-REPORT.
Gates: `make test` green (note: `ehrt.sim-engine.engine-test` carries a known seed-dependent flake, ADR-0112's disclosure — a generative failure there gets ONE re-run; if it fails twice, STOP-AND-REPORT with both seeds); gitleaks staged + pre-push; ASCII byte-check on all three messages; push; CI confirm or disclose rate-limiting.
Commit 3 (verbatim, ASCII):

```
docs: session record and prompt archive -- sim palgebra unification (ADR-0113)

```

Fences

* Touch ONLY: `docs/dev/simulator-architecture.md`; `components/corpus/docs/palgebra-design.md` (one pointer line); `components/sim-trajectory/docs/trajectory-computation.md` (one pointer line); `.agents/rulings.md`; `.agents/plans/roadmap.md`; `.agents/prompts/*`; `.agents/session-records/*`; `notes/adr/0113-*.md`; `notes/ADRs.md`; `notes/adr/README.md`; and `.agents/reading-sets.edn` ONLY under the budget-check provision above.
* The rule (ADR-0099 form): documentation-and-register surfaces named above and nothing else; the list illustrates the rule; a file outside it is STOP-AND-REPORT.
* ZERO `src`, ZERO `test/`, ZERO generated docs (`docs/cli.md`, `docs/use-cases*`, `docs/pipeline.md`, `docs/operators.md` all byte-unchanged — no docsgen run is needed or licensed), ZERO demo/scenario files, ZERO `sim-theory.md` edits (cited only).

STOP-AND-REPORT on: any witness citation failing re-verification; oracle non-identity; the engine-test flake failing twice; any "user guide" token whose home is ambiguous between live register and frozen archive; anything this prompt failed to pre-decide.
