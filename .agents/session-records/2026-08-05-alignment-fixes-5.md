# 2026-08-05 — Alignment fixes 5: the license text travels with the content — F-4 closes, gated

## Scope

Session prompt naming AR-F5-0 through AR-F5-4. Prior: alignment fixes 4
landed and was design-channel-verified (`05d6ed1`,
`notes/adr/0053-alignment-fixes-4.md`). Register rows F-2/F-3/F-4
(`.agents/plans/2026-08-05-alignment-audit-findings.md`) found that of
this repo's four Apache-2.0-sourced vendored roots, only
`components/corpus/test-fixtures/v2/simhospital/` carried the actual
Apache-2.0 license TEXT — the other three relied on NOTICE narrative
alone. The author ruled option (a): one shared license-text file,
cross-referenced from the affected NOTICEs. This session built that:
`LICENSES/Apache-2.0.txt` (byte-copy of the SimHospital `LICENSE`),
dated cross-refs in four NOTICEs (three named, one disclosed), and a
new `docs-tooling` gate proving every Apache-citing NOTICE points at
the shared text. Full account, rulings verbatim:
`notes/adr/0054-alignment-fixes-5.md`.

## Red→green evidence highlights

- **Genuine red, against the live tree, before any fix landed.** The
  new gate's `every-apache-citing-notice-points-at-the-shared-license-
  text-test` failed exactly three real NOTICEs
  (`sim/resources/sim/modules/NOTICE`, `corpus/test-fixtures/v2-nist/
  NOTICE.md`, `sim/NOTICE`) plus a fourth failure for the missing
  `LICENSES/Apache-2.0.txt` file itself — four genuine failures, zero
  false positives.
- **The predicate's own exclusion proven both ways.** Against the
  unmodified tree, `components/sim-model/resources/sim-model/
  demographics/NOTICE` (hand-curated, no Apache obligation, register
  F-2) and `components/sim-trajectory/resources/sim-trajectory/NOTICE`
  (silent on license pre-fix) both correctly did NOT trip —
  `mechanism-sanity-test` proves the same exclusion directly against
  literal excerpts of the real files, not just against the live-tree
  run's own absence of a failure.
- **Green after the fix.** Gate re-run: 3 `deftest`s / 13 assertions,
  0 failures. Full suite: 216 `Test results:` lines, 0 `FAIL`/`ERROR`
  anywhere, 511 assertions in the sim-trajectory family (unchanged from
  the Step 0 baseline). `clojure -M:poly check`: OK.
- **Canonicality verified before use.** `v2/simhospital/LICENSE` read
  in full: standard Apache-2.0 preamble, §§1–9 verbatim, unfilled
  appendix (`[yyyy] [name of copyright owner]`, brackets never filled)
  — the clean canonical source AR-F5-1 required. `diff` against the
  new `LICENSES/Apache-2.0.txt` after copy: zero bytes differ.
- **Tag verified on origin, both directions.** `git rev-parse
  stable-20260805-alignment-fixes-4^{commit}` and the peeled `git
  ls-remote --tags origin` ref both resolve to `05d6ed1` exactly; `git
  tag -l -n1` matches the ruling's message verbatim.
- `bin/regression-oracle 05d6ed1 0d36820`: all eleven vendored-root
  batches byte-identical — expected, this session touched no
  `.json`/`.edn`/`.csv` content anywhere.

## Judgment calls and disclosures

- **Two precision gaps surfaced at Step 0 preflight, put to the author
  before any code was written, not guessed.** (1) `sim-trajectory/
  NOTICE` cites no "Apache" text at all pre-fix, so it cannot
  participate in a genuine red trip the way the other two named
  targets can — resolved: its append states the Apache-2.0 governance
  explicitly (not just a bare cross-ref), and the asymmetry (3 live red
  trips + 1 proactive fix, not uniform-by-construction) is disclosed
  rather than smoothed over. (2) `demographics/NOTICE` contains the
  literal string "Apache-2.0" as background context while explicitly
  disclaiming derivation — a naive substring gate would have
  false-positived on it forever; resolved by a declarative-pattern
  predicate (a `License:`/`License |` line naming Apache, or an `under
  Apache` phrase) rather than a plain substring search, verified
  against both files' own real sentences.
- **`components/sim/NOTICE` disclosed as a fourth target, per the
  prompt's own "disclose either way" clause.** Inspection found it
  does assert Apache-2.0 governs content it describes (`resources/
  modules/NOTICE`'s own vendoring, and SNOMED CT codes riding the
  vendored Synthea module's own distribution) without a text pointer —
  treated identically to the three named targets (terse dated
  cross-ref).
- **Option (a) over option (b), per the register's own recommendation
  and the author's ruling.** One shared `LICENSES/Apache-2.0.txt`
  cross-referenced from four NOTICEs, rather than vendoring a second
  per-root `LICENSE` copy into `v2-nist/` matching the SimHospital
  pattern — less duplicative, and it makes the SimHospital `LICENSE`
  the single canonical source going forward (closing register row F-3
  by unification, not by duplicating the inconsistency's own pattern a
  second time).
- No other deviation. The gate's file-tree walk (`target/`, `.git/`
  pruned, matching on filename `NOTICE`/`NOTICE.md`) found exactly the
  five NOTICE files expected — no sixth surfaced.

## Findings and HEAD landed

No findings beyond the disclosures above. Commits, in order: tag
`stable-20260805-alignment-fixes-4` (AR-F5-0, created and pushed by the
author at `05d6ed1` — AUTHOR ACTION per the build-session skill,
verified by the session afterward), `0d36820` (Step 1, the license
text + four NOTICE appends + gate, one commit), and this session's own
closing records commit (Step 2).
