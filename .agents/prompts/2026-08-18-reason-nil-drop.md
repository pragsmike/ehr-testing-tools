# Archived prompt: reason-nil-drop (2026-08-18)

Session prompt -- S-1 lands under its own bump (1.1.0 -> 1.2.0) and
the deprecation clause gains its no-external-consumer waiver -- ADR-0151

## Context

Claude Code under R30 in ehr-testing-tools. HEAD at handoff: d4e73fc (ADR-0150
addendum; tree clean; CI green at eeb0299 per addendum; last tag
`stable-20260818-event-log-shape-defects` @eeb0299, no tag owed). Roadmap row
`roadmap.md#reason-nil-drop-owes-a-bump` (OPEN PRIORITY 1): "census S-1: every
module-compiled encounter emits `:reason nil`. The fix is written and proven
(ADR-0150 Step 2, red 2/4, green 4/4, a sibling `reason-field`) and STOPPED:
`:reason` is a required key of a closed map, so nil-dropping it forces
`{:optional true}`, which `classify-change` calls breaking. Owes a version bump
of its own; it may not share S-6's. ADR-0150 preserves the diff."

Channel probe at d4e73fc (re-derive):

* The fix, from ADR-0150 SS Step 2 (prose, NOT a patch -- the ADR describes it;
  no diff text is committed anywhere, `grep reason-field` finds only prose): a
  sibling `reason-field` beside `citation-fields` (`engine.clj` ~:366) with the
  same nil-dropping shape and its own docstring (clinical content a
  hand-authored step supplies, vs the compiler-traceability keys), used at
  `decide :admission` (~:387) and the outpatient-visit decide (~:692);
  `event_schema.clj:274` and `:387` `[:reason [:maybe [:or :string
  sim-model/Concept]]]` -> `{:optional true}`; red test = module-compiled
  admission + outpatient visit have no `:reason` key, hand-authored step with a
  reason keeps it (4 assertions, 2 red before / 4 green after -- ADR-0150's own
  numbers).
* Contract: `schema-version` "1.1.0" (`event_schema.clj:102`); baseline frozen
  at 1.1.0; policy text :77-86; the 1.1.0 note :91-101 discloses the un-run
  deprecation window and says "a future removal with any distance from
  publication owes the window". `docs/formats.md:335` restates the clause. Both
  are the surfaces the ruling below amends (`R-law-surface-propagation`).
* Gated surfaces that will move: `event-schema.edn`, `event-examples.edn`,
  formats.md event-log section (docsgen), `event-schema-baseline.edn` (freeze),
  `demos/traces/*/ground-truth.edn` where module-compiled encounters appear
  (`make traces`), the manual's invariance digest (`d00bf49c...` after ADR-0150
  -- ed-tuesday has module-compiled admissions per the census's 48/692, so
  predict it MOVES; re-witness).
* Oracle: `:reason` is not rendered by any HL7 builder on the oracle's five-arg
  emitter path (Z-segments are outside oracle coverage -- ADR-0150 (a)); predict
  `bin/regression-oracle d4e73fc HEAD` IDENTICAL. Grep `:reason` in
  `emit_hl7.clj`/`emit_fhir.clj` at Step 0 to confirm before asserting it.

Small session: one sibling fn, two call sites, one schema flag, one bump, one
freeze, policy text on two surfaces, register hygiene.

## Read first

1. ADR-0150 SS Step 0 prediction (b), SS Step 2 (whole), SS Step 3 (the bump
   mechanics you are repeating), the addendum.
2. `event_schema.clj` :30-104 (policy + version notes), :270-280, :383-390,
   `write-baseline!`; `event_schema_test.clj` :105-140;
   `event-schema-baseline.edn` header; `docs/formats.md` :325-345.
3. `engine.clj` :360-395, :686-696; `.agents/plans/2026-08-16-event-log-
   census.md` S-1 row (:558-586).
4. `rulings.md#R-session-verifies-ci-via-gh`, `#R-full-suite-before-push`,
   `#R-red-pushed-with-green`, `#R-law-surface-propagation`,
   `#R-register-hygiene-at-close`; build-session skill; `:sim` reading set.

## Author rulings, verbatim

* "No consumers yet, relax deprecation rules for now. Accept recommendations.
  go." (2026-08-18) -- i.e. Q1 (a): the deprecation window is WAIVED while the
  event contract has no consumer outside this repository, written into the
  policy so the waiver expires by itself; Q2 (a): S-1 lands now as its own
  session, its own bump.
* Tag: no tag owed at Step 0. This session's own close tag: pay in-session if
  its tip run concludes success while open, else next Step 0 -- say which
  (`R-session-verifies-ci-via-gh`).

