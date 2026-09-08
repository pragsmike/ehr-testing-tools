# 2026-09-08 -- docs currency: what the downstream can now do

Session prompt, archived verbatim as driven. Record:
[`../session-records/2026-09-08-docs-currency-downstream.md`](../session-records/2026-09-08-docs-currency-downstream.md).

---

Docs currency: what the downstream can now do -- 2026-09-08

Context. A channel review of the consumer docs at 911c9101 found three
claims that predate ADR-0179/0180 and now tell a downstream reader the
opposite of what the tree measures. Four consumer files move, prose
only; one claim is added to an existing gate. No engine run, no
bracket, no exerciser, no new figures: R-figures-file holds, and the
22,500-arrival cell is NOT quoted here -- it enters figures.edn in a
later measurement session, then the docs quote it. Correct; do not
improve.

Read first: AGENTS.md; .agents/skills/build-session/SKILL.md;
docs/future-features.md:130-150 and :176-186;
docs/consuming-ground-truth.md:514-640 (headline :575-576; the
not-re-measured flag :604; "tracked at" :624; third flagged paragraph
:627-638; footnote :777); demos/scenarios/dense-7500/README.md:128-140;
README.md:272-279; demos/scenarios/dense-7500/figures.edn:40-56;
.agents/plans/2026-09-05-performance-measurement/measurements.md:392-431
(the census correction); .agents/plans/roadmap.md:270;
components/docs-tooling/test/ehrt/docs_tooling/dense_7500_figures_test.clj
(population :64-65, table locator :106-116, num :118-124);
link_footnote_gate_test.clj:53-99 (links resolve by path, anchors
stripped; no visible ADR token in prose -- footnote it).

Author rulings, binding:
- "go" (2026-09-08), sub-choice (i): README.md joins the figures gate.
- R-figures-file: figures a consumer doc quotes live in figures.edn;
  no new wall or count enters prose from any other source.
- R-move-not-improve: each step edits the cited lines; nothing else.
- R-sentinel: read `gh run view <id>` yourself; no sleep waiters.

Steps (each: `make test` green, then commit).
1. future-features.md:141-143. The zero was the retracted predicate
   (measurements.md:392-431): the case is reachable -- 21 at the 7,500
   cell (dense-7500/config.edn byte for byte), 43 at 533,147 events.
   Keep the paragraph's argument, flipped: 2,850 merges yielding 43
   post-merge results is why the inventory column matters. Link the
   correction section (relative path; the gate ignores anchors). Name
   no ADR in this file. Invariant: :176-186 untouched in this commit.
   Commit: "docs: result-after-merge is reachable -- 21 at 7,500, 43 at
   533,147; the zero was the predicate"
2. The 10^6 claim, three places, no new numbers. consuming-ground-
   truth.md:575-576 -> a headline with no figure: 10^5 comfortable; the
   decade above was run to 22,500 arrivals under the index-and-fold
   programme[^adr-0180] and is not quoted until it has a committed
   cell; 10^6 has not been run. :624 "tracked at" -> closed (roadmap
   :270). :627-638 gains one sentence: its conclusion is superseded in
   direction by that programme; the figures stay un-re-measured.
   future-features.md:182: drop "reaches the 3.88 GB ceiling at
   ~533,000 events"; keep the stance; point at Scale. dense-7500
   README:134-136: "the only path that reaches that decade" -> what is
   warranted. Invariant: neither quoting table changes a byte
   (dense_7500_figures_test green untouched). Commit: "docs: 10^6 is
   unwitnessed, not declined -- three claims re-scoped to what the tree
   measures"
3. README.md:274-275: 1.3574 -> 1.3327, 0.643 -> 0.6489 (figures.edn
   :44, :52). Add one deftest to dense_7500_figures_test that locates
   that paragraph's two bolded figures by regex and holds them equal
   to :quoted's config-edn-7500 and config-bare-7500 msg-per-event.
   Red first against the stale README, then green; co-land. Invariant:
   the locator matches exactly two figures. Commit: "docs: README's
   msg/event figures re-quoted from figures.edn and gated there"
4. Session record (archive this prompt). Push; `gh run view` the
   per-push run; close-marker commit records its success sha; push;
   confirm the marker's own run.
