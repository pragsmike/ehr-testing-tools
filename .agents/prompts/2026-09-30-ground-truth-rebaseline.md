# The ground-truth rebaseline: reinstated stays close; churn candidates in patient-id order (ADR-0185)

> Archived verbatim (charter R-A). The session it drove is
> `.agents/session-records/2026-09-30-ground-truth-rebaseline.md`, which
> records where the live tree differed from this prompt: test 1(b) lives
> in `ehrt.sim.run-test` rather than a sim-check namespace (that is
> where both corpora are already generated, and sim-check's classpath
> carries neither config-file merging nor module resolution); the delay
> pair `[240 1440]` minutes is the session's own choice, the ruling
> having fixed the shape and named no range; and the event fleet's
> `churn-run` had to move its authored `:bed-swap`/`:merge` ahead of
> the discharge, since a step authored after a legal cancel now runs
> after the closer. Beyond the prompt's scope, and found by the suite and
> `make integration`: the conformance full-capability baseline, a
> vacuous cancel-index witness, and the whole ed-tuesday narrative
> (README, root README, manual chapters 01/04/05, use-case catalog),
> each moved by this change and each re-witnessed in its own commit.

Repo `ehr-testing-tools`, WSL clone `/home/mg/src/ehr-testing-tools`,
`main` at `c19b1024` (plus the prior session's uncommitted close note,
committed first as `cda0f2a0`).

---

# The ground-truth rebaseline: reinstated stays close; churn candidates in patient-id order (ADR-0185)

Context: roadmap rows [cancel-discharge-reopens-an-encounter-that-never-closes] (P4) and
[determinism-hash-order-dependence] (P1), both held for the downstream window since 2026-09-12. A legal
`:cancel-discharge` reinstates a stay and nothing ever closes it (224 of 224 in downstream's 28k corpus;
54 at the 10^5 cell); churn's two candidate draws resolve positionally over a hash-ordered vector.
ONE declared ground-truth change; every churn-bearing root DIFFERS by design; dense-7500's cells and the
28,024 cell are re-measured. Baseline c19b1024 or the current tip.

Read first:
- components/sim-engine/src/ehrt/sim_engine/decide.clj:1838-1875 (cancel-discharge), the :delay and :discharge decides, :order's :schedule-followup (precedent for a decide owing more work)
- components/sim-engine/src/ehrt/sim_engine/churn.clj (sample-profile; the applicability oracle's end-gap rule; strip)
- components/sim-engine/src/ehrt/sim_engine/fold.clj:515-610 (merge-eligible, swap-eligible); streams.clj (uniform-choice)
- components/sim-engine/test/ehrt/sim_engine/eligible_index_test.clj; notes/adr/0180-*.md:449-end (the R-hash-order addendum)
- AGENTS.md:355-360; demos/scenarios/dense-7500/{figures.edn,README.md}; bin/demo-exerciser-dense-7500
- components/sim-check/src/ehrt/sim_check/check.clj (every-encounter-is-opened-and-closed-or-still-open, admitted-occupies-one-slot)
- .agents/plans/roadmap.md rows 1 and 4; notes/adr/0179-*.md (merge releases the bed -- the sibling fidelity ruling)

Author rulings 2026-09-30: "Accept recommendation" -- same discharge, fixed authored delay range, every
reinstated stay. "Prioritize simplicity of design over strict preservation of determinism" -- an extra
draw is acceptable; byte-identity with prior corpora is NOT owed; downstream accepts a different dataset
for the same arguments this once. Determinism itself (seed -> bytes) is unchanged and is gated.

Steps:
1. Red tests. (a) engine: a pathway with an authored `:cancel-discharge` after its `:discharge` yields a
   SECOND `:discharge` for that patient after a positive delay, the bed released, `check-all` ok -- red
   today (stay never closes). (b) sim-check: `reinstated-stay-without-closer` (describe's measure) reads
   0 on seed-202-ed-tuesday and the dense 750 cell -- red today (1 and >0). (c) fold: `merge-eligible` and
   `swap-eligible` return candidates sorted by `:patient-id` for the hand-built colliding pair, and a
   test that permuting insertion order of `:patients` leaves both views equal -- red today.
   (d) determinism: two runs of the 750 cell at HEAD are byte-identical -- must be green before AND after.
   Invariant: red for the stated reasons only.  Gate: brick:sim-engine, brick:sim-check.
2. Row 4: `decide :cancel-discharge`'s success arm adds `:prepend-steps [{:type :delay :from A :to B}
   {:type :discharge}]`, A/B one named constant pair in churn.clj with a one-paragraph rationale (the
   `:delay` draws on the patient stream through the existing decide; the discharge reads the restored
   location through the existing decide). No new kind, no InjectChurn change, `strip` untouched.
   Invariant: 1(a)(b) green; `admitted-occupies-one-slot` and the encounter laws unchanged in text.
3. Row 1: sort both eligible views by `:patient-id` before `uniform-choice`; supersede the R-hash-order
   addendum with a dated note on ADR-0180; flip `eligible_index_test`'s colliding-pair assertion to
   order-independence; AGENTS.md:357-359 reclaims "no hash-order dependence" and drops the row pointer.
   Invariant: 1(c) green; no remaining `PersistentHashMap`-order read feeds a draw (grep uniform-choice
   callers and say so).  Gate: brick:sim-engine; poly check.
4. Measure before the banners: predict which oracle roots DIFFER (every root run with churn or carrying a
   `:merge`/`:bed-swap`/`:cancel-discharge`) and which stay IDENTICAL; then `bin/ground-truth-bracket
   <base> HEAD --declared-digest-change` and `bin/regression-oracle <base> HEAD --declared-digest-change`.
   Invariant: the DIFFERS set equals the prediction; every other root IDENTICAL; both lists in the record.
5. Figures: `bin/demo-exerciser-dense-7500` rewrites `:asserted`; re-measure the `:quoted` cells it does
   not run (nobed, bare, 750 wall/RSS, and the 28,024 cell: one run, new bytes and SHA). The 28k
   provenance block keeps the pre-rebaseline SHA 589600c5… labelled as such with the downstream
   reproduction note, and records the new SHA as current. README + Scale table + consuming-ground-truth
   figures follow; the figures gate green. `describe` over the new 750 cell: reinstated-stay-without-closer 0.
6. Docs: ADR-0185 (Accepted): the closer rule, the constant pair, the sort, the simplicity ruling and
   what it relaxed (byte-identity, fixed consumption for this sweep) and did not (determinism, the
   checker); rows 1 and 4 -> CLOSED; `make docsgen && git diff --exit-code` clean.
7. Not docs-only: full `make test` unpiped, MAKE_EXIT recorded, wrapper ends `exit "$MAKE_EXIT"`.
   Record + prompt archive. Push; gh run view the tip; CI green is the close marker. Record carries the
   downstream line: "ground truth moves on every churn-bearing corpus: reinstated stays now close after
   a delay, and churn's candidate choice is in patient-id order; schema unchanged (no new kind); your
   retained corpora remain self-check-valid as pre-rebaseline artifacts; regenerate for the new bytes."
