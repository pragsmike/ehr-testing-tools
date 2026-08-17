## ADR-0113 — Sim palgebra unification, and the manual-arc rulings recorded

**Status:** Accepted (author-directed, autonomous session per R30), 2026-08-12.

### Context

Three previously-disjoint bodies of this workspace's own writing each
held a piece of the simulator's own formal treatment, never joined:
`components/sim-trajectory/docs/trajectory-computation.md` (the
mechanics synthesis, execution-order prose); `docs/dev/
simulator-architecture.md` §4 (the palgebra section, a real
equation-and-diagram reading of the pipeline, `⨟`/`×` operators, two
honest wrinkles, the naturality witness cited by test name — landed
ADR-0108); and `components/corpus/docs/palgebra-design.md` (the
palgebra machinery itself — the two-layer abstract/lowered model, D5,
and the `lower ⨟ erase = id` soundness anchor, D6 — developed for
`corpus`'s own judge/gate factorization, never before pointed at the
sim). The author, verbatim, 2026-08-12 (`.agents/rulings.md`, "From
ADR-0113," R7): *"Did we ever write down the palgebra treatment of the
simulator mechanics? That was in another conversation, and it should
be in the manual or design docs."* Placement ruled ("Q1 a. Q2 a."):
the formal unification extends `docs/dev/simulator-architecture.md`
§4, citing `palgebra-design.md` and `trajectory-computation.md` both
ways, landed as one doc session (this session), parallel with review-3
(R5); the manual's own sim chapters get the accessible rendering later
(the two-spaces story, the founding thesis as organizing idea, derived
diagrams), formalism linked, not taught.

This session also lands the recording half of a broader 2026-08-12
design-exchange batch — R1 through R6 — that is not itself palgebra
work: the "user manual" naming ruling, the manual's own shape (a
chaptered `docs/manual/`, ed-tuesday as the one running scenario), the
demo-exerciser mechanism, the audience-register paring, the review-3
charter and the arc's own sequence, and the diagrams-derive-from-data
doctrine. Full quotes and provenance tags are in `.agents/rulings.md`,
"From ADR-0113." A DOCS-AND-REGISTERS-ONLY session: zero `src` change,
zero test-code change, zero generated-doc change.

### Tag ceremony

`git fetch` confirmed `origin/main` at `3545026`
(`354502609d1ca825726fc1a8475cfa733555e563`, ADR-0112 close) at session
start; the last five CI runs on `main` (`gh run list --limit 5 --branch
main`) were all `completed`/`success`, through `31577041782`
(2026-08-12T08:09:01Z) — no red among the five, and CI on `3545026`
itself came back green at this session's own preflight. **Split
license, disclosed:** the design channel's own 2026-08-12 verification
of the `3545026` landing (fresh clone; lineage `ed5f51d` -> `abed772`
-> `9bdc346` -> `3545026`; ASCII-clean on all three commit messages;
footprint exactly the amended fence, 13 files, zero `src`; the licensed
test-edit's diff exact; a zero-`src` diff independently re-deriving the
oracle identity's own basis; registers, ADR deviations, and the
`notes/adr/README.md` index count all content-verified) covered every
dimension the ADR-0113 driving prompt named EXCEPT CI — the sandbox's
own GitHub API rate-limited the CI check twice during that
verification, the known structural gap this workspace's tagging
doctrine already anticipates. CI is the ONE dimension this session's
own preflight supplies, confirmed green as recorded above. Per license
case (i) (channel-verified on every other dimension, CI
session-confirmed here): `stable-20260811-batch-straddle-recording`
tagged ANNOTATED at `3545026`; pushed; peeled ref confirmed
`354502609d1ca825726fc1a8475cfa733555e563` — exact match.

### Decision

