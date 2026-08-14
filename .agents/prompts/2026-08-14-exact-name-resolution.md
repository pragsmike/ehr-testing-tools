# Archived prompt: exact-name-resolution (2026-08-14)

Original driving prompt below, verbatim. Executed as ADR-0133, all
four steps landed as scoped -- TWO STOP-AND-REPORTs mid-Step-2, both
relayed to the author and both ruled ("the restoration cascade"):
`gmf-interpreter.clj`'s own `max-steps` backstop switched to
reset-on-any-advance semantics (a real, legal recurring-care loop was
false-firing the OTHER ADR-0105-licensed semantics, unmasked by the
restoration), and `compile-trajectory.clj`'s own `encounter->step`/
`encounter-end->step` gained a `:virtual` clause at both dispatch
sites (resolving a decision ADR-0029 D3f's own `gmf.clj` docstring had
explicitly deferred to "whichever future session first exercises a
closure through the full compile-trajectory pipeline" -- this one).
The declared-oracle-change prediction (Step 1: 5 roots MOVE) matched
the official `bin/regression-oracle` bracket (Step 3) on 4 of 5;
`hypothyroidism` was predicted to move but stayed byte-identical,
investigated and explained (both its own collision-pair members are
`:exact`-severity Symptom states whose only effect is never read
downstream in this module -- restored, real, but structurally
unobservable), not a bug. See `notes/adr/0133-exact-name-
resolution.md` for the full account, both rulings' verbatim text and
"Executed exactly as ruled" record in `.agents/rulings.md`.

---

# Session prompt — ADR-0133: exact-name state resolution
# (collision fix, vendoring-rider row) — 2026-08-14

You are Claude Code executing under R30 ceremony in
ehr-testing-tools. mg is the sole author and ruling authority.
This session lands the vendoring-rider row chartered by ADR-0131:
resolving the 10 slug-collision pairs across 5 vendored modules.

## Context

ADR-0131's census found 10 state-name pairs (in colorectal_cancer,
hypothyroidism, injuries, sleep_apnea, veteran_ptsd .json) whose
raw names fold to the same slug key; the current loader kebab-keys
every JSON key at parse, so the last-parsed member of each pair
silently overwrites the other, and transition targets (folded
through the same slug) misroute both inbound paths through the
surviving definition. The design channel's probe (2026-08-14,
verified against the live tree and upstream Synthea master)
established: in ALL 10 pairs both members are distinct live states
(different inbound edges, outbound targets, payloads — in
hypothyroidism the pair is a chain, one transitioning into the
other); upstream retains all 20 states (no renames — under
Synthea's exact-string namespace there is no collision); the
defect is wholly ours.

## Author rulings (verbatim, .agents/rulings.md gets these)

- Resolution option: "b" — loader-side exact-name resolution.
  (Channel framing ruled on: raw-name → key table at load;
  colliding names get deterministic disambiguated keys; every
  name-valued reference resolves by exact raw string through the
  table, never through slug. Modules stay verbatim — ADR-0071
  vendoring is preserved untouched, NOTICE hashes do not move.)
- Riding (b), per the channel recommendation mg ruled on: the
  warn→hard-error escalation chartered by ADR-0131 is DISCHARGED —
  collisions are now handled, not tolerated. The guard's warning
  becomes a disambiguation disclosure (still to *err*, new text).
  The residual strictness is structural: a name-valued reference
  that misses the table is a load REJECTION (stronger than today's
  silent dangling key). STOP if implementation reveals this
  reading of (b) to be wrong.

## Read first

- notes/adr/0131-slug-edn-round-trip.md (the census, the charter)
- components/sim-trajectory/src/ehrt/sim_trajectory/gmf.clj —
  slug, kebab-key, raw-state-names, state-name-collision-groups,
  handle-state-name-collision!, load-module, and EVERY
  normalize-* site that currently applies (keyword (slug t)) to a
  name-valued field (~lines 340-410; derive the authoritative
  list from the code, not this prompt)
- components/sim-trajectory/test/ehrt/sim_trajectory/gmf_test.clj
  (collision-guard tests — they lock the current warning text and
  WILL go red)
- .agents/skills/build-session/ (receipts, red capture, sweep
  census, checkpoint practice)
- bin/preflight, bin/tag-ceremony, bin/close-scaffold usage

## Standing text

- Anti-fabrication tripwire: drafting a skip-justification IS the
  stop. If you find yourself writing why a ceremony step doesn't
  apply, STOP and surface instead.
- Step-0 receipts: every ceremony command's output pasted into
  the session-record draft BEFORE Step 1.
- Red-before-green for all src changes; capture the red in the
  session record. Full `make test` before every push.
- Reading-set budget lock is ROUTINE-COMPANION: re-derive inline
  by the standing formula with a dated comment block; STOP only
  if measurement contradicts a recorded figure.
- Channel pre-probe figures below are EXPECTED BASELINES, not
  facts: re-derive every one; they are budgeted to be wrong.
- Declared-oracle-change ceremony applies (ADR-0131's own shape:
  predict → fix → witnessed re-baseline → records).

## Step 0 — ceremony

Run bin/preflight; paste output. Pay the licensed tag:
bin/tag-ceremony stable-20260814-clinic-decade c3b6fbc --push
(CI confirmed green by mg's gh-run-list glance, 2026-08-14;
license unconditional). Paste peeled-ref verify output. STOP if
preflight is red or HEAD is not c3b6fbc-descendant-clean.

## Step 1 — census + prediction commit

Re-derive and record in notes/adr/0133-exact-name-resolution.md:

a. The 10 collision pairs / 5 modules (STOP if the count
   disagrees with ADR-0131's recorded 10/5).
b. The complete inventory of name-valued fields, derived from
   gmf.clj's own normalize sites AND docs/gmf-interpreter.md — the
   channel's walker found: direct_transition,
   distributed_transition, conditional_transition (incl. nested),
   complex_transition, type_of_care_transition,
   lookup_table_transition, and PriorState conditions' "name"
   (46 refs). Expected ~2,331 total refs, ZERO exact-match misses
   against each module's raw state names (the finding that makes
   strict rejection safe). Re-derive with the real field
   knowledge; the walker is expected to undercount.
c. Capture check with the REAL slug: no existing raw name slugs
   to any candidate disambiguation key (channel found none with
   an approximate fold). The suffix rule must still be
   capture-avoiding by construction, not by luck.
d. Closure census: which oracle roots carry any of the 5
   modules. Predict per-root: those roots MOVE (previously
   overwritten states resume executing — real trajectory
   changes); every other root predicts pure identity. Also
   predict: the 10 ADR-0131-recorded collision WARNINGs are
   REPLACED by disambiguation disclosures.

Commit: "docs: exact-name resolution census and
declared-oracle-change prediction (ADR-0133)"

## Step 2 — red, then fix

Red first (capture in record): existing collision-guard tests red
on the retired warning text; new tests —
- cardinality: for every module, (count (:states loaded)) equals
  the raw-name count (the invariant the overwrite violated);
- disambiguation determinism: first occurrence in file order
  keeps the bare slug key; subsequent get a deterministic suffix;
  same file bytes → same table, always;
- capture avoidance as a PROPERTY (generative), not just the
  current-tree absence;
- strict miss: a name-valued reference absent from the table is a
  load rejection with a named reason;
- PriorState "name" resolves through the table (it is a
  name-valued field, currently slug-folded at gmf.clj ~347);
- restored-semantics witness: hypothyroidism's chain
  ("Hypothyroid symptom" → "Hypothyroidism" → "hypothyroidism" →
  "Hypothyroid Condition Onset") loads as FOUR distinct states
  with the chain intact.

Then implement: states built from the string-keyed parse (the
raw-state-names pathway becomes primary for state identity);
internal state-def keys still kebab-keyed; every name-valued
field resolves raw-string → key via the table; guard message
becomes the disambiguation disclosure. Keep the change scoped:
move-don't-improve — one sanctioned improvement is the strict-
miss rejection (ruled above); nothing else.

Commit: "fix: exact-name state resolution -- raw-name table,
deterministic disambiguation, strict-miss rejection; guard
becomes disclosure (ADR-0133)"

## Step 3 — oracle re-baseline

Run the oracle; verify prediction: predicted roots moved,
all others byte-identical, disclosures replaced warnings exactly
as predicted. STOP on any unpredicted movement or any predicted
mover staying identical. Re-baseline per declaration; witness one
moved root's trajectory diff showing a restored state in events.

Commit: "test: oracle re-baseline per declaration; restored-state
trajectories witnessed (ADR-0133)"

## Step 4 — records + close

ADR-0133 completed; roadmap row closed; rulings register entries
(both rulings above, provenance [A]); session record with all
receipts; prompt self-archived to .agents/prompts/. Full
`make test` green; push; bin/post-push-verify;
bin/close-scaffold --expect-tag
stable-20260814-clinic-decade@c3b6fbc.

## Fences

- NO edits to any file under components/sim/resources/sim/modules/
  — verbatim vendoring is the point of ruling (b). NO NOTICE
  changes.
- No slug-function changes (ADR-0131's fold is settled law); this
  session changes RESOLUTION, not folding.
- Companion files by rule: ADR, roadmap, rulings, session record,
  prompt archive all inside the fence.
- index_completeness_test: any new .agents file gets its README
  index row.
- Skill mirrors untouched (no skill edits chartered).
- STOP conditions as marked; mid-session STOPs surface to mg in
  this session's own channel.