## Step 0

Fresh clone, tip d4e73fc; `bin/preflight`; baseline `make test` unpiped,
MAKE_EXIT captured, reconcile vs ADR-0150's 346 blocks / 3,926 tests / 17,638
assertions; `poly check`; reading sets vs baselines. Predictions into the ADR
before any src edit: (a) `classify-change` rows -- exactly the two ADR-0150
recorded, `:admission` and `:outpatient-visit` "required -> optional", nothing
else; (b) the bump the policy owes -- quote the clause -- predicted 1.1.0 ->
1.2.0 MINOR; (c) which committed ground-truth artifacts carry `:reason nil`
today (traces, `events.edn` fixtures, manual digest) and will change; (d) oracle
IDENTICAL, with the grep that says why.

## Step 1 -- policy amendment (docs + docstring, its own commit, FIRST)

So the fix lands under the amended law, not before it. In `schema-version`'s
docstring replace the deprecation sentence with the waived form: a key or kind
slated for removal is marked deprecated in `docs/formats.md` for one minor
release before it goes -- WAIVED while the event contract has no consumer
outside this repository (no Clojars publication, no downstream repo pinning
`:event-schema-version`); the waiver expires on the first such consumer, at
which point the clause binds unamended; each removal made under the waiver says
so in its version note. Rewrite the 1.1.0 note's last sentence to "made under
the waiver, disclosed" (dated, not silently edited -- R-RP). Same sentence at
`docs/formats.md:335`. Commit: "docs: event-contract deprecation window waived
while no external consumer exists; expires on first publication (author ruling
2026-08-18, ADR-0151)"

## Step 2 -- S-1 red

Re-write the ADR-0150 test from its description (it is not in the tree):
module-compiled admission and outpatient visit -> `(not (contains? ev
:reason))`; hand-authored step with `:reason` -> present and equal. Plus
`event-schema-test`'s existing gate goes red on its own once Step 3 flips the
flag without the bump -- do NOT pre-bump to silence it. Commit: "test: red --
module-compiled encounters carry no nil :reason (ADR-0151, S-1)"

## Step 3 -- S-1 green + bump + freeze

`reason-field` beside `citation-fields` (own docstring, ADR-0150's distinction
stated); call it at both decides; `:reason` `{:optional true}` at :274 and :387
(nowhere else -- `:step-rejected`'s :492 is a different key). Run
`classify-change`: actual vs prediction (a) exact or STOP. Bump `schema-version`
to 1.2.0 with a dated note (S-1; made under the waiver, disclosed; ADR-0151);
`make event-schema-freeze` ONCE; `make docsgen`; `make traces`; re-witness the
manual's invariance digest (old `d00bf49c...`, new recorded); a fresh `sim run`
manifest reads `"1.2.0"` (assert -- extend ADR-0150's manifest test or its
version-independent property, do not add a literal pin). Oracle
`bin/regression-oracle d4e73fc HEAD` IDENTICAL or STOP. Every changed line in
regenerated artifacts is one of two classes -- `:reason nil` line removed / bump
stamp -- an "other" is a STOP. Full `make test` before the push
(`R-full-suite-before-push`); push red+green together. Commit: "feat!: event
contract 1.1.0 -> 1.2.0: :reason optional on :admission/:outpatient-visit;
module-compiled encounters no longer emit :reason nil; baseline re-frozen
(ADR-0151, S-1)"

## Step 4 -- register hygiene

Census S-1 row: CLOSED, dated, cites ADR-0151. Roadmap:
`#reason-nil-drop-owes-a-bump` -> CLOSED under `## Done`. Nothing else re-rowed.

## Close (self-archive FIRST)

Archive to `.agents/prompts/2026-08-18-reason-nil-drop.md`; open the session
record; then ADR-0151 (predictions a-d vs actuals; classifier output verbatim;
digest old/new; the waiver text as landed), roadmap, session record with `gh run
view` id/conclusion, full `make test` reconciled per namespace vs Step 0,
`bin/post-push-verify`, tag per ruling. Commit: "docs: ADR-0151 -- S-1 under its
own bump, close"

## Fences

src: `engine.clj` (`reason-field` + two call sites), `event_schema.clj` (two
`{:optional true}`, `schema-version` + notes), their tests, `docs/formats.md`
policy sentence + generated section; NO other key, kind, or schema change; NO
S-2/S-5 work; ONE freeze, in Step 3 only; oracle IDENTICAL;
`--config`/pathway/module surfaces untouched; no test deletions; exit codes
unpiped; anchored register edits; R-RP. READ-BACK names the fence per
regenerated artifact (bytes before/after, class).
