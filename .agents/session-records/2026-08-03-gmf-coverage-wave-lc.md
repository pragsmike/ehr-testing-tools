# 2026-08-03 — GMF coverage Wave LC: lookup-table column generalization (whitelist retired)

## Scope

The post-G census (`2026-08-03-synthea-7e08387-wave-g.edn`, ADR-0037)
ranked lookup-table columns as the leading frontier family: 9 modules
blocked on columns (`race`, `state`, `time`,
`diabetic_retinopathy_stage`, `operative_status`, `cardiac_surgery`,
`vhd_mr_risk`, `vhd_ps_risk`, `vhd_tr_risk`) the loader's own H2
whitelist (`recognized-lookup-table-columns`, `#{"gender"}`, ADR-0029
D3a) rejected outright. This session's own driving prompt pinned
upstream semantics directly against the Synthea checkout at
`7e08387c68a7f0e21d13076609a159fd473fc902`
(`Transition.java`'s own `LookupTableTransition`/`LookupTableKey`, and
`Utilities.parseDateRange` for the `time` column's date-range format —
both explicitly named as unverified-by-the-design-channel reads this
session had to do itself before transcribing anything) and ratified
five author rulings (`notes/ADRs.md` ADR-0038, AR-1 through AR-5):
generalize column resolution to module-attribute-first then a
persona-field mapping, add `:time-range` containment, add a config-
gated persona `:state` field (Wave F's pattern verbatim), oracle-
bracket the change, and re-run the census. Executed in one pass,
red→green per step, ending in the ADR, this record, and the paired
prompt archive.

## Red→green evidence

- **Step 1 (`26f280a`, loader).** `gmf-test`: 53/53, 0 failures (up
  from 50 pre-session) — one pre-existing test caught immediately by
  the new `:time-range` key (`lookup-table-csv-with-a-leading-bom-...`
  expected a 2-key row shape, now 3; fixed in the test's own fixture,
  not the code).
- **Step 2 (`6af4dc0`, interpreter).** `gmf-interpreter-test`: 153/153
  assertions across 407 total, 0 failures (up from 145 tests
  pre-session) — 8 new tests covering module-attribute resolution,
  persona-field fallback, module-attribute-wins-over-persona
  precedence, honest-absence walk-error, `:time-range` containment,
  and one-draw consumption.
- **Step 3 (`50f7efd`, persona).** `persona-test`: 19/19, 48
  assertions, 0 failures — 6 new tests (absence with no config,
  sampled when configured, distinct from the pre-existing
  `:address :state`, conditional draw-count 13/14/16, schema-valid
  property test).
- **Step 4 (oracle bracket, evidence only).** `bin/regression-oracle
  4d868df 50f7efd` — all 9 vendored root batches IDENTICAL,
  byte-verified (table in the ADR's own execution note).
- **Step 5 (`a12c911`, census).** `census-test`: 7/7, 27 assertions, 0
  failures, after the `default-persona-config` change.
- Throughout: `gitleaks git --staged -v` clean on every commit; each
  push verified against its own message file (every diff's only delta
  the `git log --format=%B` trailing-newline artifact).

## Judgment calls and their ratification status

- **The census-category rename (`:unrecognized-lookup-table-column` →
  `:malformed-lookup-table-range`, `census.clj`/`census_test.clj`) was
  folded into Step 1's own commit**, not given its own checkpoint —
  it is a direct, necessary consequence of the loader's own rejection-
  reason rename (without it, `census-test` would fail red), not a
  separate design decision. Not escalated; matches the prompt's own
  "checkpoints stay buildable, red→green per step" discipline.
- **The `:state-weights` census default was made a SINGLE-option pool
  (`{:state "Alabama" :weight 1.0}`) rather than a full ~50-state
  vocabulary**, reading AR-3's own "gains a fixed value" (singular)
  literally against Wave F's own multi-option race/SES pools (which
  cover Synthea's own CLOSED vocabularies in full) — `:state` has no
  closed vocabulary to cover, only the CSVs' own ~50-entry US state-
  name set, and a single fixed value still exercises the new draw and
  the module-attribute-vs-persona-field resolution path without
  fabricating a weighted demographic claim. Not escalated; disclosed
  in ADR-0038 AR-3's own execution note.
- **`docs/gmf-interpreter.md`'s three historical D3a/Wave-F passages
  describing the NOW-RETIRED whitelist were left in place, dated-
  annotated rather than rewritten** (the "RETIRED"/"BUILT"/"RESOLVED"
  inline notes, matching this repo's own established errata-sweep
  precedent for prose describing a past decision that has since
  changed) — the section's own account of what D3a/Wave F actually
  built stays the historical record.

## Findings and HEAD landed

- No live, unplanned finding this session — the census movement (all
  9 predicted modules moved cleanly, zero surfaced a next blocker,
  zero regressed) matched AR-5's own expectation exactly, traced by
  direct EDN verdict-set diff against the post-G artifact, not
  eyeballed.
- This session ran under R30 (the standing default, ADR-0007/ADR-0023)
  — every checkpoint committed and pushed by this session itself.
- Commits, in order: `26f280a` (Step 1, loader generalization +
  census category rename), `6af4dc0` (Step 2, walk-time resolution),
  `50f7efd` (Step 3, persona `:state`), `a12c911` (Step 5, census
  re-run + persona-config default), and this commit (Step 6 — ADR-0038,
  roadmap capture, `docs/gmf-interpreter.md` errata, this record and
  its paired prompt archive, both indexed).
- **Fence, explicit:** this session did NOT touch the schema-invalid
  family (`injuries`/hospice's own `complex_transition`
  NamedDistribution gap), the vital-sign channel (ADR-0036 AR-7,
  still deferred), or Wave H — exactly the driving prompt's own Fences
  section. AR-1(b)'s two named session-reads (`LookupTableKey`'s own
  case semantics, `Utilities.parseDateRange`'s two forms) were both
  done directly against the pin before transcribing anything, per the
  prompt's own explicit instruction that the design channel had not
  pre-verified either.