**[A] The rulings batch (R1-R7), recorded.** `.agents/rulings.md`
gains "From ADR-0113," seven entries, author quotes verbatim,
provenance tagged per the driving prompt's own scheme: R1's naming
quote and R3/R5/R6/R7's quoted sentences/questions are `[A]`; the
channel-proposed mechanisms the author ruled "a" on (R2, R3's
mechanism, R4, R5's sequence, R7's placement) are `[A, ruling on
channel proposal]`; R6's derive-from-data diagram doctrine is `[C,
un-vetoed]`. R4's audience-register paring and its `docs/dev/
AUDIENCES.md` "Seven segments" header fix are recorded only, not
executed — a later session's own errand, out of this session's fence.

**[A] The roadmap.** `.agents/plans/roadmap.md`'s existing
"Tool-specific user-guide design pass" row renames to "User manual
design pass" (R1), folding in R2's shape rulings (chaptered
`docs/manual/`, ed-tuesday as the running scenario, the naming-sweep
rider riding on the first manual session) and R5's own position in the
now-ratified sequence (review-3 -> CLI tweaks -> this design pass ->
chapter sessions with the demo exerciser -> a manual-review skill at
arc close). Two new Next rows land: "Review-3, user-surface scope"
(R5, the scope list verbatim: verb/flag consistency, error-message
quality, help surface, enumerable-options family, derived-out-dir
conventions) and "Demo exerciser" (R3, integration-tier, generalized
from the quickstart pattern). A new register-maintenance row records
R4 (audience paring, execution deferred). The "Now" row for this
session's own work (sim-palgebra unification) moves to Done at close,
below. Every non-quoted "user guide" token in `.agents/rulings.md`'s
own live entries and in `.agents/plans/roadmap.md` is corrected to
"user manual" in the same commit (R1) — every VERBATIM quote of the
author's own past "user guide" phrasing stays unchanged, as spoken;
this is the narrow, in-file correction the driving prompt names, not
R2's own wider repo-sweep, which still rides on the first manual
session.

**[A] The unification (§4 extension).** `docs/dev/
simulator-architecture.md` §4 gains a new subsection, "The two layers,
instantiated," after "The naturality witness," six claims, each cited
to a witnessing test re-verified at its own path:line before landing:

1. `GT` is the sim's abstract-layer object (`palgebra-design.md` §I.4);
   the sim purity lint
   (`components/docs-tooling/test/ehrt/docs_tooling/
   sim_purity_lint_test.clj`) is that layer's no-infrastructure rule,
   mechanically enforced — the lint and the layer are one discipline
   from two sides.
2. The founding thesis (`docs/glossary.md`, "Emitter": *"Formats are
   just emitters of the patient state machine"*) as algebra: `emitH`/
   `emitF` are two lowerings of one abstract object; the naturality
   witness
   (`fhir-patient-id-and-active-mrn-resolve-to-the-same-hl7-identity`,
   `components/sim-emit-fhir/test/ehrt/sim_emit_fhir/
   emit_fhir_test.clj:147`, 150-trial `defspec`) is the coherence law
   between them.
3. Honest wrinkle: no `erase` exists from wire back to `GT` — the
   emitter arrows are one-way, `lower ⨟ erase = id` does not apply at
   the `GT`->ER7 tier, and this is exactly why the regression oracle
   freezes bytes: where erasure doesn't exist, byte-identity of the
   lowered image is the checkable surrogate for abstract-object
   equality.
4. Where `lower ⨟ erase = id` genuinely holds, witnessed: the framing
   codecs at the transport tier
   (`batch-round-trip-property-test`,
   `components/corpus-io/test/ehrt/corpus_io/framing_test.clj:266`,
   plus siblings `file-per-item-round-trip-property-test:29`,
   `er7-multi-round-trip-property-test:121`,
   `ndjson-round-trip-property-test:142`,
   `mllp-round-trip-property-test:224`); pacing as movement within a
   fiber
   (`play-command-at-huge-rate-matches-show-identity-test`,
   `bases/cli/test/ehrt/cli/core_test.clj:2800`, and
   `play-command-file-sink-writes-byte-identical-to-unpaced-content-test`,
   `:2827`); the latency second clock's zero point as identity
   (`emit-wire-with-absent-nil-or-empty-offsets-is-byte-identical-to-emit`,
   `components/sim-emit-hl7/test/ehrt/sim_emit_hl7/
   latency_test.clj:29`, 100-trial `defspec`), with `GT`'s own
   architectural invariance under any latency config named and cited
   to ADR-0110's live `diff`/`sha256sum` witness.
5. Transport realism versus mutation, restated in layer terms from the
   ADR-0111 taxonomy note: transport realisms move within the erasure
   fiber (`GT` unchanged); mutation produces a different abstract
   object with an expected finding. Message loss/duplication stay the
   note's own named open boundary, unresolved here.
6. Cross-pointers landed both ways: one line each in
   `palgebra-design.md` (near §I.4) and `trajectory-computation.md`
   (its own intro), pointing at this subsection — additions only, not
   a rewrite of either doc.

All four cited witness tests were re-verified at their exact
path:line, deftest/defspec name exact, before landing (the driving
prompt's own verify-then-cite fence); none had moved or been renamed.

### Deviations, dated 2026-08-12

None. The reading-set budget check (`ehrt.docs-tooling.
reading-set-budget-test`, the `:sim` set) was checked against this
session's own doc growth before landing: `AGENTS.md` (294) +
`components/sim/src/ehrt/sim/interface.clj` (47) +
`docs/dev/engine-onboarding.md` (85) + `docs/dev/components.md` (240)
+ `docs/dev/simulator-architecture.md` (392, post-growth) +
`.agents/skills/build-session/SKILL.md` (187) = 1245 lines, under the
1295-line budget re-baselined at ADR-0108 — no re-baseline needed this
session.

### Oracle bracket

**Pre-analysis:** pure identity on all 35 roots was the prediction —
this session's own footprint is `.agents/rulings.md`,
`.agents/plans/roadmap.md`, three design docs
(`docs/dev/simulator-architecture.md`,
`components/corpus/docs/palgebra-design.md`,
`components/sim-trajectory/docs/trajectory-computation.md`), and
`notes/*`/`.agents/*` registers only — none of these are any oracle
root's own `src`.

**Bracket result.** `bin/regression-oracle 3545026 662f038` (`662f038`:
this session's own commit 2, the unification landing, run before the
close-phase commit, per the driving prompt's own step ordering):
`IDENTICAL: every root's digest matches between 3545026 and 662f038` —
all 35 roots, matching the pre-analysis; no STOP-AND-REPORT needed.

### Full gate

`make test` (`clojure -M:poly check` + `clojure -M:poly test :all
skip:integration`): green — `poly check` OK; 308 test namespaces run
(`conformance` + `ehrt-cli` projects, 17 bricks), 0 failures, 0 errors
throughout; `ehrt.sim-engine.engine-test` (ADR-0112's own disclosed
seed-dependent flake) ran clean this time, no re-run needed;
`ehrt.docs-tooling.reading-set-budget-test` green (5/5, the `:sim`
set's 1245-line actual under its 1295-line budget, no re-baseline);
`ehrt.docs-tooling.sim-purity-lint-test` green; the four witness tests
this ADR cites by name all reran and passed live, in place, as part of
this same run (`emit-wire-with-absent-nil-or-empty-offsets-is-byte-
identical-to-emit`, 100 trials; `batch-round-trip-property-test` and
its four siblings; `play-command-at-huge-rate-matches-show-identity-
test` and its file-sink sibling;
`fhir-patient-id-and-active-mrn-resolve-to-the-same-hl7-identity`, 150
trials). `bin/verify-nist-lock`: OK, 6 hit-nexus-sourced coordinates
matched. `gitleaks git --staged -v` (pre-commit, each checkpoint) and
`gitleaks detect` (pre-push): no leaks found. ASCII byte-check on all
three commit messages: clean.

**The last five `main` CI runs** (`gh run list --limit 5 --branch
main`, checked at session start): all `completed`/`success` —
`31577041782` (Integration, scheduled, 7m14s), `31560501727` (ADR-0112
session-record close, 3m12s), `31559768730` (ADR-0112 rulings commit,
4m28s), `31558837841` (ADR-0112 use-case commit, 4m31s), `31555690370`
(ADR-0111 session-record close, 4m30s) — no red among the five.

### Fences

Touched: `docs/dev/simulator-architecture.md`;
`components/corpus/docs/palgebra-design.md` (one pointer line);
`components/sim-trajectory/docs/trajectory-computation.md` (one
pointer line); `.agents/rulings.md`; `.agents/plans/roadmap.md`;
`.agents/prompts/2026-08-12-sim-palgebra-unification.md` (self-archive)
plus its README index line; `.agents/session-records/
2026-08-12-sim-palgebra-unification.md` plus its README index line;
`notes/adr/0113-*.md` (this file); `notes/ADRs.md`; `notes/adr/
README.md`. ZERO changes under any `src/` or `test/` path, zero
generated-doc regeneration (`docs/cli.md`, `docs/use-cases*`,
`docs/pipeline.md`, `docs/operators.md` all byte-unchanged — no
`docsgen` run needed or licensed), zero demo/scenario files, zero
`sim-theory.md` edits (cited only).

### Index line

```
- 2026-08-12 — sim-palgebra-unification — ADR-0113
```

(appended to `.agents/plans/roadmap.md`'s own Done section.)

### Index summary (moved verbatim from notes/ADRs.md by ADR-0143, 2026-08-16)

Sim palgebra unification, and the manual-arc rulings recorded — a docs-and-registers-only session: `.agents/rulings.md` gains "From ADR-0113," R1-R7 verbatim (the "user manual" naming ruling; the manual's chaptered shape and ed-tuesday running scenario; demos must be exercised as documented, mechanism a quickstart-generalized demo exerciser; the five-segment audience-register paring, recorded not executed; the review-3-then-manual sequence, scope verbatim, plus the manual-review skill at arc close; diagrams derive from data; the palgebra placement itself); `.agents/plans/roadmap.md`'s "Tool-specific user-guide design pass" row renames to "User manual design pass," folding in the shape and sequence rulings, with two new Next rows (review-3's user-surface scope, the demo exerciser) and a register-maintenance row (the audience paring); every non-quoted "user guide" token in both files' own live prose corrects to "user manual," verbatim author quotes left unchanged as spoken; `docs/dev/simulator-architecture.md` §4 gains "The two layers, instantiated" — `GT` as the abstract-layer object (the sim purity lint as its mechanical enforcement), the founding thesis read as two lowerings of one object (the naturality witness as their coherence law), the honest wrinkle that no `erase` exists from wire back to `GT` (why the oracle freezes bytes), four witnessed cases where `lower ⨟ erase = id` genuinely holds (the framing codecs, pacing, the latency second clock's zero point), and transport realism versus mutation restated as movement-within-a-fiber versus a different abstract object — every witness re-verified at its own path:line before landing; one pointer line each lands in `palgebra-design.md` and `trajectory-computation.md`; zero `src` change anywhere, the oracle holds pure identity across all 35 roots

### Roadmap history (moved verbatim from roadmap.md by ADR-0144, 2026-08-17)

The `.agents/plans/roadmap.md` row this ADR owns, as it stood at `deb9a33` before the ADR-0144 row contract capped rows at six lines. The live row now states what remains and cites this ADR for the rest; this is the rest, verbatim.

- **Audience register paring** (ADR-0113 R4; a small future docs
  session, not chartered). Author "Q1 a": the audience register in
  `docs/dev/AUDIENCES.md` pares to five behavioral segments --
  practitioner (agent-assistance absorbed as a global style constraint,
  evaluation as its front matter), guide reader, data consumer,
  contributor (human or agent), deferred library-consumer stub -- and
  that document's own "Seven segments" header gets corrected in the
  same edit. Ruled 2026-08-12; execution deferred to a later session.
